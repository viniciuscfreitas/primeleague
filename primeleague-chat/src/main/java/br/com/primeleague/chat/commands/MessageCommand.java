package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.PrivateMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para enviar mensagens privadas entre jogadores.
 * Suporta os aliases: /msg, /tell, /w
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class MessageCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final PrivateMessageService privateMessageService;
    
    public MessageCommand(PrimeLeagueChat plugin) {
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
        
        if (args.length < 2) {
            player.sendMessage("§cUso correto: /" + label + " <jogador> <mensagem>");
            player.sendMessage("§7💡 Exemplo: /" + label + " João Olá, como você está?");
            return true;
        }
        
        String targetName = args[0];
        String message = String.join(" ", args).substring(targetName.length() + 1);
        
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // Enviar mensagem privada
        boolean success = privateMessageService.sendPrivateMessage(player, targetName, message);
        
        return success;
    }
}
