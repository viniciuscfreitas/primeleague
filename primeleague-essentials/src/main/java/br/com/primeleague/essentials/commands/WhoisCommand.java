package br.com.primeleague.essentials.commands;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.managers.PlayerInfoManager;
import br.com.primeleague.essentials.models.PlayerDossier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Comando para exibir dossiê público de um jogador.
 * Exibe informações públicas relevantes para gameplay (identidade, estatísticas, clã, essentials).
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class WhoisCommand implements CommandExecutor {
    
    private final EssentialsPlugin plugin;
    private final PlayerInfoManager playerInfoManager;
    
    /**
     * Construtor do WhoisCommand.
     * 
     * @param plugin Instância do plugin principal
     * @param playerInfoManager Instância do PlayerInfoManager
     */
    public WhoisCommand(EssentialsPlugin plugin, PlayerInfoManager playerInfoManager) {
        this.plugin = plugin;
        this.playerInfoManager = playerInfoManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cEste comando só pode ser executado por jogadores!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Verificar permissão
        if (!player.hasPermission("primeleague.essentials.whois")) {
            player.sendMessage("§cVocê não tem permissão para usar este comando!");
            return true;
        }
        
        // Validar argumentos
        if (args.length != 1) {
            String usage = plugin.getConfig().getString("player-info.whois.messages.usage", "§cUso: /whois <jogador>");
            player.sendMessage(usage);
            return true;
        }
        
        String targetPlayerName = args[0];
        
        // Verificar se o jogador alvo existe
        playerInfoManager.playerExistsAsync(targetPlayerName, new Consumer<Boolean>() {
            @Override
            public void accept(Boolean exists) {
                if (!exists) {
                    String message = plugin.getConfig().getString("player-info.whois.messages.player-not-found", 
                        "§cJogador §e{player} §cnão encontrado!");
                    player.sendMessage(message.replace("{player}", targetPlayerName));
                    return;
                }
                
                // Mostrar mensagem de carregamento
                String loadingMessage = plugin.getConfig().getString("player-info.whois.messages.loading", 
                    "§7Carregando informações de §e{player}§7...");
                player.sendMessage(loadingMessage.replace("{player}", targetPlayerName));
                
                // Obter dossiê completo
                playerInfoManager.getPlayerDossierAsync(targetPlayerName, new Consumer<PlayerDossier>() {
                    @Override
                    public void accept(PlayerDossier dossier) {
                        if (dossier == null) {
                            String message = plugin.getConfig().getString("player-info.whois.messages.player-not-found", 
                                "§cJogador §e{player} §cnão encontrado!");
                            player.sendMessage(message.replace("{player}", targetPlayerName));
                            return;
                        }
                        
                        // Exibir dossiê completo
                        displayPlayerDossier(player, dossier);
                    }
                });
            }
        });
        
        return true;
    }
    
    /**
     * Exibe o dossiê completo do jogador.
     */
    private void displayPlayerDossier(Player player, PlayerDossier dossier) {
        String playerName = dossier.getPlayerName();
        
        // Cabeçalho
        String header = plugin.getConfig().getString("player-info.whois.layout.header", 
            "§6§l=== §eINFORMAÇÕES DE {player} §6§l===");
        player.sendMessage(header.replace("{player}", playerName));
        player.sendMessage("");
        
        // Informações Básicas
        String basicTitle = plugin.getConfig().getString("player-info.whois.sections.basic-info.title", 
            "§6§lInformações Básicas");
        player.sendMessage(basicTitle);
        
        String onlineStatus = plugin.getConfig().getString("player-info.whois.sections.basic-info.online-status", 
            "§7Status: {status}");
        player.sendMessage(onlineStatus.replace("{status}", dossier.getFormattedStatus()));
        
        if (!dossier.isOnline()) {
            String lastSeen = plugin.getConfig().getString("player-info.whois.sections.basic-info.last-seen", 
                "§7Última vez visto: {time}");
            player.sendMessage(lastSeen.replace("{time}", dossier.getFormattedLastSeen()));
        }
        player.sendMessage("");
        
        // Identidade
        String identityTitle = plugin.getConfig().getString("player-info.whois.sections.identity.title", 
            "§6§lIdentidade");
        player.sendMessage(identityTitle);
        
        String displayName = plugin.getConfig().getString("player-info.whois.sections.identity.display-name", 
            "§7Nome: §e{display_name}");
        player.sendMessage(displayName.replace("{display_name}", dossier.getDisplayName()));
        
        String rank = plugin.getConfig().getString("player-info.whois.sections.identity.rank", 
            "§7Rank: {rank}");
        player.sendMessage(rank.replace("{rank}", dossier.getRank()));
        player.sendMessage("");
        
        // Estatísticas
        String statsTitle = plugin.getConfig().getString("player-info.whois.sections.stats.title", 
            "§6§lEstatísticas");
        player.sendMessage(statsTitle);
        
        String elo = plugin.getConfig().getString("player-info.whois.sections.stats.elo", 
            "§7ELO: §a{elo}");
        player.sendMessage(elo.replace("{elo}", String.valueOf(dossier.getElo())));
        
        String money = plugin.getConfig().getString("player-info.whois.sections.stats.money", 
            "§7Dinheiro: §a{money}");
        player.sendMessage(money.replace("{money}", String.format("%.2f", dossier.getMoney())));
        
        String level = plugin.getConfig().getString("player-info.whois.sections.stats.level", 
            "§7Nível: §a{level}");
        player.sendMessage(level.replace("{level}", String.valueOf(dossier.getLevel())));
        player.sendMessage("");
        
        // Clã
        String clanTitle = plugin.getConfig().getString("player-info.whois.sections.clan.title", 
            "§6§lClã");
        player.sendMessage(clanTitle);
        
        if (dossier.hasClan()) {
            String clanName = plugin.getConfig().getString("player-info.whois.sections.clan.clan-name", 
                "§7Clã: §e{clan_name}");
            player.sendMessage(clanName.replace("{clan_name}", dossier.getClanName()));
            
            String clanRole = plugin.getConfig().getString("player-info.whois.sections.clan.clan-role", 
                "§7Cargo: §a{clan_role}");
            player.sendMessage(clanRole.replace("{clan_role}", dossier.getClanRole()));
            
            String clanTag = plugin.getConfig().getString("player-info.whois.sections.clan.clan-tag", 
                "§7Tag: §f{clan_tag}");
            player.sendMessage(clanTag.replace("{clan_tag}", dossier.getClanTag()));
        } else {
            String noClan = plugin.getConfig().getString("player-info.whois.sections.clan.no-clan", 
                "§7Clã: §cNenhum");
            player.sendMessage(noClan);
        }
        player.sendMessage("");
        
        // Essentials
        String essentialsTitle = plugin.getConfig().getString("player-info.whois.sections.essentials.title", 
            "§6§lEssentials");
        player.sendMessage(essentialsTitle);
        
        String homes = plugin.getConfig().getString("player-info.whois.sections.essentials.homes", 
            "§7Homes: §a{home_count}");
        player.sendMessage(homes.replace("{home_count}", String.valueOf(dossier.getHomeCount())));
        
        String homeList = plugin.getConfig().getString("player-info.whois.sections.essentials.home-list", 
            "§7Lista: §f{homes}");
        player.sendMessage(homeList.replace("{homes}", dossier.getFormattedHomes()));
        player.sendMessage("");
        
        // Rodapé
        String footer = plugin.getConfig().getString("player-info.whois.layout.footer", 
            "§6§l================================");
        player.sendMessage(footer);
    }
}
