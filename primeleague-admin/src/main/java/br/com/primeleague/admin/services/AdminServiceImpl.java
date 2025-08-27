package br.com.primeleague.admin.services;

import br.com.primeleague.api.AdminService;
import br.com.primeleague.admin.managers.AdminManager;
import java.util.UUID;

/**
 * Implementação do AdminService para o módulo Admin.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class AdminServiceImpl implements AdminService {
    
    private final AdminManager adminManager;
    
    public AdminServiceImpl(AdminManager adminManager) {
        this.adminManager = adminManager;
    }
    
    @Override
    public boolean isMuted(UUID playerUuid) {
        return adminManager.isMuted(playerUuid);
    }
}
