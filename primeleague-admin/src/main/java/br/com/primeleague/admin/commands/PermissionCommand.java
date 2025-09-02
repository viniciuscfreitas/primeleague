package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.events.GroupPermissionsChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Comando para gerenciar permissões individuais de jogadores.
 * 
 * Uso:
 * /permission check <jogador> <permissão> - Verifica se jogador tem permissão
 * /permission list <jogador> - Lista todas as permissões do jogador
 * /permission groups <jogador> - Lista grupos do jogador
 * /permission reload <jogador> - Recarrega permissões do jogador
 * /permission reloadall - Recarrega todo o sistema de permissões
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class PermissionCommand implements CommandExecutor {

    private final PrimeLeagueAdmin plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public PermissionCommand(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("primeleague.admin.permissions")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "check":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /permission check <jogador> <permissão>");
                    return true;
                }
                checkPermission(sender, args[1], args[2]);
                break;

            case "list":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /permission list <jogador>");
                    return true;
                }
                listPlayerPermissions(sender, args[1]);
                break;

            case "groups":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /permission groups <jogador>");
                    return true;
                }
                listPlayerGroups(sender, args[1]);
                break;

            case "reload":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /permission reload <jogador>");
                    return true;
                }
                reloadPlayerPermissions(sender, args[1]);
                break;

            case "reloadall":
                reloadAllPermissions(sender);
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos de Permissão ===");
        sender.sendMessage(ChatColor.YELLOW + "/permission check <jogador> <permissão>");
        sender.sendMessage(ChatColor.YELLOW + "/permission list <jogador>");
        sender.sendMessage(ChatColor.YELLOW + "/permission groups <jogador>");
        sender.sendMessage(ChatColor.YELLOW + "/permission reload <jogador>");
        sender.sendMessage(ChatColor.YELLOW + "/permission reloadall");
    }

    private void checkPermission(CommandSender sender, String playerName, String permissionNode) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do jogador
                Integer playerId = getPlayerIdByName(conn, playerName);
                if (playerId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }

                // Verificar permissão usando a API
                UUID playerUuid = getPlayerUUIDByName(conn, playerName);
                if (playerUuid != null) {
                    boolean hasPermission = PrimeLeagueAPI.hasPermission(playerUuid, permissionNode);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "=== Verificação de Permissão ===");
                        sender.sendMessage(ChatColor.YELLOW + "Jogador: " + ChatColor.WHITE + playerName);
                        sender.sendMessage(ChatColor.YELLOW + "Permissão: " + ChatColor.WHITE + permissionNode);
                        sender.sendMessage(ChatColor.YELLOW + "Resultado: " + 
                            (hasPermission ? ChatColor.GREEN + "✅ CONCEDIDA" : ChatColor.RED + "❌ NEGADA"));
                    });
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao verificar permissão: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void listPlayerPermissions(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do jogador
                Integer playerId = getPlayerIdByName(conn, playerName);
                if (playerId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }

                // Buscar permissões do jogador
                Set<String> permissions = new HashSet<>();
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT DISTINCT gp.permission_node, gp.is_granted, pg.group_name, pg.priority " +
                    "FROM player_groups pg " +
                    "JOIN group_permissions gp ON pg.group_id = gp.group_id " +
                    "JOIN permission_groups pgg ON pg.group_id = pgg.group_id " +
                    "WHERE pg.player_id = ? AND (pg.expires_at IS NULL OR pg.expires_at > NOW()) " +
                    "AND pgg.is_active = TRUE " +
                    "ORDER BY pgg.priority DESC, gp.permission_node ASC")) {
                    
                    stmt.setInt(1, playerId);
                    ResultSet rs = stmt.executeQuery();
                    
                    Map<String, Boolean> permissionMap = new HashMap<>();
                    Map<String, String> groupMap = new HashMap<>();
                    
                    while (rs.next()) {
                        String permissionNode = rs.getString("permission_node");
                        boolean isGranted = rs.getBoolean("is_granted");
                        String groupName = rs.getString("group_name");
                        
                        // Se a permissão já foi definida por um grupo de maior prioridade, ignorar
                        if (!permissionMap.containsKey(permissionNode)) {
                            permissionMap.put(permissionNode, isGranted);
                            groupMap.put(permissionNode, groupName);
                        }
                    }
                    
                    // Adicionar apenas permissões concedidas
                    for (Map.Entry<String, Boolean> entry : permissionMap.entrySet()) {
                        if (entry.getValue()) {
                            permissions.add(entry.getKey());
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "=== Permissões de " + playerName + " ===");
                        
                        if (permissions.isEmpty()) {
                            sender.sendMessage(ChatColor.YELLOW + "⚠️ Nenhuma permissão encontrada!");
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "Total: " + permissions.size() + " permissões");
                            sender.sendMessage("");
                            
                            for (String permission : permissions) {
                                String groupName = groupMap.get(permission);
                                sender.sendMessage(ChatColor.GREEN + "✅ " + permission + 
                                    ChatColor.GRAY + " (via " + groupName + ")");
                            }
                        }
                    });
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao listar permissões: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void listPlayerGroups(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do jogador
                Integer playerId = getPlayerIdByName(conn, playerName);
                if (playerId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }

                // Buscar grupos do jogador
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT pg.group_id, pg.is_primary, pg.expires_at, pg.added_at, pg.reason, " +
                    "pgg.group_name, pgg.display_name, pgg.priority, pgg.is_default " +
                    "FROM player_groups pg " +
                    "JOIN permission_groups pgg ON pg.group_id = pgg.group_id " +
                    "WHERE pg.player_id = ? AND (pg.expires_at IS NULL OR pg.expires_at > NOW()) " +
                    "AND pgg.is_active = TRUE " +
                    "ORDER BY pgg.priority DESC, pg.is_primary DESC")) {
                    
                    stmt.setInt(1, playerId);
                    ResultSet rs = stmt.executeQuery();
                    
                    List<GroupInfo> groups = new ArrayList<>();
                    while (rs.next()) {
                        GroupInfo group = new GroupInfo(
                            rs.getString("group_name"),
                            rs.getString("display_name"),
                            rs.getInt("priority"),
                            rs.getBoolean("is_default"),
                            rs.getBoolean("is_primary"),
                            rs.getTimestamp("expires_at"),
                            rs.getTimestamp("added_at"),
                            rs.getString("reason")
                        );
                        groups.add(group);
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "=== Grupos de " + playerName + " ===");
                        
                        if (groups.isEmpty()) {
                            sender.sendMessage(ChatColor.YELLOW + "⚠️ Nenhum grupo encontrado!");
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "Total: " + groups.size() + " grupos");
                            sender.sendMessage("");
                            
                            for (GroupInfo group : groups) {
                                sender.sendMessage(ChatColor.YELLOW + "• " + group.groupName + 
                                    (group.isDefault ? ChatColor.GREEN + " [PADRÃO]" : "") +
                                    (group.isPrimary ? ChatColor.BLUE + " [PRIMÁRIO]" : ""));
                                sender.sendMessage(ChatColor.WHITE + "  " + group.displayName);
                                sender.sendMessage(ChatColor.GRAY + "  Prioridade: " + group.priority);
                                sender.sendMessage(ChatColor.GRAY + "  Adicionado em: " + DATE_FORMAT.format(group.addedAt));
                                if (group.expiresAt != null) {
                                    sender.sendMessage(ChatColor.GRAY + "  Expira em: " + DATE_FORMAT.format(group.expiresAt));
                                }
                                if (group.reason != null && !group.reason.isEmpty()) {
                                    sender.sendMessage(ChatColor.GRAY + "  Motivo: " + group.reason);
                                }
                                sender.sendMessage("");
                            }
                        }
                    });
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao listar grupos: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void reloadPlayerPermissions(CommandSender sender, String playerName) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar UUID do jogador
                UUID playerUuid = getPlayerUUIDByName(conn, playerName);
                if (playerUuid == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }

                // Recarregar permissões do jogador
                PrimeLeagueAPI.getPermissionManager().loadPlayerPermissionsAsync(playerUuid);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "✅ Permissões de '" + playerName + "' recarregadas com sucesso!");
                    sender.sendMessage(ChatColor.GRAY + "As mudanças serão aplicadas em tempo real.");
                });
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao recarregar permissões: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void reloadAllPermissions(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                // Recarregar todo o cache de permissões
                PrimeLeagueAPI.getPermissionManager().reloadAllCache();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "✅ Sistema de permissões recarregado com sucesso!");
                    sender.sendMessage(ChatColor.GRAY + "Todos os caches foram atualizados.");
                });
                
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao recarregar sistema: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    private Integer getPlayerIdByName(Connection conn, String playerName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT player_id FROM player_data WHERE name = ?")) {
            
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("player_id");
            }
        }
        return null;
    }

    private UUID getPlayerUUIDByName(Connection conn, String playerName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT uuid FROM player_data WHERE name = ?")) {
            
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return UUID.fromString(rs.getString("uuid"));
            }
        }
        return null;
    }

    // ============================================================================
    // CLASSES AUXILIARES
    // ============================================================================

    private static class GroupInfo {
        final String groupName;
        final String displayName;
        final int priority;
        final boolean isDefault;
        final boolean isPrimary;
        final Timestamp expiresAt;
        final Timestamp addedAt;
        final String reason;

        GroupInfo(String groupName, String displayName, int priority, boolean isDefault, 
                  boolean isPrimary, Timestamp expiresAt, Timestamp addedAt, String reason) {
            this.groupName = groupName;
            this.displayName = displayName;
            this.priority = priority;
            this.isDefault = isDefault;
            this.isPrimary = isPrimary;
            this.expiresAt = expiresAt;
            this.addedAt = addedAt;
            this.reason = reason;
        }
    }
}
