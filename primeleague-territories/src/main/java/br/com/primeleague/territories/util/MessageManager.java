package br.com.primeleague.territories.util;

import br.com.primeleague.territories.PrimeLeagueTerritories;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gerenciador de mensagens centralizado para o módulo de Territórios.
 * Permite customização e tradução de mensagens através de arquivo YAML.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class MessageManager {
    
    private final PrimeLeagueTerritories plugin;
    private final Map<String, String> messageCache = new HashMap<>();
    private FileConfiguration messagesConfig;
    private File messagesFile;
    
    public MessageManager(PrimeLeagueTerritories plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    /**
     * Carrega as mensagens do arquivo messages.yml.
     */
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        // Criar arquivo se não existir
        if (!messagesFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                } else {
                    // Criar arquivo padrão se não existir no resources
                    createDefaultMessagesFile();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Erro ao criar arquivo messages.yml", e);
                return;
            }
        }
        
        // Carregar configuração
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Carregar todas as mensagens no cache
        loadAllMessages();
        
        plugin.getLogger().info("MessageManager carregado com " + messageCache.size() + " mensagens.");
    }
    
    /**
     * Cria arquivo de mensagens padrão.
     */
    private void createDefaultMessagesFile() {
        try {
            messagesFile.createNewFile();
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            
            // Adicionar mensagens padrão
            messagesConfig.set("territory.claim.success", "&aTerritório reivindicado com sucesso!");
            messagesConfig.set("territory.claim.already_claimed", "&cEste território já foi reivindicado!");
            messagesConfig.set("territory.claim.no_permission", "&cVocê não tem permissão para reivindicar territórios!");
            messagesConfig.set("territory.claim.insufficient_moral", "&cMoral insuficiente! Necessário: {required}, Atual: {current}");
            messagesConfig.set("territory.claim.no_clan", "&cVocê precisa estar em um clã para reivindicar territórios!");
            
            messagesConfig.set("territory.unclaim.success", "&aTerritório removido com sucesso!");
            messagesConfig.set("territory.unclaim.not_claimed", "&cEste território não foi reivindicado!");
            messagesConfig.set("territory.unclaim.no_permission", "&cVocê não tem permissão para remover territórios!");
            
            messagesConfig.set("territory.bank.deposit.success", "&aDepositado ${amount} no banco do clã!");
            messagesConfig.set("territory.bank.withdraw.success", "&aSacado ${amount} do banco do clã!");
            messagesConfig.set("territory.bank.insufficient_balance", "&cSaldo insuficiente no banco do clã!");
            messagesConfig.set("territory.bank.balance", "&eSaldo do banco do clã: ${amount}");
            
            messagesConfig.set("war.declare.success", "&aGuerra declarada contra {clan}!");
            messagesConfig.set("war.declare.target_not_vulnerable", "&cO clã {clan} não está vulnerável!");
            messagesConfig.set("war.declare.insufficient_balance", "&cSaldo insuficiente para declarar guerra!");
            messagesConfig.set("war.declare.active_truce", "&cExiste uma trégua ativa com o clã {clan}!");
            messagesConfig.set("war.declare.no_permission", "&cVocê não tem permissão para declarar guerra!");
            
            messagesConfig.set("war.siege.start", "&cCerco iniciado em {location}!");
            messagesConfig.set("war.siege.end", "&aCerco finalizado em {location}!");
            messagesConfig.set("war.siege.victory", "&aVitória! Território conquistado!");
            messagesConfig.set("war.siege.defeat", "&cDerrota! Território defendido!");
            
            messagesConfig.set("error.database", "&cErro de banco de dados. Tente novamente.");
            messagesConfig.set("error.generic", "&cOcorreu um erro. Tente novamente.");
            
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao criar arquivo de mensagens padrão", e);
        }
    }
    
    /**
     * Carrega todas as mensagens no cache.
     */
    private void loadAllMessages() {
        messageCache.clear();
        
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messageCache.put(key, messagesConfig.getString(key));
            }
        }
    }
    
    /**
     * Obtém uma mensagem formatada.
     * 
     * @param key Chave da mensagem
     * @param placeholders Placeholders para substituição
     * @return Mensagem formatada
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = messageCache.getOrDefault(key, "&cMensagem não encontrada: " + key);
        
        // Aplicar placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        // Traduzir cores
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Obtém uma mensagem simples.
     * 
     * @param key Chave da mensagem
     * @return Mensagem formatada
     */
    public String getMessage(String key) {
        return getMessage(key, null);
    }
    
    /**
     * Recarrega as mensagens do arquivo.
     */
    public void reload() {
        loadMessages();
    }
    
    /**
     * Adiciona uma nova mensagem ao cache.
     * 
     * @param key Chave da mensagem
     * @param message Conteúdo da mensagem
     */
    public void addMessage(String key, String message) {
        messageCache.put(key, message);
    }
    
    /**
     * Remove uma mensagem do cache.
     * 
     * @param key Chave da mensagem
     */
    public void removeMessage(String key) {
        messageCache.remove(key);
    }
    
    /**
     * Verifica se uma mensagem existe.
     * 
     * @param key Chave da mensagem
     * @return true se a mensagem existe
     */
    public boolean hasMessage(String key) {
        return messageCache.containsKey(key);
    }
    
    /**
     * Obtém o número de mensagens carregadas.
     * 
     * @return Número de mensagens
     */
    public int getMessageCount() {
        return messageCache.size();
    }
}