package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para chat global (quick send).
 * Implementa o princípio "Local é Rei" - envia mensagem única sem mudar canal.
 */
public class GlobalChatCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final ChannelManager channelManager;
    
    public GlobalChatCommand(PrimeLeagueChat plugin) {
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
            player.sendMessage("§eUso correto: /g <mensagem>");
            return true;
        }
        
        String message = String.join(" ", args);
        
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // PRINCÍPIO "QUICK SEND": Enviar mensagem única sem mudar canal
        // O jogador permanece no chat local após o envio
        
        String formattedMessage = channelManager.formatGlobalMessage(player, message);
        
        // Obter todos os jogadores online e filtrar os que estão ignorando o canal OU o remetente
        List<Player> allPlayers = new ArrayList<Player>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            allPlayers.add(p);
        }
        List<Player> filteredPlayers = plugin.getIgnoreService().filterIgnoringChannelAndSender(allPlayers, ChannelManager.ChatChannel.GLOBAL, player);
        
        // Enviar para jogadores que não estão ignorando o canal nem o remetente
        for (Player onlinePlayer : filteredPlayers) {
            onlinePlayer.sendMessage(formattedMessage);
        }
        
        // Log da mensagem
        plugin.getLoggingService().logMessage("GLOBAL", player, null, message);
        
        return true;
    }
}
