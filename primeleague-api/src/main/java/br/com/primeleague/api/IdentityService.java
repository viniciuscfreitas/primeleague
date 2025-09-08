package br.com.primeleague.api;

import org.bukkit.entity.Player;

/**
 * Interface para serviços de identidade.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface IdentityService {
    
    /**
     * Obtém o ID do jogador no banco de dados.
     * 
     * @param player Jogador
     * @return ID do jogador ou -1 se não encontrado
     */
    int getPlayerId(Player player);
}
