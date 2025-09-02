const mysql = require('mysql2/promise');

// Configuração de conexão
const dbConfig = {
    host: 'localhost',
    user: 'root', // Ajuste conforme sua configuração
    password: 'root', // Ajuste conforme sua configuração
    database: 'primeleague',
    port: 3306,
    // Configurações para MariaDB
    authPlugins: {
        mysql_clear_password: () => () => Buffer.from([0x00])
    }
};

async function fixDatabaseSchema() {
    let connection;
    
    try {
        console.log('🔧 Conectando ao banco de dados MariaDB...');
        connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado com sucesso!');
        
        // 1. Verificar estrutura atual da tabela clan_players
        console.log('\n📋 VERIFICANDO ESTRUTURA ATUAL:');
        const [currentStructure] = await connection.execute(`
            DESCRIBE clan_players
        `);
        
        console.table(currentStructure);
        
        // 2. Verificar se a coluna role é ENUM ou VARCHAR
        const [roleColumn] = await connection.execute(`
            SELECT DATA_TYPE, COLUMN_TYPE 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_SCHEMA = 'primeleague' 
            AND TABLE_NAME = 'clan_players' 
            AND COLUMN_NAME = 'role'
        `);
        
        console.log('\n🔍 TIPO DA COLUNA ROLE:', roleColumn[0]);
        
        // 3. Verificar dados atuais para entender o problema
        console.log('\n📊 DADOS ATUAIS:');
        const [currentData] = await connection.execute(`
            SELECT * FROM clan_players
        `);
        
        console.table(currentData);
        
        // 4. Verificar se há problemas de contagem
        console.log('\n🔢 VERIFICANDO PROBLEMAS DE CONTAGEM:');
        const [countIssues] = await connection.execute(`
            SELECT 
                c.id,
                c.tag,
                c.name,
                COUNT(cp.player_id) as member_count,
                GROUP_CONCAT(cp.role) as roles
            FROM clans c
            LEFT JOIN clan_players cp ON c.id = cp.clan_id
            GROUP BY c.id
        `);
        
        console.table(countIssues);
        
        // 5. CORREÇÃO: Atualizar roles incorretos
        console.log('\n🔧 CORRIGINDO ROLES INCORRETOS...');
        
        // Mapeamento de roles antigos para novos
        const roleMappings = [
            { old: 'OFFICER', new: 'LEADER' },
            { old: 'MEMBER', new: 'MEMBER' },
            { old: 'LEADER', new: 'LEADER' },
            { old: 'FOUNDER', new: 'FOUNDER' }
        ];
        
        for (const mapping of roleMappings) {
            const [updateResult] = await connection.execute(`
                UPDATE clan_players 
                SET role = ? 
                WHERE role = ?
            `, [mapping.new, mapping.old]);
            
            if (updateResult.affectedRows > 0) {
                console.log(`✅ Atualizados ${updateResult.affectedRows} registros: ${mapping.old} → ${mapping.new}`);
            }
        }
        
        // 6. CORREÇÃO: Verificar se há dados duplicados ou inconsistentes
        console.log('\n🔍 VERIFICANDO DADOS DUPLICADOS...');
        const [duplicates] = await connection.execute(`
            SELECT player_id, clan_id, COUNT(*) as count
            FROM clan_players
            GROUP BY player_id, clan_id
            HAVING COUNT(*) > 1
        `);
        
        if (duplicates.length > 0) {
            console.log('⚠️ Dados duplicados encontrados:');
            console.table(duplicates);
            
            // Remover duplicatas mantendo apenas o registro mais recente
            console.log('🧹 Removendo duplicatas...');
            await connection.execute(`
                DELETE cp1 FROM clan_players cp1
                INNER JOIN clan_players cp2 
                WHERE cp1.id < cp2.id 
                AND cp1.player_id = cp2.player_id 
                AND cp1.clan_id = cp2.clan_id
            `);
            
            console.log('✅ Duplicatas removidas!');
        } else {
            console.log('✅ Nenhuma duplicata encontrada!');
        }
        
        // 7. CORREÇÃO: Verificar se todos os clãs têm pelo menos um membro
        console.log('\n🔍 VERIFICANDO CLÃS SEM MEMBROS...');
        const [clansWithoutMembers] = await connection.execute(`
            SELECT c.id, c.tag, c.name, c.founder_player_id
            FROM clans c
            LEFT JOIN clan_players cp ON c.id = cp.clan_id
            WHERE cp.player_id IS NULL
        `);
        
        if (clansWithoutMembers.length > 0) {
            console.log('⚠️ Clãs sem membros encontrados:');
            console.table(clansWithoutMembers);
            
            // Adicionar o fundador como membro se não existir
            for (const clan of clansWithoutMembers) {
                if (clan.founder_player_id) {
                    await connection.execute(`
                        INSERT INTO clan_players (player_id, clan_id, role, join_date, kills, deaths)
                        VALUES (?, ?, 'FOUNDER', NOW(), 0, 0)
                    `, [clan.founder_player_id, clan.id]);
                    
                    console.log(`✅ Adicionado fundador ${clan.founder_player_id} ao clã ${clan.tag}`);
                }
            }
        } else {
            console.log('✅ Todos os clãs têm membros!');
        }
        
        // 8. CORREÇÃO: Verificar se há jogadores em clãs inexistentes
        console.log('\n🔍 VERIFICANDO JOGADORES EM CLÃS INEXISTENTES...');
        const [orphanedPlayers] = await connection.execute(`
            SELECT cp.player_id, cp.clan_id, cp.role
            FROM clan_players cp
            LEFT JOIN clans c ON cp.clan_id = c.id
            WHERE c.id IS NULL
        `);
        
        if (orphanedPlayers.length > 0) {
            console.log('⚠️ Jogadores em clãs inexistentes:');
            console.table(orphanedPlayers);
            
            // Remover jogadores órfãos
            await connection.execute(`
                DELETE FROM clan_players 
                WHERE clan_id NOT IN (SELECT id FROM clans)
            `);
            
            console.log('✅ Jogadores órfãos removidos!');
        } else {
            console.log('✅ Nenhum jogador órfão encontrado!');
        }
        
        // 9. CORREÇÃO: Verificar se há jogadores inexistentes
        console.log('\n🔍 VERIFICANDO JOGADORES INEXISTENTES...');
        const [invalidPlayers] = await connection.execute(`
            SELECT cp.player_id, cp.clan_id, cp.role
            FROM clan_players cp
            LEFT JOIN player_data pd ON cp.player_id = pd.player_id
            WHERE pd.player_id IS NULL
        `);
        
        if (invalidPlayers.length > 0) {
            console.log('⚠️ Jogadores inexistentes encontrados:');
            console.table(invalidPlayers);
            
            // Remover jogadores inexistentes
            await connection.execute(`
                DELETE FROM clan_players 
                WHERE player_id NOT IN (SELECT player_id FROM player_data)
            `);
            
            console.log('✅ Jogadores inexistentes removidos!');
        } else {
            console.log('✅ Todos os jogadores são válidos!');
        }
        
        // 10. VERIFICAÇÃO FINAL: Contar membros por clã
        console.log('\n📊 VERIFICAÇÃO FINAL - MEMBROS POR CLÃ:');
        const [finalCount] = await connection.execute(`
            SELECT 
                c.id,
                c.tag,
                c.name,
                COUNT(cp.player_id) as member_count,
                GROUP_CONCAT(cp.role ORDER BY cp.role) as roles
            FROM clans c
            LEFT JOIN clan_players cp ON c.id = cp.clan_id
            GROUP BY c.id
            ORDER BY c.id
        `);
        
        console.table(finalCount);
        
        // 11. VERIFICAÇÃO FINAL: Estrutura da tabela
        console.log('\n📋 ESTRUTURA FINAL DA TABELA:');
        const [finalStructure] = await connection.execute(`
            DESCRIBE clan_players
        `);
        
        console.table(finalStructure);
        
        console.log('\n🎯 CORREÇÃO COMPLETA! O banco de dados foi corrigido.');
        
    } catch (error) {
        console.error('❌ Erro ao corrigir banco de dados:', error.message);
        
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

// Executar correção
fixDatabaseSchema().catch(console.error);
