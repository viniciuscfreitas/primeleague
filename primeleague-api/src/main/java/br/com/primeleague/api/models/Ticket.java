package br.com.primeleague.api.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Representa um ticket de denúncia no sistema administrativo.
 * Gerencia o fluxo de denúncias feitas por jogadores.
 */
public class Ticket {

    public enum Status {
        OPEN, IN_PROGRESS, CLOSED_GUILTY, CLOSED_INNOCENT
    }

    private int id;
    private Status status;
    private UUID reporterUuid;
    private UUID targetUuid;
    private String reason;
    private String evidenceLink;
    private UUID claimedByUuid;
    private String resolutionNotes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Campos para nomes dos jogadores (para exibição)
    private String reporterName;
    private String targetName;
    private String claimedByName;
    
    // Campos adicionais para compatibilidade com DAO
    private int ticketId;
    private int playerId;
    private String title;
    private String description;
    private String priority;
    private Integer assignedStaffId;

    public Ticket() {}

    public Ticket(UUID reporterUuid, UUID targetUuid, String reason) {
        this.reporterUuid = reporterUuid;
        this.targetUuid = targetUuid;
        this.reason = reason;
        this.status = Status.OPEN;
        this.createdAt = new Timestamp(System.currentTimeMillis());
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public Ticket(UUID reporterUuid, UUID targetUuid, String reason, String evidenceLink) {
        this(reporterUuid, targetUuid, reason);
        this.evidenceLink = evidenceLink;
    }

    // Getters e Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public UUID getReporterUuid() {
        return reporterUuid;
    }

    public void setReporterUuid(UUID reporterUuid) {
        this.reporterUuid = reporterUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidenceLink() {
        return evidenceLink;
    }

    public void setEvidenceLink(String evidenceLink) {
        this.evidenceLink = evidenceLink;
    }

    public UUID getClaimedByUuid() {
        return claimedByUuid;
    }

    public void setClaimedByUuid(UUID claimedByUuid) {
        this.claimedByUuid = claimedByUuid;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Getters e Setters para nomes
    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getClaimedByName() {
        return claimedByName;
    }

    public void setClaimedByName(String claimedByName) {
        this.claimedByName = claimedByName;
    }
    
    // Getters e Setters para campos de compatibilidade com DAO
    public int getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    
    public Integer getAssignedStaffId() {
        return assignedStaffId;
    }
    
    public void setAssignedStaffId(Integer assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    /**
     * Verifica se o ticket está aberto para ser trabalhado.
     */
    public boolean isOpen() {
        return status == Status.OPEN || status == Status.IN_PROGRESS;
    }

    /**
     * Verifica se o ticket foi fechado.
     */
    public boolean isClosed() {
        return status == Status.CLOSED_GUILTY || status == Status.CLOSED_INNOCENT;
    }

    /**
     * Verifica se o ticket foi resolvido como culpado.
     */
    public boolean isGuilty() {
        return status == Status.CLOSED_GUILTY;
    }

    /**
     * Verifica se o ticket foi resolvido como inocente.
     */
    public boolean isInnocent() {
        return status == Status.CLOSED_INNOCENT;
    }

    /**
     * Reivindica o ticket para um membro da equipe.
     */
    public void claim(UUID staffUuid) {
        this.claimedByUuid = staffUuid;
        if (this.status == Status.OPEN) {
            this.status = Status.IN_PROGRESS;
        }
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Fecha o ticket com uma resolução.
     */
    public void close(Status finalStatus, String resolutionNotes) {
        if (finalStatus != Status.CLOSED_GUILTY && finalStatus != Status.CLOSED_INNOCENT) {
            throw new IllegalArgumentException("Status final deve ser CLOSED_GUILTY ou CLOSED_INNOCENT");
        }
        this.status = finalStatus;
        this.resolutionNotes = resolutionNotes;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Verifica se o ticket está sendo trabalhado por alguém.
     */
    public boolean isClaimed() {
        return claimedByUuid != null;
    }

    @Override
    public String toString() {
        return String.format("Ticket{id=%d, status=%s, reporter=%s, target=%s, claimed=%s}",
            id, status, reporterUuid, targetUuid, claimedByUuid);
    }
}
