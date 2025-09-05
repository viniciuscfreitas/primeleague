package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.api.models.Ticket;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Comando /report - Permite aos jogadores reportar outros jogadores.
 */
public class ReportCommand implements CommandExecutor {

    private final AdminManager adminManager;

    public ReportCommand(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        Player reporter = (Player) sender;

        if (!PrimeLeagueAPI.hasPermission(reporter, "primeleague.report")) {
            reporter.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        // Verificar argumentos
        if (args.length < 2) {
            reporter.sendMessage(ChatColor.RED + "Uso: /report <jogador> <motivo> [prova]");
            return true;
        }

        String targetName = args[0];
        String reason = args[1];
        String evidenceLink = args.length > 2 ? args[2] : null;

        // Verificar se o jogador alvo existe
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            reporter.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado!");
            return true;
        }

        // Verificar se não está reportando a si mesmo
        if (target.equals(reporter)) {
            reporter.sendMessage(ChatColor.RED + "Você não pode reportar a si mesmo!");
            return true;
        }

        // Verificar se o alvo tem permissão de bypass
        if (PrimeLeagueAPI.hasPermission(target, "primeleague.admin.report.bypass")) {
            reporter.sendMessage(ChatColor.RED + "Você não pode reportar membros da equipe!");
            return true;
        }

        // Obter UUIDs - Em Bukkit 1.5.2, usar UUID offline baseado no nome
        UUID reporterUuid;
        UUID targetUuid;
        try {
            reporterUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + reporter.getName()).getBytes("UTF-8"));
            targetUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + target.getName()).getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            reporter.sendMessage(ChatColor.RED + "Erro interno ao processar comando!");
            return true;
        }

        // Criar ticket
        Ticket ticket = new Ticket(reporterUuid, targetUuid, reason, evidenceLink);

        if (adminManager.createTicket(ticket)) {
            // Mensagem de sucesso para o reporter
            String message = ChatColor.GREEN + "Denúncia #" + ticket.getId() + " criada com sucesso. Nossa equipe irá analisar.";
            reporter.sendMessage(message);

            // Notificar staff online
            notifyStaff(ticket);

        } else {
            reporter.sendMessage(ChatColor.RED + "Erro ao criar denúncia. Tente novamente mais tarde.");
        }

        return true;
    }

    /**
     * Notifica staff online sobre um novo ticket.
     */
    private void notifyStaff(Ticket ticket) {
        String notification = ChatColor.YELLOW + "[REPORT] " + ChatColor.WHITE +
            "Nova denúncia #" + ticket.getId() + " criada por " +
            getPlayerName(ticket.getReporterUuid()) + " contra " +
            getPlayerName(ticket.getTargetUuid()) + ": " + ticket.getReason();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PrimeLeagueAPI.hasPermission(player, "primeleague.admin.tickets")) {
                player.sendMessage(notification);
            }
        }
    }

    /**
     * Obtém o nome de um jogador pelo UUID.
     */
    private String getPlayerName(UUID uuid) {
        // Em Bukkit 1.5.2, não há getPlayer(UUID), tentar por nome
        // Como não temos o nome, tentar obter do perfil
        PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(uuid);
        if (profile != null) {
            return profile.getPlayerName();
        }

        return "Desconhecido";
    }
}
