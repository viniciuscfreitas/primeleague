// ========================================
// ADIÇÃO DE COLUNAS NA TABELA player_data
// ========================================
// Este script adiciona as colunas verification_status e donor_tier
// que estão faltando na tabela player_data

const mysql = require('mysql2/promise');

async function addPlayerDataColumns() {
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

        // Verificar estrutura atual da tabela
        console.log('🔍 Verificando estrutura atual da tabela player_data...');
        const [currentColumns] = await connection.execute('DESCRIBE player_data');
        
        console.log('📊 Estrutura atual da tabela player_data:');
        currentColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se as colunas já existem
        const existingColumns = currentColumns.map(col => col.Field);
        const needsVerificationStatus = !existingColumns.includes('verification_status');
        const needsDonorTier = !existingColumns.includes('donor_tier');

        if (needsVerificationStatus) {
            console.log('\n📋 Adicionando coluna verification_status...');
            await connection.execute(`
                ALTER TABLE player_data 
                ADD COLUMN verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') 
                NOT NULL DEFAULT 'PENDING' 
                COMMENT 'Status de verificação do jogador'
            `);
            console.log('✅ Coluna verification_status adicionada com sucesso!');
        } else {
            console.log('✅ Coluna verification_status já existe');
        }

        if (needsDonorTier) {
            console.log('\n📋 Adicionando coluna donor_tier...');
            await connection.execute(`
                ALTER TABLE player_data 
                ADD COLUMN donor_tier INT NOT NULL DEFAULT 0 
                COMMENT 'Nível de doador do jogador'
            `);
            console.log('✅ Coluna donor_tier adicionada com sucesso!');
        } else {
            console.log('✅ Coluna donor_tier já existe');
        }

        // Verificar estrutura final da tabela
        console.log('\n🔍 Verificando estrutura final da tabela player_data...');
        const [finalColumns] = await connection.execute('DESCRIBE player_data');
        
        console.log('📊 Estrutura final da tabela player_data:');
        finalColumns.forEach(row => {
            console.log(`   ${row.Field} | ${row.Type} | ${row.Null} | ${row.Key} | ${row.Default} | ${row.Extra}`);
        });

        // Verificar se as colunas foram adicionadas corretamente
        const finalColumnNames = finalColumns.map(col => col.Field);
        if (finalColumnNames.includes('verification_status') && finalColumnNames.includes('donor_tier')) {
            console.log('\n🎉 Todas as colunas necessárias foram adicionadas com sucesso!');
            console.log('🚀 O SchemaValidator agora deve passar na validação completa!');
        } else {
            console.log('\n⚠️  Algumas colunas ainda estão faltando!');
        }

    } catch (error) {
        console.error('❌ Erro ao adicionar colunas:', error.message);
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
addPlayerDataColumns();
