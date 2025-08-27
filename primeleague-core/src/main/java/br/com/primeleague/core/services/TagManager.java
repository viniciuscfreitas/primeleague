package br.com.primeleague.core.services;

import org.bukkit.entity.Player;

/**
 * Manager responsável por formatar placeholders em strings de texto.
 * Refatorado para usar o Padrão de Provedor, eliminando dependências diretas.
 * 
 * @author PrimeLeague Team
 * @version 1.1 - Refatorado para eliminar reflection
 */
public interface TagManager {
    
    /**
     * Formata uma string substituindo placeholders pelos valores apropriados do jogador.
     * 
     * @param player Jogador para o qual formatar os placeholders
     * @param text Texto contendo placeholders a serem substituídos
     * @return Texto formatado com os placeholders substituídos
     */
    String formatText(Player player, String text);
    
    /**
     * Registra um novo placeholder handler.
     * Permite extensibilidade para futuras tags.
     * 
     * @param placeholder Nome do placeholder (ex: "clan_tag")
     * @param handler Handler responsável por resolver o placeholder
     */
    void registerPlaceholder(String placeholder, PlaceholderHandler handler);
    
    /**
     * Interface para handlers de placeholders.
     * Implementação do Padrão de Provedor para resolução de tags.
     */
    interface PlaceholderHandler {
        /**
         * Resolve um placeholder para um jogador específico.
         * 
         * @param player Jogador para o qual resolver o placeholder
         * @return Valor do placeholder ou string vazia se não aplicável
         */
        String resolve(Player player);
    }
}
