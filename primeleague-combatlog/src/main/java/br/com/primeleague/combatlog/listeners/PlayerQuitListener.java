package br.com.primeleague.combatlog.listeners;

import br.com.primeleague.combatlog.CombatLogPlugin;
import br.com.primeleague.combatlog.models.CombatTaggedPlayer;
import br.com.primeleague.combatlog.managers.CombatPunishmentService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Logger;

/**
 * Listener para detectar quando jogadores fazem logout durante combate.
 * Integra com o sistema de punições para aplicar consequências.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PlayerQuitListener implements Listener {
    
    private final CombatLogPlugin plugin;
    private final Logger logger;
    private final CombatPunishmentService punishmentService;
    
    /**
     * Construtor do listener.
     * 
     * @param plugin Instância do plugin principal
     */
    public PlayerQuitListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.punishmentService = plugin.getPunishmentService();
        
        logger.info("✅ PlayerQuitListener registrado com sucesso!");
    }
    
    /**
     * Handler para o evento de logout do jogador.
     * Detecta combat log e aplica punições automaticamente.
     * 
     * @param event Evento de logout do jogador
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Verificar se o sistema está habilitado
        if (!plugin.getConfig().getBoolean("combat_log.enabled", true)) {
            return;
        }
        
        // Verificar se o jogador está tagueado em combate
        CombatTaggedPlayer taggedPlayer = plugin.getCombatLogManager().getTaggedPlayer(event.getPlayer().getUniqueId());
        
        if (taggedPlayer == null) {
            // Jogador não está em combate, logout normal
            return;
        }
        
        // COMBAT LOG DETECTADO!
        String playerName = event.getPlayer().getName();
        int combatDuration = taggedPlayer.getCombatDuration();
        String zoneType = taggedPlayer.getZoneType();
        
        logger.warning("🚨 COMBAT LOG DETECTADO: " + playerName + 
                      " deslogou durante combate na zona " + zoneType + 
                      " (Duração: " + combatDuration + "s)");
        
        // Registrar o combat log no banco de dados
        recordCombatLog(taggedPlayer, combatDuration);
        
        // Aplicar punição automática
        punishmentService.applyCombatLogPunishment(event.getPlayer().getUniqueId(), playerName);
        
        // Remover o tag de combate
        plugin.getCombatLogManager().removeTag(event.getPlayer().getUniqueId());
        
        // Notificar staff online sobre o combat log
        notifyStaffAboutCombatLog(playerName, zoneType, combatDuration);
    }
    
    /**
     * Registra o combat log no banco de dados para auditoria.
     * 
     * @param taggedPlayer Jogador que fez combat log
     * @param combatDuration Duração do combate em segundos
     */
    private void recordCombatLog(CombatTaggedPlayer taggedPlayer, int combatDuration) {
        try {
            // TODO: Implementar persistência no banco de dados
            // Por enquanto, apenas log local
            
            if (plugin.getConfig().getBoolean("combat_log.debug_mode", false)) {
                logger.info("📝 Combat log registrado para " + taggedPlayer.getPlayerName() + 
                           " - Zona: " + taggedPlayer.getZoneType() + 
                           " - Duração: " + combatDuration + "s" +
                           " - Razão: " + taggedPlayer.getCombatReason());
            }
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar combat log no banco: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notifica staff online sobre o combat log detectado.
     * 
     * @param playerName Nome do jogador que fez combat log
     * @param zoneType Tipo de zona onde ocorreu
     * @param combatDuration Duração do combate
     */
    private void notifyStaffAboutCombatLog(String playerName, String zoneType, int combatDuration) {
        try {
            String message = "🚨 COMBAT LOG: " + playerName + 
                           " deslogou durante combate na zona " + zoneType + 
                           " (Duração: " + combatDuration + "s)";
            
            // Notificar todos os jogadores com permissão de staff
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (player.hasPermission("primeleague.admin.combatlog")) {
                    player.sendMessage(message);
                }
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao notificar staff sobre combat log: " + e.getMessage());
        }
    }
}
