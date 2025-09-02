package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.models.DonorLevel;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gerenciador do sistema de doadores Prime League V2.0.
 * 
 * Características:
 * - Sistema de níveis configurável
 * - Benefícios automáticos
 * - Cache em memória para performance
 * - Integração com EconomyManager
 * 
 * @author Prime League Team
 * @version 2.0.0
 */
public final class DonorManager {

    private final PrimeLeagueCore plugin;
    private final Logger logger;
    private final HikariDataSource dataSource;
    
    // Cache de níveis de doador por chave (bronze, silver, etc.)
    private final Map<String, DonorLevel> donorLevels = new ConcurrentHashMap<>();
    
    // Cache de níveis de doador por tier ID (1, 2, 3, etc.) - Performance O(1)
    private final Map<Integer, DonorLevel> donorLevelsById = new ConcurrentHashMap<>();
    
    // SQLs para operações de doador
    private static final String CREATE_DONOR_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS donors (" +
        "player_id INT PRIMARY KEY," +
        "donor_level VARCHAR(20) NOT NULL," +
        "total_donation DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
        "last_donation_date TIMESTAMP," +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
        "FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE" +
        ")";
    
    private static final String GET_DONOR_LEVEL_SQL = 
        "SELECT d.donor_level, d.total_donation, d.last_donation_date " +
        "FROM donors d " +
        "JOIN player_data pd ON d.player_id = pd.player_id " +
        "WHERE pd.uuid = ?";
    
    private static final String UPDATE_DONOR_LEVEL_SQL = 
        "INSERT INTO donors (player_id, donor_level, total_donation, last_donation_date) " +
        "VALUES (?, ?, ?, NOW()) " +
        "ON DUPLICATE KEY UPDATE " +
        "donor_level = VALUES(donor_level), " +
        "total_donation = VALUES(total_donation), " +
        "last_donation_date = VALUES(last_donation_date)";
    
    private static final String GET_ALL_DONORS_SQL = 
        "SELECT pd.uuid, pd.name, d.donor_level, d.total_donation, d.last_donation_date " +
        "FROM donors d " +
        "JOIN player_data pd ON d.player_id = pd.player_id " +
        "ORDER BY d.total_donation DESC";

    public DonorManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataSource = plugin.getDataManager().getDataSource();
        loadDonorLevels();
        createDonorTable();
    }
    
    /**
     * Carrega os níveis de doador da configuração.
     */
    private void loadDonorLevels() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection levelsSection = config.getConfigurationSection("donors.levels");
        
        if (levelsSection == null) {
            logger.warning("⚠️ [DONOR-LOAD] Seção 'donors.levels' não encontrada na configuração!");
            return;
        }
        
        donorLevels.clear();
        
        for (String levelKey : levelsSection.getKeys(false)) {
            ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
            if (levelSection == null) continue;
            
            DonorLevel level = new DonorLevel(
                levelKey,
                levelSection.getString("name", levelKey),
                levelSection.getDouble("discount", 0.0),
                levelSection.getDouble("min_donation", 0.0),
                levelSection.getString("color", "§f"),
                levelSection.getStringList("permissions"),
                levelSection.getInt("max-alt-accounts", 1)
            );
            
            donorLevels.put(levelKey, level);
            
            // Adicionar ao cache por tier ID para performance O(1)
            int tierId = donorLevels.size(); // 1 = bronze, 2 = silver, etc.
            donorLevelsById.put(tierId, level);
            
            logger.info("✅ [DONOR-LOAD] Nível carregado: " + level.getName() + " (" + levelKey + ") - Tier ID: " + tierId);
        }
        
        logger.info("📊 [DONOR-LOAD] " + donorLevels.size() + " níveis de doador carregados");
    }
    
    /**
     * Cria a tabela de doadores se não existir.
     */
    private void createDonorTable() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_DONOR_TABLE_SQL)) {
            
            ps.executeUpdate();
            logger.info("✅ [DONOR-DB] Tabela de doadores criada/verificada");
            
        } catch (SQLException e) {
            logger.severe("🚨 [DONOR-DB] Erro ao criar tabela de doadores: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtém o nível de doador por tier ID (performance O(1)).
     * 
     * @param tierId ID do tier (1 = bronze, 2 = silver, etc.)
     * @return Nível de doador ou null se não encontrado
     */
    public DonorLevel getDonorLevelById(int tierId) {
        return donorLevelsById.get(tierId);
    }
    
    /**
     * Obtém o nível de doador por chave (bronze, silver, etc.).
     * 
     * @param levelKey Chave do nível
     * @return Nível de doador ou null se não encontrado
     */
    public DonorLevel getDonorLevelByKey(String levelKey) {
        return donorLevels.get(levelKey);
    }
    

    
    /**
     * Atualiza o nível de doador de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param levelKey Chave do nível
     * @param totalDonation Total de doações
     * @return true se atualizado com sucesso, false caso contrário
     */
    public boolean updateDonorLevel(UUID playerUuid, String levelKey, double totalDonation) {
        // Validar nível
        if (!donorLevels.containsKey(levelKey)) {
            logger.warning("⚠️ [DONOR-UPDATE] Nível inválido: " + levelKey);
            return false;
        }
        
        // Obter player_id
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            logger.warning("⚠️ [DONOR-UPDATE] Player ID não encontrado para UUID: " + playerUuid);
            return false;
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_DONOR_LEVEL_SQL)) {
            
            ps.setInt(1, playerId);
            ps.setString(2, levelKey);
            ps.setDouble(3, totalDonation);
            
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("✅ [DONOR-UPDATE] Nível atualizado: " + playerUuid + " → " + levelKey);
                return true;
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [DONOR-UPDATE] Erro ao atualizar nível de doador: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Atualiza o nível de doador de um jogador de forma ASSÍNCRONA.
     * 
     * @param playerUuid UUID do jogador
     * @param levelKey Chave do nível
     * @param totalDonation Total de doações
     * @param callback Callback para receber o resultado
     */
    public void updateDonorLevelAsync(UUID playerUuid, String levelKey, double totalDonation, java.util.function.Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = updateDonorLevel(playerUuid, levelKey, totalDonation);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(success);
            });
        });
    }
    
    /**
     * Remove o status de doador de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return true se removido com sucesso, false caso contrário
     */
    public boolean removeDonorStatus(UUID playerUuid) {
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return false;
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM donors WHERE player_id = ?")) {
            
            ps.setInt(1, playerId);
            int rowsAffected = ps.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.info("✅ [DONOR-REMOVE] Status removido: " + playerUuid);
                return true;
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [DONOR-REMOVE] Erro ao remover status de doador: " + e.getMessage());
        }
        
        return false;
    }

    /**
     * Remove o status de doador de um jogador de forma ASSÍNCRONA.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback para receber o resultado
     */
    public void removeDonorStatusAsync(UUID playerUuid, java.util.function.Consumer<Boolean> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = removeDonorStatus(playerUuid);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(success);
            });
        });
    }
    
    /**
     * Obtém todos os doadores.
     * 
     * @return Lista de doadores ordenada por total de doações
     */
    public List<DonorInfo> getAllDonors() {
        List<DonorInfo> donors = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_ALL_DONORS_SQL);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String uuidStr = rs.getString("uuid");
                String name = rs.getString("name");
                String levelKey = rs.getString("donor_level");
                BigDecimal totalDonation = rs.getBigDecimal("total_donation");
                
                DonorLevel level = donorLevels.get(levelKey);
                if (level != null) {
                    donors.add(new DonorInfo(UUID.fromString(uuidStr), name, level, totalDonation.doubleValue()));
                }
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [DONOR-LIST] Erro ao listar doadores: " + e.getMessage());
        }
        
        return donors;
    }

    /**
     * Obtém todos os doadores de forma ASSÍNCRONA.
     * 
     * @param callback Callback para receber a lista de doadores
     */
    public void getAllDonorsAsync(java.util.function.Consumer<List<DonorInfo>> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DonorInfo> donors = getAllDonors();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(donors);
            });
        });
    }
    
    /**
     * Recarrega a configuração de níveis de doador.
     */
    public void reloadConfiguration() {
        loadDonorLevels();
        logger.info("🔄 [DONOR-RELOAD] Configuração de doadores recarregada");
    }
    
    /**
     * Obtém o desconto para um tier de doador.
     * 
     * @param tier tier do doador (0 = sem tier, 1+ = tiers de doador)
     * @return desconto (0.0 a 1.0)
     */
    public double getDiscountForTier(int tier) {
        if (tier <= 0) {
            return 0.0; // Sem tier, sem desconto
        }
        
        // Buscar nível de doador pelo tier ID
        DonorLevel level = donorLevelsById.get(tier);
        if (level != null) {
            return level.getDiscount();
        }
        
        // Fallback: buscar por chave numérica
        String tierKey = String.valueOf(tier);
        level = donorLevels.get(tierKey);
        if (level != null) {
            return level.getDiscount();
        }
        
        // Se não encontrou, retornar 0.0 (sem desconto)
        return 0.0;
    }
    
    /**
     * Obtém o desconto para um tier de doador (método legado para compatibilidade).
     * 
     * @param tier tier do doador
     * @return desconto (0.0 a 1.0)
     */
    public double getDonorDiscount(int tier) {
        return getDiscountForTier(tier);
    }
    
    /**
     * Limpa todo o cache de doadores.
     */
    public void clearAllCache() {
        donorLevels.clear();
        donorLevelsById.clear();
        logger.info("🧹 [DONOR-CACHE] Cache de doadores limpo");
    }
    
    /**
     * Classe interna para informações de doador.
     */
    public static class DonorInfo {
        private final UUID uuid;
        private final String name;
        private final DonorLevel level;
        private final double totalDonation;
        
        public DonorInfo(UUID uuid, String name, DonorLevel level, double totalDonation) {
            this.uuid = uuid;
            this.name = name;
            this.level = level;
            this.totalDonation = totalDonation;
        }
        
        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public DonorLevel getLevel() { return level; }
        public double getTotalDonation() { return totalDonation; }
    }
}
