// ========================================
// CRIAÇÃO DA TABELA clan_event_wins
// ========================================
// Este script cria a tabela clan_event_wins que está faltando no banco
// e é utilizada pelo código MySqlClanDAO.registerEventWin()

const mysql = require('mysql2/promise');

async function createClanEventWinsTable() {
    let connection;
    
    try {
        // Configuração da conexão (usar as mesmas credenciais do bot)
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

        // SQL para criar a tabela
        const createTableSQL = `
            CREATE TABLE IF NOT EXISTS \`clan_event_wins\` (
                \`id\` INT NOT NULL AUTO_INCREMENT,
                \`clan_id\` INT NOT NULL,
                \`event_name\` VARCHAR(64) NOT NULL,
                \`win_date\` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (\`id\`),
                KEY \`idx_clan_event_wins_clan_id\` (\`clan_id\`),
                KEY \`idx_clan_event_wins_event_name\` (\`event_name\`),
                CONSTRAINT \`fk_clan_event_wins_clan\` 
                    FOREIGN KEY (\`clan_id\`) REFERENCES \`clans\` (\`id\`) ON DELETE CASCADE ON UPDATE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        `;

        console.log('📋 Executando CREATE TABLE...');
        await connection.execute(createTableSQL);
        console.log('✅ Tabela clan_event_wins criada com sucesso!');

        // Verificar se a tabela foi criada
        console.log('🔍 Verificando estrutura da tabela...');
        const [rows] = await connection.execute('DESCRIBE clan_event_wins');
        
        console.log('📊 Estrutura da tabela clan_event_wins:');
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
            WHERE TABLE_NAME = 'clan_event_wins' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
        `);

        if (constraints.length > 0) {
            console.log('✅ Constraints de chave estrangeira criadas:');
            constraints.forEach(constraint => {
                console.log(`   ${constraint.CONSTRAINT_NAME}: ${constraint.COLUMN_NAME} -> ${constraint.REFERENCED_TABLE_NAME}.${constraint.REFERENCED_COLUMN_NAME}`);
            });
        }

        console.log('\n🎉 Tabela clan_event_wins criada e configurada com sucesso!');
        console.log('🚀 O SchemaValidator agora deve passar na validação!');

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
createClanEventWinsTable();
