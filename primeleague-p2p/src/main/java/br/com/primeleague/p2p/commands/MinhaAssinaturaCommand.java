package br.com.primeleague.p2p.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para jogadores verem informações de sua assinatura.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class MinhaAssinaturaCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c❌ Este comando só pode ser usado por jogadores!");
            return true;
        }
        
        final Player player = (Player) sender;
        
        // Executar busca de forma assíncrona
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(player.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
            @Override
            public void run() {
                try {
                    // Buscar perfil do jogador
                    final PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(player.getUniqueId());
                    
                    // Mostrar informações na thread principal
                    org.bukkit.Bukkit.getScheduler().runTask(player.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            showSubscriptionInfo(player, profile);
                        }
                    });
                    
                } catch (Exception e) {
                    player.getServer().getLogger().severe("Erro ao buscar assinatura: " + e.getMessage());
                    org.bukkit.Bukkit.getScheduler().runTask(player.getServer().getPluginManager().getPlugin("PrimeLeague-P2P"), new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError(player, "Erro interno ao buscar informações da assinatura.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
    
    /**
     * Mostra as informações da assinatura para o jogador.
     * 
     * @param player Jogador
     * @param profile Perfil do jogador
     */
    private void showSubscriptionInfo(Player player, PlayerProfile profile) {
        if (profile == null) {
            PrimeLeagueAPI.sendError(player, "Perfil não encontrado. Entre em contato com a administração.");
            return;
        }
        
        // Cabeçalho
        player.sendMessage("§6§l=== MINHA ASSINATURA ===");
        player.sendMessage("");
        
        // Status da assinatura
        if (PrimeLeagueAPI.getDataManager().hasActiveSubscription(player.getUniqueId())) {
            player.sendMessage("§a✅ Status: §fATIVA");
            
            // Dias restantes
            int daysRemaining = 0; // TODO: Implementar cálculo via DataManager
            if (daysRemaining > 0) {
                if (daysRemaining == 1) {
                    player.sendMessage("§e⚠️ Expira: §fAmanhã");
                } else {
                    player.sendMessage("§e⚠️ Expira em: §f" + daysRemaining + " dias");
                }
            } else {
                player.sendMessage("§a✅ Expira: §fHoje");
            }
            
            // Data de expiração
            // TODO: Implementar consulta SSOT via DataManager
            
        } else {
            player.sendMessage("§c❌ Status: §fINATIVA");
            player.sendMessage("§7Você não possui uma assinatura ativa.");
        }
        
        player.sendMessage("");
        
        // Informações adicionais
        player.sendMessage("§7ELO atual: §e" + profile.getElo());
        player.sendMessage("§7Dinheiro: §a$" + String.format("%.2f", profile.getMoney()));
        player.sendMessage("§7Total de logins: §b" + profile.getTotalLogins());
        
        if (profile.getLastSeen() != null) {
            player.sendMessage("§7Último login: §f" + formatDate(profile.getLastSeen()));
        }
        
        player.sendMessage("");
        
        // Mensagens de ajuda
        if (!PrimeLeagueAPI.getDataManager().hasActiveSubscription(player.getUniqueId())) {
            player.sendMessage("§c💡 Para renovar sua assinatura:");
            player.sendMessage("§7  • Entre em contato com a administração");
            player.sendMessage("§7  • Use o comando /tickets para solicitar renovação");
        } else if (false) { // TODO: Implementar verificação via DataManager
            player.sendMessage("§e💡 Sua assinatura expira em breve!");
            player.sendMessage("§7  • Entre em contato com a administração para renovar");
        }
        
        player.sendMessage("§6§l=== FIM ===");
    }
    
    /**
     * Formata uma data para exibição.
     * 
     * @param date Data a ser formatada
     * @return String formatada
     */
    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "N/A";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
}
