package br.com.primeleague.chat;

import br.com.primeleague.api.LoggingServiceRegistry;
import br.com.primeleague.chat.listeners.ChatListener;
import br.com.primeleague.chat.commands.ClanChatCommand;
import br.com.primeleague.chat.commands.AllyChatCommand;
import br.com.primeleague.chat.commands.ChatCommand;
import br.com.primeleague.chat.services.ChatLoggingService;
import br.com.primeleague.chat.services.ChannelManager;
import org.bukkit.plugin.java.JavaPlugin;

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
        
        getLogger().info("✅ PrimeLeague Chat habilitado");
    }

    @Override
    public void onDisable() {
        // Salvar logs pendentes
        if (loggingService != null) {
            loggingService.shutdown();
        }
        
        getLogger().info("✅ PrimeLeague Chat desabilitado");
        
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
        
        // Registrar no LoggingServiceRegistry para comunicação inter-módulo
        LoggingServiceRegistry.register(this.loggingService);
    }

    /**
     * Registra os listeners do plugin.
     */
    private void registerListeners() {
        // Listener principal para interceptar chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    /**
     * Registra os comandos do plugin.
     */
    private void registerCommands() {
        // Comando de chat de clã
        ClanChatCommand clanChatCommand = new ClanChatCommand(this);
        getCommand("c").setExecutor(clanChatCommand);
        
        // Comando de chat de aliança
        AllyChatCommand allyChatCommand = new AllyChatCommand(this);
        getCommand("a").setExecutor(allyChatCommand);
        
        // Comando geral de chat
        ChatCommand chatCommand = new ChatCommand(this);
        getCommand("chat").setExecutor(chatCommand);
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
}
