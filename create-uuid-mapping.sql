-- Criar tabela de mapeamento de UUIDs
CREATE TABLE IF NOT EXISTS `uuid_mapping` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `bukkit_uuid` varchar(36) NOT NULL,
  `canonical_uuid` varchar(36) NOT NULL,
  `player_id` int(11) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bukkit_uuid` (`bukkit_uuid`),
  UNIQUE KEY `uk_canonical_uuid` (`canonical_uuid`),
  KEY `idx_player_id` (`player_id`),
  CONSTRAINT `fk_uuid_mapping_player` FOREIGN KEY (`player_id`) REFERENCES `player_data` (`player_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Inserir mapeamento para o jogador vini
INSERT INTO `uuid_mapping` (`bukkit_uuid`, `canonical_uuid`, `player_id`) VALUES 
('2c048974-fd73-0b58-bd88-88bf73df16e7', 'b2d67524-ac9a-31a0-80c7-7acd45619820', 4)
ON DUPLICATE KEY UPDATE 
  `canonical_uuid` = VALUES(`canonical_uuid`),
  `updated_at` = CURRENT_TIMESTAMP;
