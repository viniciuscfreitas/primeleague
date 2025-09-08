package br.com.primeleague.territories.listeners;

import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.model.TerritoryChunk;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Listener para proteção de territórios.
 * Impede que jogadores não autorizados interajam com blocos em territórios reivindicados.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class TerritoryProtectionListener implements Listener {
    
    private final PrimeLeagueTerritories plugin;
    private final TerritoryManager territoryManager;
    private final WarManager warManager;
    
    public TerritoryProtectionListener(PrimeLeagueTerritories plugin) {
        this.plugin = plugin;
        this.territoryManager = plugin.getTerritoryManager();
        this.warManager = plugin.getWarManager();
    }
    
    /**
     * Impede quebra de blocos em territórios protegidos.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Verificar se o jogador tem permissão de admin
        if (player.hasPermission("primeleague.territories.admin")) {
            return;
        }
        
        // Verificar se está em território reivindicado
        TerritoryChunk territory = territoryManager.getTerritoryAt(block.getLocation());
        if (territory == null) {
            return; // Território neutro, permitir
        }
        
        // Verificar se o jogador tem permissão
        if (territoryManager.hasTerritoryPermission(player, block.getLocation())) {
            return; // Jogador tem permissão
        }
        
        // Verificar se está em zona de guerra
        if (warManager.isWarzone(block.getLocation())) {
            return; // Permitir em zona de guerra
        }
        
        // Bloquear ação
        event.setCancelled(true);
        
        // Obter informações do clã proprietário
        ClanDTO owner = territoryManager.getOwningClan(block.getLocation());
        if (owner != null) {
            player.sendMessage(ChatColor.RED + "Este território pertence ao clã " + owner.getTag() + "!");
            player.sendMessage(ChatColor.GRAY + "Você não pode quebrar blocos aqui.");
        }
    }
    
    /**
     * Impede colocação de blocos em territórios protegidos.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        
        // Verificar se o jogador tem permissão de admin
        if (player.hasPermission("primeleague.territories.admin")) {
            return;
        }
        
        // Verificar se está em território reivindicado
        TerritoryChunk territory = territoryManager.getTerritoryAt(block.getLocation());
        if (territory == null) {
            return; // Território neutro, permitir
        }
        
        // Verificar se o jogador tem permissão
        if (territoryManager.hasTerritoryPermission(player, block.getLocation())) {
            return; // Jogador tem permissão
        }
        
        // Verificar se está em zona de guerra
        if (warManager.isWarzone(block.getLocation())) {
            return; // Permitir em zona de guerra
        }
        
        // Bloquear ação
        event.setCancelled(true);
        
        // Obter informações do clã proprietário
        ClanDTO owner = territoryManager.getOwningClan(block.getLocation());
        if (owner != null) {
            player.sendMessage(ChatColor.RED + "Este território pertence ao clã " + owner.getTag() + "!");
            player.sendMessage(ChatColor.GRAY + "Você não pode colocar blocos aqui.");
        }
    }
    
    /**
     * Impede interação com blocos em territórios protegidos.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        
        if (block == null) {
            return;
        }
        
        // Verificar se o jogador tem permissão de admin
        if (player.hasPermission("primeleague.territories.admin")) {
            return;
        }
        
        // Verificar se está em território reivindicado
        TerritoryChunk territory = territoryManager.getTerritoryAt(block.getLocation());
        if (territory == null) {
            return; // Território neutro, permitir
        }
        
        // Verificar se o jogador tem permissão
        if (territoryManager.hasTerritoryPermission(player, block.getLocation())) {
            return; // Jogador tem permissão
        }
        
        // Verificar se está em zona de guerra
        if (warManager.isWarzone(block.getLocation())) {
            return; // Permitir em zona de guerra
        }
        
        // Verificar se é um contêiner ou bloco interativo
        if (isInteractiveBlock(block.getType())) {
            // Bloquear interação
            event.setCancelled(true);
            
            // Obter informações do clã proprietário
            ClanDTO owner = territoryManager.getOwningClan(block.getLocation());
            if (owner != null) {
                player.sendMessage(ChatColor.RED + "Este território pertence ao clã " + owner.getTag() + "!");
                player.sendMessage(ChatColor.GRAY + "Você não pode interagir com este bloco.");
            }
        }
    }
    
    /**
     * Verifica se um tipo de bloco é interativo (contêiner, etc.).
     * 
     * @param material Tipo do bloco
     * @return true se é interativo
     */
    private boolean isInteractiveBlock(Material material) {
        return material == Material.CHEST ||
               material == Material.TRAPPED_CHEST ||
               material == Material.ENDER_CHEST ||
               material == Material.DISPENSER ||
               material == Material.DROPPER ||
               material == Material.HOPPER ||
               material == Material.FURNACE ||
               material == Material.BURNING_FURNACE ||
               material == Material.BREWING_STAND ||
               material == Material.ENCHANTMENT_TABLE ||
               material == Material.ANVIL ||
               material == Material.BEACON ||
               material == Material.DRAGON_EGG ||
               material == Material.LEVER ||
               material == Material.STONE_BUTTON ||
               material == Material.WOOD_BUTTON ||
               material == Material.STONE_PLATE ||
               material == Material.WOOD_PLATE ||
               material == Material.IRON_PLATE ||
               material == Material.GOLD_PLATE ||
               material == Material.WOODEN_DOOR ||
               material == Material.IRON_DOOR ||
               material == Material.TRAP_DOOR ||
               material == Material.FENCE_GATE;
    }
    
    /**
     * Verifica se um jogador tem permissão para interagir com um território.
     * 
     * @param player Jogador
     * @param territory Território
     * @return true se tem permissão
     */
    private boolean hasTerritoryPermission(Player player, TerritoryChunk territory) {
        // Verificar se o jogador está no clã proprietário
        int playerId = plugin.getCore().getIdentityManager().getPlayerId(player);
        ClanDTO playerClan = getPlayerClan(playerId); // Placeholder temporário
        
        if (playerClan == null) {
            return false; // Jogador não está em clã
        }
        
        return playerClan.getId() == territory.getClanId();
    }
    
    // Placeholder inteligente para API do Core
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