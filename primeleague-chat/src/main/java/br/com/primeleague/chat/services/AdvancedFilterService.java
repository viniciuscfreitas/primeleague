package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Serviço para gerenciar filtros avançados de moderação do chat.
 * Inclui filtros para CAPS, Links e palavras proibidas.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class AdvancedFilterService {
    
    private final PrimeLeagueChat plugin;
    
    // Configurações dos filtros
    private final int maxCapsPercentage;
    private final int minCapsLength;
    private final boolean allowLinks;
    private final List<String> allowedDomains;
    private final Set<String> bannedWords;
    private final boolean enableWordFilter;
    
    // Padrões regex para detecção de links
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\d\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b"
    );
    
    public AdvancedFilterService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        
        // Carregar configurações do config.yml
        this.maxCapsPercentage = plugin.getConfig().getInt("filters.caps.max_percentage", 70);
        this.minCapsLength = plugin.getConfig().getInt("filters.caps.min_length", 5);
        this.allowLinks = plugin.getConfig().getBoolean("filters.links.allow", false);
        this.allowedDomains = plugin.getConfig().getStringList("filters.links.allowed_domains");
        this.bannedWords = new HashSet<String>(plugin.getConfig().getStringList("filters.words.banned"));
        this.enableWordFilter = plugin.getConfig().getBoolean("filters.words.enable", true);
        
        plugin.getLogger().info("🛡️ Advanced Filter Service inicializado:");
        plugin.getLogger().info("   📊 CAPS Filter: " + maxCapsPercentage + "% máximo, " + minCapsLength + " chars mínimo");
        plugin.getLogger().info("   🔗 Link Filter: " + (allowLinks ? "Permitido" : "Bloqueado"));
        plugin.getLogger().info("   📝 Word Filter: " + (enableWordFilter ? "Ativo" : "Inativo") + " (" + bannedWords.size() + " palavras)");
    }
    
    /**
     * Resultado da verificação de filtros.
     */
    public static class FilterResult {
        private final boolean passed;
        private final String reason;
        private final FilterType filterType;
        
        public FilterResult(boolean passed, String reason, FilterType filterType) {
            this.passed = passed;
            this.reason = reason;
            this.filterType = filterType;
        }
        
        public boolean passed() { return passed; }
        public String getReason() { return reason; }
        public FilterType getFilterType() { return filterType; }
    }
    
    /**
     * Tipos de filtros disponíveis.
     */
    public enum FilterType {
        CAPS, LINKS, WORDS
    }
    
    /**
     * Verifica uma mensagem contra todos os filtros ativos.
     * 
     * @param player Jogador que enviou a mensagem
     * @param message Mensagem a ser verificada
     * @return Resultado da verificação
     */
    public FilterResult checkMessage(Player player, String message) {
        // Verificar se o jogador tem bypass de filtros
        if (player.hasPermission("primeleague.chat.bypass_filters")) {
            return new FilterResult(true, "Bypass permitido", null);
        }
        
        // Verificar CAPS
        FilterResult capsResult = checkCapsFilter(message);
        if (!capsResult.passed()) {
            return capsResult;
        }
        
        // Verificar Links
        FilterResult linksResult = checkLinksFilter(message);
        if (!linksResult.passed()) {
            return linksResult;
        }
        
        // Verificar Words
        FilterResult wordsResult = checkWordsFilter(message);
        if (!wordsResult.passed()) {
            return wordsResult;
        }
        
        return new FilterResult(true, "Mensagem aprovada", null);
    }
    
    /**
     * Verifica se a mensagem tem excesso de letras maiúsculas.
     * 
     * @param message Mensagem a verificar
     * @return Resultado da verificação
     */
    public FilterResult checkCapsFilter(String message) {
        if (message.length() < minCapsLength) {
            return new FilterResult(true, "Mensagem muito curta para verificar CAPS", FilterType.CAPS);
        }
        
        int totalLetters = 0;
        int capsLetters = 0;
        
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                totalLetters++;
                if (Character.isUpperCase(c)) {
                    capsLetters++;
                }
            }
        }
        
        if (totalLetters == 0) {
            return new FilterResult(true, "Nenhuma letra encontrada", FilterType.CAPS);
        }
        
        double capsPercentage = (double) capsLetters / totalLetters * 100;
        
        if (capsPercentage > maxCapsPercentage) {
            plugin.getLogger().info("🚫 [CAPS-FILTER] Mensagem bloqueada:");
            plugin.getLogger().info("   📊 Percentual de maiúsculas: " + String.format("%.1f", capsPercentage) + "%");
            plugin.getLogger().info("   📝 Mensagem: " + message);
            
            return new FilterResult(false, 
                "§cExcesso de letras maiúsculas (" + String.format("%.1f", capsPercentage) + "%). Máximo permitido: " + maxCapsPercentage + "%", 
                FilterType.CAPS);
        }
        
        return new FilterResult(true, "CAPS dentro do limite", FilterType.CAPS);
    }
    
    /**
     * Verifica se a mensagem contém links não autorizados.
     * 
     * @param message Mensagem a verificar
     * @return Resultado da verificação
     */
    public FilterResult checkLinksFilter(String message) {
        if (allowLinks) {
            // Se links são permitidos, verificar apenas domínios não autorizados
            return checkUnauthorizedDomains(message);
        } else {
            // Se links são bloqueados, verificar qualquer URL
            return checkAnyLinks(message);
        }
    }
    
    /**
     * Verifica se há links não autorizados quando links são permitidos.
     */
    private FilterResult checkUnauthorizedDomains(String message) {
        // Encontrar URLs na mensagem
        java.util.regex.Matcher urlMatcher = URL_PATTERN.matcher(message);
        
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            String domain = extractDomain(url);
            
            if (!isDomainAllowed(domain)) {
                plugin.getLogger().info("🚫 [LINK-FILTER] Domínio não autorizado detectado:");
                plugin.getLogger().info("   🔗 URL: " + url);
                plugin.getLogger().info("   🌐 Domínio: " + domain);
                
                return new FilterResult(false, 
                    "§cDomínio não autorizado: " + domain, 
                    FilterType.LINKS);
            }
        }
        
        return new FilterResult(true, "Links autorizados", FilterType.LINKS);
    }
    
    /**
     * Verifica se há qualquer link quando links são bloqueados.
     */
    private FilterResult checkAnyLinks(String message) {
        // Verificar URLs
        if (URL_PATTERN.matcher(message).find()) {
            plugin.getLogger().info("🚫 [LINK-FILTER] Link detectado (bloqueado):");
            plugin.getLogger().info("   📝 Mensagem: " + message);
            
            return new FilterResult(false, 
                "§cLinks não são permitidos no chat", 
                FilterType.LINKS);
        }
        
        // Verificar IPs
        if (IP_PATTERN.matcher(message).find()) {
            plugin.getLogger().info("🚫 [LINK-FILTER] IP detectado (bloqueado):");
            plugin.getLogger().info("   📝 Mensagem: " + message);
            
            return new FilterResult(false, 
                "§cEndereços IP não são permitidos no chat", 
                FilterType.LINKS);
        }
        
        return new FilterResult(true, "Nenhum link detectado", FilterType.LINKS);
    }
    
    /**
     * Extrai o domínio de uma URL.
     */
    private String extractDomain(String url) {
        try {
            // Remover protocolo
            String domain = url.replaceAll("^https?://", "");
            
            // Remover path e query parameters
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }
            
            // Remover porta se houver
            int colonIndex = domain.indexOf(':');
            if (colonIndex != -1) {
                domain = domain.substring(0, colonIndex);
            }
            
            return domain.toLowerCase();
        } catch (Exception e) {
            return url.toLowerCase();
        }
    }
    
    /**
     * Verifica se um domínio está na lista de permitidos.
     */
    private boolean isDomainAllowed(String domain) {
        for (String allowedDomain : allowedDomains) {
            if (domain.equals(allowedDomain.toLowerCase()) || 
                domain.endsWith("." + allowedDomain.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verifica se a mensagem contém palavras proibidas.
     * 
     * @param message Mensagem a verificar
     * @return Resultado da verificação
     */
    public FilterResult checkWordsFilter(String message) {
        if (!enableWordFilter || bannedWords.isEmpty()) {
            return new FilterResult(true, "Word filter desabilitado", FilterType.WORDS);
        }
        
        String lowerMessage = message.toLowerCase();
        
        for (String bannedWord : bannedWords) {
            if (lowerMessage.contains(bannedWord.toLowerCase())) {
                plugin.getLogger().info("🚫 [WORD-FILTER] Palavra proibida detectada:");
                plugin.getLogger().info("   🚫 Palavra: " + bannedWord);
                plugin.getLogger().info("   📝 Mensagem: " + message);
                
                return new FilterResult(false, 
                    "§cPalavra proibida detectada: " + bannedWord, 
                    FilterType.WORDS);
            }
        }
        
        return new FilterResult(true, "Nenhuma palavra proibida", FilterType.WORDS);
    }
    
    /**
     * Adiciona uma palavra à lista de proibidas.
     * 
     * @param word Palavra a adicionar
     * @return true se foi adicionada com sucesso
     */
    public boolean addBannedWord(String word) {
        boolean added = bannedWords.add(word.toLowerCase());
        if (added) {
            plugin.getLogger().info("🚫 [WORD-FILTER] Palavra adicionada: " + word);
        }
        return added;
    }
    
    /**
     * Remove uma palavra da lista de proibidas.
     * 
     * @param word Palavra a remover
     * @return true se foi removida com sucesso
     */
    public boolean removeBannedWord(String word) {
        boolean removed = bannedWords.remove(word.toLowerCase());
        if (removed) {
            plugin.getLogger().info("✅ [WORD-FILTER] Palavra removida: " + word);
        }
        return removed;
    }
    
    /**
     * Obtém a lista de palavras proibidas.
     * 
     * @return Lista de palavras proibidas (cópia defensiva)
     */
    public List<String> getBannedWords() {
        return new ArrayList<String>(bannedWords);
    }
    
    /**
     * Obtém estatísticas do serviço para debugging.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        return "Advanced Filter Service Stats: " +
               "CAPS(" + maxCapsPercentage + "%/" + minCapsLength + "), " +
               "Links(" + (allowLinks ? "Permitido" : "Bloqueado") + "), " +
               "Words(" + bannedWords.size() + " proibidas)";
    }
}
