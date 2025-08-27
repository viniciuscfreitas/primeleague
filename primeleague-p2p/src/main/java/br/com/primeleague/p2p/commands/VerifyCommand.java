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
                                
                                // Mensagens de sucesso
                                player.sendMessage("");
                                player.sendMessage("§a§l✅ VERIFICAÇÃO CONCLUÍDA!");
                                player.sendMessage("");
                                player.sendMessage("§fSua conta Discord foi vinculada com sucesso!");
                                player.sendMessage("§7🎮 Agora você pode usar todos os comandos do Discord.");
                                player.sendMessage("");
                                player.sendMessage("§e⚠️ PRÓXIMO PASSO:");
                                player.sendMessage("§fAdquira uma assinatura para acessar o servidor.");
                                player.sendMessage("§7Use §a/renovar §7no Discord para mais informações.");
                                player.sendMessage("");
                                
                                // Kick jogador para forçar nova conexão (agora como INATIVO)
                                Bukkit.getScheduler().runTaskLater(PrimeLeagueP2P.getInstance(), new Runnable() {
                                    @Override
                                    public void run() {
                                        player.kickPlayer("§a✅ Verificação concluída!\n\n" +
                                                         "§fAdquira uma assinatura no Discord e\n" +
                                                         "§fconecte novamente para acessar o servidor.\n\n" +
                                                         "§7Use /renovar no Discord para mais informações.");
                                    }
                                }, 60L); // 3 segundos
                                
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
