package br.com.primeleague.core.profile;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

public final class PlayerProfile {

    private final UUID uuid;
    private String lastKnownName;
    private int elo;
    private BigDecimal money;
    private Integer clanId; // null quando sem clã
    private long totalPlaytime;
    private Timestamp subscriptionExpiry; // null quando sem assinatura ativa

    public PlayerProfile(UUID uuid, String lastKnownName) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.elo = 1000;
        this.money = new BigDecimal("0.00");
        this.clanId = null;
        this.totalPlaytime = 0L;
    }

    public UUID getUuid() { return uuid; }
    public String getLastKnownName() { return lastKnownName; }
    public void setLastKnownName(String lastKnownName) { this.lastKnownName = lastKnownName; }
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }
    public BigDecimal getMoney() { return money; }
    public void setMoney(BigDecimal money) { this.money = money; }
    public Integer getClanId() { return clanId; }
    public void setClanId(Integer clanId) { this.clanId = clanId; }
    public long getTotalPlaytime() { return totalPlaytime; }
    public void setTotalPlaytime(long totalPlaytime) { this.totalPlaytime = totalPlaytime; }
    
    public Timestamp getSubscriptionExpiry() { return subscriptionExpiry; }
    public void setSubscriptionExpiry(Timestamp subscriptionExpiry) { this.subscriptionExpiry = subscriptionExpiry; }
    
    /**
     * Verifica se a assinatura do jogador está ativa
     * @return true se a assinatura não expirou, false caso contrário
     */
    public boolean hasActiveSubscription() {
        if (subscriptionExpiry == null) {
            return false;
        }
        return subscriptionExpiry.after(new Timestamp(System.currentTimeMillis()));
    }
}


