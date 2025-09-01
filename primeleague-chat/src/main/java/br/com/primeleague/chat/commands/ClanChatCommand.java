package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelManager.ChatChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para chat de clã (versão simplificada para teste).
 */
public class ClanChatCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final ChannelManager channelManager;
    
    public ClanChatCommand(PrimeLeagueChat plugin) {
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
        
        if (!player.hasPermission("primeleague.chat.clan")) {
            player.sendMessage("§cVocê não tem permissão para usar o chat de clã.");
            return true;
        }
        
        if (args.length < 1) {
            player.sendMessage("§eUso correto: /c <mensagem>");
            return true;
        }
        
        String message = String.join(" ", args);
        
        if (message.trim().isEmpty()) {
            player.sendMessage("§cA mensagem não pode estar vazia.");
            return true;
        }
        
        // PRINCÍPIO "QUICK SEND": Enviar mensagem única sem mudar canal
        // O jogador permanece no chat local após o envio
        
        String formattedMessage = channelManager.formatClanMessage(player, message);
        
        // Encontrar membros do clã e enviar a mensagem
        List<Player> clanMembers = new ArrayList<Player>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (isPlayerInSameClan(player, onlinePlayer)) {
                clanMembers.add(onlinePlayer);
            }
        }
        
        // Enviar para membros do clã
        for (Player onlinePlayer : clanMembers) {
            onlinePlayer.sendMessage(formattedMessage);
        }
        
        // Feedback contextual para o remetente
        player.sendMessage("§7[Você -> Clã] " + formattedMessage);
        
        plugin.getLoggingService().logMessage("CLAN", player, null, message);
        
        return true;
    }
    
    // Método auxiliar para verificar se dois jogadores estão no mesmo clã
    private boolean isPlayerInSameClan(Player player1, Player player2) {
        // TODO: Implementar integração com ClanManager
        // Por enquanto, retorna false para evitar erros
        return false;
    }
}
