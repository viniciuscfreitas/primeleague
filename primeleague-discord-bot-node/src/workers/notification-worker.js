const { pool } = require('../database/mysql');
const ipAuthHandler = require('../handlers/ip-auth-handler');

/**
 * Worker de Notificações - Sistema de Autorização de IPs
 * 
 * Monitora a tabela server_notifications em busca de tarefas REQUEST_IP_AUTH
 * e processa solicitações de autorização de IP via DM no Discord.
 * 
 * @author PrimeLeague Team
 * @version 1.0.0
 */
class NotificationWorker {
    constructor(client) {
        this.client = client;
        this.isRunning = false;
        this.intervalId = null;
        this.pollingInterval = 5000; // 5 segundos
        this.processingSet = new Set(); // Evitar processamento duplicado
    }

    /**
     * Inicia o worker de notificações.
     */
    start() {
        if (this.isRunning) {
            console.log('[NotificationWorker] Worker já está em execução');
            return;
        }

        this.isRunning = true;
        console.log('[NotificationWorker] Iniciando worker de notificações...');

        // Processar imediatamente
        this.processNotifications();

        // Configurar polling
        this.intervalId = setInterval(() => {
            this.processNotifications();
        }, this.pollingInterval);

        console.log(`[NotificationWorker] Worker iniciado - polling a cada ${this.pollingInterval}ms`);
    }

    /**
     * Para o worker de notificações.
     */
    stop() {
        if (!this.isRunning) {
            console.log('[NotificationWorker] Worker já está parado');
            return;
        }

        this.isRunning = false;

        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }

        console.log('[NotificationWorker] Worker de notificações parado');
    }

    /**
     * Processa notificações pendentes da tabela server_notifications.
     */
    async processNotifications() {
        try {
            // Buscar notificações REQUEST_IP_AUTH pendentes
            const notifications = await this.getRequestIPAuthNotifications();

            if (notifications.length === 0) {
                return; // Nenhuma notificação pendente
            }

            console.log(`[NotificationWorker] Processando ${notifications.length} notificação(ões)`);

            // Processar cada notificação
            for (const notification of notifications) {
                await this.processIPAuthNotification(notification);
            }

        } catch (error) {
            console.error('[NotificationWorker] Erro ao processar notificações:', error);
        }
    }

    /**
     * Busca notificações REQUEST_IP_AUTH pendentes no banco.
     */
    async getRequestIPAuthNotifications() {
        const connection = await pool.getConnection();

        try {
            const [rows] = await connection.execute(`
                SELECT 
                    id,
                    action_type,
                    payload,
                    created_at
                FROM server_notifications 
                WHERE action_type = 'REQUEST_IP_AUTH' 
                  AND processed = FALSE
                ORDER BY created_at ASC
                LIMIT 10
            `);

            return rows;

        } finally {
            connection.release();
        }
    }

    /**
     * Processa uma notificação específica de REQUEST_IP_AUTH.
     */
    async processIPAuthNotification(notification) {
        const notificationId = notification.id;

        // Evitar processamento duplicado
        if (this.processingSet.has(notificationId)) {
            return;
        }

        this.processingSet.add(notificationId);

        try {
            console.log(`[NotificationWorker] Processando notificação ID: ${notificationId}`);

            // Parse do payload JSON
            const payload = JSON.parse(notification.payload);
            
            // Buscar informações do jogador no Discord
            const playerDiscordInfo = await this.getPlayerDiscordInfo(payload.player_name);

            if (!playerDiscordInfo) {
                console.log(`[NotificationWorker] Jogador ${payload.player_name} não está vinculado ao Discord`);
                await this.markNotificationAsProcessed(notificationId, false, 'Player not linked to Discord');
                return;
            }

            // Processar autorização via DM
            const success = await ipAuthHandler.sendIPAuthorizationDM(
                this.client,
                playerDiscordInfo.discord_id,
                payload,
                notificationId
            );

            if (success) {
                console.log(`[NotificationWorker] DM de autorização enviada para ${payload.player_name}`);
                await this.markNotificationAsProcessed(notificationId, true, 'DM sent successfully');
            } else {
                console.log(`[NotificationWorker] Falha ao enviar DM para ${payload.player_name}`);
                await this.markNotificationAsProcessed(notificationId, false, 'Failed to send DM');
            }

        } catch (error) {
            console.error(`[NotificationWorker] Erro ao processar notificação ${notificationId}:`, error);
            await this.markNotificationAsProcessed(notificationId, false, `Error: ${error.message}`);
        } finally {
            this.processingSet.delete(notificationId);
        }
    }

    /**
     * Busca informações do Discord do jogador.
     */
    async getPlayerDiscordInfo(playerName) {
        const connection = await pool.getConnection();

        try {
            const [rows] = await connection.execute(`
                SELECT 
                    dl.discord_id,
                    pd.uuid as player_uuid,
                    pd.name as player_name,
                    dl.verified
                FROM discord_links dl
                JOIN player_data pd ON dl.player_id = pd.player_id
                WHERE pd.name = ? 
                  AND dl.verified = TRUE
                LIMIT 1
            `, [playerName]);

            return rows.length > 0 ? rows[0] : null;

        } finally {
            connection.release();
        }
    }

    /**
     * Marca uma notificação como processada.
     */
    async markNotificationAsProcessed(notificationId, success, message) {
        const connection = await pool.getConnection();

        try {
            await connection.execute(`
                UPDATE server_notifications 
                SET 
                    processed = TRUE,
                    processed_at = NOW(),
                    processing_result = ?
                WHERE id = ?
            `, [
                JSON.stringify({ success, message, processed_by: 'notification-worker' }),
                notificationId
            ]);

            console.log(`[NotificationWorker] Notificação ${notificationId} marcada como processada: ${success ? 'SUCESSO' : 'FALHA'}`);

        } finally {
            connection.release();
        }
    }

    /**
     * Obtém estatísticas do worker para monitoramento.
     */
    getStats() {
        return {
            isRunning: this.isRunning,
            pollingInterval: this.pollingInterval,
            processingCount: this.processingSet.size,
            uptime: this.isRunning ? Date.now() - this.startTime : 0
        };
    }

    /**
     * Configura intervalo de polling.
     */
    setPollingInterval(intervalMs) {
        this.pollingInterval = intervalMs;
        
        if (this.isRunning) {
            // Reiniciar com novo intervalo
            this.stop();
            this.start();
        }

        console.log(`[NotificationWorker] Intervalo de polling alterado para ${intervalMs}ms`);
    }
}

module.exports = NotificationWorker;
