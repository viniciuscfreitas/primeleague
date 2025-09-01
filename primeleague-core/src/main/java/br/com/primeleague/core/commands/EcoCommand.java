package br.com.primeleague.core.commands;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.enums.TransactionReason;
import br.com.primeleague.core.models.EconomyResponse;
import br.com.primeleague.core.util.EconomyUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Comando administrativo /eco para gerenciar saldos dos jogadores.
 * 
 * Uso:
 * - /eco give <jogador> <quantia> - Adiciona dinheiro
 * - /eco take <jogador> <quantia> - Remove dinheiro
 * - /eco set <jogador> <quantia> - Define saldo exato
 * 
 * Permissão: primeleague.admin.eco
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class EcoCommand implements CommandExecutor {

    private final PrimeLeagueCore plugin;

    public EcoCommand(PrimeLeagueCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Verificar permissão
        if (!sender.hasPermission("primeleague.admin.eco")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Verificar argumentos mínimos
        if (args.length != 3) {
            sender.sendMessage(ChatColor.RED + "Uso correto: /eco <give|take|set> <jogador> <quantia>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        String amountStr = args[2];

        // Validar ação
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            sender.sendMessage(ChatColor.RED + "Ação inválida. Use: give, take ou set");
            return true;
        }

        // Validar quantia
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage(ChatColor.RED + "A quantia não pode ser negativa.");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Quantia inválida. Use números (ex: 100.50).");
            return true;
        }

        // Executar ação
        executeEcoAction(sender, action, targetName, amount);
        return true;
    }

    /**
     * Executa a ação administrativa de economia.
     * 
     * @param sender Quem está executando o comando
     * @param action Ação a ser executada (give/take/set)
     * @param targetName Nome do jogador alvo
     * @param amount Quantia
     */
    private void executeEcoAction(CommandSender sender, String action, String targetName, BigDecimal amount) {
        try {
            // Obter player_id do alvo
            Integer targetPlayerId = null;
            Player targetPlayer = Bukkit.getPlayerExact(targetName);
            
            if (targetPlayer != null) {
                // Jogador online
                targetPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(targetPlayer.getUniqueId());
            } else {
                // Jogador offline, buscar diretamente o player_id (otimizado)
                targetPlayerId = PrimeLeagueAPI.getDataManager().getPlayerIdByName(targetName);
            }

            if (targetPlayerId == null) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado.");
                return;
            }

            // REFATORADO: Usar métodos assíncronos para evitar bloqueio da thread principal
            sender.sendMessage(ChatColor.YELLOW + "⏳ Processando operação econômica...");

            // Obter nome do admin para logs
            String adminName = sender.getName();
            if (sender instanceof Player) {
                adminName = ((Player) sender).getName();
            }
            
            // Executar ação específica de forma assíncrona
            switch (action) {
                case "give":
                    PrimeLeagueAPI.getEconomyManager().creditBalanceAsync(targetPlayerId, amount.doubleValue(), "Admin Give by " + adminName, (response) -> {
                        handleEcoResponse(sender, targetPlayer, targetName, response, "adicionado", adminName, action, amount);
                    });
                    break;
                    
                case "take":
                    PrimeLeagueAPI.getEconomyManager().debitBalanceAsync(targetPlayerId, amount.doubleValue(), "Admin Take by " + adminName, (response) -> {
                        handleEcoResponse(sender, targetPlayer, targetName, response, "removido", adminName, action, amount);
                    });
                    break;
                    
                case "set":
                    // Para set, precisamos calcular a diferença de forma assíncrona
                    PrimeLeagueAPI.getEconomyManager().getBalanceAsync(targetPlayerId, (currentBalance) -> {
                        if (currentBalance == null) {
                            sender.sendMessage(ChatColor.RED + "Erro ao verificar saldo atual.");
                            return;
                        }
                        
                        BigDecimal difference = amount.subtract(currentBalance);
                        
                        if (difference.compareTo(BigDecimal.ZERO) > 0) {
                            // Precisa adicionar
                            PrimeLeagueAPI.getEconomyManager().creditBalanceAsync(targetPlayerId, difference.doubleValue(), "Admin Set by " + adminName + " (added " + difference + ")", (response) -> {
                                handleEcoResponse(sender, targetPlayer, targetName, response, "definido", adminName, action, amount);
                            });
                        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                            // Precisa remover
                            PrimeLeagueAPI.getEconomyManager().debitBalanceAsync(targetPlayerId, difference.abs().doubleValue(), "Admin Set by " + adminName + " (removed " + difference.abs() + ")", (response) -> {
                                handleEcoResponse(sender, targetPlayer, targetName, response, "definido", adminName, action, amount);
                            });
                        } else {
                            // Já está no valor correto
                            sender.sendMessage(ChatColor.YELLOW + "O saldo de " + targetName + " já está em $" + EconomyUtils.formatMoney(amount));
                        }
                    });
                    break;
            }

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro interno durante a operação.");
            plugin.getLogger().severe("Erro no comando /eco: " + e.getMessage());
        }
    }

    /**
     * Trata a resposta da operação econômica.
     * 
     * @param sender Quem executou o comando
     * @param targetPlayer Jogador alvo (se online)
     * @param targetName Nome do jogador alvo
     * @param response Resposta da operação
     * @param actionDescription Descrição da ação
     * @param adminName Nome do admin
     * @param action Ação executada
     * @param amount Quantia
     */
    private void handleEcoResponse(CommandSender sender, Player targetPlayer, String targetName, EconomyResponse response, String actionDescription, String adminName, String action, BigDecimal amount) {
        if (response != null && response.isSuccess()) {
            // Mensagem de sucesso
            sender.sendMessage(ChatColor.GREEN + "✅ Saldo de " + ChatColor.YELLOW + targetName + ChatColor.GREEN + " " + actionDescription + " com sucesso!");
            sender.sendMessage(ChatColor.GREEN + "💰 Novo saldo: " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(response.getNewBalance()));

            // Notificar o jogador se estiver online
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.sendMessage(ChatColor.GREEN + "💰 Seu saldo foi " + actionDescription + " por " + ChatColor.YELLOW + adminName);
                targetPlayer.sendMessage(ChatColor.GREEN + "💳 Novo saldo: " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(response.getNewBalance()));
            }

            // Log da ação administrativa
            plugin.getLogger().info("💰 [ECO-ADMIN] " + adminName + " " + action + " $" + amount + " para " + targetName + " (novo saldo: $" + response.getNewBalance() + ")");

        } else if (response != null) {
            // Mensagem de erro
            sender.sendMessage(ChatColor.RED + "❌ Erro na operação: " + response.getErrorMessage());
        }
    }


}
