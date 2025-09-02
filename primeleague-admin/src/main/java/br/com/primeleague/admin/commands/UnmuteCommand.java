package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.PrimeLeagueAdmin;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import br.com.primeleague.admin.models.Punishment;

/**
 * Comando para remover silenciamento de jogadores.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class UnmuteCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public UnmuteCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.mute")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }
        
        // Verificar argumentos
        if (args.length < 1) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/unmute <jogador> [motivo]");
            return true;
        }
        
        final String targetName = args[0];
        final String reason = args.length > 1 ? buildReason(args, 1) : "Sem motivo especificado";
        final String pardonerName = sender.getName();
        
        // Verificar se o jogador está online
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound((Player) sender, targetName);
            return true;
        }
        
        // Verificar se tem mute ativo
        if (!hasActivePunishment(targetPlayer.getUniqueId(), Punishment.Type.MUTE)) {
            PrimeLeagueAPI.sendError((Player) sender, "Jogador " + targetName + " não está silenciado.");
            return true;
        }
        
        // Executar unmute de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    // Remover mute no banco de dados
                    final boolean success = removePunishment(targetPlayer.getUniqueId(), Punishment.Type.MUTE, authorUuid, pardonerName, reason);
                    
                    // Executar unmute na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar o jogador
                                PrimeLeagueAPI.sendSuccess(targetPlayer, 
                                    "Seu silenciamento foi removido por " + pardonerName + 
                                    "\nMotivo: " + reason);
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Silenciamento de " + targetName + " foi removido com sucesso.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§a[UNMUTE] §f" + pardonerName + " removeu o silenciamento de " + targetName + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao remover mute no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar unmute: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar unmute.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
}
