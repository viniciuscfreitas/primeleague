package br.com.primeleague.essentials;

import br.com.primeleague.essentials.commands.DelwarpCommand;
import br.com.primeleague.essentials.commands.DiscordCommand;
import br.com.primeleague.essentials.commands.EssentialsCommand;
import br.com.primeleague.essentials.commands.KitCommand;
import br.com.primeleague.essentials.commands.SeenCommand;
import br.com.primeleague.essentials.commands.SetwarpCommand;
import br.com.primeleague.essentials.commands.TpaCommand;
import br.com.primeleague.essentials.commands.WarpCommand;
import br.com.primeleague.essentials.commands.WarplistCommand;
import br.com.primeleague.essentials.commands.WhoisCommand;
import br.com.primeleague.essentials.managers.EssentialsManager;
import br.com.primeleague.essentials.managers.KitManager;
import br.com.primeleague.essentials.managers.PlayerInfoManager;
import br.com.primeleague.essentials.managers.TpaManager;
import br.com.primeleague.essentials.managers.WarpManager;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Plugin principal do sistema de comandos essenciais.
 * Gerencia homes, teletransporte e spawn para qualidade de vida dos jogadores.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class EssentialsPlugin extends JavaPlugin {
    
    private EssentialsManager essentialsManager;
    private TpaManager tpaManager;
    private KitManager kitManager;
    private PlayerInfoManager playerInfoManager;
    private WarpManager warpManager;
    private Logger logger;
    
    @Override
    public void onEnable() {
        logger = getLogger();
        
        logger.info("🚀 Iniciando PrimeLeague Essentials v1.0.0...");
        
        // Verificar dependências
        if (!checkDependencies()) {
            logger.severe("❌ Dependências não encontradas! Desabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Carregar configurações
        loadConfiguration();
        
        // Inicializar managers
        initializeManagers();
        
        // Registrar DAOs no Core via DAOServiceRegistry
        registerDAOs();
        
        // Registrar comandos
        registerCommands();
        
        // Registrar listeners
        registerListeners();
        
        logger.info("✅ PrimeLeague Essentials habilitado com sucesso!");
    }
    
    @Override
    public void onDisable() {
        logger.info("🔄 Desabilitando PrimeLeague Essentials...");
        
        // Limpar cache
        if (essentialsManager != null) {
            essentialsManager.clearAllCache();
        }
        
        if (tpaManager != null) {
            // TpaManager não precisa de limpeza especial pois usa cache em memória
        }
        
        if (kitManager != null) {
            // KitManager não precisa de limpeza especial pois usa cache em memória
        }
        
        if (playerInfoManager != null) {
            // PlayerInfoManager não precisa de limpeza especial pois usa cache em memória
        }
        
        if (warpManager != null) {
            warpManager.clearAllCache();
        }
        
        logger.info("✅ PrimeLeague Essentials desabilitado com sucesso!");
    }
    
    /**
     * Verifica se as dependências necessárias estão disponíveis.
     * 
     * @return true se todas as dependências estão disponíveis
     */
    private boolean checkDependencies() {
        // Verificar PrimeLeague-Core
        if (getServer().getPluginManager().getPlugin("PrimeLeague-Core") == null) {
            logger.severe("❌ PrimeLeague-Core não encontrado!");
            return false;
        }
        
        // Verificar se a API está disponível
        try {
            PrimeLeagueAPI.getDataManager().getConnection();
            logger.info("✅ Conexão com banco de dados estabelecida");
        } catch (Exception e) {
            logger.severe("❌ Erro ao conectar com banco de dados: " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Carrega as configurações do plugin.
     */
    private void loadConfiguration() {
        try {
            // Salvar configuração padrão se não existir
            saveDefaultConfig();
            
            // Recarregar configuração
            reloadConfig();
            
            logger.info("✅ Configurações carregadas com sucesso");
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa os managers do sistema.
     */
    private void initializeManagers() {
        try {
            // Instanciar DAOs
            br.com.primeleague.essentials.dao.MySqlEssentialsDAO essentialsDAO = 
                new br.com.primeleague.essentials.dao.MySqlEssentialsDAO(this);
            br.com.primeleague.essentials.dao.MySqlWarpDAO warpDAO = 
                new br.com.primeleague.essentials.dao.MySqlWarpDAO(this);
            br.com.primeleague.essentials.dao.MySqlKitDAO kitDAO = 
                new br.com.primeleague.essentials.dao.MySqlKitDAO(this);
            
            // Inicializar Managers com injeção de dependência
            essentialsManager = new EssentialsManager(this, essentialsDAO);
            tpaManager = new TpaManager(this);
            kitManager = new KitManager(this, kitDAO);
            playerInfoManager = new PlayerInfoManager(this);
            warpManager = new WarpManager(this, warpDAO);
            
            logger.info("✅ Managers inicializados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao inicializar managers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra os comandos do plugin.
     */
    private void registerCommands() {
        try {
            // Registrar comando principal
            EssentialsCommand essentialsCommand = new EssentialsCommand(this);
            
            // Registrar comandos individuais
            getCommand("sethome").setExecutor(essentialsCommand);
            getCommand("home").setExecutor(essentialsCommand);
            getCommand("spawn").setExecutor(essentialsCommand);
            getCommand("delhome").setExecutor(essentialsCommand);
            getCommand("homes").setExecutor(essentialsCommand);
            getCommand("essentials").setExecutor(essentialsCommand);
            
            // Registrar comandos TPA
            TpaCommand tpaCommand = new TpaCommand(this, tpaManager);
            getCommand("tpa").setExecutor(tpaCommand);
            getCommand("tpahere").setExecutor(tpaCommand);
            getCommand("tpaccept").setExecutor(tpaCommand);
            getCommand("tpdeny").setExecutor(tpaCommand);
            getCommand("tpcancel").setExecutor(tpaCommand);
            getCommand("tpalist").setExecutor(tpaCommand);
            
            // Registrar comandos de Kits
            KitCommand kitCommand = new KitCommand(this, kitManager);
            getCommand("kit").setExecutor(kitCommand);
            
            // Registrar comando Discord
            DiscordCommand discordCommand = new DiscordCommand(this);
            getCommand("discord").setExecutor(discordCommand);
            
            // Registrar comando Seen
            SeenCommand seenCommand = new SeenCommand(this, playerInfoManager);
            getCommand("seen").setExecutor(seenCommand);
            
            // Registrar comando Whois
            WhoisCommand whoisCommand = new WhoisCommand(this, playerInfoManager);
            getCommand("whois").setExecutor(whoisCommand);
            
            // Registrar comandos de Warps
            WarpCommand warpCommand = new WarpCommand(this, warpManager);
            getCommand("warp").setExecutor(warpCommand);
            
            WarplistCommand warplistCommand = new WarplistCommand(this, warpManager);
            getCommand("warplist").setExecutor(warplistCommand);
            
            SetwarpCommand setwarpCommand = new SetwarpCommand(this, warpManager);
            getCommand("setwarp").setExecutor(setwarpCommand);
            
            DelwarpCommand delwarpCommand = new DelwarpCommand(this, warpManager);
            getCommand("delwarp").setExecutor(delwarpCommand);
            
            logger.info("✅ Comandos registrados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar comandos: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra os listeners do plugin.
     */
    private void registerListeners() {
        try {
            // Por enquanto, não há listeners específicos
            // Futuramente podem ser adicionados listeners para:
            // - Limpeza de cache quando jogador sai
            // - Validação de permissões em tempo real
            // - Logs de uso de comandos
            
            logger.info("✅ Listeners registrados com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtém o EssentialsManager.
     * 
     * @return EssentialsManager
     */
    public EssentialsManager getEssentialsManager() {
        return essentialsManager;
    }
    
    /**
     * Obtém o TpaManager.
     * 
     * @return TpaManager
     */
    public TpaManager getTpaManager() {
        return tpaManager;
    }
    
    /**
     * Obtém o KitManager.
     * 
     * @return KitManager
     */
    public KitManager getKitManager() {
        return kitManager;
    }
    
    /**
     * Obtém o PlayerInfoManager.
     * 
     * @return PlayerInfoManager
     */
    public PlayerInfoManager getPlayerInfoManager() {
        return playerInfoManager;
    }
    
    /**
     * Obtém o WarpManager.
     * 
     * @return Instância do WarpManager
     */
    public WarpManager getWarpManager() {
        return warpManager;
    }
    
    /**
     * Recarrega as configurações do plugin.
     */
    public void reloadPlugin() {
        try {
            reloadConfig();
            
            if (essentialsManager != null) {
                essentialsManager.clearAllCache();
            }
            
            logger.info("✅ Plugin recarregado com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao recarregar plugin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registra os DAOs no Core via DAOServiceRegistry.
     * Permite que outros módulos acessem os DAOs do Essentials através do Core.
     */
    private void registerDAOs() {
        try {
            // Obter o DAOServiceRegistry do Core
            br.com.primeleague.core.services.DAOServiceRegistry registry = 
                PrimeLeagueAPI.getDAOServiceRegistry();
            
            // Registrar EssentialsDAO
            registry.registerDAO(br.com.primeleague.api.dao.EssentialsDAO.class, 
                essentialsManager.getEssentialsDAO());
            
            // Registrar WarpDAO
            registry.registerDAO(br.com.primeleague.api.dao.WarpDAO.class, 
                warpManager.getWarpDAO());
            
            // Registrar KitDAO
            registry.registerDAO(br.com.primeleague.api.dao.KitDAO.class, 
                kitManager.getKitDAO());
            
            logger.info("✅ DAOs do Essentials registrados no Core com sucesso!");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao registrar DAOs no Core: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
