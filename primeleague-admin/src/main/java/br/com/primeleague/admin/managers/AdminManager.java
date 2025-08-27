package br.com.primeleague.admin.managers;

import br.com.primeleague.admin.models.Punishment;
import br.com.primeleague.admin.models.Ticket;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
// import br.com.primeleague.p2p.api.P2PAccessAPI; // Temporariamente comentado
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia todas as operações administrativas do sistema.
 * Responsável por punições, tickets e modo staff.
 * 
 * REFATORADO para usar player_id como fonte única da verdade:
 * - Usa IdentityManager para obter player_id de jogadores
 * - Compatível com schema migrado para player_id
 * - Remove queries diretas para player_data
 * - Integração total com sistema de player_id
 * 
 * @author PrimeLeague Team
 * @version 3.0.0 (Refatorado para player_id)
 */
public class AdminManager {

    private static AdminManager instance;
    private final Set<Integer> vanishedPlayerIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Long> lastReportTime = new ConcurrentHashMap<>();

    private AdminManager() {
        // Construtor privado para Singleton
    }

    // ==================== PUNIÇÕES ====================

    /**
     * Aplica uma punição a um jogador.
     * REFATORADO: Usa player_id como identificador principal.
     */
    public boolean applyPunishment(Punishment punishment) {
        // Obter player_id do alvo
        int targetPlayerId = getPlayerIdFromPunishment(punishment);
        if (targetPlayerId == -1) {
            return false; // Jogador não encontrado
        }

        // Verificar se já existe uma punição ativa do mesmo tipo
        Punishment existingPunishment = getActivePunishment(targetPlayerId, punishment.getType());
        if (existingPunishment != null) {
            // Se já existe uma punição ativa, desativar a anterior
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE punishments SET is_active = FALSE WHERE id = ?")) {

                stmt.setInt(1, existingPunishment.getId());
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Obter player_id do autor (se houver)
        Integer authorPlayerId = null;
        if (punishment.getAuthorUuid() != null) {
            authorPlayerId = getPlayerIdByUuid(punishment.getAuthorUuid());
        }

        // Obter nomes via IdentityManager
        String targetName = getPlayerNameByPlayerId(targetPlayerId);
        String authorName = authorPlayerId != null ? getPlayerNameByPlayerId(authorPlayerId) : null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO punishments (type, target_player_id, target_name, author_player_id, author_name, reason, created_at, expires_at, is_active) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, punishment.getType().name());
            stmt.setInt(2, targetPlayerId);
            stmt.setString(3, targetName);
            stmt.setObject(4, authorPlayerId); // Pode ser null
            stmt.setString(5, authorName);
            stmt.setString(6, punishment.getReason());
            stmt.setTimestamp(7, punishment.getCreatedAt());
            stmt.setTimestamp(8, punishment.getExpiresAt());
            stmt.setBoolean(9, punishment.isActive());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    punishment.setId(rs.getInt(1));
                }

                // Definir nomes no objeto punishment
                punishment.setTargetName(targetName);
                punishment.setAuthorName(authorName);

                // Aplicar efeitos imediatos
                applyPunishmentEffects(punishment, targetPlayerId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Aplica os efeitos imediatos de uma punição.
     * REFATORADO: Usa player_id para encontrar jogador.
     */
    private void applyPunishmentEffects(Punishment punishment, int targetPlayerId) {
        // Encontrar jogador pelo player_id
        Player target = PrimeLeagueAPI.getIdentityManager().getOnlinePlayer(targetPlayerId);

        if (target == null) {
            Bukkit.getLogger().warning("[ADMIN] Jogador não encontrado online para aplicar efeitos: player_id=" + targetPlayerId);
            return;
        }

        switch (punishment.getType()) {
            case KICK:
                target.kickPlayer("§cVocê foi expulso por: " + punishment.getReason());
                break;
            case BAN:
                // Sistema de banimento CUSTOMIZADO (não usar setBanned nativo)
                String banReason = "§c§lVOCÊ FOI BANIDO!\n\n" +
                              "§7Motivo: §f" + punishment.getReason() + "\n" +
                              "§7Staff: §f" + (punishment.getAuthorName() != null ? punishment.getAuthorName() : "Console") + "\n\n" +
                              "§7Para recurso, acesse nosso Discord.\n" +
                              "§7Código do ban: §f#" + punishment.getId();

                // Expulsar o jogador com mensagem customizada
                target.kickPlayer(banReason);

                // Log do banimento customizado
                Bukkit.getLogger().info("[ADMIN] Ban customizado aplicado para " + target.getName() + " (ID: " + punishment.getId() + ")");

                // Revogar acesso P2P
                // if (P2PAccessAPI.isAvailable()) {
                //     P2PAccessAPI.revokeAccess(punishment.getTargetUuid(), "Banned: " + punishment.getReason());
                // }
                break;
            case MUTE:
                // O mute será verificado no PlayerChatEvent
                target.sendMessage("§cVocê foi silenciado por: " + punishment.getReason());
                break;
            case WARN:
                target.sendMessage("§cVocê foi avisado por: " + punishment.getReason());
                break;
        }
    }

    /**
     * Verifica se um jogador tem uma punição ativa de um tipo específico.
     * REFATORADO: Usa player_id como identificador.
     */
    public Punishment getActivePunishment(int playerId, Punishment.Type type) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM punishments WHERE target_player_id = ? AND type = ? AND is_active = TRUE " +
                 "AND (expires_at IS NULL OR expires_at > NOW()) ORDER BY created_at DESC LIMIT 1")) {

            stmt.setInt(1, playerId);
            stmt.setString(2, type.name());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToPunishment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Verifica se um jogador tem uma punição ativa de um tipo específico.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public Punishment getActivePunishment(UUID playerUuid, Punishment.Type type) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return null;
        }
        return getActivePunishment(playerId, type);
    }
    
    /**
     * Verifica se um jogador está mutado.
     * REFATORADO: Usa player_id como identificador.
     * 
     * @param playerId ID numérico do jogador
     * @return true se o jogador está mutado, false caso contrário
     */
    public boolean isMuted(int playerId) {
        Punishment mute = getActivePunishment(playerId, Punishment.Type.MUTE);
        return mute != null && mute.isCurrentlyActive();
    }

    /**
     * Verifica se um jogador está mutado.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public boolean isMuted(UUID playerUuid) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return false;
        }
        return isMuted(playerId);
    }

    /**
     * Obtém o histórico de punições de um jogador com nomes.
     * REFATORADO: Usa player_id como identificador.
     */
    public List<Punishment> getPlayerHistory(int playerId) {
        List<Punishment> history = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM punishments WHERE target_player_id = ? ORDER BY created_at DESC")) {

            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(mapResultSetToPunishmentWithNames(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    /**
     * Obtém o histórico de punições de um jogador com nomes.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public List<Punishment> getPlayerHistory(UUID playerUuid) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return new ArrayList<>();
        }
        return getPlayerHistory(playerId);
    }

    /**
     * Aplica perdão a uma punição ativa.
     * REFATORADO: Usa player_id como identificador.
     */
    public boolean pardonPunishment(int targetPlayerId, Punishment.Type type, int pardonerPlayerId, String pardonReason) {
        // Obter nome do perdoador via IdentityManager
        String pardonerName = getPlayerNameByPlayerId(pardonerPlayerId);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE punishments SET is_active = FALSE, pardoned_by_player_id = ?, pardoned_by_name = ?, pardoned_at = NOW(), " +
                 "pardon_reason = ? WHERE target_player_id = ? AND type = ? AND is_active = TRUE")) {

            stmt.setInt(1, pardonerPlayerId);
            stmt.setString(2, pardonerName);
            stmt.setString(3, pardonReason);
            stmt.setInt(4, targetPlayerId);
            stmt.setString(5, type.name());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                // Se era um ban, apenas log (sistema customizado - não usa setBanned nativo)
                if (type == Punishment.Type.BAN) {
                    String playerName = getPlayerNameByPlayerId(targetPlayerId);
                    Bukkit.getLogger().info("[ADMIN] Ban customizado removido para " + playerName + " (player_id: " + targetPlayerId + ")");
                }

                // Restaurar acesso P2P se era um ban
                // if (type == Punishment.Type.BAN && P2PAccessAPI.isAvailable()) {
                //     P2PAccessAPI.grantAccess(targetUuid, "Punishment pardoned");
                // }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Aplica perdão a uma punição ativa.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public boolean pardonPunishment(UUID targetUuid, Punishment.Type type, UUID pardonerUuid, String pardonReason) {
        Integer targetPlayerId = getPlayerIdByUuid(targetUuid);
        Integer pardonerPlayerId = getPlayerIdByUuid(pardonerUuid);
        
        if (targetPlayerId == null || pardonerPlayerId == null) {
            return false;
        }
        
        return pardonPunishment(targetPlayerId, type, pardonerPlayerId, pardonReason);
    }

    // ==================== TICKETS ====================

    /**
     * Cria um novo ticket de denúncia.
     * REFATORADO: Usa player_id como identificador.
     */
    public boolean createTicket(Ticket ticket) {
        // Obter player_ids
        int reporterPlayerId = getPlayerIdByUuid(ticket.getReporterUuid());
        int targetPlayerId = getPlayerIdByUuid(ticket.getTargetUuid());
        
        if (reporterPlayerId == -1 || targetPlayerId == -1) {
            return false; // Jogador não encontrado
        }

        // Verificar limite diário
        if (hasReachedDailyLimit(reporterPlayerId)) {
            return false;
        }

        // Obter nomes via IdentityManager
        String reporterName = getPlayerNameByPlayerId(reporterPlayerId);
        String targetName = getPlayerNameByPlayerId(targetPlayerId);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO tickets (status, reporter_player_id, reporter_name, target_player_id, target_name, reason, created_at) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, ticket.getStatus().name());
            stmt.setInt(2, reporterPlayerId);
            stmt.setString(3, reporterName);
            stmt.setInt(4, targetPlayerId);
            stmt.setString(5, targetName);
            stmt.setString(6, ticket.getReason());
            stmt.setTimestamp(7, ticket.getCreatedAt());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    ticket.setId(rs.getInt(1));
                }

                // Definir nomes no objeto ticket
                ticket.setReporterName(reporterName);
                ticket.setTargetName(targetName);

                // Registrar tempo do report
                lastReportTime.put(reporterPlayerId, System.currentTimeMillis());
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Obtém tickets com filtros opcionais.
     * REFATORADO: Usa nomes já armazenados no banco.
     */
    public List<Ticket> getTickets(Ticket.Status status, int limit, int offset) {
        List<Ticket> tickets = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM tickets");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" WHERE status = ?");
            params.add(status.name());
        }

        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tickets.add(mapResultSetToTicketWithNames(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    /**
     * Obtém um ticket específico por ID.
     */
    public Ticket getTicket(int ticketId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tickets WHERE id = ?")) {

            stmt.setInt(1, ticketId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTicket(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Reivindica um ticket para um membro da equipe.
     * REFATORADO: Usa player_id como identificador.
     */
    public boolean claimTicket(int ticketId, int staffPlayerId) {
        // Obter nome do staff via IdentityManager
        String staffName = getPlayerNameByPlayerId(staffPlayerId);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE tickets SET claimed_by_player_id = ?, claimed_by_name = ?, status = 'CLAIMED', claimed_at = NOW() " +
                 "WHERE id = ? AND status = 'OPEN'")) {

            stmt.setInt(1, staffPlayerId);
            stmt.setString(2, staffName);
            stmt.setInt(3, ticketId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reivindica um ticket para um membro da equipe.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public boolean claimTicket(int ticketId, UUID staffUuid) {
        Integer staffPlayerId = getPlayerIdByUuid(staffUuid);
        if (staffPlayerId == null) {
            return false;
        }
        return claimTicket(ticketId, staffPlayerId);
    }

    /**
     * Fecha um ticket com resolução.
     */
    public boolean closeTicket(int ticketId, Ticket.Status finalStatus, String resolutionNotes) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE tickets SET status = ?, resolution = ?, closed_at = NOW() WHERE id = ?")) {

            stmt.setString(1, finalStatus.name());
            stmt.setString(2, resolutionNotes);
            stmt.setInt(3, ticketId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== MODO STAFF ====================

    /**
     * Ativa/desativa o modo vanish para um jogador.
     * REFATORADO: Usa player_id como identificador.
     */
    public boolean toggleVanish(int playerId, boolean enabled, Integer enabledByPlayerId) {
        if (enabled) {
            vanishedPlayerIds.add(playerId);
        } else {
            vanishedPlayerIds.remove(playerId);
        }

        // Obter nomes via IdentityManager
        String playerName = getPlayerNameByPlayerId(playerId);
        String enabledByName = enabledByPlayerId != null ? getPlayerNameByPlayerId(enabledByPlayerId) : null;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO staff_vanish (player_id, enabled, enabled_at, enabled_by_player_id) " +
                 "VALUES (?, ?, NOW(), ?) " +
                 "ON DUPLICATE KEY UPDATE enabled = ?, enabled_at = NOW(), enabled_by_player_id = ?")) {

            stmt.setInt(1, playerId);
            stmt.setBoolean(2, enabled);
            stmt.setObject(3, enabledByPlayerId); // Pode ser null
            stmt.setBoolean(4, enabled);
            stmt.setObject(5, enabledByPlayerId); // Pode ser null

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Ativa/desativa o modo vanish para um jogador.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public boolean toggleVanish(UUID playerUuid, boolean enabled, UUID enabledByUuid) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        Integer enabledByPlayerId = enabledByUuid != null ? getPlayerIdByUuid(enabledByUuid) : null;
        
        if (playerId == null) {
            return false;
        }
        
        return toggleVanish(playerId, enabled, enabledByPlayerId);
    }

    /**
     * Verifica se um jogador está em modo vanish.
     * REFATORADO: Usa player_id como identificador.
     */
    public boolean isVanished(int playerId) {
        return vanishedPlayerIds.contains(playerId);
    }

    /**
     * Verifica se um jogador está em modo vanish.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public boolean isVanished(UUID playerUuid) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        if (playerId == null) {
            return false;
        }
        return isVanished(playerId);
    }

    /**
     * Carrega o estado de vanish de um jogador do banco de dados.
     * REFATORADO: Usa player_id como identificador.
     */
    public void loadVanishState(int playerId) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT enabled FROM staff_vanish WHERE player_id = ?")) {

            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getBoolean("enabled")) {
                vanishedPlayerIds.add(playerId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carrega o estado de vanish de um jogador do banco de dados.
     * MÉTODO LEGADO: Mantido para compatibilidade.
     */
    @Deprecated
    public void loadVanishState(UUID playerUuid) {
        Integer playerId = getPlayerIdByUuid(playerUuid);
        if (playerId != null) {
            loadVanishState(playerId);
        }
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Verifica se um jogador atingiu o limite diário de reports.
     * REFATORADO: Usa player_id como identificador.
     */
    private boolean hasReachedDailyLimit(int playerId) {
        Long lastReport = lastReportTime.get(playerId);
        if (lastReport == null) return false;

        long dayInMillis = 24 * 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastReport) < dayInMillis;
    }

    /**
     * Obtém uma conexão com o banco de dados.
     */
    private Connection getConnection() throws SQLException {
        return PrimeLeagueAPI.getDataManager().getConnection();
    }

    /**
     * Obtém o player_id de um jogador a partir de uma punição.
     * 
     * @param punishment Punição contendo UUID do alvo
     * @return player_id ou -1 se não encontrado
     */
    private int getPlayerIdFromPunishment(Punishment punishment) {
        Integer playerId = getPlayerIdByUuid(punishment.getTargetUuid());
        return playerId != null ? playerId : -1;
    }

    /**
     * Obtém o player_id de um jogador pelo UUID.
     * 
     * @param uuid UUID do jogador
     * @return player_id ou null se não encontrado
     */
    private Integer getPlayerIdByUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(uuid);
    }

    /**
     * Obtém o nome de um jogador pelo player_id.
     * 
     * @param playerId ID numérico do jogador
     * @return Nome do jogador ou "Desconhecido" se não encontrado
     */
    private String getPlayerNameByPlayerId(int playerId) {
        String name = PrimeLeagueAPI.getIdentityManager().getNameByPlayerId(playerId);
        return name != null ? name : "Desconhecido";
    }

    /**
     * Obtém o nome de um jogador via DataManager (fonte única da verdade).
     * MÉTODO LEGADO: Mantido para compatibilidade.
     * 
     * @param uuid UUID do jogador
     * @return Nome do jogador ou UUID formatado se não encontrado
     * @deprecated Use getPlayerNameByPlayerId(int) instead
     */
    @Deprecated
    public String getPlayerNameFromDataManager(UUID uuid) {
        if (uuid == null) {
            return "Console";
        }

        // REFATORADO: Comparar usando player_id em vez de UUID diretamente
        for (Player player : Bukkit.getOnlinePlayers()) {
            Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(player.getUniqueId());
            Integer targetPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(uuid);
            if (playerId != null && targetPlayerId != null && playerId.equals(targetPlayerId)) {
                return player.getName();
            }
        }

        // Se não estiver online, usar DataManager
        try {
            PlayerProfile profile = PrimeLeagueAPI.getDataManager().loadOfflinePlayerProfile(uuid);
            if (profile != null) {
                return profile.getPlayerName();
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ADMIN] Erro ao carregar perfil para " + uuid + ": " + e.getMessage());
        }

        // Se não encontrar, retornar UUID formatado
        return uuid.toString().substring(0, 8) + "...";
    }

    /**
     * Mapeia um ResultSet para um objeto Punishment.
     * REFATORADO: Usa player_id em vez de UUID.
     */
    private Punishment mapResultSetToPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(rs.getInt("id"));
        punishment.setType(Punishment.Type.valueOf(rs.getString("type")));
        
        // Obter UUID a partir do player_id
        int targetPlayerId = rs.getInt("target_player_id");
        UUID targetUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(targetPlayerId);
        punishment.setTargetUuid(targetUuid);

        Integer authorPlayerId = rs.getObject("author_player_id", Integer.class);
        if (authorPlayerId != null) {
            UUID authorUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(authorPlayerId);
            punishment.setAuthorUuid(authorUuid);
        }

        punishment.setReason(rs.getString("reason"));
        punishment.setCreatedAt(rs.getTimestamp("created_at"));
        punishment.setExpiresAt(rs.getTimestamp("expires_at"));
        punishment.setActive(rs.getBoolean("active"));

        Integer pardonedByPlayerId = rs.getObject("pardoned_by_player_id", Integer.class);
        if (pardonedByPlayerId != null) {
            UUID pardonedByUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(pardonedByPlayerId);
            punishment.setPardonedByUuid(pardonedByUuid);
        }

        punishment.setPardonedAt(rs.getTimestamp("pardoned_at"));
        punishment.setPardonReason(rs.getString("pardon_reason"));

        return punishment;
    }

    /**
     * Mapeia um ResultSet para um objeto Punishment com nomes dos jogadores.
     * REFATORADO: Usa nomes já armazenados no banco.
     */
    private Punishment mapResultSetToPunishmentWithNames(ResultSet rs) throws SQLException {
        Punishment punishment = mapResultSetToPunishment(rs);

        // Adicionar nomes diretamente das colunas do banco
        String targetName = rs.getString("target_name");
        String authorName = rs.getString("author_name");
        String pardonedByName = rs.getString("pardoned_by_name");

        if (targetName != null) {
            punishment.setTargetName(targetName);
        }
        if (authorName != null) {
            punishment.setAuthorName(authorName);
        }
        if (pardonedByName != null) {
            punishment.setPardonedByName(pardonedByName);
        }

        return punishment;
    }

    /**
     * Mapeia um ResultSet para um objeto Ticket.
     * REFATORADO: Usa player_id em vez de UUID.
     */
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        ticket.setStatus(Ticket.Status.valueOf(rs.getString("status")));
        
        // Obter UUIDs a partir dos player_ids
        int reporterPlayerId = rs.getInt("reporter_player_id");
        int targetPlayerId = rs.getInt("target_player_id");
        
        UUID reporterUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(reporterPlayerId);
        UUID targetUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(targetPlayerId);
        
        ticket.setReporterUuid(reporterUuid);
        ticket.setTargetUuid(targetUuid);
        ticket.setReason(rs.getString("reason"));
        // evidence_link removido do schema - não mais utilizado

        Integer claimedByPlayerId = rs.getObject("claimed_by_player_id", Integer.class);
        if (claimedByPlayerId != null) {
            UUID claimedByUuid = PrimeLeagueAPI.getIdentityManager().getUuidByPlayerId(claimedByPlayerId);
            ticket.setClaimedByUuid(claimedByUuid);
        }

        ticket.setResolutionNotes(rs.getString("resolution"));
        ticket.setCreatedAt(rs.getTimestamp("created_at"));
        // updated_at removido do schema - usar claimed_at ou closed_at conforme apropriado

        return ticket;
    }

    /**
     * Mapeia um ResultSet para um objeto Ticket com nomes dos jogadores.
     * REFATORADO: Usa nomes já armazenados no banco.
     */
    private Ticket mapResultSetToTicketWithNames(ResultSet rs) throws SQLException {
        Ticket ticket = mapResultSetToTicket(rs);

        // Adicionar nomes diretamente das colunas do banco
        String reporterName = rs.getString("reporter_name");
        String targetName = rs.getString("target_name");
        String claimedName = rs.getString("claimed_by_name");

        if (reporterName != null) {
            ticket.setReporterName(reporterName);
        }
        if (targetName != null) {
            ticket.setTargetName(targetName);
        }
        if (claimedName != null) {
            ticket.setClaimedByName(claimedName);
        }

        return ticket;
    }

    /**
     * Obtém o nome de um jogador pelo UUID.
     * MÉTODO LEGADO: Mantido para compatibilidade, mas prefira getPlayerNameByPlayerId.
     * 
     * @param uuid UUID do jogador
     * @return Nome do jogador ou UUID formatado se não encontrado
     * @deprecated Use getPlayerNameByPlayerId(int) instead
     */
    @Deprecated
    public String getPlayerName(UUID uuid) {
        return getPlayerNameFromDataManager(uuid);
    }

    /**
     * Obtém a instância singleton do AdminManager.
     */
    public static AdminManager getInstance() {
        if (instance == null) {
            instance = new AdminManager();
        }
        return instance;
    }
}
