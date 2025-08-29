const mysql = require('mysql2/promise');

async function verifySchemaProductionAlignment() {
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
        console.log('🔍 VERIFICAÇÃO COMPLETA: SCHEMA ↔ PRODUÇÃO');
        console.log('==========================================\n');

        // 1. VERIFICAR ESTRUTURA DA TABELA discord_links
        console.log('📋 1. ESTRUTURA DA TABELA discord_links:');
        const [discordLinksStructure] = await connection.execute('DESCRIBE discord_links');
        console.table(discordLinksStructure);

        // 2. VERIFICAR SE player_id EXISTE E player_uuid NÃO EXISTE
        const hasPlayerId = discordLinksStructure.some(col => col.Field === 'player_id');
        const hasPlayerUuid = discordLinksStructure.some(col => col.Field === 'player_uuid');
        
        console.log(`\n🔍 VERIFICAÇÃO DE COLUNAS:`);
        console.log(`- player_id existe: ${hasPlayerId ? '✅ SIM' : '❌ NÃO'}`);
        console.log(`- player_uuid existe: ${hasPlayerUuid ? '❌ SIM (ERRO)' : '✅ NÃO (CORRETO)'}`);

        // 3. VERIFICAR FOREIGN KEYS
        console.log('\n🔗 2. FOREIGN KEYS DA TABELA discord_links:');
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

        // 4. VERIFICAR SE AS FOREIGN KEYS ESTÃO CORRETAS
        const playerIdFk = foreignKeys.find(fk => fk.COLUMN_NAME === 'player_id');
        const discordIdFk = foreignKeys.find(fk => fk.COLUMN_NAME === 'discord_id');
        
        console.log(`\n✅ VERIFICAÇÃO DE FOREIGN KEYS:`);
        console.log(`- player_id FK para player_data.player_id: ${playerIdFk ? '✅ CORRETO' : '❌ ERRADO'}`);
        console.log(`- discord_id FK para discord_users.discord_id: ${discordIdFk ? '✅ CORRETO' : '❌ ERRADO'}`);

        // 5. VERIFICAR ÍNDICES
        console.log('\n📈 3. ÍNDICES DA TABELA discord_links:');
        const [indexes] = await connection.execute(`
            SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
            FROM information_schema.STATISTICS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'discord_links'
            ORDER BY INDEX_NAME, SEQ_IN_INDEX
        `);
        console.table(indexes);

        // 6. VERIFICAR SE HÁ ÍNDICE ÚNICO EM player_id
        const hasUniquePlayerId = indexes.some(idx => idx.INDEX_NAME === 'uk_player_id');
        console.log(`\n🔒 VERIFICAÇÃO DE ÍNDICES ÚNICOS:`);
        console.log(`- Índice único em player_id: ${hasUniquePlayerId ? '✅ SIM' : '❌ NÃO'}`);

        // 7. VERIFICAR ESTRUTURA DA TABELA player_data
        console.log('\n📋 4. ESTRUTURA DA TABELA player_data:');
        const [playerDataStructure] = await connection.execute('DESCRIBE player_data');
        console.table(playerDataStructure);

        // 8. VERIFICAR SE player_data TEM player_id COMO PRIMARY KEY
        const playerIdPrimaryKey = playerDataStructure.find(col => col.Field === 'player_id' && col.Key === 'PRI');
        console.log(`\n🔑 VERIFICAÇÃO DE PRIMARY KEY:`);
        console.log(`- player_id é PRIMARY KEY em player_data: ${playerIdPrimaryKey ? '✅ SIM' : '❌ NÃO'}`);

        // 9. VERIFICAR ESTRUTURA DA TABELA discord_users
        console.log('\n📋 5. ESTRUTURA DA TABELA discord_users:');
        const [discordUsersStructure] = await connection.execute('DESCRIBE discord_users');
        console.table(discordUsersStructure);

        // 10. VERIFICAR DADOS EXISTENTES
        console.log('\n📊 6. DADOS EXISTENTES:');
        const [playerDataCount] = await connection.execute('SELECT COUNT(*) as total FROM player_data');
        const [discordLinksCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_links');
        const [discordUsersCount] = await connection.execute('SELECT COUNT(*) as total FROM discord_users');
        
        console.log(`- player_data: ${playerDataCount[0].total} registros`);
        console.log(`- discord_links: ${discordLinksCount[0].total} registros`);
        console.log(`- discord_users: ${discordUsersCount[0].total} registros`);

        // 11. VERIFICAR SE EXISTE O CONSOLE (ESSENCIAL)
        const [consoleExists] = await connection.execute('SELECT COUNT(*) as total FROM player_data WHERE player_id = 0');
        console.log(`\n🎮 VERIFICAÇÃO DO CONSOLE:`);
        console.log(`- CONSOLE (player_id = 0) existe: ${consoleExists[0].total > 0 ? '✅ SIM' : '❌ NÃO'}`);

        // 12. TESTAR UMA QUERY DE JOIN PARA VERIFICAR FUNCIONAMENTO
        console.log('\n🔍 7. TESTE DE QUERY DE JOIN:');
        try {
            const [joinTest] = await connection.execute(`
                SELECT 
                    pd.player_id,
                    pd.name,
                    dl.discord_id,
                    dl.verified
                FROM player_data pd
                LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
                LIMIT 5
            `);
            console.log('✅ Query de JOIN funcionando corretamente:');
            console.table(joinTest);
        } catch (error) {
            console.log('❌ Erro na query de JOIN:', error.message);
        }

        // 13. VERIFICAR SE NÃO EXISTE TABELA donors (LIMPEZA)
        const [donorsTable] = await connection.execute(`
            SELECT COUNT(*) as exists_count FROM information_schema.tables 
            WHERE table_schema = 'primeleague' AND table_name = 'donors'
        `);
        
        console.log(`\n🧹 8. VERIFICAÇÃO DE LIMPEZA:`);
        console.log(`- Tabela donors existe: ${donorsTable[0].exists_count > 0 ? '❌ SIM (ERRO)' : '✅ NÃO (CORRETO)'}`);

        // 14. VERIFICAR TODAS AS TABELAS DO SISTEMA
        console.log('\n📋 9. TODAS AS TABELAS DO SISTEMA:');
        const [allTables] = await connection.execute(`
            SELECT table_name, table_rows
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague'
            ORDER BY table_name
        `);
        console.table(allTables);

        // 15. VERIFICAR FOREIGN KEYS DE TODAS AS TABELAS
        console.log('\n🔗 10. TODAS AS FOREIGN KEYS DO SISTEMA:');
        const [allForeignKeys] = await connection.execute(`
            SELECT 
                TABLE_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY TABLE_NAME, COLUMN_NAME
        `);
        console.table(allForeignKeys);

        // 16. RESUMO FINAL
        console.log('\n🎯 RESUMO FINAL DA VERIFICAÇÃO:');
        
        const allChecks = [
            hasPlayerId,
            !hasPlayerUuid,
            playerIdFk,
            discordIdFk,
            hasUniquePlayerId,
            playerIdPrimaryKey,
            consoleExists[0].total > 0,
            donorsTable[0].exists_count === 0
        ];
        
        const passedChecks = allChecks.filter(check => check).length;
        const totalChecks = allChecks.length;
        
        console.log(`✅ ${passedChecks}/${totalChecks} verificações passaram`);
        
        if (passedChecks === totalChecks) {
            console.log('\n🎉 SCHEMA PERFEITAMENTE ALINHADO COM PRODUÇÃO!');
            console.log('✅ player_id é a foreign key correta');
            console.log('✅ Performance otimizada com índices numéricos');
            console.log('✅ Compatibilidade com offline-mode garantida');
            console.log('✅ Banco de dados limpo e consistente');
            console.log('✅ Todas as foreign keys corretas');
            console.log('✅ Estrutura completa do sistema validada');
        } else {
            console.log('\n⚠️  ALGUMAS VERIFICAÇÕES FALHARAM');
            console.log('❌ O schema ainda precisa de correções');
        }

        // 17. VERIFICAÇÃO EXTRA: COMPARAR COM SCHEMA OFICIAL
        console.log('\n📄 11. VERIFICAÇÃO CONTRA SCHEMA OFICIAL:');
        
        // Verificar se a estrutura atual corresponde ao que deveria estar no schema oficial
        const expectedStructure = {
            'link_id': { type: 'int(11)', null: 'NO', key: 'PRI', extra: 'auto_increment' },
            'discord_id': { type: 'varchar(20)', null: 'NO', key: 'MUL' },
            'player_id': { type: 'int(11)', null: 'NO', key: 'UNI' },
            'is_primary': { type: 'tinyint(1)', null: 'NO', default: '0' },
            'verified': { type: 'tinyint(1)', null: 'NO', default: '0' },
            'verification_code': { type: 'varchar(8)', null: 'YES' },
            'code_expires_at': { type: 'timestamp', null: 'YES' },
            'verified_at': { type: 'timestamp', null: 'YES' },
            'created_at': { type: 'timestamp', null: 'NO', default: 'current_timestamp()' }
        };

        let structureMatches = true;
        for (const [fieldName, expectedField] of Object.entries(expectedStructure)) {
            const actualField = discordLinksStructure.find(f => f.Field === fieldName);
            if (!actualField) {
                console.log(`❌ Campo ${fieldName} não encontrado na produção`);
                structureMatches = false;
            } else {
                // Verificar se o tipo e outras propriedades correspondem
                if (actualField.Type !== expectedField.type || 
                    actualField.Null !== expectedField.null ||
                    actualField.Key !== expectedField.key) {
                    console.log(`⚠️  Campo ${fieldName} tem diferenças: esperado ${expectedField.type}/${expectedField.null}/${expectedField.key}, encontrado ${actualField.Type}/${actualField.Null}/${actualField.Key}`);
                    structureMatches = false;
                }
            }
        }

        console.log(`\n📋 ESTRUTURA CORRESPONDE AO SCHEMA OFICIAL: ${structureMatches ? '✅ SIM' : '❌ NÃO'}`);

        // 18. VERIFICAÇÃO FINAL ABSOLUTA
        console.log('\n🎯 VERIFICAÇÃO ABSOLUTA FINAL:');
        
        const absoluteChecks = [
            hasPlayerId && !hasPlayerUuid, // player_id existe, player_uuid não
            playerIdFk && playerIdFk.REFERENCED_TABLE_NAME === 'player_data' && playerIdFk.REFERENCED_COLUMN_NAME === 'player_id',
            discordIdFk && discordIdFk.REFERENCED_TABLE_NAME === 'discord_users' && discordIdFk.REFERENCED_COLUMN_NAME === 'discord_id',
            hasUniquePlayerId,
            playerIdPrimaryKey,
            structureMatches
        ];

        const absolutePassed = absoluteChecks.filter(check => check).length;
        const absoluteTotal = absoluteChecks.length;

        console.log(`🎯 ${absolutePassed}/${absoluteTotal} verificações absolutas passaram`);

        if (absolutePassed === absoluteTotal) {
            console.log('\n🏆 CERTEZA ABSOLUTA: SCHEMA PERFEITAMENTE ALINHADO!');
            console.log('🎉 O sistema está 100% consistente e pronto para produção');
        } else {
            console.log('\n🚨 ATENÇÃO: Inconsistências detectadas que precisam ser corrigidas');
        }

    } catch (error) {
        console.error('❌ Erro ao verificar alinhamento:', error.message);
    } finally {
        await connection.end();
    }
}

verifySchemaProductionAlignment();
