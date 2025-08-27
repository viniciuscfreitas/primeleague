package br.com.primeleague.core.enums;

/**
 * Enum para categorizar todas as transações econômicas do Prime League.
 * Facilita a auditoria e análise de dados econômicos.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public enum TransactionReason {
    
    // ==================== ADMINISTRATIVO ====================
    CREDIT("Credit"),
    DEBIT("Debit"),
    ADMIN_GIVE("Admin Give"),
    ADMIN_TAKE("Admin Take"),
    ADMIN_SET("Admin Set"),
    
    // ==================== LOJAS ====================
    PLAYER_SHOP_SALE("Player Shop Sale"),
    PLAYER_SHOP_PURCHASE("Player Shop Purchase"),
    ADMIN_SHOP_PURCHASE("Admin Shop Purchase"),
    
    // ==================== TRANSFERÊNCIAS ====================
    PLAYER_TRANSFER("Player Transfer"),
    
    // ==================== CLÃS ====================
    CLAN_BANK_DEPOSIT("Clan Bank Deposit"),
    CLAN_BANK_WITHDRAW("Clan Bank Withdrawal"),
    CLAN_TAX_COLLECTION("Clan Tax Collection"),
    
    // ==================== SISTEMA ====================
    SYSTEM_REWARD("System Reward"),
    SYSTEM_PENALTY("System Penalty"),
    BOUNTY_REWARD("Bounty Reward"),
    BOUNTY_PAYMENT("Bounty Payment"),
    
    // ==================== EVENTOS ====================
    EVENT_REWARD("Event Reward"),
    TOURNAMENT_PRIZE("Tournament Prize"),
    
    // ==================== OUTROS ====================
    OTHER("Other");
    
    private final String displayName;
    
    TransactionReason(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
