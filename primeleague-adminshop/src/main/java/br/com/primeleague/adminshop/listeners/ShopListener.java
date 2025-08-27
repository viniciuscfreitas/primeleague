package br.com.primeleague.adminshop.listeners;

import br.com.primeleague.adminshop.AdminShopPlugin;
import br.com.primeleague.adminshop.managers.ShopManager;
import br.com.primeleague.adminshop.models.ShopCategory;
import br.com.primeleague.adminshop.models.ShopItem;
import br.com.primeleague.adminshop.models.ShopGUIHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Listener para eventos de inventário da loja.
 * 
 * Responsável por:
 * - Capturar cliques na GUI da loja
 * - Identificar itens clicados
 * - Processar compras
 * - Prevenir interações indesejadas
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopListener implements Listener {

    private final AdminShopPlugin plugin;
    private final ShopManager shopManager;
    private final Logger logger;
    
    // Cache de cooldown para prevenir spam de cliques
    private final Map<String, Long> clickCooldowns;
    private final long clickCooldownMs; // Cooldown configurável

    /**
     * Construtor do listener da loja.
     * 
     * @param plugin instância do plugin
     * @param shopManager gerenciador da loja
     */
    public ShopListener(AdminShopPlugin plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.logger = plugin.getPluginLogger();
        this.clickCooldowns = new HashMap<>();
        // REFATORADO: Carregar cooldown da configuração
        this.clickCooldownMs = plugin.getConfigManager().getSettings().getClickCooldownMs();
    }

    /**
     * Captura cliques no inventário da loja.
     * 
     * @param event evento de clique no inventário
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        ItemStack clickedItem = event.getCurrentItem();
        
        // Verificar se é um inventário da loja
        if (!isShopInventory(inventory)) {
            return;
        }
        
        // Cancelar evento para prevenir movimentação de itens
        event.setCancelled(true);
        
        // Verificar cooldown
        if (isOnCooldown(player.getName())) {
            return;
        }
        
        // Verificar se clicou em um item válido
        if (clickedItem == null || clickedItem.getType().name().equals("AIR")) {
            return;
        }
        
        // Processar clique
        handleShopClick(player, inventory, clickedItem, event.getSlot());
    }

    /**
     * Captura fechamento de inventário da loja.
     * 
     * @param event evento de fechamento do inventário
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        
        // Verificar se é um inventário da loja
        if (isShopInventory(inventory)) {
            logger.info("🏪 Jogador " + player.getName() + " fechou a loja");
        }
    }

    /**
     * Verifica se um inventário pertence à loja.
     * 
     * @param inventory inventário a ser verificado
     * @return true se é um inventário da loja
     */
    private boolean isShopInventory(Inventory inventory) {
        // Verificação robusta usando InventoryHolder
        return inventory.getHolder() instanceof ShopGUIHolder;
    }

    /**
     * Processa um clique na loja.
     * 
     * @param player jogador que clicou
     * @param inventory inventário clicado
     * @param clickedItem item clicado
     * @param slot slot clicado
     */
    private void handleShopClick(Player player, Inventory inventory, ItemStack clickedItem, int slot) {
        ShopGUIHolder holder = (ShopGUIHolder) inventory.getHolder();
        
        // Verificar se é o menu principal
        if (holder.isMainMenu()) {
            handleMainMenuClick(player, clickedItem, slot);
        } else {
            // É um menu de categoria
            handleCategoryMenuClick(player, holder.getCategoryId(), clickedItem, slot);
        }
        
        // Aplicar cooldown
        setCooldown(player.getName());
    }

    /**
     * Processa clique no menu principal.
     * 
     * @param player jogador
     * @param clickedItem item clicado
     * @param slot slot clicado
     */
    private void handleMainMenuClick(Player player, ItemStack clickedItem, int slot) {
        // Identificar categoria baseada no slot
        String categoryId = getCategoryIdBySlot(slot);
        
        if (categoryId != null) {
            shopManager.openCategoryMenu(player, categoryId);
        } else {
            player.sendMessage("§c❌ Categoria não encontrada!");
        }
    }

    /**
     * Processa clique no menu de categoria.
     * 
     * @param player jogador
     * @param categoryId ID da categoria
     * @param clickedItem item clicado
     * @param slot slot clicado
     */
    private void handleCategoryMenuClick(Player player, String categoryId, ItemStack clickedItem, int slot) {
        // Obter item da categoria
        ShopItem item = getItemFromCategory(categoryId, slot);
        
        if (item != null) {
            // Processar compra
            shopManager.handlePurchase(player, item);
        } else {
            player.sendMessage("§c❌ Item não encontrado!");
        }
    }

    /**
     * Obtém o ID da categoria baseado no slot.
     * 
     * @param slot slot clicado
     * @return ID da categoria ou null se não encontrada
     */
    private String getCategoryIdBySlot(int slot) {
        // Mapeamento baseado na configuração do shop.yml
        switch (slot) {
            case 0: return "basic_items";
            case 1: return "potions";
            case 2: return "special_blocks";
            case 3: return "vip_commands";
            case 4: return "special_kits";
            default: return null;
        }
    }



    /**
     * Obtém um item de uma categoria baseado no slot.
     * 
     * @param categoryId ID da categoria
     * @param slot slot do item
     * @return ShopItem ou null se não encontrado
     */
    private ShopItem getItemFromCategory(String categoryId, int slot) {
        ShopCategory category = plugin.getConfigManager().getCategory(categoryId);
        
        if (category == null) {
            return null;
        }
        
        // Obter item baseado no slot (índice na lista)
        if (slot >= 0 && slot < category.getItems().size()) {
            return category.getItems().get(slot);
        }
        
        return null;
    }

    /**
     * Verifica se um jogador está em cooldown.
     * 
     * @param playerName nome do jogador
     * @return true se está em cooldown
     */
    private boolean isOnCooldown(String playerName) {
        Long lastClick = clickCooldowns.get(playerName);
        if (lastClick == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastClick) < clickCooldownMs;
    }

    /**
     * Define cooldown para um jogador.
     * 
     * @param playerName nome do jogador
     */
    private void setCooldown(String playerName) {
        clickCooldowns.put(playerName, System.currentTimeMillis());
    }

    /**
     * Limpa o cache de cooldowns.
     */
    public void clearCooldowns() {
        clickCooldowns.clear();
    }
}
