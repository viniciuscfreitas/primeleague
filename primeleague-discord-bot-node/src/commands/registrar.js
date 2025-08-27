const { SlashCommandBuilder } = require('discord.js');
const {
    getPlayerByNickname,
    getPortfolioByDiscordId,
    addAccountToPortfolio,
    generateVerifyCode,
    createServerNotification,
    getVerificationStatus,
    isNicknameAvailableForLinking,
    createDiscordLink
} = require('../database/mysql');

/**
 * Comando /registrar - Sistema de Portfólio de Contas V3
 * 
 * Permite que usuários Discord vinculem múltiplas contas Minecraft
 * ao seu portfólio pessoal. Cada conta tem assinatura individual.
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('registrar')
        .setDescription('📋 Registra uma conta do Minecraft ao seu portfólio pessoal')
        .addStringOption(option =>
            option.setName('nickname')
                .setDescription('Nome da conta Minecraft que deseja vincular')
                .setRequired(true)
        ),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const nickname = interaction.options.getString('nickname');
            const discordId = interaction.user.id;

            // 1. Verificar se o jogador existe no servidor
            const player = await getPlayerByNickname(nickname);
            if (!player) {
                return interaction.editReply({
                    content: '❌ **Jogador não encontrado**\n\n' +
                            `A conta \`${nickname}\` não foi encontrada no servidor.\n\n` +
                            '**Dicas:**\n' +
                            '• Verifique se digitou corretamente (case-sensitive)\n' +
                            '• A conta deve ter entrado no servidor pelo menos uma vez\n' +
                            '• Entre no servidor como visitante primeiro'
                });
            }

            // 2. Verificar portfólio atual do usuário
            const currentPortfolio = await getPortfolioByDiscordId(discordId);
            const maxAccountsPerUser = parseInt(process.env.MAX_ACCOUNTS_PER_USER || '10');
            
            if (currentPortfolio.length >= maxAccountsPerUser) {
                return interaction.editReply({
                    content: `❌ **Limite de Contas Excedido**\n\n` +
                            `Você já possui o máximo de **${maxAccountsPerUser} contas** vinculadas.\n\n` +
                            '**Soluções:**\n' +
                            '• Use `/minhas-contas` para gerenciar seu portfólio\n' +
                            '• Remova contas não utilizadas primeiro\n' +
                            '• Entre em contato com a administração para aumentar o limite'
                });
            }

            // 3. Verificar se a conta já está vinculada
            const existingAccount = await getPlayerByNickname(nickname);
            if (existingAccount && existingAccount.discord_id) {
                const isOwnAccount = existingAccount.discord_id === discordId;
                
                if (isOwnAccount) {
                    if (existingAccount.verified) {
                        return interaction.editReply({
                            content: `✅ **Conta já vinculada!**\n\n` +
                                    `A conta \`${nickname}\` já está registrada no seu portfólio.\n\n` +
                                    'Use `/minhas-contas` para ver todas as suas contas.'
                        });
                    } else {
                        // Conta vinculada mas não verificada - mostrar código
                        const verificationInfo = await getVerificationStatus(existingAccount.discord_id, nickname);
                        if (verificationInfo && verificationInfo.verify_code) {
                            return interaction.editReply({
                                content: `⏳ **Verificação Pendente**\n\n` +
                                        `A conta \`${nickname}\` está aguardando verificação.\n\n` +
                                        '**🎮 VERIFICAÇÃO NECESSÁRIA:**\n' +
                                        `Digite \`/verify ${verificationInfo.verify_code}\` **no servidor Minecraft**\n\n` +
                                        '**⏱️ Código expira em 5 minutos**'
                            });
                        }
                    }
                } else {
                    return interaction.editReply({
                        content: `❌ **Conta já vinculada**\n\n` +
                                `A conta \`${nickname}\` já está vinculada a outro usuário Discord.\n` +
                                'Cada conta só pode estar vinculada a um usuário por vez.'
                    });
                }
            }

            // 4. Verificar disponibilidade do nickname
            const availability = await isNicknameAvailableForLinking(nickname);
            if (!availability.available) {
                return interaction.editReply({
                    content: `❌ **Conta não disponível**\n\n` +
                            `A conta \`${nickname}\` já está vinculada a outro usuário Discord.\n` +
                            'Cada conta só pode estar vinculada a um usuário por vez.'
                });
            }

            // 5. Registrar a nova conta no portfólio
            const isFirstAccount = currentPortfolio.length === 0;
            const verifyCode = await generateVerifyCode();
            
            // Criar registro na tabela discord_links
            const linkResult = await createDiscordLink(discordId, player.uuid, player.name, verifyCode);
            if (!linkResult) {
                return interaction.editReply({
                    content: '❌ **Erro:** Falha ao iniciar registro. Tente novamente.'
                });
            }

            // Criar notificação para o servidor Minecraft
            await createServerNotification('PORTFOLIO_VERIFY_REQUEST', player.name, {
                code: verifyCode,
                discord_id: discordId,
                discord_user: interaction.user.tag,
                is_primary: isFirstAccount
            });

            // Determinar tipo da conta
            const accountType = isFirstAccount ? '👑 Principal' : '➕ Adicional';
            const accountPosition = `${currentPortfolio.length + 1}/${maxAccountsPerUser}`;
            
            return interaction.editReply({
                content: `🔗 **Conta ${accountType} Adicionada ao Portfólio**\n\n` +
                        `**📱 Conta:** \`${player.name}\`\n` +
                        `**👤 Proprietário:** ${interaction.user}\n` +
                        `**📊 Posição:** ${accountPosition} slots\n\n` +
                        '**🎮 VERIFICAÇÃO NECESSÁRIA:**\n' +
                        `Digite \`/verify ${verifyCode}\` **no servidor Minecraft**\n\n` +
                        '**📋 Próximos Passos:**\n' +
                        '1. ✅ Complete a verificação no servidor\n' +
                        '2. 💎 Adquira uma assinatura individual com `/renovar`\n' +
                        '3. 📋 Use `/minhas-contas` para gerenciar seu portfólio\n\n' +
                        '**⏱️ Código expira em 5 minutos**'
            });

        } catch (error) {
            console.error('[REGISTRAR] Erro ao executar comando:', error);
            
            return interaction.editReply({
                content: '❌ **Erro interno**\n\n' +
                        'Houve um erro ao processar sua solicitação.\n' +
                        'Tente novamente em instantes.'
            });
        }
    }
};
