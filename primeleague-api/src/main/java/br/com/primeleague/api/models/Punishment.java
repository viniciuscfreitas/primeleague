package br.com.primeleague.api.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Representa uma punição no sistema administrativo.
 * Armazena todas as informações sobre uma punição aplicada a um jogador.
 */
public class Punishment {

    public enum Type {
        WARN, KICK, MUTE, BAN
    }

    private int id;
    private Type type;
    private UUID targetUuid;
    private UUID authorUuid;
    private String reason;
    private Timestamp createdAt;
    private Timestamp expiresAt;
    private boolean active;
    private UUID pardonedByUuid;
    private Timestamp pardonedAt;
    private String pardonReason;

    // Campos para nomes dos jogadores (para exibição)
    private String targetName;
    private String authorName;
    private String pardonedByName;

    // REFATORADO: Getters e Setters para player_id
    private Integer targetPlayerId;
    private Integer authorPlayerId;
    private Integer pardonedByPlayerId;
    
    // Campos adicionais para compatibilidade com DAO
    private int punishmentId;
    private int playerId;
    private int staffId;
    private String punishmentType;
    private long durationSeconds;
    private Timestamp appliedAt;
    private Timestamp removedAt;

    public Punishment() {}

    public Punishment(Type type, UUID targetUuid, UUID authorUuid, String reason) {
        this.type = type;
        this.targetUuid = targetUuid;
        this.authorUuid = authorUuid;
        this.reason = reason;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.active = true;
    }

    public Punishment(Type type, UUID targetUuid, UUID authorUuid, String reason, Timestamp expiresAt) {
        this(type, targetUuid, authorUuid, reason);
        this.expiresAt = expiresAt;
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getAuthorUuid() {
        return authorUuid;
    }

    public void setAuthorUuid(UUID authorUuid) {
        this.authorUuid = authorUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public UUID getPardonedByUuid() {
        return pardonedByUuid;
    }

    public void setPardonedByUuid(UUID pardonedByUuid) {
        this.pardonedByUuid = pardonedByUuid;
    }

    public Timestamp getPardonedAt() {
        return pardonedAt;
    }

    public void setPardonedAt(Timestamp pardonedAt) {
        this.pardonedAt = pardonedAt;
    }

    public String getPardonReason() {
        return pardonReason;
    }

    public void setPardonReason(String pardonReason) {
        this.pardonReason = pardonReason;
    }

    // Getters e Setters para nomes
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getPardonedByName() {
        return pardonedByName;
    }

    public void setPardonedByName(String pardonedByName) {
        this.pardonedByName = pardonedByName;
    }

    // REFATORADO: Getters e Setters para player_id
    public Integer getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(Integer targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public Integer getAuthorPlayerId() {
        return authorPlayerId;
    }

    public void setAuthorPlayerId(Integer authorPlayerId) {
        this.authorPlayerId = authorPlayerId;
    }

    public Integer getPardonedByPlayerId() {
        return pardonedByPlayerId;
    }

    public void setPardonedByPlayerId(Integer pardonedByPlayerId) {
        this.pardonedByPlayerId = pardonedByPlayerId;
    }
    
    // Getters e Setters para campos de compatibilidade com DAO
    public int getPunishmentId() {
        return punishmentId;
    }
    
    public void setPunishmentId(int punishmentId) {
        this.punishmentId = punishmentId;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
    
    public int getStaffId() {
        return staffId;
    }
    
    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }
    
    public String getPunishmentType() {
        return punishmentType;
    }
    
    public void setPunishmentType(String punishmentType) {
        this.punishmentType = punishmentType;
    }
    
    public long getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
    
    public Timestamp getAppliedAt() {
        return appliedAt;
    }
    
    public void setAppliedAt(Timestamp appliedAt) {
        this.appliedAt = appliedAt;
    }
    
    public Timestamp getRemovedAt() {
        return removedAt;
    }
    
    public void setRemovedAt(Timestamp removedAt) {
        this.removedAt = removedAt;
    }

    /**
     * Verifica se a punição é permanente (não tem data de expiração).
     */
    public boolean isPermanent() {
        return expiresAt == null;
    }

    /**
     * Verifica se a punição expirou.
     */
    public boolean isExpired() {
        if (isPermanent() || !active) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt.getTime();
    }

    /**
     * Verifica se a punição está ativa e não expirou.
     */
    public boolean isCurrentlyActive() {
        return active && !isExpired();
    }

    /**
     * Aplica perdão à punição.
     */
    public void pardon(UUID pardonedByUuid, String pardonReason) {
        this.active = false;
        this.pardonedByUuid = pardonedByUuid;
        this.pardonedAt = new Timestamp(System.currentTimeMillis());
        this.pardonReason = pardonReason;
    }

    @Override
    public String toString() {
        return String.format("Punishment{id=%d, type=%s, target=%s, reason='%s', active=%s}",
            id, type, targetUuid, reason, active);
    }
}
