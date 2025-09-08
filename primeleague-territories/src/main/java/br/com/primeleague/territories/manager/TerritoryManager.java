package br.com.primeleague.territories.manager;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.model.ClanBank;
import br.com.primeleague.territories.model.TerritoryChunk;
import br.com.primeleague.territories.model.TerritoryState;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Gerenciador principal de territórios.
 * Responsável por operações de claiming, cache e integração com outros módulos.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class TerritoryManager {
    
    // Dependências injetadas
    private final Plugin plugin;
    private final BukkitScheduler scheduler;
    private final MySqlTerritoryDAO territoryDAO;
    private final br.com.primeleague.api.ClanService clanService;
    private final br.com.primeleague.api.EconomyService economyService;
    
    // Cache de territórios para performance
    private final Map<String, TerritoryChunk> territoryCache = new ConcurrentHashMap<>();
    
    // Configurações
    private final int maxTerritoriesPerClan;
    private final double maintenanceBaseCost;
    private final double maintenanceScale;
    private final int maintenanceIntervalHours;
    
    public TerritoryManager(Plugin plugin, BukkitScheduler scheduler, MySqlTerritoryDAO territoryDAO, 
                           br.com.primeleague.api.ClanService clanService, br.com.primeleague.api.EconomyService economyService,
                           int maxTerritoriesPerClan, double maintenanceBaseCost, double maintenanceScale, int maintenanceIntervalHours) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.territoryDAO = territoryDAO;
        this.clanService = clanService;
        this.economyService = economyService;
        this.maxTerritoriesPerClan = maxTerritoriesPerClan;
        this.maintenanceBaseCost = maintenanceBaseCost;
        this.maintenanceScale = maintenanceScale;
        this.maintenanceIntervalHours = maintenanceIntervalHours;
        
        // Carregar territórios existentes
        loadAllTerritories();
        
        // Iniciar tarefa de manutenção
        startMaintenanceTask();
    }
    
    /**
     * Reivindica um território para um clã.
     * 
     * @param player Jogador que está reivindicando
     * @param location Localização do chunk
     * @param callback Callback para resultado
     */
    public void claimTerritory(Player player, Location location, TerritoryClaimCallback callback) {
        // Verificar se já está reivindicado
        if (isClaimed(location)) {
            callback.onResult(TerritoryClaimResult.ALREADY_CLAIMED, "Este território já está reivindicado!");
            return;
        }
        
        // Obter clã do jogador via API central
        ClanDTO playerClan = getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) {
            callback.onResult(TerritoryClaimResult.NO_CLAN, "Você precisa estar em um clã para reivindicar territórios!");
            return;
        }
        
        // Verificar permissão
        if (!hasClaimPermission(player, playerClan)) {
            callback.onResult(TerritoryClaimResult.NO_PERMISSION, "Você não tem permissão para reivindicar territórios!");
            return;
        }
        
        // Verificar moral suficiente
        double currentMoral = getClanMoral(playerClan.getId());
        int currentTerritories = getTerritoryCount(playerClan.getId());
        if (currentMoral < (currentTerritories + 1)) {
            callback.onResult(TerritoryClaimResult.INSUFFICIENT_MORAL, 
                "Moral insuficiente! Necessário: " + (currentTerritories + 1) + ", Atual: " + currentMoral);
            return;
        }
        
        // Verificar limite de territórios
        if (currentTerritories >= maxTerritoriesPerClan) {
            callback.onResult(TerritoryClaimResult.LIMIT_EXCEEDED, "Seu clã já atingiu o limite máximo de territórios!");
            return;
        }
        
        // Criar território
        TerritoryChunk territory = new TerritoryChunk();
        territory.setClanId(playerClan.getId());
        territory.setWorldName(location.getWorld().getName());
        territory.setChunkX(location.getChunk().getX());
        territory.setChunkZ(location.getChunk().getZ());
        
        // Salvar no banco
        territoryDAO.createTerritoryAsync(territory, (success) -> {
            if (success) {
                // Adicionar ao cache
                territoryCache.put(territory.getChunkKey(), territory);
                callback.onResult(TerritoryClaimResult.SUCCESS, 
                    "Território reivindicado com sucesso! Coordenadas: " + territory.getChunkX() + ", " + territory.getChunkZ());
            } else {
                callback.onResult(TerritoryClaimResult.DATABASE_ERROR, "Erro interno do servidor!");
            }
        });
    }
    
    /**
     * Remove um território.
     * 
     * @param player Jogador que está removendo
     * @param location Localização do chunk
     * @param callback Callback para resultado
     */
    public void unclaimTerritory(Player player, Location location, TerritoryUnclaimCallback callback) {
        TerritoryChunk territory = getTerritoryAt(location);
        
        if (territory == null) {
            callback.onResult(TerritoryUnclaimResult.NOT_CLAIMED, "Este território não está reivindicado!");
            return;
        }
        
        // Verificar se o jogador pertence ao clã proprietário
        ClanDTO playerClan = getPlayerClan(player.getUniqueId());
        
        if (playerClan == null || playerClan.getId() != territory.getClanId()) {
            callback.onResult(TerritoryUnclaimResult.NO_PERMISSION, "Você não pode remover territórios de outros clãs!");
            return;
        }
        
        // Remover do banco
        territoryDAO.removeTerritoryAsync(territory.getId(), (success) -> {
            if (success) {
                // Remover do cache
                territoryCache.remove(territory.getChunkKey());
                callback.onResult(TerritoryUnclaimResult.SUCCESS, "Território removido com sucesso!");
            } else {
                callback.onResult(TerritoryUnclaimResult.DATABASE_ERROR, "Erro interno do servidor!");
            }
        });
    }
    
    /**
     * Verifica se um chunk está reivindicado.
     * 
     * @param location Localização
     * @return true se está reivindicado
     */
    public boolean isClaimed(Location location) {
        String chunkKey = getChunkKey(location);
        return territoryCache.containsKey(chunkKey);
    }
    
    /**
     * Obtém o território em uma localização.
     * 
     * @param location Localização
     * @return Território ou null
     */
    public TerritoryChunk getTerritoryAt(Location location) {
        String chunkKey = getChunkKey(location);
        return territoryCache.get(chunkKey);
    }
    
    /**
     * Obtém o clã proprietário de um território.
     * 
     * @param location Localização
     * @return Clã proprietário ou null
     */
    public ClanDTO getOwningClan(Location location) {
        TerritoryChunk territory = getTerritoryAt(location);
        if (territory == null) return null;
        
        return getClanById(territory.getClanId());
    }
    
    /**
     * Obtém o número de territórios de um clã.
     * 
     * @param clanId ID do clã
     * @return Número de territórios
     */
    public int getTerritoryCount(int clanId) {
        return (int) territoryCache.values().stream()
            .filter(t -> t.getClanId() == clanId)
            .count();
    }
    
    /**
     * Obtém todos os territórios de um clã.
     * 
     * @param clanId ID do clã
     * @return Lista de territórios
     */
    public List<TerritoryChunk> getClanTerritories(int clanId) {
        return territoryCache.values().stream()
            .filter(t -> t.getClanId() == clanId)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Verifica se um jogador tem permissão para interagir com um território.
     * 
     * @param player Jogador
     * @param location Localização
     * @return true se tem permissão
     */
    public boolean hasTerritoryPermission(Player player, Location location) {
        TerritoryChunk territory = getTerritoryAt(location);
        if (territory == null) return true; // Território neutro
        
        // Verificar se o jogador está no clã proprietário
        ClanDTO playerClan = getPlayerClan(player.getUniqueId());
        
        if (playerClan == null) return false;
        
        return playerClan.getId() == territory.getClanId();
    }
    
    /**
     * Obtém o estado de um clã (Fortificado/Vulnerável).
     * 
     * @param clanId ID do clã
     * @return Estado do território
     */
    public TerritoryState getTerritoryState(int clanId) {
        ClanDTO clan = getClanById(clanId);
        if (clan == null) return TerritoryState.VULNERAVEL;
        
        int territoryCount = getTerritoryCount(clanId);
        return getClanMoral(clan.getId()) >= territoryCount ? TerritoryState.FORTIFICADO : TerritoryState.VULNERAVEL;
    }
    
    /**
     * Verifica se um clã está vulnerável (moral < territórios).
     */
    public boolean isClanVulnerable(int clanId) {
        ClanDTO clan = getClanById(clanId);
        if (clan == null) return false;
        
        int territoryCount = getTerritoryCount(clanId);
        return getClanMoral(clan.getId()) < territoryCount;
    }
    
    /**
     * Obtém o banco de um clã.
     * 
     * @param clanId ID do clã
     * @param callback Callback com o banco
     */
    public void getClanBank(int clanId, Consumer<ClanBank> callback) {
        territoryDAO.getClanBankAsync(clanId, callback);
    }
    
    /**
     * Deposita dinheiro no banco do clã.
     * 
     * @param clanId ID do clã
     * @param amount Quantia
     * @param callback Callback de resultado
     */
    public void depositToClanBank(int clanId, BigDecimal amount, Consumer<Boolean> callback) {
        territoryDAO.depositToClanBankAsync(clanId, amount, callback);
    }
    
    /**
     * Saca dinheiro do banco do clã.
     * 
     * @param clanId ID do clã
     * @param amount Quantia
     * @param callback Callback de resultado
     */
    public void withdrawFromClanBank(int clanId, BigDecimal amount, Consumer<Boolean> callback) {
        territoryDAO.withdrawFromClanBankAsync(clanId, amount, callback);
    }
    
    /**
     * Calcula o custo de manutenção de um clã.
     * 
     * @param clanId ID do clã
     * @return Custo de manutenção
     */
    public double getMaintenanceCost(int clanId) {
        int territoryCount = getTerritoryCount(clanId);
        return maintenanceBaseCost * Math.pow(maintenanceScale, territoryCount - 1);
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void loadAllTerritories() {
        plugin.getLogger().info("Carregando todos os territórios do banco de dados...");
        
        // Placeholder temporário - carregamento de territórios será implementado quando DAO estiver completo
        plugin.getLogger().info("Carregamento de territórios será implementado quando DAO estiver completo.");
    }
    
    private void startMaintenanceTask() {
        long intervalTicks = TimeUnit.HOURS.toSeconds(maintenanceIntervalHours) * 20L;
        
        scheduler.runTaskTimer(plugin, () -> {
            // Executar verificação de manutenção
            checkAllClansMaintenance();
        }, intervalTicks, intervalTicks);
    }
    
    private void checkAllClansMaintenance() {
        plugin.getLogger().info("Iniciando verificação de manutenção de todos os clãs...");
        
        // Obter todos os clãs únicos que possuem territórios
        Set<Integer> clanIds = new HashSet<>();
        for (TerritoryChunk territory : territoryCache.values()) {
            clanIds.add(territory.getClanId());
        }
        
        for (int clanId : clanIds) {
            checkClanMaintenance(clanId);
        }
        
        plugin.getLogger().info("Verificação de manutenção concluída para " + clanIds.size() + " clãs.");
    }
    
    private void checkClanMaintenance(int clanId) {
        try {
            ClanDTO clan = getClanById(clanId);
            if (clan == null) {
                plugin.getLogger().warning("Clã " + clanId + " não encontrado durante verificação de manutenção.");
                return;
            }
            
            int territoryCount = getTerritoryCount(clanId);
            if (territoryCount == 0) {
                return; // Clã não possui territórios
            }
            
            double maintenanceCost = getMaintenanceCost(clanId);
            getClanBank(clanId, (bank) -> {
                if (bank == null || !bank.hasEnoughBalance(java.math.BigDecimal.valueOf(maintenanceCost))) {
                    // Clã não tem saldo suficiente - remover território mais antigo
                    removeOldestTerritory(clanId);
                    plugin.getLogger().info("Clã " + clanId + " perdeu território por falta de saldo para manutenção.");
                } else {
                    // Debitar custo de manutenção
                    withdrawFromClanBank(clanId, java.math.BigDecimal.valueOf(maintenanceCost), (success) -> {
                        if (success) {
                            plugin.getLogger().info("Manutenção debitada do clã " + clanId + ": $" + maintenanceCost);
                        } else {
                            plugin.getLogger().warning("Falha ao debitar manutenção do clã " + clanId);
                        }
                    });
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar manutenção do clã " + clanId + ": " + e.getMessage());
        }
    }
    
    private void removeOldestTerritory(int clanId) {
        TerritoryChunk oldestTerritory = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (TerritoryChunk territory : territoryCache.values()) {
            if (territory.getClanId() == clanId && territory.getClaimedAt().getTime() < oldestTime) {
                oldestTerritory = territory;
                oldestTime = territory.getClaimedAt().getTime();
            }
        }
        
        if (oldestTerritory != null) {
            // Remover do cache
            String key = oldestTerritory.getWorldName() + ":" + oldestTerritory.getChunkX() + ":" + oldestTerritory.getChunkZ();
            territoryCache.remove(key);
            
            // Placeholder temporário - remoção do banco será implementada quando DAO estiver completo
            plugin.getLogger().info("Território mais antigo removido do clã " + clanId + " por falta de manutenção.");
        }
    }
    
    private boolean hasClaimPermission(Player player, ClanDTO clan) {
        // Verificar se o jogador tem permissão para reivindicar territórios
        if (clan == null) return false;
        
        // Líderes e co-líderes podem reivindicar territórios
        return player.hasPermission("primeleague.territories.claim") || 
               player.hasPermission("primeleague.clans.leader") ||
               player.hasPermission("primeleague.clans.coleader");
    }
    
    private boolean hasUnclaimPermission(Player player, ClanDTO clan) {
        // Verificar se o jogador tem permissão para remover territórios
        if (clan == null) return false;
        
        // Apenas líderes podem remover territórios
        return player.hasPermission("primeleague.territories.unclaim") || 
               player.hasPermission("primeleague.clans.leader");
    }
    
    private String getChunkKey(Location location) {
        return location.getWorld().getName() + ":" + location.getChunk().getX() + ":" + location.getChunk().getZ();
    }
    
    // ==================== API METHODS ====================
    
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
     * Obtém moral do clã via API injetada.
     */
    private double getClanMoral(int clanId) {
        try {
            if (clanService == null) {
                plugin.getLogger().warning("ClanService não está disponível!");
                return 0.0;
            }
            return clanService.getClanMoral(clanId);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter moral do clã " + clanId + ": " + e.getMessage());
            return 0.0; // Retorna 0 se houver erro, impedindo claims
        }
    }
    
    public void logTerritoryAction(int clanId, String action, String details) {
        plugin.getLogger().info("Territory Action: Clan " + clanId + " " + action + " " + details);
    }
    
    // ==================== CALLBACK INTERFACES ====================
    
    public interface TerritoryClaimCallback {
        void onResult(TerritoryClaimResult result, String message);
    }
    
    public interface TerritoryUnclaimCallback {
        void onResult(TerritoryUnclaimResult result, String message);
    }
    
    public enum TerritoryClaimResult {
        SUCCESS,
        ALREADY_CLAIMED,
        NO_CLAN,
        NO_PERMISSION,
        INSUFFICIENT_MORAL,
        LIMIT_EXCEEDED,
        DATABASE_ERROR
    }
    
    public enum TerritoryUnclaimResult {
        SUCCESS,
        NOT_CLAIMED,
        NO_PERMISSION,
        DATABASE_ERROR
    }
}