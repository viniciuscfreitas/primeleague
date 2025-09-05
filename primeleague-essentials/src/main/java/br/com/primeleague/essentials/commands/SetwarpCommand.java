package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.WarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * Comando para criar warps públicos (administradores).
 * Implementa /setwarp <nome> [custo] [permissão] com validações completas.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class SetwarpCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final WarpManager warpManager;
    
    /**
     * Construtor do SetwarpCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param warpManager Instância do WarpManager
     */
    public SetwarpCommand(EssentialsPlugin plugin, WarpManager warpManager) {
        this.plugin = plugin;
        this.warpManager = warpManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar argumentos mínimos
        if (args.length < 1) {
            String usage = plugin.getConfig().getString("warps.messages.setwarp.usage", "§cUso: /setwarp <nome> [custo] [permissão]");
            player.sendMessage(usage);
            return true;
        }
        
        String warpName = args[0].trim();
        
        // Validações do nome
        if (warpName.isEmpty()) {
            String usage = plugin.getConfig().getString("warps.messages.setwarp.usage", "§cUso: /setwarp <nome> [custo] [permissão]");
            player.sendMessage(usage);
            return true;
        }
        
        if (warpName.length() > 32) {
            String errorMsg = plugin.getConfig().getString("warps.messages.setwarp.name-too-long", "§cNome do warp muito longo! Máximo 32 caracteres.");
            player.sendMessage(errorMsg);
            return true;
        }
        
        if (!isValidWarpName(warpName)) {
            String errorMsg = plugin.getConfig().getString("warps.messages.setwarp.invalid-name", "§cNome do warp inválido! Use apenas letras, números e underscore.");
            player.sendMessage(errorMsg);
            return true;
        }
        
        // Processar custo (opcional)
        BigDecimal cost = BigDecimal.ZERO;
        if (args.length >= 2) {
            try {
                double costValue = Double.parseDouble(args[1]);
                if (costValue < 0) {
                    String errorMsg = plugin.getConfig().getString("warps.messages.setwarp.invalid-cost", "§cCusto inválido! Use um número positivo.");
                    player.sendMessage(errorMsg);
                    return true;
                }
                cost = BigDecimal.valueOf(costValue);
            } catch (NumberFormatException e) {
                String errorMsg = plugin.getConfig().getString("warps.messages.setwarp.invalid-cost", "§cCusto inválido! Use um número positivo.");
                player.sendMessage(errorMsg);
                return true;
            }
        }
        
        // Processar permissão (opcional)
        String permissionNode = null;
        if (args.length >= 3) {
            permissionNode = args[2].trim();
            if (permissionNode.isEmpty()) {
                permissionNode = null;
            }
        }
        
        // HARDENING: Verificar se o jogador ainda está online antes de iniciar operação assíncrona
        if (!player.isOnline()) {
            return true; // Jogador não está mais online, abortar
        }
        
        // Usar arrays para compatibilidade Java 7 (variáveis efetivamente finais)
        final String[] finalWarpName = {warpName};
        final BigDecimal[] finalCost = {cost};
        final String[] finalPermissionNode = {permissionNode};
        
        // Criar warp de forma assíncrona
        warpManager.createWarpAsync(player, finalWarpName[0], finalCost[0], finalPermissionNode[0], (success) -> {
            // HARDENING: Verificar novamente se o jogador ainda está online antes de enviar mensagem
            if (!player.isOnline()) {
                return; // Jogador não está mais online, abortar callback
            }
            
            if (success) {
                // Mensagens de sucesso já são enviadas pelo WarpManager
                // Aqui apenas logamos que a operação foi bem-sucedida
                plugin.getLogger().info("✅ Warp criado: " + finalWarpName[0] + " por " + player.getName() + " (custo: $" + finalCost[0] + ", permissão: " + finalPermissionNode[0] + ")");
            } else {
                // Mensagens de erro já são enviadas pelo WarpManager
                // Aqui apenas logamos que a operação falhou
                plugin.getLogger().warning("⚠️ Falha na criação do warp " + finalWarpName[0] + " por " + player.getName());
            }
        });
        
        return true;
    }
    
    /**
     * Valida se o nome do warp é válido.
     * 
     * @param warpName Nome do warp
     * @return true se válido
     */
    private boolean isValidWarpName(String warpName) {
        if (warpName == null || warpName.trim().isEmpty()) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, underscore)
        return warpName.matches("^[a-zA-Z0-9_]+$");
    }
}
