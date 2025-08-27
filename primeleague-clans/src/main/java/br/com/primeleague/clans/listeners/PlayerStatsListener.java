package br.com.primeleague.clans.listeners;

import br.com.primeleague.clans.PrimeLeagueClans;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.model.ClanPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Listener para gerenciar estatísticas de jogadores (KDR).
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class PlayerStatsListener implements Listener {

    private final PrimeLeagueClans plugin;
    private final ClanManager clanManager;

    public PlayerStatsListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    /**
     * Handler para o evento de morte de jogador.
     * Atualiza as estatísticas de KDR quando um jogador morre.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Verificar se a vítima e o assassino são ambos Player
        if (victim == null || killer == null) {
            return;
        }

        // Obter os ClanPlayers
        ClanPlayer victimClanPlayer = clanManager.getClanPlayer(victim);
        ClanPlayer killerClanPlayer = clanManager.getClanPlayer(killer);

        if (victimClanPlayer == null || killerClanPlayer == null) {
            return;
        }

        try {
            // Atualizar KDR de forma transacional (killer + vítima + log em uma operação)
            boolean success = clanManager.updateKDRTransactionally(killerClanPlayer, victimClanPlayer);
            
            if (!success) {
                plugin.getLogger().warning("Falha ao atualizar KDR transacionalmente para " + killer.getName() + " vs " + victim.getName());
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao atualizar estatísticas de KDR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
