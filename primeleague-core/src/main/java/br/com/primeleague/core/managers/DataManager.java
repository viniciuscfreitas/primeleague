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
import java.sql.Timestamp;

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

    // SQLs refatorados para schema OTIMIZADO (SSOT)
    private static final String SELECT_PLAYER_SQL =
            "SELECT name, elo, money, total_playtime, last_seen, total_logins, status FROM player_data WHERE uuid = ? LIMIT 1";
    
    private static final String SELECT_PLAYER_WITH_CLAN_SQL =
            "SELECT pd.name, pd.elo, pd.money, pd.total_playtime, pd.last_seen, pd.total_logins, pd.status, " +
            "cp.clan_id, cp.role " +
            "FROM player_data pd " +
            "LEFT JOIN clan_players cp ON pd.player_id = cp.player_id " +
            "WHERE pd.uuid = ? LIMIT 1";
    
    private static final String UPSERT_PLAYER_SQL =
            "INSERT INTO player_data (uuid, name, elo, money, total_playtime, last_seen, total_logins, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE name = VALUES(name), elo = VALUES(elo), money = VALUES(money), " +
            "total_playtime = VALUES(total_playtime), last_seen = VALUES(last_seen), total_logins = VALUES(total_logins), status = VALUES(status)";

    // SQLs para funcionalidades P2P (refatorados)
    private static final String SELECT_PLAYER_BY_NAME_SQL =
            "SELECT uuid, name, elo, money, total_playtime, last_seen, total_logins, status FROM player_data WHERE name = ? LIMIT 1";
    
    private static final String SELECT_PLAYER_BY_NAME_WITH_CLAN_SQL =
            "SELECT pd.uuid, pd.name, pd.elo, pd.money, pd.total_playtime, pd.last_seen, pd.total_logins, pd.status, " +
            "cp.clan_id, cp.role " +
            "FROM player_data pd " +
            "LEFT JOIN clan_players cp ON pd.player_id = cp.player_id " +
            "WHERE pd.name = ? LIMIT 1";
    
    private static final String SELECT_DISCORD_LINK_SQL =
            "SELECT pd.player_id FROM discord_links dl " +
            "JOIN player_data pd ON dl.player_uuid = pd.uuid " +
            "WHERE dl.discord_id = ? AND dl.verified = TRUE LIMIT 1";
    
    private static final String INSERT_DISCORD_LINK_SQL =
            "INSERT INTO discord_links (discord_id, player_id) VALUES (?, ?)";
    
    private static final String UPDATE_SUBSCRIPTION_SQL =
            "UPDATE discord_users SET subscription_expires_at = ? WHERE discord_id = ?";
    
    private static final String ADD_SUBSCRIPTION_DAYS_SQL =
            "UPDATE discord_users SET subscription_expires_at = CASE " +
            "WHEN subscription_expires_at IS NULL OR subscription_expires_at < NOW() THEN DATE_ADD(NOW(), INTERVAL ? DAY) " +
            "ELSE DATE_ADD(subscription_expires_at, INTERVAL ? DAY) " +
            "END WHERE discord_id = ?";

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
    public void loadPlayerProfileWithCreation(UUID uuid, String playerName) {
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
                            // subscription_expires_at e donor_tier agora estão em discord_users
                            profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                            profile.setLastSeen(rs.getTimestamp("last_seen"));
                            profile.setTotalLogins(rs.getInt("total_logins"));
                            
                            String statusStr = rs.getString("status");
                            if (statusStr != null) {
                                profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                            } else {
                                profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            }
                            
                            // Dados de doador agora estão em discord_users
                            profile.setDonorTier(0); // Será carregado via Discord ID
                            profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
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
     * Carrega o perfil de um jogador SEM criar automaticamente.
     * Usado para verificação de autenticação.
     * 
     * @param uuid UUID do jogador
     * @return PlayerProfile se existir, null se não existir
     */
    public PlayerProfile loadPlayerProfile(UUID uuid) {
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
                            profile.setClanId(null);
                            profile.setTotalPlaytime(rs.getLong("total_playtime"));
                            profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                            profile.setLastSeen(rs.getTimestamp("last_seen"));
                            profile.setTotalLogins(rs.getInt("total_logins"));
                            
                            String statusStr = rs.getString("status");
                            if (statusStr != null) {
                                profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                            } else {
                                profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            }
                            
                            profile.setDonorTier(0); // Será carregado via Discord ID
                            profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
                        }
                        // Se não existir, retorna null (NÃO cria automaticamente)
                    } finally {
                        rs.close();
                    }
                } finally {
                    ps.close();
                }
            } finally {
                conn.close();
            }
            return profile;
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha ao carregar perfil: " + uuid + " - " + e.getMessage());
            e.printStackTrace();
            return null;
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
                            // subscription_expires_at agora está em discord_users
                            profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                            profile.setLastSeen(rs.getTimestamp("last_seen"));
                            profile.setTotalLogins(rs.getInt("total_logins"));
                            
                            String statusStr = rs.getString("status");
                            if (statusStr != null) {
                                profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                            } else {
                                profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                            }
                            
                            // Dados de doador agora estão em discord_users
                            profile.setDonorTier(0); // Será carregado via Discord ID
                            profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
                            
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
                    if (profile.getLastSeen() == null) {
                        ps.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
                    } else {
                        ps.setTimestamp(6, new java.sql.Timestamp(profile.getLastSeen().getTime()));
                    }
                    ps.setInt(7, profile.getTotalLogins());
                    ps.setString(8, profile.getStatus().name());
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
            stmt.setTimestamp(6, profile.getLastSeen() != null ? new java.sql.Timestamp(profile.getLastSeen().getTime()) : null);
            stmt.setInt(7, profile.getTotalLogins());
            stmt.setString(8, profile.getStatus().name());
            
            int affectedRows = stmt.executeUpdate();
    
            
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
                    // subscription_expires_at agora está em discord_users
                    profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Dados de doador agora estão em discord_users
                    profile.setDonorTier(0); // Será carregado via Discord ID
                    profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
                    
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
                    // subscription_expires_at agora está em discord_users
                    profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Dados de doador agora estão em discord_users
                    profile.setDonorTier(0); // Será carregado via Discord ID
                    profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
                    
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
             PreparedStatement stmt = conn.prepareStatement("SELECT uuid, name, elo, money, total_playtime, last_seen, total_logins, status FROM player_data WHERE player_id = ? LIMIT 1")) {
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
                    // subscription_expires_at agora está em discord_users
                    profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
                    profile.setLastSeen(rs.getTimestamp("last_seen"));
                    profile.setTotalLogins(rs.getInt("total_logins"));
                    
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        profile.setStatus(PlayerProfile.PlayerStatus.valueOf(statusStr));
                    } else {
                        profile.setStatus(PlayerProfile.PlayerStatus.ACTIVE);
                    }
                    
                    // Dados de doador agora estão em discord_users
                    profile.setDonorTier(0); // Será carregado via Discord ID
                    profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID
                    
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
                    // subscription_expires_at agora está em discord_users
                    profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
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
                    // subscription_expires_at agora está em discord_users
                    profile.setSubscriptionExpiry(null); // Será carregado via Discord ID
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
    
    /**
     * Busca o player_id associado a um Discord ID.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @return player_id ou null se não encontrado
     */
    public Integer getPlayerIdByDiscordId(String discordId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT pd.player_id FROM discord_links dl " +
                     "JOIN player_data pd ON dl.player_uuid = pd.uuid " +
                     "WHERE dl.discord_id = ? AND dl.verified = TRUE LIMIT 1")) {
            
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("player_id");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar player_id por Discord ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Busca o Discord ID de um jogador pelo nome.
     * Suporte para a API HTTP do Core.
     * 
     * @param playerName Nome do jogador
     * @return Discord ID ou null se não encontrado
     */
    public String getDiscordIdByPlayerName(String playerName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT dl.discord_id FROM discord_links dl " +
                     "JOIN player_data pd ON dl.player_uuid = pd.uuid " +
                     "WHERE pd.name = ? AND dl.verified = TRUE LIMIT 1")) {
            
            stmt.setString(1, playerName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar Discord ID por nome: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Atualiza o status de um link Discord.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @param status Novo status
     */
    public void updateDiscordLinkStatus(String discordId, String status) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE discord_links SET status = ?, updated_at = NOW() " +
                     "WHERE discord_id = ?")) {
            
            stmt.setString(1, status);
            stmt.setString(2, discordId);
            stmt.executeUpdate();
            
            plugin.getLogger().info("[DATA-MANAGER] Status Discord atualizado para " + discordId + ": " + status);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao atualizar status Discord: " + e.getMessage());
        }
    }
    
    /**
     * Busca o status de um link Discord.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @return status do link ou null se não encontrado
     */
    public String getDiscordLinkStatus(String discordId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT status FROM discord_links WHERE discord_id = ?")) {
            
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar status Discord: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Conta quantas contas estão vinculadas a um Discord ID.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @return Número de contas vinculadas
     */
    public int getDiscordLinkCount(String discordId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) as count FROM discord_links WHERE discord_id = ? AND verified = TRUE")) {
            
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao contar links Discord: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Busca o donor tier de um usuário Discord.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @return donor tier ou 0 se não encontrado
     */
    public int getDonorTierByDiscordId(String discordId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT donor_tier FROM discord_users WHERE discord_id = ?")) {
            
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("donor_tier");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar donor tier por Discord ID: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Busca a assinatura compartilhada de um usuário Discord.
     * Suporte para a API HTTP do Core.
     * 
     * @param discordId Discord ID do usuário
     * @return data de expiração da assinatura ou null se não encontrado
     */
    public java.sql.Timestamp getSharedSubscriptionByDiscordId(String discordId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT subscription_expires_at FROM discord_users WHERE discord_id = ?")) {
            
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("subscription_expires_at");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar assinatura compartilhada por Discord ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Busca o Discord ID vinculado a um player_id.
     * 
     * @param playerId ID do jogador
     * @return Discord ID ou null se não encontrado
     */
    public String getDiscordIdByPlayerId(Integer playerId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT discord_id FROM discord_links WHERE player_id = ? AND verified = TRUE LIMIT 1")) {
            
            stmt.setInt(1, playerId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao buscar Discord ID por player_id: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Autoriza um IP permanentemente para um player.
     * Suporte para autorização via Discord.
     * 
     * @param playerName Nome do player
     * @param ipAddress Endereço IP a ser autorizado
     * @throws SQLException se houver erro no banco de dados
     */
    public void authorizeIpPermanently(String playerName, String ipAddress) throws SQLException {
        // Primeiro, buscar o player_id pelo nome
        Integer playerId = getPlayerIdByName(playerName);
        if (playerId == null) {
            throw new SQLException("Player não encontrado: " + playerName);
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO player_authorized_ips (player_id, ip_address, authorized_at) " +
                     "VALUES (?, ?, NOW()) " +
                     "ON DUPLICATE KEY UPDATE authorized_at = NOW()")) {
            
            stmt.setInt(1, playerId);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
            
            plugin.getLogger().info("[IP-AUTH] IP " + ipAddress + " autorizado permanentemente para " + playerName + " (ID: " + playerId + ")");
        }
    }
    
    /**
     * Verifica se um IP está autorizado para um player.
     * 
     * @param playerName Nome do player
     * @param ipAddress Endereço IP a verificar
     * @return true se autorizado, false caso contrário
     */
    public boolean isIpAuthorized(String playerName, String ipAddress) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT 1 FROM player_authorized_ips pai " +
                     "JOIN player_data pd ON pai.player_id = pd.player_id " +
                     "WHERE pd.name = ? AND pai.ip_address = ? LIMIT 1")) {
            
            stmt.setString(1, playerName);
            stmt.setString(2, ipAddress);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("🚨 [DATA-MANAGER] Erro ao verificar autorização de IP: " + e.getMessage());
        }
        
        return false;
    }
    
    // =====================================================
    // SISTEMA DE TRANSFERÊNCIA DE ASSINATURAS (FASE 2)
    // =====================================================
    
    /**
     * Transfere assinatura de um Discord ID para outro de forma atômica.
     * Implementa lógica de negócio centrada no cliente.
     * 
     * @param playerName Nome do jogador
     * @param newDiscordId Novo Discord ID
     * @param ipAddress IP de origem da operação
     * @return Resultado da transferência
     */
    public TransferResult transferSubscription(String playerName, String newDiscordId, String ipAddress) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // Iniciar transação
            
            // 1. Buscar player_id e discord_id atual
            Integer playerId = getPlayerIdByName(playerName);
            if (playerId == null) {
                conn.rollback();
                return TransferResult.error("Jogador não encontrado: " + playerName);
            }
            
            String oldDiscordId = getDiscordIdByPlayerId(playerId);
            if (oldDiscordId == null) {
                conn.rollback();
                return TransferResult.error("Jogador não possui vínculo Discord ativo");
            }
            
            if (oldDiscordId.equals(newDiscordId)) {
                conn.rollback();
                return TransferResult.error("Novo Discord ID é igual ao atual");
            }
            
            // 2. Buscar dados das assinaturas
            SubscriptionData oldSubscription = getSubscriptionData(conn, oldDiscordId);
            if (oldSubscription == null) {
                conn.rollback();
                return TransferResult.error("Assinatura atual não encontrada");
            }
            
            SubscriptionData newSubscription = getSubscriptionData(conn, newDiscordId);
            
            // 3. Executar transferência com lógica de negócio refinada
            boolean transferSuccess = executeTransferWithBusinessLogic(conn, oldSubscription, newSubscription, newDiscordId);
            if (!transferSuccess) {
                conn.rollback();
                return TransferResult.error("Falha na transferência da assinatura");
            }
            
            // 4. Atualizar vínculo Discord
            boolean linkSuccess = updateDiscordLink(conn, playerId, newDiscordId);
            if (!linkSuccess) {
                conn.rollback();
                return TransferResult.error("Falha na atualização do vínculo Discord");
            }
            
            // 5. Registrar auditoria
            logTransferAction(conn, playerId, oldDiscordId, newDiscordId, oldSubscription, newSubscription, ipAddress);
            
            conn.commit();
            return TransferResult.success("Transferência realizada com sucesso");
            
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            plugin.getLogger().severe("🚨 [TRANSFER] Erro na transferência: " + e.getMessage());
            return TransferResult.error("Erro interno: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
    }
    
    /**
     * Executa transferência seguindo as regras de negócio centradas no cliente.
     */
    private boolean executeTransferWithBusinessLogic(Connection conn, SubscriptionData oldSub, SubscriptionData newSub, String newDiscordId) throws SQLException {
        // Cenário A: Nenhuma assinatura no destino
        if (newSub == null) {
            return copySubscription(conn, oldSub, newDiscordId);
        }
        
        // Cenário B: Tiers iguais - Somar durações
        if (oldSub.getSubscriptionType().equals(newSub.getSubscriptionType()) && 
            oldSub.getDonorTier().equals(newSub.getDonorTier())) {
            return mergeEqualTiers(conn, oldSub, newSub, newDiscordId);
        }
        
        // Cenário C: Tier origem > Tier destino - Sobrescrever
        if (isSubscriptionHigher(oldSub, newSub)) {
            return copySubscription(conn, oldSub, newDiscordId);
        }
        
        // Cenário D: Tier origem < Tier destino - Manter maior e somar duração
        return extendHigherTier(conn, oldSub, newSub, newDiscordId);
    }
    
    /**
     * Verifica se a assinatura de origem é superior à de destino.
     */
    private boolean isSubscriptionHigher(SubscriptionData oldSub, SubscriptionData newSub) {
        // Comparar subscription_type (VIP > PREMIUM > BASIC)
        int oldTypeValue = getSubscriptionTypeValue(oldSub.getSubscriptionType());
        int newTypeValue = getSubscriptionTypeValue(newSub.getSubscriptionType());
        
        if (oldTypeValue > newTypeValue) return true;
        if (oldTypeValue < newTypeValue) return false;
        
        // Se tipos iguais, comparar donor_tier
        return oldSub.getDonorTier() > newSub.getDonorTier();
    }
    
    /**
     * Obtém valor numérico do tipo de assinatura.
     */
    private int getSubscriptionTypeValue(String subscriptionType) {
        switch (subscriptionType.toUpperCase()) {
            case "VIP": return 3;
            case "PREMIUM": return 2;
            case "BASIC": return 1;
            default: return 0;
        }
    }
    
    /**
     * Copia assinatura completa para o destino.
     */
    private boolean copySubscription(Connection conn, SubscriptionData source, String targetDiscordId) throws SQLException {
        String sql = "INSERT INTO discord_users (discord_id, donor_tier, donor_tier_expires_at, subscription_expires_at, subscription_type) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "donor_tier = VALUES(donor_tier), " +
                     "donor_tier_expires_at = VALUES(donor_tier_expires_at), " +
                     "subscription_expires_at = VALUES(subscription_expires_at), " +
                     "subscription_type = VALUES(subscription_type)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, targetDiscordId);
            stmt.setInt(2, source.getDonorTier());
            stmt.setTimestamp(3, source.getDonorTierExpiresAt());
            stmt.setTimestamp(4, source.getSubscriptionExpiresAt());
            stmt.setString(5, source.getSubscriptionType());
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Soma durações de tiers iguais.
     */
    private boolean mergeEqualTiers(Connection conn, SubscriptionData oldSub, SubscriptionData newSub, String targetDiscordId) throws SQLException {
        // Calcular novas durações somadas
        Timestamp newDonorExpiry = addTimestamps(oldSub.getDonorTierExpiresAt(), newSub.getDonorTierExpiresAt());
        Timestamp newSubscriptionExpiry = addTimestamps(oldSub.getSubscriptionExpiresAt(), newSub.getSubscriptionExpiresAt());
        
        String sql = "UPDATE discord_users SET " +
                     "donor_tier_expires_at = ?, " +
                     "subscription_expires_at = ? " +
                     "WHERE discord_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, newDonorExpiry);
            stmt.setTimestamp(2, newSubscriptionExpiry);
            stmt.setString(3, targetDiscordId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Estende duração mantendo o tier mais alto.
     */
    private boolean extendHigherTier(Connection conn, SubscriptionData oldSub, SubscriptionData newSub, String targetDiscordId) throws SQLException {
        // Manter o tier mais alto e somar a duração do tier menor
        Timestamp extendedDonorExpiry = addTimestamps(oldSub.getDonorTierExpiresAt(), newSub.getDonorTierExpiresAt());
        Timestamp extendedSubscriptionExpiry = addTimestamps(oldSub.getSubscriptionExpiresAt(), newSub.getSubscriptionExpiresAt());
        
        String sql = "UPDATE discord_users SET " +
                     "donor_tier_expires_at = ?, " +
                     "subscription_expires_at = ? " +
                     "WHERE discord_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, extendedDonorExpiry);
            stmt.setTimestamp(2, extendedSubscriptionExpiry);
            stmt.setString(3, targetDiscordId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Soma duas durações de timestamp, considerando valores nulos.
     */
    private Timestamp addTimestamps(Timestamp t1, Timestamp t2) {
        if (t1 == null && t2 == null) return null;
        if (t1 == null) return t2;
        if (t2 == null) return t1;
        
        // Calcular diferença em milissegundos desde agora
        long now = System.currentTimeMillis();
        long diff1 = Math.max(0, t1.getTime() - now);
        long diff2 = Math.max(0, t2.getTime() - now);
        
        // Somar as durações restantes
        return new Timestamp(now + diff1 + diff2);
    }
    
    /**
     * Busca dados de assinatura de um Discord ID.
     */
    private SubscriptionData getSubscriptionData(Connection conn, String discordId) throws SQLException {
        String sql = "SELECT donor_tier, donor_tier_expires_at, subscription_expires_at, subscription_type " +
                     "FROM discord_users WHERE discord_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, discordId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new SubscriptionData(
                        discordId,
                        rs.getInt("donor_tier"),
                        rs.getTimestamp("donor_tier_expires_at"),
                        rs.getTimestamp("subscription_expires_at"),
                        rs.getString("subscription_type")
                    );
                }
            }
        }
        return null;
    }
    
    /**
     * Atualiza vínculo Discord para novo ID.
     */
    private boolean updateDiscordLink(Connection conn, Integer playerId, String newDiscordId) throws SQLException {
        String sql = "UPDATE discord_links SET discord_id = ? WHERE player_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDiscordId);
            stmt.setInt(2, playerId);
            return stmt.executeUpdate() > 0;
        }
    }
    
    /**
     * Registra auditoria da transferência.
     */
    private void logTransferAction(Connection conn, Integer playerId, String oldDiscordId, String newDiscordId, 
                                  SubscriptionData oldSub, SubscriptionData newSub, String ipAddress) throws SQLException {
        String sql = "INSERT INTO discord_link_history (action, player_id, discord_id_old, discord_id_new, details, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        String details = String.format("{\"old_subscription\":\"%s\",\"new_subscription\":\"%s\",\"timestamp\":\"%s\"}", 
                                      oldSub != null ? oldSub.getSubscriptionType() : "NONE",
                                      newSub != null ? newSub.getSubscriptionType() : "NONE",
                                      new java.sql.Timestamp(System.currentTimeMillis()));
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "TRANSFER_SUB");
            stmt.setInt(2, playerId);
            stmt.setString(3, oldDiscordId);
            stmt.setString(4, newDiscordId);
            stmt.setString(5, details);
            stmt.setString(6, ipAddress);
            stmt.executeUpdate();
        }
    }
    
    // =====================================================
    // CLASSES DE SUPORTE PARA TRANSFERÊNCIA
    // =====================================================
    
    /**
     * DTO para dados de assinatura.
     */
    public static class SubscriptionData {
        private final String discordId;
        private final Integer donorTier;
        private final java.sql.Timestamp donorTierExpiresAt;
        private final java.sql.Timestamp subscriptionExpiresAt;
        private final String subscriptionType;
        
        public SubscriptionData(String discordId, Integer donorTier, java.sql.Timestamp donorTierExpiresAt, 
                               java.sql.Timestamp subscriptionExpiresAt, String subscriptionType) {
            this.discordId = discordId;
            this.donorTier = donorTier;
            this.donorTierExpiresAt = donorTierExpiresAt;
            this.subscriptionExpiresAt = subscriptionExpiresAt;
            this.subscriptionType = subscriptionType;
        }
        
        // Getters
        public String getDiscordId() { return discordId; }
        public Integer getDonorTier() { return donorTier; }
        public java.sql.Timestamp getDonorTierExpiresAt() { return donorTierExpiresAt; }
        public java.sql.Timestamp getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
        public String getSubscriptionType() { return subscriptionType; }
    }
    
    /**
     * Resultado da transferência.
     */
    public static class TransferResult {
        private final boolean success;
        private final String message;
        private final String details;
        
        private TransferResult(boolean success, String message, String details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }
        
        public static TransferResult success(String details) {
            return new TransferResult(true, "Transferência realizada com sucesso", details);
        }
        
        public static TransferResult error(String message) {
            return new TransferResult(false, message, null);
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }
}


