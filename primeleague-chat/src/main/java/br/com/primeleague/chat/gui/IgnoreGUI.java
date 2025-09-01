package br.com.primeleague.chat.gui;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelIgnoreService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Interface gráfica para gerenciar ignores de canais e jogadores.
 * Implementa o princípio "Interface Gráfica para Gestão" - fim dos comandos verbosos.
 */
public class IgnoreGUI {
    
    private final PrimeLeagueChat plugin;
    private final ChannelIgnoreService ignoreService;
    private static final String GUI_TITLE = "§8Gerenciar Ignores";
    private static final int GUI_SIZE = 27; // 3 linhas - mais simples
    
    public IgnoreGUI(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.ignoreService = plugin.getIgnoreService();
    }
    
    /**
     * Abre a GUI de ignore para o jogador.
     */
    public void openGUI(Player player) {
        Inventory inventory = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);
        
        // Preencher apenas os canais (simples e minimalista)
        populateChannelsRow(inventory, player);
        
        // Abrir para o jogador
        player.openInventory(inventory);
    }
    
    /**
     * Preenche a linha superior com os canais (Global, Clã, Aliança, Local).
     */
    private void populateChannelsRow(Inventory inventory, Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Canal Global (slot 1)
        boolean ignoringGlobal = ignoreService.isIgnoringChannel(player, ChannelManager.ChatChannel.GLOBAL);
        ItemStack globalItem = createChannelItem(Material.WOOL, ignoringGlobal, "Global", "§aGlobal");
        inventory.setItem(1, globalItem);
        
        // Canal Clã (slot 3)
        boolean ignoringClan = ignoreService.isIgnoringChannel(player, ChannelManager.ChatChannel.CLAN);
        ItemStack clanItem = createChannelItem(Material.WOOL, ignoringClan, "Clã", "§bClã");
        inventory.setItem(3, clanItem);
        
        // Canal Aliança (slot 5)
        boolean ignoringAlly = ignoreService.isIgnoringChannel(player, ChannelManager.ChatChannel.ALLY);
        ItemStack allyItem = createChannelItem(Material.WOOL, ignoringAlly, "Aliança", "§dAliança");
        inventory.setItem(5, allyItem);
        
        // Canal Local (slot 7)
        boolean ignoringLocal = ignoreService.isIgnoringChannel(player, ChannelManager.ChatChannel.LOCAL);
        ItemStack localItem = createChannelItem(Material.WOOL, ignoringLocal, "Local", "§eLocal");
        inventory.setItem(7, localItem);
    }
    
    /**
     * Cria um item de canal com status visual.
     */
    private ItemStack createChannelItem(Material material, boolean isIgnoring, String channelName, String displayName) {
        ItemStack item = new ItemStack(material, 1);
        
        // Definir cor baseada no status - LÃ VERDE (13) e LÃ VERMELHA (14)
        if (isIgnoring) {
            item.setDurability((short) 14); // Lã vermelha
        } else {
            item.setDurability((short) 13); // Lã verde
        }
        
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        
        List<String> lore = new ArrayList<String>();
        if (isIgnoring) {
            lore.add("§7Status: §cIgnorado");
            lore.add("§eClique para ativar este canal.");
        } else {
            lore.add("§7Status: §aAtivo");
            lore.add("§eClique para ignorar este canal.");
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
    

    
    /**
     * Processa um clique na GUI.
     */
    public void handleClick(Player player, int slot, boolean isRightClick) {
        UUID playerUuid = player.getUniqueId();
        
        // Canais (linha superior) - apenas isso, super simples
        if (slot >= 1 && slot <= 7 && slot % 2 == 1) {
            handleChannelClick(player, slot);
            return;
        }
    }
    
    /**
     * Processa clique em um canal.
     */
    private void handleChannelClick(Player player, int slot) {
        UUID playerUuid = player.getUniqueId();
        ChannelManager.ChatChannel channel = getChannelFromSlot(slot);
        
        if (channel == null) return;
        
        boolean isIgnoring = ignoreService.isIgnoringChannel(player, channel);
        
        if (isIgnoring) {
            // Remover ignore
            ignoreService.unignoreChannel(player, channel);
            player.sendMessage("§a✅ Canal " + getChannelDisplayName(channel) + " ativado novamente.");
        } else {
            // Adicionar ignore
            ignoreService.ignoreChannel(player, channel);
            player.sendMessage("§c🔇 Canal " + getChannelDisplayName(channel) + " ignorado.");
        }
        
        // Atualizar a GUI
        openGUI(player);
    }
    

    
    /**
     * Obtém o canal baseado no slot.
     */
    private ChannelManager.ChatChannel getChannelFromSlot(int slot) {
        switch (slot) {
            case 1: return ChannelManager.ChatChannel.GLOBAL;
            case 3: return ChannelManager.ChatChannel.CLAN;
            case 5: return ChannelManager.ChatChannel.ALLY;
            case 7: return ChannelManager.ChatChannel.LOCAL;
            default: return null;
        }
    }
    
    /**
     * Obtém o nome de exibição do canal.
     */
    private String getChannelDisplayName(ChannelManager.ChatChannel channel) {
        switch (channel) {
            case GLOBAL: return "§aGlobal";
            case CLAN: return "§bClã";
            case ALLY: return "§dAliança";
            case LOCAL: return "§eLocal";
            default: return "§7Desconhecido";
        }
    }
    

}
