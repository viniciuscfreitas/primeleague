package br.com.primeleague.essentials.managers;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.models.PlayerLastSeen;
import br.com.primeleague.essentials.models.PlayerDossier;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Gerenciador de informações de jogadores.
 * Orquestrador assíncrono responsável por consultar informações de jogadores
 * de múltiplos managers e formatar dados para comandos de informação.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PlayerInfoManager {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    
    /**
     * Construtor do PlayerInfoManager.
     * 
     * @param plugin Instância do plugin principal
     */
    public PlayerInfoManager(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        logger.info("✅ PlayerInfoManager inicializado com sucesso!");
    }
    
    /**
     * Obtém informações de última vez visto de um jogador de forma assíncrona.
     * 
     * @param playerName Nome do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    public void getPlayerLastSeenAsync(String playerName, Consumer<PlayerLastSeen> callback) {
        // Verificar se o jogador está online primeiro
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            PlayerLastSeen lastSeen = new PlayerLastSeen(
                onlinePlayer.getName(),
                onlinePlayer.getUniqueId().toString(),
                true
            );
            callback.accept(lastSeen);
            return;
        }
        
        // Se não está online, consultar banco de dados
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final PlayerLastSeen[] lastSeen = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT uuid, last_seen FROM player_data WHERE player_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String uuid = rs.getString("uuid");
                            Timestamp lastSeenTimestamp = rs.getTimestamp("last_seen");
                            
                            lastSeen[0] = new PlayerLastSeen(
                                playerName,
                                uuid,
                                lastSeenTimestamp,
                                false
                            );
                        }
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(lastSeen[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao consultar última vez visto do jogador " + playerName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    /**
     * Formata o tempo de última vez visto de forma amigável.
     * 
     * @param lastSeen Timestamp da última vez visto
     * @param callback Callback executado quando a formatação for concluída
     */
    public void formatLastSeenTime(Timestamp lastSeen, Consumer<String> callback) {
        if (lastSeen == null) {
            callback.accept("nunca foi visto");
            return;
        }
        
        long timeDiff = System.currentTimeMillis() - lastSeen.getTime();
        String formattedTime = formatTimeDifference(timeDiff);
        callback.accept(formattedTime);
    }
    
    /**
     * Formata a diferença de tempo em formato amigável.
     */
    private String formatTimeDifference(long timeDiff) {
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;
        long months = days / 30;
        long years = days / 365;
        
        if (years > 0) {
            return "há " + years + " ano" + (years > 1 ? "s" : "");
        } else if (months > 0) {
            return "há " + months + " mês" + (months > 1 ? "es" : "");
        } else if (weeks > 0) {
            return "há " + weeks + " semana" + (weeks > 1 ? "s" : "");
        } else if (days > 0) {
            return "há " + days + " dia" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return "há " + hours + " hora" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return "há " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        } else {
            return "há " + seconds + " segundo" + (seconds > 1 ? "s" : "");
        }
    }
    
    /**
     * Verifica se um jogador existe no banco de dados.
     * 
     * @param playerName Nome do jogador
     * @param callback Callback executado quando a verificação for concluída
     */
    public void playerExistsAsync(String playerName, Consumer<Boolean> callback) {
        // Verificar se está online primeiro
        if (Bukkit.getPlayer(playerName) != null) {
            callback.accept(true);
            return;
        }
        
        // Verificar no banco de dados
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final boolean[] exists = {false};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT 1 FROM player_data WHERE player_name = ? LIMIT 1";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        exists[0] = rs.next();
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(exists[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao verificar existência do jogador " + playerName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }
    
    /**
     * Obtém o UUID de um jogador pelo nome.
     * 
     * @param playerName Nome do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    public void getPlayerUuidAsync(String playerName, Consumer<String> callback) {
        // Verificar se está online primeiro
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            callback.accept(onlinePlayer.getUniqueId().toString());
            return;
        }
        
        // Consultar banco de dados
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final String[] uuid = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT uuid FROM player_data WHERE player_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            uuid[0] = rs.getString("uuid");
                        }
                    }
                }
                
                // Executar callback na thread principal
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(uuid[0]));
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao obter UUID do jogador " + playerName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    /**
     * Obtém dossiê completo de um jogador de forma assíncrona.
     * 
     * @param playerName Nome do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    public void getPlayerDossierAsync(String playerName, Consumer<PlayerDossier> callback) {
        // Verificar se o jogador está online primeiro
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            // Jogador online - criar dossiê básico
            PlayerDossier dossier = new PlayerDossier(playerName);
            dossier.setUuid(onlinePlayer.getUniqueId().toString());
            dossier.setOnline(true);
            dossier.setDisplayName(onlinePlayer.getDisplayName());
            
            // Agregar dados de múltiplos managers
            aggregatePlayerDataAsync(dossier, callback);
            return;
        }
        
        // Se não está online, consultar banco de dados
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final PlayerDossier[] dossier = {null};
            
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT uuid, last_seen FROM player_data WHERE player_name = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerName);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            dossier[0] = new PlayerDossier(playerName);
                            dossier[0].setUuid(rs.getString("uuid"));
                            dossier[0].setLastSeen(rs.getTimestamp("last_seen"));
                            dossier[0].setOnline(false);
                            
                            // Formatar última vez visto
                            if (dossier[0].getLastSeen() != null) {
                                long timeDiff = System.currentTimeMillis() - dossier[0].getLastSeen().getTime();
                                dossier[0].setFormattedLastSeen(formatTimeDifference(timeDiff));
                            } else {
                                dossier[0].setFormattedLastSeen("nunca foi visto");
                            }
                        }
                    }
                }
                
                // Se encontrou o jogador, agregar dados de múltiplos managers
                if (dossier[0] != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        aggregatePlayerDataAsync(dossier[0], callback);
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }
                
            } catch (SQLException e) {
                logger.severe("❌ Erro ao consultar dossiê do jogador " + playerName + ": " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }
    
    /**
     * Agrega dados de múltiplos managers para o dossiê.
     * Implementa a estratégia de "Agregação Assíncrona Sequencial".
     */
    private void aggregatePlayerDataAsync(PlayerDossier dossier, Consumer<PlayerDossier> callback) {
        // PASSO A: Buscar dados primários do DataManager
        loadPlayerDataFromDataManager(dossier, (dataLoaded) -> {
            if (!dataLoaded) {
                // Se falhou ao carregar dados básicos, retornar dossiê com dados mínimos
                setDefaultData(dossier);
                callback.accept(dossier);
                return;
            }
            
            // PASSO B: Buscar informações de clã do ClanManager
            loadClanDataFromClanManager(dossier, (clanLoaded) -> {
                // PASSO C: Buscar informações de homes do EssentialsManager
                loadHomesDataFromEssentialsManager(dossier, (homesLoaded) -> {
                    // Dossiê completo - retornar resultado final
                    callback.accept(dossier);
                });
            });
        });
    }
    
    /**
     * PASSO A: Carrega dados primários do DataManager.
     */
    private void loadPlayerDataFromDataManager(PlayerDossier dossier, Consumer<Boolean> callback) {
        try {
            // Obter perfil do jogador via DataManager
            br.com.primeleague.core.models.PlayerProfile profile = null;
            
            if (dossier.isOnline()) {
                // Jogador online - buscar via Bukkit
                org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(dossier.getPlayerName());
                if (onlinePlayer != null) {
                    profile = br.com.primeleague.core.api.PrimeLeagueAPI.getPlayerProfile(onlinePlayer);
                }
            } else {
                // Jogador offline - buscar pelo nome
                profile = br.com.primeleague.core.api.PrimeLeagueAPI.getProfileByName(dossier.getPlayerName());
            }
            
            if (profile != null) {
                // Preencher dados básicos
                dossier.setDisplayName(profile.getPlayerName());
                dossier.setElo(profile.getElo());
                dossier.setMoney(profile.getMoney().doubleValue());
                dossier.setLevel(1); // Placeholder - não disponível no PlayerProfile
                
                // Dados de identidade básicos
                dossier.setRank("Jogador"); // Placeholder - integrar com PermissionManager futuramente
                dossier.setGroup("default"); // Placeholder
                
                // Dados de estatísticas básicos
                dossier.setKills(0); // Placeholder - não disponível no PlayerProfile
                dossier.setDeaths(0); // Placeholder - não disponível no PlayerProfile
                dossier.setKdr(0.0); // Placeholder
                
                callback.accept(true);
            } else {
                // Perfil não encontrado - usar dados mínimos
                setDefaultData(dossier);
                callback.accept(false);
            }
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao carregar dados do DataManager para " + dossier.getPlayerName() + ": " + e.getMessage());
            setDefaultData(dossier);
            callback.accept(false);
        }
    }
    
    /**
     * PASSO B: Carrega informações de clã do ClanManager.
     * Implementação corrigida: acesso direto via PluginManager (API contratual).
     */
    private void loadClanDataFromClanManager(PlayerDossier dossier, Consumer<Boolean> callback) {
        try {
            // Acesso via PluginManager - o caminho arquiteturalmente correto
            org.bukkit.plugin.Plugin clansPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("PrimeLeague-Clans");

            // Verifica se o módulo está ativo
            if (clansPlugin != null && clansPlugin.isEnabled()) {
                
                // Cast seguro para a classe principal do plugin
                br.com.primeleague.clans.PrimeLeagueClans clansInstance = (br.com.primeleague.clans.PrimeLeagueClans) clansPlugin;
                br.com.primeleague.clans.manager.ClanManager clanManager = clansInstance.getClanManager();
                
                // Utiliza os métodos públicos do manager diretamente
                br.com.primeleague.clans.model.Clan clan = clanManager.getClanByPlayerName(dossier.getPlayerName());
                
                if (clan != null) {
                    br.com.primeleague.clans.model.ClanPlayer clanPlayer = clanManager.getClanPlayerByName(dossier.getPlayerName());
                    
                    dossier.setHasClan(true);
                    dossier.setClanName(clan.getName());
                    dossier.setClanTag(clan.getTag());
                    dossier.setClanRole(clanPlayer != null ? clanPlayer.getRole().getDisplayName() : "Membro");
                } else {
                    dossier.setHasClan(false);
                    dossier.setClanName(null);
                    dossier.setClanTag(null);
                    dossier.setClanRole(null);
                }
                
                callback.accept(true);
                
            } else {
                // Plugin de clãs não disponível, usar dados padrão
                dossier.setHasClan(false);
                dossier.setClanName(null);
                dossier.setClanTag(null);
                dossier.setClanRole(null);
                callback.accept(true);
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar dados de clã para " + dossier.getPlayerName() + ": " + e.getMessage());
            dossier.setHasClan(false); // Fallback seguro
            dossier.setClanName(null);
            dossier.setClanTag(null);
            dossier.setClanRole(null);
            callback.accept(true);
        }
    }
    
    /**
     * PASSO C: Carrega informações de homes do EssentialsManager.
     */
    private void loadHomesDataFromEssentialsManager(PlayerDossier dossier, Consumer<Boolean> callback) {
        try {
            // Obter EssentialsManager do plugin
            br.com.primeleague.essentials.EssentialsPlugin essentialsPlugin = 
                (br.com.primeleague.essentials.EssentialsPlugin) org.bukkit.Bukkit.getPluginManager().getPlugin("PrimeLeague-Essentials");
            
            if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
                br.com.primeleague.essentials.managers.EssentialsManager essentialsManager = 
                    essentialsPlugin.getEssentialsManager();
                
                // Converter nome para UUID se necessário
                java.util.UUID playerUuid = null;
                if (dossier.isOnline()) {
                    org.bukkit.entity.Player onlinePlayer = org.bukkit.Bukkit.getPlayer(dossier.getPlayerName());
                    if (onlinePlayer != null) {
                        playerUuid = onlinePlayer.getUniqueId();
                    }
                } else {
                    // Buscar UUID pelo nome
                    playerUuid = java.util.UUID.fromString(dossier.getUuid());
                }
                
                if (playerUuid != null) {
                    // Buscar homes do jogador
                    essentialsManager.getPlayerHomesAsync(playerUuid, (homes) -> {
                        if (homes != null) {
                            dossier.setHomeCount(homes.size());
                            dossier.getHomes().clear();
                            for (br.com.primeleague.api.models.Home home : homes) {
                                dossier.getHomes().add(home.getHomeName());
                            }
                        } else {
                            dossier.setHomeCount(0);
                            dossier.getHomes().clear();
                        }
                        
                        // Definir limite máximo de homes (placeholder)
                        dossier.setMaxHomes(3);
                        
                        callback.accept(true);
                    });
                } else {
                    // UUID não encontrado
                    dossier.setHomeCount(0);
                    dossier.getHomes().clear();
                    dossier.setMaxHomes(3);
                    callback.accept(true);
                }
                
            } else {
                // Plugin Essentials não disponível
                dossier.setHomeCount(0);
                dossier.getHomes().clear();
                dossier.setMaxHomes(3);
                callback.accept(true);
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar dados de homes para " + dossier.getPlayerName() + ": " + e.getMessage());
            // Em caso de erro, usar dados padrão
            dossier.setHomeCount(0);
            dossier.getHomes().clear();
            dossier.setMaxHomes(3);
            callback.accept(true);
        }
    }
    
    /**
     * Define dados padrão para o dossiê em caso de erro.
     */
    private void setDefaultData(PlayerDossier dossier) {
        // Dados básicos de identidade
        if (dossier.getDisplayName() == null) {
            dossier.setDisplayName(dossier.getPlayerName());
        }
        dossier.setRank("Jogador");
        dossier.setGroup("default");
        
        // Dados básicos de estatísticas
        dossier.setElo(1000);
        dossier.setMoney(0.0);
        dossier.setLevel(1);
        dossier.setKills(0);
        dossier.setDeaths(0);
        dossier.setKdr(0.0);
        
        // Dados básicos de clã
        dossier.setHasClan(false);
        dossier.setClanName(null);
        dossier.setClanRole(null);
        dossier.setClanTag(null);
        
        // Dados básicos de essentials
        dossier.setHomeCount(0);
        dossier.setMaxHomes(3);
        dossier.getHomes().clear();
    }
}
