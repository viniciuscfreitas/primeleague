-- Scripts SQL para o Módulo Administrativo do Prime League
-- Execute estes scripts no banco de dados para criar as tabelas necessárias

-- Tabela de punições
-- Armazena o histórico completo de todas as punições aplicadas no servidor
CREATE TABLE IF NOT EXISTS `punishments` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `type` ENUM('WARN', 'KICK', 'MUTE', 'BAN') NOT NULL,
    `target_uuid` VARCHAR(36) NOT NULL,
    `author_uuid` VARCHAR(36) NULL, -- NULL para punições automáticas do sistema
    `reason` VARCHAR(255) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL DEFAULT NULL, -- NULL para punições permanentes
    `active` BOOLEAN NOT NULL DEFAULT TRUE,
    `pardoned_by_uuid` VARCHAR(36) NULL DEFAULT NULL,
    `pardoned_at` TIMESTAMP NULL DEFAULT NULL,
    `pardon_reason` VARCHAR(255) NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_target_active` (`target_uuid`, `active`),
    INDEX `idx_type_active` (`type`, `active`),
    INDEX `idx_expires_at` (`expires_at`),
    FOREIGN KEY (`target_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE CASCADE,
    FOREIGN KEY (`author_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE SET NULL,
    FOREIGN KEY (`pardoned_by_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Tabela de tickets
-- Gerencia todas as denúncias feitas por jogadores
CREATE TABLE IF NOT EXISTS `tickets` (
    `id` INT(11) NOT NULL AUTO_INCREMENT,
    `status` ENUM('OPEN', 'IN_PROGRESS', 'CLOSED_GUILTY', 'CLOSED_INNOCENT') NOT NULL DEFAULT 'OPEN',
    `reporter_uuid` VARCHAR(36) NOT NULL,
    `target_uuid` VARCHAR(36) NOT NULL,
    `reason` TEXT NOT NULL,
    `evidence_link` VARCHAR(255) NULL DEFAULT NULL,
    `claimed_by_uuid` VARCHAR(36) NULL DEFAULT NULL, -- UUID do staff que está cuidando do ticket
    `resolution_notes` TEXT NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reporter` (`reporter_uuid`),
    INDEX `idx_target` (`target_uuid`),
    INDEX `idx_claimed_by` (`claimed_by_uuid`),
    INDEX `idx_created_at` (`created_at`),
    FOREIGN KEY (`reporter_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE CASCADE,
    FOREIGN KEY (`target_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE CASCADE,
    FOREIGN KEY (`claimed_by_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Tabela de staff vanish
-- Controla o estado de vanish dos membros da equipe
CREATE TABLE IF NOT EXISTS `staff_vanish` (
    `uuid` VARCHAR(36) NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `enabled_at` TIMESTAMP NULL DEFAULT NULL,
    `enabled_by_uuid` VARCHAR(36) NULL DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    FOREIGN KEY (`uuid`) REFERENCES `player_data`(`uuid`) ON DELETE CASCADE,
    FOREIGN KEY (`enabled_by_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Comentários sobre as tabelas
-- 
-- punishments:
-- - Registra todas as punições (ativas e inativas)
-- - Nunca deleta registros, apenas marca como inativo
-- - Suporta perdão com registro de quem perdoou e quando
-- - Índices otimizados para verificações de login e chat
--
-- tickets:
-- - Sistema de denúncias com workflow organizado
-- - Status controla o fluxo de trabalho
-- - Claim evita conflitos entre staffs
-- - Resolução separa motivo original das notas da equipe
--
-- staff_vanish:
-- - Controle persistente do estado de vanish
-- - Permite que vanish sobreviva a relogins
-- - Registra quem ativou o vanish
