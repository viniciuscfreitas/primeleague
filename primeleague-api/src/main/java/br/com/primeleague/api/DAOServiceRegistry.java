package br.com.primeleague.api;

import br.com.primeleague.api.dao.ClanDAO;

/**
 * Registry para serviços de acesso a dados (DAO).
 * Fornece acesso centralizado aos DAOs registrados pelos módulos.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public final class DAOServiceRegistry {
    
    private static DAOService instance;
    
    private DAOServiceRegistry() {
        // Construtor privado para evitar instanciação
    }
    
    /**
     * Registra uma implementação do DAOService.
     * 
     * @param service A implementação do DAOService
     * @throws IllegalStateException Se já houver um serviço registrado
     */
    public static void register(DAOService service) {
        if (instance != null) {
            throw new IllegalStateException("DAOService já registrado!");
        }
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do DAOService.
     * 
     * @return A instância do DAOService
     * @throws IllegalStateException Se nenhum serviço estiver registrado
     */
    public static DAOService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DAOService não registrado! Certifique-se de que o PrimeLeague-Core foi inicializado.");
        }
        return instance;
    }
    
    /**
     * Obtém o DAO de clãs.
     * 
     * @return O DAO de clãs
     */
    public static ClanDAO getClanDAO() {
        return getInstance().getClanDAO();
    }
}
