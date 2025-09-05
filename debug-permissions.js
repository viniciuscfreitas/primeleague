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

async function debugPermissions() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔍 DEBUGANDO SISTEMA DE PERMISSÕES...\n');
        
        // 1. Verificar jogador vini
        const [players] = await pool.execute(`
            SELECT player_id, name, uuid FROM player_data WHERE name = 'vini'
        `);
        
        if (players.length === 0) {
            console.log('❌ Jogador vini não encontrado!');
            return;
        }
        
        const player = players[0];
        console.log(`✅ Jogador: ${player.name} (ID: ${player.player_id}, UUID: ${player.uuid})`);
        
        // 2. Verificar grupos do jogador
        const [playerGroups] = await pool.execute(`
            SELECT pg.*, pg2.group_name, pg2.priority 
            FROM player_groups pg 
            JOIN permission_groups pg2 ON pg.group_id = pg2.group_id 
            WHERE pg.player_id = ?
        `, [player.player_id]);
        
        console.log(`\n📋 Grupos do jogador (${playerGroups.length}):`);
        for (const group of playerGroups) {
            console.log(`   - ${group.group_name} (ID: ${group.group_id}, Prioridade: ${group.priority})`);
        }
        
        // 3. Verificar permissões de cada grupo
        for (const playerGroup of playerGroups) {
            console.log(`\n🔑 Permissões do grupo ${playerGroup.group_name}:`);
            
            const [permissions] = await pool.execute(`
                SELECT permission_node, is_granted 
                FROM group_permissions 
                WHERE group_id = ?
            `, [playerGroup.group_id]);
            
            for (const perm of permissions) {
                const status = perm.is_granted ? '✅' : '❌';
                console.log(`   ${status} ${perm.permission_node}`);
            }
        }
        
        // 4. Verificar se há permissões específicas de clãs
        console.log('\n🏰 VERIFICANDO PERMISSÕES DE CLÃS...');
        
        const [clanPermissions] = await pool.execute(`
            SELECT gp.permission_node, gp.is_granted, pg.group_name
            FROM group_permissions gp
            JOIN permission_groups pg ON gp.group_id = pg.group_id
            JOIN player_groups pg2 ON pg.group_id = pg2.group_id
            WHERE pg2.player_id = ? AND gp.permission_node LIKE 'clans.%'
        `, [player.player_id]);
        
        if (clanPermissions.length === 0) {
            console.log('❌ NENHUMA permissão de clã encontrada para o jogador!');
        } else {
            console.log('✅ Permissões de clã encontradas:');
            for (const perm of clanPermissions) {
                const status = perm.is_granted ? '✅' : '❌';
                console.log(`   ${status} ${perm.permission_node} (grupo: ${perm.group_name})`);
            }
        }
        
        // 5. Verificar permissões wildcard
        console.log('\n🌟 VERIFICANDO PERMISSÕES WILDCARD...');
        
        const [wildcardPermissions] = await pool.execute(`
            SELECT gp.permission_node, gp.is_granted, pg.group_name
            FROM group_permissions gp
            JOIN permission_groups pg ON gp.group_id = pg.group_id
            JOIN player_groups pg2 ON pg.group_id = pg2.group_id
            WHERE pg2.player_id = ? AND (gp.permission_node LIKE '%.%' OR gp.permission_node = '*')
        `, [player.player_id]);
        
        if (wildcardPermissions.length === 0) {
            console.log('❌ NENHUMA permissão wildcard encontrada!');
        } else {
            console.log('✅ Permissões wildcard encontradas:');
            for (const perm of wildcardPermissions) {
                const status = perm.is_granted ? '✅' : '❌';
                console.log(`   ${status} ${perm.permission_node} (grupo: ${perm.group_name})`);
            }
        }
        
        // 6. Verificar se há algum problema na tabela player_groups
        console.log('\n🔍 VERIFICANDO TABELA player_groups...');
        
        const [allPlayerGroups] = await pool.execute(`
            SELECT COUNT(*) as total FROM player_groups
        `);
        
        console.log(`Total de entradas na tabela player_groups: ${allPlayerGroups[0].total}`);
        
        if (allPlayerGroups[0].total === 0) {
            console.log('❌ PROBLEMA CRÍTICO: Tabela player_groups está vazia!');
        }
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

// Executar
debugPermissions();
