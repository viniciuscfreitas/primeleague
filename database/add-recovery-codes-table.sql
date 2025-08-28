-- =====================================================
-- ADICIONAR TABELA DE CÓDIGOS DE RECUPERAÇÃO (P2P)
-- =====================================================

USE `primeleague`;

-- Criar tabela de códigos de recuperação
CREATE TABLE `recovery_codes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `player_id` INT NOT NULL,
  `code_hash` VARCHAR(60) NOT NULL COMMENT 'Hash BCrypt do código de recuperação.',
  `code_type` ENUM('BACKUP', 'TEMPORARY') NOT NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` TIMESTAMP NULL DEFAULT NULL,
  `used_at` TIMESTAMP NULL DEFAULT NULL,
  `attempts` INT NOT NULL DEFAULT 0 COMMENT 'Número de tentativas de uso.',
  `ip_address` VARCHAR(45) NULL COMMENT 'IP de origem da ação (geração/uso).',
  `discord_id` VARCHAR(20) NULL COMMENT 'Discord ID associado à ação.',
  `status` ENUM('ACTIVE', 'USED', 'EXPIRED', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (`id`),
  KEY `idx_recovery_codes_player_id_status` (`player_id`, `status`),
  KEY `idx_recovery_codes_status` (`status`),
  KEY `idx_recovery_codes_expires_at` (`expires_at`),
  KEY `idx_recovery_codes_created_at` (`created_at`),
  CONSTRAINT `fk_recovery_codes_player`
    FOREIGN KEY (`player_id`)
    REFERENCES `player_data` (`player_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserir comentário na tabela
ALTER TABLE `recovery_codes` COMMENT = 'Tabela para armazenar códigos de recuperação de conta P2P';

-- Verificar se a tabela foi criada corretamente
SELECT 
    TABLE_NAME,
    TABLE_COMMENT,
    TABLE_ROWS,
    CREATE_TIME
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'primeleague' 
AND TABLE_NAME = 'recovery_codes';
