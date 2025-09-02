-- ========================================
-- SCRIPT DE CORREÇÃO DO BANCO DE DADOS PRIMELEAGUE
-- ========================================
-- Este script corrige problemas de schema identificados no sistema de clãs

USE primeleague;

-- ========================================
-- 1. VERIFICAÇÃO INICIAL
-- ========================================

-- Verificar estrutura atual da tabela clan_players
DESCRIBE clan_players;

-- Verificar dados atuais
SELECT * FROM clan_players;

-- Verificar contagem de membros por clã
SELECT 
    c.id,
    c.tag,
    c.name,
    COUNT(cp.player_id) as member_count,
    GROUP_CONCAT(cp.role) as roles
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id
GROUP BY c.id;

-- ========================================
-- 2. CORREÇÃO DE ROLES INCORRETOS
-- ========================================

-- Mapear roles antigos para novos
UPDATE clan_players SET role = 'LEADER' WHERE role = 'OFFICER';
UPDATE clan_players SET role = 'MEMBER' WHERE role = 'MEMBER';
UPDATE clan_players SET role = 'LEADER' WHERE role = 'LEADER';
UPDATE clan_players SET role = 'FOUNDER' WHERE role = 'FOUNDER';

-- Verificar se há roles inválidos
SELECT DISTINCT role, COUNT(*) as count 
FROM clan_players 
GROUP BY role;

-- ========================================
-- 3. CORREÇÃO DE DADOS DUPLICADOS
-- ========================================

-- Identificar duplicatas
SELECT player_id, clan_id, COUNT(*) as count
FROM clan_players
GROUP BY player_id, clan_id
HAVING COUNT(*) > 1;

-- Remover duplicatas mantendo apenas o registro mais recente
DELETE cp1 FROM clan_players cp1
INNER JOIN clan_players cp2 
WHERE cp1.id < cp2.id 
AND cp1.player_id = cp2.player_id 
AND cp1.clan_id = cp2.clan_id;

-- ========================================
-- 4. CORREÇÃO DE CLÃS SEM MEMBROS
-- ========================================

-- Identificar clãs sem membros
SELECT c.id, c.tag, c.name, c.founder_player_id
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id
WHERE cp.player_id IS NULL;

-- Adicionar fundador como membro se não existir
INSERT INTO clan_players (player_id, clan_id, role, join_date, kills, deaths)
SELECT 
    c.founder_player_id,
    c.id,
    'FOUNDER',
    NOW(),
    0,
    0
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id AND cp.player_id = c.founder_player_id
WHERE cp.player_id IS NULL 
AND c.founder_player_id IS NOT NULL;

-- ========================================
-- 5. CORREÇÃO DE JOGADORES ÓRFÃOS
-- ========================================

-- Identificar jogadores em clãs inexistentes
SELECT cp.player_id, cp.clan_id, cp.role
FROM clan_players cp
LEFT JOIN clans c ON cp.clan_id = c.id
WHERE c.id IS NULL;

-- Remover jogadores em clãs inexistentes
DELETE FROM clan_players 
WHERE clan_id NOT IN (SELECT id FROM clans);

-- Identificar jogadores inexistentes
SELECT cp.player_id, cp.clan_id, cp.role
FROM clan_players cp
LEFT JOIN player_data pd ON cp.player_id = pd.player_id
WHERE pd.player_id IS NULL;

-- Remover jogadores inexistentes
DELETE FROM clan_players 
WHERE player_id NOT IN (SELECT player_id FROM player_data);

-- ========================================
-- 6. VERIFICAÇÃO FINAL
-- ========================================

-- Verificar contagem final de membros por clã
SELECT 
    c.id,
    c.tag,
    c.name,
    COUNT(cp.player_id) as member_count,
    GROUP_CONCAT(cp.role ORDER BY cp.role) as roles
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id
GROUP BY c.id
ORDER BY c.id;

-- Verificar estrutura final da tabela
DESCRIBE clan_players;

-- Verificar se há problemas restantes
SELECT 
    'Verificação Final' as status,
    COUNT(DISTINCT c.id) as total_clans,
    COUNT(cp.player_id) as total_members,
    COUNT(DISTINCT cp.player_id) as unique_members
FROM clans c
LEFT JOIN clan_players cp ON c.id = cp.clan_id;

-- ========================================
-- 7. CRIAR ÍNDICES PARA PERFORMANCE
-- ========================================

-- Adicionar índices se não existirem
CREATE INDEX IF NOT EXISTS idx_clan_players_clan_id ON clan_players(clan_id);
CREATE INDEX IF NOT EXISTS idx_clan_players_player_id ON clan_players(player_id);
CREATE INDEX IF NOT EXISTS idx_clan_players_role ON clan_players(role);

-- ========================================
-- 8. VERIFICAR INTEGRIDADE REFERENCIAL
-- ========================================

-- Verificar foreign keys
SELECT 
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'primeleague' 
AND REFERENCED_TABLE_NAME IS NOT NULL
AND TABLE_NAME = 'clan_players';

-- ========================================
-- FIM DO SCRIPT
-- ========================================

SELECT 'Script de correção executado com sucesso!' as resultado;
