package br.com.primeleague.api.enums;

/**
 * Enum que define o tipo de reversão de uma punição.
 * 
 * @author Prime League Development Team
 * @version 1.0
 */
public enum ReversalType {
    /**
     * A punição foi revertida como um ato de clemência (perdão).
     * As sanções do clã PERMANECEM.
     */
    PARDON,

    /**
     * A punição foi considerada um erro administrativo ou injusta.
     * As sanções do clã DEVEM SER REVERTIDAS.
     */
    CORRECTION;
}
