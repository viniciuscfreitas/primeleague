package br.com.primeleague.admin;

import br.com.primeleague.admin.api.AdminAPI;
import br.com.primeleague.admin.commands.*;
import br.com.primeleague.admin.listeners.*;
import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.services.AdminServiceImpl;
import br.com.primeleague.api.AdminServiceRegistry;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin principal do sistema administrativo do Prime League.
 * Gerencia punições, tickets de denúncia e modo staff.
 */
public class PrimeLeagueAdmin extends JavaPlugin {

    private static PrimeLeagueAdmin instance;
    private AdminManager adminManager;

    @Override
    public void onEnable() {
        instance = this;

        // Salvar configuração padrão
        saveDefaultConfig();

        // Verificar dependências críticas (Core)
        if (!checkCoreDependency()) {
            getLogger().severe("PrimeLeague-Core não encontrado! Desabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Inicializar gerenciador administrativo
        adminManager = AdminManager.getInstance();

        // Registrar AdminService na API
        AdminServiceRegistry.register(new AdminServiceImpl(adminManager));

        // Inicializar API
        AdminAPI.initialize(adminManager);

        // Registrar comandos
        registerCommands();

        // Registrar listeners
        registerListeners();

        // Agendar integração P2P para o próximo tick (evita race condition)
        getServer().getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                // Este código rodará DEPOIS que todos os plugins, incluindo o P2P,
                // tiverem completado seu onEnable().
                if (getServer().getPluginManager().getPlugin("PrimeLeague-P2P") != null) {
                    // Aqui você faz o "gancho" na API do P2P
                    // Ex: p2pApi = PrimeLeagueP2P.getApi();
                } else {
                    getLogger().warning("PrimeLeague-P2P não foi encontrado. A integração de punições P2P estará desabilitada.");
                }
            }
        }, 1L); // O '1L' representa o atraso de 1 tick.

        // Limpar banimentos nativos conflitantes
        cleanupNativeBans();

        getLogger().info("[Admin] PrimeLeague Admin habilitado");
    }

    @Override
    public void onDisable() {
        // Salvar dados pendentes
        if (adminManager != null) {
            // Implementar salvamento de dados se necessário
        }

        // Desabilitar API
        AdminAPI.shutdown();

        getLogger().info("[Admin] PrimeLeague Admin desabilitado");
    }

    /**
     * Verifica se as dependências críticas (Core) estão disponíveis.
     */
    private boolean checkCoreDependency() {
        // Verificar Core (crítico)
        if (getServer().getPluginManager().getPlugin("PrimeLeague-Core") == null) {
            getLogger().severe("PrimeLeague-Core não encontrado!");
            return false;
        }
        return true;
    }

    /**
     * Limpa banimentos nativos do Bukkit para usar apenas o sistema customizado.
     */
    private void cleanupNativeBans() {
        try {
            // Não vamos remover banimentos nativos automaticamente para não causar problemas
            // Apenas avisar que o sistema customizado está ativo
            getLogger().info("Sistema de banimento customizado ativo!");

        } catch (Exception e) {
            getLogger().warning("Erro ao limpar banimentos nativos: " + e.getMessage());
        }
    }

    /**
     * Registra todos os comandos do módulo administrativo.
     */
    private void registerCommands() {
        // Comandos de modo staff
        getCommand("vanish").setExecutor(new VanishCommand(adminManager));
        getCommand("invsee").setExecutor(new InvseeCommand(adminManager));
        getCommand("inspect").setExecutor(new InspectCommand(adminManager));

        // Comandos de tickets
        getCommand("report").setExecutor(new ReportCommand(adminManager));
        getCommand("tickets").setExecutor(new TicketsCommand(adminManager));

        // Comandos de punição
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("history").setExecutor(new HistoryCommand(this));
        
        // Comando de whitelist
        getCommand("whitelist").setExecutor(new WhitelistCommand(this));
    }

    /**
     * Registra todos os listeners do módulo administrativo.
     */
    private void registerListeners() {
        // Listeners de verificação de punições
        getServer().getPluginManager().registerEvents(new ChatListener(adminManager), this);
        getServer().getPluginManager().registerEvents(new LoginListener(adminManager), this);

        // Listeners de modo staff
        getServer().getPluginManager().registerEvents(new VanishListener(adminManager), this);

        // Listener de join para carregar estado de vanish
        getServer().getPluginManager().registerEvents(new JoinListener(adminManager), this);
    }

    /**
     * Obtém a instância do plugin.
     */
    public static PrimeLeagueAdmin getInstance() {
        return instance;
    }

    /**
     * Obtém o gerenciador administrativo.
     */
    public AdminManager getAdminManager() {
        return adminManager;
    }
}
