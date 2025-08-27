-- =========================================================
-- MIGRAÇÃO: discord_links V1 -> V2
-- Data: 27/08/2025
-- Descrição: Migração da tabela discord_links para nova estrutura
--            conforme especificação de Apoiadores e Contas Alternativas
-- =========================================================

-- =====================================================
-- BACKUP DA TABELA ATUAL
-- =====================================================

-- Criar backup da tabela atual
CREATE TABLE IF NOT EXISTS `discord_links_backup_v1` AS 
SELECT * FROM `discord_links`;

-- Verificar se o backup foi criado com sucesso
SELECT 
    'BACKUP CRIADO' as status,
    COUNT(*) as registros_backup
FROM `discord_links_backup_v1`;

-- =====================================================
-- MIGRAÇÃO DA ESTRUTURA
-- =====================================================

-- 1. Remover foreign key constraint existente
ALTER TABLE `discord_links` 
DROP FOREIGN KEY `fk_discord_links_player`;

-- 2. Remover índices existentes
ALTER TABLE `discord_links` 
DROP INDEX `uk_discord_links_player_id`,
DROP INDEX `idx_discord_links_verify_code`,
DROP INDEX `idx_discord_links_expires_at`,
DROP INDEX `idx_discord_links_player_name`,
DROP INDEX `idx_discord_links_verified`;

-- 3. Remover chave primária atual
ALTER TABLE `discord_links` 
DROP PRIMARY KEY;

-- 4. Adicionar nova coluna link_id
ALTER TABLE `discord_links` 
ADD COLUMN `link_id` INT NOT NULL AUTO_INCREMENT FIRST;

-- 5. Definir nova chave primária
ALTER TABLE `discord_links` 
ADD PRIMARY KEY (`link_id`);

-- 6. Renomear colunas para nova especificação
ALTER TABLE `discord_links` 
CHANGE COLUMN `verify_code` `verification_code` VARCHAR(8) NULL DEFAULT NULL,
CHANGE COLUMN `verify_expires_at` `code_expires_at` TIMESTAMP NULL DEFAULT NULL;

-- 7. Remover colunas desnecessárias
ALTER TABLE `discord_links` 
DROP COLUMN `player_name`,
DROP COLUMN `created_at`;

-- 8. Adicionar novos índices conforme especificação
ALTER TABLE `discord_links` 
ADD UNIQUE KEY `uk_player_id` (`player_id`),
ADD KEY `idx_discord_id` (`discord_id`);

-- 9. Recriar foreign key constraint
ALTER TABLE `discord_links` 
ADD CONSTRAINT `fk_discord_links_player` 
FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) 
ON DELETE CASCADE ON UPDATE CASCADE;

-- =====================================================
-- VERIFICAÇÃO DA MIGRAÇÃO
-- =====================================================

-- Verificar estrutura final
DESCRIBE `discord_links`;

-- Verificar dados migrados
SELECT 
    'DADOS MIGRADOS' as status,
    COUNT(*) as total_registros,
    COUNT(CASE WHEN verified = 1 THEN 1 END) as contas_verificadas,
    COUNT(CASE WHEN is_primary = 1 THEN 1 END) as contas_principais
FROM `discord_links`;

-- Verificar integridade dos dados
SELECT 
    'VERIFICAÇÃO DE INTEGRIDADE' as status,
    COUNT(*) as total_links,
    COUNT(DISTINCT discord_id) as usuarios_discord_unicos,
    COUNT(DISTINCT player_id) as jogadores_unicos
FROM `discord_links`;

-- =====================================================
-- LIMPEZA (OPCIONAL)
-- =====================================================

-- Comentar a linha abaixo para manter o backup
-- DROP TABLE `discord_links_backup_v1`;

-- =====================================================
-- STATUS FINAL
-- =====================================================

SELECT 
    'MIGRAÇÃO CONCLUÍDA COM SUCESSO!' as status,
    NOW() as data_migracao,
    'discord_links V2' as versao_atual;
