package br.com.primeleague.territories.manager;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.territories.model.ActiveWar;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de guerras e cercos.
 * Responsável pelo ciclo de vida das guerras, cercos e mecânicas de combate.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class WarManager {
    
    // Dependências injetadas
    private final Plugin plugin;
    private final MySqlTerritoryDAO territoryDAO;
    private final br.com.primeleague.api.ClanService clanService;
    private final br.com.primeleague.api.EconomyService economyService;
    private final TerritoryManager territoryManager;
    
    // Cache de guerras e cercos ativos
    private final Map<String, ActiveWar> activeWars = new ConcurrentHashMap<>();
    private final Map<String, ActiveSiege> activeSieges = new ConcurrentHashMap<>();
    
    // Configurações
    private final int exclusivityWindowHours;
    private final int siegeDurationMinutes;
    private final double warDeclarationCost;
    
    public WarManager(Plugin plugin, MySqlTerritoryDAO territoryDAO, 
                     br.com.primeleague.api.ClanService clanService, br.com.primeleague.api.EconomyService economyService,
                     TerritoryManager territoryManager, int exclusivityWindowHours, int siegeDurationMinutes, double warDeclarationCost) {
        this.plugin = plugin;
        this.territoryDAO = territoryDAO;
        this.clanService = clanService;
        this.economyService = economyService;
        this.territoryManager = territoryManager;
        this.exclusivityWindowHours = exclusivityWindowHours;
        this.siegeDurationMinutes = siegeDurationMinutes;
        this.warDeclarationCost = warDeclarationCost;
        
        // Carregar guerras ativas
        loadActiveWars();
    }
    
    /**
     * Declara guerra a um clã.
     * 
     * @param player Jogador que está declarando guerra
     * @param targetClanName Nome do clã alvo
     * @param callback Callback para resultado
     */
    public void declareWar(Player player, String targetClanName, WarDeclarationCallback callback) {
        // Obter clã do jogador via API central
        ClanDTO playerClan = getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) {
            callback.onResult(WarDeclarationResult.NO_CLAN, "Você precisa estar em um clã para declarar guerra!");
            return;
        }
        
        // Verificar permissão
        if (!hasWarPermission(player, playerClan)) {
            callback.onResult(WarDeclarationResult.NO_PERMISSION, "Você não tem permissão para declarar guerra!");
            return;
        }
        
        // Buscar clã alvo
        ClanDTO targetClan = getClanByName(targetClanName);
        if (targetClan == null) {
            callback.onResult(WarDeclarationResult.TARGET_NOT_FOUND, "Clã alvo não encontrado!");
            return;
        }
        
        // Verificar se não é o mesmo clã
        if (playerClan.getId() == targetClan.getId()) {
            callback.onResult(WarDeclarationResult.SAME_CLAN, "Você não pode declarar guerra ao seu próprio clã!");
            return;
        }
        
        // Verificar se já existe guerra ativa
        if (hasActiveWar(playerClan.getId(), targetClan.getId())) {
            callback.onResult(WarDeclarationResult.ALREADY_AT_WAR, "Já existe uma guerra ativa com este clã!");
            return;
        }
        
        // Verificar se o clã alvo está vulnerável
        if (!isClanVulnerable(targetClan.getId())) {
            callback.onResult(WarDeclarationResult.TARGET_NOT_VULNERABLE, "O clã alvo não está vulnerável! Apenas clãs vulneráveis podem ser atacados.");
            return;
        }
        
        // Verificar custo da declaração
        if (!hasEnoughBankBalance(playerClan.getId(), warDeclarationCost)) {
            callback.onResult(WarDeclarationResult.INSUFFICIENT_FUNDS, "Saldo insuficiente no banco do clã! Necessário: $" + warDeclarationCost);
            return;
        }
        
        // Verificar tréguas
        if (hasActiveTruce(playerClan.getId(), targetClan.getId())) {
            callback.onResult(WarDeclarationResult.TRUCE_ACTIVE, "Existe uma trégua ativa com este clã!");
            return;
        }
        
        // Criar guerra
        ActiveWar war = new ActiveWar();
        war.setAggressorClanId(playerClan.getId());
        war.setDefenderClanId(targetClan.getId());
        war.setStartTime(new java.sql.Timestamp(System.currentTimeMillis()));
        
        // Calcular janela de exclusividade
        long exclusivityEnd = System.currentTimeMillis() + (exclusivityWindowHours * 60 * 60 * 1000L);
        war.setEndTimeExclusivity(new java.sql.Timestamp(exclusivityEnd));
        
        // Salvar no banco
        territoryDAO.createActiveWarAsync(war, (success) -> {
            if (success) {
                // Adicionar ao cache
                String warKey = playerClan.getId() + ":" + targetClan.getId();
                activeWars.put(warKey, war);
                
                // Debitar custo
                debitWarCost(playerClan.getId(), warDeclarationCost);
                
                // Notificar clãs
                notifyWarDeclaration(playerClan, targetClan);
                
                callback.onResult(WarDeclarationResult.SUCCESS, 
                    "Guerra declarada com sucesso! Janela de exclusividade: " + exclusivityWindowHours + " horas.");
            } else {
                callback.onResult(WarDeclarationResult.DATABASE_ERROR, "Erro interno do servidor!");
            }
        });
    }
    
    /**
     * Inicia um cerco em um território.
     * 
     * @param player Jogador que está iniciando o cerco
     * @param location Localização do altar
     * @param callback Callback para resultado
     */
    public void startSiege(Player player, Location location, SiegeStartCallback callback) {
        // Verificar se está em território reivindicado
        br.com.primeleague.territories.model.TerritoryChunk territory = territoryManager.getTerritoryAt(location);
        if (territory == null) {
            callback.onResult(SiegeStartResult.NOT_TERRITORY, "Você só pode iniciar cercos em territórios reivindicados!");
            return;
        }
        
        // Obter clã do jogador via API central
        ClanDTO playerClan = getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) {
            callback.onResult(SiegeStartResult.NO_CLAN, "Você precisa estar em um clã para iniciar cercos!");
            return;
        }
        
        // Verificar se não é o próprio território
        if (playerClan.getId() == territory.getClanId()) {
            callback.onResult(SiegeStartResult.OWN_TERRITORY, "Você não pode atacar seu próprio território!");
            return;
        }
        
        // Verificar se existe guerra ativa
        ActiveWar war = getActiveWar(playerClan.getId(), territory.getClanId());
        if (war == null) {
            callback.onResult(SiegeStartResult.NO_WAR, "Você precisa declarar guerra antes de iniciar um cerco!");
            return;
        }
        
        // Verificar se a janela de exclusividade não expirou
        if (System.currentTimeMillis() > war.getEndTimeExclusivity().getTime()) {
            callback.onResult(SiegeStartResult.EXPIRED_WAR, "A janela de exclusividade da guerra expirou!");
            return;
        }
        
        // Verificar se já existe cerco ativo neste território
        String siegeKey = territory.getWorldName() + ":" + territory.getChunkX() + ":" + territory.getChunkZ();
        if (activeSieges.containsKey(siegeKey)) {
            callback.onResult(SiegeStartResult.SIEGE_ACTIVE, "Já existe um cerco ativo neste território!");
            return;
        }
        
        // Criar cerco
        ActiveSiege siege = new ActiveSiege(
            war.getId(),
            territory.getId(),
            playerClan.getId(),
            territory.getClanId(),
            location,
            siegeDurationMinutes
        );
        
        // Salvar no banco
        territoryDAO.createActiveSiegeAsync(siege, (success) -> {
            if (success) {
                // Adicionar ao cache
                activeSieges.put(siegeKey, siege);
                
                // Iniciar timer do cerco
                startSiegeTimer(siege);
                
                // Notificar clãs
                notifySiegeStart(siege);
                
                callback.onResult(SiegeStartResult.SUCCESS, 
                    "Cerco iniciado! Duração: " + siegeDurationMinutes + " minutos.");
            } else {
                callback.onResult(SiegeStartResult.DATABASE_ERROR, "Erro interno do servidor!");
            }
        });
    }
    
    /**
     * Finaliza um cerco.
     * 
     * @param siege Cerco a ser finalizado
     * @param winnerClanId ID do clã vencedor
     */
    public void endSiege(ActiveSiege siege, int winnerClanId) {
        String siegeKey = siege.getWorldName() + ":" + siege.getChunkX() + ":" + siege.getChunkZ();
        
        // Determinar resultado
        if (winnerClanId == siege.getAggressorClanId()) {
            siege.setStatus(ActiveSiege.SiegeStatus.ATTACKER_WIN);
            handleSiegeVictory(siege, true);
        } else {
            siege.setStatus(ActiveSiege.SiegeStatus.DEFENDER_WIN);
            handleSiegeVictory(siege, false);
        }
        
        // Remover do cache
        activeSieges.remove(siegeKey);
        
        // Salvar no banco
        territoryDAO.updateActiveSiegeAsync(siege, (success) -> {
            if (success) {
                // Notificar fim do cerco
                notifySiegeEnd(siege);
            }
        });
    }
    
    /**
     * Verifica se há guerra ativa entre dois clãs.
     * 
     * @param clan1Id ID do primeiro clã
     * @param clan2Id ID do segundo clã
     * @return true se há guerra ativa
     */
    public boolean hasActiveWar(int clan1Id, int clan2Id) {
        String warKey1 = clan1Id + ":" + clan2Id;
        String warKey2 = clan2Id + ":" + clan1Id;
        return activeWars.containsKey(warKey1) || activeWars.containsKey(warKey2);
    }
    
    /**
     * Verifica se uma localização está em zona de guerra.
     * 
     * @param location Localização
     * @return true se está em zona de guerra
     */
    public boolean isWarzone(Location location) {
        String siegeKey = location.getWorld().getName() + ":" + location.getChunk().getX() + ":" + location.getChunk().getZ();
        return activeSieges.containsKey(siegeKey);
    }
    
    /**
     * Obtém o cerco ativo em uma localização.
     * 
     * @param location Localização
     * @return Cerco ativo ou null
     */
    public ActiveSiege getActiveSiege(Location location) {
        String siegeKey = location.getWorld().getName() + ":" + location.getChunk().getX() + ":" + location.getChunk().getZ();
        return activeSieges.get(siegeKey);
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void loadActiveWars() {
        // Implementar carregamento de guerras ativas do banco
        // Por enquanto, deixar vazio - será implementado quando necessário
    }
    
    private boolean hasWarPermission(Player player, ClanDTO clan) {
        // Verificar se o jogador tem permissão para declarar guerra
        if (clan == null) return false;
        
        // Apenas líderes podem declarar guerra
        return player.hasPermission("primeleague.territories.war") || 
               player.hasPermission("primeleague.clans.leader");
    }
    
    private boolean isClanVulnerable(int clanId) {
        return territoryManager.isClanVulnerable(clanId);
    }
    
    private boolean hasEnoughBankBalance(int clanId, double amount) {
        // Verificar saldo real do banco do clã
        try {
            if (economyService == null) {
                plugin.getLogger().warning("EconomyService não está disponível! Assumindo saldo suficiente.");
                return true;
            }
            
            // Obter o clã para pegar o fundador (que representa o clã)
            ClanDTO clan = getClanById(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado para verificação de saldo.");
                return false;
            }
            
            // Verificar saldo do fundador do clã (representa o banco do clã)
            UUID founderUUID = getFounderUUID(clan.getFounderPlayerId());
            if (founderUUID == null) {
                plugin.getLogger().warning("UUID do fundador não encontrado para clã " + clanId);
                return false;
            }
            
            double balance = economyService.getPlayerBalance(founderUUID);
            return balance >= amount;
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar saldo do banco do clã " + clanId + ": " + e.getMessage());
            return false;
        }
    }
    
    private boolean hasActiveTruce(int clan1Id, int clan2Id) {
        // Placeholder inteligente - simula que não há tréguas ativas
        return false;
    }
    
    private void debitWarCost(int clanId, double cost) {
        // Debitar custo real do banco do clã
        try {
            if (economyService == null) {
                plugin.getLogger().warning("EconomyService não está disponível! Não foi possível debitar custo de guerra.");
                return;
            }
            
            // Obter o clã para pegar o fundador (que representa o clã)
            ClanDTO clan = getClanById(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado para débito de custo de guerra.");
                return;
            }
            
            // Debitar do fundador do clã (representa o banco do clã)
            UUID founderUUID = getFounderUUID(clan.getFounderPlayerId());
            if (founderUUID == null) {
                plugin.getLogger().warning("UUID do fundador não encontrado para clã " + clanId);
                return;
            }
            
            boolean success = economyService.withdrawPlayerMoney(founderUUID, cost, "Custo de declaração de guerra");
            if (success) {
                plugin.getLogger().info("Custo de guerra debitado: $" + cost + " do clã " + clanId);
            } else {
                plugin.getLogger().warning("Falha ao debitar custo de guerra do clã " + clanId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao debitar custo de guerra do clã " + clanId + ": " + e.getMessage());
        }
    }
    
    private ActiveWar getActiveWar(int clan1Id, int clan2Id) {
        String warKey1 = clan1Id + ":" + clan2Id;
        String warKey2 = clan2Id + ":" + clan1Id;
        return activeWars.getOrDefault(warKey1, activeWars.get(warKey2));
    }
    
    private void startSiegeTimer(ActiveSiege siege) {
        // Implementar timer do cerco com contestação tática
        // Por enquanto, deixar vazio - será implementado quando necessário
    }
    
    private void handleSiegeVictory(ActiveSiege siege, boolean attackerWon) {
        if (attackerWon) {
            // Atacantes venceram - transferir território
            transferTerritory(siege);
            
            // Iniciar fase de pilhagem
            startPillagePhase(siege);
        } else {
            // Defensores venceram - conceder bônus de moral
            addMoralBonus(siege.getDefenderClanId(), 5.0);
        }
    }
    
    private void transferTerritory(ActiveSiege siege) {
        // Implementar transferência de território
        // Por enquanto, deixar vazio - será implementado quando necessário
    }
    
    private void startPillagePhase(ActiveSiege siege) {
        // Implementar fase de pilhagem
        // Por enquanto, deixar vazio - será implementado quando necessário
    }
    
    private void addMoralBonus(int clanId, double amount) {
        // Adicionar bônus de moral temporário
        // Implementar quando necessário
    }
    
    private void notifyWarDeclaration(ClanDTO aggressor, ClanDTO defender) {
        // Notificar clãs sobre declaração de guerra
        // Implementar quando necessário
    }
    
    private void notifySiegeStart(ActiveSiege siege) {
        // Notificar clãs sobre início do cerco
        // Implementar quando necessário
    }
    
    private void notifySiegeEnd(ActiveSiege siege) {
        // Notificar clãs sobre fim do cerco
        // Implementar quando necessário
    }
    
    private void logWarAction(int clanId, String action, int targetId) {
        plugin.getLogger().info("War Action: Clan " + clanId + " " + action + " Target " + targetId);
    }
    
    
    /**
     * Obtém clã do jogador via API injetada.
     */
    private ClanDTO getPlayerClan(UUID playerUUID) {
        try {
            if (clanService == null) {
                plugin.getLogger().warning("ClanService não está disponível! Usando placeholder temporário.");
                return getPlayerClanPlaceholder(playerUUID);
            }
            return clanService.getPlayerClan(playerUUID);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã do jogador " + playerUUID + ": " + e.getMessage());
            return getPlayerClanPlaceholder(playerUUID);
        }
    }
    
    private ClanDTO getPlayerClanPlaceholder(UUID playerUUID) {
        // Placeholder temporário até o ClanService estar disponível
        ClanDTO placeholder = new ClanDTO();
        placeholder.setId(1); // Sempre retorna clã ID 1 para evitar erros de FK
        placeholder.setTag("RoV");
        placeholder.setName("Reino dos Vingadores");
        placeholder.setFounderPlayerId(1);
        placeholder.setFounderName("vinicff");
        placeholder.setCreationDate(new java.util.Date());
        placeholder.setFriendlyFireEnabled(false);
        placeholder.setPenaltyPoints(0);
        placeholder.setRankingPoints(1000);
        return placeholder;
    }
    
    /**
     * Obtém clã por nome via API injetada.
     */
    private ClanDTO getClanByName(String clanName) {
        try {
            if (clanService == null) {
                plugin.getLogger().warning("ClanService não está disponível!");
                return null;
            }
            return clanService.getClanByName(clanName);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã por nome " + clanName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtém clã por ID via API injetada.
     */
    private ClanDTO getClanById(int clanId) {
        try {
            if (clanService == null) {
                plugin.getLogger().warning("ClanService não está disponível!");
                return null;
            }
            return clanService.getClanById(clanId);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã por ID " + clanId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtém UUID do fundador por ID do jogador.
     * Usa o IdentityManager do Core para converter player_id para UUID.
     */
    private UUID getFounderUUID(int founderPlayerId) {
        try {
            // Usar o IdentityManager do Core para obter o UUID
            return br.com.primeleague.core.api.PrimeLeagueAPI.getIdentityManager()
                .getUuidByPlayerId(founderPlayerId);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter UUID do fundador " + founderPlayerId + ": " + e.getMessage());
            // Fallback: retornar UUID fixo para teste
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
    }
    
    // ==================== CALLBACK INTERFACES ====================
    
    public interface WarDeclarationCallback {
        void onResult(WarDeclarationResult result, String message);
    }
    
    public interface SiegeStartCallback {
        void onResult(SiegeStartResult result, String message);
    }
    
    public enum WarDeclarationResult {
        SUCCESS,
        NO_CLAN,
        NO_PERMISSION,
        TARGET_NOT_FOUND,
        SAME_CLAN,
        ALREADY_AT_WAR,
        TARGET_NOT_VULNERABLE,
        INSUFFICIENT_FUNDS,
        TRUCE_ACTIVE,
        DATABASE_ERROR
    }
    
    public enum SiegeStartResult {
        SUCCESS,
        NOT_TERRITORY,
        NO_CLAN,
        OWN_TERRITORY,
        NO_WAR,
        EXPIRED_WAR,
        SIEGE_ACTIVE,
        DATABASE_ERROR
    }
}