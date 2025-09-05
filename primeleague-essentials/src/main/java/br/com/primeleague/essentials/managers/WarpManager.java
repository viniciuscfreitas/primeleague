package br.com.primeleague.essentials.managers;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.dao.WarpDAO;
import br.com.primeleague.essentials.dao.MySqlWarpDAO;
import br.com.primeleague.api.models.Warp;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Gerenciador do sistema de warps públicos.
 * Orquestrador assíncrono responsável por gerenciar warps com custo e permissões.
 * Integra com EconomyManager para cobrança de taxas.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class WarpManager {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    private final WarpDAO warpDAO;
    
    // Cache de warps (nome -> Warp)
    private final Map<String, Warp> warpCache = new ConcurrentHashMap<>();
    
    // Cache de cooldowns por jogador (UUID -> timestamp)
    private final Map<String, Long> warpCooldowns = new ConcurrentHashMap<>();
    
    // Configurações do sistema
    private int warpCooldownSeconds = 3;
    private boolean allowCrossWorldWarp = true;
    
    /**
     * Construtor do WarpManager.
     * 
     * @param plugin Instância do plugin principal
     * @param warpDAO Instância do DAO (fornecida via injeção de dependência)
     */
    public WarpManager(EssentialsPlugin plugin, WarpDAO warpDAO) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.warpDAO = warpDAO;
        
        // Carregar configurações
        loadConfiguration();
        
        // Inicializar cache
        initializeCache();
        
        logger.info("✅ WarpManager inicializado com sucesso!");
    }
    
    /**
     * Carrega as configurações do arquivo config.yml.
     */
    private void loadConfiguration() {
        try {
            warpCooldownSeconds = plugin.getConfig().getInt("warps.cooldown", 3);
            allowCrossWorldWarp = plugin.getConfig().getBoolean("warps.cross-world", true);
            
            logger.info("✅ Configurações de warps carregadas - Cooldown: " + warpCooldownSeconds + "s");
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações de warps: " + e.getMessage());
        }
    }
    
    /**
     * Inicializa o cache de warps.
     */
    private void initializeCache() {
        // Cache será populado conforme necessário
        logger.info("✅ Cache de warps inicializado");
    }
    
    // ========================================
    // MÉTODOS DE WARP MANAGEMENT
    // ========================================
    
    /**
     * Cria um novo warp de forma assíncrona.
     * 
     * @param player Jogador que está criando o warp
     * @param warpName Nome do warp
     * @param cost Custo do warp
     * @param permissionNode Permissão necessária (null = todos)
     * @param callback Callback executado quando a operação for concluída
     */
    public void createWarpAsync(Player player, String warpName, BigDecimal cost, String permissionNode, Consumer<Boolean> callback) {
        if (player == null || warpName == null || warpName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Validações rápidas na thread principal
        if (!isValidWarpName(warpName)) {
            player.sendMessage("§cNome do warp inválido! Use apenas letras, números e underscore.");
            callback.accept(false);
            return;
        }
        
        // Verificar se o warp já existe de forma assíncrona
        warpDAO.warpExistsAsync(warpName, (exists) -> {
            if (exists) {
                player.sendMessage("§cJá existe um warp com o nome '" + warpName + "'!");
                callback.accept(false);
                return;
            }
            
            // Criar warp
            Location location = player.getLocation();
            Warp warp = new Warp(
                warpName,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                permissionNode,
                cost != null ? cost : BigDecimal.ZERO,
                player.getName()
            );
            
            // Salvar no banco de dados de forma assíncrona
            warpDAO.createWarpAsync(warp, (success) -> {
                if (success) {
                    // Atualizar cache
                    warpCache.put(warpName.toLowerCase(), warp);
                    
                    player.sendMessage("§aWarp '" + warpName + "' criado com sucesso!");
                    if (warp.hasCost()) {
                        player.sendMessage("§7Custo: §a$" + warp.getFormattedCost());
                    }
                    if (warp.requiresPermission()) {
                        player.sendMessage("§7Permissão: §e" + warp.getPermissionNode());
                    }
                    
                    logger.info("✅ Warp criado: " + warpName + " por " + player.getName());
                } else {
                    player.sendMessage("§cErro ao criar warp! Tente novamente.");
                    logger.severe("❌ Erro ao criar warp " + warpName + " por " + player.getName());
                }
                
                callback.accept(success);
            });
        });
    }
    
    /**
     * Teleporta o jogador para um warp de forma assíncrona.
     * 
     * @param player Jogador
     * @param warpName Nome do warp
     * @param callback Callback executado quando a operação for concluída
     */
    public void teleportToWarpAsync(Player player, String warpName, Consumer<Boolean> callback) {
        if (player == null || warpName == null || warpName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Verificar cooldown (validação rápida na thread principal)
        if (!canUseWarp(player)) {
            long remaining = getWarpCooldownRemaining(player);
            player.sendMessage("§cAguarde " + remaining + " segundos para usar warps novamente!");
            callback.accept(false);
            return;
        }
        
        // Buscar warp de forma assíncrona
        warpDAO.getWarpAsync(warpName, (warp) -> {
            if (warp == null) {
                player.sendMessage("§cWarp '" + warpName + "' não encontrado!");
                callback.accept(false);
                return;
            }
            
            // Verificar permissão
            if (warp.requiresPermission() && !player.hasPermission(warp.getPermissionNode())) {
                player.sendMessage("§cVocê não tem permissão para usar este warp!");
                callback.accept(false);
                return;
            }
            
            // Verificar se o mundo existe
            World world = Bukkit.getWorld(warp.getWorld());
            if (world == null) {
                player.sendMessage("§cMundo '" + warp.getWorld() + "' não está disponível!");
                callback.accept(false);
                return;
            }
            
            // Verificar teletransporte entre mundos
            if (!allowCrossWorldWarp && !player.getWorld().getName().equals(warp.getWorld())) {
                player.sendMessage("§cTeletransporte entre mundos está desabilitado!");
                callback.accept(false);
                return;
            }
            
            // Verificar e cobrar custo se necessário
            if (warp.hasCost()) {
                processWarpPayment(player, warp, callback);
            } else {
                // Teleportar diretamente se não há custo
                performWarpTeleport(player, warp, callback);
            }
        });
    }
    
    /**
     * Processa o pagamento do warp de forma assíncrona.
     */
    private void processWarpPayment(Player player, Warp warp, Consumer<Boolean> callback) {
        try {
            // Obter EconomyManager via PrimeLeagueAPI
            br.com.primeleague.core.managers.EconomyManager economyManager = PrimeLeagueAPI.getEconomyManager();
            
            // Obter player_id via IdentityManager
            int playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
            if (playerId == -1) {
                player.sendMessage("§cErro ao obter dados do jogador! Tente novamente.");
                callback.accept(false);
                return;
            }
            
            // Verificar saldo de forma assíncrona
            economyManager.getBalanceAsync(playerId, (balance) -> {
                if (balance == null) {
                    player.sendMessage("§cErro ao verificar saldo! Tente novamente.");
                    callback.accept(false);
                    return;
                }
                
                if (balance.compareTo(warp.getCost()) < 0) {
                    player.sendMessage("§cSaldo insuficiente! Você precisa de §a$" + warp.getFormattedCost() + " §cpara usar este warp.");
                    player.sendMessage("§7Seu saldo atual: §a$" + String.format("%.2f", balance.doubleValue()));
                    callback.accept(false);
                    return;
                }
                
                // Cobrar taxa de forma assíncrona
                economyManager.debitBalanceAsync(playerId, warp.getCost().doubleValue(), "Warp: " + warp.getWarpName(), (response) -> {
                    if (response.isSuccess()) {
                        player.sendMessage("§aTaxa de §a$" + warp.getFormattedCost() + " §acobrada com sucesso!");
                        // Teleportar após pagamento bem-sucedido
                        performWarpTeleport(player, warp, callback);
                    } else {
                        player.sendMessage("§cErro ao processar pagamento: " + response.getErrorMessage());
                        callback.accept(false);
                    }
                });
            });
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao processar pagamento do warp para " + player.getName() + ": " + e.getMessage());
            player.sendMessage("§cErro ao processar pagamento! Tente novamente.");
            callback.accept(false);
        }
    }
    
    /**
     * Executa o teletransporte para o warp.
     */
    private void performWarpTeleport(Player player, Warp warp, Consumer<Boolean> callback) {
        try {
            // Criar localização
            World world = Bukkit.getWorld(warp.getWorld());
            Location location = new Location(world, warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
            
            // Teleportar
            player.teleport(location);
            
            // Atualizar estatísticas de uso de forma assíncrona
            warpDAO.updateWarpUsageAsync(warp.getWarpId(), (success) -> {
                if (success) {
                    warp.markAsUsed();
                }
            });
            
            // Aplicar cooldown
            setWarpCooldown(player);
            
            player.sendMessage("§aTeleportado para warp '" + warp.getWarpName() + "'!");
            logger.info("✅ Jogador " + player.getName() + " teleportado para warp " + warp.getWarpName());
            callback.accept(true);
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao teleportar " + player.getName() + " para warp " + warp.getWarpName() + ": " + e.getMessage());
            player.sendMessage("§cErro ao teleportar! Tente novamente.");
            callback.accept(false);
        }
    }
    
    /**
     * Remove um warp de forma assíncrona.
     * 
     * @param player Jogador que está removendo o warp
     * @param warpName Nome do warp
     * @param callback Callback executado quando a operação for concluída
     */
    public void removeWarpAsync(Player player, String warpName, Consumer<Boolean> callback) {
        if (player == null || warpName == null || warpName.trim().isEmpty()) {
            callback.accept(false);
            return;
        }
        
        // Verificar se o warp existe de forma assíncrona
        warpDAO.warpExistsAsync(warpName, (exists) -> {
            if (!exists) {
                player.sendMessage("§cWarp '" + warpName + "' não encontrado!");
                callback.accept(false);
                return;
            }
            
            // Remover do banco de dados de forma assíncrona
            warpDAO.deleteWarpAsync(warpName, (success) -> {
                if (success) {
                    // Remover do cache
                    warpCache.remove(warpName.toLowerCase());
                    
                    player.sendMessage("§aWarp '" + warpName + "' removido com sucesso!");
                    logger.info("✅ Warp removido: " + warpName + " por " + player.getName());
                } else {
                    player.sendMessage("§cErro ao remover warp! Tente novamente.");
                    logger.severe("❌ Erro ao remover warp " + warpName + " por " + player.getName());
                }
                
                callback.accept(success);
            });
        });
    }
    
    /**
     * Lista warps disponíveis para o jogador de forma assíncrona.
     * 
     * @param player Jogador
     * @param callback Callback executado quando a busca for concluída
     */
    public void listAvailableWarpsAsync(Player player, Consumer<List<Warp>> callback) {
        if (player == null) {
            callback.accept(new java.util.ArrayList<Warp>());
            return;
        }
        
        warpDAO.getAvailableWarpsAsync(player.getName(), callback);
    }
    
    // ========================================
    // MÉTODOS DE COOLDOWN
    // ========================================
    
    /**
     * Verifica se o jogador pode usar warps (não está em cooldown).
     * 
     * @param player Jogador
     * @return true se pode usar warps
     */
    public boolean canUseWarp(Player player) {
        // Bypass de cooldown para staff
        if (player.hasPermission("primeleague.warps.cooldown.bypass")) {
            return true;
        }
        
        Long lastWarp = warpCooldowns.get(player.getName());
        if (lastWarp == null) {
            return true;
        }
        
        long cooldownMillis = warpCooldownSeconds * 1000L;
        return System.currentTimeMillis() - lastWarp >= cooldownMillis;
    }
    
    /**
     * Obtém o tempo restante do cooldown em segundos.
     * 
     * @param player Jogador
     * @return Segundos restantes
     */
    public long getWarpCooldownRemaining(Player player) {
        Long lastWarp = warpCooldowns.get(player.getName());
        if (lastWarp == null) {
            return 0;
        }
        
        long cooldownMillis = warpCooldownSeconds * 1000L;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastWarp);
        return Math.max(0, remaining / 1000);
    }
    
    /**
     * Define o cooldown de warp para o jogador.
     * 
     * @param player Jogador
     */
    private void setWarpCooldown(Player player) {
        warpCooldowns.put(player.getName(), System.currentTimeMillis());
    }
    
    // ========================================
    // MÉTODOS DE VALIDAÇÃO
    // ========================================
    
    /**
     * Valida se o nome do warp é válido.
     * 
     * @param warpName Nome do warp
     * @return true se válido
     */
    private boolean isValidWarpName(String warpName) {
        if (warpName == null || warpName.trim().isEmpty()) {
            return false;
        }
        
        // Verificar tamanho
        if (warpName.length() > 32) {
            return false;
        }
        
        // Verificar caracteres permitidos (letras, números, underscore)
        return warpName.matches("^[a-zA-Z0-9_]+$");
    }
    
    // ========================================
    // MÉTODOS DE CACHE
    // ========================================
    
    /**
     * Limpa o cache de warps de um jogador.
     * 
     * @param playerName Nome do jogador
     */
    public void clearPlayerCache(String playerName) {
        warpCooldowns.remove(playerName);
    }
    
    /**
     * Limpa todo o cache.
     */
    public void clearAllCache() {
        warpCache.clear();
        warpCooldowns.clear();
        logger.info("✅ Cache de warps limpo");
    }
    
    /**
     * Obtém a instância do WarpDAO para registro no Core.
     * 
     * @return Instância do WarpDAO
     */
    public WarpDAO getWarpDAO() {
        return warpDAO;
    }
}
