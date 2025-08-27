@echo off
echo ====================================================
echo MIGRAÇÃO PARA SCHEMA OTIMIZADO
echo ====================================================
echo.
echo INSTRUÇÕES:
echo 1. Abra o HeidiSQL
echo 2. Conecte ao banco 'primeleague'
echo 3. Abra o arquivo 'MIGRACAO-SCHEMA-OTIMIZADO.sql'
echo 4. Execute o script (F9)
echo 5. Verifique se a migração foi bem-sucedida
echo.
echo ARQUIVO: MIGRACAO-SCHEMA-OTIMIZADO.sql
echo.
echo ⚠️  ATENÇÃO: Esta migração irá:
echo    - Consolidar dados de 3 tabelas em 1
echo    - Remover campos redundantes
echo    - Otimizar performance
echo    - Seguir princípio SSOT
echo.
pause
