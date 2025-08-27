package br.com.primeleague.api.enums;

public enum LogActionType {
    // Ações de Clã
    CLAN_CREATE(1),
    CLAN_DISBAND(2),
    
    // Ações de Jogador
    PLAYER_INVITE(10),
    PLAYER_JOIN(11),
    PLAYER_LEAVE(12),
    PLAYER_KICK(13),
    PLAYER_PROMOTE(14),
    PLAYER_DEMOTE(15),
    
    // Ações de Fundador
    FOUNDER_CHANGE(20),
    
    // Ações de Aliança
    ALLY_INVITE(30),
    ALLY_ACCEPT(31),
    ALLY_REMOVE(32),
    
    // Ações de Sanção
    SANCTION_ADD(40),
    SANCTION_SET(41),
    SANCTION_PARDON(42),
    SANCTION_REMOVE(43),
    SANCTION_REVERSED(44),
    PENALTY_POINTS_ADDED(45),
    
    // Ações de Estatísticas
    KDR_UPDATE(50),
    
    // Ações de Ranking
    RANKING_POINTS_ADDED(51),
    RANKING_POINTS_REMOVED(52),
    EVENT_WIN_REGISTERED(53);
    
    private final int id;
    
    LogActionType(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }
    
    public static LogActionType fromId(int id) {
        for (LogActionType action : values()) {
            if (action.id == id) {
                return action;
            }
        }
        throw new IllegalArgumentException("LogActionType não encontrado para ID: " + id);
    }
}
