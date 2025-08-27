-- =========================================================
-- SCHEMA FINAL AUTOMATIZADO PRIME LEAGUE - VERSÃO 4.0
-- Data: 27/08/2025
-- Descrição: Schema final com todas as correções e melhorias
--            Compatível com Discord First Registration Flow
--            UUID compatibility fix aplicado
--            SSOT (Single Source of Truth) implementado
--            Shared subscriptions funcionando
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
  `player_uuid` CHAR(36) NOT NULL,
  `is_primary` TINYINT(1) NOT NULL DEFAULT 0,
  `verified` TINYINT(1) NOT NULL DEFAULT 0,
  `verification_code` VARCHAR(8) NULL DEFAULT NULL,
  `code_expires_at` TIMESTAMP NULL DEFAULT NULL,
  `verified_at` TIMESTAMP NULL DEFAULT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`link_id`),
  UNIQUE KEY `uk_player_uuid` (`player_uuid`),
  KEY `idx_discord_id` (`discord_id`),
  KEY `idx_verified` (`verified`),
  KEY `idx_verification_code` (`verification_code`),
  CONSTRAINT `fk_discord_links_player` 
    FOREIGN KEY (`player_uuid`) REFERENCES `player_data` (`uuid`) ON DELETE CASCADE ON UPDATE CASCADE,
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
  KEY `idx_target_player` (`target_player_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_expires_at` (`expires_at`),
  CONSTRAINT `fk_punishments_target` 
    FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_punishments_author` 
    FOREIGN KEY (`author_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_punishments_pardoned_by` 
    FOREIGN KEY (`pardoned_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- TABELA DE ESTATÍSTICAS DO SERVIDOR
-- =====================================================

CREATE TABLE `server_stats` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `total_players` INT NOT NULL DEFAULT 0,
  `online_players` INT NOT NULL DEFAULT 0,
  `total_matches` INT NOT NULL DEFAULT 0,
  `total_revenue` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
  `recorded_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recorded_at` (`recorded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- STORED PROCEDURES
-- =====================================================

DELIMITER //

-- Procedure para verificar vínculo Discord
CREATE PROCEDURE `VerifyDiscordLink`(
    IN p_player_name VARCHAR(16),
    IN p_verification_code VARCHAR(8)
)
BEGIN
    DECLARE v_player_uuid CHAR(36);
    DECLARE v_discord_id VARCHAR(20);
    DECLARE v_success BOOLEAN DEFAULT FALSE;
    DECLARE v_message VARCHAR(255);
    
    -- Obter UUID do player
    SELECT uuid INTO v_player_uuid 
    FROM player_data 
    WHERE name = p_player_name;
    
    IF v_player_uuid IS NOT NULL THEN
        -- Verificar código de verificação
        SELECT discord_id INTO v_discord_id
        FROM discord_links 
        WHERE player_uuid = v_player_uuid 
        AND verification_code = p_verification_code
        AND code_expires_at > NOW()
        AND verified = 0;
        
        IF v_discord_id IS NOT NULL THEN
            -- Marcar como verificado
            UPDATE discord_links 
            SET verified = 1, 
                verified_at = NOW(),
                verification_code = NULL,
                code_expires_at = NULL
            WHERE player_uuid = v_player_uuid 
            AND discord_id = v_discord_id;
            
            SET v_success = TRUE;
            SET v_message = 'Verificação realizada com sucesso!';
        ELSE
            SET v_message = 'Código inválido ou expirado!';
        END IF;
    ELSE
        SET v_message = 'Jogador não encontrado!';
    END IF;
    
    -- Retornar resultado
    SELECT v_success as success, v_message as message, v_discord_id as discord_id, v_player_uuid as player_uuid;
END //

-- Procedure para limpar dados expirados
CREATE PROCEDURE `CleanupExpiredData`()
BEGIN
    -- Limpar códigos de verificação expirados
    UPDATE discord_links 
    SET verification_code = NULL, 
        code_expires_at = NULL 
    WHERE code_expires_at < NOW() 
    AND verified = 0;
    
    -- Limpar punições expiradas
    UPDATE punishments 
    SET is_active = 0 
    WHERE expires_at < NOW() 
    AND is_active = 1;
    
    SELECT ROW_COUNT() as cleaned_records;
END //

-- Procedure para obter estatísticas do servidor
CREATE PROCEDURE `GetServerStats`()
BEGIN
    SELECT 
        (SELECT COUNT(*) FROM player_data WHERE status = 'ACTIVE') as total_active_players,
        (SELECT COUNT(*) FROM discord_users WHERE subscription_expires_at > NOW()) as active_subscriptions,
        (SELECT COUNT(*) FROM discord_links WHERE verified = 1) as verified_links,
        (SELECT SUM(money) FROM player_data) as total_economy,
        (SELECT COUNT(*) FROM punishments WHERE is_active = 1) as active_punishments;
END //

DELIMITER ;

-- =====================================================
-- DADOS INICIAIS
-- =====================================================

-- Inserir estatísticas iniciais
INSERT INTO `server_stats` (`total_players`, `online_players`, `total_matches`, `total_revenue`) 
VALUES (0, 0, 0, 0.00);

-- =====================================================
-- ÍNDICES ADICIONAIS PARA PERFORMANCE
-- =====================================================

-- Índices para consultas frequentes
CREATE INDEX `idx_player_data_name_lower` ON `player_data` (LOWER(`name`));
CREATE INDEX `idx_discord_links_verification` ON `discord_links` (`verification_code`, `code_expires_at`, `verified`);
CREATE INDEX `idx_discord_users_subscription_status` ON `discord_users` (`subscription_expires_at`, `donor_tier`);

-- =====================================================
-- TRIGGERS PARA MANUTENÇÃO AUTOMÁTICA
-- =====================================================

DELIMITER //

-- Trigger para atualizar updated_at automaticamente
CREATE TRIGGER `tr_player_data_update` 
BEFORE UPDATE ON `player_data`
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END //

CREATE TRIGGER `tr_discord_users_update` 
BEFORE UPDATE ON `discord_users`
FOR EACH ROW
BEGIN
    SET NEW.updated_at = NOW();
END //

DELIMITER ;

-- =====================================================
-- VIEWS PARA CONSULTAS FREQUENTES
-- =====================================================

-- View para jogadores com assinatura ativa
CREATE VIEW `v_active_subscribers` AS
SELECT 
    pd.player_id,
    pd.uuid,
    pd.name,
    du.discord_id,
    du.subscription_type,
    du.subscription_expires_at,
    du.donor_tier,
    du.donor_tier_expires_at,
    dl.verified,
    CASE 
        WHEN du.subscription_expires_at > NOW() THEN 'ACTIVE'
        WHEN du.subscription_expires_at IS NULL THEN 'NEVER_SUBSCRIBED'
        ELSE 'EXPIRED'
    END as subscription_status
FROM player_data pd
LEFT JOIN discord_links dl ON pd.uuid = dl.player_uuid
LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
WHERE dl.verified = 1;

-- View para estatísticas de assinaturas
CREATE VIEW `v_subscription_stats` AS
SELECT 
    subscription_type,
    COUNT(*) as total_subscribers,
    COUNT(CASE WHEN subscription_expires_at > NOW() THEN 1 END) as active_subscribers,
    COUNT(CASE WHEN subscription_expires_at <= NOW() OR subscription_expires_at IS NULL THEN 1 END) as expired_subscribers
FROM discord_users
GROUP BY subscription_type;

-- =====================================================
-- FIM DO SCHEMA
-- =====================================================

-- Log de criação
SELECT 'Schema Final Automatizado Prime League v4.0 criado com sucesso!' as status;
SELECT NOW() as created_at;
