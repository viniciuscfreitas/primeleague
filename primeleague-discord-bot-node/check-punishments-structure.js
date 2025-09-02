// ========================================
// VERIFICAÇÃO DA ESTRUTURA DA TABELA punishments
// ========================================
// Este script verifica a estrutura atual da tabela punishments

const mysql = require('mysql2/promise');

async function checkPunishmentsStructure() {
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
        console.log('🔍 Verificando estrutura da tabela punishments...');
        const [currentColumns] = await connection.execute('DESCRIBE punishments');
        
        console.log('📊 Estrutura atual da tabela punishments:');
        currentColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se a coluna severity existe
        const severityColumn = currentColumns.find(col => col.Field === 'severity');

        if (severityColumn) {
            console.log(`\n📋 Coluna 'severity': ${severityColumn.Type} (${severityColumn.Null})`);
        } else {
            console.log('\n❌ Coluna "severity" não encontrada!');
            console.log('💡 RECOMENDAÇÃO: Adicionar a coluna severity ou remover do schema');
        }

        console.log('\n💡 RECOMENDAÇÃO:');
        console.log('   - Se a coluna severity não existe, podemos adicioná-la');
        console.log('   - Ou podemos remover a coluna do schema se não for necessária');

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
checkPunishmentsStructure();
