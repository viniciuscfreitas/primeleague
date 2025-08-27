-- =====================================================
-- CORREÇÃO DA MIGRAÇÃO DISCORD_USERS
-- =====================================================

-- Limpar dados incorretos
DELETE FROM `discord_users`;

-- Inserir dados corretos (migração)
INSERT INTO `discord_users` (`discord_id`, `donor_tier`, `donor_tier_expires_at`)
SELECT 
    dl.discord_id,
    COALESCE(MAX(pd.donor_tier), 0) as donor_tier,
    MAX(pd.donor_tier_expires_at) as donor_tier_expires_at
FROM `discord_links` dl
JOIN `player_data` pd ON dl.player_id = pd.player_id
WHERE dl.verified = TRUE
GROUP BY dl.discord_id;

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
