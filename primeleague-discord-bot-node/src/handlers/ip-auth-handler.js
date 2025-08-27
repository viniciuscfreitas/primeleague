const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const { pool } = require('../database/mysql');

/**
 * Handler de Autorização de IPs
 * 
 * Gerencia o envio de DMs para autorização de IPs e processa
 * as interações dos botões de aprovação/negação.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
class IPAuthHandler {

    /**
     * Envia DM de autorização de IP para o jogador.
     */
    static async sendIPAuthorizationDM(client, discordId, payload, notificationId) {
        try {
            // Buscar usuário no Discord
            const user = await client.users.fetch(discordId);
            if (!user) {
                console.error(`[IPAuthHandler] Usuário Discord não encontrado: ${discordId}`);
                return false;
            }

            // Criar embed do dossiê contextual
            const embed = this.createIPAuthEmbed(payload);

            // Criar botões de ação
            const actionRow = this.createIPAuthButtons(notificationId);

            // Enviar DM
            await user.send({
                embeds: [embed],
                components: [actionRow]
            });

            console.log(`[IPAuthHandler] DM de autorização enviada para ${payload.player_name} (${discordId})`);
            return true;

        } catch (error) {
            console.error(`[IPAuthHandler] Erro ao enviar DM para ${discordId}:`, error);
            return false;
        }
    }

    /**
     * Cria o embed com o dossiê contextual do IP.
     */
    static createIPAuthEmbed(payload) {
        const embed = new EmbedBuilder()
            .setColor('#FF6B35') // Laranja para atenção
            .setTitle('🔐 Nova Tentativa de Acesso Detectada')
            .setDescription('Por segurança, detectamos uma tentativa de acesso de um novo local.')
            .addFields([
                {
                    name: '👤 Jogador',
                    value: `\`${payload.player_name}\``,
                    inline: true
                },
                {
                    name: '🌍 Localização',
                    value: `${payload.geo_city}, ${payload.geo_country}`,
                    inline: true
                },
                {
                    name: '🏢 Provedor',
                    value: `${payload.isp}`,
                    inline: true
                },
                {
                    name: '🌐 Endereço IP',
                    value: `\`${payload.ip_obfuscated}\``,
                    inline: true
                },
                {
                    name: '🕒 Data/Hora',
                    value: `<t:${Math.floor(payload.timestamp / 1000)}:F>`,
                    inline: true
                },
                {
                    name: '⚠️ Importante',
                    value: 'Se você **NÃO** tentou se conectar de este local, **NEGUE** imediatamente e entre em contato com a staff.',
                    inline: false
                }
            ])
            .setFooter({
                text: 'Prime League - Sistema de Segurança',
                iconURL: 'https://cdn.discordapp.com/icons/1344225249826443266/a_0123456789abcdef.gif'
            })
            .setTimestamp();

        return embed;
    }

    /**
     * Cria os botões de ação para autorização.
     */
    static createIPAuthButtons(notificationId) {
        const approveButton = new ButtonBuilder()
            .setCustomId(`ip_auth_approve_${notificationId}`)
            .setLabel('✅ Aprovar este Acesso')
            .setStyle(ButtonStyle.Success);

        const denyButton = new ButtonBuilder()
            .setCustomId(`ip_auth_deny_${notificationId}`)
            .setLabel('❌ Negar e Alertar Staff')
            .setStyle(ButtonStyle.Danger);

        const actionRow = new ActionRowBuilder()
            .addComponents(approveButton, denyButton);

        return actionRow;
    }

    /**
     * Processa interação de botão de autorização.
     */
    static async handleIPAuthInteraction(interaction) {
        try {
            const customId = interaction.customId;
            const [action, type, notificationId] = customId.split('_');

            if (action !== 'ip' || type !== 'auth') {
                return false; // Não é uma interação de autorização de IP
            }

            // Verificar se a notificação ainda existe e não foi processada
            const notification = await this.getNotificationById(notificationId);
            if (!notification) {
                await interaction.reply({
                    content: '❌ **Expirado:** Esta solicitação de autorização não é mais válida.',
                    ephemeral: true
                });
                return true;
            }

            // Verificar se o usuário é o dono da solicitação
            const payload = JSON.parse(notification.payload);
            const playerDiscordInfo = await this.getPlayerDiscordInfo(payload.player_name);

            if (!playerDiscordInfo || playerDiscordInfo.discord_id !== interaction.user.id) {
                await interaction.reply({
                    content: '❌ **Não autorizado:** Você não pode autorizar este acesso.',
                    ephemeral: true
                });
                return true;
            }

            // Processar ação
            const isApproval = customId.includes('approve');
            const success = await this.processIPAuthorization(notificationId, payload, isApproval, interaction.user.id);

            if (success) {
                // Atualizar a mensagem original
                await this.updateAuthMessage(interaction, payload, isApproval);
            } else {
                await interaction.reply({
                    content: '❌ **Erro:** Falha ao processar a autorização. Tente novamente.',
                    ephemeral: true
                });
            }

            return true;

        } catch (error) {
            console.error('[IPAuthHandler] Erro ao processar interação:', error);
            await interaction.reply({
                content: '❌ **Erro interno:** Falha ao processar a autorização.',
                ephemeral: true
            });
            return true;
        }
    }

    /**
     * Processa a autorização ou negação do IP.
     */
    static async processIPAuthorization(notificationId, payload, isApproval, discordUserId) {
        const connection = await pool.getConnection();

        try {
            await connection.beginTransaction();

            if (isApproval) {
                // Aprovar: Inserir IP na tabela de autorizados
                await connection.execute(`
                    CALL AuthorizeIP(?, ?, ?, ?, ?, ?, ?, ?, @success, @message)
                `, [
                    payload.player_uuid || null, // UUID do jogador (se disponível)
                    payload.ip_full,             // IP completo
                    30,                          // 30 dias
                    payload.geo_city,            // Cidade
                    payload.geo_country,         // País
                    payload.isp,                 // Provedor
                    'USER',                      // Autorizado pelo usuário
                    null                         // Nenhum admin
                ]);

                console.log(`[IPAuthHandler] IP ${payload.ip_obfuscated} autorizado para ${payload.player_name}`);

            } else {
                // Negar: Criar alerta para staff (opcional)
                await this.createSecurityAlert(payload, discordUserId);
                console.log(`[IPAuthHandler] IP ${payload.ip_obfuscated} NEGADO para ${payload.player_name}`);
            }

            // Marcar notificação como processada
            await connection.execute(`
                UPDATE server_notifications 
                SET 
                    processed = TRUE,
                    processed_at = NOW(),
                    processing_result = ?
                WHERE id = ?
            `, [
                JSON.stringify({
                    success: true,
                    action: isApproval ? 'approved' : 'denied',
                    processed_by: `discord_user:${discordUserId}`,
                    timestamp: Date.now()
                }),
                notificationId
            ]);

            await connection.commit();
            return true;

        } catch (error) {
            await connection.rollback();
            console.error('[IPAuthHandler] Erro ao processar autorização:', error);
            return false;
        } finally {
            connection.release();
        }
    }

    /**
     * Atualiza a mensagem de autorização com o resultado.
     */
    static async updateAuthMessage(interaction, payload, isApproval) {
        const resultEmbed = new EmbedBuilder()
            .setColor(isApproval ? '#00FF00' : '#FF0000')
            .setTitle(isApproval ? '✅ Acesso Autorizado' : '❌ Acesso Negado')
            .setDescription(
                isApproval 
                    ? `O IP \`${payload.ip_obfuscated}\` foi autorizado com sucesso para **${payload.player_name}**.`
                    : `O acesso foi negado e a staff foi notificada sobre a tentativa suspeita.`
            )
            .addFields([
                {
                    name: '📍 Localização',
                    value: `${payload.geo_city}, ${payload.geo_country}`,
                    inline: true
                },
                {
                    name: '🕒 Processado em',
                    value: `<t:${Math.floor(Date.now() / 1000)}:F>`,
                    inline: true
                }
            ])
            .setFooter({
                text: 'Prime League - Sistema de Segurança',
                iconURL: 'https://cdn.discordapp.com/icons/1344225249826443266/a_0123456789abcdef.gif'
            })
            .setTimestamp();

        if (isApproval) {
            resultEmbed.addFields([
                {
                    name: '⏱️ Validade',
                    value: '30 dias a partir de agora',
                    inline: true
                }
            ]);
        }

        await interaction.update({
            embeds: [resultEmbed],
            components: [] // Remover botões
        });
    }

    /**
     * Cria um alerta de segurança para a staff.
     */
    static async createSecurityAlert(payload, discordUserId) {
        const connection = await pool.getConnection();

        try {
            await connection.execute(`
                INSERT INTO server_notifications (action_type, payload) 
                VALUES (?, ?)
            `, [
                'ADMIN_SECURITY_ALERT',
                JSON.stringify({
                    type: 'IP_ACCESS_DENIED',
                    player_name: payload.player_name,
                    ip_obfuscated: payload.ip_obfuscated,
                    geo_city: payload.geo_city,
                    geo_country: payload.geo_country,
                    isp: payload.isp,
                    denied_by: discordUserId,
                    timestamp: Date.now()
                })
            ]);

            console.log(`[IPAuthHandler] Alerta de segurança criado para ${payload.player_name}`);

        } finally {
            connection.release();
        }
    }

    /**
     * Busca notificação por ID.
     */
    static async getNotificationById(notificationId) {
        const connection = await pool.getConnection();

        try {
            const [rows] = await connection.execute(`
                SELECT id, payload, processed 
                FROM server_notifications 
                WHERE id = ? AND processed = FALSE
                LIMIT 1
            `, [notificationId]);

            return rows.length > 0 ? rows[0] : null;

        } finally {
            connection.release();
        }
    }

    /**
     * Busca informações do Discord do jogador.
     */
    static async getPlayerDiscordInfo(playerName) {
        const connection = await pool.getConnection();

        try {
            const [rows] = await connection.execute(`
                SELECT dl.discord_id, pd.uuid as player_uuid, pd.name as player_name, dl.verified
                FROM discord_links dl
                JOIN player_data pd ON dl.player_id = pd.player_id
                WHERE pd.name = ? AND dl.verified = TRUE
                LIMIT 1
            `, [playerName]);

            return rows.length > 0 ? rows[0] : null;

        } finally {
            connection.release();
        }
    }
}

module.exports = IPAuthHandler;
