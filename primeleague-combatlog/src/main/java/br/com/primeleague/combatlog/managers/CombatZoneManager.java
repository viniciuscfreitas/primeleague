package br.com.primeleague.combatlog.managers;

import br.com.primeleague.combatlog.CombatLogPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Gerenciador de zonas para o sistema de combat log.
 * Determina o tipo de zona onde um jogador está localizado.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatZoneManager {
    
    private final CombatLogPlugin plugin;
    private final Logger logger;
    
    // Cache de zonas por mundo
    private final Map<String, ZoneInfo> worldZones = new HashMap<String, ZoneInfo>();
    
    // Configurações de zonas
    private List<String> safeZones;
    private List<String> pvpZones;
    private List<String> warzones;
    
    /**
     * Informações sobre uma zona específica.
     */
    private static class ZoneInfo {
        final String zoneName;
        final String zoneType;
        final double x, y, z;
        final double radius;
        
        ZoneInfo(String zoneName, String zoneType, double x, double y, double z, double radius) {
            this.zoneName = zoneName;
            this.zoneType = zoneType;
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
        }
        
        boolean isPlayerInZone(Location playerLocation) {
            if (playerLocation.getWorld() == null) return false;
            
            double distance = Math.sqrt(
                Math.pow(playerLocation.getX() - x, 2) +
                Math.pow(playerLocation.getZ() - z, 2)
            );
            
            return distance <= radius && Math.abs(playerLocation.getY() - y) <= 10;
        }
    }
    
    /**
     * Construtor do CombatZoneManager.
     * 
     * @param plugin Instância do plugin principal
     */
    public CombatZoneManager(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Carregar configurações
        loadConfiguration();
        
        // Inicializar zonas padrão
        initializeDefaultZones();
        
        logger.info("✅ CombatZoneManager inicializado com sucesso!");
    }
    
    /**
     * Carrega as configurações de zonas do arquivo config.yml.
     */
    private void loadConfiguration() {
        try {
            safeZones = plugin.getConfig().getStringList("combat_log.zones.safe_zones");
            pvpZones = plugin.getConfig().getStringList("combat_log.zones.pvp_zones");
            warzones = plugin.getConfig().getStringList("combat_log.zones.warzones");
            
            logger.info("✅ Configurações de zonas carregadas - Seguras: " + safeZones.size() + 
                       ", PvP: " + pvpZones.size() + ", Guerra: " + warzones.size());
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações de zonas, usando valores padrão: " + e.getMessage());
            loadDefaultZones();
        }
    }
    
    /**
     * Carrega zonas padrão caso as configurações falhem.
     */
    private void loadDefaultZones() {
        safeZones = java.util.Arrays.asList("spawn", "shop", "bank", "lobby");
        pvpZones = java.util.Arrays.asList("wilderness", "arena", "training");
        warzones = java.util.Arrays.asList("clan_territory", "battlefield", "conquest");
    }
    
    /**
     * Inicializa zonas padrão para o mundo principal.
     */
    private void initializeDefaultZones() {
        try {
            World world = plugin.getServer().getWorlds().get(0); // Mundo principal
            if (world != null) {
                // Zona segura no spawn (raio de 50 blocos)
                addZone(world.getName(), "spawn", "SAFE", 0, 64, 0, 50.0);
                
                // Zona PvP no wilderness (raio de 200 blocos a partir de 100,100)
                addZone(world.getName(), "wilderness", "PVP", 100, 64, 100, 200.0);
                
                // Zona de guerra no battlefield (raio de 100 blocos a partir de -100,64,-100)
                addZone(world.getName(), "battlefield", "WARZONE", -100, 64, -100, 100.0);
                
                logger.info("✅ Zonas padrão inicializadas para o mundo " + world.getName());
            }
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao inicializar zonas padrão: " + e.getMessage());
        }
    }
    
    /**
     * Adiciona uma nova zona ao sistema.
     * 
     * @param worldName Nome do mundo
     * @param zoneName Nome da zona
     * @param zoneType Tipo da zona (SAFE, PVP, WARZONE)
     * @param x Coordenada X do centro
     * @param y Coordenada Y do centro
     * @param z Coordenada Z do centro
     * @param radius Raio da zona
     */
    public void addZone(String worldName, String zoneName, String zoneType, double x, double y, double z, double radius) {
        String key = worldName + ":" + zoneName;
        ZoneInfo zoneInfo = new ZoneInfo(zoneName, zoneType, x, y, z, radius);
        worldZones.put(key, zoneInfo);
        
        logger.info("✅ Zona adicionada: " + zoneName + " (" + zoneType + ") em " + worldName + 
                   " - Centro: (" + x + ", " + y + ", " + z + ") - Raio: " + radius);
    }
    
    /**
     * Remove uma zona do sistema.
     * 
     * @param worldName Nome do mundo
     * @param zoneName Nome da zona
     */
    public void removeZone(String worldName, String zoneName) {
        String key = worldName + ":" + zoneName;
        ZoneInfo removed = worldZones.remove(key);
        
        if (removed != null) {
            logger.info("✅ Zona removida: " + zoneName + " de " + worldName);
        }
    }
    
    /**
     * Determina o tipo de zona onde um jogador está localizado.
     * 
     * @param player Jogador para verificar
     * @return Tipo de zona (SAFE, PVP, WARZONE)
     */
    public String getPlayerZoneType(Player player) {
        if (player == null || player.getLocation() == null) {
            return "PVP"; // Padrão seguro
        }
        
        Location playerLocation = player.getLocation();
        String worldName = playerLocation.getWorld().getName();
        
        // Verificar zonas definidas
        for (Map.Entry<String, ZoneInfo> entry : worldZones.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(worldName + ":")) {
                ZoneInfo zoneInfo = entry.getValue();
                if (zoneInfo.isPlayerInZone(playerLocation)) {
                    return zoneInfo.zoneType;
                }
            }
        }
        
        // Verificar zonas baseadas em nomes de mundo/bioma
        String zoneType = getZoneTypeByWorldAndBiome(playerLocation);
        if (zoneType != null) {
            return zoneType;
        }
        
        // Padrão: PVP (zona neutra)
        return "PVP";
    }
    
    /**
     * Determina o tipo de zona baseado no mundo e bioma.
     * 
     * @param location Localização do jogador
     * @return Tipo de zona ou null se não puder ser determinado
     */
    private String getZoneTypeByWorldAndBiome(Location location) {
        if (location.getWorld() == null) return null;
        
        String worldName = location.getWorld().getName().toLowerCase();
        
        // Verificar se é um mundo de spawn/shop
        if (worldName.contains("spawn") || worldName.contains("shop") || 
            worldName.contains("lobby") || worldName.contains("hub")) {
            return "SAFE";
        }
        
        // Verificar se é um mundo de arena/batalha
        if (worldName.contains("arena") || worldName.contains("battle") || 
            worldName.contains("war") || worldName.contains("clan")) {
            return "WARZONE";
        }
        
        // Verificar se é um mundo de treinamento
        if (worldName.contains("training") || worldName.contains("practice")) {
            return "PVP";
        }
        
        return null;
    }
    
    /**
     * Obtém informações sobre todas as zonas de um mundo.
     * 
     * @param worldName Nome do mundo
     * @return Lista de informações de zonas
     */
    public java.util.List<ZoneInfo> getWorldZones(String worldName) {
        java.util.List<ZoneInfo> zones = new java.util.ArrayList<ZoneInfo>();
        
        for (Map.Entry<String, ZoneInfo> entry : worldZones.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(worldName + ":")) {
                zones.add(entry.getValue());
            }
        }
        
        return zones;
    }
    
    /**
     * Obtém estatísticas das zonas.
     * 
     * @return String com estatísticas
     */
    public String getZoneStats() {
        int totalZones = worldZones.size();
        int safeZones = 0, pvpZones = 0, warzones = 0;
        
        for (ZoneInfo zoneInfo : worldZones.values()) {
            if ("SAFE".equals(zoneInfo.zoneType)) safeZones++;
            else if ("PVP".equals(zoneInfo.zoneType)) pvpZones++;
            else if ("WARZONE".equals(zoneInfo.zoneType)) warzones++;
        }
        
        return String.format(
            "📊 Zone Stats - Total: %d (SAFE: %d, PVP: %d, WARZONE: %d)",
            totalZones, safeZones, pvpZones, warzones
        );
    }
    
    /**
     * Recarrega as configurações de zonas.
     */
    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("✅ Configurações de zonas recarregadas!");
    }
}
