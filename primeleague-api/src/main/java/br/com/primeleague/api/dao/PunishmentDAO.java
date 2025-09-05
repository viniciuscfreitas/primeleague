package br.com.primeleague.api.dao;

import br.com.primeleague.api.models.Punishment;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface para operações de persistência de punições.
 * Define contratos assíncronos para todas as operações de banco de dados.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public interface PunishmentDAO {

    /**
     * Aplica uma punição no banco de dados.
     * 
     * @param punishment Punição a ser aplicada
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void applyPunishmentAsync(Punishment punishment, Consumer<Boolean> callback);

    /**
     * Remove uma punição do banco de dados.
     * 
     * @param punishmentId ID da punição a ser removida
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void removePunishmentAsync(int punishmentId, Consumer<Boolean> callback);

    /**
     * Busca punições ativas de um jogador.
     * 
     * @param playerId ID do jogador
     * @param callback Callback com lista de punições ativas
     */
    void getActivePunishmentsAsync(int playerId, Consumer<List<Punishment>> callback);

    /**
     * Busca histórico de punições de um jogador.
     * 
     * @param playerId ID do jogador
     * @param callback Callback com lista de punições (histórico completo)
     */
    void getPunishmentHistoryAsync(int playerId, Consumer<List<Punishment>> callback);

    /**
     * Busca punições por tipo.
     * 
     * @param punishmentType Tipo da punição (BAN, MUTE, WARN, etc.)
     * @param callback Callback com lista de punições do tipo especificado
     */
    void getPunishmentsByTypeAsync(String punishmentType, Consumer<List<Punishment>> callback);

    /**
     * Verifica se um jogador tem punição ativa de um tipo específico.
     * 
     * @param playerId ID do jogador
     * @param punishmentType Tipo da punição
     * @param callback Callback com punição ativa (null se não houver)
     */
    void getActivePunishmentByTypeAsync(int playerId, String punishmentType, Consumer<Punishment> callback);

    /**
     * Busca punições aplicadas por um staff.
     * 
     * @param staffPlayerId ID do staff que aplicou as punições
     * @param callback Callback com lista de punições aplicadas pelo staff
     */
    void getPunishmentsByStaffAsync(int staffPlayerId, Consumer<List<Punishment>> callback);

    /**
     * Atualiza uma punição existente.
     * 
     * @param punishment Punição com dados atualizados
     * @param callback Callback com resultado da operação (true = sucesso, false = falha)
     */
    void updatePunishmentAsync(Punishment punishment, Consumer<Boolean> callback);

    /**
     * Busca uma punição por ID.
     * 
     * @param punishmentId ID da punição
     * @param callback Callback com a punição encontrada (null se não existir)
     */
    void getPunishmentByIdAsync(int punishmentId, Consumer<Punishment> callback);
}
