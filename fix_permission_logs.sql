-- ========================================
-- CORREÇÃO DA TABELA PERMISSION_LOGS
-- ========================================

-- Permitir que actor_player_id seja NULL (para comandos do console)
ALTER TABLE `permission_logs` MODIFY COLUMN `actor_player_id` INT NULL;

-- Atualizar a constraint para permitir NULL
ALTER TABLE `permission_logs` DROP FOREIGN KEY `fk_permission_logs_actor`;
ALTER TABLE `permission_logs` ADD CONSTRAINT `fk_permission_logs_actor` 
    FOREIGN KEY (`actor_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL;

-- Verificar a estrutura corrigida
DESCRIBE `permission_logs`;
