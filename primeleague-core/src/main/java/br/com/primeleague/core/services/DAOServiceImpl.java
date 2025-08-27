package br.com.primeleague.core.services;

import br.com.primeleague.api.DAOService;
import br.com.primeleague.api.dao.ClanDAO;

/**
 * Implementação do DAOService no PrimeLeague-Core.
 * Fornece acesso aos DAOs implementados no Core.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class DAOServiceImpl implements DAOService {
    
    private final ClanDAO clanDAO;
    
    public DAOServiceImpl(ClanDAO clanDAO) {
        this.clanDAO = clanDAO;
    }
    
    @Override
    public ClanDAO getClanDAO() {
        return clanDAO;
    }
}
