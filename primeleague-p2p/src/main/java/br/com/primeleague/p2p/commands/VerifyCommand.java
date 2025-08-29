package br.com.primeleague.p2p.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import br.com.primeleague.p2p.managers.LimboManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comando para verificação de códigos do Discord.
 * 
 * Permite que jogadores confirmem seu registro no Discord
 * digitando o código recebido no servidor. Integrado com
 * o sistema de limbo para jogadores pendentes.
 * 
 * @author PrimeLeague Team
 * @version 2.0.0
 */
public class VerifyCommand implements CommandExecutor {
    
    private final LimboManager limboManager;
    
    public VerifyCommand(LimboManager limboManager) {
        this.limboManager = limboManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar se é um jogador
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c❌ Este comando só pode ser usado por jogadores!");
            return true;
        }

        final Player player = (Player) sender;
        
        // BARRERA DE SEGURANÇA: Verificar se o perfil do jogador está carregado
        if (PrimeLeagueAPI.getDataManager().isLoading(player.getUniqueId())) {
            player.sendMessage("§cSeu perfil ainda está carregando. Tente novamente em um instante.");
            return true;
        }

        // Verificar argumentos
        if (args.length != 1) {
            player.sendMessage("§c❌ Uso correto: /verify <código>");
            player.sendMessage("§7💡 Digite o código de 6 dígitos recebido no Discord.");
            return true;
        }

        final String verifyCode = args[0];

        // Validar formato do código
        if (!verifyCode.matches("\\d{6}")) {
            player.sendMessage("§c❌ Código inválido! Use apenas 6 dígitos numéricos.");
            return true;
        }

        // Executar verificação de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(PrimeLeagueP2P.getInstance(), new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean success = verifyDiscordLink(player.getName(), verifyCode);
                    
                    // Retornar resultado para a thread principal
                    Bukkit.getScheduler().runTask(PrimeLeagueP2P.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Remover jogador do limbo
                                limboManager.removePlayerFromLimbo(player.getUniqueId());
                                
                                // VERIFICAR ASSINATURA IMEDIATAMENTE APÓS VERIFICAÇÃO
                                checkSubscriptionAndKickIfNeeded(player);
                                
                                // Notificar Discord sobre sucesso
                                notifyDiscordSuccess(player.getName());
                            } else {
                                player.sendMessage("§c❌ Código inválido ou expirado!");
                                player.sendMessage("§7💡 Verifique se digitou corretamente e se não expirou (5 minutos).");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    // Log do erro
                    PrimeLeagueP2P.getInstance().getLogger().severe("Erro ao verificar código: " + e.getMessage());
                    e.printStackTrace();
                    
                    // Retornar erro para o jogador
                    Bukkit.getScheduler().runTask(PrimeLeagueP2P.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            player.sendMessage("§c❌ Erro interno ao verificar código. Tente novamente.");
                        }
                    });
                }
            }
        });

        return true;
    }

    /**
     * Verifica o código de verificação no banco de dados.
     * 
     * @param playerName Nome do jogador
     * @param verifyCode Código de verificação
     * @return true se a verificação foi bem-sucedida, false caso contrário
     */
    private boolean verifyDiscordLink(String playerName, String verifyCode) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = PrimeLeagueAPI.getDataManager().getConnection();
            
            // Usar a procedure do banco de dados
            stmt = connection.prepareStatement("CALL VerifyDiscordLink(?, ?)");
            stmt.setString(1, playerName);
            stmt.setString(2, verifyCode);
            
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                boolean success = rs.getBoolean("success");
                
                if (success) {
                    String discordId = rs.getString("discord_id");
                    String playerId = rs.getString("player_id");
                    
                    // Log da verificação bem-sucedida
                    PrimeLeagueP2P.getInstance().getLogger().info(
                        "Verificação bem-sucedida: " + playerName + " -> Discord ID: " + discordId
                    );
                    
                    return true;
                }
            }
            
            return false;
            
        } catch (SQLException e) {
            PrimeLeagueP2P.getInstance().getLogger().severe("Erro SQL ao verificar código: " + e.getMessage());
            return false;
        } finally {
            // Fechar recursos
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                PrimeLeagueP2P.getInstance().getLogger().warning("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    /**
     * Verifica a assinatura do jogador e kicka se não tiver assinatura ativa.
     * 
     * @param player Jogador para verificar
     */
    private void checkSubscriptionAndKickIfNeeded(final Player player) {
        // Executar verificação de assinatura de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(PrimeLeagueP2P.getInstance(), new Runnable() {
            @Override
            public void run() {
                try {
                    // Obter UUID canônico do jogador
                    final UUID canonicalUuid = br.com.primeleague.core.util.UUIDUtils.offlineUUIDFromName(player.getName());
                    
                    // Verificar assinatura
                    final AccountStatus status = checkAccountSubscription(canonicalUuid);
                    
                    // Retornar resultado para a thread principal
                    Bukkit.getScheduler().runTask(PrimeLeagueP2P.getInstance(), new Runnable() {
                        @Override
                        public void run() {
                            switch (status.getStatus()) {
                                case ACTIVE:
                                    // Assinatura ativa - permitir acesso
                                    player.sendMessage("");
                                    player.sendMessage("§a§l🎉 VERIFICAÇÃO CONCLUÍDA COM SUCESSO!");
                                    player.sendMessage("");
                                    player.sendMessage("§f✅ Sua conta Discord foi vinculada!");
                                    player.sendMessage("§f✅ Sua assinatura está ativa!");
                                    player.sendMessage("§f🎮 Bem-vindo ao Prime League!");
                                    player.sendMessage("");
                                    player.sendMessage("§a🔄 Você pode jogar normalmente!");
                                    break;
                                    
                                case EXPIRED:
                                    // Assinatura expirada - kickar
                                    player.sendMessage("");
                                    player.sendMessage("§a§l✅ VERIFICAÇÃO CONCLUÍDA!");
                                    player.sendMessage("");
                                    player.sendMessage("§f✅ Sua conta Discord foi vinculada!");
                                    player.sendMessage("§c❌ Sua assinatura expirou.");
                                    player.sendMessage("");
                                    player.sendMessage("§6§l📋 RENOVAÇÃO NECESSÁRIA:");
                                    player.sendMessage("§fPara continuar jogando, renove sua assinatura.");
                                    player.sendMessage("");
                                    player.sendMessage("§e💎 Como renovar:");
                                    player.sendMessage("§7• Vá para o Discord: §fdiscord.gg/primeleague");
                                    player.sendMessage("§7• Use o comando §a/assinatura §7no Discord");
                                    player.sendMessage("§7• Escolha seu plano e complete o pagamento");
                                    player.sendMessage("");
                                    
                                                                         // Kick após 3 segundos
                                     Bukkit.getScheduler().runTaskLater(PrimeLeagueP2P.getInstance(), new Runnable() {
                                         @Override
                                         public void run() {
                                             player.kickPlayer("§a✅ Verificação Concluída!\n\n" +
                                                              "§fConta Discord vinculada!\n" +
                                                              "§c❌ Assinatura Expirada\n\n" +
                                                              "§e💎 Renove: discord.gg/primeleague\n" +
                                                              "§e💎 Comando: /assinatura");
                                         }
                                     }, 60L); // 3 segundos
                                    break;
                                    
                                case NEVER_SUBSCRIBED:
                                    // Nunca teve assinatura - kickar
                                    player.sendMessage("");
                                    player.sendMessage("§a§l✅ VERIFICAÇÃO CONCLUÍDA!");
                                    player.sendMessage("");
                                    player.sendMessage("§f✅ Sua conta Discord foi vinculada!");
                                    player.sendMessage("§c❌ Você não possui assinatura ativa.");
                                    player.sendMessage("");
                                    player.sendMessage("§6§l📋 ASSINATURA NECESSÁRIA:");
                                    player.sendMessage("§fPara acessar o servidor, adquira uma assinatura.");
                                    player.sendMessage("");
                                    player.sendMessage("§e💎 Como adquirir:");
                                    player.sendMessage("§7• Vá para o Discord: §fdiscord.gg/primeleague");
                                    player.sendMessage("§7• Use o comando §a/assinatura §7no Discord");
                                    player.sendMessage("§7• Escolha seu plano e complete o pagamento");
                                    player.sendMessage("");
                                    
                                                                         // Kick após 3 segundos
                                     Bukkit.getScheduler().runTaskLater(PrimeLeagueP2P.getInstance(), new Runnable() {
                                         @Override
                                         public void run() {
                                             player.kickPlayer("§a✅ Verificação Concluída!\n\n" +
                                                              "§fConta Discord vinculada!\n" +
                                                              "§c❌ Assinatura Necessária\n\n" +
                                                              "§e💎 Adquira: discord.gg/primeleague\n" +
                                                              "§e💎 Comando: /assinatura");
                                         }
                                     }, 60L); // 3 segundos
                                    break;
                                    
                                default:
                                    // Status desconhecido - kickar por segurança
                                    player.sendMessage("");
                                    player.sendMessage("§a§l✅ VERIFICAÇÃO CONCLUÍDA!");
                                    player.sendMessage("");
                                    player.sendMessage("§f✅ Sua conta Discord foi vinculada!");
                                    player.sendMessage("§c❌ Erro ao verificar assinatura.");
                                    player.sendMessage("");
                                    player.sendMessage("§6§l📋 CONTATE A ADMINISTRAÇÃO:");
                                    player.sendMessage("§fEntre em contato no Discord para resolver.");
                                    player.sendMessage("");
                                    
                                                                         // Kick após 3 segundos
                                     Bukkit.getScheduler().runTaskLater(PrimeLeagueP2P.getInstance(), new Runnable() {
                                         @Override
                                         public void run() {
                                             player.kickPlayer("§a✅ Verificação Concluída!\n\n" +
                                                              "§fConta Discord vinculada!\n" +
                                                              "§c❌ Erro na Verificação\n\n" +
                                                              "§e💎 Contato: discord.gg/primeleague");
                                         }
                                     }, 60L); // 3 segundos
                                    break;
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    // Log do erro
                    PrimeLeagueP2P.getInstance().getLogger().severe("Erro ao verificar assinatura: " + e.getMessage());
                    e.printStackTrace();
                    
                                         // Kick por segurança em caso de erro
                     Bukkit.getScheduler().runTask(PrimeLeagueP2P.getInstance(), new Runnable() {
                         @Override
                         public void run() {
                             player.kickPlayer("§c❌ Erro na Verificação\n\n" +
                                              "§fErro ao verificar assinatura.\n" +
                                              "§e💎 Contato: discord.gg/primeleague");
                         }
                     });
                }
            }
        });
    }
    
    /**
     * Verifica o status de assinatura de uma conta.
     * 
     * @param playerUuid UUID do jogador
     * @return AccountStatus com informações da assinatura
     */
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
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
            }
            
            boolean isVerified = rs.getBoolean("verified");
            rs.close();
            ps.close();
            
            if (!isVerified) {
                // Vinculado mas não verificado
                return new AccountStatus(SubscriptionStatus.NEVER_SUBSCRIBED, 0);
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
            
            java.sql.Timestamp expiresAt = rs.getTimestamp("subscription_expires_at");
            
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
            PrimeLeagueP2P.getInstance().getLogger().severe("Erro ao verificar assinatura para " + playerUuid + ": " + e.getMessage());
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
        NEVER_SUBSCRIBED
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
     * Notifica o Discord sobre a verificação bem-sucedida.
     * 
     * @param playerName Nome do jogador verificado
     */
    private void notifyDiscordSuccess(String playerName) {
        try {
            Connection connection = PrimeLeagueAPI.getDataManager().getConnection();
            
            // Criar notificação para o Discord
            String sql = "INSERT INTO server_notifications (action_type, payload) VALUES (?, ?)";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("player_name", playerName);
            payload.put("verified_at", System.currentTimeMillis());
            payload.put("status", "success");
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, "DISCORD_VERIFY_SUCCESS");
            stmt.setString(2, new com.google.gson.Gson().toJson(payload));
            
            stmt.executeUpdate();
            
            stmt.close();
            connection.close();
            
        } catch (SQLException e) {
            PrimeLeagueP2P.getInstance().getLogger().warning("Erro ao notificar Discord: " + e.getMessage());
        }
    }
}
