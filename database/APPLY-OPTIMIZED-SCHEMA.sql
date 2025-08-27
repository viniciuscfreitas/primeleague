-- =====================================================
-- APLICAR SCHEMA OTIMIZADO PRIME LEAGUE
-- =====================================================

-- 1. Criar tabela discord_users se não existir
CREATE TABLE IF NOT EXISTS `discord_users` (
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

-- 2. Migrar dados existentes de discord_links para discord_users
-- (sem referenciar donor_tier de player_data pois não existe)
INSERT IGNORE INTO `discord_users` (`discord_id`, `donor_tier`, `subscription_type`)
SELECT DISTINCT 
    dl.discord_id,
    0 as donor_tier, -- Valor padrão para todos
    'BASIC' as subscription_type
FROM `discord_links` dl
WHERE dl.discord_id IS NOT NULL;

-- 3. Migrar assinaturas existentes (se houver)
-- Verificar se a coluna subscription_expires_at existe em player_data
SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
    AND table_name = 'player_data'
    AND column_name = 'subscription_expires_at'
);

SET @migration_sql = IF(@column_exists > 0,
    'UPDATE `discord_users` du
     JOIN (
         SELECT 
             dl.discord_id,
             MAX(pd.subscription_expires_at) as subscription_expires_at
         FROM `discord_links` dl
         JOIN `player_data` pd ON dl.player_id = pd.player_id
         WHERE dl.verified = TRUE 
             AND pd.subscription_expires_at IS NOT NULL
             AND pd.subscription_expires_at > NOW()
         GROUP BY dl.discord_id
     ) migration ON du.discord_id = migration.discord_id
     SET du.subscription_expires_at = migration.subscription_expires_at
     WHERE du.subscription_expires_at IS NULL',
    'SELECT "Column subscription_expires_at does not exist in player_data" as status'
);

PREPARE migration_stmt FROM @migration_sql;
EXECUTE migration_stmt;
DEALLOCATE PREPARE migration_stmt;

-- 4. Verificar se a foreign key constraint existe
-- Se não existir, adicionar
SET @constraint_exists = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE constraint_schema = DATABASE()
    AND table_name = 'discord_links'
    AND constraint_name = 'fk_discord_links_user'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE `discord_links` ADD CONSTRAINT `fk_discord_links_user` FOREIGN KEY (`discord_id`) REFERENCES `discord_users` (`discord_id`) ON DELETE CASCADE ON UPDATE CASCADE',
    'SELECT "Foreign key constraint already exists" as status'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Verificar dados migrados
SELECT 
    'DISCORD_USERS' as tabela,
    COUNT(*) as total_registros
FROM `discord_users`
UNION ALL
SELECT 
    'DISCORD_LINKS' as tabela,
    COUNT(*) as total_registros
FROM `discord_links`
UNION ALL
SELECT 
    'PLAYER_DATA' as tabela,
    COUNT(*) as total_registros
FROM `player_data`;

-- 6. Verificar assinaturas ativas
SELECT 
    'ASSINATURAS_ATIVAS' as info,
    COUNT(*) as total
FROM `discord_users` 
WHERE subscription_expires_at > NOW();

-- 7. Verificar estrutura das tabelas
DESCRIBE `discord_users`;
DESCRIBE `discord_links`;
DESCRIBE `player_data`;

-- =====================================================
-- FIM DO SCRIPT
-- =====================================================
