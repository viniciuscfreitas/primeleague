package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.WarpManager;
import br.com.primeleague.api.models.Warp;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para listar warps públicos disponíveis.
 * Implementa /warplist com formatação clara e informações de custo.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class WarplistCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final WarpManager warpManager;
    
    /**
     * Construtor do WarplistCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param warpManager Instância do WarpManager
     */
    public WarplistCommand(EssentialsPlugin plugin, WarpManager warpManager) {
        this.plugin = plugin;
        this.warpManager = warpManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // HARDENING: Verificar se o jogador ainda está online antes de iniciar operação assíncrona
        if (!player.isOnline()) {
            return true; // Jogador não está mais online, abortar
        }
        
        // Mostrar mensagem de carregamento
        String loadingMsg = plugin.getConfig().getString("warps.messages.warplist.loading", "§7Carregando lista de warps...");
        player.sendMessage(loadingMsg);
        
        // Buscar warps disponíveis de forma assíncrona
        warpManager.listAvailableWarpsAsync(player, (warps) -> {
            // HARDENING: Verificar novamente se o jogador ainda está online antes de enviar mensagem
            if (!player.isOnline()) {
                return; // Jogador não está mais online, abortar callback
            }
            
            if (warps == null || warps.isEmpty()) {
                String noWarpsMsg = plugin.getConfig().getString("warps.messages.warplist.no-warps", "§cNenhum warp disponível!");
                player.sendMessage(noWarpsMsg);
                return;
            }
            
            // Exibir header
            String header = plugin.getConfig().getString("warps.messages.warplist.header", "§6§l=== §eWARPS DISPONÍVEIS §6§l===");
            player.sendMessage(header);
            
            // Exibir cada warp
            String warpFormat = plugin.getConfig().getString("warps.messages.warplist.warp-format", "§7• §e{warp} §7- §a${cost} §7- §f{permission}");
            String warpFree = plugin.getConfig().getString("warps.messages.warplist.warp-free", "§7• §e{warp} §7- §aGratuito §7- §f{permission}");
            
            for (Warp warp : warps) {
                String message;
                if (warp.hasCost()) {
                    message = warpFormat
                        .replace("{warp}", warp.getWarpName())
                        .replace("{cost}", warp.getFormattedCost())
                        .replace("{permission}", warp.getFormattedPermission());
                } else {
                    message = warpFree
                        .replace("{warp}", warp.getWarpName())
                        .replace("{permission}", warp.getFormattedPermission());
                }
                player.sendMessage(message);
            }
            
            // Exibir footer
            String footer = plugin.getConfig().getString("warps.messages.warplist.footer", "§6§l================================");
            player.sendMessage(footer);
            
            // Log da operação
            plugin.getLogger().info("✅ Lista de warps exibida para " + player.getName() + " (" + warps.size() + " warps)");
        });
        
        return true;
    }
}
