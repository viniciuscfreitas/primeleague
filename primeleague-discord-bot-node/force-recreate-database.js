const mysql = require('mysql2/promise');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    authPlugins: {
        mysql_native_password: () => () => Buffer.from('root', 'utf-8')
    }
};

async function forceRecreateDatabase() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🗑️  FORÇANDO RECRIAÇÃO DO BANCO DE DADOS PRIMELEAGUE');
        console.log('=====================================================\n');
        
        // 1. Forçar drop do banco existente
        console.log('💥 Removendo banco de dados existente...');
        try {
            await pool.execute('DROP DATABASE IF EXISTS `primeleague`');
            console.log('✅ Banco removido com sucesso\n');
        } catch (error) {
            console.log('⚠️  Erro ao remover banco (pode não existir):', error.message);
        }
        
        // 2. Criar novo banco
        console.log('🏗️  Criando novo banco de dados...');
        await pool.execute('CREATE DATABASE `primeleague` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
        console.log('✅ Banco criado com sucesso\n');
        
        // 3. Conectar ao novo banco
        const dbPool = mysql.createPool({
            ...dbConfig,
            database: 'primeleague'
        });
        
        // 4. Ler e executar apenas as partes essenciais do schema
        console.log('📖 Lendo schema e aplicando tabelas essenciais...');
        const schemaPath = path.join(__dirname, '..', 'database', 'SCHEMA-FINAL-AUTOMATIZADO.sql');
        const schemaSQL = fs.readFileSync(schemaPath, 'utf8');
        
        // Extrair apenas os CREATE TABLE (ignorar procedures, triggers, etc.)
        const createTableRegex = /CREATE TABLE[^;]+;/gi;
        const createTables = schemaSQL.match(createTableRegex) || [];
        
        console.log(`📝 Aplicando ${createTables.length} tabelas...\n`);
        
        for (let i = 0; i < createTables.length; i++) {
            const createTable = createTables[i];
            try {
                await dbPool.execute(createTable);
                console.log(`✅ Tabela ${i + 1}/${createTables.length} criada`);
            } catch (error) {
                console.log(`❌ Erro na tabela ${i + 1}/${createTables.length}: ${error.message}`);
            }
        }
        
        // 5. Inserir dados essenciais
        console.log('\n📊 Inserindo dados essenciais...');
        
        // CONSOLE player
        await dbPool.execute(`
            INSERT INTO player_data (player_id, uuid, name, elo, money, status) 
            VALUES (0, '00000000-0000-0000-0000-000000000000', 'CONSOLE', 0, 0.00, 'ACTIVE')
        `);
        console.log('✅ CONSOLE player inserido');
        
        // SYSTEM player
        await dbPool.execute(`
            INSERT IGNORE INTO player_data (uuid, name, elo, money, status) 
            VALUES ('7c42729a-60a2-abfb-7aec-b4dd1c8ffd93', 'SYSTEM', 0, 0.00, 'ACTIVE')
        `);
        console.log('✅ SYSTEM player inserido');
        
        // Estatísticas iniciais
        await dbPool.execute(`
            INSERT INTO server_stats (total_players, online_players, total_matches, total_revenue) 
            VALUES (0, 0, 0, 0.00)
        `);
        console.log('✅ Estatísticas iniciais inseridas');
        
        // 6. Verificar estrutura crítica
        console.log('\n🔍 VERIFICAÇÃO CRÍTICA - discord_links:');
        const [discordLinksStructure] = await dbPool.execute('DESCRIBE discord_links');
        console.log('Estrutura:');
        discordLinksStructure.forEach(field => {
            console.log(`  ${field.Field}: ${field.Type} ${field.Null} ${field.Key}`);
        });
        
        // Verificar se player_id foi removido
        const hasPlayerId = discordLinksStructure.some(field => field.Field === 'player_id');
        const hasPlayerUuid = discordLinksStructure.some(field => field.Field === 'player_uuid');
        
        console.log('\n⚠️  ANÁLISE FINAL:');
        console.log(`  player_id existe: ${hasPlayerId ? '❌ SIM (PROBLEMA!)' : '✅ NÃO (CORRETO)'}`);
        console.log(`  player_uuid existe: ${hasPlayerUuid ? '✅ SIM (CORRETO)' : '❌ NÃO (PROBLEMA!)'}`);
        
        if (!hasPlayerId && hasPlayerUuid) {
            console.log('\n🎉 SUCESSO: Schema aplicado corretamente!');
            console.log('   A tabela discord_links agora está conforme o schema oficial.');
        } else {
            console.log('\n❌ PROBLEMA: Schema não foi aplicado corretamente.');
            console.log('   Vou tentar corrigir manualmente...');
            
            // Tentar corrigir manualmente
            if (hasPlayerId) {
                console.log('\n🔧 Removendo coluna player_id manualmente...');
                try {
                    await dbPool.execute('ALTER TABLE discord_links DROP COLUMN player_id');
                    console.log('✅ Coluna player_id removida com sucesso!');
                } catch (error) {
                    console.log('❌ Erro ao remover player_id:', error.message);
                }
            }
        }
        
        // 7. Verificação final
        console.log('\n📋 VERIFICAÇÃO FINAL:');
        const [tables] = await dbPool.execute(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        console.log('Tabelas criadas:');
        tables.forEach(table => {
            console.log(`  ✅ ${table.table_name}`);
        });
        
        // Verificar dados essenciais
        const [consolePlayer] = await dbPool.execute('SELECT * FROM player_data WHERE player_id = 0 OR name = "CONSOLE"');
        console.log(`\nCONSOLE player: ${consolePlayer.length > 0 ? '✅ Existe' : '❌ Não existe'}`);
        
        await dbPool.end();
        
        console.log('\n=====================================================');
        console.log('🎉 RECRIAÇÃO FORÇADA CONCLUÍDA!');
        console.log('\n📋 PRÓXIMOS PASSOS:');
        console.log('  1. Reiniciar o servidor Minecraft');
        console.log('  2. Testar conexão do bot Discord');
        console.log('  3. Verificar se não há mais erros de schema');
        
    } catch (error) {
        console.error('❌ Erro durante recriação forçada:', error.message);
        throw error;
    } finally {
        await pool.end();
    }
}

forceRecreateDatabase()
    .then(() => {
        console.log('\n🎉 Processo concluído com sucesso!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Erro fatal:', error);
        process.exit(1);
    });
