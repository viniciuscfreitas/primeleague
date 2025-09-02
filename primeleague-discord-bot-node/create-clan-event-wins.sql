-- ========================================
-- CRIAÇÃO DA TABELA clan_event_wins
-- ========================================
-- Esta tabela é utilizada pelo código MySqlClanDAO.registerEventWin()
-- mas estava faltando no banco de dados

-- Tabela de Vitórias de Clãs em Eventos
CREATE TABLE IF NOT EXISTS `clan_event_wins` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `clan_id` INT NOT NULL,
  `event_name` VARCHAR(64) NOT NULL,
  `win_date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_clan_event_wins_clan_id` (`clan_id`),
  KEY `idx_clan_event_wins_event_name` (`event_name`),
  CONSTRAINT `fk_clan_event_wins_clan` 
    FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Verificação da criação
SHOW CREATE TABLE `clan_event_wins`;
