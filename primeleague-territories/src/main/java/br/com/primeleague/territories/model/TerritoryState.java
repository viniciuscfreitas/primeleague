package br.com.primeleague.territories.model;

/**
 * Enum que representa o estado de um território/clã.
 * Usado para determinar se um clã está vulnerável ou fortificado.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public enum TerritoryState {
    
    /**
     * Clã está fortificado - Moral >= Territórios
     * Não pode ser atacado
     */
    FORTIFICADO("§a§lFORTIFICADO", "Clã está fortificado e protegido contra ataques"),
    
    /**
     * Clã está vulnerável - Moral < Territórios
     * Pode ser atacado
     */
    VULNERAVEL("§c§lVULNERÁVEL", "Clã está vulnerável e pode ser atacado"),
    
    /**
     * Estado temporário durante cerco
     * Clã está em guerra ativa
     */
    EM_GUERRA("§6§lEM GUERRA", "Clã está em guerra ativa"),
    
    /**
     * Estado temporário após vitória
     * Clã recebeu bônus de moral
     */
    VITORIOSO("§e§lVITORIOSO", "Clã venceu uma guerra recentemente");
    
    private final String displayName;
    private final String description;
    
    TerritoryState(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Verifica se o estado permite ataques.
     * 
     * @return true se o clã pode ser atacado
     */
    public boolean canBeAttacked() {
        return this == VULNERAVEL;
    }
    
    /**
     * Verifica se o estado é temporário.
     * 
     * @return true se o estado é temporário
     */
    public boolean isTemporary() {
        return this == EM_GUERRA || this == VITORIOSO;
    }
    
    /**
     * Verifica se o estado é de proteção.
     * 
     * @return true se o clã está protegido
     */
    public boolean isProtected() {
        return this == FORTIFICADO;
    }
}
