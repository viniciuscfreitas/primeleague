package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.events.GroupPermissionsChangedEvent;
import br.com.primeleague.core.models.PermissionGroup;
import br.com.primeleague.core.models.PlayerGroup;
import br.com.primeleague.core.util.UUIDUtils;
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
 * Comando para gerenciar grupos de permissões.
 * 
 * Uso:
 * /group create <nome> <display> [descrição] [prioridade] - Cria um novo grupo
 * /group addperm <grupo> <permissão> [conceder/negar] - Adiciona permissão ao grupo
 * /group removeperm <grupo> <permissão> - Remove permissão do grupo
 * /group addplayer <jogador> <grupo> [primário] [duração] - Adiciona jogador ao grupo
 * /group removeplayer <jogador> <grupo> - Remove jogador do grupo
 * /group list [página] - Lista todos os grupos
 * /group info <grupo> - Mostra informações detalhadas do grupo
 * /group players <grupo> [página] - Lista jogadores do grupo
 * /group delete <grupo> - Remove um grupo (apenas se vazio)
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class GroupCommand implements CommandExecutor {

    private final PrimeLeagueAdmin plugin;
    private static final int ITEMS_PER_PAGE = 10;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public GroupCommand(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador ou console
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!PrimeLeagueAPI.hasPermission(player, "primeleague.admin.groups")) {
                PrimeLeagueAPI.sendNoPermission(player);
                return true;
            }
        } else if (!sender.hasPermission("primeleague.admin.groups")) {
            // Para console, verificar permissão direta
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group create <nome> <display> [descrição] [prioridade]");
                    return true;
                }
                createGroup(sender, args[1], args[2], 
                    args.length > 3 ? joinArgs(args, 3) : null,
                    args.length > 4 ? parsePriority(args[4]) : 0);
                break;

            case "addperm":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group addperm <grupo> <permissão> [conceder/negar]");
                    return true;
                }
                boolean isGranted = args.length > 3 ? args[3].equalsIgnoreCase("conceder") : true;
                addPermission(sender, args[1], args[2], isGranted);
                break;

            case "removeperm":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group removeperm <grupo> <permissão>");
                    return true;
                }
                removePermission(sender, args[1], args[2]);
                break;

            case "addplayer":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group addplayer <jogador> <grupo> [primário] [duração]");
                    return true;
                }
                boolean isPrimary = args.length > 3 && args[3].equalsIgnoreCase("primário");
                String duration = args.length > 4 ? args[4] : null;
                addPlayerToGroup(sender, args[1], args[2], isPrimary, duration);
                break;

            case "removeplayer":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group removeplayer <jogador> <grupo>");
                    return true;
                }
                removePlayerFromGroup(sender, args[1], args[2]);
                break;

            case "list":
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                        if (page < 1) page = 1;
                    } catch (NumberFormatException e) {
                        page = 1;
                    }
                }
                listGroups(sender, page);
                break;

            case "info":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group info <grupo>");
                    return true;
                }
                showGroupInfo(sender, args[1]);
                break;

            case "players":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group players <grupo> [página]");
                    return true;
                }
                int playerPage = 1;
                if (args.length > 2) {
                    try {
                        playerPage = Integer.parseInt(args[2]);
                        if (playerPage < 1) playerPage = 1;
                    } catch (NumberFormatException e) {
                        playerPage = 1;
                    }
                }
                listGroupPlayers(sender, args[1], playerPage);
                break;

            case "delete":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Uso: /group delete <grupo>");
                    return true;
                }
                deleteGroup(sender, args[1]);
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos de Grupo ===");
        sender.sendMessage(ChatColor.YELLOW + "/group create <nome> <display> [descrição] [prioridade]");
        sender.sendMessage(ChatColor.YELLOW + "/group addperm <grupo> <permissão> [conceder/negar]");
        sender.sendMessage(ChatColor.YELLOW + "/group removeperm <grupo> <permissão>");
        sender.sendMessage(ChatColor.YELLOW + "/group addplayer <jogador> <grupo> [primário] [duração]");
        sender.sendMessage(ChatColor.YELLOW + "/group removeplayer <jogador> <grupo>");
        sender.sendMessage(ChatColor.YELLOW + "/group list [página]");
        sender.sendMessage(ChatColor.YELLOW + "/group info <grupo>");
        sender.sendMessage(ChatColor.YELLOW + "/group players <grupo> [página]");
        sender.sendMessage(ChatColor.YELLOW + "/group delete <grupo>");
    }

    private int parsePriority(String priorityStr) {
        try {
            return Integer.parseInt(priorityStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private void createGroup(CommandSender sender, String groupName, String displayName, String description, int priority) {
        // Permitir tanto jogadores quanto console
        Player player = sender instanceof Player ? (Player) sender : null;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Verificar se o grupo já existe
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT group_id FROM permission_groups WHERE group_name = ?")) {
                    
                    checkStmt.setString(1, groupName);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' já existe!"));
                        return;
                    }
                }

                // Criar o grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO permission_groups (group_name, display_name, description, priority, is_default, is_active) VALUES (?, ?, ?, ?, FALSE, TRUE)",
                    Statement.RETURN_GENERATED_KEYS)) {
                    
                    stmt.setString(1, groupName);
                    stmt.setString(2, displayName);
                    stmt.setString(3, description);
                    stmt.setInt(4, priority);
                    
                    int affected = stmt.executeUpdate();
                    
                    if (affected > 0) {
                        ResultSet rs = stmt.getGeneratedKeys();
                        if (rs.next()) {
                            int groupId = rs.getInt(1);
                            
                            // Log da ação
                            logAction(conn, "GROUP_CREATED", player, groupId, null, null, null, 
                                "Grupo criado: " + groupName);
                            
                            // Disparar evento para atualização em tempo real
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Bukkit.getPluginManager().callEvent(
                                    new GroupPermissionsChangedEvent(groupId, groupName, "GROUP_CREATED"));
                                
                                sender.sendMessage(ChatColor.GREEN + "✅ Grupo '" + groupName + "' criado com sucesso!");
                                sender.sendMessage(ChatColor.GRAY + "ID: " + groupId + " | Prioridade: " + priority);
                            });
                        }
                    }
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao criar grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void addPermission(CommandSender sender, String groupName, String permissionNode, boolean isGranted) {
        // Permitir tanto jogadores quanto console
        Player player = sender instanceof Player ? (Player) sender : null;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do grupo
                Integer groupId = getGroupId(conn, groupName);
                if (groupId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' não encontrado!"));
                    return;
                }

                // Verificar se a permissão já existe
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM group_permissions WHERE group_id = ? AND permission_node = ?")) {
                    
                    checkStmt.setInt(1, groupId);
                    checkStmt.setString(2, permissionNode);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        // Atualizar permissão existente
                        try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE group_permissions SET is_granted = ? WHERE group_id = ? AND permission_node = ?")) {
                            
                            updateStmt.setBoolean(1, isGranted);
                            updateStmt.setInt(2, groupId);
                            updateStmt.setString(3, permissionNode);
                            
                            updateStmt.executeUpdate();
                        }
                    } else {
                        // Inserir nova permissão
                        try (PreparedStatement insertStmt = conn.prepareStatement(
                            "INSERT INTO group_permissions (group_id, permission_node, is_granted, created_by_player_id) VALUES (?, ?, ?, ?)")) {
                            
                            insertStmt.setInt(1, groupId);
                            insertStmt.setString(2, permissionNode);
                            insertStmt.setBoolean(3, isGranted);
                            insertStmt.setInt(4, getPlayerId(conn, player.getUniqueId()));
                            
                            insertStmt.executeUpdate();
                        }
                    }

                    // Log da ação
                    logAction(conn, "PERMISSION_ADDED", player, groupId, null, permissionNode, 
                        String.valueOf(isGranted), "Permissão " + (isGranted ? "concedida" : "negada"));
                    
                    // Disparar evento para atualização em tempo real
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.getPluginManager().callEvent(
                            new GroupPermissionsChangedEvent(groupId, groupName, "PERMISSION_ADDED", permissionNode));
                        
                        sender.sendMessage(ChatColor.GREEN + "✅ Permissão '" + permissionNode + "' " + 
                            (isGranted ? "concedida" : "negada") + " ao grupo '" + groupName + "'!");
                    });
                    
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao adicionar permissão: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void removePermission(CommandSender sender, String groupName, String permissionNode) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        Player player = (Player) sender;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do grupo
                Integer groupId = getGroupId(conn, groupName);
                if (groupId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' não encontrado!"));
                    return;
                }

                // Remover permissão
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM group_permissions WHERE group_id = ? AND permission_node = ?")) {
                    
                    stmt.setInt(1, groupId);
                    stmt.setString(2, permissionNode);
                    
                    int affected = stmt.executeUpdate();
                    
                    if (affected > 0) {
                        // Log da ação
                        logAction(conn, "PERMISSION_REMOVED", player, groupId, null, permissionNode, 
                            null, "Permissão removida");
                        
                        // Disparar evento para atualização em tempo real
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.getPluginManager().callEvent(
                                new GroupPermissionsChangedEvent(groupId, groupName, "PERMISSION_REMOVED", permissionNode));
                            
                            sender.sendMessage(ChatColor.GREEN + "✅ Permissão '" + permissionNode + "' removida do grupo '" + groupName + "'!");
                        });
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.YELLOW + "⚠️ Permissão '" + permissionNode + "' não encontrada no grupo '" + groupName + "'!"));
                    }
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao remover permissão: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void addPlayerToGroup(CommandSender sender, String playerName, String groupName, boolean isPrimary, String duration) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        Player admin = (Player) sender;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar IDs
                Integer playerId = getPlayerIdByName(conn, playerName);
                Integer groupId = getGroupId(conn, groupName);
                
                if (playerId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }
                
                if (groupId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' não encontrado!"));
                    return;
                }

                // Verificar se já está no grupo
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT id FROM player_groups WHERE player_id = ? AND group_id = ?")) {
                    
                    checkStmt.setInt(1, playerId);
                    checkStmt.setInt(2, groupId);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.YELLOW + "⚠️ Jogador '" + playerName + "' já está no grupo '" + groupName + "'!"));
                        return;
                    }
                }

                // Se for primário, remover outros grupos primários
                if (isPrimary) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE player_groups SET is_primary = FALSE WHERE player_id = ?")) {
                        updateStmt.setInt(1, playerId);
                        updateStmt.executeUpdate();
                    }
                }

                // Calcular data de expiração
                Timestamp expiresAt = null;
                if (duration != null && !duration.equalsIgnoreCase("permanente")) {
                    expiresAt = calculateExpiration(duration);
                }

                // Adicionar jogador ao grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO player_groups (player_id, group_id, is_primary, expires_at, added_by_player_id, reason) VALUES (?, ?, ?, ?, ?, ?)")) {
                    
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, groupId);
                    stmt.setBoolean(3, isPrimary);
                    stmt.setTimestamp(4, expiresAt);
                    stmt.setInt(5, getPlayerId(conn, admin.getUniqueId()));
                    stmt.setString(6, "Adicionado por " + admin.getName());
                    
                    stmt.executeUpdate();
                }

                final int finalGroupId = groupId;
                final String finalGroupName = groupName;
                final int finalPlayerId = playerId;
                final String finalPlayerName = playerName;
                final boolean finalIsPrimary = isPrimary;
                final java.sql.Timestamp finalExpiresAt = expiresAt;
                
                // Log da ação
                logAction(conn, "PLAYER_ADDED_TO_GROUP", admin, finalGroupId, finalPlayerId, null, 
                    finalGroupName + (finalIsPrimary ? " (primário)" : ""), "Jogador adicionado ao grupo");
                
                // Disparar evento para atualização em tempo real
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getPluginManager().callEvent(
                        new GroupPermissionsChangedEvent(finalGroupId, finalGroupName, "PLAYER_ADDED_TO_GROUP"));
                    
                    sender.sendMessage(ChatColor.GREEN + "✅ Jogador '" + finalPlayerName + "' adicionado ao grupo '" + finalGroupName + "'!");
                    if (finalIsPrimary) sender.sendMessage(ChatColor.GRAY + "Grupo definido como primário.");
                    if (finalExpiresAt != null) sender.sendMessage(ChatColor.GRAY + "Expira em: " + DATE_FORMAT.format(finalExpiresAt));
                });
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao adicionar jogador ao grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void removePlayerFromGroup(CommandSender sender, String playerName, String groupName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        Player admin = (Player) sender;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar IDs
                Integer playerId = getPlayerIdByName(conn, playerName);
                Integer groupId = getGroupId(conn, groupName);
                
                if (playerId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Jogador '" + playerName + "' não encontrado!"));
                    return;
                }
                
                if (groupId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' não encontrado!"));
                    return;
                }

                // Remover jogador do grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM player_groups WHERE player_id = ? AND group_id = ?")) {
                    
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, groupId);
                    
                    int affected = stmt.executeUpdate();
                    
                    if (affected > 0) {
                        final int finalGroupId = groupId;
                        final String finalGroupName = groupName;
                        final int finalPlayerId = playerId;
                        final String finalPlayerName = playerName;
                        
                        // Log da ação
                        logAction(conn, "PLAYER_REMOVED_FROM_GROUP", admin, finalGroupId, finalPlayerId, null, 
                            null, "Jogador removido do grupo");
                        
                        // Disparar evento para atualização em tempo real
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.getPluginManager().callEvent(
                                new GroupPermissionsChangedEvent(finalGroupId, finalGroupName, "PLAYER_REMOVED_FROM_GROUP"));
                            
                            sender.sendMessage(ChatColor.GREEN + "✅ Jogador '" + finalPlayerName + "' removido do grupo '" + finalGroupName + "'!");
                        });
                    } else {
                        final String finalPlayerName2 = playerName;
                        final String finalGroupName2 = groupName;
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.YELLOW + "⚠️ Jogador '" + finalPlayerName2 + "' não está no grupo '" + finalGroupName2 + "'!"));
                    }
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao remover jogador do grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void listGroups(CommandSender sender, int page) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Contar total de grupos
                int totalGroups = 0;
                try (PreparedStatement countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM permission_groups WHERE is_active = TRUE")) {
                    ResultSet rs = countStmt.executeQuery();
                    if (rs.next()) {
                        totalGroups = rs.getInt(1);
                    }
                }

                if (totalGroups == 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.YELLOW + "⚠️ Nenhum grupo encontrado!"));
                    return;
                }

                final int finalTotalPages = (int) Math.ceil((double) totalGroups / ITEMS_PER_PAGE);
                final int finalPage = page > finalTotalPages ? finalTotalPages : page;

                // Buscar grupos
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT group_id, group_name, display_name, description, priority, is_default, created_at FROM permission_groups " +
                    "WHERE is_active = TRUE ORDER BY priority DESC, group_name ASC LIMIT ? OFFSET ?")) {
                    
                    stmt.setInt(1, ITEMS_PER_PAGE);
                    stmt.setInt(2, (finalPage - 1) * ITEMS_PER_PAGE);
                    
                    ResultSet rs = stmt.executeQuery();
                    
                    // Coletar dados antes de fechar o ResultSet
                    List<GroupInfo> groups = new ArrayList<>();
                    while (rs.next()) {
                        groups.add(new GroupInfo(
                            rs.getInt("group_id"),
                            rs.getString("group_name"),
                            rs.getString("display_name"),
                            rs.getString("description"),
                            rs.getInt("priority"),
                            rs.getBoolean("is_default"),
                            rs.getTimestamp("created_at")
                        ));
                    }
                    
                    // Agora enviar para a thread principal
                    final List<GroupInfo> finalGroups = groups;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "=== Grupos de Permissões (Página " + finalPage + "/" + finalTotalPages + ") ===");
                        
                        for (GroupInfo group : finalGroups) {
                            sender.sendMessage(ChatColor.YELLOW + "• " + group.groupName + 
                                (group.isDefault ? ChatColor.GREEN + " [PADRÃO]" : "") +
                                ChatColor.GRAY + " (ID: " + group.groupId + ", Prioridade: " + group.priority + ")");
                            sender.sendMessage(ChatColor.WHITE + "  " + group.displayName);
                            if (group.description != null && !group.description.isEmpty()) {
                                sender.sendMessage(ChatColor.GRAY + "  " + group.description);
                            }
                            sender.sendMessage("");
                        }
                        
                        if (finalPage < finalTotalPages) {
                            sender.sendMessage(ChatColor.GRAY + "Use /group list " + (finalPage + 1) + " para próxima página");
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

    private void showGroupInfo(CommandSender sender, String groupName) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar informações do grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT group_id, group_name, display_name, description, priority, is_default, is_active, created_at, updated_at FROM permission_groups WHERE group_name = ?")) {
                    
                    stmt.setString(1, groupName);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        int groupId = rs.getInt("group_id");
                        String displayName = rs.getString("display_name");
                        String description = rs.getString("description");
                        int priority = rs.getInt("priority");
                        boolean isDefault = rs.getBoolean("is_default");
                        boolean isActive = rs.getBoolean("is_active");
                        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                        java.sql.Timestamp updatedAt = rs.getTimestamp("updated_at");
                        
                        // Contar permissões
                        int permissionCount = 0;
                        try (PreparedStatement permStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM group_permissions WHERE group_id = ?")) {
                            permStmt.setInt(1, groupId);
                            ResultSet permRs = permStmt.executeQuery();
                            if (permRs.next()) {
                                permissionCount = permRs.getInt(1);
                            }
                        }
                        
                        // Contar jogadores
                        int playerCount = 0;
                        try (PreparedStatement playerStmt = conn.prepareStatement(
                            "SELECT COUNT(*) FROM player_groups WHERE group_id = ? AND (expires_at IS NULL OR expires_at > NOW())")) {
                            playerStmt.setInt(1, groupId);
                            ResultSet playerRs = playerStmt.executeQuery();
                            if (playerRs.next()) {
                                playerCount = playerRs.getInt(1);
                            }
                        }
                        
                        final String finalGroupName = groupName;
                        final int finalGroupId = groupId;
                        final String finalDisplayName = displayName;
                        final String finalDescription = description;
                        final int finalPriority = priority;
                        final boolean finalIsDefault = isDefault;
                        final boolean finalIsActive = isActive;
                        final int finalPermissionCount = permissionCount;
                        final int finalPlayerCount = playerCount;
                        final java.sql.Timestamp finalCreatedAt = createdAt;
                        final java.sql.Timestamp finalUpdatedAt = updatedAt;
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sender.sendMessage(ChatColor.GOLD + "=== Informações do Grupo: " + finalGroupName + " ===");
                            sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + finalGroupId);
                            sender.sendMessage(ChatColor.YELLOW + "Nome de Exibição: " + ChatColor.WHITE + finalDisplayName);
                            if (finalDescription != null && !finalDescription.isEmpty()) {
                                sender.sendMessage(ChatColor.YELLOW + "Descrição: " + ChatColor.WHITE + finalDescription);
                            }
                            sender.sendMessage(ChatColor.YELLOW + "Prioridade: " + ChatColor.WHITE + finalPriority);
                            sender.sendMessage(ChatColor.YELLOW + "Padrão: " + ChatColor.WHITE + (finalIsDefault ? "Sim" : "Não"));
                            sender.sendMessage(ChatColor.YELLOW + "Ativo: " + ChatColor.WHITE + (finalIsActive ? "Sim" : "Não"));
                            sender.sendMessage(ChatColor.YELLOW + "Permissões: " + ChatColor.WHITE + finalPermissionCount);
                            sender.sendMessage(ChatColor.YELLOW + "Jogadores: " + ChatColor.WHITE + finalPlayerCount);
                            sender.sendMessage(ChatColor.YELLOW + "Criado em: " + ChatColor.WHITE + DATE_FORMAT.format(finalCreatedAt));
                            sender.sendMessage(ChatColor.YELLOW + "Atualizado em: " + ChatColor.WHITE + DATE_FORMAT.format(finalUpdatedAt));
                        });
                        
                    } else {
                        final String finalGroupName = groupName;
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.RED + "❌ Grupo '" + finalGroupName + "' não encontrado!"));
                    }
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao buscar informações do grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void listGroupPlayers(CommandSender sender, String groupName, int page) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do grupo
                Integer groupId = getGroupId(conn, groupName);
                if (groupId == null) {
                    final String finalGroupName = groupName;
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + finalGroupName + "' não encontrado!"));
                    return;
                }

                // Contar total de jogadores
                int totalPlayers = 0;
                try (PreparedStatement countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM player_groups pg " +
                    "JOIN player_data pd ON pg.player_id = pd.player_id " +
                    "WHERE pg.group_id = ? AND (pg.expires_at IS NULL OR pg.expires_at > NOW())")) {
                    
                    countStmt.setInt(1, groupId);
                    ResultSet rs = countStmt.executeQuery();
                    if (rs.next()) {
                        totalPlayers = rs.getInt(1);
                    }
                }

                if (totalPlayers == 0) {
                    final String finalGroupName2 = groupName;
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.YELLOW + "⚠️ Nenhum jogador encontrado no grupo '" + finalGroupName2 + "'!"));
                    return;
                }

                final int finalTotalPages = (int) Math.ceil((double) totalPlayers / ITEMS_PER_PAGE);
                final int finalPage = page > finalTotalPages ? finalTotalPages : page;

                // Buscar jogadores
                try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT pd.name, pg.is_primary, pg.expires_at, pg.added_at, pg.reason FROM player_groups pg " +
                    "JOIN player_data pd ON pg.player_id = pd.player_id " +
                    "WHERE pg.group_id = ? AND (pg.expires_at IS NULL OR pg.expires_at > NOW()) " +
                    "ORDER BY pg.is_primary DESC, pd.name ASC LIMIT ? OFFSET ?")) {
                    
                    stmt.setInt(1, groupId);
                    stmt.setInt(2, ITEMS_PER_PAGE);
                    stmt.setInt(3, (finalPage - 1) * ITEMS_PER_PAGE);
                    
                    final ResultSet finalRs = stmt.executeQuery();
                    
                    final String finalGroupName3 = groupName;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GOLD + "=== Jogadores do Grupo: " + finalGroupName3 + " (Página " + finalPage + "/" + finalTotalPages + ") ===");
                        
                        try {
                            while (finalRs.next()) {
                                String playerName = finalRs.getString("name");
                                boolean isPrimary = finalRs.getBoolean("is_primary");
                                Timestamp expiresAt = finalRs.getTimestamp("expires_at");
                                java.sql.Timestamp addedAt = finalRs.getTimestamp("added_at");
                                String reason = finalRs.getString("reason");
                                
                                sender.sendMessage(ChatColor.YELLOW + "• " + playerName + 
                                    (isPrimary ? ChatColor.GREEN + " [PRIMÁRIO]" : ""));
                                sender.sendMessage(ChatColor.GRAY + "  Adicionado em: " + DATE_FORMAT.format(addedAt));
                                if (expiresAt != null) {
                                    sender.sendMessage(ChatColor.GRAY + "  Expira em: " + DATE_FORMAT.format(expiresAt));
                                }
                                if (reason != null && !reason.isEmpty()) {
                                    sender.sendMessage(ChatColor.GRAY + "  Motivo: " + reason);
                                }
                                sender.sendMessage("");
                            }
                            
                            if (finalPage < finalTotalPages) {
                                sender.sendMessage(ChatColor.GRAY + "Use /group players " + finalGroupName3 + " " + (finalPage + 1) + " para próxima página");
                            }
                        } catch (SQLException e) {
                            sender.sendMessage(ChatColor.RED + "❌ Erro ao processar resultados: " + e.getMessage());
                        }
                    });
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao listar jogadores do grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void deleteGroup(CommandSender sender, String groupName) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return;
        }

        Player player = (Player) sender;
        
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDataManager().getConnection()) {
                // Buscar ID do grupo
                Integer groupId = getGroupId(conn, groupName);
                if (groupId == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> 
                        sender.sendMessage(ChatColor.RED + "❌ Grupo '" + groupName + "' não encontrado!"));
                    return;
                }

                // Verificar se o grupo tem jogadores
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM player_groups WHERE group_id = ?")) {
                    
                    checkStmt.setInt(1, groupId);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next() && rs.getInt(1) > 0) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.RED + "❌ Não é possível deletar o grupo '" + groupName + "' pois ele possui jogadores!"));
                        return;
                    }
                }

                // Verificar se é o grupo padrão
                try (PreparedStatement checkStmt = conn.prepareStatement(
                    "SELECT is_default FROM permission_groups WHERE group_id = ?")) {
                    
                    checkStmt.setInt(1, groupId);
                    ResultSet rs = checkStmt.executeQuery();
                    
                    if (rs.next() && rs.getBoolean("is_default")) {
                        Bukkit.getScheduler().runTask(plugin, () -> 
                            sender.sendMessage(ChatColor.RED + "❌ Não é possível deletar o grupo padrão '" + groupName + "'!"));
                        return;
                    }
                }

                // Deletar permissões do grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM group_permissions WHERE group_id = ?")) {
                    stmt.setInt(1, groupId);
                    stmt.executeUpdate();
                }

                // Deletar o grupo
                try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM permission_groups WHERE group_id = ?")) {
                    stmt.setInt(1, groupId);
                    int affected = stmt.executeUpdate();
                    
                    if (affected > 0) {
                        // Log da ação
                        logAction(conn, "GROUP_DELETED", player, groupId, null, null, null, "Grupo deletado");
                        
                        // Disparar evento para atualização em tempo real
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.getPluginManager().callEvent(
                                new GroupPermissionsChangedEvent(groupId, groupName, "GROUP_DELETED"));
                            
                            sender.sendMessage(ChatColor.GREEN + "✅ Grupo '" + groupName + "' deletado com sucesso!");
                        });
                    }
                }
                
            } catch (SQLException e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao deletar grupo: " + e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    // ============================================================================
    // MÉTODOS AUXILIARES
    // ============================================================================

    private Integer getGroupId(Connection conn, String groupName) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT group_id FROM permission_groups WHERE group_name = ?")) {
            
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("group_id");
            }
        }
        return null;
    }

    private Integer getPlayerId(Connection conn, UUID playerUuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "SELECT player_id FROM player_data WHERE uuid = ?")) {
            
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("player_id");
            }
        }
        return null;
    }

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

    private Timestamp calculateExpiration(String duration) {
        // Implementar parsing de duração (ex: "7d", "30d", "1m", etc.)
        // Por enquanto, retorna null (permanente)
        return null;
    }

    private void logAction(Connection conn, String actionType, Player actor, Integer targetGroupId, 
                          Integer targetPlayerId, String permissionNode, String newValue, String reason) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
            "INSERT INTO permission_logs (action_type, actor_player_id, actor_name, target_group_id, target_player_id, " +
            "permission_node, new_value, reason, ip_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            
            stmt.setString(1, actionType);
            
            if (actor != null) {
                // Jogador executando o comando
                stmt.setInt(2, getPlayerId(conn, actor.getUniqueId()));
                stmt.setString(3, actor.getName());
                stmt.setString(9, actor.getAddress().getAddress().getHostAddress());
            } else {
                // Console executando o comando
                stmt.setNull(2, Types.INTEGER);
                stmt.setString(3, "Console");
                stmt.setString(9, "127.0.0.1");
            }
            
            stmt.setObject(4, targetGroupId);
            stmt.setObject(5, targetPlayerId);
            stmt.setObject(6, permissionNode);
            stmt.setObject(7, newValue);
            stmt.setString(8, reason);
            
            stmt.executeUpdate();
        }
    }
    
    // ============================================================================
    // CLASSE AUXILIAR PARA ARMAZENAR INFORMAÇÕES DOS GRUPOS
    // ============================================================================
    
    private static class GroupInfo {
        final int groupId;
        final String groupName;
        final String displayName;
        final String description;
        final int priority;
        final boolean isDefault;
        final java.sql.Timestamp createdAt;
        
        GroupInfo(int groupId, String groupName, String displayName, String description, 
                 int priority, boolean isDefault, java.sql.Timestamp createdAt) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.displayName = displayName;
            this.description = description;
            this.priority = priority;
            this.isDefault = isDefault;
            this.createdAt = createdAt;
        }
    }
}
