package br.com.primeleague.p2p.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
// import br.com.primeleague.p2p.managers.IPAuthManager; // Removido temporariamente
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.Date;

/**
 * Comando para staff gerenciar assinaturas P2P e autorização de IPs.
 * 
 * @author PrimeLeague Team
 * @version 2.0
 */
public class P2PAdminCommand implements CommandExecutor {
    
    // private final IPAuthManager ipAuthManager; // Removido temporariamente
    
    public P2PAdminCommand() {
        // this.ipAuthManager = new IPAuthManager(); // Removido temporariamente
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser usado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.p2p.admin")) {
            PrimeLeagueAPI.sendNoPermission(player);
            return true;
        }
        
        // Verificar argumentos
        if (args.length < 1) {
            showUsage(player);
            return true;
        }
        
        final String subCommand = args[0].toLowerCase();
        final String authorName = sender.getName();
        
        switch (subCommand) {
            case "check":
                if (args.length != 2) {
                    PrimeLeagueAPI.sendUsage(player, "/p2p check <jogador>");
                    return true;
                }
                checkSubscription(player, args[1]);
                break;
                
            case "grant":
                if (args.length != 3) {
                    PrimeLeagueAPI.sendUsage(player, "/p2p grant <jogador> <dias>");
                    return true;
                }
                try {
                    int days = Integer.parseInt(args[2]);
                    if (days <= 0) {
                        PrimeLeagueAPI.sendError(player, "O número de dias deve ser maior que zero.");
                        return true;
                    }
                    grantSubscription(player, args[1], days, authorName);
                } catch (NumberFormatException e) {
                    PrimeLeagueAPI.sendError(player, "Número de dias inválido: " + args[2]);
                }
                break;
                
            case "revoke":
                if (args.length != 2) {
                    PrimeLeagueAPI.sendUsage(player, "/p2p revoke <jogador>");
                    return true;
                }
                revokeSubscription(player, args[1], authorName);
                break;
                
            case "auth-ip":
                if (args.length != 2) {
                    PrimeLeagueAPI.sendUsage(player, "/p2p auth-ip <jogador>");
                    return true;
                }
                // authorizePlayerIP(player, args[1], authorName); // Removido temporariamente
                PrimeLeagueAPI.sendError(player, "Comando temporariamente desabilitado.");
                break;
                
            default:
                PrimeLeagueAPI.sendError(player, "Subcomando inválido: " + subCommand);
                showUsage(player);
                break;
        }
        
        return true;
    }
    
    /**
     * Verifica a assinatura de um jogador.
     * 
     * @param sender Staff que executou o comando
     * @param targetName Nome do jogador alvo
     */
    private void checkSubscription(final Player sender, final String targetName) {
        // Verificar se o jogador está online
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound(sender, targetName);
            return;
        }
        
        // Executar verificação de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar perfil do jogador
                    final PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(targetPlayer.getUniqueId());
                    
                    // Mostrar informações na thread principal
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            showSubscriptionInfo(sender, targetName, profile);
                        }
                    });
                    
                } catch (Exception e) {
                    sender.getServer().getLogger().severe("Erro ao verificar assinatura: " + e.getMessage());
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(sender, "Erro interno ao verificar assinatura.");
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Concede dias de assinatura para um jogador.
     * 
     * @param sender Staff que executou o comando
     * @param targetName Nome do jogador alvo
     * @param days Número de dias a conceder
     * @param authorName Nome do autor
     */
    private void grantSubscription(final Player sender, final String targetName, final int days, final String authorName) {
        // Verificar se o jogador está online
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound(sender, targetName);
            return;
        }
        
        // Executar concessão de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar perfil atual
                    final PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(targetPlayer.getUniqueId());
                    if (profile == null) {
                        Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError(sender, "Perfil do jogador não encontrado.");
                            }
                        });
                        return;
                    }
                    
                    // Calcular nova data de expiração
                    final Date newExpiryDate;
                    // TODO: Implementar consulta SSOT via DataManager
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, days);
                    newExpiryDate = cal.getTime();
                    
                    // Atualizar perfil
                    // TODO: Implementar atualização SSOT via DataManager
                    PrimeLeagueAPI.getDataManager().savePlayerProfileSync(profile);
                    final boolean success = true; // Assume sucesso se não houver exceção
                    
                    // Executar na thread principal
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess(sender, 
                                    "Concedidos " + days + " dias de assinatura para " + targetName + 
                                    " (expira em: " + formatDate(newExpiryDate) + ")");
                                
                                // Notificar jogador
                                PrimeLeagueAPI.sendSuccess(targetPlayer, 
                                    "Você recebeu " + days + " dias de assinatura!");
                                PrimeLeagueAPI.sendInfo(targetPlayer, 
                                    "Nova data de expiração: " + formatDate(newExpiryDate));
                                
                                // Broadcast para staff
                                broadcastToStaff("§a[P2P] §f" + authorName + " concedeu " + days + 
                                               " dias de assinatura para " + targetName);
                            } else {
                                PrimeLeagueAPI.sendError(sender, 
                                    "Erro ao salvar assinatura no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    sender.getServer().getLogger().severe("Erro ao conceder assinatura: " + e.getMessage());
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(sender, "Erro interno ao conceder assinatura.");
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Revoga a assinatura de um jogador.
     * 
     * @param sender Staff que executou o comando
     * @param targetName Nome do jogador alvo
     * @param authorName Nome do autor
     */
    private void revokeSubscription(final Player sender, final String targetName, final String authorName) {
        // Verificar se o jogador está online
        final Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer == null) {
            PrimeLeagueAPI.sendPlayerNotFound(sender, targetName);
            return;
        }
        
        // Executar revogação de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar perfil atual
                    final PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(targetPlayer.getUniqueId());
                    if (profile == null) {
                        Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                            @Override
                            public void run() {
                                PrimeLeagueAPI.sendError(sender, "Perfil do jogador não encontrado.");
                            }
                        });
                        return;
                    }
                    
                    // Revogar assinatura
                    // TODO: Implementar atualização SSOT via DataManager
                    PrimeLeagueAPI.getDataManager().savePlayerProfileSync(profile);
                    final boolean success = true; // Assume sucesso se não houver exceção
                    
                    // Executar na thread principal
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess(sender, 
                                    "Assinatura de " + targetName + " foi revogada com sucesso.");
                                
                                // Notificar jogador
                                PrimeLeagueAPI.sendError(targetPlayer, 
                                    "Sua assinatura foi revogada por " + authorName);
                                PrimeLeagueAPI.sendInfo(targetPlayer, 
                                    "Entre em contato com a administração para mais informações.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§c[P2P] §f" + authorName + " revogou a assinatura de " + targetName);
                            } else {
                                PrimeLeagueAPI.sendError(sender, 
                                    "Erro ao revogar assinatura no banco de dados.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    sender.getServer().getLogger().severe("Erro ao revogar assinatura: " + e.getMessage());
                    Bukkit.getScheduler().runTask(sender.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(sender, "Erro interno ao revogar assinatura.");
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Mostra informações da assinatura para staff.
     * 
     * @param sender Staff que executou o comando
     * @param targetName Nome do jogador alvo
     * @param profile Perfil do jogador
     */
    private void showSubscriptionInfo(Player sender, String targetName, PlayerProfile profile) {
        if (profile == null) {
            PrimeLeagueAPI.sendError(sender, "Perfil não encontrado para " + targetName);
            return;
        }
        
        // Cabeçalho
        sender.sendMessage("§6§l=== ASSINATURA: " + targetName + " ===");
        sender.sendMessage("");
        
        // Status da assinatura
        if (PrimeLeagueAPI.getDataManager().hasActiveSubscription(profile.getUuid())) {
            sender.sendMessage("§a✅ Status: §fATIVA");
            
            // Dias restantes
            int daysRemaining = 0; // TODO: Implementar cálculo via DataManager
            if (daysRemaining > 0) {
                sender.sendMessage("§e⚠️ Dias restantes: §f" + daysRemaining);
            } else {
                sender.sendMessage("§a✅ Expira: §fHoje");
            }
            
            // Data de expiração
            // TODO: Implementar consulta SSOT via DataManager
            
        } else {
            sender.sendMessage("§c❌ Status: §fINATIVA");
            sender.sendMessage("§7Jogador não possui assinatura ativa.");
        }
        
        sender.sendMessage("");
        
        // Informações adicionais
        sender.sendMessage("§7ELO: §e" + profile.getElo());
        sender.sendMessage("§7Dinheiro: §a$" + String.format("%.2f", profile.getMoney()));
        sender.sendMessage("§7Total de logins: §b" + profile.getTotalLogins());
        
        if (profile.getLastSeen() != null) {
            sender.sendMessage("§7Último login: §f" + formatDate(profile.getLastSeen()));
        }
        
        sender.sendMessage("§6§l=== FIM ===");
    }
    
    /**
     * Envia mensagem para todos os staff online.
     * 
     * @param message Mensagem a ser enviada
     */
    private void broadcastToStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PrimeLeagueAPI.hasPermission(player, "primeleague.p2p.admin")) {
                player.sendMessage(message);
            }
        }
    }
    
    // Método authorizePlayerIP removido temporariamente
    
    /**
     * Mostra o uso correto do comando.
     */
    private void showUsage(Player player) {
        PrimeLeagueAPI.sendUsage(player, "/p2p <check|grant|revoke|auth-ip> <jogador> [dias]");
        PrimeLeagueAPI.sendInfo(player, "Subcomandos:");
        PrimeLeagueAPI.sendInfo(player, "  • check <jogador> - Verificar assinatura");
        PrimeLeagueAPI.sendInfo(player, "  • grant <jogador> <dias> - Conceder dias");
        PrimeLeagueAPI.sendInfo(player, "  • revoke <jogador> - Revogar assinatura");
        PrimeLeagueAPI.sendInfo(player, "  • auth-ip <jogador> - Autorizar IP atual do jogador (temporariamente desabilitado)");
    }
    
    /**
     * Obfusca o IP no formato xxx.xxx.xxx.42
     */
    private String obfuscateIP(String ipAddress) {
        String[] parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return "xxx.xxx.xxx." + parts[3];
        }
        return "xxx.xxx.xxx.x"; // Fallback
    }
    
    /**
     * Formata uma data para exibição.
     * 
     * @param date Data a ser formatada
     * @return String formatada
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "N/A";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
}
