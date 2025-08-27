package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IdentityManager - O coração da arquitetura de segurança do Prime League.
 * 
 * Responsabilidades:
 * 1. Gerar e gerenciar player_id (INT) único e estável para cada jogador
 * 2. Mapear player_id ↔ UUID (Bukkit) ↔ playerName
 * 3. Garantir que toda identificação de jogador use player_id como fonte da verdade
 * 4. Prevenir roubo de identidade e corrupção de dados
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class IdentityManager {
    
    private final PrimeLeagueCore plugin;
    private final DataManager dataManager;
    
    // Mapeamentos de identidade
    private final Map<UUID, Integer> uuidToPlayerIdMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> nameToPlayerIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> playerIdToUuidMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> playerIdToNameMap = new ConcurrentHashMap<>();
    
    // Cache de jogadores online com player_id
    private final Map<Integer, Player> onlinePlayersById = new ConcurrentHashMap<>();
    
    public IdentityManager(PrimeLeagueCore plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }
    
    /**
     * Obtém o player_id único e estável de um jogador.
     * Este é o método principal da arquitetura de segurança.
     * 
     * @param player Jogador do Bukkit
     * @return player_id (INT) único e estável
     * @throws IllegalStateException se o jogador não estiver registrado
     */
    public int getPlayerId(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player não pode ser null");
        }
        
        UUID bukkitUuid = player.getUniqueId();
        String playerName = player.getName();
        
        // Verificar cache primeiro
        Integer cachedPlayerId = uuidToPlayerIdMap.get(bukkitUuid);
        if (cachedPlayerId != null) {
            return cachedPlayerId;
        }
        
        // Buscar no banco de dados
        Integer playerId = dataManager.getPlayerIdFromDatabase(bukkitUuid, playerName);
        if (playerId != null) {
            // Adicionar ao cache
            addToCache(playerId, bukkitUuid, playerName);
            return playerId;
        }
        
        // Jogador não encontrado - erro crítico
        throw new IllegalStateException(
            "Jogador não registrado no sistema: " + playerName + " (UUID: " + bukkitUuid + "). " +
            "O jogador deve fazer login primeiro para obter um player_id."
        );
    }
    
    /**
     * Obtém o player_id de um jogador pelo nome.
     * 
     * @param playerName Nome do jogador
     * @return player_id ou null se não encontrado
     */
    public Integer getPlayerIdByName(String playerName) {
        if (playerName == null) {
            return null;
        }
        
        return nameToPlayerIdMap.get(playerName);
    }
    
    /**
     * Obtém o player_id de um jogador pelo UUID do Bukkit.
     * 
     * @param bukkitUuid UUID do Bukkit
     * @return player_id ou null se não encontrado
     */
    public Integer getPlayerIdByUuid(UUID bukkitUuid) {
        if (bukkitUuid == null) {
            return null;
        }
        
        return uuidToPlayerIdMap.get(bukkitUuid);
    }
    
    /**
     * Obtém o UUID do Bukkit de um player_id.
     * 
     * @param playerId ID numérico do jogador
     * @return UUID do Bukkit ou null se não encontrado
     */
    public UUID getUuidByPlayerId(int playerId) {
        return playerIdToUuidMap.get(playerId);
    }
    
    /**
     * Obtém o nome do jogador de um player_id.
     * 
     * @param playerId ID numérico do jogador
     * @return Nome do jogador ou null se não encontrado
     */
    public String getNameByPlayerId(int playerId) {
        return playerIdToNameMap.get(playerId);
    }
    
    /**
     * Registra um jogador no sistema de identidade.
     * Chamado durante o login para criar o mapeamento.
     * 
     * @param player Jogador do Bukkit
     * @param playerId ID numérico do jogador
     */
    public void registerPlayer(Player player, int playerId) {
        if (player == null) {
            throw new IllegalArgumentException("Player não pode ser null");
        }
        
        UUID bukkitUuid = player.getUniqueId();
        String playerName = player.getName();
        
        // Adicionar ao cache
        addToCache(playerId, bukkitUuid, playerName);
        
        // Adicionar à lista de jogadores online
        onlinePlayersById.put(playerId, player);
        
        plugin.getLogger().info("🔐 [IDENTITY] Jogador registrado: " + playerName + 
                               " (player_id: " + playerId + ", UUID: " + bukkitUuid + ")");
    }
    
    /**
     * Remove um jogador do sistema de identidade.
     * Chamado durante o logout para limpar o cache.
     * 
     * @param player Jogador do Bukkit
     */
    public void unregisterPlayer(Player player) {
        if (player == null) {
            return;
        }
        
        UUID bukkitUuid = player.getUniqueId();
        String playerName = player.getName();
        
        // Remover da lista de jogadores online
        Integer playerId = uuidToPlayerIdMap.get(bukkitUuid);
        if (playerId != null) {
            onlinePlayersById.remove(playerId);
        }
        
        // Limpar cache (manter mapeamentos para consultas futuras)
        // uuidToPlayerIdMap.remove(bukkitUuid); // Não remover - pode ser consultado offline
        // nameToPlayerIdMap.remove(playerName); // Não remover - pode ser consultado offline
        
        plugin.getLogger().info("🔓 [IDENTITY] Jogador desregistrado: " + playerName + 
                               " (player_id: " + playerId + ", UUID: " + bukkitUuid + ")");
    }
    
    /**
     * Verifica se um jogador está registrado no sistema.
     * 
     * @param player Jogador do Bukkit
     * @return true se registrado, false caso contrário
     */
    public boolean isPlayerRegistered(Player player) {
        if (player == null) {
            return false;
        }
        
        return uuidToPlayerIdMap.containsKey(player.getUniqueId());
    }
    
    /**
     * Obtém um jogador online pelo player_id.
     * 
     * @param playerId ID numérico do jogador
     * @return Player do Bukkit ou null se offline
     */
    public Player getOnlinePlayer(int playerId) {
        return onlinePlayersById.get(playerId);
    }
    
    /**
     * Obtém todos os jogadores online com seus player_ids.
     * 
     * @return Map de player_id para Player
     */
    public Map<Integer, Player> getOnlinePlayers() {
        return new ConcurrentHashMap<>(onlinePlayersById);
    }
    
    /**
     * Adiciona um mapeamento ao cache interno.
     * 
     * @param playerId ID numérico do jogador
     * @param bukkitUuid UUID do Bukkit
     * @param playerName Nome do jogador
     */
    private void addToCache(int playerId, UUID bukkitUuid, String playerName) {
        uuidToPlayerIdMap.put(bukkitUuid, playerId);
        nameToPlayerIdMap.put(playerName, playerId);
        playerIdToUuidMap.put(playerId, bukkitUuid);
        playerIdToNameMap.put(playerId, playerName);
    }
    
    /**
     * Obtém estatísticas do manager para debugging.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        return String.format("IdentityManager Stats - " +
                           "UUID Mappings: %d, " +
                           "Name Mappings: %d, " +
                           "Online Players: %d",
                           uuidToPlayerIdMap.size(),
                           nameToPlayerIdMap.size(),
                           onlinePlayersById.size());
    }
    
    /**
     * Limpa todo o cache (apenas para debugging/emergências).
     */
    public void clearCache() {
        uuidToPlayerIdMap.clear();
        nameToPlayerIdMap.clear();
        playerIdToUuidMap.clear();
        playerIdToNameMap.clear();
        onlinePlayersById.clear();
        plugin.getLogger().warning("🗑️ [IDENTITY] Cache limpo manualmente");
    }
}
