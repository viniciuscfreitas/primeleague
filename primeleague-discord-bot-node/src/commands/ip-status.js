const { SlashCommandBuilder, EmbedBuilder, PermissionFlagsBits } = require('discord.js');
const { pool } = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('ip-status')
        .setDescription('Monitora o status do sistema de autorização de IPs')
        .setDefaultMemberPermissions(PermissionFlagsBits.Administrator)
        .addSubcommand(subcommand =>
            subcommand
                .setName('stats')
                .setDescription('Estatísticas do sistema de autorização'))
        .addSubcommand(subcommand =>
            subcommand
                .setName('pending')
                .setDescription('Lista notificações pendentes'))
        .addSubcommand(subcommand =>
            subcommand
                .setName('player')
                .setDescription('Verifica IPs autorizados de um jogador')
                .addStringOption(option =>
                    option
                        .setName('jogador')
                        .setDescription('Nome do jogador')
                        .setRequired(true))),

    async execute(interaction) {
        const subcommand = interaction.options.getSubcommand();

        try {
            switch (subcommand) {
                case 'stats':
                    await this.showSystemStats(interaction);
                    break;
                case 'pending':
                    await this.showPendingNotifications(interaction);
                    break;
                case 'player':
                    const playerName = interaction.options.getString('jogador');
                    await this.showPlayerIPs(interaction, playerName);
                    break;
            }
        } catch (error) {
            console.error('[ip-status] Erro ao executar comando:', error);
            await interaction.reply({
                content: '❌ **Erro:** Falha ao executar comando de status.',
                ephemeral: true
            });
        }
    },

    async showSystemStats(interaction) {
        await interaction.deferReply({ ephemeral: true });

        const connection = await pool.getConnection();
        
        try {
            // Estatísticas de notificações
            const [notificationStats] = await connection.execute(`
                SELECT 
                    COUNT(*) as total_notifications,
                    COUNT(CASE WHEN processed = TRUE THEN 1 END) as processed_notifications,
                    COUNT(CASE WHEN processed = FALSE THEN 1 END) as pending_notifications,
                    COUNT(CASE WHEN action_type = 'REQUEST_IP_AUTH' THEN 1 END) as ip_auth_requests
                FROM server_notifications 
                WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
            `);

            // Estatísticas de IPs autorizados
            const [ipStats] = await connection.execute(`
                SELECT * FROM ip_authorization_stats
            `);

            // Atividade recente
            const [recentActivity] = await connection.execute(`
                SELECT 
                    action_type,
                    created_at,
                    processed
                FROM server_notifications 
                WHERE created_at >= DATE_SUB(NOW(), INTERVAL 2 HOUR)
                ORDER BY created_at DESC 
                LIMIT 5
            `);

            const stats = notificationStats[0];
            const ipStatsData = ipStats[0];

            const embed = new EmbedBuilder()
                .setColor('#00D4FF')
                .setTitle('📊 Status do Sistema de Autorização de IPs')
                .setDescription('Estatísticas das últimas 24 horas')
                .addFields([
                    {
                        name: '📥 Notificações (24h)',
                        value: `**Total:** ${stats.total_notifications}\n` +
                               `**Processadas:** ${stats.processed_notifications}\n` +
                               `**Pendentes:** ${stats.pending_notifications}\n` +
                               `**Autorizações IP:** ${stats.ip_auth_requests}`,
                        inline: true
                    },
                    {
                        name: '🔐 IPs Autorizados',
                        value: `**Total:** ${ipStatsData.total_authorizations}\n` +
                               `**Ativos:** ${ipStatsData.active_authorizations}\n` +
                               `**Expirados:** ${ipStatsData.expired_authorizations}\n` +
                               `**Jogadores únicos:** ${ipStatsData.unique_players}`,
                        inline: true
                    },
                    {
                        name: '🕒 Atividade Recente (2h)',
                        value: recentActivity.length > 0 
                            ? recentActivity.map(activity => 
                                `\`${activity.action_type}\` ${activity.processed ? '✅' : '⏳'}`
                              ).join('\n')
                            : 'Nenhuma atividade recente',
                        inline: false
                    }
                ])
                .setFooter({
                    text: 'Prime League - Sistema de Monitoramento',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();

            await interaction.editReply({ embeds: [embed] });

        } finally {
            connection.release();
        }
    },

    async showPendingNotifications(interaction) {
        await interaction.deferReply({ ephemeral: true });

        const connection = await pool.getConnection();

        try {
            const [notifications] = await connection.execute(`
                SELECT 
                    id,
                    action_type,
                    created_at,
                    payload
                FROM server_notifications 
                WHERE processed = FALSE 
                ORDER BY created_at ASC 
                LIMIT 10
            `);

            if (notifications.length === 0) {
                await interaction.editReply({
                    content: '✅ **Nenhuma notificação pendente** - Sistema funcionando normalmente!'
                });
                return;
            }

            const embed = new EmbedBuilder()
                .setColor('#FFB800')
                .setTitle('⏳ Notificações Pendentes')
                .setDescription(`${notifications.length} notificação(ões) aguardando processamento`)
                .setFooter({
                    text: 'Prime League - Sistema de Monitoramento',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();

            for (const notification of notifications) {
                const timeDiff = Math.floor((Date.now() - new Date(notification.created_at).getTime()) / 1000);
                const timeAgo = timeDiff < 60 ? `${timeDiff}s` : 
                               timeDiff < 3600 ? `${Math.floor(timeDiff/60)}m` : 
                               `${Math.floor(timeDiff/3600)}h`;

                let description = `**Tipo:** ${notification.action_type}\n**Há:** ${timeAgo}`;
                
                if (notification.action_type === 'REQUEST_IP_AUTH' && notification.payload) {
                    try {
                        const payload = JSON.parse(notification.payload);
                        description += `\n**IP:** ${payload.ip_obfuscated}\n**Local:** ${payload.geo_city}, ${payload.geo_country}`;
                    } catch (e) {
                        // Ignorar erro de parsing
                    }
                }

                embed.addFields([{
                    name: `🎯 Notificação (ID: ${notification.id})`,
                    value: description,
                    inline: true
                }]);
            }

            await interaction.editReply({ embeds: [embed] });

        } finally {
            connection.release();
        }
    },

    async showPlayerIPs(interaction, playerName) {
        await interaction.deferReply({ ephemeral: true });

        const connection = await pool.getConnection();

        try {
            const [authorizedIPs] = await connection.execute(`
                SELECT 
                    ip_address,
                    geo_city,
                    geo_country,
                    isp,
                    authorized_at,
                    expires_at,
                    authorized_by,
                    is_active,
                    DATEDIFF(expires_at, NOW()) as days_remaining
                FROM player_authorized_ips pai
                JOIN player_data pd ON pai.player_uuid = pd.uuid
                WHERE pd.name = ?
                ORDER BY authorized_at DESC
            `, [playerName]);

            if (authorizedIPs.length === 0) {
                await interaction.editReply({
                    content: `❌ **Jogador não encontrado** ou nenhum IP autorizado para \`${playerName}\`.`
                });
                return;
            }

            const embed = new EmbedBuilder()
                .setColor('#00FF88')
                .setTitle(`🔐 IPs Autorizados - ${playerName}`)
                .setDescription(`${authorizedIPs.length} IP(s) registrado(s)`)
                .setFooter({
                    text: 'Prime League - Sistema de Monitoramento',
                    iconURL: interaction.client.user.displayAvatarURL()
                })
                .setTimestamp();

            for (const ip of authorizedIPs) {
                const obfuscatedIP = this.obfuscateIP(ip.ip_address);
                const status = ip.is_active && ip.days_remaining > 0 ? '🟢 Ativo' : 
                              ip.days_remaining <= 0 ? '🔴 Expirado' : '⚫ Inativo';
                
                const daysText = ip.days_remaining > 0 ? `${ip.days_remaining} dias` : 'Expirado';

                embed.addFields([{
                    name: `${status} ${obfuscatedIP}`,
                    value: `**Local:** ${ip.geo_city}, ${ip.geo_country}\n` +
                           `**Provedor:** ${ip.isp}\n` +
                           `**Autorizado:** ${ip.authorized_by}\n` +
                           `**Validade:** ${daysText}`,
                    inline: true
                }]);
            }

            await interaction.editReply({ embeds: [embed] });

        } finally {
            connection.release();
        }
    },

    obfuscateIP(ipAddress) {
        const parts = ipAddress.split('.');
        if (parts.length === 4) {
            return `xxx.xxx.xxx.${parts[3]}`;
        }
        return 'xxx.xxx.xxx.x';
    }
};
