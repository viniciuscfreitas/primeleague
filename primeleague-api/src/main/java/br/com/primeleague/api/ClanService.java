package br.com.primeleague.api;

import br.com.primeleague.api.dto.ClanDTO;
import org.bukkit.entity.Player;

import java.util.UUID;

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
    
    // ==================== MÉTODOS PARA O MÓDULO DE TERRITÓRIOS ====================
    
    /**
     * Obtém o clã de um jogador via UUID.
     * 
     * @param playerUUID UUID do jogador
     * @return ClanDTO do jogador ou null se não pertencer a nenhum clã
     */
    ClanDTO getPlayerClan(UUID playerUUID);
    
    /**
     * Obtém um clã por ID.
     * 
     * @param clanId ID do clã
     * @return ClanDTO do clã ou null se não encontrado
     */
    ClanDTO getClanById(int clanId);
    
    /**
     * Obtém um clã por nome.
     * 
     * @param clanName Nome do clã
     * @return ClanDTO do clã ou null se não encontrado
     */
    ClanDTO getClanByName(String clanName);
    
    /**
     * Obtém a moral de um clã.
     * 
     * @param clanId ID do clã
     * @return Moral do clã
     */
    double getClanMoral(int clanId);
    
    /**
     * Verifica se um jogador tem uma permissão específica no clã.
     * 
     * @param playerUUID UUID do jogador
     * @param permission Permissão a ser verificada
     * @return true se tem a permissão, false caso contrário
     */
    boolean hasPermission(UUID playerUUID, String permission);
    
    /**
     * Verifica se há uma trégua ativa entre dois clãs.
     * 
     * @param clanId1 ID do primeiro clã
     * @param clanId2 ID do segundo clã
     * @return true se há trégua ativa, false caso contrário
     */
    boolean hasActiveTruce(int clanId1, int clanId2);
}
