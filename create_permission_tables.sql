-- ========================================
-- SISTEMA DE GRUPOS E PERMISSÕES (CORE)
-- ========================================

-- Tabela de grupos de permissões
CREATE TABLE IF NOT EXISTS `permission_groups` (
    `group_id` INT NOT NULL AUTO_INCREMENT,
    `group_name` VARCHAR(32) NOT NULL,
    `display_name` VARCHAR(64) NOT NULL,
    `description` TEXT NULL,
    `priority` INT NOT NULL DEFAULT 0,
    `is_default` BOOLEAN NOT NULL DEFAULT FALSE,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`group_id`),
    UNIQUE KEY `idx_permission_groups_name` (`group_name`),
    KEY `idx_permission_groups_priority` (`priority`),
    KEY `idx_permission_groups_is_default` (`is_default`),
    KEY `idx_permission_groups_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de permissões dos grupos
CREATE TABLE IF NOT EXISTS `group_permissions` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `group_id` INT NOT NULL,
    `permission_node` VARCHAR(128) NOT NULL,
    `is_granted` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_player_id` INT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_group_permissions_group_node` (`group_id`, `permission_node`),
    KEY `idx_group_permissions_group_id` (`group_id`),
    KEY `idx_group_permissions_node` (`permission_node`),
    CONSTRAINT `fk_group_permissions_group` FOREIGN KEY (`group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_permissions_creator` FOREIGN KEY (`created_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de associação entre jogadores e grupos
CREATE TABLE IF NOT EXISTS `player_groups` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `group_id` INT NOT NULL,
    `is_primary` BOOLEAN NOT NULL DEFAULT FALSE,
    `expires_at` TIMESTAMP NULL DEFAULT NULL,
    `added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `added_by_player_id` INT NULL,
    `reason` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_player_groups_player_group` (`player_id`, `group_id`),
    KEY `idx_player_groups_player_id` (`player_id`),
    KEY `idx_player_groups_group_id` (`group_id`),
    KEY `idx_player_groups_is_primary` (`is_primary`),
    KEY `idx_player_groups_expires_at` (`expires_at`),
    CONSTRAINT `fk_player_groups_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_player_groups_group` FOREIGN KEY (`group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_player_groups_adder` FOREIGN KEY (`added_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela de log de mudanças nas permissões
CREATE TABLE IF NOT EXISTS `permission_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `action_type` ENUM('GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_DELETED', 'PERMISSION_ADDED', 'PERMISSION_REMOVED', 'PLAYER_ADDED_TO_GROUP', 'PLAYER_REMOVED_FROM_GROUP', 'PLAYER_GROUP_UPDATED') NOT NULL,
    `actor_player_id` INT NULL,
    `actor_name` VARCHAR(16) NOT NULL,
    `target_group_id` INT NULL,
    `target_player_id` INT NULL,
    `permission_node` VARCHAR(128) NULL,
    `old_value` TEXT NULL,
    `new_value` TEXT NULL,
    `reason` VARCHAR(255) NULL,
    `ip_address` VARCHAR(45) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_permission_logs_action_type` (`action_type`),
    KEY `idx_permission_logs_actor` (`actor_player_id`),
    KEY `idx_permission_logs_target_group` (`target_group_id`),
    KEY `idx_permission_logs_target_player` (`target_player_id`),
    KEY `idx_permission_logs_created_at` (`created_at`),
    CONSTRAINT `fk_permission_logs_actor` FOREIGN KEY (`actor_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_permission_logs_target_group` FOREIGN KEY (`target_group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_permission_logs_target_player` FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserir grupo padrão
INSERT INTO `permission_groups` (`group_name`, `display_name`, `description`, `priority`, `is_default`, `is_active`) VALUES
('default', 'Jogador', 'Grupo padrão para todos os jogadores', 0, TRUE, TRUE),
('helper', 'Helper', 'Ajudantes do servidor', 10, FALSE, TRUE),
('moderator', 'Moderador', 'Moderadores do servidor', 20, FALSE, TRUE),
('admin', 'Administrador', 'Administradores do servidor', 30, FALSE, TRUE),
('owner', 'Dono', 'Dono do servidor', 40, FALSE, TRUE);

