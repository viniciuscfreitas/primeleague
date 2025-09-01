package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.PrivateMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para responder à última mensagem privada recebida.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ReplyCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final PrivateMessageService privateMessageService;
    
    public ReplyCommand(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.privateMessageService = plugin.getPrivateMessageService();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /" + label + " <mensagem>");
            player.sendMessage("§7💡 Exemplo: /" + label + " Obrigado pela mensagem!");
            return true;
        }
        
        String message = String.join(" ", args);
        
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // Responder à última mensagem privada
        boolean success = privateMessageService.replyToLastMessage(player, message);
        
        return success;
    }
}
