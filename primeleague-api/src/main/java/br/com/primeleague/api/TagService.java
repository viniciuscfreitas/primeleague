package br.com.primeleague.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Interface para o serviço de formatação de tags.
 * Usa o Padrão de Provedor para desacoplar a lógica de resolução de placeholders.
 * 
 * @author PrimeLeague Team
 * @version 1.1 - Refatorado para eliminar reflection
 */
public interface TagService {
    
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
     * Este é o método principal para extensibilidade - outros módulos devem
     * registrar seus handlers aqui durante o onEnable().
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
