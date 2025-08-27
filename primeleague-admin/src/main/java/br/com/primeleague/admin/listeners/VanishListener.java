package br.com.primeleague.admin.listeners;

import br.com.primeleague.admin.managers.AdminManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener para gerenciar modo vanish
 */
public class VanishListener implements Listener {
    private final AdminManager adminManager;

    public VanishListener(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Implementação do vanish será adicionada aqui
    }
}
