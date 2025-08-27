package br.com.primeleague.admin.listeners;

import br.com.primeleague.admin.managers.AdminManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener para carregar estado de vanish no join
 */
public class JoinListener implements Listener {
    private final AdminManager adminManager;

    public JoinListener(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carregar estado de vanish do banco de dados
        // Em Bukkit 1.5.2, usar UUID offline baseado no nome
        java.util.UUID playerUuid;
        try {
            playerUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + event.getPlayer().getName()).getBytes("UTF-8"));
            adminManager.loadVanishState(playerUuid);
        } catch (java.io.UnsupportedEncodingException e) {
            // Se não conseguir gerar UUID, ignorar
        }
    }
}
