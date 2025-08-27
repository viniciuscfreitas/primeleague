package br.com.primeleague.p2p.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Comando administrativo para gerenciar assinaturas de jogadores.
 * 
 * Comandos disponíveis:
 * - /pl grant <player> <days> - Concede dias de acesso
 * - /pl revoke <player> - Revoga o acesso
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public final class PrimeAdminCommand implements CommandExecutor {

    private final PrimeLeagueP2P plugin;

    public PrimeAdminCommand() {
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin")) {
            sender.sendMessage("§cVocê não tem permissão para usar este comando.");
            return true;
        }

        // Verificar argumentos mínimos
        if (args.length < 2) {
            sendUsage(sender);
            return false;
        }

        String subCommand = args[0].toLowerCase();
        String playerName = args[1];

        // Buscar o perfil do jogador (online ou offline)
        PlayerProfile profile = PrimeLeagueAPI.getProfileByName(playerName);

        if (profile == null) {
            sender.sendMessage("§cJogador '" + playerName + "' não encontrado em nosso banco de dados.");
            return true;
        }

        // Executar o comando apropriado
        switch (subCommand) {
            case "grant":
                return handleGrant(sender, profile, args);
            case "revoke":
                return handleRevoke(sender, profile);
            default:
                sendUsage(sender);
                return false;
        }
    }

    /**
     * Processa o comando /pl grant.
     */
    private boolean handleGrant(CommandSender sender, PlayerProfile profile, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUso: /pl grant <player> <days>");
            return false;
        }

        try {
            int days = Integer.parseInt(args[2]);
            if (days <= 0) {
                sender.sendMessage("§cO número de dias deve ser maior que zero.");
                return false;
            }

            // Adicionar dias de assinatura
            boolean success = PrimeLeagueAPI.getDataManager().addSubscriptionDays(profile.getUuid(), days);

            if (success) {
                sender.sendMessage("§aConcedido " + days + " dias de acesso para " + profile.getPlayerName());
                
                // Log da ação
                plugin.getLogger().info("Admin " + sender.getName() + " concedeu " + days + 
                                      " dias de acesso para " + profile.getPlayerName() + " (UUID: " + profile.getUuid() + ")");
            } else {
                sender.sendMessage("§cFalha ao conceder acesso. Verifique os logs para mais detalhes.");
            }

        } catch (NumberFormatException e) {
            sender.sendMessage("§cO número de dias deve ser um número válido.");
            return false;
        }

        return true;
    }

    /**
     * Processa o comando /pl revoke.
     */
    private boolean handleRevoke(CommandSender sender, PlayerProfile profile) {
        // Definir a data de expiração para o passado
        Timestamp pastDate = new Timestamp(System.currentTimeMillis() - 1000);
        boolean success = PrimeLeagueAPI.getDataManager().updateSubscriptionExpiry(profile.getUuid(), pastDate);

        if (success) {
            sender.sendMessage("§cO acesso de " + profile.getPlayerName() + " foi revogado.");
            
            // Log da ação
            plugin.getLogger().info("Admin " + sender.getName() + " revogou o acesso de " + 
                                  profile.getPlayerName() + " (UUID: " + profile.getUuid() + ")");
        } else {
            sender.sendMessage("§cFalha ao revogar acesso. Verifique os logs para mais detalhes.");
        }

        return true;
    }

    /**
     * Envia a mensagem de uso do comando.
     */
    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6=== Comandos Prime League P2P ===");
        sender.sendMessage("§e/pl grant <player> <days> §7- Concede dias de acesso");
        sender.sendMessage("§e/pl revoke <player> §7- Revoga o acesso");
        sender.sendMessage("§7Exemplo: §e/pl grant Steve 30");
    }
}
