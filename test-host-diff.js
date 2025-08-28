const mysql = require('mysql2/promise');

async function testHostDifference() {
    console.log('🔍 TESTE DE DIFERENÇA ENTRE HOSTS');
    console.log('==================================');
    
    const configs = [
        { name: 'localhost', host: 'localhost' },
        { name: '127.0.0.1', host: '127.0.0.1' }
    ];
    
    for (const config of configs) {
        try {
            console.log(`\n📡 Testando ${config.name}...`);
            
            const dbConfig = {
                host: config.host,
                port: 3306,
                user: 'root',
                password: 'root',
                database: 'primeleague'
            };
            
            const connection = await mysql.createConnection(dbConfig);
            console.log(`✅ Conectado via ${config.name}`);
            
            const [players] = await connection.execute(
                'SELECT player_id, uuid, name FROM player_data ORDER BY player_id'
            );
            
            console.log(`📋 Total de players via ${config.name}: ${players.length}`);
            players.forEach(player => {
                console.log(`   ID: ${player.player_id} | Name: ${player.name}`);
            });
            
            await connection.end();
            
        } catch (error) {
            console.error(`❌ Erro com ${config.name}:`, error.message);
        }
    }
}

testHostDifference();
