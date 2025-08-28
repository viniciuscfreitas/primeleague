-- =====================================================
-- ADICIONAR TABELA DE AUTORIZAÇÃO DE IPs (P2P)
-- =====================================================

USE `primeleague`;

-- Criar tabela de autorização de IPs
CREATE TABLE `player_authorized_ips` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `player_id` INT NOT NULL,
  `ip_address` VARCHAR(45) NOT NULL,
  `description` VARCHAR(100) NULL,
  `authorized_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_used` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_player_ip` (`player_id`, `ip_address`),
  KEY `idx_player_authorized_ips_player_id` (`player_id`),
  KEY `idx_player_authorized_ips_ip_address` (`ip_address`),
  KEY `idx_player_authorized_ips_authorized_at` (`authorized_at`),
  CONSTRAINT `fk_player_authorized_ips_player`
    FOREIGN KEY (`player_id`)
    REFERENCES `player_data` (`player_id`)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificar se a tabela foi criada
SELECT 
    'TABELA CRIADA:' as info,
    COUNT(*) as total_registros
FROM information_schema.tables 
WHERE table_schema = 'primeleague' 
  AND table_name = 'player_authorized_ips';

-- Verificar estrutura da tabela
DESCRIBE `player_authorized_ips`;

-- Log de criação
SELECT 'Tabela player_authorized_ips criada com sucesso!' as status;
SELECT NOW() as created_at;
