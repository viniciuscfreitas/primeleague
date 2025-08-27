package br.com.primeleague.p2p.managers;

import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Gerenciador de Estado de Limbo para jogadores pendentes de verificação.
 * 
 * O limbo é um estado especial onde o jogador:
 * - É teleportado para uma área segura
 * - Tem suas ações restringidas
 * - Só pode usar comandos específicos (/verify, /ajuda, /discord)
 * - Não pode se mover, quebrar blocos, interagir, etc.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public final class LimboManager implements Listener {

    private final PrimeLeagueP2P plugin;
    private final Set<UUID> playersInLimbo;
    private final Set<String> allowedCommands;
    
    // Configurações do limbo (serão carregadas do config.yml)
    private Location limboLocation;
    private boolean limboTeleportEnabled;

    public LimboManager() {
        this.plugin = PrimeLeagueP2P.getInstance();
        this.playersInLimbo = new HashSet<UUID>();
        this.allowedCommands = new HashSet<String>(Arrays.asList(
            "verify", "ajuda", "help", "discord"
        ));
        
        // Registrar este manager como listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Carregar configurações
        loadLimboConfiguration();
    }

    /**
     * Carrega as configurações do limbo do config.yml.
     */
    private void loadLimboConfiguration() {
        try {
            // Verificar se teleporte para limbo está habilitado
            limboTeleportEnabled = plugin.getConfig().getBoolean("limbo.teleport_enabled", true);
            
            if (limboTeleportEnabled) {
                // Carregar coordenadas do limbo
                String worldName = plugin.getConfig().getString("limbo.world", "world");
                double x = plugin.getConfig().getDouble("limbo.x", 0.0);
                double y = plugin.getConfig().getDouble("limbo.y", 100.0);
                double z = plugin.getConfig().getDouble("limbo.z", 0.0);
                float yaw = (float) plugin.getConfig().getDouble("limbo.yaw", 0.0);
                float pitch = (float) plugin.getConfig().getDouble("limbo.pitch", 0.0);
                
                // Tentar obter o mundo
                if (plugin.getServer().getWorld(worldName) != null) {
                    limboLocation = new Location(plugin.getServer().getWorld(worldName), x, y, z, yaw, pitch);
                    plugin.getLogger().info("Localização do limbo carregada: " + worldName + " (" + x + ", " + y + ", " + z + ")");
                } else {
                    plugin.getLogger().warning("Mundo do limbo não encontrado: " + worldName + " - Teleporte desabilitado");
                    limboTeleportEnabled = false;
                }
            }
            
            // Carregar comandos permitidos adicionais
            if (plugin.getConfig().contains("limbo.allowed_commands")) {
                for (String cmd : plugin.getConfig().getStringList("limbo.allowed_commands")) {
                    allowedCommands.add(cmd.toLowerCase());
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar configuração do limbo: " + e.getMessage());
            e.printStackTrace();
            limboTeleportEnabled = false;
        }
    }

    /**
     * Coloca um jogador em estado de limbo.
     * UX MELHORADA: Detecta se o usuário já tem contas vinculadas.
     */
    public void putPlayerInLimbo(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        if (playersInLimbo.contains(playerUuid)) {
            return; // Jogador já está em limbo
        }
        
        // Adicionar à lista de jogadores em limbo
        playersInLimbo.add(playerUuid);
        
        // Teleportar para o limbo se configurado
        if (limboTeleportEnabled && limboLocation != null) {
            player.teleport(limboLocation);
        }
        
        // Verificar se o usuário já tem contas vinculadas
        boolean hasExistingAccounts = checkIfUserHasExistingAccounts(player);
        
        // Enviar mensagens explicativas baseadas no status
        sendLimboMessages(player, hasExistingAccounts);
        
        plugin.getLogger().info("Jogador " + player.getName() + " colocado em estado de limbo (contas existentes: " + hasExistingAccounts + ")");
    }
    
    /**
     * Verifica se o usuário já possui contas vinculadas ao Discord.
     * UX MELHORADA: Detecta usuários existentes para mensagens personalizadas.
     * 
     * LÓGICA CORRIGIDA: Verifica se o Discord ID que será usado já possui outras contas.
     * Isso detecta usuários que estão adicionando uma nova conta ao Discord existente.
     */
    private boolean checkIfUserHasExistingAccounts(Player player) {
        try {
            String playerName = player.getName();
            
            // Buscar no banco de dados se já existe algum vínculo Discord para este nome de jogador
            java.sql.Connection conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn != null) {
                // Primeiro, verificar se já existe um vínculo Discord para este nome (mesmo que não verificado)
                String sql = "SELECT dl.discord_id FROM discord_links dl " +
                           "JOIN player_data pd ON dl.player_id = pd.player_id " +
                           "WHERE pd.name = ?";
                
                try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    java.sql.ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        String discordId = rs.getString("discord_id");
                        
                        if (discordId != null) {
                            // Se encontrou um Discord ID, verificar se há outras contas verificadas para este Discord ID
                            String checkOtherAccountsSql = "SELECT COUNT(*) FROM discord_links dl2 " +
                                                         "JOIN player_data pd2 ON dl2.player_id = pd2.player_id " +
                                                         "WHERE dl2.discord_id = ? AND dl2.verified = TRUE " +
                                                         "AND pd2.name != ?";
                            
                            try (java.sql.PreparedStatement stmt2 = conn.prepareStatement(checkOtherAccountsSql)) {
                                stmt2.setString(1, discordId);
                                stmt2.setString(2, playerName);
                                java.sql.ResultSet rs2 = stmt2.executeQuery();
                                
                                if (rs2.next()) {
                                    int otherAccounts = rs2.getInt(1);
                                    boolean hasOtherAccounts = otherAccounts > 0;
                                    
                                    plugin.getLogger().info("UX-DEBUG: " + playerName + " - Discord ID: " + discordId + 
                                                          " - Contas existentes: " + otherAccounts + " - Retornando: " + hasOtherAccounts);
                                    
                                    return hasOtherAccounts;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar contas existentes para " + player.getName() + ": " + e.getMessage());
        }
        
        plugin.getLogger().info("UX-DEBUG: " + player.getName() + " - Retornando false (novo usuário)");
        return false; // Em caso de erro ou não encontrado, assume que é novo usuário
    }

    /**
     * Remove um jogador do estado de limbo.
     */
    public void removePlayerFromLimbo(UUID playerUuid) {
        if (playersInLimbo.remove(playerUuid)) {
            plugin.getLogger().info("Jogador removido do estado de limbo: " + playerUuid);
        }
    }

    /**
     * Verifica se um jogador está em limbo.
     */
    public boolean isPlayerInLimbo(UUID playerUuid) {
        return playersInLimbo.contains(playerUuid);
    }

    /**
     * Verifica se um jogador está em limbo (versão com Player).
     */
    public boolean isPlayerInLimbo(Player player) {
        return isPlayerInLimbo(player.getUniqueId());
    }

    /**
     * Envia mensagens explicativas para o jogador em limbo.
     * UX MELHORADA: Mensagens personalizadas baseadas no status do usuário.
     */
    private void sendLimboMessages(Player player, boolean hasExistingAccounts) {
        if (hasExistingAccounts) {
            // Mensagem para usuário que já tem contas vinculadas
            sendExistingUserLimboMessages(player);
        } else {
            // Mensagem para novo usuário
            sendNewUserLimboMessages(player);
        }
    }
    
    /**
     * Mensagens para usuário que já possui contas vinculadas.
     * UX MELHORADA: Mensagem específica para usuários existentes.
     */
    private void sendExistingUserLimboMessages(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l🎮 BEM-VINDO DE VOLTA AO PRIME LEAGUE!");
        player.sendMessage("");
        player.sendMessage("§f📱 Detectamos que você já possui contas vinculadas!");
        player.sendMessage("§f🔗 Para conectar esta nova conta ao seu Discord:");
        player.sendMessage("   §7→ Digite §a/registrar " + player.getName() + " §7no Discord");
        player.sendMessage("   §7→ Depois use §a/verify <código> §7aqui");
        player.sendMessage("");
        player.sendMessage("§a💡 Dica: Sua assinatura será compartilhada automaticamente!");
        player.sendMessage("§e⏱️ Você tem 5 minutos para verificar");
        player.sendMessage("§b🔗 Discord: §adiscord.gg/primeleague");
        player.sendMessage("");
        player.sendMessage("§7💡 Comandos: §a/verify§7, §a/ajuda§7, §a/discord");
        player.sendMessage("");
    }
    
    /**
     * Mensagens para novo usuário (primeira vez).
     * UX MELHORADA: Mensagem específica para novos usuários.
     */
    private void sendNewUserLimboMessages(Player player) {
        player.sendMessage("");
        player.sendMessage("§6§l🎮 BEM-VINDO AO PRIME LEAGUE!");
        player.sendMessage("");
        player.sendMessage("§f📱 Para jogar, conecte sua conta Discord:");
        player.sendMessage("   §7→ Digite §a/registrar " + player.getName() + " §7no Discord");
        player.sendMessage("   §7→ Depois use §a/verify <código> §7aqui");
        player.sendMessage("");
        player.sendMessage("§e⏱️ Você tem 5 minutos para verificar");
        player.sendMessage("§b🔗 Discord: §adiscord.gg/primeleague");
        player.sendMessage("");
        player.sendMessage("§7💡 Comandos: §a/verify§7, §a/ajuda§7, §a/discord");
        player.sendMessage("");
    }

    /**
     * Envia mensagem de ação não permitida.
     */
    private void sendRestrictedActionMessage(Player player) {
        player.sendMessage("§c🚫 Ação não permitida durante a verificação!");
        player.sendMessage("§eComplete a verificação com §a/verify <código> §epara continuar.");
    }

    // ========================================================================
    // EVENT HANDLERS - Restringem ações de jogadores em limbo
    // ========================================================================

    /**
     * Impede jogadores em limbo de se moverem.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            // Permitir apenas rotação, não movimento
            Location from = event.getFrom();
            Location to = event.getTo();
            
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
                // Não enviar mensagem a cada movimento para evitar spam
            }
        }
    }

    /**
     * Impede jogadores em limbo de quebrar blocos.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictedActionMessage(event.getPlayer());
        }
    }

    /**
     * Impede jogadores em limbo de colocar blocos.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictedActionMessage(event.getPlayer());
        }
    }

    /**
     * Impede jogadores em limbo de interagir com blocos/entidades.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictedActionMessage(event.getPlayer());
        }
    }

    /**
     * Impede jogadores em limbo de dropar itens.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
            sendRestrictedActionMessage(event.getPlayer());
        }
    }

    /**
     * Impede jogadores em limbo de pegar itens.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede jogadores em limbo de tomar dano.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isPlayerInLimbo(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Impede jogadores em limbo de perder fome.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isPlayerInLimbo(player)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Impede jogadores em limbo de usar chat público.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c🚫 Chat desabilitado durante a verificação!");
            event.getPlayer().sendMessage("§eUse §a/verify <código> §epara completar a verificação.");
        }
    }

    /**
     * Filtra comandos para jogadores em limbo - permite apenas comandos específicos.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (isPlayerInLimbo(event.getPlayer())) {
            String command = event.getMessage().toLowerCase();
            
            // Remover a barra inicial e pegar apenas o comando
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            
            // Verificar se é um comando permitido
            String[] parts = command.split(" ");
            String baseCommand = parts[0];
            
            if (!allowedCommands.contains(baseCommand)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c🚫 Comando não permitido durante a verificação!");
                event.getPlayer().sendMessage("§eComandos disponíveis: §a/verify§f, §a/ajuda§f, §a/discord");
            }
        }
    }

    /**
     * Remove jogador da lista quando sair do servidor.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removePlayerFromLimbo(event.getPlayer().getUniqueId());
    }

    /**
     * Obtém estatísticas do limbo para monitoramento.
     */
    public int getPlayersInLimboCount() {
        return playersInLimbo.size();
    }

    /**
     * Limpa a lista de jogadores em limbo (útil para recarregar configurações).
     */
    public void clearLimboList() {
        playersInLimbo.clear();
        plugin.getLogger().info("Lista de jogadores em limbo limpa");
    }
}
