const mysql = require('mysql2/promise');

// Configuração de conexão
const dbConfig = {
    host: 'localhost',
    user: 'root', // Ajuste conforme sua configuração
    password: 'root', // Ajuste conforme sua configuração
    database: 'primeleague',
    port: 3306
};

async function analyzeDatabase() {
    let connection;
    
    try {
        console.log('🔍 Conectando ao banco de dados MariaDB...');
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado com sucesso!');
        
        // 1. Verificar estrutura da tabela clan_players
        console.log('\n📋 ANALISANDO TABELA clan_players:');
        const [clanPlayersColumns] = await connection.execute(`
            SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' AND TABLE_NAME = 'clan_players'
            ORDER BY ORDINAL_POSITION
        `);
        
        console.table(clanPlayersColumns);
        
        // 2. Verificar dados atuais
        console.log('\n📊 DADOS ATUAIS DA TABELA clan_players:');
        const [clanPlayersData] = await connection.execute(`
            SELECT * FROM clan_players LIMIT 10
        `);
        
        console.table(clanPlayersData);
        
        // 3. Verificar constraints e índices (MariaDB compatível)
        console.log('\n🔗 CONSTRAINTS E ÍNDICES:');
        try {
            const [constraints] = await connection.execute(`
                SELECT 
                    CONSTRAINT_NAME,
                    COLUMN_NAME,
                    REFERENCED_TABLE_NAME,
                    REFERENCED_COLUMN_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
                WHERE TABLE_SCHEMA = 'primeleague' AND TABLE_NAME = 'clan_players'
            `);
            
            if (constraints.length > 0) {
                console.table(constraints);
            } else {
                console.log('ℹ️ Nenhuma constraint encontrada');
            }
        } catch (error) {
            console.log('ℹ️ Não foi possível verificar constraints (MariaDB):', error.message);
        }
        
        // 4. Verificar se há problemas de tipo
        console.log('\n⚠️ VERIFICANDO PROBLEMAS DE TIPO:');
        const [typeIssues] = await connection.execute(`
            SELECT role, COUNT(*) as count
            FROM clan_players 
            GROUP BY role
        `);
        
        console.table(typeIssues);
        
        // 5. Verificar se a coluna role deveria ser ENUM
        console.log('\n🔧 VERIFICANDO SE ROLE DEVERIA SER ENUM:');
        const [roleValues] = await connection.execute(`
            SELECT DISTINCT role, COUNT(*) as count
            FROM clan_players 
            GROUP BY role
        `);
        
        console.table(roleValues);
        
        // 6. Verificar estrutura da tabela clans
        console.log('\n📋 ANALISANDO TABELA clans:');
        const [clansColumns] = await connection.execute(`
            SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' AND TABLE_NAME = 'clans'
            ORDER BY ORDINAL_POSITION
        `);
        
        console.table(clansColumns);
        
        // 7. Verificar dados da tabela clans
        console.log('\n📊 DADOS ATUAIS DA TABELA clans:');
        const [clansData] = await connection.execute(`
            SELECT * FROM clans LIMIT 10
        `);
        
        console.table(clansData);
        
        // 8. Verificar se há inconsistências entre as tabelas
        console.log('\n🔍 VERIFICANDO INCONSISTÊNCIAS:');
        const [inconsistencies] = await connection.execute(`
            SELECT 
                c.id as clan_id,
                c.tag,
                c.name,
                c.founder_player_id,
                COUNT(cp.player_id) as member_count,
                GROUP_CONCAT(cp.role) as roles
            FROM clans c
            LEFT JOIN clan_players cp ON c.id = cp.clan_id
            GROUP BY c.id
        `);
        
        console.table(inconsistencies);
        
        // 9. Verificar se há problemas de foreign key
        console.log('\n🔗 VERIFICANDO FOREIGN KEYS:');
        const [foreignKeys] = await connection.execute(`
            SELECT 
                CONSTRAINT_NAME,
                COLUMN_NAME,
                REFERENCED_TABLE_NAME,
                REFERENCED_COLUMN_NAME
            FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND REFERENCED_TABLE_NAME IS NOT NULL
            AND TABLE_NAME = 'clan_players'
        `);
        
        console.table(foreignKeys);
        
        // 10. Verificar se há dados órfãos
        console.log('\n👻 VERIFICANDO DADOS ÓRFÃOS:');
        const [orphanedData] = await connection.execute(`
            SELECT 
                cp.player_id,
                cp.clan_id,
                cp.role,
                CASE 
                    WHEN pd.player_id IS NULL THEN 'JOGADOR NÃO EXISTE'
                    WHEN c.id IS NULL THEN 'CLÃ NÃO EXISTE'
                    ELSE 'OK'
                END as status
            FROM clan_players cp
            LEFT JOIN player_data pd ON cp.player_id = pd.player_id
            LEFT JOIN clans c ON cp.clan_id = c.id
            WHERE pd.player_id IS NULL OR c.id IS NULL
        `);
        
        if (orphanedData.length > 0) {
            console.table(orphanedData);
        } else {
            console.log('✅ Nenhum dado órfão encontrado!');
        }
        
        // 11. Verificar se há problemas de contagem
        console.log('\n🔢 VERIFICANDO PROBLEMAS DE CONTAGEM:');
        const [countIssues] = await connection.execute(`
            SELECT 
                c.id,
                c.tag,
                c.name,
                COUNT(cp.player_id) as actual_members,
                CASE 
                    WHEN COUNT(cp.player_id) < 0 THEN 'PROBLEMA: Contagem negativa'
                    WHEN COUNT(cp.player_id) = 0 AND c.founder_player_id IS NOT NULL THEN 'PROBLEMA: Clã sem membros mas com fundador'
                    ELSE 'OK'
                END as status
            FROM clans c
            LEFT JOIN clan_players cp ON c.id = cp.clan_id
            GROUP BY c.id
            HAVING COUNT(cp.player_id) < 0 OR (COUNT(cp.player_id) = 0 AND c.founder_player_id IS NOT NULL)
        `);
        
        if (countIssues.length > 0) {
            console.table(countIssues);
        } else {
            console.log('✅ Nenhum problema de contagem encontrado!');
        }
        
        console.log('\n🎯 ANÁLISE COMPLETA! Verifique os resultados acima.');
        
    } catch (error) {
        console.error('❌ Erro ao analisar banco de dados:', error.message);
        
        if (error.code === 'ECONNREFUSED') {
            console.log('💡 Dica: Verifique se o MariaDB está rodando na porta 3306');
        } else if (error.code === 'ER_ACCESS_DENIED_ERROR') {
            console.log('💡 Dica: Verifique usuário e senha do banco');
        } else if (error.code === 'ER_BAD_DB_ERROR') {
            console.log('💡 Dica: Verifique se o banco "primeleague" existe');
        }
    } finally {
        if (connection) {
            await connection.end();
            console.log('\n🔌 Conexão fechada.');
        }
    }
}

// Executar análise
analyzeDatabase().catch(console.error);
