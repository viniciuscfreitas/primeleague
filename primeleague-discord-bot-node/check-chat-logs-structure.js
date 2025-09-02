// ========================================
// VERIFICAÇÃO DA ESTRUTURA DA TABELA chat_logs
// ========================================
// Este script verifica a estrutura atual da tabela chat_logs

const mysql = require('mysql2/promise');

async function checkChatLogsStructure() {
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
        console.log('🔍 Verificando estrutura da tabela chat_logs...');
        const [currentColumns] = await connection.execute('DESCRIBE chat_logs');
        
        console.log('📊 Estrutura atual da tabela chat_logs:');
        currentColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se a coluna channel_type existe e seu tipo
        const channelTypeColumn = currentColumns.find(col => col.Field === 'channel_type');

        if (channelTypeColumn) {
            console.log(`\n📋 Coluna 'channel_type': ${channelTypeColumn.Type} (${channelTypeColumn.Null})`);
            
            // Se for ENUM, mostrar os valores possíveis
            if (channelTypeColumn.Type.includes('enum')) {
                console.log('💡 É um ENUM - precisamos ver os valores possíveis');
            }
        } else {
            console.log('\n❌ Coluna "channel_type" não encontrada!');
        }

        console.log('\n💡 RECOMENDAÇÃO:');
        console.log('   - Se channel_type é ENUM, podemos atualizar o schema para aceitar ENUM');
        console.log('   - Ou podemos alterar o banco para VARCHAR');

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
checkChatLogsStructure();
