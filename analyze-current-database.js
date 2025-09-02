const mysql = require('mysql2/promise');
require('dotenv').config();

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    authPlugins: {
        mysql_native_password: () => () => Buffer.from('root', 'utf-8')
    }
};

async function analyzeCurrentDatabase() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 ANALISANDO BANCO DE DADOS ATUAL (MARIA DB)');
        console.log('================================================\n');
        
        // 1. Verificar todas as tabelas existentes
        console.log('📋 TABELAS EXISTENTES NO BANCO:');
        const [tables] = await pool.execute(`
            SELECT table_name, table_rows, data_length, index_length
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        let totalTables = 0;
        let totalRows = 0;
        let totalDataSize = 0;
        
        tables.forEach(table => {
            console.log(`  ✅ ${table.table_name}`);
            console.log(`     - Linhas: ${table.table_rows || 0}`);
            console.log(`     - Tamanho: ${((table.data_length + table.index_length) / 1024 / 1024).toFixed(2)} MB`);
            totalTables++;
            totalRows += (table.table_rows || 0);
            totalDataSize += (table.data_length + table.index_length);
        });
        
        console.log(`\n📊 Total: ${totalTables} tabelas, ${totalRows} linhas, ${(totalDataSize / 1024 / 1024).toFixed(2)} MB\n`);
        
        // 2. Verificar estrutura detalhada de cada tabela
        console.log('🔍 ESTRUTURA DETALHADA DAS TABELAS:\n');
        
        for (const table of tables) {
            const tableName = table.table_name;
            console.log(`📋 TABELA: ${tableName}`);
            console.log('─'.repeat(50));
            
            // Estrutura da tabela
            const [columns] = await pool.execute(`DESCRIBE \`${tableName}\``);
            console.log('  Colunas:');
            columns.forEach(col => {
                const nullable = col.Null === 'YES' ? 'NULL' : 'NOT NULL';
                const key = col.Key ? ` (${col.Key})` : '';
                const defaultVal = col.Default ? ` DEFAULT ${col.Default}` : '';
                console.log(`    ${col.Field}: ${col.Type} ${nullable}${key}${defaultVal}`);
            });
            
            // Índices da tabela
            const [indexes] = await pool.execute(`SHOW INDEX FROM \`${tableName}\``);
            if (indexes.length > 0) {
                console.log('  Índices:');
                indexes.forEach(idx => {
                    if (idx.Key_name !== 'PRIMARY') {
                        console.log(`    ${idx.Key_name}: ${idx.Column_name} (${idx.Non_unique ? 'Não único' : 'Único'})`);
                    }
                });
            }
            
            // Foreign Keys (compatível com MariaDB)
            try {
                const [foreignKeys] = await pool.execute(`
                    SELECT 
                        CONSTRAINT_NAME,
                        COLUMN_NAME,
                        REFERENCED_TABLE_NAME,
                        REFERENCED_COLUMN_NAME
                    FROM information_schema.KEY_COLUMN_USAGE 
                    WHERE TABLE_SCHEMA = 'primeleague' 
                    AND TABLE_NAME = '${tableName}' 
                    AND REFERENCED_TABLE_NAME IS NOT NULL
                `);
                
                if (foreignKeys.length > 0) {
                    console.log('  Foreign Keys:');
                    foreignKeys.forEach(fk => {
                        console.log(`    ${fk.CONSTRAINT_NAME}: ${fk.COLUMN_NAME} → ${fk.REFERENCED_TABLE_NAME}.${fk.REFERENCED_COLUMN_NAME}`);
                    });
                }
            } catch (error) {
                console.log(`  Foreign Keys: Erro ao verificar - ${error.message}`);
            }
            
            // Estatísticas da tabela
            try {
                const [rowCount] = await pool.execute(`SELECT COUNT(*) as count FROM \`${tableName}\``);
                console.log(`  Estatísticas: ${rowCount[0].count} registros`);
            } catch (error) {
                console.log(`  Estatísticas: Erro ao contar registros - ${error.message}`);
            }
            
            console.log('\n');
        }
        
        // 3. Verificar versão do MariaDB
        console.log('🔧 INFORMAÇÕES DO SERVIDOR:');
        const [version] = await pool.execute('SELECT VERSION() as version');
        console.log(`  Versão: ${version[0].version}`);
        
        const [charset] = await pool.execute('SHOW VARIABLES LIKE "character_set_database"');
        console.log(`  Charset: ${charset[0].Value}`);
        
        const [collation] = await pool.execute('SHOW VARIABLES LIKE "collation_database"');
        console.log(`  Collation: ${collation[0].Value}\n`);
        
        // 4. Verificar se há dados nas tabelas
        console.log('📊 VERIFICAÇÃO DE DADOS:');
        for (const table of tables) {
            try {
                const [sampleData] = await pool.execute(`SELECT * FROM \`${table.table_name}\` LIMIT 1`);
                if (sampleData.length > 0) {
                    console.log(`  ✅ ${table.table_name}: Tem dados`);
                } else {
                    console.log(`  ⚠️  ${table.table_name}: Vazia`);
                }
            } catch (error) {
                console.log(`  ❌ ${table.table_name}: Erro - ${error.message}`);
            }
        }
        
        console.log('\n🎯 RESUMO DA ANÁLISE:');
        console.log(`  - Total de tabelas: ${totalTables}`);
        console.log(`  - Total de registros: ${totalRows.toLocaleString()}`);
        console.log(`  - Tamanho total: ${(totalDataSize / 1024 / 1024).toFixed(2)} MB`);
        console.log(`  - Banco ativo e funcional: ✅`);
        
    } catch (error) {
        console.error('❌ ERRO AO ANALISAR BANCO:', error.message);
        console.error('Stack:', error.stack);
    } finally {
        await pool.end();
    }
}

// Executar análise
analyzeCurrentDatabase().catch(console.error);
