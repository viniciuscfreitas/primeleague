package br.com.primeleague.clans.manager;

/**
 * Enum para resultados de operações de rebaixamento de membros.
 */
public enum DemoteResult {
    SUCCESS,
    PLAYER_NOT_FOUND,
    NOT_AN_OFFICER,
    CANNOT_DEMOTE_LEADER,
    NOT_IN_SAME_CLAN
}
