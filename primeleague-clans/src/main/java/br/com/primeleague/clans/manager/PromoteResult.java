package br.com.primeleague.clans.manager;

/**
 * Enum para resultados de operações de promoção de membros.
 */
public enum PromoteResult {
    SUCCESS,
    PLAYER_NOT_FOUND,
    ALREADY_OFFICER,
    ALREADY_LEADER,
    NOT_IN_SAME_CLAN
}
