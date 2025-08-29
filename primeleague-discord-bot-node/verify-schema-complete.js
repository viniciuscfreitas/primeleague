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

async function verifyCompleteSchema() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 VERIFICAÇÃO COMPLETA DO SCHEMA');
        console.log('=====================================\n');
        
        // 1. Verificar todas as tabelas existentes
        console.log('📋 TABELAS EXISTENTES NO BANCO:');
        const [tables] = await pool.execute(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        tables.forEach(table => {
            console.log(`  ✅ ${table.table_name}`);
        });
        
        console.log(`\n📊 Total: ${tables.length} tabelas\n`);
        
        // 2. Verificar tabelas esperadas do schema
        const expectedTables = [
            'player_data',
            'discord_users', 
            'discord_links',
            'recovery_codes',
            'punishments',
            'tickets',
            'staff_vanish',
            'player_authorized_ips',
            'whitelist_players',
            'clans',
            'clan_players',
            'clan_alliances',
            'chat_logs',
            'server_notifications',
            'economy_logs',
            'server_stats'
        ];
        
        console.log('🎯 TABELAS ESPERADAS DO SCHEMA:');
        expectedTables.forEach(table => {
            const exists = tables.some(t => t.table_name === table);
            console.log(`  ${exists ? '✅' : '❌'} ${table}`);
        });
        
        // 3. Verificar estrutura crítica da discord_links
        console.log('\n🚨 VERIFICAÇÃO CRÍTICA - discord_links:');
        const [discordLinksStructure] = await pool.execute('DESCRIBE discord_links');
        console.log('Estrutura atual:');
        discordLinksStructure.forEach(field => {
            console.log(`  ${field.Field}: ${field.Type} ${field.Null} ${field.Key}`);
        });
        
        // 4. Verificar se player_id existe (PROBLEMA!)
        const hasPlayerId = discordLinksStructure.some(field => field.Field === 'player_id');
        const hasPlayerUuid = discordLinksStructure.some(field => field.Field === 'player_uuid');
        
        console.log('\n⚠️  ANÁLISE CRÍTICA:');
        console.log(`  player_id existe: ${hasPlayerId ? '❌ SIM (PROBLEMA!)' : '✅ NÃO (CORRETO)'}`);
        console.log(`  player_uuid existe: ${hasPlayerUuid ? '✅ SIM (CORRETO)' : '❌ NÃO (PROBLEMA!)'}`);
        
        if (hasPlayerId) {
            console.log('\n🚨 PROBLEMA IDENTIFICADO:');
            console.log('  A tabela discord_links tem player_id, mas o schema define apenas player_uuid!');
            console.log('  Isso pode causar inconsistências arquiteturais.');
        }
        
        // 5. Verificar foreign keys
        console.log('\n🔗 VERIFICANDO FOREIGN KEYS:');
        const [foreignKeys] = await pool.execute(`
            SELECT 
                TABLE_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY TABLE_NAME, COLUMN_NAME
        `);
        
        foreignKeys.forEach(fk => {
            console.log(`  ${fk.TABLE_NAME}.${fk.COLUMN_NAME} → ${fk.REFERENCED_TABLE_NAME}.${fk.REFERENCED_COLUMN_NAME}`);
        });
        
        // 6. Verificar dados críticos
        console.log('\n📊 VERIFICANDO DADOS CRÍTICOS:');
        
        const [consolePlayer] = await pool.execute('SELECT * FROM player_data WHERE player_id = 0 OR name = "CONSOLE"');
        console.log(`  CONSOLE player: ${consolePlayer.length > 0 ? '✅ Existe' : '❌ Não existe'}`);
        
        const [discordLinksCount] = await pool.execute('SELECT COUNT(*) as count FROM discord_links');
        console.log(`  discord_links registros: ${discordLinksCount[0].count}`);
        
        const [discordUsersCount] = await pool.execute('SELECT COUNT(*) as count FROM discord_users');
        console.log(`  discord_users registros: ${discordUsersCount[0].count}`);
        
        // 7. Verificar procedures
        console.log('\n⚙️  VERIFICANDO STORED PROCEDURES:');
        const [procedures] = await pool.execute(`
            SELECT ROUTINE_NAME 
            FROM information_schema.ROUTINES 
            WHERE ROUTINE_SCHEMA = 'primeleague' 
            AND ROUTINE_TYPE = 'PROCEDURE'
        `);
        
        procedures.forEach(proc => {
            console.log(`  ✅ ${proc.ROUTINE_NAME}`);
        });
        
        // 8. Verificar views
        console.log('\n👁️  VERIFICANDO VIEWS:');
        const [views] = await pool.execute(`
            SELECT TABLE_NAME 
            FROM information_schema.VIEWS 
            WHERE TABLE_SCHEMA = 'primeleague'
        `);
        
        views.forEach(view => {
            console.log(`  ✅ ${view.TABLE_NAME}`);
        });
        
        // 9. Verificar triggers
        console.log('\n⚡ VERIFICANDO TRIGGERS:');
        const [triggers] = await pool.execute(`
            SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE
            FROM information_schema.TRIGGERS 
            WHERE TRIGGER_SCHEMA = 'primeleague'
        `);
        
        triggers.forEach(trigger => {
            console.log(`  ✅ ${trigger.TRIGGER_NAME} (${trigger.EVENT_MANIPULATION} on ${trigger.EVENT_OBJECT_TABLE})`);
        });
        
        console.log('\n=====================================');
        console.log('🔍 VERIFICAÇÃO COMPLETA FINALIZADA');
        
        if (hasPlayerId) {
            console.log('\n🚨 RECOMENDAÇÃO:');
            console.log('  O banco de dados tem inconsistências com o schema.');
            console.log('  Considere recriar o banco usando o SCHEMA-FINAL-AUTOMATIZADO.sql');
        } else {
            console.log('\n✅ STATUS: Schema parece estar correto!');
        }
        
    } catch (error) {
        console.error('❌ Erro durante verificação:', error.message);
        throw error;
    } finally {
        await pool.end();
    }
}

verifyCompleteSchema()
    .then(() => {
        console.log('\n🎉 Verificação concluída!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Erro fatal:', error);
        process.exit(1);
    });
