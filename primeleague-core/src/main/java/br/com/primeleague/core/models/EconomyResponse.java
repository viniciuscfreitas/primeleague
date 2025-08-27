package br.com.primeleague.core.models;

import java.math.BigDecimal;

/**
 * DTO para encapsular o resultado de uma transação econômica.
 * Contém informações sobre sucesso/falha, novo saldo e mensagens de erro.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class EconomyResponse {
    
    private final boolean success;
    private final BigDecimal newBalance;
    private final String errorMessage;
    private final String transactionId;
    
    // Construtor para transação bem-sucedida
    public EconomyResponse(BigDecimal newBalance, String transactionId) {
        this.success = true;
        this.newBalance = newBalance;
        this.errorMessage = null;
        this.transactionId = transactionId;
    }
    
    // Construtor para transação falhada
    public EconomyResponse(String errorMessage) {
        this.success = false;
        this.newBalance = null;
        this.errorMessage = errorMessage;
        this.transactionId = null;
    }
    
    // Métodos estáticos para facilitar criação
    public static EconomyResponse success(double newBalance) {
        return new EconomyResponse(BigDecimal.valueOf(newBalance), "TXN-" + System.currentTimeMillis());
    }
    
    public static EconomyResponse error(String errorMessage) {
        return new EconomyResponse(errorMessage);
    }
    
    // Métodos de acesso
    public boolean isSuccess() {
        return success;
    }
    
    public BigDecimal getNewBalance() {
        return newBalance;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    /**
     * Retorna uma mensagem formatada para o jogador.
     */
    public String getPlayerMessage() {
        if (success) {
            return "§aTransação realizada com sucesso! Novo saldo: §f$" + newBalance.toString();
        } else {
            return "§cErro na transação: §f" + errorMessage;
        }
    }
    
    @Override
    public String toString() {
        if (success) {
            return "EconomyResponse{success=true, newBalance=" + newBalance + ", transactionId='" + transactionId + "'}";
        } else {
            return "EconomyResponse{success=false, errorMessage='" + errorMessage + "'}";
        }
    }
}
