package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Ticket;
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
 * Comando /tickets - Gerenciamento de tickets
 */
public class TicketsCommand implements CommandExecutor {
    private final AdminManager adminManager;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public TicketsCommand(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("primeleague.admin.tickets")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleList(sender, args);
                break;
            case "view":
                handleView(sender, args);
                break;
            case "claim":
                handleClaim(sender, args);
                break;
            case "close":
                handleClose(sender, args);
                break;
            default:
                showHelp(sender);
                break;
        }

        return true;
    }

    /**
     * Exibe a ajuda do comando.
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos de Tickets ===");
        sender.sendMessage(ChatColor.YELLOW + "/tickets list [open] - Lista tickets");
        sender.sendMessage(ChatColor.YELLOW + "/tickets view <id> - Visualiza ticket");
        sender.sendMessage(ChatColor.YELLOW + "/tickets claim <id> - Reivindica ticket");
        sender.sendMessage(ChatColor.YELLOW + "/tickets close <id> <guilty/innocent> [notas] - Fecha ticket");
    }

    /**
     * Manipula o subcomando list.
     */
    private void handleList(CommandSender sender, String[] args) {
        Ticket.Status status = null;
        if (args.length > 1 && args[1].equalsIgnoreCase("open")) {
            status = Ticket.Status.OPEN;
        }

        List<Ticket> tickets = adminManager.getTickets(status, 10, 0);

        if (tickets.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "Nenhum ticket encontrado.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Lista de Tickets ===");
        for (Ticket ticket : tickets) {
            displayTicketSummary(sender, ticket);
        }
    }

    /**
     * Manipula o subcomando view.
     */
    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /tickets view <id>");
            return;
        }

        try {
            int ticketId = Integer.parseInt(args[1]);
            Ticket ticket = adminManager.getTicket(ticketId);

            if (ticket == null) {
                sender.sendMessage(ChatColor.RED + "Ticket #" + ticketId + " não encontrado!");
                return;
            }

            displayTicketDetails(sender, ticket);

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "ID inválido!");
        }
    }

    /**
     * Manipula o subcomando claim.
     */
    private void handleClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Uso: /tickets claim <id>");
            return;
        }

        try {
            int ticketId = Integer.parseInt(args[1]);
            Player player = (Player) sender;

            if (adminManager.claimTicket(ticketId, player.getUniqueId())) {
                sender.sendMessage(ChatColor.GREEN + "Ticket #" + ticketId + " reivindicado com sucesso!");
            } else {
                sender.sendMessage(ChatColor.RED + "Erro ao reivindicar ticket #" + ticketId + "!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "ID inválido!");
        }
    }

    /**
     * Manipula o subcomando close.
     */
    private void handleClose(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Uso: /tickets close <id> <guilty/innocent> [notas]");
            return;
        }

        try {
            int ticketId = Integer.parseInt(args[1]);
            String resolution = args[2].toLowerCase();

            if (!resolution.equals("guilty") && !resolution.equals("innocent")) {
                sender.sendMessage(ChatColor.RED + "Resolução deve ser 'guilty' ou 'innocent'!");
                return;
            }

            String notes = "";
            if (args.length > 3) {
                notes = String.join(" ", args).substring(args[0].length() + args[1].length() + args[2].length() + 3);
            }

            Ticket.Status finalStatus = resolution.equals("guilty") ?
                Ticket.Status.CLOSED_GUILTY : Ticket.Status.CLOSED_INNOCENT;

            String resolutionNotes = "Resolução: " + resolution +
                (notes.isEmpty() ? "" : " | Notas: " + notes);

            if (adminManager.closeTicket(ticketId, finalStatus, resolutionNotes)) {
                sender.sendMessage(ChatColor.GREEN + "Ticket #" + ticketId + " fechado com sucesso!");
            } else {
                sender.sendMessage(ChatColor.RED + "Erro ao fechar ticket #" + ticketId + "!");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "ID inválido!");
        }
    }

    /**
     * Exibe um resumo de um ticket.
     */
    private void displayTicketSummary(CommandSender sender, Ticket ticket) {
        ChatColor statusColor = getStatusColor(ticket.getStatus());

        // Usar nomes se disponíveis, senão UUIDs
        String reporterName = ticket.getReporterName() != null ?
            ticket.getReporterName() : getPlayerName(ticket.getReporterUuid());
        String targetName = ticket.getTargetName() != null ?
            ticket.getTargetName() : getPlayerName(ticket.getTargetUuid());

        String statusColorStr = statusColor.toString();
        sender.sendMessage(ChatColor.YELLOW + "#" + ticket.getId() + " " +
            statusColorStr + "[" + ticket.getStatus().name() + "] " +
            ChatColor.WHITE + reporterName + " → " + targetName);

        if (ticket.getCreatedAt() != null) {
            sender.sendMessage(ChatColor.GRAY + "  Criado: " + dateFormat.format(ticket.getCreatedAt()));
        }
    }

    /**
     * Exibe detalhes completos de um ticket.
     */
    private void displayTicketDetails(CommandSender sender, Ticket ticket) {
        sender.sendMessage(ChatColor.GOLD + "=== Ticket #" + ticket.getId() + " ===");

        ChatColor statusColor = getStatusColor(ticket.getStatus());
        String statusColorStr = statusColor.toString();
        sender.sendMessage(ChatColor.GRAY + "Status: " + statusColorStr + ticket.getStatus().name());

        // Usar nomes se disponíveis, senão UUIDs
        String reporterName = ticket.getReporterName() != null ?
            ticket.getReporterName() : getPlayerName(ticket.getReporterUuid());
        String targetName = ticket.getTargetName() != null ?
            ticket.getTargetName() : getPlayerName(ticket.getTargetUuid());

        sender.sendMessage(ChatColor.GRAY + "Reportado por: " + ChatColor.WHITE + reporterName);
        sender.sendMessage(ChatColor.GRAY + "Alvo: " + ChatColor.WHITE + targetName);
        sender.sendMessage(ChatColor.GRAY + "Motivo: " + ChatColor.WHITE + ticket.getReason());

        if (ticket.getEvidenceLink() != null && !ticket.getEvidenceLink().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Prova: " + ChatColor.WHITE + ticket.getEvidenceLink());
        }

        if (ticket.getCreatedAt() != null) {
            sender.sendMessage(ChatColor.GRAY + "Criado: " + ChatColor.WHITE +
                dateFormat.format(ticket.getCreatedAt()));
        }

        if (ticket.getClaimedByUuid() != null) {
            String claimedByName = ticket.getClaimedByName() != null ?
                ticket.getClaimedByName() : getPlayerName(ticket.getClaimedByUuid());
            sender.sendMessage(ChatColor.GRAY + "Reivindicado por: " + ChatColor.WHITE + claimedByName);
        }

        if (ticket.getResolutionNotes() != null && !ticket.getResolutionNotes().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "Resolução: " + ChatColor.WHITE + ticket.getResolutionNotes());
        }

        if (ticket.getUpdatedAt() != null) {
            sender.sendMessage(ChatColor.GRAY + "Atualizado: " + ChatColor.WHITE +
                dateFormat.format(ticket.getUpdatedAt()));
        }
    }

    /**
     * Retorna a cor apropriada para cada status.
     */
    private ChatColor getStatusColor(Ticket.Status status) {
        switch (status) {
            case OPEN: return ChatColor.YELLOW;
            case IN_PROGRESS: return ChatColor.BLUE;
            case CLOSED_GUILTY: return ChatColor.RED;
            case CLOSED_INNOCENT: return ChatColor.GREEN;
            default: return ChatColor.WHITE;
        }
    }

    /**
     * Tenta obter o nome de um jogador pelo UUID.
     */
    private String getPlayerName(UUID uuid) {
        return adminManager.getPlayerName(uuid);
    }
}
