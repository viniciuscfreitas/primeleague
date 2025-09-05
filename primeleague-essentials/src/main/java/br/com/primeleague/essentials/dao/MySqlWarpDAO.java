package br.com.primeleague.essentials.dao;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.models.Warp;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.api.dao.WarpDAO;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementação MySQL do WarpDAO.
 * Gerencia operações assíncronas de banco de dados para warps públicos.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class MySqlWarpDAO implements WarpDAO {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    
    /**
     * Construtor do MySqlWarpDAO.
     * 
     * @param plugin Instância do plugin principal
     */
    public MySqlWarpDAO(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @Override
    public void createWarpAsync(Warp warp, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "INSERT INTO essentials_warps (warp_name, world, x, y, z, yaw, pitch, permission_node, cost, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, warp.getWarpName());
                    stmt.setString(2, warp.getWorld());
                    stmt.setDouble(3, warp.getX());
                    stmt.setDouble(4, warp.getY());
                    stmt.setDouble(5, warp.getZ());
                    stmt.setFloat(6, warp.getYaw());
                    stmt.setFloat(7, warp.getPitch());
                    stmt.setString(8, warp.getPermissionNode());
                    stmt.setBigDecimal(9, warp.getCost());
                    stmt.setString(10, warp.getCreatedBy());
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                warp.setWarpId(generatedKeys.getInt(1));
                            }
                        }
                        success[0] = true;
                        logger.info("✅ Warp criado: " + warp.getWarpName());
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao criar warp " + warp.getWarpName() + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void getWarpAsync(String warpName, Consumer<Warp> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final Warp[] warp = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM essentials_warps WHERE warp_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, warpName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            warp[0] = mapResultSetToWarp(rs);
                        }
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(warp[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar warp " + warpName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    @Override
    public void getAllWarpsAsync(Consumer<List<Warp>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final List<Warp>[] warps = new List[]{new ArrayList<Warp>()};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM essentials_warps ORDER BY warp_name";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        Warp warp = mapResultSetToWarp(rs);
                        warps[0].add(warp);
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(warps[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar todos os warps: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<Warp>()));
            }
        });
    }
    
    @Override
    public void getAvailableWarpsAsync(String playerName, Consumer<List<Warp>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final List<Warp>[] availableWarps = new List[]{new ArrayList<Warp>()};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                // Buscar warps que não requerem permissão ou que o jogador tem permissão
                String sql = "SELECT * FROM essentials_warps WHERE permission_node IS NULL OR permission_node = '' ORDER BY warp_name";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    
                    while (rs.next()) {
                        Warp warp = mapResultSetToWarp(rs);
                        
                        // Verificar permissão se necessário
                        if (warp.requiresPermission()) {
                            org.bukkit.entity.Player player = Bukkit.getPlayer(playerName);
                            if (player != null && player.hasPermission(warp.getPermissionNode())) {
                                availableWarps[0].add(warp);
                            }
                        } else {
                            availableWarps[0].add(warp);
                        }
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(availableWarps[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar warps disponíveis para " + playerName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<Warp>()));
            }
        });
    }
    
    @Override
    public void updateWarpAsync(Warp warp, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE essentials_warps SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, permission_node = ?, cost = ? WHERE warp_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, warp.getWorld());
                    stmt.setDouble(2, warp.getX());
                    stmt.setDouble(3, warp.getY());
                    stmt.setDouble(4, warp.getZ());
                    stmt.setFloat(5, warp.getYaw());
                    stmt.setFloat(6, warp.getPitch());
                    stmt.setString(7, warp.getPermissionNode());
                    stmt.setBigDecimal(8, warp.getCost());
                    stmt.setString(9, warp.getWarpName());
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                    
                    if (success[0]) {
                        logger.info("✅ Warp atualizado: " + warp.getWarpName());
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao atualizar warp " + warp.getWarpName() + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void deleteWarpAsync(String warpName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "DELETE FROM essentials_warps WHERE warp_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, warpName);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                    
                    if (success[0]) {
                        logger.info("✅ Warp removido: " + warpName);
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao remover warp " + warpName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void warpExistsAsync(String warpName, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] exists = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT 1 FROM essentials_warps WHERE warp_name = ? LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, warpName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        exists[0] = rs.next();
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(exists[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao verificar existência do warp " + warpName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void updateWarpUsageAsync(int warpId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE essentials_warps SET last_used = CURRENT_TIMESTAMP, usage_count = usage_count + 1 WHERE warp_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, warpId);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao atualizar uso do warp ID " + warpId + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void getWarpStatsAsync(String warpName, Consumer<Warp> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final Warp[] warp = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT warp_id, warp_name, last_used, usage_count FROM essentials_warps WHERE warp_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, warpName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            warp[0] = new Warp();
                            warp[0].setWarpId(rs.getInt("warp_id"));
                            warp[0].setWarpName(rs.getString("warp_name"));
                            warp[0].setLastUsed(rs.getTimestamp("last_used"));
                            warp[0].setUsageCount(rs.getInt("usage_count"));
                        }
                    }
                }
                
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(warp[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar estatísticas do warp " + warpName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    /**
     * Mapeia um ResultSet para um objeto Warp.
     */
    private Warp mapResultSetToWarp(ResultSet rs) throws SQLException {
        Warp warp = new Warp();
        warp.setWarpId(rs.getInt("warp_id"));
        warp.setWarpName(rs.getString("warp_name"));
        warp.setWorld(rs.getString("world"));
        warp.setX(rs.getDouble("x"));
        warp.setY(rs.getDouble("y"));
        warp.setZ(rs.getDouble("z"));
        warp.setYaw(rs.getFloat("yaw"));
        warp.setPitch(rs.getFloat("pitch"));
        warp.setPermissionNode(rs.getString("permission_node"));
        warp.setCost(rs.getBigDecimal("cost"));
        warp.setCreatedBy(rs.getString("created_by"));
        warp.setCreatedAt(rs.getTimestamp("created_at"));
        warp.setLastUsed(rs.getTimestamp("last_used"));
        warp.setUsageCount(rs.getInt("usage_count"));
        return warp;
    }
}
