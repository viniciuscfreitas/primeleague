const mysql = require('mysql2/promise');

// Configuração do banco de dados
const dbConfig = {
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'primeleague',
    port: 3306
};

async function fixDuplicates() {
    let connection;
    
    try {
        console.log('🔧 === INICIANDO CORREÇÃO DE DADOS DUPLICADOS ===');
        
        // Conectar ao banco
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conexão com banco estabelecida');
        
        // 1. Verificar players duplicados
        console.log('\n📋 === VERIFICANDO PLAYERS DUPLICADOS ===');
        
        const [duplicates] = await connection.execute(`
            SELECT name, COUNT(*) as count, GROUP_CONCAT(player_id ORDER BY player_id) as player_ids
            FROM player_data 
            GROUP BY name 
            HAVING COUNT(*) > 1
        `);
        
        if (duplicates.length === 0) {
            console.log('✅ Nenhum player duplicado encontrado');
            return;
        }
        
        console.log(`❌ Encontrados ${duplicates.length} players duplicados:`);
        duplicates.forEach(dup => {
            console.log(`   ${dup.name}: ${dup.count} registros (IDs: ${dup.player_ids})`);
        });
        
        // 2. Corrigir players duplicados
        console.log('\n🔧 === CORRIGINDO PLAYERS DUPLICADOS ===');
        
        for (const dup of duplicates) {
            const playerIds = dup.player_ids.split(',').map(id => parseInt(id));
            const keepId = playerIds[0]; // Manter o primeiro ID
            const deleteIds = playerIds.slice(1); // Deletar os demais
            
            console.log(`\n👤 Corrigindo ${dup.name}:`);
            console.log(`   Manter ID: ${keepId}`);
            console.log(`   Deletar IDs: ${deleteIds.join(', ')}`);
            
            // Verificar se há discord_links para os IDs que serão deletados
            for (const deleteId of deleteIds) {
                const [links] = await connection.execute(`
                    SELECT * FROM discord_links WHERE player_id = ?
                `, [deleteId]);
                
                if (links.length > 0) {
                    console.log(`   ⚠️  ID ${deleteId} tem ${links.length} discord_links - MIGRANDO...`);
                    
                    // Migrar discord_links para o ID que será mantido
                    await connection.execute(`
                        UPDATE discord_links 
                        SET player_id = ?, player_uuid = (SELECT uuid FROM player_data WHERE player_id = ?)
                        WHERE player_id = ?
                    `, [keepId, keepId, deleteId]);
                    
                    console.log(`   ✅ Discord links migrados do ID ${deleteId} para ${keepId}`);
                }
            }
            
            // Deletar players duplicados
            await connection.execute(`
                DELETE FROM player_data WHERE player_id IN (${deleteIds.join(',')})
            `);
            
            console.log(`   ✅ Players duplicados deletados`);
        }
        
        // 3. Verificar discord_links órfãos
        console.log('\n📋 === VERIFICANDO LINKS ÓRFÃOS ===');
        
        const [orphanLinks] = await connection.execute(`
            SELECT dl.* 
            FROM discord_links dl 
            LEFT JOIN player_data pd ON dl.player_uuid = pd.uuid 
            WHERE pd.uuid IS NULL
        `);
        
        if (orphanLinks.length > 0) {
            console.log(`❌ Encontrados ${orphanLinks.length} links órfãos:`);
            orphanLinks.forEach(link => {
                console.log(`   Link ID: ${link.link_id} - Player UUID: ${link.player_uuid}`);
            });
            
            // Tentar corrigir links órfãos
            console.log('\n🔧 === CORRIGINDO LINKS ÓRFÃOS ===');
            
            for (const link of orphanLinks) {
                // Tentar encontrar player por nome (se o link tiver player_name)
                if (link.player_name) {
                    const [players] = await connection.execute(`
                        SELECT * FROM player_data WHERE name = ?
                    `, [link.player_name]);
                    
                    if (players.length > 0) {
                        const player = players[0];
                        console.log(`   🔗 Corrigindo link ${link.link_id} para player ${link.player_name}`);
                        
                        await connection.execute(`
                            UPDATE discord_links 
                            SET player_uuid = ? 
                            WHERE link_id = ?
                        `, [player.uuid, link.link_id]);
                        
                        console.log(`   ✅ Link corrigido: ${link.player_uuid} -> ${player.uuid}`);
                    } else {
                        console.log(`   ❌ Player ${link.player_name} não encontrado - DELETANDO LINK`);
                        await connection.execute(`
                            DELETE FROM discord_links WHERE link_id = ?
                        `, [link.link_id]);
                    }
                } else {
                    console.log(`   ❌ Link ${link.link_id} sem player_name - DELETANDO`);
                    await connection.execute(`
                        DELETE FROM discord_links WHERE link_id = ?
                    `, [link.link_id]);
                }
            }
        } else {
            console.log('✅ Nenhum link órfão encontrado');
        }
        
        // 4. Verificar players sem link
        console.log('\n📋 === VERIFICANDO PLAYERS SEM LINK ===');
        
        const [playersWithoutLink] = await connection.execute(`
            SELECT pd.* 
            FROM player_data pd 
            LEFT JOIN discord_links dl ON pd.uuid = dl.player_uuid 
            WHERE dl.player_uuid IS NULL
        `);
        
        if (playersWithoutLink.length > 0) {
            console.log(`⚠️  Encontrados ${playersWithoutLink.length} players sem link Discord:`);
            playersWithoutLink.forEach(player => {
                console.log(`   ${player.name} (${player.uuid})`);
            });
        } else {
            console.log('✅ Todos os players têm link Discord');
        }
        
        // 5. Verificação final
        console.log('\n📋 === VERIFICAÇÃO FINAL ===');
        
        const [finalDuplicates] = await connection.execute(`
            SELECT name, COUNT(*) as count
            FROM player_data 
            GROUP BY name 
            HAVING COUNT(*) > 1
        `);
        
        if (finalDuplicates.length === 0) {
            console.log('✅ Nenhum player duplicado restante');
        } else {
            console.log('❌ Ainda há players duplicados:');
            finalDuplicates.forEach(dup => {
                console.log(`   ${dup.name}: ${dup.count} registros`);
            });
        }
        
        const [finalOrphans] = await connection.execute(`
            SELECT COUNT(*) as count
            FROM discord_links dl 
            LEFT JOIN player_data pd ON dl.player_uuid = pd.uuid 
            WHERE pd.uuid IS NULL
        `);
        
        if (finalOrphans[0].count === 0) {
            console.log('✅ Nenhum link órfão restante');
        } else {
            console.log(`❌ Ainda há ${finalOrphans[0].count} links órfãos`);
        }
        
        console.log('\n✅ === CORREÇÃO DE DADOS DUPLICADOS CONCLUÍDA ===');
        
    } catch (error) {
        console.error('❌ Erro durante correção:', error);
    } finally {
        if (connection) {
            await connection.end();
            console.log('🔌 Conexão fechada');
        }
    }
}

// Executar a correção
fixDuplicates().catch(console.error);
