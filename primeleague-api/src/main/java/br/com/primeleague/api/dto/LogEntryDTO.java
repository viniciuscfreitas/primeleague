package br.com.primeleague.api.dto;

import java.util.UUID;

/**
 * Data Transfer Object para entrada de log de chat.
 * Classe simples para transferência de dados entre módulos.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class LogEntryDTO {
    
    private String channelType;
    private UUID senderUuid;
    private String senderName;
    private UUID receiverUuid;
    private String receiverName;
    private Integer clanId;
    private String messageContent;
    private long timestamp;
    
    // Construtores
    public LogEntryDTO() {}
    
    public LogEntryDTO(String channelType, UUID senderUuid, String senderName, UUID receiverUuid, String receiverName, Integer clanId, String messageContent, long timestamp) {
        this.channelType = channelType;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.clanId = clanId;
        this.messageContent = messageContent;
        this.timestamp = timestamp;
    }
    
    // Getters e Setters
    public String getChannelType() {
        return channelType;
    }
    
    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }
    
    public UUID getSenderUuid() {
        return senderUuid;
    }
    
    public void setSenderUuid(UUID senderUuid) {
        this.senderUuid = senderUuid;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public UUID getReceiverUuid() {
        return receiverUuid;
    }
    
    public void setReceiverUuid(UUID receiverUuid) {
        this.receiverUuid = receiverUuid;
    }
    
    public String getReceiverName() {
        return receiverName;
    }
    
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }
    
    public Integer getClanId() {
        return clanId;
    }
    
    public void setClanId(Integer clanId) {
        this.clanId = clanId;
    }
    
    public String getMessageContent() {
        return messageContent;
    }
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
