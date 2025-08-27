package br.com.primeleague.clans.listeners;

import br.com.primeleague.clans.PrimeLeagueClans;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.clans.model.ClanPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener para gerenciar o sistema de friendly fire.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class DamageListener implements Listener {

    private final PrimeLeagueClans plugin;
    private final ClanManager clanManager;

    public DamageListener(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    /**
     * Handler para o evento de dano entre entidades.
     * Controla o friendly fire entre membros do mesmo clã.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Verificar se o dano é entre jogadores
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Obter os clãs dos jogadores
        Clan attackerClan = clanManager.getClanByPlayer(attacker);
        Clan victimClan = clanManager.getClanByPlayer(victim);

        // Se ambos pertencem ao mesmo clã
        if (attackerClan != null && victimClan != null && attackerClan.equals(victimClan)) {
            // Verificar se o friendly fire está desabilitado
            if (!attackerClan.isFriendlyFireEnabled()) {
                event.setCancelled(true);
                attacker.sendMessage(org.bukkit.ChatColor.RED + "Friendly fire está desabilitado no seu clã!");
                return;
            }
        }

        // Verificar se os clãs são aliados
        if (attackerClan != null && victimClan != null && !attackerClan.equals(victimClan)) {
            if (clanManager.areAllies(attackerClan, victimClan)) {
                event.setCancelled(true);
                attacker.sendMessage(org.bukkit.ChatColor.RED + "Você não pode atacar aliados!");
                return;
            }
        }
    }
}
