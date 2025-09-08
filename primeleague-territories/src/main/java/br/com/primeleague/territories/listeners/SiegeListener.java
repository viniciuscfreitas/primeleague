package br.com.primeleague.territories.listeners;

import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.api.dto.ClanDTO;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener para mecânicas de cerco.
 * Gerencia o posicionamento do Altar da Discórdia e a contestação tática.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class SiegeListener implements Listener {
    
    private final PrimeLeagueTerritories plugin;
    private final WarManager warManager;
    
    // Controle de canalização
    private final Map<UUID, BukkitRunnable> channelingTasks = new HashMap<>();
    
    public SiegeListener(PrimeLeagueTerritories plugin) {
        this.plugin = plugin;
        this.warManager = plugin.getWarManager();
    }
    
    /**
     * Gerencia o posicionamento do Altar da Discórdia.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        
        // Verificar se é o Altar da Discórdia
        if (!isAltarItem(item)) {
            return;
        }
        
        // Cancelar colocação normal
        event.setCancelled(true);
        
        // Verificar se o jogador tem permissão
        if (!player.hasPermission("primeleague.territories.war")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar o Altar da Discórdia!");
            return;
        }
        
        // Verificar se o jogador está em um clã
        int playerId = getPlayerId(player); // Placeholder temporário
        if (getPlayerClan(playerId) == null) { // Placeholder temporário
            player.sendMessage(ChatColor.RED + "Você precisa estar em um clã para usar o Altar da Discórdia!");
            return;
        }
        
        // Iniciar canalização
        startChanneling(player, event.getBlock().getLocation());
    }
    
    /**
     * Monitora movimento durante canalização.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Verificar se está canalizando
        if (!channelingTasks.containsKey(playerId)) {
            return;
        }
        
        // Verificar se se moveu muito
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        double distance = from.distance(to);
        if (distance > 0.1) { // Permitir pequenos movimentos
            // Cancelar canalização
            cancelChanneling(player);
            player.sendMessage(ChatColor.RED + "Canalização cancelada! Você se moveu muito.");
        }
    }
    
    /**
     * Inicia o processo de canalização para posicionar o altar.
     * 
     * @param player Jogador
     * @param location Localização do altar
     */
    private void startChanneling(Player player, Location location) {
        UUID playerId = player.getUniqueId();
        
        // Cancelar canalização anterior se existir
        if (channelingTasks.containsKey(playerId)) {
            cancelChanneling(player);
        }
        
        int channelingTime = plugin.getConfig().getInt("altar.channeling-time", 5);
        
        player.sendMessage(ChatColor.YELLOW + "Iniciando canalização do Altar da Discórdia...");
        player.sendMessage(ChatColor.GRAY + "Não se mova por " + channelingTime + " segundos!");
        
        // Criar tarefa de canalização
        BukkitRunnable task = new BukkitRunnable() {
            private int timeLeft = channelingTime;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Canalização concluída
                    completeChanneling(player, location);
                    channelingTasks.remove(playerId);
                    this.cancel();
                    return;
                }
                
                // Mostrar progresso
                if (timeLeft % 2 == 0 || timeLeft <= 3) {
                    player.sendMessage(ChatColor.YELLOW + "Canalizando... " + timeLeft + "s");
                }
                
                timeLeft--;
            }
        };
        
        // Executar tarefa
        task.runTaskTimer(plugin, 0L, 20L); // A cada segundo
        channelingTasks.put(playerId, task);
    }
    
    /**
     * Cancela a canalização em andamento.
     * 
     * @param player Jogador
     */
    private void cancelChanneling(Player player) {
        UUID playerId = player.getUniqueId();
        BukkitRunnable task = channelingTasks.remove(playerId);
        
        if (task != null) {
            task.cancel();
        }
    }
    
    /**
     * Completa a canalização e inicia o cerco.
     * 
     * @param player Jogador
     * @param location Localização do altar
     */
    private void completeChanneling(Player player, Location location) {
        player.sendMessage(ChatColor.GREEN + "Canalização concluída! Iniciando cerco...");
        
        // Tentar iniciar cerco
        warManager.startSiege(player, location, (result, message) -> {
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + message);
                    // Colocar o altar no mundo
                    location.getBlock().setType(Material.BEACON);
                    break;
                case NOT_TERRITORY:
                case NO_CLAN:
                case OWN_TERRITORY:
                case NO_WAR:
                case EXPIRED_WAR:
                case SIEGE_ACTIVE:
                case DATABASE_ERROR:
                    player.sendMessage(ChatColor.RED + message);
                    // Devolver o item
                    player.getInventory().addItem(createAltarItem());
                    break;
            }
        });
    }
    
    /**
     * Verifica se um item é o Altar da Discórdia.
     * 
     * @param item Item a ser verificado
     * @return true se é o altar
     */
    private boolean isAltarItem(ItemStack item) {
        if (item == null || item.getType() != Material.BEACON) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        return meta.getDisplayName().equals(ChatColor.DARK_RED + "Altar da Discórdia");
    }
    
    /**
     * Cria um item Altar da Discórdia.
     * 
     * @return Item do altar
     */
    public static ItemStack createAltarItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ChatColor.DARK_RED + "Altar da Discórdia");
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Item especial para iniciar cercos",
            ChatColor.GRAY + "Posicione em território reivindicado",
            ChatColor.GRAY + "Requer canalização de 5 segundos",
            "",
            ChatColor.RED + "⚠ ATENÇÃO: Use com cuidado!"
        ));
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Limpa as tarefas de canalização quando o plugin é desabilitado.
     */
    public void cleanup() {
        for (BukkitRunnable task : channelingTasks.values()) {
            task.cancel();
        }
        channelingTasks.clear();
    }
    
    // Placeholders temporários para API do Core
    private int getPlayerId(Player player) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getIdentityManager().getPlayerId(player.getUniqueId()) quando API estiver disponível
        try {
            return Math.abs(player.getName().hashCode()); // ID baseado no hash do nome
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter ID do jogador " + player.getName() + ": " + e.getMessage());
            return -1; // Retorna -1 em caso de erro
        }
    }
    
    private ClanDTO getPlayerClan(int playerId) {
        // Placeholder inteligente - simula API real do Core
        // TODO: Substituir por plugin.getCore().getClanServiceRegistry().getPlayerClan(playerUUID) quando API estiver disponível
        try {
            ClanDTO mockClan = new ClanDTO();
            mockClan.setId(playerId % 10 + 1); // Simula clã baseado no ID do jogador
            mockClan.setName("Clan_" + (playerId % 10 + 1));
            mockClan.setTag("[C" + (playerId % 10 + 1) + "]");
            return mockClan;
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao obter clã do jogador " + playerId + ": " + e.getMessage());
            return null;
        }
    }
}