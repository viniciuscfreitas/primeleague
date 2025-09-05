package br.com.primeleague.api.models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modelo de dados para um warp público.
 * Representa um ponto de teletransporte público com custo e permissões.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class Warp {
    
    private int warpId;
    private String warpName;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String permissionNode;
    private BigDecimal cost;
    private String createdBy;
    private Timestamp createdAt;
    private Timestamp lastUsed;
    private int usageCount;
    
    /**
     * Construtor básico.
     */
    public Warp() {
        this.cost = BigDecimal.ZERO;
        this.usageCount = 0;
    }
    
    /**
     * Construtor completo.
     */
    public Warp(String warpName, String world, double x, double y, double z, 
                float yaw, float pitch, String permissionNode, BigDecimal cost, String createdBy) {
        this.warpName = warpName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permissionNode = permissionNode;
        this.cost = cost != null ? cost : BigDecimal.ZERO;
        this.createdBy = createdBy;
        this.usageCount = 0;
    }
    
    /**
     * Verifica se o warp tem custo.
     */
    public boolean hasCost() {
        return cost != null && cost.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Verifica se o warp requer permissão.
     */
    public boolean requiresPermission() {
        return permissionNode != null && !permissionNode.trim().isEmpty();
    }
    
    /**
     * Marca o warp como usado (atualiza timestamp e contador).
     */
    public void markAsUsed() {
        this.lastUsed = new Timestamp(System.currentTimeMillis());
        this.usageCount++;
    }
    
    /**
     * Obtém o custo formatado como string.
     */
    public String getFormattedCost() {
        if (cost == null) {
            return "0.00";
        }
        return String.format("%.2f", cost.doubleValue());
    }
    
    /**
     * Obtém a permissão formatada (ou "Nenhuma" se null).
     */
    public String getFormattedPermission() {
        if (permissionNode == null || permissionNode.trim().isEmpty()) {
            return "Nenhuma";
        }
        return permissionNode;
    }
    
    // Getters e Setters
    public int getWarpId() { return warpId; }
    public void setWarpId(int warpId) { this.warpId = warpId; }
    
    public String getWarpName() { return warpName; }
    public void setWarpName(String warpName) { this.warpName = warpName; }
    
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }
    
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    
    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }
    
    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    
    public String getPermissionNode() { return permissionNode; }
    public void setPermissionNode(String permissionNode) { this.permissionNode = permissionNode; }
    
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getLastUsed() { return lastUsed; }
    public void setLastUsed(Timestamp lastUsed) { this.lastUsed = lastUsed; }
    
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    
    @Override
    public String toString() {
        return "Warp{" +
                "warpId=" + warpId +
                ", warpName='" + warpName + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", yaw=" + yaw +
                ", pitch=" + pitch +
                ", permissionNode='" + permissionNode + '\'' +
                ", cost=" + cost +
                ", createdBy='" + createdBy + '\'' +
                ", usageCount=" + usageCount +
                '}';
    }
}
