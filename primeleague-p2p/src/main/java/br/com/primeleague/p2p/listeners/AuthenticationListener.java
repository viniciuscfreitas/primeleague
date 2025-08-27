package br.com.primeleague.p2p.listeners;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.util.UUIDUtils;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * Listener de autenticação simplificada para o Sistema de Portfólio de Contas.
 * 
 * ARQUITETURA V3.0 (Gestor de Portfólio):
 * - Verificação direta e simples por conta individual
 * - Sem complexidade de slots ou clãs
 * - 1 Discord ID pode vincular N contas
 * - Cada conta tem assinatura individual (1-para-1)
 * - Fonte da verdade: player_data.subscription_expires_at
 * 
 * REFATORADO para usar DataManager normalizado:
 * - Usa loadPlayerProfileWithClan() para obter dados completos
 * - Compatível com schema normalizado sem clan_id em player_data
 * - Usa BigDecimal para operações monetárias precisas
 * 
 * FLUXO DE AUTENTICAÇÃO SIMPLIFICADO:
 * 1. AsyncPlayerPreLoginEvent: Verificação binária (conta ativa? SIM/NÃO)
 * 2. PlayerJoinEvent: Verificação de limbo se necessário
 * 
 * @author PrimeLeague Team
 * @version 3.1.0 (Refatorado para schema normalizado)
 */
public final class AuthenticationListener implements Listener {

    private final PrimeLeagueP2P plugin;

    public AuthenticationListener() {
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    /**
     * Processa autenticação de jogadores ANTES da entrada no servidor.
     * Nova lógica simplificada do Sistema de Portfólio.
     * REFATORADO: Usa DataManager normalizado.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        final String playerName = event.getName();
        final String ipAddress = event.getAddress().getHostAddress();
        
        // 0. Verificar se o jogador está na whitelist (BYPASS TOTAL)
        if (isPlayerWhitelisted(playerName)) {
            event.allow();
            return;
        }
        
        // ================================================================
        // PASSO 1: VALIDAÇÃO DE NOME CANÔNICO (A MURALHA IMPENETRÁVEL)
        // ================================================================
        try {
            // Busca o nome canônico no banco de forma case-insensitive
            String canonicalName = getCanonicalPlayerName(playerName);
            
            // Se um nome canônico foi encontrado, mas é diferente do nome digitado
            if (canonicalName != null && !playerName.equals(canonicalName)) {
                // Nega o login com a mensagem educativa
                plugin.getLogger().warning(String.format(
                    "[AUTH] ❌ Capitalização incorreta: '%s' -> '%s'",
                    playerName, canonicalName
                ));
                event.disallow(Result.KICK_OTHER, 
                    "§cCapitalização Incorreta!\n\n" +
                    "§fO nome para esta conta está registrado como:\n" +
                    "§a" + canonicalName + "§f\n\n" +
                    "§ePor favor, utilize o nome correto para se conectar."
                );
                return; // Encerra o processamento aqui
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[AUTH] Erro na validação de nome canônico para " + playerName + ": " + e.getMessage());
            // Continua o processamento em caso de erro na validação
        }
        
        try {
            // 1. Obter UUID canônico do Core (Single Source of Truth)
            final UUID playerUuid = UUIDUtils.offlineUUIDFromName(playerName);
            
            // 2. REFATORADO: Usar DataManager normalizado para carregar perfil completo
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().loadPlayerProfileWithClan(playerUuid, playerName);
            
            if (profile == null) {
                // Erro ao carregar perfil
                plugin.getLogger().severe("[AUTH] Falha ao carregar perfil para " + playerName);
                event.disallow(Result.KICK_OTHER, 
                    "§cErro interno de autenticação.\n" +
                    "§7Tente novamente em instantes.\n" +
                    "§7Se persistir, contate a administração."
                );
                return;
            }
            
            // 3. Verificação BINÁRIA: Esta conta específica tem assinatura ativa?
            AccountStatus status = checkAccountSubscriptionFromProfile(profile);
            
            switch (status.getStatus()) {
                case ACTIVE:
                    // ✅ ACESSO AUTORIZADO
                    plugin.getLogger().info(String.format(
                        "[AUTH] ✅ Acesso autorizado: %s (expira em %d dias)",
                        playerName, status.getDaysRemaining()
                    ));
                    event.allow();
                    break;
                    
                case EXPIRED:
                    // ❌ ASSINATURA EXPIRADA
                    plugin.getLogger().info(String.format(
                        "[AUTH] ❌ Assinatura expirada: %s",
                        playerName
                    ));
                    event.disallow(Result.KICK_OTHER, 
                        "§c✖ Assinatura Expirada\n\n" +
                        "§7Sua assinatura do Prime League expirou.\n" +
                        "§7Para renovar, utilize §e/renovar§7 no Discord.\n\n" +
                        "§6Discord: §fdiscord.gg/primeleague"
                    );
                    break;
                    
                case NEVER_SUBSCRIBED:
                    // ❌ NUNCA TEVE ASSINATURA
                    plugin.getLogger().info(String.format(
                        "[AUTH] ❌ Conta sem assinatura: %s",
                        playerName
                    ));
                    event.disallow(Result.KICK_OTHER, 
                        "§c✖ Assinatura Necessária\n\n" +
                        "§7Esta conta não possui assinatura ativa.\n" +
                        "§7Para adquirir, utilize §e/renovar§7 no Discord.\n\n" +
                        "§6Discord: §fdiscord.gg/primeleague"
                    );
                    break;
                    
                case PENDING_VERIFICATION:
                    // ⏳ VERIFICAÇÃO PENDENTE - PERMITIR PARA LIMBO
                    event.allow(); // LimboManager assumirá controle
                    break;
                    
                default:
                    // ❌ STATUS DESCONHECIDO
                    plugin.getLogger().warning("[AUTH] Status desconhecido para " + playerName + ": " + status.getStatus());
                event.disallow(Result.KICK_OTHER, 
                        "§cErro interno de autenticação.\n" +
                        "§7Contate a administração."
                    );
                    break;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Exceção crítica em autenticação para " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            
            // Em caso de erro, negar acesso por segurança
            event.disallow(Result.KICK_OTHER, 
                "§cErro interno de autenticação.\n" +
                "§7Tente novamente em instantes.\n" +
                "§7Se persistir, contate a administração."
            );
        }
    }

    /**
     * Processa a entrada do jogador no servidor.
     * Verifica se precisa ser colocado em limbo.
     * REFATORADO: Usa DataManager normalizado.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final UUID playerUuid = player.getUniqueId();
        
        // Verificar se o jogador está na whitelist (BYPASS TOTAL)
        if (isPlayerWhitelisted(playerName)) {
            return; // Pula toda a verificação
        }
        
        try {
            // REFATORADO: Usar DataManager normalizado para verificar vínculo Discord
            boolean isLinked = isPlayerLinkedToDiscord(playerUuid);
            
            if (!isLinked) {
                // Jogador não vinculado - colocar em limbo para verificação
                
                // Delay para garantir que o jogador carregou completamente
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.getLimboManager().putPlayerInLimbo(player);
                        }
                    }
                }, 20L); // 1 segundo de delay
                return;
                }
            
            // Log de entrada simplificado
            plugin.getLogger().info("[JOIN] " + playerName + " entrou no servidor");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[JOIN] Erro ao processar entrada de " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se um jogador está vinculado e verificado no Discord.
     * REFATORADO: Usa DataManager normalizado.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está vinculado e verificado
     */
    private boolean isPlayerLinkedToDiscord(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                return false;
            }
            
            // Query para verificar se está vinculado e verificado
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE";
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DISCORD] Erro ao verificar vínculo para " + playerUuid + ": " + e.getMessage());
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Silenciar erros de cleanup
            }
        }
    }

    /**
     * Verifica o status de assinatura de uma conta usando o perfil carregado.
     * REFATORADO: Usa PlayerProfile do DataManager normalizado.
     * 
     * @param profile PlayerProfile carregado via DataManager
     * @return AccountStatus com informações da assinatura
     */
    private AccountStatus checkAccountSubscriptionFromProfile(PlayerProfile profile) {
        try {
            // 1. Verificar se está vinculado ao Discord
            boolean isLinked = isPlayerLinkedToDiscord(profile.getUuid());
            
            if (!isLinked) {
                // Não vinculado
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            // 2. Verificar assinatura usando dados do perfil
            if (profile.getSubscriptionExpiry() == null) {
                // Nunca teve assinatura
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // Calcular dias restantes
            long currentTime = System.currentTimeMillis();
            long expiresTime = profile.getSubscriptionExpiry().getTime();
            
            if (expiresTime > currentTime) {
                // Assinatura ativa
                int daysRemaining = (int) ((expiresTime - currentTime) / (1000 * 60 * 60 * 24));
                return new AccountStatus(SubscriptionStatus.ACTIVE, daysRemaining);
            } else {
                // Assinatura expirada
                return new AccountStatus(SubscriptionStatus.EXPIRED, 0);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Erro ao verificar assinatura para " + profile.getUuid() + ": " + e.getMessage());
            return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
        }
    }

    /**
     * Verifica o status de assinatura de uma conta específica.
     * MÉTODO LEGADO: Mantido para compatibilidade, mas prefira checkAccountSubscriptionFromProfile.
     * Fonte da verdade: player_data.subscription_expires_at
     * 
     * @param playerUuid UUID do jogador
     * @return AccountStatus com informações da assinatura
     * @deprecated Use checkAccountSubscriptionFromProfile(PlayerProfile) instead
     */
    @Deprecated
    private AccountStatus checkAccountSubscription(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // 1. Verificar se está vinculado ao Discord
            String checkLinkSql = "SELECT dl.verified FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? LIMIT 1";
            ps = conn.prepareStatement(checkLinkSql);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Não vinculado
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            boolean isVerified = rs.getBoolean("verified");
            rs.close();
            ps.close();
            
            if (!isVerified) {
                // Vinculado mas não verificado
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            // 2. Verificar assinatura na tabela player_data
            String checkSubSql = "SELECT subscription_expires_at FROM player_data WHERE uuid = ?";
            ps = conn.prepareStatement(checkSubSql);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Perfil não encontrado
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            Timestamp expiresAt = rs.getTimestamp("subscription_expires_at");
            
            if (expiresAt == null) {
                // Nunca teve assinatura
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // Calcular dias restantes
            long currentTime = System.currentTimeMillis();
            long expiresTime = expiresAt.getTime();
            
            if (expiresTime > currentTime) {
                // Assinatura ativa
                int daysRemaining = (int) ((expiresTime - currentTime) / (1000 * 60 * 60 * 24));
                return new AccountStatus(SubscriptionStatus.ACTIVE, daysRemaining);
            } else {
                // Assinatura expirada
                return new AccountStatus(SubscriptionStatus.EXPIRED, 0);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Erro ao verificar assinatura para " + playerUuid + ": " + e.getMessage());
            return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Silenciar erros de cleanup
            }
        }
    }

    /**
     * Enum para status de assinatura.
     */
    private enum SubscriptionStatus {
        ACTIVE,
        EXPIRED,
        NEVER_SUBSCRIBED,
        PENDING_VERIFICATION
    }

    /**
     * Classe para armazenar informações de status da conta.
     */
    private static class AccountStatus {
        private final SubscriptionStatus status;
        private final int daysRemaining;

        public AccountStatus(SubscriptionStatus status, int daysRemaining) {
            this.status = status;
            this.daysRemaining = daysRemaining;
        }

        public SubscriptionStatus getStatus() {
            return status;
        }

        public int getDaysRemaining() {
            return daysRemaining;
        }
    }

    /**
     * Verifica se um jogador está na whitelist usando a nova API centralizada.
     * @param playerName Nome do jogador
     * @return true se o jogador estiver na whitelist, false caso contrário
     */
    private boolean isPlayerWhitelisted(String playerName) {
        try {
            // Obter UUID canônico do jogador
            UUID playerUuid = UUIDUtils.offlineUUIDFromName(playerName);
            
            // Usar a nova API centralizada do Core com UUID como fonte única da verdade
            return br.com.primeleague.core.api.PrimeLeagueAPI.isWhitelisted(playerUuid);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao verificar whitelist para " + playerName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca o nome canônico de um jogador no banco de dados.
     * Executa busca case-insensitive para validar capitalização.
     * REFATORADO: Usa DataManager normalizado.
     * 
     * @param playerName Nome digitado pelo jogador
     * @return Nome canônico se encontrado, null se não existir
     */
    private String getCanonicalPlayerName(String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            conn = PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) return null;
            
            // Busca case-insensitive usando COLLATE
            String sql = "SELECT name FROM player_data WHERE name COLLATE utf8mb4_unicode_ci = ? LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("name"); // Retorna o nome canônico do banco
            }
            
            return null; // Nome não encontrado
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao buscar nome canônico para " + playerName + ": " + e.getMessage());
            return null;
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                // Silenciar erros de cleanup
            }
        }
    }
}