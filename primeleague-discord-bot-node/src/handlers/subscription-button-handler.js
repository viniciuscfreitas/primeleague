const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { renewAccountSubscription, getPortfolioByDiscordId, getPortfolioStats, checkSubscriptionStatus } = require('../database/mysql');

/**
 * Handler para botões de simulação de assinatura
 * Simula o processo de pagamento e ativação de assinaturas
 */
class SubscriptionButtonHandler {
    
    /**
     * Processa interações de botões de assinatura
     */
    static async handleSubscriptionButton(interaction) {
        const { customId } = interaction;
        
        try {
            // Deferir resposta para evitar timeout
            await interaction.deferUpdate();
            
            switch (customId) {
                case 'subscribe_basic':
                    return await this.handleBasicSubscription(interaction);
                    
                case 'subscribe_premium':
                    return await this.handlePremiumSubscription(interaction);
                    
                case 'subscribe_vip':
                    return await this.handleVipSubscription(interaction);
                    
                case 'subscription_help':
                    return await this.handleSubscriptionHelp(interaction);
                    
                case 'contact_support':
                    return await this.handleContactSupport(interaction);
                    
                case 'view_portfolio':
                    return await this.handleViewPortfolio(interaction);
                    
                default:
                    return false; // Botão não tratado
            }
            
        } catch (error) {
            console.error(`[SubscriptionButton] Erro ao processar botão ${customId}:`, error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro na Simulação')
                .setDescription('Ocorreu um erro ao processar sua solicitação. Tente novamente.')
                .setTimestamp();
                
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
    }
    
    /**
     * Simula assinatura básica (30 dias)
     */
    static async handleBasicSubscription(interaction) {
        const discordId = interaction.user.id;
        console.log(`[SUBSCRIPTION-BUTTON] 🎮 Iniciando simulação de assinatura básica para Discord ID: ${discordId}`);
        
        // Buscar portfólio do usuário
        const portfolio = await getPortfolioByDiscordId(discordId);
        console.log(`[SUBSCRIPTION-BUTTON] 📋 Portfólio encontrado: ${portfolio ? portfolio.length : 0} contas`);
        
        if (!portfolio || portfolio.length === 0) {
            console.log(`[SUBSCRIPTION-BUTTON] ❌ Nenhuma conta vinculada para Discord ID: ${discordId}`);
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Nenhuma Conta Vinculada')
                .setDescription('Você precisa ter pelo menos uma conta vinculada antes de adquirir uma assinatura.\n\nUse `/registrar <nickname>` para vincular sua conta.')
                .setTimestamp();
                
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
        
        // Mostrar detalhes do portfólio
        portfolio.forEach((acc, index) => {
            console.log(`[SUBSCRIPTION-BUTTON] 📋 Conta ${index + 1}: ${acc.player_name} (ID: ${acc.player_id}, Principal: ${acc.is_primary})`);
        });
        
        // Simular processamento
        const processingEmbed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⏳ Processando Pagamento...')
            .setDescription('Simulando processamento do pagamento...')
            .addFields({
                name: '📋 Detalhes da Simulação',
                value: 
                    '**Plano:** Básico\n' +
                    '**Valor:** R$ 9,90\n' +
                    '**Duração:** 30 dias\n' +
                    '**Conta:** ' + portfolio[0].player_name,
                inline: false
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [processingEmbed] });
        
        // Simular delay de processamento
        console.log(`[SUBSCRIPTION-BUTTON] ⏳ Aguardando 2 segundos para simular processamento...`);
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Ativar assinatura na conta principal
        const primaryAccount = portfolio.find(acc => acc.is_primary) || portfolio[0];
        console.log(`[SUBSCRIPTION-BUTTON] 🎯 Conta selecionada para ativação: ${primaryAccount.player_name} (ID: ${primaryAccount.player_id})`);
        
        console.log(`[SUBSCRIPTION-BUTTON] 🔄 Chamando renewAccountSubscription...`);
        const result = await renewAccountSubscription(primaryAccount.player_id, 30);
        
        console.log(`[SUBSCRIPTION-BUTTON] 📊 Resultado da renovação:`, result);
        
        if (result.success) {
            console.log(`[SUBSCRIPTION-BUTTON] ✅ Assinatura ativada com sucesso!`);
            console.log(`[SUBSCRIPTION-BUTTON] 📅 Nova data de expiração: ${result.newExpiry.toLocaleDateString('pt-BR')} ${result.newExpiry.toLocaleTimeString('pt-BR')}`);
            
            // Verificar se a assinatura foi persistida corretamente
            console.log(`[SUBSCRIPTION-BUTTON] 🔍 Verificando persistência da assinatura...`);
            const verification = await checkSubscriptionStatus(primaryAccount.player_id);
            
            if (verification) {
                console.log(`[SUBSCRIPTION-BUTTON] ✅ Verificação confirmada: assinatura persistida no banco`);
                console.log(`[SUBSCRIPTION-BUTTON] 📊 Status final: ${verification.subscription_status} (${verification.days_remaining} dias restantes)`);
            } else {
                console.log(`[SUBSCRIPTION-BUTTON] ⚠️ Aviso: não foi possível verificar a persistência`);
            }
            
            const successEmbed = new EmbedBuilder()
                .setColor('#00FF00')
                .setTitle('✅ Assinatura Ativada!')
                .setDescription('Sua assinatura básica foi ativada com sucesso!')
                .addFields([
                    {
                        name: '🎮 Conta Ativada',
                        value: `**${primaryAccount.player_name}**`,
                        inline: true
                    },
                    {
                        name: '📅 Válida Até',
                        value: `**${result.newExpiry.toLocaleDateString('pt-BR')}**`,
                        inline: true
                    },
                    {
                        name: '💰 Valor',
                        value: '**R$ 9,90** (simulado)',
                        inline: true
                    }
                ])
                .addFields({
                    name: '🎯 Próximos Passos',
                    value: 
                        '1. ✅ Conecte no servidor Minecraft\n' +
                        '2. 🎮 Aproveite o acesso completo\n' +
                        '3. 📋 Use `/conta` para gerenciar\n' +
                        '4. 🔄 Renove quando necessário',
                    inline: false
                })
                .setFooter({ 
                    text: '🎉 Bem-vindo ao Prime League!',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();
                
            await interaction.editReply({ embeds: [successEmbed] });
        } else {
            console.log(`[SUBSCRIPTION-BUTTON] ❌ Falha ao ativar assinatura: ${result.error}`);
            throw new Error('Falha ao ativar assinatura: ' + result.error);
        }
        
        return true;
    }
    
    /**
     * Simula assinatura premium (30 dias)
     */
    static async handlePremiumSubscription(interaction) {
        const discordId = interaction.user.id;
        
        // Buscar portfólio do usuário
        const portfolio = await getPortfolioByDiscordId(discordId);
        
        if (!portfolio || portfolio.length === 0) {
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Nenhuma Conta Vinculada')
                .setDescription('Você precisa ter pelo menos uma conta vinculada antes de adquirir uma assinatura.')
                .setTimestamp();
                
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
        
        // Simular processamento
        const processingEmbed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⏳ Processando Pagamento Premium...')
            .setDescription('Simulando processamento do pagamento premium...')
            .addFields({
                name: '📋 Detalhes da Simulação',
                value: 
                    '**Plano:** Premium\n' +
                    '**Valor:** R$ 19,90\n' +
                    '**Duração:** 30 dias\n' +
                    '**Conta:** ' + portfolio[0].player_name,
                inline: false
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [processingEmbed] });
        
        // Simular delay de processamento
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Ativar assinatura na conta principal
        const primaryAccount = portfolio.find(acc => acc.is_primary) || portfolio[0];
        const result = await renewAccountSubscription(primaryAccount.player_id, 30);
        
        if (result.success) {
            const successEmbed = new EmbedBuilder()
                .setColor('#4ECDC4')
                .setTitle('⚡ Assinatura Premium Ativada!')
                .setDescription('Sua assinatura premium foi ativada com sucesso!')
                .addFields([
                    {
                        name: '🎮 Conta Ativada',
                        value: `**${primaryAccount.player_name}**`,
                        inline: true
                    },
                    {
                        name: '📅 Válida Até',
                        value: `**${result.newExpiry.toLocaleDateString('pt-BR')}**`,
                        inline: true
                    },
                    {
                        name: '💰 Valor',
                        value: '**R$ 19,90** (simulado)',
                        inline: true
                    }
                ])
                .addFields({
                    name: '🎯 Benefícios Premium',
                    value: 
                        '• ✅ 2 contas simultâneas\n' +
                        '• ⚡ Prioridade no suporte\n' +
                        '• 🎨 Cores especiais no chat\n' +
                        '• 🎮 Acesso completo ao servidor',
                    inline: false
                })
                .setFooter({ 
                    text: '⚡ Aproveite os benefícios Premium!',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();
                
            await interaction.editReply({ embeds: [successEmbed] });
        } else {
            throw new Error('Falha ao ativar assinatura: ' + result.error);
        }
        
        return true;
    }
    
    /**
     * Simula assinatura VIP (30 dias)
     */
    static async handleVipSubscription(interaction) {
        const discordId = interaction.user.id;
        
        // Buscar portfólio do usuário
        const portfolio = await getPortfolioByDiscordId(discordId);
        
        if (!portfolio || portfolio.length === 0) {
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Nenhuma Conta Vinculada')
                .setDescription('Você precisa ter pelo menos uma conta vinculada antes de adquirir uma assinatura.')
                .setTimestamp();
                
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
        
        // Simular processamento
        const processingEmbed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⏳ Processando Pagamento VIP...')
            .setDescription('Simulando processamento do pagamento VIP...')
            .addFields({
                name: '📋 Detalhes da Simulação',
                value: 
                    '**Plano:** VIP\n' +
                    '**Valor:** R$ 39,90\n' +
                    '**Duração:** 30 dias\n' +
                    '**Conta:** ' + portfolio[0].player_name,
                inline: false
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [processingEmbed] });
        
        // Simular delay de processamento
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Ativar assinatura na conta principal
        const primaryAccount = portfolio.find(acc => acc.is_primary) || portfolio[0];
        const result = await renewAccountSubscription(primaryAccount.player_id, 30);
        
        if (result.success) {
            const successEmbed = new EmbedBuilder()
                .setColor('#FFD700')
                .setTitle('👑 Assinatura VIP Ativada!')
                .setDescription('Sua assinatura VIP foi ativada com sucesso!')
                .addFields([
                    {
                        name: '🎮 Conta Ativada',
                        value: `**${primaryAccount.player_name}**`,
                        inline: true
                    },
                    {
                        name: '📅 Válida Até',
                        value: `**${result.newExpiry.toLocaleDateString('pt-BR')}**`,
                        inline: true
                    },
                    {
                        name: '💰 Valor',
                        value: '**R$ 39,90** (simulado)',
                        inline: true
                    }
                ])
                .addFields({
                    name: '👑 Benefícios VIP',
                    value: 
                        '• ✅ 5 contas simultâneas\n' +
                        '• 🏆 Suporte VIP 24/7\n' +
                        '• 🎨 Comandos especiais\n' +
                        '• 🎮 Acesso completo ao servidor\n' +
                        '• ⭐ Status VIP exclusivo',
                    inline: false
                })
                .setFooter({ 
                    text: '👑 Bem-vindo ao clube VIP!',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();
                
            await interaction.editReply({ embeds: [successEmbed] });
        } else {
            throw new Error('Falha ao ativar assinatura: ' + result.error);
        }
        
        return true;
    }
    
    /**
     * Mostra ajuda sobre assinaturas
     */
    static async handleSubscriptionHelp(interaction) {
        const helpEmbed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('❓ Ajuda - Sistema de Assinaturas')
            .setDescription('Tire suas dúvidas sobre nossos planos de assinatura.')
            .addFields([
                {
                    name: '🎮 Como Funciona',
                    value: 
                        '1. **Vincule sua conta** com `/registrar`\n' +
                        '2. **Escolha um plano** clicando nos botões\n' +
                        '3. **Confirme o pagamento** (simulado)\n' +
                        '4. **Acesse o servidor** imediatamente',
                    inline: false
                },
                {
                    name: '💰 Planos Disponíveis',
                    value: 
                        '**🎮 Básico:** R$ 9,90/mês - 1 conta\n' +
                        '**⚡ Premium:** R$ 19,90/mês - 2 contas\n' +
                        '**👑 VIP:** R$ 39,90/mês - 5 contas',
                    inline: false
                },
                {
                    name: '❓ Dúvidas Frequentes',
                    value: 
                        '• **Cancelamento:** A qualquer momento\n' +
                        '• **Renovação:** Automática (simulada)\n' +
                        '• **Suporte:** Entre em contato conosco\n' +
                        '• **Reembolso:** Em até 7 dias',
                    inline: false
                }
            ])
            .setFooter({ 
                text: '💡 Esta é uma simulação para testes',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [helpEmbed] });
        return true;
    }
    
    /**
     * Abre canal de suporte
     */
    static async handleContactSupport(interaction) {
        const supportEmbed = new EmbedBuilder()
            .setColor('#FF6B35')
            .setTitle('💬 Suporte ao Cliente')
            .setDescription('Entre em contato conosco para obter ajuda.')
            .addFields([
                {
                    name: '📧 Canais de Contato',
                    value: 
                        '**Discord:** Abra um ticket no servidor\n' +
                        '**Email:** suporte@primeleague.com\n' +
                        '**WhatsApp:** (11) 99999-9999',
                    inline: false
                },
                {
                    name: '⏰ Horário de Atendimento',
                    value: 
                        '**Segunda a Sexta:** 9h às 18h\n' +
                        '**Sábados:** 9h às 12h\n' +
                        '**Domingos:** Fechado',
                    inline: false
                },
                {
                    name: '📋 Informações Úteis',
                    value: 
                        '• Tenha seu Discord ID em mãos\n' +
                        '• Descreva o problema detalhadamente\n' +
                        '• Inclua prints se necessário\n' +
                        '• Seja paciente, responderemos em breve',
                    inline: false
                }
            ])
            .setFooter({ 
                text: '💬 Estamos aqui para ajudar!',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [supportEmbed] });
        return true;
    }
    
    /**
     * Mostra portfólio do usuário
     */
    static async handleViewPortfolio(interaction) {
        const discordId = interaction.user.id;
        
        try {
            // Buscar informações do portfólio
            const [portfolio, portfolioStats] = await Promise.all([
                getPortfolioByDiscordId(discordId),
                getPortfolioStats(discordId)
            ]);
            
            const portfolioEmbed = new EmbedBuilder()
                .setColor('#4ECDC4')
                .setTitle('📋 Seu Portfólio de Contas')
                .setDescription(`Portfólio de ${interaction.user}`)
                .addFields([
                    {
                        name: '📊 Estatísticas',
                        value: 
                            `**Total de Contas:** ${portfolioStats.total_accounts}\n` +
                            `**Assinaturas Ativas:** ${portfolioStats.active_subscriptions}\n` +
                            `**Assinaturas Expiradas:** ${portfolioStats.expired_subscriptions}`,
                        inline: true
                    },
                    {
                        name: '🎯 Status Geral',
                        value: portfolioStats.active_subscriptions > 0 ? '🟢 Ativo' : '🟡 Inativo',
                        inline: true
                    }
                ]);
            
            // Adicionar lista de contas se houver
            if (portfolio && portfolio.length > 0) {
                let accountsList = '';
                portfolio.forEach((account, index) => {
                    const isPrimary = account.is_primary;
                    const status = account.subscription_status === 'ACTIVE' ? '🟢 Ativa' : 
                                 account.subscription_status === 'EXPIRED' ? '🔴 Expirada' : '⚪ Nunca Assinou';
                    const primaryIcon = isPrimary ? '👑' : '👤';
                    
                    accountsList += `${primaryIcon} **${account.player_name}**${isPrimary ? ' *(Principal)*' : ''}\n`;
                    accountsList += `└ ${status}\n`;
                    
                    if (account.days_remaining > 0) {
                        accountsList += `└ ⏰ ${account.days_remaining} dias restantes\n`;
                    }
                    
                    if (index < portfolio.length - 1) {
                        accountsList += '\n';
                    }
                });
                
                portfolioEmbed.addFields({
                    name: '🎮 Contas Vinculadas',
                    value: accountsList,
                    inline: false
                });
            } else {
                portfolioEmbed.addFields({
                    name: '🎮 Contas Vinculadas',
                    value: 'Nenhuma conta vinculada ainda.\nUse `/registrar <nickname>` para vincular sua primeira conta.',
                    inline: false
                });
            }
            
            portfolioEmbed.setFooter({ 
                text: '📋 Gerencie suas contas com /conta',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
            await interaction.editReply({ embeds: [portfolioEmbed] });
            
        } catch (error) {
            console.error('[Portfolio] Erro ao buscar portfólio:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro ao Carregar Portfólio')
                .setDescription('Ocorreu um erro ao carregar seu portfólio. Tente novamente.')
                .setTimestamp();
                
            await interaction.editReply({ embeds: [errorEmbed] });
        }
        
        return true;
    }
}

module.exports = SubscriptionButtonHandler;
