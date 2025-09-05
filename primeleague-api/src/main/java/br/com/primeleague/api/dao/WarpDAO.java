package br.com.primeleague.api.dao;

import br.com.primeleague.api.models.Warp;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface para operações de banco de dados relacionadas aos warps.
 * Define operações assíncronas para persistência de warps públicos.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public interface WarpDAO {
    
    /**
     * Cria um novo warp de forma assíncrona.
     * 
     * @param warp Warp a ser criado
     * @param callback Callback executado quando a operação for concluída
     */
    void createWarpAsync(Warp warp, Consumer<Boolean> callback);
    
    /**
     * Obtém um warp pelo nome de forma assíncrona.
     * 
     * @param warpName Nome do warp
     * @param callback Callback executado quando a busca for concluída
     */
    void getWarpAsync(String warpName, Consumer<Warp> callback);
    
    /**
     * Obtém todos os warps de forma assíncrona.
     * 
     * @param callback Callback executado quando a busca for concluída
     */
    void getAllWarpsAsync(Consumer<List<Warp>> callback);
    
    /**
     * Obtém warps que o jogador pode usar (baseado em permissões) de forma assíncrona.
     * 
     * @param playerName Nome do jogador
     * @param callback Callback executado quando a busca for concluída
     */
    void getAvailableWarpsAsync(String playerName, Consumer<List<Warp>> callback);
    
    /**
     * Atualiza um warp existente de forma assíncrona.
     * 
     * @param warp Warp a ser atualizado
     * @param callback Callback executado quando a operação for concluída
     */
    void updateWarpAsync(Warp warp, Consumer<Boolean> callback);
    
    /**
     * Remove um warp de forma assíncrona.
     * 
     * @param warpName Nome do warp a ser removido
     * @param callback Callback executado quando a operação for concluída
     */
    void deleteWarpAsync(String warpName, Consumer<Boolean> callback);
    
    /**
     * Verifica se um warp existe de forma assíncrona.
     * 
     * @param warpName Nome do warp
     * @param callback Callback executado quando a verificação for concluída
     */
    void warpExistsAsync(String warpName, Consumer<Boolean> callback);
    
    /**
     * Atualiza as estatísticas de uso de um warp de forma assíncrona.
     * 
     * @param warpId ID do warp
     * @param callback Callback executado quando a operação for concluída
     */
    void updateWarpUsageAsync(int warpId, Consumer<Boolean> callback);
    
    /**
     * Obtém estatísticas de uso de um warp de forma assíncrona.
     * 
     * @param warpName Nome do warp
     * @param callback Callback executado quando a busca for concluída
     */
    void getWarpStatsAsync(String warpName, Consumer<Warp> callback);
}
