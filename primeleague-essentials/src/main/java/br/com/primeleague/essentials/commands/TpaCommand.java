package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.TpaManager;
import br.com.primeleague.essentials.models.TeleportRequest;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando principal para o sistema de teletransporte entre jogadores.
 * Gerencia todas as operações de TPA.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class TpaCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final TpaManager tpaManager;
    
    /**
     * Construtor do TpaCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param tpaManager Instância do TpaManager
     */
    public TpaCommand(EssentialsPlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "tpa":
                return handleTpa(player, args);
            case "tpahere":
                return handleTpaHere(player, args);
            case "tpaccept":
            case "tpyes":
                return handleTpAccept(player, args);
            case "tpdeny":
            case "tpno":
                return handleTpDeny(player, args);
            case "tpcancel":
                return handleTpCancel(player, args);
            case "tpalist":
                return handleTpaList(player, args);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Manipula o comando /tpa.
     */
    private boolean handleTpa(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUso: /tpa <jogador>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage("§cVocê não pode solicitar teletransporte para si mesmo!");
            return true;
        }
        
        // Enviar solicitação
        boolean success = tpaManager.sendTeleportRequest(player, target, TeleportRequest.RequestType.TPA);
        
        if (success) {
            player.sendMessage("§aSolicitação de teletransporte enviada para " + target.getName() + "!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /tpahere.
     */
    private boolean handleTpaHere(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUso: /tpahere <jogador>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado!");
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage("§cVocê não pode solicitar teletransporte para si mesmo!");
            return true;
        }
        
        // Enviar solicitação
        boolean success = tpaManager.sendTeleportRequest(player, target, TeleportRequest.RequestType.TPAHERE);
        
        if (success) {
            player.sendMessage("§aSolicitação de teletransporte enviada para " + target.getName() + "!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /tpaccept.
     */
    private boolean handleTpAccept(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        boolean success = tpaManager.acceptTeleportRequest(player);
        
        if (success) {
            player.sendMessage("§aVocê aceitou a solicitação de teletransporte!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /tpdeny.
     */
    private boolean handleTpDeny(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        boolean success = tpaManager.denyTeleportRequest(player);
        
        if (success) {
            player.sendMessage("§aVocê negou a solicitação de teletransporte!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /tpcancel.
     */
    private boolean handleTpCancel(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        boolean success = tpaManager.cancelTeleportRequest(player);
        
        if (success) {
            player.sendMessage("§aVocê cancelou a solicitação de teletransporte!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /tpalist.
     */
    private boolean handleTpaList(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.tpa")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        String result = tpaManager.listPendingRequests(player);
        player.sendMessage(result);
        
        return true;
    }
    
    /**
     * Envia a mensagem de ajuda para o jogador.
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6=== §eSistema de Teletransporte §6===");
        player.sendMessage("§e/tpa <jogador> §7- Solicita teletransporte para um jogador");
        player.sendMessage("§e/tpahere <jogador> §7- Solicita que um jogador venha até você");
        player.sendMessage("§e/tpaccept §7- Aceita uma solicitação de teletransporte");
        player.sendMessage("§e/tpdeny §7- Nega uma solicitação de teletransporte");
        player.sendMessage("§e/tpcancel §7- Cancela uma solicitação enviada");
        player.sendMessage("§e/tpalist §7- Lista suas solicitações pendentes");
        player.sendMessage("§6================================");
    }
}
