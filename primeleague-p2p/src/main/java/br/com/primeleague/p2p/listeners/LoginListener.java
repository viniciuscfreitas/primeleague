package br.com.primeleague.p2p.listeners;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listener responsável por verificar a assinatura do jogador durante o login.
 *
 * Este listener é executado no PlayerJoinEvent com prioridade LOWEST
 * para garantir que rode após o Core carregar o perfil do jogador.
 *
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public final class LoginListener implements Listener {

    private final PrimeLeagueP2P plugin;

    public LoginListener() {
        this.plugin = PrimeLeagueP2P.getInstance();
    }

    /**
     * Verifica se o jogador possui uma assinatura ativa após o login.
     *
     * @param event O evento de join do jogador
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        try {
            // Aguardar um pouco para o Core carregar o perfil
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    // Obter o perfil do jogador através da API do Core
                    PlayerProfile profile = PrimeLeagueAPI.getProfileByName(playerName);

                    // Se o perfil não carregou por algum motivo, kickamos o jogador
                    if (profile == null) {
                        player.kickPlayer(getMessage("profile_error"));
                        plugin.getLogger().warning("Perfil não encontrado para " + playerName);
                        return;
                    }

                    // Verificar se a assinatura está ativa
                    // TODO: Implementar consulta SSOT via DataManager
                    if (!PrimeLeagueAPI.getDataManager().hasActiveSubscription(player.getUniqueId())) {
                        player.kickPlayer(getMessage("access_expired"));
                        plugin.getLogger().info("Acesso negado para " + playerName + " - Assinatura expirada");
                        return;
                    }

                    // Se chegou até aqui, a assinatura está ativa
                    plugin.getLogger().info("Acesso permitido para " + playerName + " - Assinatura válida");
                }
            }, 10L); // Aguardar 10 ticks (0.5 segundos)

        } catch (Exception e) {
            // Em caso de erro, kickamos o jogador por segurança
            player.kickPlayer(getMessage("profile_error"));
            plugin.getLogger().severe("Erro ao verificar assinatura para " + playerName + ": " + e.getMessage());
            e.printStackTrace();
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
                case "access_expired":
                    return "§cSeu acesso ao Prime League expirou.\n§aRenove sua assinatura em nosso Discord.";
                case "profile_error":
                    return "§cOcorreu um erro ao carregar seu perfil. Tente novamente.";
                default:
                    return "§cErro desconhecido.";
            }
        }
        return message;
    }
}
