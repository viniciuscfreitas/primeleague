package br.com.primeleague.essentials.models;

import java.sql.Timestamp;
import java.util.List;

/**
 * Modelo de dados para dossiê completo de um jogador.
 * Usado pelo comando /whois para exibir informações públicas sobre jogadores.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PlayerDossier {
    
    // Informações básicas
    private String playerName;
    private String uuid;
    private boolean isOnline;
    private Timestamp lastSeen;
    private String formattedLastSeen;
    
    // Dados de identidade
    private String displayName;
    private String rank;
    private String group;
    
    // Dados de estatísticas
    private int elo;
    private double money;
    private int level;
    private int kills;
    private int deaths;
    private double kdr;
    
    // Dados de clã
    private String clanName;
    private String clanRole;
    private String clanTag;
    private boolean hasClan;
    
    // Dados de essentials
    private int homeCount;
    private List<String> homes;
    private int maxHomes;
    
    /**
     * Construtor básico.
     */
    public PlayerDossier(String playerName) {
        this.playerName = playerName;
        this.homes = new java.util.ArrayList<String>();
        this.hasClan = false;
        this.isOnline = false;
    }
    
    /**
     * Verifica se o jogador tem clã.
     */
    public boolean hasClan() {
        return hasClan && clanName != null && !clanName.isEmpty();
    }
    
    /**
     * Obtém o status formatado do jogador.
     */
    public String getFormattedStatus() {
        return isOnline ? "§aOnline" : "§cOffline";
    }
    
    /**
     * Obtém o KDR formatado.
     */
    public String getFormattedKDR() {
        if (deaths == 0) {
            return kills == 0 ? "0.00" : String.format("%.2f", (double) kills);
        }
        return String.format("%.2f", kdr);
    }
    
    /**
     * Obtém a lista de homes formatada.
     */
    public String getFormattedHomes() {
        if (homes == null || homes.isEmpty()) {
            return "Nenhuma";
        }
        return String.join(", ", homes);
    }
    
    /**
     * Obtém informações de clã formatadas.
     */
    public String getFormattedClanInfo() {
        if (!hasClan()) {
            return "Nenhum";
        }
        return clanName + " (" + clanTag + ")";
    }
    
    // Getters e Setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }
    
    public String getFormattedLastSeen() { return formattedLastSeen; }
    public void setFormattedLastSeen(String formattedLastSeen) { this.formattedLastSeen = formattedLastSeen; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
    
    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }
    
    public int getElo() { return elo; }
    public void setElo(int elo) { this.elo = elo; }
    
    public double getMoney() { return money; }
    public void setMoney(double money) { this.money = money; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    
    public double getKdr() { return kdr; }
    public void setKdr(double kdr) { this.kdr = kdr; }
    
    public String getClanName() { return clanName; }
    public void setClanName(String clanName) { this.clanName = clanName; }
    
    public String getClanRole() { return clanRole; }
    public void setClanRole(String clanRole) { this.clanRole = clanRole; }
    
    public String getClanTag() { return clanTag; }
    public void setClanTag(String clanTag) { this.clanTag = clanTag; }
    
    public boolean isHasClan() { return hasClan; }
    public void setHasClan(boolean hasClan) { this.hasClan = hasClan; }
    
    public int getHomeCount() { return homeCount; }
    public void setHomeCount(int homeCount) { this.homeCount = homeCount; }
    
    public List<String> getHomes() { return homes; }
    public void setHomes(List<String> homes) { this.homes = homes; }
    
    public int getMaxHomes() { return maxHomes; }
    public void setMaxHomes(int maxHomes) { this.maxHomes = maxHomes; }
    
    @Override
    public String toString() {
        return "PlayerDossier{" +
                "playerName='" + playerName + '\'' +
                ", uuid='" + uuid + '\'' +
                ", isOnline=" + isOnline +
                ", displayName='" + displayName + '\'' +
                ", rank='" + rank + '\'' +
                ", elo=" + elo +
                ", money=" + money +
                ", level=" + level +
                ", clanName='" + clanName + '\'' +
                ", homeCount=" + homeCount +
                '}';
    }
}
