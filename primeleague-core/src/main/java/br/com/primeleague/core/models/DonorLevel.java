package br.com.primeleague.core.models;

import java.util.List;
import java.util.ArrayList;

/**
 * Modelo para representar um nível de doador.
 * 
 * @author Prime League Team
 * @version 2.0.0
 */
public final class DonorLevel {
    
    private final String key;
    private final String name;
    private final double discount;
    private final double minDonation;
    private final String color;
    private final List<String> permissions;
    private final int maxAltAccounts;
    
    /**
     * Construtor para um nível de doador.
     * 
     * @param key Chave única do nível
     * @param name Nome exibido do nível
     * @param discount Percentual de desconto (0.0 a 1.0)
     * @param minDonation Doação mínima em reais
     * @param color Cor do nível (formato Minecraft)
     * @param permissions Lista de permissões do nível
     */
    public DonorLevel(String key, String name, double discount, double minDonation, 
                     String color, List<String> permissions, int maxAltAccounts) {
        this.key = key;
        this.name = name;
        this.discount = Math.max(0.0, Math.min(1.0, discount)); // Garantir entre 0.0 e 1.0
        this.minDonation = Math.max(0.0, minDonation);
        this.color = color != null ? color : "§f";
        this.permissions = permissions != null ? permissions : new ArrayList<String>();
        this.maxAltAccounts = Math.max(1, maxAltAccounts); // Mínimo 1 conta
    }
    
    /**
     * Obtém a chave única do nível.
     * 
     * @return Chave do nível
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Obtém o nome exibido do nível.
     * 
     * @return Nome do nível
     */
    public String getName() {
        return name;
    }
    
    /**
     * Obtém o nome colorido do nível.
     * 
     * @return Nome com cor
     */
    public String getColoredName() {
        return color + name;
    }
    
    /**
     * Obtém o percentual de desconto.
     * 
     * @return Desconto (0.0 a 1.0)
     */
    public double getDiscount() {
        return discount;
    }
    
    /**
     * Obtém o percentual de desconto formatado.
     * 
     * @return Desconto formatado (ex: "10%")
     */
    public String getDiscountFormatted() {
        return String.format("%.0f%%", discount * 100);
    }
    
    /**
     * Obtém a doação mínima em reais.
     * 
     * @return Doação mínima
     */
    public double getMinDonation() {
        return minDonation;
    }
    
    /**
     * Obtém a cor do nível.
     * 
     * @return Cor (formato Minecraft)
     */
    public String getColor() {
        return color;
    }
    
    /**
     * Obtém a lista de permissões do nível.
     * 
     * @return Lista de permissões
     */
    public List<String> getPermissions() {
        return permissions;
    }
    
    /**
     * Obtém o número máximo de contas alternativas permitidas.
     * 
     * @return Número máximo de contas
     */
    public int getMaxAltAccounts() {
        return maxAltAccounts;
    }
    
    /**
     * Verifica se o nível tem uma permissão específica.
     * 
     * @param permission Permissão a verificar
     * @return true se tem a permissão, false caso contrário
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
    
    /**
     * Calcula o preço com desconto.
     * 
     * @param originalPrice Preço original
     * @return Preço com desconto aplicado
     */
    public double calculateDiscountedPrice(double originalPrice) {
        return originalPrice * (1.0 - discount);
    }
    
    /**
     * Obtém o valor do desconto para um preço.
     * 
     * @param originalPrice Preço original
     * @return Valor do desconto
     */
    public double calculateDiscountAmount(double originalPrice) {
        return originalPrice * discount;
    }
    
    @Override
    public String toString() {
        return "DonorLevel{" +
                "key='" + key + '\'' +
                ", name='" + name + '\'' +
                ", discount=" + discount +
                ", minDonation=" + minDonation +
                ", color='" + color + '\'' +
                ", permissions=" + permissions +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        DonorLevel that = (DonorLevel) obj;
        return key.equals(that.key);
    }
    
    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
