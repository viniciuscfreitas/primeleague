package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.EssentialsManager;
import br.com.primeleague.api.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando principal para o sistema de comandos essenciais.
 * Gerencia homes, teletransporte e spawn.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class EssentialsCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final EssentialsManager essentialsManager;
    
    /**
     * Construtor do comando.
     * 
     * @param plugin Instância do plugin principal
     */
    public EssentialsCommand(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.essentialsManager = plugin.getEssentialsManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "sethome":
                return handleSetHome(player, args);
            case "home":
                return handleHome(player, args);
            case "spawn":
                return handleSpawn(player, args);
            case "delhome":
            case "removehome":
                return handleDelHome(player, args);
            case "homes":
            case "listhomes":
                return handleListHomes(player, args);
            case "reload":
                return handleReload(player, args);
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Manipula o comando /sethome.
     */
    private boolean handleSetHome(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.sethome")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUso: /sethome <nome>");
            return true;
        }
        
        String homeName = args[1];
        
        // Validar nome da home
        if (!isValidHomeName(homeName)) {
            player.sendMessage("§cNome da home inválido! Use apenas letras, números e underscore.");
            return true;
        }
        
        // Criar home de forma assíncrona
        essentialsManager.createHomeAsync(player, homeName, (success) -> {
            if (success) {
                player.sendMessage("§aHome '" + homeName + "' criada com sucesso!");
            }
        });
        
        return true;
    }
    
    /**
     * Manipula o comando /home.
     */
    private boolean handleHome(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.home")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        if (args.length < 2) {
            // Listar homes disponíveis de forma assíncrona
            essentialsManager.getPlayerHomesAsync(player.getUniqueId(), (homes) -> {
                if (homes.isEmpty()) {
                    player.sendMessage("§cVocê não possui nenhuma home! Use /sethome <nome> para criar uma.");
                    return;
                }
                
                player.sendMessage("§6Suas homes:");
                for (Home home : homes) {
                    player.sendMessage("§7- §e" + home.getHomeName() + " §7(" + home.getWorld() + ")");
                }
                player.sendMessage("§7Use /home <nome> para teleportar para uma home específica.");
            });
            return true;
        }
        
        String homeName = args[1];
        
        // Teleportar para home de forma assíncrona
        essentialsManager.teleportToHomeAsync(player, homeName, (success) -> {
            if (success) {
                player.sendMessage("§aTeleportado para home '" + homeName + "'!");
            }
        });
        
        return true;
    }
    
    /**
     * Manipula o comando /spawn.
     */
    private boolean handleSpawn(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.spawn")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Teleportar para spawn
        if (essentialsManager.teleportToSpawn(player)) {
            player.sendMessage("§aTeleportado para o spawn!");
        }
        
        return true;
    }
    
    /**
     * Manipula o comando /delhome.
     */
    private boolean handleDelHome(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.delhome")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUso: /delhome <nome>");
            return true;
        }
        
        String homeName = args[1];
        
        // Remover home de forma assíncrona
        essentialsManager.removeHomeAsync(player, homeName, (success) -> {
            if (success) {
                player.sendMessage("§aHome '" + homeName + "' removida com sucesso!");
            }
        });
        
        return true;
    }
    
    /**
     * Manipula o comando /homes.
     */
    private boolean handleListHomes(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.homes")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Listar homes de forma assíncrona
        essentialsManager.getPlayerHomesAsync(player.getUniqueId(), (homes) -> {
            if (homes.isEmpty()) {
                player.sendMessage("§cVocê não possui nenhuma home!");
                return;
            }
            
            int maxHomes = essentialsManager.getMaxHomesForPlayer(player);
            int currentHomes = homes.size();
            
            player.sendMessage("§6=== Suas Homes ===");
            player.sendMessage("§7Limite: §e" + currentHomes + "§7/§e" + (maxHomes == Integer.MAX_VALUE ? "∞" : maxHomes));
            player.sendMessage("");
            
            for (Home home : homes) {
                String lastUsed = home.getLastUsed() != null ? 
                    "§7(usada: §e" + formatTimestamp(home.getLastUsed().getTime()) + "§7)" : 
                    "§7(nunca usada)";
                
                player.sendMessage("§7- §e" + home.getHomeName() + " §7- " + home.getWorld() + " " + lastUsed);
            }
        });
        
        return true;
    }
    
    /**
     * Manipula o comando /reload.
     */
    private boolean handleReload(Player player, String[] args) {
        if (!player.hasPermission("primeleague.essentials.reload")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Recarregar configurações
        plugin.reloadConfig();
        essentialsManager.clearAllCache();
        
        player.sendMessage("§aConfigurações do Essentials recarregadas!");
        return true;
    }
    
    /**
     * Envia mensagem de ajuda para o jogador.
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage("§6=== Comandos Essenciais ===");
        player.sendMessage("§7/sethome <nome> §8- Criar uma home");
        player.sendMessage("§7/home [nome] §8- Teleportar para home ou listar homes");
        player.sendMessage("§7/spawn §8- Teleportar para o spawn");
        player.sendMessage("§7/delhome <nome> §8- Remover uma home");
        player.sendMessage("§7/homes §8- Listar todas as suas homes");
        
        if (player.hasPermission("primeleague.essentials.reload")) {
            player.sendMessage("§7/essentials reload §8- Recarregar configurações");
        }
    }
    
    /**
     * Valida se o nome da home é válido.
     * 
     * @param homeName Nome da home
     * @return true se válido
     */
    private boolean isValidHomeName(String homeName) {
        if (homeName == null || homeName.trim().isEmpty()) {
            return false;
        }
        
        // Verificar tamanho
        if (homeName.length() > 32) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, underscore)
        return homeName.matches("^[a-zA-Z0-9_]+$");
    }
    
    /**
     * Formata timestamp para exibição.
     * 
     * @param timestamp Timestamp em milissegundos
     * @return String formatada
     */
    private String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) { // Menos de 1 minuto
            return "agora";
        } else if (diff < 3600000) { // Menos de 1 hora
            long minutes = diff / 60000;
            return minutes + "m atrás";
        } else if (diff < 86400000) { // Menos de 1 dia
            long hours = diff / 3600000;
            return hours + "h atrás";
        } else {
            long days = diff / 86400000;
            return days + "d atrás";
        }
    }
}
