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
import java.util.stream.Collectors;

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

    /**
     * Obtém punição ativa de um jogador por UUID e tipo.
     */
    public Punishment getActivePunishment(UUID playerUuid, Punishment.Type type) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return null;
        }
        
        // Buscar punição ativa de forma síncrona
        final Punishment[] result = {null};
        punishmentDAO.getActivePunishmentByTypeAsync(playerId, type.toString(), (punishment) -> {
            result[0] = punishment;
        });
        
        return result[0];
    }

    /**
     * Obtém punição ativa de um jogador por player_id e tipo.
     */
    public Punishment getActivePunishment(Integer playerId, Punishment.Type type) {
        if (playerId == null) {
            return null;
        }
        
        // Buscar punição ativa de forma síncrona
        final Punishment[] result = {null};
        punishmentDAO.getActivePunishmentByTypeAsync(playerId, type.toString(), (punishment) -> {
            result[0] = punishment;
        });
        
        return result[0];
    }

    /**
     * Aplica uma punição.
     */
    public void applyPunishment(Punishment punishment) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(punishment.getTargetUuid());
        if (playerId == null) {
            return;
        }
        
        applyPunishmentAsync(punishment, (success) -> {
            if (success) {
                plugin.getLogger().info("Punição aplicada: " + punishment.getPunishmentType() + " para " + punishment.getTargetUuid());
            }
        });
    }

    /**
     * Perdoa uma punição.
     */
    public void pardonPunishment(UUID targetUuid, Punishment.Type type, UUID adminUuid, String reason) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(targetUuid);
        if (playerId == null) {
            return;
        }
        
        // Buscar punição ativa e removê-la
        punishmentDAO.getActivePunishmentByTypeAsync(playerId, type.toString(), (punishment) -> {
            if (punishment != null) {
                punishmentDAO.removePunishmentAsync(punishment.getPunishmentId(), (success) -> {
                    if (success) {
                        plugin.getLogger().info("Punição perdoada: " + type + " para " + targetUuid + " por " + reason);
                    }
                });
            }
        });
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

    /**
     * Obtém tickets com filtros.
     */
    public List<Ticket> getTickets(Ticket.Status status, int limit, int offset) {
        final List<Ticket>[] result = new List[]{new ArrayList<>()};
        ticketDAO.getTicketsByStatusAsync(status.toString(), (tickets) -> {
            result[0] = tickets.stream()
                    .skip(offset)
                    .limit(limit)
                    .collect(Collectors.toList());
        });
        return result[0];
    }

    /**
     * Obtém um ticket por ID.
     */
    public Ticket getTicket(int ticketId) {
        final Ticket[] result = {null};
        ticketDAO.getTicketByIdAsync(ticketId, (ticket) -> {
            result[0] = ticket;
        });
        return result[0];
    }

    /**
     * Reivindica um ticket.
     */
    public void claimTicket(int ticketId, UUID staffUuid) {
        Integer staffPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(staffUuid);
        if (staffPlayerId == null) {
            return;
        }
        
        // Buscar o ticket e atualizar o staff
        ticketDAO.getTicketByIdAsync(ticketId, (ticket) -> {
            if (ticket != null) {
                ticket.setAssignedStaffId(staffPlayerId);
                ticketDAO.updateTicketAsync(ticket, (success) -> {
                    if (success) {
                        plugin.getLogger().info("Ticket " + ticketId + " reivindicado por " + staffUuid);
                    }
                });
            }
        });
    }

    /**
     * Fecha um ticket.
     */
    public void closeTicket(int ticketId, Ticket.Status status, String reason) {
        // Buscar o ticket e atualizar o status
        ticketDAO.getTicketByIdAsync(ticketId, (ticket) -> {
            if (ticket != null) {
                ticket.setStatus(status);
                ticketDAO.updateTicketAsync(ticket, (success) -> {
                    if (success) {
                        plugin.getLogger().info("Ticket " + ticketId + " fechado com status " + status);
                    }
                });
            }
        });
    }

    /**
     * Cria um ticket.
     */
    public void createTicket(Ticket ticket) {
        createTicketAsync(ticket, (success) -> {
            if (success) {
                plugin.getLogger().info("Ticket criado: " + ticket.getTitle());
            }
        });
    }

    /**
     * Verifica se um jogador está mutado.
     */
    public boolean isMuted(UUID playerUuid) {
        Punishment mute = getActivePunishment(playerUuid, Punishment.Type.MUTE);
        return mute != null;
    }

    /**
     * Obtém o nome de um jogador por UUID.
     */
    public String getPlayerName(UUID playerUuid) {
        PlayerProfile profile = PrimeLeagueAPI.getDataManager().getPlayerProfile(playerUuid);
        return profile != null ? profile.getPlayerName() : "Desconhecido";
    }

    /**
     * Obtém histórico de punições de um jogador.
     */
    public List<Punishment> getPlayerHistory(UUID playerUuid) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return new ArrayList<>();
        }
        
        final List<Punishment>[] result = new List[]{new ArrayList<>()};
        getPunishmentHistoryAsync(playerId, (history) -> {
            result[0] = history;
        });
        return result[0];
    }

    /**
     * Alterna o modo vanish de um jogador.
     */
    public void toggleVanish(int playerId, boolean enabled, int adminPlayerId) {
        if (enabled) {
            enableVanish(playerId);
        } else {
            disableVanish(playerId);
        }
    }

    /**
     * Carrega o estado de vanish de um jogador.
     */
    public void loadVanishState(UUID playerUuid) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUuid);
        if (playerId != null && isVanished(playerId)) {
            // Aplicar efeitos de vanish se necessário
        }
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
     * Verifica se um jogador está em modo vanish por UUID.
     */
    public boolean isVanished(UUID playerUuid) {
        Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(playerUuid);
        return playerId != null && isVanished(playerId);
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
            Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(punishment.getTargetUuid());
            return playerId != null ? playerId : -1;
        }
        
        return -1;
    }

    /**
     * Obtém o nome de um jogador pelo player_id.
     */
    private String getPlayerNameByPlayerId(int playerId) {
        PlayerProfile profile = PrimeLeagueAPI.getDataManager().getPlayerProfile(playerId);
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
