package br.com.primeleague.api.models;

import java.sql.Timestamp;

/**
 * Modelo de dados para uma home de jogador.
 * Representa uma localização salva que pode ser acessada via teletransporte.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class Home {
    
    private int homeId;
    private int playerId;
    private String homeName;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private Timestamp createdAt;
    private Timestamp lastUsed;
    
    /**
     * Construtor padrão.
     */
    public Home() {
        this.yaw = 0.0f;
        this.pitch = 0.0f;
    }
    
    /**
     * Construtor com parâmetros essenciais.
     * 
     * @param playerId ID do jogador
     * @param homeName Nome da home
     * @param world Nome do mundo
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param z Coordenada Z
     * @param yaw Rotação horizontal
     * @param pitch Rotação vertical
     */
    public Home(int playerId, String homeName, String world, 
                double x, double y, double z, float yaw, float pitch) {
        this.playerId = playerId;
        this.homeName = homeName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters e Setters
    
    public int getHomeId() {
        return homeId;
    }
    
    public void setHomeId(int homeId) {
        this.homeId = homeId;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }
    
    public String getHomeName() {
        return homeName;
    }
    
    public void setHomeName(String homeName) {
        this.homeName = homeName;
    }
    
    public String getWorld() {
        return world;
    }
    
    public void setWorld(String world) {
        this.world = world;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    
    public Timestamp getLastUsed() {
        return lastUsed;
    }
    
    public void setLastUsed(Timestamp lastUsed) {
        this.lastUsed = lastUsed;
    }
    
    /**
     * Atualiza o timestamp de último uso para agora.
     */
    public void markAsUsed() {
        this.lastUsed = new Timestamp(System.currentTimeMillis());
    }
    
    /**
     * Verifica se a home é válida (tem coordenadas válidas).
     * 
     * @return true se a home é válida
     */
    public boolean isValid() {
        return homeName != null && !homeName.trim().isEmpty() &&
               world != null && !world.trim().isEmpty() &&
               playerId > 0;
    }
    
    /**
     * Retorna uma representação string da localização.
     * 
     * @return String formatada com as coordenadas
     */
    public String getLocationString() {
        return String.format("%.1f, %.1f, %.1f", x, y, z);
    }
    
    @Override
    public String toString() {
        return String.format("Home{id=%d, playerId=%d, name='%s', world='%s', location=%s}", 
                           homeId, playerId, homeName, world, getLocationString());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Home home = (Home) obj;
        return homeId == home.homeId;
    }
    
    @Override
    public int hashCode() {
        return Integer.hashCode(homeId);
    }
}
