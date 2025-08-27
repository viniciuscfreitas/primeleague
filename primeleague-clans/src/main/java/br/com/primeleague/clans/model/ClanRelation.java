package br.com.primeleague.clans.model;

/**
 * Representa uma relação entre dois clãs.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanRelation {

    private final int clanId1;
    private final int clanId2;
    private final RelationType type;
    private final long creationDate;

    /**
     * Enum que define os tipos de relação entre clãs.
     */
    public enum RelationType {
        ALLY(1, "Aliado"),
        RIVAL(2, "Rival");

        private final int id;
        private final String displayName;

        RelationType(int id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public int getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static RelationType fromId(int id) {
            for (RelationType type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Construtor para criar uma relação entre clãs.
     *
     * @param clanId1 ID do primeiro clã
     * @param clanId2 ID do segundo clã
     * @param type Tipo da relação
     */
    public ClanRelation(int clanId1, int clanId2, RelationType type) {
        this.clanId1 = Math.min(clanId1, clanId2); // Sempre armazenar o menor ID primeiro
        this.clanId2 = Math.max(clanId1, clanId2); // Sempre armazenar o maior ID segundo
        this.type = type;
        this.creationDate = System.currentTimeMillis();
    }

    /**
     * Construtor para criar uma relação entre clãs com data específica.
     *
     * @param clanId1 ID do primeiro clã
     * @param clanId2 ID do segundo clã
     * @param type Tipo da relação
     * @param creationDate Data de criação
     */
    public ClanRelation(int clanId1, int clanId2, RelationType type, long creationDate) {
        this.clanId1 = Math.min(clanId1, clanId2);
        this.clanId2 = Math.max(clanId1, clanId2);
        this.type = type;
        this.creationDate = creationDate;
    }

    // --- Getters ---

    public int getClanId1() {
        return clanId1;
    }

    public int getClanId2() {
        return clanId2;
    }

    public RelationType getType() {
        return type;
    }

    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Verifica se um clã está envolvido nesta relação.
     *
     * @param clanId ID do clã a ser verificado
     * @return true se o clã está envolvido na relação
     */
    public boolean involvesClan(int clanId) {
        return clanId == clanId1 || clanId == clanId2;
    }

    /**
     * Obtém o ID do outro clã na relação.
     *
     * @param clanId ID do clã atual
     * @return ID do outro clã, ou -1 se o clã não está na relação
     */
    public int getOtherClanId(int clanId) {
        if (clanId == clanId1) {
            return clanId2;
        } else if (clanId == clanId2) {
            return clanId1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ClanRelation that = (ClanRelation) obj;
        return clanId1 == that.clanId1 && clanId2 == that.clanId2 && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = clanId1;
        result = 31 * result + clanId2;
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ClanRelation{" +
                "clanId1=" + clanId1 +
                ", clanId2=" + clanId2 +
                ", type=" + type.getDisplayName() +
                '}';
    }
}
