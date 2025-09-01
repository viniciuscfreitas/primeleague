-- Script para corrigir a stored procedure VerifyDiscordLink
-- Execute este script no HeidiSQL ou MySQL Workbench

DROP PROCEDURE IF EXISTS VerifyDiscordLink;

DELIMITER //

CREATE PROCEDURE `VerifyDiscordLink`(
    IN p_player_name VARCHAR(16),
    IN p_verification_code VARCHAR(8)
)
BEGIN
    DECLARE v_player_uuid CHAR(36);
    DECLARE v_discord_id VARCHAR(20);
    DECLARE v_success BOOLEAN DEFAULT FALSE;
    DECLARE v_message VARCHAR(255);
    DECLARE v_player_id INT;
    
    -- Obter UUID e player_id do player
    SELECT uuid, player_id INTO v_player_uuid, v_player_id
    FROM player_data 
    WHERE name = p_player_name;
    
    IF v_player_uuid IS NOT NULL THEN
        -- Verificar código de verificação (CORRIGIDO: usar player_id)
        SELECT dl.discord_id INTO v_discord_id
        FROM discord_links dl
        JOIN player_data pd ON dl.player_id = pd.player_id
        WHERE pd.name = p_player_name 
        AND dl.verification_code = p_verification_code
        AND dl.code_expires_at > NOW()
        AND dl.verified = 0;
        
        IF v_discord_id IS NOT NULL THEN
            -- Marcar como verificado
            UPDATE discord_links 
            SET verified = 1, 
                verified_at = NOW(),
                verification_code = NULL,
                code_expires_at = NULL
            WHERE discord_id = v_discord_id;
            
            SET v_success = TRUE;
            SET v_message = 'Verificação realizada com sucesso!';
        ELSE
            SET v_message = 'Código inválido ou expirado!';
        END IF;
    ELSE
        SET v_message = 'Jogador não encontrado!';
    END IF;
    
    -- Retornar resultado
    SELECT v_success as success, v_message as message, v_discord_id as discord_id, v_player_uuid as player_uuid, v_player_id as player_id;
END //

DELIMITER ;
