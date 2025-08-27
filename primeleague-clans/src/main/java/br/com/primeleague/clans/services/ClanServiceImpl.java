package br.com.primeleague.clans.services;

import br.com.primeleague.api.ClanService;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.model.Clan;
import org.bukkit.entity.Player;

/**
 * Implementação do ClanService para o módulo Clans.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ClanServiceImpl implements ClanService {
    
    private final ClanManager clanManager;
    
    public ClanServiceImpl(ClanManager clanManager) {
        this.clanManager = clanManager;
    }
    
    @Override
    public String getClanName(Player player) {
        Clan clan = clanManager.getClanByPlayer(player);
        return clan != null ? clan.getName() : null;
    }
    
    @Override
    public boolean areInSameClan(Player player1, Player player2) {
        Clan clan1 = clanManager.getClanByPlayer(player1);
        Clan clan2 = clanManager.getClanByPlayer(player2);
        
        if (clan1 == null || clan2 == null) {
            return false;
        }
        
        return clan1.getId() == clan2.getId();
    }
    
    @Override
    public boolean areInAlliedClans(Player player1, Player player2) {
        Clan clan1 = clanManager.getClanByPlayer(player1);
        Clan clan2 = clanManager.getClanByPlayer(player2);
        
        if (clan1 == null || clan2 == null) {
            return false;
        }
        
        return clanManager.areAllies(clan1, clan2);
    }
    
    @Override
    public Integer getClanId(Player player) {
        Clan clan = clanManager.getClanByPlayer(player);
        return clan != null ? clan.getId() : null;
    }
}
