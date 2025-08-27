package br.com.primeleague.api.enums;

/**
 * Enum que define os níveis de severidade das punições.
 * Este enum é usado para padronizar a comunicação entre módulos
 * e garantir consistência na aplicação de sanções.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public enum PunishmentSeverity {
    
    /**
     * Punições leves - advertências, avisos
     */
    LEVE("Leve"),
    
    /**
     * Punições médias - kicks, mutes temporários
     */
    MEDIA("Média"),
    
    /**
     * Punições sérias - bans temporários, mutes permanentes
     */
    SERIA("Séria"),
    
    /**
     * Punições gravíssimas - bans permanentes
     */
    GRAVISSIMA("Gravissima");
    
    private final String displayName;
    
    PunishmentSeverity(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * Obtém o nome de exibição da severidade.
     * 
     * @return Nome de exibição
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Obtém a severidade a partir de uma string.
     * 
     * @param name Nome da severidade
     * @return Severidade correspondente ou null se não encontrada
     */
    public static PunishmentSeverity fromString(String name) {
        if (name == null) return null;
        
        for (PunishmentSeverity severity : values()) {
            if (severity.name().equalsIgnoreCase(name) || 
                severity.displayName.equalsIgnoreCase(name)) {
                return severity;
            }
        }
        return null;
    }
}
