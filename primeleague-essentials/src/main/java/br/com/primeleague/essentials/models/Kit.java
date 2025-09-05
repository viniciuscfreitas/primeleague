package br.com.primeleague.essentials.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de dados para um kit.
 * Representa um kit completo com itens, configurações e restrições.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class Kit {
    
    private String name;
    private String displayName;
    private String description;
    private String permission;
    private long cooldownSeconds;
    private int maxUses;
    private boolean requireEmptyInventory;
    private boolean allowDuringCombat;
    private List<KitItem> items;
    
    /**
     * Construtor para um kit.
     */
    public Kit(String name) {
        this.name = name;
        this.items = new ArrayList<KitItem>();
        this.cooldownSeconds = 3600; // 1 hora padrão
        this.maxUses = -1; // Ilimitado por padrão
        this.requireEmptyInventory = false;
        this.allowDuringCombat = false;
    }
    
    /**
     * Adiciona um item ao kit.
     */
    public void addItem(KitItem item) {
        this.items.add(item);
    }
    
    /**
     * Gera os ItemStacks do kit.
     */
    public List<ItemStack> generateItems() {
        List<ItemStack> itemStacks = new ArrayList<ItemStack>();
        
        for (KitItem kitItem : items) {
            ItemStack itemStack = kitItem.toItemStack();
            if (itemStack != null) {
                itemStacks.add(itemStack);
            }
        }
        
        return itemStacks;
    }
    
    /**
     * Verifica se o kit tem itens.
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }
    
    /**
     * Obtém o número de itens no kit.
     */
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
    
    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }
    
    public long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    
    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }
    
    public boolean isRequireEmptyInventory() { return requireEmptyInventory; }
    public void setRequireEmptyInventory(boolean requireEmptyInventory) { this.requireEmptyInventory = requireEmptyInventory; }
    
    public boolean isAllowDuringCombat() { return allowDuringCombat; }
    public void setAllowDuringCombat(boolean allowDuringCombat) { this.allowDuringCombat = allowDuringCombat; }
    
    public List<KitItem> getItems() { return items; }
    public void setItems(List<KitItem> items) { this.items = items; }
    
    @Override
    public String toString() {
        return "Kit{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", permission='" + permission + '\'' +
                ", cooldownSeconds=" + cooldownSeconds +
                ", maxUses=" + maxUses +
                ", requireEmptyInventory=" + requireEmptyInventory +
                ", allowDuringCombat=" + allowDuringCombat +
                ", itemCount=" + getItemCount() +
                '}';
    }
    
    /**
     * Classe interna para representar um item do kit.
     */
    public static class KitItem {
        private String material;
        private int amount;
        private String name;
        private List<String> lore;
        private Map<String, Integer> enchantments;
        
        /**
         * Construtor para um item do kit.
         */
        public KitItem(String material, int amount) {
            this.material = material;
            this.amount = amount;
            this.lore = new ArrayList<String>();
            this.enchantments = new HashMap<String, Integer>();
        }
        
        /**
         * Converte para ItemStack do Bukkit.
         */
        public ItemStack toItemStack() {
            try {
                Material mat = Material.valueOf(material.toUpperCase());
                ItemStack itemStack = new ItemStack(mat, amount);
                
                ItemMeta meta = itemStack.getItemMeta();
                if (meta != null) {
                    // Definir nome personalizado
                    if (name != null && !name.isEmpty()) {
                        meta.setDisplayName(name);
                    }
                    
                    // Definir lore
                    if (lore != null && !lore.isEmpty()) {
                        meta.setLore(lore);
                    }
                    
                    itemStack.setItemMeta(meta);
                }
                
                // Aplicar encantamentos
                if (enchantments != null && !enchantments.isEmpty()) {
                    for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                        try {
                            Enchantment enchant = Enchantment.getByName(entry.getKey().toUpperCase());
                            if (enchant != null) {
                                itemStack.addUnsafeEnchantment(enchant, entry.getValue());
                            }
                        } catch (Exception e) {
                            // Ignorar encantamentos inválidos
                        }
                    }
                }
                
                return itemStack;
                
            } catch (Exception e) {
                // Retornar null para materiais inválidos
                return null;
            }
        }
        
        // Getters e Setters
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        
        public int getAmount() { return amount; }
        public void setAmount(int amount) { this.amount = amount; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public List<String> getLore() { return lore; }
        public void setLore(List<String> lore) { this.lore = lore; }
        
        public Map<String, Integer> getEnchantments() { return enchantments; }
        public void setEnchantments(Map<String, Integer> enchantments) { this.enchantments = enchantments; }
    }
}
