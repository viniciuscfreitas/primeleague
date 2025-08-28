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

        // Verificar se foi fornecido um código de backup
        if (args.length != 1) {
            player.sendMessage("§c❌ Uso correto: §f/recuperar <codigo_backup>");
            player.sendMessage("§7Use um dos códigos de backup que você salvou anteriormente.");
            return true;
        }

        String backupCode = args[0];

        // Validar formato do código (8 caracteres alfanuméricos)
        if (!backupCode.matches("^[A-Z0-9]{8}$")) {
            player.sendMessage("§c❌ Código inválido. O código deve ter 8 caracteres (letras e números).");
            return true;
        }

        // Verificar se o jogador já está em processo de recuperação
        if (isPlayerInRecovery(playerName)) {
            player.sendMessage("§e⚠️ Você já possui um processo de recuperação ativo.");
            player.sendMessage("§7Use o código de re-vinculação que apareceu no chat para finalizar.");
            return true;
        }

        // Processar recuperação de emergência
        try {
            String relinkCode = processEmergencyRecovery(playerName, backupCode, playerIp);
            
            if (relinkCode != null) {
                // Log da ação
                plugin.getLogger().info("[RECUPERAR] Jogador " + playerName + " iniciou recuperação de emergência");
                
                // Mostrar código de re-vinculação no chat
                showRelinkCode(player, relinkCode);
                
            } else {
                player.sendMessage("§c❌ Código de backup inválido ou expirado.");
                player.sendMessage("§7Verifique o código e tente novamente.");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("[RECUPERAR] Erro ao processar recuperação para " + playerName + ": " + e.getMessage());
            player.sendMessage("§c❌ Erro interno do sistema. Contate um administrador.");
        }

        return true;
    }

    /**
     * Verifica se o jogador já está em processo de recuperação (PENDING_RELINK).
     */
    private boolean isPlayerInRecovery(String playerName) {
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

            // Verificar se está em estado PENDING_RELINK
            String status = dataManager.getDiscordLinkStatus(discordId);
            return "PENDING_RELINK".equals(status);

        } catch (Exception e) {
            plugin.getLogger().warning("[RECUPERAR] Erro ao verificar status de recuperação: " + e.getMessage());
        }

        return false;
    }

    /**
     * Processa a recuperação de emergência usando código de backup.
     */
    private String processEmergencyRecovery(String playerName, String backupCode, String playerIp) {
        try {
            // Verificar o código de backup via API
            String payload = String.format(
                "{\"playerName\":\"%s\",\"backupCode\":\"%s\",\"ipAddress\":\"%s\"}",
                playerName, backupCode, playerIp
            );

            String response = makeApiRequest("POST", "/api/v1/recovery/verify", payload);
            
            if (response != null && response.contains("\"success\":true")) {
                // Extrair código de re-vinculação da resposta
                String relinkCode = extractRelinkCodeFromResponse(response);
                if (relinkCode != null) {
                    return relinkCode;
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[RECUPERAR] Erro ao processar recuperação de emergência: " + e.getMessage());
        }

        return null;
    }

    /**
     * Mostra o código de re-vinculação no chat do jogador.
     */
    private void showRelinkCode(Player player, String relinkCode) {
        String playerName = player.getName();
        
        // Enviar mensagem no chat
        player.sendMessage("§a§l✅ RECUPERAÇÃO DE EMERGÊNCIA CONCLUÍDA!");
        player.sendMessage("");
        player.sendMessage("§e🔑 Seu código de re-vinculação é: §f§l" + relinkCode);
        player.sendMessage("");
        player.sendMessage("§7📱 Use este código no Discord:");
        player.sendMessage("§7💬 Comando: §f/vincular " + playerName + " " + relinkCode);
        player.sendMessage("");
        player.sendMessage("§c⚠️ IMPORTANTE:");
        player.sendMessage("§7• Este código é válido por 5 minutos");
        player.sendMessage("§7• Use-o para re-vincular sua conta no Discord");
        player.sendMessage("§7• Após re-vincular, sua conta estará protegida novamente");
        
        // Log do código recebido
        plugin.getLogger().info("[RECUPERAR] Código de re-vinculação recebido para " + playerName + ": " + relinkCode);
    }

    /**
     * Extrai o código de re-vinculação da resposta da API.
     */
    private String extractRelinkCodeFromResponse(String response) {
        try {
            // Buscar "relinkCode":"CODIGO" na resposta JSON
            int startIndex = response.indexOf("\"relinkCode\":\"");
            if (startIndex != -1) {
                startIndex += 14; // Comprimento de "relinkCode":"
                int endIndex = response.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return response.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[RECUPERAR] Erro ao extrair código da resposta: " + e.getMessage());
        }
        return null;
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
