package br.com.primeleague.core.services;

import br.com.primeleague.api.TagService;
import org.bukkit.entity.Player;

/**
 * Adapter que expõe o TagManager como TagService através da API.
 * Implementa o Padrão de Provedor para outros módulos registrarem handlers.
 * 
 * @author PrimeLeague Team
 * @version 1.1 - Refatorado para eliminar reflection
 */
public class TagServiceAdapter implements TagService {
    
    private final TagManager tagManager;
    
    public TagServiceAdapter(TagManager tagManager) {
        this.tagManager = tagManager;
    }
    
    @Override
    public String formatText(Player player, String text) {
        return tagManager.formatText(player, text);
    }
    
    @Override
    public void registerPlaceholder(String placeholder, final TagService.PlaceholderHandler handler) {
        // Converter TagService.PlaceholderHandler para TagManager.PlaceholderHandler
        TagManager.PlaceholderHandler adapterHandler = new TagManager.PlaceholderHandler() {
            @Override
            public String resolve(Player player) {
                return handler.resolve(player);
            }
        };
        
        tagManager.registerPlaceholder(placeholder, adapterHandler);
    }
}
