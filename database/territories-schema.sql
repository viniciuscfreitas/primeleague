-- ========================================
-- SCHEMA DO MÓDULO DE TERRITÓRIOS
-- ========================================
-- Adiciona tabelas necessárias para o sistema de territórios
-- Baseado no PRD v1.0 de 07/09/2025

-- ========================================
-- TERRITÓRIOS REIVINDICADOS
-- ========================================

CREATE TABLE IF NOT EXISTS `prime_territories` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `clan_id` INT NOT NULL,
    `world_name` VARCHAR(64) NOT NULL,
    `chunk_x` INT NOT NULL,
    `chunk_z` INT NOT NULL,
    `claimed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_prime_territories_chunk` (`world_name`, `chunk_x`, `chunk_z`),
    KEY `idx_prime_territories_clan_id` (`clan_id`),
    KEY `idx_prime_territories_claimed_at` (`claimed_at`),
    CONSTRAINT `fk_prime_territories_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- GUERRAS ATIVAS
-- ========================================

CREATE TABLE IF NOT EXISTS `prime_active_wars` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `aggressor_clan_id` INT NOT NULL,
    `defender_clan_id` INT NOT NULL,
    `start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time_exclusivity` TIMESTAMP NULL,
    `status` ENUM('DECLARED', 'SIEGE_ACTIVE', 'COMPLETED', 'EXPIRED') NOT NULL DEFAULT 'DECLARED',
    PRIMARY KEY (`id`),
    KEY `idx_prime_active_wars_aggressor` (`aggressor_clan_id`),
    KEY `idx_prime_active_wars_defender` (`defender_clan_id`),
    KEY `idx_prime_active_wars_status` (`status`),
    KEY `idx_prime_active_wars_start_time` (`start_time`),
    CONSTRAINT `fk_prime_active_wars_aggressor` FOREIGN KEY (`aggressor_clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prime_active_wars_defender` FOREIGN KEY (`defender_clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- BANCO DOS CLÃS
-- ========================================

CREATE TABLE IF NOT EXISTS `prime_clan_bank` (
    `clan_id` INT NOT NULL,
    `balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `last_maintenance` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`clan_id`),
    KEY `idx_prime_clan_bank_balance` (`balance`),
    KEY `idx_prime_clan_bank_last_maintenance` (`last_maintenance`),
    CONSTRAINT `fk_prime_clan_bank_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- LOGS DE TERRITÓRIOS
-- ========================================

CREATE TABLE IF NOT EXISTS `prime_territory_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `clan_id` INT NOT NULL,
    `action_type` ENUM('CLAIM', 'UNCLAIM', 'WAR_DECLARED', 'WAR_ENDED', 'SIEGE_STARTED', 'SIEGE_ENDED', 'MAINTENANCE_PAID', 'MAINTENANCE_FAILED', 'BANK_DEPOSIT', 'BANK_WITHDRAW') NOT NULL,
    `target_clan_id` INT NULL,
    `territory_id` INT NULL,
    `war_id` INT NULL,
    `details` TEXT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_prime_territory_logs_clan_id` (`clan_id`),
    KEY `idx_prime_territory_logs_action_type` (`action_type`),
    KEY `idx_prime_territory_logs_timestamp` (`timestamp`),
    KEY `idx_prime_territory_logs_target_clan` (`target_clan_id`),
    CONSTRAINT `fk_prime_territory_logs_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prime_territory_logs_target_clan` FOREIGN KEY (`target_clan_id`) REFERENCES `clans` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_prime_territory_logs_territory` FOREIGN KEY (`territory_id`) REFERENCES `prime_territories` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_prime_territory_logs_war` FOREIGN KEY (`war_id`) REFERENCES `prime_active_wars` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- CERCOS ATIVOS (CACHE EM MEMÓRIA)
-- ========================================
-- Nota: Esta tabela é opcional e pode ser usada para persistir cercos
-- em caso de reinicialização do servidor

CREATE TABLE IF NOT EXISTS `prime_active_sieges` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `war_id` INT NOT NULL,
    `territory_id` INT NOT NULL,
    `aggressor_clan_id` INT NOT NULL,
    `defender_clan_id` INT NOT NULL,
    `world_name` VARCHAR(64) NOT NULL,
    `altar_x` INT NOT NULL,
    `altar_y` INT NOT NULL,
    `altar_z` INT NOT NULL,
    `start_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time` TIMESTAMP NOT NULL,
    `remaining_time` INT NOT NULL,
    `status` ENUM('ACTIVE', 'ATTACKER_WIN', 'DEFENDER_WIN', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (`id`),
    KEY `idx_prime_active_sieges_war_id` (`war_id`),
    KEY `idx_prime_active_sieges_territory_id` (`territory_id`),
    KEY `idx_prime_active_sieges_status` (`status`),
    KEY `idx_prime_active_sieges_end_time` (`end_time`),
    CONSTRAINT `fk_prime_active_sieges_war` FOREIGN KEY (`war_id`) REFERENCES `prime_active_wars` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prime_active_sieges_territory` FOREIGN KEY (`territory_id`) REFERENCES `prime_territories` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prime_active_sieges_aggressor` FOREIGN KEY (`aggressor_clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_prime_active_sieges_defender` FOREIGN KEY (`defender_clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- ÍNDICES ADICIONAIS PARA PERFORMANCE
-- ========================================

-- Índice composto para consultas de territórios por clã e mundo
CREATE INDEX `idx_prime_territories_clan_world` ON `prime_territories` (`clan_id`, `world_name`);

-- Índice composto para consultas de guerras ativas
CREATE INDEX `idx_prime_active_wars_active` ON `prime_active_wars` (`status`, `start_time`);

-- Índice para consultas de logs por período
CREATE INDEX `idx_prime_territory_logs_period` ON `prime_territory_logs` (`timestamp`, `action_type`);

-- ========================================
-- PROCEDURES PARA MANUTENÇÃO
-- ========================================

DELIMITER //

-- Procedure para verificar manutenção de territórios
CREATE PROCEDURE `CheckTerritoryMaintenance`()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_clan_id INT;
    DECLARE v_territory_count INT;
    DECLARE v_maintenance_cost DECIMAL(15,2);
    DECLARE v_bank_balance DECIMAL(15,2);
    DECLARE v_territory_id INT;
    
    DECLARE clan_cursor CURSOR FOR
        SELECT DISTINCT clan_id FROM prime_territories;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN clan_cursor;
    
    read_loop: LOOP
        FETCH clan_cursor INTO v_clan_id;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- Contar territórios do clã
        SELECT COUNT(*) INTO v_territory_count FROM prime_territories WHERE clan_id = v_clan_id;
        
        -- Calcular custo de manutenção (exponencial)
        SET v_maintenance_cost = 100.0 * POWER(1.2, v_territory_count - 1);
        
        -- Obter saldo do banco
        SELECT COALESCE(balance, 0) INTO v_bank_balance FROM prime_clan_bank WHERE clan_id = v_clan_id;
        
        -- Verificar se tem saldo suficiente
        IF v_bank_balance < v_maintenance_cost THEN
            -- Remover território mais antigo
            SELECT id INTO v_territory_id FROM prime_territories 
            WHERE clan_id = v_clan_id 
            ORDER BY claimed_at ASC 
            LIMIT 1;
            
            IF v_territory_id IS NOT NULL THEN
                DELETE FROM prime_territories WHERE id = v_territory_id;
                
                -- Log da ação
                INSERT INTO prime_territory_logs (clan_id, action_type, territory_id, details)
                VALUES (v_clan_id, 'MAINTENANCE_FAILED', v_territory_id, 
                    CONCAT('Território removido por falta de manutenção. Custo: ', v_maintenance_cost, ', Saldo: ', v_bank_balance));
            END IF;
        ELSE
            -- Debitar manutenção
            UPDATE prime_clan_bank 
            SET balance = balance - v_maintenance_cost, 
                last_maintenance = NOW(),
                updated_at = NOW()
            WHERE clan_id = v_clan_id;
            
            -- Log da ação
            INSERT INTO prime_territory_logs (clan_id, action_type, details)
            VALUES (v_clan_id, 'MAINTENANCE_PAID', 
                CONCAT('Manutenção paga. Custo: ', v_maintenance_cost, ', Saldo restante: ', v_bank_balance - v_maintenance_cost));
        END IF;
    END LOOP;
    
    CLOSE clan_cursor;
END //

-- Procedure para limpar dados expirados
CREATE PROCEDURE `CleanupExpiredTerritoryData`()
BEGIN
    -- Limpar guerras expiradas
    UPDATE prime_active_wars 
    SET status = 'EXPIRED' 
    WHERE status = 'DECLARED' 
    AND end_time_exclusivity < NOW();
    
    -- Limpar cercos expirados
    UPDATE prime_active_sieges 
    SET status = 'EXPIRED' 
    WHERE status = 'ACTIVE' 
    AND end_time < NOW();
    
    -- Limpar logs antigos (mais de 30 dias)
    DELETE FROM prime_territory_logs 
    WHERE timestamp < DATE_SUB(NOW(), INTERVAL 30 DAY);
    
    SELECT ROW_COUNT() as cleaned_records;
END //

DELIMITER ;

-- ========================================
-- VIEWS PARA CONSULTAS FREQUENTES
-- ========================================

-- View para estatísticas de territórios por clã
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

-- View para guerras ativas
CREATE VIEW `v_active_wars` AS
SELECT 
    paw.id as war_id,
    paw.aggressor_clan_id,
    ac.tag as aggressor_tag,
    ac.name as aggressor_name,
    paw.defender_clan_id,
    dc.tag as defender_tag,
    dc.name as defender_name,
    paw.start_time,
    paw.end_time_exclusivity,
    paw.status,
    CASE 
        WHEN paw.end_time_exclusivity IS NULL THEN NULL
        WHEN paw.end_time_exclusivity > NOW() THEN 
            TIMESTAMPDIFF(HOUR, NOW(), paw.end_time_exclusivity)
        ELSE 0
    END as hours_remaining
FROM prime_active_wars paw
JOIN clans ac ON paw.aggressor_clan_id = ac.id
JOIN clans dc ON paw.defender_clan_id = dc.id
WHERE paw.status IN ('DECLARED', 'SIEGE_ACTIVE');

-- ========================================
-- TRIGGERS PARA AUDITORIA
-- ========================================

DELIMITER //

-- Trigger para log automático de criação de territórios
CREATE TRIGGER `tr_prime_territories_insert` 
AFTER INSERT ON `prime_territories`
FOR EACH ROW
BEGIN
    INSERT INTO prime_territory_logs (clan_id, action_type, territory_id, details)
    VALUES (NEW.clan_id, 'CLAIM', NEW.id, 
        CONCAT('Território reivindicado em ', NEW.world_name, ' (', NEW.chunk_x, ', ', NEW.chunk_z, ')'));
END //

-- Trigger para log automático de remoção de territórios
CREATE TRIGGER `tr_prime_territories_delete` 
AFTER DELETE ON `prime_territories`
FOR EACH ROW
BEGIN
    INSERT INTO prime_territory_logs (clan_id, action_type, territory_id, details)
    VALUES (OLD.clan_id, 'UNCLAIM', OLD.id, 
        CONCAT('Território removido de ', OLD.world_name, ' (', OLD.chunk_x, ', ', OLD.chunk_z, ')'));
END //

DELIMITER ;

-- ========================================
-- DADOS INICIAIS
-- ========================================

-- Inserir bancos vazios para clãs existentes
INSERT IGNORE INTO prime_clan_bank (clan_id, balance, last_maintenance, created_at, updated_at)
SELECT id, 0.00, NOW(), NOW(), NOW() FROM clans;

-- ========================================
-- VERIFICAÇÃO FINAL
-- ========================================

-- Verificar se as tabelas foram criadas
SELECT 
    'TERRITORIES SCHEMA:' as info,
    'Tabelas do módulo de territórios criadas com sucesso' as resultado
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
AND table_name IN ('prime_territories', 'prime_active_wars', 'prime_clan_bank', 'prime_territory_logs', 'prime_active_sieges');

-- Verificar foreign keys
SELECT 
    'FOREIGN KEYS:' as info,
    COUNT(*) as total_foreign_keys
FROM information_schema.key_column_usage 
WHERE table_schema = DATABASE() 
AND table_name LIKE 'prime_%'
AND referenced_table_name IS NOT NULL;

-- ========================================
-- FIM DO SCHEMA
-- ========================================

SELECT 'Schema do Módulo de Territórios criado com sucesso!' as status;
SELECT 'Sistema de territórios pronto para uso' as feature;
SELECT NOW() as created_at;
