-- =====================================================
-- VERIFICAÇÃO DA ESTRUTURA ATUAL DO BANCO
-- =====================================================

USE `primeleague`;

-- 1. VERIFICAR ESTRUTURA DA TABELA discord_links
DESCRIBE `discord_links`;

-- 2. VERIFICAR ESTRUTURA DA TABELA player_data
DESCRIBE `player_data`;

-- 3. VERIFICAR QUAIS COLUNAS EXISTEM NA TABELA discord_links
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'primeleague' 
  AND TABLE_NAME = 'discord_links'
ORDER BY ORDINAL_POSITION;

-- 4. TESTAR SE A COLUNA player_id EXISTE
SELECT 
    CASE 
        WHEN EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = 'primeleague' 
                     AND TABLE_NAME = 'discord_links' 
                     AND COLUMN_NAME = 'player_id') 
        THEN 'player_id EXISTE'
        ELSE 'player_id NÃO EXISTE'
    END as status_player_id;

-- 5. TESTAR SE A COLUNA player_uuid EXISTE
SELECT 
    CASE 
        WHEN EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
                   WHERE TABLE_SCHEMA = 'primeleague' 
                     AND TABLE_NAME = 'discord_links' 
                     AND COLUMN_NAME = 'player_uuid') 
        THEN 'player_uuid EXISTE'
        ELSE 'player_uuid NÃO EXISTE'
    END as status_player_uuid;

-- 6. VERIFICAR DADOS ATUAIS (versão simplificada)
SELECT 
    'DADOS ATUAIS' as info,
    dl.link_id,
    dl.discord_id,
    pd.name as player_name,
    dl.verification_code,
    dl.code_expires_at,
    dl.verified
FROM discord_links dl
JOIN player_data pd ON dl.player_id = pd.player_id
LIMIT 5;
