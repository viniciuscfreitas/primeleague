package br.com.primeleague.essentials.managers;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.dao.EssentialsDAO;
import br.com.primeleague.essentials.dao.MySqlEssentialsDAO;
import br.com.primeleague.api.models.Home;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Gerenciador principal do sistema de comandos essenciais.
 * Orquestrador assíncrono responsável por gerenciar homes, teletransporte e spawn.
 * Segue o padrão arquitetural estabelecido para operações não-bloqueantes.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class EssentialsManager {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    private final EssentialsDAO essentialsDAO;
    
    // Cache de homes por jogador (UUID -> List<Home>)
    private final Map<UUID, List<Home>> playerHomesCache = new ConcurrentHashMap<>();
    
    // Cache de cooldowns por jogador (UUID -> timestamp)
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    
    // Configurações do sistema
    private int defaultHomeLimit = 3;
    private int teleportCooldownSeconds = 5;
    private int spawnCooldownSeconds = 10;
    private boolean allowCrossWorldTeleport = true;
    
    /**
     * Construtor do EssentialsManager.
     * 
     * @param plugin Instância do plugin principal
     * @param essentialsDAO Instância do DAO (fornecida via injeção de dependência)
     */
    public EssentialsManager(EssentialsPlugin plugin, EssentialsDAO essentialsDAO) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.essentialsDAO = essentialsDAO;
        
        // Carregar configurações
        loadConfiguration();
        
        // Inicializar cache
        initializeCache();
        
        logger.info("✅ EssentialsManager inicializado com sucesso!");
    }
    
    /**
     * Carrega as configurações do arquivo config.yml.
     */
    private void loadConfiguration() {
        try {
            defaultHomeLimit = plugin.getConfig().getInt("homes.default-limit", 3);
            teleportCooldownSeconds = plugin.getConfig().getInt("teleport.cooldown", 5);
            spawnCooldownSeconds = plugin.getConfig().getInt("spawn.cooldown", 10);
            allowCrossWorldTeleport = plugin.getConfig().getBoolean("teleport.cross-world", true);
            
            logger.info("✅ Configurações carregadas - Limite homes: " + defaultHomeLimit + 
                       ", Cooldown teleporte: " + teleportCooldownSeconds + "s");
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa o cache de homes dos jogadores.
     */
    private void initializeCache() {
        // Cache será populado conforme necessário
        logger.info("✅ Cache de homes inicializado");
    }
    
    // ========================================
    // MÉTODOS DE HOME MANAGEMENT
    // ========================================
    
    /**
     * Cria uma nova home para o jogador de forma assíncrona.
     * 
     * @param player Jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a operação for concluída
     */
    public void createHomeAsync(Player player, String homeName, Consumer<Boolean> callback) {
        if (player == null || homeName == null || homeName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Validações rápidas na thread principal
        if (!isValidHomeName(homeName)) {
            player.sendMessage("§cNome da home inválido! Use apenas letras, números e underscore.");
            callback.accept(false);
            return;
        }
        
        // Verificar limite de homes de forma assíncrona
        essentialsDAO.getPlayerHomeCountAsync(player.getUniqueId(), (currentHomes) -> {
            int maxHomes = getMaxHomesForPlayer(player);
            
            if (currentHomes >= maxHomes) {
                player.sendMessage("§cVocê atingiu o limite máximo de homes (" + maxHomes + ")!");
                callback.accept(false);
                return;
            }
            
            // Verificar se já existe home com esse nome de forma assíncrona
            essentialsDAO.homeExistsAsync(player.getUniqueId(), homeName, (exists) -> {
                if (exists) {
                    player.sendMessage("§cVocê já possui uma home com o nome '" + homeName + "'!");
                    callback.accept(false);
                    return;
                }
                
                // Obter ID do jogador de forma assíncrona
                essentialsDAO.getPlayerIdAsync(player.getUniqueId(), (playerId) -> {
                    if (playerId == -1) {
                        player.sendMessage("§cErro ao obter dados do jogador!");
                        callback.accept(false);
                        return;
                    }
                    
                    // Criar home
                    Location location = player.getLocation();
                    Home home = new Home(
                        playerId,
                        homeName,
                        location.getWorld().getName(),
                        location.getX(),
                        location.getY(),
                        location.getZ(),
                        location.getYaw(),
                        location.getPitch()
                    );
                    
                    // Salvar no banco de dados de forma assíncrona
                    essentialsDAO.createHomeAsync(home, (success) -> {
                        if (success) {
                            // Atualizar cache
                            addHomeToCache(player.getUniqueId(), home);
                            
                            player.sendMessage("§aHome '" + homeName + "' criada com sucesso!");
                            logger.info("✅ Home criada: " + homeName + " para " + player.getName());
                        } else {
                            player.sendMessage("§cErro ao criar home! Tente novamente.");
                            logger.severe("❌ Erro ao criar home para " + player.getName());
                        }
                        
                        callback.accept(success);
                    });
                });
            });
        });
    }
    
    /**
     * Teleporta o jogador para uma home de forma assíncrona.
     * 
     * @param player Jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a operação for concluída
     */
    public void teleportToHomeAsync(Player player, String homeName, Consumer<Boolean> callback) {
        if (player == null || homeName == null || homeName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Verificar cooldown (validação rápida na thread principal)
        if (!canTeleport(player)) {
            long remaining = getTeleportCooldownRemaining(player);
            player.sendMessage("§cAguarde " + remaining + " segundos para usar o teletransporte novamente!");
            callback.accept(false);
            return;
        }
        
        // Buscar home de forma assíncrona
        essentialsDAO.getHomeAsync(player.getUniqueId(), homeName, (home) -> {
            if (home == null) {
                player.sendMessage("§cHome '" + homeName + "' não encontrada!");
                callback.accept(false);
                return;
            }
            
            // Verificar se o mundo existe
            World world = Bukkit.getWorld(home.getWorld());
            if (world == null) {
                player.sendMessage("§cMundo '" + home.getWorld() + "' não está disponível!");
                callback.accept(false);
                return;
            }
            
            // Verificar teletransporte entre mundos
            if (!allowCrossWorldTeleport && !player.getWorld().getName().equals(home.getWorld())) {
                player.sendMessage("§cTeletransporte entre mundos está desabilitado!");
                callback.accept(false);
                return;
            }
            
            // Criar localização
            Location location = new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
            
            // Teleportar
            player.teleport(location);
            
            // Atualizar último uso de forma assíncrona
            essentialsDAO.updateHomeLastUsedAsync(home.getHomeId(), (success) -> {
                if (success) {
                    home.markAsUsed();
                }
            });
            
            // Aplicar cooldown
            setTeleportCooldown(player);
            
            player.sendMessage("§aTeleportado para home '" + homeName + "'!");
            logger.info("✅ Jogador " + player.getName() + " teleportado para home " + homeName);
            callback.accept(true);
        });
    }
    
    /**
     * Teleporta o jogador para o spawn.
     * 
     * @param player Jogador
     * @return true se teleportado com sucesso
     */
    public boolean teleportToSpawn(Player player) {
        if (player == null) {
            return false;
        }
        
        try {
            // Verificar cooldown
            if (!canTeleport(player)) {
                long remaining = getTeleportCooldownRemaining(player);
                player.sendMessage("§cAguarde " + remaining + " segundos para usar o teletransporte novamente!");
                return false;
            }
            
            // Buscar spawn do mundo
            Location spawn = player.getWorld().getSpawnLocation();
            if (spawn == null) {
                player.sendMessage("§cSpawn não encontrado!");
                return false;
            }
            
            // Teleportar
            player.teleport(spawn);
            
            // Aplicar cooldown
            setTeleportCooldown(player);
            
            player.sendMessage("§aTeleportado para o spawn!");
            logger.info("✅ Jogador " + player.getName() + " teleportado para spawn");
            return true;
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao teleportar " + player.getName() + " para spawn: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Remove uma home do jogador de forma assíncrona.
     * 
     * @param player Jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a operação for concluída
     */
    public void removeHomeAsync(Player player, String homeName, Consumer<Boolean> callback) {
        if (player == null || homeName == null || homeName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Buscar home de forma assíncrona
        essentialsDAO.getHomeAsync(player.getUniqueId(), homeName, (home) -> {
            if (home == null) {
                player.sendMessage("§cHome '" + homeName + "' não encontrada!");
                callback.accept(false);
                return;
            }
            
            // Remover do banco de dados de forma assíncrona
            essentialsDAO.deleteHomeAsync(home.getHomeId(), (success) -> {
                if (success) {
                    // Remover do cache
                    removeHomeFromCache(player.getUniqueId(), homeName);
                    
                    player.sendMessage("§aHome '" + homeName + "' removida com sucesso!");
                    logger.info("✅ Home removida: " + homeName + " de " + player.getName());
                } else {
                    player.sendMessage("§cErro ao remover home! Tente novamente.");
                    logger.severe("❌ Erro ao remover home " + homeName + " de " + player.getName());
                }
                
                callback.accept(success);
            });
        });
    }
    
    // ========================================
    // MÉTODOS DE CACHE E BANCO DE DADOS
    // ========================================
    
    /**
     * Obtém uma home específica do jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a busca for concluída
     */
    public void getHomeAsync(UUID playerUuid, String homeName, Consumer<Home> callback) {
        // Verificar cache primeiro
        List<Home> cachedHomes = playerHomesCache.get(playerUuid);
        if (cachedHomes != null) {
            for (Home home : cachedHomes) {
                if (home.getHomeName().equalsIgnoreCase(homeName)) {
                    callback.accept(home);
                    return;
                }
            }
            callback.accept(null);
            return;
        }
        
        // Carregar do banco de dados de forma assíncrona
        essentialsDAO.getHomeAsync(playerUuid, homeName, callback);
    }
    
    /**
     * Obtém todas as homes de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a busca for concluída
     */
    public void getPlayerHomesAsync(UUID playerUuid, Consumer<List<Home>> callback) {
        // Verificar cache primeiro
        List<Home> cachedHomes = playerHomesCache.get(playerUuid);
        if (cachedHomes != null) {
            callback.accept(new ArrayList<>(cachedHomes));
            return;
        }
        
        // Carregar do banco de dados de forma assíncrona
        essentialsDAO.loadPlayerHomesAsync(playerUuid, (homes) -> {
            // Atualizar cache
            if (homes != null && !homes.isEmpty()) {
                playerHomesCache.put(playerUuid, new ArrayList<>(homes));
            }
            callback.accept(homes != null ? homes : new ArrayList<>());
        });
    }
    
    /**
     * Obtém o número de homes de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a contagem for concluída
     */
    public void getPlayerHomeCountAsync(UUID playerUuid, Consumer<Integer> callback) {
        // Verificar cache primeiro
        List<Home> cachedHomes = playerHomesCache.get(playerUuid);
        if (cachedHomes != null) {
            callback.accept(cachedHomes.size());
            return;
        }
        
        // Carregar do banco de dados de forma assíncrona
        essentialsDAO.getPlayerHomeCountAsync(playerUuid, callback);
    }
    
    /**
     * Obtém o limite máximo de homes para um jogador baseado em permissões.
     * 
     * @param player Jogador
     * @return Limite máximo de homes
     */
    public int getMaxHomesForPlayer(Player player) {
        // Verificar permissões específicas
        for (int i = 10; i >= 1; i--) {
            if (player.hasPermission("primeleague.homes.limit." + i)) {
                return i;
            }
        }
        
        // Verificar permissão de limite ilimitado
        if (player.hasPermission("primeleague.homes.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        // Retornar limite padrão
        return defaultHomeLimit;
    }
    
    // ========================================
    // MÉTODOS DE COOLDOWN
    // ========================================
    
    /**
     * Verifica se o jogador pode usar teletransporte (não está em cooldown).
     * 
     * @param player Jogador
     * @return true se pode teleportar
     */
    public boolean canTeleport(Player player) {
        // Bypass de cooldown para staff
        if (player.hasPermission("primeleague.cooldown.bypass.teleport")) {
            return true;
        }
        
        Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
        if (lastTeleport == null) {
            return true;
        }
        
        long cooldownMillis = teleportCooldownSeconds * 1000L;
        return System.currentTimeMillis() - lastTeleport >= cooldownMillis;
    }
    
    /**
     * Obtém o tempo restante do cooldown em segundos.
     * 
     * @param player Jogador
     * @return Segundos restantes
     */
    public long getTeleportCooldownRemaining(Player player) {
        Long lastTeleport = teleportCooldowns.get(player.getUniqueId());
        if (lastTeleport == null) {
            return 0;
        }
        
        long cooldownMillis = teleportCooldownSeconds * 1000L;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastTeleport);
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Define o cooldown de teletransporte para o jogador.
     * 
     * @param player Jogador
     */
    private void setTeleportCooldown(Player player) {
        teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    // ========================================
    // MÉTODOS DE VALIDAÇÃO
    // ========================================
    
    /**
     * Valida se o nome da home é válido.
     * 
     * @param homeName Nome da home
     * @return true se válido
     */
    private boolean isValidHomeName(String homeName) {
        if (homeName == null || homeName.trim().isEmpty()) {
            return false;
        }
        
        // Verificar tamanho
        if (homeName.length() > 32) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, underscore)
        return homeName.matches("^[a-zA-Z0-9_]+$");
    }
    
    // ========================================
    // MÉTODOS DE CACHE
    // ========================================
    
    /**
     * Adiciona uma home ao cache do jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param home Home a ser adicionada
     */
    private void addHomeToCache(UUID playerUuid, Home home) {
        List<Home> homes = playerHomesCache.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        homes.add(home);
    }
    
    /**
     * Remove uma home do cache do jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param homeName Nome da home
     */
    private void removeHomeFromCache(UUID playerUuid, String homeName) {
        List<Home> homes = playerHomesCache.get(playerUuid);
        if (homes != null) {
            homes.removeIf(home -> home.getHomeName().equalsIgnoreCase(homeName));
        }
    }
    
    /**
     * Limpa o cache de homes de um jogador.
     * 
     * @param playerUuid UUID do jogador
     */
    public void clearPlayerCache(UUID playerUuid) {
        playerHomesCache.remove(playerUuid);
        teleportCooldowns.remove(playerUuid);
    }
    
    /**
     * Limpa todo o cache.
     */
    public void clearAllCache() {
        playerHomesCache.clear();
        teleportCooldowns.clear();
        logger.info("✅ Cache de essentials limpo");
    }
    
    /**
     * Obtém a instância do EssentialsDAO para registro no Core.
     * 
     * @return Instância do EssentialsDAO
     */
    public EssentialsDAO getEssentialsDAO() {
        return essentialsDAO;
    }
}
