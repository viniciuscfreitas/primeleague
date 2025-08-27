const { EmbedBuilder } = require('discord.js');
const {
    getGlobalClanStats,
    getTopActiveClans,
    getSystemConfig
} = require('../database/mysql');

/**
 * Worker para atualização automática do status global do servidor.
 * 
 * Funcionalidades:
 * - Atualiza mensagem de status a cada minuto
 * - Mostra estatísticas globais de slots em uso
 * - Exibe top clãs ativos
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
                console.log('⚠️ StatusWorker já está rodando');
                return;
            }

            // Carregar configurações
            await this.loadConfig();
            
            if (!this.statusChannelId) {
                console.log('⚠️ Canal de status não configurado. Use STATUS_CHANNEL_ID no .env');
                return;
            }

            // Primeira atualização imediata
            await this.updateStatus();

            // Agendar atualizações periódicas
            this.intervalId = setInterval(this.updateStatus, this.updateInterval);
            this.isRunning = true;

            console.log(`✅ StatusWorker iniciado - Canal: ${this.statusChannelId}`);
            console.log(`📊 Atualizações a cada ${this.updateInterval / 1000}s`);

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
        console.log('⏹️ StatusWorker parado');
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
            // Buscar dados em paralelo para melhor performance
            const [globalStats, topClans] = await Promise.all([
                getGlobalClanStats(),
                getTopActiveClans(5)
            ]);

            // Criar embed de status
            const embed = await this.createStatusEmbed(globalStats, topClans);

            // Buscar ou criar mensagem de status
            const channel = await this.client.channels.fetch(this.statusChannelId);
            if (!channel) {
                console.error(`❌ Canal ${this.statusChannelId} não encontrado`);
                return;
            }

            if (this.statusMessageId) {
                // Atualizar mensagem existente
                try {
                    const message = await channel.messages.fetch(this.statusMessageId);
                    await message.edit({ embeds: [embed] });
                } catch (error) {
                    // Mensagem não encontrada, criar nova
                    console.log('🔄 Mensagem de status não encontrada, criando nova...');
                    this.statusMessageId = null;
                }
            }

            if (!this.statusMessageId) {
                // Criar nova mensagem de status
                const message = await channel.send({ embeds: [embed] });
                this.statusMessageId = message.id;
                
                // Salvar ID da mensagem no banco (opcional)
                await this.saveMessageId(message.id);
            }

        } catch (error) {
            console.error('❌ Erro ao atualizar status:', error);
        }
    }

    /**
     * Cria o embed de status com informações atualizadas.
     */
    async createStatusEmbed(globalStats, topClans) {
        const now = new Date();
        const uptimeHours = Math.floor(process.uptime() / 3600);
        const uptimeMinutes = Math.floor((process.uptime() % 3600) / 60);

        const embed = new EmbedBuilder()
            .setColor('#4ECDC4')
            .setTitle('⚔️ Prime League - Status do Servidor ⚔️')
            .setDescription('**Sistema de Clãs V2.0** • Status em tempo real')
            .setTimestamp(now);

        // Estatísticas principais
        const totalSlots = globalStats.total_slots || 0;
        const usedSlots = globalStats.used_slots || 0;
        const availableSlots = totalSlots - usedSlots;
        const usagePercentage = totalSlots > 0 ? Math.round((usedSlots / totalSlots) * 100) : 0;

        // Barra de progresso visual
        const progressBar = this.createProgressBar(usagePercentage, 20);

        embed.addFields({
            name: '📊 Slots Globais',
            value: 
                `**Em Uso:** ${usedSlots}/${totalSlots} slots\n` +
                `**Disponíveis:** ${availableSlots} slots\n` +
                `**Taxa de Uso:** ${usagePercentage}%\n` +
                `${progressBar}`,
            inline: false
        });

        // Estatísticas de clãs
        embed.addFields({
            name: '🏰 Estatísticas de Clãs',
            value: 
                `**Clãs Ativos:** ${globalStats.active_clans || 0}\n` +
                `**Clãs Online:** ${globalStats.clans_with_sessions || 0}\n` +
                `**Jogadores Online:** ${globalStats.total_players_online || 0}\n` +
                `**Sessões Ativas:** ${usedSlots}`,
            inline: true
        });

        // Distribuição por tiers
        const tierStats = globalStats.tier_distribution || {};
        embed.addFields({
            name: '💎 Distribuição por Planos',
            value: 
                `👑 **Império:** ${tierStats.IMPERIO || 0} clãs\n` +
                `🏛️ **Guilda:** ${tierStats.GUILDA || 0} clãs\n` +
                `⚔️ **Esquadrão:** ${tierStats.ESQUADRAO || 0} clãs\n` +
                `🥷 **Lutador:** ${tierStats.LUTADOR || 0} clãs`,
            inline: true
        });

        // Top clãs ativos
        if (topClans && topClans.length > 0) {
            let topClansText = '';
            topClans.forEach((clan, index) => {
                const medal = ['🥇', '🥈', '🥉', '🏅', '🏅'][index] || '🏅';
                const percentage = clan.max_slots > 0 ? Math.round((clan.active_sessions / clan.max_slots) * 100) : 0;
                topClansText += `${medal} **${clan.subscription_tier}** ${clan.active_sessions}/${clan.max_slots} (${percentage}%)\n`;
            });

            embed.addFields({
                name: '🏆 Top Clãs Ativos',
                value: topClansText,
                inline: false
            });
        }

        // Informações do sistema
        embed.addFields({
            name: '⚙️ Sistema',
            value: 
                `**Status:** 🟢 Online\n` +
                `**Uptime:** ${uptimeHours}h ${uptimeMinutes}m\n` +
                `**Última Atualização:** <t:${Math.floor(now.getTime() / 1000)}:R>\n` +
                `**Versão:** Sistema de Clãs V2.0`,
            inline: false
        });

        // Indicador de saúde do sistema
        let healthStatus = '🟢 Excelente';
        if (usagePercentage > 90) {
            healthStatus = '🔴 Sobregarga';
        } else if (usagePercentage > 75) {
            healthStatus = '🟡 Alto Uso';
        } else if (usagePercentage > 50) {
            healthStatus = '🟢 Moderado';
        }

        embed.setFooter({ 
            text: `Saúde do Sistema: ${healthStatus} • Atualização automática a cada minuto` 
        });

        return embed;
    }

    /**
     * Cria uma barra de progresso visual.
     */
    createProgressBar(percentage, length = 20) {
        const filled = Math.round((percentage / 100) * length);
        const empty = length - filled;
        
        const fillChar = '█';
        const emptyChar = '▒';
        
        return `\`[${'█'.repeat(filled)}${'▒'.repeat(empty)}]\` ${percentage}%`;
    }

    /**
     * Salva o ID da mensagem de status no banco.
     */
    async saveMessageId(messageId) {
        try {
            // Esta função salvaria o messageId no banco para persistência
            // Por simplicidade, vamos apenas logar
            console.log(`💾 ID da mensagem de status: ${messageId}`);
        } catch (error) {
            console.error('❌ Erro ao salvar ID da mensagem:', error);
        }
    }

    /**
     * Obtém status atual do worker.
     */
    getStatus() {
        return {
            isRunning: this.isRunning,
            statusChannelId: this.statusChannelId,
            statusMessageId: this.statusMessageId,
            updateInterval: this.updateInterval,
            nextUpdate: this.intervalId ? new Date(Date.now() + this.updateInterval) : null
        };
    }

    /**
     * Força uma atualização imediata.
     */
    async forceUpdate() {
        console.log('🔄 Forçando atualização do status...');
        await this.updateStatus();
    }
}

module.exports = StatusWorker;
