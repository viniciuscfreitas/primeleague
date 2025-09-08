-- =========================================================
-- SCRIPT DE INICIALIZAÇÃO DOS GRUPOS PADRÃO
-- Prime League - Sistema de Permissões
-- =========================================================

USE `primeleague`;

-- =====================================================
-- CRIAR GRUPOS PADRÃO
-- =====================================================

-- Grupo padrão para jogadores comuns
INSERT INTO `permission_groups` (
    `group_name`, 
    `display_name`, 
    `description`, 
    `priority`, 
    `is_default`, 
    `is_active`
) VALUES (
    'default', 
    'Jogador', 
    'Grupo padrão para todos os jogadores', 
    1, 
    TRUE, 
    TRUE
);

-- Grupo para administradores
INSERT INTO `permission_groups` (
    `group_name`, 
    `display_name`, 
    `description`, 
    `priority`, 
    `is_default`, 
    `is_active`
) VALUES (
    'admin', 
    'Administrador', 
    'Grupo para administradores do servidor', 
    100, 
    FALSE, 
    TRUE
);

-- =====================================================
-- CRIAR PERMISSÕES PARA O GRUPO PADRÃO
-- =====================================================

-- Obter o ID do grupo padrão
SET @default_group_id = (SELECT group_id FROM permission_groups WHERE group_name = 'default');

-- Permissões básicas do sistema de clans para jogadores comuns
INSERT INTO `group_permissions` (
    `group_id`, 
    `permission_node`, 
    `is_granted`, 
    `created_at`
) VALUES 
    (@default_group_id, 'primeleague.clans.use', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.create', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.invite', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.kick', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.promote', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.demote', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.disband', TRUE, NOW()),
    (@default_group_id, 'primeleague.clans.chat', TRUE, NOW()),
    (@default_group_id, 'primeleague.basic.access', TRUE, NOW()),
    (@default_group_id, 'primeleague.money', TRUE, NOW()),
    (@default_group_id, 'primeleague.pay', TRUE, NOW());

-- =====================================================
-- CRIAR PERMISSÕES PARA O GRUPO ADMIN
-- =====================================================

-- Obter o ID do grupo admin
SET @admin_group_id = (SELECT group_id FROM permission_groups WHERE group_name = 'admin');

-- Permissões administrativas
INSERT INTO `group_permissions` (
    `group_id`, 
    `permission_node`, 
    `is_granted`, 
    `created_at`
) VALUES 
    (@admin_group_id, 'primeleague.clans.admin', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.eco', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.ban', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.kick', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.mute', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.warn', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.whitelist', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.vanish', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.invsee', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.inspect', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.tickets', TRUE, NOW()),
    (@admin_group_id, 'primeleague.admin.history', TRUE, NOW());

-- =====================================================
-- ATRIBUIR GRUPO PADRÃO A TODOS OS JOGADORES EXISTENTES
-- =====================================================

-- Adicionar todos os jogadores existentes ao grupo padrão
INSERT INTO `player_groups` (
    `player_id`, 
    `group_id`, 
    `is_primary`, 
    `added_at`, 
    `reason`
)
SELECT 
    p.player_id,
    @default_group_id,
    TRUE,
    NOW(),
    'Grupo padrão atribuído automaticamente'
FROM `player_data` p
WHERE NOT EXISTS (
    SELECT 1 FROM `player_groups` pg 
    WHERE pg.player_id = p.player_id 
    AND pg.group_id = @default_group_id
);

-- =====================================================
-- VERIFICAÇÃO FINAL
-- =====================================================

-- Mostrar grupos criados
SELECT 
    group_id,
    group_name,
    display_name,
    priority,
    is_default,
    is_active
FROM `permission_groups`
ORDER BY priority DESC;

-- Mostrar permissões do grupo padrão
SELECT 
    gp.group_id,
    pg.group_name,
    gp.permission_node,
    gp.is_granted
FROM `group_permissions` gp
JOIN `permission_groups` pg ON gp.group_id = pg.group_id
WHERE pg.group_name = 'default'
ORDER BY gp.permission_node;

-- Mostrar quantos jogadores estão no grupo padrão
SELECT 
    COUNT(*) as total_players_in_default_group
FROM `player_groups` pg
JOIN `permission_groups` pgrp ON pg.group_id = pgrp.group_id
WHERE pgrp.group_name = 'default';

-- =====================================================
-- MENSAGEM DE SUCESSO
-- =====================================================

SELECT '✅ Grupos padrão criados com sucesso! Jogadores agora podem criar clans.' as status;
