const mysql = require('mysql2/promise');
require('dotenv').config();

// Configurações do banco
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

async function addPlayerIdToDiscordLinks() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Adicionando player_id à tabela discord_links...');
        
        // 1. Adicionar coluna player_id
        await pool.execute(`
            ALTER TABLE discord_links 
            ADD COLUMN player_id INT NULL AFTER discord_id
        `);
        console.log('✅ Coluna player_id adicionada');
        
        // 2. Preencher player_id baseado no player_uuid
        await pool.execute(`
            UPDATE discord_links dl
            JOIN player_data pd ON dl.player_uuid = pd.uuid
            SET dl.player_id = pd.player_id
        `);
        console.log('✅ player_id preenchido com base no player_uuid');
        
        // 3. Tornar player_id NOT NULL
        await pool.execute(`
            ALTER TABLE discord_links 
            MODIFY COLUMN player_id INT NOT NULL
        `);
        console.log('✅ player_id definido como NOT NULL');
        
        // 4. Adicionar índice para player_id
        await pool.execute(`
            ALTER TABLE discord_links 
            ADD INDEX idx_player_id (player_id)
        `);
        console.log('✅ Índice idx_player_id adicionado');
        
        // 5. Adicionar FK constraint
        await pool.execute(`
            ALTER TABLE discord_links 
            ADD CONSTRAINT fk_discord_links_player_id 
            FOREIGN KEY (player_id) REFERENCES player_data(player_id) 
            ON DELETE CASCADE ON UPDATE CASCADE
        `);
        console.log('✅ FK constraint adicionada');
        
        // 6. Verificar resultado
        const [rows] = await pool.execute('DESCRIBE discord_links');
        console.log('\n📊 Estrutura final da tabela discord_links:');
        rows.forEach(row => {
            console.log(`  ${row.Field}: ${row.Type} ${row.Null} ${row.Key} ${row.Extra}`);
        });
        
        console.log('\n✅ Correção arquitetural concluída com sucesso!');
        console.log('🎯 Agora discord_links usa player_id como FK (arquitetura correta)');
        
    } catch (error) {
        console.error('❌ Erro durante correção:', error.message);
        throw error;
    } finally {
        await pool.end();
    }
}

// Executar correção
addPlayerIdToDiscordLinks()
    .then(() => {
        console.log('🎉 Script executado com sucesso!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Erro fatal:', error);
        process.exit(1);
    });
