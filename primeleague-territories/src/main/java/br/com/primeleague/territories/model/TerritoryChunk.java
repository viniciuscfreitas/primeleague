package br.com.primeleague.territories.model;

import java.sql.Timestamp;

/**
 * Modelo de dados para um chunk de território.
 * Representa um chunk reivindicado por um clã.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class TerritoryChunk {
    
    private int id;
    private int clanId;
    private String worldName;
    private int chunkX;
    private int chunkZ;
    private Timestamp claimedAt;
    
    public TerritoryChunk() {}
    
    public TerritoryChunk(int clanId, String worldName, int chunkX, int chunkZ) {
        this.clanId = clanId;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimedAt = new Timestamp(System.currentTimeMillis());
    }
    
    // Getters e Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getClanId() {
        return clanId;
    }
    
    public void setClanId(int clanId) {
        this.clanId = clanId;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public int getChunkX() {
        return chunkX;
    }
    
    public void setChunkX(int chunkX) {
        this.chunkX = chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    public void setChunkZ(int chunkZ) {
        this.chunkZ = chunkZ;
    }
    
    public Timestamp getClaimedAt() {
        return claimedAt;
    }
    
    public void setClaimedAt(Timestamp claimedAt) {
        this.claimedAt = claimedAt;
    }
    
    /**
     * Gera uma chave única para o chunk baseada em world, x, z.
     * 
     * @return Chave única do chunk
     */
    public String getChunkKey() {
        return worldName + ":" + chunkX + ":" + chunkZ;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TerritoryChunk that = (TerritoryChunk) obj;
        return chunkX == that.chunkX && 
               chunkZ == that.chunkZ && 
               worldName.equals(that.worldName);
    }
    
    @Override
    public int hashCode() {
        return getChunkKey().hashCode();
    }
    
    @Override
    public String toString() {
        return "TerritoryChunk{" +
                "id=" + id +
                ", clanId=" + clanId +
                ", worldName='" + worldName + '\'' +
                ", chunkX=" + chunkX +
                ", chunkZ=" + chunkZ +
                ", claimedAt=" + claimedAt +
                '}';
    }
}
