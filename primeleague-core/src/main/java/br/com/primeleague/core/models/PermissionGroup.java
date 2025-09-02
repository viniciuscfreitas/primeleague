package br.com.primeleague.core.models;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Modelo para um grupo de permissões.
 * Representa um grupo que pode ter múltiplas permissões associadas.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PermissionGroup {
    
    private final int groupId;
    private final String groupName;
    private final String displayName;
    private final String description;
    private final int priority;
    private final boolean isDefault;
    private final boolean isActive;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    
    /**
     * Construtor para um grupo de permissões.
     * 
     * @param groupId ID único do grupo
     * @param groupName Nome único do grupo
     * @param displayName Nome de exibição do grupo
     * @param description Descrição do grupo
     * @param priority Prioridade do grupo (maior = mais importante)
     * @param isDefault Se é o grupo padrão para novos jogadores
     * @param isActive Se o grupo está ativo
     * @param createdAt Data de criação
     * @param updatedAt Data de última atualização
     */
    public PermissionGroup(int groupId, String groupName, String displayName, String description,
                          int priority, boolean isDefault, boolean isActive,
                          Timestamp createdAt, Timestamp updatedAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Construtor para criação de novos grupos (sem ID).
     * 
     * @param groupName Nome único do grupo
     * @param displayName Nome de exibição do grupo
     * @param description Descrição do grupo
     * @param priority Prioridade do grupo
     * @param isDefault Se é o grupo padrão
     */
    public PermissionGroup(String groupName, String displayName, String description, int priority, boolean isDefault) {
        this(0, groupName, displayName, description, priority, isDefault, true, null, null);
    }
    
    // Getters
    
    public int getGroupId() {
        return groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Cria uma cópia deste grupo com novos valores.
     * 
     * @param displayName Novo nome de exibição
     * @param description Nova descrição
     * @param priority Nova prioridade
     * @param isDefault Novo status de padrão
     * @param isActive Novo status de ativo
     * @return Nova instância do grupo
     */
    public PermissionGroup withUpdates(String displayName, String description, int priority, boolean isDefault, boolean isActive) {
        return new PermissionGroup(
            this.groupId,
            this.groupName,
            displayName != null ? displayName : this.displayName,
            description != null ? description : this.description,
            priority,
            isDefault,
            isActive,
            this.createdAt,
            new Timestamp(System.currentTimeMillis())
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionGroup that = (PermissionGroup) o;
        return groupId == that.groupId && Objects.equals(groupName, that.groupName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(groupId, groupName);
    }
    
    @Override
    public String toString() {
        return "PermissionGroup{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", priority=" + priority +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}
