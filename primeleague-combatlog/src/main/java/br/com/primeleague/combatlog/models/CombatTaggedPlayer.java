package br.com.primeleague.combatlog.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Representa um jogador atualmente em combate.
 * Modelo de dados para o sistema de prevenção de combat log.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatTaggedPlayer {
    
    private final UUID playerUuid;
    private final String playerName;
    private final Timestamp combatStartTime;
    private final String combatReason;
    private final String zoneType;
    private int remainingSeconds;
    
    /**
     * Construtor para criar um novo jogador tagueado em combate.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param combatReason Razão do combate (apenas DIRECT_DAMAGE)
     * @param zoneType Tipo de zona onde ocorreu o combate
     * @param durationSeconds Duração do tag em segundos
     */
    public CombatTaggedPlayer(UUID playerUuid, String playerName, String combatReason, String zoneType, int durationSeconds) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.combatStartTime = new Timestamp(System.currentTimeMillis());
        this.combatReason = combatReason;
        this.zoneType = zoneType;
        this.remainingSeconds = durationSeconds;
    }
    
    /**
     * Construtor para carregar jogador tagueado do banco de dados.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param combatStartTime Timestamp do início do combate
     * @param combatReason Razão do combate
     * @param zoneType Tipo de zona
     * @param remainingSeconds Tempo restante em segundos
     */
    public CombatTaggedPlayer(UUID playerUuid, String playerName, Timestamp combatStartTime, 
                             String combatReason, String zoneType, int remainingSeconds) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.combatStartTime = combatStartTime;
        this.combatReason = combatReason;
        this.zoneType = zoneType;
        this.remainingSeconds = remainingSeconds;
    }
    
    // ============================================================================
    // GETTERS
    // ============================================================================
    
    /**
     * Obtém o UUID do jogador.
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Obtém o nome do jogador.
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Obtém o timestamp do início do combate.
     */
    public Timestamp getCombatStartTime() {
        return combatStartTime;
    }
    
    /**
     * Obtém a razão do combate.
     */
    public String getCombatReason() {
        return combatReason;
    }
    
    /**
     * Obtém o tipo de zona onde ocorreu o combate.
     */
    public String getZoneType() {
        return zoneType;
    }
    
    /**
     * Obtém o tempo restante do tag em segundos.
     */
    public int getRemainingSeconds() {
        return remainingSeconds;
    }
    
    // ============================================================================
    // MÉTODOS DE NEGÓCIO
    // ============================================================================
    
    /**
     * Verifica se o tag de combate expirou.
     * 
     * @return true se expirou, false caso contrário
     */
    public boolean isExpired() {
        return remainingSeconds <= 0;
    }
    
    /**
     * Decrementa o tempo restante do tag.
     * Não permite valores negativos.
     */
    public void decrementTime() {
        if (remainingSeconds > 0) {
            remainingSeconds--;
        }
    }
    
    /**
     * Calcula a duração total do combate em segundos.
     * 
     * @return Duração em segundos
     */
    public int getCombatDuration() {
        long currentTime = System.currentTimeMillis();
        long startTime = combatStartTime.getTime();
        return (int) ((currentTime - startTime) / 1000);
    }
    
    /**
     * Obtém o tempo restante formatado para exibição.
     * 
     * @return String formatada (ex: "25s", "1m 30s")
     */
    public String getFormattedRemainingTime() {
        if (remainingSeconds <= 0) {
            return "EXPIRADO";
        }
        
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Verifica se o jogador está em uma zona de guerra.
     * 
     * @return true se estiver em zona de guerra
     */
    public boolean isInWarzone() {
        return "WARZONE".equalsIgnoreCase(zoneType);
    }
    
    /**
     * Verifica se o jogador está em uma zona PvP.
     * 
     * @return true se estiver em zona PvP
     */
    public boolean isInPvpZone() {
        return "PVP".equalsIgnoreCase(zoneType);
    }
    
    /**
     * Verifica se o jogador está em uma zona segura.
     * 
     * @return true se estiver em zona segura
     */
    public boolean isInSafeZone() {
        return "SAFE".equalsIgnoreCase(zoneType);
    }
    
    // ============================================================================
    // EQUALS, HASHCODE E TOSTRING
    // ============================================================================
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CombatTaggedPlayer that = (CombatTaggedPlayer) obj;
        return playerUuid.equals(that.playerUuid);
    }
    
    @Override
    public int hashCode() {
        return playerUuid.hashCode();
    }
    
    @Override
    public String toString() {
        return "CombatTaggedPlayer{" +
                "playerUuid=" + playerUuid +
                ", playerName='" + playerName + '\'' +
                ", combatStartTime=" + combatStartTime +
                ", combatReason='" + combatReason + '\'' +
                ", zoneType='" + zoneType + '\'' +
                ", remainingSeconds=" + remainingSeconds +
                '}';
    }
}
