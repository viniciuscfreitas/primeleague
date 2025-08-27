const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const {
    getPortfolioByDiscordId,
    getPortfolioStats
} = require('../database/mysql');

/**
 * Comando /minha-assinatura - Sistema de Portfólio V3
 * 
 * Mostra informações básicas do portfólio e redireciona para /minhas-contas
 * para a experiência completa de gerenciamento.
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('minha-assinatura')
        .setDescription('📊 Exibe resumo das suas assinaturas e portfólio'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;

            // Buscar portfólio do usuário
            const portfolio = await getPortfolioByDiscordId(discordId);
            
            if (!portfolio || portfolio.length === 0) {
                // Usuário sem contas vinculadas
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('📭 Nenhuma Conta Vinculada')
                    .setDescription(
                        '**Você ainda não possui contas vinculadas.**\n\n' +
                        '🔗 **Para começar:**\n' +
                        '• Use `/registrar <seu_nick>` para vincular sua primeira conta\n' +
                        '• Complete a verificação in-game\n' +
                        '• Adquira sua assinatura com `/renovar`\n\n' +
                        '📋 **Para gerenciamento avançado:**\n' +
                        '• Use `/minhas-contas` para o dashboard completo'
                    )
                    .setThumbnail('https://mc-heads.net/avatar/MHF_Question.png')
                    .setFooter({ 
                        text: 'Prime League • Sistema de Portfólio',
                        iconURL: interaction.client.user.displayAvatarURL()
                    })
                    .setTimestamp();

                return await interaction.editReply({ embeds: [embed] });
            }

            // Calcular estatísticas do portfólio
            const activeAccounts = portfolio.filter(acc => acc.subscription_status === 'ACTIVE');
            const expiredAccounts = portfolio.filter(acc => acc.subscription_status === 'EXPIRED');
            const neverSubscribed = portfolio.filter(acc => acc.subscription_status === 'NEVER_SUBSCRIBED');

            // Encontrar conta primária
            const primaryAccount = portfolio.find(acc => acc.is_primary === 1) || portfolio[0];

            // Criar embed com resumo
            const embed = new EmbedBuilder()
                .setColor('#4CAF50')
                .setTitle('📊 Resumo das Suas Assinaturas')
                .setDescription(
                    `**Proprietário:** ${interaction.user.displayName}\n` +
                    `**Total de Contas:** ${portfolio.length}\n\n` +
                    `🟢 **Ativas:** ${activeAccounts.length}\n` +
                    `🔴 **Expiradas:** ${expiredAccounts.length}\n` +
                    `⚪ **Sem Assinatura:** ${neverSubscribed.length}\n\n` +
                    '━━━━━━━━━━━━━━━━━━━━━━━━'
                )
                .setThumbnail(`https://mc-heads.net/avatar/${primaryAccount.player_name}.png`)
                .setFooter({ 
                    text: `Prime League • ${portfolio.length} conta${portfolio.length !== 1 ? 's' : ''} vinculada${portfolio.length !== 1 ? 's' : ''}`,
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();

            // Adicionar algumas contas de exemplo
            const displayAccounts = portfolio.slice(0, 3); // Mostrar até 3 contas
            
            displayAccounts.forEach((account, index) => {
                const isPrimary = account.is_primary === 1;
                const statusEmoji = {
                    'ACTIVE': '🟢',
                    'EXPIRED': '🔴',
                    'NEVER_SUBSCRIBED': '⚪'
                }[account.subscription_status] || '⚫';

                const statusText = {
                    'ACTIVE': `Ativa (${account.days_remaining} dias)`,
                    'EXPIRED': 'Expirada',
                    'NEVER_SUBSCRIBED': 'Sem Assinatura'
                }[account.subscription_status] || 'Status Desconhecido';

                embed.addFields({
                    name: `${index + 1}. ${account.player_name}${isPrimary ? ' 👑' : ''}`,
                    value: `${statusEmoji} ${statusText}`,
                    inline: true
                });
            });

            // Adicionar campo de ações se há mais contas
            if (portfolio.length > 3) {
                embed.addFields({
                    name: '📋 Ver Todas as Contas',
                    value: `**+${portfolio.length - 3} conta${portfolio.length - 3 !== 1 ? 's' : ''} adiciona${portfolio.length - 3 !== 1 ? 'is' : 'l'}**\n` +
                           'Use `/minhas-contas` para o dashboard completo',
                    inline: false
                });
            }

            // Adicionar seção de ações rápidas
            embed.addFields({
                name: '⚡ Ações Rápidas',
                value: 
                    '📋 `/minhas-contas` - Dashboard completo\n' +
                    '🔗 `/registrar <nick>` - Vincular nova conta\n' +
                    '💎 `/renovar` - Renovar assinaturas\n' +
                    '🗑️ `/remover-membro <nick>` - Desvincular conta',
                inline: false
            });

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            console.error('[MINHA-ASSINATURA] Erro ao executar comando:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF3333')
                .setTitle('❌ Erro Interno')
                .setDescription(
                    'Houve um erro ao carregar suas informações.\n' +
                    'Tente novamente em instantes.'
                )
                .setTimestamp();

            await interaction.editReply({ embeds: [errorEmbed] });
        }
    }
};