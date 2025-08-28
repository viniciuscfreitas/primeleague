-- =====================================================
-- CRIAÇÃO DE VÍNCULOS DISCORD - FASE 2
-- =====================================================

USE primeleague;

-- Limpar vínculos de teste anteriores
DELETE FROM discord_links WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

-- =====================================================
-- CRIAR VÍNCULOS DISCORD
-- =====================================================

-- Vínculo 1: TestadorAlfa -> discord_alfa_id
INSERT INTO discord_links (discord_id, player_uuid, is_primary, verified, verified_at) 
SELECT 'discord_alfa_id', uuid, 1, 1, NOW()
FROM player_data 
WHERE name = 'TestadorAlfa';

-- Vínculo 2: TestadorBeta -> discord_beta_id
INSERT INTO discord_links (discord_id, player_uuid, is_primary, verified, verified_at) 
SELECT 'discord_beta_id', uuid, 1, 1, NOW()
FROM player_data 
WHERE name = 'TestadorBeta';

-- Vínculo 3: TestadorOmega -> discord_omega_id
INSERT INTO discord_links (discord_id, player_uuid, is_primary, verified, verified_at) 
SELECT 'discord_omega_id', uuid, 1, 1, NOW()
FROM player_data 
WHERE name = 'TestadorOmega';

-- =====================================================
-- VERIFICAÇÃO DOS VÍNCULOS CRIADOS
-- =====================================================

SELECT 'VÍNCULOS CRIADOS:' as info;
SELECT 'Discord Links:' as tabela;
SELECT dl.discord_id, dl.player_uuid, pd.name, dl.verified, dl.verified_at
FROM discord_links dl
JOIN player_data pd ON dl.player_uuid = pd.uuid
WHERE dl.discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

SELECT 'DADOS COMPLETOS:' as info;
SELECT 'Player + Discord + Link:' as tabela;
SELECT 
    pd.name,
    pd.uuid,
    du.discord_id,
    du.subscription_type,
    du.subscription_expires_at,
    dl.verified
FROM player_data pd
LEFT JOIN discord_links dl ON pd.uuid = dl.player_uuid
LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
WHERE pd.name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

SELECT 'VÍNCULOS PRONTOS PARA TESTE!' as status;
