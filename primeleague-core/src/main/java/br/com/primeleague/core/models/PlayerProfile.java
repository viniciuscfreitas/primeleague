package br.com.primeleague.core.models;

import java.util.UUID;
import java.util.Date;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para informações do jogador.
 * Contém dados essenciais do jogador para uso em toda a aplicação.
 * 
 * REFATORADO para usar BigDecimal no campo money, garantindo precisão absoluta
 * em operações monetárias conforme diretrizes do Arquiteto.
 * 
 * @author PrimeLeague Team
 * @version 1.1
 */
public class PlayerProfile {
    
    private UUID uuid;
    private String playerName;
    private int elo;
    private BigDecimal money; // REFATORADO: BigDecimal para precisão monetária
    private Integer clanId;
    private long totalPlaytime;
    private Date subscriptionExpiry;
    private Date lastSeen;
    private int totalLogins;
    private PlayerStatus status;
    private int donorTier;
    private Date donorTierExpiresAt;
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
        this.donorTier = 0; // Sem tier de doador por padrão
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
    
    public Date getSubscriptionExpiry() {
        return subscriptionExpiry;
    }
    
    public void setSubscriptionExpiry(Date subscriptionExpiry) {
        this.subscriptionExpiry = subscriptionExpiry;
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
    
    // ==================== DOADOR TIER ====================
    
    public int getDonorTier() {
        return donorTier;
    }
    
    public void setDonorTier(int donorTier) {
        this.donorTier = Math.max(0, donorTier); // Garantir que não seja negativo
    }
    
    public Date getDonorTierExpiresAt() {
        return donorTierExpiresAt;
    }
    
    public void setDonorTierExpiresAt(Date donorTierExpiresAt) {
        this.donorTierExpiresAt = donorTierExpiresAt;
    }
    
    /**
     * Verifica se o tier de doador está ativo (não expirou).
     * 
     * @return true se o tier está ativo, false caso contrário
     */
    public boolean hasActiveDonorTier() {
        if (donorTier <= 0) {
            return false; // Sem tier de doador
        }
        
        if (donorTierExpiresAt == null) {
            return true; // Tier permanente
        }
        
        return donorTierExpiresAt.after(new Date());
    }
    
    /**
     * Verifica se o tier de doador expira em menos de X dias.
     * 
     * @param days Número de dias para verificar
     * @return true se expira em menos de X dias, false caso contrário
     */
    public boolean isDonorTierExpiringSoon(int days) {
        if (donorTier <= 0 || donorTierExpiresAt == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long expiryTime = donorTierExpiresAt.getTime();
        long daysInMillis = days * 24 * 60 * 60 * 1000L;
        
        return (expiryTime - currentTime) <= daysInMillis;
    }
    
    /**
     * Verifica se o jogador tem acesso ativo (assinatura válida).
     * 
     * @return true se o jogador tem acesso, false caso contrário
     */
    public boolean hasActiveAccess() {
        if (subscriptionExpiry == null) {
            return false;
        }
        return subscriptionExpiry.after(new Date());
    }
    
    /**
     * Verifica se a assinatura expira em menos de X dias.
     * 
     * @param days Número de dias para verificar
     * @return true se expira em menos de X dias, false caso contrário
     */
    public boolean isExpiringSoon(int days) {
        if (subscriptionExpiry == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long expiryTime = subscriptionExpiry.getTime();
        long daysInMillis = days * 24 * 60 * 60 * 1000L;
        
        return (expiryTime - currentTime) <= daysInMillis;
    }
    
    /**
     * Calcula quantos dias restam até a expiração.
     * 
     * @return Número de dias restantes, -1 se não há data de expiração
     */
    public int getDaysUntilExpiry() {
        if (subscriptionExpiry == null) {
            return -1;
        }
        
        long currentTime = System.currentTimeMillis();
        long expiryTime = subscriptionExpiry.getTime();
        long diffInMillis = expiryTime - currentTime;
        
        if (diffInMillis <= 0) {
            return 0;
        }
        
        return (int) (diffInMillis / (24 * 60 * 60 * 1000L));
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
                ", hasActiveAccess=" + hasActiveAccess() +
                '}';
    }
}
