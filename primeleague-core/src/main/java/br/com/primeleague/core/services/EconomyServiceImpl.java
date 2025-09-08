package br.com.primeleague.core.services;

import br.com.primeleague.api.EconomyService;
import br.com.primeleague.core.managers.EconomyManager;
import br.com.primeleague.core.managers.IdentityManager;

import java.util.UUID;

/**
 * Implementação do EconomyService que usa o EconomyManager do Core.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class EconomyServiceImpl implements EconomyService {
    
    private final EconomyManager economyManager;
    private final IdentityManager identityManager;
    
    public EconomyServiceImpl(EconomyManager economyManager, IdentityManager identityManager) {
        this.economyManager = economyManager;
        this.identityManager = identityManager;
    }
    
    @Override
    public double getPlayerBalance(UUID playerUUID) {
        Integer playerId = identityManager.getPlayerIdByUuid(playerUUID);
        if (playerId == null) {
            return 0.0;
        }
        
        return economyManager.getBalance(playerId).doubleValue();
    }
    
    @Override
    public boolean withdrawPlayerMoney(UUID playerUUID, double amount, String reason) {
        Integer playerId = identityManager.getPlayerIdByUuid(playerUUID);
        if (playerId == null) {
            return false;
        }
        
        try {
            var response = economyManager.withdrawMoney(playerId, amount, reason);
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean depositPlayerMoney(UUID playerUUID, double amount, String reason) {
        Integer playerId = identityManager.getPlayerIdByUuid(playerUUID);
        if (playerId == null) {
            return false;
        }
        
        try {
            var response = economyManager.depositMoney(playerId, amount, reason);
            return response.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
}