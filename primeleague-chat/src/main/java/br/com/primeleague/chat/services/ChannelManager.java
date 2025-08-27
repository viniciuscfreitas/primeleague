package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.api.TagServiceRegistry;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador de canais de chat (versão simplificada para teste).
 */
public class ChannelManager {
    
    private final PrimeLeagueChat plugin;
    private final Map<UUID, ChatChannel> playerChannels = new HashMap<>();
    
    public enum ChatChannel {
        GLOBAL, CLAN, ALLY, LOCAL
    }
    
    public ChannelManager(PrimeLeagueChat plugin) {
        this.plugin = plugin;
    }
    
    public ChatChannel getPlayerChannel(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        return playerChannels.getOrDefault(canonicalUuid, ChatChannel.GLOBAL);
    }
    
    /**
     * Formata uma mensagem usando o TagService para resolver placeholders.
     */
    private String formatMessage(Player player, String format, String message) {
        try {
            // Aplicar placeholders usando TagService
            String formattedText = TagServiceRegistry.formatText(player, format);
            
            // Substituir {player} e {message} pelos valores reais
            formattedText = formattedText.replace("{player}", player.getName());
            formattedText = formattedText.replace("{message}", message);
            
            // Aplicar cores se permitido
            if (plugin.getConfig().getBoolean("global.allow_colors", true)) {
                formattedText = ChatColor.translateAlternateColorCodes('&', formattedText);
            }
            
            return formattedText;
        } catch (Exception e) {
            // Fallback em caso de erro
            plugin.getLogger().warning("Erro ao formatar mensagem: " + e.getMessage());
            return "§7[§aChat§7] §f" + player.getName() + ": §7" + message;
        }
    }
    
    public String formatGlobalMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.global.format", 
            "&7[&aGlobal&7] {clan_tag}&f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    public String formatClanMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.clan.format", 
            "&7[&bClã&7] {clan_tag}&f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    public String formatAllyMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.ally.format", 
            "&7[&dAliança&7] {clan_tag}&f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    public String formatLocalMessage(Player player, String message) {
        String format = plugin.getConfig().getString("channels.local.format", 
            "&7[&eLocal&7] {clan_tag}&f{player}&7: &f{message}");
        return formatMessage(player, format, message);
    }
    
    public void setPlayerChannel(Player player, ChatChannel channel) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        playerChannels.put(canonicalUuid, channel);
    }
    
    public int getLocalChatRadius() {
        return plugin.getConfig().getInt("channels.local.radius", 100);
    }
    
    public void clearPlayerChannel(UUID playerUuid) {
        playerChannels.remove(playerUuid);
    }
}
