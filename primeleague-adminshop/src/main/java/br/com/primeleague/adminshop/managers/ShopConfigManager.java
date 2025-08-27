package br.com.primeleague.adminshop.managers;

import br.com.primeleague.adminshop.AdminShopPlugin;
import br.com.primeleague.adminshop.models.ShopCategory;
import br.com.primeleague.adminshop.models.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Gerenciador de configuração da loja administrativa.
 * 
 * Responsável por:
 * - Carregar o arquivo shop.yml
 * - Validar a estrutura da configuração
 * - Fornecer acesso aos dados da loja
 * - Gerenciar recarregamento de configuração
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopConfigManager {

    private final AdminShopPlugin plugin;
    private final Logger logger;
    private File configFile;
    private FileConfiguration config;
    
    // Cache dos dados carregados
    private Map<String, ShopCategory> categories;
    private Map<String, ShopItem> allItems;
    private ShopSettings settings;

    /**
     * Construtor do gerenciador de configuração.
     * 
     * @param plugin instância do plugin
     */
    public ShopConfigManager(AdminShopPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getPluginLogger();
        this.categories = new HashMap<>();
        this.allItems = new HashMap<>();
    }

    /**
     * Carrega a configuração da loja.
     * 
     * @return true se o carregamento foi bem-sucedido
     */
    public boolean loadConfiguration() {
        logger.info("📁 Carregando configuração da loja...");
        
        try {
            // Criar arquivo de configuração se não existir
            if (!createConfigFile()) {
                return false;
            }
            
            // Carregar configuração
            config = YamlConfiguration.loadConfiguration(configFile);
            
            // Validar estrutura básica
            if (!validateBasicStructure()) {
                return false;
            }
            
            // Carregar configurações gerais
            if (!loadSettings()) {
                return false;
            }
            
            // Carregar categorias e itens
            if (!loadCategories()) {
                return false;
            }
            

            
            logger.info("✅ Configuração carregada com sucesso!");
            logger.info("📊 Resumo: " + categories.size() + " categorias, " + allItems.size() + " itens");
            
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar configuração: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recarrega a configuração da loja.
     * 
     * @return true se o reload foi bem-sucedido
     */
    public boolean reloadConfiguration() {
        logger.info("🔄 Recarregando configuração da loja...");
        
        // Limpar caches
        categories.clear();
        allItems.clear();
        
        return loadConfiguration();
    }

    /**
     * Cria o arquivo de configuração se não existir.
     * 
     * @return true se o arquivo foi criado ou já existe
     */
    private boolean createConfigFile() {
        configFile = new File(plugin.getDataFolder(), "shop.yml");
        
        if (!configFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                plugin.saveResource("shop.yml", false);
                logger.info("📄 Arquivo shop.yml criado com sucesso!");
                
                // Criar arquivos de categoria se não existirem
                createCategoryFiles();
                
            } catch (Exception e) {
                logger.severe("❌ Erro ao criar arquivo shop.yml: " + e.getMessage());
                return false;
            }
        }
        
        return true;
    }

    /**
     * Valida a estrutura básica da configuração.
     * 
     * @return true se a estrutura é válida
     */
    private boolean validateBasicStructure() {
        if (!config.contains("settings")) {
            logger.severe("❌ Seção 'settings' não encontrada!");
            return false;
        }
        
        if (!config.contains("categories")) {
            logger.severe("❌ Seção 'categories' não encontrada!");
            return false;
        }
        

        
        return true;
    }

    /**
     * Carrega as configurações gerais da loja.
     * 
     * @return true se o carregamento foi bem-sucedido
     */
    private boolean loadSettings() {
        try {
            ConfigurationSection settingsSection = config.getConfigurationSection("settings");
            
            settings = new ShopSettings();
            settings.setTitle(settingsSection.getString("title", "§6§l🏪 Loja Administrativa"));
            settings.setSize(settingsSection.getInt("size", 54));
                    settings.setShowDiscountedPrices(settingsSection.getBoolean("show_discounted_prices", true));
        settings.setConfirmPurchases(settingsSection.getBoolean("confirm_purchases", true));
        settings.setClickCooldownMs(settingsSection.getLong("click_cooldown_ms", 500L));
            
            // Carregar mensagens
            ConfigurationSection messagesSection = settingsSection.getConfigurationSection("messages");
            if (messagesSection != null) {
                settings.setPurchaseSuccess(messagesSection.getString("purchase_success", "§a✅ Compra realizada com sucesso!"));
                settings.setPurchaseFailed(messagesSection.getString("purchase_failed", "§c❌ Falha na compra: {reason}"));
                settings.setInsufficientFunds(messagesSection.getString("insufficient_funds", "§c❌ Saldo insuficiente. Você tem: {balance}"));
                settings.setItemNotFound(messagesSection.getString("item_not_found", "§c❌ Item não encontrado na loja"));
                settings.setCategoryNotFound(messagesSection.getString("category_not_found", "§c❌ Categoria não encontrada"));
            }
            
            logger.info("✅ Configurações gerais carregadas");
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar configurações gerais: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carrega as categorias e itens da loja.
     * 
     * @return true se o carregamento foi bem-sucedido
     */
    private boolean loadCategories() {
        try {
            ConfigurationSection categoriesSection = config.getConfigurationSection("categories");
            
            for (String categoryId : categoriesSection.getKeys(false)) {
                ConfigurationSection categorySection = categoriesSection.getConfigurationSection(categoryId);
                
                if (categorySection == null) {
                    logger.warning("⚠️ Categoria '" + categoryId + "' tem configuração inválida, pulando...");
                    continue;
                }
                
                ShopCategory category = loadCategory(categoryId, categorySection);
                if (category != null) {
                    categories.put(categoryId, category);
                    logger.info("📦 Categoria carregada: " + category.getName() + " (" + category.getItems().size() + " itens)");
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar categorias: " + e.getMessage());
            return false;
        }
    }

    /**
     * Carrega uma categoria específica.
     * 
     * @param categoryId ID da categoria
     * @param categorySection seção da configuração
     * @return ShopCategory carregada ou null se houver erro
     */
    private ShopCategory loadCategory(String categoryId, ConfigurationSection categorySection) {
        try {
            String name = categorySection.getString("name", "Categoria");
            String description = categorySection.getString("description", "");
            String iconMaterial = categorySection.getString("icon", "STONE");
            int slot = categorySection.getInt("slot", 0);
            String filePath = categorySection.getString("file", null);
            
            // Validar material do ícone
            Material icon = Material.getMaterial(iconMaterial);
            if (icon == null) {
                logger.warning("⚠️ Material inválido para ícone da categoria '" + categoryId + "': " + iconMaterial);
                icon = Material.STONE;
            }
            
            ShopCategory category = new ShopCategory(categoryId, name, description, icon, slot);
            
            // Carregar itens da categoria (design modular)
            if (filePath != null) {
                if (!loadCategoryItemsFromFile(category, filePath)) {
                    logger.warning("⚠️ Falha ao carregar arquivo de categoria '" + filePath + "' para '" + categoryId + "'");
                }
            } else {
                // Fallback: carregar itens diretamente da seção (compatibilidade)
                List<Map<?, ?>> itemsList = categorySection.getMapList("items");
                for (Map<?, ?> itemMap : itemsList) {
                    ShopItem item = loadShopItem(itemMap);
                    if (item != null) {
                        category.addItem(item);
                        allItems.put(item.getId(), item);
                    }
                }
            }
            
            return category;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar categoria '" + categoryId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Carrega um item da loja.
     * 
     * @param itemMap mapa com dados do item
     * @return ShopItem carregado ou null se houver erro
     */
    private ShopItem loadShopItem(Map<?, ?> itemMap) {
        try {
            String id = (String) itemMap.get("id");
            String name = (String) itemMap.get("name");
            String description = (String) itemMap.get("description");
            String materialName = (String) itemMap.get("material");
            double price = ((Number) itemMap.get("price")).doubleValue();
            
            // Validar material
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                logger.warning("⚠️ Material inválido para item '" + id + "': " + materialName);
                return null;
            }
            
            ShopItem item = new ShopItem(id, name, description, material, price);
            
            // Carregar quantidade
            if (itemMap.containsKey("amount")) {
                item.setAmount(((Number) itemMap.get("amount")).intValue());
            }
            
            // Carregar lore
            if (itemMap.containsKey("lore")) {
                List<String> lore = (List<String>) itemMap.get("lore");
                item.setLore(lore);
            }
            
            // Carregar encantamentos
            if (itemMap.containsKey("enchantments")) {
                Map<String, Object> enchantMap = (Map<String, Object>) itemMap.get("enchantments");
                for (Map.Entry<String, Object> entry : enchantMap.entrySet()) {
                    Enchantment enchant = Enchantment.getByName(entry.getKey());
                    if (enchant != null) {
                        int level = ((Number) entry.getValue()).intValue();
                        item.addEnchantment(enchant, level);
                    }
                }
            }
            
            // Carregar efeitos de poção
            if (itemMap.containsKey("potion_effects")) {
                Map<String, Object> effectsMap = (Map<String, Object>) itemMap.get("potion_effects");
                for (Map.Entry<String, Object> entry : effectsMap.entrySet()) {
                    PotionEffectType effectType = PotionEffectType.getByName(entry.getKey());
                    if (effectType != null) {
                        String[] parts = entry.getValue().toString().split(":");
                        int level = Integer.parseInt(parts[0]);
                        int duration = Integer.parseInt(parts[1]);
                        item.addPotionEffect(new PotionEffect(effectType, duration * 20, level - 1));
                    }
                }
            }
            
            // Carregar comandos
            if (itemMap.containsKey("commands")) {
                List<String> commands = (List<String>) itemMap.get("commands");
                item.setCommands(commands);
            }
            
            // Carregar itens do kit
            if (itemMap.containsKey("kit_items")) {
                List<Map<?, ?>> kitItemsList = (List<Map<?, ?>>) itemMap.get("kit_items");
                List<ShopItem> kitItems = new ArrayList<>();
                
                for (Map<?, ?> kitItemMap : kitItemsList) {
                    ShopItem kitItem = loadShopItem(kitItemMap);
                    if (kitItem != null) {
                        kitItems.add(kitItem);
                    }
                }
                
                item.setKitItems(kitItems);
            }
            
            return item;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria os arquivos de categoria se não existirem.
     */
    private void createCategoryFiles() {
        try {
            // Criar diretório categories se não existir
            File categoriesDir = new File(plugin.getDataFolder(), "categories");
            if (!categoriesDir.exists()) {
                categoriesDir.mkdirs();
            }
            
            // Lista de arquivos de categoria para criar
            String[] categoryFiles = {
                "categories/basic_items.yml",
                "categories/potions.yml", 
                "categories/special_blocks.yml",
                "categories/vip_commands.yml",
                "categories/special_kits.yml"
            };
            
            for (String categoryFile : categoryFiles) {
                File file = new File(plugin.getDataFolder(), categoryFile);
                if (!file.exists()) {
                    plugin.saveResource(categoryFile, false);
                    logger.info("📄 Arquivo de categoria criado: " + categoryFile);
                }
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao criar arquivos de categoria: " + e.getMessage());
        }
    }

    /**
     * Carrega itens de uma categoria a partir de um arquivo separado.
     * 
     * @param category categoria para adicionar os itens
     * @param filePath caminho do arquivo de categoria
     * @return true se o carregamento foi bem-sucedido
     */
    private boolean loadCategoryItemsFromFile(ShopCategory category, String filePath) {
        try {
            File categoryFile = new File(plugin.getDataFolder(), filePath);
            
            if (!categoryFile.exists()) {
                logger.warning("⚠️ Arquivo de categoria não encontrado: " + filePath);
                return false;
            }
            
            FileConfiguration categoryConfig = YamlConfiguration.loadConfiguration(categoryFile);
            List<Map<?, ?>> itemsList = categoryConfig.getMapList("items");
            
            int loadedItems = 0;
            for (Map<?, ?> itemMap : itemsList) {
                ShopItem item = loadShopItem(itemMap);
                if (item != null) {
                    category.addItem(item);
                    allItems.put(item.getId(), item);
                    loadedItems++;
                }
            }
            
            logger.info("📁 Carregados " + loadedItems + " itens do arquivo: " + filePath);
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar arquivo de categoria '" + filePath + "': " + e.getMessage());
            return false;
        }
    }



    // ==================== GETTERS ====================

    /**
     * Retorna uma categoria pelo ID.
     * 
     * @param categoryId ID da categoria
     * @return ShopCategory ou null se não encontrada
     */
    public ShopCategory getCategory(String categoryId) {
        return categories.get(categoryId);
    }

    /**
     * Retorna todas as categorias.
     * 
     * @return Map com todas as categorias
     */
    public Map<String, ShopCategory> getAllCategories() {
        return new HashMap<>(categories);
    }

    /**
     * Retorna um item pelo ID.
     * 
     * @param itemId ID do item
     * @return ShopItem ou null se não encontrado
     */
    public ShopItem getItem(String itemId) {
        return allItems.get(itemId);
    }

    /**
     * Retorna todos os itens.
     * 
     * @return Map com todos os itens
     */
    public Map<String, ShopItem> getAllItems() {
        return new HashMap<>(allItems);
    }



    /**
     * Retorna as configurações da loja.
     * 
     * @return ShopSettings
     */
    public ShopSettings getSettings() {
        return settings;
    }

    /**
     * Retorna o número de categorias.
     * 
     * @return número de categorias
     */
    public int getCategoryCount() {
        return categories.size();
    }

    /**
     * Retorna o número total de itens.
     * 
     * @return número total de itens
     */
    public int getTotalItemCount() {
        return allItems.size();
    }

    /**
     * Classe interna para armazenar configurações da loja.
     */
    public static class ShopSettings {
        private String title;
        private int size;
        private boolean showDiscountedPrices;
        private boolean confirmPurchases;
        private long clickCooldownMs;
        private String purchaseSuccess;
        private String purchaseFailed;
        private String insufficientFunds;
        private String itemNotFound;
        private String categoryNotFound;

        // Getters e Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        
        public boolean isShowDiscountedPrices() { return showDiscountedPrices; }
        public void setShowDiscountedPrices(boolean showDiscountedPrices) { this.showDiscountedPrices = showDiscountedPrices; }
        
        public boolean isConfirmPurchases() { return confirmPurchases; }
        public void setConfirmPurchases(boolean confirmPurchases) { this.confirmPurchases = confirmPurchases; }
        
        public long getClickCooldownMs() { return clickCooldownMs; }
        public void setClickCooldownMs(long clickCooldownMs) { this.clickCooldownMs = clickCooldownMs; }
        
        public String getPurchaseSuccess() { return purchaseSuccess; }
        public void setPurchaseSuccess(String purchaseSuccess) { this.purchaseSuccess = purchaseSuccess; }
        
        public String getPurchaseFailed() { return purchaseFailed; }
        public void setPurchaseFailed(String purchaseFailed) { this.purchaseFailed = purchaseFailed; }
        
        public String getInsufficientFunds() { return insufficientFunds; }
        public void setInsufficientFunds(String insufficientFunds) { this.insufficientFunds = insufficientFunds; }
        
        public String getItemNotFound() { return itemNotFound; }
        public void setItemNotFound(String itemNotFound) { this.itemNotFound = itemNotFound; }
        
        public String getCategoryNotFound() { return categoryNotFound; }
        public void setCategoryNotFound(String categoryNotFound) { this.categoryNotFound = categoryNotFound; }
    }
}
