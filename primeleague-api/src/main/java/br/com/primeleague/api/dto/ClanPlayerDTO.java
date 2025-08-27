package br.com.primeleague.api.dto;

import java.util.Date;

/**
 * Data Transfer Object para Jogador de Clã.
 * Classe simples para transferência de dados entre módulos.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanPlayerDTO {
    
    private int playerId; // REFATORADO: Usar player_id em vez de UUID
    private String playerName;
    private int clanId;
    private int role;
    private Date joinDate;
    private int kills;
    private int deaths;
    
    // Construtores
    public ClanPlayerDTO() {}
    
    public ClanPlayerDTO(int playerId, String playerName, int clanId, int role, Date joinDate, int kills, int deaths) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.clanId = clanId;
        this.role = role;
        this.joinDate = joinDate;
        this.kills = kills;
        this.deaths = deaths;
    }
    
    // Getters e Setters
    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    /**
     * @deprecated Use getPlayerId() instead
     */
    @Deprecated
    public String getPlayerUuid() {
        return String.valueOf(playerId);
    }
    
    /**
     * @deprecated Use setPlayerId(int playerId) instead
     */
    @Deprecated
    public void setPlayerUuid(String playerUuid) {
        try {
            this.playerId = Integer.parseInt(playerUuid);
        } catch (NumberFormatException e) {
            this.playerId = -1;
        }
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public int getClanId() {
        return clanId;
    }
    
    public void setClanId(int clanId) {
        this.clanId = clanId;
    }
    
    public int getRole() {
        return role;
    }
    
    public void setRole(int role) {
        this.role = role;
    }
    
    public Date getJoinDate() {
        return joinDate;
    }
    
    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate;
    }
    
    public int getKills() {
        return kills;
    }
    
    public void setKills(int kills) {
        this.kills = kills;
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }
    
    @Override
    public String toString() {
        return "ClanPlayerDTO{" +
                "playerId=" + playerId +
                ", playerName='" + playerName + '\'' +
                ", clanId=" + clanId +
                ", role=" + role +
                ", joinDate=" + joinDate +
                ", kills=" + kills +
                ", deaths=" + deaths +
                '}';
    }
}
