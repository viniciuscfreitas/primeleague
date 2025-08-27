package br.com.primeleague.core.services;

import br.com.primeleague.api.ProfileService;
import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.models.PlayerProfile;

import java.util.UUID;

/**
 * Implementação do ProfileService no Core.
 * Usa o DataManager para obter informações de jogadores.
 */
public class CoreProfileService implements ProfileService {
    
    private final DataManager dataManager;
    
    public CoreProfileService(DataManager dataManager) {
        this.dataManager = dataManager;
    }
    
    @Override
    public String getPlayerName(UUID uuid) {
        try {
            PlayerProfile profile = dataManager.loadOfflinePlayerProfile(uuid);
            return profile != null ? profile.getPlayerName() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public String getPlayerName(String name) {
        try {
            PlayerProfile profile = dataManager.loadOfflinePlayerProfile(name);
            return profile != null ? profile.getPlayerName() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
