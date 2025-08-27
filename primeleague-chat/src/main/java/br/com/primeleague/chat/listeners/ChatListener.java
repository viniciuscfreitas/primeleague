package br.com.primeleague.chat.listeners;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelManager.ChatChannel;
import br.com.primeleague.api.P2PServiceRegistry;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

/**
 * Listener para interceptar eventos de chat (versão simplificada para teste).
 */
public class ChatListener implements Listener {
    
    private final PrimeLeagueChat plugin;
    private final ChannelManager channelManager;
    
    public ChatListener(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.channelManager = plugin.getChannelManager();
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // BARRERA DE SEGURANÇA: Verificar se o perfil do jogador está carregado
        // USAR TRADUTOR DE IDENTIDADE para obter UUID canônico
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        if (PrimeLeagueAPI.getDataManager().isLoading(canonicalUuid)) {
            player.sendMessage("§cSeu perfil ainda está carregando. Tente novamente em um instante.");
            event.setCancelled(true);
            return;
        }
        
        // Verificar se o jogador está em limbo (P2P)
        if (isPlayerInLimbo(player)) {
            plugin.getLogger().info("🚫 [CHAT-EVENT] Jogador em limbo (P2P):");
            plugin.getLogger().info("   👤 Jogador: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
            plugin.getLogger().info("   🔒 Status: Verificação pendente");
            
            event.setCancelled(true);
            player.sendMessage("§c🚫 Chat desabilitado durante a verificação!");
            player.sendMessage("§eUse §a/verify <código> §epara completar a verificação.");
            return;
        }
        
        // Verificar se o jogador está mutado (Admin)
        if (isPlayerMuted(player)) {
            plugin.getLogger().info("🔇 [CHAT-EVENT] Jogador mutado (Admin):");
            plugin.getLogger().info("   👤 Jogador: " + player.getName() + " (UUID: " + player.getUniqueId() + ")");
            plugin.getLogger().info("   🔇 Status: Mute ativo");
            
            event.setCancelled(true);
            // A mensagem de mute será enviada pelo AdminManager
            return;
        }
        
        // Cancelar o evento padrão para processar manualmente
        event.setCancelled(true);
        
        // Obter o canal ativo do jogador
        ChatChannel activeChannel = channelManager.getPlayerChannel(player);
        
        plugin.getLogger().info("📤 [CHAT-EVENT] Canal ativo do jogador:");
        plugin.getLogger().info("   👤 Jogador: " + player.getName());
        plugin.getLogger().info("   📡 Canal: " + activeChannel.name());
        
        // Processar a mensagem baseada no canal
        switch (activeChannel) {
            case GLOBAL:
                handleGlobalChat(player, message);
                break;
            case CLAN:
                handleClanChat(player, message);
                break;
            case ALLY:
                handleAllyChat(player, message);
                break;
            case LOCAL:
                handleLocalChat(player, message);
                break;
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpar canal do jogador quando ele sai
        channelManager.clearPlayerChannel(event.getPlayer().getUniqueId());
    }
    
    private void handleGlobalChat(Player player, String message) {
        String formattedMessage = channelManager.formatGlobalMessage(player, message);
        
        // Enviar para todos os jogadores online
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(formattedMessage);
        }
        
        // Log da mensagem
        plugin.getLoggingService().logMessage("GLOBAL", player, null, message);
    }
    
    private void handleClanChat(Player player, String message) {
        if (!player.hasPermission("primeleague.chat.clan")) {
            player.sendMessage("§cVocê não tem permissão para usar o chat de clã.");
            return;
        }
        
        // Obter clã do jogador via ClanManager
        String clanName = getPlayerClanName(player);
        if (clanName == null) {
            player.sendMessage("§cVocê não pertence a nenhum clã.");
            return;
        }
        
        String formattedMessage = channelManager.formatClanMessage(player, message);
        
        // Enviar para membros do clã
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isPlayerInSameClan(player, onlinePlayer)) {
                onlinePlayer.sendMessage(formattedMessage);
            }
        }
        
        // Log da mensagem
        plugin.getLoggingService().logMessage("CLAN", player, null, message);
    }
    
    private void handleAllyChat(Player player, String message) {
        if (!player.hasPermission("primeleague.chat.ally")) {
            player.sendMessage("§cVocê não tem permissão para usar o chat de aliança.");
            return;
        }
        
        // Obter clã do jogador via ClanManager
        String clanName = getPlayerClanName(player);
        if (clanName == null) {
            player.sendMessage("§cVocê não pertence a nenhum clã.");
            return;
        }
        
        String formattedMessage = channelManager.formatAllyMessage(player, message);
        
        // Enviar para membros do clã e aliados
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isPlayerInSameClan(player, onlinePlayer) || isPlayerInAlliedClan(player, onlinePlayer)) {
                onlinePlayer.sendMessage(formattedMessage);
            }
        }
        
        // Log da mensagem
        plugin.getLoggingService().logMessage("ALLY", player, null, message);
    }
    
    private void handleLocalChat(Player player, String message) {
        String formattedMessage = channelManager.formatLocalMessage(player, message);
        
        int radius = channelManager.getLocalChatRadius();
        
        // Encontrar jogadores dentro do raio
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().equals(player.getWorld()) &&
                onlinePlayer.getLocation().distance(player.getLocation()) <= radius) {
                onlinePlayer.sendMessage(formattedMessage);
            }
        }
        
        // Log da mensagem
        plugin.getLoggingService().logMessage("LOCAL", player, null, message);
    }
    
    /**
     * Verifica se um jogador está em limbo (P2P).
     */
    private boolean isPlayerInLimbo(Player player) {
        try {
            // Usar a API correta do P2P através do P2PServiceRegistry
            return P2PServiceRegistry.getInstance().isInLimbo(player);
        } catch (Exception e) {
            // Se não conseguir acessar o P2P, assumir que não está em limbo
            plugin.getLogger().warning("Não foi possível verificar status de limbo: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Verifica se um jogador está mutado (Admin).
     */
    private boolean isPlayerMuted(Player player) {
        try {
            // Tentar obter o AdminManager do Admin
            org.bukkit.plugin.Plugin adminPlugin = Bukkit.getPluginManager().getPlugin("PrimeLeague-Admin");
            if (adminPlugin != null && adminPlugin.isEnabled()) {
                // Usar reflection para acessar o AdminManager
                Class<?> adminManagerClass = Class.forName("br.com.primeleague.admin.managers.AdminManager");
                java.lang.reflect.Method getInstanceMethod = adminManagerClass.getMethod("getInstance");
                Object adminManager = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method isMutedMethod = adminManagerClass.getMethod("isMuted", java.util.UUID.class);
                // USAR TRADUTOR DE IDENTIDADE
                UUID bukkitUuid = player.getUniqueId();
                UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
                return (Boolean) isMutedMethod.invoke(adminManager, canonicalUuid);
            }
        } catch (Exception e) {
            // Se não conseguir acessar o Admin, assumir que não está mutado
            plugin.getLogger().warning("Não foi possível verificar status de mute: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Obtém o nome do clã do jogador via ClanManager.
     */
    private String getPlayerClanName(Player player) {
        try {
            // Tentar obter o ClanManager do Clans
            org.bukkit.plugin.Plugin clansPlugin = Bukkit.getPluginManager().getPlugin("PrimeLeague-Clans");
            if (clansPlugin != null && clansPlugin.isEnabled()) {
                // Usar reflection para acessar o ClanManager
                Class<?> clansMainClass = Class.forName("br.com.primeleague.clans.PrimeLeagueClans");
                java.lang.reflect.Method getInstanceMethod = clansMainClass.getMethod("getInstance");
                Object clansPluginInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getClanManagerMethod = clansMainClass.getMethod("getClanManager");
                Object clanManager = getClanManagerMethod.invoke(clansPluginInstance);
                
                java.lang.reflect.Method getClanByPlayerMethod = clanManager.getClass().getMethod("getClanByPlayer", Player.class);
                Object clan = getClanByPlayerMethod.invoke(clanManager, player);
                
                if (clan != null) {
                    java.lang.reflect.Method getNameMethod = clan.getClass().getMethod("getName");
                    return (String) getNameMethod.invoke(clan);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Não foi possível obter clã do jogador: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Obtém o ID do clã do jogador via ClanManager.
     */
    private Integer getPlayerClanId(Player player) {
        try {
            // Tentar obter o ClanManager do Clans
            org.bukkit.plugin.Plugin clansPlugin = Bukkit.getPluginManager().getPlugin("PrimeLeague-Clans");
            if (clansPlugin != null && clansPlugin.isEnabled()) {
                // Usar reflection para acessar o ClanManager
                Class<?> clansMainClass = Class.forName("br.com.primeleague.clans.PrimeLeagueClans");
                java.lang.reflect.Method getInstanceMethod = clansMainClass.getMethod("getInstance");
                Object clansPluginInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getClanManagerMethod = clansMainClass.getMethod("getClanManager");
                Object clanManager = getClanManagerMethod.invoke(clansPluginInstance);
                
                java.lang.reflect.Method getClanByPlayerMethod = clanManager.getClass().getMethod("getClanByPlayer", Player.class);
                Object clan = getClanByPlayerMethod.invoke(clanManager, player);
                
                if (clan != null) {
                    java.lang.reflect.Method getIdMethod = clan.getClass().getMethod("getId");
                    return (Integer) getIdMethod.invoke(clan);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Não foi possível obter ID do clã do jogador: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Verifica se dois jogadores estão no mesmo clã.
     */
    private boolean isPlayerInSameClan(Player player1, Player player2) {
        String clan1 = getPlayerClanName(player1);
        String clan2 = getPlayerClanName(player2);
        return clan1 != null && clan1.equals(clan2);
    }
    
    /**
     * Verifica se dois jogadores estão em clãs aliados.
     */
    private boolean isPlayerInAlliedClan(Player player1, Player player2) {
        try {
            // Tentar obter o ClanManager do Clans
            org.bukkit.plugin.Plugin clansPlugin = Bukkit.getPluginManager().getPlugin("PrimeLeague-Clans");
            if (clansPlugin != null && clansPlugin.isEnabled()) {
                // Usar reflection para acessar o ClanManager
                Class<?> clansMainClass = Class.forName("br.com.primeleague.clans.PrimeLeagueClans");
                java.lang.reflect.Method getInstanceMethod = clansMainClass.getMethod("getInstance");
                Object clansPluginInstance = getInstanceMethod.invoke(null);
                
                java.lang.reflect.Method getClanManagerMethod = clansMainClass.getMethod("getClanManager");
                Object clanManager = getClanManagerMethod.invoke(clansPluginInstance);
                
                // Obter clãs dos jogadores
                java.lang.reflect.Method getClanByPlayerMethod = clanManager.getClass().getMethod("getClanByPlayer", Player.class);
                Object clan1 = getClanByPlayerMethod.invoke(clanManager, player1);
                Object clan2 = getClanByPlayerMethod.invoke(clanManager, player2);
                
                if (clan1 != null && clan2 != null) {
                    // Verificar se são aliados
                    java.lang.reflect.Method areAlliesMethod = clanManager.getClass().getMethod("areAllies", Object.class, Object.class);
                    return (Boolean) areAlliesMethod.invoke(clanManager, clan1, clan2);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Não foi possível verificar aliança: " + e.getMessage());
        }
        return false;
    }
}
