package br.com.primeleague.chat.commands;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.chat.services.LogRotationService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comando para gerenciar o sistema de rotação de logs.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class LogRotationCommand implements CommandExecutor, TabCompleter {
    
    private final PrimeLeagueChat plugin;
    private final LogRotationService logRotationService;
    
    public LogRotationCommand(PrimeLeagueChat plugin, LogRotationService logRotationService) {
        this.plugin = plugin;
        this.logRotationService = logRotationService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("primeleague.chat.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "status":
                showStatus(sender);
                break;
                
            case "manual":
                performManualRotation(sender);
                break;
                
            case "start":
                startAutomaticRotation(sender);
                break;
                
            case "config":
                showConfiguration(sender);
                break;
                
            case "help":
                showHelp(sender);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Subcomando desconhecido: " + subCommand);
                showHelp(sender);
                break;
        }
        
        return true;
    }
    
    /**
     * Mostra o status do sistema de rotação.
     */
    private void showStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Status do Sistema de Rotação de Logs ===");
        sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.WHITE + 
                          (logRotationService.isRunning() ? "Executando" : "Parado"));
        sender.sendMessage(ChatColor.YELLOW + "Estatísticas: " + ChatColor.WHITE + 
                          logRotationService.getRotationStats());
        
        // Mostrar configurações atuais
        boolean enabled = plugin.getConfig().getBoolean("log_rotation.enabled", true);
        int retentionDays = plugin.getConfig().getInt("log_rotation.retention_days", 30);
        int intervalHours = plugin.getConfig().getInt("log_rotation.interval_hours", 24);
        boolean archiveEnabled = plugin.getConfig().getBoolean("log_rotation.archive_enabled", true);
        
        sender.sendMessage(ChatColor.YELLOW + "Habilitado: " + ChatColor.WHITE + enabled);
        sender.sendMessage(ChatColor.YELLOW + "Retenção: " + ChatColor.WHITE + retentionDays + " dias");
        sender.sendMessage(ChatColor.YELLOW + "Intervalo: " + ChatColor.WHITE + intervalHours + " horas");
        sender.sendMessage(ChatColor.YELLOW + "Arquivamento: " + ChatColor.WHITE + archiveEnabled);
    }
    
    /**
     * Executa rotação manual de logs.
     */
    private void performManualRotation(final CommandSender sender) {
        if (logRotationService.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Sistema de rotação já está em execução!");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Iniciando rotação manual de logs...");
        
        // Executar em thread separada para não bloquear o comando
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    logRotationService.performManualRotation();
                    sender.sendMessage(ChatColor.GREEN + "✅ Rotação manual concluída com sucesso!");
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "❌ Erro durante rotação manual: " + e.getMessage());
                    plugin.getLogger().severe("Erro na rotação manual: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Inicia a rotação automática.
     */
    private void startAutomaticRotation(final CommandSender sender) {
        if (logRotationService.isRunning()) {
            sender.sendMessage(ChatColor.RED + "Sistema de rotação já está em execução!");
            return;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Iniciando sistema de rotação automática...");
        
        // Executar em thread separada
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    logRotationService.startAutomaticRotation();
                    sender.sendMessage(ChatColor.GREEN + "✅ Sistema de rotação automática iniciado!");
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "❌ Erro ao iniciar rotação automática: " + e.getMessage());
                    plugin.getLogger().severe("Erro ao iniciar rotação automática: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Mostra a configuração atual.
     */
    private void showConfiguration(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Configuração do Sistema de Rotação ===");
        
        boolean enabled = plugin.getConfig().getBoolean("log_rotation.enabled", true);
        int retentionDays = plugin.getConfig().getInt("log_rotation.retention_days", 30);
        int intervalHours = plugin.getConfig().getInt("log_rotation.interval_hours", 24);
        boolean archiveEnabled = plugin.getConfig().getBoolean("log_rotation.archive_enabled", true);
        String archivePath = plugin.getConfig().getString("log_rotation.archive_path", "logs/chat_archive");
        boolean cleanupFileLogs = plugin.getConfig().getBoolean("log_rotation.cleanup_file_logs", true);
        
        sender.sendMessage(ChatColor.YELLOW + "Habilitado: " + ChatColor.WHITE + enabled);
        sender.sendMessage(ChatColor.YELLOW + "Dias de Retenção: " + ChatColor.WHITE + retentionDays);
        sender.sendMessage(ChatColor.YELLOW + "Intervalo (horas): " + ChatColor.WHITE + intervalHours);
        sender.sendMessage(ChatColor.YELLOW + "Arquivamento: " + ChatColor.WHITE + archiveEnabled);
        sender.sendMessage(ChatColor.YELLOW + "Caminho do Arquivo: " + ChatColor.WHITE + archivePath);
        sender.sendMessage(ChatColor.YELLOW + "Limpar Logs de Arquivo: " + ChatColor.WHITE + cleanupFileLogs);
        
        sender.sendMessage(ChatColor.GRAY + "Para alterar configurações, edite o config.yml");
    }
    
    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Comandos de Rotação de Logs ===");
        sender.sendMessage(ChatColor.YELLOW + "/logrotation status " + ChatColor.WHITE + "- Mostra o status atual");
        sender.sendMessage(ChatColor.YELLOW + "/logrotation manual " + ChatColor.WHITE + "- Executa rotação manual");
        sender.sendMessage(ChatColor.YELLOW + "/logrotation start " + ChatColor.WHITE + "- Inicia rotação automática");
        sender.sendMessage(ChatColor.YELLOW + "/logrotation config " + ChatColor.WHITE + "- Mostra configurações");
        sender.sendMessage(ChatColor.YELLOW + "/logrotation help " + ChatColor.WHITE + "- Mostra esta ajuda");
        
        sender.sendMessage(ChatColor.GRAY + "Permissão necessária: primeleague.chat.admin");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("primeleague.chat.admin")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("status", "manual", "start", "config", "help");
            List<String> completions = new ArrayList<>();
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
            
            return completions;
        }
        
        return new ArrayList<>();
    }
}
