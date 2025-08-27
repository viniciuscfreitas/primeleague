package br.com.primeleague.api;

/**
 * Registry para o ClanService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ClanServiceRegistry {
    
    private static ClanService instance;
    
    /**
     * Registra uma implementação do ClanService.
     * 
     * @param service Implementação do ClanService
     */
    public static void register(ClanService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do ClanService.
     * 
     * @return Instância do ClanService ou null se não registrada
     */
    public static ClanService getInstance() {
        return instance;
    }
}
