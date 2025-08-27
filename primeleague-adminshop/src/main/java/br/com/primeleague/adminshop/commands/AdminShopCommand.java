package br.com.primeleague.adminshop.commands;

import br.com.primeleague.adminshop.AdminShopPlugin;
import br.com.primeleague.adminshop.models.ShopCategory;
import br.com.primeleague.adminshop.models.ShopItem;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Comando /adminshop para administração da loja.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class AdminShopCommand implements CommandExecutor {

    private final AdminShopPlugin plugin;

    /**
     * Construtor do comando.
     * 
     * @param plugin instância do plugin
     */
    public AdminShopCommand(AdminShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.shop.admin")) {
            sender.sendMessage("§c❌ Você não tem permissão para usar este comando!");
            return true;
        }
        
        // Verificar argumentos
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
                
            case "info":
                handleInfo(sender);
                break;
                
            case "clear":
                handleClear(sender);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            default:
                sender.sendMessage("§c❌ Subcomando não reconhecido: " + subCommand);
                showHelp(sender);
                break;
        }
        
        return true;
    }

    /**
     * Mostra a ajuda do comando.
     * 
     * @param sender sender do comando
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l🏪 Comandos da Loja Administrativa");
        sender.sendMessage("");
        sender.sendMessage("§e/adminshop reload §7- Recarrega a configuração da loja");
        sender.sendMessage("§e/adminshop info §7- Mostra informações da loja");
        sender.sendMessage("§e/adminshop clear §7- Limpa o cache da loja");
        sender.sendMessage("§e/adminshop stats §7- Mostra estatísticas da loja");
    }

    /**
     * Processa o subcomando reload.
     * 
     * @param sender sender do comando
     */
    private void handleReload(CommandSender sender) {
        sender.sendMessage("§a🔄 Recarregando configuração da loja...");
        
        try {
            plugin.getShopManager().reload();
            sender.sendMessage("§a✅ Configuração recarregada com sucesso!");
            
        } catch (Exception e) {
            sender.sendMessage("§c❌ Erro ao recarregar configuração: " + e.getMessage());
        }
    }

    /**
     * Processa o subcomando info.
     * 
     * @param sender sender do comando
     */
    private void handleInfo(CommandSender sender) {
        sender.sendMessage("§6§l🏪 Informações da Loja");
        sender.sendMessage("");
        
        // Informações da configuração
        int categoryCount = plugin.getConfigManager().getCategoryCount();
        int totalItems = plugin.getConfigManager().getTotalItemCount();
        
        sender.sendMessage("§7Categorias: §e" + categoryCount);
        sender.sendMessage("§7Total de Itens: §e" + totalItems);
        
        // Listar categorias
        sender.sendMessage("");
        sender.sendMessage("§7Categorias disponíveis:");
        
        for (String categoryId : plugin.getConfigManager().getAllCategories().keySet()) {
            ShopCategory category = plugin.getConfigManager().getCategory(categoryId);
            if (category != null) {
                sender.sendMessage("§7- §e" + category.getName() + " §7(" + category.getItems().size() + " itens)");
            }
        }
        
        // Informações de desconto (usando DonorManager como fonte única)
        sender.sendMessage("");
        sender.sendMessage("§7Descontos por Tier (via DonorManager):");
        for (int tier = 0; tier <= 5; tier++) {
            double discount = PrimeLeagueAPI.getDonorManager().getDiscountForTier(tier);
            sender.sendMessage("§7- Tier " + tier + ": §a" + String.format("%.1f%%", discount * 100));
        }
    }

    /**
     * Processa o subcomando clear.
     * 
     * @param sender sender do comando
     */
    private void handleClear(CommandSender sender) {
        sender.sendMessage("§a🗑️ Limpando cache da loja...");
        
        try {
            plugin.getShopManager().clearCache();
            sender.sendMessage("§a✅ Cache limpo com sucesso!");
            
        } catch (Exception e) {
            sender.sendMessage("§c❌ Erro ao limpar cache: " + e.getMessage());
        }
    }

    /**
     * Processa o subcomando stats.
     * 
     * @param sender sender do comando
     */
    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6§l📊 Estatísticas da Loja");
        sender.sendMessage("");
        
        // Estatísticas básicas
        int categoryCount = plugin.getConfigManager().getCategoryCount();
        int totalItems = plugin.getConfigManager().getTotalItemCount();
        
        sender.sendMessage("§7Categorias: §e" + categoryCount);
        sender.sendMessage("§7Total de Itens: §e" + totalItems);
        
        // Estatísticas por categoria
        sender.sendMessage("");
        sender.sendMessage("§7Itens por Categoria:");
        
        for (String categoryId : plugin.getConfigManager().getAllCategories().keySet()) {
            ShopCategory category = plugin.getConfigManager().getCategory(categoryId);
            if (category != null) {
                int itemCount = category.getItems().size();
                double totalPrice = 0.0;
                
                // Calcular preço total da categoria
                for (ShopItem item : category.getItems()) {
                    totalPrice += item.getPrice();
                }
                
                sender.sendMessage("§7- §e" + category.getName() + ": §7" + itemCount + 
                    " itens, §6$" + String.format("%.2f", totalPrice));
            }
        }
        
        // Estatísticas de preços
        sender.sendMessage("");
        sender.sendMessage("§7Estatísticas de Preços:");
        
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0.0;
        double totalPrice = 0.0;
        int priceCount = 0;
        
        for (ShopItem item : plugin.getConfigManager().getAllItems().values()) {
            double price = item.getPrice();
            minPrice = Math.min(minPrice, price);
            maxPrice = Math.max(maxPrice, price);
            totalPrice += price;
            priceCount++;
        }
        
        if (priceCount > 0) {
            double avgPrice = totalPrice / priceCount;
            sender.sendMessage("§7- Preço mínimo: §6$" + String.format("%.2f", minPrice));
            sender.sendMessage("§7- Preço máximo: §6$" + String.format("%.2f", maxPrice));
            sender.sendMessage("§7- Preço médio: §6$" + String.format("%.2f", avgPrice));
        }
    }
}
