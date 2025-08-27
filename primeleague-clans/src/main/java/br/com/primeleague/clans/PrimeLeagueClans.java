package br.com.primeleague.clans;

import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.commands.ClanCommand;
import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.api.TagServiceRegistry;
import br.com.primeleague.api.DAOServiceRegistry;
import br.com.primeleague.api.ClanServiceRegistry;
import br.com.primeleague.clans.services.ClanServiceImpl;

import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.clans.model.ClanPlayer;
import br.com.primeleague.clans.model.ClanRelation;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Plugin principal do sistema de Clãs do Prime League.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class PrimeLeagueClans extends JavaPlugin {

    private static PrimeLeagueClans instance;
    private ClanManager clanManager;
    private ClanDAO clanDAO;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializar o DAO (será fornecido pelo Core)
        initializeDAO();
        
        // Inicializar o gerenciador de clãs com injeção de dependência
        this.clanManager = new ClanManager(this, clanDAO);
        
        // Registrar ClanService na API
        ClanServiceRegistry.register(new ClanServiceImpl(clanManager));
        
        // Carregar dados do banco
        clanManager.load();
        
        // Registrar comandos
        registerCommands();
        
        // Iniciar limpeza automática de convites expirados (a cada 5 minutos)
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                clanManager.cleanupExpiredInvites();
                if (clanManager.getPendingInviteCount() > 0) {
                    getLogger().info("Limpeza de convites: " + clanManager.getPendingInviteCount() + " convites ativos.");
                }
            }
        }, 6000L, 6000L); // 6000 ticks = 5 minutos
        
        // Agendar tarefa de limpeza de membros inativos
        scheduleInactiveMemberCleanup();
        
        // Registrar listeners
        registerListeners();
        
        // Registrar handler para placeholder {clan_tag} no TagManager do Core
        registerTagHandlers();
        
        getLogger().info("=== Prime League Clans v1.0.0 ===");
        getLogger().info("Sistema de clãs inicializado com sucesso!");
        getLogger().info("Comandos disponíveis:");
        getLogger().info("  /clan - Comandos principais do sistema de clãs");
        getLogger().info("=====================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("=== Prime League Clans v1.0.0 ===");
        getLogger().info("Sistema de clãs desligado.");
        getLogger().info("=====================================");
        
        // Salvar dados (quando integração com Core estiver pronta)
        // saveData();
        
        instance = null;
    }

    /**
     * Registra os comandos do plugin.
     */
    private void registerCommands() {
        // Comando principal /clan
        ClanCommand clanCommand = new ClanCommand(this);
        getCommand("clan").setExecutor(clanCommand);
    }

    /**
     * Registra os listeners do plugin.
     */
    private void registerListeners() {
        // Listener para estatísticas de KDR
        getServer().getPluginManager().registerEvents(new br.com.primeleague.clans.listeners.PlayerStatsListener(this), this);
        
        // Listener para sistema de friendly fire
        getServer().getPluginManager().registerEvents(new br.com.primeleague.clans.listeners.DamageListener(this), this);
        
        // Listener para sistema de sanções de clã
        getServer().getPluginManager().registerEvents(new br.com.primeleague.clans.listeners.PunishmentListener(this, clanManager), this);
        
        // REFATORADO: Listener para gerenciamento de status online/offline
        getServer().getPluginManager().registerEvents(new br.com.primeleague.clans.listeners.PlayerConnectionListener(clanManager), this);
        
        getLogger().info("Listeners registrados: PlayerStatsListener, DamageListener, PunishmentListener, PlayerConnectionListener");
    }

    /**
     * Registra handlers de placeholders no TagManager do Core.
     * Implementa o Padrão de Provedor para exposição de dados de clã.
     */
    private void registerTagHandlers() {
        try {
            // Registrar handler para {clan_tag}
            TagServiceRegistry.registerPlaceholder("clan_tag", new br.com.primeleague.api.TagService.PlaceholderHandler() {
                @Override
                public String resolve(Player player) {
                    try {
                        Clan clan = clanManager.getClanByPlayer(player);
                        if (clan != null) {
                            return "§7[" + clan.getTag() + "]";
                        }
                        return ""; // Retorna string vazia se não tiver clã
                    } catch (Exception e) {
                        // Retorna string vazia em caso de erro
                        return "";
                    }
                }
            });
            
            getLogger().info("Handler {clan_tag} registrado no TagManager com sucesso!");
            
        } catch (Exception e) {
            getLogger().warning("Falha ao registrar handlers de tags: " + e.getMessage());
            getLogger().warning("Tags de clã podem não funcionar corretamente.");
        }
    }

    /**
     * Obtém a instância estática do plugin.
     *
     * @return A instância do plugin
     */
    public static PrimeLeagueClans getInstance() {
        return instance;
    }

    /**
     * Obtém o gerenciador de clãs.
     *
     * @return O gerenciador de clãs
     */
    public ClanManager getClanManager() {
        return clanManager;
    }

    /**
     * Inicializa o DAO para acesso ao banco de dados.
     * Obtém a implementação real do Core através do DAOServiceRegistry.
     */
    private void initializeDAO() {
        try {
            // Obter o DAO através do registry (sem reflection)
            this.clanDAO = DAOServiceRegistry.getClanDAO();
            getLogger().info("DAO de clãs conectado ao Core com sucesso!");
        } catch (IllegalStateException e) {
            // Se não conseguir conectar ao Core, desabilitar o plugin
            getLogger().severe("PrimeLeague-Core não encontrado ou não está habilitado!");
            getLogger().severe("O módulo de Clãs requer o PrimeLeague-Core para funcionar.");
            getLogger().severe("Erro: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Salva os dados do sistema (será implementado quando Core estiver pronto).
     */
    private void saveData() {
        // TODO: Implementar salvamento de dados quando integração com Core estiver pronta
        getLogger().info("Salvando dados do sistema de clãs...");
    }
    
    /**
     * Agenda a tarefa de limpeza de membros inativos.
     * Executa uma vez por dia no horário configurado.
     */
    private void scheduleInactiveMemberCleanup() {
        // Verificar se o sistema está habilitado
        if (!getConfig().getBoolean("inactive-member-cleanup.enabled", true)) {
            getLogger().info("Sistema de limpeza de membros inativos está desabilitado.");
            return;
        }
        
        int executionHour = getConfig().getInt("inactive-member-cleanup.execution-hour", 3);
        long initialDelay = calculateInitialDelay(executionHour);
        long period = 24 * 60 * 60 * 20; // 24 horas em ticks (20 ticks = 1 segundo)
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                getLogger().info("Executando limpeza automática de membros inativos...");
                boolean success = clanManager.cleanupInactiveMembers();
                if (success) {
                    getLogger().info("Limpeza de membros inativos concluída com sucesso.");
                } else {
                    getLogger().warning("Falha na limpeza de membros inativos.");
                }
            }
        }, initialDelay, period);
        
        getLogger().info("Tarefa de limpeza de membros inativos agendada para executar às " + 
                        executionHour + ":00 todos os dias (delay inicial: " + (initialDelay / 20 / 60) + " minutos)");
    }
    
    /**
     * Calcula o delay inicial para que a tarefa execute no horário especificado.
     * Implementação compatível com Java 7.
     * 
     * @param targetHour Hora do dia para execução (0-23)
     * @return Delay em ticks
     */
    private long calculateInitialDelay(int targetHour) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();

        target.set(Calendar.HOUR_OF_DAY, targetHour);
        target.set(Calendar.MINUTE, 0);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        // Se o horário alvo para hoje já passou, agenda para o dia seguinte
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Calcula a diferença em milissegundos e converte para ticks (20 ticks/segundo)
        long delayInMillis = target.getTimeInMillis() - now.getTimeInMillis();
        return delayInMillis / 50;
    }
}
