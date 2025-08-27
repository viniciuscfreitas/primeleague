package br.com.primeleague.api;

/**
 * Registry para o AdminService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class AdminServiceRegistry {
    
    private static AdminService instance;
    
    /**
     * Registra uma implementação do AdminService.
     * 
     * @param service Implementação do AdminService
     */
    public static void register(AdminService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do AdminService.
     * 
     * @return Instância do AdminService ou null se não registrada
     */
    public static AdminService getInstance() {
        return instance;
    }
}
