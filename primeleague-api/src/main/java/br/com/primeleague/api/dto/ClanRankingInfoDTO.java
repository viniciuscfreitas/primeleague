package br.com.primeleague.api.dto;

import java.util.Date;
import java.util.Map;

/**
 * Data Transfer Object para informações de ranking de clãs.
 * Container para dados consolidados do ranking com informações completas.
 * 
 * @version 1.4.0
 * @author PrimeLeague Team
 */
public class ClanRankingInfoDTO {
    
    private int rank;
    private String tag;
    private String name;
    private int rankingPoints;
    private String founderName;
    private int memberCount;
    private int totalKills;
    private int totalDeaths;
    private double clanKdr;
    private int activeSanctionTier;
    private Date sanctionExpiresAt;
    private int totalWins;
    private Date lastWinDate;
    private Map<String, Integer> wins; // Map<EventName, WinCount>
    
    // Construtores
    public ClanRankingInfoDTO() {}
    
    public ClanRankingInfoDTO(int rank, String tag, String name, int rankingPoints, 
                             String founderName, int memberCount, int totalKills, 
                             int totalDeaths, double clanKdr, int activeSanctionTier, 
                             Date sanctionExpiresAt, int totalWins, Date lastWinDate) {
        this.rank = rank;
        this.tag = tag;
        this.name = name;
        this.rankingPoints = rankingPoints;
        this.founderName = founderName;
        this.memberCount = memberCount;
        this.totalKills = totalKills;
        this.totalDeaths = totalDeaths;
        this.clanKdr = clanKdr;
        this.activeSanctionTier = activeSanctionTier;
        this.sanctionExpiresAt = sanctionExpiresAt;
        this.totalWins = totalWins;
        this.lastWinDate = lastWinDate;
    }
    
    // Getters e Setters
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
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
    
    public int getRankingPoints() {
        return rankingPoints;
    }
    
    public void setRankingPoints(int rankingPoints) {
        this.rankingPoints = rankingPoints;
    }
    
    public String getFounderName() {
        return founderName;
    }
    
    public void setFounderName(String founderName) {
        this.founderName = founderName;
    }
    
    public int getMemberCount() {
        return memberCount;
    }
    
    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
    
    public int getTotalKills() {
        return totalKills;
    }
    
    public void setTotalKills(int totalKills) {
        this.totalKills = totalKills;
    }
    
    public int getTotalDeaths() {
        return totalDeaths;
    }
    
    public void setTotalDeaths(int totalDeaths) {
        this.totalDeaths = totalDeaths;
    }
    
    public double getClanKdr() {
        return clanKdr;
    }
    
    public void setClanKdr(double clanKdr) {
        this.clanKdr = clanKdr;
    }
    
    public int getActiveSanctionTier() {
        return activeSanctionTier;
    }
    
    public void setActiveSanctionTier(int activeSanctionTier) {
        this.activeSanctionTier = activeSanctionTier;
    }
    
    public Date getSanctionExpiresAt() {
        return sanctionExpiresAt;
    }
    
    public void setSanctionExpiresAt(Date sanctionExpiresAt) {
        this.sanctionExpiresAt = sanctionExpiresAt;
    }
    
    public int getTotalWins() {
        return totalWins;
    }
    
    public void setTotalWins(int totalWins) {
        this.totalWins = totalWins;
    }
    
    public Date getLastWinDate() {
        return lastWinDate;
    }
    
    public void setLastWinDate(Date lastWinDate) {
        this.lastWinDate = lastWinDate;
    }
    
    public Map<String, Integer> getWins() {
        return wins;
    }
    
    public void setWins(Map<String, Integer> wins) {
        this.wins = wins;
    }
    
    // Métodos de conveniência
    public String getStatusColor() {
        if (activeSanctionTier == 0) {
            return "§a"; // Verde - Honrado
        } else if (activeSanctionTier == 1) {
            return "§6"; // Dourado - Advertido
        } else if (activeSanctionTier == 2) {
            return "§c"; // Vermelho - Suspenso
        } else {
            return "§4"; // Vermelho escuro - Desqualificado
        }
    }
    
    public String getStatusText() {
        if (activeSanctionTier == 0) {
            return "Honrado";
        } else if (activeSanctionTier == 1) {
            return "Advertido (Tier " + activeSanctionTier + ")";
        } else if (activeSanctionTier == 2) {
            return "Suspenso (Tier " + activeSanctionTier + ")";
        } else {
            return "Desqualificado (Tier " + activeSanctionTier + ")";
        }
    }
    
    public String getWinsFormatted() {
        if (wins == null || wins.isEmpty()) {
            return "Nenhuma vitória";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : wins.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("(").append(entry.getValue()).append(")");
            first = false;
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "ClanRankingInfoDTO{" +
                "rank=" + rank +
                ", tag='" + tag + '\'' +
                ", name='" + name + '\'' +
                ", rankingPoints=" + rankingPoints +
                ", founderName='" + founderName + '\'' +
                ", memberCount=" + memberCount +
                ", totalKills=" + totalKills +
                ", totalDeaths=" + totalDeaths +
                ", clanKdr=" + clanKdr +
                ", activeSanctionTier=" + activeSanctionTier +
                ", totalWins=" + totalWins +
                '}';
    }
}
