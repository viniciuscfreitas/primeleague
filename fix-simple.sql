-- ========================================
-- CORREÇÃO SIMPLES DO ROLE 'OFFICER'
-- ========================================
-- Execute este script no HeidiSQL para corrigir o problema

USE primeleague;

-- 1. VERIFICAR PROBLEMA ATUAL
SELECT 'PROBLEMA IDENTIFICADO:' as status;
SELECT * FROM clan_players;

-- 2. CORRIGIR ROLE 'OFFICER' PARA 'LEADER'
UPDATE clan_players SET role = 'LEADER' WHERE role = 'OFFICER';

-- 3. VERIFICAR CORREÇÃO
SELECT 'PROBLEMA CORRIGIDO:' as status;
SELECT * FROM clan_players;

-- 4. VERIFICAR CONTAGEM DE MEMBROS
SELECT 
    c.id,
    c.tag,
    c.name,
    COUNT(cp.player_id) as member_count,
    GROUP_CONCAT(cp.role) as roles
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id
GROUP BY c.id;

-- 5. VERIFICAR SE HÁ OUTROS PROBLEMAS
SELECT DISTINCT role, COUNT(*) as count 
FROM clan_players 
GROUP BY role;

SELECT 'CORREÇÃO COMPLETA!' as resultado;
