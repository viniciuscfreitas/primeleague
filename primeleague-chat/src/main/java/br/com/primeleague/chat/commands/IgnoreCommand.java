package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.ChannelIgnoreService;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.gui.IgnoreGUI;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Comando para gerenciar o sistema de ignore de canais e jogadores.
 * SIMPLIFICAÇÃO ARQUITETURAL: Apenas 2 formas de uso - GUI e atalho rápido.
 * 
 * @author PrimeLeague Team
 * @version 2.0
 */
public class IgnoreCommand implements CommandExecutor {
    
    private final PrimeLeagueChat plugin;
    private final ChannelIgnoreService ignoreService;
    
    public IgnoreCommand(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.ignoreService = plugin.getIgnoreService();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // SIMPLIFICAÇÃO ARQUITETURAL: Apenas 2 formas de uso
        if (args.length == 0) {
            // Abrir GUI completa para gerenciamento
            openIgnoreGUI(player);
            return true;
        }
        
        if (args.length == 1) {
            // Atalho rápido para ignorar jogador
            String targetName = args[0];
            handleQuickIgnorePlayer(player, targetName);
            return true;
        }
        
        // Caso inválido
        player.sendMessage("§cUso correto:");
        player.sendMessage("§e/ignore §7- Abrir interface de gerenciamento");
        player.sendMessage("§e/ignore <nome> §7- Ignorar jogador rapidamente");
        return true;
    }
    
    /**
     * Atalho rápido para ignorar um jogador.
     */
    private void handleQuickIgnorePlayer(Player player, String targetName) {
        Player targetPlayer = Bukkit.getPlayer(targetName);
        if (targetPlayer == null) {
            player.sendMessage("§cJogador '" + targetName + "' não encontrado ou offline.");
            return;
        }
        
        if (targetPlayer.equals(player)) {
            player.sendMessage("§cVocê não pode ignorar a si mesmo.");
            return;
        }
        
        if (ignoreService.ignorePlayer(player, targetPlayer)) {
            player.sendMessage("§a🔇 Jogador " + targetPlayer.getName() + " adicionado à lista de ignorados.");
            player.sendMessage("§7💡 Use §e/ignore §7para gerenciar seus ignores.");
        } else {
            player.sendMessage("§e⚠️ Você já está ignorando o jogador " + targetPlayer.getName() + ".");
        }
    }
    
    /**
     * Abre a interface gráfica de ignore para o jogador.
     */
    private void openIgnoreGUI(Player player) {
        IgnoreGUI ignoreGUI = new IgnoreGUI(plugin);
        ignoreGUI.openGUI(player);
        
        player.sendMessage("§e🔇 Interface de ignore aberta!");
        player.sendMessage("§7💡 Dica: Clique nos itens para alternar o status.");
    }
}

