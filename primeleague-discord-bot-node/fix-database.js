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

async function fixDatabaseSchema() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Corrigindo estrutura do banco de dados...\n');
        
        // 1. Verificar se a coluna player_id já existe
        const [columns] = await pool.execute(`
            SELECT COLUMN_NAME 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND COLUMN_NAME = 'player_id'
        `);
        
        if (columns.length > 0) {
            console.log('✅ Coluna player_id já existe na tabela discord_links');
        } else {
            console.log('📝 Adicionando coluna player_id...');
            
            // 2. Adicionar coluna player_id
            await pool.execute(`
                ALTER TABLE discord_links 
                ADD COLUMN player_id INT(11) AFTER discord_id
            `);
            
            console.log('✅ Coluna player_id adicionada');
        }
        
        // 3. Verificar se o índice já existe
        const [indexes] = await pool.execute(`
            SELECT INDEX_NAME 
            FROM INFORMATION_SCHEMA.STATISTICS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND INDEX_NAME = 'idx_player_id'
        `);
        
        if (indexes.length > 0) {
            console.log('✅ Índice idx_player_id já existe');
        } else {
            console.log('📝 Adicionando índice...');
            
            // 4. Adicionar índice
            await pool.execute(`
                ALTER TABLE discord_links 
                ADD INDEX idx_player_id (player_id)
            `);
            
            console.log('✅ Índice adicionado');
        }
        
        // 5. Preencher dados existentes
        console.log('📝 Preenchendo dados existentes...');
        const [result] = await pool.execute(`
            UPDATE discord_links dl 
            JOIN player_data pd ON dl.player_uuid = pd.uuid 
            SET dl.player_id = pd.player_id 
            WHERE dl.player_id IS NULL
        `);
        
        console.log(`✅ ${result.affectedRows} registros atualizados`);
        
        // 6. Tornar a coluna NOT NULL
        console.log('📝 Tornando coluna NOT NULL...');
        await pool.execute(`
            ALTER TABLE discord_links 
            MODIFY COLUMN player_id INT(11) NOT NULL
        `);
        
        console.log('✅ Coluna tornada NOT NULL');
        
        // 7. Verificar estrutura final
        console.log('\n📊 Estrutura final da tabela discord_links:');
        const [finalColumns] = await pool.execute(`
            SELECT 
                COLUMN_NAME,
                DATA_TYPE,
                IS_NULLABLE,
                COLUMN_KEY
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links'
            ORDER BY ORDINAL_POSITION
        `);
        
        console.table(finalColumns);
        
        console.log('\n✅ Correção da estrutura do banco concluída com sucesso!');
        
    } catch (error) {
        console.error('❌ Erro ao corrigir estrutura:', error);
    } finally {
        await pool.end();
    }
}

fixDatabaseSchema();
