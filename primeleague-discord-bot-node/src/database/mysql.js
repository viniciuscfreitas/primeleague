const mysql = require('mysql2/promise');
require('dotenv').config();

// Configurações do banco
const dbConfig = {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'root',
    database: 'primeleague',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0,
    authPlugins: {
        mysql_native_password: () => () => Buffer.from('root', 'utf-8')
    }
};

console.log('Conectando ao MySQL:', {
    host: dbConfig.host,
    port: dbConfig.port,
    user: dbConfig.user,
    database: dbConfig.database
});

const pool = mysql.createPool(dbConfig);

async function getPlayerByNickname(nickname) {
    try {
        const [rows] = await pool.execute(
            `SELECT pd.player_id, pd.name, pd.elo, pd.money, pd.subscription_expires_at,
                    dl.discord_id, dl.verified
             FROM player_data pd
             LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
             WHERE pd.name = ?`,
            [nickname]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar jogador:', error);
        return null;
    }
}

async function getDiscordLink(discordId) {
    try {
        const [rows] = await pool.execute(
            'SELECT player_id, player_name FROM discord_links WHERE discord_id = ?',
            [discordId]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar vínculo Discord:', error);
        return null;
    }
}

async function createDiscordLink(discordId, playerId, playerName, verifyCode = null) {
    try {
        const verifyExpiresAt = verifyCode ? new Date(Date.now() + 5 * 60 * 1000) : null; // 5 minutos
        const [result] = await pool.execute(
            `INSERT INTO discord_links (discord_id, player_id, player_name, verified, verify_code, verify_expires_at)
             VALUES (?, ?, ?, ?, ?, ?)`,
            [discordId, playerId, playerName, false, verifyCode, verifyExpiresAt]
        );
        return result.affectedRows > 0;
    } catch (error) {
        console.error('Erro ao criar vínculo Discord:', error);
        return false;
    }
}

async function getPlayerAccountInfo(discordId) {
    try {
        const [rows] = await pool.execute(
            `SELECT dl.player_id, dl.player_name, pd.elo, pd.money, pd.subscription_expires_at
             FROM discord_links dl
             JOIN player_data pd ON dl.player_id = pd.player_id
             WHERE dl.discord_id = ? AND dl.verified = TRUE`,
            [discordId]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar informações da conta:', error);
        return null;
    }
}

async function getVerificationStatus(discordId) {
    try {
        const [rows] = await pool.execute(
            'SELECT verified, verify_code, verify_expires_at FROM discord_links WHERE discord_id = ?',
            [discordId]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar status de verificação:', error);
        return null;
    }
}

async function verifyDiscordLink(playerName, verifyCode) {
    try {
        const [result] = await pool.execute(
            `UPDATE discord_links
             SET verified = TRUE, verify_code = NULL, verify_expires_at = NULL
             WHERE player_name = ? AND verify_code = ? AND verify_expires_at > NOW()`,
            [playerName, verifyCode]
        );
        return result.affectedRows > 0;
    } catch (error) {
        console.error('Erro ao verificar vínculo Discord:', error);
        return false;
    }
}
async function createServerNotification(actionType, payload) {
    try {
        const [result] = await pool.execute(
            'INSERT INTO server_notifications (action_type, payload) VALUES (?, ?)',
            [actionType, JSON.stringify(payload)]
        );
        return result.affectedRows > 0;
    } catch (error) {
        console.error('Erro ao criar notificação para servidor:', error);
        return false;
    }
}

async function generateVerifyCode() {
    // Gera código de 6 dígitos
    return Math.floor(100000 + Math.random() * 900000).toString();
}

// Testar conexão ao iniciar
pool.getConnection()
    .then(conn => {
        console.log('✅ Conexão com MySQL estabelecida!');
        conn.release();
    })
    .catch(err => {
        console.error('❌ Erro ao conectar com MySQL:', err);
    });

// =====================================================
// NOVAS FUNÇÕES PARA SISTEMA DE CLÃS
// =====================================================

async function getDiscordLinksById(discordId) {
    try {
        const [rows] = await pool.execute(
            `SELECT id, discord_id, player_id, player_name, is_primary, verified, 
                    verify_code, verify_expires_at, linked_at, verified_at
             FROM discord_links 
             WHERE discord_id = ? 
             ORDER BY is_primary DESC, linked_at ASC`,
            [discordId]
        );
        return rows;
    } catch (error) {
        console.error('Erro ao buscar vínculos do clã:', error);
        return [];
    }
}

async function createClanMemberLink(discordId, playerId, playerName, verifyCode, isPrimary = false) {
    try {
        const verifyExpiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutos
        const [result] = await pool.execute(
            `INSERT INTO discord_links 
             (discord_id, player_id, player_name, is_primary, verified, verify_code, verify_expires_at)
             VALUES (?, ?, ?, ?, FALSE, ?, ?)`,
            [discordId, playerId, playerName, isPrimary, verifyCode, verifyExpiresAt]
        );
        return result.affectedRows > 0;
    } catch (error) {
        console.error('Erro ao criar vínculo de membro do clã:', error);
        return false;
    }
}

async function removeClanMember(playerId) {
    try {
        const [result] = await pool.execute(
            'DELETE FROM discord_links WHERE player_id = ?',
            [playerId]
        );
        return result.affectedRows > 0;
    } catch (error) {
        console.error('Erro ao remover membro do clã:', error);
        return false;
    }
}

async function getClanSubscription(discordOwnerId) {
    // Função removida - tabela subscriptions não existe no schema atual
    console.log('Função getClanSubscription removida - tabela subscriptions não existe');
    return null;
}

async function getClanActiveSessionsCount(discordOwnerId) {
    // Função removida - tabela session_history não existe no schema atual
    console.log('Função getClanActiveSessionsCount removida - tabela session_history não existe');
    return 0;
}

async function getClanMemberCount(discordOwnerId) {
    try {
        const [rows] = await pool.execute(
            `SELECT COUNT(*) as member_count
             FROM discord_links 
             WHERE discord_id = ? AND verified = TRUE`,
            [discordOwnerId]
        );
        return rows[0]?.member_count || 0;
    } catch (error) {
        console.error('Erro ao contar membros do clã:', error);
        return 0;
    }
}

async function getSubscriptionTiers() {
    // Função removida - tabela subscription_tiers não existe no schema atual
    console.log('Função getSubscriptionTiers removida - tabela subscription_tiers não existe');
    return [];
}

async function updateSubscription(discordOwnerId, subscriptionTier, maxSlots, expiresAt) {
    // Função removida - tabela subscriptions não existe no schema atual
    console.log('Função updateSubscription removida - tabela subscriptions não existe');
    return false;
}

async function getClanActiveSessions(discordOwnerId) {
    // Função removida - tabela session_history não existe no schema atual
    console.log('Função getClanActiveSessions removida - tabela session_history não existe');
    return [];
}

// =====================================================
// FUNÇÕES PARA SISTEMA DE STATUS GLOBAL
// =====================================================

async function getGlobalClanStats() {
    // Função removida - tabelas subscriptions e session_history não existem no schema atual
    console.log('Função getGlobalClanStats removida - tabelas não existem no schema');
    return {
        total_slots: 0,
        clans_with_sessions: 0,
        total_players_online: 0,
        used_slots: 0,
        active_clans: 0,
        tier_distribution: {}
    };
}

async function getTopActiveClans(limit = 5) {
    // Função removida - tabelas subscriptions e session_history não existem no schema atual
    console.log('Função getTopActiveClans removida - tabelas não existem no schema');
    return [];
}

async function getSystemConfig() {
    // Função removida - tabela system_config não existe no schema atual
    console.log('Função getSystemConfig removida - tabela não existe no schema');
    return [];
}

async function getServerMetrics() {
    try {
        const [rows] = await pool.execute(`
            SELECT 
                'total_registered_players' as metric,
                COUNT(*) as value
            FROM player_data
            UNION ALL
            SELECT 
                'total_discord_links' as metric,
                COUNT(*) as value
            FROM discord_links
            UNION ALL
            SELECT 
                'verified_links' as metric,
                COUNT(*) as value
            FROM discord_links WHERE verified = TRUE
            UNION ALL
            SELECT 
                'active_subscriptions' as metric,
                COUNT(*) as value
            FROM player_data 
            WHERE subscription_expires_at > NOW()
        `);

        const metrics = {};
        rows.forEach(row => {
            metrics[row.metric] = row.value;
        });

        return metrics;
    } catch (error) {
        console.error('Erro ao buscar métricas do servidor:', error);
        return {};
    }
}

// =====================================================
// FUNÇÕES DO SISTEMA DE PORTFÓLIO DE CONTAS
// =====================================================

/**
 * Busca o portfólio completo de um usuário Discord.
 * Inclui todas as contas vinculadas com status de assinatura.
 */
async function getPortfolioByDiscordId(discordId) {
    try {
        const [rows] = await pool.execute(`
            SELECT 
                dl.player_id,
                dl.player_name,
                dl.is_primary,
                dl.linked_at,
                dl.verified,
                pd.subscription_expires_at,
                CASE 
                    WHEN pd.subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
                    WHEN pd.subscription_expires_at < NOW() THEN 'EXPIRED'
                    ELSE 'ACTIVE'
                END as subscription_status,
                CASE 
                    WHEN pd.subscription_expires_at > NOW() THEN 
                        DATEDIFF(pd.subscription_expires_at, NOW())
                    ELSE 0 
                END as days_remaining
            FROM discord_links dl
            LEFT JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE dl.discord_id = ? AND dl.verified = TRUE
            ORDER BY dl.is_primary DESC, dl.linked_at ASC
        `, [discordId]);
        
        return rows;
    } catch (error) {
        console.error('Erro ao buscar portfólio:', error);
        return [];
    }
}

/**
 * Busca estatísticas do portfólio para um usuário específico.
 */
async function getPortfolioStats(discordId) {
    try {
        const [rows] = await pool.execute(`
            SELECT 
                COUNT(*) as total_accounts,
                COUNT(CASE WHEN pd.subscription_expires_at > NOW() THEN 1 END) as active_subscriptions,
                COUNT(CASE WHEN pd.subscription_expires_at <= NOW() THEN 1 END) as expired_subscriptions,
                COUNT(CASE WHEN pd.subscription_expires_at IS NULL THEN 1 END) as never_subscribed
            FROM discord_links dl
            LEFT JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE dl.discord_id = ? AND dl.verified = TRUE
        `, [discordId]);
        
        return rows[0] || {
            total_accounts: 0,
            active_subscriptions: 0,
            expired_subscriptions: 0,
            never_subscribed: 0
        };
    } catch (error) {
        console.error('Erro ao buscar estatísticas do portfólio:', error);
        return {
            total_accounts: 0,
            active_subscriptions: 0,
            expired_subscriptions: 0,
            never_subscribed: 0
        };
    }
}

/**
 * Vincula uma nova conta ao portfólio de um usuário Discord.
 * Sistema simplificado 1-para-N.
 */
async function addAccountToPortfolio(discordId, playerId, playerName, isPrimary = false) {
    try {
        // Se for definida como primária, remover flag primary de outras contas
        if (isPrimary) {
            await pool.execute(
                'UPDATE discord_links SET is_primary = FALSE WHERE discord_id = ?',
                [discordId]
            );
        }

        const [result] = await pool.execute(`
            INSERT INTO discord_links (discord_id, player_id, player_name, is_primary, verified)
            VALUES (?, ?, ?, ?, FALSE)
        `, [discordId, playerId, playerName, isPrimary]);
        
        return { success: true, linkId: result.insertId };
    } catch (error) {
        console.error('Erro ao adicionar conta ao portfólio:', error);
        if (error.code === 'ER_DUP_ENTRY') {
            return { 
                success: false, 
                error: 'Esta conta já está vinculada a outro usuário Discord.' 
            };
        }
        return { success: false, error: 'Erro interno do banco de dados.' };
    }
}

/**
 * Remove uma conta do portfólio de um usuário.
 */
async function removeAccountFromPortfolio(discordId, playerName) {
    try {
        const [result] = await pool.execute(`
            DELETE FROM discord_links 
            WHERE discord_id = ? AND player_name = ? AND verified = TRUE
        `, [discordId, playerName]);
        
        return { 
            success: result.affectedRows > 0,
            removedCount: result.affectedRows
        };
    } catch (error) {
        console.error('Erro ao remover conta do portfólio:', error);
        return { success: false, error: 'Erro interno do banco de dados.' };
    }
}

/**
 * Renova a assinatura de uma conta específica.
 * Fonte da verdade: player_data.subscription_expires_at
 */
async function renewAccountSubscription(playerId, days = 30) {
    try {
        // Verificar se a conta existe
        const [playerCheck] = await pool.execute(
            'SELECT subscription_expires_at FROM player_data WHERE player_id = ?',
            [playerId]
        );
        
        if (playerCheck.length === 0) {
            return { success: false, error: 'Conta não encontrada.' };
        }

        const currentExpiry = playerCheck[0].subscription_expires_at;
        let newExpiry;

        if (!currentExpiry || new Date(currentExpiry) < new Date()) {
            // Assinatura expirada ou inexistente - começar de agora
            newExpiry = new Date();
            newExpiry.setDate(newExpiry.getDate() + days);
        } else {
            // Assinatura ativa - estender a partir da data atual de expiração
            newExpiry = new Date(currentExpiry);
            newExpiry.setDate(newExpiry.getDate() + days);
        }

        // Atualizar a assinatura
        const [result] = await pool.execute(
            'UPDATE player_data SET subscription_expires_at = ? WHERE player_id = ?',
            [newExpiry, playerId]
        );

        return { 
            success: result.affectedRows > 0,
            newExpiry: newExpiry,
            daysAdded: days
        };
    } catch (error) {
        console.error('Erro ao renovar assinatura:', error);
        return { success: false, error: 'Erro interno do banco de dados.' };
    }
}

/**
 * Verifica se um nickname está disponível para vinculação.
 */
async function isNicknameAvailableForLinking(nickname) {
    try {
        // Verificar se o nickname já está vinculado a algum Discord ID
        const [existingLinks] = await pool.execute(
            'SELECT discord_id FROM discord_links WHERE player_name = ? AND verified = TRUE',
            [nickname]
        );

        // Verificar se o player existe no banco
        const [playerExists] = await pool.execute(
            'SELECT uuid FROM player_data WHERE name = ?',
            [nickname]
        );

        return {
            available: existingLinks.length === 0,
            playerExists: playerExists.length > 0,
            currentOwner: existingLinks.length > 0 ? existingLinks[0].discord_id : null
        };
    } catch (error) {
        console.error('Erro ao verificar disponibilidade do nickname:', error);
        return { available: false, playerExists: false, currentOwner: null };
    }
}

/**
 * Busca informações de uma conta específica no portfólio.
 */
async function getAccountInfoDetailed(playerName) {
    try {
        const [rows] = await pool.execute(`
            SELECT 
                pd.player_id,
                pd.name,
                pd.subscription_expires_at,
                dl.discord_id,
                dl.is_primary,
                dl.verified,
                dl.linked_at,
                CASE 
                    WHEN pd.subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
                    WHEN pd.subscription_expires_at < NOW() THEN 'EXPIRED'
                    ELSE 'ACTIVE'
                END as subscription_status,
                CASE 
                    WHEN pd.subscription_expires_at > NOW() THEN 
                        DATEDIFF(pd.subscription_expires_at, NOW())
                    ELSE 0 
                END as days_remaining
            FROM player_data pd
            LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
            WHERE pd.name = ?
        `, [playerName]);
        
        return rows[0] || null;
    } catch (error) {
        console.error('Erro ao buscar informações da conta:', error);
        return null;
    }
}

/**
 * Formata o status de assinatura para exibição.
 */
function formatSubscriptionStatus(status, daysRemaining = 0) {
    switch (status) {
        case 'ACTIVE':
            return `🟢 Ativa (${daysRemaining} dia${daysRemaining !== 1 ? 's' : ''} restante${daysRemaining !== 1 ? 's' : ''})`;
        case 'EXPIRED':
            return '🔴 Expirada';
        case 'NEVER_SUBSCRIBED':
            return '⚪ Nunca Assinou';
        default:
            return '⚫ Status Desconhecido';
    }
}

module.exports = {
    pool,
    // Funções principais (mantidas para compatibilidade)
    getPlayerByNickname,
    getDiscordLink,
    createDiscordLink,
    getPlayerAccountInfo,
    getVerificationStatus,
    verifyDiscordLink,
    createServerNotification,
    generateVerifyCode,
    // Funções do sistema de clãs (depreciadas, mantidas para rollback)
    getDiscordLinksById,
    createClanMemberLink,
    removeClanMember,
    getClanSubscription,
    getClanActiveSessionsCount,
    getClanMemberCount,
    getSubscriptionTiers,
    updateSubscription,
    getClanActiveSessions,
    // Funções do sistema de status global
    getGlobalClanStats,
    getTopActiveClans,
    getSystemConfig,
    getServerMetrics,
    // ✨ NOVAS FUNÇÕES DO SISTEMA DE PORTFÓLIO
    getPortfolioByDiscordId,
    getPortfolioStats,
    addAccountToPortfolio,
    removeAccountFromPortfolio,
    renewAccountSubscription,
    isNicknameAvailableForLinking,
    getAccountInfoDetailed,
    formatSubscriptionStatus
};
