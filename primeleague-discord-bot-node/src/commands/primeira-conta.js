const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getPortfolioByDiscordId, getPlayerAccountInfo } = require('../database/mysql');

/**
 * Comando /primeira-conta - Gerenciamento da Primeira Conta Individual
 * 
 * Este comando é específico para usuários que acabaram de verificar sua primeira conta
 * e precisam adquirir uma assinatura individual (não de clã).
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('primeira-conta')
        .setDescription('💎 Adquirir assinatura para sua primeira conta verificada'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;

            // 1. Verificar se o usuário tem contas verificadas
            const portfolio = await getPortfolioByDiscordId(discordId);
            
            if (!portfolio || portfolio.length === 0) {
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('❌ Nenhuma Conta Verificada')
                    .setDescription(
                        'Você ainda não possui contas verificadas.\n\n' +
                        '**Como começar:**\n' +
                        '1. Use `/registrar <nickname>` para vincular sua conta\n' +
                        '2. Complete a verificação no servidor Minecraft\n' +
                        '3. Retorne aqui para adquirir sua assinatura'
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 2. Verificar se já tem assinatura ativa
            const hasActiveSubscription = portfolio.some(account => 
                account.subscription_status === 'ACTIVE'
            );

            if (hasActiveSubscription) {
                const embed = new EmbedBuilder()
                    .setColor('#4ECDC4')
                    .setTitle('✅ Assinatura Ativa')
                    .setDescription(
                        'Você já possui uma assinatura ativa!\n\n' +
                        '**Para gerenciar suas contas:**\n' +
                        '• Use `/minhas-contas` para ver seu portfólio\n' +
                        '• Use `/renovar` para gerenciar assinaturas de clã\n\n' +
                        '**Para adicionar mais contas:**\n' +
                        '• Use `/registrar <nickname>` para novas contas'
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 3. Mostrar planos individuais
            const embed = new EmbedBuilder()
                .setColor('#FFD93D')
                .setTitle('💎 Assinatura Individual - Prime League')
                .setDescription(
                    `**Jogador:** ${interaction.user}\n` +
                    `**Contas Verificadas:** ${portfolio.length}\n\n` +
                    '**Escolha o plano ideal para você:**'
                )
                .addFields([
                    {
                        name: '🎮 Plano Básico',
                        value: 
                            '**Preço:** R$ 9,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Acesso completo ao servidor\n' +
                            '• 1 conta simultânea\n' +
                            '• Suporte básico\n' +
                            '• Sem anúncios',
                        inline: true
                    },
                    {
                        name: '⚡ Plano Premium',
                        value: 
                            '**Preço:** R$ 19,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Tudo do Básico\n' +
                            '• 2 contas simultâneas\n' +
                            '• Prioridade no suporte\n' +
                            '• Cores especiais no chat',
                        inline: true
                    },
                    {
                        name: '👑 Plano VIP',
                        value: 
                            '**Preço:** R$ 39,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Tudo do Premium\n' +
                            '• 5 contas simultâneas\n' +
                            '• Suporte VIP 24/7\n' +
                            '• Comandos especiais',
                        inline: true
                    }
                ])
                .addFields({
                    name: '📝 Informações Importantes',
                    value: 
                        '• **Duração:** Todas as assinaturas são válidas por 30 dias\n' +
                        '• **Pagamento:** PIX, cartão de crédito e outros métodos\n' +
                        '• **Ativação:** Imediata após confirmação do pagamento\n' +
                        '• **Cancelamento:** A qualquer momento\n' +
                        '• **Suporte:** Entre em contato conosco para dúvidas',
                    inline: false
                })
                .setFooter({ 
                    text: '💡 Recomendado: Plano Básico para começar',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();

            // 4. Criar botões de ação
            const buttons = new ActionRowBuilder()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId('subscribe_basic')
                        .setLabel('🎮 Plano Básico - R$ 9,90')
                        .setStyle(ButtonStyle.Primary),
                    new ButtonBuilder()
                        .setCustomId('subscribe_premium')
                        .setLabel('⚡ Plano Premium - R$ 19,90')
                        .setStyle(ButtonStyle.Primary),
                    new ButtonBuilder()
                        .setCustomId('subscribe_vip')
                        .setLabel('👑 Plano VIP - R$ 39,90')
                        .setStyle(ButtonStyle.Primary)
                );

            const supportButtons = new ActionRowBuilder()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId('subscription_help')
                        .setLabel('❓ Ajuda')
                        .setStyle(ButtonStyle.Secondary),
                    new ButtonBuilder()
                        .setCustomId('contact_support')
                        .setLabel('💬 Suporte')
                        .setStyle(ButtonStyle.Secondary),
                    new ButtonBuilder()
                        .setCustomId('view_portfolio')
                        .setLabel('📋 Meu Portfólio')
                        .setStyle(ButtonStyle.Secondary)
                );

            // 5. Enviar resposta
            await interaction.editReply({ 
                embeds: [embed], 
                components: [buttons, supportButtons]
            });

        } catch (error) {
            console.error('Erro no comando /primeira-conta:', error);
            
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
