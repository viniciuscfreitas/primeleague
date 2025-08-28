const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const axios = require('axios');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('vincular')
        .setDescription('Vincular conta usando código de backup')
        .addStringOption(option =>
            option.setName('nickname')
                .setDescription('Nickname do jogador para vincular')
                .setRequired(true))
        .addStringOption(option =>
            option.setName('codigo')
                .setDescription('Código de backup para vincular')
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
            const codigo = interaction.options.getString('codigo');

            // Mostrar loading
            await interaction.deferReply({ ephemeral: true });

            // Validar formato do código (8 caracteres alfanuméricos)
            if (!/^[A-Z0-9]{8}$/.test(codigo)) {
                await interaction.editReply({
                    content: '❌ **Código inválido. O código deve ter 8 caracteres (letras e números).**',
                    ephemeral: true
                });
                return;
            }

            // Verificar se o código é válido
            try {
                const verifyResponse = await axios.post('http://localhost:8080/api/v1/recovery/verify', {
                    discordId: userId,
                    code: codigo
                }, {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${process.env.API_TOKEN}`
                    }
                });

                if (!verifyResponse.data.success) {
                    await interaction.editReply({
                        content: '❌ **Código inválido ou expirado. Verifique o código e tente novamente.**',
                        ephemeral: true
                    });
                    return;
                }

            } catch (error) {
                console.error('[VINCULAR] Erro ao verificar código:', error);
                
                let errorMessage = '❌ **Erro ao verificar código.**';
                
                if (error.response) {
                    if (error.response.status === 400) {
                        errorMessage = '❌ **Código inválido ou expirado. Verifique o código e tente novamente.**';
                    } else if (error.response.status === 404) {
                        errorMessage = '❌ **Você não possui códigos de backup. Use `/recuperacao` primeiro.**';
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
                return;
            }

            // Executar transferência de assinatura
            try {
                const transferResponse = await axios.post('http://localhost:8080/api/v1/discord/transfer', {
                    playerName: nickname,
                    newDiscordId: userId
                }, {
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${process.env.API_TOKEN}`
                    }
                });

                if (transferResponse.data.success) {
                    // Criar embed de sucesso
                    const successEmbed = new EmbedBuilder()
                        .setTitle('✅ Conta Vinculada com Sucesso!')
                        .setDescription(`**Sua conta foi vinculada ao Discord!**`)
                        .setColor(0x00FF00)
                        .addFields(
                            { 
                                name: '👤 Jogador', 
                                value: `\`${nickname}\`` 
                            },
                            { 
                                name: '🔗 Discord', 
                                value: `<@${userId}>` 
                            },
                            { 
                                name: '✅ Status', 
                                value: '**Vinculação ativa**' 
                            }
                        )
                        .addFields(
                            { 
                                name: '🎮 Próximos Passos', 
                                value: '• Entre no servidor Minecraft\n• Sua assinatura foi transferida automaticamente\n• Use `/conta` para verificar status' 
                            },
                            { 
                                name: '🔒 Segurança', 
                                value: '• Código usado foi invalidado\n• Vínculo protegido por autenticação\n• Auditoria completa registrada' 
                            }
                        )
                        .setFooter({ text: 'PrimeLeague - Sistema de Vinculação' })
                        .setTimestamp();

                    await interaction.editReply({
                        embeds: [successEmbed],
                        ephemeral: true
                    });

                } else {
                    throw new Error(transferResponse.data.message || 'Erro na transferência');
                }

            } catch (error) {
                console.error('[VINCULAR] Erro na transferência:', error);
                
                let errorMessage = '❌ **Erro ao vincular conta.**';
                
                if (error.response) {
                    if (error.response.status === 400) {
                        if (error.response.data.message.includes('igual ao atual')) {
                            errorMessage = '❌ **Esta conta já está vinculada ao seu Discord.**';
                        } else if (error.response.data.message.includes('não encontrado')) {
                            errorMessage = '❌ **Jogador não encontrado. Verifique o nickname.**';
                        } else if (error.response.data.message.includes('não possui vínculo')) {
                            errorMessage = '❌ **Este jogador não possui vínculo Discord ativo.**';
                        } else {
                            errorMessage = `❌ **Erro de validação: ${error.response.data.message}**`;
                        }
                    } else if (error.response.status === 404) {
                        errorMessage = '❌ **Jogador não encontrado. Verifique o nickname.**';
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
            console.error('[VINCULAR] Erro:', error);
            await interaction.editReply({
                content: '❌ **Erro interno do sistema. Tente novamente mais tarde.**',
                ephemeral: true
            });
        }
    }
};
