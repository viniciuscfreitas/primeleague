@echo off
echo ====================================================
echo    CORREÇÃO DE CORRUPÇÃO - discord_links
echo ====================================================
echo.
echo Este script irá corrigir os dados corrompidos na
echo tabela discord_links que estão causando o erro
echo "Código inválido ou expirado!"
echo.
echo ATENÇÃO: Será feito backup dos dados atuais antes
echo da correção.
echo.
pause

echo.
echo Executando correção...
echo.

REM Executar o script SQL via HeidiSQL ou linha de comando
REM Se você tiver o MySQL CLI instalado, descomente a linha abaixo:
REM mysql -u root -p primeleague < FIX-DISCORD-LINKS-CORRUPTION.sql

echo.
echo ====================================================
echo    INSTRUÇÕES MANUAIS
echo ====================================================
echo.
echo 1. Abra o HeidiSQL
echo 2. Conecte ao banco 'primeleague'
echo 3. Abra o arquivo 'FIX-DISCORD-LINKS-CORRUPTION.sql'
echo 4. Execute o script completo
echo 5. Verifique se os dados foram corrigidos
echo.
echo Após a correção, teste novamente o comando /verify
echo no servidor Minecraft.
echo.
pause
