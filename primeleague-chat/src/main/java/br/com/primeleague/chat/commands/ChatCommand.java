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
            case "global":
            case "g":
                setChannel(player, ChatChannel.GLOBAL);
                break;
            case "clan":
            case "c":
                if (player.hasPermission("primeleague.chat.clan")) {
                    setChannel(player, ChatChannel.CLAN);
                } else {
                    player.sendMessage("§cVocê não tem permissão para usar o chat de clã.");
                }
                break;
            case "ally":
            case "a":
                if (player.hasPermission("primeleague.chat.ally")) {
                    setChannel(player, ChatChannel.ALLY);
                } else {
                    player.sendMessage("§cVocê não tem permissão para usar o chat de aliança.");
                }
                break;
            case "local":
            case "l":
                setChannel(player, ChatChannel.LOCAL);
                break;
            case "help":
            case "h":
                showHelp(player);
                break;
            default:
                player.sendMessage("§cSubcomando desconhecido. Use /chat help para ver as opções.");
                break;
        }
        
        return true;
    }
    
    private void setChannel(Player player, ChatChannel channel) {
        channelManager.setPlayerChannel(player, channel);
        
        String channelName = getChannelDisplayName(channel);
        player.sendMessage("§aCanal alterado para: §f" + channelName);
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6=== Comandos de Chat ===");
        player.sendMessage("§e/chat global §7- Alterar para chat global");
        player.sendMessage("§e/chat clan §7- Alterar para chat de clã");
        player.sendMessage("§e/chat ally §7- Alterar para chat de aliança");
        player.sendMessage("§e/chat local §7- Alterar para chat local");
        player.sendMessage("§e/chat help §7- Mostrar esta ajuda");
        player.sendMessage("");
        player.sendMessage("§7Atalhos: §eg, c, a, l, h");
    }
    
    private String getChannelDisplayName(ChatChannel channel) {
        switch (channel) {
            case GLOBAL:
                return "§aGlobal";
            case CLAN:
                return "§bClã";
            case ALLY:
                return "§dAliança";
            case LOCAL:
                return "§eLocal";
            default:
                return "§7Desconhecido";
        }
    }
}
