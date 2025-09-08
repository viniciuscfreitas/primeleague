package br.com.primeleague.admin.dao;

import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.api.dao.TicketDAO;
import br.com.primeleague.api.models.Ticket;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Implementação MySQL para operações de persistência de tickets.
 * Todas as operações são assíncronas para não bloquear a thread principal.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class MySqlTicketDAO implements TicketDAO {

    private final PrimeLeagueAdmin plugin;
    private final java.util.logging.Logger logger;

    public MySqlTicketDAO(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public void createTicketAsync(Ticket ticket, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "INSERT INTO admin_tickets (player_id, title, description, priority, " +
                           "status, assigned_staff_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, ticket.getPlayerId());
                    stmt.setString(2, ticket.getTitle());
                    stmt.setString(3, ticket.getDescription());
                    stmt.setString(4, ticket.getPriority());
                    stmt.setString(5, ticket.getStatus().toString());
                    stmt.setObject(6, ticket.getAssignedStaffId());
                    stmt.setTimestamp(7, ticket.getCreatedAt());
                    stmt.setTimestamp(8, ticket.getUpdatedAt());
                    
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    if (success && ticket.getTicketId() == 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                ticket.setTicketId(generatedKeys.getInt(1));
                            }
                        }
                    }
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao criar ticket: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void updateTicketAsync(Ticket ticket, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "UPDATE admin_tickets SET title = ?, description = ?, priority = ?, " +
                           "status = ?, assigned_staff_id = ?, updated_at = ? WHERE ticket_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, ticket.getTitle());
                    stmt.setString(2, ticket.getDescription());
                    stmt.setString(3, ticket.getPriority());
                    stmt.setString(4, ticket.getStatus().toString());
                    stmt.setObject(5, ticket.getAssignedStaffId());
                    stmt.setTimestamp(6, ticket.getUpdatedAt());
                    stmt.setInt(7, ticket.getTicketId());
                    
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao atualizar ticket: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void getTicketByIdAsync(int ticketId, Consumer<Ticket> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE ticket_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, ticketId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        final Ticket ticket = rs.next() ? mapResultSetToTicket(rs) : null;
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(ticket));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar ticket por ID: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    @Override
    public void getTicketsByPlayerAsync(int playerId, Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE player_id = ? ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, playerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets por jogador: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getTicketsByStatusAsync(String status, Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE status = ? ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, status);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets por status: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getTicketsByStaffAsync(int staffPlayerId, Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE assigned_staff_id = ? ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, staffPlayerId);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets por staff: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getOpenTicketsAsync(Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE status IN ('OPEN', 'IN_PROGRESS') ORDER BY priority DESC, created_at ASC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets abertos: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void getTicketsByPriorityAsync(String priority, Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "SELECT * FROM admin_tickets WHERE priority = ? ORDER BY created_at DESC";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, priority);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets por prioridade: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    @Override
    public void deleteTicketAsync(int ticketId, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                String sql = "DELETE FROM admin_tickets WHERE ticket_id = ?";
                
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, ticketId);
                    int affectedRows = stmt.executeUpdate();
                    boolean success = affectedRows > 0;
                    
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao deletar ticket: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
            }
        });
    }

    @Override
    public void getTicketsWithFiltersAsync(String status, String priority, Integer staffPlayerId, Consumer<List<Ticket>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = PrimeLeagueAPI.getDataManager().getConnection()) {
                StringBuilder sql = new StringBuilder("SELECT * FROM admin_tickets WHERE 1=1");
                List<Object> parameters = new ArrayList<>();
                
                if (status != null) {
                    sql.append(" AND status = ?");
                    parameters.add(status);
                }
                
                if (priority != null) {
                    sql.append(" AND priority = ?");
                    parameters.add(priority);
                }
                
                if (staffPlayerId != null) {
                    sql.append(" AND assigned_staff_id = ?");
                    parameters.add(staffPlayerId);
                }
                
                sql.append(" ORDER BY created_at DESC");
                
                try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                    for (int i = 0; i < parameters.size(); i++) {
                        stmt.setObject(i + 1, parameters.get(i));
                    }
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        List<Ticket> tickets = new ArrayList<>();
                        while (rs.next()) {
                            tickets.add(mapResultSetToTicket(rs));
                        }
                        
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(tickets));
                    }
                }
            } catch (SQLException e) {
                logger.severe("❌ Erro ao buscar tickets com filtros: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(new ArrayList<>()));
            }
        });
    }

    /**
     * Mapeia um ResultSet para um objeto Ticket.
     */
    private Ticket mapResultSetToTicket(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setTicketId(rs.getInt("ticket_id"));
        ticket.setPlayerId(rs.getInt("player_id"));
        ticket.setTitle(rs.getString("title"));
        ticket.setDescription(rs.getString("description"));
        ticket.setPriority(rs.getString("priority"));
        ticket.setStatus(Ticket.Status.valueOf(rs.getString("status")));
        
        Integer assignedStaffId = rs.getObject("assigned_staff_id", Integer.class);
        ticket.setAssignedStaffId(assignedStaffId);
        
        ticket.setCreatedAt(rs.getTimestamp("created_at"));
        ticket.setUpdatedAt(rs.getTimestamp("updated_at"));
        return ticket;
    }
}
