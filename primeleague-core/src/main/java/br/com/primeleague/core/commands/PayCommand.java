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
 * Comando /pagar para transferências entre jogadores.
 * 
 * Uso: /pagar <jogador> <quantia>
 * 
 * Permissão: primeleague.pay
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class PayCommand implements CommandExecutor {

    private final PrimeLeagueCore plugin;

    public PayCommand(PrimeLeagueCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores.");
            return true;
        }
        
        Player player = (Player) sender;

        // Verificar permissão
        if (!player.hasPermission("primeleague.pay")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        // Verificar argumentos
        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Uso correto: /pagar <jogador> <quantia>");
            return true;
        }

        String targetName = args[0];
        String amountStr = args[1];

        // Validar quantia
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                player.sendMessage(ChatColor.RED + "A quantia deve ser maior que zero.");
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantia inválida. Use números (ex: 100.50).");
            return true;
        }

        // Verificar se não está tentando pagar para si mesmo
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "Você não pode pagar para si mesmo.");
            return true;
        }

        // Executar transferência
        executeTransfer(player, targetName, amount);
        return true;
    }

    /**
     * Executa a transferência entre jogadores.
     * 
     * @param sender Jogador que está enviando o dinheiro
     * @param targetName Nome do jogador que receberá
     * @param amount Quantia a transferir
     */
    private void executeTransfer(Player sender, String targetName, BigDecimal amount) {
        try {
            // Obter player_id do remetente
            Integer fromPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(sender.getUniqueId());
            if (fromPlayerId == null) {
                sender.sendMessage(ChatColor.RED + "Erro interno: não foi possível identificar sua conta.");
                return;
            }

            // Obter player_id do destinatário
            Integer toPlayerId = null;
            Player targetPlayer = Bukkit.getPlayerExact(targetName);
            
            if (targetPlayer != null) {
                // Jogador online
                toPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(targetPlayer.getUniqueId());
            } else {
                // Jogador offline, buscar diretamente o player_id (otimizado)
                toPlayerId = PrimeLeagueAPI.getDataManager().getPlayerIdByName(targetName);
            }

            if (toPlayerId == null) {
                sender.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado.");
                return;
            }

            // REFATORADO: Usar métodos assíncronos para evitar bloqueio da thread principal
            sender.sendMessage(ChatColor.YELLOW + "⏳ Processando transferência...");

            // Primeiro verificar saldo de forma assíncrona
            PrimeLeagueAPI.getEconomyManager().getBalanceAsync(fromPlayerId, (currentBalance) -> {
                // HARDENING: Verificar se o sender ainda está online
                if (!sender.isOnline()) {
                    return; // Sender não está mais online, abortar callback
                }
                
                if (currentBalance == null) {
                    sender.sendMessage(ChatColor.RED + "Erro ao verificar saldo.");
                    return;
                }

                if (currentBalance.compareTo(amount) < 0) {
                    sender.sendMessage(ChatColor.RED + "Saldo insuficiente. Seu saldo atual: $" + EconomyUtils.formatMoney(currentBalance));
                    return;
                }

                // Se tem saldo suficiente, realizar transferência assíncrona
                PrimeLeagueAPI.getEconomyManager().transferAsync(fromPlayerId, toPlayerId, amount.doubleValue(), (response) -> {
                    // HARDENING: Verificar se o sender ainda está online
                    if (!sender.isOnline()) {
                        return; // Sender não está mais online, abortar callback
                    }
                    
                    if (response.isSuccess()) {
                        // Mensagem de sucesso para o remetente
                        sender.sendMessage(ChatColor.GREEN + "✅ Transferência realizada com sucesso!");
                        sender.sendMessage(ChatColor.GREEN + "💰 Enviado para " + ChatColor.YELLOW + targetName + ChatColor.GREEN + ": " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(amount));
                        sender.sendMessage(ChatColor.GREEN + "💳 Seu novo saldo: " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(response.getNewBalance()));

                        // Mensagem para o destinatário (se online)
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            targetPlayer.sendMessage(ChatColor.GREEN + "💰 Você recebeu " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(amount) + ChatColor.GREEN + " de " + ChatColor.YELLOW + sender.getName());
                            
                            // Atualizar saldo do destinatário no cache de forma assíncrona
                            PrimeLeagueAPI.getEconomyManager().getBalanceAsync(toPlayerId, (newBalance) -> {
                                // HARDENING: Verificar se o targetPlayer ainda está online
                                if (targetPlayer == null || !targetPlayer.isOnline()) {
                                    return; // Target player não está mais online, abortar callback
                                }
                                
                                if (newBalance != null) {
                                    targetPlayer.sendMessage(ChatColor.GREEN + "💳 Seu novo saldo: " + ChatColor.GOLD + "$" + EconomyUtils.formatMoney(newBalance));
                                }
                            });
                        }

                        // Log da transação
                        plugin.getLogger().info("💰 [PAY] " + sender.getName() + " transferiu $" + amount + " para " + targetName);

                    } else {
                        // Mensagem de erro
                        sender.sendMessage(ChatColor.RED + "❌ Erro na transferência: " + response.getErrorMessage());
                    }
                });
            });

        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Erro interno durante a transferência.");
            plugin.getLogger().severe("Erro no comando /pagar: " + e.getMessage());
        }
    }


}
