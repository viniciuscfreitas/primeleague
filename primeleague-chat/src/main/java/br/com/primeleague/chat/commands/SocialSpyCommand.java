package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.PrivateMessageService;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para administradores ativarem/desativarem o modo social spy.
 * Permite monitorar mensagens privadas para fins de moderação.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class SocialSpyCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final PrivateMessageService privateMessageService;
    
    public SocialSpyCommand(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.privateMessageService = plugin.getPrivateMessageService();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.chat.socialspy")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            // Alternar estado atual
            toggleSocialSpy(player);
        } else if (args.length == 1) {
            String action = args[0].toLowerCase();
            
            switch (action) {
                case "on":
                case "ativar":
                case "enable":
                    enableSocialSpy(player);
                    break;
                    
                case "off":
                case "desativar":
                case "disable":
                    disableSocialSpy(player);
                    break;
                    
                case "status":
                case "info":
                    showStatus(player);
                    break;
                    
                default:
                    showHelp(player);
                    break;
            }
        } else {
            showHelp(player);
        }
        
        return true;
    }
    
    /**
     * Alterna o estado do social spy.
     */
    private void toggleSocialSpy(Player player) {
        if (privateMessageService.hasSocialSpy(player)) {
            disableSocialSpy(player);
        } else {
            enableSocialSpy(player);
        }
    }
    
    /**
     * Ativa o social spy.
     */
    private void enableSocialSpy(Player player) {
        if (privateMessageService.enableSocialSpy(player)) {
            player.sendMessage("§a🔍 Social Spy ativado com sucesso!");
            player.sendMessage("§7💡 Você verá todas as mensagens privadas enviadas no servidor.");
        }
    }
    
    /**
     * Desativa o social spy.
     */
    private void disableSocialSpy(Player player) {
        if (privateMessageService.disableSocialSpy(player)) {
            player.sendMessage("§c🔍 Social Spy desativado com sucesso!");
        }
    }
    
    /**
     * Mostra o status atual do social spy.
     */
    private void showStatus(Player player) {
        boolean isActive = privateMessageService.hasSocialSpy(player);
        
        player.sendMessage("§6=== Status do Social Spy ===");
        player.sendMessage("§7Status: " + (isActive ? "§aAtivo" : "§cInativo"));
        
        if (isActive) {
            player.sendMessage("§7Você está monitorando todas as mensagens privadas.");
        } else {
            player.sendMessage("§7Use §e/socialspy on §7para ativar o monitoramento.");
        }
    }
    
    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(Player player) {
        player.sendMessage("§6=== Comando Social Spy ===");
        player.sendMessage("§e/socialspy §7- Alterna o status atual");
        player.sendMessage("§e/socialspy on §7- Ativa o social spy");
        player.sendMessage("§e/socialspy off §7- Desativa o social spy");
        player.sendMessage("§e/socialspy status §7- Mostra o status atual");
        player.sendMessage("§7💡 Permissão necessária: §fprimeleague.chat.socialspy");
    }
}
