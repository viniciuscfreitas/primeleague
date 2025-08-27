package br.com.primeleague.api.events;

import br.com.primeleague.api.enums.PunishmentSeverity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando um jogador é punido pelo sistema administrativo.
 * Este evento é usado para integrar com outros módulos, como o sistema de sanções de clãs.
 * 
 * IMPORTANTE: Este evento deve residir no primeleague-api para garantir que
 * todos os módulos que precisam se comunicar usem a mesma definição de classe.
 */
public class PlayerPunishedEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    private final String playerUuid;
    private final String playerName;
    private final String authorUuid;
    private final String authorName;
    private final PunishmentSeverity severity;
    private final String reason;
    private final long duration; // -1 para permanente
    
    public PlayerPunishedEvent(String playerUuid, String playerName, String authorUuid, 
                              String authorName, PunishmentSeverity severity, String reason, long duration) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.severity = severity;
        this.reason = reason;
        this.duration = duration;
    }
    
    public String getPlayerUuid() {
        return playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getAuthorUuid() {
        return authorUuid;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public PunishmentSeverity getSeverity() {
        return severity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public boolean isPermanent() {
        return duration == -1;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
