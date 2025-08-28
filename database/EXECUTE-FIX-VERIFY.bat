@echo off
echo =====================================================
echo CORREÇÃO DA PROCEDURE VerifyDiscordLink
echo =====================================================
echo.

echo Conectando ao MySQL...
mysql -h localhost -P 3306 -u root -proot < FIX-VERIFY-PROCEDURE-CORRECT.sql

echo.
echo =====================================================
echo CORREÇÃO APLICADA!
echo =====================================================
echo.
echo Agora o comando /verify deve funcionar corretamente.
echo.
pause
