const mysql = require('mysql2/promise');

async function testConnection() {
    const connection = await mysql.createConnection({
        host: 'localhost',
        port: 3306,
        user: 'root',
        password: '',
        database: 'primeleague'
    });

    try {
        // Testar conexão
        console.log('Tentando conectar ao MySQL...');
        await connection.connect();
        console.log('✅ Conexão estabelecida!');

        // Testar query
        console.log('\nTestando query em player_data...');
        const [rows] = await connection.execute('SELECT * FROM player_data LIMIT 1');
        console.log('Resultado:', rows);

        // Testar query em discord_links
        console.log('\nTestando query em discord_links...');
        const [links] = await connection.execute('SELECT * FROM discord_links LIMIT 1');
        console.log('Resultado:', links);

    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await connection.end();
    }
}

testConnection();
