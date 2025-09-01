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



const pool = mysql.createPool(dbConfig);

async function getPlayerByNickname(nickname) {
    try {
        const [rows] = await pool.execute(
            `SELECT pd.player_id, pd.name, pd.elo, pd.money,
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
            'SELECT player_id FROM discord_links WHERE discord_id = ?',
            [discordId]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar vínculo Discord:', error);
        return null;
    }
}

async function getDiscordLinkByPlayerId(playerId) {
    try {
        const [rows] = await pool.execute(
            'SELECT discord_id, verified, verification_code, code_expires_at FROM discord_links WHERE player_id = ?',
            [playerId]
        );
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar vínculo Discord por player_id:', error);
        return null;
    }
}

async function createDiscordLink(discordId, playerId, verifyCode = null) {
    try {
        // 1. Primeiro, garantir que o discord_id existe em discord_users
        await pool.execute(
            'INSERT IGNORE INTO discord_users (discord_id, donor_tier, subscription_type) VALUES (?, 0, ?)',
            [discordId, 'BASIC']
        );
        
        // 2. Verificar se já existe um vínculo para este player_id
        const [existingRows] = await pool.execute(
            'SELECT link_id FROM discord_links WHERE player_id = ?',
            [playerId]
        );
        
        if (existingRows.length > 0) {
            return false;
        }
        
        const codeExpiresAt = verifyCode ? new Date(Date.now() + 5 * 60 * 1000) : null; // 5 minutos
        const [result] = await pool.execute(
            `INSERT INTO discord_links (discord_id, player_id, verified, verification_code, code_expires_at)
             VALUES (?, ?, ?, ?, ?)`,
            [discordId, playerId, false, verifyCode, codeExpiresAt]
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
            `SELECT pd.player_id, pd.name, pd.elo, pd.money
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

async function getVerificationStatus(discordId, playerId = null) {
    try {
        let sql, params;
        
        if (playerId) {
            // Buscar por discord_id E player_id específico
            sql = 'SELECT verified, verification_code, code_expires_at FROM discord_links WHERE discord_id = ? AND player_id = ?';
            params = [discordId, playerId];
        } else {
            // Buscar por discord_id (comportamento original)
            sql = 'SELECT verified, verification_code, code_expires_at FROM discord_links WHERE discord_id = ?';
            params = [discordId];
        }
        
        const [rows] = await pool.execute(sql, params);
        return rows[0];
    } catch (error) {
        console.error('Erro ao buscar status de verificação:', error);
        return null;
    }
}

async function verifyDiscordLink(playerName, verifyCode) {
    try {
        const [result] = await pool.execute(
            `UPDATE discord_links dl
             JOIN player_data pd ON dl.player_id = pd.player_id
             SET dl.verified = TRUE, dl.verification_code = NULL, dl.code_expires_at = NULL, dl.verified_at = NOW()
             WHERE pd.name = ? AND dl.verification_code = ? AND dl.code_expires_at > NOW()`,
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
        conn.release();
    })
    .catch(err => {
        console.error('Erro ao conectar com MySQL:', err);
    });

// =====================================================
// FUNÇÕES PARA SISTEMA DE CLÃS (REFATORADAS)
// =====================================================

async function getDiscordLinksById(discordId) {
    try {
        const [rows] = await pool.execute(
            `SELECT dl.discord_id, pd.player_id, pd.name as player_name, dl.is_primary, dl.verified, 
                    dl.verification_code, dl.code_expires_at, dl.verified_at
             FROM discord_links dl
             JOIN player_data pd ON dl.player_id = pd.player_id
             WHERE dl.discord_id = ? 
             ORDER BY dl.is_primary DESC, dl.verified_at ASC`,
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
        // 1. Primeiro, garantir que o discord_id existe em discord_users
        await pool.execute(
            'INSERT IGNORE INTO discord_users (discord_id, donor_tier, subscription_type) VALUES (?, 0, ?)',
            [discordId, 'BASIC']
        );
        
        const codeExpiresAt = new Date(Date.now() + 5 * 60 * 1000); // 5 minutos
        const [result] = await pool.execute(
            `INSERT INTO discord_links 
             (discord_id, player_id, is_primary, verified, verification_code, code_expires_at)
             VALUES (?, ?, ?, FALSE, ?, ?)`,
            [discordId, playerId, isPrimary, verifyCode, codeExpiresAt]
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

// =====================================================
// FUNÇÕES PARA SISTEMA DE STATUS GLOBAL (REFATORADAS)
// =====================================================

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
            FROM discord_users 
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
                pd.player_id,
                pd.name as player_name,
                dl.is_primary,
                dl.verified_at as linked_at,
                dl.verified,
                du.subscription_expires_at,
                CASE 
                    WHEN du.subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
                    WHEN du.subscription_expires_at < NOW() THEN 'EXPIRED'
                    ELSE 'ACTIVE'
                END as subscription_status,
                CASE 
                    WHEN du.subscription_expires_at > NOW() THEN 
                        DATEDIFF(du.subscription_expires_at, NOW())
                    ELSE 0 
                END as days_remaining
            FROM discord_links dl
            LEFT JOIN player_data pd ON dl.player_id = pd.player_id
            LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
            WHERE dl.discord_id = ? AND dl.verified = TRUE
            ORDER BY dl.is_primary DESC, dl.verified_at ASC
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
                COUNT(CASE WHEN du.subscription_expires_at > NOW() THEN 1 END) as active_subscriptions,
                COUNT(CASE WHEN du.subscription_expires_at <= NOW() THEN 1 END) as expired_subscriptions,
                COUNT(CASE WHEN du.subscription_expires_at IS NULL THEN 1 END) as never_subscribed
            FROM discord_links dl
            LEFT JOIN player_data pd ON dl.player_id = pd.player_id
            LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
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
        // 1. Primeiro, garantir que o discord_id existe em discord_users
        await pool.execute(
            'INSERT IGNORE INTO discord_users (discord_id, donor_tier, subscription_type) VALUES (?, 0, ?)',
            [discordId, 'BASIC']
        );
        
        // 2. Se for definida como primária, remover flag primary de outras contas
        if (isPrimary) {
            await pool.execute(
                'UPDATE discord_links SET is_primary = FALSE WHERE discord_id = ?',
                [discordId]
            );
        }

        const [result] = await pool.execute(`
            INSERT INTO discord_links (discord_id, player_id, is_primary, verified)
            VALUES (?, ?, ?, FALSE)
        `, [discordId, playerId, isPrimary]);
        
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
            DELETE dl FROM discord_links dl
            JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE dl.discord_id = ? AND pd.name = ? AND dl.verified = TRUE
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
 * Renova a assinatura compartilhada de um usuário Discord.
 * Fonte da verdade: discord_users.subscription_expires_at
 */
async function renewAccountSubscription(playerId, days = 30) {
    try {
        console.log(`[SUBSCRIPTION] Iniciando renovação para player_id: ${playerId}, dias: ${days}`);
        
        // Verificar se a conta existe e obter Discord ID
        const [playerCheck] = await pool.execute(
            'SELECT pd.player_id, pd.name, dl.discord_id FROM player_data pd LEFT JOIN discord_links dl ON pd.player_id = dl.player_id WHERE pd.player_id = ? AND dl.verified = TRUE',
            [playerId]
        );
        
        if (playerCheck.length === 0) {
            console.log(`[SUBSCRIPTION] ❌ Conta não encontrada para player_id: ${playerId}`);
            return { success: false, error: 'Conta não encontrada.' };
        }

        const player = playerCheck[0];
        const discordId = player.discord_id;
        
        if (!discordId) {
            console.log(`[SUBSCRIPTION] ❌ Conta não vinculada ao Discord: ${playerId}`);
            return { success: false, error: 'Conta não vinculada ao Discord.' };
        }
        
        // Verificar assinatura atual no discord_users
        const [currentSub] = await pool.execute(
            'SELECT subscription_expires_at FROM discord_users WHERE discord_id = ?',
            [discordId]
        );
        
        const currentExpiry = currentSub.length > 0 ? currentSub[0].subscription_expires_at : null;
        let newExpiry;

        console.log(`[SUBSCRIPTION] Player: ${player.name} (ID: ${player.player_id})`);
        console.log(`[SUBSCRIPTION] Assinatura atual: ${currentExpiry ? currentExpiry.toISOString() : 'NUNCA ASSINOU'}`);

        if (!currentExpiry || new Date(currentExpiry) < new Date()) {
            // Assinatura expirada ou inexistente - começar de agora
            newExpiry = new Date();
            newExpiry.setDate(newExpiry.getDate() + days);
            console.log(`[SUBSCRIPTION] 🔄 Assinatura expirada/inexistente - iniciando nova assinatura`);
        } else {
            // Assinatura ativa - estender a partir da data atual de expiração
            newExpiry = new Date(currentExpiry);
            newExpiry.setDate(newExpiry.getDate() + days);
            console.log(`[SUBSCRIPTION] ⏰ Assinatura ativa - estendendo a partir da data atual`);
        }

        console.log(`[SUBSCRIPTION] Nova data de expiração: ${newExpiry.toISOString()}`);

        // Atualizar a assinatura compartilhada
        const [result] = await pool.execute(
            'INSERT INTO discord_users (discord_id, subscription_expires_at, subscription_type) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE subscription_expires_at = VALUES(subscription_expires_at), updated_at = NOW()',
            [discordId, newExpiry, 'BASIC']
        );

        const success = result.affectedRows > 0;
        
        if (success) {
            console.log(`[SUBSCRIPTION] ✅ Assinatura renovada com sucesso para ${player.name}`);
            console.log(`[SUBSCRIPTION] 📅 Válida até: ${newExpiry.toLocaleDateString('pt-BR')} ${newExpiry.toLocaleTimeString('pt-BR')}`);
            
            // Verificar se a atualização foi persistida
            const [verification] = await pool.execute(
                'SELECT subscription_expires_at FROM discord_users WHERE discord_id = ?',
                [discordId]
            );
            
            if (verification.length > 0) {
                console.log(`[SUBSCRIPTION] 🔍 Verificação: assinatura persistida como ${verification[0].subscription_expires_at}`);
            }
        } else {
            console.log(`[SUBSCRIPTION] ❌ Falha ao atualizar assinatura - nenhuma linha afetada`);
        }

        return { 
            success: success,
            newExpiry: newExpiry,
            daysAdded: days,
            playerName: player.name
        };
    } catch (error) {
        console.error('[SUBSCRIPTION] ❌ Erro ao renovar assinatura:', error);
        console.error('[SUBSCRIPTION] Stack trace:', error.stack);
        return { success: false, error: 'Erro interno do banco de dados.' };
    }
}

/**
 * Verifica o status atual de uma assinatura no banco de dados
 * Útil para debugging e troubleshooting
 */
async function checkSubscriptionStatus(playerId) {
    try {
        console.log(`[SUBSCRIPTION-CHECK] 🔍 Verificando status da assinatura para player_id: ${playerId}`);
        
        // Primeiro obter o Discord ID do player
        const [playerInfo] = await pool.execute(
            'SELECT pd.player_id, pd.name, dl.discord_id FROM player_data pd LEFT JOIN discord_links dl ON pd.player_id = dl.player_id WHERE pd.player_id = ? AND dl.verified = TRUE',
            [playerId]
        );
        
        if (playerInfo.length === 0) {
            console.log(`[SUBSCRIPTION-CHECK] ❌ Player não encontrado ou não vinculado: ${playerId}`);
            return null;
        }
        
        const discordId = playerInfo[0].discord_id;
        
        // Verificar assinatura compartilhada
        const [rows] = await pool.execute(`
            SELECT 
                ? as player_id,
                ? as name,
                subscription_expires_at,
                updated_at,
                CASE 
                    WHEN subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
                    WHEN subscription_expires_at < NOW() THEN 'EXPIRED'
                    ELSE 'ACTIVE'
                END as subscription_status,
                CASE 
                    WHEN subscription_expires_at > NOW() THEN 
                        DATEDIFF(subscription_expires_at, NOW())
                    ELSE 0 
                END as days_remaining
            FROM discord_users 
            WHERE discord_id = ?
        `, [playerId, playerInfo[0].name, discordId]);
        
        if (rows.length === 0) {
            console.log(`[SUBSCRIPTION-CHECK] ❌ Player não encontrado: ${playerId}`);
            return null;
        }
        
        const player = rows[0];
        console.log(`[SUBSCRIPTION-CHECK] 📊 Status da assinatura:`);
        console.log(`[SUBSCRIPTION-CHECK]    Nome: ${player.name}`);
        console.log(`[SUBSCRIPTION-CHECK]    Status: ${player.subscription_status}`);
        console.log(`[SUBSCRIPTION-CHECK]    Expira em: ${player.subscription_expires_at ? player.subscription_expires_at.toISOString() : 'NUNCA ASSINOU'}`);
        console.log(`[SUBSCRIPTION-CHECK]    Dias restantes: ${player.days_remaining}`);
        console.log(`[SUBSCRIPTION-CHECK]    Última atualização: ${player.updated_at.toISOString()}`);
        
        return player;
    } catch (error) {
        console.error('[SUBSCRIPTION-CHECK] ❌ Erro ao verificar status:', error);
        return null;
    }
}

/**
 * Verifica se um nickname está disponível para vinculação.
 */
async function isNicknameAvailableForLinking(nickname) {
    try {
        // Verificar se o nickname já está vinculado a algum Discord ID
        const [existingLinks] = await pool.execute(`
            SELECT dl.discord_id 
            FROM discord_links dl
            JOIN player_data pd ON dl.player_id = pd.player_id
            WHERE pd.name = ? AND dl.verified = TRUE
        `, [nickname]);

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
                du.subscription_expires_at,
                dl.discord_id,
                dl.is_primary,
                dl.verified,
                dl.verified_at as linked_at,
                CASE 
                    WHEN du.subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
                    WHEN du.subscription_expires_at < NOW() THEN 'EXPIRED'
                    ELSE 'ACTIVE'
                END as subscription_status,
                CASE 
                    WHEN du.subscription_expires_at > NOW() THEN 
                        DATEDIFF(du.subscription_expires_at, NOW())
                    ELSE 0 
                END as days_remaining
            FROM player_data pd
            LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
            LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
            WHERE pd.name = ?
        `, [playerName]);
        
        return rows[0] || null;
    } catch (error) {
        console.error('Erro ao buscar informações da conta:', error);
        return null;
    }
}

/**
 * Busca informações de doador via API do Core.
 * O bot não define regras de negócio - apenas consome da fonte da verdade.
 */
async function getDonorInfoFromCore(discordId) {
    // Carregar configuração
    const config = require('../../bot-config.json');
    
    try {
        // Configuração da API do Core
        const coreApiUrl = process.env.CORE_API_URL || config.api.core.url;
        const apiUrl = `${coreApiUrl}/api/donor-info/${discordId}`;
        const timeout = config.api.core.timeout;
        const bearerToken = process.env.API_BEARER_TOKEN || config.api.core.bearer_token || 'primeleague_api_token_2024';
        
        console.log(`[API] Consultando Core: ${apiUrl}`);
        
        // Fazer requisição HTTP para a API do Core com autenticação
        const response = await fetch(apiUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${bearerToken}`,
                'User-Agent': 'PrimeLeague-Discord-Bot/1.0'
            },
            signal: AbortSignal.timeout(timeout)
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log(`[API] Resposta do Core:`, data);
        
        return {
            donorTier: data.donorTier || 0,
            donorName: data.donorName || 'Player',
            maxAccounts: data.maxAltAccounts || 1,
            currentAccounts: data.currentAccounts || 0,
            source: 'api'
        };
        
    } catch (error) {
        console.error('[API] Erro ao consultar Core:', error.message);
        
        // Não usar fallback do banco - retornar erro estruturado
        return {
            error: true,
            errorType: error.name === 'TimeoutError' ? 'timeout' : 'unavailable',
            message: error.name === 'TimeoutError' 
                ? config.messages.errors.apiTimeout 
                : config.messages.errors.apiUnavailable,
            donorTier: 0,
            donorName: 'Player',
            maxAccounts: 1,
            currentAccounts: 0,
            source: 'fallback'
        };
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

async function createPlayer(nickname) {
    try {
        const crypto = require('crypto');
        const source = "OfflinePlayer:" + nickname;
        const hash = crypto.createHash('md5').update(source, 'utf-8').digest();
        
        // RFC 4122: Ajustar bits de versão e variante
        hash[6] = (hash[6] & 0x0f) | 0x30;
        hash[8] = (hash[8] & 0x3f) | 0x80;
        
        const canonicalUuid = [
            hash.toString('hex', 0, 4),
            hash.toString('hex', 4, 6),
            hash.toString('hex', 6, 8),
            hash.toString('hex', 8, 10),
            hash.toString('hex', 10, 16)
        ].join('-');
        
        const [result] = await pool.execute(
            `INSERT INTO player_data (uuid, name, elo, money, total_playtime, total_logins, status, last_seen)
             VALUES (?, ?, ?, ?, ?, ?, ?, NOW())`,
            [canonicalUuid, nickname, 1000, 0.00, 0, 0, 'ACTIVE']
        );
        
        const playerId = result.insertId;
        
        // Notificação automática para limpar cache do servidor
        try {
            const https = require('https');
            const data = JSON.stringify({
                playerName: nickname,
                playerId: playerId,
                uuid: canonicalUuid,
                timestamp: new Date().toISOString()
            });
            
            const options = {
                hostname: 'localhost',
                port: 8080,
                path: '/api/player-created',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(data)
                }
            };
            
            const req = https.request(options, () => {});
            req.on('error', () => {});
            req.write(data);
            req.end();
            
        } catch (error) {
            // Erro não crítico - ignorar silenciosamente
        }
        
        // Retornar o player criado
        return {
            player_id: playerId,
            uuid: canonicalUuid,
            name: nickname,
            elo: 1000,
            money: 0.00,
            discord_id: null,
            verified: false
        };
    } catch (error) {
        console.error('[DB] Erro ao criar player:', error);
        return null;
    }
}


module.exports = {
    pool,
    // Funções principais (mantidas para compatibilidade)
    getPlayerByNickname,
    getDiscordLink,
    getDiscordLinkByPlayerUuid,
    createDiscordLink,
    getPlayerAccountInfo,
    getVerificationStatus,
    verifyDiscordLink,
    createServerNotification,
    generateVerifyCode,
    // Funções do sistema de clãs (refatoradas)
    getDiscordLinksById,
    createClanMemberLink,
    removeClanMember,
    getClanMemberCount,
    // Funções do sistema de status global (refatoradas)
    getServerMetrics,
    // ✨ FUNÇÕES DO SISTEMA DE PORTFÓLIO
    getPortfolioByDiscordId,
    getPortfolioStats,
    addAccountToPortfolio,
    removeAccountFromPortfolio,
    renewAccountSubscription,
    isNicknameAvailableForLinking,
    getAccountInfoDetailed,
    formatSubscriptionStatus,
    // 🆕 FUNÇÃO DO SISTEMA DE APOIADORES (AGUARDANDO API DO CORE)
    getDonorInfoFromCore,
    // 🔍 FUNÇÃO DE DEBUGGING
    checkSubscriptionStatus,
    createPlayer
};
