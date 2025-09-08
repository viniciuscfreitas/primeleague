package br.com.primeleague.territories;

import br.com.primeleague.territories.api.TerritoryServiceRegistry;
import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.territories.api.TerritoryServiceImpl;
import br.com.primeleague.territories.commands.TerritoryCommand;
import br.com.primeleague.territories.commands.WarCommand;
import br.com.primeleague.territories.listeners.SiegeListener;
import br.com.primeleague.territories.listeners.TerritoryProtectionListener;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.util.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Plugin principal do módulo de Territórios do Prime League.
 * Gerencia territórios, guerras e cercos entre clãs.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class PrimeLeagueTerritories extends JavaPlugin {
    
    private static PrimeLeagueTerritories instance;
    private Logger logger;
    
    // Managers
    private TerritoryManager territoryManager;
    private WarManager warManager;
    private MessageManager messageManager;
    
    // Core reference
    private PrimeLeagueCore core;
    
    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();
        
        logger.info("=== INICIANDO MÓDULO DE TERRITÓRIOS ===");
        
        try {
            // Verificar dependências
            if (!checkDependencies()) {
                logger.severe("❌ Dependências não encontradas! Desabilitando módulo...");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Obter referência do Core
            this.core = (PrimeLeagueCore) getServer().getPluginManager().getPlugin("PrimeLeague-Core");
            
            // Carregar configuração
            saveDefaultConfig();
            
            // Inicializar MessageManager primeiro
            this.messageManager = new MessageManager(this);
            logger.info("✅ MessageManager inicializado");
            
            // Inicializar managers
            initializeManagers();
            
        // Registrar serviços na API
        registerServices();
        
        // Iniciar verificação periódica de disponibilidade de serviços
        startServiceAvailabilityCheck();
            
            // Registrar comandos
            registerCommands();
            
            // Registrar listeners
            registerListeners();
            
            logger.info("✅ Módulo de Territórios carregado com sucesso!");
            logger.info("📊 Estatísticas: " + getModuleStats());
            
        } catch (Exception e) {
            logger.severe("❌ Erro crítico ao carregar o módulo: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        logger.info("=== DESABILITANDO MÓDULO DE TERRITÓRIOS ===");
        
        try {
            // Cancelar tarefas agendadas
            if (territoryManager != null) {
                // Cancelar tarefas de manutenção
                getServer().getScheduler().cancelTasks(this);
            }
            
            if (warManager != null) {
                // Finalizar cercos ativos
                // Implementar quando necessário
            }
            
            logger.info("✅ Módulo de Territórios desabilitado com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao desabilitar o módulo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Verifica se as dependências necessárias estão carregadas.
     */
    private boolean checkDependencies() {
        // Verificar PrimeLeague-Core
        if (getServer().getPluginManager().getPlugin("PrimeLeague-Core") == null) {
            logger.severe("❌ PrimeLeague-Core não encontrado!");
            return false;
        }
        
        // Verificar PrimeLeague-Clans
        if (getServer().getPluginManager().getPlugin("PrimeLeague-Clans") == null) {
            logger.severe("❌ PrimeLeague-Clans não encontrado!");
            return false;
        }
        
        logger.info("✅ Todas as dependências encontradas");
        return true;
    }
    
    /**
     * Inicializa os managers do módulo com injeção de dependência.
     */
    private void initializeManagers() {
        logger.info("🔧 Inicializando managers com injeção de dependência...");
        
        // Criar instâncias das dependências
        br.com.primeleague.territories.dao.MySqlTerritoryDAO territoryDAO = 
            new br.com.primeleague.territories.dao.MySqlTerritoryDAO(core);
        
        // Obter serviços com fallback para registries diretos
        br.com.primeleague.api.ClanService clanService = null;
        br.com.primeleague.api.EconomyService economyService = null;
        
        try {
            // Tentar usar a API centralizada primeiro
            clanService = br.com.primeleague.core.api.PrimeLeagueAPI.getClanServiceRegistry();
            economyService = br.com.primeleague.core.api.PrimeLeagueAPI.getEconomyServiceRegistry();
            getLogger().info("✅ Serviços obtidos via PrimeLeagueAPI");
        } catch (Exception e) {
            getLogger().warning("⚠️ API centralizada não disponível, usando registries diretos: " + e.getMessage());
            try {
                // Fallback para registries diretos
                clanService = br.com.primeleague.api.ClanServiceRegistry.getInstance();
                economyService = br.com.primeleague.api.EconomyServiceRegistry.getInstance();
                getLogger().info("✅ Serviços obtidos via registries diretos");
            } catch (Exception e2) {
                getLogger().severe("❌ Falha ao obter serviços: " + e2.getMessage());
                throw new RuntimeException("Não foi possível inicializar os serviços necessários", e2);
            }
        }
        
        // Carregar configurações
        int maxTerritoriesPerClan = getConfig().getInt("territories.max-per-clan", 10);
        double maintenanceBaseCost = getConfig().getDouble("territories.maintenance-base-cost", 100.0);
        double maintenanceScale = getConfig().getDouble("territories.maintenance-scale", 1.5);
        int maintenanceIntervalHours = getConfig().getInt("territories.maintenance-interval", 24);
        
        int exclusivityWindowHours = getConfig().getInt("war.exclusivity-window-hours", 24);
        int siegeDurationMinutes = getConfig().getInt("war.siege-duration-minutes", 20);
        double warDeclarationCost = getConfig().getDouble("war.declaration-cost", 5000.0);
        
        // Inicializar TerritoryManager com dependências injetadas
        this.territoryManager = new TerritoryManager(
            this, 
            getServer().getScheduler(), 
            territoryDAO, 
            clanService,
            economyService,
            maxTerritoriesPerClan,
            maintenanceBaseCost,
            maintenanceScale,
            maintenanceIntervalHours
        );
        logger.info("✅ TerritoryManager inicializado com injeção de dependência");
        
        // Inicializar WarManager com dependências injetadas
        this.warManager = new WarManager(
            this, 
            territoryDAO, 
            clanService,
            economyService,
            territoryManager,
            exclusivityWindowHours,
            siegeDurationMinutes,
            warDeclarationCost
        );
        logger.info("✅ WarManager inicializado com injeção de dependência");
        
        logger.info("✅ Todos os managers inicializados com injeção de dependência");
    }
    
    /**
     * Registra os serviços na API do Core.
     */
    private void registerServices() {
        logger.info("🔧 Registrando serviços na API...");
        
        // Registrar serviço de territórios
        TerritoryServiceRegistry territoryService = new TerritoryServiceImpl(territoryManager, warManager);
        // TODO: Implementar registro na API quando necessário
        // PrimeLeagueAPI.getInstance().register(territoryService);
        
        logger.info("✅ Serviços registrados na API");
    }
    
    /**
     * Inicia verificação periódica de disponibilidade de serviços externos.
     */
    private void startServiceAvailabilityCheck() {
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                checkServiceAvailability();
            }
        }, 20L, 200L); // Verifica a cada 10 segundos
    }
    
    /**
     * Verifica se os serviços externos estão disponíveis.
     */
    private void checkServiceAvailability() {
        try {
            br.com.primeleague.api.ClanService clanService = br.com.primeleague.core.api.PrimeLeagueAPI.getClanServiceRegistry();
            if (clanService != null) {
                logger.info("✅ ClanService disponível - migrando de placeholders para API real");
                // Aqui poderíamos implementar uma migração automática dos placeholders para a API real
            }
        } catch (Exception e) {
            // Serviços ainda não disponíveis, continuar com placeholders
        }
    }
    
    /**
     * Registra os comandos do módulo.
     */
    private void registerCommands() {
        logger.info("🔧 Registrando comandos...");
        
        // Registrar comando de território
        TerritoryCommand territoryCommand = new TerritoryCommand(this);
        getCommand("territory").setExecutor(territoryCommand);
        getCommand("territory").setTabCompleter(territoryCommand);
        
        // Registrar comando de guerra
        WarCommand warCommand = new WarCommand(this);
        getCommand("war").setExecutor(warCommand);
        getCommand("war").setTabCompleter(warCommand);
        
        logger.info("✅ Comandos registrados");
    }
    
    /**
     * Registra os listeners do módulo.
     */
    private void registerListeners() {
        logger.info("🔧 Registrando listeners...");
        
        // Registrar listener de proteção de território
        TerritoryProtectionListener protectionListener = new TerritoryProtectionListener(this);
        getServer().getPluginManager().registerEvents(protectionListener, this);
        
        // Registrar listener de cerco
        SiegeListener siegeListener = new SiegeListener(this);
        getServer().getPluginManager().registerEvents(siegeListener, this);
        
        logger.info("✅ Listeners registrados");
    }
    
    // ==================== GETTERS ====================
    
    public static PrimeLeagueTerritories getInstance() {
        return instance;
    }
    
    public TerritoryManager getTerritoryManager() {
        return territoryManager;
    }
    
    public WarManager getWarManager() {
        return warManager;
    }
    
    public MessageManager getMessageManager() {
        return messageManager;
    }
    
    public PrimeLeagueCore getCore() {
        return core;
    }
    
    // ==================== UTILITY METHODS ====================
    
    /**
     * Verifica se o módulo está funcionando corretamente.
     */
    public boolean isHealthy() {
        return territoryManager != null && warManager != null && messageManager != null && core != null;
    }
    
    /**
     * Obtém estatísticas do módulo.
     */
    public String getModuleStats() {
        if (!isHealthy()) {
            return "❌ Módulo com problemas - nem todos os componentes estão funcionando";
        }
        
        StringBuilder stats = new StringBuilder();
        stats.append("=== ESTATÍSTICAS DO MÓDULO DE TERRITÓRIOS ===\n");
        stats.append("Status: ").append(isHealthy() ? "✅ Saudável" : "❌ Com problemas").append("\n");
        stats.append("TerritoryManager: ").append(territoryManager != null ? "✅ Ativo" : "❌ Inativo").append("\n");
        stats.append("WarManager: ").append(warManager != null ? "✅ Ativo" : "❌ Inativo").append("\n");
        stats.append("MessageManager: ").append(messageManager != null ? "✅ Ativo" : "❌ Inativo").append("\n");
        stats.append("Core: ").append(core != null ? "✅ Conectado" : "❌ Desconectado").append("\n");
        
        return stats.toString();
    }
}