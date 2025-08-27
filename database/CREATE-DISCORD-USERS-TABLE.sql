-- =====================================================
-- CRIAÇÃO DA TABELA DISCORD_USERS
-- Armazena donor tier por Discord ID (não por conta)
-- =====================================================

-- Criar tabela discord_users
CREATE TABLE IF NOT EXISTS `discord_users` (
    `discord_id` VARCHAR(20) NOT NULL PRIMARY KEY,
    `donor_tier` INT NOT NULL DEFAULT 0,
    `donor_tier_expires_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_discord_id` (`discord_id`),
    INDEX `idx_donor_tier` (`donor_tier`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserir dados existentes (migração)
INSERT IGNORE INTO `discord_users` (`discord_id`, `donor_tier`, `donor_tier_expires_at`)
SELECT 
    dl.discord_id,
    pd.donor_tier,
    pd.donor_tier_expires_at
FROM `discord_links` dl
JOIN `player_data` pd ON dl.player_id = pd.player_id
WHERE dl.verified = TRUE
GROUP BY dl.discord_id
HAVING MAX(pd.donor_tier); -- Pega o maior donor tier do usuário

-- Verificar dados migrados
SELECT 
    du.discord_id,
    du.donor_tier,
    du.donor_tier_expires_at,
    COUNT(dl.player_id) as contas_vinculadas
FROM `discord_users` du
LEFT JOIN `discord_links` dl ON du.discord_id = dl.discord_id AND dl.verified = TRUE
GROUP BY du.discord_id;

-- =====================================================
-- FIM DO SCRIPT
-- =====================================================
