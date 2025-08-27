package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.models.PlayerProfile;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Gerenciador central de dados do Prime League Core.
 * Responsável por todas as operações de banco de dados relacionadas aos jogadores.
 * 
 * REFATORADO para schema normalizado:
 * - Removidas referências a clan_id da tabela player_data
 * - Implementados LEFT JOINs para obter informações de clã
 * - Adicionado método loadPlayerProfileWithClan para busca com dados de clã
 */
public final class DataManager {

    private final PrimeLeagueCore plugin;
    private final Map<UUID, PlayerProfile> profileCache = new ConcurrentHashMap<UUID, PlayerProfile>();
    private final Set<UUID> loadingProfiles = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private HikariDataSource dataSource;
    
    // TRADUTOR DE IDENTIDADE: Mapeia UUID do Bukkit para UUID canônico
    private final Map<UUID, UUID> bukkitToCanonicalUuidMap = new ConcurrentHashMap<>();

    // SQLs refatorados para schema V2.1 (usando player_id)
    private static final String SELECT_PLAYER_SQL =
            "SELECT name, elo, money, total_playtime, subscription_expires_at, last_seen, total_logins, status, donor_tier, donor_tier_expires_at FROM player_data WHERE uuid = ? LIMIT 1";
    
    private static final String SELECT_PLAYER_WITH_CLAN_SQL =
            "SELECT pd.name, pd.elo, pd.money, pd.total_playtime, pd.subscription_expires_at, pd.last_seen, pd.total_logins, pd.status, pd.donor_tier, pd.donor_tier_expires_at, " +
            "cp.clan_id, cp.role " +
            "FROM player_data pd " +
            "LEFT JOIN clan_players cp ON pd.player_id = cp.player_id " +
            "WHERE pd.uuid = ? LIMIT 1";
    
    private static final String UPSERT_PLAYER_SQL =
            "INSERT INTO player_data (uuid, name, elo, money, total_playtime, subscription_expires_at, last_seen, total_logins, status, donor_tier, donor_tier_expires_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), elo = VALUES(elo), money = VALUES(money), " +
            "total_playtime = VALUES(total_playtime), subscription_expires_at = VALUES(subscription_expires_at), " +
            "last_seen = VALUES(last_seen), total_logins = VALUES(total_logins), status = VALUES(status), " +
            "donor_tier = VALUES(donor_tier), donor_tier_expires_at = VALUES(donor_tier_expires_at)";

    // SQLs para funcionalidades P2P (refatorados)
    private static final String SELECT_PLAYER_BY_NAME_SQL =
            "SELECT uuid, name, elo, money, total_playtime, subscription_expires_at, last_seen, total_logins, status, donor_tier, donor_tier_expires_at FROM player_data WHERE name = ? LIMIT 1";
    
    private static final String SELECT_PLAYER_BY_NAME_WITH_CLAN_SQL =
            "SELECT pd.uuid, pd.name, pd.elo, pd.money, pd.total_playtime, pd.subscription_expires_at, pd.last_seen, pd.total_logins, pd.status, pd.donor_tier, pd.donor_tier_expires_at, " +
            "cp.clan_id, cp.role " +
            "FROM player_data pd " +
            "LEFT JOIN clan_players cp ON pd.player_id = cp.player_id " +
            "WHERE pd.name = ? LIMIT 1";
    
    private static final String SELECT_DISCORD_LINK_SQL =
            "SELECT dl.player_id FROM discord_links dl " +
            "WHERE dl.discord_id = ? LIMIT 1";
    
    private static final String INSERT_DISCORD_LINK_SQL =
            "INSERT INTO discord_links (discord_id, player_id) VALUES (?, ?)";
    
    private static final String UPDATE_SUBSCRIPTION_SQL =
            "UPDATE player_data SET subscription_expires_at = ? WHERE uuid = ?";
    
    private static final String ADD_SUBSCRIPTION_DAYS_SQL =
            "UPDATE player_data SET subscription_expires_at = CASE " +
            "WHEN subscription_expires_at IS NULL OR subscription_expires_at < NOW() THEN DATE_ADD(NOW(), INTERVAL ? DAY) " +
            "ELSE DATE_ADD(subscription_expires_at, INTERVAL ? DAY) " +
            "END WHERE uuid = ?";

    public DataManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        final String host = plugin.getConfig().getString("database.host", "127.0.0.1");
        final int port = plugin.getConfig().getInt("database.port", 3306);
        final String name = plugin.getConfig().getString("database.name", "primeleague");
        final String user = plugin.getConfig().getString("database.user", "root");
        final String password = plugin.getConfig().getString("database.password", "");
        final String jdbcParams = plugin.getConfig().getString("database.jdbcParams", "useUnicode=true&characterEncoding=utf8");
        final String testQuery = plugin.getConfig().getString("database.connectionTestQuery", "SELECT 1");

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + name + "?" + jdbcParams);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setConnectionTestQuery(testQuery);
        cfg.setMaximumPoolSize(plugin.getConfig().getInt("database.pool.maximumPoolSize", 10));
        cfg.setMinimumIdle(plugin.getConfig().getInt("database.pool.minimumIdle", 2));
        cfg.setConnectionTimeout(plugin.getConfig().getLong("database.pool.connectionTimeoutMs", 10000L));
        cfg.setIdleTimeout(plugin.getConfig().getLong("database.pool.idleTimeoutMs", 600000L));
        cfg.setMaxLifetime(plugin.getConfig().getLong("database.pool.maxLifetimeMs", 1800000L));

        // Otimizações para MySQL
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(cfg);
        plugin.getLogger().info("Pool de conexões inicializado.");
    }

    public void disconnect() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
            plugin.getLogger().info("Pool de conexões encerrado.");
        }
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }
    
    public HikariDataSource getDataSource() {
        return this.dataSource;
    }
    
    /**
     * Obtém o UUID de um jogador pelo nome.
     * 
     * @param playerName Nome do jogador
     * @return UUID do jogador ou null se não encontrado
     */
    public UUID getPlayerUUID(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }
        
        String sql = "SELECT uuid FROM player_data WHERE name = ? LIMIT 1";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, playerName.trim());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return UUID.fromString(rs.getString("uuid"));
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao buscar UUID do jogador " + playerName + ": " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Carrega o perfil de um jogador (sem informações de clã).
     * 
     * @param uuid UUID do jogador
     * @param playerName Nome do jogador (para novos jogadores)
     */
    public void loadPlayerProfile(UUID uuid, String playerName) {
        // Evento de pré-login é assíncrono; esta chamada é bloqueante mas fora do main thread
        try {
            PlayerProfile profile = null;
            Connection conn = getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_SQL);
                try {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    try {
                        if (rs.next()) {
                            // JOGADOR EXISTENTE - Carregar dados do banco
                            profile = new PlayerProfile();
                            profile.setUuid(uuid);
                            profile.setPlayerName(rs.getString("name"));
                            profile.setElo(rs.getInt("elo"));
                            profile.setMoney(rs.getBigDecimal("money"));
                            // clan_id não existe mais em player_data - será obtido via JOIN quando necessário
                            profile.setClanId(null);
                            profile.setTotalPlaytime(rs.getLong("total_playtime"));
                            profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                            profile.setLastSeen(rs.getTimestamp("last_seen"));
                            profile.setTotalLogins(rs.getInt("total_logins"));
                            
                            String statusStr = rs.getString("status");
                            if (statusStr != null) {
                                profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                            } else {
                                profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            }
                            
                            // Carregar dados de doador
                            profile.setDonorTier(rs.getInt("donor_tier"));
                            profile.setDonorTierExpiresAt(rs.getTimestamp("donor_tier_expires_at"));
                        } else {
                            // JOGADOR NOVO - Criar perfil e salvar no banco
                            profile = new PlayerProfile();
                            profile.setUuid(uuid);
                            profile.setPlayerName(playerName);
                            profile.setElo(plugin.getConfig().getInt("economy.initial_elo", 1000));
                            profile.setMoney(BigDecimal.ZERO);
                            profile.setTotalLogins(1);
                            profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            profile.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
                            // Salvar novo perfil no banco de dados
                            saveNewPlayerProfile(profile);
                        }
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
            this.profileCache.put(uuid, profile);
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar perfil: " + uuid + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Carrega o perfil de um jogador com informações de clã (usando LEFT JOIN).
     * 
     * @param uuid UUID do jogador
     * @param playerName Nome do jogador (para novos jogadores)
     * @return PlayerProfile com informações de clã ou null se erro
     */
    public PlayerProfile loadPlayerProfileWithClan(UUID uuid, String playerName) {
        try {
            PlayerProfile profile = null;
            Connection conn = getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(SELECT_PLAYER_WITH_CLAN_SQL);
                try {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    try {
                        if (rs.next()) {
                            // JOGADOR EXISTENTE - Carregar dados do banco com informações de clã
                            profile = new PlayerProfile();
                            profile.setUuid(uuid);
                            profile.setPlayerName(rs.getString("name"));
                            profile.setElo(rs.getInt("elo"));
                            profile.setMoney(rs.getBigDecimal("money"));
                            profile.setTotalPlaytime(rs.getLong("total_playtime"));
                            profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                            profile.setLastSeen(rs.getTimestamp("last_seen"));
                            profile.setTotalLogins(rs.getInt("total_logins"));
                            
                            String statusStr = rs.getString("status");
                            if (statusStr != null) {
                                profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                            } else {
                                profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            }
                            
                            // Carregar dados de doador
                            profile.setDonorTier(rs.getInt("donor_tier"));
                            profile.setDonorTierExpiresAt(rs.getTimestamp("donor_tier_expires_at"));
                            
                            // Obter informações de clã via LEFT JOIN
                            int clanId = rs.getInt("clan_id");
                            if (!rs.wasNull()) {
                                profile.setClanId(clanId);
                            } else {
                                profile.setClanId(null);
                            }
                        } else {
                            // JOGADOR NOVO - Criar perfil e salvar no banco
                            profile = new PlayerProfile();
                            profile.setUuid(uuid);
                            profile.setPlayerName(playerName);
                            profile.setElo(plugin.getConfig().getInt("economy.initial_elo", 1000));
                            profile.setMoney(BigDecimal.ZERO);
                            profile.setTotalLogins(1);
                            profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            profile.setLastSeen(new java.sql.Timestamp(System.currentTimeMillis()));
                            profile.setClanId(null);
                            // Salvar novo perfil no banco de dados
                            saveNewPlayerProfile(profile);
                        }
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
            
            // Atualizar cache
            this.profileCache.put(uuid, profile);
            return profile;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar perfil com clã: " + uuid + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void savePlayerProfileAsync(final PlayerProfile profile) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    savePlayerProfileSync(profile);
                } finally {
                    // Remoção atômica: evita apagar perfil novo carregado em relogin rápido
                    profileCache.remove(profile.getUuid(), profile);
                }
            }
        });
    }

    public void savePlayerProfileSync(final PlayerProfile profile) {
        try {
            Connection conn = getConnection();
            try {
                PreparedStatement ps = conn.prepareStatement(UPSERT_PLAYER_SQL);
                try {
                    ps.setString(1, profile.getUuid().toString());
                    ps.setString(2, profile.getPlayerName());
                    ps.setInt(3, profile.getElo());
                    ps.setBigDecimal(4, profile.getMoney());
                    ps.setLong(5, profile.getTotalPlaytime());
                    if (profile.getSubscriptionExpiry() == null) {
                        ps.setNull(6, java.sql.Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(6, new java.sql.Timestamp(profile.getSubscriptionExpiry().getTime()));
                    }
                    if (profile.getLastSeen() == null) {
                        ps.setTimestamp(7, new java.sql.Timestamp(System.currentTimeMillis()));
                    } else {
                        ps.setTimestamp(7, new java.sql.Timestamp(profile.getLastSeen().getTime()));
                    }
                    ps.setInt(8, profile.getTotalLogins());
                    ps.setString(9, profile.getStatus().name());
                    ps.executeUpdate();
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao salvar perfil: " + profile.getUuid() + " - " + e.getMessage());
        }
    }

    /**
     * Salva um novo perfil de jogador no banco de dados.
     * @param profile O perfil a ser salvo
     */
    public void saveNewPlayerProfile(PlayerProfile profile) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPSERT_PLAYER_SQL)) {
            
            
            stmt.setString(1, profile.getUuid().toString());
            stmt.setString(2, profile.getPlayerName());
            stmt.setInt(3, profile.getElo());
            stmt.setBigDecimal(4, profile.getMoney());
            stmt.setLong(5, profile.getTotalPlaytime());
            stmt.setTimestamp(6, profile.getSubscriptionExpiry() != null ? new java.sql.Timestamp(profile.getSubscriptionExpiry().getTime()) : null);
            stmt.setTimestamp(7, profile.getLastSeen() != null ? new java.sql.Timestamp(profile.getLastSeen().getTime()) : null);
            stmt.setInt(8, profile.getTotalLogins());
            stmt.setString(9, profile.getStatus().name());
            stmt.setInt(10, profile.getDonorTier());
            stmt.setTimestamp(11, profile.getDonorTierExpiresAt() != null ? new java.sql.Timestamp(profile.getDonorTierExpiresAt().getTime()) : null);
            
            int affectedRows = stmt.executeUpdate();
            plugin.getLogger().info("[DEBUG] Perfil salvo no banco: " + affectedRows + " linhas afetadas");
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao salvar novo perfil: " + profile.getUuid() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================================
    // MÉTODOS DE CACHE - CONSOLIDADOS DO PLAYERPROFILEMANAGER
    // ============================================================================
    
    /**
     * Obtém o perfil de um jogador pelo UUID (com cache).
     * @param uuid UUID do jogador
     * @return PlayerProfile do jogador, ou null se não encontrado
     */
    public PlayerProfile getPlayerProfile(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        
        // Verificar cache primeiro
        PlayerProfile cachedProfile = profileCache.get(uuid);
        if (cachedProfile != null) {
            return cachedProfile;
        }
        
        // Se não está no cache, carregar do banco
        PlayerProfile profile = loadOfflinePlayerProfile(uuid);
        if (profile != null) {
            profileCache.put(uuid, profile);
        }
        
        return profile;
    }
    
    /**
     * Obtém o perfil de um jogador pelo Player.
     * @param player Jogador do Bukkit
     * @return PlayerProfile do jogador, ou null se não encontrado
     */
    public PlayerProfile getPlayerProfile(Player player) {
        if (player == null) {
            return null;
        }
        return getPlayerProfile(player.getUniqueId());
    }
    
    /**
     * Obtém o perfil de um jogador pelo nome.
     * @param playerName Nome do jogador
     * @return PlayerProfile do jogador, ou null se não encontrado
     */
    public PlayerProfile getPlayerProfileByName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return null;
        }
        
        // Primeiro, verificar se o jogador está online
        Player onlinePlayer = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return getPlayerProfile(onlinePlayer);
        }
        
        // Se estiver offline, buscar DIRETAMENTE no banco pelo nome
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PLAYER_BY_NAME_SQL)) {
            stmt.setString(1, playerName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerProfile profile = new PlayerProfile();
                    profile.setUuid(uuid);
                    profile.setPlayerName(rs.getString("name"));
                    profile.setElo(rs.getInt("elo"));
                    profile.setMoney(rs.getBigDecimal("money"));
                    profile.setClanId(null); // clan_id não existe mais em player_data
                    profile.setTotalPlaytime(rs.getLong("total_playtime"));
                    profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Carregar dados de doador
                    profile.setDonorTier(rs.getInt("donor_tier"));
                    profile.setDonorTierExpiresAt(rs.getTimestamp("donor_tier_expires_at"));
                    
                    // Adicionar ao cache
                    profileCache.put(uuid, profile);
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao buscar jogador por nome " + playerName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Carrega o perfil de um jogador offline diretamente do banco de dados.
     * @param uuid UUID do jogador a ser carregado.
     * @return O PlayerProfile se encontrado, caso contrário null.
     */
    public PlayerProfile loadOfflinePlayerProfile(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PLAYER_SQL)) {
            stmt.setString(1, uuid.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    PlayerProfile profile = new PlayerProfile();
                    profile.setUuid(uuid);
                    profile.setPlayerName(rs.getString("name"));
                    profile.setElo(rs.getInt("elo"));
                    profile.setMoney(rs.getBigDecimal("money"));
                    profile.setClanId(null); // clan_id não existe mais em player_data
                    profile.setTotalPlaytime(rs.getLong("total_playtime"));
                    profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Carregar dados de doador
                    profile.setDonorTier(rs.getInt("donor_tier"));
                    profile.setDonorTierExpiresAt(rs.getTimestamp("donor_tier_expires_at"));
                    
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar o perfil offline do jogador " + uuid + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Obtém o perfil de um jogador pelo player_id.
     * @param playerId ID do jogador
     * @return PlayerProfile do jogador, ou null se não encontrado
     */
    public PlayerProfile getPlayerProfile(int playerId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid, name, elo, money, total_playtime, subscription_expires_at, last_seen, total_logins, status, donor_tier, donor_tier_expires_at FROM player_data WHERE player_id = ? LIMIT 1")) {
            stmt.setInt(1, playerId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerProfile profile = new PlayerProfile();
                    profile.setUuid(uuid);
                    profile.setPlayerName(rs.getString("name"));
                    profile.setElo(rs.getInt("elo"));
                    profile.setMoney(rs.getBigDecimal("money"));
                    profile.setClanId(null); // clan_id não existe mais em player_data
                    profile.setTotalPlaytime(rs.getLong("total_playtime"));
                    profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Carregar dados de doador
                    profile.setDonorTier(rs.getInt("donor_tier"));
                    profile.setDonorTierExpiresAt(rs.getTimestamp("donor_tier_expires_at"));
                    
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar o perfil do jogador ID " + playerId + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Remove um perfil do cache.
     * @param uuid UUID do jogador
     */
    public void removeFromCache(UUID uuid) {
        if (uuid != null) {
            profileCache.remove(uuid);
        }
    }
    
    /**
     * Limpa todo o cache de perfis.
     */
    public void clearCache() {
        profileCache.clear();
    }
    
    /**
     * Obtém o tamanho atual do cache.
     * @return Número de perfis em cache
     */
    public int getCacheSize() {
        return profileCache.size();
    }
    
    /**
     * Verifica se um perfil está em cache.
     * @param uuid UUID do jogador
     * @return true se está em cache, false caso contrário
     */
    public boolean isCached(UUID uuid) {
        return uuid != null && profileCache.containsKey(uuid);
    }
    
    // Métodos de compatibilidade mantidos
    public PlayerProfile getPlayerProfileFromCache(UUID uuid) {
        return this.profileCache.get(uuid);
    }

    public void putPlayerProfileInCache(UUID uuid, PlayerProfile profile) {
        this.profileCache.put(uuid, profile);
    }

    public Map<UUID, PlayerProfile> getProfileCache() {
        return this.profileCache;
    }

    public PrimeLeagueCore getPlugin() {
        return this.plugin;
    }

    /**
     * Carrega o perfil de um jogador offline diretamente do banco de dados.
     * @param playerName O nome do jogador a ser carregado.
     * @return O PlayerProfile se encontrado, caso contrário null.
     */
    public PlayerProfile loadOfflinePlayerProfile(String playerName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PLAYER_BY_NAME_SQL)) {
            stmt.setString(1, playerName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerProfile profile = new PlayerProfile();
                    profile.setUuid(uuid);
                    profile.setPlayerName(rs.getString("name"));
                    profile.setElo(rs.getInt("elo"));
                    profile.setMoney(rs.getBigDecimal("money"));
                    // clan_id não existe mais em player_data
                    profile.setClanId(null);
                    profile.setTotalPlaytime(rs.getLong("total_playtime"));
                    profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar o perfil offline do jogador " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Carrega o perfil de um jogador offline com informações de clã.
     * @param playerName O nome do jogador a ser carregado.
     * @return O PlayerProfile com informações de clã se encontrado, caso contrário null.
     */
    public PlayerProfile loadOfflinePlayerProfileWithClan(String playerName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_PLAYER_BY_NAME_WITH_CLAN_SQL)) {
            stmt.setString(1, playerName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerProfile profile = new PlayerProfile();
                    profile.setUuid(uuid);
                    profile.setPlayerName(rs.getString("name"));
                    profile.setElo(rs.getInt("elo"));
                    profile.setMoney(rs.getBigDecimal("money"));
                    profile.setTotalPlaytime(rs.getLong("total_playtime"));
                    profile.setSubscriptionExpiry(rs.getTimestamp("subscription_expires_at"));
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Obter informações de clã via LEFT JOIN
                    int clanId = rs.getInt("clan_id");
                    if (!rs.wasNull()) {
                        profile.setClanId(clanId);
                    } else {
                        profile.setClanId(null);
                    }
                    return profile;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar o perfil offline do jogador " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Método duplicado removido - já existe na seção consolidada acima

    /**
     * Busca o UUID de um jogador pelo seu Discord ID.
     * @param discordId O ID do Discord.
     * @return O UUID do jogador se encontrado, caso contrário null.
     */
    public UUID findUUIDFromDiscordId(String discordId) {
        if (discordId == null || discordId.trim().isEmpty()) {
            return null;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_DISCORD_LINK_SQL)) {
            
            stmt.setString(1, discordId.trim());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    // Converter player_id para UUID usando IdentityManager
                    return plugin.getIdentityManager().getUuidByPlayerId(playerId);
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao buscar UUID do Discord ID " + discordId + ": " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Cria um vínculo entre um Discord ID e um UUID de jogador.
     * @param discordId O ID do Discord.
     * @param playerUuid O UUID do jogador.
     * @param playerName O nome do jogador.
     * @return true se o vínculo foi criado com sucesso, false caso contrário.
     */
    public boolean createDiscordLink(String discordId, UUID playerUuid, String playerName) {
        if (discordId == null || playerUuid == null || playerName == null) {
            return false;
        }

        // Converter UUID para player_id usando IdentityManager
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            plugin.getLogger().severe("UUID não encontrado no IdentityManager: " + playerUuid);
            return false;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_DISCORD_LINK_SQL)) {
            
            stmt.setString(1, discordId.trim());
            stmt.setInt(2, playerId.intValue());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao criar vínculo Discord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza a data de expiração da assinatura de um jogador.
     * @param playerUuid O UUID do jogador.
     * @param expiryDate A nova data de expiração.
     * @return true se a atualização foi bem-sucedida, false caso contrário.
     */
    public boolean updateSubscriptionExpiry(UUID playerUuid, java.util.Date expiryDate) {
        if (playerUuid == null) {
            return false;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SUBSCRIPTION_SQL)) {
            
            if (expiryDate == null) {
                stmt.setNull(1, java.sql.Types.TIMESTAMP);
            } else {
                stmt.setTimestamp(1, new java.sql.Timestamp(expiryDate.getTime()));
            }
            stmt.setString(2, playerUuid.toString());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao atualizar expiração da assinatura: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adiciona dias à assinatura de um jogador.
     * @param playerUuid O UUID do jogador.
     * @param days O número de dias a adicionar.
     * @return true se a operação foi bem-sucedida, false caso contrário.
     */
    public boolean addSubscriptionDays(UUID playerUuid, int days) {
        if (playerUuid == null || days <= 0) {
            return false;
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(ADD_SUBSCRIPTION_DAYS_SQL)) {
            
            stmt.setInt(1, days);
            stmt.setInt(2, days);
            stmt.setString(3, playerUuid.toString());
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao adicionar dias à assinatura: " + e.getMessage());
            return false;
        }
    }

    // ===== MÉTODOS DE CONTROLE DE ESTADO DE CARREGAMENTO =====
    
    /**
     * Marca um jogador como em processo de carregamento.
     * @param uuid O UUID do jogador.
     */
    public void startLoading(UUID uuid) {
        loadingProfiles.add(uuid);
    }

    /**
     * Marca um jogador como tendo terminado o carregamento.
     * @param uuid O UUID do jogador.
     */
    public void finishLoading(UUID uuid) {
        loadingProfiles.remove(uuid);
    }

    /**
     * Verifica se um jogador está em processo de carregamento.
     * @param uuid O UUID do jogador.
     * @return true se o jogador está carregando, false caso contrário.
     */
    public boolean isLoading(UUID uuid) {
        return loadingProfiles.contains(uuid);
    }
    
    // ===== MÉTODOS DO TRADUTOR DE IDENTIDADE =====
    
    /**
     * Mapeia um UUID do Bukkit para o nosso UUID canônico.
     * @param bukkitUuid O UUID gerado pelo Bukkit.
     * @param canonicalUuid O UUID canônico do nosso banco de dados.
     */
    public void addUuidMapping(UUID bukkitUuid, UUID canonicalUuid) {
        bukkitToCanonicalUuidMap.put(bukkitUuid, canonicalUuid);
        plugin.getLogger().info("🔗 [UUID-MAP] Mapeamento criado: " + bukkitUuid + " → " + canonicalUuid);
    }

    /**
     * Remove o mapeamento quando um jogador sai.
     * @param bukkitUuid O UUID do Bukkit a ser removido.
     */
    public void removeUuidMapping(UUID bukkitUuid) {
        UUID canonicalUuid = bukkitToCanonicalUuidMap.remove(bukkitUuid);
        if (canonicalUuid != null) {
            plugin.getLogger().info("🗑️ [UUID-MAP] Mapeamento removido: " + bukkitUuid + " → " + canonicalUuid);
        }
    }

    /**
     * Traduz um UUID do Bukkit para o UUID canônico. Esta é a função mais importante.
     * Se nenhum mapeamento for encontrado, retorna o próprio UUID de entrada como fallback.
     * @param bukkitUuid O UUID do Bukkit.
     * @return O UUID canônico correspondente, ou o próprio bukkitUuid se não encontrado.
     */
    public UUID getCanonicalUuid(UUID bukkitUuid) {
        UUID canonicalUuid = bukkitToCanonicalUuidMap.get(bukkitUuid);
        if (canonicalUuid != null) {
            return canonicalUuid;
        }
        // Fallback: retorna o próprio UUID se não houver mapeamento
        return bukkitUuid;
    }
    
    /**
     * Obtém o player_id de um jogador do banco de dados.
     * Suporte para o IdentityManager.
     * 
     * @param bukkitUuid UUID do Bukkit
     * @param playerName Nome do jogador
     * @return player_id ou null se não encontrado
     */
    public Integer getPlayerIdFromDatabase(UUID bukkitUuid, String playerName) {
        try (Connection conn = dataSource.getConnection()) {
            // Primeiro tentar buscar pelo UUID
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT player_id FROM player_data WHERE uuid = ? LIMIT 1")) {
                stmt.setString(1, bukkitUuid.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("player_id");
                    }
                }
            }
            
            // Se não encontrou pelo UUID, tentar pelo nome
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT player_id FROM player_data WHERE name = ? LIMIT 1")) {
                stmt.setString(1, playerName);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("player_id");
                    }
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar player_id: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Obtém o player_id de um jogador diretamente pelo nome.
     * Método otimizado para comandos que precisam apenas do player_id.
     * 
     * @param playerName Nome do jogador
     * @return player_id ou null se não encontrado
     */
    public Integer getPlayerIdByName(String playerName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT player_id FROM player_data WHERE name = ? LIMIT 1")) {
            
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_id");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar player_id por nome: " + e.getMessage());
        }
        
        return null;
    }
}


