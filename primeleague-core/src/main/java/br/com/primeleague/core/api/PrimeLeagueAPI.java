package br.com.primeleague.core.api;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.managers.IdentityManager;
// PlayerProfileManager removido - consolidado no DataManager
import br.com.primeleague.core.managers.MessageManager;
import br.com.primeleague.core.managers.WhitelistManager;
import br.com.primeleague.core.managers.EconomyManager;
import br.com.primeleague.core.managers.DonorManager;
import br.com.primeleague.core.managers.PermissionManager;
import br.com.primeleague.core.services.TagManager;
import br.com.primeleague.core.services.DAOServiceRegistry;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.models.PlayerGroup;
import br.com.primeleague.core.util.UUIDUtils;
import br.com.primeleague.api.ClanServiceRegistry;
import br.com.primeleague.api.EconomyServiceRegistry;
import br.com.primeleague.api.IdentityServiceRegistry;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PrimeLeagueAPI {

    private static boolean initialized = false;
    private static DataManager dataManager;
    private static IdentityManager identityManager;
    // PlayerProfileManager removido - consolidado no DataManager
    private static MessageManager messageManager;
    private static WhitelistManager whitelistManager;
    private static TagManager tagManager;
    private static EconomyManager economyManager;
    private static DonorManager donorManager;
    private static ProfileProvider provider;
    private static PermissionManager permissionManager;
    private static DAOServiceRegistry daoServiceRegistry;

    private PrimeLeagueAPI() {}

    public static void initialize(PrimeLeagueCore core) {
        if (initialized) {
            throw new IllegalStateException("PrimeLeagueAPI já inicializada");
        }
        dataManager = core.getDataManager();
        identityManager = core.getIdentityManager();
        // PlayerProfileManager removido - consolidado no DataManager
        messageManager = MessageManager.getInstance();
        whitelistManager = new WhitelistManager(core);
        tagManager = core.getTagManager();
        economyManager = core.getEconomyManager();
        donorManager = core.getDonorManager();
        permissionManager = core.getPermissionManager();
        daoServiceRegistry = core.getDAOServiceRegistry();
        initialized = true;
    }

    private static void ensureInit() {
        if (!initialized) {
            throw new IllegalStateException("PrimeLeagueAPI não inicializada (Core não habilitado)");
        }
    }

    public static DataManager getDataManager() {
        ensureInit();
        return dataManager;
    }
    
    public static IdentityManager getIdentityManager() {
        ensureInit();
        return identityManager;
    }
    
    // PlayerProfileManager removido - consolidado no DataManager
    
    public static MessageManager getMessageManager() {
        ensureInit();
        return messageManager;
    }
    
    public static WhitelistManager getWhitelistManager() {
        ensureInit();
        return whitelistManager;
    }
    
    public static TagManager getTagManager() {
        ensureInit();
        return tagManager;
    }
    
    public static EconomyManager getEconomyManager() {
        ensureInit();
        return economyManager;
    }
    
    public static DonorManager getDonorManager() {
        ensureInit();
        return donorManager;
    }
    
    public static PermissionManager getPermissionManager() {
        ensureInit();
        return permissionManager;
    }

    public static PlayerProfile getPlayerProfile(Player player) {
        ensureInit();
        return dataManager.getPlayerProfile(player);
    }

    public static PlayerProfile getPlayerProfile(UUID uuid) {
        ensureInit();
        return dataManager.getPlayerProfile(uuid);
    }

    /**
     * Obtém o perfil de um jogador pelo nome.
     *
     * @param playerName O nome do jogador.
     * @return O PlayerProfile do jogador, ou null se não for encontrado.
     */
    public static PlayerProfile getProfileByName(String playerName) {
        ensureInit();
        return dataManager.getPlayerProfileByName(playerName);
    }
    
    // Métodos de conveniência para mensagens
    
    public static void sendMessage(Player player, String message) {
        ensureInit();
        messageManager.sendMessage(player, message);
    }
    
    public static void sendSuccess(Player player, String message) {
        ensureInit();
        messageManager.sendSuccess(player, message);
    }
    
    public static void sendError(Player player, String message) {
        ensureInit();
        messageManager.sendError(player, message);
    }
    
    public static void sendWarning(Player player, String message) {
        ensureInit();
        messageManager.sendWarning(player, message);
    }
    
    public static void sendInfo(Player player, String message) {
        ensureInit();
        messageManager.sendInfo(player, message);
    }
    
    public static void sendNoPermission(Player player) {
        ensureInit();
        messageManager.sendNoPermission(player);
    }
    
    public static void sendUsage(Player player, String usage) {
        ensureInit();
        messageManager.sendUsage(player, usage);
    }
    
    public static void sendPlayerNotFound(Player player, String targetName) {
        ensureInit();
        messageManager.sendPlayerNotFound(player, targetName);
    }
    
    /**
     * Obtém o UUID de um jogador pelo nome (fonte única da verdade).
     * Primeiro busca na tabela player_data, depois gera UUID offline se não encontrar.
     * 
     * @param playerName Nome do jogador
     * @return UUID do jogador ou null se não encontrado
     */
    public static UUID getPlayerUUID(String playerName) {
        ensureInit();
        try {
            // Primeiro tentar buscar na tabela player_data
            UUID uuid = dataManager.getPlayerUUID(playerName);
            if (uuid != null) {
                return uuid;
            }
            
            // Se não encontrou, gerar UUID offline
            return UUIDUtils.offlineUUIDFromName(playerName);
            
        } catch (Exception e) {
            // Em caso de erro, usar UUID offline como fallback
            return UUIDUtils.offlineUUIDFromName(playerName);
        }
    }
    
    // ============================================================================
    // WHITELIST V2 - API PÚBLICA
    // ============================================================================
    
    /**
     * Verifica se um jogador está na whitelist (verificação instantânea em cache).
     * 
     * @param playerUuid UUID do jogador (fonte única da verdade)
     * @return true se o jogador estiver na whitelist, false caso contrário
     */
    public static boolean isWhitelisted(UUID playerUuid) {
        ensureInit();
        return whitelistManager.isWhitelisted(playerUuid);
    }
    
    /**
     * Adiciona um jogador à whitelist.
     * 
     * @param targetUuid UUID do jogador a ser adicionado (fonte única da verdade)
     * @param targetName Nome do jogador (para auditoria)
     * @param authorUuid UUID do admin que está adicionando
     * @param authorName Nome do admin
     * @param reason Motivo da adição
     * @return true se adicionado com sucesso, false caso contrário
     */
    public static boolean addToWhitelist(UUID targetUuid, String targetName, UUID authorUuid, String authorName, String reason) {
        ensureInit();
        return whitelistManager.addToWhitelist(targetUuid, targetName, authorUuid, authorName, reason);
    }
    
    /**
     * Remove um jogador da whitelist.
     * 
     * @param targetUuid UUID do jogador a ser removido
     * @param targetName Nome do jogador a ser removido
     * @param authorUuid UUID do admin que está removendo
     * @param authorName Nome do admin
     * @param reason Motivo da remoção
     * @return true se removido com sucesso, false caso contrário
     */
    public static boolean removeFromWhitelist(UUID targetUuid, String targetName, UUID authorUuid, String authorName, String reason) {
        ensureInit();
        return whitelistManager.removeFromWhitelist(targetUuid, targetName, authorUuid, authorName, reason);
    }
    
    /**
     * Retorna lista de todos os jogadores na whitelist.
     * 
     * @return Lista de WhitelistEntry com informações completas
     */
    public static List<WhitelistManager.WhitelistEntry> getWhitelistedPlayers() {
        ensureInit();
        return whitelistManager.getWhitelistedPlayers();
    }
    
    /**
     * Retorna estatísticas da whitelist.
     * 
     * @return WhitelistStats com informações estatísticas
     */
    public static WhitelistManager.WhitelistStats getWhitelistStats() {
        ensureInit();
        return whitelistManager.getWhitelistStats();
    }
    
    /**
     * Recarrega o cache da whitelist do banco de dados.
     * Útil após mudanças manuais no banco ou para sincronização.
     */
    public static void reloadWhitelistCache() {
        ensureInit();
        whitelistManager.reloadCache();
    }
    
    // ============================================================================
    // SISTEMA DE PERMISSÕES - API PÚBLICA
    // ============================================================================
    
    /**
     * Verifica se um jogador tem uma permissão específica.
     * Este é o método principal para verificação de permissões no sistema.
     * 
     * @param player Jogador a verificar
     * @param permissionNode Nó da permissão (ex: primeleague.admin.kick)
     * @return true se o jogador tem a permissão, false caso contrário
     */
    public static boolean hasPermission(Player player, String permissionNode) {
        // 🔧 DEBUG: Log de entrada na API
        System.out.println("🔍 [API-DEBUG] PrimeLeagueAPI.hasPermission() chamado:");
        System.out.println("🔍 [API-DEBUG] - Player: " + (player != null ? player.getName() : "NULL"));
        System.out.println("🔍 [API-DEBUG] - Permission: " + permissionNode);
        System.out.println("🔍 [API-DEBUG] - API inicializada: " + initialized);
        
        try {
            ensureInit();
            System.out.println("🔍 [API-DEBUG] - PermissionManager: " + (permissionManager != null ? "OK" : "NULL"));
            
            boolean result = permissionManager.hasPermission(player, permissionNode);
            System.out.println("🔍 [API-DEBUG] - Resultado: " + result);
            
            return result;
        } catch (Exception e) {
            System.err.println("🔍 [API-DEBUG] ❌ Erro na API hasPermission: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica se um jogador tem uma permissão específica pelo UUID.
     * 
     * @param playerUuid UUID do jogador
     * @param permissionNode Nó da permissão
     * @return true se o jogador tem a permissão, false caso contrário
     */
    public static boolean hasPermission(UUID playerUuid, String permissionNode) {
        ensureInit();
        return permissionManager.hasPermission(playerUuid, permissionNode);
    }
    
    /**
     * Obtém todas as permissões de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return Set de permissões do jogador
     */
    public static Set<String> getPlayerPermissions(UUID playerUuid) {
        ensureInit();
        return permissionManager.getPlayerPermissions(playerUuid);
    }
    
    /**
     * Obtém os grupos de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return Lista de grupos do jogador
     */
    public static List<PlayerGroup> getPlayerGroups(UUID playerUuid) {
        ensureInit();
        return permissionManager.getPlayerGroups(playerUuid);
    }
    
    // ============================================================================
    // PROFILE PROVIDER - API PARA MÓDULOS EXTERNOS
    // ============================================================================
    
    public static interface ProfileProvider {
        PlayerProfile getProfile(UUID uuid);
        PlayerProfile getProfile(String name);
    }
    
    public static void registerProfileProvider(ProfileProvider providerImpl) {
        provider = providerImpl;
    }
    
    public static PlayerProfile getProfile(UUID uuid) {
        if (provider == null) return null;
        return provider.getProfile(uuid);
    }
    
    public static PlayerProfile getProfile(String name) {
        if (provider == null) return null;
        return provider.getProfile(name);
    }
    
    public static DAOServiceRegistry getDAOServiceRegistry() {
        ensureInit();
        return daoServiceRegistry;
    }
    
    // ==================== SERVIÇOS PARA MÓDULOS EXTERNOS ====================
    
    /**
     * Obtém o ClanService para comunicação com o módulo de Clãs.
     * 
     * @return ClanService ou null se não registrado
     */
    public static br.com.primeleague.api.ClanService getClanServiceRegistry() {
        return ClanServiceRegistry.getInstance();
    }
    
    /**
     * Obtém o EconomyService para comunicação com o sistema de economia.
     * 
     * @return EconomyService ou null se não registrado
     */
    public static br.com.primeleague.api.EconomyService getEconomyServiceRegistry() {
        return EconomyServiceRegistry.getInstance();
    }
    
    /**
     * Obtém o IdentityService para comunicação com o sistema de identidade.
     * 
     * @return IdentityService ou null se não registrado
     */
    public static br.com.primeleague.api.IdentityService getIdentityServiceRegistry() {
        return IdentityServiceRegistry.getInstance();
    }
}


