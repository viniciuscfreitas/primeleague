const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const axios = require('axios');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('recuperacao')
        .setDescription('Sistema de recuperação de conta - Gera códigos de backup seguros'),

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

            // Criar embed inicial
            const embed = new EmbedBuilder()
                .setTitle('🛡️ Sistema de Recuperação de Conta')
                .setDescription('Este sistema permite gerar códigos de backup para recuperar sua conta em caso de perda de acesso.')
                .setColor(0x0099FF)
                .addFields(
                    { 
                        name: '⚠️ Importante', 
                        value: '• Os códigos são válidos por **30 dias**\n• Guarde-os em local seguro\n• Nunca compartilhe com ninguém\n• Use apenas em emergências' 
                    },
                    { 
                        name: '🔐 Segurança', 
                        value: '• Códigos criptografados com BCrypt\n• Rate limiting para prevenir ataques\n• Auditoria completa de tentativas' 
                    }
                )
                .setFooter({ text: 'PrimeLeague - Sistema de Segurança' })
                .setTimestamp();

            // Botão para gerar códigos
            const row = new ActionRowBuilder()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId('gerar_codigos_backup')
                        .setLabel('🔑 Gerar Códigos de Backup')
                        .setStyle(ButtonStyle.Primary)
                        .setEmoji('🔑')
                );

            await interaction.reply({
                embeds: [embed],
                components: [row],
                ephemeral: true
            });

        } catch (error) {
            console.error('[RECUPERACAO] Erro:', error);
            await interaction.reply({
                content: '❌ **Erro interno do sistema. Tente novamente mais tarde.**',
                ephemeral: true
            });
        }
    },

    // Handler para o botão de gerar códigos
    async handleButton(interaction) {
        if (interaction.customId === 'gerar_codigos_backup') {
            await this.generateBackupCodes(interaction);
        }
    },

    async generateBackupCodes(interaction) {
        try {
            // Mostrar loading
            await interaction.deferUpdate();

            const userId = interaction.user.id;

            // Chamar API para gerar códigos
            const response = await axios.post('http://localhost:8080/api/v1/recovery/backup/generate', {
                discordId: userId
            }, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${process.env.API_TOKEN}`
                }
            });

            if (response.data.success) {
                // Buscar códigos gerados (a API retorna apenas sucesso, precisamos buscar os códigos)
                const statusResponse = await axios.get(`http://localhost:8080/api/v1/recovery/status/${userId}`, {
                    headers: {
                        'Authorization': `Bearer ${process.env.API_TOKEN}`
                    }
                });

                if (statusResponse.data.success && statusResponse.data.backupCodes) {
                    const codes = statusResponse.data.backupCodes;
                    
                    // Criar embed com os códigos
                    const codesEmbed = new EmbedBuilder()
                        .setTitle('🔑 Códigos de Backup Gerados')
                        .setDescription('**GUARDE ESTES CÓDIGOS EM LOCAL SEGURO!**')
                        .setColor(0x00FF00)
                        .addFields(
                            { 
                                name: '📋 Seus Códigos de Backup', 
                                value: codes.map((code, index) => `**${index + 1}.** \`${code}\``).join('\n') 
                            },
                            { 
                                name: '⏰ Validade', 
                                value: '**30 dias** a partir de agora' 
                            },
                            { 
                                name: '🔒 Segurança', 
                                value: '• Códigos criptografados\n• Rate limiting ativo\n• Auditoria completa' 
                            }
                        )
                        .addFields(
                            { 
                                name: '⚠️ AVISO CRÍTICO', 
                                value: '• **NUNCA** compartilhe estes códigos\n• Use apenas em emergências\n• Após usar um código, ele será invalidado\n• Guarde em local seguro offline' 
                            }
                        )
                        .setFooter({ text: 'PrimeLeague - Códigos de Backup' })
                        .setTimestamp();

                    await interaction.followUp({
                        embeds: [codesEmbed],
                        ephemeral: true
                    });

                } else {
                    throw new Error('Não foi possível recuperar os códigos gerados');
                }

            } else {
                throw new Error(response.data.message || 'Erro ao gerar códigos');
            }

        } catch (error) {
            console.error('[RECUPERACAO] Erro ao gerar códigos:', error);
            
            let errorMessage = '❌ **Erro ao gerar códigos de backup.**';
            
            if (error.response) {
                if (error.response.status === 404) {
                    errorMessage = '❌ **Você não possui uma conta vinculada. Use `/registrar` primeiro.**';
                } else if (error.response.status === 429) {
                    errorMessage = '⏰ **Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.**';
                } else {
                    errorMessage = `❌ **Erro do servidor: ${error.response.data?.message || 'Erro desconhecido'}**`;
                }
            }

            await interaction.followUp({
                content: errorMessage,
                ephemeral: true
            });
        }
    }
};
