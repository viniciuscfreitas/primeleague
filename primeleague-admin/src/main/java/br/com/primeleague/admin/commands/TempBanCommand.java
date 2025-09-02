package br.com.primeleague.admin.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import br.com.primeleague.admin.models.Punishment;

import java.util.Date;

/**
 * Comando para banir jogadores temporariamente.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class TempBanCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public TempBanCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.tempban")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }

        // Verificar argumentos
        if (args.length < 2) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/tempban <jogador> <tempo> [motivo]");
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

        // Verificar se não está tentando banir a si mesmo
        if (targetPlayer.equals(sender)) {
            PrimeLeagueAPI.sendError((Player) sender, "Você não pode banir a si mesmo.");
            return true;
        }

        // Verificar se já tem ban ativo
        if (hasActivePunishment(targetPlayer.getUniqueId(), Punishment.Type.BAN)) {
            PrimeLeagueAPI.sendError((Player) sender, "Jogador " + targetName + " já está banido.");
            return true;
        }

        // Parsear tempo
        final Date expiresAt = parseTimeString(timeString);
        if (expiresAt == null) {
            PrimeLeagueAPI.sendError((Player) sender, "Formato de tempo inválido: " + timeString);
            PrimeLeagueAPI.sendInfo((Player) sender, "Formatos aceitos: 30s, 5m, 2h, 1d, 1w, 1M, 1y");
            return true;
        }
        
        // Executar tempban de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Registrar tempban no banco de dados
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    final boolean success = registerPunishment(targetPlayer.getUniqueId(), targetName, authorUuid, authorName, 
                                                       Punishment.Type.BAN, reason, expiresAt);
                    
                    // Disparar evento com severidade SERIA
                    if (success) {
                        dispatchPlayerPunishedEvent(targetPlayer.getUniqueId(), targetName, authorName, 
                                                  br.com.primeleague.api.enums.PunishmentSeverity.SERIA, reason, expiresAt);
                    }
                    
                    // Executar ban na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Banir o jogador
                                targetPlayer.kickPlayer("§cVocê foi banido temporariamente por " + authorName + 
                                                      "\n§7Motivo: " + reason +
                                                      "\n§7Expira em: " + formatDate(expiresAt));
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Jogador " + targetName + " foi banido temporariamente até " + formatDate(expiresAt));
                                
                                // Broadcast para staff
                                broadcastToStaff("§4[TEMPBAN] §f" + authorName + " baniu " + targetName + 
                                               " temporariamente até " + formatDate(expiresAt) + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao registrar tempban no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar tempban: " + e.getMessage());
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("PrimeLeague-Admin"), new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar tempban.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
}
