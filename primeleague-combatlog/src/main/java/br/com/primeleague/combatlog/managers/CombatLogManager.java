package br.com.primeleague.combatlog.managers;

import br.com.primeleague.combatlog.CombatLogPlugin;
import br.com.primeleague.combatlog.models.CombatTaggedPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gerenciador central do sistema de prevenção de combat log.
 * Responsável por tag, monitoramento e aplicação de punições.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatLogManager {
    
    private final CombatLogPlugin plugin;
    private final Logger logger;
    
    // Cache de jogadores em combate (UUID -> CombatTaggedPlayer)
    private final Map<UUID, CombatTaggedPlayer> taggedPlayers = new ConcurrentHashMap<UUID, CombatTaggedPlayer>();
    
    // Configurações do sistema
    private int defaultTagDuration = 30; // segundos
    private int pvpZoneTagDuration = 60; // segundos
    private int warzoneTagDuration = 90; // segundos
    
    // Flag para controle de shutdown
    private boolean isShutdown = false;
    
    /**
     * Construtor do CombatLogManager.
     * 
     * @param plugin Instância do plugin principal
     */
    public CombatLogManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Carregar configurações
        loadConfiguration();
        
        // Iniciar scheduler para limpeza de tags
        startCleanupScheduler();
        
        logger.info("✅ CombatLogManager inicializado com sucesso!");
    }
    
    /**
     * Carrega as configurações do arquivo config.yml.
     */
    private void loadConfiguration() {
        try {
            defaultTagDuration = plugin.getConfig().getInt("combat_log.tag_duration.default", 30);
            pvpZoneTagDuration = plugin.getConfig().getInt("combat_log.tag_duration.pvp_zone", 60);
            warzoneTagDuration = plugin.getConfig().getInt("combat_log.tag_duration.warzone", 90);
            
            logger.info("✅ Configurações carregadas - Padrão: " + defaultTagDuration + "s, PvP: " + pvpZoneTagDuration + "s, Guerra: " + warzoneTagDuration + "s");
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações, usando valores padrão: " + e.getMessage());
        }
    }
    
    /**
     * Aplica tag de combate a um jogador.
     * 
     * @param player Jogador a ser tagueado
     * @param reason Razão do combate (apenas DIRECT_DAMAGE)
     * @param zoneType Tipo de zona onde ocorreu o combate
     */
    public void tagPlayer(Player player, String reason, String zoneType) {
        if (player == null || reason == null || zoneType == null) {
            return;
        }
        
        // Validar razão do combate (apenas DIRECT_DAMAGE conforme diretriz)
        if (!"DIRECT_DAMAGE".equals(reason)) {
            logger.warning("⚠️ Tentativa de aplicar tag com razão inválida: " + reason + " para " + player.getName());
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        int duration = getTagDurationForZone(zoneType);
        
        // Se já estiver tagueado, estender o tempo se necessário
        CombatTaggedPlayer existingTag = taggedPlayers.get(playerUuid);
        if (existingTag != null) {
            int newDuration = Math.max(existingTag.getRemainingSeconds(), duration);
            if (newDuration > existingTag.getRemainingSeconds()) {
                // Atualizar tag existente com duração maior
                CombatTaggedPlayer updatedTag = new CombatTaggedPlayer(
                    playerUuid, 
                    player.getName(), 
                    reason, 
                    zoneType, 
                    newDuration
                );
                taggedPlayers.put(playerUuid, updatedTag);
                
                if (plugin.getConfig().getBoolean("combat_log.notifications.tag_applied", true)) {
                    player.sendMessage("⚔️ Tag de combate estendido para " + newDuration + " segundos!");
                }
                
                logger.info("⚔️ Tag de combate estendido para " + player.getName() + " por " + newDuration + "s");
            }
            return;
        }
        
        // Criar novo tag de combate
        CombatTaggedPlayer taggedPlayer = new CombatTaggedPlayer(
            playerUuid, 
            player.getName(), 
            reason, 
            zoneType, 
            duration
        );
        
        taggedPlayers.put(playerUuid, taggedPlayer);
        
        // Notificar jogador
        if (plugin.getConfig().getBoolean("combat_log.notifications.tag_applied", true)) {
            player.sendMessage("⚔️ Você está em combate por " + duration + " segundos!");
        }
        
        logger.info("⚔️ Jogador " + player.getName() + " tagueado em combate por " + duration + "s na zona " + zoneType);
    }
    
    /**
     * Remove tag de combate de um jogador.
     * 
     * @param playerUuid UUID do jogador
     */
    public void removeTag(UUID playerUuid) {
        CombatTaggedPlayer taggedPlayer = taggedPlayers.remove(playerUuid);
        if (taggedPlayer != null) {
            logger.info("✅ Tag de combate removido de " + taggedPlayer.getPlayerName());
        }
    }
    
    /**
     * Verifica se um jogador está em combate.
     * 
     * @param playerUuid UUID do jogador
     * @return true se estiver em combate
     */
    public boolean isPlayerTagged(UUID playerUuid) {
        CombatTaggedPlayer taggedPlayer = taggedPlayers.get(playerUuid);
        if (taggedPlayer != null && taggedPlayer.isExpired()) {
            // Tag expirado, remover
            taggedPlayers.remove(playerUuid);
            return false;
        }
        return taggedPlayer != null;
    }
    
    /**
     * Obtém informações do tag de combate de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return CombatTaggedPlayer ou null se não estiver tagueado
     */
    public CombatTaggedPlayer getTaggedPlayer(UUID playerUuid) {
        CombatTaggedPlayer taggedPlayer = taggedPlayers.get(playerUuid);
        if (taggedPlayer != null && taggedPlayer.isExpired()) {
            // Tag expirado, remover
            taggedPlayers.remove(playerUuid);
            return null;
        }
        return taggedPlayer;
    }
    
    /**
     * Obtém duração do tag baseada no tipo de zona.
     * 
     * @param zoneType Tipo de zona
     * @return Duração em segundos
     */
    private int getTagDurationForZone(String zoneType) {
        if ("WARZONE".equalsIgnoreCase(zoneType)) {
            return warzoneTagDuration;
        } else if ("PVP".equalsIgnoreCase(zoneType)) {
            return pvpZoneTagDuration;
        } else {
            return defaultTagDuration;
        }
    }
    
    /**
     * Inicia o scheduler para limpeza automática de tags expirados.
     * CORREÇÃO: Usar Iterator explícito para compatibilidade com Java 7.
     */
    private void startCleanupScheduler() {
        int cleanupInterval = plugin.getConfig().getInt("combat_log.performance.cleanup_interval", 20);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isShutdown) {
                    this.cancel();
                    return;
                }
                
                // CORREÇÃO APLICADA: Usar Iterator explícito para compatibilidade Java 7
                Iterator<Map.Entry<UUID, CombatTaggedPlayer>> iterator = taggedPlayers.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, CombatTaggedPlayer> entry = iterator.next();
                    CombatTaggedPlayer taggedPlayer = entry.getValue();
                    
                    // Decrementar tempo restante primeiro
                    taggedPlayer.decrementTime();
                    
                    if (taggedPlayer.isExpired()) {
                        logger.fine("⚔️ Tag expirado removido de " + taggedPlayer.getPlayerName());
                        iterator.remove(); // Remover usando o iterator (Java 7 compatível)
                    } else if (plugin.getConfig().getBoolean("combat_log.notifications.tag_expiring", true)) {
                        // Notificar quando estiver próximo de expirar (últimos 10 segundos)
                        if (taggedPlayer.getRemainingSeconds() <= 10 && taggedPlayer.getRemainingSeconds() > 0) {
                            Player player = plugin.getServer().getPlayer(taggedPlayer.getPlayerName());
                            if (player != null && player.isOnline()) {
                                player.sendMessage("⚠️ Tag de combate expira em " + taggedPlayer.getRemainingSeconds() + " segundos!");
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, cleanupInterval, cleanupInterval);
        
        logger.info("✅ Scheduler de limpeza iniciado com intervalo de " + cleanupInterval + " ticks");
    }
    
    /**
     * Obtém estatísticas do sistema.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        return String.format(
            "📊 Combat Log Stats - Jogadores Tagueados: %d",
            taggedPlayers.size()
        );
    }
    
    /**
     * Obtém lista de todos os jogadores tagueados.
     * 
     * @return Map de jogadores tagueados
     */
    public Map<UUID, CombatTaggedPlayer> getAllTaggedPlayers() {
        return new ConcurrentHashMap<UUID, CombatTaggedPlayer>(taggedPlayers);
    }
    
    /**
     * Força a aplicação de um tag de combate (para staff).
     * 
     * @param player Jogador a ser tagueado
     * @param duration Duração em segundos
     * @param reason Razão do tag
     */
    public void forceTagPlayer(Player player, int duration, String reason) {
        if (player == null) return;
        
        UUID playerUuid = player.getUniqueId();
        
        CombatTaggedPlayer taggedPlayer = new CombatTaggedPlayer(
            playerUuid, 
            player.getName(), 
            reason, 
            "ADMIN_FORCED", 
            duration
        );
        
        taggedPlayers.put(playerUuid, taggedPlayer);
        
        player.sendMessage("⚔️ Tag de combate aplicado por staff por " + duration + " segundos!");
        logger.info("⚔️ Tag de combate forçado para " + player.getName() + " por " + duration + "s (Staff)");
    }
    
    /**
     * Recarrega as configurações do sistema.
     */
    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("✅ Configurações do CombatLogManager recarregadas!");
    }
    
    /**
     * Desliga o manager e limpa recursos.
     */
    public void shutdown() {
        isShutdown = true;
        taggedPlayers.clear();
        logger.info("✅ CombatLogManager desligado e recursos limpos!");
    }
}
