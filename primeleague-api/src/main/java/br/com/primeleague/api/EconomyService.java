package br.com.primeleague.api;

import java.util.UUID;

/**
 * Interface para serviços de economia.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface EconomyService {
    
    /**
     * Obtém o saldo de um jogador via UUID.
     * 
     * @param playerUUID UUID do jogador
     * @return Saldo do jogador
     */
    double getPlayerBalance(UUID playerUUID);
    
    /**
     * Retira dinheiro da conta de um jogador.
     * 
     * @param playerUUID UUID do jogador
     * @param amount Quantia a ser retirada
     * @param reason Motivo da transação
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean withdrawPlayerMoney(UUID playerUUID, double amount, String reason);
    
    /**
     * Deposita dinheiro na conta de um jogador.
     * 
     * @param playerUUID UUID do jogador
     * @param amount Quantia a ser depositada
     * @param reason Motivo da transação
     * @return true se a operação foi bem-sucedida, false caso contrário
     */
    boolean depositPlayerMoney(UUID playerUUID, double amount, String reason);
}
