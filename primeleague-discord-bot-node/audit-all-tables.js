// ========================================
// AUDITORIA COMPLETA DE TODAS AS TABELAS
// ========================================
// Este script verifica todas as tabelas definidas no schema
// e identifica quais estão faltando no banco

const mysql = require('mysql2/promise');

async function auditAllTables() {
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

        // Lista de todas as tabelas definidas no schema
        const expectedTables = [
            'player_data',
            'clans',
            'clan_players',
            'clan_alliances',
            'clan_logs',
            'clan_event_wins',
            'donors',
            'economy_logs',
            'discord_links',
            'discord_users',
            'discord_link_history',
            'player_authorized_ips',
            'recovery_codes',
            'whitelist_players',
            'punishments',
            'tickets',
            'staff_vanish',
            'chat_logs',
            'server_notifications'
        ];

        console.log('📋 Verificando tabelas esperadas...\n');

        const missingTables = [];
        const existingTables = [];

        for (const tableName of expectedTables) {
            try {
                const [rows] = await connection.execute(`DESCRIBE ${tableName}`);
                existingTables.push(tableName);
                console.log(`✅ ${tableName} - EXISTE (${rows.length} colunas)`);
            } catch (error) {
                if (error.code === 'ER_NO_SUCH_TABLE') {
                    missingTables.push(tableName);
                    console.log(`❌ ${tableName} - FALTANDO`);
                } else {
                    console.log(`⚠️ ${tableName} - ERRO: ${error.message}`);
                }
            }
        }

        console.log('\n📊 RESUMO DA AUDITORIA:');
        console.log(`✅ Tabelas existentes: ${existingTables.length}`);
        console.log(`❌ Tabelas faltando: ${missingTables.length}`);
        console.log(`📋 Total esperado: ${expectedTables.length}`);

        if (missingTables.length > 0) {
            console.log('\n🚨 TABELAS FALTANDO:');
            missingTables.forEach(table => {
                console.log(`   - ${table}`);
            });

            console.log('\n🔧 PRÓXIMOS PASSOS:');
            console.log('1. Criar as tabelas faltantes');
            console.log('2. Verificar se há colunas faltando nas tabelas existentes');
            console.log('3. Testar o servidor novamente');
        } else {
            console.log('\n🎉 TODAS AS TABELAS ESTÃO PRESENTES!');
            console.log('🚀 O SchemaValidator deve passar na validação completa!');
        }

        // Verificar tabelas extras no banco
        console.log('\n🔍 Verificando tabelas extras no banco...');
        const [allTables] = await connection.execute('SHOW TABLES');
        const dbTables = allTables.map(row => Object.values(row)[0]);
        
        const extraTables = dbTables.filter(table => !expectedTables.includes(table));
        if (extraTables.length > 0) {
            console.log('\n📝 TABELAS EXTRAS NO BANCO:');
            extraTables.forEach(table => {
                console.log(`   - ${table}`);
            });
        }

    } catch (error) {
        console.error('❌ Erro durante auditoria:', error.message);
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
auditAllTables();
