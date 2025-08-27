package br.com.primeleague.api;

import br.com.primeleague.api.dto.LogEntryDTO;

/**
 * Registry para o LoggingService.
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
public class LoggingServiceRegistry {
    
    private static LoggingService instance;
    
    /**
     * Registra uma implementação do LoggingService.
     * 
     * @param service Implementação do LoggingService
     */
    public static void register(LoggingService service) {
        instance = service;
    }
    
    /**
     * Obtém a instância registrada do LoggingService.
     * 
     * @return Instância do LoggingService ou null se não registrada
     */
    public static LoggingService getInstance() {
        return instance;
    }
    
    /**
     * Registra uma mensagem de chat usando o serviço registrado.
     * 
     * @param entry DTO contendo os dados da mensagem para logging
     */
    public static void logChatMessage(LogEntryDTO entry) {
        if (instance != null) {
            instance.logChatMessage(entry);
        }
    }
}
