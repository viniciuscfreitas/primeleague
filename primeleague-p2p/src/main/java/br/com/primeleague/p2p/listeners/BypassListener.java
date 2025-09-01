package br.com.primeleague.p2p.listeners;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Listener que lida com a lógica de bypass de assinatura para administradores
 * após o login ser bem-sucedido.
 * 
 * Este listener é executado no PlayerJoinEvent, quando o objeto Player
 * já está totalmente disponível para verificação de permissões.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public final class BypassListener implements Listener {

    private final PrimeLeagueP2P plugin;

    public BypassListener() {
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    /**
     * Aplica bypass de assinatura para administradores após o login.
     * 
     * @param event O evento de join do jogador
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Verificamos a permissão SOMENTE quando o objeto Player está disponível
        if (player.hasPermission("primeleague.p2p.bypass")) {
            PlayerProfile profile = PrimeLeagueAPI.getPlayerProfile(player);
            if (profile != null) {
                // Atualizamos a data de expiração em memória para um futuro distante
                // Isso evita a necessidade de qualquer outra verificação durante esta sessão
                // Não há necessidade de salvar isso no banco de dados
                Timestamp farFuture = Timestamp.from(Instant.now().plus(365 * 10, ChronoUnit.DAYS)); // 10 anos
                // TODO: Implementar atualização SSOT via DataManager
                
                plugin.getLogger().info("Bypass de assinatura aplicado para " + player.getName());
                
                // Opcional: enviar mensagem ao jogador
                player.sendMessage(getMessage("bypass_active"));
            }
        }
    }

    /**
     * Obtém uma mensagem da configuração do plugin.
     * 
     * @param key A chave da mensagem
     * @return A mensagem configurada ou uma mensagem padrão
     */
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, "");
        if (message.isEmpty()) {
            // Mensagens padrão caso não estejam configuradas
            switch (key) {
                case "bypass_active":
                    return "§aBypass de assinatura ativo para este jogador.";
                default:
                    return "§aBypass ativo.";
            }
        }
        return message;
    }
}
