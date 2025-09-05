const mysql = require('mysql2/promise');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague'
};

async function fixUuidMapping() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Criando tabela uuid_mapping...\n');
        
        // 1. Criar tabela
        await pool.execute(`
            CREATE TABLE IF NOT EXISTS uuid_mapping (
                id int(11) NOT NULL AUTO_INCREMENT,
                bukkit_uuid varchar(36) NOT NULL,
                canonical_uuid varchar(36) NOT NULL,
                player_id int(11) NOT NULL,
                created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id),
                UNIQUE KEY uk_bukkit_uuid (bukkit_uuid),
                UNIQUE KEY uk_canonical_uuid (canonical_uuid),
                KEY idx_player_id (player_id),
                CONSTRAINT fk_uuid_mapping_player FOREIGN KEY (player_id) REFERENCES player_data (player_id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `);
        
        console.log('✅ Tabela uuid_mapping criada com sucesso!');
        
        // 2. Inserir mapeamento para vini
        await pool.execute(`
            INSERT INTO uuid_mapping (bukkit_uuid, canonical_uuid, player_id) VALUES 
            (?, ?, ?)
            ON DUPLICATE KEY UPDATE 
                canonical_uuid = VALUES(canonical_uuid),
                updated_at = CURRENT_TIMESTAMP
        `, ['2c048974-fd73-0b58-bd88-88bf73df16e7', 'b2d67524-ac9a-31a0-80c7-7acd45619820', 4]);
        
        console.log('✅ Mapeamento de UUID inserido com sucesso!');
        
        // 3. Verificar se foi criado
        const [result] = await pool.execute('SELECT * FROM uuid_mapping');
        console.log(`✅ Verificação: ${result.length} mapeamento(s) encontrado(s)`);
        
        if (result.length > 0) {
            console.log('   - Bukkit UUID:', result[0].bukkit_uuid);
            console.log('   - Canônico UUID:', result[0].canonical_uuid);
            console.log('   - Player ID:', result[0].player_id);
        }
        
        console.log('\n🎉 Tabela uuid_mapping criada e configurada com sucesso!');
        console.log('💡 Agora reinicie o servidor para testar as permissões.');
        
    } catch (error) {
        console.error('❌ Erro:', error);
    } finally {
        await pool.end();
    }
}

// Executar
fixUuidMapping();
