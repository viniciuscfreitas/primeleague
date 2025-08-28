const { EmbedBuilder, ActionRowBuilder, ButtonBuilder, ButtonStyle } = require('discord.js');
const config = require('../config/bot-config.json');

/**
 * Handler para webhook de autorização de IP
 * Recebe notificações do Core quando IP não autorizado é detectado
 */
class IpAuthWebhookHandler {
    constructor(client) {
        this.client = client;
        this.pendingNotifications = new Map(); // Cache de notificações pendentes
        this.bearerToken = config.coreApi.bearerToken;
    }

    /**
     * Configura o endpoint do webhook
     */
    setupWebhook(app) {
        app.post('/webhooks/ip-auth-notification', (req, res) => {
            this.handleIpAuthNotification(req, res);
        });
    }

    /**
     * Processa notificação de IP não autorizado
     * CORREÇÃO: Validação de schema robusta implementada
     */
    async handleIpAuthNotification(req, res) {
        try {
            // Verificar autenticação Bearer Token
            const authHeader = req.headers.authorization;
            if (!authHeader || !authHeader.startsWith('Bearer ') || 
                authHeader.substring(7) !== this.bearerToken) {
                return res.status(401).json({ error: 'Unauthorized' });
            }

            // VALIDAÇÃO DE SCHEMA ROBUSTA (CORREÇÃO CRÍTICA)
            const validationResult = this.validateIpAuthPayload(req.body);
            if (!validationResult.isValid) {
                console.error('[IP-AUTH] Payload malformado rejeitado:', {
                    payload: req.body,
                    errors: validationResult.errors
                });
                return res.status(400).json({ 
                    error: 'Payload inválido', 
                    details: validationResult.errors 
                });
            }

            const { playerName, playerId, ipAddress, discordId, timestamp, serverName } = req.body;

            // Verificar se já existe notificação pendente para este IP
            const notificationKey = `${discordId}-${ipAddress}`;
            if (this.pendingNotifications.has(notificationKey)) {
                console.log(`[IP-AUTH] Notificação duplicada ignorada para ${playerName} (${ipAddress})`);
                return res.status(200).json({ success: true, message: 'Notificação duplicada ignorada' });
            }

            // Adicionar à cache de notificações pendentes
            this.pendingNotifications.set(notificationKey, {
                playerName,
                playerId,
                ipAddress,
                discordId,
                timestamp,
                serverName,
                createdAt: Date.now()
            });

            // Enviar DM com botões
            const success = await this.sendIpAuthDM(playerName, ipAddress, discordId, serverName);

            if (success) {
                console.log(`[IP-AUTH] DM enviada para ${playerName} (${discordId}) sobre IP ${ipAddress}`);
                res.status(200).json({ success: true, message: 'DM enviada com sucesso' });
            } else {
                // Remover da cache se falhou
                this.pendingNotifications.delete(notificationKey);
                res.status(500).json({ error: 'Erro ao enviar DM' });
            }

        } catch (error) {
            console.error('[IP-AUTH] Erro no webhook:', error);
            res.status(500).json({ error: 'Erro interno do servidor' });
        }
    }

    /**
     * Envia DM com botões para autorização de IP
     */
    async sendIpAuthDM(playerName, ipAddress, discordId, serverName) {
        try {
            // Buscar usuário Discord
            const user = await this.client.users.fetch(discordId);
            if (!user) {
                console.error(`[IP-AUTH] Usuário Discord não encontrado: ${discordId}`);
                return false;
            }

            // Criar embed informativo
            const embed = new EmbedBuilder()
                .setTitle('🔐 Autorização de IP Necessária')
                .setDescription(`Detectamos uma tentativa de conexão de um IP não autorizado no servidor **${serverName}**.`)
                .addFields(
                    { name: '👤 Player', value: playerName, inline: true },
                    { name: '🌐 Endereço IP', value: `\`${ipAddress}\``, inline: true },
                    { name: '🕐 Timestamp', value: `<t:${Math.floor(Date.now() / 1000)}:R>`, inline: true },
                    { name: 'ℹ️ O que fazer?', value: 'Clique em **Autorizar** se este IP é confiável, ou **Rejeitar** se não reconhece este endereço.' }
                )
                .setColor(config.ui.colors.warning)
                .setFooter({ text: config.ui.footer })
                .setTimestamp();

            // Criar botões
            const buttons = new ActionRowBuilder()
                .addComponents(
                    new ButtonBuilder()
                        .setCustomId(`ip_auth_allow_${playerName}_${ipAddress}`)
                        .setLabel('✅ Autorizar IP')
                        .setStyle(ButtonStyle.Success),
                    new ButtonBuilder()
                        .setCustomId(`ip_auth_deny_${playerName}_${ipAddress}`)
                        .setLabel('❌ Rejeitar IP')
                        .setStyle(ButtonStyle.Danger)
                );

            // Enviar DM
            await user.send({ embeds: [embed], components: [buttons] });

            // Configurar timeout para remover da cache (5 minutos)
            setTimeout(() => {
                this.pendingNotifications.delete(`${discordId}-${ipAddress}`);
                console.log(`[IP-AUTH] Timeout de notificação para ${playerName} (${ipAddress})`);
            }, 5 * 60 * 1000);

            return true;

        } catch (error) {
            console.error(`[IP-AUTH] Erro ao enviar DM para ${discordId}:`, error);
            return false;
        }
    }

    /**
     * Processa interação dos botões de autorização
     */
    async handleButtonInteraction(interaction) {
        const customId = interaction.customId;

        if (!customId.startsWith('ip_auth_')) {
            return false; // Não é nosso botão
        }

        try {
            // Extrair dados do customId
            const parts = customId.split('_');
            if (parts.length !== 4) {
                await interaction.reply({ content: '❌ Erro interno: dados inválidos', ephemeral: true });
                return true;
            }

            const action = parts[2]; // 'allow' ou 'deny'
            const playerName = parts[3];
            const ipAddress = parts[4];

            // Verificar se a notificação ainda está pendente
            const notificationKey = `${interaction.user.id}-${ipAddress}`;
            const notification = this.pendingNotifications.get(notificationKey);

            if (!notification) {
                await interaction.reply({ 
                    content: '❌ Esta autorização expirou ou já foi processada. Tente conectar novamente ao servidor.', 
                    ephemeral: true 
                });
                return true;
            }

            // Remover da cache
            this.pendingNotifications.delete(notificationKey);

            // Processar decisão
            const authorized = action === 'allow';
            const success = await this.sendDecisionToCore(playerName, ipAddress, authorized, interaction.user.id);

            if (success) {
                const message = authorized 
                    ? `✅ IP \`${ipAddress}\` autorizado com sucesso! Você pode tentar conectar ao servidor novamente.`
                    : `❌ IP \`${ipAddress}\` rejeitado. Este endereço não será autorizado.`;

                await interaction.reply({ content: message, ephemeral: true });
                console.log(`[IP-AUTH] Decisão processada: ${playerName} ${authorized ? 'autorizou' : 'rejeitou'} IP ${ipAddress}`);
            } else {
                await interaction.reply({ 
                    content: '❌ Erro ao processar sua decisão. Tente novamente ou contate um administrador.', 
                    ephemeral: true 
                });
            }

            return true;

        } catch (error) {
            console.error('[IP-AUTH] Erro ao processar interação:', error);
            await interaction.reply({ 
                content: '❌ Erro interno ao processar sua decisão.', 
                ephemeral: true 
            });
            return true;
        }
    }

    /**
     * Envia decisão para o Core
     */
    async sendDecisionToCore(playerName, ipAddress, authorized, discordId) {
        try {
            const payload = {
                playerName,
                ipAddress,
                authorized,
                discordId,
                timestamp: Date.now()
            };

            const response = await fetch(`${config.coreApi.baseUrl}/api/v1/ip-authorize`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.bearerToken}`
                },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                console.log(`[IP-AUTH] Decisão enviada para Core: ${playerName} ${authorized ? 'autorizou' : 'rejeitou'} ${ipAddress}`);
                return true;
            } else {
                console.error(`[IP-AUTH] Erro ao enviar decisão para Core: ${response.status} ${response.statusText}`);
                return false;
            }

        } catch (error) {
            console.error('[IP-AUTH] Erro ao enviar decisão para Core:', error);
            return false;
        }
    }

    /**
     * Valida schema do payload de autorização de IP
     * CORREÇÃO: Validação robusta para evitar crashes
     */
    validateIpAuthPayload(payload) {
        const errors = [];
        
        // Verificar se payload é um objeto
        if (!payload || typeof payload !== 'object') {
            errors.push('Payload deve ser um objeto válido');
            return { isValid: false, errors };
        }
        
        // Validar campos obrigatórios
        const requiredFields = {
            playerName: 'string',
            playerId: 'number',
            ipAddress: 'string',
            discordId: 'string',
            timestamp: 'number',
            serverName: 'string'
        };
        
        for (const [field, expectedType] of Object.entries(requiredFields)) {
            if (!(field in payload)) {
                errors.push(`Campo obrigatório ausente: ${field}`);
                continue;
            }
            
            const value = payload[field];
            const actualType = typeof value;
            
            if (actualType !== expectedType) {
                errors.push(`Campo ${field} deve ser ${expectedType}, recebido ${actualType}`);
                continue;
            }
            
            // Validações específicas por campo
            switch (field) {
                case 'playerName':
                    if (value.trim().length === 0 || value.length > 16) {
                        errors.push('playerName deve ter entre 1 e 16 caracteres');
                    }
                    break;
                    
                case 'playerId':
                    if (value <= 0 || !Number.isInteger(value)) {
                        errors.push('playerId deve ser um número inteiro positivo');
                    }
                    break;
                    
                case 'ipAddress':
                    if (!this.isValidIpAddress(value)) {
                        errors.push('ipAddress deve ser um endereço IP válido');
                    }
                    break;
                    
                case 'discordId':
                    if (!/^\d{17,19}$/.test(value)) {
                        errors.push('discordId deve ser um ID do Discord válido (17-19 dígitos)');
                    }
                    break;
                    
                case 'timestamp':
                    if (value <= 0 || value > Date.now() + 60000) { // Não pode ser futuro + 1 minuto
                        errors.push('timestamp deve ser um timestamp válido');
                    }
                    break;
                    
                case 'serverName':
                    if (value.trim().length === 0 || value.length > 50) {
                        errors.push('serverName deve ter entre 1 e 50 caracteres');
                    }
                    break;
            }
        }
        
        return {
            isValid: errors.length === 0,
            errors
        };
    }
    
    /**
     * Valida formato de endereço IP
     */
    isValidIpAddress(ip) {
        // Regex para IPv4
        const ipv4Regex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/;
        
        // Regex para IPv6 (simplificado)
        const ipv6Regex = /^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$/;
        
        return ipv4Regex.test(ip) || ipv6Regex.test(ip);
    }

    /**
     * Limpa notificações expiradas
     */
    cleanupExpiredNotifications() {
        const now = Date.now();
        const expiredKeys = [];

        for (const [key, notification] of this.pendingNotifications.entries()) {
            if (now - notification.createdAt > 5 * 60 * 1000) { // 5 minutos
                expiredKeys.push(key);
            }
        }

        expiredKeys.forEach(key => {
            this.pendingNotifications.delete(key);
            console.log(`[IP-AUTH] Notificação expirada removida: ${key}`);
        });
    }
}

module.exports = IpAuthWebhookHandler;
