-- ========================================
-- CORREÇÃO DO SCHEMA DE TERRITÓRIOS
-- ========================================
-- Correção da VIEW v_clan_territory_stats que estava referenciando
-- uma coluna 'moral' inexistente na tabela 'clans'

-- Remover a VIEW problemática se ela existir
DROP VIEW IF EXISTS `v_clan_territory_stats`;

-- Recriar a VIEW corrigida
CREATE VIEW `v_clan_territory_stats` AS
SELECT 
    c.id as clan_id,
    c.tag as clan_tag,
    c.name as clan_name,
    COUNT(pt.id) as territory_count,
    COALESCE(pcb.balance, 0) as bank_balance,
    COALESCE(pcb.last_maintenance, c.creation_date) as last_maintenance,
    'UNKNOWN' as territory_state
FROM clans c
LEFT JOIN prime_territories pt ON c.id = pt.clan_id
LEFT JOIN prime_clan_bank pcb ON c.id = pcb.clan_id
GROUP BY c.id, c.tag, c.name, pcb.balance, pcb.last_maintenance;

-- Verificar se a VIEW foi criada corretamente
SELECT 'VIEW v_clan_territory_stats criada com sucesso!' as status;
