package br.com.primeleague.chat.services;

import br.com.primeleague.chat.PrimeLeagueChat;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Serviço de rotação automática de logs de chat.
 * Responsável por limpar logs antigos e arquivar dados importantes.
 * 
 * @version 1.0
 * @author PrimeLeague Team
 */
public class LogRotationService {
    
    private final PrimeLeagueChat plugin;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public LogRotationService(PrimeLeagueChat plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("📊 Log Rotation Service inicializado");
    }
    
    /**
     * Inicia o sistema de rotação automática de logs.
     */
    public void startAutomaticRotation() {
        if (isRunning.get()) {
            plugin.getLogger().warning("Sistema de rotação já está em execução!");
            return;
        }
        
        // Verificar se o sistema está habilitado
        if (!plugin.getConfig().getBoolean("log_rotation.enabled", true)) {
            plugin.getLogger().info("Sistema de rotação de logs está desabilitado");
            return;
        }
        
        // Obter intervalo de execução (em horas)
        int intervalHours = plugin.getConfig().getInt("log_rotation.interval_hours", 24);
        long intervalTicks = intervalHours * 20 * 60 * 60; // Converter para ticks
        
        // Agendar execução automática
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isRunning.compareAndSet(false, true)) {
                    try {
                        plugin.getLogger().info("🔄 Iniciando rotação automática de logs...");
                        performLogRotation();
                        plugin.getLogger().info("✅ Rotação de logs concluída");
                    } catch (Exception e) {
                        plugin.getLogger().severe("❌ Erro durante rotação de logs: " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        isRunning.set(false);
                    }
                } else {
                    plugin.getLogger().warning("Rotação de logs já está em execução, pulando...");
                }
            }
        }.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
        
        plugin.getLogger().info("📅 Rotação automática agendada para executar a cada " + intervalHours + " horas");
    }
    
    /**
     * Executa a rotação de logs manualmente.
     */
    public void performManualRotation() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                plugin.getLogger().info("🔄 Iniciando rotação manual de logs...");
                performLogRotation();
                plugin.getLogger().info("✅ Rotação manual de logs concluída");
            } catch (Exception e) {
                plugin.getLogger().severe("❌ Erro durante rotação manual de logs: " + e.getMessage());
                e.printStackTrace();
            } finally {
                isRunning.set(false);
            }
        } else {
            plugin.getLogger().warning("Rotação de logs já está em execução!");
        }
    }
    
    /**
     * Executa a rotação de logs.
     */
    private void performLogRotation() {
        try {
            // 1. Verificar configurações
            int retentionDays = plugin.getConfig().getInt("log_rotation.retention_days", 30);
            boolean archiveEnabled = plugin.getConfig().getBoolean("log_rotation.archive_enabled", true);
            String archivePath = plugin.getConfig().getString("log_rotation.archive_path", "logs/chat_archive");
            
            plugin.getLogger().info("📊 Configurações: Retenção=" + retentionDays + " dias, Arquivamento=" + archiveEnabled);
            
            // 2. Calcular data limite
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            Date cutoffDate = new Date(cutoffTime);
            
            // 3. Executar rotação no banco de dados
            int deletedCount = rotateDatabaseLogs(cutoffDate, archiveEnabled, archivePath);
            
            // 4. Limpar logs de arquivo se habilitado
            if (plugin.getConfig().getBoolean("log_rotation.cleanup_file_logs", true)) {
                cleanupFileLogs(retentionDays);
            }
            
            plugin.getLogger().info("📊 Rotação concluída: " + deletedCount + " registros processados");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Erro durante rotação de logs: " + e.getMessage());
            throw new RuntimeException("Falha na rotação de logs", e);
        }
    }
    
    /**
     * Rotaciona logs no banco de dados.
     */
    private int rotateDatabaseLogs(Date cutoffDate, boolean archiveEnabled, String archivePath) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = br.com.primeleague.core.api.PrimeLeagueAPI.getDataManager().getConnection();
            if (conn == null) {
                plugin.getLogger().severe("Não foi possível obter conexão com o banco de dados!");
                return 0;
            }
            
            int totalProcessed = 0;
            
            // 1. Arquivar logs antigos se habilitado
            if (archiveEnabled) {
                totalProcessed += archiveOldLogs(conn, cutoffDate, archivePath);
            }
            
            // 2. Deletar logs antigos
            totalProcessed += deleteOldLogs(conn, cutoffDate);
            
            return totalProcessed;
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Erro SQL durante rotação de logs: " + e.getMessage());
            throw new RuntimeException("Erro de banco de dados", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * Arquivar logs antigos em arquivo.
     */
    private int archiveOldLogs(Connection conn, Date cutoffDate, String archivePath) throws SQLException {
        // Criar diretório de arquivo se não existir
        File archiveDir = new File(archivePath);
        if (!archiveDir.exists()) {
            if (!archiveDir.mkdirs()) {
                plugin.getLogger().warning("Não foi possível criar diretório de arquivo: " + archivePath);
                return 0;
            }
        }
        
        // Nome do arquivo de arquivo
        String archiveFileName = "chat_logs_" + dateFormat.format(new Date()) + ".txt";
        File archiveFile = new File(archiveDir, archiveFileName);
        
        // Buscar logs antigos
        String selectSql = "SELECT * FROM chat_logs WHERE timestamp < ? ORDER BY timestamp ASC";
        PreparedStatement selectStmt = conn.prepareStatement(selectSql);
        selectStmt.setTimestamp(1, new java.sql.Timestamp(cutoffDate.getTime()));
        
        ResultSet rs = selectStmt.executeQuery();
        
        int archivedCount = 0;
        try (PrintWriter writer = new PrintWriter(new FileWriter(archiveFile, true))) {
            writer.println("=== ARQUIVO DE LOGS DE CHAT - " + new Date() + " ===");
            writer.println("Logs anteriores a: " + cutoffDate);
            writer.println();
            
            while (rs.next()) {
                // Formatar linha do log
                String logLine = formatLogLine(rs);
                writer.println(logLine);
                archivedCount++;
            }
            
            writer.println();
            writer.println("=== FIM DO ARQUIVO - " + archivedCount + " registros ===");
        } catch (IOException e) {
            plugin.getLogger().severe("Erro ao escrever arquivo de arquivo: " + e.getMessage());
            return 0;
        } finally {
            rs.close();
            selectStmt.close();
        }
        
        plugin.getLogger().info("📁 Arquivados " + archivedCount + " registros em: " + archiveFile.getAbsolutePath());
        return archivedCount;
    }
    
    /**
     * Deletar logs antigos do banco de dados.
     */
    private int deleteOldLogs(Connection conn, Date cutoffDate) throws SQLException {
        String deleteSql = "DELETE FROM chat_logs WHERE timestamp < ?";
        PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
        deleteStmt.setTimestamp(1, new java.sql.Timestamp(cutoffDate.getTime()));
        
        int deletedCount = deleteStmt.executeUpdate();
        deleteStmt.close();
        
        plugin.getLogger().info("🗑️ Deletados " + deletedCount + " registros antigos do banco de dados");
        return deletedCount;
    }
    
    /**
     * Formatar linha de log para arquivo.
     */
    private String formatLogLine(ResultSet rs) throws SQLException {
        StringBuilder line = new StringBuilder();
        line.append("[").append(rs.getTimestamp("timestamp")).append("] ");
        line.append("Player: ").append(rs.getString("player_name")).append(" ");
        line.append("Channel: ").append(rs.getString("channel")).append(" ");
        line.append("Message: ").append(rs.getString("message_content"));
        return line.toString();
    }
    
    /**
     * Limpar logs de arquivo antigos.
     */
    private void cleanupFileLogs(int retentionDays) {
        try {
            String logPath = plugin.getConfig().getString("log_rotation.file_logs_path", "logs");
            File logDir = new File(logPath);
            
            if (!logDir.exists()) {
                return;
            }
            
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
            int deletedFiles = 0;
            
            File[] logFiles = logDir.listFiles();
            if (logFiles != null) {
                for (File file : logFiles) {
                    if (file.isFile() && file.lastModified() < cutoffTime) {
                        if (file.delete()) {
                            deletedFiles++;
                        }
                    }
                }
            }
            
            if (deletedFiles > 0) {
                plugin.getLogger().info("🗑️ Deletados " + deletedFiles + " arquivos de log antigos");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao limpar arquivos de log: " + e.getMessage());
        }
    }
    
    /**
     * Fechar recursos de banco de dados.
     */
    private void closeResources(Connection conn, PreparedStatement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Erro ao fechar recursos de banco: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se o sistema está em execução.
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * Obtém estatísticas do sistema de rotação.
     */
    public String getRotationStats() {
        return "Status: " + (isRunning.get() ? "Executando" : "Parado") + 
               ", Habilitado: " + plugin.getConfig().getBoolean("log_rotation.enabled", true) +
               ", Retenção: " + plugin.getConfig().getInt("log_rotation.retention_days", 30) + " dias";
    }
}
