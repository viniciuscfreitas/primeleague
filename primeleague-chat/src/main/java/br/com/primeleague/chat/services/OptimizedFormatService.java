package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço otimizado de formatação de mensagens com cache e StringBuilder.
 * Resolve o problema de performance identificado na formatação de strings na thread principal.
 */
public class OptimizedFormatService {
    
    private final PrimeLeagueChat plugin;
    
    // Cache para templates formatados (thread-safe)
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();
    
    // Cache para partes estáticas dos formatos
    private final Map<String, String> staticFormatCache = new ConcurrentHashMap<>();
    
    public OptimizedFormatService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        
        // Limpeza forçada do cache para evitar dados corrompidos
        templateCache.clear();
        staticFormatCache.clear();
        
        plugin.getLogger().info("⚡ Optimized Format Service inicializado");
        plugin.getLogger().info("🧹 Cache limpo para evitar tags corrompidas");
    }
    
    /**
     * Formata uma mensagem de forma otimizada usando StringBuilder e cache.
     * 
     * @param player Jogador que enviou a mensagem
     * @param formatTemplate Template de formatação
     * @param message Conteúdo da mensagem
     * @return Mensagem formatada
     */
    public String formatMessage(Player player, String formatTemplate, String message) {
        try {
            // 1. Tentar obter do cache primeiro
            String cacheKey = formatTemplate + ":" + player.getName();
            String cachedResult = templateCache.get(cacheKey);
            if (cachedResult != null) {
                // Apenas substituir a mensagem no resultado em cache
                return cachedResult.replace("{message}", message);
            }
            
            // 2. Processar template com tags de clã
            String processedTemplate = processClanTags(player, formatTemplate);
            
            // 3. Usar StringBuilder para substituições (mais eficiente que múltiplos replace)
            StringBuilder formattedText = new StringBuilder(processedTemplate);
            
            // Substituir placeholders dinâmicos
            replacePlaceholder(formattedText, "{player}", player.getName());
            
            // 4. Aplicar formatação ANTES de substituir a mensagem
            String templateWithColors = applyGranularFormatting(player, formattedText.toString());
            
            // 5. Agora substituir a mensagem (que pode ter códigos de cor)
            String finalResult = templateWithColors.replace("{message}", message);
            
            // 6. Cache do resultado (sem a mensagem específica)
            String staticResult = templateWithColors;
            templateCache.put(cacheKey, staticResult);
            
            // 6. Limpar cache se ficar muito grande (prevenir memory leak)
            if (templateCache.size() > 1000) {
                templateCache.clear();
                plugin.getLogger().info("🧹 Cache de templates limpo (muito grande)");
            }
            
            return finalResult;
            
        } catch (Exception e) {
            // Fallback em caso de erro
            plugin.getLogger().warning("Erro ao formatar mensagem otimizada: " + e.getMessage());
            return formatFallback(player, message);
        }
    }
    
    /**
     * Substitui um placeholder no StringBuilder de forma eficiente.
     */
    private void replacePlaceholder(StringBuilder sb, String placeholder, String replacement) {
        int index = sb.indexOf(placeholder);
        if (index != -1) {
            sb.replace(index, index + placeholder.length(), replacement);
        }
    }
    
    /**
     * Formatação de fallback em caso de erro.
     */
    private String formatFallback(Player player, String message) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("§7[§aChat§7] §f");
        fallback.append(player.getName());
        fallback.append(": §7");
        fallback.append(message);
        return fallback.toString();
    }
    
    /**
     * Processa tags de clã no template.
     */
    private String processClanTags(Player player, String template) {
        if (!template.contains("{clan_tag}")) {
            return template;
        }
        
        try {
            // Obter tag do clã diretamente do banco (sem TagServiceRegistry)
            String clanTag = getPlayerClanTagFromDatabase(player);
            return template.replace("{clan_tag}", clanTag);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao processar tag de clã para " + player.getName() + ": " + e.getMessage());
            return template.replace("{clan_tag}", "");
        }
    }
    
    /**
     * Obtém a tag do clã do jogador diretamente do banco de dados.
     */
    private String getPlayerClanTagFromDatabase(Player player) {
        try {
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Iniciando busca de tag para " + player.getName());
            
            // Usar a API do Core para obter dados do jogador
            UUID canonicalUuid = br.com.primeleague.core.api.PrimeLeagueAPI.getDataManager().getCanonicalUuid(player.getUniqueId());
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] UUID canônico para " + player.getName() + ": " + canonicalUuid);
            
            // Query CORRIGIDA: usar pd.player_id em vez de pd.id
            String sql = "SELECT c.tag FROM clans c " +
                        "INNER JOIN clan_players cp ON c.id = cp.clan_id " +
                        "INNER JOIN player_data pd ON cp.player_id = pd.player_id " +
                        "WHERE pd.uuid = ?";
            
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Executando query: " + sql);
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Parâmetro UUID: " + canonicalUuid.toString());
            
            // Usar a conexão do Core
            java.sql.Connection conn = br.com.primeleague.core.api.PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("🔍 [CLAN-TAG-DEBUG] Conexão com banco é null!");
                return "";
            }
            
            java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, canonicalUuid.toString());
            
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Query preparada, executando...");
            java.sql.ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String tag = rs.getString("tag");
                rs.close();
                stmt.close();
                
                plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] ✅ Tag encontrada para " + player.getName() + ": " + tag);
                return "[" + tag + "] ";
            }
            
            rs.close();
            stmt.close();
            
            plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] ❌ Nenhuma tag encontrada para " + player.getName());
            
            // DEBUG: Verificar se o jogador existe na tabela player_data
            String debugSql = "SELECT player_id, name FROM player_data WHERE uuid = ?";
            java.sql.PreparedStatement debugStmt = conn.prepareStatement(debugSql);
            debugStmt.setString(1, canonicalUuid.toString());
            java.sql.ResultSet debugRs = debugStmt.executeQuery();
            
            if (debugRs.next()) {
                int playerId = debugRs.getInt("player_id");
                String playerName = debugRs.getString("name");
                plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Jogador encontrado na player_data: ID=" + playerId + ", Nome=" + playerName);
                
                // DEBUG: Verificar se existe na clan_players
                String clanDebugSql = "SELECT cp.clan_id, cp.role FROM clan_players cp WHERE cp.player_id = ?";
                java.sql.PreparedStatement clanDebugStmt = conn.prepareStatement(clanDebugSql);
                clanDebugStmt.setInt(1, playerId);
                java.sql.ResultSet clanDebugRs = clanDebugStmt.executeQuery();
                
                if (clanDebugRs.next()) {
                    int clanId = clanDebugRs.getInt("clan_id");
                    String role = clanDebugRs.getString("role");
                    plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Jogador encontrado na clan_players: ClanID=" + clanId + ", Role=" + role);
                    
                    // DEBUG: Verificar se o clã existe
                    String clanExistsSql = "SELECT tag FROM clans WHERE id = ?";
                    java.sql.PreparedStatement clanExistsStmt = conn.prepareStatement(clanExistsSql);
                    clanExistsStmt.setInt(1, clanId);
                    java.sql.ResultSet clanExistsRs = clanExistsStmt.executeQuery();
                    
                    if (clanExistsRs.next()) {
                        String clanTag = clanExistsRs.getString("tag");
                        plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Clã encontrado: Tag=" + clanTag);
                        clanExistsRs.close();
                        clanExistsStmt.close();
                    } else {
                        plugin.getLogger().warning("🔍 [CLAN-TAG-DEBUG] Clã com ID " + clanId + " não encontrado!");
                    }
                    
                    clanDebugRs.close();
                    clanDebugStmt.close();
                } else {
                    plugin.getLogger().info("🔍 [CLAN-TAG-DEBUG] Jogador NÃO encontrado na clan_players");
                }
                
            } else {
                plugin.getLogger().warning("🔍 [CLAN-TAG-DEBUG] Jogador NÃO encontrado na player_data!");
            }
            
            debugRs.close();
            debugStmt.close();
            
            return "";
            
        } catch (Exception e) {
            plugin.getLogger().warning("🔍 [CLAN-TAG-DEBUG] Erro ao buscar tag de clã no banco para " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Formata mensagem global com cache otimizado.
     */
    public String formatGlobalMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.global.format", 
            "&7[&aGlobal&7] &f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    /**
     * Formata mensagem de clã com cache otimizado.
     */
    public String formatClanMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.clan.format", 
            "&7[&bClã&7] &f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    /**
     * Formata mensagem de aliança com cache otimizado.
     */
    public String formatAllyMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.ally.format", 
            "&7[&dAliança&7] &f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    /**
     * Formata mensagem local com cache otimizado.
     */
    public String formatLocalMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.local.format", 
            "&7[&eLocal&7] &f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    /**
     * Limpa o cache de formatação.
     * Útil para manutenção ou quando configurações mudam.
     */
    public void clearCache() {
        templateCache.clear();
        staticFormatCache.clear();
        plugin.getLogger().info("🧹 Cache de formatação limpo");
    }
    
    /**
     * Obtém estatísticas do cache para debugging.
     */
    public String getCacheStats() {
        return "Template Cache: " + templateCache.size() + " entradas, " +
               "Static Format Cache: " + staticFormatCache.size() + " entradas";
    }
    
    /**
     * Aplica formatação granular baseada em permissões do jogador.
     * 
     * @param player Jogador que enviou a mensagem
     * @param message Mensagem a ser formatada
     * @return Mensagem com formatação aplicada ou códigos removidos
     */
    private String applyGranularFormatting(Player player, String message) {
        // SEMPRE aplicar cores para mensagens do sistema (formatação dos canais)
        // As permissões só afetam a mensagem do jogador, não a formatação do sistema
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // Verificar permissões apenas para a mensagem do jogador
        boolean hasColorPermission = PrimeLeagueAPI.hasPermission(player, "primeleague.chat.color");
        boolean hasFormatPermission = PrimeLeagueAPI.hasPermission(player, "primeleague.chat.format");
        
        // Se o jogador não tem permissões, remover códigos apenas da sua mensagem
        if (!hasColorPermission && !hasFormatPermission) {
            return stripPlayerMessageCodes(formattedMessage);
        }
        
        return formattedMessage;
    }
    
    /**
     * Remove códigos de cor apenas da mensagem do jogador, preservando formatação do sistema.
     */
    private String stripPlayerMessageCodes(String message) {
        // Preservar formatação do sistema (até o ": ")
        int colonIndex = message.lastIndexOf(": ");
        if (colonIndex != -1) {
            String systemPart = message.substring(0, colonIndex + 2); // Incluir ": "
            String playerMessage = message.substring(colonIndex + 2);
            
            // Remover códigos apenas da mensagem do jogador (tanto & quanto §)
            String cleanPlayerMessage = playerMessage.replaceAll("[&§][0-9a-fk-or]", "");
            
            return systemPart + cleanPlayerMessage;
        }
        
        // Se não encontrar ":", remover todos os códigos
        return message.replaceAll("[&§][0-9a-fk-or]", "");
    }
    
    /**
     * Aplica apenas códigos de cor (&0-9, &a-f) na mensagem.
     */
    private void applyColorCodes(StringBuilder message) {
        String result = message.toString();
        result = result.replaceAll("&([0-9a-f])", "§$1");
        message.setLength(0);
        message.append(result);
    }
    
    /**
     * Remove códigos de cor (&0-9, &a-f) da mensagem.
     */
    private void stripColorCodes(StringBuilder message) {
        String result = message.toString();
        result = result.replaceAll("&[0-9a-f]", "");
        message.setLength(0);
        message.append(result);
    }
    
    /**
     * Aplica apenas códigos de formatação (&k, &l, &m, &n, &o) na mensagem.
     */
    private void applyFormatCodes(StringBuilder message) {
        String result = message.toString();
        result = result.replaceAll("&([k-or])", "§$1");
        message.setLength(0);
        message.append(result);
    }
    
    /**
     * Remove códigos de formatação (&k, &l, &m, &n, &o) da mensagem.
     */
    private void stripFormatCodes(StringBuilder message) {
        String result = message.toString();
        result = result.replaceAll("&[k-or]", "");
        message.setLength(0);
        message.append(result);
    }
}
