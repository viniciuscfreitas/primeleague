package br.com.primeleague.core.services;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.api.dao.EssentialsDAO;
import br.com.primeleague.api.dao.WarpDAO;
import br.com.primeleague.api.dao.KitDAO;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Registry central para gerenciar instâncias de DAOs.
 * Permite injeção de dependência entre módulos e Core.
 * 
 * Este registry segue o padrão arquitetural modular onde:
 * - Interfaces DAO ficam na API
 * - Implementações ficam nos módulos específicos
 * - Core gerencia o registry para injeção de dependência
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class DAOServiceRegistry {
    
    private final Logger logger;
    private final Map<Class<?>, Object> daoInstances;
    
    /**
     * Construtor do registry.
     * 
     * @param logger Logger para debug e informações
     */
    public DAOServiceRegistry(Logger logger) {
        this.logger = logger;
        this.daoInstances = new HashMap<>();
    }
    
    /**
     * Registra uma instância de DAO no registry.
     * 
     * @param daoInterface Classe da interface do DAO
     * @param daoInstance Instância da implementação
     * @param <T> Tipo do DAO
     */
    public <T> void registerDAO(Class<T> daoInterface, T daoInstance) {
        if (daoInstance == null) {
            logger.warning("⚠️ Tentativa de registrar DAO nulo para interface: " + daoInterface.getSimpleName());
            return;
        }
        
        daoInstances.put(daoInterface, daoInstance);
        logger.info("✅ DAO registrado: " + daoInterface.getSimpleName() + " -> " + daoInstance.getClass().getSimpleName());
    }
    
    /**
     * Obtém uma instância de DAO do registry.
     * 
     * @param daoInterface Classe da interface do DAO
     * @param <T> Tipo do DAO
     * @return Instância do DAO ou null se não encontrado
     */
    @SuppressWarnings("unchecked")
    public <T> T getDAO(Class<T> daoInterface) {
        T daoInstance = (T) daoInstances.get(daoInterface);
        
        if (daoInstance == null) {
            logger.warning("⚠️ DAO não encontrado no registry: " + daoInterface.getSimpleName());
            logger.warning("⚠️ DAOs registrados: " + daoInstances.keySet());
        }
        
        return daoInstance;
    }
    
    /**
     * Verifica se um DAO está registrado.
     * 
     * @param daoInterface Classe da interface do DAO
     * @return true se registrado, false caso contrário
     */
    public boolean isDAORegistered(Class<?> daoInterface) {
        return daoInstances.containsKey(daoInterface);
    }
    
    /**
     * Remove um DAO do registry.
     * 
     * @param daoInterface Classe da interface do DAO
     * @param <T> Tipo do DAO
     * @return Instância removida ou null se não encontrado
     */
    @SuppressWarnings("unchecked")
    public <T> T unregisterDAO(Class<T> daoInterface) {
        T removed = (T) daoInstances.remove(daoInterface);
        
        if (removed != null) {
            logger.info("🗑️ DAO removido do registry: " + daoInterface.getSimpleName());
        } else {
            logger.warning("⚠️ Tentativa de remover DAO não registrado: " + daoInterface.getSimpleName());
        }
        
        return removed;
    }
    
    /**
     * Limpa todos os DAOs registrados.
     */
    public void clearAll() {
        int count = daoInstances.size();
        daoInstances.clear();
        logger.info("🧹 Registry limpo: " + count + " DAOs removidos");
    }
    
    /**
     * Obtém estatísticas do registry.
     * 
     * @return String com informações sobre DAOs registrados
     */
    public String getRegistryStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("📊 DAOServiceRegistry Stats:\n");
        stats.append("   Total de DAOs registrados: ").append(daoInstances.size()).append("\n");
        
        for (Map.Entry<Class<?>, Object> entry : daoInstances.entrySet()) {
            stats.append("   - ").append(entry.getKey().getSimpleName())
                 .append(" -> ").append(entry.getValue().getClass().getSimpleName()).append("\n");
        }
        
        return stats.toString();
    }
    
    // Métodos de conveniência para DAOs específicos
    
    /**
     * Obtém o ClanDAO registrado.
     * 
     * @return Instância do ClanDAO ou null se não registrado
     */
    public ClanDAO getClanDAO() {
        return getDAO(ClanDAO.class);
    }
    
    /**
     * Obtém o EssentialsDAO registrado.
     * 
     * @return Instância do EssentialsDAO ou null se não registrado
     */
    public EssentialsDAO getEssentialsDAO() {
        return getDAO(EssentialsDAO.class);
    }
    
    /**
     * Obtém o WarpDAO registrado.
     * 
     * @return Instância do WarpDAO ou null se não registrado
     */
    public WarpDAO getWarpDAO() {
        return getDAO(WarpDAO.class);
    }
    
    /**
     * Obtém o KitDAO registrado.
     * 
     * @return Instância do KitDAO ou null se não registrado
     */
    public KitDAO getKitDAO() {
        return getDAO(KitDAO.class);
    }
}
