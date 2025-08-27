const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const { 
    getPortfolioByDiscordId, 
    getPortfolioStats, 
    formatSubscriptionStatus,
    getDonorInfoFromCore 
} = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('conta')
        .setDescription('Visualiza informações completas da sua conta e portfólio'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;
            const config = require('../../bot-config.json');

            // Buscar informações do portfólio e doador
            const [portfolio, portfolioStats, donorInfo] = await Promise.all([
                getPortfolioByDiscordId(discordId),
                getPortfolioStats(discordId),
                getDonorInfoFromCore(discordId)
            ]);

            // Verificar se há erro na API
            if (donorInfo.error) {
                const errorEmbed = new EmbedBuilder()
                    .setColor(config.ui.colors.error)
                    .setTitle('❌ Erro de Conexão')
                    .setDescription(donorInfo.message)
                    .setFooter(config.ui.embeds.footer)
                    .setTimestamp();

                return interaction.editReply({ embeds: [errorEmbed] });
            }

            // Criar embed principal
            const embed = new EmbedBuilder()
                .setColor(config.ui.colors.info)
                .setTitle('💎 Sua Conta Prime League')
                .setDescription(`**Discord:** ${interaction.user}\n**Nível:** ${donorInfo.donorName}`)
                .setFooter(config.ui.embeds.footer)
                .setTimestamp();

            // Informações do doador
            embed.addFields({
                name: '👑 Nível de Apoiador',
                value: 
                    `**${donorInfo.donorName}**\n` +
                    `**Contas Vinculadas:** ${donorInfo.currentAccounts}/${donorInfo.maxAccounts}\n` +
                    `**Fonte:** ${donorInfo.source === 'api' ? '🟢 API Core' : '🟡 Fallback'}`,
                inline: true
            });

            // Estatísticas do portfólio
            embed.addFields({
                name: '📊 Estatísticas',
                value: 
                    `**Total de Contas:** ${portfolioStats.total_accounts}\n` +
                    `**Assinaturas Ativas:** ${portfolioStats.active_subscriptions}\n` +
                    `**Assinaturas Expiradas:** ${portfolioStats.expired_subscriptions}`,
                inline: true
            });

            // Status geral
            const statusColor = portfolioStats.active_subscriptions > 0 ? config.ui.colors.success : config.ui.colors.warning;
            const statusText = portfolioStats.active_subscriptions > 0 ? '🟢 Ativo' : '🟡 Inativo';
            
            embed.addFields({
                name: '📈 Status Geral',
                value: statusText,
                inline: true
            });

            // Lista de contas (se houver)
            if (portfolio && portfolio.length > 0) {
                const primaryAccount = portfolio.find(acc => acc.is_primary);
                
                if (primaryAccount) {
                    embed.setThumbnail(`https://mc-heads.net/avatar/${primaryAccount.player_name}.png`);
                }

                let accountsList = '';
                portfolio.forEach((account, index) => {
                    const isPrimary = account.is_primary;
                    const status = formatSubscriptionStatus(account.subscription_status, account.days_remaining);
                    const primaryIcon = isPrimary ? '👑' : '👤';
                    
                    accountsList += `${primaryIcon} **${account.player_name}**${isPrimary ? ' *(Principal)*' : ''}\n`;
                    accountsList += `└ ${status}\n`;
                    
                    if (index < portfolio.length - 1) {
                        accountsList += '\n';
                    }
                });

                embed.addFields({
                    name: '🎮 Contas Vinculadas',
                    value: accountsList,
                    inline: false
                });
            } else {
                embed.addFields({
                    name: '🎮 Contas Vinculadas',
                    value: 'Nenhuma conta vinculada ainda.\nUse `/registrar <nickname>` para vincular sua primeira conta.',
                    inline: false
                });
            }

            // Informações adicionais
            embed.addFields({
                name: '💡 Comandos Úteis',
                value: 
                    '• `/registrar <nickname>` - Vincular nova conta\n' +
                    '• `/upgrade-doador` - Ver opções de upgrade\n' +
                    '• `/primeira-conta` - Assinatura individual',
                inline: false
            });

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            console.error('Erro no comando /conta:', error);
            
            const config = require('../../bot-config.json');
            const errorEmbed = new EmbedBuilder()
                .setColor(config.ui.colors.error)
                .setTitle('❌ Erro Interno')
                .setDescription('Ocorreu um erro ao carregar suas informações. Tente novamente.')
                .setFooter(config.ui.embeds.footer)
                .setTimestamp();

            await interaction.editReply({ embeds: [errorEmbed] });
        }
    },
};
