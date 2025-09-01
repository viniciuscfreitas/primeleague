package br.com.primeleague.core.managers;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.enums.TransactionReason;
import br.com.primeleague.core.models.EconomyResponse;
import br.com.primeleague.core.utils.EconomyUtils;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * Gerenciador centralizado da economia do Prime League V2.0.
 * 
 * Características:
 * - Controle de concorrência com ReentrantLock por player_id
 * - Transações atômicas e seguras
 * - Auditoria completa de todas as operações
 * - Cache em memória para performance
 * - Integração com sistema de doadores
 * - Validações anti-inflacionárias configuráveis
 * - API thread-safe para uso por outros módulos
 * 
 * @author PrimeLeague Team
 * @version 2.0.0
 */
public final class EconomyManager {

    private final PrimeLeagueCore plugin;
    private final Logger logger;
    private final HikariDataSource dataSource;
    private final DonorManager donorManager;
    
    // Cache em memória para saldos (player_id -> saldo)
    private final Map<Integer, BigDecimal> balanceCache = new ConcurrentHashMap<>();
    
    // Sistema de locks para controle de concorrência
    private final Map<Integer, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    
    // Contadores de transações diárias por jogador
    private final Map<Integer, Integer> dailyTransactionCounts = new ConcurrentHashMap<>();
    
    // Configurações carregadas do config.yml
    private double initialBalance;
    private boolean enableTransactionLogs;
    private String logLevel;
    
    // SQLs para operações econômicas
    private static final String GET_BALANCE_SQL = 
        "SELECT money FROM player_data WHERE player_id = ?";
    
    private static final String UPDATE_BALANCE_SQL = 
        "UPDATE player_data SET money = ? WHERE player_id = ?";
    
    private static final String INSERT_ECONOMY_LOG_SQL = 
        "INSERT INTO economy_logs (player_id, change_type, amount, balance_before, new_balance, reason, context_info, related_player_id) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    private static final String CREATE_ECONOMY_LOGS_TABLE_SQL = 
        "CREATE TABLE IF NOT EXISTS economy_logs (" +
        "log_id INT AUTO_INCREMENT PRIMARY KEY," +
        "player_id INT NOT NULL," +
        "change_type ENUM('CREDIT', 'DEBIT', 'ADMIN_GIVE', 'ADMIN_TAKE', 'ADMIN_SET', 'PLAYER_SHOP_SALE', 'PLAYER_SHOP_PURCHASE', 'ADMIN_SHOP_PURCHASE', 'PLAYER_TRANSFER', 'CLAN_BANK_DEPOSIT', 'CLAN_BANK_WITHDRAW', 'CLAN_TAX_COLLECTION', 'SYSTEM_REWARD', 'SYSTEM_PENALTY', 'BOUNTY_REWARD', 'BOUNTY_PAYMENT', 'EVENT_REWARD', 'TOURNAMENT_PRIZE', 'OTHER') NOT NULL," +
        "amount DECIMAL(15,2) NOT NULL," +
        "balance_before DECIMAL(15,2) NOT NULL," +
        "new_balance DECIMAL(15,2) NOT NULL," +
        "reason VARCHAR(100) NOT NULL," +
        "context_info TEXT," +
        "related_player_id INT," +
        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
        "FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE," +
        "FOREIGN KEY (related_player_id) REFERENCES player_data(player_id) ON DELETE SET NULL" +
        ")";

    public EconomyManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataSource = plugin.getDataManager().getDataSource();
        this.donorManager = plugin.getDonorManager();
        
        loadConfiguration();
        initializeEconomyUtils();
        createEconomyLogsTable();
        
        logger.info("🔄 [ECONOMY] EconomyManager V2.0 inicializado");
    }
    
    /**
     * Carrega as configurações do config.yml.
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        
        this.initialBalance = config.getDouble("economy.initial_balance", 100.0);
        this.enableTransactionLogs = config.getBoolean("economy.enable_transaction_logs", true);
        this.logLevel = config.getString("economy.log_level", "INFO");
        
        logger.info("💰 [ECONOMY-CONFIG] Configuração carregada:");
        logger.info("   - Saldo inicial: " + EconomyUtils.formatCurrency(initialBalance));
        logger.info("   - Logs habilitados: " + enableTransactionLogs);
        logger.info("   - Nível de log: " + logLevel);
    }
    
    /**
     * Inicializa o EconomyUtils com as configurações carregadas.
     */
    private void initializeEconomyUtils() {
        FileConfiguration config = plugin.getConfig();
        
        double maxTransaction = config.getDouble("economy.max_transaction_amount", 10000.0);
        double minPrice = config.getDouble("economy.min_shop_price", 0.01);
        double transactionFee = config.getDouble("economy.transaction_fee_rate", 0.01);
        double marketTax = config.getDouble("economy.market_tax_rate", 0.05);
        double maintenanceFee = config.getDouble("economy.shop_maintenance_fee", 10.0);
        double suspiciousThreshold = config.getDouble("security.suspicious_amount_threshold", 5000.0);
        int maxDaily = config.getInt("security.max_daily_transactions", 1000);
        
        EconomyUtils.initializeConfiguration(maxTransaction, minPrice, transactionFee, 
                                           marketTax, maintenanceFee, suspiciousThreshold, maxDaily);
    }
    
    /**
     * Cria a tabela de logs econômicos se não existir.
     */
    private void createEconomyLogsTable() {
        if (!enableTransactionLogs) {
            logger.info("ℹ️ [ECONOMY-LOGS] Logs de transação desabilitados na configuração");
            return;
        }
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(CREATE_ECONOMY_LOGS_TABLE_SQL)) {
            
            ps.executeUpdate();
            logger.info("✅ [ECONOMY-LOGS] Tabela de logs econômicos criada/verificada");
            
        } catch (SQLException e) {
            logger.severe("🚨 [ECONOMY-LOGS] Erro ao criar tabela de logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtém o lock para um player_id específico.
     * Cria um novo lock se não existir.
     */
    private ReentrantLock getPlayerLock(int playerId) {
        ReentrantLock lock = playerLocks.get(playerId);
        if (lock == null) {
            lock = new ReentrantLock();
            ReentrantLock existingLock = playerLocks.putIfAbsent(playerId, lock);
            if (existingLock != null) {
                lock = existingLock;
            }
        }
        return lock;
    }

    /**
     * Obtém o saldo de um jogador.
     * 
     * @param playerId ID do jogador
     * @return Saldo atual do jogador
     */
    public BigDecimal getBalance(int playerId) {
        // Primeiro verificar cache
        BigDecimal cachedBalance = balanceCache.get(playerId);
        if (cachedBalance != null) {
            return cachedBalance;
        }
        
        // Se não está no cache, buscar no banco
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(GET_BALANCE_SQL)) {
            
            ps.setInt(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("money");
                    // Adicionar ao cache
                    balanceCache.put(playerId, balance);
                    return balance;
                } else {
                    // Jogador não encontrado, retornar saldo inicial
                    logger.warning("⚠️ [ECONOMY-GET] Player ID não encontrado: " + playerId + " - usando saldo inicial");
                    return BigDecimal.valueOf(initialBalance);
                }
            }
            
        } catch (SQLException e) {
            logger.severe("🚨 [ECONOMY-GET] Erro ao buscar saldo: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Obtém o saldo de um jogador de forma ASSÍNCRONA.
     * Primeiro verifica o cache; se não encontrar, busca no banco em outra thread.
     * 
     * @param playerId ID do jogador
     * @param callback Callback para receber o resultado
     */
    public void getBalanceAsync(int playerId, java.util.function.Consumer<BigDecimal> callback) {
        // Verificar cache primeiro (na thread principal, é seguro e rápido)
        BigDecimal cachedBalance = balanceCache.get(playerId);
        if (cachedBalance != null) {
            callback.accept(cachedBalance);
            return;
        }
        
        // Se não está no cache, buscar no banco de forma assíncrona
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            BigDecimal balanceFromDb = getBalance(playerId); // Chama a versão síncrona fora da thread principal
            
            // Retorna para a thread principal para entregar o resultado
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(balanceFromDb);
            });
        });
    }

    /**
     * Obtém o saldo de um jogador por UUID de forma ASSÍNCRONA.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback para receber o resultado
     */
    public void getBalanceAsync(UUID playerUuid, java.util.function.Consumer<BigDecimal> callback) {
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            logger.warning("⚠️ [ECONOMY-GET] Player ID não encontrado para UUID: " + playerUuid);
            callback.accept(null);
            return;
        }
        getBalanceAsync(playerId, callback);
    }
    
    /**
     * Verifica se um jogador tem saldo suficiente.
     * 
     * @param playerId ID do jogador
     * @param amount Valor necessário
     * @return true se tem saldo suficiente, false caso contrário
     */
    public boolean hasBalance(int playerId, double amount) {
        BigDecimal balance = getBalance(playerId);
        return balance.compareTo(BigDecimal.valueOf(amount)) >= 0;
    }
    
    /**
     * Verifica se um jogador tem saldo suficiente.
     * 
     * @param playerUuid UUID do jogador
     * @param amount Valor necessário
     * @return true se tem saldo suficiente, false caso contrário
     */
    public boolean hasBalance(UUID playerUuid, double amount) {
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) return false;
        
        BigDecimal balance = getBalance(playerId);
        if (balance == null) return false;
        return balance.compareTo(BigDecimal.valueOf(amount)) >= 0;
    }
    
    /**
     * Credita um valor na conta de um jogador.
     * 
     * @param playerId ID do jogador
     * @param amount Valor a creditar
     * @param reason Motivo do crédito
     * @return Resposta da operação
     */
    public EconomyResponse creditBalance(int playerId, double amount, String reason) {
        return processTransaction(playerId, amount, TransactionReason.CREDIT, reason, null);
    }
    
    /**
     * Credita um valor na conta de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param amount Valor a creditar
     * @param reason Motivo do crédito
     * @return Resposta da operação
     */
    public EconomyResponse creditBalance(UUID playerUuid, double amount, String reason) {
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return EconomyResponse.error("Player ID não encontrado para UUID: " + playerUuid);
        }
        return creditBalance(playerId.intValue(), amount, reason);
    }
    
    /**
     * Debita um valor da conta de um jogador.
     * 
     * @param playerId ID do jogador
     * @param amount Valor a debitar
     * @param reason Motivo do débito
     * @return Resposta da operação
     */
    public EconomyResponse debitBalance(int playerId, double amount, String reason) {
        return processTransaction(playerId, -amount, TransactionReason.DEBIT, reason, null);
    }
    
    /**
     * Debita um valor da conta de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param amount Valor a debitar
     * @param reason Motivo do débito
     * @return Resposta da operação
     */
    public EconomyResponse debitBalance(UUID playerUuid, double amount, String reason) {
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return EconomyResponse.error("Player ID não encontrado para UUID: " + playerUuid);
        }
        return debitBalance(playerId.intValue(), amount, reason);
    }
    
    /**
     * Processa uma compra com desconto de doador aplicado.
     * 
     * @param playerUuid UUID do jogador
     * @param originalPrice Preço original
     * @param reason Motivo da compra
     * @return Resposta da operação
     */
    public EconomyResponse processPurchase(UUID playerUuid, double originalPrice, String reason) {
        // Obter player_id e tier de doador
        Integer playerId = plugin.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return EconomyResponse.error("Player ID não encontrado para UUID: " + playerUuid);
        }
        
        // Obter tier de doador via SSOT (discord_users)
        int donorTier = 0; // TODO: Implementar consulta ao discord_users via DataManager
        
        // Calcular desconto de doador
        double discount = donorManager.getDonorDiscount(donorTier);
        double finalPrice = originalPrice * (1.0 - discount);
        
        // Log do desconto aplicado
        if (discount > 0) {
            logger.info("💰 [ECONOMY-PURCHASE] Desconto aplicado: " + playerUuid + 
                       " - Tier: " + donorTier +
                       " - Original: " + EconomyUtils.formatCurrency(originalPrice) + 
                       " - Final: " + EconomyUtils.formatCurrency(finalPrice) + 
                       " - Desconto: " + (discount * 100) + "%");
        } else {
            logger.info("💰 [ECONOMY-PURCHASE] Compra processada: " + playerUuid + 
                       " - Preço: " + EconomyUtils.formatCurrency(originalPrice) + 
                       " - Sem desconto (Tier: " + donorTier + ")");
        }
        
        return debitBalance(playerUuid, finalPrice, reason);
    }
    
    /**
     * Processa uma transação atômica.
     * 
     * @param playerId ID do jogador
     * @param amount Valor da transação (positivo para crédito, negativo para débito)
     * @param reason Motivo da transação
     * @param contextInfo Informações adicionais
     * @param relatedPlayerId ID do jogador relacionado (para transferências)
     * @return Resposta da operação
     */
    private EconomyResponse processTransaction(int playerId, double amount, TransactionReason reason, 
                                             String reasonText, Integer relatedPlayerId) {
        // Validações anti-inflacionárias
        if (!EconomyUtils.isValidAmount(Math.abs(amount))) {
            return EconomyResponse.error("Valor inválido: " + amount);
        }
        
        // Verificar limite de transações diárias
        if (!checkDailyTransactionLimit(playerId)) {
            return EconomyResponse.error("Limite de transações diárias excedido");
        }
        
        // Obter lock do jogador
        ReentrantLock lock = getPlayerLock(playerId);
        lock.lock();
        
        try {
            // Obter saldo atual
            BigDecimal currentBalance = getBalance(playerId);
            BigDecimal newBalance = currentBalance.add(BigDecimal.valueOf(amount));
            
            // Verificar se o novo saldo seria negativo
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                return EconomyResponse.error("Saldo insuficiente");
            }
            
            // Atualizar saldo no banco
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPDATE_BALANCE_SQL)) {
                
                ps.setBigDecimal(1, newBalance);
                ps.setInt(2, playerId);
                
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    // Atualizar cache
                    balanceCache.put(playerId, newBalance);
                    
                    // Incrementar contador de transações diárias
                    incrementDailyTransactionCount(playerId);
                    
                    // Log da transação
                    logTransaction(playerId, reason, amount, currentBalance, newBalance, reasonText, relatedPlayerId);
                    
                    // Log de valor suspeito
                    if (EconomyUtils.isSuspiciousAmount(Math.abs(amount))) {
                        logger.warning("⚠️ [ECONOMY-SUSPICIOUS] Transação suspeita detectada:");
                        logger.warning("   - Player ID: " + playerId);
                        logger.warning("   - Valor: " + EconomyUtils.formatCurrency(Math.abs(amount)));
                        logger.warning("   - Motivo: " + reasonText);
                    }
                    
                    return EconomyResponse.success(newBalance.doubleValue());
                } else {
                    return EconomyResponse.error("Falha ao atualizar saldo");
                }
                
            } catch (SQLException e) {
                logger.severe("🚨 [ECONOMY-TRANSACTION] Erro na transação: " + e.getMessage());
                return EconomyResponse.error("Erro interno: " + e.getMessage());
            }
            
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Verifica se o jogador não excedeu o limite de transações diárias.
     * 
     * @param playerId ID do jogador
     * @return true se dentro do limite, false caso contrário
     */
    private boolean checkDailyTransactionLimit(int playerId) {
        int currentCount = dailyTransactionCounts.getOrDefault(playerId, 0);
        return EconomyUtils.isValidDailyTransactionCount(currentCount);
    }
    
    /**
     * Incrementa o contador de transações diárias do jogador.
     * 
     * @param playerId ID do jogador
     */
    private void incrementDailyTransactionCount(int playerId) {
        dailyTransactionCounts.merge(playerId, 1, new java.util.function.BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer oldValue, Integer value) {
                return oldValue + value;
            }
        });
    }
    
    /**
     * Registra uma transação no log (método público).
     * 
     * @param playerId ID do jogador
     * @param reason Motivo da transação
     * @param amount Valor da transação
     * @param balanceBefore Saldo anterior
     * @param newBalance Novo saldo
     * @param reasonText Texto do motivo
     * @param relatedPlayerId ID do jogador relacionado
     */
    private void logTransaction(int playerId, TransactionReason reason, double amount, 
                              BigDecimal balanceBefore, BigDecimal newBalance, 
                              String reasonText, Integer relatedPlayerId) {
        if (!enableTransactionLogs) return;
        
        try (Connection conn = dataSource.getConnection()) {
            logTransaction(conn, playerId, reason, amount, balanceBefore, newBalance, reasonText, relatedPlayerId);
        } catch (SQLException e) {
            logger.severe("🚨 [ECONOMY-LOG] Erro ao obter conexão para log: " + e.getMessage());
        }
    }
    
    /**
     * Registra uma transação no log usando uma conexão existente (método privado).
     * 
     * @param conn Conexão existente (para uso em transações)
     * @param playerId ID do jogador
     * @param reason Motivo da transação
     * @param amount Valor da transação
     * @param balanceBefore Saldo anterior
     * @param newBalance Novo saldo
     * @param reasonText Texto do motivo
     * @param relatedPlayerId ID do jogador relacionado
     */
    private void logTransaction(Connection conn, int playerId, TransactionReason reason, double amount, 
                              BigDecimal balanceBefore, BigDecimal newBalance, 
                              String reasonText, Integer relatedPlayerId) throws SQLException {
        if (!enableTransactionLogs) return;
        
        try (PreparedStatement ps = conn.prepareStatement(INSERT_ECONOMY_LOG_SQL)) {
            ps.setInt(1, playerId);
            ps.setString(2, reason.name());
            ps.setBigDecimal(3, BigDecimal.valueOf(amount));
            ps.setBigDecimal(4, balanceBefore);
            ps.setBigDecimal(5, newBalance);
            ps.setString(6, reasonText);
            ps.setString(7, "EconomyManager V2.0");
            ps.setObject(8, relatedPlayerId);
            
            ps.executeUpdate();
        }
    }
    
    /**
     * Limpa o cache de um jogador específico.
     * 
     * @param playerId ID do jogador
     */
    public void clearPlayerCache(int playerId) {
        balanceCache.remove(playerId);
        playerLocks.remove(playerId);
        dailyTransactionCounts.remove(playerId);
    }
    
    /**
     * Limpa todo o cache econômico.
     */
    public void clearAllCache() {
        balanceCache.clear();
        playerLocks.clear();
        dailyTransactionCounts.clear();
        logger.info("🧹 [ECONOMY-CACHE] Cache econômico limpo");
    }
    
    /**
     * Recarrega as configurações.
     */
    public void reloadConfiguration() {
        loadConfiguration();
        initializeEconomyUtils();
        logger.info("🔄 [ECONOMY-RELOAD] Configuração recarregada");
    }
    
    /**
     * Obtém o saldo inicial configurado.
     * 
     * @return Saldo inicial
     */
    public double getInitialBalance() {
        return initialBalance;
    }
    
    /**
     * Realiza uma transferência entre dois jogadores.
     * 
     * @param fromPlayerId ID do jogador que envia
     * @param toPlayerId ID do jogador que recebe
     * @param amount Valor a transferir
     * @return Resposta da operação
     */
    public EconomyResponse transfer(int fromPlayerId, int toPlayerId, double amount) {
        if (amount <= 0) {
            return EconomyResponse.error("Valor deve ser maior que zero");
        }
        
        if (fromPlayerId == toPlayerId) {
            return EconomyResponse.error("Não é possível transferir para si mesmo");
        }
        
        // Obter locks em ordem para evitar deadlock
        ReentrantLock fromLock = getPlayerLock(fromPlayerId);
        ReentrantLock toLock = getPlayerLock(toPlayerId);
        
        // Sempre adquirir locks na mesma ordem (menor ID primeiro)
        if (fromPlayerId < toPlayerId) {
            fromLock.lock();
            toLock.lock();
        } else {
            toLock.lock();
            fromLock.lock();
        }
        
        try {
            // Realizar transferência no banco
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false); // Iniciar transação
                
                try {
                    // Verificar saldo do remetente (dentro da transação para consistência)
                    BigDecimal fromBalance;
                    try (PreparedStatement ps = conn.prepareStatement(GET_BALANCE_SQL)) {
                        ps.setInt(1, fromPlayerId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                fromBalance = rs.getBigDecimal("money");
                            } else {
                                conn.rollback();
                                return EconomyResponse.error("Remetente não encontrado");
                            }
                        }
                    }
                    
                    if (fromBalance.compareTo(BigDecimal.valueOf(amount)) < 0) {
                        conn.rollback();
                        return EconomyResponse.error("Saldo insuficiente para transferência");
                    }
                    
                    // Obter saldo do destinatário
                    BigDecimal toBalance;
                    try (PreparedStatement ps = conn.prepareStatement(GET_BALANCE_SQL)) {
                        ps.setInt(1, toPlayerId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                toBalance = rs.getBigDecimal("money");
                            } else {
                                conn.rollback();
                                return EconomyResponse.error("Destinatário não encontrado");
                            }
                        }
                    }
                    
                    // Calcular novos saldos
                    BigDecimal fromNewBalance = fromBalance.subtract(BigDecimal.valueOf(amount));
                    BigDecimal toNewBalance = toBalance.add(BigDecimal.valueOf(amount));
                    
                    // Atualizar saldo do remetente
                    try (PreparedStatement ps = conn.prepareStatement(UPDATE_BALANCE_SQL)) {
                        ps.setBigDecimal(1, fromNewBalance);
                        ps.setInt(2, fromPlayerId);
                        ps.executeUpdate();
                    }
                    
                    // Atualizar saldo do destinatário
                    try (PreparedStatement ps = conn.prepareStatement(UPDATE_BALANCE_SQL)) {
                        ps.setBigDecimal(1, toNewBalance);
                        ps.setInt(2, toPlayerId);
                        ps.executeUpdate();
                    }
                    
                                         // Registrar logs usando a conexão da transação
                     logTransaction(conn, fromPlayerId, TransactionReason.PLAYER_TRANSFER, -amount, fromBalance, fromNewBalance, "Transferência para player_id " + toPlayerId, toPlayerId);
                     logTransaction(conn, toPlayerId, TransactionReason.PLAYER_TRANSFER, amount, toBalance, toNewBalance, "Transferência de player_id " + fromPlayerId, fromPlayerId);
                    
                    conn.commit(); // Confirmar transação
                    
                    // Atualizar cache
                    balanceCache.put(fromPlayerId, fromNewBalance);
                    balanceCache.put(toPlayerId, toNewBalance);
                    
                    // Incrementar contadores de transações diárias
                    incrementDailyTransactionCount(fromPlayerId);
                    incrementDailyTransactionCount(toPlayerId);
                    
                    logger.info("✅ [ECONOMY-TRANSFER] Transferência realizada: " + fromPlayerId + " -> " + toPlayerId + " ($" + amount + ")");
                    return EconomyResponse.success(fromNewBalance.doubleValue());
                    
                } catch (SQLException e) {
                    conn.rollback(); // Reverter transação em caso de erro
                    throw e;
                }
                
            } catch (SQLException e) {
                logger.severe("🚨 [ECONOMY-TRANSFER] Erro ao realizar transferência: " + e.getMessage());
                return EconomyResponse.error("Erro interno: " + e.getMessage());
            }
            
        } finally {
            // Liberar locks na ordem inversa
            if (fromPlayerId < toPlayerId) {
                toLock.unlock();
                fromLock.unlock();
            } else {
                fromLock.unlock();
                toLock.unlock();
            }
        }
    }

    /**
     * Credita um valor na conta de um jogador de forma ASSÍNCRONA.
     * 
     * @param playerId ID do jogador
     * @param amount Valor a creditar
     * @param reason Motivo do crédito
     * @param callback Callback para receber a resposta da operação
     */
    public void creditBalanceAsync(int playerId, double amount, String reason, java.util.function.Consumer<EconomyResponse> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            EconomyResponse response = processTransaction(playerId, amount, TransactionReason.CREDIT, reason, null);
            
            // Retorna para a thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(response);
            });
        });
    }

    /**
     * Debita um valor da conta de um jogador de forma ASSÍNCRONA.
     * 
     * @param playerId ID do jogador
     * @param amount Valor a debitar
     * @param reason Motivo do débito
     * @param callback Callback para receber a resposta da operação
     */
    public void debitBalanceAsync(int playerId, double amount, String reason, java.util.function.Consumer<EconomyResponse> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            EconomyResponse response = processTransaction(playerId, -amount, TransactionReason.DEBIT, reason, null);
            
            // Retorna para a thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(response);
            });
        });
    }

    /**
     * Transfere dinheiro entre jogadores de forma ASSÍNCRONA.
     * 
     * @param fromPlayerId ID do jogador remetente
     * @param toPlayerId ID do jogador destinatário
     * @param amount Valor a transferir
     * @param callback Callback para receber o resultado
     */
    public void transferAsync(int fromPlayerId, int toPlayerId, double amount, java.util.function.Consumer<EconomyResponse> callback) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            EconomyResponse response = transfer(fromPlayerId, toPlayerId, amount);
            
            // Retorna para a thread principal
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                callback.accept(response);
            });
        });
    }
}
