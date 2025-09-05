package br.com.primeleague.essentials.dao;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.models.KitCooldown;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.api.dao.KitDAO;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementação MySQL do KitDAO.
 * Gerencia operações de banco de dados para cooldowns de kits.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class MySqlKitDAO implements KitDAO {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    
    /**
     * Construtor do MySqlKitDAO.
     * 
     * @param plugin Instância do plugin principal
     */
    public MySqlKitDAO(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @Override
    public void loadPlayerKitCooldownsAsync(int playerId, Consumer<List<KitCooldown>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final List<KitCooldown> cooldowns = new ArrayList<KitCooldown>();
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT cooldown_id, player_id, kit_name, last_used, uses_count, expires_at " +
                           "FROM player_kit_cooldowns WHERE player_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            KitCooldown cooldown = new KitCooldown(
                                rs.getInt("cooldown_id"),
                                rs.getInt("player_id"),
                                rs.getString("kit_name"),
                                rs.getTimestamp("last_used"),
                                rs.getInt("uses_count"),
                                rs.getTimestamp("expires_at")
                            );
                            cooldowns.add(cooldown);
                        }
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(cooldowns));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao carregar cooldowns de kits do jogador " + playerId + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(cooldowns));
            }
        });
    }
    
    @Override
    public void saveKitCooldownAsync(KitCooldown cooldown, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "INSERT INTO player_kit_cooldowns (player_id, kit_name, last_used, uses_count, expires_at) " +
                           "VALUES (?, ?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE " +
                           "last_used = VALUES(last_used), " +
                           "uses_count = VALUES(uses_count), " +
                           "expires_at = VALUES(expires_at)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, cooldown.getPlayerId());
                    stmt.setString(2, cooldown.getKitName());
                    stmt.setTimestamp(3, cooldown.getLastUsed());
                    stmt.setInt(4, cooldown.getUsesCount());
                    stmt.setTimestamp(5, cooldown.getExpiresAt());
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao salvar cooldown de kit: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void getKitCooldownAsync(int playerId, String kitName, Consumer<KitCooldown> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final KitCooldown[] cooldown = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT cooldown_id, player_id, kit_name, last_used, uses_count, expires_at " +
                           "FROM player_kit_cooldowns WHERE player_id = ? AND kit_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, kitName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            cooldown[0] = new KitCooldown(
                                rs.getInt("cooldown_id"),
                                rs.getInt("player_id"),
                                rs.getString("kit_name"),
                                rs.getTimestamp("last_used"),
                                rs.getInt("uses_count"),
                                rs.getTimestamp("expires_at")
                            );
                        }
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(cooldown[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao obter cooldown de kit: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    @Override
    public void removeKitCooldownAsync(int playerId, String kitName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "DELETE FROM player_kit_cooldowns WHERE player_id = ? AND kit_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, kitName);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao remover cooldown de kit: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void cleanExpiredCooldownsAsync(Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final int[] cleanedCount = {0};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "DELETE FROM player_kit_cooldowns WHERE expires_at IS NOT NULL AND expires_at < NOW()";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    cleanedCount[0] = stmt.executeUpdate();
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(cleanedCount[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao limpar cooldowns expirados: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(0));
            }
        });
    }
    
    @Override
    public void getPlayerIdAsync(String playerUuid, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final Integer[] playerId = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT player_id FROM player_data WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            playerId[0] = rs.getInt("player_id");
                        }
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(playerId[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao obter ID do jogador: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    @Override
    public void cooldownExistsAsync(int playerId, String kitName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] exists = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT 1 FROM player_kit_cooldowns WHERE player_id = ? AND kit_name = ? LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, kitName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        exists[0] = rs.next();
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(exists[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao verificar existência de cooldown: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void updateKitLastUsedAsync(int playerId, String kitName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE player_kit_cooldowns SET last_used = NOW() WHERE player_id = ? AND kit_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, kitName);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao atualizar último uso do kit: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void incrementKitUsesAsync(int playerId, String kitName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE player_kit_cooldowns SET uses_count = uses_count + 1, last_used = NOW() " +
                           "WHERE player_id = ? AND kit_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, kitName);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao incrementar usos do kit: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
}