package br.com.primeleague.clans.manager;

/**
 * Enum para resultados de operações de expulsão de membros.
 */
public enum KickResult {
    SUCCESS,
    PLAYER_NOT_FOUND,
    NOT_IN_SAME_CLAN,
    CANNOT_KICK_SELF,
    CANNOT_KICK_LEADER
}
