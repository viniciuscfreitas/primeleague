const mysql = require('mysql2/promise');

// Configurações do banco (mesmas do bot)
const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function testRealTimeSync() {
    let connection;
    try {
        console.log('🔍 TESTE DE SINCRONIZAÇÃO EM TEMPO REAL');
        console.log('=======================================');
        
        // Conectar ao banco
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado ao banco de dados');
        
        // 1. Verificar estado atual
        console.log('\n1️⃣ Estado atual do banco:');
        const [currentPlayers] = await connection.execute(
            'SELECT player_id, uuid, name, status FROM player_data ORDER BY player_id'
        );
        
        console.log(`📋 Total de players: ${currentPlayers.length}`);
        currentPlayers.forEach(player => {
            console.log(`   ID: ${player.player_id} | UUID: ${player.uuid} | Name: ${player.name} | Status: ${player.status}`);
        });
        
        // 2. Criar um player de teste
        console.log('\n2️⃣ Criando player de teste...');
        const testPlayerName = 'teste_sync_' + Date.now();
        
        // Gerar UUID compatível com Java
        const crypto = require('crypto');
        const source = "OfflinePlayer:" + testPlayerName;
        const hash = crypto.createHash('md5').update(source, 'utf-8').digest();
        hash[6] = (hash[6] & 0x0f) | 0x30;
        hash[8] = (hash[8] & 0x3f) | 0x80;
        const testUuid = [
            hash.toString('hex', 0, 4),
            hash.toString('hex', 4, 6),
            hash.toString('hex', 6, 8),
            hash.toString('hex', 8, 10),
            hash.toString('hex', 10, 16)
        ].join('-');
        
        const [result] = await connection.execute(
            `INSERT INTO player_data (uuid, name, elo, money, total_playtime, total_logins, status, last_seen)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW())`,
            [testUuid, testPlayerName, 1000, 0.00, 0, 0, 'ACTIVE']
        );
        
        const playerId = result.insertId;
        console.log(`✅ Player criado: ${testPlayerName} (ID: ${playerId}, UUID: ${testUuid})`);
        
        // 3. Verificar imediatamente se foi criado
        console.log('\n3️⃣ Verificando se foi criado...');
        const [verifyPlayer] = await connection.execute(
            'SELECT player_id, uuid, name, status FROM player_data WHERE name = ?',
            [testPlayerName]
        );
        
        if (verifyPlayer.length > 0) {
            console.log('✅ Player encontrado imediatamente:', verifyPlayer[0]);
        } else {
            console.log('❌ Player NÃO encontrado!');
        }
        
        // 4. Verificar total de players
        console.log('\n4️⃣ Total de players após criação:');
        const [totalPlayers] = await connection.execute(
            'SELECT COUNT(*) as total FROM player_data'
        );
        console.log(`📋 Total: ${totalPlayers[0].total}`);
        
        // 5. Limpar o player de teste
        console.log('\n5️⃣ Limpando player de teste...');
        await connection.execute(
            'DELETE FROM player_data WHERE name = ?',
            [testPlayerName]
        );
        console.log('✅ Player de teste removido');
        
        console.log('\n✅ Teste de sincronização concluído!');
        
    } catch (error) {
        console.error('❌ Erro no teste:', error);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

testRealTimeSync();
