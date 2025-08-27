const { SlashCommandBuilder } = require('discord.js');
const {
    getPlayerByNickname,
    getPortfolioByDiscordId,
    addAccountToPortfolio,
    generateVerifyCode,
    createServerNotification,
    getVerificationStatus,
    isNicknameAvailableForLinking,
    createDiscordLink,
    getDiscordLinkByPlayerId,
    getDonorInfoFromCore,
    createPlayer
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
            console.log(`[REGISTRAR] Buscando player: ${nickname}`);
            let player = await getPlayerByNickname(nickname);
            console.log(`[REGISTRAR] Resultado da busca:`, player);
            
            if (!player) {
                // Player não existe - criar automaticamente
                console.log(`[REGISTRAR] Player não encontrado, criando novo: ${nickname}`);
                player = await createPlayer(nickname);
                
                if (!player) {
                    return interaction.editReply({
                        content: '❌ **Erro:** Falha ao criar conta. Tente novamente.'
                    });
                }
                
                console.log(`[REGISTRAR] Player criado com sucesso:`, player);
            }

            // 2. Verificar portfólio atual e limite baseado no nível de doador
            const currentPortfolio = await getPortfolioByDiscordId(discordId);
            const donorInfo = await getDonorInfoFromCore(discordId);
            const config = require('../../bot-config.json');
            
            // Verificar se há erro na API
            if (donorInfo.error) {
                return interaction.editReply({
                    content: config.messages.errors.apiUnavailable
                });
            }
            
            if (currentPortfolio.length >= donorInfo.maxAccounts) {
                return interaction.editReply({
                    content: `❌ **Limite de Contas Excedido**\n\n` +
                            `Você já possui o máximo de **${donorInfo.maxAccounts} contas** permitidas para seu nível.\n\n` +
                            `**Seu Nível:** ${donorInfo.donorName} (Tier ${donorInfo.donorTier})\n` +
                            `**Limite Atual:** ${donorInfo.maxAccounts} contas\n\n` +
                            '**Soluções:**\n' +
                            '• Use `/conta` para gerenciar seu portfólio\n' +
                            '• Remova contas não utilizadas primeiro\n' +
                            '• Faça upgrade para um nível superior para mais contas\n' +
                            '• Entre em contato com a administração para dúvidas'
                });
            }

            // 3. Verificar se a conta já está vinculada
            console.log(`[REGISTRAR] Verificando vínculo existente para: ${nickname}`);
            console.log(`[REGISTRAR] Player encontrado:`, player);
            
            if (player && player.discord_id) {
                // Conta já está vinculada a algum Discord
                const isOwnAccount = player.discord_id === discordId;
                
                if (isOwnAccount) {
                    if (player.verified) {
                        return interaction.editReply({
                            content: `✅ **Conta já vinculada!**\n\n` +
                                    `A conta \`${nickname}\` já está registrada no seu portfólio.\n\n` +
                                    'Use `/conta` para ver todas as suas contas.'
                        });
                    } else {
                        // Conta vinculada mas não verificada - mostrar código
                        const verificationInfo = await getVerificationStatus(discordId, player.player_id);
                        if (verificationInfo && verificationInfo.verification_code) {
                            return interaction.editReply({
                                content: `⏳ **Verificação Pendente**\n\n` +
                                        `A conta \`${nickname}\` está aguardando verificação.\n\n` +
                                        '**🎮 VERIFICAÇÃO NECESSÁRIA:**\n' +
                                        `Digite \`/verify ${verificationInfo.verification_code}\` **no servidor Minecraft**\n\n` +
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
            const linkResult = await createDiscordLink(discordId, player.player_id, verifyCode);
            if (!linkResult) {
                // Verificar se o player já está vinculado
                const existingLink = await getDiscordLinkByPlayerId(player.player_id);
                if (existingLink) {
                    if (existingLink.discord_id === discordId) {
                        // É o mesmo usuário - verificar se precisa de verificação
                        const verificationInfo = await getVerificationStatus(discordId, player.player_id);
                        if (verificationInfo && verificationInfo.verification_code) {
                            return interaction.editReply({
                                content: `⏳ **Verificação Pendente**\n\n` +
                                        `A conta \`${nickname}\` está aguardando verificação.\n\n` +
                                        '**🎮 VERIFICAÇÃO NECESSÁRIA:**\n' +
                                        `Digite \`/verify ${verificationInfo.verification_code}\` **no servidor Minecraft**\n\n` +
                                        '**⏱️ Código expira em 5 minutos**'
                            });
                        } else {
                            return interaction.editReply({
                                content: `✅ **Conta já vinculada!**\n\n` +
                                        `A conta \`${nickname}\` já está registrada no seu portfólio.\n\n` +
                                        'Use `/conta` para ver todas as suas contas.'
                            });
                        }
                    } else {
                        return interaction.editReply({
                            content: `❌ **Conta já vinculada**\n\n` +
                                    `A conta \`${nickname}\` já está vinculada a outro usuário Discord.\n` +
                                    'Cada conta só pode estar vinculada a um usuário por vez.'
                        });
                    }
                }
                
                return interaction.editReply({
                    content: '❌ **Erro:** Falha ao iniciar registro. Tente novamente.'
                });
            }

            // Criar notificação para o servidor Minecraft
            await createServerNotification('PORTFOLIO_VERIFY_REQUEST', {
                player_name: player.name,
                code: verifyCode,
                discord_id: discordId,
                discord_user: interaction.user.tag,
                is_primary: isFirstAccount
            });

            // Determinar tipo da conta
            const accountType = isFirstAccount ? '👑 Principal' : '➕ Adicional';
            const accountPosition = `${currentPortfolio.length + 1}/${donorInfo.maxAccounts}`;
            
            return interaction.editReply({
                content: `🔗 **Conta ${accountType} Adicionada ao Portfólio**\n\n` +
                        `**📱 Conta:** \`${player.name}\`\n` +
                        `**👤 Proprietário:** ${interaction.user}\n` +
                        `**📊 Posição:** ${accountPosition} slots\n\n` +
                        '**🎮 VERIFICAÇÃO NECESSÁRIA:**\n' +
                        `Digite \`/verify ${verifyCode}\` **no servidor Minecraft**\n\n` +
                        '**📋 Próximos Passos:**\n' +
                        '1. ✅ Complete a verificação no servidor\n' +
                        '2. 💎 Adquira uma assinatura com `/assinatura`\n' +
                        '3. 📋 Use `/conta` para gerenciar seu portfólio\n\n' +
                        `**🎯 Seu Nível:** ${donorInfo.donorName} (${donorInfo.maxAccounts} contas max)\n` +
                        '**⏱️ Código expira em 5 minutos**'
            });

        } catch (error) {
            console.error('[REGISTRAR] Erro ao executar comando:', error);
            console.error('[REGISTRAR] Stack trace:', error.stack);
            
            return interaction.editReply({
                content: '❌ **Erro interno**\n\n' +
                        'Houve um erro ao processar sua solicitação.\n' +
                        'Tente novamente em instantes.'
            });
        }
    }
};
