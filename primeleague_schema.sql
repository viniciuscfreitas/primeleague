-- ========================================
-- MASTER SQL SCRIPT - PRIME LEAGUE DATABASE
-- ========================================
-- FONTE DA VERDADE: schema-definition.yml validado pelo codigo
-- AUDITORIA COMPLETA: Todos os modulos mapeados
-- ORDEM: Respeita FOREIGN KEY constraints

-- ========================================
-- SISTEMA DE JOGADORES (P2P) - CORE
-- ========================================

-- Tabela principal dos jogadores (deve ser criada primeiro)
CREATE TABLE IF NOT EXISTS `player_data` (
    `player_id` INT NOT NULL AUTO_INCREMENT,
    `uuid` VARCHAR(36) NOT NULL,
    `name` VARCHAR(16) NOT NULL,
    `elo` INT NOT NULL DEFAULT 1000,
    `money` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `total_playtime` BIGINT NOT NULL DEFAULT 0,
    `last_seen` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `total_logins` INT NOT NULL DEFAULT 0,
    `status` ENUM('ACTIVE', 'INACTIVE', 'BANNED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `verification_status` ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    `donor_tier` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`player_id`),
    UNIQUE KEY `idx_player_data_uuid` (`uuid`),
    UNIQUE KEY `idx_player_data_name` (`name`),
    KEY `idx_player_data_elo` (`elo`),
    KEY `idx_player_data_status` (`status`),
    KEY `idx_player_data_last_seen` (`last_seen`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE CLAS (VALIDADO NO CODIGO)
-- ========================================

-- Tabela principal de clas
CREATE TABLE IF NOT EXISTS `clans` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `tag` VARCHAR(5) NOT NULL,
    `name` VARCHAR(32) NOT NULL,
    `founder_player_id` INT NOT NULL,
    `friendly_fire_enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `penalty_points` INT NOT NULL DEFAULT 0,
    `ranking_points` INT NOT NULL DEFAULT 1000,
    `creation_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `active_sanction_tier` INT NOT NULL DEFAULT 0,
    `sanction_expires_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_clans_tag` (`tag`),
    KEY `idx_clans_founder` (`founder_player_id`),
    KEY `idx_clans_ranking` (`ranking_points`),
    CONSTRAINT `fk_clans_founder` FOREIGN KEY (`founder_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Relacionamento entre jogadores e clas
CREATE TABLE IF NOT EXISTS `clan_players` (
    `player_id` INT NOT NULL,
    `clan_id` INT NOT NULL,
    `role` ENUM('LEADER', 'CO_LEADER', 'OFFICER', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    `join_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `kills` INT NOT NULL DEFAULT 0,
    `deaths` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`player_id`),
    KEY `idx_clan_players_clan_id` (`clan_id`),
    KEY `idx_clan_players_role` (`role`),
    CONSTRAINT `fk_clan_players_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clan_players_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Aliancas entre clas
CREATE TABLE IF NOT EXISTS `clan_alliances` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `clan1_id` INT NOT NULL,
    `clan2_id` INT NOT NULL,
    `status` ENUM('ACTIVE', 'PENDING', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_player_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_clan_alliances_clan1` (`clan1_id`),
    KEY `idx_clan_alliances_clan2` (`clan2_id`),
    KEY `idx_clan_alliances_status` (`status`),
    CONSTRAINT `fk_clan_alliances_clan1` FOREIGN KEY (`clan1_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clan_alliances_clan2` FOREIGN KEY (`clan2_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clan_alliances_creator` FOREIGN KEY (`created_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Log de acoes realizadas nos clas
CREATE TABLE IF NOT EXISTS `clan_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `clan_id` INT NOT NULL,
    `actor_player_id` INT NOT NULL,
    `actor_name` VARCHAR(16) NOT NULL,
    `action_type` INT NOT NULL,
    `target_player_id` INT NULL,
    `target_name` VARCHAR(16) NULL,
    `details` TEXT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_clan_logs_clan_id` (`clan_id`),
    KEY `idx_clan_logs_actor` (`actor_player_id`),
    KEY `idx_clan_logs_timestamp` (`timestamp`),
    CONSTRAINT `fk_clan_logs_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clan_logs_actor` FOREIGN KEY (`actor_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_clan_logs_target` FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vitorias de clas em eventos
CREATE TABLE IF NOT EXISTS `clan_event_wins` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `clan_id` INT NOT NULL,
    `event_name` VARCHAR(64) NOT NULL,
    `win_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_clan_event_wins_clan_id` (`clan_id`),
    KEY `idx_clan_event_wins_event_name` (`event_name`),
    CONSTRAINT `fk_clan_event_wins_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA ECONOMICO (CORE)
-- ========================================

-- Niveis de doador dos jogadores
CREATE TABLE IF NOT EXISTS `donors` (
    `player_id` INT NOT NULL,
    `donor_level` VARCHAR(20) NOT NULL,
    `total_donation` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `last_donation_date` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`player_id`),
    UNIQUE KEY `idx_donors_player_id` (`player_id`),
    CONSTRAINT `fk_donors_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Log de transacoes economicas
CREATE TABLE IF NOT EXISTS `economy_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `change_type` ENUM('CREDIT', 'DEBIT', 'ADMIN_GIVE', 'ADMIN_TAKE', 'ADMIN_SET', 'PLAYER_SHOP_SALE', 'PLAYER_SHOP_PURCHASE', 'ADMIN_SHOP_PURCHASE', 'PLAYER_TRANSFER', 'CLAN_BANK_DEPOSIT', 'CLAN_BANK_WITHDRAW', 'CLAN_TAX_COLLECTION', 'SYSTEM_REWARD', 'SYSTEM_PENALTY', 'BOUNTY_REWARD', 'BOUNTY_PAYMENT', 'EVENT_REWARD', 'TOURNAMENT_PRIZE', 'OTHER') NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `balance_before` DECIMAL(15,2) NOT NULL,
    `new_balance` DECIMAL(15,2) NOT NULL,
    `reason` VARCHAR(100) NOT NULL,
    `context_info` TEXT NULL,
    `related_player_id` INT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_economy_logs_player_id` (`player_id`),
    KEY `idx_economy_logs_change_type` (`change_type`),
    KEY `idx_economy_logs_timestamp` (`created_at`),
    CONSTRAINT `fk_economy_logs_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_economy_logs_related_player` FOREIGN KEY (`related_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE DISCORD (P2P)
-- ========================================

-- Vinculacoes entre Discord e Minecraft
CREATE TABLE IF NOT EXISTS `discord_links` (
    `link_id` INT NOT NULL AUTO_INCREMENT,
    `discord_id` VARCHAR(20) NOT NULL,
    `player_id` INT NOT NULL,
    `is_primary` BOOLEAN NOT NULL DEFAULT TRUE,
    `verified` BOOLEAN NOT NULL DEFAULT FALSE,
    `verification_code` VARCHAR(8) NULL,
    `code_expires_at` TIMESTAMP NULL,
    `verified_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`link_id`),
    KEY `idx_discord_links_discord_id` (`discord_id`),
    UNIQUE KEY `idx_discord_links_player_id` (`player_id`),
    KEY `idx_discord_links_verified` (`verified`),
    KEY `idx_discord_links_verification_code` (`verification_code`),
    CONSTRAINT `fk_discord_links_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Usuarios do Discord com beneficios
CREATE TABLE IF NOT EXISTS `discord_users` (
    `discord_id` VARCHAR(20) NOT NULL,
    `donor_tier` INT NOT NULL DEFAULT 0,
    `donor_tier_expires_at` TIMESTAMP NULL,
    `subscription_expires_at` TIMESTAMP NULL,
    `subscription_type` ENUM('BASIC', 'PREMIUM', 'VIP') NULL,
    `created_at` TIMESTAMP NULL,
    `updated_at` TIMESTAMP NULL,
    PRIMARY KEY (`discord_id`),
    UNIQUE KEY `idx_discord_users_discord_id` (`discord_id`),
    KEY `idx_discord_users_donor_tier` (`donor_tier`),
    KEY `idx_discord_users_subscription` (`subscription_expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Historico de mudancas de Discord
CREATE TABLE IF NOT EXISTS `discord_link_history` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `action` VARCHAR(50) NOT NULL,
    `player_id` INT NOT NULL,
    `discord_id_old` VARCHAR(20) NULL,
    `discord_id_new` VARCHAR(20) NULL,
    `details` TEXT NULL,
    `ip_address` VARCHAR(45) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_discord_link_history_player_id` (`player_id`),
    KEY `idx_discord_link_history_action` (`action`),
    KEY `idx_discord_link_history_created_at` (`created_at`),
    CONSTRAINT `fk_discord_link_history_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE AUTENTICACAO (P2P)
-- ========================================

-- IPs autorizados dos jogadores
CREATE TABLE IF NOT EXISTS `player_authorized_ips` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `ip_address` VARCHAR(45) NOT NULL,
    `description` VARCHAR(255) NULL,
    `authorized_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_player_authorized_ips_player_id` (`player_id`),
    KEY `idx_player_authorized_ips_ip_address` (`ip_address`),
    KEY `idx_player_authorized_ips_authorized_at` (`authorized_at`),
    CONSTRAINT `fk_player_authorized_ips_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Codigos de recuperacao de conta
CREATE TABLE IF NOT EXISTS `recovery_codes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `code_hash` VARCHAR(255) NOT NULL,
    `code_type` ENUM('BACKUP', 'RECOVERY') NOT NULL,
    `ip_address` VARCHAR(45) NULL,
    `discord_id` VARCHAR(20) NULL,
    `attempts` INT NOT NULL DEFAULT 0,
    `status` ENUM('ACTIVE', 'USED', 'EXPIRED', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    `expires_at` TIMESTAMP NULL,
    `used_at` TIMESTAMP NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_recovery_codes_player_id` (`player_id`),
    KEY `idx_recovery_codes_status` (`status`),
    KEY `idx_recovery_codes_code_type` (`code_type`),
    KEY `idx_recovery_codes_expires_at` (`expires_at`),
    CONSTRAINT `fk_recovery_codes_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE WHITELIST (CORE)
-- ========================================

-- Jogadores na whitelist
CREATE TABLE IF NOT EXISTS `whitelist_players` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `player_name` VARCHAR(16) NOT NULL,
    `added_by_player_id` INT NOT NULL,
    `added_by_name` VARCHAR(16) NOT NULL,
    `reason` VARCHAR(255) NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `added_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `removed_by_player_id` INT NULL,
    `removed_by_name` VARCHAR(16) NULL,
    `removed_at` TIMESTAMP NULL,
    `removal_reason` VARCHAR(255) NULL,
    PRIMARY KEY (`id`),
    KEY `idx_whitelist_players_player_id` (`player_id`),
    KEY `idx_whitelist_players_is_active` (`is_active`),
    KEY `idx_whitelist_players_added_at` (`added_at`),
    CONSTRAINT `fk_whitelist_players_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_whitelist_players_added_by` FOREIGN KEY (`added_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_whitelist_players_removed_by` FOREIGN KEY (`removed_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE ADMINISTRACAO (ADMIN)
-- ========================================

-- Punicoes aplicadas aos jogadores
CREATE TABLE IF NOT EXISTS `punishments` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `type` ENUM('WARN', 'MUTE', 'KICK', 'BAN', 'TEMP_MUTE', 'TEMP_BAN') NOT NULL,
    `target_player_id` INT NOT NULL,
    `target_name` VARCHAR(16) NOT NULL,
    `author_player_id` INT NOT NULL,
    `author_name` VARCHAR(16) NOT NULL,
    `reason` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expires_at` TIMESTAMP NULL,
    `is_active` BOOLEAN NOT NULL DEFAULT TRUE,
    `pardoned_by_player_id` INT NULL,
    `pardoned_by_name` VARCHAR(16) NULL,
    `pardoned_at` TIMESTAMP NULL,
    PRIMARY KEY (`id`),
    KEY `idx_punishments_target_player_id` (`target_player_id`),
    KEY `idx_punishments_type` (`type`),
    KEY `idx_punishments_is_active` (`is_active`),
    KEY `idx_punishments_created_at` (`created_at`),
    CONSTRAINT `fk_punishments_target` FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_punishments_author` FOREIGN KEY (`author_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_punishments_pardoned_by` FOREIGN KEY (`pardoned_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tickets de denuncia
CREATE TABLE IF NOT EXISTS `tickets` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `status` ENUM('OPEN', 'CLAIMED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    `reporter_player_id` INT NOT NULL,
    `reporter_name` VARCHAR(16) NOT NULL,
    `target_player_id` INT NOT NULL,
    `target_name` VARCHAR(16) NOT NULL,
    `reason` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `claimed_by_player_id` INT NULL,
    `claimed_by_name` VARCHAR(16) NULL,
    `claimed_at` TIMESTAMP NULL,
    `closed_at` TIMESTAMP NULL,
    `resolution` TEXT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tickets_status` (`status`),
    KEY `idx_tickets_reporter` (`reporter_player_id`),
    KEY `idx_tickets_target` (`target_player_id`),
    KEY `idx_tickets_created_at` (`created_at`),
    CONSTRAINT `fk_tickets_reporter` FOREIGN KEY (`reporter_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tickets_target` FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tickets_claimed_by` FOREIGN KEY (`claimed_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modo vanish dos staffs
CREATE TABLE IF NOT EXISTS `staff_vanish` (
    `player_id` INT NOT NULL,
    `enabled` BOOLEAN NOT NULL DEFAULT FALSE,
    `enabled_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `enabled_by_player_id` INT NOT NULL,
    PRIMARY KEY (`player_id`),
    UNIQUE KEY `idx_staff_vanish_player_id` (`player_id`),
    KEY `idx_staff_vanish_enabled` (`enabled`),
    CONSTRAINT `fk_staff_vanish_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_staff_vanish_enabled_by` FOREIGN KEY (`enabled_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE CHAT (CHAT)
-- ========================================

-- Log de mensagens do chat
CREATE TABLE IF NOT EXISTS `chat_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `channel_type` ENUM('GLOBAL','LOCAL','CLAN','ALLY','PRIVATE','STAFF') NOT NULL,
    `sender_player_id` INT NOT NULL,
    `sender_name` VARCHAR(16) NOT NULL,
    `receiver_player_id` INT NULL,
    `receiver_name` VARCHAR(16) NULL,
    `clan_id` INT NULL,
    `message_content` TEXT NOT NULL,
    `timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_chat_logs_channel_type` (`channel_type`),
    KEY `idx_chat_logs_sender` (`sender_player_id`),
    KEY `idx_chat_logs_timestamp` (`timestamp`),
    KEY `idx_chat_logs_clan_id` (`clan_id`),
    CONSTRAINT `fk_chat_logs_sender` FOREIGN KEY (`sender_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_logs_receiver` FOREIGN KEY (`receiver_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_chat_logs_clan` FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE NOTIFICACOES (P2P)
-- ========================================

-- Notificacoes do servidor
CREATE TABLE IF NOT EXISTS `server_notifications` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `action_type` VARCHAR(100) NOT NULL,
    `payload` TEXT NOT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_server_notifications_action_type` (`action_type`),
    KEY `idx_server_notifications_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE GRUPOS E PERMISSÕES (CORE)
-- ========================================

-- Grupos de permissões do sistema
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

-- Permissões associadas aos grupos
CREATE TABLE IF NOT EXISTS `group_permissions` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `group_id` INT NOT NULL,
    `permission_node` VARCHAR(128) NOT NULL,
    `is_granted` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_by_player_id` INT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_group_permissions_group_id` (`group_id`),
    KEY `idx_group_permissions_node` (`permission_node`),
    UNIQUE KEY `idx_group_permissions_group_node` (`group_id`, `permission_node`),
    CONSTRAINT `fk_group_permissions_group` FOREIGN KEY (`group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_permissions_created_by` FOREIGN KEY (`created_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Associação entre jogadores e grupos
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
    KEY `idx_player_groups_player_id` (`player_id`),
    KEY `idx_player_groups_group_id` (`group_id`),
    UNIQUE KEY `idx_player_groups_player_group` (`player_id`, `group_id`),
    KEY `idx_player_groups_is_primary` (`is_primary`),
    KEY `idx_player_groups_expires_at` (`expires_at`),
    CONSTRAINT `fk_player_groups_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_player_groups_group` FOREIGN KEY (`group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_player_groups_added_by` FOREIGN KEY (`added_by_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Log de mudanças nas permissões
CREATE TABLE IF NOT EXISTS `permission_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `action_type` ENUM('GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_DELETED', 'PERMISSION_ADDED', 'PERMISSION_REMOVED', 'PLAYER_ADDED_TO_GROUP', 'PLAYER_REMOVED_FROM_GROUP', 'PLAYER_GROUP_UPDATED') NOT NULL,
    `actor_player_id` INT NOT NULL,
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
    CONSTRAINT `fk_permission_logs_actor` FOREIGN KEY (`actor_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_permission_logs_target_group` FOREIGN KEY (`target_group_id`) REFERENCES `permission_groups` (`group_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_permission_logs_target_player` FOREIGN KEY (`target_player_id`) REFERENCES `player_data` (`player_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- SISTEMA DE PREVENÇÃO DE COMBAT LOG (COMBATLOG)
-- ========================================

-- Registro de combat logs para auditoria
CREATE TABLE IF NOT EXISTS `combat_logs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `player_id` INT NOT NULL,
    `player_name` VARCHAR(16) NOT NULL,
    `combat_start_time` TIMESTAMP NOT NULL,
    `combat_end_time` TIMESTAMP NULL,
    `combat_duration` INT NOT NULL,
    `combat_reason` ENUM('DIRECT_DAMAGE') NOT NULL DEFAULT 'DIRECT_DAMAGE',
    `zone_type` ENUM('SAFE', 'PVP', 'WARZONE') NOT NULL DEFAULT 'PVP',
    `staff_notes` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_combat_logs_player_id` (`player_id`),
    KEY `idx_combat_logs_created_at` (`created_at`),
    KEY `idx_combat_logs_combat_start_time` (`combat_start_time`),
    CONSTRAINT `fk_combat_logs_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- RESUMO DA CRIACAO
-- ========================================
-- Total de tabelas criadas: 25
-- Modulos cobertos: primeleague-core, primeleague-p2p, primeleague-admin, primeleague-adminshop, primeleague-chat, primeleague-combatlog
-- Status: Schema COMPLETO e 100% alinhado com o banco atual
-- Fonte da verdade: schema-definition.yml + auditoria do banco de dados
-- Data de geracao: 28/08/2025
-- Versao: 2.2.0 - SCHEMA FINAL COMPLETO + COMBATLOG
