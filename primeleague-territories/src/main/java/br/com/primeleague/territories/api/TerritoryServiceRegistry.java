package br.com.primeleague.territories.api;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.model.TerritoryState;
import org.bukkit.Location;

/**
 * Interface para o serviço de territórios.
 * Expõe métodos para integração com outros módulos via PrimeLeagueAPI.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public interface TerritoryServiceRegistry {
    
    /**
     * Obtém o clã proprietário de um território na localização especificada.
     * 
     * @param location Localização a ser verificada
     * @return ClanDTO do clã proprietário, ou null se não for território reivindicado
     */
    ClanDTO getOwningClan(Location location);
    
    /**
     * Verifica se uma localização está em território reivindicado.
     * 
     * @param location Localização a ser verificada
     * @return true se está em território reivindicado
     */
    boolean isClaimed(Location location);
    
    /**
     * Verifica se uma localização está em zona de guerra (cerco ativo).
     * 
     * @param location Localização a ser verificada
     * @return true se está em zona de guerra
     */
    boolean isWarzone(Location location);
    
    /**
     * Adiciona um bônus temporário de moral a um clã.
     * 
     * @param clanId ID do clã
     * @param amount Quantidade de moral a ser adicionada
     * @param durationInSeconds Duração do bônus em segundos
     */
    void addTemporaryMoralBoost(int clanId, double amount, long durationInSeconds);
    
    /**
     * Obtém o estado territorial de um clã.
     * 
     * @param clanId ID do clã
     * @return Estado territorial (FORTIFICADO, VULNERAVEL, etc.)
     */
    TerritoryState getTerritoryState(int clanId);
    
    /**
     * Verifica se um clã pode ser atacado.
     * 
     * @param clanId ID do clã
     * @return true se o clã pode ser atacado
     */
    boolean canClanBeAttacked(int clanId);
    
    /**
     * Obtém o número de territórios de um clã.
     * 
     * @param clanId ID do clã
     * @return Número de territórios
     */
    int getTerritoryCount(int clanId);
    
    /**
     * Verifica se um clã tem territórios suficientes para ser vulnerável.
     * 
     * @param clanId ID do clã
     * @return true se o clã está vulnerável
     */
    boolean isClanVulnerable(int clanId);
    
    /**
     * Obtém o saldo do banco de um clã.
     * 
     * @param clanId ID do clã
     * @return Saldo do banco
     */
    double getClanBankBalance(int clanId);
    
    /**
     * Verifica se existe uma guerra ativa entre dois clãs.
     * 
     * @param clan1Id ID do primeiro clã
     * @param clan2Id ID do segundo clã
     * @return true se existe guerra ativa
     */
    boolean hasActiveWar(int clan1Id, int clan2Id);
    
    /**
     * Obtém o custo de manutenção de um clã.
     * 
     * @param clanId ID do clã
     * @return Custo de manutenção
     */
    double getMaintenanceCost(int clanId);
    
    /**
     * Força a verificação de manutenção de um clã.
     * 
     * @param clanId ID do clã
     */
    void forceMaintenanceCheck(int clanId);
}
