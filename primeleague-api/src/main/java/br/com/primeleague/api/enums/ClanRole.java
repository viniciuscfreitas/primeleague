package br.com.primeleague.api.enums;

/**
 * Enum que define os cargos possíveis dentro de um clã.
 * Usado para transferência de dados entre módulos.
 */
public enum ClanRole {
    MEMBRO(1, "Membro"),
    LIDER(2, "Líder"),
    FUNDADOR(3, "Fundador");

    private final int id;
    private final String displayName;

    ClanRole(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte um ID para o enum correspondente.
     * @param id O ID do cargo
     * @return O enum correspondente, ou MEMBRO como fallback
     */
    public static ClanRole fromId(int id) {
        for (ClanRole role : values()) {
            if (role.id == id) {
                return role;
            }
        }
        return MEMBRO; // Default fallback
    }
}
