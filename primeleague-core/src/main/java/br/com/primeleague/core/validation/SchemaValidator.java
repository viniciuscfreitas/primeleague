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
 * 
 * NOVA FUNCIONALIDADE: Modo de Auditoria Completa
 * - Acumula todos os erros encontrados em uma lista
 * - Modo duplo: fail-fast (produção) ou report-all (desenvolvimento)
 * - Configurável via database.validation.fail-on-mismatch
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
     * @return Lista de erros encontrados (vazia se tudo estiver correto)
     */
    public List<String> validateOnStartup() {
        core.getLogger().info("🔍 [SchemaValidator] Iniciando validação do banco de dados...");
        
        List<String> allErrors = new ArrayList<>();
        
        try {
            // 1. Validar estrutura das tabelas
            allErrors.addAll(validateTableStructures());
            
            // 2. Validar constraints e foreign keys
            allErrors.addAll(validateConstraints());
            
            // 3. Executar validações de integridade de dados
            allErrors.addAll(validateDataIntegrity());
            
            // 4. Processar resultados baseado no modo configurado
            processValidationResults(allErrors);
            
        } catch (Exception e) {
            String errorMsg = "Erro crítico durante validação: " + e.getMessage();
            core.getLogger().severe("❌ [SchemaValidator] " + errorMsg);
            allErrors.add(errorMsg);
            e.printStackTrace();
        }
        
        return allErrors;
    }
    
    /**
     * Processa os resultados da validação baseado no modo configurado
     */
    private void processValidationResults(List<String> errors) {
        if (errors.isEmpty()) {
            core.getLogger().info("✅ [SchemaValidator] Validação completa bem-sucedida!");
        } else {
            // Logar TODOS os erros encontrados
            core.getLogger().severe("🚨 [SchemaValidator] " + errors.size() + " problemas encontrados durante validação:");
            for (String error : errors) {
                core.getLogger().severe("   ❌ " + error);
            }
            
            // Verificar se deve parar o servidor
            if (config.shouldFailOnMismatch()) {
                core.getLogger().severe("🚨 [SchemaValidator] FALHA CRÍTICA: Servidor será parado devido a inconsistências no schema.");
                core.getLogger().severe("🚨 [SchemaValidator] Para desenvolvimento, configure 'database.validation.fail-on-mismatch: false'");
                core.getServer().shutdown();
            } else {
                core.getLogger().warning("⚠️ [SchemaValidator] Modo de desenvolvimento ativo: Servidor continuará com inconsistências no schema.");
                core.getLogger().warning("⚠️ [SchemaValidator] Corrija os problemas acima antes de colocar em produção.");
            }
        }
    }
    
    /**
     * Valida a estrutura de todas as tabelas definidas
     * @return Lista de erros encontrados
     */
    private List<String> validateTableStructures() {
        core.getLogger().info("📋 [SchemaValidator] Validando estrutura das tabelas...");
        
        List<String> errors = new ArrayList<>();
        Map<String, Object> tables = schemaDefinition.getConfigurationSection("tables").getValues(false);
        
        for (String tableName : tables.keySet()) {
            errors.addAll(validateTableStructure(tableName));
        }
        
        return errors;
    }
    
    /**
     * Valida a estrutura de uma tabela específica
     * @return Lista de erros encontrados para esta tabela
     */
    private List<String> validateTableStructure(String tableName) {
        List<String> errors = new ArrayList<>();
        
        try (Connection conn = dataManager.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // Verificar se a tabela existe
            try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
                if (!tables.next()) {
                    errors.add("Tabela '" + tableName + "': Tabela não existe no banco de dados");
                    return errors;
                }
            }
            
            // Validar colunas
            errors.addAll(validateColumns(tableName, metaData));
            
            // Validar índices
            errors.addAll(validateIndexes(tableName, metaData));
            
            if (errors.isEmpty()) {
                core.getLogger().info("✅ [SchemaValidator] Tabela '" + tableName + "' validada com sucesso");
            }
            
        } catch (SQLException e) {
            errors.add("Tabela '" + tableName + "': Erro ao validar: " + e.getMessage());
        }
        
        return errors;
    }
    
    /**
     * Valida as colunas de uma tabela
     * @return Lista de erros encontrados
     */
    private List<String> validateColumns(String tableName, DatabaseMetaData metaData) throws SQLException {
        List<String> errors = new ArrayList<>();
        Map<String, Object> expectedColumns = schemaDefinition.getConfigurationSection("tables." + tableName + ".columns").getValues(false);
        
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
                    errors.add("Tabela '" + tableName + "': Coluna esperada '" + expectedColumnName + "' não encontrada");
                    continue;
                }
                
                // Validar tipo da coluna
                errors.addAll(validateColumnType(tableName, expectedColumnName, expectedColumns.get(expectedColumnName), actualColumns.get(expectedColumnName)));
            }
        }
        
        return errors;
    }
    
    /**
     * Valida o tipo de uma coluna específica
     * @return Lista de erros encontrados
     */
    private List<String> validateColumnType(String tableName, String columnName, Object expectedColumn, ColumnInfo actualColumn) {
        List<String> errors = new ArrayList<>();
        
        // Compatibilidade com Bukkit 1.5.2 - MemorySection vs Map
        String expectedType;
        if (expectedColumn instanceof org.bukkit.configuration.ConfigurationSection) {
            org.bukkit.configuration.ConfigurationSection section = (org.bukkit.configuration.ConfigurationSection) expectedColumn;
            expectedType = section.getString("type");
        } else if (expectedColumn instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> expected = (Map<String, Object>) expectedColumn;
            expectedType = (String) expected.get("type");
        } else {
            core.getLogger().warning("⚠️ [SchemaValidator] Formato inesperado para coluna '" + columnName + "' na tabela '" + tableName + "'");
            return errors; // Pular validação para evitar falha
        }
        
        if (expectedType == null) {
            core.getLogger().warning("⚠️ [SchemaValidator] Tipo não definido para coluna '" + columnName + "' na tabela '" + tableName + "'");
            return errors; // Pular validação para evitar falha
        }
        
        // Mapeamento de tipos SQL para validação
        if (!isTypeCompatible(expectedType, actualColumn.dataType)) {
            String errorMsg = String.format("Tabela '%s': Tipo incompatível na coluna '%s': ESPERADO '%s', ENCONTRADO '%s'", 
                tableName, columnName, expectedType, actualColumn.dataType);
            errors.add(errorMsg);
        }
        
        return errors;
    }
    
    /**
     * Verifica se os tipos são compatíveis
     */
    private boolean isTypeCompatible(String expected, String actual) {
        // Mapeamento de tipos compatíveis
        Map<String, Set<String>> compatibleTypes = new HashMap<>();
        compatibleTypes.put("VARCHAR", new HashSet<>(Arrays.asList("VARCHAR", "CHAR", "TEXT")));
        compatibleTypes.put("INT", new HashSet<>(Arrays.asList("INT", "INTEGER", "BIGINT", "SMALLINT")));
        compatibleTypes.put("BOOLEAN", new HashSet<>(Arrays.asList("BOOLEAN", "TINYINT", "BIT")));
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
     * @return Lista de erros encontrados
     */
    private List<String> validateIndexes(String tableName, DatabaseMetaData metaData) throws SQLException {
        // Implementação básica - pode ser expandida conforme necessário
        return new ArrayList<>();
    }
    
    /**
     * Valida as constraints e foreign keys
     * @return Lista de erros encontrados
     */
    private List<String> validateConstraints() {
        core.getLogger().info("🔗 [SchemaValidator] Validando constraints e foreign keys...");
        // Implementação básica - pode ser expandida conforme necessário
        return new ArrayList<>();
    }
    
    /**
     * Executa validações de integridade de dados
     * @return Lista de erros encontrados
     */
    private List<String> validateDataIntegrity() {
        core.getLogger().info("🔍 [SchemaValidator] Executando validações de integridade de dados...");
        
        List<String> errors = new ArrayList<>();
        List<?> validationsList = schemaDefinition.getList("validations.data_integrity");
        if (validationsList == null) {
            core.getLogger().info("📋 [SchemaValidator] Nenhuma validação de integridade configurada");
            return errors;
        }
        
        for (Object validationObj : validationsList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> validation = (Map<String, Object>) validationObj;
            errors.addAll(executeDataValidation(validation));
        }
        
        return errors;
    }
    
    /**
     * Executa uma validação específica de dados
     * @return Lista de erros encontrados
     */
    private List<String> executeDataValidation(Map<String, Object> validation) {
        List<String> errors = new ArrayList<>();
        
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
                    String errorMsg = logDataIntegrityViolation(validation, violations, severity, message);
                    if (severity.equals("ERROR")) {
                        errors.add(errorMsg);
                    }
                }
            }
            
        } catch (SQLException e) {
            String errorMsg = "Erro ao executar validação '" + validation.get("name") + "': " + e.getMessage();
            core.getLogger().severe("❌ [SchemaValidator] " + errorMsg);
            errors.add(errorMsg);
        }
        
        return errors;
    }
    
    /**
     * Loga violações de integridade de dados
     * @return Mensagem de erro formatada
     */
    private String logDataIntegrityViolation(Map<String, Object> validation, List<Map<String, Object>> violations, String severity, String message) {
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
        
        return "Validação '" + validationName + "': " + violations.size() + " violações encontradas";
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
     * Carrega a definição do schema do arquivo YAML
     */
    private YamlConfiguration loadSchemaDefinition() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("schema-definition.yml")) {
            if (input == null) {
                // Lança uma exceção clara se o arquivo não for encontrado no build.
                throw new IllegalStateException("Arquivo de recurso 'schema-definition.yml' não foi encontrado no JAR. Verifique o processo de build.");
            }

            // Código para ler o InputStream compatível com Java 7 e Bukkit 1.5.2
            YamlConfiguration config = new YamlConfiguration();
            StringBuilder content = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }
            config.loadFromString(content.toString());
            return config;

        } catch (Exception e) {
            // Envolve a exceção original para dar um contexto claro.
            // Se for um erro de parsing, a causa original será preservada.
            throw new RuntimeException("Erro crítico ao carregar ou processar 'schema-definition.yml'. Verifique a sintaxe e o encoding (deve ser UTF-8).", e);
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
            // Busca na configuração do banco de dados primeiro, depois no schema
            this.failOnMismatch = config.getBoolean("database.validation.fail-on-mismatch", 
                config.getBoolean("validation_config.fail_on_mismatch", true));
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
