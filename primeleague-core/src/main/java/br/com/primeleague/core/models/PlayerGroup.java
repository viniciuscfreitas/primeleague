package br.com.primeleague.core.models;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Modelo para associação entre um jogador e um grupo.
 * Representa a relação entre um jogador e um grupo de permissões.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PlayerGroup {
    
    private final int id;
    private final int playerId;
    private final int groupId;
    private final boolean isPrimary;
    private final Timestamp expiresAt;
    private final Timestamp addedAt;
    private final Integer addedByPlayerId;
    private final String reason;
    
    /**
     * Construtor para uma associação jogador-grupo.
     * 
     * @param id ID único da associação
     * @param playerId ID do jogador
     * @param groupId ID do grupo
     * @param isPrimary Se é o grupo primário do jogador
     * @param expiresAt Data de expiração da associação
     * @param addedAt Data de adição ao grupo
     * @param addedByPlayerId ID do jogador que adicionou
     * @param reason Motivo da adição ao grupo
     */
    public PlayerGroup(int id, int playerId, int groupId, boolean isPrimary,
                      Timestamp expiresAt, Timestamp addedAt, Integer addedByPlayerId, String reason) {
        this.id = id;
        this.playerId = playerId;
        this.groupId = groupId;
        this.isPrimary = isPrimary;
        this.expiresAt = expiresAt;
        this.addedAt = addedAt;
        this.addedByPlayerId = addedByPlayerId;
        this.reason = reason;
    }
    
    /**
     * Construtor para criação de novas associações (sem ID).
     * 
     * @param playerId ID do jogador
     * @param groupId ID do grupo
     * @param isPrimary Se é o grupo primário
     * @param expiresAt Data de expiração
     * @param addedByPlayerId ID do jogador que adicionou
     * @param reason Motivo da adição
     */
    public PlayerGroup(int playerId, int groupId, boolean isPrimary, Timestamp expiresAt,
                      Integer addedByPlayerId, String reason) {
        this(0, playerId, groupId, isPrimary, expiresAt, null, addedByPlayerId, reason);
    }
    
    // Getters
    
    public int getId() {
        return id;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public int getGroupId() {
        return groupId;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public Timestamp getExpiresAt() {
        return expiresAt;
    }
    
    public Timestamp getAddedAt() {
        return addedAt;
    }
    
    public Integer getAddedByPlayerId() {
        return addedByPlayerId;
    }
    
    public String getReason() {
        return reason;
    }
    
    /**
     * Verifica se esta associação expirou.
     * 
     * @return true se expirou, false caso contrário
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // Sem expiração
        }
        return System.currentTimeMillis() > expiresAt.getTime();
    }
    
    /**
     * Verifica se esta associação é temporária.
     * 
     * @return true se é temporária, false se é permanente
     */
    public boolean isTemporary() {
        return expiresAt != null;
    }
    
    /**
     * Cria uma cópia desta associação com novos valores.
     * 
     * @param isPrimary Novo status de grupo primário
     * @param expiresAt Nova data de expiração
     * @param reason Novo motivo
     * @return Nova instância da associação
     */
    public PlayerGroup withUpdates(boolean isPrimary, Timestamp expiresAt, String reason) {
        return new PlayerGroup(
            this.id,
            this.playerId,
            this.groupId,
            isPrimary,
            expiresAt,
            this.addedAt,
            this.addedByPlayerId,
            reason != null ? reason : this.reason
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerGroup that = (PlayerGroup) o;
        return playerId == that.playerId && groupId == that.groupId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(playerId, groupId);
    }
    
    @Override
    public String toString() {
        return "PlayerGroup{" +
                "id=" + id +
                ", playerId=" + playerId +
                ", groupId=" + groupId +
                ", isPrimary=" + isPrimary +
                ", expiresAt=" + expiresAt +
                ", isExpired=" + isExpired() +
                '}';
    }
}
