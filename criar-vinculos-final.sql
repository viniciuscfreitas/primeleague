-- =====================================================
-- CRIAÇÃO DE VÍNCULOS DISCORD - VERSÃO FINAL CORRETA
-- =====================================================

USE primeleague;

-- Limpar vínculos de teste anteriores
DELETE FROM discord_links WHERE discord_id IN (
    SELECT discord_id FROM discord_users 
    WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id')
);

-- =====================================================
-- CRIAR VÍNCULOS COM ESTRUTURA CORRETA
-- =====================================================

-- Vínculo 1: TestadorAlfa -> discord_alfa_id
INSERT INTO discord_links (discord_id, player_id, is_primary, verified, verified_at) 
SELECT 
    (SELECT discord_id FROM discord_users WHERE discord_id = 'discord_alfa_id'),
    (SELECT player_id FROM player_data WHERE name = 'TestadorAlfa'),
    1, 1, NOW();

-- Vínculo 2: TestadorBeta -> discord_beta_id
INSERT INTO discord_links (discord_id, player_id, is_primary, verified, verified_at) 
SELECT 
    (SELECT discord_id FROM discord_users WHERE discord_id = 'discord_beta_id'),
    (SELECT player_id FROM player_data WHERE name = 'TestadorBeta'),
    1, 1, NOW();

-- Vínculo 3: TestadorOmega -> discord_omega_id
INSERT INTO discord_links (discord_id, player_id, is_primary, verified, verified_at) 
SELECT 
    (SELECT discord_id FROM discord_users WHERE discord_id = 'discord_omega_id'),
    (SELECT player_id FROM player_data WHERE name = 'TestadorOmega'),
    1, 1, NOW();

-- =====================================================
-- VERIFICAÇÃO DOS VÍNCULOS CRIADOS
-- =====================================================

SELECT 'VÍNCULOS CRIADOS:' as info;
SELECT 
    dl.link_id,
    dl.discord_id,
    dl.player_id,
    pd.name as player_name,
    du.discord_id as discord_string_id,
    du.subscription_type,
    dl.verified,
    dl.verified_at
FROM discord_links dl
JOIN player_data pd ON dl.player_id = pd.player_id
JOIN discord_users du ON dl.discord_id = du.discord_id
WHERE pd.name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

-- =====================================================
-- VERIFICAÇÃO COMPLETA DOS DADOS
-- =====================================================

SELECT 'DADOS COMPLETOS PARA TESTE:' as info;
SELECT 
    pd.name as player_name,
    pd.player_id,
    pd.uuid,
    du.discord_id as discord_string_id,
    du.subscription_type,
    du.subscription_expires_at,
    CASE WHEN dl.discord_id IS NOT NULL THEN 'VINCULADO' ELSE 'SEM VINCULO' END as status_vinculo
FROM player_data pd
LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
WHERE pd.name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

SELECT 'VÍNCULOS PRONTOS PARA TESTE!' as status;
