package br.com.primeleague.adminshop.commands;

import br.com.primeleague.adminshop.AdminShopPlugin;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /shop para abrir a loja administrativa.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class ShopCommand implements CommandExecutor {

    private final AdminShopPlugin plugin;

    /**
     * Construtor do comando.
     * 
     * @param plugin instância do plugin
     */
    public ShopCommand(AdminShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c❌ Este comando só pode ser usado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.shop.use")) {
            player.sendMessage("§c❌ Você não tem permissão para usar a loja!");
            return true;
        }
        
        // Verificar argumentos
        if (args.length == 0) {
            // Abrir menu principal
            plugin.getShopManager().openMainMenu(player);
            return true;
        }
        
        // Verificar se quer abrir uma categoria específica
        String categoryId = args[0].toLowerCase();
        
        // Mapear aliases para IDs de categoria
        String mappedCategoryId = mapCategoryAlias(categoryId);
        
        if (mappedCategoryId != null) {
            plugin.getShopManager().openCategoryMenu(player, mappedCategoryId);
            return true;
        }
        
        // Categoria não encontrada
        player.sendMessage("§c❌ Categoria não encontrada!");
        player.sendMessage("§7Categorias disponíveis:");
        player.sendMessage("§7- basic_items (itens básicos)");
        player.sendMessage("§7- potions (poções)");
        player.sendMessage("§7- special_blocks (blocos especiais)");
        player.sendMessage("§7- vip_commands (comandos VIP)");
        player.sendMessage("§7- special_kits (kits especiais)");
        
        return true;
    }

    /**
     * Mapeia aliases para IDs de categoria.
     * 
     * @param alias alias da categoria
     * @return ID da categoria ou null se não encontrada
     */
    private String mapCategoryAlias(String alias) {
        switch (alias) {
            case "basic":
            case "basic_items":
            case "itens":
            case "items":
                return "basic_items";
                
            case "potions":
            case "poções":
            case "potion":
                return "potions";
                
            case "blocks":
            case "special_blocks":
            case "blocos":
            case "block":
                return "special_blocks";
                
            case "vip":
            case "vip_commands":
            case "comandos":
            case "commands":
                return "vip_commands";
                
            case "kits":
            case "special_kits":
            case "kit":
                return "special_kits";
                
            default:
                return null;
        }
    }
}
