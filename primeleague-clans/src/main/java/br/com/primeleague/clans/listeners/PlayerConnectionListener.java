package br.com.primeleague.clans.listeners;

import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.core.events.PlayerIdentityLoadedEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listener para gerenciar eventos de conexão de jogadores.
 * Mantém o status online/offline dos membros do clã atualizado em tempo real.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class PlayerConnectionListener implements Listener {

    private final ClanManager clanManager;

    public PlayerConnectionListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    /**
     * CORREÇÃO ARQUITETURAL: Agora escuta PlayerIdentityLoadedEvent em vez de PlayerJoinEvent
     * Isso garante que a identidade do jogador esteja carregada antes de tentar acessá-la
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onIdentityLoaded(PlayerIdentityLoadedEvent event) {
        Player player = event.getPlayer();
        
        // ✅ AGORA É SEGURO: A identidade já está carregada no cache
        clanManager.setPlayerOnline(player);

        Clan clan = clanManager.getClanByPlayer(player);
        if (clan != null) {
            String message = ChatColor.DARK_GREEN + "» " + ChatColor.GREEN + player.getName() + " ficou online.";
            // Notifica o clã, excluindo o próprio jogador que acabou de entrar
            clanManager.notifyClanMembers(clan, message, event.getPlayerId());
        }
    }
    
    /**
     * Método mantido para compatibilidade, mas agora não executa lógica crítica
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // ✅ CORREÇÃO: Não executar lógica crítica aqui - aguardar PlayerIdentityLoadedEvent
        // Apenas log para debug
        Player player = event.getPlayer();
        player.getServer().getLogger().info("[CLANS] PlayerJoinEvent recebido para " + player.getName() + " - aguardando PlayerIdentityLoadedEvent");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // REFATORADO: Usar player_id em vez de UUID
        int playerId = br.com.primeleague.core.api.PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        if (playerId != -1) {
            clanManager.setPlayerOffline(playerId);
        } else {
            // Fallback para UUID se player_id não estiver disponível
            UUID playerUUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            clanManager.setPlayerOffline(playerUUID);
        }

        Clan clan = clanManager.getClanByPlayer(player);
        if (clan != null) {
            String message = ChatColor.DARK_RED + "» " + ChatColor.RED + player.getName() + " ficou offline.";
            // Notifica o clã sobre o jogador que saiu
            clanManager.notifyClanMembers(clan, message);
        }
    }
}
