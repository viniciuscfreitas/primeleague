-- =====================================================
-- CRIAÇÃO DE VÍNCULOS DISCORD - ESTRUTURA REAL
-- =====================================================

USE primeleague;

-- Limpar vínculos de teste anteriores
DELETE FROM discord_links WHERE discord_id IN (
    SELECT discord_id FROM discord_users 
    WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id')
);

-- =====================================================
-- DESCOBRIR OS IDs CORRETOS
-- =====================================================

-- Verificar se discord_links usa discord_id como INT ou VARCHAR
SELECT 'ESTRUTURA discord_links:' as info;
DESCRIBE discord_links;

-- Verificar se discord_users tem IDs numéricos
SELECT 'DISCORD USERS COM IDs:' as info;
SELECT discord_id, subscription_type FROM discord_users 
WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

-- Verificar player_data
SELECT 'PLAYER DATA:' as info;
SELECT player_id, name, uuid FROM player_data 
WHERE name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

-- =====================================================
-- TENTATIVA 1: USANDO STRINGS (se discord_links aceitar)
-- =====================================================

-- Tentar inserir usando strings (como no schema original)
INSERT INTO discord_links (discord_id, player_uuid, is_primary, verified, verified_at) 
SELECT 'discord_alfa_id', uuid, 1, 1, NOW()
FROM player_data 
WHERE name = 'TestadorAlfa';

-- Verificar se funcionou
SELECT 'VÍNCULOS CRIADOS (TENTATIVA 1):' as info;
SELECT * FROM discord_links WHERE discord_id = 'discord_alfa_id';

-- =====================================================
-- SE FALHAR, TENTATIVA 2: USANDO IDs NUMÉRICOS
-- =====================================================

-- Se a tentativa 1 falhar, vamos tentar com IDs numéricos
-- Primeiro, vamos ver se existe algum padrão de IDs numéricos
SELECT 'PADRÃO DE IDs:' as info;
SELECT discord_id, LENGTH(discord_id) as tamanho, 
       CASE 
           WHEN discord_id REGEXP '^[0-9]+$' THEN 'NUMERICO'
           ELSE 'STRING'
       END as tipo
FROM discord_users 
WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

-- =====================================================
-- VERIFICAÇÃO FINAL
-- =====================================================

SELECT 'VERIFICAÇÃO FINAL:' as info;
SELECT 
    pd.name,
    pd.player_id,
    du.discord_id,
    du.subscription_type,
    CASE WHEN dl.discord_id IS NOT NULL THEN 'VINCULADO' ELSE 'SEM VINCULO' END as status
FROM player_data pd
LEFT JOIN discord_links dl ON pd.uuid = dl.player_uuid
LEFT JOIN discord_users du ON dl.discord_id = du.discord_id
WHERE pd.name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');
