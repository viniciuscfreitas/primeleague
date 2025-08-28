package br.com.primeleague.p2p.managers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache em tempo real para IPs autorizados
 * Resolve race condition entre DataManager e AuthenticationListener
 */
public class IpAuthCache {
    
    private final ConcurrentMap<String, Boolean> authorizedIps = new ConcurrentHashMap<>();
    
    /**
     * Adiciona um IP autorizado ao cache
     */
    public void addAuthorizedIp(String playerName, String ipAddress) {
        String key = generateKey(playerName, ipAddress);
        authorizedIps.put(key, true);
    }
    
    /**
     * Remove um IP do cache
     */
    public void removeAuthorizedIp(String playerName, String ipAddress) {
        String key = generateKey(playerName, ipAddress);
        authorizedIps.remove(key);
    }
    
    /**
     * Verifica se um IP está autorizado no cache
     */
    public boolean isIpAuthorized(String playerName, String ipAddress) {
        String key = generateKey(playerName, ipAddress);
        return authorizedIps.containsKey(key);
    }
    
    /**
     * Limpa o cache (útil para reinicialização)
     */
    public void clear() {
        authorizedIps.clear();
    }
    
    /**
     * Gera chave única para player + IP
     */
    private String generateKey(String playerName, String ipAddress) {
        return playerName.toLowerCase() + ":" + ipAddress;
    }
}
