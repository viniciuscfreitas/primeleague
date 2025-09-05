package br.com.primeleague.combatlog;

import br.com.primeleague.combatlog.commands.CombatLogCommand;
import br.com.primeleague.combatlog.listeners.CombatDetectionListener;
import br.com.primeleague.combatlog.listeners.PlayerQuitListener;
import br.com.primeleague.combatlog.managers.CombatLogManager;
import br.com.primeleague.combatlog.managers.CombatPunishmentService;
import br.com.primeleague.combatlog.managers.CombatZoneManager;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Plugin principal do sistema de prevenção de combat log.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatLogPlugin extends JavaPlugin {
    
    private static CombatLogPlugin instance;
    private Logger logger;
    
    // Managers do sistema
    private CombatLogManager combatLogManager;
    private CombatPunishmentService punishmentService;
    private CombatZoneManager zoneManager;
    
    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        
        logger.info("🚀 Iniciando PrimeLeague CombatLog v1.0.0...");
        
        // Verificar dependências
        if (!checkDependencies()) {
            logger.severe("❌ Dependências não encontradas! Desabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Carregar configuração
        saveDefaultConfig();
        reloadConfig();
        
        // Inicializar managers
        initializeManagers();
        
        // Registrar listeners
        registerListeners();
        
        // Registrar comandos
        registerCommands();
        
        logger.info("✅ PrimeLeague CombatLog habilitado com sucesso!");
    }
    
    @Override
    public void onDisable() {
        logger.info("🔄 Desabilitando PrimeLeague CombatLog...");
        
        // Limpar recursos
        if (combatLogManager != null) {
            combatLogManager.shutdown();
        }
        
        logger.info("✅ PrimeLeague CombatLog desabilitado!");
    }
    
    /**
     * Verifica se as dependências necessárias estão disponíveis.
     */
    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("PrimeLeague-Core") == null) {
            logger.severe("❌ PrimeLeague-Core não encontrado!");
            return false;
        }
        
        if (getServer().getPluginManager().getPlugin("PrimeLeagueAdmin") == null) {
            logger.warning("⚠️ PrimeLeague-Admin não encontrado! Algumas funcionalidades podem não funcionar.");
        }
        
        return true;
    }
    
    /**
     * Inicializa os managers do sistema.
     */
    private void initializeManagers() {
        try {
            // Inicializar zone manager primeiro
            zoneManager = new CombatZoneManager(this);
            
            // Inicializar combat log manager
            combatLogManager = new CombatLogManager(this);
            
            // Inicializar punishment service
            punishmentService = new CombatPunishmentService(this);
            
            logger.info("✅ Managers inicializados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao inicializar managers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra os listeners do sistema.
     */
    private void registerListeners() {
        try {
            // Listener para detecção de combate
            getServer().getPluginManager().registerEvents(new CombatDetectionListener(this), this);
            
            // Listener para logout de jogadores
            getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
            
            logger.info("✅ Listeners registrados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra os comandos do sistema.
     */
    private void registerCommands() {
        try {
            // Comando principal de combat log
            getCommand("combatlog").setExecutor(new CombatLogCommand(this));
            
            logger.info("✅ Comandos registrados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar comandos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtém a instância do plugin.
     */
    public static CombatLogPlugin getInstance() {
        return instance;
    }
    
    /**
     * Obtém o manager de combat log.
     */
    public CombatLogManager getCombatLogManager() {
        return combatLogManager;
    }
    
    /**
     * Obtém o service de punições.
     */
    public CombatPunishmentService getPunishmentService() {
        return punishmentService;
    }
    
    /**
     * Obtém o manager de zonas.
     */
    public CombatZoneManager getZoneManager() {
        return zoneManager;
    }
    
    /**
     * Recarrega a configuração do plugin.
     */
    public void reloadPlugin() {
        reloadConfig();
        
        if (zoneManager != null) {
            zoneManager.reloadConfiguration();
        }
        
        logger.info("✅ Configuração recarregada com sucesso!");
    }
}
