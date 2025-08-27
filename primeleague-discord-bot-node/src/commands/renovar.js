const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, SelectMenuBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const {
    getClanSubscription,
    getSubscriptionTiers,
    getDiscordLinksById,
    getClanMemberCount
} = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('renovar')
        .setDescription('Renova ou faz upgrade da assinatura do seu clã'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;

            // 1. Buscar informações do clã
            const [subscription, availableTiers, clanMembers] = await Promise.all([
                getClanSubscription(discordId),
                getSubscriptionTiers(),
                getDiscordLinksById(discordId)
            ]);

            // 2. Verificar se tem clã registrado
            if (!clanMembers || clanMembers.length === 0) {
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('❌ Clã Não Registrado')
                    .setDescription(
                        'Você ainda não possui um clã registrado.\n\n' +
                        '**Como começar:**\n' +
                        '1. Use `/registrar <nickname>` para vincular sua primeira conta\n' +
                        '2. Complete a verificação no servidor\n' +
                        '3. Retorne aqui para escolher um plano de assinatura'
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 3. Determinar situação atual
            const hasActiveSubscription = subscription && new Date(subscription.expires_at) > new Date();
            const currentTier = subscription?.subscription_tier;
            const memberCount = clanMembers.length;

            // 4. Criar embed principal
            const embed = new EmbedBuilder()
                .setColor('#4ECDC4')
                .setTitle('💎 Sistema de Assinaturas Prime League')
                .setDescription(
                    `**Líder do Clã:** ${interaction.user}\n` +
                    `**Membros Vinculados:** ${memberCount} conta(s)\n\n` +
                    '**Escolha o plano ideal para seu clã:**'
                )
                .setTimestamp();

            // 5. Adicionar status atual se houver assinatura
            if (hasActiveSubscription) {
                const expiresAt = new Date(subscription.expires_at);
                const daysRemaining = Math.ceil((expiresAt - new Date()) / (1000 * 60 * 60 * 24));
                
                embed.addFields({
                    name: '📋 Assinatura Atual',
                    value: 
                        `**Plano:** ${currentTier}\n` +
                        `**Slots:** ${subscription.max_simultaneous_logins}\n` +
                        `**Expira em:** ${daysRemaining} dias\n` +
                        `**Renovação:** Estende por +30 dias`,
                    inline: false
                });
            }

            // 6. Adicionar descrição dos planos
            let plansDescription = '';
            availableTiers.forEach(tier => {
                const isCurrentPlan = currentTier === tier.tier_name;
                const planIcon = isCurrentPlan ? '👑' : '💰';
                const statusText = isCurrentPlan ? ' *(Plano Atual)*' : '';
                
                plansDescription += 
                    `${planIcon} **${tier.display_name}**${statusText}\n` +
                    `└ ${tier.max_slots} slots simultâneos • R$ ${tier.price_brl.toFixed(2)}\n` +
                    `└ ${tier.description}\n\n`;
            });

            embed.addFields({
                name: '💎 Planos Disponíveis',
                value: plansDescription,
                inline: false
            });

            // 7. Informações adicionais
            embed.addFields({
                name: '📝 Informações Importantes',
                value: 
                    '• **Slots Simultâneos:** Quantos membros podem estar online ao mesmo tempo\n' +
                    '• **Duração:** Todas as assinaturas são válidas por 30 dias\n' +
                    '• **Upgrade:** Você pode fazer upgrade a qualquer momento\n' +
                    '• **Pagamento:** PIX, cartão de crédito e outros métodos disponíveis\n' +
                    '• **Ativação:** Imediata após confirmação do pagamento',
                inline: false
            });

            // 8. Criar menu de seleção de planos
            const selectMenu = new SelectMenuBuilder()
                .setCustomId('select_subscription_tier')
                .setPlaceholder('Escolha um plano de assinatura')
                .addOptions(
                    availableTiers.map(tier => {
                        const isCurrentPlan = currentTier === tier.tier_name;
                        const label = isCurrentPlan ? 
                            `${tier.display_name} (Renovar)` : 
                            `${tier.display_name} - R$ ${tier.price_brl.toFixed(2)}`;
                        
                        return {
                            label: label,
                            description: `${tier.max_slots} slots • ${tier.description}`,
                            value: tier.tier_name,
                            emoji: isCurrentPlan ? '👑' : '💎'
                        };
                    })
                );

            const selectRow = new ActionRowBuilder().addComponents(selectMenu);

            // 9. Criar botões adicionais
            const buttons = new ActionRowBuilder();
            
            if (hasActiveSubscription) {
                buttons.addComponents(
                    new ButtonBuilder()
                        .setCustomId('view_clan_details')
                        .setLabel('📊 Detalhes do Clã')
                        .setStyle(ButtonStyle.Secondary),
                    new ButtonBuilder()
                        .setCustomId('subscription_history')
                        .setLabel('📜 Histórico')
                        .setStyle(ButtonStyle.Secondary)
                );
            } else {
                buttons.addComponents(
                    new ButtonBuilder()
                        .setCustomId('subscription_help')
                        .setLabel('❓ Ajuda')
                        .setStyle(ButtonStyle.Secondary),
                    new ButtonBuilder()
                        .setCustomId('contact_support')
                        .setLabel('💬 Suporte')
                        .setStyle(ButtonStyle.Secondary)
                );
            }

            // 10. Recomendação inteligente
            let recommendation = '';
            if (memberCount <= 1) {
                recommendation = '💡 **Recomendado:** Plano Lutador para uso individual';
            } else if (memberCount <= 5) {
                recommendation = '💡 **Recomendado:** Plano Esquadrão para seu grupo';
            } else if (memberCount <= 10) {
                recommendation = '💡 **Recomendado:** Plano Guilda para clãs médios';
            } else {
                recommendation = '💡 **Recomendado:** Plano Império para grandes organizações';
            }

            embed.setFooter({ text: recommendation });

            // 11. Enviar resposta
            await interaction.editReply({ 
                embeds: [embed], 
                components: [selectRow, buttons]
            });

        } catch (error) {
            console.error('Erro no comando /renovar:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro Interno')
                .setDescription(
                    'Ocorreu um erro ao carregar os planos de assinatura.\n\n' +
                    'Tente novamente em instantes ou entre em contato com a administração.'
                )
                .setTimestamp();

            return interaction.editReply({ embeds: [errorEmbed] });
        }
    },
};