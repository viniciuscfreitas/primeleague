package br.com.primeleague.core.commands;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.util.EconomyUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Comando /money para exibir saldos dos jogadores.
 * 
 * Uso:
 * - /money - Exibe o próprio saldo
 * - /money <jogador> - Exibe o saldo de outro jogador
 * 
 * Permissão: primeleague.money
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class MoneyCommand implements CommandExecutor {

    private final PrimeLeagueCore plugin;

    public MoneyCommand(PrimeLeagueCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Verificar permissão
        if (!sender.hasPermission("primeleague.money")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Caso 1: /money (sem argumentos) - exibir próprio saldo
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
                return true;
            }
            
            Player player = (Player) sender;
            displayBalance(sender, player.getName(), player.getUniqueId());
            return true;
        }

        // Caso 2: /money <jogador> - exibir saldo de outro jogador
        if (args.length == 1) {
            String targetName = args[0];
            displayBalance(sender, targetName, null);
            return true;
        }

        // Argumentos inválidos
        sender.sendMessage(ChatColor.RED + "Uso correto: /money [jogador]");
        return true;
    }

    /**
     * Exibe o saldo de um jogador.
     * 
     * @param sender Quem está executando o comando
     * @param playerName Nome do jogador
     * @param playerUuid UUID do jogador (se disponível)
     */
    private void displayBalance(CommandSender sender, String playerName, java.util.UUID playerUuid) {
        try {
            // Tentar obter o player_id
            Integer playerId = null;
            
            if (playerUuid != null) {
                // Se temos o UUID, usar IdentityManager
                playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUuid);
            } else {
                // Se não temos UUID, buscar pelo nome
                Player onlinePlayer = Bukkit.getPlayerExact(playerName);
                if (onlinePlayer != null) {
                    playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(onlinePlayer.getUniqueId());
                } else {
                    // Jogador offline, buscar diretamente o player_id (otimizado)
                    DataManager dataManager = PrimeLeagueAPI.getDataManager();
                    if (dataManager != null) {
                        playerId = dataManager.getPlayerIdByName(playerName);
                    }
                }
            }

            if (playerId == null) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + playerName + "' não encontrado.");
                return;
            }

            // Obter saldo via EconomyManager
            BigDecimal balance = PrimeLeagueAPI.getEconomyManager().getBalance(playerId);
            
            // Exibir saldo formatado
            String formattedBalance = EconomyUtils.formatMoney(balance);
            
            if (sender.getName().equals(playerName)) {
                sender.sendMessage(ChatColor.GREEN + "💰 Seu saldo atual: " + ChatColor.GOLD + "$" + formattedBalance);
            } else {
                sender.sendMessage(ChatColor.GREEN + "💰 Saldo de " + ChatColor.YELLOW + playerName + ChatColor.GREEN + ": " + ChatColor.GOLD + "$" + formattedBalance);
            }

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro ao buscar saldo: " + e.getMessage());
            plugin.getLogger().severe("Erro no comando /money: " + e.getMessage());
        }
    }


}
