const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getDonorInfoFromCore, getPortfolioByDiscordId } = require('../database/mysql');

/**
 * Comando /upgrade-doador - Sistema de Upgrade de Níveis de Doador
 * 
 * Permite que usuários vejam informações sobre seu nível atual
 * e como fazer upgrade para obter mais benefícios.
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('upgrade-doador')
        .setDescription('💎 Ver informações sobre seu nível de doador e como fazer upgrade'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;

            // 1. Buscar informações atuais do usuário
            const [donorInfo, portfolio] = await Promise.all([
                getDonorInfoFromCore(discordId),
                getPortfolioByDiscordId(discordId)
            ]);

            // Verificar se há erro na API
            if (donorInfo.error) {
                const config = require('../../bot-config.json');
                const errorEmbed = new EmbedBuilder()
                    .setColor(config.ui.colors.error)
                    .setTitle('❌ Erro de Conexão')
                    .setDescription(donorInfo.message)
                    .setFooter(config.ui.embeds.footer)
                    .setTimestamp();

                return interaction.editReply({ embeds: [errorEmbed] });
            }

            // 2. Carregar configuração centralizada
            const config = require('../../bot-config.json');
            const availableTiers = config.donorTiers.fallback;

            // 3. Criar embed principal
            const embed = new EmbedBuilder()
                .setColor('#FFD700') // Dourado
                .setTitle('💎 Sistema de Apoiadores - Prime League')
                .setDescription(
                    `**Jogador:** ${interaction.user}\n` +
                    `**Nível Atual:** ${donorInfo.donorName} (Tier ${donorInfo.donorTier})\n` +
                    `**Contas Vinculadas:** ${portfolio.length}/${donorInfo.maxAccounts}\n\n` +
                    '**Escolha o nível ideal para você:**'
                )
                .setTimestamp();

            // 4. Adicionar informações de cada nível
            Object.entries(availableTiers).forEach(([tierId, tier]) => {
                const isCurrentTier = parseInt(tierId) === donorInfo.donorTier;
                const canUpgrade = parseInt(tierId) > donorInfo.donorTier;
                const tierColor = isCurrentTier ? '🟢' : canUpgrade ? '🟡' : '⚪';
                const tierStatus = isCurrentTier ? ' *(Atual)*' : canUpgrade ? ' *(Disponível)*' : ' *(Inferior)*';

                embed.addFields({
                    name: `${tierColor} ${tier.name}${tierStatus}`,
                    value: 
                        `**Preço:** ${tier.price}\n` +
                        `**Contas:** ${tier.maxAccounts} simultâneas\n` +
                        `**Descrição:** ${tier.description}\n\n` +
                        `**Benefícios:**\n${tier.benefits.join('\n')}`,
                    inline: false
                });
            });

            // 5. Adicionar informações sobre upgrade
            const nextTier = availableTiers[donorInfo.donorTier + 1];
            if (nextTier) {
                embed.addFields({
                    name: '🚀 Próximo Upgrade Disponível',
                    value: 
                        `**Para:** ${nextTier.name}\n` +
                        `**Preço:** ${nextTier.price}\n` +
                        `**Novos Benefícios:**\n` +
                        `• ${nextTier.maxAccounts - donorInfo.maxAccounts} conta(s) adicional(is)\n` +
                        `• ${nextTier.benefits.filter(b => !availableTiers[donorInfo.donorTier].benefits.includes(b)).join('\n• ')}`,
                    inline: false
                });
            }

            // 6. Criar botões de ação
            const buttons = new ActionRowBuilder();

            if (nextTier) {
                buttons.addComponents(
                    new ButtonBuilder()
                        .setCustomId(`upgrade_to_${donorInfo.donorTier + 1}`)
                        .setLabel(`🚀 Upgrade para ${nextTier.name}`)
                        .setStyle(ButtonStyle.Primary)
                );
            }

            buttons.addComponents(
                new ButtonBuilder()
                    .setCustomId('donor_help')
                    .setLabel('❓ Ajuda')
                    .setStyle(ButtonStyle.Secondary),
                new ButtonBuilder()
                    .setCustomId('contact_support')
                    .setLabel('💬 Suporte')
                    .setStyle(ButtonStyle.Secondary)
            );

            // 7. Enviar resposta
            await interaction.editReply({ 
                embeds: [embed], 
                components: [buttons]
            });

        } catch (error) {
            console.error('Erro no comando /upgrade-doador:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro Interno')
                .setDescription(
                    'Ocorreu um erro ao carregar informações de doador.\n\n' +
                    'Tente novamente em instantes ou entre em contato com a administração.'
                )
                .setTimestamp();

            return interaction.editReply({ embeds: [errorEmbed] });
        }
    },
};
