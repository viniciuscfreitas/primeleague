package br.com.primeleague.adminshop.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo para representar uma categoria da loja administrativa.
 * 
 * Uma categoria contém:
 * - ID único
 * - Nome e descrição
 * - Ícone (material)
 * - Posição no inventário
 * - Lista de itens
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopCategory {

    private final String id;
    private final String name;
    private final String description;
    private final Material icon;
    private final int slot;
    private final List<ShopItem> items;

    /**
     * Construtor da categoria.
     * 
     * @param id ID único da categoria
     * @param name nome da categoria
     * @param description descrição da categoria
     * @param icon material do ícone
     * @param slot posição no inventário
     */
    public ShopCategory(String id, String name, String description, Material icon, int slot) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.slot = slot;
        this.items = new ArrayList<>();
    }

    /**
     * Adiciona um item à categoria.
     * 
     * @param item item a ser adicionado
     */
    public void addItem(ShopItem item) {
        if (item != null) {
            items.add(item);
        }
    }

    /**
     * Remove um item da categoria.
     * 
     * @param itemId ID do item a ser removido
     * @return true se o item foi removido
     */
    public boolean removeItem(String itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(itemId)) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Busca um item pelo ID.
     * 
     * @param itemId ID do item
     * @return ShopItem ou null se não encontrado
     */
    public ShopItem getItem(String itemId) {
        for (ShopItem item : items) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Verifica se a categoria contém um item.
     * 
     * @param itemId ID do item
     * @return true se a categoria contém o item
     */
    public boolean containsItem(String itemId) {
        return getItem(itemId) != null;
    }

    /**
     * Retorna o número de itens na categoria.
     * 
     * @return número de itens
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Verifica se a categoria está vazia.
     * 
     * @return true se não há itens
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ==================== GETTERS ====================

    /**
     * Retorna o ID da categoria.
     * 
     * @return ID da categoria
     */
    public String getId() {
        return id;
    }

    /**
     * Retorna o nome da categoria.
     * 
     * @return nome da categoria
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna a descrição da categoria.
     * 
     * @return descrição da categoria
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retorna o ícone da categoria.
     * 
     * @return material do ícone
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Retorna a posição da categoria no inventário.
     * 
     * @return slot da categoria
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Retorna a lista de itens da categoria.
     * 
     * @return lista de itens
     */
    public List<ShopItem> getItems() {
        return new ArrayList<>(items);
    }

    // ==================== EQUALS & HASHCODE ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ShopCategory that = (ShopCategory) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "ShopCategory{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", itemCount=" + items.size() +
                '}';
    }
}
