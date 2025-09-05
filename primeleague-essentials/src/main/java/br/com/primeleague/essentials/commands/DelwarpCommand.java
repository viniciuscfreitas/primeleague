package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para remover warps públicos (administradores).
 * Implementa /delwarp <nome> com validações e confirmação.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class DelwarpCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final WarpManager warpManager;
    
    /**
     * Construtor do DelwarpCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param warpManager Instância do WarpManager
     */
    public DelwarpCommand(EssentialsPlugin plugin, WarpManager warpManager) {
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
        
        // Verificar argumentos
        if (args.length != 1) {
            String usage = plugin.getConfig().getString("warps.messages.delwarp.usage", "§cUso: /delwarp <nome>");
            player.sendMessage(usage);
            return true;
        }
        
        String warpName = args[0].trim();
        
        // Validação básica do nome
        if (warpName.isEmpty()) {
            String usage = plugin.getConfig().getString("warps.messages.delwarp.usage", "§cUso: /delwarp <nome>");
            player.sendMessage(usage);
            return true;
        }
        
        // HARDENING: Verificar se o jogador ainda está online antes de iniciar operação assíncrona
        if (!player.isOnline()) {
            return true; // Jogador não está mais online, abortar
        }
        
        // Remover warp de forma assíncrona
        warpManager.removeWarpAsync(player, warpName, (success) -> {
            // HARDENING: Verificar novamente se o jogador ainda está online antes de enviar mensagem
            if (!player.isOnline()) {
                return; // Jogador não está mais online, abortar callback
            }
            
            if (success) {
                // Mensagens de sucesso já são enviadas pelo WarpManager
                // Aqui apenas logamos que a operação foi bem-sucedida
                plugin.getLogger().info("✅ Warp removido: " + warpName + " por " + player.getName());
            } else {
                // Mensagens de erro já são enviadas pelo WarpManager
                // Aqui apenas logamos que a operação falhou
                plugin.getLogger().warning("⚠️ Falha na remoção do warp " + warpName + " por " + player.getName());
            }
        });
        
        return true;
    }
}
