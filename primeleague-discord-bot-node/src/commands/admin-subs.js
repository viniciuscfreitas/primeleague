const { SlashCommandBuilder, EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle, PermissionFlagsBits } = require('discord.js');
const {
    getDiscordLinksById,
    getClanSubscription,
    getClanActiveSessions,
    getPlayerByNickname,
    getClanActiveSessionsCount,
    updateSubscription
} = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('admin-subs')
        .setDescription('Comandos administrativos para gestão de clãs e assinaturas')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
        .addSubcommand(subcommand =>
            subcommand
                .setName('lookup')
                .setDescription('Busca informações detalhadas de um clã ou jogador')
                .addStringOption(option =>
                    option.setName('busca')
                        .setDescription('Discord ID, nickname do jogador ou @ mention')
                        .setRequired(true)))
        .addSubcommand(subcommand =>
            subcommand
                .setName('stats')
                .setDescription('Exibe estatísticas globais do sistema de clãs'))
        .addSubcommand(subcommand =>
            subcommand
                .setName('extend')
                .setDescription('Estende a assinatura de um clã manualmente')
                .addStringOption(option =>
                    option.setName('discord_id')
                        .setDescription('Discord ID do líder do clã')
                        .setRequired(true))
                .addIntegerOption(option =>
                    option.setName('dias')
                        .setDescription('Número de dias para estender')
                        .setRequired(true)
                        .setMinValue(1)
                        .setMaxValue(365)))
        .addSubcommand(subcommand =>
            subcommand
                .setName('suspend')
                .setDescription('Suspende ou reativa um clã')
                .addStringOption(option =>
                    option.setName('discord_id')
                        .setDescription('Discord ID do líder do clã')
                        .setRequired(true))
                .addStringOption(option =>
                    option.setName('acao')
                        .setDescription('Ação a ser executada')
                        .setRequired(true)
                        .addChoices(
                            { name: 'Suspender', value: 'suspend' },
                            { name: 'Reativar', value: 'reactivate' }
                        ))),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const subcommand = interaction.options.getSubcommand();

            switch (subcommand) {
                case 'lookup':
                    await handleLookup(interaction);
                    break;
                case 'stats':
                    await handleStats(interaction);
                    break;
                case 'extend':
                    await handleExtend(interaction);
                    break;
                case 'suspend':
                    await handleSuspend(interaction);
                    break;
                default:
                    await interaction.editReply({ content: '❌ Subcomando não reconhecido.' });
            }

        } catch (error) {
            console.error('Erro no comando /admin-subs:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro Administrativo')
                .setDescription('Ocorreu um erro ao executar o comando administrativo.')
                .setTimestamp();

            await interaction.editReply({ embeds: [errorEmbed] });
        }
    },
};

/**
 * Handle para o subcomando lookup
 */
async function handleLookup(interaction) {
    const busca = interaction.options.getString('busca');
    let discordId = null;

    // Extrair Discord ID de diferentes formatos
    if (busca.startsWith('<@') && busca.endsWith('>')) {
        // Mention format: <@123456789>
        discordId = busca.slice(2, -1).replace('!', '');
    } else if (/^\d{17,19}$/.test(busca)) {
        // Direct Discord ID
        discordId = busca;
    } else {
        // Assumir que é um nickname - buscar o Discord ID
        const player = await getPlayerByNickname(busca);
        if (player && player.discord_id) {
            discordId = player.discord_id;
        }
    }

    if (!discordId) {
        const embed = new EmbedBuilder()
            .setColor('#FF6B6B')
            .setTitle('❌ Não Encontrado')
            .setDescription(
                `Não foi possível encontrar informações para: \`${busca}\`\n\n` +
                '**Formatos aceitos:**\n' +
                '• Discord ID: `123456789012345678`\n' +
                '• Mention: `@username`\n' +
                '• Nickname: `finicff`'
            )
            .setTimestamp();

        return interaction.editReply({ embeds: [embed] });
    }

    // Buscar informações completas do clã
    const [subscription, clanMembers, activeSessions] = await Promise.all([
        getClanSubscription(discordId),
        getDiscordLinksById(discordId),
        getClanActiveSessions(discordId)
    ]);

    if (!clanMembers || clanMembers.length === 0) {
        const embed = new EmbedBuilder()
            .setColor('#FFB84D')
            .setTitle('⚠️ Clã Não Encontrado')
            .setDescription(`Discord ID \`${discordId}\` não possui clã registrado.`)
            .setTimestamp();

        return interaction.editReply({ embeds: [embed] });
    }

    // Criar dossiê completo
    const embed = new EmbedBuilder()
        .setColor('#4ECDC4')
        .setTitle('🔍 Dossiê Administrativo do Clã')
        .setDescription(`**Discord ID:** \`${discordId}\``)
        .setTimestamp();

    // Informações da assinatura
    if (subscription) {
        const isActive = new Date(subscription.expires_at) > new Date();
        const expiresAt = new Date(subscription.expires_at);
        const daysRemaining = Math.ceil((expiresAt - new Date()) / (1000 * 60 * 60 * 24));

        embed.addFields({
            name: '💎 Assinatura',
            value: 
                `**Status:** ${isActive ? '✅ Ativa' : '❌ Expirada'}\n` +
                `**Plano:** ${subscription.subscription_tier}\n` +
                `**Slots Máximos:** ${subscription.max_simultaneous_logins}\n` +
                `**Expira em:** ${isActive ? `${daysRemaining} dias` : 'Expirada'}\n` +
                `**Data de Expiração:** <t:${Math.floor(expiresAt.getTime() / 1000)}:F>\n` +
                `**Criada:** <t:${Math.floor(new Date(subscription.created_at).getTime() / 1000)}:R>\n` +
                `**Atualizada:** <t:${Math.floor(new Date(subscription.updated_at).getTime() / 1000)}:R>`,
            inline: false
        });
    } else {
        embed.addFields({
            name: '💎 Assinatura',
            value: '**Status:** ❌ Sem assinatura ativa',
            inline: false
        });
    }

    // Membros do clã
    const primaryAccount = clanMembers.find(m => m.is_primary);
    const secondaryMembers = clanMembers.filter(m => !m.is_primary);

    let membersText = '';
    if (primaryAccount) {
        const verifiedIcon = primaryAccount.verified ? '✅' : '⏳';
        membersText += `👑 **${primaryAccount.player_name}** ${verifiedIcon} *(Principal)*\n`;
    }

    secondaryMembers.slice(0, 8).forEach(member => {
        const verifiedIcon = member.verified ? '✅' : '⏳';
        membersText += `👤 **${member.player_name}** ${verifiedIcon}\n`;
    });

    if (secondaryMembers.length > 8) {
        membersText += `\n*... e mais ${secondaryMembers.length - 8} membros*`;
    }

    embed.addFields({
        name: `👥 Membros (${clanMembers.length})`,
        value: membersText || '*Nenhum membro*',
        inline: true
    });

    // Sessões ativas
    if (activeSessions.length > 0) {
        let sessionsText = '';
        activeSessions.slice(0, 5).forEach(session => {
            sessionsText += `🎮 **${session.player_name}** (${session.duration_minutes}min)\n`;
        });

        if (activeSessions.length > 5) {
            sessionsText += `\n*... e mais ${activeSessions.length - 5} sessões*`;
        }

        embed.addFields({
            name: `🎮 Sessões Ativas (${activeSessions.length})`,
            value: sessionsText,
            inline: true
        });
    } else {
        embed.addFields({
            name: '🎮 Sessões Ativas',
            value: '*Nenhuma sessão ativa*',
            inline: true
        });
    }

    // Estatísticas gerais
    const verifiedMembers = clanMembers.filter(m => m.verified).length;
    const pendingMembers = clanMembers.filter(m => !m.verified).length;

    embed.addFields({
        name: '📊 Estatísticas',
        value: 
            `**Verificados:** ${verifiedMembers}\n` +
            `**Pendentes:** ${pendingMembers}\n` +
            `**Taxa de Ocupação:** ${subscription ? Math.round((activeSessions.length / subscription.max_simultaneous_logins) * 100) : 0}%\n` +
            `**Último Acesso:** ${activeSessions.length > 0 ? 'Agora' : 'Offline'}`,
        inline: false
    });

    // Botões administrativos
    const buttons = new ActionRowBuilder();
    
    if (subscription && new Date(subscription.expires_at) > new Date()) {
        buttons.addComponents(
            new ButtonBuilder()
                .setCustomId(`admin_extend_${discordId}`)
                .setLabel('⏰ Estender')
                .setStyle(ButtonStyle.Primary),
            new ButtonBuilder()
                .setCustomId(`admin_suspend_${discordId}`)
                .setLabel('🚫 Suspender')
                .setStyle(ButtonStyle.Danger)
        );
    } else {
        buttons.addComponents(
            new ButtonBuilder()
                .setCustomId(`admin_reactivate_${discordId}`)
                .setLabel('✅ Reativar')
                .setStyle(ButtonStyle.Success)
        );
    }

    buttons.addComponents(
        new ButtonBuilder()
            .setCustomId(`admin_refresh_${discordId}`)
            .setLabel('🔄 Atualizar')
            .setStyle(ButtonStyle.Secondary)
    );

    embed.setFooter({ 
        text: `Dossiê gerado por ${interaction.user.tag} • ID: ${discordId}` 
    });

    await interaction.editReply({ embeds: [embed], components: [buttons] });
}

/**
 * Handle para o subcomando stats
 */
async function handleStats(interaction) {
    // Esta função precisa ser implementada com queries específicas para estatísticas globais
    const embed = new EmbedBuilder()
        .setColor('#4ECDC4')
        .setTitle('📊 Estatísticas Globais do Sistema')
        .setDescription('*Em desenvolvimento - Estatísticas globais do sistema de clãs*')
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}

/**
 * Handle para o subcomando extend
 */
async function handleExtend(interaction) {
    const discordId = interaction.options.getString('discord_id');
    const dias = interaction.options.getInteger('dias');

    // Buscar assinatura atual
    const subscription = await getClanSubscription(discordId);
    
    if (!subscription) {
        const embed = new EmbedBuilder()
            .setColor('#FF6B6B')
            .setTitle('❌ Assinatura Não Encontrada')
            .setDescription(`Discord ID \`${discordId}\` não possui assinatura ativa.`)
            .setTimestamp();

        return interaction.editReply({ embeds: [embed] });
    }

    // Calcular nova data de expiração
    const currentExpiry = new Date(subscription.expires_at);
    const newExpiry = new Date(currentExpiry.getTime() + (dias * 24 * 60 * 60 * 1000));

    // Atualizar no banco
    const success = await updateSubscription(
        discordId, 
        subscription.subscription_tier, 
        subscription.max_simultaneous_logins, 
        newExpiry
    );

    if (success) {
        const embed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('✅ Assinatura Estendida')
            .setDescription(
                `Assinatura estendida com sucesso!\n\n` +
                `**Discord ID:** \`${discordId}\`\n` +
                `**Dias Adicionados:** ${dias}\n` +
                `**Nova Expiração:** <t:${Math.floor(newExpiry.getTime() / 1000)}:F>\n` +
                `**Administrador:** ${interaction.user}`
            )
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });
    } else {
        const embed = new EmbedBuilder()
            .setColor('#FF6B6B')
            .setTitle('❌ Erro na Extensão')
            .setDescription('Falha ao estender a assinatura. Tente novamente.')
            .setTimestamp();

        await interaction.editReply({ embeds: [embed] });
    }
}

/**
 * Handle para o subcomando suspend
 */
async function handleSuspend(interaction) {
    const discordId = interaction.options.getString('discord_id');
    const acao = interaction.options.getString('acao');

    // Esta funcionalidade requer implementação específica no banco
    const embed = new EmbedBuilder()
        .setColor('#FFB84D')
        .setTitle('⚠️ Funcionalidade em Desenvolvimento')
        .setDescription(
            'O sistema de suspensão/reativação está em desenvolvimento.\n\n' +
            'Use o comando de extensão para gerenciar assinaturas temporariamente.'
        )
        .setTimestamp();

    await interaction.editReply({ embeds: [embed] });
}
