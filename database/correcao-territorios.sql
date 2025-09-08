-- ========================================
-- CORREÇÃO DOS PROBLEMAS DE TERRITÓRIOS
-- ========================================

-- PROBLEMA 1: Foreign Key Constraint
-- O jogador está tentando usar clan_id=2, mas só existe clan_id=1

-- Verificar qual clã o jogador vinicff pertence
SELECT '=== VERIFICANDO CLÃ DO JOGADOR ===' as info;
SELECT 
    pd.username,
    pd.player_id,
    c.id as clan_id,
    c.tag as clan_tag,
    c.name as clan_name,
    cp.role
FROM player_data pd
JOIN clan_players cp ON pd.player_id = cp.player_id
JOIN clans c ON cp.clan_id = c.id
WHERE pd.username = 'vinicff';

-- PROBLEMA 2: Inserir banco vazio para o clã existente
-- Se o clã não tem banco, criar um
INSERT IGNORE INTO prime_clan_bank (clan_id, balance, last_maintenance, created_at, updated_at)
SELECT 
    c.id,
    1000.00,  -- Saldo inicial de R$ 1000
    NOW(),
    NOW(),
    NOW()
FROM clans c
WHERE c.id NOT IN (SELECT clan_id FROM prime_clan_bank);

-- Verificar se o banco foi criado
SELECT '=== BANCOS DOS CLÃS ===' as info;
SELECT 
    c.id as clan_id,
    c.tag as clan_tag,
    c.name as clan_name,
    COALESCE(pcb.balance, 0) as bank_balance,
    pcb.last_maintenance
FROM clans c
LEFT JOIN prime_clan_bank pcb ON c.id = pcb.clan_id
ORDER BY c.id;

-- PROBLEMA 3: Verificar se o jogador tem permissão para claim
-- O jogador precisa ser LEADER ou OFFICER do clã
SELECT '=== PERMISSÕES DO JOGADOR ===' as info;
SELECT 
    pd.username,
    c.tag as clan_tag,
    cp.role,
    CASE 
        WHEN cp.role = 'LEADER' THEN 'PODE FAZER CLAIM'
        WHEN cp.role = 'OFFICER' THEN 'PODE FAZER CLAIM'
        ELSE 'NÃO PODE FAZER CLAIM'
    END as permissao_claim
FROM player_data pd
JOIN clan_players cp ON pd.player_id = cp.player_id
JOIN clans c ON cp.clan_id = c.id
WHERE pd.username = 'vinicff';

-- PROBLEMA 4: Verificar se o chunk já está claimado
-- Verificar se o chunk (11, -6) no mundo 'world' já está claimado
SELECT '=== VERIFICANDO CHUNK ===' as info;
SELECT 
    id,
    clan_id,
    world_name,
    chunk_x,
    chunk_z,
    claimed_at
FROM prime_territories 
WHERE world_name = 'world' 
AND chunk_x = 11 
AND chunk_z = -6;

-- Se não estiver claimado, mostrar como fazer o claim manualmente
SELECT '=== INSTRUÇÕES PARA CLAIM MANUAL ===' as info;
SELECT 
    'Para fazer claim manual, use:' as instrucao,
    CONCAT('INSERT INTO prime_territories (clan_id, world_name, chunk_x, chunk_z, claimed_at) VALUES (1, ''world'', 11, -6, NOW());') as sql_exemplo;
