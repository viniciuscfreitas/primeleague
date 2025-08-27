const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { getDonorInfoFromCore, pool } = require('../database/mysql');

/**
 * Handler para botões de simulação de upgrade de doador
 * Simula o processo de upgrade de tier de doador
 */
class DonorButtonHandler {
    
    /**
     * Processa interações de botões de upgrade de doador
     */
    static async handleDonorButton(interaction) {
        const { customId } = interaction;
        
        try {
            // Deferir resposta para evitar timeout
            await interaction.deferUpdate();
            
            switch (customId) {
                case 'upgrade_to_1':
                    return await this.handleUpgradeToTier1(interaction);
                    
                case 'upgrade_to_2':
                    return await this.handleUpgradeToTier2(interaction);
                    
                case 'donor_help':
                    return await this.handleDonorHelp(interaction);
                    
                case 'donor_benefits':
                    return await this.handleDonorBenefits(interaction);
                    
                default:
                    return false; // Botão não tratado
            }
            
        } catch (error) {
            console.error(`[DonorButton] Erro ao processar botão ${customId}:`, error);
            
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
     * Atualiza o donor tier no banco de dados (por Discord ID)
     */
    static async updateDonorTierInDatabase(discordId, newTier) {
        try {
            console.log(`[DONOR-BUTTON] 🔄 Atualizando donor tier para ${newTier} no banco...`);
            
            // Verificar se existe pelo menos uma conta vinculada
            const [discordLinks] = await pool.execute(`
                SELECT dl.player_id, pd.name 
                FROM discord_links dl 
                JOIN player_data pd ON dl.player_id = pd.player_id 
                WHERE dl.discord_id = ? AND dl.verified = TRUE
                LIMIT 1
            `, [discordId]);
            
            if (discordLinks.length === 0) {
                console.log(`[DONOR-BUTTON] ❌ Nenhum player vinculado encontrado para Discord ID: ${discordId}`);
                return { success: false, error: 'Nenhuma conta vinculada encontrada.' };
            }
            
            const playerName = discordLinks[0].name;
            console.log(`[DONOR-BUTTON] 📋 Conta principal: ${playerName}`);
            
            // Calcular data de expiração (30 dias)
            const expiryDate = new Date();
            expiryDate.setDate(expiryDate.getDate() + 30);
            
            // Atualizar donor_tier na tabela discord_users (por Discord ID)
            const [result] = await pool.execute(`
                INSERT INTO discord_users (discord_id, donor_tier, donor_tier_expires_at, updated_at)
                VALUES (?, ?, ?, NOW())
                ON DUPLICATE KEY UPDATE 
                    donor_tier = VALUES(donor_tier),
                    donor_tier_expires_at = VALUES(donor_tier_expires_at),
                    updated_at = NOW()
            `, [discordId, newTier, expiryDate]);
            
            if (result.affectedRows > 0 || result.insertId > 0) {
                console.log(`[DONOR-BUTTON] ✅ Donor tier atualizado com sucesso para Discord ID: ${discordId}`);
                console.log(`[DONOR-BUTTON] 📅 Expira em: ${expiryDate.toLocaleDateString('pt-BR')}`);
                
                // Nota: donor_tier agora está centralizado em discord_users
                // Não precisamos mais atualizar player_data
                console.log(`[DONOR-BUTTON] ✅ Donor tier centralizado em discord_users`);
                
                return { 
                    success: true, 
                    playerName: playerName,
                    newTier: newTier,
                    expiryDate: expiryDate
                };
            } else {
                console.log(`[DONOR-BUTTON] ❌ Falha ao atualizar donor tier`);
                return { success: false, error: 'Falha ao atualizar no banco.' };
            }
            
        } catch (error) {
            console.error('[DONOR-BUTTON] ❌ Erro ao atualizar donor tier:', error);
            return { success: false, error: 'Erro interno do banco.' };
        }
    }
    
    /**
     * Simula upgrade para Tier 1 (Apoiador)
     */
    static async handleUpgradeToTier1(interaction) {
        const discordId = interaction.user.id;
        console.log(`[DONOR-BUTTON] 🎮 Iniciando simulação de upgrade para Tier 1 para Discord ID: ${discordId}`);
        
        // Simular processamento
        const processingEmbed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⏳ Processando Upgrade...')
            .setDescription('Simulando processamento do upgrade para Apoiador...')
            .addFields({
                name: '📋 Detalhes da Simulação',
                value: 
                    '**Tier Atual:** Player (0)\n' +
                    '**Novo Tier:** Apoiador (1)\n' +
                    '**Valor:** R$ 29,90\n' +
                    '**Benefício:** 2 contas simultâneas',
                inline: false
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [processingEmbed] });
        
        // Simular delay de processamento
        console.log(`[DONOR-BUTTON] ⏳ Aguardando 2 segundos para simular processamento...`);
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // ATUALIZAR BANCO DE DADOS REALMENTE
        console.log(`[DONOR-BUTTON] 🔄 Executando upgrade real no banco...`);
        const dbResult = await this.updateDonorTierInDatabase(discordId, 1);
        
        if (!dbResult.success) {
            console.log(`[DONOR-BUTTON] ❌ Falha no upgrade: ${dbResult.error}`);
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro no Upgrade')
                .setDescription(`Falha ao processar upgrade: ${dbResult.error}`)
                .setTimestamp();
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
        
        console.log(`[DONOR-BUTTON] ✅ Upgrade real concluído com sucesso!`);
        
        const successEmbed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('🎉 Upgrade Concluído!')
            .setDescription(`Parabéns, ${dbResult.playerName}! Você agora é um **Apoiador** do Prime League!`)
            .addFields([
                {
                    name: '🏆 Novo Status',
                    value: '**Apoiador (Tier 1)**',
                    inline: true
                },
                {
                    name: '💰 Valor',
                    value: '**R$ 29,90** (simulado)',
                    inline: true
                },
                {
                    name: '📅 Expira em',
                    value: `<t:${Math.floor(dbResult.expiryDate.getTime() / 1000)}:F>`,
                    inline: true
                }
            ])
            .addFields({
                name: '🎯 Novos Benefícios',
                value: 
                    '• ✅ **2 contas simultâneas**\n' +
                    '• 🎨 Cores especiais no chat\n' +
                    '• ⚡ Prioridade no suporte\n' +
                    '• 🎮 Acesso completo ao servidor\n' +
                    '• 🏆 Status de Apoiador',
                inline: false
            })
            .addFields({
                name: '🎮 Próximos Passos',
                value: 
                    '1. ✅ Use `/registrar` para vincular mais contas\n' +
                    '2. 🎮 Aproveite os benefícios de Apoiador\n' +
                    '3. 📋 Use `/conta` para gerenciar seu portfólio\n' +
                    '4. 🔄 Renove quando necessário',
                inline: false
            })
            .setFooter({ 
                text: '🎉 Obrigado por apoiar o Prime League!',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [successEmbed] });
        return true;
    }
    
    /**
     * Simula upgrade para Tier 2 (Patrocinador)
     */
    static async handleUpgradeToTier2(interaction) {
        const discordId = interaction.user.id;
        console.log(`[DONOR-BUTTON] 🎮 Iniciando simulação de upgrade para Tier 2 para Discord ID: ${discordId}`);
        
        // Simular processamento
        const processingEmbed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⏳ Processando Upgrade VIP...')
            .setDescription('Simulando processamento do upgrade para Patrocinador...')
            .addFields({
                name: '📋 Detalhes da Simulação',
                value: 
                    '**Tier Atual:** Player (0)\n' +
                    '**Novo Tier:** Patrocinador (2)\n' +
                    '**Valor:** R$ 59,90\n' +
                    '**Benefício:** 5 contas simultâneas',
                inline: false
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [processingEmbed] });
        
        // Simular delay de processamento
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // ATUALIZAR BANCO DE DADOS REALMENTE
        console.log(`[DONOR-BUTTON] 🔄 Executando upgrade VIP real no banco...`);
        const dbResult = await this.updateDonorTierInDatabase(discordId, 2);
        
        if (!dbResult.success) {
            console.log(`[DONOR-BUTTON] ❌ Falha no upgrade VIP: ${dbResult.error}`);
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro no Upgrade VIP')
                .setDescription(`Falha ao processar upgrade: ${dbResult.error}`)
                .setTimestamp();
            await interaction.editReply({ embeds: [errorEmbed] });
            return true;
        }
        
        console.log(`[DONOR-BUTTON] ✅ Upgrade VIP real concluído com sucesso!`);
        
        const successEmbed = new EmbedBuilder()
            .setColor('#FFD700')
            .setTitle('👑 Upgrade VIP Concluído!')
            .setDescription(`Parabéns, ${dbResult.playerName}! Você agora é um **Patrocinador** do Prime League!`)
            .addFields([
                {
                    name: '👑 Novo Status',
                    value: '**Patrocinador (Tier 2)**',
                    inline: true
                },
                {
                    name: '💰 Valor',
                    value: '**R$ 59,90** (simulado)',
                    inline: true
                },
                {
                    name: '📅 Expira em',
                    value: `<t:${Math.floor(dbResult.expiryDate.getTime() / 1000)}:F>`,
                    inline: true
                }
            ])
            .addFields({
                name: '👑 Benefícios VIP',
                value: 
                    '• ✅ **5 contas simultâneas**\n' +
                    '• 🏆 Suporte VIP 24/7\n' +
                    '• 🎨 Comandos especiais\n' +
                    '• 🎮 Acesso completo ao servidor\n' +
                    '• ⭐ Status VIP exclusivo\n' +
                    '• 🎁 Benefícios exclusivos',
                inline: false
            })
            .addFields({
                name: '🎮 Próximos Passos',
                value: 
                    '1. ✅ Use `/registrar` para vincular mais contas\n' +
                    '2. 👑 Aproveite os benefícios VIP\n' +
                    '3. 📋 Use `/conta` para gerenciar seu portfólio\n' +
                    '4. 🔄 Renove quando necessário',
                inline: false
            })
            .setFooter({ 
                text: '👑 Bem-vindo ao clube VIP!',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [successEmbed] });
        return true;
    }
    
    /**
     * Mostra ajuda sobre o sistema de doadores
     */
    static async handleDonorHelp(interaction) {
        const helpEmbed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('❓ Ajuda - Sistema de Apoiadores')
            .setDescription('Tire suas dúvidas sobre nosso sistema de apoiadores.')
            .addFields([
                {
                    name: '🎯 Como Funciona',
                    value: 
                        '1. **Escolha um tier** clicando nos botões\n' +
                        '2. **Confirme o pagamento** (simulado)\n' +
                        '3. **Ganhe benefícios** imediatamente\n' +
                        '4. **Vincule mais contas** conforme seu tier',
                    inline: false
                },
                {
                    name: '🏆 Tiers Disponíveis',
                    value: 
                        '**👤 Player (Tier 0):** 1 conta - Gratuito\n' +
                        '**🎉 Apoiador (Tier 1):** 2 contas - R$ 29,90\n' +
                        '**👑 Patrocinador (Tier 2):** 5 contas - R$ 59,90',
                    inline: false
                },
                {
                    name: '❓ Dúvidas Frequentes',
                    value: 
                        '• **Upgrade:** A qualquer momento\n' +
                        '• **Downgrade:** No próximo ciclo\n' +
                        '• **Contas:** Vinculadas individualmente\n' +
                        '• **Benefícios:** Ativos imediatamente',
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
     * Mostra benefícios detalhados dos doadores
     */
    static async handleDonorBenefits(interaction) {
        const benefitsEmbed = new EmbedBuilder()
            .setColor('#FFD700')
            .setTitle('🏆 Benefícios dos Apoiadores')
            .setDescription('Conheça todos os benefícios exclusivos para nossos apoiadores.')
            .addFields([
                {
                    name: '👤 Player (Tier 0)',
                    value: 
                        '• ✅ 1 conta simultânea\n' +
                        '• 🎮 Acesso básico ao servidor\n' +
                        '• 📋 Suporte padrão',
                    inline: true
                },
                {
                    name: '🎉 Apoiador (Tier 1)',
                    value: 
                        '• ✅ 2 contas simultâneas\n' +
                        '• 🎨 Cores especiais no chat\n' +
                        '• ⚡ Prioridade no suporte\n' +
                        '• 🏆 Status de Apoiador',
                    inline: true
                },
                {
                    name: '👑 Patrocinador (Tier 2)',
                    value: 
                        '• ✅ 5 contas simultâneas\n' +
                        '• 🏆 Suporte VIP 24/7\n' +
                        '• 🎨 Comandos especiais\n' +
                        '• ⭐ Status VIP exclusivo',
                    inline: true
                }
            ])
            .addFields({
                name: '🎁 Benefícios Exclusivos',
                value: 
                    '• 🎮 **Contas Simultâneas:** Gerencie múltiplas contas\n' +
                    '• 🎨 **Personalização:** Cores e comandos especiais\n' +
                    '• ⚡ **Suporte Prioritário:** Atendimento VIP\n' +
                    '• 🏆 **Status Exclusivo:** Reconhecimento especial\n' +
                    '• 🎁 **Benefícios Futuros:** Novos recursos exclusivos',
                inline: false
            })
            .setFooter({ 
                text: '🏆 Apoie o Prime League e ganhe benefícios exclusivos!',
                iconURL: interaction.client.user.displayAvatarURL()
            })
            .setTimestamp();
            
        await interaction.editReply({ embeds: [benefitsEmbed] });
        return true;
    }
}

module.exports = DonorButtonHandler;
