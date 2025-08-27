package br.com.primeleague.api.dto;

import br.com.primeleague.api.enums.LogActionType;

public class ClanLogDTO {
    private int id;
    private int clanId;
    private int actorPlayerId; // REFATORADO: Usar player_id em vez de UUID
    private String actorName;
    private LogActionType actionType;
    private int targetPlayerId; // REFATORADO: Usar player_id em vez de UUID
    private String targetName;
    private String details;
    private long timestamp;
    
    public ClanLogDTO() {}
    
    public ClanLogDTO(int clanId, int actorPlayerId, String actorName, LogActionType actionType, 
                     int targetPlayerId, String targetName, String details) {
        this.clanId = clanId;
        this.actorPlayerId = actorPlayerId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.targetPlayerId = targetPlayerId;
        this.targetName = targetName;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public int getId() { return id; }
    public int getClanId() { return clanId; }
    public int getActorPlayerId() { return actorPlayerId; }
    public String getActorName() { return actorName; }
    public LogActionType getActionType() { return actionType; }
    public int getTargetPlayerId() { return targetPlayerId; }
    public String getTargetName() { return targetName; }
    public String getDetails() { return details; }
    public long getTimestamp() { return timestamp; }
    
    // Setters
    public void setId(int id) { this.id = id; }
    public void setClanId(int clanId) { this.clanId = clanId; }
    public void setActorPlayerId(int actorPlayerId) { this.actorPlayerId = actorPlayerId; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public void setActionType(LogActionType actionType) { this.actionType = actionType; }
    public void setTargetPlayerId(int targetPlayerId) { this.targetPlayerId = targetPlayerId; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public void setDetails(String details) { this.details = details; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * @deprecated Use getActorPlayerId() instead
     */
    @Deprecated
    public String getActorUuid() { return String.valueOf(actorPlayerId); }
    
    /**
     * @deprecated Use setActorPlayerId(int actorPlayerId) instead
     */
    @Deprecated
    public void setActorUuid(String actorUuid) {
        try {
            this.actorPlayerId = Integer.parseInt(actorUuid);
        } catch (NumberFormatException e) {
            this.actorPlayerId = -1;
        }
    }

    /**
     * @deprecated Use getTargetPlayerId() instead
     */
    @Deprecated
    public String getTargetUuid() { return String.valueOf(targetPlayerId); }
    
    /**
     * @deprecated Use setTargetPlayerId(int targetPlayerId) instead
     */
    @Deprecated
    public void setTargetUuid(String targetUuid) {
        try {
            this.targetPlayerId = Integer.parseInt(targetUuid);
        } catch (NumberFormatException e) {
            this.targetPlayerId = -1;
        }
    }

    @Override
    public String toString() {
        return "ClanLogDTO{" +
                "id=" + id +
                ", clanId=" + clanId +
                ", actorPlayerId=" + actorPlayerId +
                ", actorName='" + actorName + '\'' +
                ", actionType=" + actionType +
                ", targetPlayerId=" + targetPlayerId +
                ", targetName='" + targetName + '\'' +
                ", details='" + details + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
