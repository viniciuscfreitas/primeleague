const {
    generateVerifyCode,
    createDiscordLink,
    getVerificationStatus,
    verifyDiscordLink,
    createServerNotification
} = require('./src/database/mysql');

async function testVerificationSystem() {
    console.log('🧪 Testando Sistema de Verificação Segura...\n');

    try {
        // 1. Gerar código de verificação
        console.log('1️⃣ Gerando código de verificação...');
        const code = await generateVerifyCode();
        console.log(`   Código gerado: ${code}`);
        console.log(`   Tamanho: ${code.length} dígitos\n`);

        // 2. Criar vínculo pendente (simular registro)
        console.log('2️⃣ Criando vínculo pendente...');
        const testDiscordId = '999999999999999999';
        const testUUID = 'test-uuid-1234-5678-9012';
        const testNickname = 'TestPlayer';

        const linkCreated = await createDiscordLink(testDiscordId, testUUID, testNickname, code);
        console.log(`   Vínculo criado: ${linkCreated ? '✅' : '❌'}\n`);

        // 3. Verificar status antes da confirmação
        console.log('3️⃣ Verificando status (pendente)...');
        const statusPending = await getVerificationStatus(testDiscordId);
        console.log('   Status:', statusPending);
        console.log(`   Verificado: ${statusPending?.verified ? '✅' : '❌'}`);
        console.log(`   Código: ${statusPending?.verify_code || 'N/A'}`);
        console.log(`   Expira: ${statusPending?.verify_expires_at || 'N/A'}\n`);

        // 4. Simular verificação no Minecraft
        console.log('4️⃣ Simulando verificação no Minecraft...');
        const verified = await verifyDiscordLink(testNickname, code);
        console.log(`   Verificação: ${verified ? '✅ Sucesso' : '❌ Falhou'}\n`);

        // 5. Verificar status após confirmação
        console.log('5️⃣ Verificando status (confirmado)...');
        const statusVerified = await getVerificationStatus(testDiscordId);
        console.log('   Status:', statusVerified);
        console.log(`   Verificado: ${statusVerified?.verified ? '✅' : '❌'}`);
        console.log(`   Código: ${statusVerified?.verify_code || 'REMOVIDO'}`);
        console.log(`   Expira: ${statusVerified?.verify_expires_at || 'REMOVIDO'}\n`);

        // 6. Criar notificação para servidor
        console.log('6️⃣ Criando notificação para servidor...');
        const notification = await createServerNotification(
            'DISCORD_VERIFY_SUCCESS',
            testNickname,
            { discord_id: testDiscordId, timestamp: Date.now() }
        );
        console.log(`   Notificação criada: ${notification ? '✅' : '❌'}\n`);

        // 7. Teste de código inválido
        console.log('7️⃣ Testando código inválido...');
        const invalidVerify = await verifyDiscordLink(testNickname, '999999');
        console.log(`   Código inválido rejeitado: ${!invalidVerify ? '✅' : '❌'}\n`);

        console.log('🎉 Testes concluídos com sucesso!');

    } catch (error) {
        console.error('❌ Erro nos testes:', error);
    } finally {
        // Limpar dados de teste
        console.log('\n🧹 Limpando dados de teste...');
        try {
            const { pool } = require('./src/database/mysql');
            await pool.execute('DELETE FROM discord_links WHERE discord_id = ?', ['999999999999999999']);
            await pool.execute('DELETE FROM server_notifications WHERE target_player = ?', ['TestPlayer']);
            console.log('   Limpeza concluída ✅');
            process.exit(0);
        } catch (err) {
            console.error('   Erro na limpeza:', err);
            process.exit(1);
        }
    }
}

testVerificationSystem();
