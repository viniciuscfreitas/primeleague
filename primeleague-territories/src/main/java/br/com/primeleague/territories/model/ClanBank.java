package br.com.primeleague.territories.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modelo de dados para o banco de um clã.
 * Representa a economia virtual de um clã para manutenção de territórios.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ClanBank {
    
    private int clanId;
    private BigDecimal balance;
    private Timestamp lastMaintenance;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    public ClanBank() {}
    
    public ClanBank(int clanId) {
        this.clanId = clanId;
        this.balance = BigDecimal.ZERO;
        this.lastMaintenance = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    public ClanBank(int clanId, BigDecimal initialBalance) {
        this.clanId = clanId;
        this.balance = initialBalance;
        this.lastMaintenance = new Timestamp(System.currentTimeMillis());
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters e Setters
    public int getClanId() {
        return clanId;
    }
    
    public void setClanId(int clanId) {
        this.clanId = clanId;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public Timestamp getLastMaintenance() {
        return lastMaintenance;
    }
    
    public void setLastMaintenance(Timestamp lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Adiciona valor ao saldo do banco.
     * 
     * @param amount Valor a ser adicionado
     * @return Novo saldo
     */
    public BigDecimal deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
            this.updatedAt = new Timestamp(System.currentTimeMillis());
        }
        return this.balance;
    }
    
    /**
     * Remove valor do saldo do banco.
     * 
     * @param amount Valor a ser removido
     * @return true se a operação foi bem-sucedida, false se saldo insuficiente
     */
    public boolean withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            this.updatedAt = new Timestamp(System.currentTimeMillis());
            return true;
        }
        return false;
    }
    
    /**
     * Verifica se o banco tem saldo suficiente.
     * 
     * @param amount Valor a ser verificado
     * @return true se tem saldo suficiente
     */
    public boolean hasEnoughBalance(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }
    
    /**
     * Verifica se o banco está vazio.
     * 
     * @return true se o saldo é zero
     */
    public boolean isEmpty() {
        return this.balance.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Atualiza o timestamp da última manutenção.
     */
    public void updateLastMaintenance() {
        this.lastMaintenance = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    @Override
    public String toString() {
        return "ClanBank{" +
                "clanId=" + clanId +
                ", balance=" + balance +
                ", lastMaintenance=" + lastMaintenance +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
