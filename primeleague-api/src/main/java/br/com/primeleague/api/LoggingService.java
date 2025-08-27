package br.com.primeleague.api;

import br.com.primeleague.api.dto.LogEntryDTO;

/**
 * Interface para serviços de logging de chat.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public interface LoggingService {
    
    /**
     * Registra uma mensagem de chat no sistema de logging.
     * 
     * @param entry DTO contendo os dados da mensagem para logging
     */
    void logChatMessage(LogEntryDTO entry);
}
