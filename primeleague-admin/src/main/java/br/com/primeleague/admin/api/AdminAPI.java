package br.com.primeleague.admin.api;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Punishment;

import java.util.UUID;

/**
 * API pública do módulo administrativo.
 * Fornece métodos para outros módulos integrarem com o sistema de punições.
 */
public class AdminAPI {
    
    private static AdminManager adminManager;
    private static boolean initialized = false;
    
    /**
     * Inicializa a API administrativa.
     */
    public static void initialize(AdminManager manager) {
        adminManager = manager;
        initialized = true;
    }
    
    /**
     * Desabilita a API administrativa.
     */
    public static void shutdown() {
        adminManager = null;
        initialized = false;
    }
    
    /**
     * Verifica se a API está disponível.
     */
    public static boolean isAvailable() {
        return initialized && adminManager != null;
    }
    
    /**
     * Verifica se um jogador tem uma punição ativa de um tipo específico.
     */
    public static boolean hasActivePunishment(UUID playerUuid, Punishment.Type type) {
        if (!isAvailable()) {
            return false;
        }
        
        Punishment punishment = adminManager.getActivePunishment(playerUuid, type);
        return punishment != null && punishment.isCurrentlyActive();
    }
    
    /**
     * Obtém a punição ativa de um jogador de um tipo específico.
     */
    public static Punishment getActivePunishment(UUID playerUuid, Punishment.Type type) {
        if (!isAvailable()) {
            return null;
        }
        
        return adminManager.getActivePunishment(playerUuid, type);
    }
    
    /**
     * Verifica se um jogador está banido.
     */
    public static boolean isBanned(UUID playerUuid) {
        return hasActivePunishment(playerUuid, Punishment.Type.BAN);
    }
    
    /**
     * Verifica se um jogador está silenciado.
     */
    public static boolean isMuted(UUID playerUuid) {
        return hasActivePunishment(playerUuid, Punishment.Type.MUTE);
    }
    
    /**
     * Aplica uma punição a um jogador.
     */
    public static boolean applyPunishment(Punishment punishment) {
        if (!isAvailable()) {
            return false;
        }
        
        return adminManager.applyPunishment(punishment);
    }
    
    /**
     * Aplica perdão a uma punição ativa.
     */
    public static boolean pardonPunishment(UUID targetUuid, Punishment.Type type, UUID pardonerUuid, String pardonReason) {
        if (!isAvailable()) {
            return false;
        }
        
        return adminManager.pardonPunishment(targetUuid, type, pardonerUuid, pardonReason);
    }
    
    /**
     * Verifica se um jogador está em modo vanish.
     */
    public static boolean isVanished(UUID playerUuid) {
        if (!isAvailable()) {
            return false;
        }
        
        return adminManager.isVanished(playerUuid);
    }
    
    /**
     * Obtém o gerenciador administrativo (para uso interno).
     */
    public static AdminManager getAdminManager() {
        return adminManager;
    }
}
