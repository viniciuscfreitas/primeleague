package br.com.primeleague.admin.dao;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.api.dao.PunishmentDAO;
import br.com.primeleague.api.models.Punishment;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implementação MySQL para operações de persistência de punições.
 * Todas as operações são assíncronas para não bloquear a thread principal.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class MySqlPunishmentDAO implements PunishmentDAO {

    private final PrimeLeagueAdmin plugin;
    private final java.util.logging.Logger logger;

    public MySqlPunishmentDAO(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void applyPunishmentAsync(Punishment punishment, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "INSERT INTO admin_punishments (player_id, staff_id, punishment_type, reason, " +
                           "duration_seconds, applied_at, expires_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, punishment.getPlayerId());
                    stmt.setInt(2, punishment.getStaffId());
                    stmt.setString(3, punishment.getPunishmentType());
                    stmt.setString(4, punishment.getReason());
                    stmt.setLong(5, punishment.getDurationSeconds());
                    stmt.setTimestamp(6, punishment.getAppliedAt());
                    stmt.setTimestamp(7, punishment.getExpiresAt());
                    stmt.setBoolean(8, punishment.isActive());
                    
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    if (success && punishment.getPunishmentId() == 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                punishment.setPunishmentId(generatedKeys.getInt(1));
                            }
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao aplicar punição: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void removePunishmentAsync(int punishmentId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE admin_punishments SET is_active = false, removed_at = NOW() WHERE punishment_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, punishmentId);
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao remover punição: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void getActivePunishmentsAsync(int playerId, Consumer<List<Punishment>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE player_id = ? AND is_active = true " +
                           "AND (expires_at IS NULL OR expires_at > NOW())";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Punishment> punishments = new ArrayList<>();
                        while (rs.next()) {
                            punishments.add(mapResultSetToPunishment(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishments));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar punições ativas: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getPunishmentHistoryAsync(int playerId, Consumer<List<Punishment>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE player_id = ? ORDER BY applied_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Punishment> punishments = new ArrayList<>();
                        while (rs.next()) {
                            punishments.add(mapResultSetToPunishment(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishments));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar histórico de punições: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getPunishmentsByTypeAsync(String punishmentType, Consumer<List<Punishment>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE punishment_type = ? ORDER BY applied_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, punishmentType);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Punishment> punishments = new ArrayList<>();
                        while (rs.next()) {
                            punishments.add(mapResultSetToPunishment(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishments));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar punições por tipo: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getActivePunishmentByTypeAsync(int playerId, String punishmentType, Consumer<Punishment> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE player_id = ? AND punishment_type = ? " +
                           "AND is_active = true AND (expires_at IS NULL OR expires_at > NOW()) " +
                           "ORDER BY applied_at DESC LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    stmt.setString(2, punishmentType);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        final Punishment punishment = rs.next() ? mapResultSetToPunishment(rs) : null;
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishment));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar punição ativa por tipo: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    @Override
    public void getPunishmentsByStaffAsync(int staffPlayerId, Consumer<List<Punishment>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE staff_id = ? ORDER BY applied_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, staffPlayerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Punishment> punishments = new ArrayList<>();
                        while (rs.next()) {
                            punishments.add(mapResultSetToPunishment(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishments));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar punições por staff: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void updatePunishmentAsync(Punishment punishment, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE admin_punishments SET reason = ?, duration_seconds = ?, " +
                           "expires_at = ?, is_active = ? WHERE punishment_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, punishment.getReason());
                    stmt.setLong(2, punishment.getDurationSeconds());
                    stmt.setTimestamp(3, punishment.getExpiresAt());
                    stmt.setBoolean(4, punishment.isActive());
                    stmt.setInt(5, punishment.getPunishmentId());
                    
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao atualizar punição: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void getPunishmentByIdAsync(int punishmentId, Consumer<Punishment> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_punishments WHERE punishment_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, punishmentId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        final Punishment punishment = rs.next() ? mapResultSetToPunishment(rs) : null;
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(punishment));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar punição por ID: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    /**
     * Mapeia um ResultSet para um objeto Punishment.
     */
    private Punishment mapResultSetToPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setPunishmentId(rs.getInt("punishment_id"));
        punishment.setPlayerId(rs.getInt("player_id"));
        punishment.setStaffId(rs.getInt("staff_id"));
        punishment.setPunishmentType(rs.getString("punishment_type"));
        punishment.setReason(rs.getString("reason"));
        punishment.setDurationSeconds(rs.getLong("duration_seconds"));
        punishment.setAppliedAt(rs.getTimestamp("applied_at"));
        punishment.setExpiresAt(rs.getTimestamp("expires_at"));
        punishment.setActive(rs.getBoolean("is_active"));
        punishment.setRemovedAt(rs.getTimestamp("removed_at"));
        return punishment;
    }
}
