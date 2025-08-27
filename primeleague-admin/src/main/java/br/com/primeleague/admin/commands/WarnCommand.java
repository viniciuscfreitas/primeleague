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
 * Comando /warn - Aplica um aviso a um jogador.
 */
public class WarnCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public WarnCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin.warn")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }
        
        // Verificar argumentos
        if (args.length < 2) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/warn <jogador> <motivo>");
            return true;
        }
        
        final String targetName = args[0];
        final String reason = buildReason(args, 1);
        final String authorName = sender.getName();
        
        // Verificar se o jogador alvo existe
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound((Player) sender, targetName);
            return true;
        }
        
        // Verificar se não está tentando avisar a si mesmo
        if (targetPlayer.equals(sender)) {
            PrimeLeagueAPI.sendError((Player) sender, "Você não pode avisar a si mesmo.");
            return true;
        }
        
        // Executar warn de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    // Registrar warn no banco de dados
                    final boolean success = registerPunishment(targetPlayer.getUniqueId(), targetName, authorUuid, authorName, 
                                                       Punishment.Type.WARN, reason, null);
                    
                    // Disparar evento com severidade LEVE
                    if (success) {
                        dispatchPlayerPunishedEvent(targetPlayer.getUniqueId(), targetName, authorName, 
                                                  br.com.primeleague.api.enums.PunishmentSeverity.LEVE, reason, null);
                    }
                    
                    // Executar notificações na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar jogador avisado
                                targetPlayer.sendMessage("§c§l⚠ AVISO ⚠");
                                targetPlayer.sendMessage("§7Você recebeu um aviso de " + authorName);
                                targetPlayer.sendMessage("§7Motivo: §f" + reason);
                                targetPlayer.sendMessage("§7Acumular muitos avisos pode resultar em punições maiores.");
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Aviso aplicado a " + targetName + " com sucesso.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§e[WARN] §f" + authorName + " aplicou aviso em " + targetName + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao registrar aviso no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar warn: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar warn.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
}
