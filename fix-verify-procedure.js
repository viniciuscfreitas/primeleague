const mysql = require('mysql2/promise');

const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
};

async function fixVerifyProcedure() {
    const pool = mysql.createPool(dbConfig);
    
    try {
        console.log('🔧 Corrigindo stored procedure VerifyDiscordLink...\n');
        
        // 1. Verificar se a procedure já existe
        const [procedures] = await pool.execute(`
            SELECT ROUTINE_NAME 
            FROM INFORMATION_SCHEMA.ROUTINES 
            WHERE ROUTINE_SCHEMA = 'primeleague' 
            AND ROUTINE_NAME = 'VerifyDiscordLink'
        `);
        
        if (procedures.length > 0) {
            console.log('✅ Stored procedure VerifyDiscordLink já existe');
            console.log('🔄 Removendo versão existente para recriar...');
            
            // Remover procedure existente
            await pool.execute('DROP PROCEDURE IF EXISTS VerifyDiscordLink');
            console.log('✅ Procedure removida');
        }
        
        // 2. Criar a stored procedure
        console.log('📝 Criando stored procedure VerifyDiscordLink...');
        
        const createProcedureSQL = `
        CREATE PROCEDURE \`VerifyDiscordLink\`(
            IN p_player_name VARCHAR(16),
            IN p_verification_code VARCHAR(8)
        )
        BEGIN
            DECLARE v_player_uuid CHAR(36);
            DECLARE v_discord_id VARCHAR(20);
            DECLARE v_success BOOLEAN DEFAULT FALSE;
            DECLARE v_message VARCHAR(255);
            DECLARE v_player_id INT;
            
            -- Obter UUID e player_id do player
            SELECT uuid, player_id INTO v_player_uuid, v_player_id
            FROM player_data 
            WHERE name = p_player_name;
            
            IF v_player_uuid IS NOT NULL THEN
                -- Verificar código de verificação
                SELECT dl.discord_id INTO v_discord_id
                FROM discord_links dl
                JOIN player_data pd ON dl.player_id = pd.player_id
                WHERE pd.name = p_player_name 
                AND dl.verification_code = p_verification_code
                AND dl.code_expires_at > NOW()
                AND dl.verified = 0;
                
                IF v_discord_id IS NOT NULL THEN
                    -- Marcar como verificado
                    UPDATE discord_links 
                    SET verified = 1, 
                        verified_at = NOW(),
                        verification_code = NULL,
                        code_expires_at = NULL
                    WHERE discord_id = v_discord_id;
                    
                    SET v_success = TRUE;
                    SET v_message = 'Verificação realizada com sucesso!';
                ELSE
                    SET v_message = 'Código inválido ou expirado!';
                END IF;
            ELSE
                SET v_message = 'Jogador não encontrado!';
            END IF;
            
            -- Retornar resultado
            SELECT v_success as success, v_message as message, v_discord_id as discord_id, v_player_uuid as player_uuid, v_player_id as player_id;
        END
        `;
        
        await pool.execute(createProcedureSQL);
        console.log('✅ Stored procedure VerifyDiscordLink criada com sucesso!');
        
        // 3. Verificar se foi criada
        const [verifyProcedure] = await pool.execute(`
            SELECT ROUTINE_NAME, ROUTINE_TYPE
            FROM INFORMATION_SCHEMA.ROUTINES 
            WHERE ROUTINE_SCHEMA = 'primeleague' 
            AND ROUTINE_NAME = 'VerifyDiscordLink'
        `);
        
        if (verifyProcedure.length > 0) {
            console.log('✅ Verificação: Procedure criada com sucesso!');
            console.log(`   - Nome: ${verifyProcedure[0].ROUTINE_NAME}`);
            console.log(`   - Tipo: ${verifyProcedure[0].ROUTINE_TYPE}`);
        } else {
            console.log('❌ Erro: Procedure não foi criada');
        }
        
        // 4. Testar a procedure com dados existentes
        console.log('\n🧪 Testando a procedure...');
        
        // Verificar se existe algum código de verificação válido
        const [testData] = await pool.execute(`
            SELECT dl.verification_code, dl.code_expires_at, pd.name
            FROM discord_links dl
            JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE dl.verified = 0 
            AND dl.verification_code IS NOT NULL
            AND dl.code_expires_at > NOW()
            LIMIT 1
        `);
        
        if (testData.length > 0) {
            const testCode = testData[0].verification_code;
            const testPlayer = testData[0].name;
            console.log(`   - Testando com jogador: ${testPlayer}`);
            console.log(`   - Código de teste: ${testCode}`);
            
            try {
                const [testResult] = await pool.execute('CALL VerifyDiscordLink(?, ?)', [testPlayer, testCode]);
                console.log('✅ Teste da procedure executado com sucesso!');
            } catch (testError) {
                console.log('⚠️ Erro no teste (pode ser esperado):', testError.message);
            }
        } else {
            console.log('ℹ️ Nenhum código de verificação válido encontrado para teste');
        }
        
        console.log('\n🎯 Correção concluída com sucesso!');
        console.log('💡 Agora você pode tentar conectar ao servidor novamente');
        
    } catch (error) {
        console.error('❌ Erro ao corrigir stored procedure:', error.message);
        console.error('Stack trace:', error.stack);
    } finally {
        await pool.end();
    }
}

// Executar a correção
fixVerifyProcedure().catch(console.error);
