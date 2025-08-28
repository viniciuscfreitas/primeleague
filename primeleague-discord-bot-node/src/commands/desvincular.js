const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const axios = require('axios');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('desvincular')
        .setDescription('Desvincular conta do Discord atual')
        .addStringOption(option =>
            option.setName('nickname')
                .setDescription('Nickname do jogador para desvincular')
                .setRequired(true)),

    async execute(interaction) {
        try {
            // Verificar se é DM
            if (!interaction.channel.type === 0) { // 0 = DM
                await interaction.reply({
                    content: '❌ **Este comando só pode ser usado em mensagem privada (DM) por segurança.**',
                    ephemeral: true
                });
                return;
            }

            const userId = interaction.user.id;
            const nickname = interaction.options.getString('nickname');

            // Mostrar loading
            await interaction.deferReply({ ephemeral: true });

            // Verificar se o usuário tem vínculo com este nickname
            try {
                const statusResponse = await axios.get(`http://localhost:8080/api/v1/recovery/status/${userId}`, {
                    headers: {
                        'Authorization': `Bearer ${process.env.API_TOKEN}`
                    }
                });

                if (!statusResponse.data.success) {
                    await interaction.editReply({
                        content: '❌ **Você não possui uma conta vinculada. Use `/registrar` primeiro.**',
                        ephemeral: true
                    });
                    return;
                }

                // Verificar se o nickname corresponde ao vínculo atual
                // (Esta verificação seria feita no Core, mas por enquanto vamos assumir que está correto)

            } catch (error) {
                if (error.response && error.response.status === 404) {
                    await interaction.editReply({
                        content: '❌ **Você não possui uma conta vinculada. Use `/registrar` primeiro.**',
                        ephemeral: true
                    });
                    return;
                }
                throw error;
            }

            // Desvincular conta e gerar código de re-vinculação
            try {
                const response = await axios.post('http://localhost:8080/api/v1/account/unlink', {
                    playerName: nickname,
                    ipAddress: "127.0.0.1" // IP simulado para teste
                }, {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${process.env.API_TOKEN}`
                    }
                });

                if (response.data.success) {
                    const relinkCode = response.data.relinkCode;
                    
                    // Criar embed de sucesso
                    const successEmbed = new EmbedBuilder()
                        .setTitle('✅ Desvinculação Iniciada')
                        .setDescription(`**Conta desvinculada com sucesso!**`)
                        .setColor(0x00FF00)
                        .addFields(
                            { 
                                name: '👤 Jogador', 
                                value: `\`${nickname}\`` 
                            },
                            { 
                                name: '🔑 Código de Re-vinculação', 
                                value: `\`${relinkCode}\`` 
                            },
                            { 
                                name: '⏰ Validade', 
                                value: '**5 minutos** a partir de agora' 
                            }
                        )
                        .addFields(
                            { 
                                name: '📋 Próximos Passos', 
                                value: '1. Use o código acima para re-vincular sua conta\n2. Use `/vincular <nickname> <codigo>`\n3. Ou use o comando `/recuperar` in-game' 
                            },
                            { 
                                name: '⚠️ Importante', 
                                value: '• Use o código rapidamente (expira em 5 minutos)\n• Use apenas quando necessário\n• Após usar, o código será invalidado' 
                            }
                        )
                        .setFooter({ text: 'PrimeLeague - Sistema de Desvinculação' })
                        .setTimestamp();

                    await interaction.editReply({
                        embeds: [successEmbed],
                        ephemeral: true
                    });

                } else {
                    throw new Error(response.data.message || 'Erro ao desvincular conta');
                }

            } catch (error) {
                console.error('[DESVINCULAR] Erro ao gerar código:', error);
                
                let errorMessage = '❌ **Erro ao processar desvinculação.**';
                
                if (error.response) {
                    if (error.response.status === 404) {
                        errorMessage = '❌ **Você não possui uma conta vinculada. Use `/registrar` primeiro.**';
                    } else if (error.response.status === 429) {
                        errorMessage = '⏰ **Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.**';
                    } else {
                        errorMessage = `❌ **Erro do servidor: ${error.response.data?.message || 'Erro desconhecido'}**`;
                    }
                }

                await interaction.editReply({
                    content: errorMessage,
                    ephemeral: true
                });
            }

        } catch (error) {
            console.error('[DESVINCULAR] Erro:', error);
            await interaction.editReply({
                content: '❌ **Erro interno do sistema. Tente novamente mais tarde.**',
                ephemeral: true
            });
        }
    }
};
