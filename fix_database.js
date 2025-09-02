const mysql = require('mysql2/promise');

async function fixDatabase() {
    try {
        console.log('Conectando ao banco de dados...');
        const conn = await mysql.createConnection({
            host: 'localhost',
            user: 'root',
            password: 'root',
            database: 'primeleague'
        });
        
        console.log('Conectado! Corrigindo tabela permission_logs...');
        
        // Permitir que actor_player_id seja NULL
        await conn.execute('ALTER TABLE permission_logs MODIFY COLUMN actor_player_id INT NULL');
        console.log('Coluna actor_player_id modificada para permitir NULL');
        
        await conn.end();
        console.log('Correcao aplicada com sucesso!');
        
    } catch (e) {
        console.log('Erro:', e.message);
    }
}

fixDatabase();
