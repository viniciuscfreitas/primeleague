package br.com.primeleague.territories.dao;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.territories.model.ActiveWar;
import br.com.primeleague.territories.model.ClanBank;
import br.com.primeleague.territories.model.TerritoryChunk;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * DAO para operações de banco de dados relacionadas a territórios.
 * Implementa operações assíncronas para performance.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class MySqlTerritoryDAO {
    
    private final PrimeLeagueCore core;
    private final HikariDataSource dataSource;
    
    public MySqlTerritoryDAO(PrimeLeagueCore core) {
        this.core = core;
        this.dataSource = core.getDataManager().getDataSource();
    }
    
    /**
     * Cria um território de forma assíncrona.
     * 
     * @param territory Território a ser criado
     * @param callback Callback com resultado
     */
    public void createTerritoryAsync(TerritoryChunk territory, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "INSERT INTO prime_territories (clan_id, world_name, chunk_x, chunk_z, claimed_at) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setInt(1, territory.getClanId());
                stmt.setString(2, territory.getWorldName());
                stmt.setInt(3, territory.getChunkX());
                stmt.setInt(4, territory.getChunkZ());
                stmt.setTimestamp(5, territory.getClaimedAt());
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            territory.setId(generatedKeys.getInt(1));
                        }
                    }
                    callback.accept(true);
                } else {
                    callback.accept(false);
                }
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao criar território:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: clanId=" + territory.getClanId() + 
                                      ", world=" + territory.getWorldName() + 
                                      ", chunkX=" + territory.getChunkX() + 
                                      ", chunkZ=" + territory.getChunkZ());
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Remove um território de forma assíncrona.
     * 
     * @param territoryId ID do território
     * @param callback Callback com resultado
     */
    public void removeTerritoryAsync(int territoryId, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "DELETE FROM prime_territories WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, territoryId);
                int affectedRows = stmt.executeUpdate();
                callback.accept(affectedRows > 0);
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao remover território:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: territoryId=" + territoryId);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Busca território por localização de forma assíncrona.
     * 
     * @param worldName Nome do mundo
     * @param chunkX Coordenada X do chunk
     * @param chunkZ Coordenada Z do chunk
     * @param callback Callback com o território
     */
    public void getTerritoryByLocationAsync(String worldName, int chunkX, int chunkZ, Consumer<TerritoryChunk> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "SELECT * FROM prime_territories WHERE world_name = ? AND chunk_x = ? AND chunk_z = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, worldName);
                stmt.setInt(2, chunkX);
                stmt.setInt(3, chunkZ);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        TerritoryChunk territory = new TerritoryChunk();
                        territory.setId(rs.getInt("id"));
                        territory.setClanId(rs.getInt("clan_id"));
                        territory.setWorldName(rs.getString("world_name"));
                        territory.setChunkX(rs.getInt("chunk_x"));
                        territory.setChunkZ(rs.getInt("chunk_z"));
                        territory.setClaimedAt(rs.getTimestamp("claimed_at"));
                        callback.accept(territory);
                    } else {
                        callback.accept(null);
                    }
                }
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao buscar território:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: world=" + worldName + ", chunkX=" + chunkX + ", chunkZ=" + chunkZ);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(null);
            }
        });
    }
    
    /**
     * Busca territórios por clã de forma assíncrona.
     * 
     * @param clanId ID do clã
     * @param callback Callback com lista de territórios
     */
    public void getTerritoriesByClanAsync(int clanId, Consumer<List<TerritoryChunk>> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "SELECT * FROM prime_territories WHERE clan_id = ?";
            List<TerritoryChunk> territories = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, clanId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TerritoryChunk territory = new TerritoryChunk();
                        territory.setId(rs.getInt("id"));
                        territory.setClanId(rs.getInt("clan_id"));
                        territory.setWorldName(rs.getString("world_name"));
                        territory.setChunkX(rs.getInt("chunk_x"));
                        territory.setChunkZ(rs.getInt("chunk_z"));
                        territory.setClaimedAt(rs.getTimestamp("claimed_at"));
                        territories.add(territory);
                    }
                }
                
                callback.accept(territories);
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao buscar territórios do clã:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: clanId=" + clanId);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(new ArrayList<>());
            }
        });
    }
    
    /**
     * Cria uma guerra ativa de forma assíncrona.
     * 
     * @param war Guerra a ser criada
     * @param callback Callback com resultado
     */
    public void createActiveWarAsync(ActiveWar war, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "INSERT INTO prime_active_wars (aggressor_clan_id, defender_clan_id, start_time, end_time_exclusivity) VALUES (?, ?, ?, ?)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setInt(1, war.getAggressorClanId());
                stmt.setInt(2, war.getDefenderClanId());
                stmt.setTimestamp(3, war.getStartTime());
                stmt.setTimestamp(4, war.getEndTimeExclusivity());
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            war.setId(generatedKeys.getInt(1));
                        }
                    }
                    callback.accept(true);
                } else {
                    callback.accept(false);
                }
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao criar guerra ativa:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: aggressor=" + war.getAggressorClanId() + 
                                      ", defender=" + war.getDefenderClanId() + 
                                      ", startTime=" + war.getStartTime());
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Cria um cerco ativo de forma assíncrona.
     * 
     * @param siege Cerco a ser criado
     * @param callback Callback com resultado
     */
    public void createActiveSiegeAsync(ActiveSiege siege, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "INSERT INTO prime_active_sieges (war_id, territory_id, aggressor_clan_id, defender_clan_id, start_time, end_time, altar_location, current_timer) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setInt(1, siege.getWarId());
                stmt.setInt(2, siege.getTerritoryId());
                stmt.setInt(3, siege.getAggressorClanId());
                stmt.setInt(4, siege.getDefenderClanId());
                stmt.setTimestamp(5, siege.getStartTime());
                stmt.setTimestamp(6, siege.getEndTime());
                stmt.setString(7, siege.getAltarLocation().getWorld().getName() + ":" + siege.getAltarLocation().getBlockX() + ":" + siege.getAltarLocation().getBlockY() + ":" + siege.getAltarLocation().getBlockZ());
                stmt.setInt(8, siege.getRemainingTime());
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            siege.setId(generatedKeys.getInt(1));
                        }
                    }
                    callback.accept(true);
                } else {
                    callback.accept(false);
                }
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao criar cerco ativo:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: warId=" + siege.getWarId() + 
                                      ", territoryId=" + siege.getTerritoryId() + 
                                      ", aggressor=" + siege.getAggressorClanId() + 
                                      ", defender=" + siege.getDefenderClanId());
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Atualiza um cerco ativo de forma assíncrona.
     * 
     * @param siege Cerco a ser atualizado
     * @param callback Callback com resultado
     */
    public void updateActiveSiegeAsync(ActiveSiege siege, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "UPDATE prime_active_sieges SET end_time = ?, current_timer = ?, status = ? WHERE id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setTimestamp(1, siege.getEndTime());
                stmt.setInt(2, siege.getRemainingTime());
                stmt.setString(3, siege.getStatus().name());
                stmt.setInt(4, siege.getId());
                
                int affectedRows = stmt.executeUpdate();
                callback.accept(affectedRows > 0);
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao atualizar cerco ativo:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: siegeId=" + siege.getId() + 
                                      ", endTime=" + siege.getEndTime() + 
                                      ", timer=" + siege.getRemainingTime() + 
                                      ", status=" + siege.getStatus());
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Obtém o banco de um clã de forma assíncrona.
     * 
     * @param clanId ID do clã
     * @param callback Callback com o banco
     */
    public void getClanBankAsync(int clanId, Consumer<ClanBank> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "SELECT * FROM prime_clan_bank WHERE clan_id = ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, clanId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ClanBank bank = new ClanBank();
                        bank.setClanId(rs.getInt("clan_id"));
                        bank.setBalance(rs.getBigDecimal("balance"));
                        callback.accept(bank);
                    } else {
                        // Criar banco se não existir
                        ClanBank bank = new ClanBank();
                        bank.setClanId(clanId);
                        bank.setBalance(BigDecimal.ZERO);
                        callback.accept(bank);
                    }
                }
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao buscar banco do clã:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: clanId=" + clanId);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(null);
            }
        });
    }
    
    /**
     * Deposita dinheiro no banco do clã de forma assíncrona.
     * 
     * @param clanId ID do clã
     * @param amount Quantia
     * @param callback Callback com resultado
     */
    public void depositToClanBankAsync(int clanId, BigDecimal amount, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "INSERT INTO prime_clan_bank (clan_id, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = balance + ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, clanId);
                stmt.setBigDecimal(2, amount);
                stmt.setBigDecimal(3, amount);
                
                int affectedRows = stmt.executeUpdate();
                callback.accept(affectedRows > 0);
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao depositar no banco do clã:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: clanId=" + clanId + ", amount=" + amount);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
    
    /**
     * Saca dinheiro do banco do clã de forma assíncrona.
     * 
     * @param clanId ID do clã
     * @param amount Quantia
     * @param callback Callback com resultado
     */
    public void withdrawFromClanBankAsync(int clanId, BigDecimal amount, Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            String sql = "UPDATE prime_clan_bank SET balance = balance - ? WHERE clan_id = ? AND balance >= ?";
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setBigDecimal(1, amount);
                stmt.setInt(2, clanId);
                stmt.setBigDecimal(3, amount);
                
                int affectedRows = stmt.executeUpdate();
                callback.accept(affectedRows > 0);
                
            } catch (SQLException e) {
                // Logging aprimorado com detalhes da query e parâmetros
                core.getLogger().severe("Erro ao sacar do banco do clã:");
                core.getLogger().severe("  Query: " + sql);
                core.getLogger().severe("  Parâmetros: clanId=" + clanId + ", amount=" + amount);
                core.getLogger().severe("  Erro SQL: " + e.getMessage());
                core.getLogger().severe("  SQL State: " + e.getSQLState());
                core.getLogger().severe("  Error Code: " + e.getErrorCode());
                callback.accept(false);
            }
        });
    }
}