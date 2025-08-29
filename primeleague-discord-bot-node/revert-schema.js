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

async function revertSchema() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔄 Revertendo schema para o original correto...\n');
        
        // 1. Verificar se a coluna player_id existe
        const [columns] = await pool.execute(`
            SELECT COLUMN_NAME 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND COLUMN_NAME = 'player_id'
        `);
        
        if (columns.length > 0) {
            console.log('📝 Removendo coluna player_id incorreta...');
            
            // 2. Remover constraint de FK se existir
            try {
                await pool.execute(`
                    ALTER TABLE discord_links 
                    DROP FOREIGN KEY fk_discord_links_player_id
                `);
                console.log('✅ Constraint FK removida');
            } catch (error) {
                console.log('ℹ️ Constraint FK não existia');
            }
            
            // 3. Remover índice se existir
            try {
                await pool.execute(`
                    ALTER TABLE discord_links 
                    DROP INDEX idx_player_id
                `);
                console.log('✅ Índice removido');
            } catch (error) {
                console.log('ℹ️ Índice não existia');
            }
            
            // 4. Remover coluna player_id
            await pool.execute(`
                ALTER TABLE discord_links 
                DROP COLUMN player_id
            `);
            
            console.log('✅ Coluna player_id removida');
        } else {
            console.log('✅ Schema já está correto (sem player_id)');
        }
        
        // 5. Verificar se a constraint FK original existe
        const [constraints] = await pool.execute(`
            SELECT CONSTRAINT_NAME 
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND REFERENCED_TABLE_NAME = 'player_data'
            AND REFERENCED_COLUMN_NAME = 'uuid'
        `);
        
        if (constraints.length === 0) {
            console.log('📝 Adicionando constraint FK original...');
            await pool.execute(`
                ALTER TABLE discord_links 
                ADD CONSTRAINT fk_discord_links_player 
                FOREIGN KEY (player_uuid) REFERENCES player_data(uuid) 
                ON DELETE CASCADE ON UPDATE CASCADE
            `);
            console.log('✅ Constraint FK original adicionada');
        } else {
            console.log('✅ Constraint FK original já existe');
        }
        
        // 6. Verificar estrutura final
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
        
        console.log('\n✅ Schema revertido para o original correto!');
        console.log('📋 Agora o código deve usar player_uuid em vez de player_id');
        
    } catch (error) {
        console.error('❌ Erro ao reverter schema:', error);
    } finally {
        await pool.end();
    }
}

revertSchema();
