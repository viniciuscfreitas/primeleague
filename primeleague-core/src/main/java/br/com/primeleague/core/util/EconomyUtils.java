package br.com.primeleague.core.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utilitários para formatação e manipulação de valores monetários.
 * Centraliza a lógica de formatação para evitar duplicação de código.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public final class EconomyUtils {
    
    /**
     * Formata um valor monetário para exibição.
     * Centraliza a formatação para garantir consistência em todo o sistema.
     * 
     * @param amount Valor a ser formatado
     * @return String formatada com 2 casas decimais
     */
    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        
        // Formatar com 2 casas decimais usando HALF_UP para arredondamento consistente
        return amount.setScale(2, RoundingMode.HALF_UP).toString();
    }
    
    /**
     * Formata um valor monetário com símbolo de moeda.
     * 
     * @param amount Valor a ser formatado
     * @return String formatada com símbolo "$"
     */
    public static String formatMoneyWithSymbol(BigDecimal amount) {
        return "$" + formatMoney(amount);
    }
    
    /**
     * Valida se um valor monetário é válido (não nulo e não negativo).
     * 
     * @param amount Valor a validar
     * @return true se o valor é válido
     */
    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    /**
     * Converte uma string para BigDecimal de forma segura.
     * 
     * @param amountStr String a converter
     * @return BigDecimal ou null se inválido
     */
    public static BigDecimal parseAmount(String amountStr) {
        try {
            return new BigDecimal(amountStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
