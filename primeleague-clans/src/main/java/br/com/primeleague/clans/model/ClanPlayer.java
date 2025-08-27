package br.com.primeleague.clans.model;

import org.bukkit.entity.Player;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import java.util.UUID;

/**
 * Representa um jogador no contexto do sistema de clãs.
 * Encapsula um Player do Bukkit adicionando metadados específicos do clã.
 *
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanPlayer {

    private final String playerName;
    private final int playerId; // REFATORADO: Usar player_id em vez de UUID
    private Clan clan;
    private ClanRole role;
    private long joinDate;
    private int kills;
    private int deaths;

    /**
     * Enum que define os cargos possíveis dentro de um clã.
     * Nova hierarquia baseada em níveis de poder para máxima flexibilidade.
     */
    public enum ClanRole {
        MEMBRO(1, 10, "Membro"),
        LIDER(2, 50, "Líder"),
        FUNDADOR(3, 99, "Fundador");

        private final int id;
        private final int powerLevel;
        private final String displayName;

        ClanRole(int id, int powerLevel, String displayName) {
            this.id = id;
            this.powerLevel = powerLevel;
            this.displayName = displayName;
        }

        public int getId() {
            return id;
        }

        public int getPowerLevel() {
            return powerLevel;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Verifica se este cargo tem pelo menos o poder do cargo especificado.
         *
         * @param role O cargo mínimo necessário
         * @return true se este cargo tem pelo menos o poder especificado
         */
        public boolean hasPowerOf(ClanRole role) {
            return this.powerLevel >= role.powerLevel;
        }

        /**
         * Verifica se este cargo tem pelo menos o nível especificado (método legado).
         *
         * @param role O cargo mínimo necessário
         * @return true se este cargo tem pelo menos o nível especificado
         */
        public boolean isAtLeast(ClanRole role) {
            return hasPowerOf(role);
        }

        public static ClanRole fromId(int id) {
            for (ClanRole role : values()) {
                if (role.id == id) {
                    return role;
                }
            }
            return MEMBRO; // Default fallback
        }
    }

    /**
     * Construtor para criar um ClanPlayer a partir de um Player do Bukkit.
     * REFATORADO: Usa player_id através do IdentityManager
     *
     * @param player O jogador do Bukkit
     */
    public ClanPlayer(Player player) {
        this.playerName = player.getName();
        this.playerId = PrimeLeagueAPI.getIdentityManager().getPlayerId(player);
        this.clan = null;
        this.role = ClanRole.MEMBRO;
        this.joinDate = System.currentTimeMillis();
    }

    /**
     * Construtor para criar um ClanPlayer com dados específicos.
     * REFATORADO: Usa player_id em vez de UUID
     *
     * @param playerName Nome do jogador
     * @param playerId ID do jogador
     * @param clan O clã ao qual pertence (pode ser null)
     * @param role O cargo no clã
     * @param joinDate Data de entrada no clã
     */
    public ClanPlayer(String playerName, int playerId, Clan clan, ClanRole role, long joinDate) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.clan = clan;
        this.role = role;
        this.joinDate = joinDate;
        this.kills = 0;
        this.deaths = 0;
    }

    // --- Getters ---

    public String getPlayerName() {
        return playerName;
    }

    public int getPlayerId() {
        return playerId;
    }

    /**
     * @deprecated Use getPlayerId() instead
     */
    @Deprecated
    public UUID getPlayerUUID() {
        // Retorna um UUID dummy - não é mais usado como identificador principal
        return java.util.UUID.randomUUID();
    }

    public Clan getClan() {
        return clan;
    }

    public ClanRole getRole() {
        return role;
    }

    public long getJoinDate() {
        return joinDate;
    }

    // --- Setters ---

    public void setClan(Clan clan) {
        this.clan = clan;
    }

    public void setRole(ClanRole role) {
        this.role = role;
    }

    public void setJoinDate(long joinDate) {
        this.joinDate = joinDate;
    }

    // --- Getters e Setters para KDR ---

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    /**
     * Calcula o KDR (Kill/Death Ratio) do jogador.
     *
     * @return O KDR calculado, ou 0.0 se não há mortes
     */
    public double getKDR() {
        if (deaths == 0) {
            return kills > 0 ? kills : 0.0;
        }
        return (double) kills / deaths;
    }

    /**
     * Incrementa o número de kills do jogador.
     * 
     * @param amount Quantidade a adicionar (padrão: 1)
     */
    public void addKill(int amount) {
        this.kills += amount;
    }
    
    /**
     * Incrementa o número de kills do jogador em 1.
     */
    public void addKill() {
        addKill(1);
    }

    /**
     * Incrementa o número de deaths do jogador.
     * 
     * @param amount Quantidade a adicionar (padrão: 1)
     */
    public void addDeath(int amount) {
        this.deaths += amount;
    }
    
    /**
     * Incrementa o número de deaths do jogador em 1.
     */
    public void addDeath() {
        addDeath(1);
    }

    // --- Métodos de Conveniência ---

    /**
     * Verifica se o jogador pertence a algum clã.
     *
     * @return true se o jogador pertence a um clã
     */
    public boolean hasClan() {
        return clan != null;
    }

    /**
     * Verifica se o jogador é fundador de um clã.
     *
     * @return true se o jogador é fundador
     */
    public boolean isFounder() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    /**
     * Verifica se o jogador é líder de um clã.
     *
     * @return true se o jogador é líder
     */
    public boolean isLeader() {
        return hasClan() && role.hasPowerOf(ClanRole.LIDER);
    }

    /**
     * Verifica se o jogador é membro de um clã.
     *
     * @return true se o jogador é membro
     */
    public boolean isMember() {
        return hasClan() && role == ClanRole.MEMBRO;
    }

    /**
     * Verifica se o jogador pode convidar outros jogadores.
     * Apenas Líderes e Fundadores podem convidar.
     *
     * @return true se o jogador pode convidar
     */
    public boolean canInvite() {
        return hasClan() && role.hasPowerOf(ClanRole.LIDER);
    }

    /**
     * Verifica se o jogador pode expulsar um membro específico.
     * Só pode expulsar quem tem cargo inferior.
     *
     * @param target O jogador alvo
     * @return true se o jogador pode expulsar o alvo
     */
    public boolean canKick(ClanPlayer target) {
        if (!hasClan() || !role.hasPowerOf(ClanRole.LIDER)) return false;
        return this.role.powerLevel > target.role.powerLevel;
    }

    /**
     * Verifica se o jogador pode gerenciar promoções e rebaixamentos.
     * Apenas o Fundador pode promover/rebaixar.
     *
     * @return true se o jogador pode gerenciar promoções
     */
    public boolean canManagePromotions() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    /**
     * Verifica se o jogador pode alterar configurações do clã (friendly fire, etc.).
     * Apenas o Fundador pode alterar configurações.
     *
     * @return true se o jogador pode alterar configurações
     */
    public boolean canManageSettings() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    /**
     * Verifica se o jogador pode gerenciar alianças e rivalidades.
     * Apenas o Fundador pode gerenciar relações.
     *
     * @return true se o jogador pode gerenciar relações
     */
    public boolean canManageRelations() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    /**
     * Verifica se o jogador pode passar o trono (set founder).
     * Apenas o Fundador pode transferir a posse do clã.
     *
     * @return true se o jogador pode transferir a posse
     */
    public boolean canTransferOwnership() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    /**
     * Verifica se o jogador pode dissolver o clã.
     * Apenas o Fundador pode dissolver o clã.
     *
     * @return true se o jogador pode dissolver
     */
    public boolean canDisband() {
        return hasClan() && role == ClanRole.FUNDADOR;
    }

    // --- Métodos de Conveniência (Legacy) ---

    /**
     * Verifica se o jogador pode gerenciar membros do clã (legacy).
     *
     * @return true se o jogador pode gerenciar membros
     */
    public boolean canManageMembers() {
        return canInvite();
    }

    /**
     * Verifica se o jogador pode promover membros (legacy).
     *
     * @return true se o jogador pode promover
     */
    public boolean canPromote() {
        return canManagePromotions();
    }

    /**
     * Verifica se o jogador pode rebaixar membros (legacy).
     *
     * @return true se o jogador pode rebaixar
     */
    public boolean canDemote() {
        return canManagePromotions();
    }

    /**
     * Verifica se o jogador pode expulsar membros (legacy).
     *
     * @return true se o jogador pode expulsar
     */
    public boolean canKick() {
        return hasClan() && role.hasPowerOf(ClanRole.LIDER);
    }

    @Override
    public String toString() {
        return "ClanPlayer{" +
                "playerName='" + playerName + '\'' +
                ", clan=" + (clan != null ? clan.getTag() : "Nenhum") +
                ", role=" + role.getDisplayName() +
                '}';
    }
}
