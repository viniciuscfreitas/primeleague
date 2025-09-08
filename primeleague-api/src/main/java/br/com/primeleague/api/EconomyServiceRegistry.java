package br.com.primeleague.api;

/**
 * Registry para o EconomyService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class EconomyServiceRegistry {
    
    private static EconomyService instance;
    
    /**
     * Registra uma implementação do EconomyService.
     * 
     * @param service Implementação do EconomyService
     */
    public static void register(EconomyService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do EconomyService.
     * 
     * @return Instância do EconomyService ou null se não registrada
     */
    public static EconomyService getInstance() {
        return instance;
    }
}
