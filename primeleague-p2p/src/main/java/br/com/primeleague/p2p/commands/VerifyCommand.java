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
                                
                                // UX MELHORADA: Mensagens de sucesso mais claras e tempo para leitura
                                player.sendMessage("");
                                player.sendMessage("§a§l🎉 VERIFICAÇÃO CONCLUÍDA COM SUCESSO!");
                                player.sendMessage("");
                                player.sendMessage("§f✅ Sua conta Discord foi vinculada!");
                                player.sendMessage("§f🎮 Agora você pode usar todos os comandos do Discord.");
                                player.sendMessage("");
                                player.sendMessage("§6§l📋 PRÓXIMO PASSO NECESSÁRIO:");
                                player.sendMessage("§fPara acessar o servidor, você precisa de uma assinatura.");
                                player.sendMessage("");
                                player.sendMessage("§e💎 Como adquirir:");
                                player.sendMessage("§7• Vá para o Discord: §fdiscord.gg/primeleague");
                                player.sendMessage("§7• Use o comando §a/assinatura §7no Discord");
                                player.sendMessage("§7• Escolha seu plano e complete o pagamento");
                                player.sendMessage("");
                                player.sendMessage("§a🔄 Após adquirir a assinatura, conecte novamente!");
                                player.sendMessage("");
                                
                                // Kick jogador após tempo suficiente para leitura
                                Bukkit.getScheduler().runTaskLater(PrimeLeagueP2P.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        player.kickPlayer("§a§l✅ Verificação Concluída!\n\n" +
                                                         "§fSua conta Discord foi vinculada com sucesso!\n\n" +
                                                         "§6§l📋 Próximo Passo:\n" +
                                                         "§fAdquira uma assinatura no Discord para acessar o servidor.\n\n" +
                                                         "§e💎 Discord: §fdiscord.gg/primeleague\n" +
                                                         "§e💎 Comando: §f/assinatura\n\n" +
                                                         "§a🔄 Conecte novamente após adquirir a assinatura!");
                                    }
                                }, 120L); // 6 segundos para leitura completa
                                
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
                    String playerUuid = rs.getString("player_uuid");
                    
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
     * Notifica o Discord sobre a verificação bem-sucedida.
     * 
     * @param playerName Nome do jogador verificado
     */
    private void notifyDiscordSuccess(String playerName) {
        try {
            Connection connection = PrimeLeagueAPI.getDataManager().getConnection();
            
            // Criar notificação para o Discord
            String sql = "INSERT INTO server_notifications (action_type, target_player, payload) VALUES (?, ?, ?)";
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("player_name", playerName);
            payload.put("verified_at", System.currentTimeMillis());
            payload.put("status", "success");
            
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, "DISCORD_VERIFY_SUCCESS");
            stmt.setString(2, playerName);
            stmt.setString(3, new com.google.gson.Gson().toJson(payload));
            
            stmt.executeUpdate();
            
            stmt.close();
            connection.close();
            
        } catch (SQLException e) {
            PrimeLeagueP2P.getInstance().getLogger().warning("Erro ao notificar Discord: " + e.getMessage());
        }
    }
}
