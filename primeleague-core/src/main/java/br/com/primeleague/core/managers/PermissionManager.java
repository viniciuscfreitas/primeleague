package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.events.GroupPermissionsChangedEvent;
import br.com.primeleague.core.models.GroupPermission;
import br.com.primeleague.core.models.PermissionGroup;
import br.com.primeleague.core.models.PlayerGroup;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gerenciador central do sistema de permissões.
 * Responsável por cache, verificação de permissões e atualizações em tempo real.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PermissionManager implements Listener {
    
    private final PrimeLeagueCore core;
    private final Logger logger;
    private final DataManager dataManager;
    
    // Cache de permissões por jogador (UUID -> Set<permission_node>)
    private final Map<UUID, Set<String>> playerPermissionsCache = new ConcurrentHashMap<>();
    
    // Cache de grupos por jogador (UUID -> List<PlayerGroup>)
    private final Map<UUID, List<PlayerGroup>> playerGroupsCache = new ConcurrentHashMap<>();
    
    // Cache de grupos (group_id -> PermissionGroup)
    private final Map<Integer, PermissionGroup> groupsCache = new ConcurrentHashMap<>();
    
    // Cache de permissões por grupo (group_id -> List<GroupPermission>)
    private final Map<Integer, List<GroupPermission>> groupPermissionsCache = new ConcurrentHashMap<>();
    
    // Cache de jogadores por grupo (group_id -> Set<UUID>)
    private final Map<Integer, Set<UUID>> groupPlayersCache = new ConcurrentHashMap<>();
    
    /**
     * Construtor do PermissionManager.
     * 
     * @param core Instância principal do Core
     */
    public PermissionManager(PrimeLeagueCore core) {
        this.core = core;
        this.logger = core.getLogger();
        this.dataManager = core.getDataManager();
        
        // Registrar este manager como listener
        Bukkit.getPluginManager().registerEvents(this, core);
        
        // Carregar cache inicial
        loadInitialCache();
        
        logger.info("✅ PermissionManager inicializado com sucesso!");
    }
    
    /**
     * Carrega o cache inicial do sistema de permissões.
     */
    private void loadInitialCache() {
        try {
            // Carregar grupos
            loadGroupsCache();
            
            // Carregar permissões dos grupos
            loadGroupPermissionsCache();
            
            // Carregar jogadores online
            for (Player player : Bukkit.getOnlinePlayers()) {
                loadPlayerPermissionsAsync(player.getUniqueId());
            }
            
            logger.info("✅ Cache inicial de permissões carregado com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar cache inicial de permissões: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega o cache de grupos.
     */
    private void loadGroupsCache() {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT group_id, group_name, display_name, description, priority, is_default, is_active, created_at, updated_at " +
                 "FROM permission_groups WHERE is_active = true ORDER BY priority DESC")) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PermissionGroup group = new PermissionGroup(
                    rs.getInt("group_id"),
                    rs.getString("group_name"),
                    rs.getString("display_name"),
                    rs.getString("description"),
                    rs.getInt("priority"),
                    rs.getBoolean("is_default"),
                    rs.getBoolean("is_active"),
                    rs.getTimestamp("created_at"),
                    rs.getTimestamp("updated_at")
                );
                
                groupsCache.put(group.getGroupId(), group);
                groupPlayersCache.put(group.getGroupId(), new HashSet<>());
            }
            
            logger.info("✅ Cache de grupos carregado: " + groupsCache.size() + " grupos");
            
        } catch (SQLException e) {
            logger.severe("❌ Erro ao carregar cache de grupos: " + e.getMessage());
        }
    }
    
    /**
     * Carrega o cache de permissões dos grupos.
     */
    private void loadGroupPermissionsCache() {
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, group_id, permission_node, is_granted, created_at, created_by_player_id " +
                 "FROM group_permissions")) {
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GroupPermission perm = new GroupPermission(
                    rs.getInt("id"),
                    rs.getInt("group_id"),
                    rs.getString("permission_node"),
                    rs.getBoolean("is_granted"),
                    rs.getTimestamp("created_at"),
                    rs.getInt("created_by_player_id")
                );
                
                groupPermissionsCache.computeIfAbsent(perm.getGroupId(), k -> new ArrayList<>()).add(perm);
            }
            
            logger.info("✅ Cache de permissões de grupos carregado");
            
        } catch (SQLException e) {
            logger.severe("❌ Erro ao carregar cache de permissões de grupos: " + e.getMessage());
        }
    }
    
    /**
     * Carrega as permissões de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     */
    public void loadPlayerPermissionsAsync(UUID playerUuid) {
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            try {
                loadPlayerPermissions(playerUuid);
                
                // Sincronizar com a thread principal
                Bukkit.getScheduler().runTask(core, () -> {
                    logger.fine("✅ Permissões carregadas para jogador: " + playerUuid);
                });
                
            } catch (Exception e) {
                logger.severe("❌ Erro ao carregar permissões do jogador " + playerUuid + ": " + e.getMessage());
            }
        });
    }
    
    /**
     * Carrega as permissões de um jogador.
     * 
     * @param playerUuid UUID do jogador
     */
    private void loadPlayerPermissions(UUID playerUuid) {
        try (Connection conn = dataManager.getConnection()) {
            // Buscar ID do jogador
            Integer playerId = getPlayerId(conn, playerUuid);
            if (playerId == null) {
                logger.warning("⚠️ Jogador não encontrado: " + playerUuid);
                return;
            }
            
            // Carregar grupos do jogador
            List<PlayerGroup> playerGroups = loadPlayerGroups(conn, playerId);
            playerGroupsCache.put(playerUuid, playerGroups);
            
            // Calcular permissões consolidadas
            Set<String> permissions = calculatePlayerPermissions(playerId, playerGroups);
            playerPermissionsCache.put(playerUuid, permissions);
            
            // Atualizar cache de jogadores por grupo
            updateGroupPlayersCache(playerUuid, playerGroups);
            
        } catch (SQLException e) {
            logger.severe("❌ Erro ao carregar permissões do jogador " + playerUuid + ": " + e.getMessage());
        }
    }
    
    /**
     * Busca o ID do jogador pelo UUID.
     */
    private Integer getPlayerId(Connection conn, UUID playerUuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT player_id FROM player_data WHERE uuid = ?")) {
            
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("player_id");
            }
        }
        return null;
    }
    
    /**
     * Carrega os grupos de um jogador.
     */
    private List<PlayerGroup> loadPlayerGroups(Connection conn, int playerId) throws SQLException {
        List<PlayerGroup> groups = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT id, player_id, group_id, is_primary, expires_at, added_at, added_by_player_id, reason " +
            "FROM player_groups WHERE player_id = ? AND (expires_at IS NULL OR expires_at > NOW())")) {
            
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PlayerGroup group = new PlayerGroup(
                    rs.getInt("id"),
                    rs.getInt("player_id"),
                    rs.getInt("group_id"),
                    rs.getBoolean("is_primary"),
                    rs.getTimestamp("expires_at"),
                    rs.getTimestamp("added_at"),
                    rs.getInt("added_by_player_id"),
                    rs.getString("reason")
                );
                groups.add(group);
            }
        }
        
        return groups;
    }
    
    /**
     * Calcula as permissões consolidadas de um jogador.
     */
    private Set<String> calculatePlayerPermissions(int playerId, List<PlayerGroup> playerGroups) {
        Set<String> permissions = new HashSet<>();
        Map<String, Boolean> permissionMap = new HashMap<>();
        
        // Ordenar grupos por prioridade (maior primeiro)
        playerGroups.sort((g1, g2) -> {
            PermissionGroup pg1 = groupsCache.get(g1.getGroupId());
            PermissionGroup pg2 = groupsCache.get(g2.getGroupId());
            if (pg1 == null || pg2 == null) return 0;
            return Integer.compare(pg2.getPriority(), pg1.getPriority());
        });
        
        // Processar permissões de cada grupo
        for (PlayerGroup playerGroup : playerGroups) {
            if (playerGroup.isExpired()) continue;
            
            PermissionGroup group = groupsCache.get(playerGroup.getGroupId());
            if (group == null || !group.isActive()) continue;
            
            List<GroupPermission> groupPerms = groupPermissionsCache.get(playerGroup.getGroupId());
            if (groupPerms == null) continue;
            
            for (GroupPermission perm : groupPerms) {
                String node = perm.getPermissionNode();
                
                // Se a permissão já foi definida por um grupo de maior prioridade, ignorar
                if (permissionMap.containsKey(node)) continue;
                
                permissionMap.put(node, perm.isGranted());
            }
        }
        
        // Adicionar apenas permissões concedidas
        for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
            if (entry.getValue()) {
                permissions.add(entry.getKey());
            }
        }
        
        return permissions;
    }
    
    /**
     * Atualiza o cache de jogadores por grupo.
     */
    private void updateGroupPlayersCache(UUID playerUuid, List<PlayerGroup> playerGroups) {
        // Remover jogador de todos os grupos
        for (Set<UUID> players : groupPlayersCache.values()) {
            players.remove(playerUuid);
        }
        
        // Adicionar jogador aos grupos ativos
        for (PlayerGroup playerGroup : playerGroups) {
            if (!playerGroup.isExpired()) {
                Set<UUID> players = groupPlayersCache.get(playerGroup.getGroupId());
                if (players != null) {
                    players.add(playerUuid);
                }
            }
        }
    }
    
    /**
     * Verifica se um jogador tem uma permissão específica.
     * Este é o método principal da API de permissões.
     * 
     * @param player Jogador a verificar
     * @param permissionNode Nó da permissão
     * @return true se o jogador tem a permissão, false caso contrário
     */
    public boolean hasPermission(Player player, String permissionNode) {
        if (player == null || permissionNode == null) {
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        Set<String> permissions = playerPermissionsCache.get(playerUuid);
        
        if (permissions == null) {
            // Cache miss - carregar permissões
            loadPlayerPermissionsAsync(playerUuid);
            return false; // Por segurança, negar até carregar
        }
        
        return permissions.contains(permissionNode);
    }
    
    /**
     * Verifica se um jogador tem uma permissão específica pelo UUID.
     * 
     * @param playerUuid UUID do jogador
     * @param permissionNode Nó da permissão
     * @return true se o jogador tem a permissão, false caso contrário
     */
    public boolean hasPermission(UUID playerUuid, String permissionNode) {
        if (playerUuid == null || permissionNode == null) {
            return false;
        }
        
        Set<String> permissions = playerPermissionsCache.get(playerUuid);
        
        if (permissions == null) {
            // Cache miss - carregar permissões
            loadPlayerPermissionsAsync(playerUuid);
            return false; // Por segurança, negar até carregar
        }
        
        return permissions.contains(permissionNode);
    }
    
    /**
     * Obtém todas as permissões de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return Set de permissões do jogador
     */
    public Set<String> getPlayerPermissions(UUID playerUuid) {
        Set<String> permissions = playerPermissionsCache.get(playerUuid);
        if (permissions == null) {
            permissions = new HashSet<>();
        }
        return new HashSet<>(permissions);
    }
    
    /**
     * Obtém os grupos de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return Lista de grupos do jogador
     */
    public List<PlayerGroup> getPlayerGroups(UUID playerUuid) {
        List<PlayerGroup> groups = playerGroupsCache.get(playerUuid);
        if (groups == null) {
            groups = new ArrayList<>();
        }
        return new ArrayList<>(groups);
    }
    
    /**
     * Obtém um grupo pelo ID.
     * 
     * @param groupId ID do grupo
     * @return Grupo ou null se não encontrado
     */
    public PermissionGroup getGroup(int groupId) {
        return groupsCache.get(groupId);
    }
    
    /**
     * Obtém um grupo pelo nome.
     * 
     * @param groupName Nome do grupo
     * @return Grupo ou null se não encontrado
     */
    public PermissionGroup getGroupByName(String groupName) {
        for (PermissionGroup group : groupsCache.values()) {
            if (group.getGroupName().equalsIgnoreCase(groupName)) {
                return group;
            }
        }
        return null;
    }
    
    /**
     * Obtém todos os grupos ativos.
     * 
     * @return Lista de grupos ativos
     */
    public List<PermissionGroup> getAllGroups() {
        return new ArrayList<>(groupsCache.values());
    }
    
    // ============================================================================
    // EVENT HANDLERS - ATUALIZAÇÃO EM TEMPO REAL
    // ============================================================================
    
    /**
     * Handler para o evento de mudança de permissões de grupo.
     * Atualiza o cache em tempo real.
     */
    @EventHandler
    public void onGroupPermissionsChanged(GroupPermissionsChangedEvent event) {
        int groupId = event.getGroupId();
        String actionType = event.getActionType();
        
        logger.info("🔄 Atualizando cache devido a mudança no grupo " + groupId + ": " + actionType);
        
        // Recarregar cache do grupo afetado
        reloadGroupCache(groupId);
        
        // Recarregar permissões de todos os jogadores afetados
        reloadAffectedPlayers(groupId);
    }
    
    /**
     * Recarrega o cache de um grupo específico.
     */
    private void reloadGroupCache(int groupId) {
        try (Connection conn = dataManager.getConnection()) {
            // Recarregar grupo
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT group_id, group_name, display_name, description, priority, is_default, is_active, created_at, updated_at " +
                "FROM permission_groups WHERE group_id = ?")) {
                
                stmt.setInt(1, groupId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    PermissionGroup group = new PermissionGroup(
                        rs.getInt("group_id"),
                        rs.getString("group_name"),
                        rs.getString("display_name"),
                        rs.getString("description"),
                        rs.getInt("priority"),
                        rs.getBoolean("is_default"),
                        rs.getBoolean("is_active"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at")
                    );
                    
                    groupsCache.put(groupId, group);
                } else {
                    groupsCache.remove(groupId);
                }
            }
            
            // Recarregar permissões do grupo
            List<GroupPermission> permissions = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, group_id, permission_node, is_granted, created_at, created_by_player_id " +
                "FROM group_permissions WHERE group_id = ?")) {
                
                stmt.setInt(1, groupId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    GroupPermission perm = new GroupPermission(
                        rs.getInt("id"),
                        rs.getInt("group_id"),
                        rs.getString("permission_node"),
                        rs.getBoolean("is_granted"),
                        rs.getTimestamp("created_at"),
                        rs.getInt("created_by_player_id")
                    );
                    permissions.add(perm);
                }
            }
            
            groupPermissionsCache.put(groupId, permissions);
            
        } catch (SQLException e) {
            logger.severe("❌ Erro ao recarregar cache do grupo " + groupId + ": " + e.getMessage());
        }
    }
    
    /**
     * Recarrega as permissões de todos os jogadores afetados por uma mudança de grupo.
     */
    private void reloadAffectedPlayers(int groupId) {
        Set<UUID> affectedPlayers = groupPlayersCache.get(groupId);
        if (affectedPlayers == null) return;
        
        logger.info("🔄 Recarregando permissões de " + affectedPlayers.size() + " jogadores afetados");
        
        for (UUID playerUuid : affectedPlayers) {
            loadPlayerPermissionsAsync(playerUuid);
        }
    }
    
    // ============================================================================
    // EVENT HANDLERS - LOGIN/LOGOUT
    // ============================================================================
    
    /**
     * Handler para jogador entrando no servidor.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerPermissionsAsync(player.getUniqueId());
    }
    
    /**
     * Handler para jogador saindo do servidor.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Limpar cache do jogador
        playerPermissionsCache.remove(playerUuid);
        playerGroupsCache.remove(playerUuid);
        
        // Remover jogador de todos os grupos
        for (Set<UUID> players : groupPlayersCache.values()) {
            players.remove(playerUuid);
        }
    }
    
    /**
     * Recarrega todo o cache do sistema de permissões.
     * Útil para sincronização manual.
     */
    public void reloadAllCache() {
        logger.info("🔄 Recarregando todo o cache de permissões...");
        
        // Limpar caches
        playerPermissionsCache.clear();
        playerGroupsCache.clear();
        groupsCache.clear();
        groupPermissionsCache.clear();
        groupPlayersCache.clear();
        
        // Recarregar
        loadInitialCache();
        
        logger.info("✅ Cache de permissões recarregado com sucesso!");
    }
    
    /**
     * Obtém estatísticas do cache.
     * 
     * @return String com estatísticas
     */
    public String getCacheStats() {
        return String.format(
            "📊 Cache Stats - Grupos: %d, Jogadores: %d, Permissões: %d",
            groupsCache.size(),
            playerPermissionsCache.size(),
            groupPermissionsCache.values().stream().mapToInt(List::size).sum()
        );
    }
}
