package br.com.primeleague.admin.listeners;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Punishment;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener para verificar punições de chat (mutes).
 */
public class ChatListener implements Listener {
    
    private final AdminManager adminManager;
    
    public ChatListener(AdminManager adminManager) {
        this.adminManager = adminManager;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // Verificar se o jogador tem permissão de bypass
        if (PrimeLeagueAPI.hasPermission(player, "primeleague.admin.mute.bypass")) {
            return;
        }
        
        // Verificar se o jogador está silenciado
        Punishment mute = adminManager.getActivePunishment(player.getUniqueId(), Punishment.Type.MUTE);
        if (mute != null && mute.isCurrentlyActive()) {
            // Cancelar o evento de chat
            event.setCancelled(true);
            
            // Enviar mensagem de mute
            String message = ChatColor.RED + "Você está silenciado!";
            if (mute.isPermanent()) {
                message += " Motivo: " + mute.getReason();
            } else {
                long remainingTime = mute.getExpiresAt().getTime() - System.currentTimeMillis();
                long hours = remainingTime / (1000 * 60 * 60);
                long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);
                
                message += " Motivo: " + mute.getReason() + 
                          " | Tempo restante: " + hours + "h " + minutes + "m";
            }
            
            player.sendMessage(message);
        }
    }
}
