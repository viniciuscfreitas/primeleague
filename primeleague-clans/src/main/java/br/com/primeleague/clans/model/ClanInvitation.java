package br.com.primeleague.clans.model;

import java.util.UUID;

/**
 * Representa um convite para entrar em um clã.
 * Inclui informações sobre quem convidou, quem foi convidado e tempo de expiração.
 *
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanInvitation {

    private final String inviterName;
    private final int targetPlayerId; // REFATORADO: Usar player_id em vez de UUID
    private final Clan clan;
    private final long creationTimestamp;
    
    // Tempo de expiração em milissegundos (5 minutos)
    private static final long EXPIRATION_TIME = 5 * 60 * 1000;

    /**
     * Construtor para criar um novo convite.
     * REFATORADO: Usa player_id em vez de UUID
     *
     * @param inviterName Nome do jogador que convidou
     * @param targetPlayerId player_id do jogador convidado
     * @param clan O clã para o qual foi convidado
     */
    public ClanInvitation(String inviterName, int targetPlayerId, Clan clan) {
        this.inviterName = inviterName;
        this.targetPlayerId = targetPlayerId;
        this.clan = clan;
        this.creationTimestamp = System.currentTimeMillis();
    }

    /**
     * Verifica se o convite expirou.
     *
     * @return true se o convite expirou
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - creationTimestamp > EXPIRATION_TIME;
    }

    /**
     * Obtém o tempo restante em segundos.
     *
     * @return Tempo restante em segundos, ou 0 se expirou
     */
    public long getTimeRemainingSeconds() {
        long remaining = EXPIRATION_TIME - (System.currentTimeMillis() - creationTimestamp);
        return remaining > 0 ? remaining / 1000 : 0;
    }

    // --- Getters ---

    public String getInviterName() {
        return inviterName;
    }

    public int getTargetPlayerId() {
        return targetPlayerId;
    }

    /**
     * @deprecated Use getTargetPlayerId() instead
     */
    @Deprecated
    public UUID getTargetUUID() {
        // Retorna um UUID dummy - não é mais usado como identificador principal
        return java.util.UUID.randomUUID();
    }

    public Clan getClan() {
        return clan;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    @Override
    public String toString() {
        return "ClanInvitation{" +
                "inviterName='" + inviterName + '\'' +
                ", targetPlayerId=" + targetPlayerId +
                ", clan=" + (clan != null ? clan.getTag() : "null") +
                ", expired=" + isExpired() +
                '}';
    }
}
