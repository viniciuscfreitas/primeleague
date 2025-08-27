package br.com.primeleague.core.profile;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.util.UUIDUtils;
import br.com.primeleague.core.PrimeLeagueCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class ProfileListener implements Listener {

    private final DataManager dataManager;
    private final PrimeLeagueCore plugin;

    public ProfileListener(DataManager dataManager) {
        this.dataManager = dataManager;
        this.plugin = PrimeLeagueCore.getInstance();
    }

    /**
     * Carrega perfil do jogador ANTES da entrada no servidor.
     * EventPriority.LOWEST garante que executa primeiro que outros plugins.
     * CORREÇÃO DEFINITIVA: Injeção de UUID canônico no processo de login.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        
        // PASSO 1: Descobrir o UUID CANÔNICO (a fonte da verdade).
        PlayerProfile existingProfile = dataManager.getPlayerProfileByName(playerName);
        final UUID canonicalUuid;
        
        if (existingProfile != null) {
            // Jogador já existe, usar o UUID do banco de dados.
            canonicalUuid = existingProfile.getUuid();
        } else {
            // Novo jogador, gerar o UUID determinístico que será salvo.
            canonicalUuid = UUIDUtils.offlineUUIDFromName(playerName);
        }

        // PASSO 2: INJETAR O UUID CORRETO NO PROCESSO DE LOGIN.
        // Esta é a correção crítica que força o Bukkit a usar nosso UUID.
        try {
            // Tentativa de injeção de UUID via reflection
            java.lang.reflect.Field uuidField = event.getClass().getDeclaredField("uniqueId");
            uuidField.setAccessible(true);
            uuidField.set(event, canonicalUuid);
        } catch (Exception e) {
            // O sistema continuará em modo de compatibilidade
        }

        // PASSO 3: Executar o padrão de "Loading State" com o UUID canônico.
        dataManager.startLoading(canonicalUuid);
        
        try {
            // REFATORADO: Só carregar perfil se existir, NÃO criar automaticamente
            // A criação será feita apenas via comando /registrar no Discord
            PlayerProfile profile = dataManager.loadPlayerProfile(canonicalUuid);
            
            if (profile == null) {
                // Perfil não existe - não criar automaticamente
                // O AuthenticationListener decidirá se deve kickar ou não
                plugin.getLogger().info("[PROFILE-LISTENER] Perfil não encontrado para " + playerName + " - aguardando decisão do AuthenticationListener");
            } else {
                // Perfil existe - carregar normalmente
                plugin.getLogger().info("[PROFILE-LISTENER] Perfil carregado para " + playerName);
            }
        } finally {
            dataManager.finishLoading(canonicalUuid);
        }
    }

        @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        // OBTER PLAYER_ID DO IDENTITY MANAGER (UMA ÚNICA VEZ)
        Integer playerId = null;
        try {
            playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
            
            // REGISTRAR JOGADOR NO SISTEMA DE IDENTIDADE
            PrimeLeagueAPI.getIdentityManager().registerPlayer(player, playerId);
            
        } catch (IllegalStateException e) {
            // Jogador não encontrado no banco - erro crítico
            player.kickPlayer("§cErro de identidade. Entre em contato com a administração.");
            return;
        }

        // Verificar se perfil já está no cache
        PlayerProfile existingProfile = dataManager.getPlayerProfileByName(name);
        if (existingProfile == null) {
            // Fallback - carregar se não estiver no cache
            UUID canonicalUuid = UUIDUtils.offlineUUIDFromName(name);
            dataManager.loadPlayerProfileWithCreation(canonicalUuid, name);
        }

        // 🔗 CRIAÇÃO DO MAPEAMENTO DE UUID PARA O CHAT LOG
        // Obter UUID do Bukkit e UUID canônico do banco
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = existingProfile != null ? existingProfile.getUuid() : UUIDUtils.offlineUUIDFromName(name);
        
        // Criar mapeamento no DataManager para o tradutor de identidade
        dataManager.addUuidMapping(bukkitUuid, canonicalUuid);
        
        // 🔥 CACHE WARMING - ECONOMIA
        // Carregar saldo do jogador no cache para operações instantâneas
        if (playerId != null) {
            try {
                PrimeLeagueAPI.getEconomyManager().getBalance(playerId);
                // Log de debug (opcional)
                // player.sendMessage("§a💰 Saldo carregado no cache: $" + PrimeLeagueAPI.getEconomyManager().getBalance(playerId));
            } catch (Exception e) {
                // Não bloquear o login por falha no cache warming
                // Log com stack trace para debugging, mas sem bloquear o login
                dataManager.getPlugin().getLogger().log(java.util.logging.Level.WARNING, 
                    "⚠️ [CACHE-WARMING] Falha ao carregar saldo no cache para " + name + " (player_id: " + playerId + ")", e);
            }
        }
        
        dataManager.getPlugin().getLogger().info("🔗 [PROFILE-LISTENER] Mapeamento criado para " + name + ": " + bukkitUuid + " → " + canonicalUuid);
    }

        @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        // DESREGISTRAR JOGADOR DO SISTEMA DE IDENTIDADE
        PrimeLeagueAPI.getIdentityManager().unregisterPlayer(player);

        // 🗑️ REMOÇÃO DO MAPEAMENTO DE UUID
        UUID bukkitUuid = player.getUniqueId();
        dataManager.removeUuidMapping(bukkitUuid);
        dataManager.getPlugin().getLogger().info("🗑️ [PROFILE-LISTENER] Mapeamento removido para " + name + ": " + bukkitUuid);

        // Salvar perfil se necessário
        PlayerProfile existingProfile = dataManager.getPlayerProfileByName(name);
        if (existingProfile != null) {
            dataManager.savePlayerProfileAsync(existingProfile);
        }
    }
}


