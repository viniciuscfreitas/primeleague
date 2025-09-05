package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.PlayerInfoManager;
import br.com.primeleague.essentials.models.PlayerLastSeen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Comando para verificar quando um jogador foi visto pela última vez.
 * Exibe informações de última vez visto para jogadores online e offline.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class SeenCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final PlayerInfoManager playerInfoManager;
    
    /**
     * Construtor do SeenCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param playerInfoManager Instância do PlayerInfoManager
     */
    public SeenCommand(EssentialsPlugin plugin, PlayerInfoManager playerInfoManager) {
        this.plugin = plugin;
        this.playerInfoManager = playerInfoManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!player.hasPermission("primeleague.essentials.seen")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Validar argumentos
        if (args.length != 1) {
            String usage = plugin.getConfig().getString("player-info.seen.messages.usage", "§cUso: /seen <jogador>");
            player.sendMessage(usage);
            return true;
        }
        
        String targetPlayerName = args[0];
        
        // Verificar se o jogador alvo existe
        playerInfoManager.playerExistsAsync(targetPlayerName, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean exists) {
                if (!exists) {
                    String message = plugin.getConfig().getString("player-info.seen.messages.player-not-found", 
                        "§cJogador §e{player} §cnão encontrado!");
                    player.sendMessage(message.replace("{player}", targetPlayerName));
                    return;
                }
                
                // Obter informações de última vez visto
                playerInfoManager.getPlayerLastSeenAsync(targetPlayerName, new Consumer<PlayerLastSeen>() {
                    @Override
                    public void accept(PlayerLastSeen lastSeen) {
                        if (lastSeen == null) {
                            String message = plugin.getConfig().getString("player-info.seen.messages.player-not-found", 
                                "§cJogador §e{player} §cnão encontrado!");
                            player.sendMessage(message.replace("{player}", targetPlayerName));
                            return;
                        }
                        
                        // Exibir resultado
                        displayLastSeenInfo(player, lastSeen);
                    }
                });
            }
        });
        
        return true;
    }
    
    /**
     * Exibe as informações de última vez visto para o jogador.
     */
    private void displayLastSeenInfo(Player player, PlayerLastSeen lastSeen) {
        String playerName = lastSeen.getPlayerName();
        
        if (lastSeen.isOnline()) {
            // Jogador online
            String message = plugin.getConfig().getString("player-info.seen.messages.player-online", 
                "§a{player} §aestá online agora!");
            player.sendMessage(message.replace("{player}", playerName));
            
        } else if (lastSeen.neverSeen()) {
            // Jogador nunca foi visto
            String message = plugin.getConfig().getString("player-info.seen.messages.never-seen", 
                "§e{player} §7nunca foi visto no servidor!");
            player.sendMessage(message.replace("{player}", playerName));
            
        } else {
            // Jogador offline - mostrar última vez visto
            String formattedTime = lastSeen.getFormattedRelativeTime();
            String message = plugin.getConfig().getString("player-info.seen.messages.last-seen", 
                "§e{player} §7foi visto pela última vez: §a{time}");
            player.sendMessage(message.replace("{player}", playerName).replace("{time}", formattedTime));
        }
    }
}
