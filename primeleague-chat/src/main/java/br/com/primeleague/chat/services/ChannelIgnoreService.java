package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço para gerenciar o sistema de ignore de canais e jogadores.
 * Permite que jogadores silenciem canais específicos e jogadores específicos.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class ChannelIgnoreService {
    
    private final PrimeLeagueChat plugin;
    
    // Cache de canais ignorados por jogador (thread-safe)
    private final Map<UUID, Set<ChannelManager.ChatChannel>> ignoredChannels = new ConcurrentHashMap<>();
    
    // Cache de jogadores ignorados por jogador (thread-safe)
    private final Map<UUID, Set<UUID>> ignoredPlayers = new ConcurrentHashMap<>();
    
    public ChannelIgnoreService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("🔇 Channel & Player Ignore Service inicializado");
    }
    
    // ==================== MÉTODOS PARA CANAIS ====================
    
    /**
     * Verifica se um jogador está ignorando um canal específico.
     * 
     * @param player Jogador para verificar
     * @param channel Canal a verificar
     * @return true se o jogador está ignorando o canal
     */
    public boolean isIgnoringChannel(Player player, ChannelManager.ChatChannel channel) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<ChannelManager.ChatChannel> ignored = ignoredChannels.get(canonicalUuid);
        return ignored != null && ignored.contains(channel);
    }
    
    /**
     * Adiciona um canal à lista de ignorados de um jogador.
     * 
     * @param player Jogador
     * @param channel Canal a ignorar
     * @return true se foi adicionado com sucesso
     */
    public boolean ignoreChannel(Player player, ChannelManager.ChatChannel channel) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<ChannelManager.ChatChannel> ignored = ignoredChannels.get(canonicalUuid);
        if (ignored == null) {
            ignored = Collections.newSetFromMap(new ConcurrentHashMap<ChannelManager.ChatChannel, Boolean>());
            ignoredChannels.put(canonicalUuid, ignored);
        }
        
        boolean added = ignored.add(channel);
        if (added) {
            plugin.getLogger().info("🔇 [IGNORE-CHANNEL] Jogador " + player.getName() + " ignorando canal: " + channel.name());
        }
        
        return added;
    }
    
    /**
     * Remove um canal da lista de ignorados de um jogador.
     * 
     * @param player Jogador
     * @param channel Canal a parar de ignorar
     * @return true se foi removido com sucesso
     */
    public boolean unignoreChannel(Player player, ChannelManager.ChatChannel channel) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<ChannelManager.ChatChannel> ignored = ignoredChannels.get(canonicalUuid);
        if (ignored != null) {
            boolean removed = ignored.remove(channel);
            if (removed) {
                plugin.getLogger().info("🔊 [UNIGNORE-CHANNEL] Jogador " + player.getName() + " parou de ignorar canal: " + channel.name());
                
                // Limpar entrada se não há mais canais ignorados
                if (ignored.isEmpty()) {
                    ignoredChannels.remove(canonicalUuid);
                }
            }
            return removed;
        }
        
        return false;
    }
    
    /**
     * Obtém a lista de canais ignorados por um jogador.
     * 
     * @param player Jogador
     * @return Set de canais ignorados (cópia defensiva)
     */
    public Set<ChannelManager.ChatChannel> getIgnoredChannels(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<ChannelManager.ChatChannel> ignored = ignoredChannels.get(canonicalUuid);
        if (ignored != null) {
            return new HashSet<ChannelManager.ChatChannel>(ignored);
        }
        return new HashSet<ChannelManager.ChatChannel>();
    }
    
    // ==================== MÉTODOS PARA JOGADORES ====================
    
    /**
     * Verifica se um jogador está ignorando outro jogador específico.
     * 
     * @param player Jogador que pode estar ignorando
     * @param targetPlayer Jogador que pode estar sendo ignorado
     * @return true se o jogador está ignorando o alvo
     */
    public boolean isIgnoringPlayer(Player player, Player targetPlayer) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        UUID targetBukkitUuid = targetPlayer.getUniqueId();
        UUID targetCanonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(targetBukkitUuid);
        
        Set<UUID> ignored = ignoredPlayers.get(canonicalUuid);
        return ignored != null && ignored.contains(targetCanonicalUuid);
    }
    
    /**
     * Adiciona um jogador à lista de ignorados de outro jogador.
     * 
     * @param player Jogador que vai ignorar
     * @param targetPlayer Jogador a ser ignorado
     * @return true se foi adicionado com sucesso
     */
    public boolean ignorePlayer(Player player, Player targetPlayer) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        UUID targetBukkitUuid = targetPlayer.getUniqueId();
        UUID targetCanonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(targetBukkitUuid);
        
        // Não permitir ignorar a si mesmo
        if (canonicalUuid.equals(targetCanonicalUuid)) {
            return false;
        }
        
        Set<UUID> ignored = ignoredPlayers.get(canonicalUuid);
        if (ignored == null) {
            ignored = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
            ignoredPlayers.put(canonicalUuid, ignored);
        }
        
        boolean added = ignored.add(targetCanonicalUuid);
        if (added) {
            plugin.getLogger().info("🔇 [IGNORE-PLAYER] Jogador " + player.getName() + " ignorando jogador: " + targetPlayer.getName());
        }
        
        return added;
    }
    
    /**
     * Remove um jogador da lista de ignorados de outro jogador.
     * 
     * @param player Jogador que vai parar de ignorar
     * @param targetPlayer Jogador a parar de ignorar
     * @return true se foi removido com sucesso
     */
    public boolean unignorePlayer(Player player, Player targetPlayer) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        UUID targetBukkitUuid = targetPlayer.getUniqueId();
        UUID targetCanonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(targetBukkitUuid);
        
        Set<UUID> ignored = ignoredPlayers.get(canonicalUuid);
        if (ignored != null) {
            boolean removed = ignored.remove(targetCanonicalUuid);
            if (removed) {
                plugin.getLogger().info("🔊 [UNIGNORE-PLAYER] Jogador " + player.getName() + " parou de ignorar jogador: " + targetPlayer.getName());
                
                // Limpar entrada se não há mais jogadores ignorados
                if (ignored.isEmpty()) {
                    ignoredPlayers.remove(canonicalUuid);
                }
            }
            return removed;
        }
        
        return false;
    }
    
    /**
     * Obtém a lista de jogadores ignorados por um jogador.
     * 
     * @param player Jogador
     * @return Set de UUIDs de jogadores ignorados (cópia defensiva)
     */
    public Set<UUID> getIgnoredPlayers(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<UUID> ignored = ignoredPlayers.get(canonicalUuid);
        if (ignored != null) {
            return new HashSet<UUID>(ignored);
        }
        return new HashSet<UUID>();
    }
    
    // ==================== MÉTODOS COMUNS ====================
    
    /**
     * Remove todos os canais ignorados de um jogador.
     * 
     * @param player Jogador
     * @return Número de canais removidos
     */
    public int clearIgnoredChannels(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<ChannelManager.ChatChannel> ignored = ignoredChannels.remove(canonicalUuid);
        if (ignored != null) {
            int count = ignored.size();
            plugin.getLogger().info("🔊 [CLEAR-IGNORE-CHANNELS] Jogador " + player.getName() + " removeu " + count + " canais ignorados");
            return count;
        }
        
        return 0;
    }
    
    /**
     * Remove todos os jogadores ignorados de um jogador.
     * 
     * @param player Jogador
     * @return Número de jogadores removidos
     */
    public int clearIgnoredPlayers(Player player) {
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        Set<UUID> ignored = ignoredPlayers.remove(canonicalUuid);
        if (ignored != null) {
            int count = ignored.size();
            plugin.getLogger().info("🔊 [CLEAR-IGNORE-PLAYERS] Jogador " + player.getName() + " removeu " + count + " jogadores ignorados");
            return count;
        }
        
        return 0;
    }
    
    /**
     * Remove todos os ignores (canais e jogadores) de um jogador.
     * 
     * @param player Jogador
     * @return Número total de ignores removidos
     */
    public int clearAllIgnores(Player player) {
        int channelsCleared = clearIgnoredChannels(player);
        int playersCleared = clearIgnoredPlayers(player);
        return channelsCleared + playersCleared;
    }
    
    /**
     * Filtra uma lista de jogadores removendo aqueles que estão ignorando o canal.
     * 
     * @param players Lista original de jogadores
     * @param channel Canal sendo enviado
     * @return Lista filtrada de jogadores
     */
    public List<Player> filterIgnoringPlayers(List<Player> players, ChannelManager.ChatChannel channel) {
        List<Player> filteredPlayers = new ArrayList<Player>();
        
        for (Player player : players) {
            if (!isIgnoringChannel(player, channel)) {
                filteredPlayers.add(player);
            }
        }
        
        return filteredPlayers;
    }
    
    /**
     * Filtra uma lista de jogadores removendo aqueles que estão ignorando o remetente.
     * 
     * @param players Lista original de jogadores
     * @param sender Jogador que está enviando a mensagem
     * @return Lista filtrada de jogadores
     */
    public List<Player> filterIgnoringSender(List<Player> players, Player sender) {
        List<Player> filteredPlayers = new ArrayList<Player>();
        
        for (Player player : players) {
            if (!isIgnoringPlayer(player, sender)) {
                filteredPlayers.add(player);
            }
        }
        
        return filteredPlayers;
    }
    
    /**
     * Filtra uma lista de jogadores removendo aqueles que estão ignorando o canal OU o remetente.
     * 
     * @param players Lista original de jogadores
     * @param channel Canal sendo enviado
     * @param sender Jogador que está enviando a mensagem
     * @return Lista filtrada de jogadores
     */
    public List<Player> filterIgnoringChannelAndSender(List<Player> players, ChannelManager.ChatChannel channel, Player sender) {
        List<Player> filteredPlayers = new ArrayList<Player>();
        
        for (Player player : players) {
            if (!isIgnoringChannel(player, channel) && !isIgnoringPlayer(player, sender)) {
                filteredPlayers.add(player);
            }
        }
        
        return filteredPlayers;
    }
    
    /**
     * Limpa o histórico de ignore de um jogador quando ele sai do servidor.
     * 
     * @param playerUuid UUID do jogador
     */
    public void clearPlayerHistory(UUID playerUuid) {
        // Converter UUID do Bukkit para UUID canônico se necessário
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(playerUuid);
        ignoredChannels.remove(canonicalUuid);
        ignoredPlayers.remove(canonicalUuid);
    }
    
    /**
     * Obtém estatísticas do serviço para debugging.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        int totalPlayersWithChannelIgnores = ignoredChannels.size();
        int totalPlayersWithPlayerIgnores = ignoredPlayers.size();
        
        int totalChannelIgnores = 0;
        for (Set<ChannelManager.ChatChannel> ignored : ignoredChannels.values()) {
            totalChannelIgnores += ignored.size();
        }
        
        int totalPlayerIgnores = 0;
        for (Set<UUID> ignored : ignoredPlayers.values()) {
            totalPlayerIgnores += ignored.size();
        }
        
        return "Ignore Service Stats: " + totalPlayersWithChannelIgnores + " jogadores com ignores de canal (" + totalChannelIgnores + " total), " + 
               totalPlayersWithPlayerIgnores + " jogadores com ignores de jogador (" + totalPlayerIgnores + " total)";
    }
}
