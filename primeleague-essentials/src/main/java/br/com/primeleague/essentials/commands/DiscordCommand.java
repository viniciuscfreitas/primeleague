package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para exibir informações do Discord.
 * Exibe uma mensagem configurável com link para o servidor Discord.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class DiscordCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    
    /**
     * Construtor do DiscordCommand.
     * 
     * @param plugin Instância do plugin principal
     */
    public DiscordCommand(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!player.hasPermission("primeleague.essentials.discord")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Exibir mensagem do Discord
        displayDiscordMessage(player);
        return true;
    }
    
    /**
     * Exibe a mensagem do Discord para o jogador.
     */
    private void displayDiscordMessage(Player player) {
        try {
            // Obter mensagem do config
            List<String> messageLines = plugin.getConfig().getStringList("discord-command.message");
            
            if (messageLines == null || messageLines.isEmpty()) {
                // Mensagem padrão caso não esteja configurada
                player.sendMessage("§6§l=== §eNOSSO DISCORD §6§l===");
                player.sendMessage("");
                player.sendMessage("§7Todas as regras, atualizações e anúncios");
                player.sendMessage("§7estão em nosso servidor do Discord!");
                player.sendMessage("");
                player.sendMessage("§a§nhttps://discord.gg/primeleague");
                player.sendMessage("");
                player.sendMessage("§6§l========================");
                return;
            }
            
            // Enviar cada linha da mensagem
            for (String line : messageLines) {
                if (line != null && !line.isEmpty()) {
                    player.sendMessage(line);
                } else {
                    // Linha vazia - enviar string vazia para criar espaço
                    player.sendMessage("");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("⚠️ Erro ao exibir mensagem do Discord: " + e.getMessage());
            
            // Mensagem de fallback em caso de erro
            player.sendMessage("§6§l=== §eNOSSO DISCORD §6§l===");
            player.sendMessage("");
            player.sendMessage("§7Todas as regras, atualizações e anúncios");
            player.sendMessage("§7estão em nosso servidor do Discord!");
            player.sendMessage("");
            player.sendMessage("§a§nhttps://discord.gg/primeleague");
            player.sendMessage("");
            player.sendMessage("§6§l========================");
        }
    }
}
