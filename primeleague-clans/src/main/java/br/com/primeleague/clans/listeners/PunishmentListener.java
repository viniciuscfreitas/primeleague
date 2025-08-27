package br.com.primeleague.clans.listeners;

import br.com.primeleague.api.events.PlayerPunishedEvent;
import br.com.primeleague.api.events.PlayerPunishmentReversedEvent;
import br.com.primeleague.api.enums.PunishmentSeverity;
import br.com.primeleague.api.enums.ReversalType;
import br.com.primeleague.clans.PrimeLeagueClans;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.UUID;

/**
 * Listener que escuta eventos de punição do módulo Admin e adiciona pontos de penalidade
 * aos clãs dos jogadores punidos, implementando o sistema de sanções de clã.
 */
public class PunishmentListener implements Listener {
    
    private final PrimeLeagueClans plugin;
    private final ClanManager clanManager;
    
    public PunishmentListener(PrimeLeagueClans plugin, ClanManager clanManager) {
        this.plugin = plugin;
        this.clanManager = clanManager;
    }
    
    @EventHandler
    public void onPlayerPunished(final PlayerPunishedEvent event) {
        // Extrair dados do evento ANTES da task assíncrona para evitar race conditions
        final String playerUuid = event.getPlayerUuid();
        final String playerName = event.getPlayerName();
        final String authorName = event.getAuthorName();
        final PunishmentSeverity severity = event.getSeverity();
        
        // Executar de forma assíncrona para não travar o servidor
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    processPunishment(playerUuid, playerName, authorName, severity);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar punição para sanções de clã: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void processPunishment(String playerUuid, String playerName, String authorName, PunishmentSeverity severity) {
        // Verificar se o jogador está em um clã
        if (!clanManager.isPlayerInClan(playerUuid)) {
            plugin.getLogger().info("Jogador " + playerName + " não está em clã - ignorando sanção");
            return;
        }
        
        // Obter pontos de penalidade baseados na severidade
        int penaltyPoints = getPenaltyPointsForSeverity(severity);
        if (penaltyPoints <= 0) {
            plugin.getLogger().info("Severidade '" + severity.getDisplayName() + "' não gera pontos de penalidade");
            return;
        }
        
        // REFATORADO: Converter UUID para player_id
        int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(UUID.fromString(playerUuid));
        if (playerId == -1) {
            plugin.getLogger().warning("Não foi possível obter player_id para " + playerName + " - ignorando sanção");
            return;
        }
        
        // Aplicar sanções ao clã usando o novo método transacional
        boolean success = clanManager.applyPunishmentSanctions(playerId, severity, authorName, playerName);
        
        if (success) {
            plugin.getLogger().info("Aplicadas sanções de severidade " + severity.getDisplayName() + 
                                  " ao clã de " + playerName + " (+" + penaltyPoints + " pontos)");
        } else {
            plugin.getLogger().warning("Falha ao aplicar sanções ao clã de " + playerName);
        }
    }
    
    private int getPenaltyPointsForSeverity(PunishmentSeverity severity) {
        ConfigurationSection penaltyConfig = plugin.getConfig().getConfigurationSection("sanctions.penalty-points");
        if (penaltyConfig == null) {
            plugin.getLogger().warning("Seção 'sanctions.penalty-points' não encontrada no config.yml");
            return 0;
        }
        
        // CORREÇÃO: Converte o nome do enum para minúsculas para corresponder ao config.yml
        // Isso torna o código robusto independentemente da formatação do config
        String configKey = severity.name().toLowerCase();
        
        // DEBUG: Log todas as chaves disponíveis (apenas em desenvolvimento)
        // plugin.getLogger().info("DEBUG: Procurando por chave '" + configKey + "'");
        // plugin.getLogger().info("DEBUG: Chaves disponíveis no config: " + penaltyConfig.getKeys(false));
        
        int points = penaltyConfig.getInt(configKey, 0);
        
        // Log para debug - apenas se realmente não encontrou a chave
        if (points == 0) {
            plugin.getLogger().warning("Config key '" + configKey + "' não encontrada ou valor 0 para severidade " + severity.getDisplayName());
        } else {
            plugin.getLogger().info("Config key '" + configKey + "' encontrada com valor " + points + " para severidade " + severity.getDisplayName());
        }
        
        return points;
    }
    
    @EventHandler
    public void onPlayerPunishmentReversed(final PlayerPunishmentReversedEvent event) {
        // Extrair dados do evento ANTES da task assíncrona para evitar race conditions
        final String playerUuid = event.getPlayerUuid();
        final String playerName = event.getPlayerName();
        final String adminName = event.getAdminName();
        final ReversalType reversalType = event.getReversalType();
        final PunishmentSeverity originalSeverity = event.getOriginalPunishmentSeverity();
        
        // Executar de forma assíncrona para não travar o servidor
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    processPunishmentReversal(playerUuid, playerName, adminName, reversalType, originalSeverity);
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar reversão de punição para sanções de clã: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void processPunishmentReversal(String playerUuid, String playerName, String adminName, 
                                         ReversalType reversalType, PunishmentSeverity originalSeverity) {
        // Verificar se o jogador está em um clã
        if (!clanManager.isPlayerInClan(playerUuid)) {
            plugin.getLogger().info("Jogador " + playerName + " não está em clã - ignorando reversão de sanção");
            return;
        }
        
        // Apenas processar se for uma correção (reversão de sanções)
        if (reversalType == ReversalType.CORRECTION) {
            // REFATORADO: Converter UUID para player_id
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(UUID.fromString(playerUuid));
            if (playerId == -1) {
                plugin.getLogger().warning("Não foi possível obter player_id para " + playerName + " - ignorando reversão de sanção");
                return;
            }
            
            boolean success = clanManager.revertClanSanction(playerId, originalSeverity, adminName);
            
            if (success) {
                plugin.getLogger().info("Sanções de clã revertidas para " + playerName + " (correção administrativa)");
            } else {
                plugin.getLogger().warning("Falha ao reverter sanções de clã para " + playerName);
            }
        } else {
            // PARDON - não fazer nada, sanções permanecem
            plugin.getLogger().info("Punição de " + playerName + " revertida como perdão - sanções de clã mantidas");
        }
    }
}
