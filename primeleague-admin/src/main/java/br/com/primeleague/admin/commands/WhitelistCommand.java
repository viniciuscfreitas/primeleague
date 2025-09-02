package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.managers.WhitelistManager;
import br.com.primeleague.core.util.UUIDUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * Comando para gerenciar a whitelist centralizada V2.
 * 
 * Uso:
 * /whitelist add <jogador> <motivo> - Adiciona jogador à whitelist
 * /whitelist remove <jogador> <motivo> - Remove jogador da whitelist
 * /whitelist list [página] - Lista jogadores na whitelist
 * /whitelist stats - Mostra estatísticas da whitelist
 * /whitelist reload - Recarrega cache da whitelist
 * 
 * @author PrimeLeague Team
 * @version 2.0.0
 */
public class WhitelistCommand implements CommandExecutor {

    private final PrimeLeagueAdmin plugin;
    private static final int ITEMS_PER_PAGE = 10;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public WhitelistCommand(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.whitelist")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /whitelist add <jogador> <motivo>");
                    return true;
                }
                addToWhitelist(sender, args[1], joinArgs(args, 2));
                break;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /whitelist remove <jogador> <motivo>");
                    return true;
                }
                removeFromWhitelist(sender, args[1], joinArgs(args, 2));
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
                listWhitelist(sender, page);
                break;

            case "stats":
                showStats(sender);
                break;

            case "reload":
                reloadCache(sender);
                break;

            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    private void addToWhitelist(final CommandSender sender, final String targetName, final String reason) {
        // Executar operação de forma assíncrona para não travar a thread principal
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UUID authorUuid = getAuthorUuid(sender);
                    String authorName = getAuthorName(sender);

                    // PRIMEIRA ETAPA: Obter UUID do jogador usando a API do Core
                    UUID targetUuid = PrimeLeagueAPI.getPlayerUUID(targetName);
                    if (targetUuid == null) {
                        sender.sendMessage(ChatColor.RED + "Jogador " + ChatColor.WHITE + targetName + 
                                         ChatColor.RED + " não encontrado no banco de dados.");
                        return;
                    }

                    // SEGUNDA ETAPA: Verificar se o jogador já está na whitelist (usando UUID)
                    if (PrimeLeagueAPI.isWhitelisted(targetUuid)) {
                        sender.sendMessage(ChatColor.YELLOW + "O jogador " + ChatColor.WHITE + targetName + 
                                         ChatColor.YELLOW + " já está na whitelist.");
                        return;
                    }

                    // TERCEIRA ETAPA: Adicionar à whitelist (usando UUID como fonte única da verdade)
                    boolean success = PrimeLeagueAPI.addToWhitelist(targetUuid, targetName, authorUuid, authorName, reason);

                    if (success) {
                        sender.sendMessage(ChatColor.GREEN + "Jogador " + ChatColor.WHITE + targetName + 
                                          ChatColor.GREEN + " adicionado à whitelist com sucesso!");
                        sender.sendMessage(ChatColor.GRAY + "Motivo: " + reason);
                        
                        plugin.getLogger().info(String.format(
                            "WHITELIST: %s adicionou %s (UUID: %s) à whitelist. Motivo: %s",
                            authorName, targetName, targetUuid, reason
                        ));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Erro ao adicionar jogador à whitelist.");
                    }

                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Erro ao processar comando: " + e.getMessage());
                    plugin.getLogger().severe("Erro no comando whitelist add: " + e.getMessage());
                }
            }
        };
        task.runTaskAsynchronously(plugin);
    }

    private void removeFromWhitelist(final CommandSender sender, final String targetName, final String reason) {
        // Executar operação de forma assíncrona para não travar a thread principal
        org.bukkit.scheduler.BukkitRunnable task = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UUID authorUuid = getAuthorUuid(sender);
                    String authorName = getAuthorName(sender);

                    // PRIMEIRA ETAPA: Obter UUID do jogador usando a API do Core
                    UUID targetUuid = PrimeLeagueAPI.getPlayerUUID(targetName);
                    if (targetUuid == null) {
                        sender.sendMessage(ChatColor.RED + "Jogador " + ChatColor.WHITE + targetName + 
                                         ChatColor.RED + " não encontrado no banco de dados.");
                        return;
                    }

                    // SEGUNDA ETAPA: Verificar se o jogador está na whitelist (usando UUID)
                    if (!PrimeLeagueAPI.isWhitelisted(targetUuid)) {
                        sender.sendMessage(ChatColor.YELLOW + "O jogador " + ChatColor.WHITE + targetName + 
                                         ChatColor.YELLOW + " não está na whitelist.");
                        return;
                    }

                    // TERCEIRA ETAPA: Remover da whitelist (usando UUID como fonte única da verdade)
                    boolean success = PrimeLeagueAPI.removeFromWhitelist(targetUuid, targetName, authorUuid, authorName, reason);

                    if (success) {
                        sender.sendMessage(ChatColor.GREEN + "Jogador " + ChatColor.WHITE + targetName + 
                                          ChatColor.GREEN + " removido da whitelist com sucesso!");
                        sender.sendMessage(ChatColor.GRAY + "Motivo: " + reason);
                        
                        plugin.getLogger().info(String.format(
                            "WHITELIST: %s removeu %s (UUID: %s) da whitelist. Motivo: %s",
                            authorName, targetName, targetUuid, reason
                        ));
                    } else {
                        sender.sendMessage(ChatColor.RED + "Erro ao remover jogador da whitelist.");
                    }

                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "Erro ao processar comando: " + e.getMessage());
                    plugin.getLogger().severe("Erro no comando whitelist remove: " + e.getMessage());
                }
            }
        };
        task.runTaskAsynchronously(plugin);
    }

    private void listWhitelist(CommandSender sender, int page) {
        try {
            List<WhitelistManager.WhitelistEntry> entries = PrimeLeagueAPI.getWhitelistedPlayers();
            
            if (entries.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "A whitelist está vazia.");
                return;
            }

            int totalPages = (int) Math.ceil((double) entries.size() / ITEMS_PER_PAGE);
            if (page > totalPages) page = totalPages;

            int startIndex = (page - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

            sender.sendMessage(ChatColor.GOLD + "=== Whitelist V2 - Página " + page + "/" + totalPages + " ===");
            
            for (int i = startIndex; i < endIndex; i++) {
                WhitelistManager.WhitelistEntry entry = entries.get(i);
                
                sender.sendMessage(ChatColor.WHITE + "• " + entry.getPlayerName() + 
                                 ChatColor.GRAY + " (adicionado por " + entry.getAddedByName() + ")");
                sender.sendMessage(ChatColor.GRAY + "  Motivo: " + entry.getReason());
            }

            sender.sendMessage(ChatColor.GOLD + "Total: " + ChatColor.WHITE + entries.size() + " jogador(es)");

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao listar whitelist: " + e.getMessage());
            plugin.getLogger().severe("Erro no comando whitelist list: " + e.getMessage());
        }
    }

    private void showStats(CommandSender sender) {
        try {
            WhitelistManager.WhitelistStats stats = PrimeLeagueAPI.getWhitelistStats();
            
            sender.sendMessage(ChatColor.GOLD + "=== Estatísticas da Whitelist V2 ===");
            sender.sendMessage(ChatColor.WHITE + "Jogadores ativos: " + ChatColor.GREEN + stats.getActivePlayers());
            sender.sendMessage(ChatColor.WHITE + "Total de entradas: " + ChatColor.YELLOW + stats.getTotalEntries());
            sender.sendMessage(ChatColor.WHITE + "Jogadores removidos: " + ChatColor.RED + stats.getRemovedPlayers());
            sender.sendMessage(ChatColor.WHITE + "Admins únicos: " + ChatColor.BLUE + stats.getUniqueAdmins());
            
            if (stats.getFirstAddition() != null) {
                sender.sendMessage(ChatColor.WHITE + "Primeira adição: " + ChatColor.GRAY + DATE_FORMAT.format(stats.getFirstAddition()));
            }
            if (stats.getLastAddition() != null) {
                sender.sendMessage(ChatColor.WHITE + "Última adição: " + ChatColor.GRAY + DATE_FORMAT.format(stats.getLastAddition()));
            }

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao buscar estatísticas: " + e.getMessage());
            plugin.getLogger().severe("Erro no comando whitelist stats: " + e.getMessage());
        }
    }

    private void reloadCache(CommandSender sender) {
        try {
            PrimeLeagueAPI.reloadWhitelistCache();
            sender.sendMessage(ChatColor.GREEN + "Cache da whitelist recarregado com sucesso!");
            
            plugin.getLogger().info("WHITELIST: Cache recarregado por " + getAuthorName(sender));
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao recarregar cache: " + e.getMessage());
            plugin.getLogger().severe("Erro no comando whitelist reload: " + e.getMessage());
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos da Whitelist V2 ===");
        sender.sendMessage(ChatColor.WHITE + "/whitelist add <jogador> <motivo> " + ChatColor.GRAY + "- Adiciona jogador");
        sender.sendMessage(ChatColor.WHITE + "/whitelist remove <jogador> <motivo> " + ChatColor.GRAY + "- Remove jogador");
        sender.sendMessage(ChatColor.WHITE + "/whitelist list [página] " + ChatColor.GRAY + "- Lista jogadores");
        sender.sendMessage(ChatColor.WHITE + "/whitelist stats " + ChatColor.GRAY + "- Mostra estatísticas");
        sender.sendMessage(ChatColor.WHITE + "/whitelist reload " + ChatColor.GRAY + "- Recarrega cache");
    }

    private UUID getAuthorUuid(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        // UUID do console
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    private String getAuthorName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getName();
        }
        return "CONSOLE";
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
