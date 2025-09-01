package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelManager.ChatChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando geral para gerenciar canais de chat (versão simplificada para teste).
 */
public class ChatCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final ChannelManager channelManager;
    
    public ChatCommand(PrimeLeagueChat plugin) {
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
        
        if (args.length < 1) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
            case "h":
                showHelp(player);
                break;
            default:
                player.sendMessage("§cSubcomando desconhecido. Use /chat help para ver as opções.");
                player.sendMessage("§7💡 Dica: Use /g, /c ou /a para enviar mensagens rápidas.");
                break;
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6=== Sistema de Chat - Local é Rei ===");
        player.sendMessage("§a📝 Chat Local: §7Digite normalmente no chat (padrão)");
        player.sendMessage("");
        player.sendMessage("§6=== Comandos de Envio Rápido ===");
        player.sendMessage("§e/g <mensagem> §7- Enviar mensagem única para Global");
        player.sendMessage("§e/c <mensagem> §7- Enviar mensagem única para Clã");
        player.sendMessage("§e/a <mensagem> §7- Enviar mensagem única para Aliança");
        player.sendMessage("");
        player.sendMessage("§6=== Outros Comandos ===");
        player.sendMessage("§e/ignore §7- Gerenciar ignores (GUI)");
        player.sendMessage("§e/chat help §7- Mostrar esta ajuda");
        player.sendMessage("");
        player.sendMessage("§7💡 Dica: Você sempre volta ao chat local após usar /g, /c ou /a");
    }
}
