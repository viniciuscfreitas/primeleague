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

async function addViniToOwner() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Adicionando jogador vini ao grupo owner...\n');
        
        // 1. Verificar se o jogador vini existe
        const [players] = await pool.execute(`
            SELECT player_id, name, uuid FROM player_data WHERE name = 'vini'
        `);
        
        if (players.length === 0) {
            console.log('❌ Jogador vini não encontrado no banco de dados!');
            return;
        }
        
        const player = players[0];
        console.log(`✅ Jogador encontrado: ${player.name} (ID: ${player.player_id})`);
        
        // 2. Verificar se o grupo owner existe
        const [groups] = await pool.execute(`
            SELECT group_id, group_name, priority FROM permission_groups WHERE group_name = 'owner'
        `);
        
        if (groups.length === 0) {
            console.log('❌ Grupo owner não encontrado!');
            return;
        }
        
        const group = groups[0];
        console.log(`✅ Grupo encontrado: ${group.group_name} (ID: ${group.group_id}, Prioridade: ${group.priority})`);
        
        // 3. Verificar se o jogador já está no grupo
        const [existingMembership] = await pool.execute(`
            SELECT * FROM player_groups WHERE player_id = ? AND group_id = ?
        `, [player.player_id, group.group_id]);
        
        if (existingMembership.length > 0) {
            console.log('✅ Jogador já está no grupo owner!');
            return;
        }
        
        // 4. Adicionar jogador ao grupo owner
        await pool.execute(`
            INSERT INTO player_groups (player_id, group_id, is_primary, added_at, reason) 
            VALUES (?, ?, true, NOW(), 'Adicionado via script de correção')
        `, [player.player_id, group.group_id]);
        
        console.log('✅ Jogador vini adicionado ao grupo owner com sucesso!');
        
        // 5. Verificar se foi adicionado
        const [newMembership] = await pool.execute(`
            SELECT pg.*, pg2.group_name, pd.name 
            FROM player_groups pg 
            JOIN permission_groups pg2 ON pg.group_id = pg2.group_id 
            JOIN player_data pd ON pg.player_id = pd.player_id 
            WHERE pg.player_id = ? AND pg.group_id = ?
        `, [player.player_id, group.group_id]);
        
        if (newMembership.length > 0) {
            console.log('✅ Verificação: Jogador está no grupo owner');
            console.log(`   - Nome: ${newMembership[0].name}`);
            console.log(`   - Grupo: ${newMembership[0].group_name}`);
            console.log(`   - Primário: ${newMembership[0].is_primary ? 'Sim' : 'Não'}`);
            console.log(`   - Adicionado em: ${newMembership[0].added_at}`);
        }
        
        // 6. Adicionar permissões básicas se não existirem
        console.log('\n🔧 Verificando permissões básicas...');
        
        const basicPermissions = [
            'clans.create',
            'clans.manage',
            'clans.delete',
            'admin.*',
            'primeleague.*'
        ];
        
        for (const permission of basicPermissions) {
            // Verificar se a permissão já existe
            const [existingPerm] = await pool.execute(`
                SELECT * FROM group_permissions WHERE group_id = ? AND permission_node = ?
            `, [group.group_id, permission]);
            
            if (existingPerm.length === 0) {
                // Adicionar permissão
                await pool.execute(`
                    INSERT INTO group_permissions (group_id, permission_node, is_granted, created_at, created_by_player_id) 
                    VALUES (?, ?, true, NOW(), ?)
                `, [group.group_id, permission, player.player_id]);
                
                console.log(`✅ Permissão adicionada: ${permission}`);
            } else {
                console.log(`ℹ️ Permissão já existe: ${permission}`);
            }
        }
        
        console.log('\n🎉 Processo concluído com sucesso!');
        console.log('💡 Agora o jogador vini deve conseguir criar clãs.');
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

// Executar
addViniToOwner();
