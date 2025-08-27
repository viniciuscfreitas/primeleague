const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getPortfolioByDiscordId, getPortfolioStats, formatSubscriptionStatus } = require('../database/mysql');

/**
 * Comando /minhas-contas - Dashboard do Portfólio de Contas
 * 
 * Filosofia do Sistema de Portfólio:
 * - Um Discord ID pode vincular múltiplas contas Minecraft
 * - Cada conta tem assinatura individual (1-para-1)
 * - Interface elegante para gerenciar todas as contas
 * - Botões interativos para ações rápidas
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('minhas-contas')
        .setDescription('📋 Visualizar e gerenciar seu portfólio de contas do Prime League'),

    async execute(interaction) {
        const discordId = interaction.user.id;

        try {
            // Defer reply para operações assíncronas
            await interaction.deferReply({ ephemeral: true });

            // Buscar portfólio do usuário
            const portfolio = await getPortfolioByDiscordId(discordId);
            
            if (!portfolio || portfolio.length === 0) {
                // Usuário sem contas vinculadas
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B') // Vermelho suave
                    .setTitle('📭 Portfólio Vazio')
                    .setDescription(
                        '**Você ainda não possui contas vinculadas.**\n\n' +
                        '🎮 Para vincular sua primeira conta:\n' +
                        '• Use `/registrar <seu_nick>` para começar\n' +
                        '• Siga as instruções de verificação in-game\n' +
                        '• Adquira sua assinatura com `/renovar`\n\n' +
                        '🔗 **Vantagens do Sistema de Portfólio:**\n' +
                        '• Gerencie múltiplas contas de um lugar\n' +
                        '• Assinaturas individuais por conta\n' +
                        '• Interface intuitiva no Discord'
                    )
                    .setThumbnail('https://mc-heads.net/avatar/MHF_Question.png')
                    .setFooter({ 
                        text: 'Prime League • Sistema de Portfólio',
                        iconURL: interaction.client.user.displayAvatarURL()
                    })
                    .setTimestamp();

                const newAccountButton = new ActionRowBuilder()
                    .addComponents(
                        new ButtonBuilder()
                            .setCustomId('portfolio_register_new')
                            .setLabel('🔗 Vincular Nova Conta')
                            .setStyle(ButtonStyle.Primary)
                    );

                return await interaction.editReply({
                    embeds: [embed],
                    components: [newAccountButton]
                });
            }

            // Construir embed do portfólio
            const embed = await buildPortfolioEmbed(interaction.user, portfolio);
            const actionButtons = buildActionButtons(portfolio);

            await interaction.editReply({
                embeds: [embed],
                components: [actionButtons]
            });

        } catch (error) {
            console.error('[MINHAS-CONTAS] Erro ao processar comando:', error);

            const errorEmbed = new EmbedBuilder()
                .setColor('#FF3333')
                .setTitle('❌ Erro Interno')
                .setDescription(
                    'Houve um erro ao carregar seu portfólio.\n' +
                    'Tente novamente em instantes.'
                )
                .setTimestamp();

            await interaction.editReply({
                embeds: [errorEmbed],
                components: []
            });
        }
    }
};

/**
 * Constrói o embed principal do portfólio.
 */
async function buildPortfolioEmbed(user, portfolio) {
    // Estatísticas do portfólio
    const totalAccounts = portfolio.length;
    const activeSubscriptions = portfolio.filter(acc => acc.subscription_status === 'ACTIVE').length;
    const expiredSubscriptions = portfolio.filter(acc => acc.subscription_status === 'EXPIRED').length;
    const neverSubscribed = portfolio.filter(acc => acc.subscription_status === 'NEVER_SUBSCRIBED').length;
    
    // Identificar conta primária
    const primaryAccount = portfolio.find(acc => acc.is_primary === 1) || portfolio[0];

    // Header do embed
    const embed = new EmbedBuilder()
        .setColor('#4CAF50') // Verde do Prime League
        .setTitle('📋 Seu Portfólio de Contas - Prime League')
        .setDescription(
            `**Proprietário:** ${user.displayName}\n` +
            `**Discord:** ${user.tag}\n\n` +
            `📊 **Resumo do Portfólio:**\n` +
            `• **${totalAccounts}** contas vinculadas\n` +
            `• **${activeSubscriptions}** assinaturas ativas\n` +
            `• **${expiredSubscriptions}** assinaturas expiradas\n` +
            `• **${neverSubscribed}** nunca assinaram\n\n` +
            '━━━━━━━━━━━━━━━━━━━━━━━━'
        )
        .setThumbnail(`https://mc-heads.net/avatar/${primaryAccount.player_name}.png`)
        .setFooter({ 
            text: `Prime League • ${totalAccounts} conta${totalAccounts !== 1 ? 's' : ''} vinculada${totalAccounts !== 1 ? 's' : ''}`,
            iconURL: 'https://mc-heads.net/avatar/MHF_Chest.png'
        })
        .setTimestamp();

    // Adicionar cada conta como field
    portfolio.forEach((account, index) => {
        const isPrimary = account.is_primary === 1;
        const status = formatAccountStatus(account);
        const daysInfo = getDaysInfo(account);

        embed.addFields({
            name: `${index + 1}. ${account.player_name}${isPrimary ? ' 👑' : ''}`,
            value: 
                `**Status:** ${status.emoji} ${status.text}\n` +
                `**${daysInfo.label}:** ${daysInfo.value}\n` +
                `**Vinculado em:** <t:${Math.floor(new Date(account.linked_at).getTime() / 1000)}:d>`,
            inline: true
        });
    });

    return embed;
}

/**
 * Constrói os botões de ação do portfólio.
 */
function buildActionButtons(portfolio) {
    const actionRow = new ActionRowBuilder();

    // Botão para vincular nova conta
    actionRow.addComponents(
        new ButtonBuilder()
            .setCustomId('portfolio_add_account')
            .setLabel('🔗 Vincular Nova Conta')
            .setStyle(ButtonStyle.Primary)
    );

    // Botão para renovar (se houver contas expiradas)
    const expiredAccounts = portfolio.filter(acc => 
        acc.subscription_status === 'EXPIRED' || acc.subscription_status === 'NEVER_SUBSCRIBED'
    );
    
    if (expiredAccounts.length > 0) {
        actionRow.addComponents(
            new ButtonBuilder()
                .setCustomId('portfolio_renew_subscription')
                .setLabel('💎 Renovar Assinatura')
                .setStyle(ButtonStyle.Success)
        );
    }

    // Botão para remover conta (se houver mais de 1)
    if (portfolio.length > 1) {
        actionRow.addComponents(
            new ButtonBuilder()
                .setCustomId('portfolio_remove_account')
                .setLabel('🗑️ Remover Conta')
                .setStyle(ButtonStyle.Danger)
        );
    }

    // Botão de atualizar
    actionRow.addComponents(
        new ButtonBuilder()
            .setCustomId('portfolio_refresh')
            .setLabel('🔄 Atualizar')
            .setStyle(ButtonStyle.Secondary)
    );

    return actionRow;
}

/**
 * Formatar status da conta com emoji e texto.
 */
function formatAccountStatus(account) {
    switch (account.subscription_status) {
        case 'ACTIVE':
            return {
                emoji: '🟢',
                text: 'Assinatura Ativa'
            };
        case 'EXPIRED':
            return {
                emoji: '🔴',
                text: 'Assinatura Expirada'
            };
        case 'NEVER_SUBSCRIBED':
            return {
                emoji: '⚪',
                text: 'Nunca Assinou'
            };
        default:
            return {
                emoji: '⚫',
                text: 'Status Desconhecido'
            };
    }
}

/**
 * Obter informações de dias (restantes ou desde expiração).
 */
function getDaysInfo(account) {
    switch (account.subscription_status) {
        case 'ACTIVE':
            return {
                label: 'Expira em',
                value: `${account.days_remaining} dia${account.days_remaining !== 1 ? 's' : ''}`
            };
        case 'EXPIRED':
            // Calcular há quantos dias expirou
            const now = new Date();
            const expiredDate = new Date(account.subscription_expires_at);
            const daysSinceExpired = Math.floor((now - expiredDate) / (1000 * 60 * 60 * 24));
            return {
                label: 'Expirou há',
                value: `${daysSinceExpired} dia${daysSinceExpired !== 1 ? 's' : ''}`
            };
        case 'NEVER_SUBSCRIBED':
            return {
                label: 'Status',
                value: 'Nunca adquiriu assinatura'
            };
        default:
            return {
                label: 'Status',
                value: 'Informação indisponível'
            };
    }
}
