package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador de canais de chat com formatação otimizada.
 */
public class ChannelManager {
    
    private final PrimeLeagueChat plugin;
    private final Map<UUID, ChatChannel> playerChannels = new HashMap<>();
    private final OptimizedFormatService formatService;
    
    public enum ChatChannel {
        GLOBAL, CLAN, ALLY, LOCAL
    }
    
    public ChannelManager(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.formatService = new OptimizedFormatService(plugin);
    }
    
    public ChatChannel getPlayerChannel(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        return playerChannels.getOrDefault(canonicalUuid, ChatChannel.GLOBAL);
    }
    
    /**
     * Formata uma mensagem usando o serviço otimizado.
     */
    private String formatMessage(Player player, String format, String message) {
        return formatService.formatMessage(player, format, message);
    }
    
    public String formatGlobalMessage(Player player, String message) {
        return formatService.formatGlobalMessage(player, message);
    }
    
    public String formatClanMessage(Player player, String message) {
        return formatService.formatClanMessage(player, message);
    }
    
    public String formatAllyMessage(Player player, String message) {
        return formatService.formatAllyMessage(player, message);
    }
    
    public String formatLocalMessage(Player player, String message) {
        return formatService.formatLocalMessage(player, message);
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
