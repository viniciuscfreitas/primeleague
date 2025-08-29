package br.com.primeleague.p2p.listeners;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.util.UUIDUtils;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Listener de autenticação com DEBUG COMPLETO para investigar problemas de UUID.
 * 
 * SISTEMA DE DEBUG IMPLEMENTADO:
 * - Logs detalhados em cada etapa do processo
 * - Verificação de UUID em múltiplas fontes
 * - Comparação entre algoritmos Java e Node.js
 * - Análise completa do banco de dados
 * 
 * @author PrimeLeague Team
 * @version 3.2.0 (Debug Completo)
 */
public final class AuthenticationListener implements Listener {

    private final PrimeLeagueP2P plugin;

    public AuthenticationListener() {
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    /**
     * Processa autenticação de jogadores.
     * CORREÇÃO ARQUITETURAL: Verificação de IP movida para AsyncPlayerPreLoginEvent
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        final String playerName = event.getName();
        final String playerIp = event.getAddress().getHostAddress();
        
        plugin.getLogger().info("[IP-AUTH] 🔍 Verificando IP para " + playerName + " (" + playerIp + ")");
        
        // Verificar se o jogador está na whitelist
        if (isPlayerWhitelisted(playerName)) {
            plugin.getLogger().info("[IP-AUTH] ✅ Jogador na whitelist - bypass de IP");
            event.allow();
            return;
        }
        
        try {
            final UUID playerUuid = UUIDUtils.offlineUUIDFromName(playerName);
            
            // Verificar se o player existe no banco
            boolean playerExistsInDB = checkPlayerExistsInDatabase(playerUuid, playerName);
            
            if (!playerExistsInDB) {
                PlayerProfile profile = PrimeLeagueAPI.getDataManager().loadOfflinePlayerProfile(playerName);
                
                if (profile == null) {
                    event.disallow(Result.KICK_OTHER, 
                        "§c§l❌ Registro Necessário\n\n" +
                        "§fVocê precisa se registrar no Discord primeiro!\n\n" +
                        "§e📱 Discord: §fdiscord.gg/primeleague\n" +
                        "§e💬 Comando: §f/registrar " + playerName + "\n\n" +
                        "§a💡 Após o registro, use o código de verificação no servidor!");
                    return;
                }
            }
            
            // VERIFICAÇÃO DE PENDING_RELINK (FASE 2)
            if (isPlayerPendingRelink(playerName)) {
                plugin.getLogger().info("[PENDING-RELINK] ⏳ Jogador em processo de recuperação: " + playerName + " - permitindo login");
                // Permitir login - bypass de IP para jogadores em recuperação
                event.allow();
                return;
            }
            
            // VERIFICAÇÃO DE IP (CORREÇÃO ARQUITETURAL)
            if (!isIpAuthorized(playerName, playerIp)) {
                plugin.getLogger().info("[IP-AUTH] ❌ IP não autorizado detectado: " + playerName + " (" + playerIp + ")");
                
                // Kick imediato com mensagem sobre DM
                event.disallow(Result.KICK_OTHER,
                    "§c§l🔐 IP Não Autorizado\n\n" +
                    "§fDetectamos uma tentativa de conexão de um IP não autorizado.\n\n" +
                    "§e📱 Verifique sua DM no Discord para autorizar este IP.\n" +
                    "§e💬 Discord: §fdiscord.gg/primeleague\n\n" +
                    "§a💡 Após autorizar, tente conectar novamente!"
                );
                
                // Notificar Bot Discord de forma assíncrona (já estamos em thread assíncrona)
                notifyBotAboutUnauthorizedIp(playerName, playerIp);
                
                return;
            }
            
            plugin.getLogger().info("[IP-AUTH] ✅ IP autorizado para " + playerName + " (" + playerIp + ")");
            event.allow();
            
        } catch (Exception e) {
            plugin.getLogger().severe("[IP-AUTH] ❌ Erro na verificação de IP para " + playerName + ": " + e.getMessage());
            event.disallow(Result.KICK_OTHER, "§c§l❌ Erro interno do servidor");
        }
    }

    /**
     * Processa a entrada do jogador no servidor.
     * SOLUÇÃO SIMPLIFICADA: Remove duplicação de lógica.
     * O AsyncPlayerPreLoginEvent já faz toda a verificação necessária.
     * Este método apenas coloca em limbo se necessário.
     * DEBUG: Logs detalhados para troubleshooting.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final UUID playerUuid = player.getUniqueId();
        
        plugin.getLogger().info("[JOIN-DEBUG] 🎮 Jogador entrou: " + playerName + " (UUID: " + playerUuid + ")");
        
        // Verificar se o jogador está na whitelist (BYPASS TOTAL)
        if (isPlayerWhitelisted(playerName)) {
            plugin.getLogger().info("[JOIN-DEBUG] ✅ Jogador na whitelist - bypass total");
            return; // Pula toda a verificação
        }
        
        try {
            plugin.getLogger().info("[JOIN-DEBUG] 🔍 Verificando se precisa de verificação para: " + playerName);
            
            // CORREÇÃO CRÍTICA: Usar UUID canônico do Core (mesmo do AsyncPlayerPreLoginEvent)
            final UUID canonicalUuid = UUIDUtils.offlineUUIDFromName(playerName);
            plugin.getLogger().info("[JOIN-DEBUG] 🔄 UUID do jogador: " + playerUuid);
            plugin.getLogger().info("[JOIN-DEBUG] 🔄 UUID canônico: " + canonicalUuid);
            
            // CORREÇÃO ARQUITETURAL: Verificar assinatura ativa PRIMEIRO (evita loop infinito)
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().loadOfflinePlayerProfile(playerName);
            if (profile != null && profile.hasActiveAccess()) {
                plugin.getLogger().info("[JOIN-DEBUG] ✅ Jogador com assinatura ativa: " + playerName + " - bypass de limbo");
                plugin.getLogger().info("[JOIN-DEBUG] ✅ " + playerName + " entrou no servidor (assinatura ativa)");
                
                // Verificar se está em PENDING_RELINK e enviar mensagens persistentes
                if (isPlayerPendingRelink(playerName)) {
                    plugin.getLogger().info("[PENDING-RELINK] Iniciando mensagens persistentes para: " + playerName);
                    startPendingRelinkReminders(player);
                }
                return; // Pula toda a lógica de limbo para jogadores com assinatura ativa
            }
            
            // Se não tem assinatura ativa, verificar se o jogador tem registro pendente (não verificado)
            boolean hasPendingVerification = hasPendingVerification(canonicalUuid);
            
            plugin.getLogger().info("[JOIN-DEBUG] 📊 Resultado da verificação: " + (hasPendingVerification ? "PENDENTE" : "VERIFICADO/ATIVO"));
            
            if (hasPendingVerification) {
                // Jogador tem registro pendente - colocar em limbo para verificação
                plugin.getLogger().info("[JOIN-DEBUG] ⏳ Jogador com verificação pendente: " + playerName + " - colocando em limbo");
                
                // Delay para garantir que o jogador carregou completamente
                plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            plugin.getLogger().info("[JOIN-DEBUG] 🚀 Executando putPlayerInLimbo para: " + playerName);
                            plugin.getLimboManager().putPlayerInLimbo(player);
                            plugin.getLogger().info("[JOIN-DEBUG] ✅ Jogador colocado em limbo: " + playerName);
                        } else {
                            plugin.getLogger().warning("[JOIN-DEBUG] ⚠️ Jogador offline durante colocação em limbo: " + playerName);
                        }
                    }
                }, 20L); // 1 segundo de delay
            } else {
                // Jogador verificado e ativo - log de entrada
                plugin.getLogger().info("[JOIN-DEBUG] ✅ " + playerName + " entrou no servidor (verificado e ativo)");
            }
            
            // Verificar se está em PENDING_RELINK e enviar mensagens persistentes
            if (isPlayerPendingRelink(playerName)) {
                plugin.getLogger().info("[PENDING-RELINK] Iniciando mensagens persistentes para: " + playerName);
                startPendingRelinkReminders(player);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[JOIN-DEBUG] ❌ Erro ao processar entrada de " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se um player existe diretamente no banco de dados.
     * DEBUG: Para diagnosticar problemas de UUID incompatibilidade.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @return true se o player existe no banco
     */
    private boolean checkPlayerExistsInDatabase(UUID playerUuid, String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DB-DEBUG] 🔍 Verificando se player existe no banco: " + playerName);
        plugin.getLogger().info("[DB-DEBUG] 📝 UUID: " + playerUuid.toString());
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[DB-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[DB-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se o player existe por UUID
            String sqlByUuid = "SELECT COUNT(*) FROM player_data WHERE uuid = ?";
            plugin.getLogger().info("[DB-DEBUG] 📝 Executando query por UUID: " + sqlByUuid);
            
            ps = conn.prepareStatement(sqlByUuid);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int countByUuid = rs.getInt(1);
                plugin.getLogger().info("[DB-DEBUG] 📊 Resultado por UUID: " + countByUuid + " registros");
                
                if (countByUuid > 0) {
                    plugin.getLogger().info("[DB-DEBUG] ✅ Player encontrado por UUID!");
                    return true;
                }
            }
            
            // Se não encontrou por UUID, tentar por nome
            plugin.getLogger().info("[DB-DEBUG] 🔍 Player não encontrado por UUID, tentando por nome...");
            
            rs.close();
            ps.close();
            
            String sqlByName = "SELECT COUNT(*) FROM player_data WHERE name = ?";
            plugin.getLogger().info("[DB-DEBUG] 📝 Executando query por nome: " + sqlByName);
            
            ps = conn.prepareStatement(sqlByName);
            ps.setString(1, playerName);
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int countByName = rs.getInt(1);
                plugin.getLogger().info("[DB-DEBUG] 📊 Resultado por nome: " + countByName + " registros");
                
                if (countByName > 0) {
                    plugin.getLogger().info("[DB-DEBUG] ✅ Player encontrado por nome!");
                    
                    // Se encontrou por nome, verificar qual UUID está no banco
                    rs.close();
                    ps.close();
                    
                    String sqlGetUuid = "SELECT uuid FROM player_data WHERE name = ?";
                    ps = conn.prepareStatement(sqlGetUuid);
                    ps.setString(1, playerName);
                    
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        String storedUuid = rs.getString("uuid");
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUID no banco: " + storedUuid);
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUID gerado: " + playerUuid.toString());
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUIDs são iguais: " + storedUuid.equals(playerUuid.toString()));
                    }
                    
                    return true;
                }
            }
            
            plugin.getLogger().warning("[DB-DEBUG] ❌ Player não encontrado nem por UUID nem por nome");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DB-DEBUG] ❌ Erro ao verificar player no banco: " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[DB-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[DB-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se um jogador está vinculado ao Discord (registrado via Discord).
     * REFATORADO: Usa DataManager normalizado.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está vinculado ao Discord (mesmo que não verificado)
     */
    private boolean isPlayerLinkedToDiscord(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DISCORD-DEBUG] 🔍 Verificando vínculo Discord para UUID: " + playerUuid);
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[DISCORD-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[DISCORD-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se está vinculado (mesmo que não verificado)
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ?";
            plugin.getLogger().info("[DISCORD-DEBUG] 📝 Executando query: " + sql);
            plugin.getLogger().info("[DISCORD-DEBUG] 📝 Parâmetro UUID: " + playerUuid.toString());
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[DISCORD-DEBUG] 📊 Resultado da query: " + count + " vínculos encontrados");
                
                boolean isLinked = count > 0;
                plugin.getLogger().info("[DISCORD-DEBUG] " + (isLinked ? "✅" : "❌") + " Jogador " + (isLinked ? "VINCULADO" : "NÃO VINCULADO") + " ao Discord");
                
                return isLinked;
            }
            
            plugin.getLogger().warning("[DISCORD-DEBUG] ❌ Nenhum resultado retornado pela query");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DISCORD-DEBUG] ❌ Erro ao verificar vínculo para " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[DISCORD-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[DISCORD-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se um jogador está verificado no Discord.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está verificado
     */
    private boolean isPlayerVerified(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[VERIFY-DEBUG] 🔍 Verificando se está verificado para UUID: " + playerUuid);
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[VERIFY-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[VERIFY-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se está verificado
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE";
            plugin.getLogger().info("[VERIFY-DEBUG] 📝 Executando query: " + sql);
            plugin.getLogger().info("[VERIFY-DEBUG] 📝 Parâmetro UUID: " + playerUuid.toString());
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[VERIFY-DEBUG] 📊 Resultado da query: " + count + " vínculos verificados encontrados");
                
                boolean isVerified = count > 0;
                plugin.getLogger().info("[VERIFY-DEBUG] " + (isVerified ? "✅" : "❌") + " Jogador " + (isVerified ? "VERIFICADO" : "NÃO VERIFICADO"));
                
                return isVerified;
            }
            
            plugin.getLogger().warning("[VERIFY-DEBUG] ❌ Nenhum resultado retornado pela query");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[VERIFY-DEBUG] ❌ Erro ao verificar status de verificação para " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[VERIFY-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[VERIFY-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica o status de assinatura de uma conta usando o perfil carregado.
     * REFATORADO: Usa PlayerProfile do DataManager normalizado.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param profile PlayerProfile carregado via DataManager
     * @return AccountStatus com informações da assinatura
     */
    private AccountStatus checkAccountSubscriptionFromProfile(PlayerProfile profile) {
        plugin.getLogger().info("[AUTH-DEBUG] 🔍 Verificando assinatura para perfil: " + profile.getUuid());
        
        try {
            // 1. Verificar se está vinculado ao Discord
            plugin.getLogger().info("[AUTH-DEBUG] 📋 Verificando vínculo Discord...");
            boolean isLinked = isPlayerLinkedToDiscord(profile.getUuid());
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Vínculo Discord: " + (isLinked ? "SIM" : "NÃO"));
            
            if (!isLinked) {
                // Não vinculado - KICK IMEDIATO
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: NOT_REGISTERED (não registrado no Discord)");
                return new AccountStatus(SubscriptionStatus.NOT_REGISTERED, 0);
            }
            
            // 2. Verificar se está verificado
            plugin.getLogger().info("[AUTH-DEBUG] 📋 Verificando se está verificado...");
            boolean isVerified = isPlayerVerified(profile.getUuid());
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Verificado: " + (isVerified ? "SIM" : "NÃO"));
            
            if (!isVerified) {
                // Registrado mas não verificado - permitir entrada para verificação
                plugin.getLogger().info("[AUTH-DEBUG] ⏳ Status: PENDING_VERIFICATION (registrado mas não verificado)");
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            // 3. Obter Discord ID do jogador
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 Buscando Discord ID...");
            String discordId = getDiscordIdByPlayerUuid(profile.getUuid());
            if (discordId == null) {
                plugin.getLogger().warning("[AUTH-DEBUG] ❌ Discord ID não encontrado para UUID: " + profile.getUuid());
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // 4. Verificar assinatura compartilhada por Discord ID
            plugin.getLogger().info("[AUTH-DEBUG] 📅 Verificando assinatura compartilhada para Discord ID: " + discordId);
            java.sql.Timestamp sharedSubscriptionExpiry = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getSharedSubscriptionByDiscordId(discordId);
            
            if (sharedSubscriptionExpiry == null) {
                // Sem assinatura compartilhada
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: NEVER_SUBSCRIBED (sem assinatura compartilhada)");
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // Calcular dias restantes
            long currentTime = System.currentTimeMillis();
            long expiresTime = sharedSubscriptionExpiry.getTime();
            
            plugin.getLogger().info("[AUTH-DEBUG] ⏰ Tempo atual: " + new java.util.Date(currentTime));
            plugin.getLogger().info("[AUTH-DEBUG] ⏰ Expira em: " + new java.util.Date(expiresTime));
            
            if (expiresTime > currentTime) {
                // Assinatura ativa
                int daysRemaining = (int) ((expiresTime - currentTime) / (1000 * 60 * 60 * 24));
                plugin.getLogger().info("[AUTH-DEBUG] ✅ Status: ACTIVE (" + daysRemaining + " dias restantes)");
                return new AccountStatus(SubscriptionStatus.ACTIVE, daysRemaining);
            } else {
                // Assinatura expirada
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: EXPIRED (expiresTime <= currentTime)");
                return new AccountStatus(SubscriptionStatus.EXPIRED, 0);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH-DEBUG] ❌ Erro ao verificar assinatura para " + profile.getUuid() + ": " + e.getMessage());
            e.printStackTrace();
            return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
        }
    }

    /**
     * Verifica o status de assinatura de uma conta específica.
     * MÉTODO LEGADO: Mantido para compatibilidade, mas prefira checkAccountSubscriptionFromProfile.
     * Fonte da verdade: discord_users.subscription_expires_at (assinatura compartilhada)
     * 
     * @param playerUuid UUID do jogador
     * @return AccountStatus com informações da assinatura
     * @deprecated Use checkAccountSubscriptionFromProfile(PlayerProfile) instead
     */
    @Deprecated
    private AccountStatus checkAccountSubscription(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // 1. Verificar se está vinculado ao Discord
            String checkLinkSql = "SELECT dl.verified FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? LIMIT 1";
            ps = conn.prepareStatement(checkLinkSql);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Não vinculado
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            boolean isVerified = rs.getBoolean("verified");
            rs.close();
            ps.close();
            
            if (!isVerified) {
                // Vinculado mas não verificado
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            // 2. Verificar assinatura compartilhada via Discord ID
            // Primeiro, obter o Discord ID do jogador
            String getDiscordIdSql = "SELECT dl.discord_id FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE LIMIT 1";
            ps = conn.prepareStatement(getDiscordIdSql);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Jogador não vinculado ao Discord
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            String discordId = rs.getString("discord_id");
            rs.close();
            ps.close();
            
            // Agora verificar a assinatura compartilhada
                         String checkSubSql = "SELECT subscription_expires_at FROM discord_users WHERE discord_id = ?";
            ps = conn.prepareStatement(checkSubSql);
            ps.setString(1, discordId);
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Discord ID não tem assinatura
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            Timestamp expiresAt = rs.getTimestamp("subscription_expires_at");
            
            if (expiresAt == null) {
                // Nunca teve assinatura
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // Calcular dias restantes
            long currentTime = System.currentTimeMillis();
            long expiresTime = expiresAt.getTime();
            
            if (expiresTime > currentTime) {
                // Assinatura ativa
                int daysRemaining = (int) ((expiresTime - currentTime) / (1000 * 60 * 60 * 24));
                return new AccountStatus(SubscriptionStatus.ACTIVE, daysRemaining);
            } else {
                // Assinatura expirada
                return new AccountStatus(SubscriptionStatus.EXPIRED, 0);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Erro ao verificar assinatura para " + playerUuid + ": " + e.getMessage());
            return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Silenciar erros de cleanup
            }
        }
    }

    /**
     * Enum para status de assinatura.
     */
         private enum SubscriptionStatus {
         ACTIVE,
         EXPIRED,
         NEVER_SUBSCRIBED,
         PENDING_VERIFICATION,
         NOT_REGISTERED
     }

    /**
     * Classe para armazenar informações de status da conta.
     */
    private static class AccountStatus {
        private final SubscriptionStatus status;
        private final int daysRemaining;

        public AccountStatus(SubscriptionStatus status, int daysRemaining) {
            this.status = status;
            this.daysRemaining = daysRemaining;
        }

        public SubscriptionStatus getStatus() {
            return status;
        }

        public int getDaysRemaining() {
            return daysRemaining;
        }
    }

    /**
     * Verifica se um jogador está na whitelist usando a nova API centralizada.
     * @param playerName Nome do jogador
     * @return true se o jogador estiver na whitelist, false caso contrário
     */
    private boolean isPlayerWhitelisted(String playerName) {
        try {
            // Obter UUID canônico do jogador
            UUID playerUuid = UUIDUtils.offlineUUIDFromName(playerName);
            
            // Usar a nova API centralizada do Core com UUID como fonte única da verdade
            return br.com.primeleague.core.api.PrimeLeagueAPI.isWhitelisted(playerUuid);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar whitelist para " + playerName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca o nome canônico de um jogador no banco de dados.
     * Executa busca case-insensitive para validar capitalização.
     * REFATORADO: Usa DataManager normalizado.
     * 
     * @param playerName Nome digitado pelo jogador
     * @return Nome canônico se encontrado, null se não existir
     */
    private String getCanonicalPlayerName(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) return null;
            
            // Busca case-insensitive usando COLLATE
            String sql = "SELECT name FROM player_data WHERE name COLLATE utf8mb4_unicode_ci = ? LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name"); // Retorna o nome canônico do banco
            }
            
            return null; // Nome não encontrado
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao buscar nome canônico para " + playerName + ": " + e.getMessage());
            return null;
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Silenciar erros de cleanup
            }
        }
    }

    /**
     * FUNÇÃO DE TESTE: Verifica diretamente o banco de dados para debugging.
     * Executa queries detalhadas para identificar problemas.
     * 
     * @param playerUuid UUID do jogador para testar
     */
    private void debugDatabaseState(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DEBUG-DB] 🔍 === INICIANDO DEBUG DO BANCO DE DADOS ===");
        plugin.getLogger().info("[DEBUG-DB] 🎯 UUID alvo: " + playerUuid);
        
        try {
            conn = PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("[DEBUG-DB] ❌ Conexão nula");
                return;
            }
            
            // TESTE 1: Verificar se o jogador existe em player_data
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 1: Verificando player_data...");
            String sql1 = "SELECT player_id, name, uuid FROM player_data WHERE uuid = ?";
            ps = conn.prepareStatement(sql1);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int playerId = rs.getInt("player_id");
                String name = rs.getString("name");
                String uuid = rs.getString("uuid");
                
                plugin.getLogger().info("[DEBUG-DB] ✅ player_data encontrado:");
                plugin.getLogger().info("[DEBUG-DB]    - player_id: " + playerId);
                plugin.getLogger().info("[DEBUG-DB]    - name: " + name);
                plugin.getLogger().info("[DEBUG-DB]    - uuid: " + uuid);
                plugin.getLogger().info("[DEBUG-DB]    - subscription_expires_at: (removido do player_data)");
            } else {
                plugin.getLogger().warning("[DEBUG-DB] ❌ player_data NÃO encontrado para UUID: " + playerUuid);
            }
            
            rs.close();
            ps.close();
            
            // TESTE 2: Verificar discord_links
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 2: Verificando discord_links...");
            String sql2 = "SELECT dl.*, pd.name FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ?";
            ps = conn.prepareStatement(sql2);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            boolean foundLink = false;
            while (rs.next()) {
                foundLink = true;
                int linkId = rs.getInt("link_id");
                int playerId = rs.getInt("player_id");
                String discordId = rs.getString("discord_id");
                boolean verified = rs.getBoolean("verified");
                String playerName = rs.getString("name");
                
                plugin.getLogger().info("[DEBUG-DB] ✅ discord_links encontrado:");
                plugin.getLogger().info("[DEBUG-DB]    - link_id: " + linkId);
                plugin.getLogger().info("[DEBUG-DB]    - player_id: " + playerId);
                plugin.getLogger().info("[DEBUG-DB]    - discord_id: " + discordId);
                plugin.getLogger().info("[DEBUG-DB]    - verified: " + verified);
                plugin.getLogger().info("[DEBUG-DB]    - player_name: " + playerName);
            }
            
            if (!foundLink) {
                plugin.getLogger().warning("[DEBUG-DB] ❌ discord_links NÃO encontrado para UUID: " + playerUuid);
            }
            
            rs.close();
            ps.close();
            
            // TESTE 3: Query exata que está falhando
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 3: Query exata do isPlayerLinkedToDiscord...");
            String sql3 = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE";
            ps = conn.prepareStatement(sql3);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[DEBUG-DB] 📊 Resultado da query: " + count + " vínculos verificados");
            }
            
            rs.close();
            ps.close();
            
            plugin.getLogger().info("[DEBUG-DB] ✅ === DEBUG DO BANCO CONCLUÍDO ===");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG-DB] ❌ Erro durante debug: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[DEBUG-DB] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }
    
         /**
      * Verifica se um jogador tem verificação pendente (registrado mas não verificado).
      * 
      * @param playerUuid UUID do jogador
      * @return true se tem verificação pendente, false caso contrário
      */
     private boolean hasPendingVerification(UUID playerUuid) {
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         
         try {
             // Obter conexão via API do Core
             conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
             if (conn == null) {
                 return false;
             }
             
                         // Query para verificar se tem registro mas não está verificado
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = FALSE";
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
             
             rs = ps.executeQuery();
             if (rs.next()) {
                 int count = rs.getInt(1);
                 return count > 0; // Tem registro pendente se count > 0
             }
             
             return false;
             
         } catch (Exception e) {
             plugin.getLogger().severe("[AUTH] Erro ao verificar verificação pendente para " + playerUuid + ": " + e.getMessage());
             return false;
             
         } finally {
             // Cleanup de recursos
             try {
                 if (rs != null) rs.close();
                 if (ps != null) ps.close();
                 if (conn != null) conn.close();
             } catch (Exception e) {
                 plugin.getLogger().warning("[AUTH] Erro ao fechar recursos: " + e.getMessage());
             }
         }
     }
     
     /**
      * Obtém o Discord ID de um jogador pelo UUID.
      * 
      * @param playerUuid UUID do jogador
      * @return Discord ID ou null se não encontrado
      */
     private String getDiscordIdByPlayerUuid(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                return null;
            }
            
            // Query para buscar Discord ID
            String sql = "SELECT dl.discord_id FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
            
            return null;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Erro ao buscar Discord ID para " + playerUuid + ": " + e.getMessage());
            return null;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[AUTH] Erro ao fechar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Gera UUID manualmente para comparação com o método Java.
     */
    private UUID generateUUIDManually(String playerName) {
        try {
            String source = "OfflinePlayer:" + playerName;
            plugin.getLogger().info("🔍 [DEBUG-UUID] Source string: " + source);
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(source.getBytes(StandardCharsets.UTF_8));
            
            plugin.getLogger().info("🔍 [DEBUG-UUID] MD5 hash (hex): " + bytesToHex(hash));
            
            // Converter para UUID usando o mesmo algoritmo do Java
            long msb = 0;
            long lsb = 0;
            
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            
            // Aplicar versão e variante
            msb &= ~(0xf << 12);
            msb |= 3 << 12; // versão 3
            lsb &= ~(0x3L << 62);
            lsb |= 2L << 62; // variante
            
            UUID uuid = new UUID(msb, lsb);
            plugin.getLogger().info("🔍 [DEBUG-UUID] UUID manual final: " + uuid.toString());
            
            return uuid;
            
        } catch (NoSuchAlgorithmException e) {
            plugin.getLogger().severe("🔍 [DEBUG-UUID] ❌ Erro ao gerar UUID manual: " + e.getMessage());
            return UUID.randomUUID();
        }
    }

    /**
     * Debug detalhado do processo de geração de UUID.
     */
    private void debugUUIDGeneration(String playerName) {
        plugin.getLogger().info("🔍 [DEBUG-UUID] === DEBUG DETALHADO DE GERAÇÃO ===");
        
        try {
            String source = "OfflinePlayer:" + playerName;
            plugin.getLogger().info("🔍 [DEBUG-UUID] 1. Source string: '" + source + "'");
            plugin.getLogger().info("🔍 [DEBUG-UUID] 2. Bytes da source: " + bytesToHex(source.getBytes(StandardCharsets.UTF_8)));
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(source.getBytes(StandardCharsets.UTF_8));
            
            plugin.getLogger().info("🔍 [DEBUG-UUID] 3. MD5 hash (bytes): " + bytesToHex(hash));
            plugin.getLogger().info("🔍 [DEBUG-UUID] 4. MD5 hash (hex): " + bytesToHex(hash));
            
            // Simular o processo do Java UUID.nameUUIDFromBytes
            UUID javaUuid = UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
            plugin.getLogger().info("🔍 [DEBUG-UUID] 5. Java UUID.nameUUIDFromBytes: " + javaUuid.toString());
            
            // Comparar com nosso método manual
            UUID manualUuid = generateUUIDManually(playerName);
            plugin.getLogger().info("🔍 [DEBUG-UUID] 6. Nosso método manual: " + manualUuid.toString());
            
            boolean match = javaUuid.equals(manualUuid);
            plugin.getLogger().info("🔍 [DEBUG-UUID] 7. São iguais: " + match);
            
        } catch (Exception e) {
            plugin.getLogger().severe("🔍 [DEBUG-UUID] ❌ Erro no debug: " + e.getMessage());
            e.printStackTrace();
        }
        
        plugin.getLogger().info("🔍 [DEBUG-UUID] === FIM DO DEBUG ===");
    }

    /**
     * Verifica UUIDs conhecidos para comparação.
     */
    private void checkKnownUUIDs(String playerName, UUID generatedUuid) {
        plugin.getLogger().info("🔍 [DEBUG-UUID] === VERIFICAÇÃO DE UUIDs CONHECIDOS ===");
        
        if ("vini".equals(playerName)) {
            String expectedVini = "9b261df7-633c-3e05-9b0e-811f72be39ab";
            boolean viniMatch = generatedUuid.toString().equals(expectedVini);
            plugin.getLogger().info("🔍 [DEBUG-UUID] vini - Esperado: " + expectedVini);
            plugin.getLogger().info("🔍 [DEBUG-UUID] vini - Gerado: " + generatedUuid.toString());
            plugin.getLogger().info("🔍 [DEBUG-UUID] vini - Match: " + viniMatch);
        }
        
        if ("mlkpiranha0".equals(playerName)) {
            String expectedMlk = "d00e7769-18de-3002-b821-cf11996f8963";
            boolean mlkMatch = generatedUuid.toString().equals(expectedMlk);
            plugin.getLogger().info("🔍 [DEBUG-UUID] mlkpiranha0 - Esperado: " + expectedMlk);
            plugin.getLogger().info("🔍 [DEBUG-UUID] mlkpiranha0 - Gerado: " + generatedUuid.toString());
            plugin.getLogger().info("🔍 [DEBUG-UUID] mlkpiranha0 - Match: " + mlkMatch);
        }
        
        plugin.getLogger().info("🔍 [DEBUG-UUID] === FIM DA VERIFICAÇÃO ===");
    }

    /**
     * Converte bytes para string hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verifica se um player existe no banco com DEBUG DETALHADO.
     */
    private boolean checkPlayerExistsInDatabaseDetailed(String playerName) {
        plugin.getLogger().info("🔍 [DEBUG-DB] === VERIFICAÇÃO DETALHADA NO BANCO ===");
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("🔍 [DEBUG-DB] ❌ Conexão nula");
                return false;
            }
            
            plugin.getLogger().info("🔍 [DEBUG-DB] ✅ Conexão obtida");
            
            // 1. Verificar por nome
            String sqlByName = "SELECT player_id, uuid, name FROM player_data WHERE name = ?";
            plugin.getLogger().info("🔍 [DEBUG-DB] Query por nome: " + sqlByName);
            
            ps = conn.prepareStatement(sqlByName);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int playerId = rs.getInt("player_id");
                String storedUuid = rs.getString("uuid");
                String storedName = rs.getString("name");
                
                plugin.getLogger().info("🔍 [DEBUG-DB] ✅ Player encontrado por nome:");
                plugin.getLogger().info("🔍 [DEBUG-DB]   - player_id: " + playerId);
                plugin.getLogger().info("🔍 [DEBUG-DB]   - uuid: " + storedUuid);
                plugin.getLogger().info("🔍 [DEBUG-DB]   - name: " + storedName);
                
                // 2. Verificar se existe discord_link
                rs.close();
                ps.close();
                
                String sqlLink = "SELECT dl.*, du.subscription_expires_at FROM discord_links dl " +
                               "LEFT JOIN discord_users du ON dl.discord_id = du.discord_id " +
                               "WHERE dl.player_id = ?";
                
                ps = conn.prepareStatement(sqlLink);
                ps.setInt(1, playerId);
                rs = ps.executeQuery();
                
                if (rs.next()) {
                    plugin.getLogger().info("🔍 [DEBUG-DB] ✅ Discord link encontrado:");
                    plugin.getLogger().info("🔍 [DEBUG-DB]   - discord_id: " + rs.getString("discord_id"));
                    plugin.getLogger().info("🔍 [DEBUG-DB]   - verified: " + rs.getBoolean("verified"));
                    plugin.getLogger().info("🔍 [DEBUG-DB]   - subscription_expires_at: " + rs.getTimestamp("subscription_expires_at"));
                } else {
                    plugin.getLogger().info("🔍 [DEBUG-DB] ❌ Nenhum discord_link encontrado");
                }
                
                return true;
            } else {
                plugin.getLogger().info("🔍 [DEBUG-DB] ❌ Player não encontrado por nome");
                
                // 3. Listar todos os players para debug
                rs.close();
                ps.close();
                
                String sqlAll = "SELECT player_id, uuid, name FROM player_data ORDER BY player_id";
                ps = conn.prepareStatement(sqlAll);
                rs = ps.executeQuery();
                
                plugin.getLogger().info("🔍 [DEBUG-DB] === TODOS OS PLAYERS NO BANCO ===");
                while (rs.next()) {
                    plugin.getLogger().info("🔍 [DEBUG-DB] ID: " + rs.getInt("player_id") + 
                                          " | UUID: " + rs.getString("uuid") + 
                                          " | Name: " + rs.getString("name"));
                }
                plugin.getLogger().info("🔍 [DEBUG-DB] === FIM DA LISTA ===");
                
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("🔍 [DEBUG-DB] ❌ Erro na verificação: " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("🔍 [DEBUG-DB] Erro ao fechar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Cria um player no banco para debug.
     */
    private void createPlayerForDebug(String playerName) {
        plugin.getLogger().info("🔍 [DEBUG-DB] === CRIANDO PLAYER PARA DEBUG ===");
        
        Connection conn = null;
        PreparedStatement ps = null;
        
        try {
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("🔍 [DEBUG-DB] ❌ Conexão nula para criação");
                return;
            }
            
            UUID playerUuid = UUIDUtils.offlineUUIDFromName(playerName);
            
            String sql = "INSERT INTO player_data (uuid, name, elo, money, total_playtime, total_logins, status, last_seen) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setInt(3, 1000);
            ps.setBigDecimal(4, new java.math.BigDecimal("0.00"));
            ps.setInt(5, 0);
            ps.setInt(6, 0);
            ps.setString(7, "ACTIVE");
            
            int result = ps.executeUpdate();
            plugin.getLogger().info("🔍 [DEBUG-DB] Player criado com sucesso: " + result + " linhas afetadas");
            plugin.getLogger().info("🔍 [DEBUG-DB] UUID usado na criação: " + playerUuid.toString());
            
        } catch (Exception e) {
            plugin.getLogger().severe("🔍 [DEBUG-DB] ❌ Erro ao criar player: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("🔍 [DEBUG-DB] Erro ao fechar recursos: " + e.getMessage());
            }
        }
    }
    
    /**
     * Verifica se um IP está autorizado para um player
     * CORREÇÃO: Verifica primeiro o cache em memória, depois o banco
     */
    private boolean isIpAuthorized(String playerName, String ipAddress) {
        try {
            // Primeiro verificar cache em memória (mais rápido)
            if (plugin.getIpAuthCache() != null && plugin.getIpAuthCache().isIpAuthorized(playerName, ipAddress)) {
                plugin.getLogger().info("[IP-AUTH] ✅ IP autorizado via cache: " + playerName + " (" + ipAddress + ")");
                return true;
            }
            
            // Se não está no cache, verificar banco de dados
            boolean authorized = br.com.primeleague.core.PrimeLeagueCore.getInstance()
                .getDataManager()
                .isIpAuthorized(playerName, ipAddress);
            
            // Se autorizado no banco, adicionar ao cache
            if (authorized && plugin.getIpAuthCache() != null) {
                plugin.getIpAuthCache().addAuthorizedIp(playerName, ipAddress);
                plugin.getLogger().info("[IP-AUTH] ✅ IP autorizado via banco e adicionado ao cache: " + playerName + " (" + ipAddress + ")");
            }
            
            return authorized;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[IP-AUTH] Erro ao verificar autorização de IP: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Notifica o Bot Discord sobre IP não autorizado de forma assíncrona
     */
    private void notifyBotAboutUnauthorizedIp(final String playerName, final String ipAddress) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar Discord ID do player
                    String discordId = getDiscordIdByPlayerName(playerName);
                    if (discordId == null) {
                        plugin.getLogger().warning("[IP-AUTH] Discord ID não encontrado para " + playerName);
                        return;
                    }
                    
                    // Preparar payload para o webhook
                    String payload = String.format(
                        "{\"playerName\":\"%s\",\"playerId\":%d,\"ipAddress\":\"%s\",\"discordId\":\"%s\",\"timestamp\":%d,\"serverName\":\"PrimeLeague\"}",
                        playerName,
                        getPlayerIdByName(playerName),
                        ipAddress,
                        discordId,
                        System.currentTimeMillis()
                    );
                    
                    // Enviar para webhook do Bot
                    sendWebhookNotification(payload);
                    
                    plugin.getLogger().info("[IP-AUTH] Notificação enviada para Bot Discord: " + playerName + " (" + ipAddress + ")");
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("[IP-AUTH] Erro ao notificar Bot Discord: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Busca Discord ID por nome do player
     */
    private String getDiscordIdByPlayerName(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            
            String sql = "SELECT dl.discord_id FROM discord_links dl " +
                        "JOIN player_data pd ON dl.player_id = pd.player_id " +
                        "WHERE pd.name = ? AND dl.verified = TRUE LIMIT 1";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("discord_id");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[IP-AUTH] Erro ao buscar Discord ID: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Ignorar erros de fechamento
            }
        }
        
        return null;
    }
    
    /**
     * Busca player ID por nome
     */
    private int getPlayerIdByName(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            
            String sql = "SELECT player_id FROM player_data WHERE name = ? LIMIT 1";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("player_id");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[IP-AUTH] Erro ao buscar player ID: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Ignorar erros de fechamento
            }
        }
        
        return 0;
    }
    
    /**
     * Envia notificação para webhook do Bot Discord
     */
    private void sendWebhookNotification(String payload) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:3000/webhooks/ip-auth-notification");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer primeleague_api_token_2024");
            connection.setDoOutput(true);
            
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = connection.getResponseCode();
            plugin.getLogger().info("[IP-AUTH] Webhook response code: " + responseCode);
            
        } catch (Exception e) {
            plugin.getLogger().severe("[IP-AUTH] Erro ao enviar webhook: " + e.getMessage());
        }
    }

    /**
     * Verifica se o jogador está em estado PENDING_RELINK (processo de recuperação).
     * 
     * @param playerName Nome do jogador
     * @return true se o jogador está em processo de recuperação
     */
    private boolean isPlayerPendingRelink(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            
            String sql = "SELECT dl.status FROM discord_links dl " +
                        "JOIN player_data pd ON dl.player_id = pd.player_id " +
                        "WHERE pd.name = ? AND dl.verified = TRUE LIMIT 1";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                boolean isPending = "PENDING_RELINK".equals(status);
                
                if (isPending) {
                    plugin.getLogger().info("[PENDING-RELINK] Jogador " + playerName + " está em estado PENDING_RELINK");
                }
                
                return isPending;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[PENDING-RELINK] Erro ao verificar estado PENDING_RELINK: " + e.getMessage());
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Ignorar erros de fechamento
            }
        }
        
        return false;
    }

    /**
     * Inicia o sistema de lembretes persistentes para jogadores em PENDING_RELINK.
     */
    private void startPendingRelinkReminders(final Player player) {
        final String playerName = player.getName();
        
        // Enviar mensagem inicial
        player.sendMessage("§e§l⚠️ ATENÇÃO: SUA CONTA ESTÁ EM RECUPERAÇÃO");
        player.sendMessage("§7Você precisa finalizar a vinculação no Discord para proteger sua conta.");
        player.sendMessage("§7Use o código de re-vinculação que apareceu no chat anteriormente.");
        player.sendMessage("§7Comando: §f/vincular <seu_nickname> <codigo>");
        
        // Agendar lembretes a cada 5 minutos
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    // Jogador desconectou - cancelar lembretes
                    return;
                }
                
                if (!isPlayerPendingRelink(playerName)) {
                    // Jogador já finalizou a recuperação - cancelar lembretes
                    player.sendMessage("§a§l✅ RECUPERAÇÃO FINALIZADA!");
                    player.sendMessage("§7Sua conta está protegida novamente.");
                    return;
                }
                
                // Enviar lembrete
                player.sendMessage("§e§l⏰ LEMBRETE: FINALIZE SUA RECUPERAÇÃO");
                player.sendMessage("§7Sua conta ainda está em processo de recuperação.");
                player.sendMessage("§7Use o código de re-vinculação no Discord para finalizar.");
                player.sendMessage("§7Comando: §f/vincular <seu_nickname> <codigo>");
                
            }
        }, 6000L, 6000L); // 5 minutos = 6000 ticks (20 ticks/segundo * 60 segundos * 5)
    }
}