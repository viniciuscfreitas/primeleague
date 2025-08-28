package br.com.primeleague.core;

import br.com.primeleague.core.managers.DataManager;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.core.managers.DonorManager;
import br.com.primeleague.core.models.DonorLevel;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * HttpApiManager - Servidor HTTP para API do Core
 * 
 * Fornece endpoints HTTP para integração com serviços externos
 * como o bot do Discord, eliminando duplicação de lógica de negócio.
 */
public class HttpApiManager {
    
    private final PrimeLeagueCore plugin;
    private final Logger logger;
    private final DataManager dataManager;
    private final DonorManager donorManager;
    private HttpServer server;
    private int port;
    
    public HttpApiManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataManager = plugin.getDataManager();
        this.donorManager = plugin.getDonorManager();
        this.port = plugin.getConfig().getInt("api.port", 8080);
    }
    
    /**
     * Inicia o servidor HTTP da API
     */
    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Registrar endpoints
            server.createContext("/api/donor-info", new DonorInfoHandler());
            server.createContext("/api/health", new HealthHandler());
            server.createContext("/api/player-created", new PlayerCreatedHandler());
            
            // Configurar executor
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            // Iniciar servidor
            server.start();
            
            logger.info("✅ API HTTP iniciada na porta " + port);
            
        } catch (IOException e) {
            logger.severe("❌ Erro ao iniciar API HTTP: " + e.getMessage());
        }
    }
    
    /**
     * Para o servidor HTTP
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("🛑 API HTTP parada");
        }
    }
    
    /**
     * Handler para informações de doador
     * GET /api/donor-info/{discordId}
     */
    private class DonorInfoHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            // Responder a requisições OPTIONS (CORS preflight)
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Extrair discordId da URL
                String path = exchange.getRequestURI().getPath();
                String[] pathParts = path.split("/");
                
                if (pathParts.length < 4) {
                    sendErrorResponse(exchange, 400, "Discord ID não fornecido");
                    return;
                }
                
                String discordId = pathParts[3];
                
                // Executar lógica de negócio de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        DonorInfoResponse response = getDonorInfo(discordId);
                        sendJsonResponse(exchange, 200, response.toJson());
                    } catch (Exception e) {
                        logger.severe("Erro ao processar requisição de doador: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno do servidor");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("Erro no handler de doador: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno do servidor");
            }
        }
        
        /**
         * Busca informações de doador para um Discord ID
         */
        private DonorInfoResponse getDonorInfo(String discordId) {
            try {
                // 1. Buscar donor tier diretamente da tabela discord_users
                int donorTier = dataManager.getDonorTierByDiscordId(discordId);
                
                // 2. Obter informações do nível de doador
                DonorLevel donorLevel = donorManager.getDonorLevelById(donorTier);
                
                if (donorLevel == null) {
                    // Nível não encontrado - usar padrão
                    return new DonorInfoResponse(donorTier, "Player", 1, 0);
                }
                
                // 3. Contar contas atuais vinculadas
                int currentAccounts = dataManager.getDiscordLinkCount(discordId);
                
                // 4. Montar resposta
                return new DonorInfoResponse(
                    donorTier,
                    donorLevel.getName(),
                    donorLevel.getMaxAltAccounts(),
                    currentAccounts
                );
                
            } catch (Exception e) {
                logger.severe("Erro ao buscar informações de doador: " + e.getMessage());
                return new DonorInfoResponse(0, "Player", 1, 0);
            }
        }
    }
    
    /**
     * Handler para notificação de player criado
     * POST /api/player-created
     */
    private class PlayerCreatedHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Ler dados do request
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                
                // Executar limpeza de cache de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        // Limpar cache do DataManager
                        dataManager.clearCache();
                        logger.info("🔄 Cache limpo automaticamente após criação de player via Discord");
                        
                        // Enviar resposta de sucesso
                        String response = "{\"success\":true,\"message\":\"Cache limpo com sucesso\"}";
                        sendJsonResponse(exchange, 200, response);
                        
                    } catch (Exception e) {
                        logger.severe("Erro ao limpar cache: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno do servidor");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("Erro no handler de player criado: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno do servidor");
            }
        }
    }
    
    /**
     * Handler para verificação de saúde da API
     * GET /api/health
     */
    private class HealthHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            String response = "{\"status\":\"ok\",\"service\":\"PrimeLeague Core API\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
            sendJsonResponse(exchange, 200, response);
        }
    }
    
    /**
     * Envia resposta JSON
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) {
        try {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IOException e) {
            logger.severe("Erro ao enviar resposta JSON: " + e.getMessage());
        }
    }
    
    /**
     * Envia resposta de erro
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) {
        try {
            String errorJson = "{\"error\":\"" + message + "\",\"status\":" + statusCode + "}";
            sendJsonResponse(exchange, statusCode, errorJson);
        } catch (Exception e) {
            logger.severe("Erro ao enviar resposta de erro: " + e.getMessage());
        }
    }
    
    /**
     * Classe de resposta para informações de doador
     */
    public static class DonorInfoResponse {
        private final int donorTier;
        private final String donorName;
        private final int maxAltAccounts;
        private final int currentAccounts;
        
        public DonorInfoResponse(int donorTier, String donorName, int maxAltAccounts, int currentAccounts) {
            this.donorTier = donorTier;
            this.donorName = donorName;
            this.maxAltAccounts = maxAltAccounts;
            this.currentAccounts = currentAccounts;
        }
        
        public String toJson() {
            return String.format(
                "{\"donorTier\":%d,\"donorName\":\"%s\",\"maxAltAccounts\":%d,\"currentAccounts\":%d}",
                donorTier, donorName, maxAltAccounts, currentAccounts
            );
        }
        
        // Getters
        public int getDonorTier() { return donorTier; }
        public String getDonorName() { return donorName; }
        public int getMaxAltAccounts() { return maxAltAccounts; }
        public int getCurrentAccounts() { return currentAccounts; }
    }
}
