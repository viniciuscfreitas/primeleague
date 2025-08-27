package br.com.primeleague.api;

import br.com.primeleague.api.dao.ClanDAO;

/**
 * Interface para serviços de acesso a dados (DAO).
 * Centraliza o acesso aos diferentes DAOs do sistema.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface DAOService {
    
    /**
     * Obtém o DAO de clãs.
     * 
     * @return O DAO de clãs
     */
    ClanDAO getClanDAO();
}
