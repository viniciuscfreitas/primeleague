package br.com.primeleague.api.dto;

import java.util.Date;

/**
 * Data Transfer Object para Clã.
 * Classe simples para transferência de dados entre módulos.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanDTO {
    
    private int id;
    private String tag;
    private String name;
    private int founderPlayerId; // REFATORADO: Usar player_id em vez de UUID
    private String founderName;
    private Date creationDate;
    private boolean friendlyFireEnabled;
    private int penaltyPoints;
    private int rankingPoints;
    
    // Construtores
    public ClanDTO() {}
    
    public ClanDTO(int id, String tag, String name, int founderPlayerId, String founderName, Date creationDate, boolean friendlyFireEnabled, int penaltyPoints, int rankingPoints) {
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.founderPlayerId = founderPlayerId;
        this.founderName = founderName;
        this.creationDate = creationDate;
        this.friendlyFireEnabled = friendlyFireEnabled;
        this.penaltyPoints = penaltyPoints;
        this.rankingPoints = rankingPoints;
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getTag() {
        return tag;
    }
    
    public void setTag(String tag) {
        this.tag = tag;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getFounderPlayerId() {
        return founderPlayerId;
    }
    
    public void setFounderPlayerId(int founderPlayerId) {
        this.founderPlayerId = founderPlayerId;
    }

    /**
     * @deprecated Use getFounderPlayerId() instead
     */
    @Deprecated
    public String getFounderUuid() {
        return String.valueOf(founderPlayerId);
    }
    
    /**
     * @deprecated Use setFounderPlayerId(int founderPlayerId) instead
     */
    @Deprecated
    public void setFounderUuid(String founderUuid) {
        try {
            this.founderPlayerId = Integer.parseInt(founderUuid);
        } catch (NumberFormatException e) {
            this.founderPlayerId = -1;
        }
    }
    
    public String getFounderName() {
        return founderName;
    }
    
    public void setFounderName(String founderName) {
        this.founderName = founderName;
    }
    
    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
    }
    
    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) {
        this.friendlyFireEnabled = friendlyFireEnabled;
    }
    
    public int getPenaltyPoints() {
        return penaltyPoints;
    }
    
    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }
    
    public int getRankingPoints() {
        return rankingPoints;
    }
    
    public void setRankingPoints(int rankingPoints) {
        this.rankingPoints = rankingPoints;
    }
    
    @Override
    public String toString() {
        return "ClanDTO{" +
                "id=" + id +
                ", tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", founderPlayerId=" + founderPlayerId +
                ", founderName='" + founderName + '\'' +
                ", creationDate=" + creationDate +
                ", friendlyFireEnabled=" + friendlyFireEnabled +
                ", penaltyPoints=" + penaltyPoints +
                ", rankingPoints=" + rankingPoints +
                '}';
    }
}
