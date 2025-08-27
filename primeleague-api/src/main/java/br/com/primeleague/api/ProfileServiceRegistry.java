package br.com.primeleague.api;

/**
 * Registry para o ProfileService.
 * Permite que o Core registre sua implementação do ProfileService.
 */
public class ProfileServiceRegistry {
    
    private static ProfileService instance;
    
    /**
     * Registra uma implementação do ProfileService.
     * 
     * @param service Implementação do ProfileService
     */
    public static void register(ProfileService service) {
        instance = service;
    }
    
    /**
     * Obtém a implementação registrada do ProfileService.
     * 
     * @return Implementação do ProfileService ou null se não registrada
     */
    public static ProfileService getInstance() {
        return instance;
    }
    
    /**
     * Obtém o nome de um jogador pelo UUID usando o serviço registrado.
     * 
     * @param uuid UUID do jogador
     * @return Nome do jogador ou null se não encontrado ou serviço não registrado
     */
    public static String getPlayerName(java.util.UUID uuid) {
        if (instance == null) return null;
        return instance.getPlayerName(uuid);
    }
    
    /**
     * Obtém o nome de um jogador pelo nome usando o serviço registrado.
     * 
     * @param name Nome do jogador
     * @return Nome do jogador ou null se não encontrado ou serviço não registrado
     */
    public static String getPlayerName(String name) {
        if (instance == null) return null;
        return instance.getPlayerName(name);
    }
}
