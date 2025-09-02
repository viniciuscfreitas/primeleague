// ========================================
// CORREÇÃO DA TABELA economy_logs
// ========================================
// Este script corrige a tabela economy_logs que está faltando colunas

const mysql = require('mysql2/promise');

async function fixEconomyLogsTable() {
    let connection;
    
    try {
        // Configuração da conexão
        connection = await mysql.createConnection({
            host: 'localhost',
            port: 3306,
            user: 'root',
            password: 'root',
            database: 'primeleague',
            authPlugins: {
                mysql_native_password: () => () => Buffer.from('root', 'utf-8')
            }
        });

        console.log('🔗 Conectado ao banco de dados...');

        // Verificar estrutura atual da tabela
        console.log('🔍 Verificando estrutura atual da tabela economy_logs...');
        const [currentColumns] = await connection.execute('DESCRIBE economy_logs');
        
        console.log('📊 Estrutura atual da tabela economy_logs:');
        currentColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se as colunas necessárias existem
        const existingColumns = currentColumns.map(col => col.Field);
        const needsCreatedAt = !existingColumns.includes('created_at');

        // A tabela já tem 'id' como auto_increment, então não precisamos adicionar log_id
        console.log('✅ Coluna id já existe como auto_increment');

        if (needsCreatedAt) {
            console.log('\n📋 Adicionando coluna created_at...');
            await connection.execute(`
                ALTER TABLE economy_logs 
                ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            `);
            console.log('✅ Coluna created_at adicionada com sucesso!');
        } else {
            console.log('✅ Coluna created_at já existe');
        }

        // Verificar estrutura final da tabela
        console.log('\n🔍 Verificando estrutura final da tabela economy_logs...');
        const [finalColumns] = await connection.execute('DESCRIBE economy_logs');
        
        console.log('📊 Estrutura final da tabela economy_logs:');
        finalColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se as colunas foram adicionadas corretamente
        const finalColumnNames = finalColumns.map(col => col.Field);
        if (finalColumnNames.includes('id') && finalColumnNames.includes('created_at')) {
            console.log('\n🎉 Todas as colunas necessárias estão presentes!');
            console.log('🚀 O SchemaValidator agora deve passar na validação completa!');
        } else {
            console.log('\n⚠️  Algumas colunas ainda estão faltando!');
        }

    } catch (error) {
        console.error('❌ Erro ao corrigir tabela:', error.message);
        if (error.code) {
            console.error('   Código de erro:', error.code);
        }
    } finally {
        if (connection) {
            await connection.end();
            console.log('\n🔌 Conexão fechada.');
        }
    }
}

// Executar o script
fixEconomyLogsTable();
