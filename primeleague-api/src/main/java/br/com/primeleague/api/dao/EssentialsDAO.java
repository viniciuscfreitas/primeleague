package br.com.primeleague.api.dao;

import br.com.primeleague.api.models.Home;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Interface para acesso assíncrono aos dados do sistema de Essentials.
 * Segue o padrão arquitetural estabelecido para operações não-bloqueantes.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public interface EssentialsDAO {
    
    /**
     * Carrega todas as homes de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    void loadPlayerHomesAsync(UUID playerUuid, Consumer<List<Home>> callback);
    
    /**
     * Cria uma nova home de forma assíncrona.
     * 
     * @param home Home a ser criada
     * @param callback Callback executado quando a operação for concluída
     */
    void createHomeAsync(Home home, Consumer<Boolean> callback);
    
    /**
     * Remove uma home de forma assíncrona.
     * 
     * @param homeId ID da home a ser removida
     * @param callback Callback executado quando a operação for concluída
     */
    void deleteHomeAsync(int homeId, Consumer<Boolean> callback);
    
    /**
     * Atualiza o timestamp de último uso de uma home de forma assíncrona.
     * 
     * @param homeId ID da home
     * @param callback Callback executado quando a operação for concluída
     */
    void updateHomeLastUsedAsync(int homeId, Consumer<Boolean> callback);
    
    /**
     * Verifica se uma home com o nome especificado já existe para o jogador.
     * 
     * @param playerUuid UUID do jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a verificação for concluída
     */
    void homeExistsAsync(UUID playerUuid, String homeName, Consumer<Boolean> callback);
    
    /**
     * Obtém o número de homes de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a contagem for concluída
     */
    void getPlayerHomeCountAsync(UUID playerUuid, Consumer<Integer> callback);
    
    /**
     * Obtém uma home específica de um jogador de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param homeName Nome da home
     * @param callback Callback executado quando a busca for concluída
     */
    void getHomeAsync(UUID playerUuid, String homeName, Consumer<Home> callback);
    
    /**
     * Obtém o ID do jogador no banco de dados de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a busca for concluída
     */
    void getPlayerIdAsync(UUID playerUuid, Consumer<Integer> callback);
}
