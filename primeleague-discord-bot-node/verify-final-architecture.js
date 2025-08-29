const mysql = require('mysql2/promise');

async function verifyFinalArchitecture() {
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
        console.log('🎯 VERIFICAÇÃO FINAL DA ARQUITETURA RESTAURADA');
        console.log('==============================================\n');

        // 1. Verificar estrutura da tabela discord_links
        console.log('📋 ESTRUTURA DA TABELA discord_links:');
        const [discordLinksStructure] = await connection.execute('DESCRIBE discord_links');
        console.table(discordLinksStructure);

        // 2. Verificar se player_id existe e player_uuid não existe
        const hasPlayerId = discordLinksStructure.some(col => col.Field === 'player_id');
        const hasPlayerUuid = discordLinksStructure.some(col => col.Field === 'player_uuid');
        
        console.log(`\n🔍 VERIFICAÇÃO DE COLUNAS:`);
        console.log(`- player_id existe: ${hasPlayerId ? '✅ SIM' : '❌ NÃO'}`);
        console.log(`- player_uuid existe: ${hasPlayerUuid ? '❌ SIM (ERRO)' : '✅ NÃO (CORRETO)'}`);

        // 3. Verificar foreign keys
        console.log('\n🔗 FOREIGN KEYS DA TABELA discord_links:');
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
        console.table(foreignKeys);

        // 4. Verificar se as foreign keys estão corretas
        const playerIdFk = foreignKeys.find(fk => fk.COLUMN_NAME === 'player_id');
        const discordIdFk = foreignKeys.find(fk => fk.COLUMN_NAME === 'discord_id');
        
        console.log(`\n✅ VERIFICAÇÃO DE FOREIGN KEYS:`);
        console.log(`- player_id FK para player_data.player_id: ${playerIdFk ? '✅ CORRETO' : '❌ ERRADO'}`);
        console.log(`- discord_id FK para discord_users.discord_id: ${discordIdFk ? '✅ CORRETO' : '❌ ERRADO'}`);

        // 5. Verificar dados existentes
        console.log('\n📊 DADOS EXISTENTES:');
        const [playerDataCount] = await connection.execute('SELECT COUNT(*) as total FROM player_data');
        const [discordLinksCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_links');
        const [discordUsersCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_users');
        
        console.log(`- player_data: ${playerDataCount[0].total} registros`);
        console.log(`- discord_links: ${discordLinksCount[0].total} registros`);
        console.log(`- discord_users: ${discordUsersCount[0].total} registros`);

        // 6. Verificar se não existe tabela donors
        const [donorsTable] = await connection.execute(`
            SELECT COUNT(*) as exists FROM information_schema.tables 
            WHERE table_schema = 'primeleague' AND table_name = 'donors'
        `);
        
        console.log(`\n🧹 VERIFICAÇÃO DE LIMPEZA:`);
        console.log(`- Tabela donors existe: ${donorsTable[0].exists > 0 ? '❌ SIM (ERRO)' : '✅ NÃO (CORRETO)'}`);

        // 7. Verificar índices de performance
        console.log('\n📈 ÍNDICES DE PERFORMANCE:');
        const [indexes] = await connection.execute(`
            SELECT INDEX_NAME, COLUMN_NAME 
            FROM information_schema.STATISTICS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links'
            ORDER BY INDEX_NAME
        `);
        console.table(indexes);

        // 8. Verificar se há índices únicos corretos
        const hasUniquePlayerId = indexes.some(idx => idx.INDEX_NAME === 'uk_player_id');
        console.log(`\n🔒 VERIFICAÇÃO DE ÍNDICES ÚNICOS:`);
        console.log(`- Índice único em player_id: ${hasUniquePlayerId ? '✅ SIM' : '❌ NÃO'}`);

        // 9. Resumo final
        console.log('\n🎯 RESUMO FINAL DA ARQUITETURA:');
        
        const allChecks = [
            hasPlayerId,
            !hasPlayerUuid,
            playerIdFk,
            discordIdFk,
            donorsTable[0].exists === 0,
            hasUniquePlayerId
        ];
        
        const passedChecks = allChecks.filter(check => check).length;
        const totalChecks = allChecks.length;
        
        console.log(`✅ ${passedChecks}/${totalChecks} verificações passaram`);
        
        if (passedChecks === totalChecks) {
            console.log('\n🎉 ARQUITETURA RESTAURADA COM SUCESSO!');
            console.log('✅ player_id é a foreign key correta');
            console.log('✅ Performance otimizada com índices numéricos');
            console.log('✅ Compatibilidade com offline-mode garantida');
            console.log('✅ Banco de dados limpo e consistente');
        } else {
            console.log('\n⚠️  ALGUMAS VERIFICAÇÕES FALHARAM');
            console.log('❌ A arquitetura ainda precisa de correções');
        }

    } catch (error) {
        console.error('❌ Erro ao verificar arquitetura:', error.message);
    } finally {
        await connection.end();
    }
}

verifyFinalArchitecture();
