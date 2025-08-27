-- =====================================================
-- MIGRAÇÃO PARA SCHEMA OTIMIZADO
-- Converte schema atual para versão otimizada (SSOT)
-- =====================================================

-- =====================================================
-- PASSO 1: CRIAR TABELA DISCORD_USERS OTIMIZADA
-- =====================================================

-- Criar tabela discord_users otimizada (se não existir)
CREATE TABLE IF NOT EXISTS `discord_users_optimized` (
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
-- PASSO 2: MIGRAR DADOS DE DISCORD_USERS (se existir)
-- =====================================================

-- Migrar dados da tabela discord_users existente
INSERT IGNORE INTO `discord_users_optimized` (`discord_id`, `donor_tier`, `donor_tier_expires_at`)
SELECT 
    discord_id,
    COALESCE(donor_tier, 0) as donor_tier,
    donor_tier_expires_at
FROM `discord_users`
WHERE discord_id IS NOT NULL;

-- =====================================================
-- PASSO 3: MIGRAR DADOS DE DISCORD_SUBSCRIPTIONS (se existir)
-- =====================================================

-- Migrar dados da tabela discord_subscriptions existente
INSERT IGNORE INTO `discord_users_optimized` (`discord_id`, `subscription_expires_at`, `subscription_type`)
SELECT 
    discord_id,
    subscription_expires_at,
    subscription_type
FROM `discord_subscriptions`
WHERE discord_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    subscription_expires_at = VALUES(subscription_expires_at),
    subscription_type = VALUES(subscription_type);

-- =====================================================
-- PASSO 4: MIGRAR DADOS DE PLAYER_DATA (assinaturas individuais)
-- =====================================================

-- Migrar assinaturas individuais de player_data para discord_users
INSERT IGNORE INTO `discord_users_optimized` (`discord_id`, `subscription_expires_at`)
SELECT 
    dl.discord_id,
    pd.subscription_expires_at
FROM `discord_links` dl
JOIN `player_data` pd ON dl.player_id = pd.player_id
WHERE dl.verified = TRUE 
    AND pd.subscription_expires_at IS NOT NULL
    AND pd.subscription_expires_at > NOW()
ON DUPLICATE KEY UPDATE
    subscription_expires_at = CASE 
        WHEN VALUES(subscription_expires_at) > subscription_expires_at 
        THEN VALUES(subscription_expires_at)
        ELSE subscription_expires_at
    END;

-- =====================================================
-- PASSO 5: LIMPAR PLAYER_DATA (remover campos redundantes)
-- =====================================================

-- Remover campos redundantes de player_data
ALTER TABLE `player_data` 
DROP COLUMN IF EXISTS `donor_tier`,
DROP COLUMN IF EXISTS `donor_tier_expires_at`,
DROP COLUMN IF EXISTS `subscription_expires_at`;

-- =====================================================
-- PASSO 6: SUBSTITUIR TABELAS ANTIGAS
-- =====================================================

-- Fazer backup das tabelas antigas
RENAME TABLE `discord_users` TO `discord_users_backup`;
RENAME TABLE `discord_subscriptions` TO `discord_subscriptions_backup`;

-- Renomear tabela otimizada
RENAME TABLE `discord_users_optimized` TO `discord_users`;

-- =====================================================
-- PASSO 7: ATUALIZAR FOREIGN KEYS
-- =====================================================

-- Adicionar foreign key para discord_links
ALTER TABLE `discord_links` 
ADD CONSTRAINT `fk_discord_links_user` 
FOREIGN KEY (`discord_id`) REFERENCES `discord_users` (`discord_id`) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- =====================================================
-- PASSO 8: VERIFICAÇÃO FINAL
-- =====================================================

-- Verificar dados migrados
SELECT 
    'DADOS MIGRADOS:' as info,
    COUNT(*) as total_discord_users
FROM `discord_users`;

-- Verificar assinaturas ativas
SELECT 
    'ASSINATURAS ATIVAS:' as info,
    COUNT(*) as total_ativas
FROM `discord_users` 
WHERE subscription_expires_at > NOW();

-- Verificar doadores ativos
SELECT 
    'DOADORES ATIVOS:' as info,
    COUNT(*) as total_doadores
FROM `discord_users` 
WHERE donor_tier > 0;

-- =====================================================
-- FIM DA MIGRAÇÃO
-- =====================================================

SELECT 
    'MIGRAÇÃO CONCLUÍDA COM SUCESSO!' as status,
    NOW() as data_migracao;
