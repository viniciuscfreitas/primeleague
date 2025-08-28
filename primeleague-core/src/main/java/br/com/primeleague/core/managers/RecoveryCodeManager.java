package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import javax.sql.DataSource;
import java.sql.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gerenciador de códigos de recuperação de conta P2P.
 * Implementa segurança inegociável, UX clara e auditabilidade total.
 */
public class RecoveryCodeManager {
    
    private final PrimeLeagueCore plugin;
    private final DataSource dataSource;
    private final SecureRandom secureRandom;
    
    // Cache para rate limiting (IP -> tentativas)
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    // Configurações de segurança
    private static final int MAX_ATTEMPTS_PER_IP = 5;
    private static final int RATE_LIMIT_WINDOW_HOURS = 1;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final int TEMPORARY_CODE_LENGTH = 6;
    
    public RecoveryCodeManager(PrimeLeagueCore plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.secureRandom = new SecureRandom();
        
        // Limpar cache de rate limiting a cada hora
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this::cleanupRateLimitCache, 20 * 60 * 60, 20 * 60 * 60);
    }
    
    /**
     * Gera novos códigos de backup para um jogador.
     * Invalida todos os códigos antigos e cria um novo conjunto.
     * 
     * @param playerId ID do jogador
     * @param discordId Discord ID do jogador
     * @param ipAddress IP de origem
     * @return Lista de códigos gerados (mostrados apenas uma vez)
     * @throws SQLException se houver erro no banco
     */
    public List<String> generateBackupCodes(int playerId, String discordId, String ipAddress) throws SQLException {
        // Invalidar todos os códigos de backup existentes
        invalidateAllBackupCodes(playerId);
        
        // Gerar 5 novos códigos de backup
        List<String> codes = new ArrayList<>();
        List<String> hashedCodes = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            String code = generateRandomCode(BACKUP_CODE_LENGTH);
            codes.add(code);
            hashedCodes.add(hashCode(code));
        }
        
        // Salvar códigos no banco
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO recovery_codes (player_id, code_hash, code_type, ip_address, discord_id) " +
                     "VALUES (?, ?, 'BACKUP', ?, ?)")) {
            
            for (String hashedCode : hashedCodes) {
                stmt.setInt(1, playerId);
                stmt.setString(2, hashedCode);
                stmt.setString(3, ipAddress);
                stmt.setString(4, discordId);
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        }
        
        plugin.getLogger().info("[RECOVERY] 5 códigos de backup gerados para player_id=" + playerId + " via Discord " + discordId);
        return codes;
    }
    
    /**
     * Verifica se um código de recuperação é válido.
     * Implementa rate limiting e auditoria completa.
     * 
     * @param playerName Nome do jogador
     * @param code Código fornecido
     * @param ipAddress IP de origem
     * @return true se válido, false caso contrário
     */
    public boolean verifyCode(String playerName, String code, String ipAddress) {
        // Verificar rate limiting
        if (isRateLimited(ipAddress)) {
            plugin.getLogger().warning("[RECOVERY] Rate limit atingido para IP: " + ipAddress);
            return false;
        }
        
        try {
            // Buscar player_id pelo nome
            Integer playerId = getPlayerIdByName(playerName);
            plugin.getLogger().info("[RECOVERY] DEBUG: Buscando player_id para '" + playerName + "', resultado: " + playerId);
            if (playerId == null) {
                plugin.getLogger().warning("[RECOVERY] Jogador não encontrado: " + playerName);
                return false;
            }
            
            // Buscar código ativo
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, code_hash, attempts, status FROM recovery_codes " +
                         "WHERE player_id = ? AND status = 'ACTIVE' AND code_type = 'BACKUP' " +
                         "AND (expires_at IS NULL OR expires_at > NOW())")) {
                
                stmt.setInt(1, playerId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int codesFound = 0;
                    while (rs.next()) {
                        codesFound++;
                        String storedHash = rs.getString("code_hash");
                        int attempts = rs.getInt("attempts");
                        long codeId = rs.getLong("id");
                        
                        plugin.getLogger().info("[RECOVERY] DEBUG: Verificando código " + codesFound + " (ID: " + codeId + ", tentativas: " + attempts + ")");
                        
                        // Verificar se o código corresponde
                        plugin.getLogger().info("[RECOVERY] DEBUG: Verificando código '" + code + "' contra hash '" + storedHash + "'");
                        boolean hashMatches = verifyHash(code, storedHash);
                        plugin.getLogger().info("[RECOVERY] DEBUG: Resultado da verificação: " + hashMatches);
                        
                        if (hashMatches) {
                            // Código válido encontrado - marcar como usado
                            markCodeAsUsed(codeId, ipAddress);
                            plugin.getLogger().info("[RECOVERY] Código válido usado por " + playerName + " (IP: " + ipAddress + ")");
                            return true;
                        } else {
                            plugin.getLogger().info("[RECOVERY] DEBUG: Hash não corresponde para código " + codeId);
                            // Incrementar tentativas
                            incrementAttempts(codeId, attempts);
                        }
                    }
                    plugin.getLogger().info("[RECOVERY] DEBUG: Total de códigos encontrados para player_id=" + playerId + ": " + codesFound);
                }
            }
            
            // Código não encontrado ou inválido
            incrementRateLimit(ipAddress);
            plugin.getLogger().warning("[RECOVERY] Código inválido para " + playerName + " (IP: " + ipAddress + ")");
            return false;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("[RECOVERY] Erro ao verificar código: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Invalida todos os códigos de backup de um jogador.
     * 
     * @param playerId ID do jogador
     * @throws SQLException se houver erro no banco
     */
    public void invalidateAllBackupCodes(int playerId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE recovery_codes SET status = 'USED', used_at = NOW() " +
                     "WHERE player_id = ? AND code_type = 'BACKUP' AND status = 'ACTIVE'")) {
            
            stmt.setInt(1, playerId);
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                plugin.getLogger().info("[RECOVERY] " + affected + " códigos de backup invalidados para player_id=" + playerId);
            }
        }
    }
    
    /**
     * Gera código aleatório seguro.
     */
    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return code.toString();
    }
    
    /**
     * Aplica hash BCrypt ao código.
     */
    private String hashCode(String code) {
        return org.mindrot.jbcrypt.BCrypt.hashpw(code, org.mindrot.jbcrypt.BCrypt.gensalt());
    }
    
    /**
     * Verifica se o código corresponde ao hash.
     */
    private boolean verifyHash(String code, String hash) {
        return org.mindrot.jbcrypt.BCrypt.checkpw(code, hash);
    }
    
    /**
     * Busca player_id pelo nome.
     */
    private Integer getPlayerIdByName(String playerName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player_id FROM player_data WHERE name = ?")) {
            
            stmt.setString(1, playerName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_id");
                }
            }
        }
        return null;
    }
    
    /**
     * Marca código como usado.
     */
    private void markCodeAsUsed(long codeId, String ipAddress) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE recovery_codes SET status = 'USED', used_at = NOW(), ip_address = ? " +
                     "WHERE id = ?")) {
            
            stmt.setString(1, ipAddress);
            stmt.setLong(2, codeId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Incrementa tentativas de uso.
     */
    private void incrementAttempts(long codeId, int currentAttempts) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE recovery_codes SET attempts = ?, status = ? " +
                     "WHERE id = ?")) {
            
            int newAttempts = currentAttempts + 1;
            String newStatus = newAttempts >= MAX_ATTEMPTS_PER_IP ? "BLOCKED" : "ACTIVE";
            
            stmt.setInt(1, newAttempts);
            stmt.setString(2, newStatus);
            stmt.setLong(3, codeId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Rate limiting por IP.
     */
    private boolean isRateLimited(String ipAddress) {
        RateLimitInfo info = rateLimitCache.get(ipAddress);
        if (info == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (now - info.firstAttempt > TimeUnit.HOURS.toMillis(RATE_LIMIT_WINDOW_HOURS)) {
            rateLimitCache.remove(ipAddress);
            return false;
        }
        
        return info.attempts >= MAX_ATTEMPTS_PER_IP;
    }
    
    /**
     * Incrementa contador de rate limiting.
     */
    private void incrementRateLimit(String ipAddress) {
        RateLimitInfo info = rateLimitCache.computeIfAbsent(ipAddress, k -> new RateLimitInfo());
        info.attempts++;
        
        if (info.firstAttempt == 0) {
            info.firstAttempt = System.currentTimeMillis();
        }
    }
    
    /**
     * Limpa cache de rate limiting.
     */
    private void cleanupRateLimitCache() {
        long cutoff = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(RATE_LIMIT_WINDOW_HOURS);
        rateLimitCache.entrySet().removeIf(entry -> entry.getValue().firstAttempt < cutoff);
    }
    
    /**
     * Gera um código temporário de re-vinculação.
     * Código válido por 5 minutos para re-vinculação de conta.
     * 
     * @param playerName Nome do jogador
     * @param discordId Discord ID do jogador
     * @param ipAddress IP de origem
     * @return Código temporário gerado
     */
    public String generateTemporaryRelinkCode(String playerName, String discordId, String ipAddress) {
        try {
            Integer playerId = getPlayerIdByName(playerName);
            if (playerId == null) {
                plugin.getLogger().warning("[RECOVERY] Player não encontrado para gerar código temporário: " + playerName);
                return null;
            }
            
            // Gerar código temporário
            String code = generateRandomCode(TEMPORARY_CODE_LENGTH);
            String hashedCode = hashCode(code);
            
            // Salvar no banco com expiração de 5 minutos
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO recovery_codes (player_id, code_hash, code_type, ip_address, discord_id, expires_at) " +
                         "VALUES (?, ?, 'TEMPORARY', ?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))")) {
                
                stmt.setInt(1, playerId);
                stmt.setString(2, hashedCode);
                stmt.setString(3, ipAddress);
                stmt.setString(4, discordId);
                stmt.executeUpdate();
            }
            
            plugin.getLogger().info("[RECOVERY] Código temporário gerado para " + playerName + " (Discord: " + discordId + ")");
            return code;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("[RECOVERY] Erro ao gerar código temporário: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica um código temporário de re-vinculação.
     * 
     * @param playerName Nome do jogador
     * @param code Código fornecido
     * @param ipAddress IP de origem
     * @return true se válido, false caso contrário
     */
    public boolean verifyTemporaryRelinkCode(String playerName, String code, String ipAddress) {
        try {
            Integer playerId = getPlayerIdByName(playerName);
            if (playerId == null) {
                return false;
            }
            
            String hashedCode = hashCode(code);
            
            // Verificar código temporário válido e não expirado
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT id, attempts FROM recovery_codes " +
                         "WHERE player_id = ? AND code_hash = ? AND code_type = 'TEMPORARY' " +
                         "AND status = 'ACTIVE' AND expires_at > NOW() " +
                         "ORDER BY created_at DESC LIMIT 1")) {
                
                stmt.setInt(1, playerId);
                stmt.setString(2, hashedCode);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long codeId = rs.getLong("id");
                        int attempts = rs.getInt("attempts");
                        
                        // Marcar como usado
                        markCodeAsUsed(codeId, ipAddress);
                        
                        plugin.getLogger().info("[RECOVERY] Código temporário validado para " + playerName);
                        return true;
                    }
                }
            }
            
            plugin.getLogger().warning("[RECOVERY] Código temporário inválido para " + playerName);
            return false;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("[RECOVERY] Erro ao verificar código temporário: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Classe para armazenar informações de rate limiting.
     */
    private static class RateLimitInfo {
        int attempts = 0;
        long firstAttempt = 0;
    }
}
