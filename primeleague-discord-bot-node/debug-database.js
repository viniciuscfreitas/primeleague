const mysql = require('mysql2/promise');

// Configuração do banco de dados
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

async function debugDatabase() {
    let connection;
    
    try {
        console.log('🔍 === INICIANDO DEBUG DO BANCO DE DADOS ===');
        
        // Conectar ao banco
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conexão com banco estabelecida');
        
        // 1. Verificar estrutura das tabelas
        console.log('\n📋 === ESTRUTURA DAS TABELAS ===');
        
        const tables = ['player_data', 'discord_links', 'discord_users', 'ip_authorizations'];
        
        for (const table of tables) {
            try {
                const [rows] = await connection.execute(`DESCRIBE ${table}`);
                console.log(`\n📊 Estrutura da tabela ${table}:`);
                rows.forEach(row => {
                    console.log(`  - ${row.Field}: ${row.Type} ${row.Null === 'YES' ? '(NULL)' : '(NOT NULL)'} ${row.Key ? `[${row.Key}]` : ''}`);
                });
            } catch (error) {
                console.log(`❌ Erro ao verificar tabela ${table}: ${error.message}`);
            }
        }
        
        // 2. Verificar dados dos players
        console.log('\n🎮 === DADOS DOS PLAYERS ===');
        
        const [players] = await connection.execute('SELECT * FROM player_data ORDER BY player_id');
        console.log(`📊 Total de players: ${players.length}`);
        
        players.forEach(player => {
            console.log(`\n👤 Player ID: ${player.player_id}`);
            console.log(`   Nome: ${player.name}`);
            console.log(`   UUID: ${player.uuid}`);
            console.log(`   ELO: ${player.elo}`);
            console.log(`   Money: ${player.money}`);
            console.log(`   Status: ${player.status}`);
            console.log(`   Last Seen: ${player.last_seen}`);
        });
        
        // 3. Verificar discord_links
        console.log('\n🔗 === DISCORD LINKS ===');
        
        const [links] = await connection.execute(`
            SELECT dl.*, pd.name, pd.uuid 
            FROM discord_links dl 
            JOIN player_data pd ON dl.player_id = pd.player_id 
            ORDER BY dl.link_id
        `);
        
        console.log(`📊 Total de links: ${links.length}`);
        
        links.forEach(link => {
            console.log(`\n🔗 Link ID: ${link.link_id}`);
            console.log(`   Player: ${link.name} (${link.uuid})`);
            console.log(`   Discord ID: ${link.discord_id}`);
            console.log(`   Player UUID: ${link.player_uuid}`);
            console.log(`   Verified: ${link.verified}`);
            console.log(`   Status: ${link.status || 'NULL'}`);
            console.log(`   Created At: ${link.created_at}`);
        });
        
        // 4. Verificar discord_users
        console.log('\n👤 === DISCORD USERS ===');
        
        const [discordUsers] = await connection.execute('SELECT * FROM discord_users ORDER BY discord_id');
        console.log(`📊 Total de usuários Discord: ${discordUsers.length}`);
        
        discordUsers.forEach(user => {
            console.log(`\n👤 Discord ID: ${user.discord_id}`);
            console.log(`   Username: ${user.username}`);
            console.log(`   Subscription Expires: ${user.subscription_expires_at}`);
            console.log(`   Donor Tier: ${user.donor_tier}`);
            console.log(`   Created At: ${user.created_at}`);
        });
        
        // 5. Verificar IP authorizations
        console.log('\n🌐 === IP AUTHORIZATIONS ===');
        
        const [ipAuths] = await connection.execute('SELECT * FROM ip_authorizations ORDER BY id');
        console.log(`📊 Total de autorizações de IP: ${ipAuths.length}`);
        
        ipAuths.forEach(auth => {
            console.log(`\n🌐 ID: ${auth.id}`);
            console.log(`   Player Name: ${auth.player_name}`);
            console.log(`   IP Address: ${auth.ip_address}`);
            console.log(`   Authorized At: ${auth.authorized_at}`);
            console.log(`   Expires At: ${auth.expires_at}`);
        });
        
        // 6. Análise de problemas específicos
        console.log('\n🚨 === ANÁLISE DE PROBLEMAS ===');
        
        // Verificar players duplicados
        const [duplicates] = await connection.execute(`
            SELECT name, COUNT(*) as count, GROUP_CONCAT(player_id) as player_ids
            FROM player_data 
            GROUP BY name 
            HAVING COUNT(*) > 1
        `);
        
        if (duplicates.length > 0) {
            console.log('❌ PLAYERS DUPLICADOS ENCONTRADOS:');
            duplicates.forEach(dup => {
                console.log(`   Nome: ${dup.name} - ${dup.count} registros (IDs: ${dup.player_ids})`);
            });
        } else {
            console.log('✅ Nenhum player duplicado encontrado');
        }
        
        // Verificar links sem player correspondente
        const [orphanLinks] = await connection.execute(`
            SELECT dl.* 
            FROM discord_links dl 
            LEFT JOIN player_data pd ON dl.player_id = pd.player_id 
            WHERE pd.player_id IS NULL
        `);
        
        if (orphanLinks.length > 0) {
            console.log('❌ LINKS ÓRFÃOS ENCONTRADOS:');
            orphanLinks.forEach(link => {
                console.log(`   Link ID: ${link.link_id} - Player UUID: ${link.player_uuid} (não encontrado)`);
            });
        } else {
            console.log('✅ Nenhum link órfão encontrado');
        }
        
        // Verificar players sem link
        const [playersWithoutLink] = await connection.execute(`
            SELECT pd.* 
            FROM player_data pd 
            LEFT JOIN discord_links dl ON pd.player_id = dl.player_id 
            WHERE dl.player_id IS NULL
        `);
        
        if (playersWithoutLink.length > 0) {
            console.log('❌ PLAYERS SEM LINK DISCORD:');
            playersWithoutLink.forEach(player => {
                console.log(`   ${player.name} (${player.uuid})`);
            });
        } else {
            console.log('✅ Todos os players têm link Discord');
        }
        
        // 7. Teste de queries específicas
        console.log('\n🧪 === TESTE DE QUERIES ===');
        
        // Teste da query do isPlayerVerified
        const testPlayer = 'vinicff';
        console.log(`\n🔍 Testando query para: ${testPlayer}`);
        
        try {
            const [verifyResult] = await connection.execute(`
                SELECT COUNT(*) as count 
                FROM discord_links dl 
                JOIN player_data pd ON dl.player_id = pd.player_id 
                WHERE pd.name = ? AND dl.verified = TRUE
            `, [testPlayer]);
            
            console.log(`   Resultado: ${verifyResult[0].count} vínculos verificados`);
        } catch (error) {
            console.log(`   ❌ Erro na query: ${error.message}`);
        }
        
        // Teste da query do PENDING_RELINK
        try {
            const [pendingResult] = await connection.execute(`
                SELECT dl.status 
                FROM discord_links dl 
                JOIN player_data pd ON dl.player_id = pd.player_id 
                WHERE pd.name = ? AND dl.verified = TRUE 
                LIMIT 1
            `, [testPlayer]);
            
            if (pendingResult.length > 0) {
                console.log(`   Status PENDING_RELINK: ${pendingResult[0].status}`);
            } else {
                console.log('   Nenhum resultado para PENDING_RELINK');
            }
        } catch (error) {
            console.log(`   ❌ Erro na query PENDING_RELINK: ${error.message}`);
        }
        
        console.log('\n✅ === DEBUG DO BANCO CONCLUÍDO ===');
        
    } catch (error) {
        console.error('❌ Erro durante debug:', error);
    } finally {
        if (connection) {
            await connection.end();
            console.log('🔌 Conexão fechada');
        }
    }
}

// Executar o debug
debugDatabase().catch(console.error);
