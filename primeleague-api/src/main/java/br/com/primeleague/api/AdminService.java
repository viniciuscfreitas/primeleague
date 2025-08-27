package br.com.primeleague.api;

import java.util.UUID;

/**
 * Interface para serviços administrativos.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface AdminService {
    
    /**
     * Verifica se um jogador está mutado.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está mutado, false caso contrário
     */
    boolean isMuted(UUID playerUuid);
}
