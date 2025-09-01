package br.com.primeleague.chat.listeners;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.gui.IgnoreGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

/**
 * Listener para gerenciar interações com a GUI de ignore.
 * Implementa o princípio "Interface Gráfica para Gestão".
 */
public class InventoryListener implements Listener {
    
    private final PrimeLeagueChat plugin;
    private final IgnoreGUI ignoreGUI;
    private static final String GUI_TITLE = "§8Gerenciar Ignores";
    
    public InventoryListener(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.ignoreGUI = new IgnoreGUI(plugin);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        // Verificar se é a GUI de ignore
        if (!isIgnoreGUI(event.getView().getTitle())) {
            return;
        }
        
        // Cancelar o evento para evitar que o jogador mova itens
        event.setCancelled(true);
        
        // Verificar se é um jogador
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Verificar se clicou em um item válido
        if (event.getCurrentItem() == null) {
            return;
        }
        
        // Processar o clique
        int slot = event.getRawSlot();
        boolean isRightClick = event.isRightClick();
        
        ignoreGUI.handleClick(player, slot, isRightClick);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        // Verificar se é a GUI de ignore
        if (!isIgnoreGUI(event.getView().getTitle())) {
            return;
        }
        
        // Verificar se é um jogador
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        
        // Log para debugging (opcional)
        plugin.getLogger().info("🔇 [GUI] Jogador " + player.getName() + " fechou a GUI de ignore.");
    }
    
    /**
     * Verifica se o inventário é a GUI de ignore.
     */
    private boolean isIgnoreGUI(String title) {
        return GUI_TITLE.equals(title);
    }
    
    /**
     * Obtém a instância da GUI de ignore.
     */
    public IgnoreGUI getIgnoreGUI() {
        return ignoreGUI;
    }
}
