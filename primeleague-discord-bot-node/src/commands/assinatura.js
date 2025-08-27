const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getPortfolioByDiscordId, getPlayerAccountInfo } = require('../database/mysql');

/**
 * Comando /assinatura - Gerenciamento de Assinaturas
 * 
 * Este comando permite que usuários adquiram e gerenciem suas assinaturas
 * para acessar o servidor Prime League.
 */

module.exports = {
    data: new SlashCommandBuilder()
        .setName('assinatura')
        .setDescription('💎 Adquirir ou gerenciar sua assinatura do Prime League'),

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
                // Buscar informações detalhadas da assinatura compartilhada
                const activeAccount = portfolio.find(account => account.subscription_status === 'ACTIVE');
                const daysRemaining = activeAccount ? activeAccount.days_remaining : 0;
                const expiryDate = activeAccount ? new Date(activeAccount.subscription_expires_at) : null;
                
                const embed = new EmbedBuilder()
                    .setColor('#4ECDC4')
                    .setTitle('✅ Assinatura Compartilhada Ativa')
                    .setDescription(
                        'Você já possui uma **assinatura compartilhada** ativa!\n\n' +
                        '**📊 Status da Assinatura:**\n' +
                        `• **Status:** Ativa\n` +
                        `• **Dias Restantes:** ${daysRemaining} dias\n` +
                        `• **Expira em:** ${expiryDate ? expiryDate.toLocaleDateString('pt-BR') : 'N/A'}\n\n` +
                        '**🎮 Contas Vinculadas:**\n' +
                        `• **Total:** ${portfolio.length} conta${portfolio.length > 1 ? 's' : ''}\n` +
                        portfolio.map(acc => `• ${acc.player_name} (${acc.subscription_status === 'ACTIVE' ? '✅' : '❌'})`).join('\n') + '\n\n' +
                        '**💡 Como Funciona:**\n' +
                        '• Sua assinatura é **compartilhada** entre todas as contas\n' +
                        '• Todas as contas vinculadas têm acesso ao servidor\n' +
                        '• Você pode adicionar mais contas sem custo adicional'
                    )
                    .addFields({
                        name: '🔧 Ações Disponíveis',
                        value: 
                            '• `/conta` - Gerenciar seu portfólio completo\n' +
                            '• `/registrar <nickname>` - Adicionar mais contas\n' +
                            '• `/upgrade-doador` - Ver opções de upgrade\n' +
                            '• `/assinatura` - Renovar quando necessário',
                        inline: false
                    })
                    .setFooter({ 
                        text: '💡 Sua assinatura é compartilhada automaticamente com todas as contas vinculadas',
                        iconURL: interaction.client.user.displayAvatarURL()
                    })
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 3. Mostrar planos de assinatura compartilhada
            const embed = new EmbedBuilder()
                .setColor('#FFD93D')
                .setTitle('💎 Assinatura Compartilhada - Prime League')
                .setDescription(
                    `**Jogador:** ${interaction.user}\n` +
                    `**Contas Verificadas:** ${portfolio.length}\n\n` +
                    '**🎯 Sistema de Assinatura Compartilhada:**\n' +
                    '• Uma única assinatura para **todas** suas contas\n' +
                    '• Economia e simplicidade\n' +
                    '• Adicione quantas contas quiser sem custo extra\n\n' +
                    '**Escolha o plano ideal para você:**'
                )
                .addFields([
                    {
                        name: '🎮 Plano Básico',
                        value: 
                            '**Preço:** R$ 9,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Acesso completo ao servidor\n' +
                            '• **Todas** suas contas vinculadas\n' +
                            '• Suporte básico\n' +
                            '• Sem anúncios\n' +
                            '• **Compartilhado** automaticamente',
                        inline: true
                    },
                    {
                        name: '⚡ Plano Premium',
                        value: 
                            '**Preço:** R$ 19,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Tudo do Básico\n' +
                            '• **Todas** suas contas vinculadas\n' +
                            '• Prioridade no suporte\n' +
                            '• Cores especiais no chat\n' +
                            '• **Compartilhado** automaticamente',
                        inline: true
                    },
                    {
                        name: '👑 Plano VIP',
                        value: 
                            '**Preço:** R$ 39,90/mês\n' +
                            '**Benefícios:**\n' +
                            '• Tudo do Premium\n' +
                            '• **Todas** suas contas vinculadas\n' +
                            '• Suporte VIP 24/7\n' +
                            '• Comandos especiais\n' +
                            '• **Compartilhado** automaticamente',
                        inline: true
                    }
                ])
                .addFields({
                    name: '📝 Informações Importantes',
                    value: 
                        '• **Duração:** Todas as assinaturas são válidas por 30 dias\n' +
                        '• **Compartilhamento:** Uma assinatura para **todas** suas contas\n' +
                        '• **Pagamento:** PIX, cartão de crédito e outros métodos\n' +
                        '• **Ativação:** Imediata após confirmação do pagamento\n' +
                        '• **Cancelamento:** A qualquer momento\n' +
                        '• **Suporte:** Entre em contato conosco para dúvidas',
                    inline: false
                })
                .setFooter({ 
                    text: '💡 Sistema Compartilhado: Uma assinatura para todas suas contas!',
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
