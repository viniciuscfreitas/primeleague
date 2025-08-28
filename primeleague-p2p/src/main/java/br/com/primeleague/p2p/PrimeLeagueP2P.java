package br.com.primeleague.p2p;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.p2p.commands.VerifyCommand;
import br.com.primeleague.p2p.listeners.AuthenticationListener;
import br.com.primeleague.p2p.listeners.BypassListener;
import br.com.primeleague.p2p.managers.LimboManager;
import br.com.primeleague.p2p.managers.IpAuthCache;
import br.com.primeleague.p2p.services.P2PServiceImpl;
import br.com.primeleague.p2p.tasks.CleanupTask;
import br.com.primeleague.p2p.web.PortfolioWebhookManager;
import br.com.primeleague.api.P2PServiceRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Módulo de Acesso P2P para o servidor Prime League.
 *
 * Este módulo implementa o Sistema de Portfólio de Contas:
 * - Verificação direta e simples por conta individual
 * - Estado de limbo para jogadores pendentes
 * - Webhook para notificações de pagamento individual
 * - Comandos administrativos para gerenciar acesso
 * - Sistema de bypass para administradores
 *
 * @author PrimeLeague Team
 * @version 3.0.0 (Sistema de Portfólio)
 */
public final class PrimeLeagueP2P extends JavaPlugin {

    private PortfolioWebhookManager webhookManager;
    private LimboManager limboManager;
    private AuthenticationListener authenticationListener;
    private IpAuthCache ipAuthCache;
    private static PrimeLeagueP2P instance;

    @Override
    public void onEnable() {
        instance = this;

        // Verificar se o Core está carregado
        if (!isCoreLoaded()) {
            getLogger().severe("PrimeLeague-Core não está carregado! Este módulo depende do Core.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Salvar configuração padrão
        saveDefaultConfig();

        // Inicializar componentes
        initializeManagers();
        initializeWebhook();
        registerListeners();
        registerCommands();
        startCleanupTask();

        getLogger().info("[P2P] PrimeLeague P2P habilitado");
    }

    @Override
    public void onDisable() {
        // Parar o servidor webhook
        if (webhookManager != null) {
            webhookManager.stopServer();
        }

        getLogger().info("[P2P] PrimeLeague P2P desabilitado");
    }

    /**
     * Verifica se o módulo Core está carregado.
     * @return true se o Core estiver disponível, false caso contrário
     */
    private boolean isCoreLoaded() {
        try {
            PrimeLeagueAPI.getDataManager();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Inicializa o servidor webhook para receber notificações de pagamento.
     */
    private void initializeWebhook() {
        try {
            int port = getConfig().getInt("webhook.port", 8765);
            String secret = getConfig().getString("webhook.secret", "");

            if (secret.isEmpty() || secret.equals("seu_webhook_secret_aqui")) {
                getLogger().warning("Webhook secret não configurado! Configure em config.yml");
                return;
            }

            if (webhookManager == null) {
                webhookManager = new PortfolioWebhookManager(secret);
                webhookManager.startServer(port);
            }
        } catch (Exception e) {
            getLogger().severe("Falha ao inicializar servidor webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Inicializa os managers do sistema.
     */
    private void initializeManagers() {
        try {
            // Inicializar LimboManager
            limboManager = new LimboManager();
            
            // Inicializar cache de IPs autorizados
            ipAuthCache = new IpAuthCache();
            
            // Registrar P2PService na API
            P2PServiceRegistry.register(new P2PServiceImpl(limboManager));
            
            // Inicializar AuthenticationListener (simplificado, sem SessionManager)
            authenticationListener = new AuthenticationListener();
            
        } catch (Exception e) {
            getLogger().severe("Erro ao inicializar managers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra os listeners do módulo.
     */
    private void registerListeners() {
        // Sistema de autenticação avançada (novo)
        getServer().getPluginManager().registerEvents(authenticationListener, this);

        // Listener para bypass de administradores
        getServer().getPluginManager().registerEvents(new BypassListener(), this);

        getLogger().info("Listeners registrados.");
    }

    /**
     * Registra os comandos do módulo.
     */
    private void registerCommands() {
        // Comando para jogadores /minhaassinatura
        getCommand("minhaassinatura").setExecutor(new br.com.primeleague.p2p.commands.MinhaAssinaturaCommand());

        // Comando administrativo /p2p
        getCommand("p2p").setExecutor(new br.com.primeleague.p2p.commands.P2PAdminCommand());

        // Comando de verificação /verify (integrado com LimboManager)
        getCommand("verify").setExecutor(new VerifyCommand(limboManager));



        getLogger().info("Comandos registrados.");
    }

    /**
     * Inicia a tarefa de limpeza automática.
     */
    private void startCleanupTask() {
        CleanupTask.startCleanupTask(this);
    }

    /**
     * Obtém a instância do plugin.
     * @return A instância do PrimeLeagueP2P
     */
    public static PrimeLeagueP2P getInstance() {
        return instance;
    }

    /**
     * Obtém o gerenciador de webhook.
     * @return O PortfolioWebhookManager ou null se não estiver inicializado
     */
    public PortfolioWebhookManager getWebhookManager() {
        return webhookManager;
    }
    
    /**
     * Obtém o gerenciador de limbo.
     * @return O LimboManager ou null se não estiver inicializado
     */
    public LimboManager getLimboManager() {
        return limboManager;
    }
    
    /**
     * Obtém o listener de autenticação.
     * @return O AuthenticationListener ou null se não estiver inicializado
     */
    public AuthenticationListener getAuthenticationListener() {
        return authenticationListener;
    }
    
    /**
     * Obtém o cache de IPs autorizados.
     * @return O IpAuthCache ou null se não estiver inicializado
     */
    public IpAuthCache getIpAuthCache() {
        return ipAuthCache;
    }
    

    
    /**
     * Obtém o gerenciador de banco de dados via Core.
     * @return O DatabaseManager do Core
     */
    public br.com.primeleague.core.managers.DataManager getDatabaseManager() {
        return PrimeLeagueAPI.getDataManager();
    }
}
