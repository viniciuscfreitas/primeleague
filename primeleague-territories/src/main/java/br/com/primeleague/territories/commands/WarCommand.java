package br.com.primeleague.territories.commands;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.model.ActiveWar;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando para operações de guerra e cerco.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class WarCommand implements CommandExecutor, TabCompleter {
    
    private final PrimeLeagueTerritories plugin;
    private final WarManager warManager;
    
    public WarCommand(PrimeLeagueTerritories plugin) {
        this.plugin = plugin;
        this.warManager = plugin.getWarManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "declare":
                handleDeclare(player, args);
                break;
            case "status":
                handleStatus(player);
                break;
            case "altar":
                handleAltar(player);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Subcomando não reconhecido! Use /war help para ver os comandos disponíveis.");
                break;
        }
        
        return true;
    }
    
    /**
     * Trata o subcomando declare.
     */
    private void handleDeclare(Player player, String[] args) {
        if (!player.hasPermission("primeleague.territories.war")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para declarar guerra!");
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /war declare <clã>");
            return;
        }
        
        String targetClanName = args[1];
        
        warManager.declareWar(player, targetClanName, (result, message) -> {
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + message);
                    break;
                case NO_CLAN:
                case NO_PERMISSION:
                case TARGET_NOT_FOUND:
                case SAME_CLAN:
                case ALREADY_AT_WAR:
                case TARGET_NOT_VULNERABLE:
                case INSUFFICIENT_FUNDS:
                case TRUCE_ACTIVE:
                case DATABASE_ERROR:
                    player.sendMessage(ChatColor.RED + message);
                    break;
            }
        });
    }
    
    /**
     * Trata o subcomando status.
     */
    private void handleStatus(Player player) {
        // Obter clã do jogador
        int playerId = plugin.getCore().getIdentityManager().getPlayerId(player);
        ClanDTO playerClan = getPlayerClan(playerId); // Placeholder temporário
        
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "Você precisa estar em um clã para ver o status de guerra!");
            return;
        }
        
        // Verificar se há guerras ativas
        // TODO: Implementar getActiveWarsByClanAsync quando necessário
        List<ActiveWar> wars = new java.util.ArrayList<>();
        if (wars.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Seu clã não está em guerra no momento.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "=== STATUS DE GUERRA ===");
        
        for (ActiveWar war : wars) {
            String opponentName = (war.getAggressorClanId() == playerClan.getId()) ? 
                "Defendendo contra" : "Atacando";
            
            int opponentId = (war.getAggressorClanId() == playerClan.getId()) ? 
                war.getDefenderClanId() : war.getAggressorClanId();
            
            ClanDTO opponentClan = getClanById(opponentId); // Placeholder temporário
            String opponentTag = (opponentClan != null) ? opponentClan.getTag() : "Desconhecido";
            
            player.sendMessage(ChatColor.WHITE + opponentName + ": " + ChatColor.RED + opponentTag);
            player.sendMessage(ChatColor.GRAY + "Status: " + war.getStatus().name());
            
            if (war.getEndTimeExclusivity() != null) {
                long timeLeft = war.getEndTimeExclusivity().getTime() - System.currentTimeMillis();
                if (timeLeft > 0) {
                    long hoursLeft = timeLeft / (1000 * 60 * 60);
                    long minutesLeft = (timeLeft % (1000 * 60 * 60)) / (1000 * 60);
                    player.sendMessage(ChatColor.GRAY + "Tempo restante: " + hoursLeft + "h " + minutesLeft + "m");
                }
            }
        }
    }
    
    /**
     * Trata o subcomando altar.
     */
    private void handleAltar(Player player) {
        if (!player.hasPermission("primeleague.territories.war")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar o Altar da Discórdia!");
            return;
        }
        
        // Verificar se o jogador está em um clã
        int playerId = plugin.getCore().getIdentityManager().getPlayerId(player);
        ClanDTO playerClan = getPlayerClan(playerId); // Placeholder temporário
        
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "Você precisa estar em um clã para usar o Altar da Discórdia!");
            return;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Seu inventário está cheio!");
            return;
        }
        
        // Verificar custo
        double altarCost = plugin.getConfig().getDouble("altar.creation-cost", 1000.0);
        double playerMoney = getPlayerBalance(player); // Placeholder temporário
        
        if (playerMoney < altarCost) {
            player.sendMessage(ChatColor.RED + "Você não tem dinheiro suficiente! Necessário: $" + altarCost);
            return;
        }
        
        // Debitar custo
        withdrawPlayerMoney(player, altarCost, "Criação do Altar da Discórdia"); // Placeholder temporário
        
        // Dar o altar
        ItemStack altarItem = createAltarItem();
        player.getInventory().addItem(altarItem);
        
        player.sendMessage(ChatColor.GREEN + "Altar da Discórdia criado! Custo: $" + altarCost);
        player.sendMessage(ChatColor.YELLOW + "Posicione o altar em território reivindicado para iniciar um cerco.");
        player.sendMessage(ChatColor.GRAY + "Requer canalização de 5 segundos sem se mover.");
    }
    
    /**
     * Cria um item Altar da Discórdia.
     */
    private ItemStack createAltarItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.DARK_RED + "Altar da Discórdia");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Item especial para iniciar cercos",
            ChatColor.GRAY + "Posicione em território reivindicado",
            ChatColor.GRAY + "Requer canalização de 5 segundos",
            "",
            ChatColor.RED + "⚠ ATENÇÃO: Use com cuidado!"
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== COMANDOS DE GUERRA ===");
        player.sendMessage(ChatColor.WHITE + "/war declare <clã> " + ChatColor.GRAY + "- Declarar guerra a um clã");
        player.sendMessage(ChatColor.WHITE + "/war status " + ChatColor.GRAY + "- Ver status de guerras ativas");
        player.sendMessage(ChatColor.WHITE + "/war altar " + ChatColor.GRAY + "- Criar Altar da Discórdia");
        player.sendMessage(ChatColor.WHITE + "/war help " + ChatColor.GRAY + "- Mostrar esta ajuda");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Para declarar guerra, o clã alvo deve estar vulnerável.");
        player.sendMessage(ChatColor.GRAY + "Use o Altar da Discórdia para iniciar cercos em territórios.");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("declare", "status", "altar", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("declare")) {
            // TODO: Implementar autocompletar com nomes de clãs
            // Por enquanto, deixar vazio
        }
        
        return completions;
    }
    
    // Placeholders temporários para API do Core
    private double getPlayerBalance(Player player) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getEconomyManager().getBalance(player.getUniqueId()) quando API estiver disponível
        try {
            return 1000.0; // Saldo mock para permitir funcionalidade
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter saldo do jogador " + player.getName() + ": " + e.getMessage());
            return 0.0; // Retorna 0 em caso de erro
        }
    }
    
    private void withdrawPlayerMoney(Player player, double amount, String reason) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getEconomyManager().withdraw(player.getUniqueId(), amount, reason) quando API estiver disponível
        try {
            plugin.getLogger().info("Mock: Debitado $" + amount + " de " + player.getName() + " - " + reason);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao debitar dinheiro de " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private ClanDTO getPlayerClan(int playerId) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getClanServiceRegistry().getPlayerClan(playerUUID) quando API estiver disponível
        try {
            ClanDTO mockClan = new ClanDTO();
            mockClan.setId(playerId % 10 + 1); // Simula clã baseado no ID do jogador
            mockClan.setName("Clan_" + (playerId % 10 + 1));
            mockClan.setTag("[C" + (playerId % 10 + 1) + "]");
            return mockClan;
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã do jogador " + playerId + ": " + e.getMessage());
            return null;
        }
    }
    
    private ClanDTO getClanById(int clanId) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getClanServiceRegistry().getClanById(clanId) quando API estiver disponível
        try {
            ClanDTO mockClan = new ClanDTO();
            mockClan.setId(clanId);
            mockClan.setName("Clan_" + clanId);
            mockClan.setTag("[C" + clanId + "]");
            return mockClan;
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã por ID " + clanId + ": " + e.getMessage());
            return null;
        }
    }
}