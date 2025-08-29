-- =========================================================
-- CORREÇÃO DA ESTRUTURA DO BANCO DE DADOS
-- Adiciona player_id como FK na tabela discord_links
-- =========================================================

-- 1. Adicionar coluna player_id na tabela discord_links
ALTER TABLE discord_links 
ADD COLUMN player_id INT(11) AFTER discord_id;

-- 2. Adicionar índice para performance
ALTER TABLE discord_links 
ADD INDEX idx_player_id (player_id);

-- 3. Adicionar constraint de chave estrangeira
ALTER TABLE discord_links 
ADD CONSTRAINT fk_discord_links_player_id 
FOREIGN KEY (player_id) REFERENCES player_data(player_id) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- 4. Preencher dados existentes (se houver)
UPDATE discord_links dl 
JOIN player_data pd ON dl.player_uuid = pd.uuid 
SET dl.player_id = pd.player_id 
WHERE dl.player_id IS NULL;

-- 5. Tornar a coluna NOT NULL após preencher
ALTER TABLE discord_links 
MODIFY COLUMN player_id INT(11) NOT NULL;

-- 6. Adicionar índice composto para otimização
ALTER TABLE discord_links 
ADD INDEX idx_discord_player (discord_id, player_id);

-- Verificar se a correção foi aplicada
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'primeleague' 
AND TABLE_NAME = 'discord_links'
ORDER BY ORDINAL_POSITION;
