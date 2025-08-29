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

async function debugSchema() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 Verificando estrutura das tabelas...\n');
        
        // 1. Verificar estrutura da tabela player_data
        console.log('📊 Estrutura da tabela player_data:');
        const [playerColumns] = await pool.execute('DESCRIBE player_data');
        console.log(playerColumns);
        
        // 2. Verificar estrutura da tabela discord_links
        console.log('\n🔗 Estrutura da tabela discord_links:');
        const [linkColumns] = await pool.execute('DESCRIBE discord_links');
        console.log(linkColumns);
        
        // 3. Verificar estrutura da tabela discord_users
        console.log('\n👤 Estrutura da tabela discord_users:');
        const [userColumns] = await pool.execute('DESCRIBE discord_users');
        console.log(userColumns);
        
        // 4. Verificar dados da tabela player_data
        console.log('\n📊 Dados da tabela player_data:');
        const [playerData] = await pool.execute('SELECT * FROM player_data LIMIT 5');
        console.log(playerData);
        
        // 5. Verificar dados da tabela discord_links
        console.log('\n🔗 Dados da tabela discord_links:');
        const [linkData] = await pool.execute('SELECT * FROM discord_links LIMIT 5');
        console.log(linkData);
        
        // 6. Verificar dados da tabela discord_users
        console.log('\n👤 Dados da tabela discord_users:');
        const [userData] = await pool.execute('SELECT * FROM discord_users LIMIT 5');
        console.log(userData);
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

debugSchema();
