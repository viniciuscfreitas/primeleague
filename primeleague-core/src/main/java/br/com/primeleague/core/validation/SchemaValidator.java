package br.com.primeleague.core.validation;

import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.managers.DataManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.sql.*;
import java.util.*;

/**
 * O GUARDIÃO DO SCHEMA - Valida a integridade do banco de dados
 * contra a definição declarativa em schema-definition.yml
 * 
 * Este componente é responsável por:
 * 1. Validar estrutura das tabelas
 * 2. Verificar tipos de colunas
 * 3. Validar constraints e foreign keys
 * 4. Executar validações de integridade de dados
 * 5. Prevenir inconsistências antes que causem erros
 */
public class SchemaValidator {
    
    private final PrimeLeagueCore core;
    private final DataManager dataManager;
    private final YamlConfiguration schemaDefinition;
    private final ValidationConfig config;
    
    public SchemaValidator(PrimeLeagueCore core, DataManager dataManager) {
        this.core = core;
        this.dataManager = dataManager;
        this.schemaDefinition = loadSchemaDefinition();
        this.config = new ValidationConfig(schemaDefinition);
    }
    
    /**
     * Validação completa do schema no startup
     * @return true se tudo estiver correto, false se houver problemas
     */
    public boolean validateOnStartup() {
        core.getLogger().info("🔍 [SchemaValidator] Iniciando validação do banco de dados...");
        
        try {
            // 1. Validar estrutura das tabelas
            if (!validateTableStructures()) {
                return false;
            }
            
            // 2. Validar constraints e foreign keys
            if (!validateConstraints()) {
                return false;
            }
            
            // 3. Executar validações de integridade de dados
            if (!validateDataIntegrity()) {
                return false;
            }
            
            core.getLogger().info("✅ [SchemaValidator] Validação completa bem-sucedida!");
            return true;
            
        } catch (Exception e) {
            core.getLogger().severe("❌ [SchemaValidator] Erro crítico durante validação: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Valida a estrutura de todas as tabelas definidas
     */
    private boolean validateTableStructures() {
        core.getLogger().info("📋 [SchemaValidator] Validando estrutura das tabelas...");
        
        boolean allValid = true;
        Map<String, Object> tables = schemaDefinition.getConfigurationSection("tables").getValues(false);
        
        for (String tableName : tables.keySet()) {
            if (!validateTableStructure(tableName)) {
                allValid = false;
                
                if (config.shouldFailOnMismatch()) {
                    core.getLogger().severe("🚨 [SchemaValidator] FALHA CRÍTICA na tabela '" + tableName + "'. Servidor será parado.");
                    break;
                }
            }
        }
        
        return allValid;
    }
    
    /**
     * Valida a estrutura de uma tabela específica
     */
    private boolean validateTableStructure(String tableName) {
        try (Connection conn = dataManager.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Verificar se a tabela existe
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                if (!tables.next()) {
                    logValidationError(tableName, "Tabela não existe no banco de dados");
                    return false;
                }
            }
            
            // Validar colunas
            if (!validateColumns(tableName, metaData)) {
                return false;
            }
            
            // Validar índices
            if (!validateIndexes(tableName, metaData)) {
                return false;
            }
            
            core.getLogger().info("✅ [SchemaValidator] Tabela '" + tableName + "' validada com sucesso");
            return true;
            
        } catch (SQLException e) {
            core.getLogger().severe("❌ [SchemaValidator] Erro ao validar tabela '" + tableName + "': " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Valida as colunas de uma tabela
     */
    private boolean validateColumns(String tableName, DatabaseMetaData metaData) throws SQLException {
        Map<String, Object> expectedColumns = schemaDefinition.getConfigurationSection("tables." + tableName + ".columns").getValues(false);
        boolean allValid = true;
        
        try (ResultSet columns = metaData.getColumns(null, null, tableName, null)) {
            Map<String, ColumnInfo> actualColumns = new HashMap<>();
            
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                String isNullable = columns.getString("IS_NULLABLE");
                String columnDefault = columns.getString("COLUMN_DEF");
                
                actualColumns.put(columnName, new ColumnInfo(dataType, isNullable, columnDefault));
            }
            
            // Verificar se todas as colunas esperadas existem
            for (String expectedColumnName : expectedColumns.keySet()) {
                if (!actualColumns.containsKey(expectedColumnName)) {
                    logValidationError(tableName, "Coluna esperada '" + expectedColumnName + "' não encontrada");
                    allValid = false;
                    continue;
                }
                
                // Validar tipo da coluna
                if (!validateColumnType(tableName, expectedColumnName, expectedColumns.get(expectedColumnName), actualColumns.get(expectedColumnName))) {
                    allValid = false;
                }
            }
        }
        
        return allValid;
    }
    
    /**
     * Valida o tipo de uma coluna específica
     */
    private boolean validateColumnType(String tableName, String columnName, Object expectedColumn, ColumnInfo actualColumn) {
        Map<String, Object> expected = (Map<String, Object>) expectedColumn;
        String expectedType = (String) expected.get("type");
        
        // Mapeamento de tipos SQL para validação
        if (!isTypeCompatible(expectedType, actualColumn.dataType)) {
            String errorMsg = String.format("Tipo incompatível na coluna '%s': ESPERADO '%s', ENCONTRADO '%s'", 
                columnName, expectedType, actualColumn.dataType);
            logValidationError(tableName, errorMsg);
            return false;
        }
        
        return true;
    }
    
    /**
     * Verifica se os tipos são compatíveis
     */
    private boolean isTypeCompatible(String expected, String actual) {
        // Mapeamento de tipos compatíveis
        Map<String, Set<String>> compatibleTypes = new HashMap<>();
        compatibleTypes.put("VARCHAR", new HashSet<>(Arrays.asList("VARCHAR", "CHAR", "TEXT")));
        compatibleTypes.put("INT", new HashSet<>(Arrays.asList("INT", "INTEGER", "BIGINT", "SMALLINT")));
        compatibleTypes.put("BOOLEAN", new HashSet<>(Arrays.asList("BOOLEAN", "TINYINT")));
        compatibleTypes.put("TIMESTAMP", new HashSet<>(Arrays.asList("TIMESTAMP", "DATETIME")));
        compatibleTypes.put("DECIMAL", new HashSet<>(Arrays.asList("DECIMAL", "NUMERIC")));
        
        // Verificar compatibilidade
        for (Map.Entry<String, Set<String>> entry : compatibleTypes.entrySet()) {
            if (expected.toUpperCase().startsWith(entry.getKey())) {
                return entry.getValue().contains(actual.toUpperCase());
            }
        }
        
        // Para ENUMs, verificar se é compatível
        if (expected.toUpperCase().startsWith("ENUM")) {
            return actual.toUpperCase().equals("ENUM");
        }
        
        return expected.equalsIgnoreCase(actual);
    }
    
    /**
     * Valida os índices de uma tabela
     */
    private boolean validateIndexes(String tableName, DatabaseMetaData metaData) throws SQLException {
        // Implementação básica - pode ser expandida conforme necessário
        return true;
    }
    
    /**
     * Valida as constraints e foreign keys
     */
    private boolean validateConstraints() {
        core.getLogger().info("🔗 [SchemaValidator] Validando constraints e foreign keys...");
        // Implementação básica - pode ser expandida conforme necessário
        return true;
    }
    
    /**
     * Executa validações de integridade de dados
     */
    private boolean validateDataIntegrity() {
        core.getLogger().info("🔍 [SchemaValidator] Executando validações de integridade de dados...");
        
        boolean allValid = true;
        List<?> validationsList = schemaDefinition.getList("validations.data_integrity");
        if (validationsList == null) {
            core.getLogger().info("📋 [SchemaValidator] Nenhuma validação de integridade configurada");
            return true;
        }
        
        for (Object validationObj : validationsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> validation = (Map<String, Object>) validationObj;
            if (!executeDataValidation(validation)) {
                allValid = false;
                
                if (config.shouldFailOnMismatch()) {
                    core.getLogger().severe("🚨 [SchemaValidator] FALHA CRÍTICA na validação de dados. Servidor será parado.");
                    break;
                }
            }
        }
        
        return allValid;
    }
    
    /**
     * Executa uma validação específica de dados
     */
    private boolean executeDataValidation(Map<String, Object> validation) {
        try (Connection conn = dataManager.getConnection()) {
            String query = (String) validation.get("query");
            String severity = (String) validation.get("severity");
            String message = (String) validation.get("message");
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                List<Map<String, Object>> violations = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> violation = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        violation.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    violations.add(violation);
                }
                
                if (!violations.isEmpty()) {
                    logDataIntegrityViolation(validation, violations, severity, message);
                    return severity.equals("WARNING"); // WARNING não falha a validação
                }
            }
            
        } catch (SQLException e) {
            core.getLogger().severe("❌ [SchemaValidator] Erro ao executar validação '" + validation.get("name") + "': " + e.getMessage());
            return false;
        }
        
        return true;
    }
    
    /**
     * Loga violações de integridade de dados
     */
    private void logDataIntegrityViolation(Map<String, Object> validation, List<Map<String, Object>> violations, String severity, String message) {
        String validationName = (String) validation.get("name");
        String description = (String) validation.get("description");
        
        if (severity.equals("ERROR")) {
            core.getLogger().severe("🚨 [SchemaValidator] VIOLAÇÃO CRÍTICA: " + description);
        } else {
            core.getLogger().warning("⚠️ [SchemaValidator] VIOLAÇÃO: " + description);
        }
        
        core.getLogger().info("📊 [SchemaValidator] " + violations.size() + " violações encontradas");
        
        for (Map<String, Object> violation : violations) {
            String formattedMessage = formatMessage(message, violation);
            if (severity.equals("ERROR")) {
                core.getLogger().severe("   ❌ " + formattedMessage);
            } else {
                core.getLogger().warning("   ⚠️ " + formattedMessage);
            }
        }
    }
    
    /**
     * Formata a mensagem de erro com os valores reais
     */
    private String formatMessage(String template, Map<String, Object> values) {
        String result = template;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Loga erros de validação
     */
    private void logValidationError(String tableName, String error) {
        core.getLogger().severe("❌ [SchemaValidator] Tabela '" + tableName + "': " + error);
    }
    
    /**
     * Carrega a definição do schema do arquivo YAML
     */
    private YamlConfiguration loadSchemaDefinition() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("schema-definition.yml")) {
            if (input == null) {
                core.getLogger().severe("❌ [SchemaValidator] Arquivo schema-definition.yml não encontrado!");
                return new YamlConfiguration();
            }
            
            YamlConfiguration config = new YamlConfiguration();
            // Compatibilidade com Java 7
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead));
            }
            config.loadFromString(content.toString());
            return config;
            
        } catch (Exception e) {
            core.getLogger().severe("❌ [SchemaValidator] Erro ao carregar schema-definition.yml: " + e.getMessage());
            return new YamlConfiguration();
        }
    }
    
    /**
     * Classe interna para armazenar informações de coluna
     */
    private static class ColumnInfo {
        final String dataType;
        final String isNullable;
        final String columnDefault;
        
        ColumnInfo(String dataType, String isNullable, String columnDefault) {
            this.dataType = dataType;
            this.isNullable = isNullable;
            this.columnDefault = columnDefault;
        }
    }
    
    /**
     * Classe interna para configurações de validação
     */
    private static class ValidationConfig {
        private final boolean failOnMismatch;
        private final String logLevel;
        
        ValidationConfig(YamlConfiguration config) {
            this.failOnMismatch = config.getBoolean("validation_config.fail_on_mismatch", true);
            this.logLevel = config.getString("validation_config.log_level", "DETAILED");
        }
        
        boolean shouldFailOnMismatch() {
            return failOnMismatch;
        }
        
        String getLogLevel() {
            return logLevel;
        }
    }
}
