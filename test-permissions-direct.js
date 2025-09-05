const mysql = require('mysql2/promise');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

async function testPermissionsDirect() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 TESTANDO SISTEMA DE PERMISSÕES DIRETAMENTE...\n');
        
        // Simular o que o Java está fazendo
        const bukkitUuid = '2c048974-fd73-0b58-bd88-88bf73df16e7';
        const canonicalUuid = 'b2d67524-ac9a-31a0-80c7-7acd45619820';
        
        console.log(`🔍 Testando com UUID do Bukkit: ${bukkitUuid}`);
        console.log(`🔍 UUID Canônico esperado: ${canonicalUuid}\n`);
        
        // 1. Testar busca direta por UUID do Bukkit (o que está falhando)
        console.log('1️⃣ TESTE: Busca por UUID do Bukkit (deve falhar)');
        const [bukkitResult] = await pool.execute(`
            SELECT player_id, name FROM player_data WHERE uuid = ?
        `, [bukkitUuid]);
        
        if (bukkitResult.length === 0) {
            console.log('   ❌ RESULTADO: Nenhum jogador encontrado (ESPERADO)');
        } else {
            console.log('   ✅ RESULTADO: Jogador encontrado (INESPERADO)');
        }
        
        // 2. Testar busca por UUID canônico (deve funcionar)
        console.log('\n2️⃣ TESTE: Busca por UUID Canônico (deve funcionar)');
        const [canonicalResult] = await pool.execute(`
            SELECT player_id, name FROM player_data WHERE uuid = ?
        `, [canonicalUuid]);
        
        if (canonicalResult.length === 0) {
            console.log('   ❌ RESULTADO: Nenhum jogador encontrado (PROBLEMA!)');
        } else {
            console.log('   ✅ RESULTADO: Jogador encontrado (ESPERADO)');
            console.log(`      - Nome: ${canonicalResult[0].name}`);
            console.log(`      - ID: ${canonicalResult[0].player_id}`);
        }
        
        // 3. Testar se o jogador tem permissões de clã
        if (canonicalResult.length > 0) {
            const playerId = canonicalResult[0].player_id;
            
            console.log('\n3️⃣ TESTE: Verificando permissões de clã');
            const [clanPermissions] = await pool.execute(`
                SELECT gp.permission_node, gp.is_granted, pg.group_name
                FROM group_permissions gp
                JOIN permission_groups pg ON gp.group_id = pg.group_id
                JOIN player_groups pg2 ON pg.group_id = pg2.group_id
                WHERE pg2.player_id = ? AND gp.permission_node LIKE 'clans.%'
            `, [playerId]);
            
            if (clanPermissions.length === 0) {
                console.log('   ❌ RESULTADO: Nenhuma permissão de clã encontrada!');
            } else {
                console.log('   ✅ RESULTADO: Permissões de clã encontradas:');
                for (const perm of clanPermissions) {
                    const status = perm.is_granted ? '✅' : '❌';
                    console.log(`      ${status} ${perm.permission_node} (${perm.group_name})`);
                }
            }
        }
        
        // 4. Testar se existe mapeamento de UUIDs
        console.log('\n4️⃣ TESTE: Verificando mapeamento de UUIDs');
        const [uuidMapping] = await pool.execute(`
            SELECT * FROM uuid_mapping WHERE bukkit_uuid = ?
        `, [bukkitUuid]);
        
        if (uuidMapping.length === 0) {
            console.log('   ❌ RESULTADO: Nenhum mapeamento encontrado!');
        } else {
            console.log('   ✅ RESULTADO: Mapeamento encontrado:');
            console.log(`      - Bukkit UUID: ${uuidMapping[0].bukkit_uuid}`);
            console.log(`      - Canônico UUID: ${uuidMapping[0].canonical_uuid}`);
        }
        
        // 5. Resumo do problema
        console.log('\n📋 RESUMO DO PROBLEMA:');
        console.log('   ❌ Java está buscando por UUID do Bukkit');
        console.log('   ✅ Banco tem dados por UUID canônico');
        console.log('   ❌ Sistema de permissões não consegue encontrar jogador');
        console.log('   💡 SOLUÇÃO: Java precisa usar UUID canônico');
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

// Executar
testPermissionsDirect();
