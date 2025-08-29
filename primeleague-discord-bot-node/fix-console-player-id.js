const mysql = require('mysql2/promise');

async function fixConsolePlayerId() {
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
        console.log('🔧 CORRIGINDO player_id DO CONSOLE...');
        
        // Verificar estado atual
        const [currentConsole] = await connection.execute('SELECT player_id, name FROM player_data WHERE name = "CONSOLE"');
        console.log('Estado atual do CONSOLE:', currentConsole[0]);
        
        if (currentConsole.length > 0) {
            // Atualizar para player_id = 0
            await connection.execute('UPDATE player_data SET player_id = 0 WHERE name = "CONSOLE"');
            console.log('✅ CONSOLE atualizado para player_id = 0');
            
            // Verificar se foi atualizado
            const [updatedConsole] = await connection.execute('SELECT player_id, name FROM player_data WHERE name = "CONSOLE"');
            console.log('Estado após correção:', updatedConsole[0]);
        } else {
            console.log('❌ CONSOLE não encontrado na tabela player_data');
        }
        
    } catch (error) {
        console.error('❌ Erro ao corrigir CONSOLE:', error.message);
    } finally {
        await connection.end();
    }
}

fixConsolePlayerId();
