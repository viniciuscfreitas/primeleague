package br.com.primeleague.core;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.api.ProfileServiceRegistry;
import br.com.primeleague.api.TagServiceRegistry;
import br.com.primeleague.api.DAOServiceRegistry;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.database.MySqlClanDAO;
import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.managers.IdentityManager;
import br.com.primeleague.core.managers.DonorManager;
import br.com.primeleague.core.profile.ProfileListener;
import br.com.primeleague.core.services.CoreProfileService;
import br.com.primeleague.core.services.TagManager;
import br.com.primeleague.core.services.TagManagerImpl;
import br.com.primeleague.core.services.TagServiceAdapter;
import br.com.primeleague.core.services.DAOServiceImpl;
import br.com.primeleague.core.managers.PrivateMessageManager;
import br.com.primeleague.core.managers.MessageManager;
import br.com.primeleague.core.managers.EconomyManager;
import br.com.primeleague.core.commands.PrivateMessageCommand;
import br.com.primeleague.core.commands.ReplyCommand;
import br.com.primeleague.core.commands.MoneyCommand;
import br.com.primeleague.core.commands.PayCommand;
import br.com.primeleague.core.commands.EcoCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

public final class PrimeLeagueCore extends JavaPlugin {

    private static PrimeLeagueCore instance;
    private Logger logger;
    private DataManager dataManager;
    private IdentityManager identityManager;
    private DonorManager donorManager;
    private ClanDAO clanDAO;
    private TagManager tagManager;
    private PrivateMessageManager privateMessageManager;
    private EconomyManager economyManager;
    private HttpApiManager httpApiManager;

    @Override
    public void onEnable() {
        instance = this;
        this.logger = getLogger();

        saveDefaultConfig();

        this.dataManager = new DataManager(this);
        this.dataManager.connect();

        // Inicializa o IdentityManager (coração da arquitetura de segurança)
        this.identityManager = new IdentityManager(this, this.dataManager);

        // Inicializa o DonorManager (sistema de doadores)
        this.donorManager = new DonorManager(this);

        // Inicializa o DAO de clãs
        this.clanDAO = new MySqlClanDAO(this);

        // Inicializa o TagManager
        this.tagManager = new TagManagerImpl(this.dataManager);
        
        // Inicializa o PrivateMessageManager
        this.privateMessageManager = new PrivateMessageManager(this.tagManager, MessageManager.getInstance());
        
        // Inicializa o EconomyManager (após DonorManager para integração)
        this.economyManager = new EconomyManager(this);
        
        // Inicializa API HTTP (para integração com bot Discord)
        if (getConfig().getBoolean("api.enabled", true)) {
            this.httpApiManager = new HttpApiManager(this);
            this.httpApiManager.start();
        }
        
        // Inicializa API
        PrimeLeagueAPI.initialize(this);
        
        // Registra o DataManager como provedor de perfis para outros módulos
        PrimeLeagueAPI.registerProfileProvider(new PrimeLeagueAPI.ProfileProvider() {
            @Override
            public br.com.primeleague.core.models.PlayerProfile getProfile(UUID uuid) {
                return getDataManager().loadOfflinePlayerProfile(uuid);
            }

            @Override
            public br.com.primeleague.core.models.PlayerProfile getProfile(String name) {
                return getDataManager().loadOfflinePlayerProfile(name);
            }
        });
        
        // Registra o ProfileService para outros módulos
        ProfileServiceRegistry.register(new CoreProfileService(this.dataManager));
        
        // Registra o TagService para outros módulos
        TagServiceRegistry.register(new TagServiceAdapter(this.tagManager));
        
        // Registra o DAOService para outros módulos
        DAOServiceRegistry.register(new DAOServiceImpl(this.clanDAO));

        // Registra comandos
        getCommand("msg").setExecutor(new PrivateMessageCommand(this.privateMessageManager));
        getCommand("tell").setExecutor(new PrivateMessageCommand(this.privateMessageManager));
        getCommand("r").setExecutor(new ReplyCommand(this.privateMessageManager));
        
        // Registra comandos econômicos
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pagar").setExecutor(new PayCommand(this));
        getCommand("eco").setExecutor(new EcoCommand(this));
        

        
        // Registra listeners
        getServer().getPluginManager().registerEvents(new ProfileListener(this.dataManager), this);
        
        logger.info("✅ [CORE] PrimeLeague Core V2.0 habilitado com sucesso!");
        logger.info("✅ [CORE] Sistema de identidade: ATIVO");
        logger.info("✅ [CORE] Sistema de doadores: ATIVO");
        logger.info("✅ [CORE] Sistema econômico: ATIVO");
        if (httpApiManager != null) {
            logger.info("✅ [CORE] API HTTP: ATIVA");
        }
    }

    @Override
    public void onDisable() {
        logger.info("🔄 [CORE] PrimeLeague Core sendo desabilitado...");
        
        // Parar API HTTP
        if (httpApiManager != null) {
            httpApiManager.stop();
        }
        
        // Limpar caches
        if (economyManager != null) {
            economyManager.clearAllCache();
        }
        if (donorManager != null) {
            donorManager.clearAllCache();
        }
        
        logger.info("✅ [CORE] PrimeLeague Core desabilitado com sucesso!");
    }

    public static PrimeLeagueCore getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public IdentityManager getIdentityManager() {
        return identityManager;
    }
    
    public DonorManager getDonorManager() {
        return donorManager;
    }

    public ClanDAO getClanDAO() {
        return clanDAO;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public HttpApiManager getHttpApiManager() {
        return httpApiManager;
    }
}


