// ========================================
// VERIFICAÇÃO DA ESTRUTURA DA TABELA whitelist_players
// ========================================
// Este script verifica a estrutura atual da tabela whitelist_players

const mysql = require('mysql2/promise');

async function checkWhitelistStructure() {
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
        console.log('🔍 Verificando estrutura da tabela whitelist_players...');
        const [currentColumns] = await connection.execute('DESCRIBE whitelist_players');
        
        console.log('📊 Estrutura atual da tabela whitelist_players:');
        currentColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se as colunas problemáticas existem
        const reasonColumn = currentColumns.find(col => col.Field === 'reason');
        const removalReasonColumn = currentColumns.find(col => col.Field === 'removal_reason');

        if (reasonColumn) {
            console.log(`\n📋 Coluna 'reason': ${reasonColumn.Type} (${reasonColumn.Null})`);
        }

        if (removalReasonColumn) {
            console.log(`📋 Coluna 'removal_reason': ${removalReasonColumn.Type} (${removalReasonColumn.Null})`);
        }

        console.log('\n💡 RECOMENDAÇÃO:');
        console.log('   - Se as colunas são VARCHAR, podemos alterar para TEXT');
        console.log('   - Ou podemos atualizar o schema para aceitar ambos os tipos');

    } catch (error) {
        console.error('❌ Erro ao verificar tabela:', error.message);
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
checkWhitelistStructure();
