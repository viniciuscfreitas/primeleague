package br.com.primeleague.core.services;

import br.com.primeleague.api.IdentityService;
import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.managers.IdentityManager;
import org.bukkit.entity.Player;

/**
 * Implementação do IdentityService para o módulo Core.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class IdentityServiceImpl implements IdentityService {
    
    private final PrimeLeagueCore plugin;
    private final IdentityManager identityManager;
    
    public IdentityServiceImpl(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.identityManager = plugin.getIdentityManager();
    }
    
    @Override
    public int getPlayerId(Player player) {
        return identityManager.getPlayerId(player);
    }
}
