package br.com.primeleague.admin.listeners;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Punishment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

/**
 * Listener para verificar punições de login (bans).
 */
public class LoginListener implements Listener {

    private final AdminManager adminManager;

    public LoginListener(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        // Verificar se o jogador está banido
        // Em Bukkit 1.5.2, não há getUniqueId(), usar getName() para gerar UUID offline
        java.util.UUID playerUuid;
        try {
            playerUuid = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + event.getName()).getBytes("UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            // Se não conseguir gerar UUID, permitir login
            return;
        }



        Punishment ban = adminManager.getActivePunishment(playerUuid, Punishment.Type.BAN);
        if (ban != null && ban.isCurrentlyActive()) {
            // Negar login com mensagem customizada bonita
            String kickMessage = "§c§l✘ ACESSO NEGADO ✘\n\n" +
                                "§7Você está §cBANIDO §7deste servidor!\n\n" +
                                "§7Motivo: §f" + ban.getReason() + "\n" +
                                "§7Staff: §f" + getAuthorName(ban.getAuthorUuid()) + "\n" +
                                "§7Código: §f#" + ban.getId() + "\n\n";

            if (ban.isPermanent()) {
                kickMessage += "§7Tipo: §cBANIMENTO PERMANENTE\n\n";
            } else {
                long remainingTime = ban.getExpiresAt().getTime() - System.currentTimeMillis();
                long days = remainingTime / (1000 * 60 * 60 * 24);
                long hours = (remainingTime % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);

                kickMessage += "§7Tipo: §eBAN TEMPORÁRIO\n";
                kickMessage += "§7Expira em: §e" + days + "d " + hours + "h " + minutes + "m\n\n";
            }

            kickMessage += "§7Para recurso, acesse nosso Discord.";

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);

            // Log da tentativa de login negada
            org.bukkit.Bukkit.getLogger().info("[ADMIN] Login negado para " + event.getName() + " - Ban ativo #" + ban.getId());
        }
    }

    /**
     * Obtém o nome do autor da punição.
     */
    private String getAuthorName(java.util.UUID authorUuid) {
        if (authorUuid == null) {
            return "Sistema";
        }

        // Tentar obter do servidor - Em Bukkit 1.5.2, getPlayer() espera String
        // Como não temos o nome, vamos direto para o perfil

        // Tentar obter do perfil
        try {
                            br.com.primeleague.core.models.PlayerProfile profile =
                br.com.primeleague.core.api.PrimeLeagueAPI.getPlayerProfile(authorUuid);
            if (profile != null) {
                return profile.getPlayerName();
            }
        } catch (Exception e) {
            // Ignorar erros
        }

        return "Desconhecido";
    }
}
