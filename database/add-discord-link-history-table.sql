-- =====================================================
-- TABELA DE AUDITORIA DE VÍNCULOS DISCORD
-- Sistema de auditoria para transferências de assinaturas (FASE 2)
-- =====================================================

CREATE TABLE `discord_link_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `action` ENUM('LINK', 'UNLINK', 'TRANSFER_SUB', 'RECOVERY_LINK') NOT NULL,
  `player_id` INT NOT NULL,
  `discord_id_old` VARCHAR(20) NULL,
  `discord_id_new` VARCHAR(20) NULL,
  `details` JSON NULL,
  `ip_address` VARCHAR(45) NULL,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_discord_link_history_player` (`player_id`),
  KEY `idx_discord_link_history_action` (`action`),
  KEY `idx_discord_link_history_created` (`created_at`),
  CONSTRAINT `fk_discord_link_history_player` 
    FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- COMENTÁRIOS SOBRE A TABELA
-- =====================================================

-- action: Tipo de ação realizada
--   - LINK: Vínculo inicial de uma conta Discord
--   - UNLINK: Desvinculação de uma conta Discord
--   - TRANSFER_SUB: Transferência de assinatura entre Discord IDs
--   - RECOVERY_LINK: Re-vinculação após recuperação de conta

-- player_id: ID do jogador Minecraft afetado
-- discord_id_old: Discord ID anterior (NULL para ações de LINK)
-- discord_id_new: Discord ID novo (NULL para ações de UNLINK)
-- details: JSON com detalhes da operação (tipos de assinatura, estratégia usada, etc.)
-- ip_address: IP de origem da operação para auditoria de segurança

-- =====================================================
-- VERIFICAÇÃO DA CRIAÇÃO
-- =====================================================

-- Verificar se a tabela foi criada corretamente
DESCRIBE `discord_link_history`;

-- Verificar índices criados
SHOW INDEX FROM `discord_link_history`;

-- =====================================================
-- FIM DO SCRIPT
-- =====================================================
