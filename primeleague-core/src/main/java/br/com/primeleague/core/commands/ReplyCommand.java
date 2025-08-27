package br.com.primeleague.core.commands;

import br.com.primeleague.core.managers.PrivateMessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para responder à última mensagem privada recebida.
 * Uso: /r <mensagem>
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ReplyCommand implements CommandExecutor {
    
    private final PrivateMessageManager privateMessageManager;
    
    public ReplyCommand(PrivateMessageManager privateMessageManager) {
        this.privateMessageManager = privateMessageManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se o sender é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar se há argumentos
        if (args.length < 1) {
            player.sendMessage("§eUso correto: /r <mensagem>");
            return true;
        }
        
        // Extrair mensagem
        String message = String.join(" ", args);
        
        // Verificar se a mensagem não está vazia
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // Enviar resposta
        boolean success = privateMessageManager.replyToLastMessage(player, message);
        
        return success;
    }
}
