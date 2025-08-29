const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');

async function restorePlayerIdArchitecture() {
    const connection = await mysql.createConnection({
        host: 'localhost',
        port: 3306,
        user: 'root',
        password: 'root',
        database: 'primeleague',
        authPlugins: {
            mysql_native_password: () => () => Buffer.from('root', 'utf-8')
        }
    });

    try {
        console.log('🔄 RESTAURANDO ARQUITETURA CORRETA COM player_id');
        console.log('===============================================\n');

        // 1. Verificar estado atual
        console.log('📋 VERIFICANDO ESTADO ATUAL...');
        const [currentStructure] = await connection.execute('DESCRIBE discord_links');
        const hasPlayerId = currentStructure.some(col => col.Field === 'player_id');
        
        if (hasPlayerId) {
            console.log('✅ player_id já existe em discord_links');
            return;
        }

        console.log('⚠️  player_id NÃO existe - restaurando arquitetura correta...\n');

        // 2. Fazer backup dos dados existentes
        console.log('💾 FAZENDO BACKUP DOS DADOS EXISTENTES...');
        const [existingLinks] = await connection.execute(`
            SELECT dl.*, pd.player_id 
            FROM discord_links dl 
            JOIN player_data pd ON dl.player_uuid = pd.uuid
        `);
        
        console.log(`- ${existingLinks.length} vínculos encontrados para backup`);

        // 3. Dropar foreign key constraints existentes
        console.log('\n🔧 REMOVENDO CONSTRAINTS EXISTENTES...');
        await connection.execute('ALTER TABLE discord_links DROP FOREIGN KEY fk_discord_links_player');
        await connection.execute('ALTER TABLE discord_links DROP FOREIGN KEY fk_discord_links_user');

        // 4. Adicionar coluna player_id
        console.log('➕ ADICIONANDO COLUNA player_id...');
        await connection.execute('ALTER TABLE discord_links ADD COLUMN player_id INT NOT NULL AFTER discord_id');

        // 5. Preencher player_id com dados corretos
        console.log('🔄 PREENCHENDO player_id...');
        for (const link of existingLinks) {
            await connection.execute(
                'UPDATE discord_links SET player_id = ? WHERE link_id = ?',
                [link.player_id, link.link_id]
            );
        }

        // 6. Remover coluna player_uuid
        console.log('🗑️  REMOVENDO COLUNA player_uuid...');
        await connection.execute('ALTER TABLE discord_links DROP COLUMN player_uuid');

        // 7. Recriar foreign key constraints corretas
        console.log('🔗 RECRIANDO FOREIGN KEYS CORRETAS...');
        await connection.execute(`
            ALTER TABLE discord_links 
            ADD CONSTRAINT fk_discord_links_player 
            FOREIGN KEY (player_id) REFERENCES player_data(player_id) ON DELETE CASCADE ON UPDATE CASCADE
        `);
        
        await connection.execute(`
            ALTER TABLE discord_links 
            ADD CONSTRAINT fk_discord_links_user 
            FOREIGN KEY (discord_id) REFERENCES discord_users(discord_id) ON DELETE CASCADE ON UPDATE CASCADE
        `);

        // 8. Adicionar índices para performance
        console.log('📈 ADICIONANDO ÍNDICES DE PERFORMANCE...');
        await connection.execute('CREATE UNIQUE INDEX uk_player_id ON discord_links (player_id)');
        await connection.execute('CREATE INDEX idx_discord_links_player_id ON discord_links (player_id)');

        // 9. Verificar resultado
        console.log('\n✅ VERIFICANDO RESULTADO...');
        const [newStructure] = await connection.execute('DESCRIBE discord_links');
        console.table(newStructure);

        const [foreignKeys] = await connection.execute(`
            SELECT 
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `);
        console.log('\n🔗 FOREIGN KEYS FINAIS:');
        console.table(foreignKeys);

        console.log('\n🎉 ARQUITETURA RESTAURADA COM SUCESSO!');
        console.log('✅ discord_links agora usa player_id como foreign key');
        console.log('✅ Performance otimizada com índices numéricos');
        console.log('✅ Compatibilidade com offline-mode garantida');

    } catch (error) {
        console.error('❌ Erro ao restaurar arquitetura:', error.message);
        throw error;
    } finally {
        await connection.end();
    }
}

restorePlayerIdArchitecture();
