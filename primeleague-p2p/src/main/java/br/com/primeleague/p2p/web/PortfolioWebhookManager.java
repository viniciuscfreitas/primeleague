package br.com.primeleague.p2p.web;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Gerenciador do servidor webhook para o Sistema de Portfólio de Contas.
 *
 * Sistema simplificado que processa pagamentos individuais por conta específica.
 * Fonte da verdade: player_data.subscription_expires_at
 * 
 * REFATORADO para usar DataManager normalizado:
 * - Usa DataManager para buscar jogadores e atualizar assinaturas
 * - Remove queries diretas ao banco de dados
 * - Usa métodos normalizados para operações de dados
 * - Compatível com schema normalizado
 *
 * @author PrimeLeague Team
 * @version 3.1.0 (Refatorado para DataManager normalizado)
 */
public final class PortfolioWebhookManager {

    private final String webhookSecret;
    private final PrimeLeagueP2P plugin;
    private HttpServer server;
    private final Gson gson = new Gson();

    public PortfolioWebhookManager(String secret) {
        this.webhookSecret = secret;
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    /**
     * Inicia o servidor webhook na porta especificada.
     */
    public void startServer(int port) throws IOException {
        if (server != null) {
            plugin.getLogger().warning("Servidor webhook já está executando!");
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/webhook", new WebhookHandler());
        server.setExecutor(null); // Usa o executor padrão
        server.start();

        plugin.getLogger().info("✅ Servidor webhook do portfólio iniciado na porta " + port);
    }

    /**
     * Para o servidor webhook.
     */
    public void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
            plugin.getLogger().info("🛑 Servidor webhook do portfólio encerrado");
        }
    }

    /**
     * Handler principal do webhook.
     */
    private class WebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                // Verificar autenticação
                String authHeader = exchange.getRequestHeaders().getFirst("X-Webhook-Secret");
                if (authHeader == null || !webhookSecret.equals(authHeader)) {
                    plugin.getLogger().warning("Tentativa de acesso não autorizada ao webhook");
                    sendResponse(exchange, 401, "Unauthorized");
                    return;
                }

                // Ler o corpo da requisição
                String requestBody = readRequestBody(exchange.getRequestBody());
                
                // Parse do JSON
                JsonParser parser = new JsonParser();
                JsonObject payload = parser.parse(requestBody).getAsJsonObject();

                // Processar notificação (assíncrono)
                processNotificationAsync(payload);

                // Resposta de sucesso
                JsonObject response = new JsonObject();
                response.addProperty("status", "success");
                response.addProperty("message", "Notificação processada com sucesso");
                sendResponse(exchange, 200, gson.toJson(response));

            } catch (Exception e) {
                plugin.getLogger().severe("Erro ao processar webhook: " + e.getMessage());
                e.printStackTrace();

                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("status", "error");
                errorResponse.addProperty("message", "Erro interno do servidor");
                sendResponse(exchange, 500, gson.toJson(errorResponse));
            }
        }
    }

    /**
     * Processa a notificação de forma assíncrona.
     */
    private void processNotificationAsync(final JsonObject payload) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    String status = payload.get("status").getAsString();
                    String discordId = payload.get("discord_id").getAsString();
                    String playerName = payload.get("player_name").getAsString();
                    int subscriptionDays = payload.get("subscription_days").getAsInt();

                    if ("approved".equals(status)) {
                        processApprovedPayment(discordId, playerName, subscriptionDays);
                    } else if ("pending".equals(status)) {
                        plugin.getLogger().info("💰 Pagamento pendente: " + playerName);
                    } else if ("cancelled".equals(status)) {
                        plugin.getLogger().info("❌ Pagamento cancelado: " + playerName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro ao processar notificação: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Processa um pagamento aprovado no Sistema de Portfólio.
     * REFATORADO: Usa DataManager normalizado para todas as operações.
     */
    private void processApprovedPayment(String discordId, String playerName, int subscriptionDays) {
        try {
            // REFATORADO: Usar DataManager para buscar jogador
            PlayerProfile playerProfile = PrimeLeagueAPI.getDataManager().loadOfflinePlayerProfile(playerName);
            if (playerProfile == null) {
                plugin.getLogger().warning("❌ Jogador não encontrado: " + playerName);
                return;
            }

            UUID playerUuid = playerProfile.getUuid();

            // REFATORADO: Calcular nova data de expiração usando DataManager
            Date newExpiryDate = calculateNewExpiryDate(playerProfile, subscriptionDays);

            // REFATORADO: Atualizar assinatura usando DataManager
            boolean success = updateAccountSubscription(playerUuid, newExpiryDate);

            if (success) {
                plugin.getLogger().info(String.format(
                    "✅ Assinatura renovada: %s (%s) +%d dias → %s",
                    playerName, playerUuid.toString().substring(0, 8), subscriptionDays, newExpiryDate
                ));

                // 4. Notificar Discord sobre a renovação
                notifyDiscordAccountRenewal(discordId, playerName, subscriptionDays, newExpiryDate);
            } else {
                plugin.getLogger().warning("❌ Falha ao renovar assinatura: " + playerName);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Erro crítico no processamento de pagamento: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Calcula a nova data de expiração baseada na assinatura atual.
     * REFATORADO: Usa PlayerProfile do DataManager.
     * 
     * @param playerProfile Perfil do jogador carregado via DataManager
     * @param daysToAdd Dias a adicionar à assinatura
     * @return Nova data de expiração
     */
    private Date calculateNewExpiryDate(PlayerProfile playerProfile, int daysToAdd) {
        Calendar calendar = Calendar.getInstance();
        
        Date currentExpiry = playerProfile.getSubscriptionExpiry();
        
        if (currentExpiry == null || currentExpiry.before(new Date())) {
            // Assinatura expirada ou nunca teve - começar de agora
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
        } else {
            // Assinatura ativa - estender a partir da data atual de expiração
            calendar.setTime(currentExpiry);
            calendar.add(Calendar.DAY_OF_YEAR, daysToAdd);
        }
        
        return calendar.getTime();
    }

    /**
     * Atualiza a assinatura da conta usando DataManager.
     * REFATORADO: Usa métodos do DataManager refatorado.
     * 
     * @param playerUuid UUID do jogador
     * @param newExpiryDate Nova data de expiração
     * @return true se atualizado com sucesso, false caso contrário
     */
    private boolean updateAccountSubscription(UUID playerUuid, Date newExpiryDate) {
        try {
            // REFATORADO: Usar método do DataManager para atualizar assinatura
            boolean success = PrimeLeagueAPI.getDataManager().updateSubscriptionExpiry(playerUuid, newExpiryDate);
            
            if (success) {
                // Atualizar cache se o jogador estiver online
                PlayerProfile cachedProfile = PrimeLeagueAPI.getDataManager().getPlayerProfileFromCache(playerUuid);
                if (cachedProfile != null) {
                    cachedProfile.setSubscriptionExpiry(newExpiryDate);
                }
            }
            
            return success;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao atualizar assinatura da conta: " + e.getMessage());
            return false;
        }
    }

    /**
     * Notifica o Discord sobre a renovação da conta.
     */
    private void notifyDiscordAccountRenewal(String discordId, String playerName, int days, Date expiryDate) {
        try {
            // Criar notificação para o bot Discord
            String notificationData = String.format(
                "{\"discord_id\":\"%s\",\"player_name\":\"%s\",\"days_added\":%d,\"expires_at\":\"%s\"}",
                discordId, playerName, days, expiryDate.toString()
            );

            createServerNotification("ACCOUNT_SUBSCRIPTION_RENEWED", notificationData);

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao notificar Discord: " + e.getMessage());
        }
    }

    /**
     * Cria uma notificação para o servidor Discord.
     * REFATORADO: Mantém query direta apenas para esta tabela específica.
     */
    private void createServerNotification(String type, String data) {
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) return;

            ps = conn.prepareStatement(
                "INSERT INTO server_notifications (action_type, target_player, payload) VALUES (?, ?, ?)"
            );
            ps.setString(1, type);
            ps.setString(2, null); // target_player não aplicável para este tipo
            ps.setString(3, data);
            ps.executeUpdate();

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao criar notificação do servidor: " + e.getMessage());
        } finally {
            closeResources(null, ps, conn);
        }
    }

    // ========================================
    // MÉTODOS UTILITÁRIOS
    // ========================================

    private String readRequestBody(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void closeResources(java.sql.ResultSet rs, PreparedStatement ps, Connection conn) {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (Exception e) {
            // Silenciar erros de cleanup
        }
    }
}
