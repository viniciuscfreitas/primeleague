const mysql = require('mysql2/promise');

// Configurações do banco (mesmas do bot)
const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function testSchemaDiff() {
    let connection;
    try {
        console.log('🔍 TESTE DE DIFERENÇAS DE SCHEMA');
        console.log('================================');
        
        // Conectar ao banco
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado ao banco de dados');
        
        // 1. Verificar estrutura da tabela player_data
        console.log('\n1️⃣ Estrutura da tabela player_data:');
        const [playerDataColumns] = await connection.execute(
            "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'primeleague' AND TABLE_NAME = 'player_data' ORDER BY ORDINAL_POSITION"
        );
        
        playerDataColumns.forEach(col => {
            console.log(`   ${col.COLUMN_NAME}: ${col.DATA_TYPE} ${col.IS_NULLABLE === 'YES' ? 'NULL' : 'NOT NULL'} ${col.COLUMN_DEFAULT ? `DEFAULT ${col.COLUMN_DEFAULT}` : ''}`);
        });
        
        // 2. Verificar estrutura da tabela discord_links
        console.log('\n2️⃣ Estrutura da tabela discord_links:');
        const [discordLinksColumns] = await connection.execute(
            "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'primeleague' AND TABLE_NAME = 'discord_links' ORDER BY ORDINAL_POSITION"
        );
        
        discordLinksColumns.forEach(col => {
            console.log(`   ${col.COLUMN_NAME}: ${col.DATA_TYPE} ${col.IS_NULLABLE === 'YES' ? 'NULL' : 'NOT NULL'} ${col.COLUMN_DEFAULT ? `DEFAULT ${col.COLUMN_DEFAULT}` : ''}`);
        });
        
        // 3. Verificar se há foreign keys
        console.log('\n3️⃣ Foreign Keys:');
        const [foreignKeys] = await connection.execute(
            "SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'primeleague' AND REFERENCED_TABLE_NAME IS NOT NULL"
        );
        
        foreignKeys.forEach(fk => {
            console.log(`   ${fk.CONSTRAINT_NAME}: ${fk.TABLE_NAME}.${fk.COLUMN_NAME} -> ${fk.REFERENCED_TABLE_NAME}.${fk.REFERENCED_COLUMN_NAME}`);
        });
        
        // 4. Verificar dados específicos do testeuuidbolado
        console.log('\n4️⃣ Dados específicos do testeuuidbolado:');
        const [playerData] = await connection.execute(
            'SELECT * FROM player_data WHERE name = ?',
            ['testeuuidbolado']
        );
        
        if (playerData.length > 0) {
            console.log('   Player Data:', JSON.stringify(playerData[0], null, 2));
            
            // Verificar se há link Discord
            const [discordLink] = await connection.execute(
                'SELECT * FROM discord_links WHERE player_id = ?',
                [playerData[0].player_id]
            );
            
            if (discordLink.length > 0) {
                console.log('   Discord Link:', JSON.stringify(discordLink[0], null, 2));
            } else {
                console.log('   ❌ Nenhum link Discord encontrado');
            }
        } else {
            console.log('   ❌ Player não encontrado');
        }
        
        console.log('\n✅ Teste de schema concluído!');
        
    } catch (error) {
        console.error('❌ Erro no teste:', error);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

testSchemaDiff();
