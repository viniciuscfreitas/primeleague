const { EmbedBuilder } = require('discord.js');
const {
    getServerMetrics
} = require('../database/mysql');

/**
 * Worker para atualização automática do status global do servidor.
 * 
 * Funcionalidades:
 * - Atualiza mensagem de status a cada minuto
 * - Mostra estatísticas básicas do servidor
 * - Informações de sistema em tempo real
 */
class StatusWorker {
    constructor(client) {
        this.client = client;
        this.isRunning = false;
        this.intervalId = null;
        this.statusChannelId = null;
        this.statusMessageId = null;
        this.updateInterval = 60000; // 1 minuto em millisegundos
        
        // Bind methods
        this.updateStatus = this.updateStatus.bind(this);
    }

    /**
     * Inicia o worker de status.
     */
    async start() {
        try {
                    if (this.isRunning) {
            return;
        }

            // Carregar configurações
            await this.loadConfig();
            
                    if (!this.statusChannelId) {
            return;
        }

            // Primeira atualização imediata
            await this.updateStatus();

            // Agendar atualizações periódicas
            this.intervalId = setInterval(this.updateStatus, this.updateInterval);
            this.isRunning = true;

        } catch (error) {
            console.error('❌ Erro ao iniciar StatusWorker:', error);
        }
    }

    /**
     * Para o worker de status.
     */
    stop() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
        
        this.isRunning = false;
    }

    /**
     * Carrega configurações do sistema.
     */
    async loadConfig() {
        // Usar variável de ambiente diretamente (tabela system_config não existe)
        this.statusChannelId = process.env.STATUS_CHANNEL_ID;
    }

    /**
     * Atualiza a mensagem de status.
     */
    async updateStatus() {
        try {
            // Buscar métricas do servidor
            const serverMetrics = await getServerMetrics();

            // Criar embed de status
            const embed = await this.createStatusEmbed(serverMetrics);

            // Buscar ou criar mensagem de status
            const channel = await this.client.channels.fetch(this.statusChannelId);
            if (!channel) {
                console.error('❌ Canal de status não encontrado:', this.statusChannelId);
                return;
            }

            // Se não temos uma mensagem salva, criar uma nova
            if (!this.statusMessageId) {
                const message = await channel.send({ embeds: [embed] });
                            this.statusMessageId = message.id;
            } else {
                // Atualizar mensagem existente
                try {
                    const message = await channel.messages.fetch(this.statusMessageId);
                    await message.edit({ embeds: [embed] });
                } catch (error) {
                    // Se a mensagem não existe mais, criar uma nova
                    const message = await channel.send({ embeds: [embed] });
                    this.statusMessageId = message.id;
                }
            }

        } catch (error) {
            console.error('❌ Erro ao atualizar status:', error);
        }
    }

    /**
     * Cria o embed de status com as informações do servidor.
     */
    async createStatusEmbed(serverMetrics) {
        const embed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('🖥️ Status do Servidor Prime League')
            .setDescription('Estatísticas em tempo real do servidor')
            .setTimestamp();

        // Métricas básicas do servidor
        const totalPlayers = serverMetrics.total_registered_players || 0;
        const totalLinks = serverMetrics.total_discord_links || 0;
        const verifiedLinks = serverMetrics.verified_links || 0;
        const activeSubscriptions = serverMetrics.active_subscriptions || 0;

        embed.addFields(
            {
                name: '👥 Jogadores',
                value: `**${totalPlayers.toLocaleString()}** registrados`,
                inline: true
            },
            {
                name: '🔗 Vínculos Discord',
                value: `**${verifiedLinks}** verificados de **${totalLinks}** total`,
                inline: true
            },
            {
                name: '💎 Assinaturas Ativas',
                value: `**${activeSubscriptions}** contas`,
                inline: true
            }
        );

        // Status do sistema
        const uptime = process.uptime();
        const uptimeHours = Math.floor(uptime / 3600);
        const uptimeMinutes = Math.floor((uptime % 3600) / 60);

        embed.addFields({
            name: '⚙️ Sistema',
            value: 
                `**Uptime:** ${uptimeHours}h ${uptimeMinutes}m\n` +
                `**Memória:** ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB\n` +
                `**Status:** 🟢 Online`,
            inline: false
        });

        // Informações adicionais
        embed.addFields({
            name: '📊 Informações',
            value: 
                '• Sistema de Apoiadores ativo\n' +
                '• Contas alternativas por tier\n' +
                '• Verificação via Discord\n' +
                '• API Core integrada',
            inline: false
        });

        return embed;
    }

    /**
     * Retorna o status atual do worker.
     */
    getStatus() {
        return {
            isRunning: this.isRunning,
            statusChannelId: this.statusChannelId,
            statusMessageId: this.statusMessageId,
            updateInterval: this.updateInterval
        };
    }
}

module.exports = StatusWorker;
