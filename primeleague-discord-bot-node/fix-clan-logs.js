const mysql = require('mysql2/promise');
require('dotenv').config();

// Configurações do banco (mesma do bot Discord)
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

async function fixClanLogsTable() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Iniciando correção da tabela clan_logs...');
        
        // Verificar se a tabela já existe
        const [existingTables] = await pool.execute(
            "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'primeleague' AND table_name = 'clan_logs'"
        );
        
        if (existingTables[0].count > 0) {
            console.log('✅ Tabela clan_logs já existe!');
            return true;
        }
        
        console.log('📋 Criando tabela clan_logs...');
        
        // Criar a tabela clan_logs
        const createTableSQL = `
            CREATE TABLE \`clan_logs\` (
              \`id\` INT NOT NULL AUTO_INCREMENT,
              \`clan_id\` INT NOT NULL,
              \`actor_player_id\` INT NOT NULL,
              \`actor_name\` VARCHAR(16) NOT NULL,
              \`action_type\` INT NOT NULL,
              \`target_player_id\` INT NULL DEFAULT NULL,
              \`target_name\` VARCHAR(16) NULL DEFAULT NULL,
              \`details\` TEXT NOT NULL,
              \`timestamp\` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              PRIMARY KEY (\`id\`),
              KEY \`idx_clan_logs_clan_id\` (\`clan_id\`),
              KEY \`idx_clan_logs_actor_player_id\` (\`actor_player_id\`),
              KEY \`idx_clan_logs_target_player_id\` (\`target_player_id\`),
              KEY \`idx_clan_logs_action_type\` (\`action_type\`),
              KEY \`idx_clan_logs_timestamp\` (\`timestamp\`),
              CONSTRAINT \`fk_clan_logs_clan\` 
                FOREIGN KEY (\`clan_id\`) REFERENCES \`clans\` (\`id\`) ON DELETE CASCADE ON UPDATE CASCADE,
              CONSTRAINT \`fk_clan_logs_actor_player\` 
                FOREIGN KEY (\`actor_player_id\`) REFERENCES \`player_data\` (\`player_id\`) ON DELETE CASCADE ON UPDATE CASCADE,
              CONSTRAINT \`fk_clan_logs_target_player\` 
                FOREIGN KEY (\`target_player_id\`) REFERENCES \`player_data\` (\`player_id\`) ON DELETE SET NULL ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
        `;
        
        await pool.execute(createTableSQL);
        
        // Verificar se a tabela foi criada com sucesso
        const [verification] = await pool.execute(
            "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'primeleague' AND table_name = 'clan_logs'"
        );
        
        if (verification[0].count > 0) {
            console.log('✅ Tabela clan_logs criada com sucesso!');
            
            // Verificar estrutura da tabela
            const [columns] = await pool.execute(
                "SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT FROM information_schema.columns WHERE table_schema = 'primeleague' AND table_name = 'clan_logs' ORDER BY ORDINAL_POSITION"
            );
            
            console.log('📊 Estrutura da tabela clan_logs:');
            columns.forEach(col => {
                console.log(`  - ${col.COLUMN_NAME}: ${col.DATA_TYPE} ${col.IS_NULLABLE === 'YES' ? 'NULL' : 'NOT NULL'} ${col.COLUMN_DEFAULT ? `DEFAULT ${col.COLUMN_DEFAULT}` : ''}`);
            });
            
            // Verificar foreign keys
            const [foreignKeys] = await pool.execute(
                "SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.key_column_usage WHERE table_schema = 'primeleague' AND table_name = 'clan_logs' AND referenced_table_name IS NOT NULL"
            );
            
            console.log('🔗 Foreign Keys da tabela clan_logs:');
            foreignKeys.forEach(fk => {
                console.log(`  - ${fk.CONSTRAINT_NAME}: ${fk.REFERENCED_TABLE_NAME}.${fk.REFERENCED_COLUMN_NAME}`);
            });
            
            return true;
        } else {
            console.log('❌ ERRO: Tabela clan_logs não foi criada!');
            return false;
        }
        
    } catch (error) {
        console.error('❌ Erro ao corrigir tabela clan_logs:', error);
        return false;
    } finally {
        await pool.end();
    }
}

// Executar a correção
fixClanLogsTable()
    .then(success => {
        if (success) {
            console.log('🎉 Correção da tabela clan_logs concluída com sucesso!');
            console.log('📝 Agora o sistema de clãs deve funcionar corretamente.');
        } else {
            console.log('💥 Falha na correção da tabela clan_logs!');
            process.exit(1);
        }
    })
    .catch(error => {
        console.error('💥 Erro inesperado:', error);
        process.exit(1);
    });
