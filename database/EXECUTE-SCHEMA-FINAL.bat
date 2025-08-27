@echo off
echo ========================================
echo SCHEMA FINAL AUTOMATIZADO PRIME LEAGUE
echo ========================================
echo.
echo Executando schema final v4.0...
echo.

REM Configurações do banco
set MYSQL_HOST=localhost
set MYSQL_PORT=3306
set MYSQL_USER=root
set MYSQL_PASS=root
set MYSQL_DB=primeleague

echo Conectando ao MySQL...
echo Host: %MYSQL_HOST%:%MYSQL_PORT%
echo Usuario: %MYSQL_USER%
echo.

REM Executar schema
mysql -h %MYSQL_HOST% -P %MYSQL_PORT% -u %MYSQL_USER% -p%MYSQL_PASS% < SCHEMA-FINAL-AUTOMATIZADO.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ SCHEMA EXECUTADO COM SUCESSO!
    echo ========================================
    echo.
    echo Caracteristicas do schema:
    echo - Discord First Registration Flow
    echo - UUID compatibility fix
    echo - SSOT (Single Source of Truth)
    echo - Shared subscriptions
    echo - Stored procedures otimizadas
    echo - Views para consultas frequentes
    echo - Triggers para manutencao automatica
    echo.
    echo O banco esta pronto para uso!
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ ERRO AO EXECUTAR SCHEMA!
    echo ========================================
    echo.
    echo Verifique:
    echo - MySQL esta rodando?
    echo - Credenciais estao corretas?
    echo - Arquivo SCHEMA-FINAL-AUTOMATIZADO.sql existe?
    echo.
)

pause
