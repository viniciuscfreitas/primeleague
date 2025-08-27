@echo off
echo =====================================================
echo APLICANDO SCHEMA OTIMIZADO PRIME LEAGUE
echo =====================================================

echo.
echo 1. Abra o HeidiSQL
echo 2. Conecte ao banco de dados 'primeleague'
echo 3. Abra o arquivo 'APPLY-OPTIMIZED-SCHEMA.sql'
echo 4. Execute o script completo
echo 5. Verifique se todas as tabelas foram criadas
echo.

echo Script localizado em: database/APPLY-OPTIMIZED-SCHEMA.sql
echo.
echo IMPORTANTE: Este script irá:
echo - Criar a tabela discord_users se não existir
echo - Migrar dados existentes
echo - Configurar foreign keys
echo - Verificar a integridade dos dados
echo.
pause
