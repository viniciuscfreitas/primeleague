const mysql = require('mysql2/promise');

async function testAccount() {
    const connection = await mysql.createConnection({
        host: 'localhost',
        port: 3306,
        user: 'root',
        password: 'root',
        database: 'primeleague'
    });

    try {
        console.log('Testando consulta de conta...\n');

        // Buscar vínculo Discord
        const [links] = await connection.execute(
            'SELECT * FROM discord_links WHERE player_name = ?',
            ['finicff']
        );
        console.log('Discord Link:', links[0]);

        if (links[0]) {
            // Buscar informações da conta
            const [accounts] = await connection.execute(
                `SELECT dl.player_uuid, dl.player_name, pd.elo, pd.money, pd.subscription_expires_at
                 FROM discord_links dl
                 JOIN player_data pd ON dl.player_uuid = pd.uuid
                 WHERE dl.discord_id = ?`,
                [links[0].discord_id]
            );
            console.log('\nConta:', accounts[0]);

            if (accounts[0]) {
                console.log('\nTipos de dados:');
                console.log('- elo:', typeof accounts[0].elo);
                console.log('- money:', typeof accounts[0].money);
                console.log('- subscription_expires_at:', typeof accounts[0].subscription_expires_at);
            }
        }

    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await connection.end();
    }
}

testAccount();
