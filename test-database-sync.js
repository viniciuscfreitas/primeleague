const mysql = require('mysql2/promise');

// Configurações do banco (mesmas do bot)
const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function testDatabaseSync() {
    let connection;
    try {
        console.log('🔍 TESTE DE SINCRONIZAÇÃO DO BANCO DE DADOS');
        console.log('==========================================');
        
        // Conectar ao banco
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado ao banco de dados');
        
        // 1. Verificar se o player testeuuidbolado existe
        console.log('\n1️⃣ Verificando player "testeuuidbolado"...');
        const [rows] = await connection.execute(
            'SELECT player_id, uuid, name, status FROM player_data WHERE name = ?',
            ['testeuuidbolado']
        );
        
        if (rows.length > 0) {
            console.log('✅ Player encontrado:', rows[0]);
        } else {
            console.log('❌ Player NÃO encontrado');
        }
        
        // 2. Listar todos os players
        console.log('\n2️⃣ Listando todos os players...');
        const [allPlayers] = await connection.execute(
            'SELECT player_id, uuid, name, status FROM player_data ORDER BY player_id'
        );
        
        console.log(`📋 Total de players: ${allPlayers.length}`);
        allPlayers.forEach(player => {
            console.log(`   ID: ${player.player_id} | UUID: ${player.uuid} | Name: ${player.name} | Status: ${player.status}`);
        });
        
        // 3. Verificar discord_links
        console.log('\n3️⃣ Verificando discord_links...');
        const [links] = await connection.execute(
            'SELECT link_id, discord_id, player_id, verified FROM discord_links ORDER BY link_id'
        );
        
        console.log(`📋 Total de links: ${links.length}`);
        links.forEach(link => {
            console.log(`   Link ID: ${link.link_id} | Discord: ${link.discord_id} | Player ID: ${link.player_id} | Verified: ${link.verified}`);
        });
        
        // 4. Verificar discord_users
        console.log('\n4️⃣ Verificando discord_users...');
        const [users] = await connection.execute(
            'SELECT discord_id, donor_tier, subscription_type FROM discord_users ORDER BY discord_id'
        );
        
        console.log(`📋 Total de usuários Discord: ${users.length}`);
        users.forEach(user => {
            console.log(`   Discord ID: ${user.discord_id} | Tier: ${user.donor_tier} | Type: ${user.subscription_type}`);
        });
        
        console.log('\n✅ Teste concluído!');
        
    } catch (error) {
        console.error('❌ Erro no teste:', error);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

testDatabaseSync();
