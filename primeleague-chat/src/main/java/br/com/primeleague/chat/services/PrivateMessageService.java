package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço dedicado para gerenciar mensagens privadas entre jogadores.
 * Responsável por rastrear conversas, formatar mensagens e integrar com outros serviços.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class PrivateMessageService {
    
    private final PrimeLeagueChat plugin;
    private final ChannelIgnoreService ignoreService;
    private final ChatLoggingService loggingService;
    
    // Mapa para rastrear o último parceiro de conversa de cada jogador
    private final Map<UUID, UUID> lastConversationPartner = new ConcurrentHashMap<>();
    
    // Lista de administradores com social spy ativo
    private final Set<UUID> socialSpyUsers = ConcurrentHashMap.newKeySet();
    
    // Templates de formatação configuráveis
    private String senderFormat;
    private String receiverFormat;
    private String spyFormat;
    
    public PrivateMessageService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        this.ignoreService = plugin.getIgnoreService();
        this.loggingService = plugin.getLoggingService();
        loadConfiguration();
    }
    
    /**
     * Carrega as configurações de formatação do config.yml
     */
    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        
        // Carregar templates com fallbacks padrão
        senderFormat = config.getString("private_messages.sender_format", 
            "§d[Você -> {receiver}] §f{message}");
        receiverFormat = config.getString("private_messages.receiver_format", 
            "§d[{sender} -> Você] §f{message}");
        spyFormat = config.getString("private_messages.spy_format", 
            "§7[Spy: {sender} -> {receiver}] §f{message}");
    }
    
    /**
     * Envia uma mensagem privada de um jogador para outro.
     * 
     * @param sender Jogador que envia a mensagem
     * @param receiverName Nome do jogador que recebe a mensagem
     * @param message Conteúdo da mensagem
     * @return true se a mensagem foi enviada com sucesso, false caso contrário
     */
    public boolean sendPrivateMessage(Player sender, String receiverName, String message) {
        // Verificar se o destinatário existe e está online
        Player receiver = Bukkit.getPlayer(receiverName);
        if (receiver == null) {
            sender.sendMessage("§cJogador '" + receiverName + "' não encontrado ou offline.");
            return false;
        }
        
        // Verificar se não está tentando enviar para si mesmo
        if (receiver.equals(sender)) {
            sender.sendMessage("§cVocê não pode enviar mensagens privadas para si mesmo.");
            return false;
        }
        
        // Verificar se o destinatário está ignorando o remetente
        if (ignoreService.isIgnoringPlayer(receiver, sender)) {
            sender.sendMessage("§cEste jogador está ignorando você.");
            return false;
        }
        
        // Formatar mensagens
        String senderMessage = formatSenderMessage(sender.getName(), receiver.getName(), message);
        String receiverMessage = formatReceiverMessage(sender.getName(), receiver.getName(), message);
        
        // Enviar mensagens
        sender.sendMessage(senderMessage);
        receiver.sendMessage(receiverMessage);
        
        // Atualizar rastreamento de conversas
        updateConversationTracking(sender, receiver);
        
        // Registrar no log
        loggingService.logMessage("PRIVATE", sender, receiver, message);
        
        // Enviar para espiões sociais
        sendToSocialSpies(sender, receiver, message);
        
        return true;
    }
    
    /**
     * Responde à última mensagem privada recebida.
     * 
     * @param sender Jogador que está respondendo
     * @param message Conteúdo da resposta
     * @return true se a resposta foi enviada com sucesso, false caso contrário
     */
    public boolean replyToLastMessage(Player sender, String message) {
        UUID lastPartner = lastConversationPartner.get(sender.getUniqueId());
        
        if (lastPartner == null) {
            sender.sendMessage("§cVocê não tem nenhuma conversa recente para responder.");
            return false;
        }
        
        Player lastPartnerPlayer = null;
        // Buscar o jogador pelo UUID (Bukkit 1.5.2 não tem getPlayer(UUID))
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getUniqueId().equals(lastPartner)) {
                lastPartnerPlayer = onlinePlayer;
                break;
            }
        }
        
        if (lastPartnerPlayer == null) {
            sender.sendMessage("§cO jogador com quem você conversou não está mais online.");
            lastConversationPartner.remove(sender.getUniqueId());
            return false;
        }
        
        // Usar o método principal para enviar a resposta
        return sendPrivateMessage(sender, lastPartnerPlayer.getName(), message);
    }
    
    /**
     * Ativa o modo social spy para um administrador.
     * 
     * @param player Administrador que ativa o social spy
     * @return true se ativado com sucesso, false se já estava ativo
     */
    public boolean enableSocialSpy(Player player) {
        if (socialSpyUsers.add(player.getUniqueId())) {
            player.sendMessage("§a🔍 Social Spy ativado. Você verá todas as mensagens privadas.");
            return true;
        } else {
            player.sendMessage("§e⚠️ Social Spy já estava ativo.");
            return false;
        }
    }
    
    /**
     * Desativa o modo social spy para um administrador.
     * 
     * @param player Administrador que desativa o social spy
     * @return true se desativado com sucesso, false se já estava inativo
     */
    public boolean disableSocialSpy(Player player) {
        if (socialSpyUsers.remove(player.getUniqueId())) {
            player.sendMessage("§c🔍 Social Spy desativado.");
            return true;
        } else {
            player.sendMessage("§e⚠️ Social Spy já estava inativo.");
            return false;
        }
    }
    
    /**
     * Verifica se um jogador tem social spy ativo.
     * 
     * @param player Jogador para verificar
     * @return true se o social spy está ativo
     */
    public boolean hasSocialSpy(Player player) {
        return socialSpyUsers.contains(player.getUniqueId());
    }
    
    /**
     * Atualiza o rastreamento de conversas entre dois jogadores.
     * 
     * @param player1 Primeiro jogador
     * @param player2 Segundo jogador
     */
    private void updateConversationTracking(Player player1, Player player2) {
        UUID uuid1 = player1.getUniqueId();
        UUID uuid2 = player2.getUniqueId();
        
        // Atualizar o último parceiro de conversa para ambos
        lastConversationPartner.put(uuid1, uuid2);
        lastConversationPartner.put(uuid2, uuid1);
    }
    
    /**
     * Envia mensagens privadas para todos os espiões sociais ativos.
     * 
     * @param sender Remetente da mensagem
     * @param receiver Destinatário da mensagem
     * @param message Conteúdo da mensagem
     */
    private void sendToSocialSpies(Player sender, Player receiver, String message) {
        if (socialSpyUsers.isEmpty()) {
            return;
        }
        
        String spyMessage = formatSpyMessage(sender.getName(), receiver.getName(), message);
        
        for (UUID spyUUID : socialSpyUsers) {
            Player spy = null;
            // Buscar o jogador pelo UUID (Bukkit 1.5.2 não tem getPlayer(UUID))
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getUniqueId().equals(spyUUID)) {
                    spy = onlinePlayer;
                    break;
                }
            }
            
            if (spy != null && !spy.equals(sender) && !spy.equals(receiver)) {
                spy.sendMessage(spyMessage);
            }
        }
    }
    
    /**
     * Formata a mensagem para quem envia.
     * 
     * @param senderName Nome do remetente
     * @param receiverName Nome do destinatário
     * @param message Conteúdo da mensagem
     * @return Mensagem formatada
     */
    private String formatSenderMessage(String senderName, String receiverName, String message) {
        return senderFormat
            .replace("{sender}", senderName)
            .replace("{receiver}", receiverName)
            .replace("{message}", message);
    }
    
    /**
     * Formata a mensagem para quem recebe.
     * 
     * @param senderName Nome do remetente
     * @param receiverName Nome do destinatário
     * @param message Conteúdo da mensagem
     * @return Mensagem formatada
     */
    private String formatReceiverMessage(String senderName, String receiverName, String message) {
        return receiverFormat
            .replace("{sender}", senderName)
            .replace("{receiver}", receiverName)
            .replace("{message}", message);
    }
    
    /**
     * Formata a mensagem para espiões sociais.
     * 
     * @param senderName Nome do remetente
     * @param receiverName Nome do destinatário
     * @param message Conteúdo da mensagem
     * @return Mensagem formatada
     */
    private String formatSpyMessage(String senderName, String receiverName, String message) {
        return spyFormat
            .replace("{sender}", senderName)
            .replace("{receiver}", receiverName)
            .replace("{message}", message);
    }
    
    /**
     * Limpa o rastreamento de conversas quando um jogador sai.
     * 
     * @param player Jogador que saiu
     */
    public void onPlayerQuit(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        // Remover o jogador do rastreamento de conversas
        lastConversationPartner.remove(playerUUID);
        
        // Remover de jogadores que tinham ele como último parceiro
        java.util.Iterator<java.util.Map.Entry<UUID, UUID>> iterator = lastConversationPartner.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<UUID, UUID> entry = iterator.next();
            if (entry.getValue().equals(playerUUID)) {
                iterator.remove();
            }
        }
        
        // Remover do social spy se estiver ativo
        socialSpyUsers.remove(playerUUID);
    }
    
    /**
     * Recarrega as configurações do arquivo config.yml
     */
    public void reloadConfiguration() {
        loadConfiguration();
    }
}
