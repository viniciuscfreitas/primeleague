package br.com.primeleague.core.commands;

import br.com.primeleague.core.PrimeLeagueCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para gerenciar cache do sistema.
 * Permite limpar caches para resolver problemas de sincronização.
 */
public class CacheCommand implements CommandExecutor {

    private final PrimeLeagueCore plugin;

    public CacheCommand(PrimeLeagueCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin.cache")) {
            sender.sendMessage("§c❌ Você não tem permissão para usar este comando!");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "clear":
                handleClear(sender);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            case "clearplayer":
                if (args.length < 2) {
                    sender.sendMessage("§c❌ Uso: /cache clearplayer <jogador>");
                    return true;
                }
                handleClearPlayer(sender, args[1]);
                break;
                
            default:
                sender.sendMessage("§c❌ Subcomando não reconhecido: " + subCommand);
                showHelp(sender);
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6§l🗑️ Comandos de Cache");
        sender.sendMessage("");
        sender.sendMessage("§e/cache clear §7- Limpa todo o cache do sistema");
        sender.sendMessage("§e/cache stats §7- Mostra estatísticas do cache");
        sender.sendMessage("§e/cache clearplayer <jogador> §7- Limpa cache de um jogador específico");
    }

    private void handleClear(CommandSender sender) {
        sender.sendMessage("§a🗑️ Limpando cache do sistema...");
        
        try {
            // Limpar cache do DataManager
            plugin.getDataManager().clearCache();
            sender.sendMessage("§a✅ Cache do DataManager limpo");
            
            // Limpar cache do EconomyManager
            plugin.getEconomyManager().clearAllCache();
            sender.sendMessage("§a✅ Cache do EconomyManager limpo");
            
            // Limpar cache do DonorManager
            plugin.getDonorManager().clearAllCache();
            sender.sendMessage("§a✅ Cache do DonorManager limpo");
            
            // Limpar cache do IdentityManager
            plugin.getIdentityManager().clearCache();
            sender.sendMessage("§a✅ Cache do IdentityManager limpo");
            
            sender.sendMessage("§a✅ Todos os caches foram limpos com sucesso!");
            
        } catch (Exception e) {
            sender.sendMessage("§c❌ Erro ao limpar cache: " + e.getMessage());
            plugin.getLogger().severe("Erro no comando cache clear: " + e.getMessage());
        }
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage("§6§l📊 Estatísticas do Cache");
        sender.sendMessage("");
        
        try {
            // Estatísticas do DataManager
            int dataManagerCacheSize = plugin.getDataManager().getCacheSize();
            sender.sendMessage("§7DataManager: §e" + dataManagerCacheSize + " perfis em cache");
            
            // Estatísticas do IdentityManager
            String identityStats = plugin.getIdentityManager().getStats();
            sender.sendMessage("§7IdentityManager: §e" + identityStats);
            
            // Estatísticas do EconomyManager (se disponível)
            sender.sendMessage("§7EconomyManager: §eCache de saldos e transações");
            
            // Estatísticas do DonorManager (se disponível)
            sender.sendMessage("§7DonorManager: §eCache de níveis de doador");
            
        } catch (Exception e) {
            sender.sendMessage("§c❌ Erro ao buscar estatísticas: " + e.getMessage());
        }
    }

    private void handleClearPlayer(CommandSender sender, String playerName) {
        sender.sendMessage("§a🗑️ Limpando cache do jogador: " + playerName);
        
        try {
            // Buscar o jogador
            Player player = plugin.getServer().getPlayerExact(playerName);
            if (player != null) {
                // Jogador online
                plugin.getDataManager().removeFromCache(player.getUniqueId());
                plugin.getEconomyManager().clearPlayerCache(player.getEntityId());
                sender.sendMessage("§a✅ Cache do jogador online limpo");
            } else {
                // Jogador offline - tentar buscar pelo nome
                br.com.primeleague.core.models.PlayerProfile profile = plugin.getDataManager().loadOfflinePlayerProfile(playerName);
                if (profile != null) {
                    plugin.getDataManager().removeFromCache(profile.getUuid());
                    sender.sendMessage("§a✅ Cache do jogador offline limpo");
                } else {
                    sender.sendMessage("§c❌ Jogador não encontrado: " + playerName);
                }
            }
            
        } catch (Exception e) {
            sender.sendMessage("§c❌ Erro ao limpar cache do jogador: " + e.getMessage());
        }
    }
}
