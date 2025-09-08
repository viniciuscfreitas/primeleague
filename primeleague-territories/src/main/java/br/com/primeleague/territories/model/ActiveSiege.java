package br.com.primeleague.territories.model;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modelo de dados para um cerco ativo.
 * Representa um cerco em andamento em um território.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ActiveSiege {
    
    private int id;
    private int warId;
    private int territoryId;
    private int aggressorClanId;
    private int defenderClanId;
    private Location altarLocation;
    private Timestamp startTime;
    private Timestamp endTime;
    private int remainingTime; // em segundos
    private SiegeStatus status;
    
    // Controle de contestação
    private final Set<Player> attackersInZone = ConcurrentHashMap.newKeySet();
    private final Set<Player> defendersInZone = ConcurrentHashMap.newKeySet();
    
    public enum SiegeStatus {
        ACTIVE,      // Cerco em andamento
        ATTACKER_WIN, // Atacantes venceram
        DEFENDER_WIN, // Defensores venceram
        EXPIRED      // Cerco expirado
    }
    
    public ActiveSiege() {}
    
    public ActiveSiege(int warId, int territoryId, int aggressorClanId, int defenderClanId, Location altarLocation, int durationMinutes) {
        this.warId = warId;
        this.territoryId = territoryId;
        this.aggressorClanId = aggressorClanId;
        this.defenderClanId = defenderClanId;
        this.altarLocation = altarLocation;
        this.startTime = new Timestamp(System.currentTimeMillis());
        this.remainingTime = durationMinutes * 60; // converter para segundos
        this.status = SiegeStatus.ACTIVE;
        
        // Calcular tempo de fim
        long endTimeMillis = System.currentTimeMillis() + (durationMinutes * 60 * 1000L);
        this.endTime = new Timestamp(endTimeMillis);
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getWarId() {
        return warId;
    }
    
    public void setWarId(int warId) {
        this.warId = warId;
    }
    
    public int getTerritoryId() {
        return territoryId;
    }
    
    public void setTerritoryId(int territoryId) {
        this.territoryId = territoryId;
    }
    
    public int getAggressorClanId() {
        return aggressorClanId;
    }
    
    public void setAggressorClanId(int aggressorClanId) {
        this.aggressorClanId = aggressorClanId;
    }
    
    public int getDefenderClanId() {
        return defenderClanId;
    }
    
    public void setDefenderClanId(int defenderClanId) {
        this.defenderClanId = defenderClanId;
    }
    
    public Location getAltarLocation() {
        return altarLocation;
    }
    
    public void setAltarLocation(Location altarLocation) {
        this.altarLocation = altarLocation;
    }
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }
    
    public int getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(int remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    public SiegeStatus getStatus() {
        return status;
    }
    
    public void setStatus(SiegeStatus status) {
        this.status = status;
    }
    
    // Métodos de controle de contestação
    public Set<Player> getAttackersInZone() {
        return attackersInZone;
    }
    
    public Set<Player> getDefendersInZone() {
        return defendersInZone;
    }
    
    /**
     * Adiciona um jogador à zona de contestação.
     * 
     * @param player Jogador
     * @param isAttacker true se é atacante, false se é defensor
     */
    public void addPlayerToZone(Player player, boolean isAttacker) {
        if (isAttacker) {
            attackersInZone.add(player);
        } else {
            defendersInZone.add(player);
        }
    }
    
    /**
     * Remove um jogador da zona de contestação.
     * 
     * @param player Jogador
     */
    public void removePlayerFromZone(Player player) {
        attackersInZone.remove(player);
        defendersInZone.remove(player);
    }
    
    /**
     * Calcula o controle da zona baseado no número de jogadores.
     * 
     * @return 1 se atacantes dominam, 0 se empate, -1 se defensores dominam
     */
    public int getZoneControl() {
        int attackerCount = attackersInZone.size();
        int defenderCount = defendersInZone.size();
        
        if (attackerCount > defenderCount) return 1;
        if (attackerCount < defenderCount) return -1;
        return 0; // empate
    }
    
    /**
     * Verifica se o cerco expirou.
     * 
     * @return true se o cerco expirou
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime.getTime();
    }
    
    /**
     * Atualiza o tempo restante do cerco.
     */
    public void updateRemainingTime() {
        long currentTime = System.currentTimeMillis();
        long endTimeMillis = endTime.getTime();
        
        if (currentTime >= endTimeMillis) {
            remainingTime = 0;
        } else {
            remainingTime = (int) ((endTimeMillis - currentTime) / 1000);
        }
    }
    
    /**
     * Obtém o nome do mundo do altar.
     */
    public String getWorldName() {
        return altarLocation != null ? altarLocation.getWorld().getName() : null;
    }
    
    /**
     * Obtém a coordenada X do chunk do altar.
     */
    public int getChunkX() {
        return altarLocation != null ? altarLocation.getChunk().getX() : 0;
    }
    
    /**
     * Obtém a coordenada Z do chunk do altar.
     */
    public int getChunkZ() {
        return altarLocation != null ? altarLocation.getChunk().getZ() : 0;
    }
    
    @Override
    public String toString() {
        return "ActiveSiege{" +
                "warId=" + warId +
                ", territoryId=" + territoryId +
                ", aggressorClanId=" + aggressorClanId +
                ", defenderClanId=" + defenderClanId +
                ", altarLocation=" + altarLocation +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", remainingTime=" + remainingTime +
                ", status=" + status +
                '}';
    }
}
