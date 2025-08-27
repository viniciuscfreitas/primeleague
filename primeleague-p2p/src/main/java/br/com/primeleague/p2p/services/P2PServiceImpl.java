package br.com.primeleague.p2p.services;

import br.com.primeleague.api.P2PService;
import br.com.primeleague.p2p.managers.LimboManager;
import org.bukkit.entity.Player;

/**
 * Implementação do P2PService para o módulo P2P.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class P2PServiceImpl implements P2PService {
    
    private final LimboManager limboManager;
    
    public P2PServiceImpl(LimboManager limboManager) {
        this.limboManager = limboManager;
    }
    
    @Override
    public boolean isInLimbo(Player player) {
        return limboManager.isPlayerInLimbo(player);
    }
}
