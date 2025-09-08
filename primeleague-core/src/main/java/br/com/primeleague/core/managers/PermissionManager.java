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
        logger.info("🔍 [PERMISSION-DEBUG] loadInitialCache() iniciado");
        
        try {
            // Carregar grupos
            logger.info("🔍 [PERMISSION-DEBUG] Carregando cache de grupos...");
            loadGroupsCache();
            
            // Carregar permissões dos grupos
            logger.info("🔍 [PERMISSION-DEBUG] Carregando cache de permissões de grupos...");
            loadGroupPermissionsCache();
            
            // Carregar jogadores online
            logger.info("🔍 [PERMISSION-DEBUG] Carregando permissões de jogadores online...");
            Player[] onlinePlayers = Bukkit.getOnlinePlayers();
            logger.info("🔍 [PERMISSION-DEBUG] - Total de jogadores online: " + onlinePlayers.length);
            
            for (Player player : onlinePlayers) {
                logger.info("🔍 [PERMISSION-DEBUG] - Carregando permissões para: " + player.getName() + " (" + player.getUniqueId() + ")");
                loadPlayerPermissionsAsync(player.getUniqueId());
            }
            
            logger.info("🔍 [PERMISSION-DEBUG] ✅ Cache inicial de permissões carregado com sucesso!");
            logger.info("🔍 [PERMISSION-DEBUG] - Grupos carregados: " + groupsCache.size());
            logger.info("🔍 [PERMISSION-DEBUG] - Permissões de grupos carregadas: " + groupPermissionsCache.size());
            
        } catch (Exception e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro ao carregar cache inicial de permissões: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega o cache de grupos.
     */
    private void loadGroupsCache() {
        logger.info("🔍 [PERMISSION-DEBUG] loadGroupsCache() iniciado");
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT group_id, group_name, display_name, description, priority, is_default, is_active, created_at, updated_at " +
                 "FROM permission_groups WHERE is_active = true ORDER BY priority DESC")) {
            
            logger.info("🔍 [PERMISSION-DEBUG] Executando query para carregar grupos...");
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;
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
                
                logger.info("🔍 [PERMISSION-DEBUG] - Grupo carregado: ID=" + group.getGroupId() + 
                           ", Nome=" + group.getGroupName() + 
                           ", Prioridade=" + group.getPriority() + 
                           ", Padrão=" + group.isDefault() + 
                           ", Ativo=" + group.isActive());
                count++;
            }
            
            logger.info("🔍 [PERMISSION-DEBUG] ✅ Cache de grupos carregado: " + count + " grupos");
            
        } catch (SQLException e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro ao carregar cache de grupos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega o cache de permissões dos grupos.
     */
    private void loadGroupPermissionsCache() {
        logger.info("🔍 [PERMISSION-DEBUG] loadGroupPermissionsCache() iniciado");
        
        try (Connection conn = dataManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT id, group_id, permission_node, is_granted, created_at, created_by_player_id " +
                 "FROM group_permissions")) {
            
            logger.info("🔍 [PERMISSION-DEBUG] Executando query para carregar permissões de grupos...");
            ResultSet rs = stmt.executeQuery();
            
            int totalPermissions = 0;
            Map<Integer, Integer> permissionsPerGroup = new HashMap<>();
            
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
                
                // Contar permissões por grupo
                permissionsPerGroup.put(perm.getGroupId(), 
                    permissionsPerGroup.getOrDefault(perm.getGroupId(), 0) + 1);
                
                logger.info("🔍 [PERMISSION-DEBUG] - Permissão carregada: Grupo=" + perm.getGroupId() + 
                           ", Node=" + perm.getPermissionNode() + 
                           ", Granted=" + perm.isGranted());
                
                totalPermissions++;
            }
            
            logger.info("🔍 [PERMISSION-DEBUG] ✅ Cache de permissões de grupos carregado: " + totalPermissions + " permissões");
            
            // Log de permissões por grupo
            for (Map.Entry<Integer, Integer> entry : permissionsPerGroup.entrySet()) {
                PermissionGroup group = groupsCache.get(entry.getKey());
                String groupName = group != null ? group.getGroupName() : "UNKNOWN";
                logger.info("🔍 [PERMISSION-DEBUG] - Grupo " + groupName + " (ID=" + entry.getKey() + "): " + entry.getValue() + " permissões");
            }
            
        } catch (SQLException e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro ao carregar cache de permissões de grupos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega as permissões de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     */
    public void loadPlayerPermissionsAsync(UUID playerUuid) {
        logger.info("🔍 [PERMISSION-DEBUG] loadPlayerPermissionsAsync() chamado para UUID: " + playerUuid);
        
        Bukkit.getScheduler().runTaskAsynchronously(core, () -> {
            try {
                logger.info("🔍 [PERMISSION-DEBUG] Iniciando carregamento assíncrono para: " + playerUuid);
                loadPlayerPermissions(playerUuid);
                
                // Sincronizar com a thread principal
                Bukkit.getScheduler().runTask(core, () -> {
                    logger.info("🔍 [PERMISSION-DEBUG] ✅ Permissões carregadas para jogador: " + playerUuid);
                    
                    // Log das permissões carregadas
                    Set<String> permissions = playerPermissionsCache.get(playerUuid);
                    if (permissions != null) {
                        logger.info("🔍 [PERMISSION-DEBUG] - Permissões carregadas: " + permissions);
                    } else {
                        logger.warning("🔍 [PERMISSION-DEBUG] ⚠️ Permissões ainda NULL após carregamento!");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro ao carregar permissões do jogador " + playerUuid + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Carrega as permissões de um jogador de forma síncrona (para resolver cache miss).
     * 
     * @param playerUuid UUID do jogador
     * @throws Exception se houver erro no carregamento
     */
    public void loadPlayerPermissionsSync(UUID playerUuid) throws Exception {
        logger.info("🔍 [PERMISSION-DEBUG] loadPlayerPermissionsSync() chamado para UUID: " + playerUuid);
        
        try {
            logger.info("🔍 [PERMISSION-DEBUG] Iniciando carregamento síncrono para: " + playerUuid);
            loadPlayerPermissions(playerUuid);
            
            // Verificar se o carregamento foi bem-sucedido
            Set<String> permissions = playerPermissionsCache.get(playerUuid);
            if (permissions != null) {
                logger.info("🔍 [PERMISSION-DEBUG] ✅ Carregamento síncrono bem-sucedido para: " + playerUuid);
                logger.info("🔍 [PERMISSION-DEBUG] - Permissões carregadas: " + permissions);
            } else {
                logger.warning("🔍 [PERMISSION-DEBUG] ⚠️ Carregamento síncrono falhou - permissões ainda NULL para: " + playerUuid);
                throw new Exception("Falha no carregamento síncrono de permissões");
            }
            
        } catch (Exception e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro no carregamento síncrono para " + playerUuid + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Carrega as permissões de um jogador.
     * 
     * @param playerUuid UUID do jogador
     */
    private void loadPlayerPermissions(UUID playerUuid) {
        logger.info("🔍 [PERMISSION-DEBUG] loadPlayerPermissions() iniciado para UUID: " + playerUuid);
        
        try (Connection conn = dataManager.getConnection()) {
            logger.info("🔍 [PERMISSION-DEBUG] Conexão com banco obtida");
            
            // Buscar ID do jogador
            Integer playerId = getPlayerId(conn, playerUuid);
            logger.info("🔍 [PERMISSION-DEBUG] Player ID encontrado: " + playerId);
            
            if (playerId == null) {
                logger.warning("🔍 [PERMISSION-DEBUG] ⚠️ Jogador não encontrado no banco: " + playerUuid);
                return;
            }
            
            // Carregar grupos do jogador
            logger.info("🔍 [PERMISSION-DEBUG] Carregando grupos do jogador...");
            List<PlayerGroup> playerGroups = loadPlayerGroups(conn, playerId);
            logger.info("🔍 [PERMISSION-DEBUG] Grupos carregados: " + playerGroups.size() + " grupos");
            
            for (PlayerGroup group : playerGroups) {
                logger.info("🔍 [PERMISSION-DEBUG] - Grupo ID: " + group.getGroupId() + ", Primary: " + group.isPrimary() + ", Expired: " + group.isExpired());
            }
            
            playerGroupsCache.put(playerUuid, playerGroups);
            
            // Calcular permissões consolidadas
            logger.info("🔍 [PERMISSION-DEBUG] Calculando permissões consolidadas...");
            Set<String> permissions = calculatePlayerPermissions(playerId, playerGroups);
            logger.info("🔍 [PERMISSION-DEBUG] Permissões calculadas: " + permissions.size() + " permissões");
            logger.info("🔍 [PERMISSION-DEBUG] - Lista de permissões: " + permissions);
            
            playerPermissionsCache.put(playerUuid, permissions);
            
            // Atualizar cache de jogadores por grupo
            updateGroupPlayersCache(playerUuid, playerGroups);
            
            logger.info("🔍 [PERMISSION-DEBUG] ✅ Carregamento de permissões concluído para: " + playerUuid);
            
        } catch (SQLException e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro SQL ao carregar permissões do jogador " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro geral ao carregar permissões do jogador " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Busca o ID do jogador pelo UUID.
     * CORREÇÃO: Usar UUID canônico em vez de UUID do Bukkit
     */
    private Integer getPlayerId(Connection conn, UUID playerUuid) throws SQLException {
        // ✅ CORREÇÃO: Converter UUID do Bukkit para UUID canônico
        UUID canonicalUuid = dataManager.getCanonicalUuid(playerUuid);
        if (canonicalUuid == null) {
            // Fallback: tentar com UUID original
            canonicalUuid = playerUuid;
        }
        
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT player_id FROM player_data WHERE uuid = ?")) {
            
            stmt.setString(1, canonicalUuid.toString());
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
        logger.info("🔍 [PERMISSION-DEBUG] calculatePlayerPermissions() iniciado para Player ID: " + playerId);
        
        Set<String> permissions = new HashSet<>();
        Map<String, Boolean> permissionMap = new HashMap<>();
        
        logger.info("🔍 [PERMISSION-DEBUG] - Total de grupos do jogador: " + playerGroups.size());
        
        // Ordenar grupos por prioridade (maior primeiro)
        playerGroups.sort((g1, g2) -> {
            PermissionGroup pg1 = groupsCache.get(g1.getGroupId());
            PermissionGroup pg2 = groupsCache.get(g2.getGroupId());
            if (pg1 == null || pg2 == null) return 0;
            return Integer.compare(pg2.getPriority(), pg1.getPriority());
        });
        
        logger.info("🔍 [PERMISSION-DEBUG] - Grupos ordenados por prioridade:");
        for (PlayerGroup group : playerGroups) {
            PermissionGroup pg = groupsCache.get(group.getGroupId());
            logger.info("🔍 [PERMISSION-DEBUG]   - Grupo ID: " + group.getGroupId() + 
                       ", Nome: " + (pg != null ? pg.getGroupName() : "NULL") + 
                       ", Prioridade: " + (pg != null ? pg.getPriority() : "NULL") +
                       ", Ativo: " + (pg != null ? pg.isActive() : "NULL") +
                       ", Expirado: " + group.isExpired());
        }
        
        // Processar permissões de cada grupo
        for (PlayerGroup playerGroup : playerGroups) {
            if (playerGroup.isExpired()) {
                logger.info("🔍 [PERMISSION-DEBUG] - Pulando grupo expirado: " + playerGroup.getGroupId());
                continue;
            }
            
            PermissionGroup group = groupsCache.get(playerGroup.getGroupId());
            if (group == null) {
                logger.warning("🔍 [PERMISSION-DEBUG] - Grupo não encontrado no cache: " + playerGroup.getGroupId());
                continue;
            }
            
            if (!group.isActive()) {
                logger.info("🔍 [PERMISSION-DEBUG] - Pulando grupo inativo: " + group.getGroupName());
                continue;
            }
            
            List<GroupPermission> groupPerms = groupPermissionsCache.get(playerGroup.getGroupId());
            if (groupPerms == null) {
                logger.warning("🔍 [PERMISSION-DEBUG] - Nenhuma permissão encontrada para grupo: " + group.getGroupName());
                continue;
            }
            
            logger.info("🔍 [PERMISSION-DEBUG] - Processando grupo: " + group.getGroupName() + " (" + groupPerms.size() + " permissões)");
            
            for (GroupPermission perm : groupPerms) {
                String node = perm.getPermissionNode();
                
                // Se a permissão já foi definida por um grupo de maior prioridade, ignorar
                if (permissionMap.containsKey(node)) {
                    logger.info("🔍 [PERMISSION-DEBUG]   - Permissão já definida por grupo de maior prioridade: " + node);
                    continue;
                }
                
                permissionMap.put(node, perm.isGranted());
                logger.info("🔍 [PERMISSION-DEBUG]   - Adicionada permissão: " + node + " = " + perm.isGranted());
            }
        }
        
        // Adicionar apenas permissões concedidas
        for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
            if (entry.getValue()) {
                permissions.add(entry.getKey());
                logger.info("🔍 [PERMISSION-DEBUG] - Permissão final concedida: " + entry.getKey());
            } else {
                logger.info("🔍 [PERMISSION-DEBUG] - Permissão negada: " + entry.getKey());
            }
        }
        
        logger.info("🔍 [PERMISSION-DEBUG] - Total de permissões finais: " + permissions.size());
        logger.info("🔍 [PERMISSION-DEBUG] - Lista final de permissões: " + permissions);
        
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
        // 🔧 DEBUG: Log de entrada
        logger.info("🔍 [PERMISSION-DEBUG] hasPermission() chamado:");
        logger.info("🔍 [PERMISSION-DEBUG] - Player: " + (player != null ? player.getName() : "NULL"));
        logger.info("🔍 [PERMISSION-DEBUG] - Permission: " + permissionNode);
        
        if (player == null || permissionNode == null) {
            logger.warning("🔍 [PERMISSION-DEBUG] ❌ Parâmetros inválidos - Player: " + (player != null ? "OK" : "NULL") + ", Permission: " + (permissionNode != null ? "OK" : "NULL"));
            return false;
        }
        
        UUID playerUuid = player.getUniqueId();
        logger.info("🔍 [PERMISSION-DEBUG] - Player UUID: " + playerUuid);
        
        Set<String> permissions = playerPermissionsCache.get(playerUuid);
        logger.info("🔍 [PERMISSION-DEBUG] - Cache hit: " + (permissions != null ? "SIM" : "NÃO"));
        
        if (permissions == null) {
            logger.warning("🔍 [PERMISSION-DEBUG] ⚠️ CACHE MISS - Tentando carregamento síncrono para " + player.getName());
            
            // 🔧 CORREÇÃO: Tentar carregamento síncrono primeiro
            try {
                loadPlayerPermissionsSync(playerUuid);
                permissions = playerPermissionsCache.get(playerUuid);
                
                if (permissions != null) {
                    logger.info("🔍 [PERMISSION-DEBUG] ✅ Carregamento síncrono bem-sucedido para " + player.getName());
                } else {
                    logger.warning("🔍 [PERMISSION-DEBUG] ⚠️ Carregamento síncrono falhou, iniciando assíncrono para " + player.getName());
                    loadPlayerPermissionsAsync(playerUuid);
                    return false; // Por segurança, negar até carregar
                }
            } catch (Exception e) {
                logger.severe("🔍 [PERMISSION-DEBUG] ❌ Erro no carregamento síncrono para " + player.getName() + ": " + e.getMessage());
                loadPlayerPermissionsAsync(playerUuid);
                return false; // Por segurança, negar até carregar
            }
        }
        
        boolean hasPermission = permissions.contains(permissionNode);
        logger.info("🔍 [PERMISSION-DEBUG] - Permissões do jogador: " + permissions);
        logger.info("🔍 [PERMISSION-DEBUG] - Tem permissão '" + permissionNode + "': " + hasPermission);
        
        if (!hasPermission) {
            logger.warning("🔍 [PERMISSION-DEBUG] ❌ PERMISSÃO NEGADA para " + player.getName() + " - " + permissionNode);
            logger.warning("🔍 [PERMISSION-DEBUG] - Permissões disponíveis: " + permissions);
        } else {
            logger.info("🔍 [PERMISSION-DEBUG] ✅ PERMISSÃO CONCEDIDA para " + player.getName() + " - " + permissionNode);
        }
        
        return hasPermission;
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
