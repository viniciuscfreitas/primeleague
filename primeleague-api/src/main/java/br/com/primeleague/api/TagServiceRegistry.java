package br.com.primeleague.api;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Registry para o TagService.
 * Permite que o Core registre sua implementação do TagService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class TagServiceRegistry {
    
    private static TagService instance;
    
    /**
     * Registra uma implementação do TagService.
     * 
     * @param service Implementação do TagService
     */
    public static void register(TagService service) {
        instance = service;
    }
    
    /**
     * Obtém a implementação registrada do TagService.
     * 
     * @return Implementação do TagService ou null se não registrada
     */
    public static TagService getInstance() {
        return instance;
    }
    
    /**
     * Formata uma string substituindo placeholders pelos valores apropriados do jogador.
     * 
     * @param player Jogador para o qual formatar os placeholders
     * @param text Texto contendo placeholders a serem substituídos
     * @return Texto formatado com os placeholders substituídos ou texto original se serviço não registrado
     */
    public static String formatText(Player player, String text) {
        if (instance == null) return text;
        return instance.formatText(player, text);
    }
    
    /**
     * Registra um novo placeholder handler.
     * 
     * @param placeholder Nome do placeholder
     * @param handler Handler responsável por resolver o placeholder
     */
    public static void registerPlaceholder(String placeholder, TagService.PlaceholderHandler handler) {
        if (instance != null) {
            instance.registerPlaceholder(placeholder, handler);
        }
    }
}
