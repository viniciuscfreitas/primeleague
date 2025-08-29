const mysql = require('mysql2/promise');

async function checkCurrentSchema() {
    const connection = await mysql.createConnection({
        host: 'localhost',
        port: 3306,
        user: 'root',
        password: 'root',
        database: 'primeleague',
        authPlugins: {
            mysql_native_password: () => () => Buffer.from('root', 'utf-8')
        }
    });

    try {
        console.log('🔍 VERIFICAÇÃO DO ESTADO ATUAL DO BANCO');
        console.log('=====================================\n');

        // Verificar estrutura da tabela discord_links
        console.log('📋 ESTRUTURA DA TABELA discord_links:');
        const [discordLinksStructure] = await connection.execute(`
            DESCRIBE discord_links
        `);
        console.table(discordLinksStructure);

        // Verificar se player_id existe em discord_links
        const hasPlayerId = discordLinksStructure.some(col => col.Field === 'player_id');
        console.log(`\n🔍 player_id existe em discord_links: ${hasPlayerId ? 'SIM' : 'NÃO'}`);

        // Verificar foreign keys da tabela discord_links
        console.log('\n🔗 FOREIGN KEYS DA TABELA discord_links:');
        const [foreignKeys] = await connection.execute(`
            SELECT 
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `);
        console.table(foreignKeys);

        // Verificar dados existentes
        console.log('\n📊 DADOS EXISTENTES:');
        const [playerDataCount] = await connection.execute('SELECT COUNT(*) as total FROM player_data');
        const [discordLinksCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_links');
        const [discordUsersCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_users');
        
        console.log(`- player_data: ${playerDataCount[0].total} registros`);
        console.log(`- discord_links: ${discordLinksCount[0].total} registros`);
        console.log(`- discord_users: ${discordUsersCount[0].total} registros`);

        // Verificar se há dados que precisam ser migrados
        if (hasPlayerId) {
            console.log('\n⚠️  PROBLEMA IDENTIFICADO:');
            console.log('A tabela discord_links tem player_id, mas deveria usar apenas player_uuid');
            console.log('Isso indica que o banco está no estado ERRADO (com player_id)');
        } else {
            console.log('\n✅ ESTADO ATUAL:');
            console.log('A tabela discord_links está usando apenas player_uuid (estado correto)');
        }

        // Verificar se existe tabela donors (que não deveria existir)
        const [donorsTable] = await connection.execute(`
            SELECT COUNT(*) as exists FROM information_schema.tables 
            WHERE table_schema = 'primeleague' AND table_name = 'donors'
        `);
        
        if (donorsTable[0].exists > 0) {
            console.log('\n⚠️  TABELA donors EXISTE (não deveria):');
            console.log('Esta tabela não faz parte do schema canônico');
        }

    } catch (error) {
        console.error('❌ Erro ao verificar schema:', error.message);
    } finally {
        await connection.end();
    }
}

checkCurrentSchema();
