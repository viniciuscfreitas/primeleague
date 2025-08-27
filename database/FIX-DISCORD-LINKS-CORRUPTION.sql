-- =====================================================
-- CORREÇÃO DE CORRUPÇÃO DE DADOS - discord_links
-- =====================================================
-- Problema: Dados completamente desalinhados com a estrutura
-- Solução: Backup, limpeza e recriação com dados corretos
-- =====================================================

-- 1. BACKUP DOS DADOS ATUAIS (mesmo que corrompidos)
CREATE TABLE IF NOT EXISTS discord_links_backup_corruption AS 
SELECT * FROM discord_links;

-- 2. LIMPAR TABELA ATUAL
TRUNCATE TABLE discord_links;

-- 3. VERIFICAR SE A ESTRUTURA ESTÁ CORRETA
-- (A estrutura já deve estar correta baseada no SCHEMA-CANONICO-ULTIMATE.sql)

-- 4. INSERIR DADOS CORRETOS BASEADOS NO BACKUP
-- Analisando os dados corrompidos:
-- - discord_id real: 531571143035846657 (estava em player_id)
-- - player_id real: 3 (estava em player_name)
-- - verify_code: precisa ser gerado novamente
-- - verify_expires_at: precisa ser recalculado

INSERT INTO discord_links (
    discord_id, 
    player_id, 
    is_primary, 
    verified, 
    verification_code, 
    code_expires_at, 
    verified_at
) VALUES (
    '531571143035846657',  -- Discord ID correto
    3,                     -- Player ID correto (vini)
    1,                     -- Conta principal
    0,                     -- Não verificado ainda
    NULL,                  -- Código será gerado pelo bot
    NULL,                  -- Expiração será definida pelo bot
    NULL                   -- Ainda não verificado
);

-- 5. VERIFICAR CORREÇÃO
SELECT 
    'DADOS CORRIGIDOS' as status,
    link_id,
    discord_id,
    player_id,
    is_primary,
    verified,
    verification_code,
    code_expires_at,
    verified_at
FROM discord_links;

-- 6. VERIFICAR INTEGRIDADE COM player_data
SELECT 
    'VERIFICAÇÃO DE INTEGRIDADE' as status,
    dl.link_id,
    dl.discord_id,
    dl.player_id,
    pd.name as player_name,
    dl.is_primary,
    dl.verified
FROM discord_links dl
JOIN player_data pd ON dl.player_id = pd.player_id;

-- =====================================================
-- RESULTADO ESPERADO:
-- - discord_id: 531571143035846657 (Discord ID do vini)
-- - player_id: 3 (ID do jogador vini)
-- - player_name: vini (nome do jogador)
-- - verified: 0 (aguardando verificação)
-- =====================================================
