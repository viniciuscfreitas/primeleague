package br.com.primeleague.api;

import org.bukkit.entity.Player;

/**
 * Interface para serviços de clã.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface ClanService {
    
    /**
     * Obtém o nome do clã de um jogador.
     * 
     * @param player Jogador
     * @return Nome do clã ou null se não pertencer a nenhum clã
     */
    String getClanName(Player player);
    
    /**
     * Verifica se dois jogadores estão no mesmo clã.
     * 
     * @param player1 Primeiro jogador
     * @param player2 Segundo jogador
     * @return true se estão no mesmo clã, false caso contrário
     */
    boolean areInSameClan(Player player1, Player player2);
    
    /**
     * Verifica se dois jogadores estão em clãs aliados.
     * 
     * @param player1 Primeiro jogador
     * @param player2 Segundo jogador
     * @return true se estão em clãs aliados, false caso contrário
     */
    boolean areInAlliedClans(Player player1, Player player2);
    
    /**
     * Obtém o ID do clã de um jogador.
     * 
     * @param player Jogador
     * @return ID do clã ou null se não pertencer a nenhum clã
     */
    Integer getClanId(Player player);
}
