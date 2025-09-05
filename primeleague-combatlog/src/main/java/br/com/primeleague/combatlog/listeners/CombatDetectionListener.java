package br.com.primeleague.combatlog.listeners;

import br.com.primeleague.combatlog.CombatLogPlugin;
import br.com.primeleague.combatlog.managers.CombatZoneManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.logging.Logger;

/**
 * Listener para detecção de combate entre jogadores.
 * Implementa a lógica simplificada: apenas dano PvP direto entre Players.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatDetectionListener implements Listener {
    
    private final CombatLogPlugin plugin;
    private final Logger logger;
    private final CombatZoneManager zoneManager;
    
    /**
     * Construtor do listener.
     * 
     * @param plugin Instância do plugin principal
     */
    public CombatDetectionListener(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.zoneManager = plugin.getZoneManager();
        
        logger.info("✅ CombatDetectionListener registrado com sucesso!");
    }
    
    /**
     * Handler para o evento de dano entre entidades.
     * Este é o GATILHO ÚNICO E INEQUÍVOCO para detecção de combate.
     * 
     * @param event Evento de dano entre entidades
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Verificar se o dano foi cancelado por outro plugin
        if (event.isCancelled()) {
            return;
        }
        
        // Verificar se é dano PvP direto entre jogadores
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // Verificar se os jogadores estão online
        if (!damager.isOnline() || !victim.isOnline()) {
            return;
        }
        
        // Verificar se o dano é significativo (mais de 0.5 corações)
        if (event.getDamage() < 1.0) {
            return;
        }
        
        // Verificar se não é dano de queda ou fogo
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL || 
            event.getCause() == EntityDamageEvent.DamageCause.FIRE || 
            event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
            event.getCause() == EntityDamageEvent.DamageCause.LAVA ||
            event.getCause() == EntityDamageEvent.DamageCause.DROWNING ||
            event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
            return;
        }
        
        // Determinar tipo de zona para ambos os jogadores
        String damagerZone = zoneManager.getPlayerZoneType(damager);
        String victimZone = zoneManager.getPlayerZoneType(victim);
        
        // Aplicar tag de combate para ambos os jogadores
        // Usar a zona mais perigosa entre os dois
        String combatZone = getMoreDangerousZone(damagerZone, victimZone);
        
        // Aplicar tag usando a razão DIRECT_DAMAGE (única permitida)
        plugin.getCombatLogManager().tagPlayer(damager, "DIRECT_DAMAGE", combatZone);
        plugin.getCombatLogManager().tagPlayer(victim, "DIRECT_DAMAGE", combatZone);
        
        // Log detalhado em modo debug
        if (plugin.getConfig().getBoolean("combat_log.debug_mode", false)) {
            logger.info("⚔️ Combate detectado: " + damager.getName() + " -> " + victim.getName() + 
                       " (Dano: " + event.getDamage() + ", Causa: " + event.getCause() + 
                       ", Zona: " + combatZone + ")");
        }
    }
    
    /**
     * Determina qual zona é mais perigosa para aplicação do tag.
     * 
     * @param zone1 Primeira zona
     * @param zone2 Segunda zona
     * @return Zona mais perigosa
     */
    private String getMoreDangerousZone(String zone1, String zone2) {
        // Hierarquia de perigo: WARZONE > PVP > SAFE
        if ("WARZONE".equalsIgnoreCase(zone1) || "WARZONE".equalsIgnoreCase(zone2)) {
            return "WARZONE";
        }
        
        if ("PVP".equalsIgnoreCase(zone1) || "PVP".equalsIgnoreCase(zone2)) {
            return "PVP";
        }
        
        // Se ambas são seguras, usar PVP como padrão para combate
        return "PVP";
    }
    
    /**
     * Handler para eventos de dano geral (para debug).
     * 
     * @param event Evento de dano
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamage(EntityDamageEvent event) {
        // Apenas log em modo debug
        if (plugin.getConfig().getBoolean("combat_log.debug_mode", false)) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                logger.fine("🔍 Dano detectado para " + player.getName() + 
                           " - Causa: " + event.getCause() + 
                           " - Valor: " + event.getDamage() + 
                           " - Cancelado: " + event.isCancelled());
            }
        }
    }
}
