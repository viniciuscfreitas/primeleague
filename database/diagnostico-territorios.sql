-- ========================================
-- DIAGNÓSTICO DOS PROBLEMAS DE TERRITÓRIOS
-- ========================================

-- 1. Verificar clãs existentes
SELECT '=== CLÃS EXISTENTES ===' as info;
SELECT id, tag, name, creation_date FROM clans ORDER BY id;

-- 2. Verificar jogadores e seus clãs
SELECT '=== JOGADORES E CLÃS ===' as info;
SELECT 
    pd.player_id,
    pd.username,
    pd.uuid,
    c.id as clan_id,
    c.tag as clan_tag,
    c.name as clan_name,
    cp.role
FROM player_data pd
LEFT JOIN clan_players cp ON pd.player_id = cp.player_id
LEFT JOIN clans c ON cp.clan_id = c.id
ORDER BY pd.player_id;

-- 3. Verificar se as tabelas de territórios existem
SELECT '=== TABELAS DE TERRITÓRIOS ===' as info;
SELECT 
    table_name,
    table_rows
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name LIKE 'prime_%'
ORDER BY table_name;

-- 4. Verificar foreign keys das tabelas de territórios
SELECT '=== FOREIGN KEYS DE TERRITÓRIOS ===' as info;
SELECT 
    table_name,
    column_name,
    referenced_table_name,
    referenced_column_name
FROM information_schema.key_column_usage 
WHERE table_schema = DATABASE() 
AND table_name LIKE 'prime_%'
AND referenced_table_name IS NOT NULL
ORDER BY table_name, column_name;

-- 5. Verificar se há dados nas tabelas de territórios
SELECT '=== DADOS EM TERRITÓRIOS ===' as info;
SELECT 'prime_territories' as tabela, COUNT(*) as registros FROM prime_territories
UNION ALL
SELECT 'prime_active_wars' as tabela, COUNT(*) as registros FROM prime_active_wars
UNION ALL
SELECT 'prime_clan_bank' as tabela, COUNT(*) as registros FROM prime_clan_bank
UNION ALL
SELECT 'prime_territory_logs' as tabela, COUNT(*) as registros FROM prime_territory_logs
UNION ALL
SELECT 'prime_active_sieges' as tabela, COUNT(*) as registros FROM prime_active_sieges;
