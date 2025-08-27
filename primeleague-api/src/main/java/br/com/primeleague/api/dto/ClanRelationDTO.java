package br.com.primeleague.api.dto;

import java.util.Date;

/**
 * Data Transfer Object para Relação entre Clãs.
 * Classe simples para transferência de dados entre módulos.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanRelationDTO {
    
    private int clanId1;
    private int clanId2;
    private int status; // 1: ALLY, 2: RIVAL
    private Date creationDate;
    
    // Construtores
    public ClanRelationDTO() {}
    
    public ClanRelationDTO(int clanId1, int clanId2, int status, Date creationDate) {
        this.clanId1 = clanId1;
        this.clanId2 = clanId2;
        this.status = status;
        this.creationDate = creationDate;
    }
    
    // Getters e Setters
    public int getClanId1() {
        return clanId1;
    }
    
    public void setClanId1(int clanId1) {
        this.clanId1 = clanId1;
    }
    
    public int getClanId2() {
        return clanId2;
    }
    
    public void setClanId2(int clanId2) {
        this.clanId2 = clanId2;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public Date getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
    
    @Override
    public String toString() {
        return "ClanRelationDTO{" +
                "clanId1=" + clanId1 +
                ", clanId2=" + clanId2 +
                ", status=" + status +
                ", creationDate=" + creationDate +
                '}';
    }
}
