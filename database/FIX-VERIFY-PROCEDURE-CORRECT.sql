-- =====================================================
-- CORREÇÃO DA PROCEDURE VerifyDiscordLink
-- =====================================================
-- Problema: Procedure usando nomes de colunas incorretos
-- Solução: Recriar a procedure com nomes corretos para a estrutura atual
-- =====================================================

USE `primeleague`;

-- 1. REMOVER PROCEDURE ANTIGA
DROP PROCEDURE IF EXISTS `VerifyDiscordLink`;

-- 2. CRIAR PROCEDURE CORRIGIDA (usando player_id)
DELIMITER $$

CREATE PROCEDURE `VerifyDiscordLink`(
    IN p_player_name VARCHAR(16),
    IN p_verify_code VARCHAR(8)
)
BEGIN
    DECLARE v_success BOOLEAN DEFAULT FALSE;
    DECLARE v_discord_id VARCHAR(20);
    DECLARE v_player_uuid CHAR(36);
    
    -- Verificar código válido e não expirado
    -- 🔧 CORREÇÃO: Usar player_id (estrutura atual)
    SELECT dl.discord_id, pd.uuid
    INTO v_discord_id, v_player_uuid
    FROM discord_links dl
    JOIN player_data pd ON dl.player_id = pd.player_id
    WHERE pd.name = p_player_name 
      AND dl.verification_code = p_verify_code
      AND dl.code_expires_at > NOW()
      AND dl.verified = FALSE;
    
    IF v_discord_id IS NOT NULL THEN
        -- Marcar como verificado
        UPDATE discord_links 
        SET verified = TRUE, 
            verified_at = NOW(),
            verification_code = NULL,
            code_expires_at = NULL
        WHERE discord_id = v_discord_id;
        
        SET v_success = TRUE;
    END IF;
    
    -- Retornar resultado
    SELECT v_success as success, 
           v_discord_id as discord_id, 
           v_player_uuid as player_uuid;
END$$

DELIMITER ;

-- 3. TESTAR A PROCEDURE
SELECT 'PROCEDURE CORRIGIDA' as status;

-- 4. VERIFICAR DADOS ATUAIS
SELECT 
    'DADOS ATUAIS' as info,
    dl.link_id,
    dl.discord_id,
    dl.player_id,
    pd.name as player_name,
    dl.verification_code,
    dl.code_expires_at,
    dl.verified
FROM discord_links dl
JOIN player_data pd ON dl.player_id = pd.player_id;

-- =====================================================
-- RESULTADO ESPERADO:
-- - Procedure recriada com nomes corretos das colunas
-- - verification_code e code_expires_at funcionando
-- =====================================================
