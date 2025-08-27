package br.com.primeleague.core.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Manager responsável por centralizar e padronizar mensagens do sistema.
 * Garante consistência visual em toda a aplicação.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class MessageManager {
    
    private static MessageManager instance;
    
    // Prefixos e cores padrão
    private static final String PREFIX = "&b[PrimeLeague] &f";
    private static final String SUCCESS_PREFIX = "&a[✓] &f";
    private static final String ERROR_PREFIX = "&c[✗] &f";
    private static final String WARNING_PREFIX = "&e[⚠] &f";
    private static final String INFO_PREFIX = "&9[ℹ] &f";
    
    /**
     * Construtor privado para singleton.
     */
    private MessageManager() {
        // Construtor vazio
    }
    
    /**
     * Obtém a instância singleton do MessageManager.
     * 
     * @return Instância do MessageManager
     */
    public static MessageManager getInstance() {
        if (instance == null) {
            instance = new MessageManager();
        }
        return instance;
    }
    
    /**
     * Envia uma mensagem simples para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param message Mensagem a ser enviada
     */
    public void sendMessage(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + message));
        }
    }
    
    /**
     * Envia uma mensagem de sucesso para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param message Mensagem de sucesso
     */
    public void sendSuccess(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', SUCCESS_PREFIX + message));
        }
    }
    
    /**
     * Envia uma mensagem de erro para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param message Mensagem de erro
     */
    public void sendError(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', ERROR_PREFIX + message));
        }
    }
    
    /**
     * Envia uma mensagem de aviso para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param message Mensagem de aviso
     */
    public void sendWarning(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', WARNING_PREFIX + message));
        }
    }
    
    /**
     * Envia uma mensagem informativa para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param message Mensagem informativa
     */
    public void sendInfo(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', INFO_PREFIX + message));
        }
    }
    
    /**
     * Envia uma mensagem de acesso negado para um jogador.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendNoPermission(Player player) {
        sendError(player, "Você não tem permissão para usar este comando.");
    }
    
    /**
     * Envia uma mensagem de uso incorreto do comando.
     * 
     * @param player Jogador que receberá a mensagem
     * @param usage Uso correto do comando
     */
    public void sendUsage(Player player, String usage) {
        sendWarning(player, "Uso correto: " + usage);
    }
    
    /**
     * Envia uma mensagem de jogador não encontrado.
     * 
     * @param player Jogador que receberá a mensagem
     * @param targetName Nome do jogador não encontrado
     */
    public void sendPlayerNotFound(Player player, String targetName) {
        sendError(player, "Jogador '" + targetName + "' não encontrado.");
    }
    
    /**
     * Envia uma mensagem de assinatura expirada.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendSubscriptionExpired(Player player) {
        sendError(player, "Sua assinatura expirou. Renove para continuar jogando.");
    }
    
    /**
     * Envia uma mensagem de assinatura expirando em breve.
     * 
     * @param player Jogador que receberá a mensagem
     * @param days Número de dias restantes
     */
    public void sendSubscriptionExpiringSoon(Player player, int days) {
        if (days == 1) {
            sendWarning(player, "Sua assinatura expira amanhã! Renove para continuar jogando.");
        } else {
            sendWarning(player, "Sua assinatura expira em " + days + " dias. Renove para continuar jogando.");
        }
    }
    
    /**
     * Envia uma mensagem de punição aplicada.
     * 
     * @param player Jogador que receberá a mensagem
     * @param targetName Nome do jogador punido
     * @param type Tipo de punição
     * @param reason Motivo da punição
     */
    public void sendPunishmentApplied(Player player, String targetName, String type, String reason) {
        sendSuccess(player, "Punição aplicada: " + targetName + " foi " + type.toLowerCase() + 
                   (reason != null && !reason.isEmpty() ? " por: " + reason : "") + ".");
    }
    
    /**
     * Envia uma mensagem de punição removida.
     * 
     * @param player Jogador que receberá a mensagem
     * @param targetName Nome do jogador que teve a punição removida
     * @param type Tipo de punição removida
     */
    public void sendPunishmentRemoved(Player player, String targetName, String type) {
        sendSuccess(player, "Punição removida: " + targetName + " teve o " + type.toLowerCase() + " removido.");
    }
    
    /**
     * Envia uma mensagem de verificação Discord bem-sucedida.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendDiscordVerificationSuccess(Player player) {
        sendSuccess(player, "Conta Discord vinculada com sucesso!");
        sendInfo(player, "Agora você pode usar os comandos do Discord.");
    }
    
    /**
     * Envia uma mensagem de verificação Discord falhou.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendDiscordVerificationFailed(Player player) {
        sendError(player, "Código inválido ou expirado!");
        sendInfo(player, "Verifique se digitou corretamente e se não expirou (5 minutos).");
    }
    
    /**
     * Envia uma mensagem de ticket criado.
     * 
     * @param player Jogador que receberá a mensagem
     * @param ticketId ID do ticket criado
     */
    public void sendTicketCreated(Player player, int ticketId) {
        sendSuccess(player, "Ticket #" + ticketId + " criado com sucesso!");
        sendInfo(player, "Nossa equipe irá analisar seu ticket em breve.");
    }
    
    /**
     * Envia uma mensagem de ticket atualizado.
     * 
     * @param player Jogador que receberá a mensagem
     * @param ticketId ID do ticket atualizado
     * @param status Novo status do ticket
     */
    public void sendTicketUpdated(Player player, int ticketId, String status) {
        sendInfo(player, "Ticket #" + ticketId + " atualizado para: " + status);
    }
    
    /**
     * Envia uma mensagem de estatísticas do jogador.
     * 
     * @param player Jogador que receberá a mensagem
     * @param targetName Nome do jogador das estatísticas
     * @param elo ELO do jogador
     * @param money Dinheiro do jogador
     * @param playtime Tempo de jogo
     */
    public void sendPlayerStats(Player player, String targetName, int elo, double money, long playtime) {
        sendInfo(player, "Estatísticas de " + targetName + ":");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7  • ELO: &e" + elo + 
            "&7  • Dinheiro: &a$" + String.format("%.2f", money) + 
            "&7  • Tempo de jogo: &b" + formatPlaytime(playtime)));
    }
    
    /**
     * Formata o tempo de jogo em formato legível.
     * 
     * @param playtime Tempo em milissegundos
     * @return Tempo formatado
     */
    private String formatPlaytime(long playtime) {
        long seconds = playtime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Envia uma mensagem de ajuda para comandos de punição.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendPunishmentHelp(Player player) {
        sendInfo(player, "Comandos de punição disponíveis:");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7  • /kick <jogador> [motivo] - Expulsa um jogador\n" +
            "&7  • /mute <jogador> [motivo] - Silencia um jogador permanentemente\n" +
            "&7  • /tempmute <jogador> <tempo> [motivo] - Silencia temporariamente\n" +
            "&7  • /unmute <jogador> - Remove o silenciamento\n" +
            "&7  • /ban <jogador> [motivo] - Bane um jogador permanentemente\n" +
            "&7  • /tempban <jogador> <tempo> [motivo] - Bane temporariamente\n" +
            "&7  • /unban <jogador> - Remove o banimento\n" +
            "&7  • /history <jogador> - Ver histórico de punições"));
    }
    
    /**
     * Envia uma mensagem de ajuda para comandos P2P.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendP2PHelp(Player player) {
        sendInfo(player, "Comandos P2P disponíveis:");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7  • /minhaassinatura - Ver informações da sua assinatura\n" +
            "&7  • /p2p check <jogador> - Verificar assinatura de um jogador\n" +
            "&7  • /p2p grant <jogador> <dias> - Conceder dias de assinatura\n" +
            "&7  • /p2p revoke <jogador> - Revogar assinatura"));
    }
    
    /**
     * Envia uma mensagem de ajuda para comandos de staff.
     * 
     * @param player Jogador que receberá a mensagem
     */
    public void sendStaffHelp(Player player) {
        sendInfo(player, "Comandos de staff disponíveis:");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            "&7  • /vanish - Alternar visibilidade\n" +
            "&7  • /staffchat ou /sc - Enviar mensagem para staff\n" +
            "&7  • /history <jogador> - Ver histórico de punições"));
    }
}
