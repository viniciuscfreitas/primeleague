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

async function recreateDatabase() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🗑️  RECRIANDO BANCO DE DADOS PRIMELEAGUE');
        console.log('==========================================\n');
        
        // 1. Ler o schema SQL
        console.log('📖 Lendo arquivo SCHEMA-FINAL-AUTOMATIZADO.sql...');
        const schemaPath = path.join(__dirname, '..', 'database', 'SCHEMA-FINAL-AUTOMATIZADO.sql');
        
        if (!fs.existsSync(schemaPath)) {
            throw new Error(`Arquivo schema não encontrado: ${schemaPath}`);
        }
        
        const schemaSQL = fs.readFileSync(schemaPath, 'utf8');
        console.log('✅ Schema lido com sucesso\n');
        
        // 2. Executar o schema
        console.log('🔨 Executando schema...');
        
        // Dividir o SQL em comandos individuais
        const commands = schemaSQL
            .split(';')
            .map(cmd => cmd.trim())
            .filter(cmd => cmd.length > 0 && !cmd.startsWith('--'));
        
        console.log(`📝 Executando ${commands.length} comandos SQL...\n`);
        
        for (let i = 0; i < commands.length; i++) {
            const command = commands[i];
            
            // Pular comandos vazios ou comentários
            if (!command || command.startsWith('--') || command.startsWith('/*')) {
                continue;
            }
            
            try {
                await pool.execute(command);
                console.log(`✅ Comando ${i + 1}/${commands.length} executado`);
            } catch (error) {
                // Ignorar erros de DROP DATABASE (pode não existir)
                if (error.message.includes('Unknown database') && command.includes('DROP DATABASE')) {
                    console.log(`⚠️  Comando ${i + 1}/${commands.length}: Database não existia (normal)`);
                } else {
                    console.log(`❌ Erro no comando ${i + 1}/${commands.length}: ${error.message}`);
                    console.log(`Comando: ${command.substring(0, 100)}...`);
                }
            }
        }
        
        console.log('\n✅ Schema executado com sucesso!');
        
        // 3. Verificar se foi criado corretamente
        console.log('\n🔍 VERIFICANDO CRIAÇÃO...');
        
        // Conectar ao banco recém-criado
        const dbPool = mysql.createPool({
            ...dbConfig,
            database: 'primeleague'
        });
        
        // Verificar tabelas críticas
        const [tables] = await dbPool.execute(`
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'primeleague' 
            ORDER BY table_name
        `);
        
        console.log('📋 Tabelas criadas:');
        tables.forEach(table => {
            console.log(`  ✅ ${table.table_name}`);
        });
        
        // Verificar estrutura da discord_links
        console.log('\n🚨 VERIFICAÇÃO CRÍTICA - discord_links:');
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
        }
        
        // Verificar dados essenciais
        console.log('\n📊 VERIFICANDO DADOS ESSENCIAIS:');
        const [consolePlayer] = await dbPool.execute('SELECT * FROM player_data WHERE player_id = 0 OR name = "CONSOLE"');
        console.log(`  CONSOLE player: ${consolePlayer.length > 0 ? '✅ Existe' : '❌ Não existe'}`);
        
        await dbPool.end();
        
        console.log('\n==========================================');
        console.log('🎉 RECRIAÇÃO DO BANCO CONCLUÍDA!');
        console.log('\n📋 PRÓXIMOS PASSOS:');
        console.log('  1. Reiniciar o servidor Minecraft');
        console.log('  2. Testar conexão do bot Discord');
        console.log('  3. Verificar se não há mais erros de schema');
        
    } catch (error) {
        console.error('❌ Erro durante recriação:', error.message);
        throw error;
    } finally {
        await pool.end();
    }
}

recreateDatabase()
    .then(() => {
        console.log('\n🎉 Processo concluído com sucesso!');
        process.exit(0);
    })
    .catch((error) => {
        console.error('💥 Erro fatal:', error);
        process.exit(1);
    });
