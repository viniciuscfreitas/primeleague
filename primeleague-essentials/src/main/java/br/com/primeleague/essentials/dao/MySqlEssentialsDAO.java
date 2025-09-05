package br.com.primeleague.essentials.dao;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.models.Home;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.api.dao.EssentialsDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Implementação MySQL do EssentialsDAO com operações 100% assíncronas.
 * Segue o padrão arquitetural estabelecido para performance não-bloqueante.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class MySqlEssentialsDAO implements EssentialsDAO {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    
    /**
     * Construtor do DAO.
     * 
     * @param plugin Instância do plugin principal
     */
    public MySqlEssentialsDAO(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }
    
    @Override
    public void loadPlayerHomesAsync(UUID playerUuid, Consumer<List<Home>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final List<Home> homes = new ArrayList<>();
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT h.* FROM player_homes h " +
                            "JOIN player_data p ON h.1 = p.player_id " +
                            "WHERE p.uuid = ? ORDER BY h.home_name";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            Home home = new Home();
                            home.setHomeId(rs.getInt("home_id"));
                            home.setPlayerId(rs.getInt("player_id"));
                            home.setHomeName(rs.getString("home_name"));
                            home.setWorld(rs.getString("world"));
                            home.setX(rs.getDouble("x"));
                            home.setY(rs.getDouble("y"));
                            home.setZ(rs.getDouble("z"));
                            home.setYaw(rs.getFloat("yaw"));
                            home.setPitch(rs.getFloat("pitch"));
                            home.setCreatedAt(rs.getTimestamp("created_at"));
                            home.setLastUsed(rs.getTimestamp("last_used"));
                            
                            homes.add(home);
                        }
                    }
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(homes));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao carregar homes do banco: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com lista vazia em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }
    
    @Override
    public void createHomeAsync(Home home, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "INSERT INTO player_homes (player_id, home_name, world, x, y, z, yaw, pitch, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, home.getPlayerId());
                    stmt.setString(2, home.getHomeName());
                    stmt.setString(3, home.getWorld());
                    stmt.setDouble(4, home.getX());
                    stmt.setDouble(5, home.getY());
                    stmt.setDouble(6, home.getZ());
                    stmt.setFloat(7, home.getYaw());
                    stmt.setFloat(8, home.getPitch());
                    stmt.setTimestamp(9, home.getCreatedAt());
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao criar home no banco: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com false em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void deleteHomeAsync(int homeId, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "DELETE FROM player_homes WHERE home_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, homeId);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao remover home do banco: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com false em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void updateHomeLastUsedAsync(int homeId, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] success = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE player_homes SET last_used = CURRENT_TIMESTAMP WHERE home_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, homeId);
                    
                    int rowsAffected = stmt.executeUpdate();
                    success[0] = rowsAffected > 0;
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(success[0]));
                
            } catch (SQLException e) {
                logger.warning("⚠️ Erro ao atualizar último uso da home: " + e.getMessage());
                
                // Executar callback com false em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void homeExistsAsync(UUID playerUuid, String homeName, Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] exists = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT COUNT(*) FROM player_homes h " +
                            "JOIN player_data p ON h.player_id = p.player_id " +
                            "WHERE p.uuid = ? AND h.home_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, homeName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            exists[0] = rs.getInt(1) > 0;
                        }
                    }
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(exists[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao verificar existência da home: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com false em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    @Override
    public void getPlayerHomeCountAsync(UUID playerUuid, Consumer<Integer> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final int[] count = {0};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT COUNT(*) FROM player_homes h " +
                            "JOIN player_data p ON h.player_id = p.player_id " +
                            "WHERE p.uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            count[0] = rs.getInt(1);
                        }
                    }
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(count[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao contar homes do jogador: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com 0 em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(0));
            }
        });
    }
    
    @Override
    public void getHomeAsync(UUID playerUuid, String homeName, Consumer<Home> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final Home[] home = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT h.* FROM player_homes h " +
                            "JOIN player_data p ON h.player_id = p.player_id " +
                            "WHERE p.uuid = ? AND h.home_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, homeName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            home[0] = new Home();
                            home[0].setHomeId(rs.getInt("home_id"));
                            home[0].setPlayerId(rs.getInt("player_id"));
                            home[0].setHomeName(rs.getString("home_name"));
                            home[0].setWorld(rs.getString("world"));
                            home[0].setX(rs.getDouble("x"));
                            home[0].setY(rs.getDouble("y"));
                            home[0].setZ(rs.getDouble("z"));
                            home[0].setYaw(rs.getFloat("yaw"));
                            home[0].setPitch(rs.getFloat("pitch"));
                            home[0].setCreatedAt(rs.getTimestamp("created_at"));
                            home[0].setLastUsed(rs.getTimestamp("last_used"));
                        }
                    }
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(home[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar home específica: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com null em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    @Override
    public void getPlayerIdAsync(UUID playerUuid, Consumer<Integer> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            final int[] playerId = {-1};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT player_id FROM player_data WHERE uuid = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            playerId[0] = rs.getInt("player_id");
                        }
                    }
                }
                
                // Executar callback na thread principal
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(playerId[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao obter ID do jogador: " + e.getMessage());
                e.printStackTrace();
                
                // Executar callback com -1 em caso de erro
                plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(-1));
            }
        });
    }
}