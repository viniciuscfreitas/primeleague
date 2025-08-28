package br.com.primeleague.p2p.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.managers.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Comando para recuperação de conta in-game.
 * 
 * Este comando permite aos jogadores iniciar o processo de recuperação
 * de conta quando não conseguem acessar o Discord.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class RecuperarCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final String API_URL = "http://localhost:8080";
    private final String API_TOKEN = "primeleague_api_token_2024";

    public RecuperarCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c❌ Este comando só pode ser usado por jogadores.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        String playerIp = player.getAddress().getAddress().getHostAddress();

        // Verificar se o jogador já está em processo de recuperação
        if (isPlayerInRecovery(playerName)) {
            player.sendMessage("§e⚠️ Você já possui um processo de recuperação ativo.");
            player.sendMessage("§7Use o código enviado no Discord para finalizar a recuperação.");
            return true;
        }

        // Verificar se o jogador possui vínculo Discord
        if (!hasDiscordLink(playerName)) {
            player.sendMessage("§c❌ Você não possui uma conta vinculada ao Discord.");
            player.sendMessage("§7Use §f/registrar §7no Discord para criar um vínculo primeiro.");
            return true;
        }

        // Iniciar processo de recuperação
        try {
            boolean success = initiateRecovery(playerName, playerIp);
            
            if (success) {
                // Kickar o jogador com instruções
                kickPlayerWithInstructions(player);
                
                // Log da ação
                plugin.getLogger().info("[RECUPERAR] Jogador " + playerName + " iniciou processo de recuperação");
                
            } else {
                player.sendMessage("§c❌ Erro ao iniciar processo de recuperação.");
                player.sendMessage("§7Tente novamente em alguns minutos ou contate um administrador.");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[RECUPERAR] Erro ao processar recuperação para " + playerName + ": " + e.getMessage());
            player.sendMessage("§c❌ Erro interno do sistema. Contate um administrador.");
        }

        return true;
    }

    /**
     * Verifica se o jogador já está em processo de recuperação.
     */
    private boolean isPlayerInRecovery(String playerName) {
        try {
            DataManager dataManager = PrimeLeagueAPI.getDataManager();
            Integer playerId = dataManager.getPlayerIdByName(playerName);
            
            if (playerId == null) {
                return false;
            }

            // Verificar se há códigos de recuperação ativos
            String discordId = dataManager.getDiscordIdByPlayerId(playerId);
            if (discordId == null) {
                return false;
            }

            // Fazer requisição para verificar status
            String response = makeApiRequest("GET", "/api/v1/recovery/status/" + discordId, null);
            
            if (response != null && response.contains("\"hasActiveBackupCodes\":true")) {
                return true;
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[RECUPERAR] Erro ao verificar status de recuperação: " + e.getMessage());
        }

        return false;
    }

    /**
     * Verifica se o jogador possui vínculo Discord ativo.
     */
    private boolean hasDiscordLink(String playerName) {
        try {
            DataManager dataManager = PrimeLeagueAPI.getDataManager();
            Integer playerId = dataManager.getPlayerIdByName(playerName);
            
            if (playerId == null) {
                return false;
            }

            String discordId = dataManager.getDiscordIdByPlayerId(playerId);
            return discordId != null;

        } catch (Exception e) {
            plugin.getLogger().warning("[RECUPERAR] Erro ao verificar vínculo Discord: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inicia o processo de recuperação gerando códigos de backup.
     */
    private boolean initiateRecovery(String playerName, String playerIp) {
        try {
            DataManager dataManager = PrimeLeagueAPI.getDataManager();
            Integer playerId = dataManager.getPlayerIdByName(playerName);
            
            if (playerId == null) {
                return false;
            }

            String discordId = dataManager.getDiscordIdByPlayerId(playerId);
            if (discordId == null) {
                return false;
            }

            // Gerar códigos de backup via API
            String payload = String.format(
                "{\"discordId\":\"%s\",\"ipAddress\":\"%s\"}",
                discordId, playerIp
            );

            String response = makeApiRequest("POST", "/api/v1/recovery/backup/generate", payload);
            
            if (response != null && response.contains("\"success\":true")) {
                // Marcar jogador como PENDING_RELINK
                dataManager.updateDiscordLinkStatus(discordId, "PENDING_RELINK");
                return true;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[RECUPERAR] Erro ao gerar códigos: " + e.getMessage());
        }

        return false;
    }

    /**
     * Kicka o jogador com instruções de recuperação.
     */
    private void kickPlayerWithInstructions(Player player) {
        String kickMessage = String.join("\n",
            "§c§l🛡️ PROCESSO DE RECUPERAÇÃO INICIADO",
            "",
            "§7Para finalizar a recuperação da sua conta:",
            "",
            "§e1. §7Acesse o Discord do servidor",
            "§e2. §7Use o comando §f/recuperacao §7para gerar códigos",
            "§e3. §7Use o comando §f/vincular <seu_nickname> <codigo> §7para re-vincular",
            "",
            "§c⚠️ IMPORTANTE:",
            "§7• Os códigos são válidos por 30 dias",
            "§7• Guarde-os em local seguro",
            "§7• Nunca compartilhe com ninguém",
            "",
            "§7Após re-vincular, você poderá entrar novamente no servidor.",
            "",
            "§7Em caso de dúvidas, contate um administrador."
        );

        player.kickPlayer(kickMessage);
    }

    /**
     * Faz uma requisição HTTP para a API Core.
     */
    private String makeApiRequest(String method, String endpoint, String payload) {
        try {
            URL url = new URL(API_URL + endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + API_TOKEN);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Enviar payload se fornecido
            if (payload != null) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Ler resposta
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                plugin.getLogger().warning("[RECUPERAR] API retornou código " + responseCode + " para " + endpoint);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[RECUPERAR] Erro na requisição HTTP: " + e.getMessage());
        }

        return null;
    }
}
