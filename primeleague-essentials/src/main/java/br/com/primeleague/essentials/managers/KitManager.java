package br.com.primeleague.essentials.managers;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.api.dao.KitDAO;
import br.com.primeleague.essentials.dao.MySqlKitDAO;
import br.com.primeleague.essentials.models.Kit;
import br.com.primeleague.api.models.KitCooldown;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.combatlog.CombatLogPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Gerenciador do sistema de kits.
 * Orquestrador assíncrono responsável por gerenciar kits e cooldowns.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class KitManager {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    private final KitDAO kitDAO;
    
    // Cache de kits carregados do YAML
    private final Map<String, Kit> loadedKits = new ConcurrentHashMap<String, Kit>();
    
    // Cache de cooldowns em memória (UUID -> Map<kitName, KitCooldown>)
    private final Map<UUID, Map<String, KitCooldown>> cooldownCache = new ConcurrentHashMap<UUID, Map<String, KitCooldown>>();
    
    // Configurações globais
    private boolean allowDuringCombat = false;
    private long globalCooldown = 30;
    private int maxItemsPerKit = 36;
    
    /**
     * Construtor do KitManager.
     * 
     * @param plugin Instância do plugin principal
     * @param kitDAO Instância do DAO (fornecida via injeção de dependência)
     */
    public KitManager(EssentialsPlugin plugin, KitDAO kitDAO) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.kitDAO = kitDAO;
        
        // Carregar configurações e kits
        loadConfiguration();
        loadKitsFromYaml();
        
        // Inicializar sistema de limpeza automática
        initializeCleanupTask();
        
        logger.info("✅ KitManager inicializado com sucesso!");
    }
    
    /**
     * Usa um kit para um jogador de forma assíncrona.
     * 
     * @param player Jogador que vai usar o kit
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    public void useKitAsync(Player player, String kitName, Consumer<Boolean> callback) {
        // Validações rápidas na thread principal
        if (!canUseKit(player, kitName)) {
            callback.accept(false);
            return;
        }
        
        Kit kit = loadedKits.get(kitName.toLowerCase());
        if (kit == null) {
            player.sendMessage("§cKit §e" + kitName + " §cnão encontrado!");
            callback.accept(false);
            return;
        }
        
        // Verificar permissão
        if (!player.hasPermission(kit.getPermission())) {
            player.sendMessage("§cVocê não tem permissão para usar o kit §e" + kit.getDisplayName() + "§c!");
            callback.accept(false);
            return;
        }
        
        // Verificar se está em combate
        if (!kit.isAllowDuringCombat() && isPlayerInCombat(player)) {
            player.sendMessage("§cNão é possível usar kits durante o combate!");
            callback.accept(false);
            return;
        }
        
        // Verificar inventário vazio se necessário
        if (kit.isRequireEmptyInventory() && !isInventoryEmpty(player)) {
            player.sendMessage("§cVocê deve ter o inventário vazio para usar este kit!");
            callback.accept(false);
            return;
        }
        
        // Verificar cooldown
        checkCooldownAndUseKit(player, kit, callback);
    }
    
    /**
     * Lista os kits disponíveis para um jogador de forma assíncrona.
     * 
     * @param player Jogador
     * @param callback Callback executado quando a operação for concluída
     */
    public void listAvailableKitsAsync(Player player, Consumer<List<Kit>> callback) {
        List<Kit> availableKits = new ArrayList<Kit>();
        
        for (Kit kit : loadedKits.values()) {
            if (player.hasPermission(kit.getPermission())) {
                availableKits.add(kit);
            }
        }
        
        callback.accept(availableKits);
    }
    
    /**
     * Verifica se um jogador pode usar um kit.
     */
    private boolean canUseKit(Player player, String kitName) {
        if (player == null || kitName == null || kitName.isEmpty()) {
            return false;
        }
        
        Kit kit = loadedKits.get(kitName.toLowerCase());
        if (kit == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica cooldown e usa o kit se possível.
     */
    private void checkCooldownAndUseKit(Player player, Kit kit, Consumer<Boolean> callback) {
        // Obter ID do jogador
        kitDAO.getPlayerIdAsync(player.getUniqueId().toString(), (playerId) -> {
            if (playerId == null) {
                player.sendMessage("§cErro interno: não foi possível identificar o jogador!");
                callback.accept(false);
                return;
            }
            
            // Verificar cooldown
            kitDAO.getKitCooldownAsync(playerId, kit.getName(), (cooldown) -> {
                if (cooldown != null && !cooldown.canUse(kit.getCooldownSeconds())) {
                    String remainingTime = cooldown.getFormattedRemainingTime(kit.getCooldownSeconds());
                    player.sendMessage("§cAguarde §e" + remainingTime + " §cantes de usar este kit novamente!");
                    callback.accept(false);
                    return;
                }
                
                // Verificar limite de usos
                if (kit.getMaxUses() > 0 && cooldown != null && cooldown.getUsesCount() >= kit.getMaxUses()) {
                    player.sendMessage("§cVocê atingiu o limite máximo de usos deste kit!");
                    callback.accept(false);
                    return;
                }
                
                // Usar o kit
                giveKitToPlayer(player, kit);
                
                // Atualizar cooldown
                updateKitCooldown(playerId, kit, cooldown);
                
                player.sendMessage("§aVocê recebeu o kit §e" + kit.getDisplayName() + "§a!");
                logger.info("✅ Kit usado: " + player.getName() + " usou " + kit.getName());
                
                callback.accept(true);
            });
        });
    }
    
    /**
     * Dá os itens do kit para o jogador.
     */
    private void giveKitToPlayer(Player player, Kit kit) {
        List<ItemStack> items = kit.generateItems();
        
        for (ItemStack item : items) {
            if (item != null) {
                // Verificar se há espaço no inventário
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventário cheio, dropar item no chão
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                } else {
                    // Adicionar ao inventário
                    player.getInventory().addItem(item);
                }
            }
        }
    }
    
    /**
     * Atualiza o cooldown do kit.
     */
    private void updateKitCooldown(int playerId, Kit kit, KitCooldown existingCooldown) {
        KitCooldown cooldown;
        
        if (existingCooldown != null) {
            cooldown = existingCooldown;
            cooldown.incrementUses();
        } else {
            cooldown = new KitCooldown(playerId, kit.getName());
            cooldown.incrementUses();
        }
        
        // Salvar no banco de dados
        kitDAO.saveKitCooldownAsync(cooldown, (success) -> {
            if (success) {
                logger.fine("✅ Cooldown do kit atualizado: " + kit.getName());
            } else {
                logger.warning("⚠️ Falha ao atualizar cooldown do kit: " + kit.getName());
            }
        });
    }
    
    /**
     * Verifica se o jogador está em combate.
     */
    private boolean isPlayerInCombat(Player player) {
        if (player.hasPermission("primeleague.kits.bypass.combat")) {
            return false;
        }
        
        CombatLogPlugin combatLogPlugin = CombatLogPlugin.getInstance();
        if (combatLogPlugin != null) {
            return combatLogPlugin.getCombatLogManager().isPlayerTagged(player.getUniqueId());
        }
        
        return false;
    }
    
    /**
     * Verifica se o inventário do jogador está vazio.
     */
    private boolean isInventoryEmpty(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Carrega as configurações do sistema.
     */
    private void loadConfiguration() {
        // Carregar configurações do config.yml
        allowDuringCombat = plugin.getConfig().getBoolean("kits.global.allow-during-combat", false);
        globalCooldown = plugin.getConfig().getLong("kits.global.global-cooldown", 30);
        maxItemsPerKit = plugin.getConfig().getInt("kits.global.max-items-per-kit", 36);
        
        logger.info("✅ Configurações de kits carregadas: combat=" + allowDuringCombat + 
                   ", globalCooldown=" + globalCooldown + "s, maxItems=" + maxItemsPerKit);
    }
    
    /**
     * Carrega os kits do arquivo YAML de forma dinâmica.
     */
    private void loadKitsFromYaml() {
        loadedKits.clear();
        
        try {
            // Verificar se a seção 'kits' existe no config
            if (!plugin.getConfig().contains("kits")) {
                logger.warning("⚠️ Seção 'kits' não encontrada no config.yml!");
                return;
            }
            
            // Obter a seção de kits
            org.bukkit.configuration.ConfigurationSection kitsSection = plugin.getConfig().getConfigurationSection("kits");
            if (kitsSection == null) {
                logger.warning("⚠️ Seção de kits está vazia!");
                return;
            }
            
            // Iterar sobre cada kit definido
            for (String kitName : kitsSection.getKeys(false)) {
                try {
                    Kit kit = loadKitFromConfig(kitName, kitsSection.getConfigurationSection(kitName));
                    if (kit != null) {
                        loadedKits.put(kitName.toLowerCase(), kit);
                        logger.info("✅ Kit carregado: " + kit.getDisplayName() + " (" + kit.getItemCount() + " itens)");
                    }
                } catch (Exception e) {
                    logger.severe("❌ Erro ao carregar kit '" + kitName + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            logger.info("✅ " + loadedKits.size() + " kits carregados dinamicamente do YAML");
            
        } catch (Exception e) {
            logger.severe("❌ Erro crítico ao carregar kits do YAML: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega um kit específico da configuração.
     */
    private Kit loadKitFromConfig(String kitName, org.bukkit.configuration.ConfigurationSection kitSection) {
        if (kitSection == null) {
            logger.warning("⚠️ Seção do kit '" + kitName + "' não encontrada!");
            return null;
        }
        
        // Criar instância do kit
        Kit kit = new Kit(kitName);
        
        // Carregar propriedades básicas
        kit.setDisplayName(kitSection.getString("name", kitName));
        kit.setDescription(kitSection.getString("description", "Kit sem descrição"));
        kit.setPermission(kitSection.getString("permission", "primeleague.kits." + kitName));
        kit.setCooldownSeconds(kitSection.getLong("cooldown", 3600));
        kit.setMaxUses(kitSection.getInt("max-uses", -1));
        kit.setRequireEmptyInventory(kitSection.getBoolean("require-empty-inventory", false));
        kit.setAllowDuringCombat(kitSection.getBoolean("allow-during-combat", false));
        
        // Carregar itens do kit
        if (kitSection.contains("items")) {
            List<?> itemsList = kitSection.getList("items");
            loadKitItems(kit, itemsList);
        }
        
        return kit;
    }
    
    /**
     * Carrega os itens de um kit da configuração.
     */
    private void loadKitItems(Kit kit, List<?> itemsList) {
        if (itemsList == null || itemsList.isEmpty()) {
            logger.warning("⚠️ Kit '" + kit.getName() + "' não possui itens!");
            return;
        }
        
        for (Object itemObj : itemsList) {
            if (!(itemObj instanceof java.util.Map)) {
                logger.warning("⚠️ Item inválido no kit '" + kit.getName() + "': " + itemObj);
                continue;
            }
            
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> itemMap = (java.util.Map<String, Object>) itemObj;
            
            try {
                Kit.KitItem kitItem = createKitItemFromMap(itemMap);
                if (kitItem != null) {
                    kit.addItem(kitItem);
                }
            } catch (Exception e) {
                logger.warning("⚠️ Erro ao processar item no kit '" + kit.getName() + "': " + e.getMessage());
            }
        }
    }
    
    /**
     * Cria um KitItem a partir de um mapa de configuração.
     */
    private Kit.KitItem createKitItemFromMap(java.util.Map<String, Object> itemMap) {
        // Obter material e quantidade
        String materialStr = (String) itemMap.get("item");
        if (materialStr == null) {
            logger.warning("⚠️ Item sem material especificado!");
            return null;
        }
        
        int amount = (Integer) itemMap.getOrDefault("amount", 1);
        
        // Criar KitItem
        Kit.KitItem kitItem = new Kit.KitItem(materialStr, amount);
        
        // Definir nome personalizado
        String name = (String) itemMap.get("name");
        if (name != null && !name.isEmpty()) {
            kitItem.setName(name);
        }
        
        // Definir lore
        Object loreObj = itemMap.get("lore");
        if (loreObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> loreList = (List<String>) loreObj;
            kitItem.setLore(loreList);
        }
        
        // Definir encantamentos
        Object enchantmentsObj = itemMap.get("enchantments");
        if (enchantmentsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> enchantmentsList = (List<String>) enchantmentsObj;
            for (String enchantStr : enchantmentsList) {
                parseAndAddEnchantment(kitItem, enchantStr);
            }
        }
        
        return kitItem;
    }
    
    /**
     * Parseia e adiciona um encantamento ao KitItem.
     */
    private void parseAndAddEnchantment(Kit.KitItem kitItem, String enchantStr) {
        try {
            // Formato esperado: "DAMAGE_ALL:1"
            String[] parts = enchantStr.split(":");
            if (parts.length != 2) {
                logger.warning("⚠️ Formato de encantamento inválido: " + enchantStr);
                return;
            }
            
            String enchantName = parts[0].trim();
            int level = Integer.parseInt(parts[1].trim());
            
            // Verificar se o encantamento existe
            org.bukkit.enchantments.Enchantment enchant = org.bukkit.enchantments.Enchantment.getByName(enchantName);
            if (enchant == null) {
                logger.warning("⚠️ Encantamento não encontrado: " + enchantName);
                return;
            }
            
            kitItem.getEnchantments().put(enchantName, level);
            
        } catch (NumberFormatException e) {
            logger.warning("⚠️ Nível de encantamento inválido: " + enchantStr);
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao processar encantamento '" + enchantStr + "': " + e.getMessage());
        }
    }
    
    /**
     * Inicializa a task de limpeza automática.
     */
    private void initializeCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanExpiredCooldowns();
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Executa a cada minuto
    }
    
    /**
     * Remove cooldowns expirados do cache e banco de dados.
     */
    private void cleanExpiredCooldowns() {
        kitDAO.cleanExpiredCooldownsAsync((cleanedCount) -> {
            if (cleanedCount > 0) {
                logger.info("🧹 " + cleanedCount + " cooldowns de kits expirados removidos");
            }
        });
    }
    
    /**
     * Limpa o cache quando um jogador sai do servidor.
     */
    public void onPlayerQuit(Player player) {
        cooldownCache.remove(player.getUniqueId());
    }
    
    /**
     * Obtém um kit pelo nome.
     */
    public Kit getKit(String kitName) {
        return loadedKits.get(kitName.toLowerCase());
    }
    
    /**
     * Obtém todos os kits carregados.
     */
    public Map<String, Kit> getAllKits() {
        return new HashMap<String, Kit>(loadedKits);
    }
    
    /**
     * Recarrega os kits do YAML.
     */
    public void reloadKits() {
        loadedKits.clear();
        loadKitsFromYaml();
        logger.info("✅ Kits recarregados com sucesso!");
    }
    
    /**
     * Obtém a instância do KitDAO para registro no Core.
     * 
     * @return Instância do KitDAO
     */
    public KitDAO getKitDAO() {
        return kitDAO;
    }
}
