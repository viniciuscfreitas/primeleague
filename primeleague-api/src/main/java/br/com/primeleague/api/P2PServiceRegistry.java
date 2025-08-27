package br.com.primeleague.api;

/**
 * Registry para o P2PService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class P2PServiceRegistry {
    
    private static P2PService instance;
    
    /**
     * Registra uma implementação do P2PService.
     * 
     * @param service Implementação do P2PService
     */
    public static void register(P2PService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do P2PService.
     * 
     * @return Instância do P2PService ou null se não registrada
     */
    public static P2PService getInstance() {
        return instance;
    }
}
