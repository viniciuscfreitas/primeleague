package br.com.primeleague.chat;

import br.com.primeleague.api.LoggingServiceRegistry;
import br.com.primeleague.chat.listeners.ChatListener;
import br.com.primeleague.chat.listeners.InventoryListener;
import br.com.primeleague.chat.commands.ClanChatCommand;
import br.com.primeleague.chat.commands.AllyChatCommand;
import br.com.primeleague.chat.commands.ChatCommand;
import br.com.primeleague.chat.commands.IgnoreCommand;
import br.com.primeleague.chat.commands.LogRotationCommand;
import br.com.primeleague.chat.commands.GlobalChatCommand;
import br.com.primeleague.chat.commands.MessageCommand;
import br.com.primeleague.chat.commands.ReplyCommand;
import br.com.primeleague.chat.commands.SocialSpyCommand;
import br.com.primeleague.chat.services.ChatLoggingService;
import br.com.primeleague.chat.services.ChannelManager;
import br.com.primeleague.chat.services.ChannelIgnoreService;
import br.com.primeleague.chat.services.LogRotationService;
import br.com.primeleague.chat.services.PrivateMessageService;
import org.bukkit.plugin.java.JavaPlugin;
import br.com.primeleague.chat.services.AdvancedFilterService;

/**
 * Plugin principal do sistema de Chat do Prime League.
 * Gerencia canais de comunicação (Global, Clã, Aliança, Local).
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class PrimeLeagueChat extends JavaPlugin {

    private static PrimeLeagueChat instance;
    private ChannelManager channelManager;
    private ChatLoggingService loggingService;
    private ChannelIgnoreService ignoreService;
    private AdvancedFilterService advancedFilterService;
    private LogRotationService logRotationService;
    private PrivateMessageService privateMessageService;

    @Override
    public void onEnable() {
        instance = this;
        
        // Salvar configuração padrão se não existir
        saveDefaultConfig();
        
        // Inicializar serviços
        initializeServices();
        
        // Registrar listeners
        registerListeners();
        
        // Registrar comandos
        registerCommands();
        
        getLogger().info("🔊 [Chat] PrimeLeague Chat habilitado");
        getLogger().info("   🛡️  Rate Limiting: Ativo");
        getLogger().info("   ⚡ Formatação Otimizada: Ativa");
        getLogger().info("   📊 Logging Assíncrono: Ativo");
        getLogger().info("   🔇 Sistema de Ignore: Ativo");
        getLogger().info("   📊 Rotação de Logs: Ativa");
    }

    @Override
    public void onDisable() {
        // Salvar logs pendentes
        if (loggingService != null) {
            loggingService.shutdown();
        }
        
        getLogger().info("[Chat] PrimeLeague Chat desabilitado");
        
        instance = null;
    }

    /**
     * Inicializa os serviços do plugin.
     */
    private void initializeServices() {
        // Inicializar gerenciador de canais
        this.channelManager = new ChannelManager(this);
        
        // Inicializar serviço de logging
        this.loggingService = new ChatLoggingService(this);
        
        // Inicializar serviço de ignore de canais
        this.ignoreService = new ChannelIgnoreService(this);
        
        // Inicializar serviço de filtros avançados
        this.advancedFilterService = new AdvancedFilterService(this);
        
        // Inicializar serviço de rotação de logs
        this.logRotationService = new LogRotationService(this);
        
        // Inicializar serviço de mensagens privadas
        this.privateMessageService = new PrivateMessageService(this);
        
        // Registrar no LoggingServiceRegistry para comunicação inter-módulo
        LoggingServiceRegistry.register(this.loggingService);
        
        // Iniciar rotação automática de logs
        this.logRotationService.startAutomaticRotation();
    }

    /**
     * Registra os listeners do plugin.
     */
    private void registerListeners() {
        // Listener principal para interceptar chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Listener para a GUI de ignore
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    /**
     * Registra os comandos do plugin.
     */
    private void registerCommands() {
        // Comando de chat global (quick send)
        GlobalChatCommand globalChatCommand = new GlobalChatCommand(this);
        getCommand("g").setExecutor(globalChatCommand);
        
        // Comando de chat de clã (quick send)
        ClanChatCommand clanChatCommand = new ClanChatCommand(this);
        getCommand("c").setExecutor(clanChatCommand);
        
        // Comando de chat de aliança (quick send)
        AllyChatCommand allyChatCommand = new AllyChatCommand(this);
        getCommand("a").setExecutor(allyChatCommand);
        
        // Comando de ajuda do chat
        ChatCommand chatCommand = new ChatCommand(this);
        getCommand("chat").setExecutor(chatCommand);
        
        // Comando de ignore de canais (GUI)
        IgnoreCommand ignoreCommand = new IgnoreCommand(this);
        getCommand("ignore").setExecutor(ignoreCommand);
        
        // Comandos de mensagens privadas
        MessageCommand messageCommand = new MessageCommand(this);
        getCommand("msg").setExecutor(messageCommand);
        
        ReplyCommand replyCommand = new ReplyCommand(this);
        getCommand("r").setExecutor(replyCommand);
        
        // Comando de social spy para administradores
        SocialSpyCommand socialSpyCommand = new SocialSpyCommand(this);
        getCommand("socialspy").setExecutor(socialSpyCommand);
        
        // Comando de rotação de logs
        LogRotationCommand logRotationCommand = new LogRotationCommand(this, logRotationService);
        getCommand("logrotation").setExecutor(logRotationCommand);
        getCommand("logrotation").setTabCompleter(logRotationCommand);
    }

    /**
     * Obtém a instância estática do plugin.
     *
     * @return A instância do plugin
     */
    public static PrimeLeagueChat getInstance() {
        return instance;
    }

    /**
     * Obtém o gerenciador de canais.
     *
     * @return O gerenciador de canais
     */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * Obtém o serviço de logging.
     *
     * @return O serviço de logging
     */
    public ChatLoggingService getLoggingService() {
        return loggingService;
    }
    
    /**
     * Obtém o serviço de ignore de canais.
     *
     * @return O serviço de ignore de canais
     */
    public ChannelIgnoreService getIgnoreService() {
        return ignoreService;
    }
    
    /**
     * Obtém o serviço de filtros avançados.
     *
     * @return O serviço de filtros avançados
     */
    public AdvancedFilterService getAdvancedFilterService() {
        return advancedFilterService;
    }
    
    /**
     * Obtém o serviço de rotação de logs.
     *
     * @return O serviço de rotação de logs
     */
    public LogRotationService getLogRotationService() {
        return logRotationService;
    }
    
    /**
     * Obtém o serviço de mensagens privadas.
     *
     * @return O serviço de mensagens privadas
     */
    public PrivateMessageService getPrivateMessageService() {
        return privateMessageService;
    }
}
