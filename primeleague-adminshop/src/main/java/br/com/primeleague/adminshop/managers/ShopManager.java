package br.com.primeleague.adminshop.managers;

import br.com.primeleague.adminshop.AdminShopPlugin;
import br.com.primeleague.adminshop.models.ShopCategory;
import br.com.primeleague.adminshop.models.ShopItem;
import br.com.primeleague.adminshop.models.ShopGUIHolder;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.managers.EconomyManager;
import br.com.primeleague.core.managers.IdentityManager;
import br.com.primeleague.core.managers.DonorManager;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.models.EconomyResponse;
import java.math.BigDecimal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gerenciador principal da loja administrativa.
 * 
 * Responsável por:
 * - Criar e gerenciar GUIs da loja
 * - Processar transações de compra
 * - Integrar com sistemas Core (Economia, Identidade, Doadores)
 * - Gerenciar entrega de itens e comandos
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopManager {

    private final AdminShopPlugin plugin;
    private final ShopConfigManager configManager;
    private final Logger logger;
    
    // Cache híbrido: apenas templates base (sem personalização)
    private final Map<String, Inventory> categoryTemplates;
    private Inventory mainMenuTemplate;

    /**
     * Construtor do gerenciador da loja.
     * 
     * @param plugin instância do plugin
     * @param configManager gerenciador de configuração
     */
    public ShopManager(AdminShopPlugin plugin, ShopConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logger = plugin.getPluginLogger();
        this.categoryTemplates = new HashMap<>();
    }

    /**
     * Abre o menu principal da loja para um jogador.
     * 
     * @param player jogador que abrirá a loja
     */
    public void openMainMenu(Player player) {
        if (mainMenuTemplate == null) {
            mainMenuTemplate = createMainMenuTemplate();
        }
        
        // Criar cópia personalizada do template
        Inventory personalizedMenu = createPersonalizedMainMenu(player);
        player.openInventory(personalizedMenu);
        logger.info("🏪 Jogador " + player.getName() + " abriu o menu principal da loja");
    }

    /**
     * Abre o menu de uma categoria específica para um jogador.
     * 
     * @param player jogador que abrirá a categoria
     * @param categoryId ID da categoria
     */
    public void openCategoryMenu(Player player, String categoryId) {
        ShopCategory category = configManager.getCategory(categoryId);
        if (category == null) {
            player.sendMessage(configManager.getSettings().getCategoryNotFound());
            return;
        }
        
        // Criar inventário personalizado para o jogador
        Inventory categoryInventory = createPersonalizedCategoryInventory(category, player);
        player.openInventory(categoryInventory);
        logger.info("📦 Jogador " + player.getName() + " abriu categoria: " + category.getName());
    }

    /**
     * Processa uma compra de item.
     * 
     * @param player jogador que está comprando
     * @param item item a ser comprado
     * @return true se a compra foi bem-sucedida
     */
    public boolean handlePurchase(Player player, ShopItem item) {
        try {
            // 1. Obter player_id via IdentityManager
            IdentityManager identityManager = PrimeLeagueAPI.getIdentityManager();
            Integer playerId = identityManager.getPlayerIdByUuid(player.getUniqueId());
            
            if (playerId == null) {
                player.sendMessage("§c❌ Erro: Jogador não encontrado no sistema");
                return false;
            }
            
            // 2. Obter PlayerProfile para donor_tier
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().getPlayerProfile(playerId);
            if (profile == null) {
                player.sendMessage("§c❌ Erro: Perfil do jogador não encontrado");
                return false;
            }
            
            // 3. Calcular desconto baseado no donor_tier (usando DonorManager como fonte única)
            DonorManager donorManager = PrimeLeagueAPI.getDonorManager();
            double discount = donorManager.getDiscountForTier(profile.getDonorTier());
            double finalPrice = item.getPrice() * (1.0 - discount);
            
            // 4. Verificar saldo
            EconomyManager economyManager = PrimeLeagueAPI.getEconomyManager();
            BigDecimal currentBalance = economyManager.getBalance(playerId);
            
            if (currentBalance.compareTo(BigDecimal.valueOf(finalPrice)) < 0) {
                String message = configManager.getSettings().getInsufficientFunds()
                    .replace("{balance}", String.format("$%.2f", currentBalance.doubleValue()));
                player.sendMessage(message);
                return false;
            }
            
            // 5. Processar transação
            EconomyResponse transactionResponse = economyManager.debitBalance(playerId, finalPrice, 
                "Compra na loja: " + item.getName());
            
            if (!transactionResponse.isSuccess()) {
                player.sendMessage(configManager.getSettings().getPurchaseFailed()
                    .replace("{reason}", "Falha na transação: " + transactionResponse.getErrorMessage()));
                return false;
            }
            
            // 6. Entregar item/comando/kit
            boolean deliverySuccess = deliverItem(player, item);
            
            if (!deliverySuccess) {
                // Reembolsar se a entrega falhou
                economyManager.creditBalance(playerId, finalPrice, 
                    "Reembolso - Falha na entrega: " + item.getName());
                player.sendMessage(configManager.getSettings().getPurchaseFailed()
                    .replace("{reason}", "Falha na entrega do item"));
                return false;
            }
            
            // 7. Mensagem de sucesso
            String successMessage = configManager.getSettings().getPurchaseSuccess();
            if (discount > 0) {
                successMessage += " §e(Desconto: " + String.format("%.1f%%", discount * 100) + ")";
            }
            player.sendMessage(successMessage);
            

            
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao processar compra para " + player.getName() + ": " + e.getMessage());
            player.sendMessage(configManager.getSettings().getPurchaseFailed()
                .replace("{reason}", "Erro interno"));
            return false;
        }
    }

    /**
     * Cria o template do menu principal da loja.
     * 
     * @return Inventory template do menu principal
     */
    private Inventory createMainMenuTemplate() {
        String title = configManager.getSettings().getTitle();
        int size = configManager.getSettings().getSize();
        
        Inventory inventory = Bukkit.createInventory(new ShopGUIHolder(), size, title);
        
        // Adicionar categorias ao menu (sem personalização)
        Map<String, ShopCategory> categories = configManager.getAllCategories();
        for (ShopCategory category : categories.values()) {
            if (category.getSlot() < size) {
                ItemStack categoryItem = createCategoryItem(category);
                inventory.setItem(category.getSlot(), categoryItem);
            }
        }
        
        return inventory;
    }

    /**
     * Cria o template de inventário de uma categoria.
     * 
     * @param category categoria
     * @return Inventory template da categoria
     */
    private Inventory createCategoryTemplate(ShopCategory category) {
        String title = category.getName();
        int size = configManager.getSettings().getSize();
        
        Inventory inventory = Bukkit.createInventory(new ShopGUIHolder(category.getId()), size, title);
        
        // Adicionar itens da categoria (sem personalização)
        List<ShopItem> items = category.getItems();
        int slot = 0;
        
        for (ShopItem item : items) {
            if (slot >= size) break;
            
            ItemStack displayItem = item.createItemStack();
            inventory.setItem(slot, displayItem);
            slot++;
        }
        
        return inventory;
    }

    /**
     * Cria um inventário personalizado do menu principal para um jogador.
     * 
     * @param player jogador
     * @return Inventory personalizado
     */
    private Inventory createPersonalizedMainMenu(Player player) {
        // Clonar o template
        Inventory personalizedMenu = Bukkit.createInventory(new ShopGUIHolder(), 
            mainMenuTemplate.getSize(), mainMenuTemplate.getTitle());
        
        // Copiar itens do template
        for (int i = 0; i < mainMenuTemplate.getSize(); i++) {
            ItemStack item = mainMenuTemplate.getItem(i);
            if (item != null) {
                personalizedMenu.setItem(i, item.clone());
            }
        }
        
        return personalizedMenu;
    }
    
    /**
     * Cria um inventário personalizado de categoria para um jogador.
     * 
     * @param category categoria
     * @param player jogador
     * @return Inventory personalizado
     */
    private Inventory createPersonalizedCategoryInventory(ShopCategory category, Player player) {
        // Obter ou criar template da categoria
        Inventory template = categoryTemplates.get(category.getId());
        if (template == null) {
            template = createCategoryTemplate(category);
            categoryTemplates.put(category.getId(), template);
        }
        
        // Criar inventário personalizado
        Inventory personalizedInventory = Bukkit.createInventory(new ShopGUIHolder(category.getId()), 
            template.getSize(), template.getTitle());
        
        // Copiar e personalizar itens
        for (int i = 0; i < template.getSize(); i++) {
            ItemStack templateItem = template.getItem(i);
            if (templateItem != null) {
                ItemStack personalizedItem = createShopItemDisplay(category.getItems().get(i), player);
                personalizedInventory.setItem(i, personalizedItem);
            }
        }
        
        return personalizedInventory;
    }

    /**
     * Cria o item de exibição de uma categoria.
     * 
     * @param category categoria
     * @return ItemStack do item de categoria
     */
    private ItemStack createCategoryItem(ShopCategory category) {
        ItemStack item = new ItemStack(category.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(category.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + category.getDescription());
        lore.add("");
        lore.add("§eClique para ver os itens!");
        lore.add("§7" + category.getItems().size() + " itens disponíveis");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

    /**
     * Cria o item de exibição de um item da loja.
     * 
     * @param item item da loja
     * @param player jogador (para cálculo de desconto)
     * @return ItemStack do item de exibição
     */
    private ItemStack createShopItemDisplay(ShopItem item, Player player) {
        // Obter desconto do jogador (usando DonorManager como fonte única)
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(player.getUniqueId());
        double discount = 0.0;
        
        if (playerId != null) {
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().getPlayerProfile(playerId);
            if (profile != null) {
                discount = PrimeLeagueAPI.getDonorManager().getDiscountForTier(profile.getDonorTier());
            }
        }
        
        double finalPrice = item.getPrice() * (1.0 - discount);
        
        // Criar ItemStack base
        ItemStack displayItem = item.createItemStack();
        ItemMeta meta = displayItem.getItemMeta();
        
        // Adicionar lore personalizada
        List<String> lore = new ArrayList<>();
        if (item.getLore() != null) {
            lore.addAll(item.getLore());
        }
        
        lore.add("");
        lore.add("§6Preço: $" + String.format("%.2f", item.getPrice()));
        
        if (discount > 0) {
            lore.add("§aDesconto: " + String.format("%.1f%%", discount * 100));
            lore.add("§ePreço Final: $" + String.format("%.2f", finalPrice));
        }
        
        lore.add("");
        lore.add("§eClique para comprar!");
        
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        
        return displayItem;
    }

    /**
     * Entrega um item/comando/kit ao jogador.
     * 
     * @param player jogador que receberá o item
     * @param item item a ser entregue
     * @return true se a entrega foi bem-sucedida
     */
    private boolean deliverItem(Player player, ShopItem item) {
        try {
            // Verificar tipo de item e entregar adequadamente
            if (item.isKitItem()) {
                return deliverKit(player, item);
            } else if (item.isCommandItem()) {
                return executeCommands(player, item);
            } else if (item.isPotionItem()) {
                return deliverPotion(player, item);
            } else {
                return deliverBasicItem(player, item);
            }
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao entregar item " + item.getName() + " para " + 
                player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Entrega um item básico ao jogador.
     * 
     * @param player jogador
     * @param item item básico
     * @return true se entregue com sucesso
     */
    private boolean deliverBasicItem(Player player, ShopItem item) {
        ItemStack itemStack = item.createItemStack();
        
        // Verificar se há espaço no inventário
        if (!hasInventorySpace(player, itemStack)) {
            player.sendMessage("§c❌ Seu inventário está cheio!");
            return false;
        }
        
        player.getInventory().addItem(itemStack);
        return true;
    }

    /**
     * Entrega um kit ao jogador.
     * 
     * @param player jogador
     * @param item kit
     * @return true se entregue com sucesso
     */
    private boolean deliverKit(Player player, ShopItem item) {
        List<ShopItem> kitItems = item.getKitItems();
        
        // Verificar espaço para todos os itens do kit
        for (ShopItem kitItem : kitItems) {
            ItemStack itemStack = kitItem.createItemStack();
            if (!hasInventorySpace(player, itemStack)) {
                player.sendMessage("§c❌ Seu inventário está cheio para receber o kit!");
                return false;
            }
        }
        
        // Entregar todos os itens do kit
        for (ShopItem kitItem : kitItems) {
            ItemStack itemStack = kitItem.createItemStack();
            player.getInventory().addItem(itemStack);
        }
        
        return true;
    }

    /**
     * Executa comandos para o jogador.
     * 
     * @param player jogador
     * @param item item de comando
     * @return true se executado com sucesso
     */
    private boolean executeCommands(Player player, ShopItem item) {
        List<String> commands = item.getCommands();
        
        for (String command : commands) {
            try {
                // Substituir placeholders
                String processedCommand = command.replace("{player}", player.getName());
                
                // Executar comando
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                
            } catch (Exception e) {
                logger.warning("⚠️ Erro ao executar comando '" + command + "' para " + 
                    player.getName() + ": " + e.getMessage());
            }
        }
        
        return true;
    }

    /**
     * Entrega uma poção ao jogador.
     * 
     * @param player jogador
     * @param item poção
     * @return true se entregue com sucesso
     */
    private boolean deliverPotion(Player player, ShopItem item) {
        // Para poções, aplicar efeitos diretamente no jogador
        if (item.getPotionEffects() != null) {
            for (PotionEffect effect : item.getPotionEffects()) {
                player.addPotionEffect(effect);
            }
        }
        
        return true;
    }

    /**
     * Verifica se o jogador tem espaço no inventário.
     * 
     * @param player jogador
     * @param itemStack item a ser verificado
     * @return true se há espaço
     */
    private boolean hasInventorySpace(Player player, ItemStack itemStack) {
        return player.getInventory().firstEmpty() != -1;
    }

    /**
     * Limpa o cache de templates.
     */
    public void clearCache() {
        categoryTemplates.clear();
        mainMenuTemplate = null;
        logger.info("🗑️ Cache de templates da loja limpo");
    }

    /**
     * Recarrega a configuração e limpa o cache.
     */
    public void reload() {
        if (configManager.reloadConfiguration()) {
            clearCache();
            logger.info("🔄 Loja recarregada com sucesso");
        } else {
            logger.severe("❌ Falha ao recarregar configuração da loja");
        }
    }
}
