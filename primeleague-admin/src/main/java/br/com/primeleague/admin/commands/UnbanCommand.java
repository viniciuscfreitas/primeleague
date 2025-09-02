package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.api.events.PlayerPunishmentReversedEvent;
import br.com.primeleague.api.enums.PunishmentSeverity;
import br.com.primeleague.api.enums.ReversalType;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import br.com.primeleague.admin.models.Punishment;

/**
 * Comando para remover banimento de jogadores.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class UnbanCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public UnbanCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.ban")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }

        // Verificar argumentos
        if (args.length < 1) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/unban <jogador> [motivo]");
            return true;
        }

        final String targetName = args[0];
        final String reason = args.length > 1 ? buildReason(args, 1) : "Sem motivo especificado";
        final String pardonerName = sender.getName();
        
        // Determinar o tipo de reversão baseado no motivo
        final ReversalType reversalType = ReversalType.PARDON; // Padrão é perdão
        final String cleanReason = reason;
        
        if (reason.startsWith("!")) {
            // Verificar permissão para correção
            if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.correction")) {
                PrimeLeagueAPI.sendError((Player) sender, 
                    "Você não tem permissão para reverter sanções de clã. A reversão será processada como perdão.");
            } else {
                // Não posso modificar reversalType e cleanReason aqui pois são final
                // Vou usar variáveis locais para o processamento
                final ReversalType finalReversalType = ReversalType.CORRECTION;
                final String finalCleanReason = reason.substring(1).trim(); // Remove o prefixo "!"
                
                // Continuar com o processamento usando as variáveis finais
                processUnban(targetName, pardonerName, finalCleanReason, finalReversalType, sender);
                return true;
            }
        }
        
        // Se chegou aqui, é um perdão normal
        processUnban(targetName, pardonerName, cleanReason, reversalType, sender);
        return true;
    }
    
    private void processUnban(final String targetName, final String pardonerName, final String cleanReason, 
                            final ReversalType reversalType, final CommandSender sender) {
        // Verificar se o jogador está online (para obter UUID)
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            // Se não estiver online, tentar buscar pelo nome no banco
            PrimeLeagueAPI.sendInfo((Player) sender, "Jogador não está online. Tentando buscar no banco de dados...");
            unbanOfflinePlayer(targetName, pardonerName, cleanReason, reversalType, sender);
            return;
        }

        // Verificar se tem ban ativo
        if (!hasActivePunishment(targetPlayer.getUniqueId(), Punishment.Type.BAN)) {
            PrimeLeagueAPI.sendError((Player) sender, "Jogador " + targetName + " não está banido.");
            return;
        }

        // Executar unban de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    // Buscar severidade original da punição
                    final PunishmentSeverity originalSeverity = getOriginalPunishmentSeverity(targetPlayer.getUniqueId(), Punishment.Type.BAN);
                    
                    // Remover ban no banco de dados
                    final boolean success = removePunishment(targetPlayer.getUniqueId(), Punishment.Type.BAN, authorUuid, pardonerName, cleanReason);
                    
                    // Executar unban na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar o jogador
                                PrimeLeagueAPI.sendSuccess(targetPlayer, 
                                    "Seu banimento foi removido por " + pardonerName + 
                                    "\nMotivo: " + cleanReason);
                                
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Banimento de " + targetName + " foi removido com sucesso.");
                                
                                // Disparar evento de reversão de punição
                                dispatchPlayerPunishmentReversedEvent(targetPlayer.getUniqueId().toString(), targetName, 
                                                                     originalSeverity, reversalType, pardonerName, cleanReason);
                                
                                // Broadcast para staff
                                broadcastToStaff("§a[UNBAN] §f" + pardonerName + " removeu o banimento de " + targetName + 
                                               " por: " + cleanReason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao remover ban no banco de dados.");
                            }
                        }
                    });
                    
        } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar unban: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar unban.");
                        }
                    });
                }
            }
        });
    }

    /**
     * Remove banimento de um jogador offline.
     * 
     * @param targetName Nome do jogador
     * @param pardonerName Nome de quem removeu o ban
     * @param reason Motivo da remoção
     * @param reversalType Tipo de reversão
     * @param sender Comando sender
     */
    private void unbanOfflinePlayer(final String targetName, final String pardonerName, final String reason, 
                                  final ReversalType reversalType, final CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar UUID do jogador no banco
                    final java.util.UUID targetUuid = findPlayerUUID(targetName);
                    if (targetUuid == null) {
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Jogador " + targetName + " não encontrado no banco de dados.");
                            }
                        });
                        return;
                    }
                    
                    // Verificar se tem ban ativo
                    if (!hasActivePunishment(targetUuid, Punishment.Type.BAN)) {
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Jogador " + targetName + " não está banido.");
                            }
                        });
                        return;
                    }
                    
                    // Obter UUID do autor (sender)
                    final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    
                    // Buscar severidade original da punição
                    final PunishmentSeverity originalSeverity = getOriginalPunishmentSeverity(targetUuid, Punishment.Type.BAN);
                    
                    // Remover ban no banco de dados
                    final boolean success = removePunishment(targetUuid, Punishment.Type.BAN, authorUuid, pardonerName, reason);
                    
                    // Executar unban na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Banimento de " + targetName + " foi removido com sucesso.");
                                
                                // Disparar evento de reversão de punição
                                dispatchPlayerPunishmentReversedEvent(targetUuid.toString(), targetName, 
                                                                     originalSeverity, reversalType, pardonerName, reason);
                                
                                // Broadcast para staff
                                broadcastToStaff("§a[UNBAN] §f" + pardonerName + " removeu o banimento de " + targetName + 
                                               " por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao remover ban no banco de dados.");
                            }
                        }
                    });
                    
        } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar unban offline: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar unban.");
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Busca a severidade original de uma punição no banco de dados.
     * 
     * @param playerUuid UUID do jogador
     * @param punishmentType Tipo da punição
     * @return Severidade original ou LEVE como padrão
     */
    private PunishmentSeverity getOriginalPunishmentSeverity(java.util.UUID playerUuid, Punishment.Type punishmentType) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            connection = PrimeLeagueAPI.getDataManager().getConnection();
            stmt = connection.prepareStatement(
                "SELECT severity FROM punishments WHERE target_uuid = ? AND type = ? AND pardoned_at IS NULL " +
                "ORDER BY created_at DESC LIMIT 1"
            );
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, punishmentType.name());
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String severityStr = rs.getString("severity");
                try {
                    return PunishmentSeverity.valueOf(severityStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Se a severidade não for válida, retornar LEVE como padrão
                    return PunishmentSeverity.LEVE;
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro SQL ao buscar severidade original: " + e.getMessage());
        } finally {
            // Fechar recursos manualmente
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (connection != null) try { connection.close(); } catch (SQLException e) {}
        }
        return PunishmentSeverity.LEVE; // Padrão
    }
    
    /**
     * Dispara o evento de reversão de punição.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param originalSeverity Severidade original
     * @param reversalType Tipo de reversão
     * @param adminName Nome do administrador
     * @param reason Motivo da reversão
     */
    private void dispatchPlayerPunishmentReversedEvent(String playerUuid, String playerName, 
                                                     PunishmentSeverity originalSeverity, 
                                                     ReversalType reversalType, String adminName, String reason) {
        PlayerPunishmentReversedEvent event = new PlayerPunishmentReversedEvent(
            playerUuid, playerName, originalSeverity, reversalType, adminName, reason
        );
        Bukkit.getPluginManager().callEvent(event);
    }
    
}
