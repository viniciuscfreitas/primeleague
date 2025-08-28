package br.com.primeleague.chat.services;

import br.com.primeleague.api.LoggingService;
import br.com.primeleague.api.dto.LogEntryDTO;
import br.com.primeleague.chat.PrimeLeagueChat;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serviço de logging assíncrono para mensagens de chat. (Versão Canônica v2.0)
 * Implementa LoggingService e confia nos dados em cache do DataManager.
 * 
 * MELHORIAS ARQUITETURAIS:
 * - Implementa interface LoggingService
 * - Confia em player.getUniqueId() após login
 * - Zero queries desnecessárias por mensagem
 * - Comunicação via API limpa
 */
public class ChatLoggingService implements LoggingService {

    private final PrimeLeagueChat plugin;
    private final BlockingQueue<ChatLogEntry> logQueue;
    private final AtomicBoolean running;
    private final Thread loggingThread;

    // Classe interna para representar uma entrada de log de forma segura.
    private static class ChatLogEntry {
        final String channelType;
        final UUID senderUuid;
        final String senderName;
        final UUID receiverUuid;
        final String receiverName;
        final Integer clanId;
        final String messageContent;
        final long timestamp;

        ChatLogEntry(String channelType, Player sender, Player receiver, Integer clanId, String messageContent, ChatLoggingService service) {
            this.channelType = channelType;
            // OBTER UUID DO NOSSO SISTEMA, NÃO DO BUKKIT
            this.senderUuid = service.getPlayerUuidFromDataManager(sender);
            this.senderName = sender.getName();
            this.receiverUuid = (receiver != null) ? service.getPlayerUuidFromDataManager(receiver) : null;
            this.receiverName = (receiver != null) ? receiver.getName() : null;
            this.clanId = clanId;
            this.messageContent = messageContent;
            this.timestamp = System.currentTimeMillis();
            
            // Verificar se o UUID do remetente é null (jogador não encontrado no DataManager)
            if (this.senderUuid == null) {
                service.plugin.getLogger().severe("🚨 [CHAT-LOG] JOGADOR NÃO ENCONTRADO NO DATAMANAGER: " + this.senderName + " - PULANDO LOGGING");
                throw new IllegalStateException("Jogador não encontrado no DataManager: " + this.senderName);
            }
        }
        
        ChatLogEntry(String channelType, UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, Integer clanId, String messageContent, long timestamp) {
            this.channelType = channelType;
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.receiverUuid = receiverUuid;
            this.receiverName = receiverName;
            this.clanId = clanId;
            this.messageContent = messageContent;
            this.timestamp = timestamp;
        }
    }

    public ChatLoggingService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.logQueue = new LinkedBlockingQueue<>();
        this.running = new AtomicBoolean(true);

        this.loggingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                processQueue();
            }
        }, "PrimeLeague-ChatLoggingThread");
        this.loggingThread.setDaemon(true);
        this.loggingThread.start();
    }

    /**
     * Implementação da interface LoggingService.
     * Registra uma mensagem de chat usando LogEntryDTO.
     */
    @Override
    public void logChatMessage(LogEntryDTO entry) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) {
            return;
        }
        
        // Converter LogEntryDTO para ChatLogEntry interno
        ChatLogEntry chatEntry = new ChatLogEntry(
            entry.getChannelType(),
            entry.getSenderUuid(),
            entry.getSenderName(),
            entry.getReceiverUuid(),
            entry.getReceiverName(),
            entry.getClanId(),
            entry.getMessageContent(),
            entry.getTimestamp()
        );
        
        boolean added = logQueue.offer(chatEntry);
        if (!added) {
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Fila cheia, mensagem DTO descartada");
        }
    }
    
    /**
     * Método de compatibilidade para uso interno do plugin de chat.
     * Adiciona uma mensagem à fila de logging usando objetos Player.
     */
    public void logMessage(String channel, Player sender, Player receiver, String message) {
        if (!plugin.getConfig().getBoolean("logging.enabled", true)) {
            return;
        }
        

        
        try {
            Integer clanId = getClanIdFromPlayer(sender);
            ChatLogEntry entry = new ChatLogEntry(channel, sender, receiver, clanId, message, this);
            
            boolean added = logQueue.offer(entry);
            if (!added) {
                plugin.getLogger().warning("⚠️ [CHAT-LOG] Fila cheia, mensagem descartada");
            }
        } catch (IllegalStateException e) {
            // Jogador não encontrado no DataManager - não fazer logging
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Pulando logging para jogador não registrado: " + sender.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("🚨 [CHAT-LOG] Erro inesperado ao criar ChatLogEntry: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processQueue() {
        long batchInterval = plugin.getConfig().getLong("logging.batch-interval", 5000);
        int maxBatchSize = plugin.getConfig().getInt("logging.batch-size", 100);
        List<ChatLogEntry> batch = new ArrayList<>(maxBatchSize);



        while (running.get() || !logQueue.isEmpty()) {
            try {
                // Drena a fila para um lote, respeitando o tamanho máximo
                int drained = logQueue.drainTo(batch, maxBatchSize);
                
                if (drained > 0) {
                    persistBatch(batch);
                    batch.clear();
                }

                // Aguarda o próximo intervalo ou se a thread for interrompida
                Thread.sleep(batchInterval);
            } catch (InterruptedException e) {
                        plugin.getLogger().warning("⚠️ [CHAT-LOG] Thread de logging interrompida");
        Thread.currentThread().interrupt();
        break;
    }
}
    }

    private void persistBatch(List<ChatLogEntry> batch) {
        if (batch.isEmpty()) {
            return;
        }
        
        // REFATORADO: Query usando player_id em vez de UUID
        String sql = "INSERT INTO chat_logs (channel_type, sender_player_id, sender_name, receiver_player_id, receiver_name, clan_id, message_content) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getDatabaseConnection()) {
            if (conn == null) {
                plugin.getLogger().severe("🚨 [CHAT-LOG] Falha ao obter conexão com banco de dados!");
                return;
            }
            
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                int successCount = 0;
                int errorCount = 0;
                
                for (ChatLogEntry entry : batch) {
                    try {
                        // REFATORADO: Converter UUID para player_id usando IdentityManager
                        Integer senderPlayerId = getPlayerIdFromUuid(entry.senderUuid, entry.senderName);
                        Integer receiverPlayerId = null;
                        
                        if (entry.receiverUuid != null) {
                            receiverPlayerId = getPlayerIdFromUuid(entry.receiverUuid, entry.receiverName);
                        }
                        
                        // Verificar se o player_id do remetente foi obtido
                        if (senderPlayerId == null) {
                            plugin.getLogger().warning("⚠️ [CHAT-LOG] Player ID do remetente não encontrado: " + entry.senderName + " (UUID: " + entry.senderUuid + ") - Pulando entrada");
                            continue;
                        }
                        
                        // Verificar se o player_id do destinatário foi obtido (se houver)
                        if (entry.receiverUuid != null && receiverPlayerId == null) {
                            plugin.getLogger().warning("⚠️ [CHAT-LOG] Player ID do destinatário não encontrado: " + entry.receiverName + " (UUID: " + entry.receiverUuid + ") - Pulando entrada");
                            continue;
                        }
                        
                        // REFATORADO: Query usando player_id
                        stmt.setString(1, entry.channelType);
                        stmt.setInt(2, senderPlayerId); // player_id do remetente
                        stmt.setString(3, entry.senderName);
                        if (receiverPlayerId != null) {
                            stmt.setInt(4, receiverPlayerId); // player_id do destinatário
                        } else {
                            stmt.setNull(4, java.sql.Types.INTEGER);
                        }
                        stmt.setString(5, entry.receiverName);
                        if (entry.clanId != null) {
                            stmt.setInt(6, entry.clanId);
                        } else {
                            stmt.setNull(6, java.sql.Types.INTEGER);
                        }
                        stmt.setString(7, entry.messageContent);
                        stmt.addBatch();
                        
                        successCount++;
                        
                        plugin.getLogger().info("✅ [CHAT-LOG] Mensagem preparada para batch: " + entry.senderName + " → " + entry.receiverName + " (clan_id: " + entry.clanId + ")");
                        
                    } catch (Exception e) {
                        errorCount++;
                        plugin.getLogger().severe("🚨 [CHAT-LOG] Erro ao preparar entrada do batch:");
                        plugin.getLogger().severe("   👤 Remetente: " + entry.senderName + " (UUID: " + entry.senderUuid + ")");
                        plugin.getLogger().severe("   💬 Mensagem: " + entry.messageContent);
                        plugin.getLogger().severe("   ❌ Erro: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                if (successCount > 0) {
                    int[] results = stmt.executeBatch();
                    conn.commit();
                    plugin.getLogger().info("✅ [CHAT-LOG] Batch executado com sucesso: " + successCount + " mensagens salvas");
                } else {
                    plugin.getLogger().warning("⚠️ [CHAT-LOG] Nenhuma entrada válida no batch, pulando execução");
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("🚨 [CHAT-LOG] Erro CRÍTICO ao persistir lote de logs de chat: " + e.getMessage());
                plugin.getLogger().severe("   📊 Tamanho do batch: " + batch.size());
                plugin.getLogger().severe("   ⏰ Timestamp: " + System.currentTimeMillis());
                
                e.printStackTrace();
                
                // Tentar rollback
                try {
                    conn.rollback();
                    plugin.getLogger().info("🔄 [CHAT-LOG] Rollback executado com sucesso");
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().severe("🚨 [CHAT-LOG] Erro ao fazer rollback: " + rollbackEx.getMessage());
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("🚨 [CHAT-LOG] Erro geral ao persistir batch: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtém conexão com o banco de dados via Core.
     */
    private Connection getDatabaseConnection() {
        try {
            // Tentar obter conexão via Core
            org.bukkit.plugin.Plugin corePlugin = plugin.getServer().getPluginManager().getPlugin("PrimeLeague-Core");
            if (corePlugin != null && corePlugin.isEnabled()) {
                // Usar reflection para acessar o DataManager
                Class<?> coreMainClass = Class.forName("br.com.primeleague.core.PrimeLeagueCore");
                java.lang.reflect.Method getInstanceMethod = coreMainClass.getMethod("getInstance");
                Object coreInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getDataManagerMethod = coreMainClass.getMethod("getDataManager");
                Object dataManager = getDataManagerMethod.invoke(coreInstance);
                
                java.lang.reflect.Method getConnectionMethod = dataManager.getClass().getMethod("getConnection");
                Connection conn = (Connection) getConnectionMethod.invoke(dataManager);
                
                if (conn != null) {
                    plugin.getLogger().info("🔗 [CHAT-LOG] Conexão com banco obtida com sucesso");
                } else {
                    plugin.getLogger().warning("⚠️ [CHAT-LOG] Conexão com banco retornou null");
                }
                
                return conn;
            } else {
                plugin.getLogger().severe("🚨 [CHAT-LOG] Plugin PrimeLeague-Core não encontrado ou desabilitado!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("🚨 [CHAT-LOG] Erro ao obter conexão com banco: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private Integer getClanIdFromPlayer(Player player) {
        try {
            // Tentar obter o ClanManager do Clans via reflection
            org.bukkit.plugin.Plugin clansPlugin = plugin.getServer().getPluginManager().getPlugin("PrimeLeague-Clans");
            if (clansPlugin != null && clansPlugin.isEnabled()) {
                // Usar reflection para acessar o ClanManager
                Class<?> clansMainClass = Class.forName("br.com.primeleague.clans.PrimeLeagueClans");
                java.lang.reflect.Method getInstanceMethod = clansMainClass.getMethod("getInstance");
                Object clansPluginInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getClanManagerMethod = clansMainClass.getMethod("getClanManager");
                Object clanManager = getClanManagerMethod.invoke(clansPluginInstance);
                
                java.lang.reflect.Method getClanByPlayerMethod = clanManager.getClass().getMethod("getClanByPlayer", Player.class);
                Object clan = getClanByPlayerMethod.invoke(clanManager, player);
                
                if (clan != null) {
                    java.lang.reflect.Method getIdMethod = clan.getClass().getMethod("getId");
                    Integer clanId = (Integer) getIdMethod.invoke(clan);
            
                    return clanId;
                } else {
            
                }
            } else {
        
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Erro ao obter clan ID: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Verifica se um UUID existe na tabela player_data.
     */
    private boolean playerExistsInDatabase(Connection conn, String uuid) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM player_data WHERE uuid = ?")) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Retorna true se encontrou o UUID
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Erro ao verificar existência do UUID " + uuid + ": " + e.getMessage());
            return false; // Em caso de erro, assume que não existe
        }
    }
    
    /**
     * Obtém o UUID correto do jogador usando o TRADUTOR DE IDENTIDADE.
     * Usa o UUID canônico do banco de dados através do mapeamento.
     */
    private UUID getPlayerUuidFromDataManager(Player player) {

        
        try {
            // Obter o DataManager do Core
            org.bukkit.plugin.Plugin corePlugin = plugin.getServer().getPluginManager().getPlugin("PrimeLeague-Core");
            if (corePlugin != null && corePlugin.isEnabled()) {
        
                
                Class<?> coreMainClass = Class.forName("br.com.primeleague.core.PrimeLeagueCore");
        
                
                java.lang.reflect.Method getInstanceMethod = coreMainClass.getMethod("getInstance");
                Object coreInstance = getInstanceMethod.invoke(null);
        
                
                java.lang.reflect.Method getDataManagerMethod = coreMainClass.getMethod("getDataManager");
                Object dataManager = getDataManagerMethod.invoke(coreInstance);
        
                
                // REFATORADO: Obter UUID canônico através do DataManager em vez de usar UUID do Bukkit diretamente
                UUID bukkitUuid = player.getUniqueId();
                java.lang.reflect.Method getCanonicalUuidMethod = dataManager.getClass().getMethod("getCanonicalUuid", UUID.class);
                UUID canonicalUuid = (UUID) getCanonicalUuidMethod.invoke(dataManager, bukkitUuid);
                
        
                
                if (canonicalUuid != null && !canonicalUuid.equals(bukkitUuid)) {
            
                    return canonicalUuid;
                } else {
                    plugin.getLogger().warning("⚠️ [CHAT-LOG] Tradutor não encontrou mapeamento para " + player.getName() + " (Bukkit UUID: " + bukkitUuid + ")");
                }
            } else {
                plugin.getLogger().warning("⚠️ [CHAT-LOG] Core plugin não encontrado ou desabilitado");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Erro ao obter UUID via tradutor para " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // NÃO usar fallback do Bukkit - jogador deve estar no DataManager
        plugin.getLogger().severe("🚨 [CHAT-LOG] JOGADOR NÃO ENCONTRADO NO DATAMANAGER: " + player.getName() + " - PULANDO LOGGING");
        return null; // Retorna null para indicar que não deve fazer logging
    }

    /**
     * Obtém o player_id de um jogador pelo UUID usando IdentityManager.
     * REFATORADO: Usa player_id como identificador principal
     */
    private Integer getPlayerIdFromUuid(UUID playerUuid, String playerName) {
        try {
            // Tentar obter via IdentityManager primeiro
            org.bukkit.plugin.Plugin corePlugin = plugin.getServer().getPluginManager().getPlugin("PrimeLeague-Core");
            if (corePlugin != null && corePlugin.isEnabled()) {
                Class<?> coreMainClass = Class.forName("br.com.primeleague.core.PrimeLeagueCore");
                java.lang.reflect.Method getInstanceMethod = coreMainClass.getMethod("getInstance");
                Object coreInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getIdentityManagerMethod = coreMainClass.getMethod("getIdentityManager");
                Object identityManager = getIdentityManagerMethod.invoke(coreInstance);
                
                java.lang.reflect.Method getPlayerIdByUuidMethod = identityManager.getClass().getMethod("getPlayerIdByUuid", UUID.class);
                Integer playerId = (Integer) getPlayerIdByUuidMethod.invoke(identityManager, playerUuid);
                
                if (playerId != null) {
                    plugin.getLogger().info("🔍 [CHAT-LOG] Player ID obtido via IdentityManager: " + playerId + " para " + playerName);
                    return playerId;
                }
            }
            
            // Fallback: buscar no banco de dados
            try (Connection conn = getDatabaseConnection()) {
                if (conn != null) {
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT player_id FROM player_data WHERE uuid = ? LIMIT 1")) {
                        stmt.setString(1, playerUuid.toString());
                        try (java.sql.ResultSet rs = stmt.executeQuery()) {
                            if (rs.next()) {
                                int playerId = rs.getInt("player_id");
                        
                                return playerId;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("⚠️ [CHAT-LOG] Erro ao buscar player_id no banco: " + e.getMessage());
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ [CHAT-LOG] Erro ao obter player_id via IdentityManager: " + e.getMessage());
        }
        
        plugin.getLogger().warning("⚠️ [CHAT-LOG] Player ID não encontrado para: " + playerName + " (UUID: " + playerUuid + ")");
        return null;
    }


    public void shutdown() {
        plugin.getLogger().info("🛑 [CHAT-LOG] Iniciando shutdown do ChatLoggingService...");
        
        running.set(false);
        if (loggingThread != null) {
            loggingThread.interrupt();
            try {
                // Aguarda um pouco para a thread terminar de processar o último lote
                loggingThread.join(2000);
                plugin.getLogger().info("✅ [CHAT-LOG] Thread de logging finalizada");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().warning("⚠️ [CHAT-LOG] Thread interrompida durante shutdown");
            }
        }
        
        // Processa qualquer item restante na fila antes de desligar
        plugin.getLogger().info("📦 [CHAT-LOG] Processando itens restantes na fila...");
        processQueue();
        
        plugin.getLogger().info("✅ [CHAT-LOG] ChatLoggingService finalizado com sucesso.");
    }
}
