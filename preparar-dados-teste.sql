-- =====================================================
-- PREPARAÇÃO DE DADOS DE TESTE - FASE 2
-- =====================================================

USE primeleague;

-- Limpar dados de teste anteriores
DELETE FROM recovery_codes WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');
DELETE FROM discord_users WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');
DELETE FROM player_data WHERE name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

-- =====================================================
-- CENÁRIO 1: Jogador com Assinatura Premium
-- =====================================================

-- Insere o jogador 'TestadorAlfa'
INSERT INTO player_data (name, uuid, elo, money, status) 
VALUES ('TestadorAlfa', '11111111-1111-1111-1111-111111111111', 1000, 0.00, 'ACTIVE');

-- Insere a conta Discord 'DiscordAlfa' com assinatura PREMIUM de 30 dias
INSERT INTO discord_users (discord_id, subscription_type, subscription_expires_at) 
VALUES ('discord_alfa_id', 'PREMIUM', DATE_ADD(NOW(), INTERVAL 30 DAY));

-- =====================================================
-- CENÁRIO 2: Jogador com Assinatura Basic
-- =====================================================

-- Insere o jogador 'TestadorBeta'
INSERT INTO player_data (name, uuid, elo, money, status) 
VALUES ('TestadorBeta', '22222222-2222-2222-2222-222222222222', 1000, 0.00, 'ACTIVE');

-- Insere a conta Discord 'DiscordBeta' com assinatura BASIC de 15 dias
INSERT INTO discord_users (discord_id, subscription_type, subscription_expires_at) 
VALUES ('discord_beta_id', 'BASIC', DATE_ADD(NOW(), INTERVAL 15 DAY));

-- =====================================================
-- CENÁRIO 3: Conta Discord sem Assinatura
-- =====================================================

-- Insere o jogador 'TestadorOmega'
INSERT INTO player_data (name, uuid, elo, money, status) 
VALUES ('TestadorOmega', '33333333-3333-3333-3333-333333333333', 1000, 0.00, 'ACTIVE');

-- Insere a conta Discord 'DiscordOmega' sem nenhuma assinatura
INSERT INTO discord_users (discord_id) 
VALUES ('discord_omega_id');

-- =====================================================
-- VERIFICAÇÃO DOS DADOS INSERIDOS
-- =====================================================

SELECT 'DADOS INSERIDOS:' as info;
SELECT 'Player Data:' as tabela;
SELECT player_id, name, uuid FROM player_data WHERE name IN ('TestadorAlfa', 'TestadorBeta', 'TestadorOmega');

SELECT 'Discord Users:' as tabela;
SELECT discord_id, subscription_type, subscription_expires_at FROM discord_users WHERE discord_id IN ('discord_alfa_id', 'discord_beta_id', 'discord_omega_id');

SELECT 'DADOS DE TESTE PRONTOS!' as status;
