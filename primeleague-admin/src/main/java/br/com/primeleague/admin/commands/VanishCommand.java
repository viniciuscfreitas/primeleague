package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /vanish - Ativa/desativa o modo staff invisível.
 */
public class VanishCommand implements CommandExecutor {
    
    private final AdminManager adminManager;
    
    public VanishCommand(AdminManager adminManager) {
        this.adminManager = adminManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("primeleague.admin.vanish")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }
        
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        boolean isCurrentlyVanished = adminManager.isVanished(playerId);
        boolean newVanishState = !isCurrentlyVanished;
        
        // Aplicar vanish
        if (adminManager.toggleVanish(playerId, newVanishState, playerId)) {
            if (newVanishState) {
                // Ativar vanish
                enableVanish(player);
                player.sendMessage(ChatColor.GREEN + "Modo vanish ativado!");
            } else {
                // Desativar vanish
                disableVanish(player);
                player.sendMessage(ChatColor.GREEN + "Modo vanish desativado!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Erro ao alterar modo vanish!");
        }
        
        return true;
    }
    
    /**
     * Ativa o modo vanish para um jogador.
     */
    private void enableVanish(Player player) {
        // Remover da tab list para todos os jogadores
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.hasPermission("primeleague.admin.vanish")) {
                onlinePlayer.hidePlayer(player);
            }
        }
        
        // Configurar invisibilidade
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Remover join/quit messages (será feito pelo VanishListener)
    }
    
    /**
     * Desativa o modo vanish para um jogador.
     */
    private void disableVanish(Player player) {
        // Mostrar para todos os jogadores
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showPlayer(player);
        }
        
        // Restaurar configurações normais
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Restaurar join/quit messages (será feito pelo VanishListener)
    }
}
