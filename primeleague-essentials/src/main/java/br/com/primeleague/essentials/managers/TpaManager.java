package br.com.primeleague.essentials.managers;

import br.com.primeleague.essentials.EssentialsPlugin;
import br.com.primeleague.essentials.models.TeleportRequest;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.combatlog.CombatLogPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Gerenciador do sistema de teletransporte entre jogadores.
 * Orquestrador síncrono responsável por gerenciar solicitações de TPA.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class TpaManager {
    
    private final EssentialsPlugin plugin;
    private final Logger logger;
    
    // Cache de solicitações ativas (UUID do target -> TeleportRequest)
    private final Map<UUID, TeleportRequest> activeRequests = new ConcurrentHashMap<>();
    
    // Cache de cooldowns por jogador (UUID -> timestamp)
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    
    // Configurações do sistema
    private int requestExpirationSeconds = 60;
    private int requestCooldownSeconds = 30;
    private int teleportDelaySeconds = 5;
    
    /**
     * Construtor do TpaManager.
     * 
     * @param plugin Instância do plugin principal
     */
    public TpaManager(EssentialsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Carregar configurações
        loadConfigurations();
        
        // Inicializar sistema de limpeza automática
        initializeCleanupTask();
        
        logger.info("✅ TpaManager inicializado com sucesso!");
    }
    
    /**
     * Envia uma solicitação de teletransporte.
     * 
     * @param requester Jogador que solicita
     * @param target Jogador alvo
     * @param type Tipo de solicitação (TPA ou TPAHERE)
     * @return true se a solicitação foi enviada com sucesso
     */
    public boolean sendTeleportRequest(Player requester, Player target, TeleportRequest.RequestType type) {
        // Validações rápidas
        if (!canSendRequest(requester, target)) {
            return false;
        }
        
        // Verificar se já existe solicitação ativa para o target
        if (activeRequests.containsKey(target.getUniqueId())) {
            requester.sendMessage("§c" + target.getName() + " já possui uma solicitação de teletransporte pendente!");
            return false;
        }
        
        // Criar solicitação
        TeleportRequest request = new TeleportRequest(
            requester.getUniqueId(),
            requester.getName(),
            target.getUniqueId(),
            target.getName(),
            type
        );
        
        // Adicionar ao cache
        activeRequests.put(target.getUniqueId(), request);
        
        // Aplicar cooldown
        setRequestCooldown(requester);
        
        // Notificar jogadores
        notifyRequestSent(requester, target, type);
        
        logger.info("✅ Solicitação TPA enviada: " + requester.getName() + " -> " + target.getName());
        return true;
    }
    
    /**
     * Aceita uma solicitação de teletransporte.
     * 
     * @param accepter Jogador que aceita
     * @return true se a solicitação foi aceita com sucesso
     */
    public boolean acceptTeleportRequest(Player accepter) {
        TeleportRequest request = activeRequests.get(accepter.getUniqueId());
        
        if (request == null) {
            accepter.sendMessage("§cVocê não possui solicitações de teletransporte pendentes!");
            return false;
        }
        
        // Verificar se a solicitação não expirou
        if (request.isExpired()) {
            activeRequests.remove(accepter.getUniqueId());
            accepter.sendMessage("§cA solicitação de teletransporte expirou!");
            return false;
        }
        
        // Verificar se o solicitante ainda está online
        Player requester = Bukkit.getPlayer(request.getRequesterName());
        if (requester == null) {
            activeRequests.remove(accepter.getUniqueId());
            accepter.sendMessage("§cO jogador que solicitou o teletransporte não está mais online!");
            return false;
        }
        
        // Verificar se ambos os jogadores não estão em combate
        if (isPlayerInCombat(requester) || isPlayerInCombat(accepter)) {
            accepter.sendMessage("§cNão é possível aceitar teletransporte durante o combate!");
            return false;
        }
        
        // Marcar como aceito
        request.setStatus(TeleportRequest.RequestStatus.ACCEPTED);
        
        // Remover do cache
        activeRequests.remove(accepter.getUniqueId());
        
        // Executar teletransporte com delay
        executeTeleportWithDelay(requester, accepter, request.getType());
        
        // Notificar jogadores
        notifyRequestAccepted(requester, accepter);
        
        logger.info("✅ Solicitação TPA aceita: " + accepter.getName() + " aceitou de " + requester.getName());
        return true;
    }
    
    /**
     * Nega uma solicitação de teletransporte.
     * 
     * @param denier Jogador que nega
     * @return true se a solicitação foi negada com sucesso
     */
    public boolean denyTeleportRequest(Player denier) {
        TeleportRequest request = activeRequests.get(denier.getUniqueId());
        
        if (request == null) {
            denier.sendMessage("§cVocê não possui solicitações de teletransporte pendentes!");
            return false;
        }
        
        // Marcar como negado
        request.setStatus(TeleportRequest.RequestStatus.DENIED);
        
        // Remover do cache
        activeRequests.remove(denier.getUniqueId());
        
        // Notificar jogadores
        Player requester = Bukkit.getPlayer(request.getRequesterName());
        if (requester != null) {
            requester.sendMessage("§c" + denier.getName() + " negou sua solicitação de teletransporte!");
        }
        
        denier.sendMessage("§aVocê negou a solicitação de teletransporte!");
        
        logger.info("✅ Solicitação TPA negada: " + denier.getName() + " negou de " + request.getRequesterName());
        return true;
    }
    
    /**
     * Cancela uma solicitação de teletransporte enviada.
     * 
     * @param canceller Jogador que cancela
     * @return true se a solicitação foi cancelada com sucesso
     */
    public boolean cancelTeleportRequest(Player canceller) {
        // Procurar solicitação enviada pelo jogador
        TeleportRequest request = null;
        for (TeleportRequest req : activeRequests.values()) {
            if (req.getRequesterUuid().equals(canceller.getUniqueId())) {
                request = req;
                break;
            }
        }
        
        if (request == null) {
            canceller.sendMessage("§cVocê não possui solicitações de teletransporte enviadas!");
            return false;
        }
        
        // Marcar como cancelado
        request.setStatus(TeleportRequest.RequestStatus.CANCELLED);
        
        // Remover do cache
        activeRequests.remove(request.getTargetUuid());
        
        // Notificar jogadores
        Player target = Bukkit.getPlayer(request.getTargetName());
        if (target != null) {
            target.sendMessage("§c" + canceller.getName() + " cancelou a solicitação de teletransporte!");
        }
        
        canceller.sendMessage("§aVocê cancelou a solicitação de teletransporte!");
        
        logger.info("✅ Solicitação TPA cancelada: " + canceller.getName() + " cancelou para " + request.getTargetName());
        return true;
    }
    
    /**
     * Lista as solicitações pendentes de um jogador.
     * 
     * @param player Jogador
     * @return String com a lista de solicitações
     */
    public String listPendingRequests(Player player) {
        TeleportRequest request = activeRequests.get(player.getUniqueId());
        
        if (request == null) {
            return "§cVocê não possui solicitações de teletransporte pendentes!";
        }
        
        if (request.isExpired()) {
            activeRequests.remove(player.getUniqueId());
            return "§cA solicitação de teletransporte expirou!";
        }
        
        long timeLeft = (request.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000;
        String typeText = request.getType() == TeleportRequest.RequestType.TPA ? "para você" : "para ir até ele";
        
        return "§aSolicitação pendente de §e" + request.getRequesterName() + 
               "§a " + typeText + " (§e" + timeLeft + "s§a restantes)";
    }
    
    /**
     * Verifica se o jogador pode enviar solicitações de teletransporte.
     */
    private boolean canSendRequest(Player requester, Player target) {
        // Verificar permissão básica
        if (!requester.hasPermission("primeleague.essentials.tpa")) {
            requester.sendMessage("§cVocê não tem permissão para usar este comando!");
            return false;
        }
        
        // Verificar cooldown
        if (!canSendRequestNow(requester)) {
            return false;
        }
        
        // Verificar se não está em combate
        if (isPlayerInCombat(requester)) {
            requester.sendMessage("§cNão é possível solicitar teletransporte durante o combate!");
            return false;
        }
        
        // Verificar se o target não está em combate
        if (isPlayerInCombat(target)) {
            requester.sendMessage("§c" + target.getName() + " está em combate! Aguarde o combate terminar.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica se o jogador pode enviar solicitação agora (cooldown).
     */
    private boolean canSendRequestNow(Player player) {
        if (player.hasPermission("primeleague.essentials.tpa.bypass.cooldown")) {
            return true;
        }
        
        Long lastRequest = requestCooldowns.get(player.getUniqueId());
        if (lastRequest == null) {
            return true;
        }
        
        long timeSinceLastRequest = System.currentTimeMillis() - lastRequest;
        long cooldownMs = requestCooldownSeconds * 1000L;
        
        if (timeSinceLastRequest < cooldownMs) {
            long remainingSeconds = (cooldownMs - timeSinceLastRequest) / 1000;
            player.sendMessage("§cAguarde " + remainingSeconds + " segundos antes de enviar outra solicitação!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Define o cooldown para o jogador.
     */
    private void setRequestCooldown(Player player) {
        requestCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Verifica se o jogador está em combate.
     */
    private boolean isPlayerInCombat(Player player) {
        if (player.hasPermission("primeleague.essentials.tpa.bypass.combat")) {
            return false;
        }
        
        // Integração com CombatLogManager
        CombatLogPlugin combatLogPlugin = CombatLogPlugin.getInstance();
        if (combatLogPlugin != null) {
            return combatLogPlugin.getCombatLogManager().isPlayerTagged(player.getUniqueId());
        }
        
        return false;
    }
    
    /**
     * Executa o teletransporte com delay.
     */
    private void executeTeleportWithDelay(Player requester, Player accepter, TeleportRequest.RequestType type) {
        Player teleportingPlayer;
        Player destinationPlayer;
        
        if (type == TeleportRequest.RequestType.TPA) {
            teleportingPlayer = requester;
            destinationPlayer = accepter;
        } else {
            teleportingPlayer = accepter;
            destinationPlayer = requester;
        }
        
        // Notificar sobre o delay
        teleportingPlayer.sendMessage("§aTeletransportando em " + teleportDelaySeconds + " segundos... Não se mova!");
        destinationPlayer.sendMessage("§a" + teleportingPlayer.getName() + " será teletransportado em " + teleportDelaySeconds + " segundos!");
        
        // Executar teletransporte após delay
        new BukkitRunnable() {
            @Override
            public void run() {
                // Verificar se ambos ainda estão online
                if (!teleportingPlayer.isOnline() || !destinationPlayer.isOnline()) {
                    return;
                }
                
                // Verificar se não estão em combate
                if (isPlayerInCombat(teleportingPlayer) || isPlayerInCombat(destinationPlayer)) {
                    teleportingPlayer.sendMessage("§cTeletransporte cancelado! Um dos jogadores entrou em combate!");
                    destinationPlayer.sendMessage("§cTeletransporte cancelado! Um dos jogadores entrou em combate!");
                    return;
                }
                
                // Executar teletransporte
                Location destination = destinationPlayer.getLocation();
                teleportingPlayer.teleport(destination);
                
                // Notificar sucesso
                teleportingPlayer.sendMessage("§aTeletransporte realizado com sucesso!");
                destinationPlayer.sendMessage("§a" + teleportingPlayer.getName() + " foi teletransportado até você!");
                
                logger.info("✅ Teletransporte executado: " + teleportingPlayer.getName() + " -> " + destinationPlayer.getName());
            }
        }.runTaskLater(plugin, teleportDelaySeconds * 20L); // 20 ticks = 1 segundo
    }
    
    /**
     * Notifica que uma solicitação foi enviada.
     */
    private void notifyRequestSent(Player requester, Player target, TeleportRequest.RequestType type) {
        String typeText = type == TeleportRequest.RequestType.TPA ? "para você" : "para ir até ele";
        
        requester.sendMessage("§aSolicitação de teletransporte enviada para " + target.getName() + "!");
        target.sendMessage("§a" + requester.getName() + " solicitou teletransporte " + typeText + "!");
        target.sendMessage("§eUse §a/tpaccept §eou §c/tpdeny §epara responder!");
    }
    
    /**
     * Notifica que uma solicitação foi aceita.
     */
    private void notifyRequestAccepted(Player requester, Player accepter) {
        requester.sendMessage("§a" + accepter.getName() + " aceitou sua solicitação de teletransporte!");
        accepter.sendMessage("§aVocê aceitou a solicitação de teletransporte de " + requester.getName() + "!");
    }
    
    /**
     * Carrega as configurações do sistema.
     */
    private void loadConfigurations() {
        requestExpirationSeconds = plugin.getConfig().getInt("tpa.request-expiration", 60);
        requestCooldownSeconds = plugin.getConfig().getInt("tpa.request-cooldown", 30);
        teleportDelaySeconds = plugin.getConfig().getInt("tpa.teleport-delay", 5);
        
        logger.info("✅ Configurações TPA carregadas: expiração=" + requestExpirationSeconds + 
                   "s, cooldown=" + requestCooldownSeconds + "s, delay=" + teleportDelaySeconds + "s");
    }
    
    /**
     * Inicializa a task de limpeza automática.
     */
    private void initializeCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanExpiredRequests();
            }
        }.runTaskTimer(plugin, 20L, 20L); // Executa a cada segundo
    }
    
    /**
     * Remove solicitações expiradas do cache.
     */
    private void cleanExpiredRequests() {
        Iterator<Map.Entry<UUID, TeleportRequest>> iterator = activeRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TeleportRequest> entry = iterator.next();
            TeleportRequest request = entry.getValue();
            if (request.isExpired()) {
                // Notificar jogadores sobre expiração
                Player target = Bukkit.getPlayer(request.getTargetName());
                Player requester = Bukkit.getPlayer(request.getRequesterName());
                
                if (target != null) {
                    target.sendMessage("§cA solicitação de teletransporte de " + request.getRequesterName() + " expirou!");
                }
                if (requester != null) {
                    requester.sendMessage("§cSua solicitação de teletransporte para " + request.getTargetName() + " expirou!");
                }
                
                logger.info("🧹 Solicitação TPA expirada removida: " + request.getRequesterName() + " -> " + request.getTargetName());
                iterator.remove();
            }
        }
    }
    
    /**
     * Limpa o cache quando um jogador sai do servidor.
     */
    public void onPlayerQuit(Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Remover solicitações onde o jogador é o target
        TeleportRequest request = activeRequests.remove(playerUuid);
        if (request != null) {
            Player requester = Bukkit.getPlayer(request.getRequesterName());
            if (requester != null) {
                requester.sendMessage("§c" + player.getName() + " saiu do servidor! Solicitação cancelada.");
            }
        }
        
        // Remover solicitações onde o jogador é o requester
        Iterator<Map.Entry<UUID, TeleportRequest>> iterator = activeRequests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TeleportRequest> entry = iterator.next();
            TeleportRequest req = entry.getValue();
            if (req.getRequesterUuid().equals(playerUuid)) {
                Player target = Bukkit.getPlayer(req.getTargetName());
                if (target != null) {
                    target.sendMessage("§c" + player.getName() + " saiu do servidor! Solicitação cancelada.");
                }
                iterator.remove();
            }
        }
        
        // Limpar cooldowns
        requestCooldowns.remove(playerUuid);
    }
}
