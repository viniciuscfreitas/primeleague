console.log('🔍 TESTE SIMPLES DE BANCO DE DADOS');

const mysql = require('mysql2/promise');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function test() {
    try {
        console.log('Conectando...');
        const connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado!');
        
        const [rows] = await connection.execute('SELECT COUNT(*) as total FROM player_data');
        console.log('Total de players:', rows[0].total);
        
        const [player] = await connection.execute('SELECT * FROM player_data WHERE name = ?', ['testeuuidbolado']);
        if (player.length > 0) {
            console.log('✅ Player encontrado:', player[0]);
        } else {
            console.log('❌ Player não encontrado');
        }
        
        await connection.end();
    } catch (error) {
        console.error('Erro:', error.message);
    }
}

test();
