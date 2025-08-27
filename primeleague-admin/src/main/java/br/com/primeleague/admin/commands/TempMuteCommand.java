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

import java.util.Date;

/**
 * Comando para silenciar jogadores temporariamente.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class TempMuteCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public TempMuteCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin.mute")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }

        // Verificar argumentos
        if (args.length < 2) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/tempmute <jogador> <tempo> [motivo]");
            PrimeLeagueAPI.sendInfo((Player) sender, "Formatos de tempo: 30s, 5m, 2h, 1d, 1w, 1M, 1y");
            return true;
        }

        final String targetName = args[0];
        final String timeString = args[1];
        final String reason = args.length > 2 ? buildReason(args, 2) : "Sem motivo especificado";
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

        // Parsear tempo
        final Date expiresAt = parseTimeString(timeString);
        if (expiresAt == null) {
            PrimeLeagueAPI.sendError((Player) sender, "Formato de tempo inválido: " + timeString);
            PrimeLeagueAPI.sendInfo((Player) sender, "Formatos aceitos: 30s, 5m, 2h, 1d, 1w, 1M, 1y");
        return true;
        }
        
        // Executar tempmute de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Registrar tempmute no banco de dados
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    final boolean success = registerPunishment(targetPlayer.getUniqueId(), targetName, authorUuid, authorName, 
                                                       Punishment.Type.MUTE, reason, expiresAt);
                    
                    // Disparar evento com severidade MEDIA
                    if (success) {
                        dispatchPlayerPunishedEvent(targetPlayer.getUniqueId(), targetName, authorName, 
                                                  br.com.primeleague.api.enums.PunishmentSeverity.MEDIA, reason, expiresAt);
                    }
                    
                    // Executar mute na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar o jogador
                                PrimeLeagueAPI.sendError(targetPlayer, 
                                    "Você foi silenciado temporariamente por " + authorName + 
                                    "\nMotivo: " + reason +
                                    "\nExpira em: " + formatDate(expiresAt));
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Jogador " + targetName + " foi silenciado temporariamente até " + formatDate(expiresAt));
                                
                                // Broadcast para staff
                                broadcastToStaff("§6[TEMPMUTE] §f" + authorName + " silenciou " + targetName + 
                                               " temporariamente até " + formatDate(expiresAt) + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao registrar tempmute no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar tempmute: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar tempmute.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
}
