package br.com.primeleague.core.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Evento disparado quando a identidade de um jogador é carregada com sucesso no Core.
 * Este evento garante que outros módulos só tentem acessar a identidade após o carregamento completo.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class PlayerIdentityLoadedEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final int playerId;
    private final String canonicalUuid;
    
    /**
     * Construtor do evento.
     * 
     * @param player O jogador cuja identidade foi carregada
     * @param playerId O ID do jogador no banco de dados
     * @param canonicalUuid O UUID canônico do jogador
     */
    public PlayerIdentityLoadedEvent(Player player, int playerId, String canonicalUuid) {
        this.player = player;
        this.playerId = playerId;
        this.canonicalUuid = canonicalUuid;
    }
    
    /**
     * Obtém o jogador.
     * 
     * @return O jogador
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Obtém o ID do jogador no banco de dados.
     * 
     * @return O player_id
     */
    public int getPlayerId() {
        return playerId;
    }
    
    /**
     * Obtém o UUID canônico do jogador.
     * 
     * @return O UUID canônico
     */
    public String getCanonicalUuid() {
        return canonicalUuid;
    }
    
    /**
     * Obtém o nome do jogador (conveniência).
     * 
     * @return O nome do jogador
     */
    public String getPlayerName() {
        return player.getName();
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
