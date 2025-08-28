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
    private final String bearerToken;
    
    public HttpApiManager(PrimeLeagueCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataManager = plugin.getDataManager();
        this.donorManager = plugin.getDonorManager();
        this.port = plugin.getConfig().getInt("api.port", 8080);
        this.bearerToken = plugin.getConfig().getString("api.security.bearer_token", "primeleague_api_token_2024");
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
            server.createContext("/api/v1/ip-authorize", new IpAuthorizeHandler());
            
            // Endpoints de recuperação de conta P2P
            server.createContext("/api/v1/recovery/backup/generate", new RecoveryBackupGenerateHandler());
            server.createContext("/api/v1/recovery/verify", new RecoveryVerifyHandler());
            server.createContext("/api/v1/recovery/status", new RecoveryStatusHandler());
            server.createContext("/api/v1/recovery/audit", new RecoveryAuditHandler());
            
            // Endpoints de transferência de assinaturas (FASE 2)
            server.createContext("/api/v1/discord/transfer", new DiscordTransferHandler());
            
            // Endpoints de desvinculação e re-vinculação (FASE 2)
            server.createContext("/api/v1/account/unlink", new AccountUnlinkHandler());
            server.createContext("/api/v1/recovery/complete-relink", new CompleteRelinkHandler());
            
            // Configurar executor
            server.setExecutor(Executors.newFixedThreadPool(10));
            
            // Iniciar servidor
            server.start();
            
            logger.info("[Core] API HTTP iniciada na porta " + port);
            
        } catch (IOException e) {
            logger.severe("Erro ao iniciar API HTTP: " + e.getMessage());
        }
    }
    
    /**
     * Para o servidor HTTP
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            logger.info("[Core] API HTTP parada");
        }
    }
    
    /**
     * Verifica autenticação Bearer Token
     */
    private boolean authenticateRequest(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        
        logger.info("[AUTH] Verificando autenticação para: " + exchange.getRequestURI().getPath());
        logger.info("[AUTH] Authorization header: " + (authHeader != null ? "presente" : "ausente"));
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warning("[AUTH] Header Authorization inválido ou ausente");
            return false;
        }
        
        String token = authHeader.substring(7); // Remove "Bearer "
        boolean isValid = bearerToken.equals(token);
        
        logger.info("[AUTH] Token válido: " + isValid);
        
        return isValid;
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
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
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized - Invalid or missing Bearer token");
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized - Invalid or missing Bearer token");
                return;
            }
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Ler dados do request (compatível com Java 7)
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                // Executar limpeza de cache de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        // Limpar cache do DataManager
                        dataManager.clearCache();
                        logger.info("Cache limpo automaticamente após criação de player via Discord");
                        
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized - Invalid or missing Bearer token");
                return;
            }
            
            String response = "{\"status\":\"ok\",\"service\":\"PrimeLeague Core API v2.0\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";
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
     * Envia resposta simples
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String response) {
        try {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } catch (IOException e) {
            logger.severe("Erro ao enviar resposta: " + e.getMessage());
        }
    }
    
    /**
     * Configura headers CORS
     */
    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    /**
     * Lê o corpo da requisição HTTP (compatível com Java 7)
     */
    private String readRequestBody(HttpExchange exchange) {
        try {
            java.io.InputStream inputStream = exchange.getRequestBody();
            java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        } catch (IOException e) {
            logger.severe("Erro ao ler corpo da requisição: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtém o IP do cliente
     */
    private String getClientIpAddress(HttpExchange exchange) {
        String xForwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequestHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }
    
    /**
     * Extrai valor de campo JSON simples
     */
    private String extractJsonValue(String json, String field) {
        try {
            String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1);
            }
            
            // Tentar extrair boolean
            pattern = "\"" + field + "\"\\s*:\\s*(true|false)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            
            if (m.find()) {
                return m.group(1);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Handler para autorização de IP via Discord
     * POST /api/v1/ip-authorize
     */
    private class IpAuthorizeHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized - Invalid or missing Bearer token");
                return;
            }
            
            try {
                // Ler dados do request (compatível com Java 7)
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                // Executar autorização de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        IpAuthorizeRequest request = parseIpAuthorizeRequest(requestBody);
                        
                        if (request == null) {
                            sendErrorResponse(exchange, 400, "Payload inválido");
                            return;
                        }
                        
                        // Processar autorização
                        boolean success = processIpAuthorization(request);
                        
                        // ==========================================================
                        //  CORREÇÃO OBRIGATÓRIA: Verificar retorno do processamento
                        // ==========================================================
                        if (success) {
                            String response = "{\"success\":true,\"message\":\"IP autorizado com sucesso\"}";
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            // Se processIpAuthorization retornou false, envie um erro 500
                            sendErrorResponse(exchange, 500, "Erro interno ao processar a autorização no servidor.");
                        }
                        // ==========================================================
                        //  FIM DA CORREÇÃO OBRIGATÓRIA
                        // ==========================================================
                        
                    } catch (Exception e) {
                        logger.severe("Erro ao processar autorização de IP: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno do servidor");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("Erro no handler de autorização de IP: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno do servidor");
            }
        }
        
        /**
         * Processa autorização de IP
         * CORREÇÃO: Tratamento correto de exceções SQL para garantir consistência
         */
        private boolean processIpAuthorization(IpAuthorizeRequest request) {
            try {
                if (request.isAuthorized()) {
                    // Autorizar IP permanentemente no banco
                    dataManager.authorizeIpPermanently(request.getPlayerName(), request.getIpAddress());
                    logger.info("[IP-AUTH] IP " + request.getIpAddress() + " autorizado para " + request.getPlayerName() + " via Discord");
                    
                    // ATUALIZAR CACHE EM TEMPO REAL (CORREÇÃO CRÍTICA)
                    updateP2PCache(request.getPlayerName(), request.getIpAddress());
                    
                } else {
                    // Registrar rejeição (para auditoria)
                    logger.info("[IP-AUTH] IP " + request.getIpAddress() + " rejeitado para " + request.getPlayerName() + " via Discord");
                }
                
                return true;
                
            } catch (java.sql.SQLException e) {
                logger.severe("🚨 [IP-AUTH] Erro SQL ao processar autorização de IP: " + e.getMessage());
                return false;
            } catch (Exception e) {
                logger.severe("🚨 [IP-AUTH] Erro inesperado ao processar autorização de IP: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * Atualiza o cache do P2P em tempo real
         * CORREÇÃO: Resolve race condition entre Core e P2P usando reflexão
         */
        private void updateP2PCache(String playerName, String ipAddress) {
            try {
                // Buscar plugin P2P via Bukkit usando reflexão para evitar dependência circular
                org.bukkit.plugin.Plugin p2pPlugin = Bukkit.getPluginManager().getPlugin("PrimeLeague-P2P");
                if (p2pPlugin != null) {
                    // Usar reflexão para acessar métodos do P2P
                    Class<?> p2pClass = p2pPlugin.getClass();
                    java.lang.reflect.Method getCacheMethod = p2pClass.getMethod("getIpAuthCache");
                    Object ipAuthCache = getCacheMethod.invoke(p2pPlugin);
                    
                    if (ipAuthCache != null) {
                        // Usar reflexão para chamar addAuthorizedIp
                        Class<?> cacheClass = ipAuthCache.getClass();
                        java.lang.reflect.Method addMethod = cacheClass.getMethod("addAuthorizedIp", String.class, String.class);
                        addMethod.invoke(ipAuthCache, playerName, ipAddress);
                        
                        logger.info("[IP-AUTH] ✅ Cache P2P atualizado em tempo real: " + playerName + " (" + ipAddress + ")");
                    } else {
                        logger.warning("[IP-AUTH] ⚠️ Cache P2P não disponível para atualização");
                    }
                } else {
                    logger.warning("[IP-AUTH] ⚠️ Plugin P2P não encontrado para atualização de cache");
                }
                
            } catch (Exception e) {
                logger.severe("[IP-AUTH] ❌ Erro ao atualizar cache P2P: " + e.getMessage());
            }
        }
        
        /**
         * Parse do JSON de autorização de IP
         */
        private IpAuthorizeRequest parseIpAuthorizeRequest(String json) {
            try {
                // Parse simples do JSON (sem dependências externas)
                if (!json.contains("\"playerName\"") || !json.contains("\"ipAddress\"") || 
                    !json.contains("\"authorized\"") || !json.contains("\"discordId\"")) {
                    return null;
                }
                
                // Extrair valores usando regex simples
                String playerName = extractJsonValue(json, "playerName");
                String ipAddress = extractJsonValue(json, "ipAddress");
                String authorizedStr = extractJsonValue(json, "authorized");
                String discordId = extractJsonValue(json, "discordId");
                
                if (playerName == null || ipAddress == null || authorizedStr == null || discordId == null) {
                    return null;
                }
                
                boolean authorized = Boolean.parseBoolean(authorizedStr);
                
                return new IpAuthorizeRequest(playerName, ipAddress, authorized, discordId);
                
            } catch (Exception e) {
                logger.severe("Erro ao fazer parse do JSON de autorização: " + e.getMessage());
                return null;
            }
        }
        
    }
    
    /**
     * Classe de requisição para autorização de IP
     */
    public static class IpAuthorizeRequest {
        private final String playerName;
        private final String ipAddress;
        private final boolean authorized;
        private final String discordId;
        
        public IpAuthorizeRequest(String playerName, String ipAddress, boolean authorized, String discordId) {
            this.playerName = playerName;
            this.ipAddress = ipAddress;
            this.authorized = authorized;
            this.discordId = discordId;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public String getIpAddress() { return ipAddress; }
        public boolean isAuthorized() { return authorized; }
        public String getDiscordId() { return discordId; }
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
    
    // =====================================================
    // HANDLERS DE RECUPERAÇÃO DE CONTA P2P
    // =====================================================
    
    /**
     * Handler para geração de códigos de backup
     */
    private class RecoveryBackupGenerateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Ler dados do request (compatível com Java 7)
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                // Executar geração de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        RecoveryBackupGenerateRequest request = parseRecoveryBackupGenerateRequest(requestBody);
                        if (request == null) {
                            sendErrorResponse(exchange, 400, "Dados de requisição inválidos");
                            return;
                        }
                        
                        // Processar geração de códigos
                        boolean success = processRecoveryBackupGeneration(request);
                        
                        if (success) {
                            String response = "{\"success\":true,\"message\":\"Códigos de backup gerados com sucesso\"}";
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            // Verificar se é erro de Discord ID não encontrado
                            Integer playerId = dataManager.getPlayerIdByDiscordId(request.getDiscordId());
                            if (playerId == null) {
                                sendErrorResponse(exchange, 404, "Discord ID não encontrado");
                            } else {
                                sendErrorResponse(exchange, 500, "Erro interno ao gerar códigos de backup");
                            }
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[RECOVERY] Erro ao processar geração de códigos: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao ler requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao ler a requisição");
            }
        }
        
        /**
         * Processa geração de códigos de backup
         */
        private boolean processRecoveryBackupGeneration(RecoveryBackupGenerateRequest request) {
            try {
                // Buscar player_id pelo discord_id
                Integer playerId = dataManager.getPlayerIdByDiscordId(request.getDiscordId());
                if (playerId == null) {
                    logger.warning("[RECOVERY] Discord ID não encontrado: " + request.getDiscordId());
                    return false;
                }
                
                // Gerar códigos de backup
                java.util.List<String> codes = plugin.getRecoveryCodeManager().generateBackupCodes(
                    playerId, request.getDiscordId(), request.getIpAddress());
                
                logger.info("[RECOVERY] Códigos de backup gerados para Discord ID: " + request.getDiscordId());
                return true;
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao gerar códigos: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * Faz parse da requisição de geração de códigos
         */
        private RecoveryBackupGenerateRequest parseRecoveryBackupGenerateRequest(String json) {
            try {
                String discordId = extractJsonValue(json, "discordId");
                String ipAddress = extractJsonValue(json, "ipAddress");
                
                if (discordId == null || ipAddress == null) {
                    return null;
                }
                
                return new RecoveryBackupGenerateRequest(discordId, ipAddress);
                
            } catch (Exception e) {
                logger.severe("Erro ao fazer parse do JSON de geração: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Handler para verificação de códigos de recuperação
     */
    private class RecoveryVerifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Ler dados do request (compatível com Java 7)
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                // Executar verificação de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        RecoveryVerifyRequest request = parseRecoveryVerifyRequest(requestBody);
                        if (request == null) {
                            sendErrorResponse(exchange, 400, "Dados de requisição inválidos");
                            return;
                        }
                        
                        // Processar verificação de código
                        RecoveryVerificationResult result = processRecoveryVerification(request);
                        
                        if (result.isSuccess()) {
                            String response = String.format(
                                "{\"success\":true,\"message\":\"Código validado com sucesso. A conta está pronta para ser revinculada.\",\"relinkCode\":\"%s\"}",
                                result.getRelinkCode()
                            );
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            sendErrorResponse(exchange, 400, "Código inválido, expirado ou já utilizado");
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[RECOVERY] Erro ao processar verificação: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao ler requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao ler a requisição");
            }
        }
        
        /**
         * Processa verificação de código de recuperação
         */
        private RecoveryVerificationResult processRecoveryVerification(RecoveryVerifyRequest request) {
            try {
                // Verificar código
                boolean isValid = plugin.getRecoveryCodeManager().verifyCode(
                    request.getPlayerName(), request.getBackupCode(), request.getIpAddress());
                
                if (isValid) {
                    // Buscar discord_id do jogador
                    String discordId = dataManager.getDiscordIdByPlayerName(request.getPlayerName());
                    if (discordId != null) {
                        // Marcar estado como PENDING_RELINK
                        dataManager.updateDiscordLinkStatus(discordId, "PENDING_RELINK");
                        logger.info("[RECOVERY] Estado alterado para PENDING_RELINK para: " + request.getPlayerName());
                        
                        // Gerar código temporário de re-vinculação
                        String relinkCode = plugin.getRecoveryCodeManager().generateTemporaryRelinkCode(
                            request.getPlayerName(), discordId, request.getIpAddress());
                        
                        return new RecoveryVerificationResult(true, relinkCode);
                    }
                }
                
                return new RecoveryVerificationResult(false, null);
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao verificar código: " + e.getMessage());
                return new RecoveryVerificationResult(false, null);
            }
        }
        
        /**
         * Faz parse da requisição de verificação
         */
        private RecoveryVerifyRequest parseRecoveryVerifyRequest(String json) {
            try {
                String playerName = extractJsonValue(json, "playerName");
                String backupCode = extractJsonValue(json, "backupCode");
                String ipAddress = extractJsonValue(json, "ipAddress");
                
                if (playerName == null || backupCode == null || ipAddress == null) {
                    return null;
                }
                
                return new RecoveryVerifyRequest(playerName, backupCode, ipAddress);
                
            } catch (Exception e) {
                logger.severe("Erro ao fazer parse do JSON de verificação: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Handler para status de códigos de recuperação
     */
    private class RecoveryStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 400, "Token de autenticação inválido");
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equals("GET")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Extrair discord_id da URL
                String path = exchange.getRequestURI().getPath();
                String discordId = path.substring(path.lastIndexOf("/") + 1);
                
                if (discordId == null || discordId.isEmpty()) {
                    sendErrorResponse(exchange, 400, "Discord ID não fornecido");
                    return;
                }
                
                // Executar consulta de status de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        RecoveryStatusResponse status = getRecoveryStatus(discordId);
                        
                        if (status != null) {
                            String response = String.format(
                                "{\"success\":true,\"hasActiveBackupCodes\":%s,\"codesGeneratedAt\":\"%s\",\"activeCodeCount\":%d}",
                                status.hasActiveBackupCodes(), status.getCodesGeneratedAt(), status.getActiveCodeCount()
                            );
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            sendErrorResponse(exchange, 404, "Discord ID não encontrado");
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[RECOVERY] Erro ao consultar status: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao consultar status");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao processar requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
            }
        }
        
        /**
         * Obtém status dos códigos de recuperação
         */
        private RecoveryStatusResponse getRecoveryStatus(String discordId) {
            try {
                // Buscar player_id pelo discord_id
                Integer playerId = dataManager.getPlayerIdByDiscordId(discordId);
                if (playerId == null) {
                    return null;
                }
                
                // Consultar códigos ativos
                try (java.sql.Connection conn = dataManager.getDataSource().getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(
                         "SELECT COUNT(*) as count, MAX(created_at) as last_created " +
                         "FROM recovery_codes " +
                         "WHERE player_id = ? AND status = 'ACTIVE' AND code_type = 'BACKUP'")) {
                    
                    stmt.setInt(1, playerId);
                    
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int count = rs.getInt("count");
                            java.sql.Timestamp lastCreated = rs.getTimestamp("last_created");
                            
                            return new RecoveryStatusResponse(
                                count > 0,
                                lastCreated != null ? lastCreated.toString() : null,
                                count
                            );
                        }
                    }
                }
                
                return new RecoveryStatusResponse(false, null, 0);
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao consultar status: " + e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Handler para auditoria de códigos de recuperação
     */
    private class RecoveryAuditHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Configurar CORS
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            // Verificar autenticação
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            // Verificar método HTTP
            if (!exchange.getRequestMethod().equals("GET")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                // Extrair discord_id da URL
                String path = exchange.getRequestURI().getPath();
                String discordId = path.substring(path.lastIndexOf("/") + 1);
                
                if (discordId == null || discordId.isEmpty()) {
                    sendErrorResponse(exchange, 400, "Discord ID não fornecido");
                    return;
                }
                
                // Executar consulta de auditoria de forma assíncrona
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        String auditData = getRecoveryAudit(discordId);
                        
                        if (auditData != null) {
                            sendJsonResponse(exchange, 200, auditData);
                        } else {
                            sendErrorResponse(exchange, 404, "Discord ID não encontrado");
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[RECOVERY] Erro ao consultar auditoria: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao consultar auditoria");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao processar requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
            }
        }
        
        /**
         * Obtém dados de auditoria
         */
        private String getRecoveryAudit(String discordId) {
            try {
                // Buscar player_id pelo discord_id
                Integer playerId = dataManager.getPlayerIdByDiscordId(discordId);
                if (playerId == null) {
                    return null;
                }
                
                // Consultar tentativas recentes
                try (java.sql.Connection conn = dataManager.getDataSource().getConnection();
                     java.sql.PreparedStatement stmt = conn.prepareStatement(
                         "SELECT rc.*, pd.name as player_name " +
                         "FROM recovery_codes rc " +
                         "JOIN player_data pd ON rc.player_id = pd.player_id " +
                         "WHERE rc.player_id = ? " +
                         "ORDER BY rc.created_at DESC " +
                         "LIMIT 10")) {
                    
                    stmt.setInt(1, playerId);
                    
                    StringBuilder json = new StringBuilder();
                    json.append("{\"success\":true,\"recentAttempts\":[");
                    
                    boolean first = true;
                    try (java.sql.ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            if (!first) json.append(",");
                            first = false;
                            
                            json.append("{");
                            json.append("\"id\":").append(rs.getLong("id")).append(",");
                            json.append("\"codeType\":\"").append(rs.getString("code_type")).append("\",");
                            json.append("\"status\":\"").append(rs.getString("status")).append("\",");
                            json.append("\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\",");
                            json.append("\"usedAt\":\"").append(rs.getTimestamp("used_at")).append("\",");
                            json.append("\"attempts\":").append(rs.getInt("attempts")).append(",");
                            json.append("\"ipAddress\":\"").append(rs.getString("ip_address")).append("\"");
                            json.append("}");
                        }
                    }
                    
                    json.append("]}");
                    return json.toString();
                    
                }
                
            } catch (Exception e) {
                logger.severe("[RECOVERY] Erro ao consultar auditoria: " + e.getMessage());
                return null;
            }
        }
    }
    
    // =====================================================
    // CLASSES DE REQUISIÇÃO E RESPOSTA
    // =====================================================
    
    /**
     * Classe de requisição para geração de códigos de backup
     */
    public static class RecoveryBackupGenerateRequest {
        private final String discordId;
        private final String ipAddress;
        
        public RecoveryBackupGenerateRequest(String discordId, String ipAddress) {
            this.discordId = discordId;
            this.ipAddress = ipAddress;
        }
        
        // Getters
        public String getDiscordId() { return discordId; }
        public String getIpAddress() { return ipAddress; }
    }
    
    /**
     * Classe de requisição para verificação de código
     */
    public static class RecoveryVerifyRequest {
        private final String playerName;
        private final String backupCode;
        private final String ipAddress;
        
        public RecoveryVerifyRequest(String playerName, String backupCode, String ipAddress) {
            this.playerName = playerName;
            this.backupCode = backupCode;
            this.ipAddress = ipAddress;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public String getBackupCode() { return backupCode; }
        public String getIpAddress() { return ipAddress; }
    }
    
    /**
     * Classe de resposta para status de códigos
     */
    public static class RecoveryStatusResponse {
        private final boolean hasActiveBackupCodes;
        private final String codesGeneratedAt;
        private final int activeCodeCount;
        
        public RecoveryStatusResponse(boolean hasActiveBackupCodes, String codesGeneratedAt, int activeCodeCount) {
            this.hasActiveBackupCodes = hasActiveBackupCodes;
            this.codesGeneratedAt = codesGeneratedAt;
            this.activeCodeCount = activeCodeCount;
        }
        
        // Getters
        public boolean hasActiveBackupCodes() { return hasActiveBackupCodes; }
        public String getCodesGeneratedAt() { return codesGeneratedAt; }
        public int getActiveCodeCount() { return activeCodeCount; }
    }
    
    // =====================================================
    // HANDLER DE DESVINCULAÇÃO DE CONTA (FASE 2)
    // =====================================================
    
    /**
     * Handler para desvinculação proativa de conta
     * POST /api/v1/account/unlink
     */
    private class AccountUnlinkHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        AccountUnlinkRequest request = parseAccountUnlinkRequest(requestBody);
                        if (request == null) {
                            sendErrorResponse(exchange, 400, "Dados de requisição inválidos");
                            return;
                        }
                        
                        AccountUnlinkResult result = processAccountUnlink(request);
                        
                        if (result.isSuccess()) {
                            String response = String.format(
                                "{\"success\":true,\"message\":\"Conta desvinculada com sucesso. Use o código para re-vincular.\",\"relinkCode\":\"%s\"}",
                                result.getRelinkCode()
                            );
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            sendErrorResponse(exchange, 400, result.getMessage());
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[UNLINK] Erro ao processar desvinculação: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[UNLINK] Erro ao ler requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao ler a requisição");
            }
        }
        
        /**
         * Processa desvinculação de conta
         */
        private AccountUnlinkResult processAccountUnlink(AccountUnlinkRequest request) {
            try {
                // Verificar se o jogador existe e está vinculado
                String discordId = dataManager.getDiscordIdByPlayerName(request.getPlayerName());
                if (discordId == null) {
                    return new AccountUnlinkResult(false, "Jogador não encontrado ou não vinculado", null);
                }
                
                // Marcar como PENDING_RELINK
                dataManager.updateDiscordLinkStatus(discordId, "PENDING_RELINK");
                logger.info("[UNLINK] Estado alterado para PENDING_RELINK para: " + request.getPlayerName());
                
                // Gerar código temporário de re-vinculação
                String relinkCode = plugin.getRecoveryCodeManager().generateTemporaryRelinkCode(
                    request.getPlayerName(), discordId, request.getIpAddress());
                
                return new AccountUnlinkResult(true, "Desvinculação realizada com sucesso", relinkCode);
                
            } catch (Exception e) {
                logger.severe("[UNLINK] Erro ao processar desvinculação: " + e.getMessage());
                return new AccountUnlinkResult(false, "Erro interno ao processar desvinculação", null);
            }
        }
        
        /**
         * Parse da requisição de desvinculação
         */
        private AccountUnlinkRequest parseAccountUnlinkRequest(String json) {
            try {
                String playerName = extractJsonValue(json, "playerName");
                String ipAddress = extractJsonValue(json, "ipAddress");
                
                if (playerName == null || ipAddress == null) {
                    return null;
                }
                
                return new AccountUnlinkRequest(playerName, ipAddress);
                
            } catch (Exception e) {
                logger.severe("[UNLINK] Erro ao fazer parse da requisição: " + e.getMessage());
                return null;
            }
        }
    }
    
    // =====================================================
    // HANDLER DE RE-VINCULAÇÃO TRANSACIONAL (FASE 2)
    // =====================================================
    
    /**
     * Handler para re-vinculação transacional (verificação + transferência)
     * POST /api/v1/recovery/complete-relink
     */
    private class CompleteRelinkHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            try {
                java.io.InputStream inputStream = exchange.getRequestBody();
                java.io.ByteArrayOutputStream result = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                String requestBody = result.toString("UTF-8");
                
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        CompleteRelinkRequest request = parseCompleteRelinkRequest(requestBody);
                        if (request == null) {
                            sendErrorResponse(exchange, 400, "Dados de requisição inválidos");
                            return;
                        }
                        
                        CompleteRelinkResult result = processCompleteRelink(request);
                        
                        if (result.isSuccess()) {
                            String response = "{\"success\":true,\"message\":\"" + result.getMessage() + "\"}";
                            sendJsonResponse(exchange, 200, response);
                        } else {
                            sendErrorResponse(exchange, 400, result.getMessage());
                        }
                        
                    } catch (Exception e) {
                        logger.severe("[COMPLETE-RELINK] Erro ao processar re-vinculação: " + e.getMessage());
                        sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
                    }
                });
                
            } catch (Exception e) {
                logger.severe("[COMPLETE-RELINK] Erro ao ler requisição: " + e.getMessage());
                sendErrorResponse(exchange, 500, "Erro interno ao ler a requisição");
            }
        }
        
        /**
         * Processa re-vinculação transacional
         */
        private CompleteRelinkResult processCompleteRelink(CompleteRelinkRequest request) {
            try {
                // 1. Verificar código temporário
                boolean isValidCode = plugin.getRecoveryCodeManager().verifyTemporaryRelinkCode(
                    request.getPlayerName(), request.getRelinkCode(), request.getIpAddress());
                
                if (!isValidCode) {
                    return new CompleteRelinkResult(false, "Código de re-vinculação inválido ou expirado");
                }
                
                // 2. Executar transferência de assinatura
                DataManager.TransferResult transferResult = dataManager.transferSubscription(
                    request.getPlayerName(), 
                    request.getNewDiscordId(), 
                    request.getIpAddress()
                );
                
                if (!transferResult.isSuccess()) {
                    return new CompleteRelinkResult(false, transferResult.getMessage());
                }
                
                // 3. Finalizar vínculo (remover PENDING_RELINK)
                String discordId = dataManager.getDiscordIdByPlayerName(request.getPlayerName());
                if (discordId != null) {
                    dataManager.updateDiscordLinkStatus(discordId, "ACTIVE");
                    logger.info("[COMPLETE-RELINK] Estado alterado para ACTIVE para: " + request.getPlayerName());
                }
                
                return new CompleteRelinkResult(true, "Conta re-vinculada com sucesso");
                
            } catch (Exception e) {
                logger.severe("[COMPLETE-RELINK] Erro ao processar re-vinculação: " + e.getMessage());
                return new CompleteRelinkResult(false, "Erro interno ao processar re-vinculação");
            }
        }
        
        /**
         * Parse da requisição de re-vinculação
         */
        private CompleteRelinkRequest parseCompleteRelinkRequest(String json) {
            try {
                String playerName = extractJsonValue(json, "playerName");
                String relinkCode = extractJsonValue(json, "relinkCode");
                String newDiscordId = extractJsonValue(json, "newDiscordId");
                String ipAddress = extractJsonValue(json, "ipAddress");
                
                if (playerName == null || relinkCode == null || newDiscordId == null || ipAddress == null) {
                    return null;
                }
                
                return new CompleteRelinkRequest(playerName, relinkCode, newDiscordId, ipAddress);
                
            } catch (Exception e) {
                logger.severe("[COMPLETE-RELINK] Erro ao fazer parse da requisição: " + e.getMessage());
                return null;
            }
        }
    }
    
    // =====================================================
    // HANDLER DE TRANSFERÊNCIA DE ASSINATURAS (FASE 2)
    // =====================================================
    
    /**
     * Handler para transferência de assinaturas entre Discord IDs
     * POST /api/v1/discord/transfer
     */
    private class DiscordTransferHandler implements HttpHandler {
        
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            setCorsHeaders(exchange);
            
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (!authenticateRequest(exchange)) {
                sendErrorResponse(exchange, 401, "Token de autenticação inválido");
                return;
            }
            
            if (!exchange.getRequestMethod().equals("POST")) {
                sendErrorResponse(exchange, 405, "Método não permitido");
                return;
            }
            
            // Ler corpo da requisição
            String requestBody = readRequestBody(exchange);
            if (requestBody == null) {
                sendErrorResponse(exchange, 400, "Corpo da requisição inválido");
                return;
            }
            
            // Processar em thread assíncrona
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    DiscordTransferRequest request = parseDiscordTransferRequest(requestBody);
                    if (request == null) {
                        sendErrorResponse(exchange, 400, "Dados de requisição inválidos");
                        return;
                    }
                    
                    // Obter IP de origem
                    String ipAddress = getClientIpAddress(exchange);
                    
                    // Executar transferência
                    DataManager.TransferResult result = dataManager.transferSubscription(
                        request.getPlayerName(), 
                        request.getNewDiscordId(), 
                        ipAddress
                    );
                    
                    if (result.isSuccess()) {
                        String response = "{\"success\":true,\"message\":\"" + result.getMessage() + "\"}";
                        sendJsonResponse(exchange, 200, response);
                    } else {
                        sendErrorResponse(exchange, 400, result.getMessage());
                    }
                    
                } catch (Exception e) {
                    logger.severe("[TRANSFER] Erro ao processar transferência: " + e.getMessage());
                    sendErrorResponse(exchange, 500, "Erro interno ao processar a requisição");
                }
            });
        }
        
        /**
         * Processa requisição de transferência
         */
        private boolean processDiscordTransfer(DiscordTransferRequest request) {
            try {
                // Obter IP de origem (simulado para teste)
                String ipAddress = "127.0.0.1";
                
                DataManager.TransferResult result = dataManager.transferSubscription(
                    request.getPlayerName(), 
                    request.getNewDiscordId(), 
                    ipAddress
                );
                
                return result.isSuccess();
                
            } catch (Exception e) {
                logger.severe("[TRANSFER] Erro ao processar transferência: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * Parse da requisição de transferência
         */
        private DiscordTransferRequest parseDiscordTransferRequest(String requestBody) {
            try {
                String playerName = extractJsonValue(requestBody, "playerName");
                String newDiscordId = extractJsonValue(requestBody, "newDiscordId");
                
                if (playerName == null || newDiscordId == null) {
                    return null;
                }
                
                return new DiscordTransferRequest(playerName, newDiscordId);
                
            } catch (Exception e) {
                logger.severe("[TRANSFER] Erro ao fazer parse da requisição: " + e.getMessage());
                return null;
            }
        }
    }
    
    // =====================================================
    // CLASSES DE REQUISIÇÃO PARA TRANSFERÊNCIA
    // =====================================================
    
    /**
     * Classe de requisição para transferência de assinatura
     */
    public static class DiscordTransferRequest {
        private final String playerName;
        private final String newDiscordId;
        
        public DiscordTransferRequest(String playerName, String newDiscordId) {
            this.playerName = playerName;
            this.newDiscordId = newDiscordId;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public String getNewDiscordId() { return newDiscordId; }
    }
    
    // =====================================================
    // CLASSES DE REQUISIÇÃO E RESULTADO PARA DESVINCULAÇÃO
    // =====================================================
    
    /**
     * Classe de requisição para desvinculação de conta
     */
    public static class AccountUnlinkRequest {
        private final String playerName;
        private final String ipAddress;
        
        public AccountUnlinkRequest(String playerName, String ipAddress) {
            this.playerName = playerName;
            this.ipAddress = ipAddress;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public String getIpAddress() { return ipAddress; }
    }
    
    /**
     * Classe de resultado para desvinculação de conta
     */
    public static class AccountUnlinkResult {
        private final boolean success;
        private final String message;
        private final String relinkCode;
        
        public AccountUnlinkResult(boolean success, String message, String relinkCode) {
            this.success = success;
            this.message = message;
            this.relinkCode = relinkCode;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRelinkCode() { return relinkCode; }
    }
    
    // =====================================================
    // CLASSES DE REQUISIÇÃO E RESULTADO PARA RE-VINCULAÇÃO
    // =====================================================
    
    /**
     * Classe de requisição para re-vinculação transacional
     */
    public static class CompleteRelinkRequest {
        private final String playerName;
        private final String relinkCode;
        private final String newDiscordId;
        private final String ipAddress;
        
        public CompleteRelinkRequest(String playerName, String relinkCode, String newDiscordId, String ipAddress) {
            this.playerName = playerName;
            this.relinkCode = relinkCode;
            this.newDiscordId = newDiscordId;
            this.ipAddress = ipAddress;
        }
        
        // Getters
        public String getPlayerName() { return playerName; }
        public String getRelinkCode() { return relinkCode; }
        public String getNewDiscordId() { return newDiscordId; }
        public String getIpAddress() { return ipAddress; }
    }
    
    /**
     * Classe de resultado para re-vinculação transacional
     */
    public static class CompleteRelinkResult {
        private final boolean success;
        private final String message;
        
        public CompleteRelinkResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    // =====================================================
    // CLASSES DE RESULTADO PARA RECUPERAÇÃO
    // =====================================================
    
    /**
     * Classe de resultado para verificação de recuperação
     */
    public static class RecoveryVerificationResult {
        private final boolean success;
        private final String relinkCode;
        
        public RecoveryVerificationResult(boolean success, String relinkCode) {
            this.success = success;
            this.relinkCode = relinkCode;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getRelinkCode() { return relinkCode; }
    }
}
