package br.com.primeleague.api;

/**
 * Registry para o IdentityService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class IdentityServiceRegistry {
    
    private static IdentityService instance;
    
    /**
     * Registra uma implementação do IdentityService.
     * 
     * @param service Implementação do IdentityService
     */
    public static void register(IdentityService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do IdentityService.
     * 
     * @return Instância do IdentityService ou null se não registrada
     */
    public static IdentityService getInstance() {
        return instance;
    }
}
