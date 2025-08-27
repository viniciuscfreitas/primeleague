package br.com.primeleague.core.managers;

import br.com.primeleague.api.LoggingServiceRegistry;
import br.com.primeleague.api.dto.LogEntryDTO;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.services.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager responsável por gerenciar mensagens privadas entre jogadores.
 * Suporta comandos /msg, /tell e /r com histórico de conversas.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class PrivateMessageManager {
    
    private final TagManager tagManager;
    private final MessageManager messageManager;
    
    // Cache do último interlocutor de cada jogador para o comando /r
    private final Map<UUID, String> lastConversation;
    
    // Prefixos para mensagens privadas
    private static final String SEND_PREFIX = "&d[→] &f";
    private static final String RECEIVE_PREFIX = "&d[←] &f";
    private static final String ERROR_PREFIX = "&c[✗] &f";
    
    public PrivateMessageManager(TagManager tagManager, MessageManager messageManager) {
        this.tagManager = tagManager;
        this.messageManager = messageManager;
        this.lastConversation = new ConcurrentHashMap<>();
    }
    
    /**
     * Envia uma mensagem privada de um jogador para outro.
     * 
     * @param sender Jogador que envia a mensagem
     * @param targetName Nome do jogador que receberá a mensagem
     * @param message Conteúdo da mensagem
     * @return true se a mensagem foi enviada com sucesso, false caso contrário
     */
    public boolean sendPrivateMessage(Player sender, String targetName, String message) {
        if (sender == null || targetName == null || message == null) {
            return false;
        }
        
        // Verificar se o jogador está tentando enviar mensagem para si mesmo
        if (sender.getName().equalsIgnoreCase(targetName)) {
            messageManager.sendError(sender, "Você não pode enviar mensagem privada para si mesmo.");
            return false;
        }
        
        // Buscar o jogador alvo
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messageManager.sendError(sender, "Jogador '" + targetName + "' não está online.");
            return false;
        }
        
        // Formatar mensagens com tags
        String formattedSenderName = tagManager.formatText(sender, "{clan_tag}{elo}" + sender.getName());
        String formattedTargetName = tagManager.formatText(target, "{clan_tag}{elo}" + target.getName());
        
        // Enviar mensagem para o remetente
        String sendMessage = SEND_PREFIX + "Para " + formattedTargetName + ": " + message;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', sendMessage));
        
        // Enviar mensagem para o destinatário
        String receiveMessage = RECEIVE_PREFIX + "De " + formattedSenderName + ": " + message;
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', receiveMessage));
        
        // LOGGING: Registrar mensagem privada no banco de dados
        logPrivateMessage(sender, target, message);
        
        // Atualizar histórico de conversas
        lastConversation.put(sender.getUniqueId(), target.getName());
        lastConversation.put(target.getUniqueId(), sender.getName());
        
        return true;
    }
    
    /**
     * Registra mensagem privada no sistema de logging via API.
     * 
     * @param sender Jogador que envia a mensagem
     * @param receiver Jogador que recebe a mensagem
     * @param message Conteúdo da mensagem
     */
    private void logPrivateMessage(Player sender, Player receiver, String message) {
        try {
            // Criar LogEntryDTO para comunicação via API
            LogEntryDTO entry = new LogEntryDTO(
                "PRIVATE",
                sender.getUniqueId(),
                sender.getName(),
                receiver.getUniqueId(),
                receiver.getName(),
                null, // Clan ID será obtido pelo ChatLoggingService
                message,
                System.currentTimeMillis()
            );
            
            // Usar LoggingServiceRegistry para comunicação desacoplada
            LoggingServiceRegistry.logChatMessage(entry);
            
            // Log de debug (apenas se necessário)
            // System.out.println("🔒 [PRIVATE-MESSAGE] Mensagem privada registrada via API: " + sender.getName() + " → " + receiver.getName() + ": " + message);
        } catch (Exception e) {
            System.out.println("🚨 [PRIVATE-MESSAGE] Erro ao registrar mensagem privada: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Responde à última mensagem privada recebida.
     * 
     * @param sender Jogador que está respondendo
     * @param message Conteúdo da resposta
     * @return true se a resposta foi enviada com sucesso, false caso contrário
     */
    public boolean replyToLastMessage(Player sender, String message) {
        if (sender == null || message == null) {
            return false;
        }
        
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = sender.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        String lastInterlocutorName = lastConversation.get(canonicalUuid);
        if (lastInterlocutorName == null) {
            messageManager.sendError(sender, "Você não tem ninguém para responder. Use /msg <jogador> <mensagem>.");
            return false;
        }
        
        Player target = Bukkit.getPlayerExact(lastInterlocutorName);
        if (target == null) {
            messageManager.sendError(sender, "O jogador com quem você conversou não está mais online.");
            lastConversation.remove(canonicalUuid);
            return false;
        }
        
        // Enviar a mensagem usando o método principal
        return sendPrivateMessage(sender, target.getName(), message);
    }
    
    /**
     * Verifica se um jogador tem alguém para responder.
     * 
     * @param player Jogador para verificar
     * @return true se o jogador tem histórico de conversa, false caso contrário
     */
    public boolean hasLastConversation(Player player) {
        if (player == null) {
            return false;
        }
        
        // USAR TRADUTOR DE IDENTIDADE
        UUID bukkitUuid = player.getUniqueId();
        UUID canonicalUuid = PrimeLeagueAPI.getDataManager().getCanonicalUuid(bukkitUuid);
        
        String lastInterlocutorName = lastConversation.get(canonicalUuid);
        if (lastInterlocutorName == null) {
            return false;
        }
        
        // Verificar se o último interlocutor ainda está online
        Player target = Bukkit.getPlayerExact(lastInterlocutorName);
        return target != null;
    }
    
    /**
     * Obtém o nome do último interlocutor de um jogador.
     * 
     * @param player Jogador para verificar
     * @return Nome do último interlocutor ou null se não houver
     */
    public String getLastInterlocutorName(Player player) {
        if (player == null) {
            return null;
        }
        
        String lastInterlocutorName = lastConversation.get(player.getUniqueId());
        if (lastInterlocutorName == null) {
            return null;
        }
        
        Player target = Bukkit.getPlayerExact(lastInterlocutorName);
        return target != null ? target.getName() : null;
    }
    
    /**
     * Limpa o histórico de conversa de um jogador.
     * Útil quando um jogador sai do servidor.
     * 
     * @param playerUuid UUID do jogador
     */
    public void clearConversationHistory(UUID playerUuid) {
        if (playerUuid != null) {
            lastConversation.remove(playerUuid);
        }
    }
    
    /**
     * Obtém estatísticas do manager para debugging.
     * 
     * @return String com estatísticas
     */
    public String getStats() {
        return String.format("PrivateMessageManager Stats - Active Conversations: %d", 
                           lastConversation.size());
    }
}
