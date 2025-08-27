package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gerenciador centralizado da Whitelist V2.
 * 
 * Características:
 * - Cache em memória para verificações instantâneas (baseado em UUID)
 * - Cache interno baseado em player_id para evitar race conditions
 * - Auditoria completa de todas as ações
 * - Sincronização automática com banco de dados
 * - API thread-safe para uso por outros módulos
 * 
 * @author PrimeLeague Team
 * @version 2.1.0 (Correção de race condition)
 */
public final class WhitelistManager {

    private final PrimeLeagueCore plugin;
    private final Logger logger;
    private final HikariDataSource dataSource;
    
    // Cache em memória para verificações instantâneas (usando UUID como chave)
    private final Set<UUID> whitelistedPlayers = ConcurrentHashMap.newKeySet();
    
    // Cache interno baseado em player_id para evitar race conditions na inicialização
    private final Set<Integer> whitelistedPlayerIds = ConcurrentHashMap.newKeySet();
    
    // SQLs refatorados para schema V2.0 (usando player_id)
    private static final String SELECT_ACTIVE_WHITELIST_SQL = 
        "SELECT wp.player_id FROM whitelist_players wp " +
        "WHERE wp.is_active = TRUE AND wp.removed_at IS NULL";
    
    private static final String ADD_TO_WHITELIST_SQL = 
        "INSERT INTO whitelist_players (player_id, player_name, added_by_player_id, added_by_name, reason) " +
        "VALUES (?, ?, ?, ?, ?)";
    
    private static final String REMOVE_FROM_WHITELIST_SQL = 
        "UPDATE whitelist_players SET is_active = FALSE, removed_by_player_id = ?, removed_by_name = ?, " +
        "removed_at = NOW(), removal_reason = ? WHERE player_id = ? AND is_active = TRUE";
    
    private static final String GET_WHITELISTED_PLAYERS_SQL = 
        "SELECT wp.player_id, wp.player_name, wp.added_by_name, wp.added_at, wp.reason " +
        "FROM whitelist_players wp " +
        "WHERE wp.is_active = TRUE AND wp.removed_at IS NULL " +
        "ORDER BY wp.added_at DESC";
    
    private static final String GET_WHITELIST_STATS_SQL = 
        "SELECT " +
        "COUNT(*) as total_entries, " +
        "COUNT(CASE WHEN is_active = TRUE AND removed_at IS NULL THEN 1 END) as active_players, " +
        "COUNT(CASE WHEN is_active = FALSE OR removed_at IS NOT NULL THEN 1 END) as removed_players, " +
        "MIN(added_at) as first_addition, " +
        "MAX(added_at) as last_addition, " +
        "COUNT(DISTINCT added_by_uuid) as unique_admins " +
        "FROM whitelist_players";

    public WhitelistManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataSource = plugin.getDataManager().getDataSource();
        loadWhitelistFromDatabase();
    }

    /**
     * Carrega a whitelist do banco de dados para o cache em memória.
     * CORREÇÃO ARQUITETÔNICA: Usa cache interno baseado em player_id para evitar race conditions.
     */
    private void loadWhitelistFromDatabase() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ACTIVE_WHITELIST_SQL);
             ResultSet rs = ps.executeQuery()) {
            
            whitelistedPlayers.clear();
            whitelistedPlayerIds.clear();
            int loadedCount = 0;
            
            logger.info("🔄 [WHITELIST-LOAD] Iniciando carregamento da whitelist do banco...");
            
            while (rs.next()) {
                int playerId = rs.getInt("player_id");
                
                // CORREÇÃO: Adicionar ao cache interno baseado em player_id
                whitelistedPlayerIds.add(playerId);
                
                // Tentar converter para UUID se IdentityManager estiver disponível
                try {
                    UUID playerUuid = plugin.getIdentityManager().getUuidByPlayerId(playerId);
                    if (playerUuid != null) {
                        whitelistedPlayers.add(playerUuid);
                        logger.info("✅ [WHITELIST-LOAD] Player ID " + playerId + " → UUID " + playerUuid + " carregado");
                    } else {
                        logger.info("ℹ️ [WHITELIST-LOAD] Player ID " + playerId + " adicionado ao cache interno (IdentityManager não disponível ainda)");
                        
                        // TENTATIVA DE RECUPERAÇÃO: Buscar UUID diretamente no banco
                        try (Connection conn2 = dataSource.getConnection();
                             PreparedStatement ps2 = conn2.prepareStatement("SELECT uuid, name FROM player_data WHERE player_id = ?")) {
                            ps2.setInt(1, playerId);
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    String uuidStr = rs2.getString("uuid");
                                    String playerName = rs2.getString("name");
                                    UUID recoveredUuid = UUID.fromString(uuidStr);
                                    whitelistedPlayers.add(recoveredUuid);
                                    logger.info("🔄 [WHITELIST-LOAD] UUID recuperado do banco: " + playerId + " → " + recoveredUuid + " (" + playerName + ")");
                                } else {
                                    logger.warning("⚠️ [WHITELIST-LOAD] Player ID " + playerId + " não encontrado na tabela player_data!");
                                }
                            }
                        } catch (SQLException e2) {
                            logger.warning("⚠️ [WHITELIST-LOAD] Erro ao recuperar UUID para player_id " + playerId + ": " + e2.getMessage());
                        }
                    }
                } catch (Exception e) {
                    // IdentityManager pode não estar pronto ainda - isso é normal na inicialização
                    logger.info("ℹ️ [WHITELIST-LOAD] Player ID " + playerId + " adicionado ao cache interno (IdentityManager não disponível)");
                }
                
                loadedCount++;
            }
            
            logger.info("📊 [WHITELIST-LOAD] Whitelist carregada do banco de dados: " + loadedCount + " jogadores");
            logger.info("📊 [WHITELIST-LOAD] Cache interno (player_id): " + whitelistedPlayerIds.size() + " entradas");
            logger.info("📊 [WHITELIST-LOAD] Cache UUID: " + whitelistedPlayers.size() + " entradas");
            
        } catch (SQLException e) {
            logger.severe("🚨 [WHITELIST-LOAD] Erro ao carregar whitelist do banco de dados: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se um jogador está na whitelist (verificação instantânea em cache).
     * CORREÇÃO: Usa cache interno baseado em player_id como fallback.
     * 
     * @param playerUuid UUID do jogador (fonte única da verdade)
     * @return true se o jogador estiver na whitelist, false caso contrário
     */
    public boolean isWhitelisted(UUID playerUuid) {
        if (playerUuid == null) {
            return false;
        }
        
        // Verificação primária: cache UUID
        if (whitelistedPlayers.contains(playerUuid)) {
            return true;
        }
        
        // Verificação secundária: cache player_id (para evitar race conditions)
        try {
            Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
            if (playerId != null && whitelistedPlayerIds.contains(playerId)) {
                // Adicionar ao cache UUID para futuras verificações
                whitelistedPlayers.add(playerUuid);
                return true;
            }
        } catch (Exception e) {
            // IdentityManager pode não estar disponível - usar apenas cache player_id
            logger.fine("ℹ️ [WHITELIST-CHECK] IdentityManager não disponível, usando cache player_id");
        }
        
        return false;
    }

    /**
     * Verifica se um player_id está na whitelist (método interno para evitar race conditions).
     * 
     * @param playerId ID do jogador
     * @return true se o jogador estiver na whitelist, false caso contrário
     */
    public boolean isWhitelistedByPlayerId(int playerId) {
        return whitelistedPlayerIds.contains(playerId);
    }

    /**
     * Adiciona um jogador à whitelist.
     * 
     * @param targetUuid UUID do jogador a ser adicionado (fonte única da verdade)
     * @param targetName Nome do jogador (para auditoria)
     * @param authorUuid UUID do admin que está adicionando
     * @param authorName Nome do admin
     * @param reason Motivo da adição
     * @return true se adicionado com sucesso, false caso contrário
     */
    public boolean addToWhitelist(UUID targetUuid, String targetName, UUID authorUuid, String authorName, String reason) {
        if (targetUuid == null || targetName == null || targetName.trim().isEmpty() || 
            authorUuid == null || authorName == null || reason == null) {
            logger.warning("🚨 [WHITELIST-ADD] Tentativa de adicionar à whitelist com parâmetros inválidos");
            return false;
        }

        logger.info("🔄 [WHITELIST-ADD] Iniciando adição à whitelist: " + targetName + " (UUID: " + targetUuid + ")");

        // VERIFICAÇÃO DUPLA: Verificar se já existe na whitelist (por nome e UUID)
        boolean alreadyWhitelisted = checkIfAlreadyWhitelisted(targetUuid, targetName);
        if (alreadyWhitelisted) {
            logger.warning("⚠️ [WHITELIST-ADD] Jogador " + targetName + " já está na whitelist!");
            return false;
        }

        // Converter UUIDs para player_id usando IdentityManager ou DataManager
        Integer targetPlayerId = plugin.getIdentityManager().getPlayerIdByUuid(targetUuid);
        Integer authorPlayerId = plugin.getIdentityManager().getPlayerIdByUuid(authorUuid);
        
        logger.info("🔍 [WHITELIST-ADD] Target Player ID via IdentityManager: " + targetPlayerId);
        logger.info("🔍 [WHITELIST-ADD] Author Player ID via IdentityManager: " + authorPlayerId);
        
        // Se não encontrou no IdentityManager (jogador offline), buscar no banco
        if (targetPlayerId == null) {
            targetPlayerId = plugin.getDataManager().getPlayerIdFromDatabase(targetUuid, targetName);
            logger.info("🔍 [WHITELIST-ADD] Target Player ID via DataManager: " + targetPlayerId);
            if (targetPlayerId == null) {
                logger.severe("🚨 [WHITELIST-ADD] Target UUID não encontrado no banco de dados: " + targetUuid);
                return false;
            }
        }
        
        if (authorPlayerId == null) {
            authorPlayerId = plugin.getDataManager().getPlayerIdFromDatabase(authorUuid, authorName);
            logger.info("🔍 [WHITELIST-ADD] Author Player ID via DataManager: " + authorPlayerId);
            if (authorPlayerId == null) {
                // EXCEÇÃO ESPECIAL: CONSOLE não tem UUID no banco, usar player_id = 0
                if ("CONSOLE".equals(authorName) && "00000000-0000-0000-0000-000000000000".equals(authorUuid.toString())) {
                    authorPlayerId = 0; // CONSOLE sempre tem player_id = 0
                    logger.info("🔍 [WHITELIST-ADD] Usando player_id = 0 para CONSOLE");
                } else {
                    logger.severe("🚨 [WHITELIST-ADD] Author UUID não encontrado no banco de dados: " + authorUuid);
                    return false;
                }
            }
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(ADD_TO_WHITELIST_SQL)) {
            
            logger.info("💾 [WHITELIST-ADD] Executando INSERT: player_id=" + targetPlayerId + ", name=" + targetName + ", author_id=" + authorPlayerId);
            
            // REFATORADO: Query usando player_id
            ps.setInt(1, targetPlayerId.intValue()); // player_id
            ps.setString(2, targetName); // player_name
            ps.setInt(3, authorPlayerId.intValue()); // added_by_player_id
            ps.setString(4, authorName); // added_by_name
            ps.setString(5, reason); // reason
            
            ps.executeUpdate();
            
            // Atualizar cache com UUID
            whitelistedPlayers.add(targetUuid);
            
            // Atualizar cache interno baseado em player_id
            whitelistedPlayerIds.add(targetPlayerId);
            
            logger.info("✅ [WHITELIST-ADD] Jogador " + targetName + " (UUID: " + targetUuid + ", player_id: " + targetPlayerId + ") adicionado à whitelist por " + authorName + ". Motivo: " + reason);
            
            return true;
            
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("UNIQUE constraint")) {
                logger.warning("⚠️ [WHITELIST-ADD] Tentativa de adicionar jogador já existente na whitelist: " + targetName);
                logger.warning("⚠️ [WHITELIST-ADD] Erro SQL: " + e.getMessage());
                
                // TENTATIVA DE RECUPERAÇÃO: Verificar qual player_id está causando conflito
                checkWhitelistConflict(targetUuid, targetName, targetPlayerId);
                
                return false;
            }
            
            logger.severe("🚨 [WHITELIST-ADD] Erro ao adicionar jogador à whitelist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se um jogador já está na whitelist (verificação dupla).
     */
    private boolean checkIfAlreadyWhitelisted(UUID targetUuid, String targetName) {
        // Verificação 1: Cache em memória
        if (whitelistedPlayers.contains(targetUuid)) {
            logger.info("🔍 [WHITELIST-CHECK] Jogador " + targetName + " encontrado no cache (UUID: " + targetUuid + ")");
            return true;
        }
        
        // Verificação 2: Banco de dados (por nome)
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT wp.player_id, wp.player_name, pd.uuid FROM whitelist_players wp " +
                 "LEFT JOIN player_data pd ON wp.player_id = pd.player_id " +
                 "WHERE wp.is_active = TRUE AND wp.removed_at IS NULL " +
                 "AND (wp.player_name = ? OR pd.uuid = ?)")) {
            
            ps.setString(1, targetName);
            ps.setString(2, targetUuid.toString());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingPlayerId = rs.getInt("player_id");
                    String existingPlayerName = rs.getString("player_name");
                    String existingUuid = rs.getString("uuid");
                    
                    logger.warning("⚠️ [WHITELIST-CHECK] Jogador já existe na whitelist:");
                    logger.warning("   Nome: " + existingPlayerName + " (solicitado: " + targetName + ")");
                    logger.warning("   Player ID: " + existingPlayerId);
                    logger.warning("   UUID: " + existingUuid + " (solicitado: " + targetUuid + ")");
                    
                    // Adicionar ao cache se não estiver
                    if (existingUuid != null) {
                        UUID existingUuidObj = UUID.fromString(existingUuid);
                        whitelistedPlayers.add(existingUuidObj);
                        logger.info("🔄 [WHITELIST-CHECK] UUID " + existingUuidObj + " adicionado ao cache");
                    }
                    
                    return true;
                }
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [WHITELIST-CHECK] Erro ao verificar whitelist no banco: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Verifica conflitos na whitelist para diagnóstico.
     */
    private void checkWhitelistConflict(UUID targetUuid, String targetName, Integer targetPlayerId) {
        logger.info("🔍 [WHITELIST-CONFLICT] Investigando conflito para: " + targetName);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT wp.*, pd.uuid as player_uuid, pd.name as player_name " +
                 "FROM whitelist_players wp " +
                 "LEFT JOIN player_data pd ON wp.player_id = pd.player_id " +
                 "WHERE wp.is_active = TRUE AND wp.removed_at IS NULL " +
                 "AND (wp.player_name = ? OR pd.uuid = ? OR wp.player_id = ?)")) {
            
            ps.setString(1, targetName);
            ps.setString(2, targetUuid.toString());
            ps.setInt(3, targetPlayerId != null ? targetPlayerId : -1);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int existingId = rs.getInt("id");
                    int existingPlayerId = rs.getInt("player_id");
                    String existingPlayerName = rs.getString("player_name");
                    String existingUuid = rs.getString("player_uuid");
                    String existingWhitelistName = rs.getString("player_name");
                    
                    logger.warning("⚠️ [WHITELIST-CONFLICT] Registro conflitante encontrado:");
                    logger.warning("   ID: " + existingId);
                    logger.warning("   Player ID: " + existingPlayerId + " (solicitado: " + targetPlayerId + ")");
                    logger.warning("   Nome na whitelist: " + existingWhitelistName + " (solicitado: " + targetName + ")");
                    logger.warning("   UUID: " + existingUuid + " (solicitado: " + targetUuid + ")");
                }
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [WHITELIST-CONFLICT] Erro ao investigar conflito: " + e.getMessage());
        }
    }

    /**
     * Remove um jogador da whitelist.
     * 
     * @param targetUuid UUID do jogador a ser removido
     * @param targetName Nome do jogador a ser removido
     * @param authorUuid UUID do admin que está removendo
     * @param authorName Nome do admin
     * @param reason Motivo da remoção
     * @return true se removido com sucesso, false caso contrário
     */
    public boolean removeFromWhitelist(UUID targetUuid, String targetName, UUID authorUuid, String authorName, String reason) {
        if (targetUuid == null || targetName == null || authorUuid == null || authorName == null || reason == null) {
            logger.warning("Tentativa de remover da whitelist com parâmetros inválidos");
            return false;
        }

        // Converter UUIDs para player_id usando IdentityManager ou DataManager
        Integer targetPlayerId = plugin.getIdentityManager().getPlayerIdByUuid(targetUuid);
        Integer authorPlayerId = plugin.getIdentityManager().getPlayerIdByUuid(authorUuid);
        
        // Se não encontrou no IdentityManager (jogador offline), buscar no banco
        if (targetPlayerId == null) {
            targetPlayerId = plugin.getDataManager().getPlayerIdFromDatabase(targetUuid, targetName);
            if (targetPlayerId == null) {
                logger.severe("Target UUID não encontrado no banco de dados: " + targetUuid);
                return false;
            }
        }
        
        if (authorPlayerId == null) {
            authorPlayerId = plugin.getDataManager().getPlayerIdFromDatabase(authorUuid, authorName);
            if (authorPlayerId == null) {
                // EXCEÇÃO ESPECIAL: CONSOLE não tem UUID no banco, usar player_id = 0
                if ("CONSOLE".equals(authorName) && "00000000-0000-0000-0000-000000000000".equals(authorUuid.toString())) {
                    authorPlayerId = 0; // CONSOLE sempre tem player_id = 0
                } else {
                    logger.severe("Author UUID não encontrado no banco de dados: " + authorUuid);
                    return false;
                }
            }
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(REMOVE_FROM_WHITELIST_SQL)) {
            
            // REFATORADO: Query usando player_id
            ps.setInt(1, authorPlayerId.intValue()); // removed_by_player_id
            ps.setString(2, authorName); // removed_by_name
            ps.setString(3, reason); // removal_reason
            ps.setInt(4, targetPlayerId.intValue()); // player_id (WHERE clause)
            
            int rowsAffected = ps.executeUpdate();
            
            // Verificar se algum registro foi afetado
            if (rowsAffected == 0) {
                logger.warning("Tentativa de remover jogador inexistente da whitelist: " + targetUuid + " (player_id: " + targetPlayerId + ")");
                return false;
            }
            
            // Atualizar cache com UUID
            whitelistedPlayers.remove(targetUuid);
            
            // Atualizar cache interno baseado em player_id
            whitelistedPlayerIds.remove(targetPlayerId);
            
            logger.info(String.format(
                "Jogador (UUID: %s, player_id: %d) removido da whitelist por %s. Motivo: %s",
                targetUuid, targetPlayerId, authorName, reason
            ));
            
            return true;
            
        } catch (SQLException e) {
            logger.severe("Erro ao remover jogador da whitelist: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retorna lista de todos os jogadores na whitelist.
     * 
     * @return Lista de WhitelistEntry com informações completas
     */
    public List<WhitelistEntry> getWhitelistedPlayers() {
        List<WhitelistEntry> entries = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_WHITELISTED_PLAYERS_SQL);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                int playerId = rs.getInt("player_id");
                String playerName = rs.getString("player_name");
                String addedByName = rs.getString("added_by_name");
                String reason = rs.getString("reason");
                
                // Converter player_id para UUID usando IdentityManager
                UUID playerUuid = plugin.getIdentityManager().getUuidByPlayerId(playerId);
                if (playerUuid != null) {
                    WhitelistEntry entry = new WhitelistEntry(playerUuid, playerName, addedByName, reason);
                    entries.add(entry);
                } else {
                    logger.warning("Player ID não encontrado no IdentityManager: " + playerId);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("Erro ao buscar jogadores da whitelist: " + e.getMessage());
        }
        
        return entries;
    }

    /**
     * Retorna estatísticas da whitelist.
     * 
     * @return WhitelistStats com informações estatísticas
     */
    public WhitelistStats getWhitelistStats() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_WHITELIST_STATS_SQL);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                return new WhitelistStats(
                    rs.getInt("total_entries"),
                    rs.getInt("active_players"),
                    rs.getInt("removed_players"),
                    rs.getTimestamp("first_addition"),
                    rs.getTimestamp("last_addition"),
                    rs.getInt("unique_admins")
                );
            }
            
        } catch (SQLException e) {
            logger.severe("Erro ao buscar estatísticas da whitelist: " + e.getMessage());
        }
        
        return new WhitelistStats(0, 0, 0, null, null, 0);
    }

    /**
     * Recarrega o cache da whitelist do banco de dados.
     * Útil após mudanças manuais no banco ou para sincronização.
     */
    public void reloadCache() {
        loadWhitelistFromDatabase();
        logger.info("Cache da whitelist recarregado");
    }

    /**
     * Classe interna para representar uma entrada na whitelist.
     */
    public static class WhitelistEntry {
        private final UUID playerUuid;
        private final String playerName;
        private final String addedByName;
        private final String reason;

        public WhitelistEntry(UUID playerUuid, String playerName, String addedByName, String reason) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.addedByName = addedByName;
            this.reason = reason;
        }

        public UUID getPlayerUuid() { return playerUuid; }
        public String getPlayerName() { return playerName; }
        public String getAddedByName() { return addedByName; }
        public String getReason() { return reason; }
    }

    /**
     * Classe interna para representar estatísticas da whitelist.
     */
    public static class WhitelistStats {
        private final int totalEntries;
        private final int activePlayers;
        private final int removedPlayers;
        private final java.sql.Timestamp firstAddition;
        private final java.sql.Timestamp lastAddition;
        private final int uniqueAdmins;

        public WhitelistStats(int totalEntries, int activePlayers, int removedPlayers, 
                            java.sql.Timestamp firstAddition, java.sql.Timestamp lastAddition, int uniqueAdmins) {
            this.totalEntries = totalEntries;
            this.activePlayers = activePlayers;
            this.removedPlayers = removedPlayers;
            this.firstAddition = firstAddition;
            this.lastAddition = lastAddition;
            this.uniqueAdmins = uniqueAdmins;
        }

        public int getTotalEntries() { return totalEntries; }
        public int getActivePlayers() { return activePlayers; }
        public int getRemovedPlayers() { return removedPlayers; }
        public java.sql.Timestamp getFirstAddition() { return firstAddition; }
        public java.sql.Timestamp getLastAddition() { return lastAddition; }
        public int getUniqueAdmins() { return uniqueAdmins; }
    }
}
