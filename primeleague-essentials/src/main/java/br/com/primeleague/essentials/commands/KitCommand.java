package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.KitManager;
import br.com.primeleague.essentials.models.Kit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

/**
 * Comando para o sistema de kits.
 * Gerencia o uso e listagem de kits disponíveis.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class KitCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final KitManager kitManager;
    
    /**
     * Construtor do KitCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param kitManager Instância do KitManager
     */
    public KitCommand(EssentialsPlugin plugin, KitManager kitManager) {
        this.plugin = plugin;
        this.kitManager = kitManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Listar kits disponíveis
            listAvailableKits(player);
            return true;
        }
        
        String kitName = args[0].toLowerCase();
        
        // Verificar se é um comando de administração
        if (kitName.equals("reload") && player.hasPermission("primeleague.kits.admin")) {
            kitManager.reloadKits();
            player.sendMessage("§aKits recarregados com sucesso!");
            return true;
        }
        
        // Usar kit
        useKit(player, kitName);
        return true;
    }
    
    /**
     * Lista os kits disponíveis para o jogador.
     */
    private void listAvailableKits(Player player) {
        if (!player.hasPermission("primeleague.kits.use")) {
            player.sendMessage("§cVocê não tem permissão para usar kits!");
            return;
        }
        
        kitManager.listAvailableKitsAsync(player, new Consumer<List<Kit>>() {
            @Override
            public void accept(List<Kit> kits) {
                if (kits.isEmpty()) {
                    player.sendMessage("§cVocê não tem acesso a nenhum kit!");
                    return;
                }
                
                player.sendMessage("§6=== §eKits Disponíveis §6===");
                
                for (Kit kit : kits) {
                    String kitInfo = "§e" + kit.getDisplayName() + " §7- " + kit.getDescription();
                    player.sendMessage(kitInfo);
                }
                
                player.sendMessage("§6================================");
                player.sendMessage("§7Use §e/kit <nome> §7para receber um kit!");
            }
        });
    }
    
    /**
     * Usa um kit específico.
     */
    private void useKit(Player player, String kitName) {
        if (!player.hasPermission("primeleague.kits.use")) {
            player.sendMessage("§cVocê não tem permissão para usar kits!");
            return;
        }
        
        kitManager.useKitAsync(player, kitName, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean success) {
                if (!success) {
                    // Mensagens de erro já foram enviadas pelo KitManager
                    return;
                }
                
                // Sucesso - mensagem já foi enviada pelo KitManager
                plugin.getLogger().info("✅ Kit usado: " + player.getName() + " usou " + kitName);
            }
        });
    }
}
