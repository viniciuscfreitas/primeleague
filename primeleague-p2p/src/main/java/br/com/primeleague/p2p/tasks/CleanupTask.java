package br.com.primeleague.p2p.tasks;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.p2p.PrimeLeagueP2P;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Tarefa agendada para limpar códigos de verificação expirados.
 * 
 * Executa diariamente para manter o banco de dados limpo
 * e remover tentativas de verificação abandonadas.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
public class CleanupTask extends BukkitRunnable {

    private final PrimeLeagueP2P plugin;

    public CleanupTask(PrimeLeagueP2P plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Executar limpeza de forma assíncrona
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    int deletedCount = cleanupExpiredCodes();
                    
                    // Log do resultado
                    plugin.getLogger().info("Limpeza automática: " + deletedCount + " códigos expirados removidos.");
                    
                } catch (Exception e) {
                    plugin.getLogger().severe("Erro na limpeza automática: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Remove códigos de verificação expirados do banco de dados.
     * 
     * @return Número de registros removidos
     */
    private int cleanupExpiredCodes() {
        Connection connection = null;
        PreparedStatement stmt = null;

        try {
            connection = PrimeLeagueAPI.getDataManager().getConnection();
            
            // Usar a procedure do banco de dados
            stmt = connection.prepareStatement("CALL CleanupExpiredCodes()");
            
            boolean hasResults = stmt.execute();
            
            if (hasResults) {
                java.sql.ResultSet rs = stmt.getResultSet();
                if (rs.next()) {
                    return rs.getInt("deleted_count");
                }
            }
            
            return 0;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL na limpeza: " + e.getMessage());
            return 0;
        } finally {
            // Fechar recursos
            try {
                if (stmt != null) stmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Erro ao fechar conexão: " + e.getMessage());
            }
        }
    }

    /**
     * Inicia a tarefa de limpeza agendada.
     * 
     * @param plugin Instância do plugin
     */
    public static void startCleanupTask(PrimeLeagueP2P plugin) {
        // Executar limpeza a cada 24 horas (1728000 ticks)
        CleanupTask task = new CleanupTask(plugin);
        task.runTaskTimer(plugin, 1728000L, 1728000L);
        
        plugin.getLogger().info("Tarefa de limpeza automática iniciada (24h).");
    }
}
