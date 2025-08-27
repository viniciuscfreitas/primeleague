package br.com.primeleague.core.services;

import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação do TagManager usando o Padrão de Provedor.
 * Orquestra handlers registrados por outros módulos, eliminando dependências diretas.
 * 
 * @author PrimeLeague Team
 * @version 1.1 - Refatorado para eliminar reflection
 */
public class TagManagerImpl implements TagManager {
    
    private final DataManager dataManager;
    private final Map<String, PlaceholderHandler> placeholderHandlers;
    private final Map<UUID, String> eloCache;
    
    // Pattern para encontrar placeholders no formato {placeholder}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    public TagManagerImpl(DataManager dataManager) {
        this.dataManager = dataManager;
        this.placeholderHandlers = new ConcurrentHashMap<>();
        this.eloCache = new ConcurrentHashMap<>();
        
        // Registrar apenas placeholders que o Core pode resolver diretamente
        registerCoreePlaceholders();
    }
    
    @Override
    public String formatText(Player player, String text) {
        if (player == null || text == null) {
            return text;
        }
        
        String result = text;
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            PlaceholderHandler handler = placeholderHandlers.get(placeholder);
            
            if (handler != null) {
                String replacement = handler.resolve(player);
                if (replacement != null) {
                    result = result.replace("{" + matcher.group(1) + "}", replacement);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public void registerPlaceholder(String placeholder, PlaceholderHandler handler) {
        if (placeholder != null && handler != null) {
            placeholderHandlers.put(placeholder.toLowerCase(), handler);
        }
    }
    
    /**
     * Registra os placeholders que o Core pode resolver diretamente.
     * Placeholders de outros módulos (como clan_tag) devem ser registrados
     * pelos próprios módulos durante o onEnable().
     */
    private void registerCoreePlaceholders() {
        // Placeholder para ELO - o Core tem acesso direto ao DataManager
        registerPlaceholder("elo", new PlaceholderHandler() {
            @Override
            public String resolve(Player player) {
                // USAR TRADUTOR DE IDENTIDADE
                UUID bukkitUuid = player.getUniqueId();
                UUID canonicalUuid = dataManager.getCanonicalUuid(bukkitUuid);
                
                // Verificar cache primeiro
                String cachedElo = eloCache.get(canonicalUuid);
                if (cachedElo != null) {
                    return cachedElo;
                }
                
                try {
                    PlayerProfile profile = dataManager.loadPlayerProfileWithClan(canonicalUuid, player.getName());
                    if (profile != null) {
                        String eloTag = "§6[" + profile.getElo() + "]";
                        eloCache.put(canonicalUuid, eloTag);
                        return eloTag;
                    }
                } catch (Exception e) {
                    // Log error silently to avoid spam
                }
                
                // Fallback: ELO padrão
                String defaultElo = "§6[1000]";
                eloCache.put(canonicalUuid, defaultElo);
                return defaultElo;
            }
        });
    }
    
    /**
     * Limpa o cache de ELO de um jogador específico.
     * Útil quando dados do jogador são atualizados.
     * 
     * @param playerUuid UUID do jogador
     */
    public void clearCache(UUID playerUuid) {
        eloCache.remove(playerUuid);
    }
    
    /**
     * Limpa todo o cache de ELO.
     * Útil para limpeza periódica ou quando há muitas atualizações.
     */
    public void clearAllCache() {
        eloCache.clear();
    }
    
    /**
     * Obtém estatísticas do manager para debugging.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        return String.format("TagManager Stats - Registered Handlers: %d, ELO Cache: %d", 
                           placeholderHandlers.size(), eloCache.size());
    }
}
