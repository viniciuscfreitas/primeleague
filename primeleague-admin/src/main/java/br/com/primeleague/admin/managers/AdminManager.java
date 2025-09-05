package br.com.primeleague.admin.managers;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.api.dao.PunishmentDAO;
import br.com.primeleague.api.dao.TicketDAO;
import br.com.primeleague.api.models.Punishment;
import br.com.primeleague.api.models.Ticket;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Gerencia todas as operações administrativas do sistema.
 * Responsável por punições, tickets e modo staff.
 * 
 * REFATORADO para Padrão-Ouro v3.0:
 * - Usa DAOs para operações de banco de dados
 * - Injeção de dependência via construtor
 * - Separação clara de responsabilidades
 * - Operações assíncronas para melhor performance
 * 
 * @author PrimeLeague Team
 * @version 4.0.0 (Refatorado para Padrão-Ouro v3.0)
 */
public class AdminManager {

    private final PrimeLeagueAdmin plugin;
    private final PunishmentDAO punishmentDAO;
    private final TicketDAO ticketDAO;
    private final Set<Integer> vanishedPlayerIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Long> lastReportTime = new ConcurrentHashMap<>();

    /**
     * Construtor do AdminManager.
     * 
     * @param plugin Instância do plugin principal
     * @param punishmentDAO Instância do DAO de punições (fornecida via injeção de dependência)
     * @param ticketDAO Instância do DAO de tickets (fornecida via injeção de dependência)
     */
    public AdminManager(PrimeLeagueAdmin plugin, PunishmentDAO punishmentDAO, TicketDAO ticketDAO) {
        this.plugin = plugin;
        this.punishmentDAO = punishmentDAO;
        this.ticketDAO = ticketDAO;
    }

    // ==================== PUNIÇÕES ====================

    /**
     * Aplica uma punição a um jogador.
     * REFATORADO: Usa DAO assíncrono para operações de banco de dados.
     */
    public void applyPunishmentAsync(Punishment punishment, Consumer<Boolean> callback) {
        // Obter player_id do alvo
        int targetPlayerId = getPlayerIdFromPunishment(punishment);
        if (targetPlayerId == -1) {
            callback.accept(false); // Jogador não encontrado
            return;
        }

        // Verificar se já existe uma punição ativa do mesmo tipo
        punishmentDAO.getActivePunishmentByTypeAsync(targetPlayerId, punishment.getPunishmentType(), (existingPunishment) -> {
            if (existingPunishment != null) {
                // Se já existe uma punição ativa, desativar a anterior
                punishmentDAO.removePunishmentAsync(existingPunishment.getPunishmentId(), (removed) -> {
                    // Continuar com a aplicação da nova punição
                    applyNewPunishment(punishment, targetPlayerId, callback);
                });
            } else {
                // Não há punição ativa, aplicar nova punição diretamente
                applyNewPunishment(punishment, targetPlayerId, callback);
            }
        });
    }

    /**
     * Aplica uma nova punição após verificar se não há conflitos.
     */
    private void applyNewPunishment(Punishment punishment, int targetPlayerId, Consumer<Boolean> callback) {
        // Obter player_id do autor (se houver)
        Integer authorPlayerId = null;
        if (punishment.getStaffId() != 0) {
            authorPlayerId = punishment.getStaffId();
        }

        // Obter nomes via IdentityManager
        String targetName = getPlayerNameByPlayerId(targetPlayerId);
        String authorName = authorPlayerId != null ? getPlayerNameByPlayerId(authorPlayerId) : null;

        // Definir dados no objeto punishment
        punishment.setPlayerId(targetPlayerId);
        if (authorPlayerId != null) {
            punishment.setStaffId(authorPlayerId);
        }

        // Aplicar punição via DAO
        punishmentDAO.applyPunishmentAsync(punishment, (success) -> {
            if (success) {
                // Log da punição aplicada
                plugin.getLogger().info("✅ Punição aplicada: " + punishment.getPunishmentType() + 
                    " para " + targetName + " por " + (authorName != null ? authorName : "Sistema"));
            }
            callback.accept(success);
        });
    }

    /**
     * Remove uma punição ativa.
     */
    public void removePunishmentAsync(int punishmentId, Consumer<Boolean> callback) {
        punishmentDAO.removePunishmentAsync(punishmentId, callback);
    }

    /**
     * Busca punições ativas de um jogador.
     */
    public void getActivePunishmentsAsync(int playerId, Consumer<List<Punishment>> callback) {
        punishmentDAO.getActivePunishmentsAsync(playerId, callback);
    }

    /**
     * Busca histórico de punições de um jogador.
     */
    public void getPunishmentHistoryAsync(int playerId, Consumer<List<Punishment>> callback) {
        punishmentDAO.getPunishmentHistoryAsync(playerId, callback);
    }

    /**
     * Verifica se um jogador tem punição ativa de um tipo específico.
     */
    public void getActivePunishmentByTypeAsync(int playerId, String punishmentType, Consumer<Punishment> callback) {
        punishmentDAO.getActivePunishmentByTypeAsync(playerId, punishmentType, callback);
    }

    // ==================== TICKETS ====================

    /**
     * Cria um novo ticket.
     */
    public void createTicketAsync(Ticket ticket, Consumer<Boolean> callback) {
        ticketDAO.createTicketAsync(ticket, callback);
    }

    /**
     * Atualiza um ticket existente.
     */
    public void updateTicketAsync(Ticket ticket, Consumer<Boolean> callback) {
        ticketDAO.updateTicketAsync(ticket, callback);
    }

    /**
     * Busca um ticket por ID.
     */
    public void getTicketByIdAsync(int ticketId, Consumer<Ticket> callback) {
        ticketDAO.getTicketByIdAsync(ticketId, callback);
    }

    /**
     * Busca tickets de um jogador.
     */
    public void getTicketsByPlayerAsync(int playerId, Consumer<List<Ticket>> callback) {
        ticketDAO.getTicketsByPlayerAsync(playerId, callback);
    }

    /**
     * Busca tickets abertos.
     */
    public void getOpenTicketsAsync(Consumer<List<Ticket>> callback) {
        ticketDAO.getOpenTicketsAsync(callback);
    }

    /**
     * Busca tickets por status.
     */
    public void getTicketsByStatusAsync(String status, Consumer<List<Ticket>> callback) {
        ticketDAO.getTicketsByStatusAsync(status, callback);
    }

    /**
     * Busca tickets atribuídos a um staff.
     */
    public void getTicketsByStaffAsync(int staffPlayerId, Consumer<List<Ticket>> callback) {
        ticketDAO.getTicketsByStaffAsync(staffPlayerId, callback);
    }

    // ==================== MODO STAFF ====================

    /**
     * Ativa o modo vanish para um jogador.
     */
    public void enableVanish(int playerId) {
        vanishedPlayerIds.add(playerId);
    }

    /**
     * Desativa o modo vanish para um jogador.
     */
    public void disableVanish(int playerId) {
        vanishedPlayerIds.remove(playerId);
    }

    /**
     * Verifica se um jogador está em modo vanish.
     */
    public boolean isVanished(int playerId) {
        return vanishedPlayerIds.contains(playerId);
    }

    /**
     * Obtém todos os jogadores em modo vanish.
     */
    public Set<Integer> getVanishedPlayers() {
        return new HashSet<>(vanishedPlayerIds);
    }

    // ==================== SISTEMA DE REPORTS ====================

    /**
     * Verifica se um jogador pode reportar (cooldown).
     */
    public boolean canReport(int playerId) {
        Long lastReport = lastReportTime.get(playerId);
        if (lastReport == null) {
            return true;
        }
        
        long cooldownTime = 5 * 60 * 1000; // 5 minutos em millisegundos
        return System.currentTimeMillis() - lastReport > cooldownTime;
    }

    /**
     * Registra o tempo do último report de um jogador.
     */
    public void recordReport(int playerId) {
        lastReportTime.put(playerId, System.currentTimeMillis());
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Obtém o player_id de uma punição.
     */
    private int getPlayerIdFromPunishment(Punishment punishment) {
        if (punishment.getPlayerId() != 0) {
            return punishment.getPlayerId();
        }
        
        // Se não tem player_id, tentar obter via UUID
        if (punishment.getTargetUuid() != null) {
            return PrimeLeagueAPI.getIdentityManager().getPlayerId(punishment.getTargetUuid());
        }
        
        return -1;
    }

    /**
     * Obtém o nome de um jogador pelo player_id.
     */
    private String getPlayerNameByPlayerId(int playerId) {
        PlayerProfile profile = PrimeLeagueAPI.getIdentityManager().getPlayerProfile(playerId);
        return profile != null ? profile.getPlayerName() : "Desconhecido";
    }

    /**
     * Obtém a instância do PunishmentDAO para registro no Core.
     * 
     * @return Instância do PunishmentDAO
     */
    public PunishmentDAO getPunishmentDAO() {
        return punishmentDAO;
    }

    /**
     * Obtém a instância do TicketDAO para registro no Core.
     * 
     * @return Instância do TicketDAO
     */
    public TicketDAO getTicketDAO() {
        return ticketDAO;
    }
}
