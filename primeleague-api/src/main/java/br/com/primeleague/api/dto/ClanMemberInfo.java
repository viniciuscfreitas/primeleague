package br.com.primeleague.api.dto;

import br.com.primeleague.api.enums.ClanRole;

/**
 * DTO para transportar informações detalhadas de um membro do clã.
 * Usado para exibição na interface de usuário.
 */
public class ClanMemberInfo {
    
    private String playerName;
    private String playerUuid;
    private ClanRole role;
    private int kills;
    private int deaths;
    private long joinDate;
    private long lastSeen;
    private boolean isOnline;
    
    public ClanMemberInfo() {}
    
    public ClanMemberInfo(String playerName, String playerUuid, ClanRole role, 
                         int kills, int deaths, long joinDate, long lastSeen, boolean isOnline) {
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.role = role;
        this.kills = kills;
        this.deaths = deaths;
        this.joinDate = joinDate;
        this.lastSeen = lastSeen;
        this.isOnline = isOnline;
    }
    
    // Getters e Setters
    public String getPlayerName() {
        return playerName;
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    public String getPlayerUuid() {
        return playerUuid;
    }
    
    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }
    
    public ClanRole getRole() {
        return role;
    }
    
    public void setRole(ClanRole role) {
        this.role = role;
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
    
    public long getJoinDate() {
        return joinDate;
    }
    
    public void setJoinDate(long joinDate) {
        this.joinDate = joinDate;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public boolean isOnline() {
        return isOnline;
    }
    
    public void setOnline(boolean online) {
        isOnline = online;
    }
    
    /**
     * Calcula o KDR (Kill/Death Ratio) do jogador.
     * @return O KDR formatado como string (ex: "2.5" ou "0.0")
     */
    public String getKDR() {
        if (deaths == 0) {
            return kills == 0 ? "0.0" : String.valueOf(kills);
        }
        double kdr = (double) kills / deaths;
        return String.format("%.1f", kdr);
    }
    
    /**
     * Retorna o nome do cargo formatado.
     * @return "Fundador", "Líder" ou "Membro"
     */
    public String getRoleName() {
        switch (role) {
            case FUNDADOR:
                return "Fundador";
            case LIDER:
                return "Líder";
            case MEMBRO:
                return "Membro";
            default:
                return "Desconhecido";
        }
    }
}
