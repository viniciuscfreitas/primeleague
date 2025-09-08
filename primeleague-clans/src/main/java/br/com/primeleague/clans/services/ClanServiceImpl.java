package br.com.primeleague.clans.services;

import br.com.primeleague.api.ClanService;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.clans.model.ClanPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Implementação do ClanService para o módulo Clans.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ClanServiceImpl implements ClanService {
    
    private final ClanManager clanManager;
    
    public ClanServiceImpl(ClanManager clanManager) {
        this.clanManager = clanManager;
    }
    
    @Override
    public String getClanName(Player player) {
        Clan clan = clanManager.getClanByPlayer(player);
        return clan != null ? clan.getName() : null;
    }
    
    @Override
    public boolean areInSameClan(Player player1, Player player2) {
        Clan clan1 = clanManager.getClanByPlayer(player1);
        Clan clan2 = clanManager.getClanByPlayer(player2);
        
        if (clan1 == null || clan2 == null) {
            return false;
        }
        
        return clan1.getId() == clan2.getId();
    }
    
    @Override
    public boolean areInAlliedClans(Player player1, Player player2) {
        Clan clan1 = clanManager.getClanByPlayer(player1);
        Clan clan2 = clanManager.getClanByPlayer(player2);
        
        if (clan1 == null || clan2 == null) {
            return false;
        }
        
        return clanManager.areAllies(clan1, clan2);
    }
    
    @Override
    public Integer getClanId(Player player) {
        Clan clan = clanManager.getClanByPlayer(player);
        return clan != null ? clan.getId() : null;
    }
    
    // ==================== MÉTODOS PARA O MÓDULO DE TERRITÓRIOS ====================
    
    @Override
    public ClanDTO getPlayerClan(UUID playerUUID) {
        Clan clan = clanManager.getClanByPlayer(playerUUID);
        if (clan == null) {
            return null;
        }
        
        return convertToClanDTO(clan);
    }
    
    @Override
    public ClanDTO getClanById(int clanId) {
        Clan clan = clanManager.getClanById(clanId);
        if (clan == null) {
            return null;
        }
        
        return convertToClanDTO(clan);
    }
    
    @Override
    public ClanDTO getClanByName(String clanName) {
        Clan clan = clanManager.getClanByName(clanName);
        if (clan == null) {
            return null;
        }
        
        return convertToClanDTO(clan);
    }
    
    @Override
    public double getClanMoral(int clanId) {
        Clan clan = clanManager.getClanById(clanId);
        if (clan == null) {
            return 0.0;
        }
        
        // Calcular moral baseada em pontos de ranking e penalidades
        int baseMoral = clan.getRankingPoints();
        int penaltyReduction = clan.getPenaltyPoints() * 10; // Cada penalidade reduz 10 pontos de moral
        
        return Math.max(0.0, baseMoral - penaltyReduction);
    }
    
    @Override
    public boolean hasPermission(UUID playerUUID, String permission) {
        Clan clan = clanManager.getClanByPlayer(playerUUID);
        if (clan == null) {
            return false;
        }
        
        ClanPlayer clanPlayer = clanManager.getClanPlayer(playerUUID);
        if (clanPlayer == null) {
            return false;
        }
        
        // Verificar permissões baseadas no cargo
        switch (permission.toLowerCase()) {
            case "claim":
            case "territories.claim":
                return clanPlayer.getRole().equals("LEADER") || clanPlayer.getRole().equals("OFFICER");
            case "war":
            case "territories.war":
                return clanPlayer.getRole().equals("LEADER");
            case "bank":
            case "territories.bank":
                return clanPlayer.getRole().equals("LEADER") || clanPlayer.getRole().equals("OFFICER");
            default:
                return false;
        }
    }
    
    @Override
    public boolean hasActiveTruce(int clanId1, int clanId2) {
        Clan clan1 = clanManager.getClanById(clanId1);
        Clan clan2 = clanManager.getClanById(clanId2);
        
        if (clan1 == null || clan2 == null) {
            return false;
        }
        
        // Verificar se há uma relação de não agressão ativa
        return clanManager.hasNonAggressionPact(clan1, clan2);
    }
    
    /**
     * Converte um objeto Clan para ClanDTO.
     * 
     * @param clan Clã a ser convertido
     * @return ClanDTO correspondente
     */
    private ClanDTO convertToClanDTO(Clan clan) {
        ClanDTO dto = new ClanDTO();
        dto.setId(clan.getId());
        dto.setTag(clan.getTag());
        dto.setName(clan.getName());
        dto.setFounderPlayerId(-1); // Placeholder - não disponível no modelo atual
        dto.setFounderName(clan.getFounderName());
        dto.setCreationDate(new java.util.Date()); // Placeholder - não disponível no modelo atual
        dto.setFriendlyFireEnabled(false); // Placeholder - não disponível no modelo atual
        dto.setPenaltyPoints(clan.getPenaltyPoints());
        dto.setRankingPoints(clan.getRankingPoints());
        return dto;
    }
}
