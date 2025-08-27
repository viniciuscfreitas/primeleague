package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelManager.ChatChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para chat de aliança (versão simplificada para teste).
 */
public class AllyChatCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final ChannelManager channelManager;
    
    public AllyChatCommand(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.channelManager = plugin.getChannelManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("primeleague.chat.ally")) {
            player.sendMessage("§cVocê não tem permissão para usar o chat de aliança.");
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage("§eUso correto: /a <mensagem>");
            return true;
        }
        
        String message = String.join(" ", args);
        
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        channelManager.setPlayerChannel(player, ChatChannel.ALLY);
        
        String formattedMessage = channelManager.formatAllyMessage(player, message);
        player.sendMessage(formattedMessage);
        
        plugin.getLoggingService().logMessage("ALLY", player, null, message);
        
        return true;
    }
}
