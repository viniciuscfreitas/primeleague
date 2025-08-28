-- =====================================================
-- VERIFICAÇÃO DA ESTRUTURA DO BANCO
-- =====================================================

USE primeleague;

-- Verificar se as tabelas existem
SELECT 'TABELAS EXISTENTES:' as info;
SHOW TABLES;

-- Verificar estrutura da tabela player_data
SELECT 'ESTRUTURA player_data:' as info;
DESCRIBE player_data;

-- Verificar estrutura da tabela discord_users
SELECT 'ESTRUTURA discord_users:' as info;
DESCRIBE discord_users;

-- Verificar se discord_links existe
SELECT 'ESTRUTURA discord_links:' as info;
DESCRIBE discord_links;

-- Verificar dados atuais
SELECT 'DADOS ATUAIS player_data:' as info;
SELECT player_id, name, uuid FROM player_data WHERE name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

SELECT 'DADOS ATUAIS discord_users:' as info;
SELECT discord_id, subscription_type, subscription_expires_at FROM discord_users WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

-- Verificar se discord_links tem dados
SELECT 'DADOS ATUAIS discord_links:' as info;
SELECT * FROM discord_links WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');
