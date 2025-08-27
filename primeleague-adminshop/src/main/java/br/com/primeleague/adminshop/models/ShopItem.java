package br.com.primeleague.adminshop.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo para representar um item da loja administrativa.
 * 
 * Um item contém:
 * - ID único
 * - Nome e descrição
 * - Material e quantidade
 * - Preço
 * - Lore personalizada
 * - Encantamentos
 * - Efeitos de poção
 * - Comandos a executar
 * - Itens do kit (para kits)
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopItem {

    private final String id;
    private final String name;
    private final String description;
    private final Material material;
    private final double price;
    
    private int amount;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;
    private List<PotionEffect> potionEffects;
    private List<String> commands;
    private List<ShopItem> kitItems;

    /**
     * Construtor do item.
     * 
     * @param id ID único do item
     * @param name nome do item
     * @param description descrição do item
     * @param material material do item
     * @param price preço do item
     */
    public ShopItem(String id, String name, String description, Material material, double price) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.material = material;
        this.price = price;
        
        this.amount = 1;
        this.lore = new ArrayList<>();
        this.enchantments = new HashMap<>();
        this.potionEffects = new ArrayList<>();
        this.commands = new ArrayList<>();
        this.kitItems = new ArrayList<>();
    }

    /**
     * Adiciona um encantamento ao item.
     * 
     * @param enchantment encantamento
     * @param level nível do encantamento
     */
    public void addEnchantment(Enchantment enchantment, int level) {
        if (enchantment != null && level > 0) {
            enchantments.put(enchantment, level);
        }
    }

    /**
     * Remove um encantamento do item.
     * 
     * @param enchantment encantamento a ser removido
     */
    public void removeEnchantment(Enchantment enchantment) {
        enchantments.remove(enchantment);
    }

    /**
     * Adiciona um efeito de poção ao item.
     * 
     * @param effect efeito de poção
     */
    public void addPotionEffect(PotionEffect effect) {
        if (effect != null) {
            potionEffects.add(effect);
        }
    }

    /**
     * Remove um efeito de poção do item.
     * 
     * @param effect efeito a ser removido
     */
    public void removePotionEffect(PotionEffect effect) {
        potionEffects.remove(effect);
    }

    /**
     * Adiciona um comando ao item.
     * 
     * @param command comando a ser executado
     */
    public void addCommand(String command) {
        if (command != null && !command.trim().isEmpty()) {
            commands.add(command);
        }
    }

    /**
     * Remove um comando do item.
     * 
     * @param command comando a ser removido
     */
    public void removeCommand(String command) {
        commands.remove(command);
    }

    /**
     * Adiciona um item ao kit.
     * 
     * @param kitItem item do kit
     */
    public void addKitItem(ShopItem kitItem) {
        if (kitItem != null) {
            kitItems.add(kitItem);
        }
    }

    /**
     * Remove um item do kit.
     * 
     * @param kitItem item a ser removido
     */
    public void removeKitItem(ShopItem kitItem) {
        kitItems.remove(kitItem);
    }

    /**
     * Cria um ItemStack baseado nas configurações do item.
     * 
     * @return ItemStack configurado
     */
    public ItemStack createItemStack() {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        
        // Definir nome
        if (name != null && !name.isEmpty()) {
            meta.setDisplayName(name);
        }
        
        // Definir lore
        if (!lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        // Aplicar encantamentos
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }
        
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    /**
     * Verifica se o item é um kit.
     * 
     * @return true se o item é um kit
     */
    public boolean isKit() {
        return !kitItems.isEmpty();
    }

    /**
     * Verifica se o item executa comandos.
     * 
     * @return true se o item executa comandos
     */
    public boolean hasCommands() {
        return !commands.isEmpty();
    }

    /**
     * Verifica se o item tem efeitos de poção.
     * 
     * @return true se o item tem efeitos
     */
    public boolean hasPotionEffects() {
        return !potionEffects.isEmpty();
    }

    /**
     * Verifica se o item tem encantamentos.
     * 
     * @return true se o item tem encantamentos
     */
    public boolean hasEnchantments() {
        return !enchantments.isEmpty();
    }

    /**
     * Verifica se o item é um kit.
     * 
     * @return true se o item é um kit
     */
    public boolean isKitItem() {
        return kitItems != null && !kitItems.isEmpty();
    }

    /**
     * Verifica se o item é um comando.
     * 
     * @return true se o item é um comando
     */
    public boolean isCommandItem() {
        return commands != null && !commands.isEmpty();
    }

    /**
     * Verifica se o item é uma poção.
     * 
     * @return true se o item é uma poção
     */
    public boolean isPotionItem() {
        return potionEffects != null && !potionEffects.isEmpty();
    }

    /**
     * Calcula o preço com desconto aplicado.
     * 
     * @param discountRate taxa de desconto (0.0 a 1.0)
     * @return preço com desconto
     */
    public double getDiscountedPrice(double discountRate) {
        return price * (1.0 - discountRate);
    }

    // ==================== GETTERS E SETTERS ====================

    /**
     * Retorna o ID do item.
     * 
     * @return ID do item
     */
    public String getId() {
        return id;
    }

    /**
     * Retorna o nome do item.
     * 
     * @return nome do item
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna a descrição do item.
     * 
     * @return descrição do item
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna o material do item.
     * 
     * @return material do item
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Retorna o preço do item.
     * 
     * @return preço do item
     */
    public double getPrice() {
        return price;
    }

    /**
     * Retorna a quantidade do item.
     * 
     * @return quantidade do item
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Define a quantidade do item.
     * 
     * @param amount nova quantidade
     */
    public void setAmount(int amount) {
        this.amount = Math.max(1, amount);
    }

    /**
     * Retorna a lore do item.
     * 
     * @return lista de lore
     */
    public List<String> getLore() {
        return new ArrayList<>(lore);
    }

    /**
     * Define a lore do item.
     * 
     * @param lore nova lore
     */
    public void setLore(List<String> lore) {
        this.lore = new ArrayList<>(lore);
    }

    /**
     * Retorna os encantamentos do item.
     * 
     * @return mapa de encantamentos
     */
    public Map<Enchantment, Integer> getEnchantments() {
        return new HashMap<>(enchantments);
    }

    /**
     * Retorna os efeitos de poção do item.
     * 
     * @return lista de efeitos
     */
    public List<PotionEffect> getPotionEffects() {
        return new ArrayList<>(potionEffects);
    }

    /**
     * Retorna os comandos do item.
     * 
     * @return lista de comandos
     */
    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    /**
     * Define os comandos do item.
     * 
     * @param commands nova lista de comandos
     */
    public void setCommands(List<String> commands) {
        this.commands = new ArrayList<>(commands);
    }

    /**
     * Retorna os itens do kit.
     * 
     * @return lista de itens do kit
     */
    public List<ShopItem> getKitItems() {
        return new ArrayList<>(kitItems);
    }

    /**
     * Define os itens do kit.
     * 
     * @param kitItems nova lista de itens do kit
     */
    public void setKitItems(List<ShopItem> kitItems) {
        this.kitItems = new ArrayList<>(kitItems);
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ShopItem that = (ShopItem) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ShopItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", material=" + material +
                ", price=" + price +
                ", amount=" + amount +
                '}';
    }
}
