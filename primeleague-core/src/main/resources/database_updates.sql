-- =====================================================
-- ATUALIZAÇÕES DO BANCO DE DADOS - MÓDULO P2P
-- =====================================================

-- 1. Adicionar coluna de expiração da assinatura na tabela player_data
ALTER TABLE `player_data` 
ADD COLUMN `subscription_expires_at` TIMESTAMP NULL DEFAULT NULL 
AFTER `total_playtime`;

-- 2. Criar tabela para vínculos Discord-Minecraft
CREATE TABLE `discord_links` (
    `discord_id` VARCHAR(30) NOT NULL,
    `player_uuid` VARCHAR(36) NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`discord_id`),
    UNIQUE KEY `uk_player_uuid` (`player_uuid`),
    FOREIGN KEY (`player_uuid`) REFERENCES `player_data`(`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 3. Índices para performance
CREATE INDEX `idx_subscription_expires` ON `player_data` (`subscription_expires_at`);
CREATE INDEX `idx_discord_links_created` ON `discord_links` (`created_at`);

-- =====================================================
-- VERIFICAÇÃO DAS ALTERAÇÕES
-- =====================================================

-- Verificar se a coluna foi adicionada
DESCRIBE `player_data`;

-- Verificar se a tabela foi criada
SHOW CREATE TABLE `discord_links`;

-- Verificar índices
SHOW INDEX FROM `player_data`;
SHOW INDEX FROM `discord_links`;
