package br.com.primeleague.territories.model;

import java.sql.Timestamp;

/**
 * Modelo de dados para uma guerra ativa.
 * Representa uma guerra declarada entre dois clãs.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ActiveWar {
    
    private int id;
    private int aggressorClanId;
    private int defenderClanId;
    private Timestamp startTime;
    private Timestamp endTimeExclusivity;
    private WarStatus status;
    
    public enum WarStatus {
        DECLARED,    // Guerra declarada, aguardando cerco
        SIEGE_ACTIVE, // Cerco em andamento
        COMPLETED,   // Guerra concluída
        EXPIRED      // Guerra expirada sem cerco
    }
    
    public ActiveWar() {}
    
    public ActiveWar(int aggressorClanId, int defenderClanId) {
        this.aggressorClanId = aggressorClanId;
        this.defenderClanId = defenderClanId;
        this.startTime = new Timestamp(System.currentTimeMillis());
        this.status = WarStatus.DECLARED;
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
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
    
    public Timestamp getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
    
    public Timestamp getEndTimeExclusivity() {
        return endTimeExclusivity;
    }
    
    public void setEndTimeExclusivity(Timestamp endTimeExclusivity) {
        this.endTimeExclusivity = endTimeExclusivity;
    }
    
    public WarStatus getStatus() {
        return status;
    }
    
    public void setStatus(WarStatus status) {
        this.status = status;
    }
    
    /**
     * Verifica se a guerra ainda está dentro da janela de exclusividade.
     * 
     * @return true se ainda está na janela de exclusividade
     */
    public boolean isWithinExclusivityWindow() {
        if (endTimeExclusivity == null) return false;
        return System.currentTimeMillis() < endTimeExclusivity.getTime();
    }
    
    /**
     * Verifica se a guerra expirou.
     * 
     * @return true se a guerra expirou
     */
    public boolean isExpired() {
        return !isWithinExclusivityWindow() && status == WarStatus.DECLARED;
    }
    
    @Override
    public String toString() {
        return "ActiveWar{" +
                "id=" + id +
                ", aggressorClanId=" + aggressorClanId +
                ", defenderClanId=" + defenderClanId +
                ", startTime=" + startTime +
                ", endTimeExclusivity=" + endTimeExclusivity +
                ", status=" + status +
                '}';
    }
}
