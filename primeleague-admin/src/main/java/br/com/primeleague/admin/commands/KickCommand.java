package br.com.primeleague.admin.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import br.com.primeleague.api.models.Punishment;

/**
 * Comando para expulsar jogadores do servidor.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class KickCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public KickCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.kick")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }
        
        // Verificar argumentos
        if (args.length < 1) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/kick <jogador> [motivo]");
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
        
        // Verificar se não está tentando kickar a si mesmo
        if (targetPlayer.equals(sender)) {
            PrimeLeagueAPI.sendError((Player) sender, "Você não pode expulsar a si mesmo.");
            return true;
        }
        
        // Executar kick de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Usar UUID determinístico (mesmo do Core)
                    final java.util.UUID targetUuid = br.com.primeleague.core.util.UUIDUtils.offlineUUIDFromName(targetName);
                    
                    // VERIFICAÇÃO DE SEGURANÇA - Prevenir race condition
                    if (!profileExists(targetUuid)) {
                        // Logar erro grave no console
                        Bukkit.getLogger().severe("TENTATIVA DE PUNIR JOGADOR SEM PERFIL PERSISTIDO: " + targetName + " (UUID: " + targetUuid + ")");
                        
                        // Enviar mensagem de erro para o admin na thread principal
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError((Player) sender, "O perfil do jogador " + targetName + " ainda não foi carregado. Tente novamente em alguns segundos.");
                            }
                        });
                        return; // Aborta a operação
                    }
                    
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    // Registrar kick no banco de dados
                    final boolean success = registerPunishment(targetUuid, targetName, authorUuid, authorName, 
                                                       Punishment.Type.KICK, reason, null);
                    
                    // Disparar evento com severidade MEDIA
                    if (success) {
                        dispatchPlayerPunishedEvent(targetUuid, targetName, authorName, 
                                                  br.com.primeleague.api.enums.PunishmentSeverity.MEDIA, reason, null);
                    }
                    
                    // Executar kick na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Kickar o jogador
                                targetPlayer.kickPlayer("§cVocê foi expulso por " + authorName + 
                                                      "\n§7Motivo: " + reason);
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Jogador " + targetName + " foi expulso com sucesso.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§c[KICK] §f" + authorName + " expulsou " + targetName + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao registrar kick no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar kick: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar kick.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
    

}
