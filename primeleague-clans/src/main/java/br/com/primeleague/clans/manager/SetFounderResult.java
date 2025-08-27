package br.com.primeleague.clans.manager;

/**
 * Enum para resultados de operações de transferência de fundador.
 * 
 * @author PrimeLeague Team
 */
public enum SetFounderResult {
    SUCCESS,
    PLAYER_NOT_FOUND,
    NOT_IN_SAME_CLAN,
    NOT_LEADER,
    ALREADY_FOUNDER
}
