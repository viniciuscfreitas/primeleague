package br.com.primeleague.core.database;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.api.dto.ClanPlayerDTO;
import br.com.primeleague.api.dto.ClanRelationDTO;
import br.com.primeleague.api.dto.ClanLogDTO;
import br.com.primeleague.api.dto.InactiveMemberInfo;
import br.com.primeleague.api.dto.ClanMemberInfo;
import br.com.primeleague.api.dto.ClanRankingInfoDTO;
import br.com.primeleague.api.enums.LogActionType;
import br.com.primeleague.api.enums.ClanRole;
import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.managers.DataManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.*;
import java.util.Date;

/**
 * Implementação MySQL do ClanDAO para o Prime League Core.
 * Esta classe é responsável por todas as operações de banco de dados relacionadas aos clãs.
 * 
 * REFATORADO para schema normalizado:
 * - Removidas referências a founder_name e player_name das tabelas
 * - Implementados LEFT JOINs com player_data para obter nomes
 * - removeInactiveMember usa DELETE conforme decisão arquitetural
 */
public class MySqlClanDAO implements ClanDAO {

    private final PrimeLeagueCore core;
    private final DataManager dataManager;

    public MySqlClanDAO(PrimeLeagueCore core) {
        this.core = core;
        this.dataManager = core.getDataManager();
    }

    @Override
    public Map<Integer, ClanDTO> loadAllClans() {
        Map<Integer, ClanDTO> clans = new HashMap<>();
        
        // Query com LEFT JOIN para obter founder_name da tabela player_data
        String sql = "SELECT c.*, pd.name as founder_name FROM clans c " +
                    "LEFT JOIN player_data pd ON c.founder_player_id = pd.player_id";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int id = rs.getInt("id");
                String tag = rs.getString("tag");
                String name = rs.getString("name");
                int founderPlayerId = rs.getInt("founder_player_id");
                String founderName = rs.getString("founder_name"); // Obtido via JOIN
                boolean friendlyFire = rs.getBoolean("friendly_fire_enabled");
                Date creationDate = rs.getTimestamp("creation_date");
                int penaltyPoints = rs.getInt("penalty_points");
                int rankingPoints = rs.getInt("ranking_points");
                
                ClanDTO clanDTO = new ClanDTO(id, tag, name, founderPlayerId, founderName, creationDate, friendlyFire, penaltyPoints, rankingPoints);
                clans.put(id, clanDTO);
            }
            
            core.getLogger().info("Carregados " + clans.size() + " clãs do banco de dados.");
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao carregar clãs: " + e.getMessage());
        }
        
        return clans;
    }

    @Override
    public Map<Integer, ClanPlayerDTO> loadAllClanPlayers(Map<Integer, ClanDTO> clans) {
        Map<Integer, ClanPlayerDTO> clanPlayers = new HashMap<>();
        
        // Query com LEFT JOIN para obter player_name da tabela player_data
        String sql = "SELECT cp.*, pd.name as player_name FROM clan_players cp " +
                    "LEFT JOIN player_data pd ON cp.player_id = pd.player_id";
        
        core.getLogger().info("🔧 [CLAN-LOAD-DEBUG] Carregando jogadores de clã...");
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                try {
                    int playerId = rs.getInt("player_id");
                    String playerName = rs.getString("player_name"); // Obtido via JOIN
                    int clanId = rs.getInt("clan_id");
                    String roleString = rs.getString("role"); // CORRIGIDO: ler como string
                    int kills = rs.getInt("kills");
                    int deaths = rs.getInt("deaths");
                    Date joinDate = rs.getTimestamp("join_date");
                    
                    // Converter string do role para int baseado no enum do banco
                    int roleId = convertRoleStringToId(roleString);
                    
                    core.getLogger().info("🔧 [CLAN-LOAD-DEBUG] Jogador carregado: " + playerName + 
                                        " (ID: " + playerId + ", Clan: " + clanId + 
                                        ", Role: " + roleString + " -> " + roleId + ")");
                    
                    ClanPlayerDTO clanPlayerDTO = new ClanPlayerDTO(playerId, playerName, clanId, roleId, joinDate, kills, deaths);
                    clanPlayers.put(playerId, clanPlayerDTO);
                    
                } catch (Exception e) {
                    core.getLogger().severe("🔧 [CLAN-LOAD-DEBUG] Erro ao processar linha de clan_players: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            core.getLogger().info("🔧 [CLAN-LOAD-DEBUG] Carregados " + clanPlayers.size() + " jogadores de clã do banco de dados.");
            
        } catch (SQLException e) {
            core.getLogger().severe("🔧 [CLAN-LOAD-DEBUG] Erro ao carregar jogadores de clã: " + e.getMessage());
            e.printStackTrace();
        }
        
        return clanPlayers;
    }
    
    /**
     * Converte string do role do banco para ID do enum Java.
     * Mapeamento: LEADER -> 2, CO_LEADER -> 2, OFFICER -> 1, MEMBER -> 1
     */
    private int convertRoleStringToId(String roleString) {
        if (roleString == null) {
            return 1; // MEMBER como default
        }
        
        switch (roleString.toUpperCase()) {
            case "LEADER":
            case "CO_LEADER":
                return 2; // LIDER
            case "OFFICER":
            case "MEMBER":
            default:
                return 1; // MEMBRO
        }
    }

    @Override
    public ClanDTO createClan(ClanDTO clanDTO) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Inserir o clã (sem founder_name - obtido via JOIN)
            String insertClanSql = "INSERT INTO clans (tag, name, founder_player_id, friendly_fire_enabled, penalty_points, ranking_points) " +
                                 "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertClanSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, clanDTO.getTag());
                stmt.setString(2, clanDTO.getName());
                stmt.setInt(3, clanDTO.getFounderPlayerId());
                stmt.setBoolean(4, clanDTO.isFriendlyFireEnabled());
                stmt.setInt(5, clanDTO.getPenaltyPoints());
                stmt.setInt(6, clanDTO.getRankingPoints());
                
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return null;
                }
                
                // Obter o ID gerado
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        clanDTO.setId(generatedKeys.getInt(1));
                    } else {
                        conn.rollback();
                        return null;
                    }
                }
            }
            
            // 2. Adicionar o fundador como membro do clã (sem player_name - obtido via JOIN)
            String insertFounderSql = "INSERT INTO clan_players (player_id, clan_id, role, join_date, kills, deaths) " +
                                    "VALUES (?, ?, ?, NOW(), 0, 0)";
            try (PreparedStatement stmt = conn.prepareStatement(insertFounderSql)) {
                stmt.setInt(1, clanDTO.getFounderPlayerId());
                stmt.setInt(2, clanDTO.getId());
                stmt.setString(3, "LEADER"); // CORRIGIDO: usar string do enum do banco
                
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return null;
                }
            }
            
            // 3. Registrar log da criação do clã
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, clanDTO.getId());
                stmt.setInt(2, clanDTO.getFounderPlayerId());
                stmt.setString(3, clanDTO.getFounderName());
                stmt.setInt(4, LogActionType.CLAN_CREATE.getId());
                stmt.setInt(5, clanDTO.getFounderPlayerId());
                stmt.setString(6, clanDTO.getFounderName());
                stmt.setString(7, "Clã criado: " + clanDTO.getTag() + " (" + clanDTO.getName() + ")");
                stmt.executeUpdate();
            }
            
            conn.commit(); // Confirmar todas as operações
            core.getLogger().info("Clã criado com sucesso: " + clanDTO.getTag() + " (" + clanDTO.getName() + ")");
            return clanDTO;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Desfazer tudo em caso de erro
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de criação: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de criação de clã: " + e.getMessage());
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    /**
     * Cria um clã de forma ASSÍNCRONA com integridade transacional.
     * 
     * @param clanDTO Dados do clã a ser criado
     * @param callback Callback para receber o resultado
     */
    public void createClanAsync(ClanDTO clanDTO, java.util.function.Consumer<ClanDTO> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            ClanDTO result = createClan(clanDTO);
            
            // Retornar para a thread principal
            core.getServer().getScheduler().runTask(core, () -> {
                callback.accept(result);
            });
        });
    }

    @Override
    public void deleteClan(ClanDTO clanDTO) {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clans WHERE id = ?")) {
            
            stmt.setInt(1, clanDTO.getId());
            
            int affectedRows = stmt.executeUpdate();
            core.getLogger().info("Clã deletado do banco: " + clanDTO.getTag() + " (linhas afetadas: " + affectedRows + ")");
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao deletar clã: " + e.getMessage());
        }
    }

    @Override
    public void saveOrUpdateClanPlayer(ClanPlayerDTO clanPlayerDTO) {
        // Verificar se o jogador existe na tabela player_data
        try (Connection conn = dataManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement("SELECT player_id FROM player_data WHERE player_id = ?")) {
            
            checkStmt.setInt(1, clanPlayerDTO.getPlayerId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    core.getLogger().severe("ERRO: Jogador NÃO encontrado em player_data: " + clanPlayerDTO.getPlayerId());
                    return; // Não tentar inserir se o jogador não existe
                }
            }
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao verificar jogador em player_data: " + e.getMessage());
            return;
        }
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO clan_players (player_id, clan_id, role, kills, deaths) " +
                 "VALUES (?, ?, ?, ?, ?) " +
                 "ON DUPLICATE KEY UPDATE " +
                 "clan_id = VALUES(clan_id), " +
                 "role = VALUES(role), " +
                 "kills = VALUES(kills), " +
                 "deaths = VALUES(deaths)")) {
            
            stmt.setInt(1, clanPlayerDTO.getPlayerId());
            stmt.setInt(2, clanPlayerDTO.getClanId());
            stmt.setInt(3, clanPlayerDTO.getRole());
            stmt.setInt(4, clanPlayerDTO.getKills());
            stmt.setInt(5, clanPlayerDTO.getDeaths());
            
            int affectedRows = stmt.executeUpdate();
            core.getLogger().info("Jogador de clã salvo no banco: " + clanPlayerDTO.getPlayerName());
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao salvar jogador de clã: " + e.getMessage());
        }
    }

    /**
     * Salva ou atualiza um jogador de clã de forma ASSÍNCRONA.
     * 
     * @param clanPlayerDTO Dados do jogador de clã
     * @param callback Callback para receber o resultado
     */
    public void saveOrUpdateClanPlayerAsync(ClanPlayerDTO clanPlayerDTO, java.util.function.Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                // Verificar se o jogador existe na tabela player_data
                try (Connection conn = dataManager.getConnection();
                     PreparedStatement checkStmt = conn.prepareStatement("SELECT player_id FROM player_data WHERE player_id = ?")) {
                    
                    checkStmt.setInt(1, clanPlayerDTO.getPlayerId());
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            core.getLogger().severe("ERRO: Jogador NÃO encontrado em player_data: " + clanPlayerDTO.getPlayerId());
                            // Retornar para a thread principal
                            core.getServer().getScheduler().runTask(core, () -> {
                                callback.accept(false);
                            });
                            return;
                        }
                    }
                }
                
                // Salvar ou atualizar o jogador de clã
                try (Connection conn = dataManager.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO clan_players (player_id, clan_id, role, kills, deaths) " +
                         "VALUES (?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE " +
                         "clan_id = VALUES(clan_id), " +
                         "role = VALUES(role), " +
                         "kills = VALUES(kills), " +
                         "deaths = VALUES(deaths)")) {
                    
                    stmt.setInt(1, clanPlayerDTO.getPlayerId());
                    stmt.setInt(2, clanPlayerDTO.getClanId());
                    stmt.setInt(3, clanPlayerDTO.getRole());
                    stmt.setInt(4, clanPlayerDTO.getKills());
                    stmt.setInt(5, clanPlayerDTO.getDeaths());
                    
                    int affectedRows = stmt.executeUpdate();
                    core.getLogger().info("Jogador de clã salvo no banco: " + clanPlayerDTO.getPlayerName());
                    
                    // Retornar para a thread principal
                    core.getServer().getScheduler().runTask(core, () -> {
                        callback.accept(true);
                    });
                    
                } catch (SQLException e) {
                    core.getLogger().severe("Erro ao salvar jogador de clã: " + e.getMessage());
                    // Retornar para a thread principal
                    core.getServer().getScheduler().runTask(core, () -> {
                        callback.accept(false);
                    });
                }
                
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao verificar jogador em player_data: " + e.getMessage());
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(false);
                });
            }
        });
    }

    @Override
    public void updateClanSettings(ClanDTO clanDTO) {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE clans SET friendly_fire_enabled = ?, penalty_points = ?, ranking_points = ? WHERE id = ?")) {
            
            stmt.setBoolean(1, clanDTO.isFriendlyFireEnabled());
            stmt.setInt(2, clanDTO.getPenaltyPoints());
            stmt.setInt(3, clanDTO.getRankingPoints());
            stmt.setInt(4, clanDTO.getId());
            
            stmt.executeUpdate();
            core.getLogger().info("Configurações do clã atualizadas: " + clanDTO.getTag());
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao atualizar configurações do clã: " + e.getMessage());
        }
    }

    @Override
    public List<ClanRelationDTO> loadAllClanRelations() {
        List<ClanRelationDTO> relations = new ArrayList<>();
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM clan_alliances");
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                int clanId1 = rs.getInt("clan_id_1");
                int clanId2 = rs.getInt("clan_id_2");
                int status = rs.getInt("status");
                Date creationDate = rs.getTimestamp("creation_date");
                
                ClanRelationDTO relationDTO = new ClanRelationDTO(clanId1, clanId2, status, creationDate);
                relations.add(relationDTO);
            }
            
            core.getLogger().info("Carregadas " + relations.size() + " relações de clã do banco de dados.");
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao carregar relações de clã: " + e.getMessage());
        }
        
        return relations;
    }

    @Override
    public void saveClanRelation(ClanRelationDTO relationDTO) {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO clan_alliances (clan1_id, clan2_id, status) VALUES (?, ?, ?)")) {
            
            stmt.setInt(1, relationDTO.getClanId1());
            stmt.setInt(2, relationDTO.getClanId2());
            stmt.setInt(3, relationDTO.getStatus());
            
            stmt.executeUpdate();
            core.getLogger().info("Relação de clã salva no banco: " + relationDTO.getClanId1() + " <-> " + relationDTO.getClanId2());
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao salvar relação de clã: " + e.getMessage());
        }
    }

    @Override
    public void deleteClanRelation(ClanRelationDTO relationDTO) {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM clan_alliances WHERE clan1_id = ? AND clan2_id = ?")) {
            
            stmt.setInt(1, relationDTO.getClanId1());
            stmt.setInt(2, relationDTO.getClanId2());
            
            stmt.executeUpdate();
            core.getLogger().info("Relação de clã removida do banco: " + relationDTO.getClanId1() + " <-> " + relationDTO.getClanId2());
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao deletar relação de clã: " + e.getMessage());
        }
    }

    /**
     * Salva uma relação de clã de forma ASSÍNCRONA.
     * 
     * @param relationDTO Dados da relação de clã
     * @param callback Callback para receber o resultado
     */
    public void saveClanRelationAsync(ClanRelationDTO relationDTO, java.util.function.Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                saveClanRelation(relationDTO);
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(true);
                });
                
            } catch (Exception e) {
                core.getLogger().severe("Erro ao salvar relação de clã de forma assíncrona: " + e.getMessage());
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(false);
                });
            }
        });
    }

    /**
     * Remove uma relação de clã de forma ASSÍNCRONA.
     * 
     * @param relationDTO Dados da relação de clã
     * @param callback Callback para receber o resultado
     */
    public void deleteClanRelationAsync(ClanRelationDTO relationDTO, java.util.function.Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                deleteClanRelation(relationDTO);
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(true);
                });
                
            } catch (Exception e) {
                core.getLogger().severe("Erro ao deletar relação de clã de forma assíncrona: " + e.getMessage());
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(false);
                });
            }
        });
    }

    @Override
    public boolean setFounder(ClanDTO clanDTO, int newFounderPlayerId, String newFounderName, int oldFounderPlayerId) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Atualizar o fundador do clã
            String updateClanSql = "UPDATE clans SET founder_player_id = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateClanSql)) {
                stmt.setInt(1, newFounderPlayerId);
                stmt.setInt(2, clanDTO.getId());
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            // 2. Atualizar o role do novo fundador para FUNDADOR (3)
            String updateNewFounderSql = "UPDATE clan_players SET role = ? WHERE player_id = ? AND clan_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateNewFounderSql)) {
                stmt.setInt(1, ClanRole.FUNDADOR.getId());
                stmt.setInt(2, newFounderPlayerId);
                stmt.setInt(3, clanDTO.getId());
                stmt.executeUpdate();
            }
            
            // 3. Atualizar o role do fundador antigo para MEMBRO (0)
            String updateOldFounderSql = "UPDATE clan_players SET role = ? WHERE player_id = ? AND clan_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateOldFounderSql)) {
                stmt.setInt(1, ClanRole.MEMBRO.getId());
                stmt.setInt(2, oldFounderPlayerId);
                stmt.setInt(3, clanDTO.getId());
                stmt.executeUpdate();
            }
            
            // 4. Registrar log da mudança de fundador
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, clanDTO.getId());
                stmt.setInt(2, newFounderPlayerId);
                stmt.setString(3, "Novo Fundador"); // Nome será preenchido pelo ClanManager
                stmt.setInt(4, LogActionType.FOUNDER_CHANGE.getId());
                stmt.setInt(5, oldFounderPlayerId);
                stmt.setString(6, "Fundador Anterior"); // Nome será preenchido pelo ClanManager
                stmt.setString(7, "Mudança de fundador: " + clanDTO.getTag());
                stmt.executeUpdate();
            }
            
            conn.commit(); // Confirmar todas as operações
            core.getLogger().info("Fundador do clã alterado: " + clanDTO.getTag());
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Desfazer tudo em caso de erro
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de mudança de fundador: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de mudança de fundador: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    /**
     * Altera o fundador de um clã de forma ASSÍNCRONA com integridade transacional.
     * 
     * @param clanDTO Dados do clã
     * @param newFounderPlayerId ID do novo fundador
     * @param newFounderName Nome do novo fundador
     * @param oldFounderPlayerId ID do fundador antigo
     * @param callback Callback para receber o resultado
     */
    public void setFounderAsync(ClanDTO clanDTO, int newFounderPlayerId, String newFounderName, int oldFounderPlayerId, java.util.function.Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            boolean result = setFounder(clanDTO, newFounderPlayerId, newFounderName, oldFounderPlayerId);
            
            // Retornar para a thread principal
            core.getServer().getScheduler().runTask(core, () -> {
                callback.accept(result);
            });
        });
    }
    
    @Override
    public void logAction(int clanId, int actorPlayerId, String actorName, LogActionType actionType, int targetPlayerId, String targetName, String details) {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())")) {
            
            stmt.setInt(1, clanId);
            stmt.setInt(2, actorPlayerId);
            stmt.setString(3, actorName);
            stmt.setInt(4, actionType.getId());
            stmt.setInt(5, targetPlayerId);
            stmt.setString(6, targetName);
            stmt.setString(7, details);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao registrar log de ação do clã: " + e.getMessage());
        }
    }

    /**
     * Registra uma ação de clã de forma ASSÍNCRONA.
     * 
     * @param clanId ID do clã
     * @param actorPlayerId ID do jogador que executou a ação
     * @param actorName Nome do jogador que executou a ação
     * @param actionType Tipo da ação
     * @param targetPlayerId ID do jogador alvo da ação
     * @param targetName Nome do jogador alvo da ação
     * @param details Detalhes da ação
     * @param callback Callback para receber o resultado
     */
    public void logActionAsync(int clanId, int actorPlayerId, String actorName, LogActionType actionType, int targetPlayerId, String targetName, String details, java.util.function.Consumer<Boolean> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            try {
                logAction(clanId, actorPlayerId, actorName, actionType, targetPlayerId, targetName, details);
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(true);
                });
                
            } catch (Exception e) {
                core.getLogger().severe("Erro ao logar ação de clã de forma assíncrona: " + e.getMessage());
                
                // Retornar para a thread principal
                core.getServer().getScheduler().runTask(core, () -> {
                    callback.accept(false);
                });
            }
        });
    }
    
    public List<ClanLogDTO> getClanLogs(int clanId, int limit) {
        List<ClanLogDTO> logs = new ArrayList<>();
        
        String sql = "SELECT * FROM clan_logs WHERE clan_id = ? ORDER BY timestamp DESC LIMIT ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, clanId);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int actorPlayerId = rs.getInt("actor_player_id");
                    String actorName = rs.getString("actor_name");
                    int actionTypeId = rs.getInt("action_type");
                    int targetPlayerId = rs.getInt("target_player_id");
                    String targetName = rs.getString("target_name");
                    String details = rs.getString("details");
                    Date timestamp = rs.getTimestamp("timestamp");
                    
                    LogActionType actionType = LogActionType.fromId(actionTypeId);
                    ClanLogDTO logDTO = new ClanLogDTO(clanId, actorPlayerId, actorName, actionType, targetPlayerId, targetName, details);
                    logDTO.setId(id);
                    logDTO.setTimestamp(timestamp.getTime());
                    logs.add(logDTO);
                }
            }
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao carregar logs do clã: " + e.getMessage());
        }
        
        return logs;
    }
    
    @Override
    public List<ClanLogDTO> getPlayerLogs(int clanId, int playerId, int page, int pageSize) {
        List<ClanLogDTO> logs = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        
        String sql = "SELECT * FROM clan_logs WHERE clan_id = ? AND (actor_player_id = ? OR target_player_id = ?) " +
                     "ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, clanId);
            stmt.setInt(2, playerId);
            stmt.setInt(3, playerId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClanLogDTO log = new ClanLogDTO();
                    log.setId(rs.getInt("id"));
                    log.setClanId(rs.getInt("clan_id"));
                    log.setActorPlayerId(rs.getInt("actor_player_id"));
                    log.setActorName(rs.getString("actor_name"));
                    log.setActionType(LogActionType.fromId(rs.getInt("action_type")));
                    log.setTargetPlayerId(rs.getInt("target_player_id"));
                    log.setTargetName(rs.getString("target_name"));
                    log.setDetails(rs.getString("details"));
                    log.setTimestamp(rs.getTimestamp("timestamp").getTime());
                    
                    logs.add(log);
                }
            }
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao carregar logs do jogador: " + e.getMessage());
        }
        
        return logs;
    }
    
    @Override
    public boolean addPenaltyPointsAndLog(int clanId, int currentPoints, int pointsToAdd, 
                                        int authorPlayerId, String authorName, 
                                        int targetPlayerId, String targetName, String details) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Atualizar pontos de penalidade
            String updateSql = "UPDATE clans SET penalty_points = ? WHERE id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, currentPoints + pointsToAdd);
            updateStmt.setInt(2, clanId);
            int updateRows = updateStmt.executeUpdate();
            
            if (updateRows == 0) {
                conn.rollback();
                return false;
            }
            
            // 2. Inserir log da ação
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            PreparedStatement logStmt = conn.prepareStatement(logSql);
            logStmt.setInt(1, clanId);
            logStmt.setInt(2, authorPlayerId);
            logStmt.setString(3, authorName);
            logStmt.setInt(4, LogActionType.PENALTY_POINTS_ADDED.getId());
            logStmt.setInt(5, targetPlayerId);
            logStmt.setString(6, targetName);
            logStmt.setString(7, details);
            int logRows = logStmt.executeUpdate();
            
            if (logRows == 0) {
                conn.rollback();
                return false;
            }
            
            // Commit da transação
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de pontos de penalidade: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public boolean revertSanctionAndLog(int clanId, int currentPoints, int pointsToRevert, 
                                       int newSanctionTier, int authorPlayerId, String authorName,
                                       int targetPlayerId, String targetName, String details) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Atualizar pontos de penalidade e tier de sanção
            String updateSql = "UPDATE clans SET penalty_points = ?, active_sanction_tier = ? WHERE id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setInt(1, currentPoints - pointsToRevert);
            updateStmt.setInt(2, newSanctionTier);
            updateStmt.setInt(3, clanId);
            int updateRows = updateStmt.executeUpdate();
            
            if (updateRows == 0) {
                conn.rollback();
                return false;
            }
            
            // 2. Se o novo tier for 0, limpar a data de expiração
            if (newSanctionTier == 0) {
                String clearExpirySql = "UPDATE clans SET sanction_expires_at = NULL WHERE id = ?";
                PreparedStatement clearStmt = conn.prepareStatement(clearExpirySql);
                clearStmt.setInt(1, clanId);
                clearStmt.executeUpdate();
            }
            
            // 3. Inserir log da reversão
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            PreparedStatement logStmt = conn.prepareStatement(logSql);
            logStmt.setInt(1, clanId);
            logStmt.setInt(2, authorPlayerId);
            logStmt.setString(3, authorName);
            logStmt.setInt(4, LogActionType.SANCTION_REVERSED.getId());
            logStmt.setInt(5, targetPlayerId);
            logStmt.setString(6, targetName);
            logStmt.setString(7, details);
            int logRows = logStmt.executeUpdate();
            
            if (logRows == 0) {
                conn.rollback();
                return false;
            }
            
            // Commit da transação
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de reversão: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de reversão de sanção: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public boolean updateKDRAndLog(int killerPlayerId, String killerName, int killerKills, int killerDeaths,
                                   int victimPlayerId, String victimName, int victimKills, int victimDeaths,
                                   int clanId) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Atualizar KDR do killer
            String updateKillerSql = "UPDATE clan_players SET kills = ?, deaths = ? WHERE player_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateKillerSql)) {
                stmt.setInt(1, killerKills);
                stmt.setInt(2, killerDeaths);
                stmt.setInt(3, killerPlayerId);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            // 2. Atualizar KDR da vítima
            String updateVictimSql = "UPDATE clan_players SET kills = ?, deaths = ? WHERE player_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateVictimSql)) {
                stmt.setInt(1, victimKills);
                stmt.setInt(2, victimDeaths);
                stmt.setInt(3, victimPlayerId);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            // 3. Registrar log da morte
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, clanId);
                stmt.setInt(2, killerPlayerId);
                stmt.setString(3, killerName);
                stmt.setInt(4, LogActionType.KDR_UPDATE.getId());
                stmt.setInt(5, victimPlayerId);
                stmt.setString(6, victimName);
                stmt.setString(7, "KDR atualizado: " + killerName + " matou " + victimName);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            conn.commit(); // Confirmar todas as operações
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Desfazer tudo em caso de erro
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de KDR: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de atualização de KDR: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<InactiveMemberInfo> findInactiveMembers(int inactiveDays, int limit) {
        List<InactiveMemberInfo> inactiveMembers = new ArrayList<>();
        
        // Query com JOIN entre clan_players e player_data para identificar inativos
        String sql = "SELECT cp.player_id, pd.name as player_name, cp.clan_id, c.name as clan_name, c.tag as clan_tag, " +
                    "cp.role, pd.last_seen, DATEDIFF(NOW(), pd.last_seen) as days_inactive " +
                    "FROM clan_players cp " +
                    "INNER JOIN clans c ON cp.clan_id = c.id " +
                    "INNER JOIN player_data pd ON cp.player_id = pd.player_id " +
                    "WHERE pd.last_seen < DATE_SUB(NOW(), INTERVAL ? DAY) " +
                    "AND cp.role != 3 " + // Não incluir fundadores (role 3)
                    "ORDER BY pd.last_seen ASC " +
                    "LIMIT ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, inactiveDays);
            stmt.setInt(2, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    String playerName = rs.getString("player_name");
                    int clanId = rs.getInt("clan_id");
                    String clanName = rs.getString("clan_name");
                    String clanTag = rs.getString("clan_tag");
                    int role = rs.getInt("role");
                    Timestamp lastSeen = rs.getTimestamp("last_seen");
                    int daysInactive = rs.getInt("days_inactive");
                    
                    // Converter Timestamp para java.util.Date (compatível com Java 7)
                    Date lastSeenDate = new Date(lastSeen.getTime());
                    
                    InactiveMemberInfo info = new InactiveMemberInfo(
                        String.valueOf(playerId), playerName, clanId, clanName, clanTag, 
                        role, lastSeenDate, daysInactive
                    );
                    inactiveMembers.add(info);
                }
            }
            
            core.getLogger().info("Encontrados " + inactiveMembers.size() + " membros inativos para limpeza.");
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao buscar membros inativos: " + e.getMessage());
        }
        
        return inactiveMembers;
    }
    
    @Override
    public boolean removeInactiveMember(int playerId, int clanId, String reason) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Remover o jogador do clã
            String deleteSql = "DELETE FROM clan_players WHERE player_id = ? AND clan_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setInt(1, playerId);
                stmt.setInt(2, clanId);
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }
            }
            
            // 2. Registrar log da remoção
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, target_player_id, target_name, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, clanId);
                stmt.setInt(2, 0); // Sistema como autor
                stmt.setString(3, "Sistema");
                stmt.setInt(4, LogActionType.PLAYER_KICK.getId());
                stmt.setInt(5, playerId);
                stmt.setString(6, "Membro Inativo");
                stmt.setString(7, reason);
                stmt.executeUpdate();
            }
            
            conn.commit(); // Confirmar todas as operações
            core.getLogger().info("Membro inativo removido do clã: player_id=" + playerId + ", clan_id=" + clanId);
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Desfazer tudo em caso de erro
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de remoção: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de remoção de membro inativo: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public List<ClanMemberInfo> getClanMembersInfo(int clanId) {
        List<ClanMemberInfo> members = new ArrayList<>();
        
        // Query otimizada para buscar todos os dados necessários de uma só vez com LEFT JOIN
        String sql = "SELECT cp.player_id, pd.name as player_name, cp.role, cp.kills, cp.deaths, cp.join_date, pd.last_seen " +
                     "FROM clan_players cp " +
                     "LEFT JOIN player_data pd ON cp.player_id = pd.player_id " +
                     "WHERE cp.clan_id = ? " +
                     "ORDER BY cp.role DESC, pd.name ASC";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, clanId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int playerId = rs.getInt("player_id");
                    int roleId = rs.getInt("role");
                    int kills = rs.getInt("kills");
                    int deaths = rs.getInt("deaths");
                    long joinDate = rs.getLong("join_date");
                    
                    // CORREÇÃO CRÍTICA: last_seen é TIMESTAMP, não BIGINT
                    long lastSeen = 0;
                    Timestamp lastSeenTimestamp = rs.getTimestamp("last_seen");
                    if (lastSeenTimestamp != null) {
                        lastSeen = lastSeenTimestamp.getTime();
                    }
                    
                    // Converter roleId para enum
                    ClanRole role = ClanRole.fromId(roleId);
                    
                    // O DAO não deve verificar status online - isso é responsabilidade da camada de negócios
                    boolean isOnline = false; // Valor padrão, será sobrescrito pelo ClanManager
                    
                    ClanMemberInfo memberInfo = new ClanMemberInfo(
                        playerName, String.valueOf(playerId), role, kills, deaths, joinDate, lastSeen, isOnline
                    );
                    
                    members.add(memberInfo);
                }
            }
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao buscar informações dos membros do clã: " + e.getMessage());
            e.printStackTrace();
        }
        
        return members;
    }
    
    @Override
    public List<ClanRankingInfoDTO> getClanRankings(String criteria, int limit, int offset) {
        List<ClanRankingInfoDTO> rankings = new ArrayList<>();
        
        // Query com LEFT JOIN para obter founder_name
        String sql = "SELECT c.*, pd.name as founder_name FROM clans c " +
                    "LEFT JOIN player_data pd ON c.founder_player_id = pd.player_id " +
                    "ORDER BY " + criteria + " DESC " +
                    "LIMIT ? OFFSET ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String tag = rs.getString("tag");
                    String name = rs.getString("name");
                    int founderPlayerId = rs.getInt("founder_player_id");
                    String founderName = rs.getString("founder_name"); // Obtido via JOIN
                    int rankingPoints = rs.getInt("ranking_points");
                    int penaltyPoints = rs.getInt("penalty_points");
                    Date creationDate = rs.getTimestamp("creation_date");
                    
                    ClanRankingInfoDTO rankingInfo = new ClanRankingInfoDTO();
                    rankingInfo.setRank(0); // Será calculado pelo ClanManager
                    rankingInfo.setTag(tag);
                    rankingInfo.setName(name);
                    rankingInfo.setRankingPoints(rankingPoints);
                    rankingInfo.setFounderName(founderName);
                    rankingInfo.setMemberCount(0); // Será calculado pelo ClanManager
                    rankingInfo.setTotalKills(0); // Será calculado pelo ClanManager
                    rankingInfo.setTotalDeaths(0); // Será calculado pelo ClanManager
                    rankingInfo.setClanKdr(0.0); // Será calculado pelo ClanManager
                    rankingInfo.setActiveSanctionTier(0); // Será calculado pelo ClanManager
                    rankingInfo.setSanctionExpiresAt(null);
                    rankingInfo.setTotalWins(0);
                    rankingInfo.setLastWinDate(null);
                    rankings.add(rankingInfo);
                }
            }
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao buscar ranking de clãs: " + e.getMessage());
        }
        
        return rankings;
    }
    
    @Override
    public Map<Integer, Map<String, Integer>> getEventWinsForClans(List<Integer> clanIds) {
        // Implementação para estatísticas de eventos (se necessário)
        // Por enquanto, retorna mapa vazio
        return new HashMap<>();
    }
    
    @Override
    public boolean updateRankingPointsAndLog(int clanId, int pointsChange, String reason) {
        Connection conn = null;
        try {
            conn = dataManager.getConnection();
            conn.setAutoCommit(false);
            
            // 1. Atualizar pontos de ranking
            String updateSql = "UPDATE clans SET ranking_points = ranking_points + ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, pointsChange);
                stmt.setInt(2, clanId);
                if (stmt.executeUpdate() == 0) {
                    conn.rollback();
                    return false; // Clã não encontrado
                }
            }
            
            // 2. Registrar no log
            String logSql = "INSERT INTO clan_logs (clan_id, actor_player_id, actor_name, action_type, details, timestamp) " +
                           "VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement stmt = conn.prepareStatement(logSql)) {
                stmt.setInt(1, clanId);
                stmt.setInt(2, 0); // Sistema como autor
                stmt.setString(3, "Sistema de Ranking");
                stmt.setInt(4, pointsChange > 0 ? LogActionType.RANKING_POINTS_ADDED.getId() : LogActionType.RANKING_POINTS_REMOVED.getId());
                stmt.setString(5, "Ranking: " + (pointsChange > 0 ? "+" : "") + pointsChange + " pontos - " + reason);
                stmt.executeUpdate();
            }
            
            conn.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                core.getLogger().severe("Erro ao fazer rollback da transação de ranking: " + rollbackEx.getMessage());
            }
            core.getLogger().severe("Erro na transação de atualização de ranking: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                core.getLogger().severe("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void registerEventWin(int clanId, String eventName) {
        String sql = "INSERT INTO clan_event_wins (clan_id, event_name, win_date) VALUES (?, ?, NOW())";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, clanId);
            stmt.setString(2, eventName);
            stmt.executeUpdate();
            
            // Registrar no log
            logAction(clanId, 0, "Sistema de Eventos", LogActionType.EVENT_WIN_REGISTERED, 
                     0, null, "Vitória em evento: " + eventName);
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao registrar vitória em evento: " + e.getMessage());
        }
    }
    
    @Override
    public List<ClanLogDTO> getClanLogs(int clanId, int page, int pageSize) {
        List<ClanLogDTO> logs = new ArrayList<>();
        int offset = (page - 1) * pageSize;
        
        String sql = "SELECT * FROM clan_logs WHERE clan_id = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, clanId);
            stmt.setInt(2, pageSize);
            stmt.setInt(3, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ClanLogDTO log = new ClanLogDTO(
                        rs.getInt("clan_id"),
                        rs.getInt("actor_player_id"),
                        rs.getString("actor_name"),
                        LogActionType.fromId(rs.getInt("action_type")),
                        rs.getInt("target_player_id"),
                        rs.getString("target_name"),
                        rs.getString("details")
                    );
                    log.setId(rs.getInt("id"));
                    log.setTimestamp(rs.getTimestamp("timestamp").getTime());
                    logs.add(log);
                }
            }
            
        } catch (SQLException e) {
            core.getLogger().severe("Erro ao buscar logs do clã: " + e.getMessage());
        }
        
        return logs;
    }
    
}
