require('dotenv').config();
const mysql = require('mysql2/promise');

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

async function debugViniAccount() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 Verificando conta "vini"...\n');
        
        // 1. Verificar se o player existe
        const [playerRows] = await pool.execute(
            'SELECT * FROM player_data WHERE name = ?',
            ['vini']
        );
        
        console.log('📊 Player Data:');
        console.log(playerRows);
        
        if (playerRows.length > 0) {
            const playerId = playerRows[0].player_id;
            
            // 2. Verificar discord_links
            const [linkRows] = await pool.execute(
                'SELECT * FROM discord_links WHERE player_id = ?',
                [playerId]
            );
            
            console.log('\n🔗 Discord Links:');
            console.log(linkRows);
            
            // 3. Verificar se há vínculos verificados
            const [verifiedRows] = await pool.execute(
                'SELECT * FROM discord_links WHERE player_id = ? AND verified = TRUE',
                [playerId]
            );
            
            console.log('\n✅ Vínculos Verificados:');
            console.log(verifiedRows);
            
            // 4. Verificar discord_users se houver discord_id
            if (linkRows.length > 0 && linkRows[0].discord_id) {
                const [userRows] = await pool.execute(
                    'SELECT * FROM discord_users WHERE discord_id = ?',
                    [linkRows[0].discord_id]
                );
                
                console.log('\n👤 Discord Users:');
                console.log(userRows);
            }
        }
        
        // 5. Verificar todas as contas vinculadas
        const [allLinks] = await pool.execute(`
            SELECT pd.name, dl.discord_id, dl.verified, dl.verified_at
            FROM discord_links dl
            JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE pd.name LIKE '%vini%'
        `);
        
        console.log('\n🔍 Todas as contas com "vini":');
        console.log(allLinks);
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

debugViniAccount();
