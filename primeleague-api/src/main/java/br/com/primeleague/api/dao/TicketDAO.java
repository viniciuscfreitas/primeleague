package br.com.primeleague.api.dao;

import br.com.primeleague.api.models.Ticket;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface para operações de persistência de tickets.
 * Define contratos assíncronos para todas as operações de banco de dados.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public interface TicketDAO {

    /**
     * Cria um novo ticket no banco de dados.
     * 
     * @param ticket Ticket a ser criado
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void createTicketAsync(Ticket ticket, Consumer<Boolean> callback);

    /**
     * Atualiza um ticket existente.
     * 
     * @param ticket Ticket com dados atualizados
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void updateTicketAsync(Ticket ticket, Consumer<Boolean> callback);

    /**
     * Busca um ticket por ID.
     * 
     * @param ticketId ID do ticket
     * @param callback Callback com o ticket encontrado (null se não existir)
     */
    void getTicketByIdAsync(int ticketId, Consumer<Ticket> callback);

    /**
     * Busca tickets de um jogador.
     * 
     * @param playerId ID do jogador
     * @param callback Callback com lista de tickets do jogador
     */
    void getTicketsByPlayerAsync(int playerId, Consumer<List<Ticket>> callback);

    /**
     * Busca tickets por status.
     * 
     * @param status Status dos tickets (OPEN, IN_PROGRESS, CLOSED, etc.)
     * @param callback Callback com lista de tickets do status especificado
     */
    void getTicketsByStatusAsync(String status, Consumer<List<Ticket>> callback);

    /**
     * Busca tickets atribuídos a um staff.
     * 
     * @param staffPlayerId ID do staff responsável
     * @param callback Callback com lista de tickets atribuídos ao staff
     */
    void getTicketsByStaffAsync(int staffPlayerId, Consumer<List<Ticket>> callback);

    /**
     * Busca todos os tickets abertos.
     * 
     * @param callback Callback com lista de tickets abertos
     */
    void getOpenTicketsAsync(Consumer<List<Ticket>> callback);

    /**
     * Busca tickets por prioridade.
     * 
     * @param priority Prioridade dos tickets (LOW, MEDIUM, HIGH, URGENT)
     * @param callback Callback com lista de tickets da prioridade especificada
     */
    void getTicketsByPriorityAsync(String priority, Consumer<List<Ticket>> callback);

    /**
     * Remove um ticket do banco de dados.
     * 
     * @param ticketId ID do ticket a ser removido
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void deleteTicketAsync(int ticketId, Consumer<Boolean> callback);

    /**
     * Busca tickets com filtros combinados.
     * 
     * @param status Status dos tickets (opcional, null para ignorar)
     * @param priority Prioridade dos tickets (opcional, null para ignorar)
     * @param staffPlayerId ID do staff (opcional, null para ignorar)
     * @param callback Callback com lista de tickets filtrados
     */
    void getTicketsWithFiltersAsync(String status, String priority, Integer staffPlayerId, Consumer<List<Ticket>> callback);
}
