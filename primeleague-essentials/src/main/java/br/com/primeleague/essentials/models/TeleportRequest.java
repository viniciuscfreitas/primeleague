package br.com.primeleague.essentials.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Modelo de dados para solicitação de teletransporte.
 * Representa uma solicitação de teletransporte entre jogadores.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class TeleportRequest {
    
    private UUID requesterUuid;
    private String requesterName;
    private UUID targetUuid;
    private String targetName;
    private Timestamp requestTime;
    private Timestamp expiresAt;
    private RequestType type;
    private RequestStatus status;
    
    public enum RequestType {
        TPA,    // Requester vai para target
        TPAHERE // Target vai para requester
    }
    
    public enum RequestStatus {
        PENDING,    // Aguardando resposta
        ACCEPTED,   // Aceito
        DENIED,     // Negado
        EXPIRED,    // Expirado
        CANCELLED   // Cancelado
    }
    
    /**
     * Construtor para nova solicitação.
     */
    public TeleportRequest(UUID requesterUuid, String requesterName, 
                          UUID targetUuid, String targetName, RequestType type) {
        this.requesterUuid = requesterUuid;
        this.requesterName = requesterName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.type = type;
        this.status = RequestStatus.PENDING;
        this.requestTime = new Timestamp(System.currentTimeMillis());
        this.expiresAt = new Timestamp(System.currentTimeMillis() + 60000); // 60 segundos
    }
    
    /**
     * Verifica se a solicitação expirou.
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt.getTime();
    }
    
    /**
     * Verifica se a solicitação está pendente.
     */
    public boolean isPending() {
        return status == RequestStatus.PENDING && !isExpired();
    }
    
    // Getters e Setters
    public UUID getRequesterUuid() { return requesterUuid; }
    public void setRequesterUuid(UUID requesterUuid) { this.requesterUuid = requesterUuid; }
    
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    
    public UUID getTargetUuid() { return targetUuid; }
    public void setTargetUuid(UUID targetUuid) { this.targetUuid = targetUuid; }
    
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    
    public Timestamp getRequestTime() { return requestTime; }
    public void setRequestTime(Timestamp requestTime) { this.requestTime = requestTime; }
    
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    
    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }
    
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    
    @Override
    public String toString() {
        return "TeleportRequest{" +
                "requester=" + requesterName + " (" + requesterUuid + ")" +
                ", target=" + targetName + " (" + targetUuid + ")" +
                ", type=" + type +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
