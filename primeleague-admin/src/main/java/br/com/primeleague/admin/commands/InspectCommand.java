package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Punishment;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

/**
 * Comando /inspect - Inspeção completa de jogadores
 */
public class InspectCommand implements CommandExecutor {
    private final AdminManager adminManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public InspectCommand(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.inspect")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        // Verificar argumentos
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /inspect <jogador>");
            return true;
        }

        String targetName = args[0];

        // Verificar se o jogador alvo existe (pode estar offline)
        UUID targetUuid = null;
        Player target = Bukkit.getPlayer(targetName);
        if (target != null) {
            targetUuid = target.getUniqueId();
        } else {
            // Tentar obter UUID offline (implementação básica)
            targetUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes());
        }

        // Verificar se não está inspecionando a si mesmo
        if (sender instanceof Player && target != null && target.equals(sender)) {
            sender.sendMessage(ChatColor.RED + "Você não pode inspecionar a si mesmo!");
            return true;
        }

        // Exibir dossiê completo
        displayPlayerDossier(sender, targetName, targetUuid, target);

        // Log da ação
        if (sender instanceof Player) {
            logAction((Player) sender, targetName, "INSPECT");
        }

        return true;
    }

    /**
     * Exibe o dossiê completo do jogador.
     */
    private void displayPlayerDossier(CommandSender sender, String targetName, UUID targetUuid, Player target) {
        sender.sendMessage(ChatColor.GOLD + "=== DOSSIÊ: " + targetName + " ===");
        sender.sendMessage("");

        // Informações básicas
        displayBasicInfo(sender, targetName, targetUuid, target);
        sender.sendMessage("");

        // Informações do Core (perfil)
        displayCoreInfo(sender, targetUuid);
        sender.sendMessage("");

        // Informações P2P (assinatura)
        displayP2PInfo(sender, targetUuid);
        sender.sendMessage("");

        // Histórico de punições
        displayPunishmentHistory(sender, targetUuid);
        sender.sendMessage("");

        // Status de vanish
        displayVanishStatus(sender, targetUuid);
        sender.sendMessage("");

        // Permissões (se online)
        if (target != null) {
            displayPermissions(sender, target);
        }
    }

    /**
     * Exibe informações básicas do jogador.
     */
    private void displayBasicInfo(CommandSender sender, String targetName, UUID targetUuid, Player target) {
        sender.sendMessage(ChatColor.YELLOW + "=== INFORMAÇÕES BÁSICAS ===");
        sender.sendMessage(ChatColor.GRAY + "Nome: " + ChatColor.WHITE + targetName);
        sender.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + targetUuid.toString());
        sender.sendMessage(ChatColor.GRAY + "Status: " + (target != null ? ChatColor.GREEN + "ONLINE" : ChatColor.RED + "OFFLINE"));

        if (target != null) {
            sender.sendMessage(ChatColor.GRAY + "Vida: " + ChatColor.RED + (int)target.getHealth() + "/" + (int)target.getMaxHealth());
            sender.sendMessage(ChatColor.GRAY + "Fome: " + ChatColor.YELLOW + target.getFoodLevel() + "/20");
            sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.GREEN + target.getLevel());
            sender.sendMessage(ChatColor.GRAY + "Mundo: " + ChatColor.WHITE + target.getWorld().getName());
            sender.sendMessage(ChatColor.GRAY + "Localização: " + ChatColor.WHITE +
                "X:" + (int)target.getLocation().getX() +
                " Y:" + (int)target.getLocation().getY() +
                " Z:" + (int)target.getLocation().getZ());
        }
    }

    /**
     * Exibe informações do Core (perfil do jogador).
     */
    private void displayCoreInfo(CommandSender sender, UUID targetUuid) {
        sender.sendMessage(ChatColor.YELLOW + "=== INFORMAÇÕES DO CORE ===");

        try {
            PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(targetUuid);
            if (profile != null) {
                sender.sendMessage(ChatColor.GRAY + "Elo: " + ChatColor.WHITE + profile.getElo());
                sender.sendMessage(ChatColor.GRAY + "Money: " + ChatColor.GREEN + "$" + profile.getMoney());
                sender.sendMessage(ChatColor.GRAY + "Clan ID: " + ChatColor.WHITE +
                    (profile.getClanId() != null ? profile.getClanId().toString() : "Nenhum"));
                sender.sendMessage(ChatColor.GRAY + "Tempo total: " + ChatColor.WHITE +
                    formatPlaytime(profile.getTotalPlaytime()));
            } else {
                sender.sendMessage(ChatColor.RED + "Perfil não encontrado no Core.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao obter informações do Core: " + e.getMessage());
        }
    }

    /**
     * Exibe informações P2P do jogador.
     */
    private void displayP2PInfo(CommandSender sender, UUID targetUuid) {
        sender.sendMessage(ChatColor.YELLOW + "=== INFORMAÇÕES P2P ===");

        try {
            // Verificar se o jogador tem acesso P2P ativo
            Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(targetUuid);
            if (playerId != null) {
                // REFATORADO: Usar método assíncrono para verificar tier de doador
                sender.sendMessage(ChatColor.YELLOW + "⏳ Verificando status da assinatura...");
                
                PrimeLeagueAPI.getDataManager().getDonorTierAsync(targetUuid, (donorTier) -> {
                    // HARDENING: Verificar se o sender ainda é válido antes de enviar mensagens
                    if (!(sender instanceof org.bukkit.entity.Player) || !((org.bukkit.entity.Player) sender).isOnline()) {
                        return; // Sender não é mais válido, abortar callback
                    }
                    
                    if (donorTier != null && donorTier > 0) {
                        sender.sendMessage(ChatColor.GRAY + "Status da assinatura: " + ChatColor.GREEN + "ATIVA");
                        sender.sendMessage(ChatColor.GRAY + "Tier de doador: " + ChatColor.WHITE + donorTier);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Sem assinatura P2P ativa.");
                    }
                });
            } else {
                sender.sendMessage(ChatColor.RED + "Jogador não encontrado no sistema.");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao obter informações P2P: " + e.getMessage());
        }
    }

    /**
     * Exibe histórico de punições.
     */
    private void displayPunishmentHistory(CommandSender sender, UUID targetUuid) {
        sender.sendMessage(ChatColor.YELLOW + "=== HISTÓRICO DE PUNIÇÕES ===");

        try {
            List<Punishment> history = adminManager.getPlayerHistory(targetUuid);

            if (history.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Nenhuma punição registrada.");
                return;
            }

            // Contar punições por tipo
            int warns = 0, kicks = 0, mutes = 0, bans = 0;
            int activeWarns = 0, activeMutes = 0, activeBans = 0;

            for (Punishment punishment : history) {
                switch (punishment.getType()) {
                    case WARN:
                        warns++;
                        if (punishment.isActive()) activeWarns++;
                        break;
                    case KICK: kicks++; break;
                    case MUTE:
                        mutes++;
                        if (punishment.isActive()) activeMutes++;
                        break;
                    case BAN:
                        bans++;
                        if (punishment.isActive()) activeBans++;
                        break;
                }
            }

            sender.sendMessage(ChatColor.GRAY + "Total de punições: " + ChatColor.WHITE + history.size());
            sender.sendMessage(ChatColor.GRAY + "Avisos: " + ChatColor.YELLOW + warns +
                (activeWarns > 0 ? ChatColor.RED + " (" + activeWarns + " ativos)" : ""));
            sender.sendMessage(ChatColor.GRAY + "Expulsões: " + ChatColor.GOLD + kicks);
            sender.sendMessage(ChatColor.GRAY + "Mutes: " + ChatColor.RED + mutes +
                (activeMutes > 0 ? ChatColor.RED + " (" + activeMutes + " ativos)" : ""));
            sender.sendMessage(ChatColor.GRAY + "Bans: " + ChatColor.DARK_RED + bans +
                (activeBans > 0 ? ChatColor.RED + " (" + activeBans + " ativos)" : ""));

            // Mostrar punições ativas
            if (activeWarns > 0 || activeMutes > 0 || activeBans > 0) {
                sender.sendMessage(ChatColor.RED + "PUNIÇÕES ATIVAS:");
                for (Punishment punishment : history) {
                    if (punishment.isActive()) {
                        String statusColor = punishment.getType() == Punishment.Type.WARN ?
                            ChatColor.YELLOW.toString() : ChatColor.RED.toString();
                        sender.sendMessage(statusColor + "  - " + punishment.getType().name() +
                            ": " + punishment.getReason());
                    }
                }
            }

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao obter histórico: " + e.getMessage());
        }
    }

    /**
     * Exibe status de vanish.
     */
    private void displayVanishStatus(CommandSender sender, UUID targetUuid) {
        sender.sendMessage(ChatColor.YELLOW + "=== STATUS DE VANISH ===");

        boolean isVanished = adminManager.isVanished(targetUuid);
        String status = isVanished ? ChatColor.GREEN + "ATIVO" : ChatColor.RED + "INATIVO";

        sender.sendMessage(ChatColor.GRAY + "Vanish: " + status);
    }

    /**
     * Exibe permissões do jogador.
     */
    private void displayPermissions(CommandSender sender, Player target) {
        sender.sendMessage(ChatColor.YELLOW + "=== PERMISSÕES ===");

        String permissions = "";
        if (PrimeLeagueAPI.hasPermission(target, "primeleague.admin")) {
            permissions += ChatColor.RED + "Admin\n";
        }
        if (PrimeLeagueAPI.hasPermission(target, "primeleague.mod")) {
            permissions += ChatColor.YELLOW + "Moderador\n";
        }
        if (PrimeLeagueAPI.hasPermission(target, "primeleague.helper")) {
            permissions += ChatColor.BLUE + "Helper\n";
        }
        if (PrimeLeagueAPI.hasPermission(target, "primeleague.vip")) {
            permissions += ChatColor.GOLD + "VIP\n";
        }

        if (!permissions.isEmpty()) {
            sender.sendMessage(permissions.trim());
        } else {
            sender.sendMessage(ChatColor.GRAY + "Nenhuma permissão especial.");
        }
    }

    /**
     * Formata o tempo de jogo em horas e minutos.
     */
    private String formatPlaytime(long playtimeMillis) {
        long hours = playtimeMillis / (1000 * 60 * 60);
        long minutes = (playtimeMillis % (1000 * 60 * 60)) / (1000 * 60);
        return hours + "h " + minutes + "m";
    }

    /**
     * Registra a ação no log.
     */
    private void logAction(Player author, String targetName, String action) {
        String logMessage = String.format("[ADMIN] %s usou %s em %s",
            author.getName(), action, targetName);



        // Notificar outros staffs
        String notification = ChatColor.YELLOW + "[ADMIN] " + ChatColor.WHITE +
            author.getName() + " usou " + action + " em " + targetName;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PrimeLeagueAPI.hasPermission(player, "primeleague.admin.notifications") &&
                !player.equals(author)) {
                player.sendMessage(notification);
            }
        }
    }
}
