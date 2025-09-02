package br.com.primeleague.core.models;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Modelo para uma permissão associada a um grupo.
 * Representa uma permissão específica que pode ser concedida ou negada a um grupo.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class GroupPermission {
    
    private final int id;
    private final int groupId;
    private final String permissionNode;
    private final boolean isGranted;
    private final Timestamp createdAt;
    private final Integer createdByPlayerId;
    
    /**
     * Construtor para uma permissão de grupo.
     * 
     * @param id ID único da permissão
     * @param groupId ID do grupo
     * @param permissionNode Nó da permissão
     * @param isGranted Se a permissão é concedida (true) ou negada (false)
     * @param createdAt Data de criação
     * @param createdByPlayerId ID do jogador que criou a permissão
     */
    public GroupPermission(int id, int groupId, String permissionNode, boolean isGranted,
                          Timestamp createdAt, Integer createdByPlayerId) {
        this.id = id;
        this.groupId = groupId;
        this.permissionNode = permissionNode;
        this.isGranted = isGranted;
        this.createdAt = createdAt;
        this.createdByPlayerId = createdByPlayerId;
    }
    
    /**
     * Construtor para criação de novas permissões (sem ID).
     * 
     * @param groupId ID do grupo
     * @param permissionNode Nó da permissão
     * @param isGranted Se a permissão é concedida
     * @param createdByPlayerId ID do jogador que criou
     */
    public GroupPermission(int groupId, String permissionNode, boolean isGranted, Integer createdByPlayerId) {
        this(0, groupId, permissionNode, isGranted, null, createdByPlayerId);
    }
    
    // Getters
    
    public int getId() {
        return id;
    }
    
    public int getGroupId() {
        return groupId;
    }
    
    public String getPermissionNode() {
        return permissionNode;
    }
    
    public boolean isGranted() {
        return isGranted;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public Integer getCreatedByPlayerId() {
        return createdByPlayerId;
    }
    
    /**
     * Verifica se esta permissão é uma negação.
     * 
     * @return true se é uma negação, false se é uma concessão
     */
    public boolean isDenied() {
        return !isGranted;
    }
    
    /**
     * Cria uma cópia desta permissão com novos valores.
     * 
     * @param isGranted Novo status de concessão
     * @return Nova instância da permissão
     */
    public GroupPermission withGrantStatus(boolean isGranted) {
        return new GroupPermission(
            this.id,
            this.groupId,
            this.permissionNode,
            isGranted,
            this.createdAt,
            this.createdByPlayerId
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupPermission that = (GroupPermission) o;
        return groupId == that.groupId && Objects.equals(permissionNode, that.permissionNode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(groupId, permissionNode);
    }
    
    @Override
    public String toString() {
        return "GroupPermission{" +
                "id=" + id +
                ", groupId=" + groupId +
                ", permissionNode='" + permissionNode + '\'' +
                ", isGranted=" + isGranted +
                ", createdAt=" + createdAt +
                '}';
    }
}
