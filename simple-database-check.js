const mysql = require('mysql2/promise');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function checkDatabase() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 VERIFICAÇÃO RÁPIDA DO BANCO ATUAL');
        console.log('=====================================\n');
        
        // 1. Listar todas as tabelas
        const [tables] = await pool.execute(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        console.log('📋 TABELAS EXISTENTES:');
        tables.forEach((table, index) => {
            console.log(`  ${index + 1}. ${table.table_name}`);
        });
        
        console.log(`\n📊 Total: ${tables.length} tabelas\n`);
        
        // 2. Verificar tabelas com dados
        console.log('📊 TABELAS COM DADOS:');
        for (const table of tables) {
            try {
                const [result] = await pool.execute(`SELECT COUNT(*) as count FROM \`${table.table_name}\``);
                const count = result[0].count;
                if (count > 0) {
                    console.log(`  ✅ ${table.table_name}: ${count} registros`);
                }
            } catch (error) {
                // Ignorar erros
            }
        }
        
        // 3. Verificar versão
        const [version] = await pool.execute('SELECT VERSION() as version');
        console.log(`\n🔧 Versão do servidor: ${version[0].version}`);
        
        // 4. Comparar com schema esperado
        console.log('\n🎯 COMPARAÇÃO COM SCHEMA ESPERADO:');
        const expectedTables = [
            'player_data', 'clans', 'clan_players', 'clan_alliances', 'clan_logs',
            'clan_event_wins', 'donors', 'economy_logs', 'discord_links', 'discord_users',
            'discord_link_history', 'player_authorized_ips', 'recovery_codes', 'whitelist_players',
            'punishments', 'tickets', 'staff_vanish', 'chat_logs', 'server_notifications',
            'permission_groups', 'group_permissions', 'player_groups', 'permission_logs'
        ];
        
        let foundTables = 0;
        expectedTables.forEach(expected => {
            const exists = tables.some(t => t.table_name === expected);
            if (exists) {
                foundTables++;
                console.log(`  ✅ ${expected}`);
            } else {
                console.log(`  ❌ ${expected} (FALTANDO)`);
            }
        });
        
        console.log(`\n📈 Cobertura: ${foundTables}/${expectedTables.length} (${((foundTables/expectedTables.length)*100).toFixed(1)}%)`);
        
    } catch (error) {
        console.error('❌ Erro:', error.message);
    } finally {
        await pool.end();
    }
}

checkDatabase();
