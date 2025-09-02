const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function compareSchemas() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 COMPARANDO SCHEMA ANEXADO vs BANCO ATUAL');
        console.log('==============================================\n');
        
        // 1. Ler o schema anexado
        const schemaPath = path.join(__dirname, '..', 'primeleague_schema.sql');
        if (!fs.existsSync(schemaPath)) {
            console.log('❌ Arquivo primeleague_schema.sql não encontrado!');
            return;
        }
        
        const schemaContent = fs.readFileSync(schemaPath, 'utf8');
        
        // 2. Extrair tabelas do schema anexado
        const createTableRegex = /CREATE TABLE[^;]+;/gi;
        const createTables = schemaContent.match(createTableRegex) || [];
        
        console.log('📋 TABELAS NO SCHEMA ANEXADO:');
        const schemaTables = [];
        createTables.forEach((createTable, index) => {
            const tableNameMatch = createTable.match(/`([^`]+)`/);
            if (tableNameMatch) {
                const tableName = tableNameMatch[1];
                schemaTables.push(tableName);
                console.log(`  ${index + 1}. ${tableName}`);
            }
        });
        
        console.log(`\n📊 Total no schema: ${schemaTables.length} tabelas\n`);
        
        // 3. Verificar tabelas no banco atual
        const [dbTables] = await pool.execute(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        console.log('📋 TABELAS NO BANCO ATUAL:');
        const currentTables = dbTables.map(t => t.table_name);
        currentTables.forEach((tableName, index) => {
            console.log(`  ${index + 1}. ${tableName}`);
        });
        
        console.log(`\n📊 Total no banco: ${currentTables.length} tabelas\n`);
        
        // 4. COMPARAÇÃO DETALHADA
        console.log('🎯 COMPARAÇÃO DETALHADA:\n');
        
        // Tabelas que estão no schema mas NÃO no banco
        const missingInDB = schemaTables.filter(table => !currentTables.includes(table));
        if (missingInDB.length > 0) {
            console.log('❌ TABELAS FALTANDO NO BANCO:');
            missingInDB.forEach(table => console.log(`  - ${table}`));
            console.log('');
        }
        
        // Tabelas que estão no banco mas NÃO no schema
        const missingInSchema = currentTables.filter(table => !schemaTables.includes(table));
        if (missingInSchema.length > 0) {
            console.log('❌ TABELAS FALTANDO NO SCHEMA:');
            missingInSchema.forEach(table => console.log(`  - ${table}`));
            console.log('');
        }
        
        // Tabelas que estão em ambos
        const commonTables = schemaTables.filter(table => currentTables.includes(table));
        if (commonTables.length > 0) {
            console.log('✅ TABELAS EM AMBOS (COMUNS):');
            commonTables.forEach(table => console.log(`  - ${table}`));
            console.log('');
        }
        
        // 5. VERIFICAR ESTRUTURA DAS TABELAS COMUNS
        console.log('🔍 VERIFICANDO ESTRUTURA DAS TABELAS COMUNS:\n');
        
        for (const tableName of commonTables.slice(0, 3)) { // Verificar apenas 3 para não sobrecarregar
            console.log(`📋 TABELA: ${tableName}`);
            console.log('─'.repeat(40));
            
            try {
                const [columns] = await pool.execute(`DESCRIBE \`${tableName}\``);
                console.log('  Colunas no banco:');
                columns.forEach(col => {
                    console.log(`    ${col.Field}: ${col.Type} ${col.Null === 'YES' ? 'NULL' : 'NOT NULL'}`);
                });
                
                // Extrair estrutura do schema anexado
                const tableCreateSQL = createTables.find(ct => ct.includes(`\`${tableName}\``));
                if (tableCreateSQL) {
                    console.log('  ✅ Estrutura encontrada no schema anexado');
                } else {
                    console.log('  ❌ Estrutura NÃO encontrada no schema anexado');
                }
                
            } catch (error) {
                console.log(`  ❌ Erro ao verificar: ${error.message}`);
            }
            
            console.log('');
        }
        
        // 6. RESUMO FINAL
        console.log('📊 RESUMO FINAL:');
        console.log('─'.repeat(40));
        console.log(`Schema anexado: ${schemaTables.length} tabelas`);
        console.log(`Banco atual: ${currentTables.length} tabelas`);
        console.log(`Tabelas comuns: ${commonTables.length}`);
        console.log(`Faltando no banco: ${missingInDB.length}`);
        console.log(`Faltando no schema: ${missingInSchema.length}`);
        
        if (missingInDB.length === 0 && missingInSchema.length === 0) {
            console.log('\n🎉 PERFEITO! Schema e banco estão 100% alinhados!');
        } else if (missingInDB.length === 0) {
            console.log('\n✅ Schema anexado está COMPLETO para o banco atual!');
        } else {
            console.log('\n⚠️  Schema anexado está INCOMPLETO para o banco atual!');
        }
        
    } catch (error) {
        console.error('❌ Erro:', error.message);
    } finally {
        await pool.end();
    }
}

compareSchemas();
