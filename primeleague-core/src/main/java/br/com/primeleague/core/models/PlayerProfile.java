package br.com.primeleague.core.models;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para informações do jogador.
 * Contém dados essenciais do jogador para uso em toda a aplicação.
 * 
 * REFATORADO para arquitetura SSOT (Single Source of Truth):
 * - Removidos campos de assinatura/doação (agora em discord_users)
 * - PlayerProfile foca apenas em dados de gameplay
 * - BigDecimal para precisão monetária
 * 
 * @author PrimeLeague Team
 * @version 2.0 (SSOT Architecture)
 */
public class PlayerProfile {
    
    private UUID uuid;
    private String playerName;
    private int elo;
    private BigDecimal money; // REFATORADO: BigDecimal para precisão monetária
    private Integer clanId;
    private long totalPlaytime;
    private Date lastSeen;
    private int totalLogins;
    private PlayerStatus status;
    private Date createdAt;
    private Date updatedAt;
    
    /**
     * Status do jogador no sistema.
     */
    public enum PlayerStatus {
        ACTIVE("Ativo"),
        INACTIVE("Inativo"),
        BANNED("Banido");
        
        private final String displayName;
        
        PlayerStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Construtor padrão.
     */
    public PlayerProfile() {
        this.status = PlayerStatus.ACTIVE;
        this.elo = 1000;
        this.money = BigDecimal.ZERO; // REFATORADO: BigDecimal.ZERO em vez de 0.0
        this.totalLogins = 0;
        this.totalPlaytime = 0;
    }
    
    /**
     * Construtor com dados básicos.
     * 
     * @param uuid UUID do jogador
     * @param playerName Nome do jogador
     */
    public PlayerProfile(UUID uuid, String playerName) {
        this();
        this.uuid = uuid;
        this.playerName = playerName;
    }
    
    // Getters e Setters
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getElo() {
        return elo;
    }
    
    public void setElo(int elo) {
        this.elo = elo;
    }
    
    /**
     * Obtém o saldo monetário do jogador.
     * REFATORADO: Retorna BigDecimal para precisão absoluta.
     * 
     * @return BigDecimal representando o saldo
     */
    public BigDecimal getMoney() {
        return money;
    }
    
    /**
     * Define o saldo monetário do jogador.
     * REFATORADO: Aceita BigDecimal para precisão absoluta.
     * 
     * @param money BigDecimal representando o saldo
     */
    public void setMoney(BigDecimal money) {
        this.money = money != null ? money : BigDecimal.ZERO;
    }
    
    /**
     * Define o saldo monetário do jogador usando double (método de compatibilidade).
     * ATENÇÃO: Este método converte double para BigDecimal, mas é recomendado
     * usar setMoney(BigDecimal) diretamente para evitar imprecisões.
     * 
     * @param money Valor double representando o saldo
     * @deprecated Use setMoney(BigDecimal) para precisão absoluta
     */
    @Deprecated
    public void setMoney(double money) {
        this.money = BigDecimal.valueOf(money);
    }
    
    /**
     * Obtém o saldo monetário como double (método de compatibilidade).
     * ATENÇÃO: Este método pode causar perda de precisão. Use getMoney() 
     * para obter o valor exato como BigDecimal.
     * 
     * @return double representando o saldo
     * @deprecated Use getMoney() para precisão absoluta
     */
    @Deprecated
    public double getMoneyAsDouble() {
        return money.doubleValue();
    }
    
    public Integer getClanId() {
        return clanId;
    }
    
    public void setClanId(Integer clanId) {
        this.clanId = clanId;
    }
    
    public long getTotalPlaytime() {
        return totalPlaytime;
    }
    
    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }
    

    
    public Date getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public int getTotalLogins() {
        return totalLogins;
    }
    
    public void setTotalLogins(int totalLogins) {
        this.totalLogins = totalLogins;
    }
    
    public PlayerStatus getStatus() {
        return status;
    }
    
    public void setStatus(PlayerStatus status) {
        this.status = status;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    

    

    

    
    /**
     * Adiciona uma quantia ao saldo do jogador.
     * 
     * @param amount Quantia a adicionar (BigDecimal)
     */
    public void addMoney(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            this.money = this.money.add(amount);
        }
    }
    
    /**
     * Remove uma quantia do saldo do jogador.
     * 
     * @param amount Quantia a remover (BigDecimal)
     * @return true se a operação foi bem-sucedida, false se saldo insuficiente
     */
    public boolean removeMoney(BigDecimal amount) {
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            if (this.money.compareTo(amount) >= 0) {
                this.money = this.money.subtract(amount);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica se o jogador tem saldo suficiente.
     * 
     * @param amount Quantia a verificar (BigDecimal)
     * @return true se tem saldo suficiente, false caso contrário
     */
    public boolean hasEnoughMoney(BigDecimal amount) {
        return amount != null && this.money.compareTo(amount) >= 0;
    }
    
    @Override
    public String toString() {
        return "PlayerProfile{" +
                "uuid=" + uuid +
                ", playerName='" + playerName + '\'' +
                ", elo=" + elo +
                ", money=" + money +
                ", status=" + status +
                '}';
    }
}
