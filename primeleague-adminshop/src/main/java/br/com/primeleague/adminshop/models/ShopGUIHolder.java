package br.com.primeleague.adminshop.models;

import org.bukkit.inventory.InventoryHolder;

/**
 * Holder customizado para identificar inventários da loja.
 * 
 * Esta classe implementa InventoryHolder para permitir identificação
 * segura e robusta dos inventários da loja, evitando a fragilidade
 * de verificação por título de inventário.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopGUIHolder implements InventoryHolder {
    
    private final String categoryId;
    private final boolean isMainMenu;
    
    /**
     * Construtor para menu principal.
     */
    public ShopGUIHolder() {
        this.categoryId = null;
        this.isMainMenu = true;
    }
    
    /**
     * Construtor para menu de categoria.
     * 
     * @param categoryId ID da categoria
     */
    public ShopGUIHolder(String categoryId) {
        this.categoryId = categoryId;
        this.isMainMenu = false;
    }
    
    /**
     * Verifica se é o menu principal.
     * 
     * @return true se é menu principal
     */
    public boolean isMainMenu() {
        return isMainMenu;
    }
    
    /**
     * Obtém o ID da categoria.
     * 
     * @return ID da categoria ou null se for menu principal
     */
    public String getCategoryId() {
        return categoryId;
    }
    
    /**
     * Obtém o inventário (implementação de InventoryHolder).
     * 
     * @return null (não usado neste contexto)
     */
    @Override
    public org.bukkit.inventory.Inventory getInventory() {
        return null;
    }
}
