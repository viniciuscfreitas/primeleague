-- =========================================================
-- SCHEMA OTIMIZADO PRIME LEAGUE - VERSÃO FINAL
-- Data: 27/08/2025
-- Descrição: Schema otimizado seguindo SSOT (Single Source of Truth)
--            Eliminação de redundâncias, centralização de dados
--            Máxima performance e consistência
-- =========================================================

-- =====================================================
-- CONFIGURAÇÃO INICIAL
-- =====================================================

DROP DATABASE IF EXISTS `primeleague`;
CREATE DATABASE `primeleague` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */;
USE `primeleague`;

-- =====================================================
-- TABELA CENTRAL DE JOGADORES (SSOT PARA DADOS DE JOGADOR)
-- =====================================================

CREATE TABLE `player_data` (
  `player_id` INT NOT NULL AUTO_INCREMENT,
  `uuid` CHAR(36) NOT NULL,
  `name` VARCHAR(16) NOT NULL,
  `elo` INT NOT NULL DEFAULT 1000,
  `money` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `total_playtime` BIGINT NOT NULL DEFAULT 0,
  `last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `total_logins` INT NOT NULL DEFAULT 0,
  `status` ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'INACTIVE',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`player_id`),
  UNIQUE KEY `uk_player_data_uuid` (`uuid`),
  UNIQUE KEY `uk_player_data_name` (`name`),
  KEY `idx_player_data_status` (`status`),
  KEY `idx_player_data_elo` (`elo`),
  KEY `idx_player_data_last_seen` (`last_seen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA CENTRAL DE USUÁRIOS DISCORD (SSOT PARA DADOS DISCORD)
-- =====================================================

CREATE TABLE `discord_users` (
  `discord_id` VARCHAR(20) NOT NULL PRIMARY KEY,
  `donor_tier` INT NOT NULL DEFAULT 0,
  `donor_tier_expires_at` TIMESTAMP NULL,
  `subscription_expires_at` TIMESTAMP NULL,
  `subscription_type` ENUM('BASIC', 'PREMIUM', 'VIP') DEFAULT 'BASIC',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_discord_id` (`discord_id`),
  INDEX `idx_donor_tier` (`donor_tier`),
  INDEX `idx_subscription_expires` (`subscription_expires_at`),
  INDEX `idx_donor_expiry` (`donor_tier_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE VÍNCULOS (RELAÇÃO N:N DISCORD ↔ MINECRAFT)
-- =====================================================

CREATE TABLE `discord_links` (
  `link_id` INT NOT NULL AUTO_INCREMENT,
  `discord_id` VARCHAR(20) NOT NULL,
  `player_id` INT NOT NULL,
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `verified` TINYINT(1) NOT NULL DEFAULT 0,
  `verification_code` VARCHAR(8) NULL DEFAULT NULL,
  `code_expires_at` TIMESTAMP NULL DEFAULT NULL,
  `verified_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `uk_player_id` (`player_id`),
  KEY `idx_discord_id` (`discord_id`),
  KEY `idx_verified` (`verified`),
  CONSTRAINT `fk_discord_links_player` 
    FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_discord_links_user` 
    FOREIGN KEY (`discord_id`) REFERENCES `discord_users` (`discord_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE PUNIÇÕES (ADMIN)
-- =====================================================

CREATE TABLE `punishments` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `type` ENUM('WARN', 'KICK', 'MUTE', 'BAN') NOT NULL,
  `target_player_id` INT NOT NULL,
  `target_name` VARCHAR(16) NOT NULL,
  `author_player_id` INT DEFAULT NULL,
  `author_name` VARCHAR(16) DEFAULT NULL,
  `reason` TEXT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NULL DEFAULT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `pardoned_by_player_id` INT DEFAULT NULL,
  `pardoned_by_name` VARCHAR(16) DEFAULT NULL,
  `pardoned_at` TIMESTAMP NULL DEFAULT NULL,
  `pardon_reason` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_punishments_target_type_active` (`target_player_id`, `type`, `is_active`),
  KEY `idx_punishments_expires_at` (`expires_at`),
  KEY `idx_punishments_target_name` (`target_name`),
  KEY `idx_punishments_author_name` (`author_name`),
  CONSTRAINT `fk_punishments_target_player` 
    FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_punishments_author_player` 
    FOREIGN KEY (`author_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_punishments_pardoned_by_player` 
    FOREIGN KEY (`pardoned_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE TICKETS (ADMIN)
-- =====================================================

CREATE TABLE `tickets` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `reporter_player_id` INT NOT NULL,
  `reporter_name` VARCHAR(16) NOT NULL,
  `target_player_id` INT NOT NULL,
  `target_name` VARCHAR(16) NOT NULL,
  `reason` TEXT NOT NULL,
  `status` ENUM('OPEN', 'CLAIMED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
  `priority` ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `claimed_by_player_id` INT DEFAULT NULL,
  `claimed_by_name` VARCHAR(16) DEFAULT NULL,
  `claimed_at` TIMESTAMP NULL DEFAULT NULL,
  `closed_at` TIMESTAMP NULL DEFAULT NULL,
  `resolution` TEXT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tickets_status` (`status`),
  KEY `idx_tickets_priority` (`priority`),
  KEY `idx_tickets_created_at` (`created_at`),
  KEY `idx_tickets_reporter` (`reporter_player_id`),
  KEY `idx_tickets_target` (`target_player_id`),
  CONSTRAINT `fk_tickets_reporter_player` 
    FOREIGN KEY (`reporter_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tickets_target_player` 
    FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_tickets_claimed_by_player` 
    FOREIGN KEY (`claimed_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE STAFF VANISH (ADMIN)
-- =====================================================

CREATE TABLE `staff_vanish` (
  `player_id` INT NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `enabled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `enabled_by_player_id` INT DEFAULT NULL,
  PRIMARY KEY (`player_id`),
  CONSTRAINT `fk_staff_vanish_player` 
    FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_staff_vanish_enabled_by_player` 
    FOREIGN KEY (`enabled_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE WHITELIST (ACESSO ADMINISTRATIVO)
-- =====================================================

CREATE TABLE `whitelist_players` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `player_id` INT NOT NULL,
  `player_name` VARCHAR(16) NOT NULL,
  `added_by_player_id` INT NOT NULL,
  `added_by_name` VARCHAR(16) NOT NULL,
  `added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reason` VARCHAR(255) NOT NULL,
  `is_active` TINYINT(1) NOT NULL DEFAULT 1,
  `removed_by_player_id` INT DEFAULT NULL,
  `removed_by_name` VARCHAR(16) DEFAULT NULL,
  `removed_at` TIMESTAMP NULL DEFAULT NULL,
  `removal_reason` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_whitelist_player_id_active` (`player_id`, `is_active`),
  KEY `idx_whitelist_is_active` (`is_active`),
  KEY `idx_whitelist_added_at` (`added_at`),
  CONSTRAINT `fk_whitelist_player` 
    FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_whitelist_added_by_player` 
    FOREIGN KEY (`added_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_whitelist_removed_by_player` 
    FOREIGN KEY (`removed_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELAS DE CLÃS E COMUNICAÇÃO
-- =====================================================

-- Tabela de Clãs
CREATE TABLE `clans` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `tag` VARCHAR(5) NOT NULL,
  `name` VARCHAR(32) NOT NULL,
  `founder_player_id` INT NOT NULL,
  `creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `friendly_fire_enabled` TINYINT(1) NOT NULL DEFAULT 0,
  `penalty_points` INT NOT NULL DEFAULT 0,
  `ranking_points` INT NOT NULL DEFAULT 1000,
  `active_sanction_tier` INT NOT NULL DEFAULT 0,
  `sanction_expires_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clans_tag` (`tag`),
  UNIQUE KEY `uk_clans_name` (`name`),
  KEY `idx_clans_founder_player_id` (`founder_player_id`),
  KEY `idx_clans_penalty_points` (`penalty_points`),
  KEY `idx_clans_ranking_points` (`ranking_points`),
  KEY `idx_clans_creation_date` (`creation_date`),
  CONSTRAINT `fk_clans_founder_player` 
    FOREIGN KEY (`founder_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Relação Jogador-Clã (Fonte Única de Afiliação)
CREATE TABLE `clan_players` (
  `player_id` INT NOT NULL,
  `clan_id` INT NOT NULL,
  `role` ENUM('LEADER', 'CO_LEADER', 'OFFICER', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
  `join_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `kills` INT NOT NULL DEFAULT 0,
  `deaths` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`player_id`),
  KEY `idx_clan_players_clan_id` (`clan_id`),
  KEY `idx_clan_players_role` (`role`),
  KEY `idx_clan_players_join_date` (`join_date`),
  CONSTRAINT `fk_clan_players_player` 
    FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_clan_players_clan` 
    FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de Alianças entre Clãs
CREATE TABLE `clan_alliances` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `clan1_id` INT NOT NULL,
  `clan2_id` INT NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by_player_id` INT NOT NULL,
  `status` ENUM('ACTIVE', 'PENDING', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_clan_alliances_unique` (`clan1_id`, `clan2_id`),
  KEY `idx_clan_alliances_status` (`status`),
  CONSTRAINT `fk_clan_alliances_clan1` 
    FOREIGN KEY (`clan1_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_clan_alliances_clan2` 
    FOREIGN KEY (`clan2_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_clan_alliances_created_by` 
    FOREIGN KEY (`created_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELAS DE COMUNICAÇÃO
-- =====================================================

-- Tabela de Logs de Chat (Sistema de Comunicação)
CREATE TABLE `chat_logs` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `channel_type` ENUM('GLOBAL', 'LOCAL', 'CLAN', 'ALLY', 'PRIVATE', 'STAFF') NOT NULL,
  `sender_player_id` INT NOT NULL,
  `sender_name` VARCHAR(16) NOT NULL,
  `receiver_player_id` INT NULL DEFAULT NULL,
  `receiver_name` VARCHAR(16) NULL DEFAULT NULL,
  `clan_id` INT NULL DEFAULT NULL,
  `message_content` TEXT NOT NULL,
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
  `deleted_by_player_id` INT NULL DEFAULT NULL,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_logs_timestamp` (`timestamp`),
  KEY `idx_chat_logs_channel_type` (`channel_type`),
  KEY `idx_chat_logs_sender` (`sender_player_id`),
  KEY `idx_chat_logs_receiver` (`receiver_player_id`),
  KEY `idx_chat_logs_clan` (`clan_id`),
  KEY `idx_chat_logs_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_chat_logs_sender` 
    FOREIGN KEY (`sender_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chat_logs_receiver` 
    FOREIGN KEY (`receiver_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chat_logs_clan` 
    FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_chat_logs_deleted_by` 
    FOREIGN KEY (`deleted_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SISTEMA DE NOTIFICAÇÕES
-- =====================================================

-- Tabela de Notificações para Serviços Externos (Core)
CREATE TABLE `server_notifications` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `action_type` VARCHAR(50) NOT NULL,
  `payload` TEXT NOT NULL,
  `processed` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_server_notifications_processed` (`processed`),
  KEY `idx_server_notifications_action_type` (`action_type`),
  KEY `idx_server_notifications_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE LOGS DE ECONOMIA (AUDITORIA)
-- =====================================================

CREATE TABLE `economy_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `player_id` INT NOT NULL,
  `change_type` ENUM('CREDIT', 'DEBIT', 'ADMIN_GIVE', 'ADMIN_TAKE', 'ADMIN_SET', 'PLAYER_SHOP_SALE', 'PLAYER_SHOP_PURCHASE', 'ADMIN_SHOP_PURCHASE', 'PLAYER_TRANSFER', 'CLAN_BANK_DEPOSIT', 'CLAN_BANK_WITHDRAW', 'CLAN_TAX_COLLECTION', 'SYSTEM_REWARD', 'SYSTEM_PENALTY', 'BOUNTY_REWARD', 'BOUNTY_PAYMENT', 'EVENT_REWARD', 'TOURNAMENT_PRIZE', 'OTHER') NOT NULL,
  `amount` DECIMAL(15,2) NOT NULL,
  `balance_before` DECIMAL(15,2) NOT NULL,
  `new_balance` DECIMAL(15,2) NOT NULL,
  `reason` VARCHAR(100) NOT NULL,
  `context_info` TEXT NULL,
  `related_player_id` INT NULL,
  `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_economy_logs_player_id` (`player_id`),
  KEY `idx_economy_logs_reason` (`reason`),
  CONSTRAINT `fk_economy_logs_player` FOREIGN KEY (`player_id`) REFERENCES `player_data`(`player_id`),
  CONSTRAINT `fk_economy_logs_related_player` FOREIGN KEY (`related_player_id`) REFERENCES `player_data`(`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- DADOS INICIAIS ESSENCIAIS
-- =====================================================

-- Inserir o CONSOLE (ESSENCIAL para funcionamento da whitelist)
INSERT INTO `player_data` (`player_id`, `uuid`, `name`, `elo`, `money`, `status`) 
VALUES (0, '00000000-0000-0000-0000-000000000000', 'CONSOLE', 0, 0.00, 'ACTIVE');

-- Jogador inicial do sistema (opcional)
INSERT IGNORE INTO `player_data` (`uuid`, `name`, `elo`, `money`, `status`) 
VALUES ('7c42729a-60a2-abfb-7aec-b4dd1c8ffd93', 'SYSTEM', 0, 0.00, 'ACTIVE');

-- =====================================================
-- PROCEDIMENTOS DE MANUTENÇÃO
-- =====================================================

DELIMITER $$

-- Procedure para verificação de vínculos Discord
CREATE PROCEDURE `VerifyDiscordLink`(
    IN p_player_name VARCHAR(16),
    IN p_verify_code VARCHAR(8)
)
BEGIN
    DECLARE v_success BOOLEAN DEFAULT FALSE;
    DECLARE v_discord_id VARCHAR(20);
    DECLARE v_player_uuid CHAR(36);
    
    -- Verificar código válido e não expirado
    SELECT dl.discord_id, pd.uuid
    INTO v_discord_id, v_player_uuid
    FROM discord_links dl
    JOIN player_data pd ON dl.player_id = pd.player_id
    WHERE pd.name = p_player_name 
      AND dl.verification_code = p_verify_code
      AND dl.code_expires_at > NOW()
      AND dl.verified = FALSE;
    
    IF v_discord_id IS NOT NULL THEN
        -- Marcar como verificado
        UPDATE discord_links 
        SET verified = TRUE, 
            verified_at = NOW(),
            verification_code = NULL,
            code_expires_at = NULL
        WHERE discord_id = v_discord_id;
        
        SET v_success = TRUE;
    END IF;
    
    -- Retornar resultado
    SELECT v_success as success, 
           v_discord_id as discord_id, 
           v_player_uuid as player_uuid;
END$$

-- Procedure para limpeza de dados expirados
CREATE PROCEDURE `CleanupExpiredData`()
BEGIN
    -- Limpeza de códigos de verificação expirados
    UPDATE `discord_links` 
    SET `verification_code` = NULL, `code_expires_at` = NULL
    WHERE `code_expires_at` IS NOT NULL AND `code_expires_at` < NOW();
    
    -- Limpeza de banimentos temporários expirados
    UPDATE `punishments` 
    SET `is_active` = 0 
    WHERE `is_active` = 1 AND `expires_at` IS NOT NULL AND `expires_at` < NOW();
    
    -- Limpeza de notificações processadas antigas (mais de 30 dias)
    DELETE FROM `server_notifications` 
    WHERE `processed` = 1 AND `created_at` < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    SELECT 'Limpeza de dados expirados concluida' as resultado;
END$$

-- Procedure para estatísticas do servidor
CREATE PROCEDURE `GetServerStats`()
BEGIN
    SELECT 
        'player_data' as tabela,
        COUNT(*) as total_registros,
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as jogadores_ativos,
        COUNT(CASE WHEN status = 'BANNED' THEN 1 END) as jogadores_banidos
    FROM player_data
    UNION ALL
    SELECT 
        'discord_users' as tabela,
        COUNT(*) as total_registros,
        COUNT(CASE WHEN donor_tier > 0 THEN 1 END) as doadores_ativos,
        COUNT(CASE WHEN subscription_expires_at > NOW() THEN 1 END) as assinaturas_ativas
    FROM discord_users
    UNION ALL
    SELECT 
        'clans' as tabela,
        COUNT(*) as total_registros,
        COUNT(CASE WHEN penalty_points = 0 THEN 1 END) as clans_sem_penalidade,
        COUNT(CASE WHEN penalty_points > 0 THEN 1 END) as clans_com_penalidade
    FROM clans
    UNION ALL
    SELECT 
        'whitelist_players' as tabela,
        COUNT(*) as total_registros,
        COUNT(CASE WHEN is_active = 1 THEN 1 END) as whitelist_ativos,
        COUNT(CASE WHEN is_active = 0 THEN 1 END) as whitelist_inativos
    FROM whitelist_players;
END$$

DELIMITER ;

-- =====================================================
-- VERIFICAÇÃO FINAL
-- =====================================================

-- Verificar se o CONSOLE foi inserido corretamente
SELECT 
    'VERIFICACAO FINAL:' as info,
    'CONSOLE player_id = 0 existe na player_data' as resultado
FROM player_data 
WHERE player_id = 0;

-- Verificar estrutura das tabelas principais
SELECT 
    'ESTRUTURA CRIADA:' as info,
    COUNT(*) as total_tabelas
FROM information_schema.tables 
WHERE table_schema = 'primeleague';

-- Verificar foreign keys
SELECT 
    'FOREIGN KEYS:' as info,
    COUNT(*) as total_foreign_keys
FROM information_schema.key_column_usage 
WHERE table_schema = 'primeleague' 
  AND referenced_table_name IS NOT NULL;

SELECT 
    'SCHEMA OTIMIZADO PRIME LEAGUE CRIADO COM SUCESSO!' as status,
    NOW() as data_criacao;
