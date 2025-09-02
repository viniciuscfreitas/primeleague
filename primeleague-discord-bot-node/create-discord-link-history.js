// ========================================
// CRIAÇÃO DA TABELA discord_link_history
// ========================================
// Este script cria a tabela discord_link_history que está faltando no banco

const mysql = require('mysql2/promise');

async function createDiscordLinkHistoryTable() {
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

        // SQL para criar a tabela discord_link_history
        const createTableSQL = `
            CREATE TABLE IF NOT EXISTS \`discord_link_history\` (
                \`id\` INT NOT NULL AUTO_INCREMENT,
                \`action\` VARCHAR(50) NOT NULL,
                \`player_id\` INT NOT NULL,
                \`discord_id_old\` VARCHAR(20) NULL DEFAULT NULL,
                \`discord_id_new\` VARCHAR(20) NULL DEFAULT NULL,
                \`details\` TEXT NULL DEFAULT NULL,
                \`ip_address\` VARCHAR(45) NULL DEFAULT NULL,
                \`created_at\` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (\`id\`),
                KEY \`idx_discord_link_history_player_id\` (\`player_id\`),
                KEY \`idx_discord_link_history_action\` (\`action\`),
                KEY \`idx_discord_link_history_created_at\` (\`created_at\`),
                CONSTRAINT \`fk_discord_link_history_player\` 
                    FOREIGN KEY (\`player_id\`) REFERENCES \`player_data\` (\`player_id\`) ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `;

        console.log('📋 Executando CREATE TABLE para discord_link_history...');
        await connection.execute(createTableSQL);
        console.log('✅ Tabela discord_link_history criada com sucesso!');

        // Verificar se a tabela foi criada
        console.log('🔍 Verificando estrutura da tabela...');
        const [rows] = await connection.execute('DESCRIBE discord_link_history');
        
        console.log('📊 Estrutura da tabela discord_link_history:');
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
            WHERE TABLE_NAME = 'discord_link_history' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `);

        if (constraints.length > 0) {
            console.log('✅ Constraints de chave estrangeira criadas:');
            constraints.forEach(constraint => {
                console.log(`   ${constraint.CONSTRAINT_NAME}: ${constraint.COLUMN_NAME} -> ${constraint.REFERENCED_TABLE_NAME}.${constraint.REFERENCED_COLUMN_NAME}`);
            });
        }

        console.log('\n🎉 Tabela discord_link_history criada e configurada com sucesso!');
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
createDiscordLinkHistoryTable();
