package br.com.primeleague.adminshop;

import br.com.primeleague.adminshop.managers.ShopConfigManager;
import br.com.primeleague.adminshop.managers.ShopManager;
import br.com.primeleague.adminshop.commands.AdminShopCommand;
import br.com.primeleague.adminshop.commands.ShopCommand;
import br.com.primeleague.adminshop.listeners.ShopListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * Plugin principal do módulo Admin Shop do Prime League.
 * 
 * Este módulo implementa uma loja administrativa completa com:
 * - Categorias modulares de itens
 * - Sistema de preços configurável
 * - Descontos por tier de doador
 * - Comandos VIP
 * - Kits especiais
 * - Logs detalhados
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class AdminShopPlugin extends JavaPlugin {

    private static AdminShopPlugin instance;
    private ShopConfigManager configManager;
    private ShopManager shopManager;
    private ShopListener shopListener;
    private Logger logger;

    @Override
    public void onEnable() {
        instance = this;
        logger = getLogger();
        

        
        try {
            // Verificar dependência do Core
            if (getServer().getPluginManager().getPlugin("PrimeLeague-Core") == null) {
                logger.severe("❌ PrimeLeague-Core não encontrado! Este módulo é obrigatório.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Inicializar gerenciadores
            initializeManagers();
            
            // Registrar comandos
            registerCommands();
            
            // Registrar listeners
            registerListeners();
            
            logger.info("[Shop] PrimeLeague AdminShop habilitado");
            
        } catch (Exception e) {
            logger.severe("❌ Erro fatal ao inicializar Admin Shop: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (shopManager != null) {
            shopManager.clearCache();
        }
        
        logger.info("[Shop] PrimeLeague AdminShop desabilitado");
    }

    /**
     * Inicializa os gerenciadores principais do plugin.
     */
    private void initializeManagers() {
        // Configuração
        configManager = new ShopConfigManager(this);
        if (!configManager.loadConfiguration()) {
            throw new RuntimeException("Falha ao carregar configuração da loja");
        }
        
        // Gerenciador da loja
        shopManager = new ShopManager(this, configManager);
        shopListener = new ShopListener(this, shopManager);
    }

    /**
     * Registra os comandos do plugin.
     */
    private void registerCommands() {
        getCommand("shop").setExecutor(new ShopCommand(this));
        getCommand("adminshop").setExecutor(new AdminShopCommand(this));
    }

    /**
     * Registra os listeners do plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(shopListener, this);
    }



    /**
     * Recarrega a configuração da loja.
     * 
     * @return true se o reload foi bem-sucedido
     */
    public boolean reloadConfiguration() {
        try {
            if (configManager.reloadConfiguration()) {
                // shopManager.updateConfiguration(configManager);
                return true;
            } else {
                logger.severe("❌ Falha ao recarregar configuração!");
                return false;
            }
        } catch (Exception e) {
            logger.severe("❌ Erro ao recarregar configuração: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==================== GETTERS ====================

    /**
     * Retorna a instância única do plugin.
     * 
     * @return instância do AdminShopPlugin
     */
    public static AdminShopPlugin getInstance() {
        return instance;
    }

    /**
     * Retorna o gerenciador de configuração.
     * 
     * @return ShopConfigManager
     */
    public ShopConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Retorna o gerenciador da loja.
     * 
     * @return ShopManager
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Retorna o logger do plugin.
     * 
     * @return Logger
     */
    public Logger getPluginLogger() {
        return logger;
    }
}
