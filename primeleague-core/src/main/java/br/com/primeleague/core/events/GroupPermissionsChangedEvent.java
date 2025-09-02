package br.com.primeleague.core.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando as permissões de um grupo são modificadas.
 * Permite que o sistema atualize o cache de permissões em tempo real.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class GroupPermissionsChangedEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final int groupId;
    private final String groupName;
    private final String actionType;
    private final String permissionNode;
    
    /**
     * Construtor para mudanças gerais no grupo.
     * 
     * @param groupId ID do grupo modificado
     * @param groupName Nome do grupo modificado
     * @param actionType Tipo da ação realizada
     */
    public GroupPermissionsChangedEvent(int groupId, String groupName, String actionType) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.actionType = actionType;
        this.permissionNode = null;
    }
    
    /**
     * Construtor para mudanças específicas de permissão.
     * 
     * @param groupId ID do grupo modificado
     * @param groupName Nome do grupo modificado
     * @param actionType Tipo da ação realizada
     * @param permissionNode Nó da permissão afetada
     */
    public GroupPermissionsChangedEvent(int groupId, String groupName, String actionType, String permissionNode) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.actionType = actionType;
        this.permissionNode = permissionNode;
    }
    
    /**
     * Obtém o ID do grupo que foi modificado.
     * 
     * @return ID do grupo
     */
    public int getGroupId() {
        return groupId;
    }
    
    /**
     * Obtém o nome do grupo que foi modificado.
     * 
     * @return Nome do grupo
     */
    public String getGroupName() {
        return groupName;
    }
    
    /**
     * Obtém o tipo da ação realizada.
     * 
     * @return Tipo da ação
     */
    public String getActionType() {
        return actionType;
    }
    
    /**
     * Obtém o nó da permissão afetada (pode ser null para mudanças gerais no grupo).
     * 
     * @return Nó da permissão ou null
     */
    public String getPermissionNode() {
        return permissionNode;
    }
    
    /**
     * Verifica se este evento afeta uma permissão específica.
     * 
     * @return true se afeta uma permissão específica, false caso contrário
     */
    public boolean isPermissionSpecific() {
        return permissionNode != null;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public String toString() {
        return "GroupPermissionsChangedEvent{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", actionType='" + actionType + '\'' +
                ", permissionNode='" + permissionNode + '\'' +
                '}';
    }
}
