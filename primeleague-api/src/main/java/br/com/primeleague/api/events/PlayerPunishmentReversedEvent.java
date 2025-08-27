package br.com.primeleague.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import br.com.primeleague.api.enums.PunishmentSeverity;
import br.com.primeleague.api.enums.ReversalType;

/**
 * Evento disparado quando uma punição é revertida.
 * Carrega a intenção da reversão (perdão ou correção).
 * 
 * @author Prime League Development Team
 * @version 1.0
 */
public class PlayerPunishmentReversedEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final String playerUuid;
    private final String playerName;
    private final PunishmentSeverity originalPunishmentSeverity;
    private final ReversalType reversalType;
    private final String adminName;
    private final String reason;

    /**
     * Construtor do evento.
     * 
     * @param playerUuid UUID do jogador que teve a punição revertida
     * @param playerName Nome do jogador que teve a punição revertida
     * @param originalPunishmentSeverity Severidade original da punição
     * @param reversalType Tipo de reversão (perdão ou correção)
     * @param adminName Nome do administrador que reverteu a punição
     * @param reason Motivo da reversão
     */
    public PlayerPunishmentReversedEvent(String playerUuid, String playerName, 
                                       PunishmentSeverity originalPunishmentSeverity,
                                       ReversalType reversalType, String adminName, String reason) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.originalPunishmentSeverity = originalPunishmentSeverity;
        this.reversalType = reversalType;
        this.adminName = adminName;
        this.reason = reason;
    }

    /**
     * Obtém o UUID do jogador.
     * 
     * @return UUID do jogador
     */
    public String getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Obtém o nome do jogador.
     * 
     * @return Nome do jogador
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Obtém a severidade original da punição.
     * 
     * @return Severidade original
     */
    public PunishmentSeverity getOriginalPunishmentSeverity() {
        return originalPunishmentSeverity;
    }

    /**
     * Obtém o tipo de reversão.
     * 
     * @return Tipo de reversão (perdão ou correção)
     */
    public ReversalType getReversalType() {
        return reversalType;
    }

    /**
     * Obtém o nome do administrador.
     * 
     * @return Nome do administrador
     */
    public String getAdminName() {
        return adminName;
    }

    /**
     * Obtém o motivo da reversão.
     * 
     * @return Motivo da reversão
     */
    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
