package br.com.primeleague.api.dao;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.api.dto.ClanPlayerDTO;
import br.com.primeleague.api.dto.ClanRelationDTO;
import br.com.primeleague.api.dto.ClanLogDTO;
import br.com.primeleague.api.dto.InactiveMemberInfo;
import br.com.primeleague.api.dto.ClanMemberInfo;
import br.com.primeleague.api.dto.ClanRankingInfoDTO;
import br.com.primeleague.api.enums.LogActionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface para acesso a dados de clãs.
 * Define o contrato para operações de persistência de clãs.
 * 
 * REFATORADO para usar player_id (int) em vez de UUID (String) seguindo o Padrão de Identidade Prime League.
 * 
 * @version 2.0
 * @author PrimeLeague Team
 */
public interface ClanDAO {
    
    /**
     * Carrega todos os clãs do banco de dados.
     * 
     * @return Mapa com ID do clã como chave e ClanDTO como valor
     */
    Map<Integer, ClanDTO> loadAllClans();
    
    /**
     * Carrega todos os jogadores de clã do banco de dados.
     * 
     * @param clans Mapa de clãs carregados (para referência)
     * @return Mapa com player_id do jogador como chave e ClanPlayerDTO como valor
     */
    Map<Integer, ClanPlayerDTO> loadAllClanPlayers(Map<Integer, ClanDTO> clans);
    
    /**
     * Cria um novo clã no banco de dados.
     * 
     * @param clanDTO Dados do clã a ser criado
     * @return ClanDTO com o ID gerado pelo banco
     */
    ClanDTO createClan(ClanDTO clanDTO);
    
    /**
     * Deleta um clã do banco de dados.
     * 
     * @param clanDTO Clã a ser deletado
     */
    void deleteClan(ClanDTO clanDTO);
    
    /**
     * Salva ou atualiza um jogador de clã no banco de dados.
     * 
     * @param clanPlayerDTO Dados do jogador a ser salvo
     */
    void saveOrUpdateClanPlayer(ClanPlayerDTO clanPlayerDTO);
    
    /**
     * Atualiza as configurações de um clã no banco de dados.
     * 
     * @param clanDTO Dados do clã com configurações atualizadas
     */
    void updateClanSettings(ClanDTO clanDTO);
    
    /**
     * Carrega todas as relações entre clãs do banco de dados.
     * 
     * @return Lista de relações entre clãs
     */
    List<ClanRelationDTO> loadAllClanRelations();
    
    /**
     * Salva uma nova relação entre clãs no banco de dados.
     * 
     * @param relationDTO Relação a ser salva
     */
    void saveClanRelation(ClanRelationDTO relationDTO);
    
    /**
     * Remove uma relação entre clãs do banco de dados.
     * 
     * @param relationDTO Relação a ser removida
     */
    void deleteClanRelation(ClanRelationDTO relationDTO);
    
    /**
     * Transfere o cargo de fundador de um clã.
     * Executa uma transação atômica para atualizar tanto a tabela clans quanto clan_players.
     * 
     * @param clanDTO Clã onde a transferência ocorrerá
     * @param newFounderPlayerId player_id do novo fundador
     * @param newFounderName Nome do novo fundador
     * @param oldFounderPlayerId player_id do fundador atual
     * @return true se a transação foi bem-sucedida
     */
    boolean setFounder(ClanDTO clanDTO, int newFounderPlayerId, String newFounderName, int oldFounderPlayerId);
    
    /**
     * Registra uma ação no log de auditoria do clã.
     * 
     * @param clanId ID do clã
     * @param actorPlayerId player_id de quem executou a ação
     * @param actorName Nome de quem executou a ação
     * @param action Tipo da ação executada
     * @param targetPlayerId player_id do alvo da ação (pode ser 0 se não aplicável)
     * @param targetName Nome do alvo da ação (pode ser null)
     * @param details Detalhes adicionais da ação (pode ser null)
     */
    void logAction(int clanId, int actorPlayerId, String actorName, LogActionType action, 
                   int targetPlayerId, String targetName, String details);
    
    /**
     * Carrega os logs de um clã com paginação.
     * 
     * @param clanId ID do clã
     * @param page Número da página (começa em 1)
     * @param pageSize Tamanho da página
     * @return Lista de logs do clã
     */
    List<ClanLogDTO> getClanLogs(int clanId, int page, int pageSize);
    
    /**
     * Carrega os logs de um jogador específico em um clã.
     * 
     * @param clanId ID do clã
     * @param playerId player_id do jogador
     * @param page Número da página (começa em 1)
     * @param pageSize Tamanho da página
     * @return Lista de logs do jogador
     */
    List<ClanLogDTO> getPlayerLogs(int clanId, int playerId, int page, int pageSize);
    
    /**
     * Adiciona pontos de penalidade e registra o log em uma transação atômica.
     *
     * @param clanId ID do clã
     * @param currentPoints Pontos atuais do clã
     * @param pointsToAdd Pontos a adicionar
     * @param authorPlayerId player_id do autor da ação
     * @param authorName Nome do autor da ação
     * @param targetPlayerId player_id do jogador punido
     * @param targetName Nome do jogador punido
     * @param details Detalhes da ação
     * @return true se a operação foi bem-sucedida
     */
    boolean addPenaltyPointsAndLog(int clanId, int currentPoints, int pointsToAdd, 
                                  int authorPlayerId, String authorName, 
                                  int targetPlayerId, String targetName, String details);
    
    /**
     * Reverte uma sanção de clã e registra o log em uma transação atômica.
     * Subtrai pontos de penalidade, recalcula o tier de sanção ativa e registra a reversão.
     *
     * @param clanId ID do clã
     * @param currentPoints Pontos atuais do clã
     * @param pointsToRevert Pontos a reverter (serão subtraídos)
     * @param newSanctionTier Novo tier de sanção após a reversão
     * @param authorPlayerId player_id do administrador que reverteu
     * @param authorName Nome do administrador que reverteu
     * @param targetPlayerId player_id do jogador que teve a punição revertida
     * @param targetName Nome do jogador que teve a punição revertida
     * @param details Detalhes da reversão
     * @return true se a operação foi bem-sucedida
     */
    boolean revertSanctionAndLog(int clanId, int currentPoints, int pointsToRevert, 
                                int newSanctionTier, int authorPlayerId, String authorName,
                                int targetPlayerId, String targetName, String details);
    
    /**
     * Atualiza estatísticas de KDR de dois jogadores e registra o log em uma transação atômica.
     * Garante que tanto as estatísticas quanto o log sejam salvos juntos, evitando inconsistências.
     *
     * @param killerPlayerId player_id do jogador que matou
     * @param killerName Nome do jogador que matou
     * @param killerKills Novas kills do killer
     * @param killerDeaths Novas deaths do killer
     * @param victimPlayerId player_id do jogador que morreu
     * @param victimName Nome do jogador que morreu
     * @param victimKills Novas kills da vítima
     * @param victimDeaths Novas deaths da vítima
     * @param clanId ID do clã (para o log)
     * @return true se a operação foi bem-sucedida
     */
    boolean updateKDRAndLog(int killerPlayerId, String killerName, int killerKills, int killerDeaths,
                           int victimPlayerId, String victimName, int victimKills, int victimDeaths,
                           int clanId);
    
    /**
     * Busca membros inativos de todos os clãs.
     * 
     * @param inactiveDays Número de dias de inatividade
     * @param limit Número máximo de resultados (para processamento em lotes)
     * @return Lista de membros inativos com informações do clã
     */
    List<InactiveMemberInfo> findInactiveMembers(int inactiveDays, int limit);
    
    /**
     * Remove um membro inativo de um clã de forma transacional.
     * Atualiza tanto clan_players quanto player_data e registra o log.
     * 
     * @param playerId player_id do jogador a ser removido
     * @param clanId ID do clã
     * @param reason Motivo da remoção (para o log)
     * @return true se a operação foi bem-sucedida
     */
    boolean removeInactiveMember(int playerId, int clanId, String reason);
    
    /**
     * Busca uma lista enriquecida de informações de todos os membros de um clã.
     * @param clanId O ID do clã.
     * @return Uma lista de DTOs ClanMemberInfo.
     */
    List<ClanMemberInfo> getClanMembersInfo(int clanId);
    
    /**
     * Busca uma lista paginada e enriquecida de clãs para o ranking.
     * @param criteria Coluna pela qual ordenar (ex: "ranking_points").
     * @param limit Número de resultados por página.
     * @param offset Ponto de partida para a paginação.
     * @return Uma lista de DTOs ClanRankingInfoDTO.
     */
    List<ClanRankingInfoDTO> getClanRankings(String criteria, int limit, int offset);

    /**
     * Busca um resumo das vitórias em eventos para uma lista de clãs.
     * @return Mapa<ID do Clã, Mapa<NomeDoEvento, ContagemDeVitórias>>.
     */
    Map<Integer, Map<String, Integer>> getEventWinsForClans(List<Integer> clanIds);

    /**
     * Adiciona ou remove pontos de ranking de um clã e registra a ação no log.
     * DEVE ser uma operação transacional.
     * @return true se a operação foi bem-sucedida.
     */
    boolean updateRankingPointsAndLog(int clanId, int pointsChange, String reason);

    /**
     * Registra uma vitória em evento para um clã.
     */
    void registerEventWin(int clanId, String eventName);
}
