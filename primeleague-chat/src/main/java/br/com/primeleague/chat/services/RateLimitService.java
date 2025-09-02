package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de controle de rate limiting para prevenir spam no chat.
 * Implementa controle de frequência de mensagens e cooldown para mensagens idênticas.
 */
public class RateLimitService {
    
    private final PrimeLeagueChat plugin;
    
    // Cache de última mensagem por jogador (thread-safe)
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();
    
    // Configurações de rate limiting
    private final int minIntervalMs;
    private final int identicalMessageCooldownMs;
    private final int maxMessagesPerMinute;
    
    public RateLimitService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        
        // Carregar configurações do config.yml
        this.minIntervalMs = plugin.getConfig().getInt("rate_limiting.min_interval_ms", 1000); // 1 segundo
        this.identicalMessageCooldownMs = plugin.getConfig().getInt("rate_limiting.identical_cooldown_ms", 5000); // 5 segundos
        this.maxMessagesPerMinute = plugin.getConfig().getInt("rate_limiting.max_messages_per_minute", 30); // 30 mensagens/min
        
        plugin.getLogger().info("🔒 Rate Limit Service inicializado:");
        plugin.getLogger().info("   ⏱️  Intervalo mínimo: " + minIntervalMs + "ms");
        plugin.getLogger().info("   🔄 Cooldown mensagens idênticas: " + identicalMessageCooldownMs + "ms");
        plugin.getLogger().info("   📊 Máximo por minuto: " + maxMessagesPerMinute + " mensagens");
    }
    
    /**
     * Verifica se uma mensagem pode ser enviada baseada no rate limiting.
     * 
     * @param player Jogador tentando enviar a mensagem
     * @param message Conteúdo da mensagem
     * @return Resultado da verificação com detalhes
     */
    public RateLimitResult checkRateLimit(Player player, String message) {
        UUID playerUuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // DEBUG: Log de entrada
        plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Verificando rate limit:");
        plugin.getLogger().info("   👤 Jogador: " + player.getName() + " (UUID: " + playerUuid + ")");
        plugin.getLogger().info("   📝 Mensagem: " + message);
        plugin.getLogger().info("   ⏰ Timestamp atual: " + currentTime);
        
        // Verificar se o jogador tem permissão para bypassar rate limiting
        if (PrimeLeagueAPI.hasPermission(player, "primeleague.chat.bypass_rate_limit")) {
            plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Bypass permitido para " + player.getName());
            return new RateLimitResult(true, "Bypass permitido", 0);
        }
        
        // 1. Verificar intervalo mínimo entre mensagens
        Long lastTime = lastMessageTime.get(playerUuid);
        plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Última mensagem: " + lastTime);
        
        if (lastTime != null) {
            long timeSinceLastMessage = currentTime - lastTime;
            plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Tempo desde última mensagem: " + timeSinceLastMessage + "ms");
            plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Intervalo mínimo: " + minIntervalMs + "ms");
            
            if (timeSinceLastMessage < minIntervalMs) {
                long remainingCooldown = minIntervalMs - timeSinceLastMessage;
                
                plugin.getLogger().info("🚫 [RATE-LIMIT] Intervalo mínimo violado:");
                plugin.getLogger().info("   👤 Jogador: " + player.getName());
                plugin.getLogger().info("   ⏱️  Tempo desde última mensagem: " + timeSinceLastMessage + "ms");
                plugin.getLogger().info("   ⏳ Cooldown restante: " + remainingCooldown + "ms");
                
                return new RateLimitResult(false, 
                    "§cAguarde " + (remainingCooldown / 1000.0) + " segundos antes de enviar outra mensagem.", 
                    remainingCooldown);
            }
        }
        
        // 2. Verificar cooldown para mensagens idênticas
        String lastMessage = lastMessageContent.get(playerUuid);
        plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Última mensagem: " + lastMessage);
        
        if (lastMessage != null && lastMessage.equals(message)) {
            Long lastIdenticalTime = lastMessageTime.get(playerUuid);
            if (lastIdenticalTime != null) {
                long timeSinceIdentical = currentTime - lastIdenticalTime;
                plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Tempo desde mensagem idêntica: " + timeSinceIdentical + "ms");
                plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Cooldown idênticas: " + identicalMessageCooldownMs + "ms");
                
                if (timeSinceIdentical < identicalMessageCooldownMs) {
                    long remainingCooldown = identicalMessageCooldownMs - timeSinceIdentical;
                    
                    plugin.getLogger().info("🚫 [RATE-LIMIT] Mensagem idêntica detectada:");
                    plugin.getLogger().info("   👤 Jogador: " + player.getName());
                    plugin.getLogger().info("   📝 Mensagem: " + message);
                    plugin.getLogger().info("   ⏳ Cooldown restante: " + remainingCooldown + "ms");
                    
                    return new RateLimitResult(false, 
                        "§cAguarde " + (remainingCooldown / 1000.0) + " segundos antes de repetir a mesma mensagem.", 
                        remainingCooldown);
                }
            }
        }
        
        // 3. Verificar limite de mensagens por minuto (implementação simplificada)
        // TODO: Implementar contador de mensagens por minuto com limpeza automática
        
        // Mensagem permitida - atualizar timestamps
        lastMessageTime.put(playerUuid, currentTime);
        lastMessageContent.put(playerUuid, message);
        
        plugin.getLogger().info("🔍 [RATE-LIMIT-DEBUG] Mensagem permitida para " + player.getName());
        
        return new RateLimitResult(true, "Mensagem permitida", 0);
    }
    
    /**
     * Limpa o histórico de rate limiting de um jogador.
     * Útil quando o jogador sai do servidor ou é punido.
     */
    public void clearPlayerHistory(UUID playerUuid) {
        lastMessageTime.remove(playerUuid);
        lastMessageContent.remove(playerUuid);
    }
    
    /**
     * Limpa todo o histórico de rate limiting.
     * Útil para manutenção ou reset do sistema.
     */
    public void clearAllHistory() {
        lastMessageTime.clear();
        lastMessageContent.clear();
        plugin.getLogger().info("🧹 Histórico de rate limiting limpo");
    }
    
    /**
     * Resultado da verificação de rate limiting.
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        private final long remainingCooldownMs;
        
        public RateLimitResult(boolean allowed, String message, long remainingCooldownMs) {
            this.allowed = allowed;
            this.message = message;
            this.remainingCooldownMs = remainingCooldownMs;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public long getRemainingCooldownMs() {
            return remainingCooldownMs;
        }
    }
}
