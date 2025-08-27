package br.com.primeleague.core.commands;

import br.com.primeleague.core.managers.PrivateMessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para enviar mensagens privadas.
 * Uso: /msg <jogador> <mensagem>
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class PrivateMessageCommand implements CommandExecutor {
    
    private final PrivateMessageManager privateMessageManager;
    
    public PrivateMessageCommand(PrivateMessageManager privateMessageManager) {
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
        
        // Verificar argumentos mínimos
        if (args.length < 2) {
            player.sendMessage("§eUso correto: /msg <jogador> <mensagem>");
            return true;
        }
        
        // Extrair nome do jogador alvo e mensagem
        String targetName = args[0];
        String message = String.join(" ", args).substring(targetName.length() + 1);
        
        // Verificar se a mensagem não está vazia
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // Enviar mensagem privada
        boolean success = privateMessageManager.sendPrivateMessage(player, targetName, message);
        
        return success;
    }
}
