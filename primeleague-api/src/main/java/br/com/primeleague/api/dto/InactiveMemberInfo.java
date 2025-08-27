package br.com.primeleague.api.dto;

import java.util.Date;

/**
 * DTO para informações de membros inativos em clãs.
 * Usado pelo sistema de limpeza automática.
 */
public class InactiveMemberInfo {
    private final String playerUuid;
    private final String playerName;
    private final int clanId;
    private final String clanName;
    private final String clanTag;
    private final int role;
    private final Date lastSeen;
    private final int daysInactive;

    public InactiveMemberInfo(String playerUuid, String playerName, int clanId, 
                             String clanName, String clanTag, int role, 
                             Date lastSeen, int daysInactive) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.clanId = clanId;
        this.clanName = clanName;
        this.clanTag = clanTag;
        this.role = role;
        this.lastSeen = lastSeen;
        this.daysInactive = daysInactive;
    }

    // Getters
    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public int getClanId() { return clanId; }
    public String getClanName() { return clanName; }
    public String getClanTag() { return clanTag; }
    public int getRole() { return role; }
    public Date getLastSeen() { return lastSeen; }
    public int getDaysInactive() { return daysInactive; }

    @Override
    public String toString() {
        return String.format("InactiveMemberInfo{player=%s, clan=%s, daysInactive=%d}", 
                           playerName, clanTag, daysInactive);
    }
}
