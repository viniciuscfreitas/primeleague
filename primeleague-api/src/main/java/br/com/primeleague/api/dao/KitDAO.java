package br.com.primeleague.api.dao;

import br.com.primeleague.api.models.KitCooldown;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface para operações de banco de dados relacionadas a kits.
 * Define métodos assíncronos para persistência de cooldowns de kits.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public interface KitDAO {
    
    /**
     * Carrega todos os cooldowns de kits de um jogador de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    void loadPlayerKitCooldownsAsync(int playerId, Consumer<List<KitCooldown>> callback);
    
    /**
     * Salva ou atualiza um cooldown de kit de forma assíncrona.
     * 
     * @param cooldown Cooldown a ser salvo
     * @param callback Callback executado quando a operação for concluída
     */
    void saveKitCooldownAsync(KitCooldown cooldown, Consumer<Boolean> callback);
    
    /**
     * Obtém um cooldown específico de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    void getKitCooldownAsync(int playerId, String kitName, Consumer<KitCooldown> callback);
    
    /**
     * Remove um cooldown de kit de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    void removeKitCooldownAsync(int playerId, String kitName, Consumer<Boolean> callback);
    
    /**
     * Limpa todos os cooldowns expirados de forma assíncrona.
     * 
     * @param callback Callback executado quando a operação for concluída
     */
    void cleanExpiredCooldownsAsync(Consumer<Integer> callback);
    
    /**
     * Obtém o ID do jogador pelo UUID de forma assíncrona.
     * 
     * @param playerUuid UUID do jogador
     * @param callback Callback executado quando a operação for concluída
     */
    void getPlayerIdAsync(String playerUuid, Consumer<Integer> callback);
    
    /**
     * Verifica se um cooldown existe de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    void cooldownExistsAsync(int playerId, String kitName, Consumer<Boolean> callback);
    
    /**
     * Atualiza o timestamp de uso de um kit de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    void updateKitLastUsedAsync(int playerId, String kitName, Consumer<Boolean> callback);
    
    /**
     * Incrementa o contador de usos de um kit de forma assíncrona.
     * 
     * @param playerId ID do jogador
     * @param kitName Nome do kit
     * @param callback Callback executado quando a operação for concluída
     */
    void incrementKitUsesAsync(int playerId, String kitName, Consumer<Boolean> callback);
}
