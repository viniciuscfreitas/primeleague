package br.com.primeleague.territories.commands;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.model.TerritoryChunk;
import br.com.primeleague.territories.util.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comando principal para operações de território.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class TerritoryCommand implements CommandExecutor, TabCompleter {
    
    private final PrimeLeagueTerritories plugin;
    private final TerritoryManager territoryManager;
    private final MessageManager messageManager;
    
    public TerritoryCommand(PrimeLeagueTerritories plugin) {
        this.plugin = plugin;
        this.territoryManager = plugin.getTerritoryManager();
        this.messageManager = plugin.getMessageManager();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messageManager.getMessage("error.no_permission"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "claim":
                handleClaim(player);
                break;
            case "unclaim":
                handleUnclaim(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "list":
                handleList(player);
                break;
            case "bank":
                handleBank(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Subcomando não reconhecido! Use /territory help para ver os comandos disponíveis.");
                break;
        }
        
        return true;
    }
    
    /**
     * Trata o subcomando claim.
     */
    private void handleClaim(Player player) {
        if (!player.hasPermission("primeleague.territories.claim")) {
            player.sendMessage(messageManager.getMessage("territory.claim.no_permission"));
            return;
        }
        
        territoryManager.claimTerritory(player, player.getLocation(), (result, message) -> {
            switch (result) {
                case SUCCESS:
                    player.sendMessage(messageManager.getMessage("territory.claim.success"));
                    break;
                case ALREADY_CLAIMED:
                    player.sendMessage(messageManager.getMessage("territory.claim.already_claimed"));
                    break;
                case NO_CLAN:
                    player.sendMessage(messageManager.getMessage("territory.claim.no_clan"));
                    break;
                case NO_PERMISSION:
                    player.sendMessage(messageManager.getMessage("territory.claim.no_permission"));
                    break;
                case INSUFFICIENT_MORAL:
                    // Usar placeholder para valores dinâmicos
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("required", "10"); // Valor placeholder
                    placeholders.put("current", "5");   // Valor placeholder
                    player.sendMessage(messageManager.getMessage("territory.claim.insufficient_moral", placeholders));
                    break;
                case LIMIT_EXCEEDED:
                case DATABASE_ERROR:
                    player.sendMessage(messageManager.getMessage("error.database"));
                    break;
            }
        });
    }
    
    /**
     * Trata o subcomando unclaim.
     */
    private void handleUnclaim(Player player) {
        if (!player.hasPermission("primeleague.territories.unclaim")) {
            player.sendMessage(messageManager.getMessage("territory.unclaim.no_permission"));
            return;
        }
        
        territoryManager.unclaimTerritory(player, player.getLocation(), (result, message) -> {
            switch (result) {
                case SUCCESS:
                    player.sendMessage(messageManager.getMessage("territory.unclaim.success"));
                    break;
                case NOT_CLAIMED:
                    player.sendMessage(messageManager.getMessage("territory.unclaim.not_claimed"));
                    break;
                case NO_PERMISSION:
                    player.sendMessage(messageManager.getMessage("territory.unclaim.no_permission"));
                    break;
                case DATABASE_ERROR:
                    player.sendMessage(messageManager.getMessage("error.database"));
                    break;
            }
        });
    }
    
    /**
     * Trata o subcomando info.
     */
    private void handleInfo(Player player) {
        TerritoryChunk territory = territoryManager.getTerritoryAt(player.getLocation());
        
        if (territory == null) {
            player.sendMessage(ChatColor.GRAY + "Você não está em um território reivindicado.");
            return;
        }
        
        ClanDTO owner = territoryManager.getOwningClan(player.getLocation());
        if (owner == null) {
            player.sendMessage(ChatColor.RED + "Erro ao obter informações do proprietário!");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "=== INFORMAÇÕES DO TERRITÓRIO ===");
        player.sendMessage(ChatColor.WHITE + "Proprietário: " + ChatColor.GREEN + owner.getTag());
        player.sendMessage(ChatColor.WHITE + "Nome: " + ChatColor.GREEN + owner.getName());
        player.sendMessage(ChatColor.WHITE + "Estado: " + ChatColor.GREEN + territoryManager.getTerritoryState(owner.getId()).name());
        player.sendMessage(ChatColor.WHITE + "Territórios: " + ChatColor.GREEN + territoryManager.getTerritoryCount(owner.getId()));
    }
    
    /**
     * Trata o subcomando list.
     */
    private void handleList(Player player) {
        // Obter clã do jogador
        int playerId = getPlayerId(player); // Placeholder temporário
        ClanDTO playerClan = getPlayerClan(playerId); // Placeholder temporário
        
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "Você precisa estar em um clã para ver a lista de territórios!");
            return;
        }
        
        // Obter territórios do clã
        List<TerritoryChunk> territories = territoryManager.getClanTerritories(playerClan.getId());
        
        if (territories.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Seu clã não possui territórios reivindicados.");
            return;
        }
        
        player.sendMessage(ChatColor.YELLOW + "=== TERRITÓRIOS DO CLÃ ===");
        player.sendMessage(ChatColor.WHITE + "Total: " + ChatColor.GREEN + territories.size());
        
        for (int i = 0; i < Math.min(territories.size(), 10); i++) {
            TerritoryChunk territory = territories.get(i);
            player.sendMessage(ChatColor.WHITE + String.valueOf(i + 1) + ". " + ChatColor.GRAY + 
                territory.getWorldName() + " " + territory.getChunkX() + ", " + territory.getChunkZ());
        }
        
        if (territories.size() > 10) {
            player.sendMessage(ChatColor.GRAY + "... e mais " + (territories.size() - 10) + " territórios.");
        }
    }
    
    /**
     * Trata o subcomando bank.
     */
    private void handleBank(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /territory bank <deposit|withdraw|balance> [quantia]");
            return;
        }
        
        // Obter clã do jogador
        int playerId = getPlayerId(player); // Placeholder temporário
        ClanDTO playerClan = getPlayerClan(playerId); // Placeholder temporário
        
        if (playerClan == null) {
            player.sendMessage(ChatColor.RED + "Você precisa estar em um clã para acessar o banco!");
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "balance":
                handleBankBalance(player, playerClan);
                break;
            case "deposit":
                handleBankDeposit(player, args, playerClan);
                break;
            case "withdraw":
                handleBankWithdraw(player, args, playerClan);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Ação não reconhecida! Use: deposit, withdraw ou balance");
                break;
        }
    }
    
    /**
     * Trata o saldo do banco.
     */
    private void handleBankBalance(Player player, ClanDTO playerClan) {
        territoryManager.getClanBank(playerClan.getId(), (bank) -> {
            if (bank != null) {
                player.sendMessage(ChatColor.YELLOW + "=== BANCO DO CLÃ ===");
                player.sendMessage(ChatColor.WHITE + "Saldo: " + ChatColor.GREEN + "$" + bank.getBalance());
                player.sendMessage(ChatColor.WHITE + "Custo de manutenção: " + ChatColor.YELLOW + "$" + territoryManager.getMaintenanceCost(playerClan.getId()) + "/dia");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao acessar o banco do clã!");
            }
        });
    }
    
    /**
     * Trata depósito no banco.
     */
    private void handleBankDeposit(Player player, String[] args, ClanDTO playerClan) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso: /territory bank deposit <quantia>");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "A quantia deve ser positiva!");
                return;
            }
            
        // Verificar se o jogador tem dinheiro suficiente
        double playerMoney = getPlayerBalance(player); // Placeholder temporário
        if (playerMoney < amount) {
            player.sendMessage(ChatColor.RED + "Você não tem dinheiro suficiente!");
            return;
        }
        
        // Transferir dinheiro
        withdrawPlayerMoney(player, amount, "Depósito no banco do clã"); // Placeholder temporário
            territoryManager.depositToClanBank(playerClan.getId(), java.math.BigDecimal.valueOf(amount), (success) -> {
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Depositado $" + amount + " no banco do clã!");
                } else {
                    player.sendMessage(ChatColor.RED + "Erro ao depositar no banco!");
                    // Reverter transação
                    depositPlayerMoney(player, amount, "Reversão de depósito"); // Placeholder temporário
                }
            });
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida!");
        }
    }
    
    /**
     * Trata saque do banco.
     */
    private void handleBankWithdraw(Player player, String[] args, ClanDTO playerClan) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso: /territory bank withdraw <quantia>");
            return;
        }
        
        try {
            double amount = Double.parseDouble(args[2]);
            
            if (amount <= 0) {
                player.sendMessage(ChatColor.RED + "A quantia deve ser positiva!");
                return;
            }
            
            // Verificar saldo do banco
            territoryManager.getClanBank(playerClan.getId(), (bank) -> {
                if (bank == null || !bank.hasEnoughBalance(java.math.BigDecimal.valueOf(amount))) {
                    player.sendMessage(ChatColor.RED + "Saldo insuficiente no banco do clã!");
                    return;
                }
                
                // Retirar dinheiro
                territoryManager.withdrawFromClanBank(playerClan.getId(), java.math.BigDecimal.valueOf(amount), (success) -> {
                    if (success) {
                        depositPlayerMoney(player, amount, "Saque do banco do clã"); // Placeholder temporário
                        player.sendMessage(ChatColor.GREEN + "Sacado $" + amount + " do banco do clã!");
                    } else {
                        player.sendMessage(ChatColor.RED + "Erro ao sacar do banco!");
                    }
                });
            });
            
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida!");
        }
    }
    
    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "=== COMANDOS DE TERRITÓRIO ===");
        player.sendMessage(ChatColor.WHITE + "/territory claim " + ChatColor.GRAY + "- Reivindicar território");
        player.sendMessage(ChatColor.WHITE + "/territory unclaim " + ChatColor.GRAY + "- Remover território");
        player.sendMessage(ChatColor.WHITE + "/territory info " + ChatColor.GRAY + "- Informações do território");
        player.sendMessage(ChatColor.WHITE + "/territory list " + ChatColor.GRAY + "- Listar territórios do clã");
        player.sendMessage(ChatColor.WHITE + "/territory bank " + ChatColor.GRAY + "- Acessar banco do clã");
        player.sendMessage(ChatColor.WHITE + "/territory help " + ChatColor.GRAY + "- Mostrar esta ajuda");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("claim", "unclaim", "info", "list", "bank", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bank")) {
            List<String> bankActions = Arrays.asList("deposit", "withdraw", "balance");
            for (String action : bankActions) {
                if (action.toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(action);
                }
            }
        }
        
        return completions;
    }
    
    // Placeholders temporários para API do Core
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
        try {
            br.com.primeleague.api.EconomyService economyService = br.com.primeleague.core.api.PrimeLeagueAPI.getEconomyServiceRegistry();
            if (economyService == null) {
                plugin.getLogger().warning("EconomyService não está registrado!");
                return;
            }
            boolean success = economyService.withdrawPlayerMoney(player.getUniqueId(), amount, reason);
            if (!success) {
                plugin.getLogger().warning("Falha ao debitar $" + amount + " de " + player.getName() + " - " + reason);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao debitar dinheiro de " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private void depositPlayerMoney(Player player, double amount, String reason) {
        try {
            br.com.primeleague.api.EconomyService economyService = br.com.primeleague.core.api.PrimeLeagueAPI.getEconomyServiceRegistry();
            if (economyService == null) {
                plugin.getLogger().warning("EconomyService não está registrado!");
                return;
            }
            boolean success = economyService.depositPlayerMoney(player.getUniqueId(), amount, reason);
            if (!success) {
                plugin.getLogger().warning("Falha ao depositar $" + amount + " para " + player.getName() + " - " + reason);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao depositar dinheiro para " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private int getPlayerId(Player player) {
        try {
            br.com.primeleague.api.IdentityService identityService = br.com.primeleague.core.api.PrimeLeagueAPI.getIdentityServiceRegistry();
            if (identityService == null) {
                plugin.getLogger().warning("IdentityService não está registrado!");
                return -1;
            }
            return identityService.getPlayerId(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter ID do jogador " + player.getName() + ": " + e.getMessage());
            return -1; // Retorna -1 em caso de erro
        }
    }
}