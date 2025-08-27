package br.com.primeleague.api;

import java.util.UUID;

/**
 * Interface para serviço de perfis de jogadores.
 * Permite que módulos externos acessem perfis sem dependência direta do Core.
 */
public interface ProfileService {
    
    /**
     * Obtém o nome de um jogador pelo UUID.
     * 
     * @param uuid UUID do jogador
     * @return Nome do jogador ou null se não encontrado
     */
    String getPlayerName(UUID uuid);
    
    /**
     * Obtém o nome de um jogador pelo nome.
     * 
     * @param name Nome do jogador
     * @return Nome do jogador (normalizado) ou null se não encontrado
     */
    String getPlayerName(String name);
}
