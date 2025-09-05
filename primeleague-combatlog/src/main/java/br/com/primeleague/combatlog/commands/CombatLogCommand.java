package br.com.primeleague.combatlog.commands;

import br.com.primeleague.combatlog.CombatLogPlugin;
import br.com.primeleague.combatlog.models.CombatTaggedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Comando principal para gerenciar o sistema de combat log.
 * Fornece funcionalidades para staff monitorar e controlar o sistema.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatLogCommand implements CommandExecutor {
    
    private final CombatLogPlugin plugin;
    
    /**
     * Construtor do comando.
     * 
     * @param plugin Instância do plugin principal
     */
    public CombatLogCommand(CombatLogPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("primeleague.admin.combatlog")) {
            sender.sendMessage("❌ Você não tem permissão para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "check":
                if (args.length < 2) {
                    sender.sendMessage("❌ Uso: /combatlog check <jogador>");
                    return true;
                }
                handleCheck(sender, args[1]);
                break;
                
            case "force":
                if (args.length < 4) {
                    sender.sendMessage("❌ Uso: /combatlog force <jogador> <duração> <razão>");
                    return true;
                }
                handleForce(sender, args[1], args[2], args[3]);
                break;
                
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("❌ Uso: /combatlog remove <jogador>");
                    return true;
                }
                handleRemove(sender, args[1]);
                break;
                
            case "history":
                if (args.length < 2) {
                    sender.sendMessage("❌ Uso: /combatlog history <jogador>");
                    return true;
                }
                handleHistory(sender, args[1]);
                break;
                
            case "stats":
                handleStats(sender);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "list":
                handleList(sender);
                break;
                
            default:
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Mostra a ajuda do comando.
     * 
     * @param sender Remetente do comando
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage("⚔️ === Comandos de Combat Log ===");
        sender.sendMessage("§e/combatlog check <jogador> §7- Verifica status de combate");
        sender.sendMessage("§e/combatlog force <jogador> <duração> <razão> §7- Força tag de combate");
        sender.sendMessage("§e/combatlog remove <jogador> §7- Remove tag de combate");
        sender.sendMessage("§e/combatlog history <jogador> §7- Mostra histórico de combat logs");
        sender.sendMessage("§e/combatlog stats §7- Mostra estatísticas do sistema");
        sender.sendMessage("§e/combatlog reload §7- Recarrega configurações");
        sender.sendMessage("§e/combatlog list §7- Lista jogadores em combate");
    }
    
    /**
     * Handler para o subcomando check.
     * 
     * @param sender Remetente do comando
     * @param playerName Nome do jogador
     */
    private void handleCheck(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("❌ Jogador " + playerName + " não encontrado ou offline.");
            return;
        }
        
        UUID targetUuid = target.getUniqueId();
        CombatTaggedPlayer taggedPlayer = plugin.getCombatLogManager().getTaggedPlayer(targetUuid);
        
        if (taggedPlayer == null) {
            sender.sendMessage("✅ " + playerName + " não está em combate.");
        } else {
            sender.sendMessage("⚔️ === Status de Combate ===");
            sender.sendMessage("§eJogador: §7" + taggedPlayer.getPlayerName());
            sender.sendMessage("§eTempo restante: §7" + taggedPlayer.getFormattedRemainingTime());
            sender.sendMessage("§eZona: §7" + taggedPlayer.getZoneType());
            sender.sendMessage("§eRazão: §7" + taggedPlayer.getCombatReason());
            sender.sendMessage("§eDuração total: §7" + taggedPlayer.getCombatDuration() + "s");
        }
    }
    
    /**
     * Handler para o subcomando force.
     * 
     * @param sender Remetente do comando
     * @param playerName Nome do jogador
     * @param durationStr Duração em segundos
     * @param reason Razão do tag
     */
    private void handleForce(CommandSender sender, String playerName, String durationStr, String reason) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("❌ Jogador " + playerName + " não encontrado ou offline.");
            return;
        }
        
        int duration;
        try {
            duration = Integer.parseInt(durationStr);
            if (duration <= 0) {
                sender.sendMessage("❌ Duração deve ser maior que 0 segundos.");
                return;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage("❌ Duração inválida: " + durationStr);
            return;
        }
        
        // Aplicar tag forçado
        plugin.getCombatLogManager().forceTagPlayer(target, duration, reason);
        
        sender.sendMessage("✅ Tag de combate forçado aplicado para " + playerName + 
                          " por " + duration + " segundos.");
        
        // Log da ação
        plugin.getLogger().info("⚔️ Tag de combate forçado por " + sender.getName() + 
                               " para " + playerName + " - Duração: " + duration + "s - Razão: " + reason);
    }
    
    /**
     * Handler para o subcomando remove.
     * 
     * @param sender Remetente do comando
     * @param playerName Nome do jogador
     */
    private void handleRemove(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("❌ Jogador " + playerName + " não encontrado ou offline.");
            return;
        }
        
        UUID targetUuid = target.getUniqueId();
        CombatTaggedPlayer taggedPlayer = plugin.getCombatLogManager().getTaggedPlayer(targetUuid);
        
        if (taggedPlayer == null) {
            sender.sendMessage("❌ " + playerName + " não está em combate.");
            return;
        }
        
        // Remover tag
        plugin.getCombatLogManager().removeTag(targetUuid);
        
        sender.sendMessage("✅ Tag de combate removido de " + playerName + ".");
        target.sendMessage("✅ Seu tag de combate foi removido por um administrador.");
        
        // Log da ação
        plugin.getLogger().info("⚔️ Tag de combate removido por " + sender.getName() + " de " + playerName);
    }
    
    /**
     * Handler para o subcomando history.
     * 
     * @param sender Remetente do comando
     * @param playerName Nome do jogador
     */
    private void handleHistory(CommandSender sender, String playerName) {
        // TODO: Implementar histórico do banco de dados
        sender.sendMessage("📝 Histórico de combat logs para " + playerName + ":");
        sender.sendMessage("§7Funcionalidade em desenvolvimento...");
    }
    
    /**
     * Handler para o subcomando stats.
     * 
     * @param sender Remetente do comando
     */
    private void handleStats(CommandSender sender) {
        sender.sendMessage("📊 === Estatísticas do Sistema ===");
        sender.sendMessage(plugin.getCombatLogManager().getStats());
        sender.sendMessage(plugin.getZoneManager().getZoneStats());
        sender.sendMessage(plugin.getPunishmentService().getPunishmentStats());
    }
    
    /**
     * Handler para o subcomando reload.
     * 
     * @param sender Remetente do comando
     */
    private void handleReload(CommandSender sender) {
        try {
            plugin.reloadPlugin();
            sender.sendMessage("✅ Configurações recarregadas com sucesso!");
        } catch (Exception e) {
            sender.sendMessage("❌ Erro ao recarregar configurações: " + e.getMessage());
            plugin.getLogger().severe("Erro ao recarregar configurações: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handler para o subcomando list.
     * 
     * @param sender Remetente do comando
     */
    private void handleList(CommandSender sender) {
        Map<UUID, CombatTaggedPlayer> taggedPlayers = plugin.getCombatLogManager().getAllTaggedPlayers();
        
        if (taggedPlayers.isEmpty()) {
            sender.sendMessage("✅ Nenhum jogador está em combate no momento.");
            return;
        }
        
        sender.sendMessage("⚔️ === Jogadores em Combate ===");
        int count = 1;
        
        for (CombatTaggedPlayer taggedPlayer : taggedPlayers.values()) {
            if (!taggedPlayer.isExpired()) {
                sender.sendMessage("§e" + count + ". §7" + taggedPlayer.getPlayerName() + 
                                 " - " + taggedPlayer.getFormattedRemainingTime() + 
                                 " - " + taggedPlayer.getZoneType());
                count++;
            }
        }
        
        sender.sendMessage("§7Total: " + (count - 1) + " jogador(es) em combate.");
    }
}
