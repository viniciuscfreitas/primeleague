package br.com.primeleague.clans.manager;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.api.dto.ClanPlayerDTO;
import br.com.primeleague.api.dto.ClanRelationDTO;
import br.com.primeleague.api.dto.ClanLogDTO;
import br.com.primeleague.api.dto.InactiveMemberInfo;
import br.com.primeleague.api.dto.ClanMemberInfo;
import br.com.primeleague.api.dto.ClanRankingInfoDTO;
import br.com.primeleague.api.enums.LogActionType;
import br.com.primeleague.api.enums.PunishmentSeverity;
import br.com.primeleague.clans.PrimeLeagueClans;
import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.clans.model.ClanInvitation;
import br.com.primeleague.clans.model.ClanPlayer;
import br.com.primeleague.clans.model.ClanRelation;
import br.com.primeleague.core.api.PrimeLeagueAPI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.Arrays;

/**
 * Gerenciador principal do sistema de clãs.
 * Responsável por todas as operações de negócio relacionadas aos clãs.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanManager {

    private final PrimeLeagueClans plugin;
    private final ClanDAO clanDAO;
    
    // Cache em memória para performance
    private final Map<Integer, Clan> clans;
    private final Map<Integer, ClanPlayer> clanPlayers; // REFATORADO: player_id como chave
    private final Map<Integer, ClanInvitation> pendingInvites; // REFATORADO: player_id como chave
    private final Map<String, ClanRelation> clanRelations;
    
    // REFATORADO: Set para rastrear membros online de forma proativa
    private final Map<Integer, Player> onlinePlayers = new ConcurrentHashMap<>(); // REFATORADO: player_id como chave

    public ClanManager(PrimeLeagueClans plugin, ClanDAO clanDAO) {
        this.plugin = plugin;
        this.clanDAO = clanDAO;
        this.clans = new ConcurrentHashMap<>();
        this.clanPlayers = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.clanRelations = new ConcurrentHashMap<>();
    }

    /**
     * Carrega todos os dados do banco de dados.
     * REFATORADO: Abordagem de duas passagens para garantir integridade dos dados.
     */
    public void load() {
        plugin.getLogger().info("Carregando dados de clãs do banco de dados...");
        
        // ========================================
        // PRIMEIRA PASSAGEM: Carregar todos os DTOs
        // ========================================
        plugin.getLogger().info("Passagem 1: Carregando DTOs do banco de dados...");
        
        // Carregar todos os DTOs primeiro
        Map<Integer, ClanDTO> clanDTOs = clanDAO.loadAllClans();
        Map<Integer, ClanPlayerDTO> playerDTOs = clanDAO.loadAllClanPlayers(clanDTOs);
        List<ClanRelationDTO> relationDTOs = clanDAO.loadAllClanRelations();
        
        plugin.getLogger().info("DTOs carregados: " + clanDTOs.size() + " clãs, " + playerDTOs.size() + " jogadores, " + relationDTOs.size() + " relações");
        
        // ========================================
        // SEGUNDA PASSAGEM: Criar objetos do modelo
        // ========================================
        plugin.getLogger().info("Passagem 2: Criando objetos do modelo...");
        
        // Primeiro, criar todos os clãs (sem dependências)
        for (Map.Entry<Integer, ClanDTO> entry : clanDTOs.entrySet()) {
            Clan clan = createClanFromDTO(entry.getValue());
            clans.put(entry.getKey(), clan);
        }
        
        // Depois, criar todos os jogadores (agora com clãs disponíveis)
        for (Map.Entry<Integer, ClanPlayerDTO> entry : playerDTOs.entrySet()) {
            ClanPlayer player = createClanPlayerFromDTO(entry.getValue());
            // REFATORADO: Usar player_id diretamente como chave
            int playerId = entry.getKey();
            clanPlayers.put(playerId, player);
        }
        
        // Finalmente, criar as relações (com clãs e jogadores disponíveis)
        for (ClanRelationDTO relationDTO : relationDTOs) {
            ClanRelation relation = createClanRelationFromDTO(relationDTO);
            String key = relation.getClanId1() + "_" + relation.getClanId2();
            clanRelations.put(key, relation);
        }
        
        plugin.getLogger().info("Carregamento concluído: " + clans.size() + " clãs, " + clanPlayers.size() + " jogadores e " + clanRelations.size() + " relações carregados.");
    }

    // ===== MÉTODOS DE CONVERSÃO ENTRE MODELOS E DTOs =====
    
    /**
     * REFATORADO: Cria Clan a partir de DTO sem depender do cache.
     * Usado durante o carregamento inicial (duas passagens).
     */
    private Clan createClanFromDTO(ClanDTO dto) {
        // Usar o nome do fundador que vem do JOIN no DAO
        String founderName = dto.getFounderName();
        
        // REFATORADO: Usar pontos iniciais configuráveis
        int initialRankingPoints = plugin.getConfig().getInt("general.initial_ranking_points", 1000);
        Clan clan = new Clan(dto.getId(), dto.getTag(), dto.getName(), founderName, initialRankingPoints);
        clan.setFriendlyFireEnabled(dto.isFriendlyFireEnabled());
        clan.setPenaltyPoints(dto.getPenaltyPoints());
        clan.setRankingPoints(dto.getRankingPoints());
        return clan;
    }
    
    /**
     * REFATORADO: Cria ClanPlayer a partir de DTO sem depender do cache.
     * Usado durante o carregamento inicial (duas passagens).
     */
    private ClanPlayer createClanPlayerFromDTO(ClanPlayerDTO dto) {
        // Buscar o clã diretamente no mapa de clãs já carregado
        Clan clan = clans.get(dto.getClanId());
        ClanPlayer.ClanRole role = ClanPlayer.ClanRole.fromId(dto.getRole());
        int playerId = dto.getPlayerId();
        
        ClanPlayer player = new ClanPlayer(dto.getPlayerName(), playerId, clan, role, dto.getJoinDate().getTime());
        player.setKills(dto.getKills());
        player.setDeaths(dto.getDeaths());
        return player;
    }
    
    /**
     * REFATORADO: Cria ClanRelation a partir de DTO sem depender do cache.
     * Usado durante o carregamento inicial (duas passagens).
     */
    private ClanRelation createClanRelationFromDTO(ClanRelationDTO dto) {
        ClanRelation.RelationType type = ClanRelation.RelationType.fromId(dto.getStatus());
        return new ClanRelation(dto.getClanId1(), dto.getClanId2(), type, dto.getCreationDate().getTime());
    }
    
    /**
     * Converte ClanDTO para Clan (método original mantido para compatibilidade).
     * @deprecated Use createClanFromDTO() para carregamento inicial
     */
    private Clan fromDTO(ClanDTO dto) {
        // Para carregar um clã, o fundador já deve existir em clan_players.
        // O ideal é que o DAO já traga o nome do fundador.
        // Como fallback, podemos buscar no cache de jogadores.
        ClanPlayer founder = null;
        if (dto.getFounderUuid() != null) {
            try {
                founder = clanPlayers.get(UUID.fromString(dto.getFounderUuid()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido do fundador: " + dto.getFounderUuid());
            }
        }
        String founderName = (founder != null) ? founder.getPlayerName() : dto.getFounderName();

        // REFATORADO: Usar pontos iniciais configuráveis
        int initialRankingPoints = plugin.getConfig().getInt("general.initial_ranking_points", 1000);
        Clan clan = new Clan(dto.getId(), dto.getTag(), dto.getName(), founderName, initialRankingPoints);
        clan.setFriendlyFireEnabled(dto.isFriendlyFireEnabled());
        clan.setPenaltyPoints(dto.getPenaltyPoints());
        return clan;
    }
    
    /**
     * Converte Clan para ClanDTO.
     */
    private ClanDTO toDTO(Clan clan) {
        ClanPlayer founderPlayer = getClanPlayerByName(clan.getFounderName());
        int founderPlayerId = founderPlayer != null ? founderPlayer.getPlayerId() : -1;

        return new ClanDTO(
            clan.getId(),
            clan.getTag(),
            clan.getName(),
            founderPlayerId,
            clan.getFounderName(),
            new Date(), // TODO: Adicionar campo creationDate na classe Clan
            clan.isFriendlyFireEnabled(),
            clan.getPenaltyPoints(),
            clan.getRankingPoints()
        );
    }
    
    /**
     * Converte ClanPlayerDTO para ClanPlayer.
     */
    private ClanPlayer fromDTO(ClanPlayerDTO dto) {
        Clan clan = clans.get(dto.getClanId());
        ClanPlayer.ClanRole role = ClanPlayer.ClanRole.fromId(dto.getRole());
        int playerId = dto.getPlayerId();
        ClanPlayer player = new ClanPlayer(dto.getPlayerName(), playerId, clan, role, dto.getJoinDate().getTime());
        player.setKills(dto.getKills());
        player.setDeaths(dto.getDeaths());
        return player;
    }
    
    /**
     * Converte ClanPlayer para ClanPlayerDTO.
     */
    private ClanPlayerDTO toDTO(ClanPlayer player) {
        return new ClanPlayerDTO(
            player.getPlayerId(),
            player.getPlayerName(),
            player.getClan() != null ? player.getClan().getId() : -1,
            player.getRole().getId(),
            new Date(player.getJoinDate()),
            player.getKills(),
            player.getDeaths()
        );
    }
    
    /**
     * Converte ClanRelationDTO para ClanRelation.
     */
    private ClanRelation fromDTO(ClanRelationDTO dto) {
        ClanRelation.RelationType type = ClanRelation.RelationType.fromId(dto.getStatus());
        return new ClanRelation(dto.getClanId1(), dto.getClanId2(), type);
    }
    
    /**
     * Converte ClanRelation para ClanRelationDTO.
     */
    private ClanRelationDTO toDTO(ClanRelation relation) {
        return new ClanRelationDTO(
            relation.getClanId1(),
            relation.getClanId2(),
            relation.getType().getId(),
            new Date(relation.getCreationDate())
        );
    }

    // ===== MÉTODOS DE NEGÓCIO =====

    /**
     * Cria um novo clã.
     * REFATORADO: Usa player_id como identificador principal
     */
    public Clan createClan(String tag, String name, Player leader) {
        // REFATORADO: Obter player_id através do IdentityManager
        int leaderPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(leader);
        if (leaderPlayerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + leader.getName());
            return null;
        }
        
        // Validações prévias (tag/nome disponível, jogador sem clã)
        if (getClanByTag(tag) != null || getClanByName(name) != null || getClanByPlayer(leader) != null) {
            plugin.getLogger().warning("Falha na pré-validação para criar clã: " + tag);
            return null;
        }

        // 1. Criar o DTO com os dados corretos PRIMEIRO
        ClanDTO clanToCreate = new ClanDTO();
        clanToCreate.setTag(tag);
        clanToCreate.setName(name);
        clanToCreate.setFounderPlayerId(leaderPlayerId); // REFATORADO: Usar player_id
        clanToCreate.setFounderName(leader.getName());
        clanToCreate.setFriendlyFireEnabled(false);
        clanToCreate.setCreationDate(new Date());

        // 2. Tentar persistir no banco de dados
        ClanDTO savedClanDTO = clanDAO.createClan(clanToCreate);

        // 3. VERIFICAÇÃO CRÍTICA: Se a persistência falhou, o DAO retorna null.
        if (savedClanDTO == null || savedClanDTO.getId() <= 0) {
            plugin.getLogger().severe("FALHA CRÍTICA: O DAO retornou nulo ou um ID inválido ao tentar criar o clã " + tag);
            return null;
        }

        // 4. Se a persistência foi bem-sucedida, crie os objetos de negócio
        Clan clan = fromDTO(savedClanDTO);
        clans.put(clan.getId(), clan);

        // 5. Criar e configurar o ClanPlayer do fundador
        ClanPlayer founderPlayer = getClanPlayer(leader);
        if (founderPlayer == null) {
            founderPlayer = new ClanPlayer(leader); // Cria um novo se não existir
            clanPlayers.put(leaderPlayerId, founderPlayer); // REFATORADO: Usar player_id
        }
        founderPlayer.setClan(clan);
        founderPlayer.setRole(ClanPlayer.ClanRole.FUNDADOR);
        founderPlayer.setJoinDate(System.currentTimeMillis());

        // 6. Persistir o ClanPlayer do fundador
        clanDAO.saveOrUpdateClanPlayer(toDTO(founderPlayer));

        // 7. Registrar log da criação do clã
        clanDAO.logAction(
            clan.getId(),
            leaderPlayerId, // REFATORADO: Usar player_id diretamente
            leader.getName(),
            LogActionType.CLAN_CREATE,
            0, // Não há alvo específico
            null,
            "Clã criado: " + tag + " (" + name + ")"
        );

        plugin.getLogger().info("Clã " + tag + " criado com sucesso e persistido no DB.");
        return clan;
    }

    /**
     * Remove um clã completamente.
     *
     * @param clan O clã a ser removido
     * @return true se foi removido com sucesso
     */
    public boolean disbandClan(Clan clan) {
        if (clan == null) {
            return false;
        }

        try {
            // Remover todos os membros
            Set<String> allMembers = clan.getAllMemberNames();
            
            for (String memberName : allMembers) {
                // REFATORADO: Obter player_id através do IdentityManager em vez de usar UUID diretamente
                Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByName(memberName);
                if (playerId != null) {
                    ClanPlayer clanPlayer = clanPlayers.get(playerId);
                    if (clanPlayer != null) {
                        clanPlayer.setClan(null);
                        clanPlayer.setRole(ClanPlayer.ClanRole.MEMBRO);
                        // Persistir mudança do jogador
                        clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));
                    }
                }
            }

            // Registrar log da dissolução do clã ANTES de deletar
            clanDAO.logAction(
                clan.getId(),
                0, // Sistema como autor
                "Sistema",
                LogActionType.CLAN_DISBAND,
                0, // Não há alvo específico
                null,
                "Clã dissolvido: " + clan.getTag() + " (" + clan.getName() + ")"
            );

            // Deletar o clã do banco de dados
            clanDAO.deleteClan(toDTO(clan));

            // Remover das coleções em memória
            clans.remove(clan.getId());

            plugin.getLogger().info("Clã dissolvido: " + clan.getTag() + " (" + clan.getName() + ")");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao dissolver clã no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Métodos de Busca ---

    /**
     * Busca um clã pela tag.
     *
     * @param tag A tag do clã
     * @return O clã encontrado ou null
     */
    public Clan getClanByTag(String tag) {
        if (tag == null) {
            return null;
        }
        for (Clan clan : clans.values()) {
            if (clan.getTag().equalsIgnoreCase(tag)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Busca um clã pelo nome.
     *
     * @param name O nome do clã
     * @return O clã encontrado ou null
     */
    public Clan getClanByName(String name) {
        if (name == null) {
            return null;
        }
        for (Clan clan : clans.values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    /**
     * Obtém o clã de um jogador.
     * REFATORADO: Usa player_id como identificador principal
     * @param player O jogador.
     * @return O clã do jogador, ou null se não pertencer a nenhum clã.
     */
    public Clan getClanByPlayer(Player player) {
        // REFATORADO: Obter player_id através do IdentityManager
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        if (playerId == -1) {
            return null;
        }
        return getClanByPlayer(playerId);
    }

    /**
     * Obtém o clã de um jogador pelo player_id.
     * REFATORADO: Usa player_id em vez de UUID
     * @param playerId O player_id do jogador.
     * @return O clã do jogador, ou null se não pertencer a nenhum clã.
     */
    public Clan getClanByPlayer(int playerId) {
        if (playerId <= 0) {
            return null;
        }
        
        ClanPlayer clanPlayer = clanPlayers.get(playerId);
        return clanPlayer != null ? clanPlayer.getClan() : null;
    }

    /**
     * @deprecated Use getClanByPlayer(int playerId) instead
     */
    @Deprecated
    public Clan getClanByPlayer(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        
        // REFATORADO: Converter UUID para player_id
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUUID);
        if (playerId == -1) {
            return null;
        }
        return getClanByPlayer(playerId);
    }

    /**
     * Busca o clã de um jogador pelo nome.
     *
     * @param playerName O nome do jogador
     * @return O clã do jogador ou null
     */
    public Clan getClanByPlayerName(String playerName) {
        if (playerName == null) {
            return null;
        }
        
        for (ClanPlayer player : clanPlayers.values()) {
            if (player.getPlayerName().equalsIgnoreCase(playerName)) {
                return player.getClan();
            }
        }
        return null;
    }

    /**
     * Obtém o ClanPlayer de um jogador.
     * REFATORADO: Usa player_id como identificador principal
     * @param player O jogador.
     * @return O ClanPlayer do jogador, ou null se não pertencer a nenhum clã.
     */
    public ClanPlayer getClanPlayer(Player player) {
        // REFATORADO: Obter player_id através do IdentityManager
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        if (playerId == -1) {
            return null;
        }
        return clanPlayers.get(playerId);
    }

    /**
     * Obtém o ClanPlayer de um jogador pelo player_id.
     * REFATORADO: Usa player_id em vez de UUID
     * @param playerId O player_id do jogador
     * @return O ClanPlayer ou null
     */
    public ClanPlayer getClanPlayer(int playerId) {
        if (playerId <= 0) {
            return null;
        }
        return clanPlayers.get(playerId);
    }

    /**
     * @deprecated Use getClanPlayer(int playerId) instead
     */
    @Deprecated
    public ClanPlayer getClanPlayer(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        // REFATORADO: Converter UUID para player_id
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(uuid);
        if (playerId == -1) {
            return null;
        }
        return clanPlayers.get(playerId);
    }

    /**
     * Obtém o ClanPlayer de um jogador pelo nome.
     *
     * @param playerName O nome do jogador
     * @return O ClanPlayer ou null
     */
    public ClanPlayer getClanPlayerByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        for (ClanPlayer player : clanPlayers.values()) {
            if (player.getPlayerName().equalsIgnoreCase(playerName)) {
                return player;
            }
        }
        return null;
    }

    // --- Métodos de Gerenciamento de Membros ---

    /**
     * Adiciona um jogador a um clã.
     * REFATORADO: Usa player_id como identificador principal
     *
     * @param clan O clã
     * @param player O jogador
     * @param role O cargo do jogador
     * @return true se foi adicionado com sucesso
     */
    public boolean addPlayerToClan(Clan clan, Player player, ClanPlayer.ClanRole role) {
        if (clan == null || player == null) {
            return false;
        }

        // REFATORADO: Obter player_id através do IdentityManager
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        if (playerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + player.getName());
            return false;
        }

        String playerName = player.getName();
        
        // Verificar se o jogador já pertence a um clã
        ClanPlayer existingPlayer = getClanPlayer(playerId);
        if (existingPlayer != null && existingPlayer.hasClan()) {
            return false;
        }

        // Criar ou atualizar o ClanPlayer
        ClanPlayer clanPlayer = existingPlayer != null ? existingPlayer : new ClanPlayer(player);
        clanPlayer.setClan(clan);
        clanPlayer.setRole(role);
        clanPlayer.setJoinDate(System.currentTimeMillis());

        try {
            // Adicionar ao clã
            clan.addMember(playerName);
            
            // Persistir no banco de dados
            clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));
            
            // Adicionar às coleções em memória
            clanPlayers.put(playerId, clanPlayer); // REFATORADO: Usar player_id

            plugin.getLogger().info("Jogador " + playerName + " adicionado ao clã " + clan.getTag() + " como " + role.getDisplayName());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao adicionar jogador ao clã no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove um jogador de um clã.
     *
     * @param clan O clã
     * @param playerName O nome do jogador
     * @return true se foi removido com sucesso
     */
    public boolean removePlayerFromClan(Clan clan, String playerName) {
        if (clan == null || playerName == null) {
            return false;
        }

        ClanPlayer clanPlayer = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(playerName) && p.getClan() != null && p.getClan().equals(clan)) {
                clanPlayer = p;
                break;
            }
        }
        if (clanPlayer == null) {
            return false;
        }

        try {
            // Remover do clã
            clan.removeMember(playerName);
            clanPlayer.setClan(null);
            clanPlayer.setRole(ClanPlayer.ClanRole.MEMBRO);

            // Persistir mudança no banco de dados
            clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));

            // REFATORADO: Remover do cache principal para evitar memory leak
            // Converter UUID para player_id para remoção do cache
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(clanPlayer.getPlayerUUID());
            if (playerId != -1) {
                clanPlayers.remove(playerId);
            }

            plugin.getLogger().info("Jogador " + playerName + " removido do clã " + clan.getTag());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover jogador do clã no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Expulsa um jogador de um clã.
     * A verificação de permissão deve ser feita ANTES de chamar este método.
     *
     * @param clan O clã
     * @param playerName O nome do jogador a ser expulso
     * @param kickerName O nome do jogador que está expulsando
     * @return Resultado da operação
     */
    public KickResult kickPlayerFromClan(Clan clan, String playerName, String kickerName) {
        if (clan == null || playerName == null) {
            return KickResult.PLAYER_NOT_FOUND;
        }

        // Verificar se está tentando expulsar a si mesmo
        if (playerName.equalsIgnoreCase(kickerName)) {
            return KickResult.CANNOT_KICK_SELF;
        }

        ClanPlayer clanPlayer = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(playerName) && p.getClan() != null && p.getClan().equals(clan)) {
                clanPlayer = p;
                break;
            }
        }
        if (clanPlayer == null) {
            return KickResult.PLAYER_NOT_FOUND;
        }
        
        if (!clan.equals(clanPlayer.getClan())) {
            return KickResult.NOT_IN_SAME_CLAN;
        }

        // Verificar se está tentando expulsar o fundador (regra de negócio final)
        if (clanPlayer.isFounder()) {
            return KickResult.CANNOT_KICK_LEADER;
        }

        try {
            // Expulsar o jogador
            clan.removeMember(playerName);
            clanPlayer.setClan(null);
            clanPlayer.setRole(ClanPlayer.ClanRole.MEMBRO);

            // Persistir mudança no banco de dados
            clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));

            // REFATORADO: Remover do cache principal para evitar memory leak
            clanPlayers.remove(clanPlayer.getPlayerUUID());

            // Registrar log da expulsão
            clanDAO.logAction(
                clan.getId(),
                getPlayerIdByName(kickerName), // player_id do kicker
                kickerName,
                LogActionType.PLAYER_KICK,
                getPlayerIdByName(playerName), // player_id do alvo
                playerName,
                "Expulso por " + kickerName
            );

            plugin.getLogger().info("Jogador " + playerName + " expulso do clã " + clan.getTag() + " por " + kickerName);
            return KickResult.SUCCESS;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao expulsar jogador no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return KickResult.PLAYER_NOT_FOUND; // Fallback para erro de banco
        }
    }

    /**
     * Promove um membro a líder.
     * Apenas o Fundador pode promover membros.
     *
     * @param clan O clã
     * @param playerName O nome do jogador
     * @return Resultado da operação
     */
    public PromoteResult promotePlayer(Clan clan, String playerName) {
        if (clan == null || playerName == null) {
            return PromoteResult.PLAYER_NOT_FOUND;
        }

        ClanPlayer clanPlayer = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(playerName) && p.getClan() != null && p.getClan().equals(clan)) {
                clanPlayer = p;
                break;
            }
        }
        if (clanPlayer == null) {
            return PromoteResult.PLAYER_NOT_FOUND;
        }
        
        if (!clan.equals(clanPlayer.getClan())) {
            return PromoteResult.NOT_IN_SAME_CLAN;
        }

        // Verificar se já é fundador
        if (clanPlayer.isFounder()) {
            return PromoteResult.ALREADY_LEADER; // Usar mensagem existente
        }

        // Verificar se já é líder
        if (clanPlayer.isLeader()) {
            return PromoteResult.ALREADY_OFFICER; // Usar mensagem existente
        }

        try {
            // Promover a líder
            clan.promoteMember(playerName);
            clanPlayer.setRole(ClanPlayer.ClanRole.LIDER);

            // Persistir mudança no banco de dados
            clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));

            // Registrar log da promoção
            clanDAO.logAction(
                clan.getId(),
                getPlayerIdByName(clan.getFounderName()), // player_id do fundador (quem promove)
                clan.getFounderName(),
                LogActionType.PLAYER_PROMOTE,
                getPlayerIdByName(playerName), // player_id do alvo
                playerName,
                "Promovido a Líder"
            );

            plugin.getLogger().info("Jogador " + playerName + " promovido a líder no clã " + clan.getTag());
            return PromoteResult.SUCCESS;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao promover jogador no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return PromoteResult.PLAYER_NOT_FOUND; // Fallback para erro de banco
        }
    }

    /**
     * Rebaixa um líder a membro.
     * Apenas o Fundador pode rebaixar líderes.
     *
     * @param clan O clã
     * @param playerName O nome do jogador
     * @return Resultado da operação
     */
    public DemoteResult demotePlayer(Clan clan, String playerName) {
        if (clan == null || playerName == null) {
            return DemoteResult.PLAYER_NOT_FOUND;
        }

        ClanPlayer clanPlayer = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(playerName) && p.getClan() != null && p.getClan().equals(clan)) {
                clanPlayer = p;
                break;
            }
        }
        if (clanPlayer == null) {
            return DemoteResult.PLAYER_NOT_FOUND;
        }
        
        if (!clan.equals(clanPlayer.getClan())) {
            return DemoteResult.NOT_IN_SAME_CLAN;
        }

        // Verificar se é fundador (fundadores não podem ser rebaixados)
        if (clanPlayer.isFounder()) {
            return DemoteResult.CANNOT_DEMOTE_LEADER; // Usar mensagem existente
        }

        // Verificar se é líder
        if (!clanPlayer.isLeader()) {
            return DemoteResult.NOT_AN_OFFICER; // Usar mensagem existente
        }

        try {
            // Rebaixar a membro
            clan.demoteMember(playerName);
            clanPlayer.setRole(ClanPlayer.ClanRole.MEMBRO);

            // Persistir mudança no banco de dados
            clanDAO.saveOrUpdateClanPlayer(toDTO(clanPlayer));

            // Registrar log da demissão
            clanDAO.logAction(
                clan.getId(),
                getPlayerIdByName(clan.getFounderName()), // player_id do fundador (quem rebaixa)
                clan.getFounderName(),
                LogActionType.PLAYER_DEMOTE,
                getPlayerIdByName(playerName), // player_id do alvo
                playerName,
                "Rebaixado a Membro"
            );

            plugin.getLogger().info("Jogador " + playerName + " rebaixado a membro no clã " + clan.getTag());
            return DemoteResult.SUCCESS;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao rebaixar jogador no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return DemoteResult.PLAYER_NOT_FOUND; // Fallback para erro de banco
        }
    }

    /**
     * Transfere o cargo de fundador para outro jogador.
     * Apenas o fundador atual pode transferir o cargo.
     *
     * @param clan O clã
     * @param playerName O nome do jogador que se tornará fundador
     * @param oldFounderName O nome do fundador atual
     * @return Resultado da operação
     */
    public SetFounderResult setFounder(Clan clan, String playerName, String oldFounderName) {
        if (clan == null || playerName == null) {
            return SetFounderResult.PLAYER_NOT_FOUND;
        }

        // Buscar o jogador que se tornará fundador
        ClanPlayer newFounder = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(playerName) && p.getClan() != null && p.getClan().equals(clan)) {
                newFounder = p;
                break;
            }
        }
        if (newFounder == null) {
            return SetFounderResult.PLAYER_NOT_FOUND;
        }
        
        if (!clan.equals(newFounder.getClan())) {
            return SetFounderResult.NOT_IN_SAME_CLAN;
        }

        // Verificar se já é fundador
        if (newFounder.isFounder()) {
            return SetFounderResult.ALREADY_FOUNDER;
        }

        // Verificar se é líder (apenas líderes podem se tornar fundadores)
        if (!newFounder.isLeader()) {
            return SetFounderResult.NOT_LEADER;
        }

        // Buscar o fundador atual
        ClanPlayer oldFounder = null;
        for (ClanPlayer p : clanPlayers.values()) {
            if (p.getPlayerName().equalsIgnoreCase(oldFounderName) && p.getClan() != null && p.getClan().equals(clan)) {
                oldFounder = p;
                break;
            }
        }
        if (oldFounder == null || !oldFounder.isFounder()) {
            return SetFounderResult.PLAYER_NOT_FOUND;
        }

        try {
            // Executar a transação de transferência de fundador
                        boolean success = clanDAO.setFounder(toDTO(clan), getPlayerIdByName(newFounder.getPlayerName()),
                newFounder.getPlayerName(), getPlayerIdByName(oldFounder.getPlayerName()));
            
            if (success) {
                // Atualizar os objetos em memória
                oldFounder.setRole(ClanPlayer.ClanRole.LIDER);
                newFounder.setRole(ClanPlayer.ClanRole.FUNDADOR);
                
                // Atualizar o objeto Clan
                clan.setFounderName(newFounder.getPlayerName());
                
                // Persistir as mudanças dos jogadores
                clanDAO.saveOrUpdateClanPlayer(toDTO(oldFounder));
                clanDAO.saveOrUpdateClanPlayer(toDTO(newFounder));

                // Registrar log da transferência de fundador
                clanDAO.logAction(
                    clan.getId(),
                    getPlayerIdByName(oldFounderName), // player_id do antigo fundador (quem transferiu)
                    oldFounderName,
                    LogActionType.FOUNDER_CHANGE,
                    getPlayerIdByName(playerName), // player_id do novo fundador
                    playerName,
                    "Transferência de fundador: " + oldFounderName + " -> " + playerName
                );

                plugin.getLogger().info("Fundador transferido: " + oldFounderName + " -> " + playerName + " no clã " + clan.getTag());
                return SetFounderResult.SUCCESS;
            } else {
                plugin.getLogger().severe("Falha na transação de transferência de fundador no banco de dados");
                return SetFounderResult.PLAYER_NOT_FOUND;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao transferir fundador no banco de dados: " + e.getMessage());
            e.printStackTrace();
            return SetFounderResult.PLAYER_NOT_FOUND;
        }
    }

    // --- Métodos de Utilidade ---

    /**
     * Obtém todos os clãs.
     *
     * @return Lista de todos os clãs
     */
    public Collection<Clan> getAllClans() {
        return clans.values();
    }

    /**
     * Obtém o número total de clãs.
     *
     * @return O número de clãs
     */
    public int getClanCount() {
        return clans.size();
    }

    /**
     * Verifica se uma tag de clã está disponível.
     *
     * @param tag A tag a ser verificada
     * @return true se a tag está disponível
     */
    public boolean isTagAvailable(String tag) {
        if (tag == null) {
            return false;
        }
        return getClanByTag(tag) == null;
    }

    /**
     * Verifica se um nome de clã está disponível.
     *
     * @param name O nome a ser verificado
     * @return true se o nome está disponível
     */
    public boolean isNameAvailable(String name) {
        if (name == null) {
            return false;
        }
        return getClanByName(name) == null;
    }

    /**
     * Obtém a instância do plugin.
     *
     * @return A instância do plugin
     */
    public PrimeLeagueClans getPlugin() {
        return plugin;
    }

    /**
     * Obtém a instância do DAO.
     *
     * @return A instância do DAO
     */
    public ClanDAO getClanDAO() {
        return clanDAO;
    }

    // --- Métodos de Gerenciamento de Convites ---

    /**
     * Envia um convite para um jogador entrar no clã.
     *
     * @param inviter O jogador que está convidando
     * @param target O jogador convidado
     * @return true se o convite foi enviado com sucesso
     */
    public boolean sendInvitation(Player inviter, Player target) {
        if (inviter == null || target == null) {
            return false;
        }

        // Verificar se o convidador pode convidar
        ClanPlayer clanPlayer = getClanPlayer(inviter);
        if (clanPlayer == null || !clanPlayer.canInvite()) {
            return false;
        }

        Clan clan = clanPlayer.getClan();
        if (clan == null) {
            return false;
        }

        // Verificar se o alvo já pertence a um clã
        Clan targetClan = getClanByPlayer(target);
        if (targetClan != null) {
            return false;
        }

        // REFATORADO: Obter player_ids através do IdentityManager
        int targetPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(target);
        if (targetPlayerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + target.getName());
            return false;
        }

        // Verificar se já existe um convite pendente
        ClanInvitation existingInvite = pendingInvites.get(targetPlayerId);
        if (existingInvite != null && !existingInvite.isExpired()) {
            return false;
        }

        // Criar e armazenar o convite
        ClanInvitation invitation = new ClanInvitation(inviter.getName(), targetPlayerId, clan);
        pendingInvites.put(targetPlayerId, invitation);

        // Enviar mensagens
        inviter.sendMessage(org.bukkit.ChatColor.GREEN + "Convite enviado para " + target.getName() + "!");
        target.sendMessage(org.bukkit.ChatColor.GOLD + "=== Convite para Clã ===");
        target.sendMessage(org.bukkit.ChatColor.YELLOW + "Você foi convidado por " + inviter.getName() + " para entrar no clã " + clan.getTag() + "!");
        target.sendMessage(org.bukkit.ChatColor.YELLOW + "Use /clan accept para aceitar ou /clan deny para recusar.");
        target.sendMessage(org.bukkit.ChatColor.GRAY + "O convite expira em 5 minutos.");

        // REFATORADO: Obter player_id do inviter
        int inviterPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(inviter);
        if (inviterPlayerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + inviter.getName());
            return false;
        }

        // Registrar log do convite
        clanDAO.logAction(
            clan.getId(),
            inviterPlayerId,
            inviter.getName(),
            LogActionType.PLAYER_INVITE,
            targetPlayerId,
            target.getName(),
            "Convite enviado para " + target.getName()
        );

        plugin.getLogger().info("Convite enviado: " + inviter.getName() + " convidou " + target.getName() + " para o clã " + clan.getTag());
        return true;
    }

    /**
     * Aceita um convite pendente.
     * REFATORADO: Usa player_id como identificador principal
     *
     * @param target O jogador que está aceitando o convite
     * @return true se o convite foi aceito com sucesso
     */
    public boolean acceptInvitation(Player target) {
        if (target == null) {
            return false;
        }

        // REFATORADO: Obter player_id através do IdentityManager
        int targetPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(target);
        if (targetPlayerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + target.getName());
            return false;
        }

        ClanInvitation invitation = pendingInvites.get(targetPlayerId);
        if (invitation == null) {
            return false;
        }

        if (invitation.isExpired()) {
            pendingInvites.remove(targetPlayerId);
            return false;
        }

        Clan clan = invitation.getClan();
        
        // Adicionar o jogador ao clã
        if (addPlayerToClan(clan, target, ClanPlayer.ClanRole.MEMBRO)) {
            // Remover o convite
            pendingInvites.remove(targetPlayerId);
            
            // Notificar o convidador
            Player inviter = org.bukkit.Bukkit.getPlayer(invitation.getInviterName());
            if (inviter != null && inviter.isOnline()) {
                inviter.sendMessage(org.bukkit.ChatColor.GREEN + target.getName() + " aceitou seu convite e entrou no clã!");
            }
            
            // Notificar o clã
            notifyClanMembers(clan, org.bukkit.ChatColor.GREEN + target.getName() + " entrou no clã!");
            
            // Registrar log da entrada no clã
            clanDAO.logAction(
                clan.getId(),
                targetPlayerId,
                target.getName(),
                LogActionType.PLAYER_JOIN,
                0, // Não há alvo específico
                null,
                "Entrou no clã via convite de " + invitation.getInviterName()
            );
            
            plugin.getLogger().info("Convite aceito: " + target.getName() + " entrou no clã " + clan.getTag());
            return true;
        }

        return false;
    }

    /**
     * Recusa um convite pendente.
     * REFATORADO: Usa player_id como identificador principal
     *
     * @param target O jogador que está recusando o convite
     * @return true se o convite foi recusado com sucesso
     */
    public boolean denyInvitation(Player target) {
        if (target == null) {
            return false;
        }

        // REFATORADO: Obter player_id através do IdentityManager
        int targetPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(target);
        if (targetPlayerId == -1) {
            plugin.getLogger().severe("FALHA CRÍTICA: Não foi possível obter player_id para " + target.getName());
            return false;
        }

        ClanInvitation invitation = pendingInvites.get(targetPlayerId);
        if (invitation == null) {
            return false;
        }

        if (invitation.isExpired()) {
            pendingInvites.remove(targetPlayerId);
            return false;
        }

        // Remover o convite
        pendingInvites.remove(targetPlayerId);
        
        // Notificar o convidador
        Player inviter = org.bukkit.Bukkit.getPlayer(invitation.getInviterName());
        if (inviter != null && inviter.isOnline()) {
            inviter.sendMessage(org.bukkit.ChatColor.RED + target.getName() + " recusou seu convite para o clã.");
        }
        
        plugin.getLogger().info("Convite recusado: " + target.getName() + " recusou convite para o clã " + invitation.getClan().getTag());
        return true;
    }

    /**
     * Remove convites expirados.
     * REFATORADO: Usa player_id como identificador principal
     */
    public void cleanupExpiredInvites() {
        Iterator<Map.Entry<Integer, ClanInvitation>> iterator = pendingInvites.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, ClanInvitation> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    /**
     * Obtém o número de convites pendentes.
     *
     * @return O número de convites pendentes
     */
    public int getPendingInviteCount() {
        return pendingInvites.size();
    }

    /**
     * Notifica todos os membros online do clã.
     *
     * @param clan O clã
     * @param message A mensagem
     */
    public void notifyClanMembers(Clan clan, String message) {
        for (String memberName : clan.getAllMemberNames()) {
            org.bukkit.entity.Player member = org.bukkit.Bukkit.getPlayerExact(memberName);
            if (member != null && member.isOnline()) {
                member.sendMessage(message);
            }
        }
    }

    // --- Métodos de Gerenciamento de Relações ---

    /**
     * Verifica se dois clãs são aliados.
     *
     * @param clan1 Primeiro clã
     * @param clan2 Segundo clã
     * @return true se os clãs são aliados
     */
    public boolean areAllies(Clan clan1, Clan clan2) {
        if (clan1 == null || clan2 == null || clan1.equals(clan2)) {
            return false;
        }
        
        String key1 = clan1.getId() + "_" + clan2.getId();
        String key2 = clan2.getId() + "_" + clan1.getId();
        
        ClanRelation relation = clanRelations.get(key1);
        if (relation == null) {
            relation = clanRelations.get(key2);
        }
        
        return relation != null && relation.getType() == ClanRelation.RelationType.ALLY;
    }

    /**
     * Verifica se dois clãs são rivais.
     *
     * @param clan1 Primeiro clã
     * @param clan2 Segundo clã
     * @return true se os clãs são rivais
     */
    public boolean areRivals(Clan clan1, Clan clan2) {
        if (clan1 == null || clan2 == null || clan1.equals(clan2)) {
            return false;
        }
        
        String key1 = clan1.getId() + "_" + clan2.getId();
        String key2 = clan2.getId() + "_" + clan1.getId();
        
        ClanRelation relation = clanRelations.get(key1);
        if (relation == null) {
            relation = clanRelations.get(key2);
        }
        
        return relation != null && relation.getType() == ClanRelation.RelationType.RIVAL;
    }

    /**
     * Cria uma aliança entre dois clãs.
     *
     * @param clan1 Primeiro clã
     * @param clan2 Segundo clã
     * @return true se a aliança foi criada com sucesso
     */
    public boolean createAlliance(Clan clan1, Clan clan2) {
        if (clan1 == null || clan2 == null || clan1.equals(clan2)) {
            return false;
        }

        try {
            ClanRelation relation = new ClanRelation(clan1.getId(), clan2.getId(), ClanRelation.RelationType.ALLY);
            clanDAO.saveClanRelation(toDTO(relation));
            
            String key = relation.getClanId1() + ":" + relation.getClanId2();
            clanRelations.put(key, relation);
            
            plugin.getLogger().info("Aliança criada entre " + clan1.getTag() + " e " + clan2.getTag());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao criar aliança: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove uma aliança entre dois clãs.
     *
     * @param clan1 Primeiro clã
     * @param clan2 Segundo clã
     * @return true se a aliança foi removida com sucesso
     */
    public boolean removeAlliance(Clan clan1, Clan clan2) {
        if (clan1 == null || clan2 == null || clan1.equals(clan2)) {
            return false;
        }

        try {
            String key1 = clan1.getId() + ":" + clan2.getId();
            String key2 = clan2.getId() + ":" + clan1.getId();
            
            ClanRelation relation = clanRelations.get(key1);
            if (relation == null) {
                relation = clanRelations.get(key2);
            }
            
            if (relation != null && relation.getType() == ClanRelation.RelationType.ALLY) {
                clanDAO.deleteClanRelation(toDTO(relation));
                clanRelations.remove(key1);
                clanRelations.remove(key2);
                
                plugin.getLogger().info("Aliança removida entre " + clan1.getTag() + " e " + clan2.getTag());
                return true;
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover aliança: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Declara uma rivalidade entre dois clãs.
     *
     * @param clan1 Primeiro clã
     * @param clan2 Segundo clã
     * @return true se a rivalidade foi declarada com sucesso
     */
    public boolean declareRivalry(Clan clan1, Clan clan2) {
        if (clan1 == null || clan2 == null || clan1.equals(clan2)) {
            return false;
        }

        try {
            ClanRelation relation = new ClanRelation(clan1.getId(), clan2.getId(), ClanRelation.RelationType.RIVAL);
            clanDAO.saveClanRelation(toDTO(relation));
            
            String key = relation.getClanId1() + ":" + relation.getClanId2();
            clanRelations.put(key, relation);
            
            plugin.getLogger().info("Rivalidade declarada entre " + clan1.getTag() + " e " + clan2.getTag());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao declarar rivalidade: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    /**
     * Busca o player_id de um jogador pelo nome.
     * REFATORADO: Usa player_id como identificador principal
     * 
     * @param playerName Nome do jogador
     * @return player_id do jogador ou -1 se não encontrado
     */
    private int getPlayerIdByName(String playerName) {
        if (playerName == null) {
            return -1;
        }
        
        // Primeiro, tentar buscar no cache de ClanPlayer
        for (Map.Entry<Integer, ClanPlayer> entry : clanPlayers.entrySet()) {
            ClanPlayer clanPlayer = entry.getValue();
            if (clanPlayer.getPlayerName().equalsIgnoreCase(playerName)) {
                return entry.getKey(); // Retorna o player_id
            }
        }
        
        // Se não encontrou no cache, tentar buscar via Bukkit
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            return PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        }
        
        // Se ainda não encontrou, tentar buscar via IdentityManager
        return PrimeLeagueAPI.getIdentityManager().getPlayerIdByName(playerName);
    }

    /**
     * @deprecated Use getPlayerIdByName(String playerName) instead
     */
    @Deprecated
    private String getPlayerUUIDByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        
        // REFATORADO: Converter player_id para UUID
        int playerId = getPlayerIdByName(playerName);
        if (playerId == -1) {
            return null;
        }
        
        UUID uuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(playerId);
        return uuid != null ? uuid.toString() : null;
    }

    /**
     * Busca os logs de um clã com paginação.
     * 
     * @param clanId ID do clã
     * @param page Número da página (começa em 1)
     * @param pageSize Tamanho da página
     * @return Lista de logs do clã
     */
    public List<ClanLogDTO> getClanLogs(int clanId, int page, int pageSize) {
        return clanDAO.getClanLogs(clanId, page, pageSize);
    }

    /**
     * Busca os logs de um jogador específico em um clã.
     * 
     * @param clanId ID do clã
     * @param playerUuid UUID do jogador
     * @param page Número da página (começa em 1)
     * @param pageSize Tamanho da página
     * @return Lista de logs do jogador
     */
    public List<ClanLogDTO> getPlayerLogs(int clanId, int playerId, int page, int pageSize) {
        return clanDAO.getPlayerLogs(clanId, playerId, page, pageSize);
    }

    // ===== SISTEMA DE SANÇÕES DE CLÃ =====

    /**
     * Aplica sanções de punição ao clã de um jogador usando transação atômica.
     * REFATORADO: Usa player_id como identificador principal
     * 
     * @param playerId player_id do jogador punido
     * @param severity Severidade da punição
     * @param authorName Nome do staff que aplicou a punição
     * @param targetName Nome do jogador punido
     * @return true se as sanções foram aplicadas com sucesso
     */
    public boolean applyPunishmentSanctions(int playerId, PunishmentSeverity severity, String authorName, String targetName) {
        try {
            // Verificar se o jogador está em um clã
            ClanPlayer clanPlayer = clanPlayers.get(playerId);
            if (clanPlayer == null) {
                plugin.getLogger().warning("Jogador com player_id " + playerId + " não está em clã");
                return false;
            }

            Clan clan = clans.get(clanPlayer.getClan().getId());
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanPlayer.getClan().getId() + " não encontrado");
                return false;
            }

            // Obter pontos de penalidade baseados na severidade
            int penaltyPoints = getPenaltyPointsForSeverity(severity);
            plugin.getLogger().info("DEBUG: Severidade " + severity.getDisplayName() + " gera " + penaltyPoints + " pontos");
            if (penaltyPoints <= 0) {
                plugin.getLogger().info("Severidade " + severity.getDisplayName() + " não gera pontos de penalidade");
                return true; // Não é erro, apenas não gera pontos
            }

            // Aplicar pontos usando transação atômica
            int currentPoints = clan.getPenaltyPoints();
            String details = "Punição " + severity.getDisplayName() + " (+" + penaltyPoints + " pontos)";
            
            plugin.getLogger().info("DEBUG: Aplicando " + penaltyPoints + " pontos ao clã " + clan.getTag() + " (atual: " + currentPoints + ")");
            
            boolean success = clanDAO.addPenaltyPointsAndLog(
                clan.getId(), currentPoints, penaltyPoints,
                0, authorName, playerId, targetName, details
            );
            
            if (!success) {
                plugin.getLogger().severe("Falha na transação de pontos de penalidade");
                return false;
            }
            
            plugin.getLogger().info("DEBUG: Transação de pontos executada com sucesso");

            // Atualizar cache
            clan.setPenaltyPoints(currentPoints + penaltyPoints);

            // Verificar thresholds de sanção
            checkSanctionThresholds(clan, currentPoints, currentPoints + penaltyPoints);

            plugin.getLogger().info("Aplicadas sanções de severidade " + severity.getDisplayName() + 
                                  " ao clã " + clan.getTag() + " (+" + penaltyPoints + " pontos)");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao aplicar sanções de punição: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtém pontos de penalidade baseados na severidade.
     * 
     * @param severity Severidade da punição
     * @return Pontos de penalidade
     */
    private int getPenaltyPointsForSeverity(PunishmentSeverity severity) {
        // CORREÇÃO: Converte para minúsculas para corresponder ao config.yml
        String configKey = severity.name().toLowerCase();
        return plugin.getConfig().getInt("sanctions.penalty-points." + configKey, 0);
    }
    
    /**
     * Adiciona pontos de penalidade diretamente a um clã (comando administrativo).
     * 
     * @param clanId ID do clã
     * @param points Pontos a adicionar
     * @param authorName Nome do staff que adicionou os pontos
     * @return true se os pontos foram adicionados com sucesso
     */
    public boolean addPenaltyPointsDirectly(int clanId, int points, String authorName) {
        try {
            Clan clan = clans.get(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado");
                return false;
            }

            int currentPoints = clan.getPenaltyPoints();
            String details = "Adição administrativa: +" + points + " pontos";
            
            boolean success = clanDAO.addPenaltyPointsAndLog(
                clanId, currentPoints, points,
                0, authorName, 0, null, details
            );
            
            if (!success) {
                plugin.getLogger().severe("Falha na transação de pontos de penalidade");
                return false;
            }

            // Atualizar cache
            clan.setPenaltyPoints(currentPoints + points);

            // Verificar thresholds de sanção
            checkSanctionThresholds(clan, currentPoints, currentPoints + points);

            plugin.getLogger().info("Adicionados " + points + " pontos de penalidade ao clã " + 
                                  clan.getTag() + " por " + authorName + " (Total: " + (currentPoints + points) + ")");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao adicionar pontos de penalidade diretamente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Define os pontos de penalidade de um clã.
     * 
     * @param clanId ID do clã
     * @param points Novos pontos de penalidade
     * @param authorName Nome do staff que definiu os pontos
     * @return true se os pontos foram definidos com sucesso
     */
    public boolean setPenaltyPoints(int clanId, int points, String authorName) {
        try {
            Clan clan = clans.get(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado");
                return false;
            }

            int oldPoints = clan.getPenaltyPoints();
            
            // Atualizar no banco de dados usando transação atômica
            int pointsToAdd = points - oldPoints;
            String details = "Pontos definidos: " + oldPoints + " → " + points;
            boolean success = clanDAO.addPenaltyPointsAndLog(
                clanId, oldPoints, pointsToAdd,
                0, authorName, 0, null, details
            );
            if (!success) {
                plugin.getLogger().severe("Falha na transação de pontos de penalidade");
                return false;
            }

            // Atualizar no cache
            clan.setPenaltyPoints(points);

            plugin.getLogger().info("Pontos de penalidade do clã " + clan.getTag() + 
                                  " definidos para " + points + " por " + authorName);
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao definir pontos de penalidade: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove pontos de penalidade de um clã (remoção parcial).
     * 
     * @param clanId ID do clã
     * @param pointsToRemove Pontos a remover
     * @param authorName Nome do staff que removeu os pontos
     * @return true se os pontos foram removidos com sucesso
     */
    public boolean removePenaltyPoints(int clanId, int pointsToRemove, String authorName) {
        try {
            Clan clan = clans.get(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado");
                return false;
            }

            int currentPoints = clan.getPenaltyPoints();
            
            // Verificar se há pontos suficientes para remover
            if (currentPoints < pointsToRemove) {
                plugin.getLogger().warning("Clã " + clanId + " tem apenas " + currentPoints + " pontos, não é possível remover " + pointsToRemove);
                return false;
            }

            // Calcular novos pontos (não pode ser negativo)
            int newPoints = Math.max(0, currentPoints - pointsToRemove);
            int pointsActuallyRemoved = currentPoints - newPoints;
            
            // Atualizar no banco de dados usando transação atômica
            String details = "Remoção de pontos: -" + pointsActuallyRemoved + " pontos";
            boolean success = clanDAO.addPenaltyPointsAndLog(
                clanId, currentPoints, -pointsActuallyRemoved,
                0, authorName, 0, null, details
            );
            if (!success) {
                plugin.getLogger().severe("Falha na transação de remoção de pontos de penalidade");
                return false;
            }

            // Atualizar no cache
            clan.setPenaltyPoints(newPoints);

            plugin.getLogger().info("Removidos " + pointsActuallyRemoved + " pontos de penalidade do clã " + 
                                  clan.getTag() + " por " + authorName + " (Total: " + newPoints + ")");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao remover pontos de penalidade: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove todos os pontos de penalidade de um clã (perdão).
     * 
     * @param clanId ID do clã
     * @param authorName Nome do staff que perdoou o clã
     * @return true se o perdão foi aplicado com sucesso
     */
    public boolean pardonClan(int clanId, String authorName) {
        return setPenaltyPoints(clanId, 0, authorName);
    }

    /**
     * Verifica se um clã atingiu thresholds de sanção e aplica as consequências.
     * Verifica todos os tiers cruzados, não apenas o primeiro.
     * 
     * @param clan Clã a verificar
     * @param oldPoints Pontos anteriores
     * @param newPoints Novos pontos
     */
    private void checkSanctionThresholds(Clan clan, int oldPoints, int newPoints) {
        // Obter configurações de tiers
        int tier1Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-1.threshold", 10);
        int tier2Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-2.threshold", 25);
        int tier3Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-3.threshold", 50);
        int tier4Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-4.threshold", 100);

        // Verificar todos os tiers cruzados
        if (oldPoints < tier1Threshold && newPoints >= tier1Threshold) {
            applySanction(clan, 1, "Tier 1 atingido");
        }
        if (oldPoints < tier2Threshold && newPoints >= tier2Threshold) {
            applySanction(clan, 2, "Tier 2 atingido");
        }
        if (oldPoints < tier3Threshold && newPoints >= tier3Threshold) {
            applySanction(clan, 3, "Tier 3 atingido");
        }
        if (oldPoints < tier4Threshold && newPoints >= tier4Threshold) {
            applySanction(clan, 4, "Tier 4 atingido");
        }
    }

    /**
     * Aplica uma sanção ao clã baseada no tier.
     * 
     * @param clan Clã a ser sancionado
     * @param tier Nível da sanção (1, 2, 3 ou 4)
     * @param reason Motivo da sanção
     */
    private void applySanction(Clan clan, int tier, String reason) {
        try {
            // Obter configurações do tier
            String tierPath = "sanctions.sanction-tiers.tier-" + tier;
            String penaltyType = plugin.getConfig().getString(tierPath + ".penalty", "warning");
            int durationDays = plugin.getConfig().getInt(tierPath + ".duration-days", 0);
            int finePercentage = plugin.getConfig().getInt(tierPath + ".fine-percentage", 0);
            int eloDeduction = plugin.getConfig().getInt(tierPath + ".elo-deduction-percentage", 0);

            // Aplicar consequências baseadas no tipo de penalidade
            String penaltyDetails = "";
            switch (penaltyType.toLowerCase()) {
                case "warning":
                    penaltyDetails = "Aviso formal";
                    break;
                case "fine":
                    penaltyDetails = "Multa econômica (" + finePercentage + "%)";
                    break;
                case "suspension":
                    penaltyDetails = "Suspensão competitiva (" + durationDays + " dias, ELO: -" + eloDeduction + "%)";
                    break;
                case "disqualification":
                    penaltyDetails = "Desqualificação (" + durationDays + " dias, ELO: -" + eloDeduction + "%)";
                    break;
                default:
                    penaltyDetails = "Penalidade não especificada";
            }

            // Registrar log da sanção
            String details = "Sanção Tier " + tier + " aplicada: " + reason + " - " + penaltyDetails;
            clanDAO.logAction(clan.getId(), 0, "Sistema", LogActionType.SANCTION_ADD,
                            0, null, details);

            plugin.getLogger().warning("Sanção Tier " + tier + " aplicada ao clã " + clan.getTag() + ": " + penaltyDetails);

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao aplicar sanção: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtém os pontos de penalidade de um clã.
     * 
     * @param clanId ID do clã
     * @return Pontos de penalidade ou -1 se o clã não for encontrado
     */
    public int getClanPenaltyPoints(int clanId) {
        Clan clan = clans.get(clanId);
        return clan != null ? clan.getPenaltyPoints() : -1;
    }

    /**
     * Obtém o tier atual de sanção de um clã.
     * 
     * @param clanId ID do clã
     * @return Tier atual (0 = sem sanção, 1-4 = tiers de sanção)
     */
    public int getClanSanctionTier(int clanId) {
        int points = getClanPenaltyPoints(clanId);
        if (points < 0) return -1;

        int tier1Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-1.threshold", 10);
        int tier2Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-2.threshold", 25);
        int tier3Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-3.threshold", 50);
        int tier4Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-4.threshold", 100);

        if (points >= tier4Threshold) return 4;
        if (points >= tier3Threshold) return 3;
        if (points >= tier2Threshold) return 2;
        if (points >= tier1Threshold) return 1;
        return 0;
    }

    /**
     * Verifica se um jogador está em um clã.
     * REFATORADO: Usa player_id como identificador principal
     * 
     * @param playerId player_id do jogador
     * @return true se o jogador está em um clã
     */
    public boolean isPlayerInClan(int playerId) {
        ClanPlayer clanPlayer = clanPlayers.get(playerId);
        return clanPlayer != null && clanPlayer.hasClan();
    }

    /**
     * @deprecated Use isPlayerInClan(int playerId) instead
     */
    @Deprecated
    public boolean isPlayerInClan(String playerUuid) {
        try {
            // REFATORADO: Converter UUID para player_id
            UUID uuid = UUID.fromString(playerUuid);
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(uuid);
            if (playerId == -1) {
                return false;
            }
            return isPlayerInClan(playerId);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Reverte uma sanção de clã baseada na severidade original da punição.
     * REFATORADO: Usa player_id como identificador principal
     * 
     * @param playerId player_id do jogador que teve a punição revertida
     * @param originalSeverity Severidade original da punição
     * @param adminName Nome do administrador que reverteu
     * @return true se a reversão foi bem-sucedida
     */
    public boolean revertClanSanction(int playerId, PunishmentSeverity originalSeverity, String adminName) {
        try {
            // Encontrar o clã do jogador
            ClanPlayer clanPlayer = clanPlayers.get(playerId);
            if (clanPlayer == null || !clanPlayer.hasClan()) {
                plugin.getLogger().info("Jogador com player_id " + playerId + " não está em clã - ignorando reversão de sanção");
                return false;
            }
            
            Clan clan = clanPlayer.getClan();
            int currentPoints = clan.getPenaltyPoints();
            
            // Calcular pontos a reverter baseado na severidade original
            int pointsToRevert = getPenaltyPointsForSeverity(originalSeverity);
            if (pointsToRevert <= 0) {
                plugin.getLogger().warning("Severidade " + originalSeverity.getDisplayName() + " não gera pontos para reverter");
                return false;
            }
            
            // Calcular novos pontos e tier
            int newPoints = Math.max(0, currentPoints - pointsToRevert);
            int newSanctionTier = calculateSanctionTier(newPoints);
            
            // Executar reversão transacional
            String details = "Reversão de sanção: -" + pointsToRevert + " pontos (severidade: " + originalSeverity.getDisplayName() + ")";
            boolean success = clanDAO.revertSanctionAndLog(
                clan.getId(), currentPoints, pointsToRevert, newSanctionTier,
                0, adminName, playerId, clanPlayer.getPlayerName(), details
            );
            
            if (success) {
                // Atualizar cache em memória
                clan.setPenaltyPoints(newPoints);
                
                plugin.getLogger().info("Sanção revertida para o clã " + clan.getTag() + 
                                      ": -" + pointsToRevert + " pontos (novo total: " + newPoints + ")");
                return true;
            } else {
                plugin.getLogger().severe("Falha ao reverter sanção no banco de dados para o clã " + clan.getTag());
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao reverter sanção de clã: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Calcula o tier de sanção baseado nos pontos de penalidade.
     * 
     * @param points Pontos de penalidade
     * @return Tier de sanção (0 = sem sanção, 1-4 = tiers de sanção)
     */
    private int calculateSanctionTier(int points) {
        int tier1Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-1.threshold", 10);
        int tier2Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-2.threshold", 25);
        int tier3Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-3.threshold", 50);
        int tier4Threshold = plugin.getConfig().getInt("sanctions.sanction-tiers.tier-4.threshold", 100);

        if (points >= tier4Threshold) return 4;
        if (points >= tier3Threshold) return 3;
        if (points >= tier2Threshold) return 2;
        if (points >= tier1Threshold) return 1;
        return 0;
    }
    
    /**
     * Atualiza estatísticas de KDR de dois jogadores de forma transacional.
     * REFATORADO: Usa player_id como identificador principal
     * Garante que tanto as estatísticas quanto o log sejam salvos juntos.
     * 
     * @param killer ClanPlayer do jogador que matou
     * @param victim ClanPlayer do jogador que morreu
     * @return true se a operação foi bem-sucedida
     */
    public boolean updateKDRTransactionally(ClanPlayer killer, ClanPlayer victim) {
        try {
            // Verificar se ambos os jogadores têm clãs
            if (!killer.hasClan() || !victim.hasClan()) {
                plugin.getLogger().warning("Tentativa de atualizar KDR para jogadores sem clã");
                return false;
            }
            
            // Incrementar estatísticas em memória
            killer.addKill();
            victim.addDeath();
            
            // REFATORADO: Obter player_ids através do IdentityManager
            int killerPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(killer.getPlayerUUID());
            int victimPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(victim.getPlayerUUID());
            
            if (killerPlayerId == -1 || victimPlayerId == -1) {
                plugin.getLogger().severe("Não foi possível obter player_id para atualização de KDR");
                return false;
            }
            
            // Executar atualização transacional no banco
            boolean success = clanDAO.updateKDRAndLog(
                killerPlayerId, killer.getPlayerName(), 
                killer.getKills(), killer.getDeaths(),
                victimPlayerId, victim.getPlayerName(), 
                victim.getKills(), victim.getDeaths(),
                killer.getClan().getId() // Usar o clã do killer para o log
            );
            
            if (success) {
                plugin.getLogger().info("KDR atualizado transacionalmente - " + killer.getPlayerName() + 
                                      " matou " + victim.getPlayerName() + 
                                      " (KDR: " + String.format("%.2f", killer.getKDR()) + ")");
                return true;
            } else {
                // Reverter mudanças em memória se a transação falhou
                killer.addKill(-1); // Reverter o incremento
                victim.addDeath(-1); // Reverter o incremento
                plugin.getLogger().severe("Falha na transação de KDR - mudanças revertidas em memória");
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao atualizar KDR transacionalmente: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Executa a limpeza automática de membros inativos dos clãs.
     * Remove jogadores que não jogam há mais tempo que o configurado.
     * 
     * @return true se a operação foi bem-sucedida
     */
    public boolean cleanupInactiveMembers() {
        try {
            // Verificar se o sistema está habilitado
            if (!plugin.getConfig().getBoolean("inactive-member-cleanup.enabled", true)) {
                plugin.getLogger().info("Sistema de limpeza de membros inativos está desabilitado.");
                return true;
            }
            
            int inactiveDays = plugin.getConfig().getInt("inactive-member-cleanup.inactive-days", 90);
            int batchSize = plugin.getConfig().getInt("inactive-member-cleanup.batch-size", 500);
            boolean notifyFounders = plugin.getConfig().getBoolean("inactive-member-cleanup.notify-founders", true);
            
            plugin.getLogger().info("Iniciando limpeza de membros inativos (dias: " + inactiveDays + ", lote: " + batchSize + ")");
            
            // Buscar membros inativos
            List<InactiveMemberInfo> inactiveMembers = clanDAO.findInactiveMembers(inactiveDays, batchSize);
            
            if (inactiveMembers.isEmpty()) {
                plugin.getLogger().info("Nenhum membro inativo encontrado para remoção.");
                return true;
            }
            
            // Agrupar remoções por clã para notificação
            Map<Integer, List<String>> removalsByClan = new HashMap<>();
            int totalRemoved = 0;
            
            for (InactiveMemberInfo member : inactiveMembers) {
                // REFATORADO: Converter UUID para player_id
                UUID playerUUID = UUID.fromString(member.getPlayerUuid());
                int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUUID);
                
                if (playerId == -1) {
                    plugin.getLogger().warning("Não foi possível obter player_id para " + member.getPlayerName() + " - pulando remoção");
                    continue;
                }
                
                // Remover membro via DAO (transação atômica)
                String reason = "Removido por inatividade (" + member.getDaysInactive() + " dias)";
                boolean success = clanDAO.removeInactiveMember(
                    playerId, member.getClanId(), reason
                );
                
                if (success) {
                    // Atualizar cache em memória
                    ClanPlayer clanPlayer = clanPlayers.get(playerId);
                    if (clanPlayer != null) {
                        clanPlayer.setClan(null); // Desassociar do clã
                    }
                    
                    // Agrupar para notificação
                    if (!removalsByClan.containsKey(member.getClanId())) {
                        removalsByClan.put(member.getClanId(), new ArrayList<String>());
                    }
                    removalsByClan.get(member.getClanId()).add(member.getPlayerName());
                    
                    totalRemoved++;
                    
                    plugin.getLogger().info("Membro inativo removido: " + member.getPlayerName() + 
                                          " do clã " + member.getClanTag() + 
                                          " (" + member.getDaysInactive() + " dias inativo)");
                } else {
                    plugin.getLogger().warning("Falha ao remover membro inativo: " + member.getPlayerName());
                }
            }
            
            // Notificar fundadores se habilitado
            if (notifyFounders && !removalsByClan.isEmpty()) {
                for (Map.Entry<Integer, List<String>> entry : removalsByClan.entrySet()) {
                    int clanId = entry.getKey();
                    List<String> removedPlayers = entry.getValue();
                    
                    Clan clan = clans.get(clanId);
                    if (clan != null) {
                        String founderName = clan.getFounderName();
                        plugin.getLogger().info("Notificando fundador " + founderName + 
                                              " do clã " + clan.getTag() + 
                                              " sobre " + removedPlayers.size() + " remoções");
                        
                        // Aqui você pode implementar notificação via Discord, email, etc.
                        // Por enquanto, apenas log
                    }
                }
            }
            
            plugin.getLogger().info("Limpeza de membros inativos concluída: " + totalRemoved + " membros removidos.");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro durante limpeza de membros inativos: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Obtém informações detalhadas de todos os membros de um clã.
     * REFATORADO: Usa player_id como identificador principal
     * @param clan O clã para buscar os membros.
     * @return Lista de informações detalhadas dos membros.
     */
    public List<ClanMemberInfo> getClanMembers(Clan clan) {
        List<ClanMemberInfo> members = clanDAO.getClanMembersInfo(clan.getId());
        
        // REFATORADO: Usar o sistema proativo de status online/offline com player_id
        for (ClanMemberInfo member : members) {
            // REFATORADO: Converter UUID para player_id
            UUID playerUUID = UUID.fromString(member.getPlayerUuid());
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUUID);
            if (playerId != -1) {
                boolean isOnline = isPlayerOnline(playerId);
                member.setOnline(isOnline);
            } else {
                member.setOnline(false);
            }
        }
        
        return members;
    }
    
    // ===== MÉTODOS DE RANKING =====
    
    /**
     * Busca ranking de clãs com paginação e critérios de ordenação.
     * @param criteria Critério de ordenação (ranking_points, member_count, kdr, total_kills, total_wins)
     * @param page Página (começa em 1)
     * @param pageSize Tamanho da página
     * @return Lista de informações de ranking dos clãs
     */
    public List<ClanRankingInfoDTO> getClanRankings(String criteria, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        
        // Buscar ranking do DAO
        List<ClanRankingInfoDTO> rankings = clanDAO.getClanRankings(criteria, pageSize, offset);
        
        if (rankings.isEmpty()) {
            return rankings;
        }
        
        // Buscar vitórias de eventos para os clãs da página atual
        List<Integer> clanIds = new ArrayList<>();
        for (ClanRankingInfoDTO ranking : rankings) {
            // Extrair ID do clã do cache
            for (Map.Entry<Integer, Clan> entry : clans.entrySet()) {
                if (entry.getValue().getTag().equals(ranking.getTag())) {
                    clanIds.add(entry.getKey());
                    break;
                }
            }
        }
        
        Map<Integer, Map<String, Integer>> eventWins = clanDAO.getEventWinsForClans(clanIds);
        
        // Associar vitórias aos rankings
        for (ClanRankingInfoDTO ranking : rankings) {
            for (Map.Entry<Integer, Clan> entry : clans.entrySet()) {
                if (entry.getValue().getTag().equals(ranking.getTag())) {
                    int clanId = entry.getKey();
                    Map<String, Integer> wins = eventWins.get(clanId);
                    if (wins != null) {
                        ranking.setWins(wins);
                    }
                    break;
                }
            }
        }
        
        return rankings;
    }
    
    /**
     * Adiciona pontos de ranking a um clã.
     * Método público para futuros módulos de eventos.
     * @param clanId ID do clã
     * @param points Pontos a adicionar (pode ser negativo para remover)
     * @param reason Motivo da alteração
     * @return true se a operação foi bem-sucedida
     */
    public boolean addRankingPoints(int clanId, int points, String reason) {
        boolean success = clanDAO.updateRankingPointsAndLog(clanId, points, reason);
        
        if (success) {
            // Atualizar cache em memória
            Clan clan = clans.get(clanId);
            if (clan != null) {
                // Atualizar pontos no cache (será recarregado na próxima inicialização)
                plugin.getLogger().info("Pontos de ranking atualizados para clã " + clan.getTag() + 
                                      ": " + (points > 0 ? "+" : "") + points + " (" + reason + ")");
            }
        }
        
        return success;
    }
    
    /**
     * Registra uma vitória em evento para um clã.
     * Método público para futuros módulos de eventos.
     * @param clanId ID do clã
     * @param eventName Nome do evento
     */
    public void registerWin(int clanId, String eventName) {
        clanDAO.registerEventWin(clanId, eventName);
        
        // Atualizar cache em memória se necessário
        Clan clan = clans.get(clanId);
        if (clan != null) {
            plugin.getLogger().info("Vitória registrada para clã " + clan.getTag() + " no evento: " + eventName);
        }
    }
    
    /**
     * Obtém o total de clãs para cálculo de páginas.
     * @return Total de clãs no sistema
     */
    public int getTotalClans() {
        return clans.size();
    }
    
    /**
     * Obtém informações de ranking de um clã específico.
     * @param clanId ID do clã
     * @return Informações de ranking ou null se não encontrado
     */
    public ClanRankingInfoDTO getClanRankingInfo(int clanId) {
        Clan clan = clans.get(clanId);
        if (clan == null) {
            return null;
        }
        
        // Buscar ranking específico
        List<ClanRankingInfoDTO> rankings = clanDAO.getClanRankings("ranking_points", 1, 0);
        
        // Encontrar o clã específico
        for (ClanRankingInfoDTO ranking : rankings) {
            if (ranking.getTag().equals(clan.getTag())) {
                // Buscar vitórias específicas
                List<Integer> clanIds = Arrays.asList(clanId);
                Map<Integer, Map<String, Integer>> eventWins = clanDAO.getEventWinsForClans(clanIds);
                Map<String, Integer> wins = eventWins.get(clanId);
                if (wins != null) {
                    ranking.setWins(wins);
                }
                return ranking;
            }
        }
        
        return null;
    }
    
    // ===== MÉTODOS DE GERENCIAMENTO DE STATUS ONLINE/OFFLINE =====
    
    /**
     * Define um jogador como online, armazenando seu objeto Player.
     * REFATORADO: Usa player_id como identificador principal
     */
    public void setPlayerOnline(Player player) {
        // REFATORADO: Obter player_id através do IdentityManager
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        if (playerId != -1) {
            onlinePlayers.put(playerId, player);
        }
    }

    /**
     * Define um jogador como offline, removendo seu objeto Player do cache.
     * REFATORADO: Usa player_id como identificador principal
     */
    public void setPlayerOffline(int playerId) {
        onlinePlayers.remove(playerId);
    }

    /**
     * @deprecated Use setPlayerOffline(int playerId) instead
     */
    @Deprecated
    public void setPlayerOffline(UUID playerUUID) {
        // REFATORADO: Converter UUID para player_id
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUUID);
        if (playerId != -1) {
            onlinePlayers.remove(playerId);
        }
    }

    /**
     * Verifica se um jogador está online.
     * REFATORADO: Usa player_id como identificador principal
     * @param playerId O player_id do jogador.
     * @return true se o jogador está online, false caso contrário.
     */
    public boolean isPlayerOnline(int playerId) {
        return onlinePlayers.containsKey(playerId);
    }

    /**
     * @deprecated Use isPlayerOnline(int playerId) instead
     */
    @Deprecated
    public boolean isPlayerOnline(UUID playerUUID) {
        // REFATORADO: Converter UUID para player_id
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUUID);
        if (playerId == -1) {
            return false;
        }
        return onlinePlayers.containsKey(playerId);
    }

    /**
     * Notifica todos os membros online de um clã, com exceções.
     * REFATORADO: Usa player_id como identificador principal
     * Itera apenas sobre o cache de membros online para máxima performance.
     */
    public void notifyClanMembers(Clan clan, String message, int... exclusions) {
        List<Integer> excludedPlayerIds = new ArrayList<>();
        for (int exclusion : exclusions) {
            excludedPlayerIds.add(exclusion);
        }
        
        for (Map.Entry<Integer, Player> entry : onlinePlayers.entrySet()) {
            int playerId = entry.getKey();
            Player onlinePlayer = entry.getValue();
            
            // Pular jogadores excluídos
            if (excludedPlayerIds.contains(playerId)) {
                continue;
            }

            ClanPlayer member = getClanPlayer(playerId);
            // Verificar se o membro online pertence ao clã alvo
            if (member != null && member.hasClan() && member.getClan().equals(clan)) {
                onlinePlayer.sendMessage(message);
            }
        }
    }

    /**
     * @deprecated Use notifyClanMembers(Clan clan, String message, int... exclusions) instead
     */
    @Deprecated
    public void notifyClanMembers(Clan clan, String message, UUID... exclusions) {
        // REFATORADO: Converter UUIDs para player_ids
        List<Integer> excludedPlayerIds = new ArrayList<>();
        for (UUID uuid : exclusions) {
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(uuid);
            if (playerId != -1) {
                excludedPlayerIds.add(playerId);
            }
        }
        
        int[] exclusionsArray = new int[excludedPlayerIds.size()];
        for (int i = 0; i < excludedPlayerIds.size(); i++) {
            exclusionsArray[i] = excludedPlayerIds.get(i);
        }
        notifyClanMembers(clan, message, exclusionsArray);
    }
}
