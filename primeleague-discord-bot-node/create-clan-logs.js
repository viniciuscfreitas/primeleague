// ========================================
// CRIAÇÃO DA TABELA clan_logs
// ========================================
// Este script cria a tabela clan_logs que está faltando no banco
// e é utilizada pelo código MySqlClanDAO para logging de ações

const mysql = require('mysql2/promise');

async function createClanLogsTable() {
    let connection;
    
    try {
        // Configuração da conexão
        connection = await mysql.createConnection({
            host: 'localhost',
            port: 3306,
            user: 'root',
            password: 'root',
            database: 'primeleague',
            authPlugins: {
                mysql_native_password: () => () => Buffer.from('root', 'utf-8')
            }
        });

        console.log('🔗 Conectado ao banco de dados...');

        // SQL para criar a tabela clan_logs
        const createTableSQL = `
            CREATE TABLE IF NOT EXISTS \`clan_logs\` (
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
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `;

        console.log('📋 Executando CREATE TABLE para clan_logs...');
        await connection.execute(createTableSQL);
        console.log('✅ Tabela clan_logs criada com sucesso!');

        // Verificar se a tabela foi criada
        console.log('🔍 Verificando estrutura da tabela...');
        const [rows] = await connection.execute('DESCRIBE clan_logs');
        
        console.log('📊 Estrutura da tabela clan_logs:');
        rows.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar constraints
        console.log('\n🔗 Verificando constraints...');
        const [constraints] = await connection.execute(`
            SELECT 
                CONSTRAINT_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE 
            WHERE TABLE_NAME = 'clan_logs' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `);

        if (constraints.length > 0) {
            console.log('✅ Constraints de chave estrangeira criadas:');
            constraints.forEach(constraint => {
                console.log(`   ${constraint.CONSTRAINT_NAME}: ${constraint.COLUMN_NAME} -> ${constraint.REFERENCED_TABLE_NAME}.${constraint.REFERENCED_COLUMN_NAME}`);
            });
        }

        console.log('\n🎉 Tabela clan_logs criada e configurada com sucesso!');
        console.log('🚀 O SchemaValidator agora deve passar na validação completa!');

    } catch (error) {
        console.error('❌ Erro ao criar tabela:', error.message);
        if (error.code) {
            console.error('   Código de erro:', error.code);
        }
    } finally {
        if (connection) {
            await connection.end();
            console.log('🔌 Conexão fechada.');
        }
    }
}

// Executar o script
createClanLogsTable();
