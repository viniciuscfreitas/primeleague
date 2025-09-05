package br.com.primeleague.api.models;

import java.sql.Timestamp;

/**
 * Modelo de dados para cooldown de kits.
 * Representa o controle de uso de kits por jogador.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class KitCooldown {
    
    private int cooldownId;
    private int playerId;
    private String kitName;
    private Timestamp lastUsed;
    private int usesCount;
    private Timestamp expiresAt;
    
    /**
     * Construtor para novo cooldown.
     */
    public KitCooldown(int playerId, String kitName) {
        this.playerId = playerId;
        this.kitName = kitName;
        this.lastUsed = new Timestamp(System.currentTimeMillis());
        this.usesCount = 0;
        this.expiresAt = null;
    }
    
    /**
     * Construtor completo.
     */
    public KitCooldown(int cooldownId, int playerId, String kitName, 
                      Timestamp lastUsed, int usesCount, Timestamp expiresAt) {
        this.cooldownId = cooldownId;
        this.playerId = playerId;
        this.kitName = kitName;
        this.lastUsed = lastUsed;
        this.usesCount = usesCount;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Verifica se o cooldown expirou.
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt.getTime();
    }
    
    /**
     * Incrementa o contador de usos.
     */
    public void incrementUses() {
        this.usesCount++;
        this.lastUsed = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Verifica se o jogador pode usar o kit (não está em cooldown).
     */
    public boolean canUse(long cooldownSeconds) {
        if (isExpired()) {
            return true;
        }
        
        long timeSinceLastUse = System.currentTimeMillis() - lastUsed.getTime();
        long cooldownMs = cooldownSeconds * 1000L;
        
        return timeSinceLastUse >= cooldownMs;
    }
    
    /**
     * Obtém o tempo restante do cooldown em segundos.
     */
    public long getRemainingSeconds(long cooldownSeconds) {
        if (canUse(cooldownSeconds)) {
            return 0;
        }
        
        long timeSinceLastUse = System.currentTimeMillis() - lastUsed.getTime();
        long cooldownMs = cooldownSeconds * 1000L;
        
        return (cooldownMs - timeSinceLastUse) / 1000;
    }
    
    /**
     * Formata o tempo restante em formato legível.
     */
    public String getFormattedRemainingTime(long cooldownSeconds) {
        long remaining = getRemainingSeconds(cooldownSeconds);
        
        if (remaining <= 0) {
            return "Pronto";
        }
        
        long hours = remaining / 3600;
        long minutes = (remaining % 3600) / 60;
        long seconds = remaining % 60;
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    // Getters e Setters
    public int getCooldownId() { return cooldownId; }
    public void setCooldownId(int cooldownId) { this.cooldownId = cooldownId; }
    
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    
    public String getKitName() { return kitName; }
    public void setKitName(String kitName) { this.kitName = kitName; }
    
    public Timestamp getLastUsed() { return lastUsed; }
    public void setLastUsed(Timestamp lastUsed) { this.lastUsed = lastUsed; }
    
    public int getUsesCount() { return usesCount; }
    public void setUsesCount(int usesCount) { this.usesCount = usesCount; }
    
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    
    @Override
    public String toString() {
        return "KitCooldown{" +
                "cooldownId=" + cooldownId +
                ", playerId=" + playerId +
                ", kitName='" + kitName + '\'' +
                ", lastUsed=" + lastUsed +
                ", usesCount=" + usesCount +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
