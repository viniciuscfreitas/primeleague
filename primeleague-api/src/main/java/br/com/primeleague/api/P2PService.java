package br.com.primeleague.api;

import org.bukkit.entity.Player;

/**
 * Interface para serviços P2P.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface P2PService {
    
    /**
     * Verifica se um jogador está em limbo.
     * 
     * @param player Jogador a verificar
     * @return true se o jogador está em limbo, false caso contrário
     */
    boolean isInLimbo(Player player);
}
