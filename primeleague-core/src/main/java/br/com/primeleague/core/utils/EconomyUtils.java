package br.com.primeleague.core.utils;

import java.text.DecimalFormat;
import java.util.logging.Logger;

/**
 * Utilitários para o sistema econômico Prime League V2.0.
 * 
 * Características:
 * - Validações anti-inflacionárias
 * - Formatação de moeda consistente
 * - Valores configuráveis via config.yml
 * - Logs de auditoria
 * 
 * @author Prime League Team
 * @version 2.0.0
 */
public final class EconomyUtils {

    private static final Logger logger = Logger.getLogger(EconomyUtils.class.getName());
    
    // Formatação de moeda
    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("#,##0.00");
    
    // Valores padrão (serão sobrescritos pela configuração)
    private static double maxTransactionAmount = 10000.0;
    private static double minShopPrice = 0.01;
    private static double transactionFeeRate = 0.01;
    private static double marketTaxRate = 0.05;
    private static double shopMaintenanceFee = 10.0;
    private static double suspiciousAmountThreshold = 5000.0;
    private static int maxDailyTransactions = 1000;
    
    /**
     * Inicializa os valores de configuração.
     * Deve ser chamado pelo EconomyManager na inicialização.
     */
    public static void initializeConfiguration(double maxTransaction, double minPrice, 
                                             double transactionFee, double marketTax, 
                                             double maintenanceFee, double suspiciousThreshold,
                                             int maxDaily) {
        maxTransactionAmount = maxTransaction;
        minShopPrice = minPrice;
        transactionFeeRate = transactionFee;
        marketTaxRate = marketTax;
        shopMaintenanceFee = maintenanceFee;
        suspiciousAmountThreshold = suspiciousThreshold;
        maxDailyTransactions = maxDaily;
        
        logger.info("💰 [ECONOMY-UTILS] Configuração inicializada:");
        logger.info("   - Max Transaction: " + formatCurrency(maxTransactionAmount));
        logger.info("   - Min Shop Price: " + formatCurrency(minShopPrice));
        logger.info("   - Transaction Fee: " + (transactionFeeRate * 100) + "%");
        logger.info("   - Market Tax: " + (marketTaxRate * 100) + "%");
        logger.info("   - Maintenance Fee: " + formatCurrency(shopMaintenanceFee));
    }
    
    /**
     * Valida se um valor monetário é válido e dentro dos limites.
     * 
     * @param amount Valor a ser validado
     * @return true se o valor for válido, false caso contrário
     */
    public static boolean isValidAmount(double amount) {
        if (amount < 0) {
            logger.warning("⚠️ [ECONOMY-VALIDATION] Valor negativo rejeitado: " + amount);
            return false;
        }
        
        if (amount > maxTransactionAmount) {
            logger.warning("⚠️ [ECONOMY-VALIDATION] Valor excede limite máximo: " + amount + " > " + maxTransactionAmount);
            return false;
        }
        
        if (amount < minShopPrice && amount > 0) {
            logger.warning("⚠️ [ECONOMY-VALIDATION] Valor abaixo do mínimo: " + amount + " < " + minShopPrice);
            return false;
        }
        
        return true;
    }
    
    /**
     * Valida se um preço de loja é válido.
     * 
     * @param price Preço a ser validado
     * @return true se o preço for válido, false caso contrário
     */
    public static boolean isValidShopPrice(double price) {
        if (price < minShopPrice) {
            logger.warning("⚠️ [ECONOMY-VALIDATION] Preço de loja abaixo do mínimo: " + price + " < " + minShopPrice);
            return false;
        }
        
        return isValidAmount(price);
    }
    
    /**
     * Verifica se um valor é suspeito (para detecção de fraudes).
     * 
     * @param amount Valor a ser verificado
     * @return true se o valor for suspeito, false caso contrário
     */
    public static boolean isSuspiciousAmount(double amount) {
        return amount >= suspiciousAmountThreshold;
    }
    
    /**
     * Calcula a taxa de transação para um valor.
     * 
     * @param amount Valor da transação
     * @return Taxa calculada
     */
    public static double calculateTransactionFee(double amount) {
        return amount * transactionFeeRate;
    }
    
    /**
     * Calcula a taxa de mercado para um valor.
     * 
     * @param amount Valor da transação
     * @return Taxa calculada
     */
    public static double calculateMarketTax(double amount) {
        return amount * marketTaxRate;
    }
    
    /**
     * Obtém a taxa de manutenção de lojas.
     * 
     * @return Taxa de manutenção
     */
    public static double getShopMaintenanceFee() {
        return shopMaintenanceFee;
    }
    
    /**
     * Obtém o limite máximo de transação.
     * 
     * @return Limite máximo
     */
    public static double getMaxTransactionAmount() {
        return maxTransactionAmount;
    }
    
    /**
     * Obtém o preço mínimo de loja.
     * 
     * @return Preço mínimo
     */
    public static double getMinShopPrice() {
        return minShopPrice;
    }
    
    /**
     * Obtém o limite máximo de transações diárias.
     * 
     * @return Limite máximo
     */
    public static int getMaxDailyTransactions() {
        return maxDailyTransactions;
    }
    
    /**
     * Formata um valor monetário para exibição.
     * 
     * @param amount Valor a ser formatado
     * @return String formatada
     */
    public static String formatCurrency(double amount) {
        return "R$ " + CURRENCY_FORMAT.format(amount);
    }
    
    /**
     * Formata um valor monetário de forma compacta.
     * Implementação compatível com Java 7/8 (sem NumberFormat.Style).
     * 
     * @param amount Valor a ser formatado
     * @return String formatada compacta
     */
    public static String formatCompactCurrency(double amount) {
        if (amount < 1000) {
            return formatCurrency(amount); // Usa o formato normal para valores baixos
        }
        if (amount < 1000000) {
            // Formata como 1.2k, 15.5k, etc.
            return "R$ " + String.format("%.1fk", amount / 1000.0).replace(",", ".");
        }
        if (amount < 1000000000) {
            // Formata como 1.23M, 25.50M, etc.
            return "R$ " + String.format("%.2fM", amount / 1000000.0).replace(",", ".");
        }
        // Adicione mais sufixos (B para Bilhões, T para Trilhões) se necessário
        return "R$ " + String.format("%.2fB", amount / 1000000000.0).replace(",", ".");
    }
    
    /**
     * Formata um valor monetário para logs.
     * 
     * @param amount Valor a ser formatado
     * @return String formatada para logs
     */
    public static String formatForLogs(double amount) {
        return String.format("%.2f", amount);
    }
    
    /**
     * Valida se uma quantidade de transações diárias é válida.
     * 
     * @param dailyCount Quantidade de transações
     * @return true se válida, false caso contrário
     */
    public static boolean isValidDailyTransactionCount(int dailyCount) {
        return dailyCount >= 0 && dailyCount <= maxDailyTransactions;
    }
    
    /**
     * Calcula o valor total com taxa de transação.
     * 
     * @param baseAmount Valor base
     * @return Valor total com taxa
     */
    public static double calculateTotalWithFee(double baseAmount) {
        return baseAmount + calculateTransactionFee(baseAmount);
    }
    
    /**
     * Calcula o valor base a partir do valor total com taxa.
     * 
     * @param totalAmount Valor total com taxa
     * @return Valor base
     */
    public static double calculateBaseFromTotal(double totalAmount) {
        return totalAmount / (1 + transactionFeeRate);
    }
}
