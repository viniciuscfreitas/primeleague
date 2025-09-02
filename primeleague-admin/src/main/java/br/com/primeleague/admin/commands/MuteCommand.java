package br.com.primeleague.admin.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.admin.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Comando para silenciar jogadores permanentemente.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class MuteCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public MuteCommand(PrimeLeagueAdmin plugin) {
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
            PrimeLeagueAPI.sendUsage((Player) sender, "/mute <jogador> [motivo]");
            return true;
        }
        
        final String targetName = args[0];
        final String reason = args.length > 1 ? buildReason(args, 1) : "Sem motivo especificado";
        final String authorName = sender.getName();
        
        // Verificar se o jogador está online
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound((Player) sender, targetName);
            return true;
        }
        
        // Verificar se não está tentando silenciar a si mesmo
        if (targetPlayer.equals(sender)) {
            PrimeLeagueAPI.sendError((Player) sender, "Você não pode silenciar a si mesmo.");
            return true;
        }
        
        // Verificar se já tem mute ativo
        if (hasActivePunishment(targetPlayer.getUniqueId(), Punishment.Type.MUTE)) {
            PrimeLeagueAPI.sendError((Player) sender, "Jogador " + targetName + " já está silenciado.");
            return true;
        }
        
        // Executar mute de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Registrar mute no banco de dados (permanente)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    final boolean success = registerPunishment(targetPlayer.getUniqueId(), targetName, authorUuid, authorName, 
                                                       Punishment.Type.MUTE, reason, null);
                    
                    // Executar mute na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar o jogador
                                PrimeLeagueAPI.sendError(targetPlayer, 
                                    "Você foi silenciado permanentemente por " + authorName + 
                                    "\nMotivo: " + reason);
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Jogador " + targetName + " foi silenciado permanentemente.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§6[MUTE] §f" + authorName + " silenciou " + targetName + 
                                               " permanentemente por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao registrar mute no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar mute: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar mute.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
}
