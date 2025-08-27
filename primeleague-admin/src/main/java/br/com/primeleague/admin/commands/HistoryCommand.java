package br.com.primeleague.admin.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para mostrar histórico de punições de um jogador.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class HistoryCommand extends BasePunishmentCommand implements CommandExecutor {
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public HistoryCommand(PrimeLeagueAdmin plugin) {
        super(plugin); // Passa a instância para a classe pai
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin.history")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }

        // Verificar argumentos
        if (args.length < 1) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/history <jogador>");
            return true;
        }

        final String targetName = args[0];
        final Player senderPlayer = (Player) sender;
        
        // Verificar se o jogador está online (para obter UUID)
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            // Se não estiver online, tentar buscar pelo nome no banco
            PrimeLeagueAPI.sendInfo(senderPlayer, "Jogador não está online. Buscando no banco de dados...");
            showHistoryOffline(targetName, senderPlayer);
            return true;
        }

        // Executar busca de histórico de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar histórico no banco de dados
                    final List<PunishmentRecord> history = getPunishmentHistory(targetPlayer.getUniqueId());
                    
                    // Mostrar histórico na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            showHistory(senderPlayer, targetName, history);
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao buscar histórico: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(senderPlayer, 
                                "Erro interno ao buscar histórico.");
                        }
                    });
                }
            }
        });

        return true;
    }

    /**
     * Mostra histórico de um jogador offline.
     * 
     * @param targetName Nome do jogador
     * @param senderPlayer Jogador que executou o comando
     */
    private void showHistoryOffline(final String targetName, final Player senderPlayer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar UUID do jogador no banco
                    java.util.UUID targetUuid = findPlayerUUID(targetName);
                    if (targetUuid == null) {
                        Bukkit.getScheduler().runTask(plugin, new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError(senderPlayer, 
                                    "Jogador " + targetName + " não encontrado no banco de dados.");
                            }
                        });
                        return;
                    }
                    
                    // Buscar histórico no banco de dados
                    final List<PunishmentRecord> history = getPunishmentHistory(targetUuid);
                    
                    // Mostrar histórico na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            showHistory(senderPlayer, targetName, history);
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao buscar histórico offline: " + e.getMessage());
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(senderPlayer, 
                                "Erro interno ao buscar histórico.");
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Busca o histórico de punições de um jogador.
     * 
     * @param targetUuid UUID do jogador
     * @return Lista de punições
     */
    private List<PunishmentRecord> getPunishmentHistory(java.util.UUID targetUuid) {
        List<PunishmentRecord> history = new ArrayList<PunishmentRecord>();
        
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            connection = PrimeLeagueAPI.getDataManager().getConnection();
            stmt = connection.prepareStatement(
                "SELECT type, reason, author_name, created_at, expires_at, active, " +
                "pardoned_by_name, pardoned_at, pardon_reason " +
                "FROM punishments " +
                "WHERE target_uuid = ? " +
                "ORDER BY created_at DESC " +
                "LIMIT 20"
            );
            
            stmt.setString(1, targetUuid.toString());
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                PunishmentRecord record = new PunishmentRecord();
                record.type = rs.getString("type");
                record.reason = rs.getString("reason");
                record.authorName = rs.getString("author_name");
                record.createdAt = rs.getTimestamp("created_at");
                record.expiresAt = rs.getTimestamp("expires_at");
                record.active = rs.getBoolean("active");
                record.pardonedByName = rs.getString("pardoned_by_name");
                record.pardonedAt = rs.getTimestamp("pardoned_at");
                record.pardonReason = rs.getString("pardon_reason");
                
                history.add(record);
            }
            
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Erro SQL ao buscar histórico: " + e.getMessage());
        } finally {
            // Fechar recursos manualmente
            if (rs != null) try { rs.close(); } catch (SQLException e) {}
            if (stmt != null) try { stmt.close(); } catch (SQLException e) {}
            if (connection != null) try { connection.close(); } catch (SQLException e) {}
        }
        
        return history;
    }
    
    /**
     * Mostra o histórico de punições para o jogador.
     * 
     * @param senderPlayer Jogador que executou o comando
     * @param targetName Nome do jogador alvo
     * @param history Lista de punições
     */
    private void showHistory(Player senderPlayer, String targetName, List<PunishmentRecord> history) {
        if (history.isEmpty()) {
            PrimeLeagueAPI.sendInfo(senderPlayer, "Jogador " + targetName + " não possui histórico de punições.");
            return;
        }
        
        // Cabeçalho
        senderPlayer.sendMessage("§6§l=== HISTÓRICO DE PUNIÇÕES: " + targetName + " ===");
        senderPlayer.sendMessage("§7Total de punições: §e" + history.size());
        senderPlayer.sendMessage("");
        
        // Listar punições
        for (int i = 0; i < history.size(); i++) {
            PunishmentRecord record = history.get(i);
            
            // Número da punição
            senderPlayer.sendMessage("§6#" + (i + 1) + " §7- §e" + record.type);
            
            // Status
            String status;
            if (record.active) {
                if (record.expiresAt != null && record.expiresAt.before(new java.util.Date())) {
                    status = "§cEXPIRADA";
                } else {
                    status = "§aATIVA";
                }
            } else {
                status = "§7REMOVIDA";
            }
            senderPlayer.sendMessage("  §7Status: " + status);
            
            // Motivo
            senderPlayer.sendMessage("  §7Motivo: §f" + record.reason);
            
            // Autor
            senderPlayer.sendMessage("  §7Autor: §f" + record.authorName);
            
            // Data de criação
            senderPlayer.sendMessage("  §7Data: §f" + formatDate(record.createdAt));
            
            // Data de expiração (se aplicável)
            if (record.expiresAt != null) {
                senderPlayer.sendMessage("  §7Expira: §f" + formatDate(record.expiresAt));
            }
            
            // Informações de remoção (se aplicável)
            if (!record.active && record.pardonedByName != null) {
                senderPlayer.sendMessage("  §7Removido por: §f" + record.pardonedByName);
                senderPlayer.sendMessage("  §7Data de remoção: §f" + formatDate(record.pardonedAt));
                if (record.pardonReason != null) {
                    senderPlayer.sendMessage("  §7Motivo da remoção: §f" + record.pardonReason);
                }
            }
            
            senderPlayer.sendMessage("");
        }
        
        senderPlayer.sendMessage("§6§l=== FIM DO HISTÓRICO ===");
    }
    

    
    /**
     * Classe para representar um registro de punição.
     */
    private static class PunishmentRecord {
        String type;
        String reason;
        String authorName;
        java.util.Date createdAt;
        java.util.Date expiresAt;
        boolean active;
        String pardonedByName;
        java.util.Date pardonedAt;
        String pardonReason;
    }
}
