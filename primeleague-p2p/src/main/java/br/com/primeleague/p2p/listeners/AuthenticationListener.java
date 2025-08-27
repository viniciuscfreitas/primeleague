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
 * - Fonte da verdade: discord_users.subscription_expires_at (assinatura compartilhada)
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
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 UUID gerado para " + playerName + ": " + playerUuid.toString());
            
            // DEBUG: Verificar se o UUID gerado é o correto
            String expectedUuid = "9b261df7-633c-3e05-9b0e-811f72be39ab";
            boolean uuidMatches = playerUuid.toString().equals(expectedUuid);
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 UUID esperado: " + expectedUuid);
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 UUIDs são iguais: " + uuidMatches);
            
            // 2. DEBUG: Verificar se o player existe diretamente no banco
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 Verificando se player existe no banco...");
            boolean playerExistsInDB = checkPlayerExistsInDatabase(playerUuid, playerName);
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Player existe no banco: " + (playerExistsInDB ? "SIM" : "NÃO"));
            
            // 3. REFATORADO: Usar DataManager normalizado para carregar perfil completo
            // NÃO criar perfil automaticamente - só carregar se existir
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 Tentando carregar perfil via DataManager...");
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().loadPlayerProfile(playerUuid);
            
            if (profile == null) {
                plugin.getLogger().warning("[AUTH-DEBUG] ❌ DataManager retornou null para " + playerName);
                plugin.getLogger().warning("[AUTH-DEBUG] ❌ UUID usado: " + playerUuid.toString());
                plugin.getLogger().warning("[AUTH-DEBUG] ❌ Player existe no banco: " + (playerExistsInDB ? "SIM" : "NÃO"));
                
                // Perfil não existe - jogador não registrado no Discord
                plugin.getLogger().info("[AUTH] Perfil não encontrado para " + playerName + " - não registrado no Discord");
                event.disallow(Result.KICK_OTHER, 
                    "§c§l✖ Registro Necessário\n\n" +
                    "§fVocê precisa se registrar no Discord primeiro!\n\n" +
                    "§e📱 Discord: §fdiscord.gg/primeleague\n" +
                    "§e📱 Comando: §f/registrar " + playerName + "\n\n" +
                    "§a🔄 Após o registro, use o código de verificação no servidor!"
                );
                return;
            }
            
            plugin.getLogger().info("[AUTH-DEBUG] ✅ Perfil carregado com sucesso para " + playerName);
            
            // 3. Verificação BINÁRIA: Esta conta específica tem assinatura ativa?
            AccountStatus status = checkAccountSubscriptionFromProfile(profile);
            
            // DEBUG: Log detalhado do status
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Status final para " + playerName + ": " + status.getStatus() + " (dias: " + status.getDaysRemaining() + ")");
            
                         switch (status.getStatus()) {
                 case ACTIVE:
                     // ✅ ACESSO AUTORIZADO - Assinatura ativa
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
                         "§c§l✖ Assinatura Expirada\n\n" +
                         "§fSua assinatura do Prime League expirou.\n" +
                         "§7Para renovar e continuar jogando:\n\n" +
                         "§e💎 Discord: §fdiscord.gg/primeleague\n" +
                         "§e💎 Comando: §f/assinatura\n\n" +
                         "§a🔄 Conecte novamente após renovar!"
                     );
                     break;
                     
                 case NEVER_SUBSCRIBED:
                     // ❌ NUNCA TEVE ASSINATURA
                     plugin.getLogger().info(String.format(
                         "[AUTH] ❌ Conta sem assinatura: %s",
                         playerName
                     ));
                     event.disallow(Result.KICK_OTHER, 
                         "§c§l✖ Assinatura Necessária\n\n" +
                         "§fEsta conta não possui assinatura ativa.\n" +
                         "§7Para adquirir e acessar o servidor:\n\n" +
                         "§e💎 Discord: §fdiscord.gg/primeleague\n" +
                         "§e💎 Comando: §f/assinatura\n\n" +
                         "§a🔄 Conecte novamente após adquirir!"
                     );
                     break;
                     
                 case PENDING_VERIFICATION:
                     // ⏳ VERIFICAÇÃO PENDENTE - PERMITIR PARA VERIFICAÇÃO
                     plugin.getLogger().info(String.format(
                         "[AUTH] ⏳ Verificação pendente: %s - permitindo entrada para verificação",
                         playerName
                     ));
                     event.allow(); // Permitir entrada para verificação
                     break;
                     
                 case NOT_REGISTERED:
                     // ❌ NÃO REGISTRADO - KICK IMEDIATO
                     plugin.getLogger().info(String.format(
                         "[AUTH] ❌ Não registrado: %s - kick imediato",
                         playerName
                     ));
                     event.disallow(Result.KICK_OTHER, 
                         "§c§l✖ Registro Necessário\n\n" +
                         "§fVocê precisa se registrar no Discord primeiro!\n\n" +
                         "§e📱 Discord: §fdiscord.gg/primeleague\n" +
                         "§e📱 Comando: §f/registrar " + playerName + "\n\n" +
                         "§a🔄 Após o registro, use o código de verificação no servidor!"
                     );
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
     * SOLUÇÃO SIMPLIFICADA: Remove duplicação de lógica.
     * O AsyncPlayerPreLoginEvent já faz toda a verificação necessária.
     * Este método apenas coloca em limbo se necessário.
     * DEBUG: Logs detalhados para troubleshooting.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();
        final UUID playerUuid = player.getUniqueId();
        
        plugin.getLogger().info("[JOIN-DEBUG] 🎮 Jogador entrou: " + playerName + " (UUID: " + playerUuid + ")");
        
        // Verificar se o jogador está na whitelist (BYPASS TOTAL)
        if (isPlayerWhitelisted(playerName)) {
            plugin.getLogger().info("[JOIN-DEBUG] ✅ Jogador na whitelist - bypass total");
            return; // Pula toda a verificação
        }
        
                 try {
             plugin.getLogger().info("[JOIN-DEBUG] 🔍 Verificando se precisa de verificação para: " + playerName);
             
             // CORREÇÃO: Usar UUID canônico do Core (mesmo do AsyncPlayerPreLoginEvent)
             final UUID canonicalUuid = UUIDUtils.offlineUUIDFromName(playerName);
             plugin.getLogger().info("[JOIN-DEBUG] 🔄 UUID do jogador: " + playerUuid);
             plugin.getLogger().info("[JOIN-DEBUG] 🔄 UUID canônico: " + canonicalUuid);
             
             // Verificar se o jogador tem registro pendente (não verificado)
             boolean hasPendingVerification = hasPendingVerification(canonicalUuid);
             
             plugin.getLogger().info("[JOIN-DEBUG] 📊 Resultado da verificação: " + (hasPendingVerification ? "PENDENTE" : "VERIFICADO/ATIVO"));
             
             if (hasPendingVerification) {
                 // Jogador tem registro pendente - colocar em limbo para verificação
                 plugin.getLogger().info("[JOIN-DEBUG] ⏳ Jogador com verificação pendente: " + playerName + " - colocando em limbo");
                 
                 // Delay para garantir que o jogador carregou completamente
                 plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                     @Override
                     public void run() {
                         if (player.isOnline()) {
                             plugin.getLogger().info("[JOIN-DEBUG] 🚀 Executando putPlayerInLimbo para: " + playerName);
                             plugin.getLimboManager().putPlayerInLimbo(player);
                             plugin.getLogger().info("[JOIN-DEBUG] ✅ Jogador colocado em limbo: " + playerName);
                         } else {
                             plugin.getLogger().warning("[JOIN-DEBUG] ⚠️ Jogador offline durante colocação em limbo: " + playerName);
                         }
                     }
                 }, 20L); // 1 segundo de delay
             } else {
                 // Jogador verificado e ativo - log de entrada
                 plugin.getLogger().info("[JOIN-DEBUG] ✅ " + playerName + " entrou no servidor (verificado e ativo)");
             }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[JOIN-DEBUG] ❌ Erro ao processar entrada de " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se um player existe diretamente no banco de dados.
     * DEBUG: Para diagnosticar problemas de UUID incompatibilidade.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @return true se o player existe no banco
     */
    private boolean checkPlayerExistsInDatabase(UUID playerUuid, String playerName) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DB-DEBUG] 🔍 Verificando se player existe no banco: " + playerName);
        plugin.getLogger().info("[DB-DEBUG] 📝 UUID: " + playerUuid.toString());
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[DB-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[DB-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se o player existe por UUID
            String sqlByUuid = "SELECT COUNT(*) FROM player_data WHERE uuid = ?";
            plugin.getLogger().info("[DB-DEBUG] 📝 Executando query por UUID: " + sqlByUuid);
            
            ps = conn.prepareStatement(sqlByUuid);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int countByUuid = rs.getInt(1);
                plugin.getLogger().info("[DB-DEBUG] 📊 Resultado por UUID: " + countByUuid + " registros");
                
                if (countByUuid > 0) {
                    plugin.getLogger().info("[DB-DEBUG] ✅ Player encontrado por UUID!");
                    return true;
                }
            }
            
            // Se não encontrou por UUID, tentar por nome
            plugin.getLogger().info("[DB-DEBUG] 🔍 Player não encontrado por UUID, tentando por nome...");
            
            rs.close();
            ps.close();
            
            String sqlByName = "SELECT COUNT(*) FROM player_data WHERE name = ?";
            plugin.getLogger().info("[DB-DEBUG] 📝 Executando query por nome: " + sqlByName);
            
            ps = conn.prepareStatement(sqlByName);
            ps.setString(1, playerName);
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int countByName = rs.getInt(1);
                plugin.getLogger().info("[DB-DEBUG] 📊 Resultado por nome: " + countByName + " registros");
                
                if (countByName > 0) {
                    plugin.getLogger().info("[DB-DEBUG] ✅ Player encontrado por nome!");
                    
                    // Se encontrou por nome, verificar qual UUID está no banco
                    rs.close();
                    ps.close();
                    
                    String sqlGetUuid = "SELECT uuid FROM player_data WHERE name = ?";
                    ps = conn.prepareStatement(sqlGetUuid);
                    ps.setString(1, playerName);
                    
                    rs = ps.executeQuery();
                    if (rs.next()) {
                        String storedUuid = rs.getString("uuid");
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUID no banco: " + storedUuid);
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUID gerado: " + playerUuid.toString());
                        plugin.getLogger().warning("[DB-DEBUG] ⚠️ UUIDs são iguais: " + storedUuid.equals(playerUuid.toString()));
                    }
                    
                    return true;
                }
            }
            
            plugin.getLogger().warning("[DB-DEBUG] ❌ Player não encontrado nem por UUID nem por nome");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DB-DEBUG] ❌ Erro ao verificar player no banco: " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[DB-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[DB-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se um jogador está vinculado ao Discord (registrado via Discord).
     * REFATORADO: Usa DataManager normalizado.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está vinculado ao Discord (mesmo que não verificado)
     */
    private boolean isPlayerLinkedToDiscord(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DISCORD-DEBUG] 🔍 Verificando vínculo Discord para UUID: " + playerUuid);
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[DISCORD-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[DISCORD-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se está vinculado (mesmo que não verificado)
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ?";
            plugin.getLogger().info("[DISCORD-DEBUG] 📝 Executando query: " + sql);
            plugin.getLogger().info("[DISCORD-DEBUG] 📝 Parâmetro UUID: " + playerUuid.toString());
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[DISCORD-DEBUG] 📊 Resultado da query: " + count + " vínculos encontrados");
                
                boolean isLinked = count > 0;
                plugin.getLogger().info("[DISCORD-DEBUG] " + (isLinked ? "✅" : "❌") + " Jogador " + (isLinked ? "VINCULADO" : "NÃO VINCULADO") + " ao Discord");
                
                return isLinked;
            }
            
            plugin.getLogger().warning("[DISCORD-DEBUG] ❌ Nenhum resultado retornado pela query");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DISCORD-DEBUG] ❌ Erro ao verificar vínculo para " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[DISCORD-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[DISCORD-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se um jogador está verificado no Discord.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o jogador está verificado
     */
    private boolean isPlayerVerified(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[VERIFY-DEBUG] 🔍 Verificando se está verificado para UUID: " + playerUuid);
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().warning("[VERIFY-DEBUG] ❌ Conexão com banco nula");
                return false;
            }
            
            plugin.getLogger().info("[VERIFY-DEBUG] ✅ Conexão obtida com sucesso");
            
            // Query para verificar se está verificado
            String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE";
            plugin.getLogger().info("[VERIFY-DEBUG] 📝 Executando query: " + sql);
            plugin.getLogger().info("[VERIFY-DEBUG] 📝 Parâmetro UUID: " + playerUuid.toString());
            
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[VERIFY-DEBUG] 📊 Resultado da query: " + count + " vínculos verificados encontrados");
                
                boolean isVerified = count > 0;
                plugin.getLogger().info("[VERIFY-DEBUG] " + (isVerified ? "✅" : "❌") + " Jogador " + (isVerified ? "VERIFICADO" : "NÃO VERIFICADO"));
                
                return isVerified;
            }
            
            plugin.getLogger().warning("[VERIFY-DEBUG] ❌ Nenhum resultado retornado pela query");
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[VERIFY-DEBUG] ❌ Erro ao verificar status de verificação para " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
                plugin.getLogger().info("[VERIFY-DEBUG] 🧹 Recursos de banco liberados");
            } catch (Exception e) {
                plugin.getLogger().warning("[VERIFY-DEBUG] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica o status de assinatura de uma conta usando o perfil carregado.
     * REFATORADO: Usa PlayerProfile do DataManager normalizado.
     * DEBUG: Logs detalhados para troubleshooting.
     * 
     * @param profile PlayerProfile carregado via DataManager
     * @return AccountStatus com informações da assinatura
     */
    private AccountStatus checkAccountSubscriptionFromProfile(PlayerProfile profile) {
        plugin.getLogger().info("[AUTH-DEBUG] 🔍 Verificando assinatura para perfil: " + profile.getUuid());
        
        try {
            // 1. Verificar se está vinculado ao Discord
            plugin.getLogger().info("[AUTH-DEBUG] 📋 Verificando vínculo Discord...");
            boolean isLinked = isPlayerLinkedToDiscord(profile.getUuid());
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Vínculo Discord: " + (isLinked ? "SIM" : "NÃO"));
            
            if (!isLinked) {
                // Não vinculado - KICK IMEDIATO
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: NOT_REGISTERED (não registrado no Discord)");
                return new AccountStatus(SubscriptionStatus.NOT_REGISTERED, 0);
            }
            
            // 2. Verificar se está verificado
            plugin.getLogger().info("[AUTH-DEBUG] 📋 Verificando se está verificado...");
            boolean isVerified = isPlayerVerified(profile.getUuid());
            plugin.getLogger().info("[AUTH-DEBUG] 📊 Verificado: " + (isVerified ? "SIM" : "NÃO"));
            
            if (!isVerified) {
                // Registrado mas não verificado - permitir entrada para verificação
                plugin.getLogger().info("[AUTH-DEBUG] ⏳ Status: PENDING_VERIFICATION (registrado mas não verificado)");
                return new AccountStatus(SubscriptionStatus.PENDING_VERIFICATION, 0);
            }
            
            // 3. Obter Discord ID do jogador
            plugin.getLogger().info("[AUTH-DEBUG] 🔍 Buscando Discord ID...");
            String discordId = getDiscordIdByPlayerUuid(profile.getUuid());
            if (discordId == null) {
                plugin.getLogger().warning("[AUTH-DEBUG] ❌ Discord ID não encontrado para UUID: " + profile.getUuid());
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // 4. Verificar assinatura compartilhada por Discord ID
            plugin.getLogger().info("[AUTH-DEBUG] 📅 Verificando assinatura compartilhada para Discord ID: " + discordId);
            java.sql.Timestamp sharedSubscriptionExpiry = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getSharedSubscriptionByDiscordId(discordId);
            
            if (sharedSubscriptionExpiry == null) {
                // Sem assinatura compartilhada
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: NEVER_SUBSCRIBED (sem assinatura compartilhada)");
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            // Calcular dias restantes
            long currentTime = System.currentTimeMillis();
            long expiresTime = sharedSubscriptionExpiry.getTime();
            
            plugin.getLogger().info("[AUTH-DEBUG] ⏰ Tempo atual: " + new java.util.Date(currentTime));
            plugin.getLogger().info("[AUTH-DEBUG] ⏰ Expira em: " + new java.util.Date(expiresTime));
            
            if (expiresTime > currentTime) {
                // Assinatura ativa
                int daysRemaining = (int) ((expiresTime - currentTime) / (1000 * 60 * 60 * 24));
                plugin.getLogger().info("[AUTH-DEBUG] ✅ Status: ACTIVE (" + daysRemaining + " dias restantes)");
                return new AccountStatus(SubscriptionStatus.ACTIVE, daysRemaining);
            } else {
                // Assinatura expirada
                plugin.getLogger().info("[AUTH-DEBUG] ❌ Status: EXPIRED (expiresTime <= currentTime)");
                return new AccountStatus(SubscriptionStatus.EXPIRED, 0);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH-DEBUG] ❌ Erro ao verificar assinatura para " + profile.getUuid() + ": " + e.getMessage());
            e.printStackTrace();
            return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
        }
    }

    /**
     * Verifica o status de assinatura de uma conta específica.
     * MÉTODO LEGADO: Mantido para compatibilidade, mas prefira checkAccountSubscriptionFromProfile.
     * Fonte da verdade: discord_users.subscription_expires_at (assinatura compartilhada)
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
            
            // 2. Verificar assinatura compartilhada via Discord ID
            // Primeiro, obter o Discord ID do jogador
            String getDiscordIdSql = "SELECT dl.discord_id FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE LIMIT 1";
            ps = conn.prepareStatement(getDiscordIdSql);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Jogador não vinculado ao Discord
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            String discordId = rs.getString("discord_id");
            rs.close();
            ps.close();
            
            // Agora verificar a assinatura compartilhada
                         String checkSubSql = "SELECT subscription_expires_at FROM discord_users WHERE discord_id = ?";
            ps = conn.prepareStatement(checkSubSql);
            ps.setString(1, discordId);
            rs = ps.executeQuery();
            
            if (!rs.next()) {
                // Discord ID não tem assinatura
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
         PENDING_VERIFICATION,
         NOT_REGISTERED
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

    /**
     * FUNÇÃO DE TESTE: Verifica diretamente o banco de dados para debugging.
     * Executa queries detalhadas para identificar problemas.
     * 
     * @param playerUuid UUID do jogador para testar
     */
    private void debugDatabaseState(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        plugin.getLogger().info("[DEBUG-DB] 🔍 === INICIANDO DEBUG DO BANCO DE DADOS ===");
        plugin.getLogger().info("[DEBUG-DB] 🎯 UUID alvo: " + playerUuid);
        
        try {
            conn = PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("[DEBUG-DB] ❌ Conexão nula");
                return;
            }
            
            // TESTE 1: Verificar se o jogador existe em player_data
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 1: Verificando player_data...");
            String sql1 = "SELECT player_id, name, uuid FROM player_data WHERE uuid = ?";
            ps = conn.prepareStatement(sql1);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int playerId = rs.getInt("player_id");
                String name = rs.getString("name");
                String uuid = rs.getString("uuid");
                
                plugin.getLogger().info("[DEBUG-DB] ✅ player_data encontrado:");
                plugin.getLogger().info("[DEBUG-DB]    - player_id: " + playerId);
                plugin.getLogger().info("[DEBUG-DB]    - name: " + name);
                plugin.getLogger().info("[DEBUG-DB]    - uuid: " + uuid);
                plugin.getLogger().info("[DEBUG-DB]    - subscription_expires_at: (removido do player_data)");
            } else {
                plugin.getLogger().warning("[DEBUG-DB] ❌ player_data NÃO encontrado para UUID: " + playerUuid);
            }
            
            rs.close();
            ps.close();
            
            // TESTE 2: Verificar discord_links
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 2: Verificando discord_links...");
            String sql2 = "SELECT dl.*, pd.name FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ?";
            ps = conn.prepareStatement(sql2);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            boolean foundLink = false;
            while (rs.next()) {
                foundLink = true;
                int linkId = rs.getInt("link_id");
                int playerId = rs.getInt("player_id");
                String discordId = rs.getString("discord_id");
                boolean verified = rs.getBoolean("verified");
                String playerName = rs.getString("name");
                
                plugin.getLogger().info("[DEBUG-DB] ✅ discord_links encontrado:");
                plugin.getLogger().info("[DEBUG-DB]    - link_id: " + linkId);
                plugin.getLogger().info("[DEBUG-DB]    - player_id: " + playerId);
                plugin.getLogger().info("[DEBUG-DB]    - discord_id: " + discordId);
                plugin.getLogger().info("[DEBUG-DB]    - verified: " + verified);
                plugin.getLogger().info("[DEBUG-DB]    - player_name: " + playerName);
            }
            
            if (!foundLink) {
                plugin.getLogger().warning("[DEBUG-DB] ❌ discord_links NÃO encontrado para UUID: " + playerUuid);
            }
            
            rs.close();
            ps.close();
            
            // TESTE 3: Query exata que está falhando
            plugin.getLogger().info("[DEBUG-DB] 📋 TESTE 3: Query exata do isPlayerLinkedToDiscord...");
            String sql3 = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE";
            ps = conn.prepareStatement(sql3);
            ps.setString(1, playerUuid.toString());
            rs = ps.executeQuery();
            
            if (rs.next()) {
                int count = rs.getInt(1);
                plugin.getLogger().info("[DEBUG-DB] 📊 Resultado da query: " + count + " vínculos verificados");
            }
            
            rs.close();
            ps.close();
            
            plugin.getLogger().info("[DEBUG-DB] ✅ === DEBUG DO BANCO CONCLUÍDO ===");
            
        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG-DB] ❌ Erro durante debug: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[DEBUG-DB] ⚠️ Erro ao liberar recursos: " + e.getMessage());
            }
        }
    }
    
         /**
      * Verifica se um jogador tem verificação pendente (registrado mas não verificado).
      * 
      * @param playerUuid UUID do jogador
      * @return true se tem verificação pendente, false caso contrário
      */
     private boolean hasPendingVerification(UUID playerUuid) {
         Connection conn = null;
         PreparedStatement ps = null;
         ResultSet rs = null;
         
         try {
             // Obter conexão via API do Core
             conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
             if (conn == null) {
                 return false;
             }
             
             // Query para verificar se tem registro mas não está verificado
             String sql = "SELECT COUNT(*) FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = FALSE";
             ps = conn.prepareStatement(sql);
             ps.setString(1, playerUuid.toString());
             
             rs = ps.executeQuery();
             if (rs.next()) {
                 int count = rs.getInt(1);
                 return count > 0; // Tem registro pendente se count > 0
             }
             
             return false;
             
         } catch (Exception e) {
             plugin.getLogger().severe("[AUTH] Erro ao verificar verificação pendente para " + playerUuid + ": " + e.getMessage());
             return false;
             
         } finally {
             // Cleanup de recursos
             try {
                 if (rs != null) rs.close();
                 if (ps != null) ps.close();
                 if (conn != null) conn.close();
             } catch (Exception e) {
                 plugin.getLogger().warning("[AUTH] Erro ao fechar recursos: " + e.getMessage());
             }
         }
     }
     
     /**
      * Obtém o Discord ID de um jogador pelo UUID.
      * 
      * @param playerUuid UUID do jogador
      * @return Discord ID ou null se não encontrado
      */
     private String getDiscordIdByPlayerUuid(UUID playerUuid) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            // Obter conexão via API do Core
            conn = br.com.primeleague.core.PrimeLeagueCore.getInstance().getDataManager().getConnection();
            if (conn == null) {
                return null;
            }
            
            // Query para buscar Discord ID
            String sql = "SELECT dl.discord_id FROM discord_links dl JOIN player_data pd ON dl.player_id = pd.player_id WHERE pd.uuid = ? AND dl.verified = TRUE LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
            
            return null;
            
        } catch (Exception e) {
            plugin.getLogger().severe("[AUTH] Erro ao buscar Discord ID para " + playerUuid + ": " + e.getMessage());
            return null;
            
        } finally {
            // Cleanup de recursos
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[AUTH] Erro ao fechar recursos: " + e.getMessage());
            }
        }
    }
}