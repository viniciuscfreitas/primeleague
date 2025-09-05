@echo off
echo ========================================
echo   CONSULTA BANCO DE DADOS - EVIDENCIAS
echo ========================================
echo.
echo Executando consultas para validar punicoes no banco...
echo.

REM Verificar se o MySQL esta rodando
echo [1/4] Verificando conexao com banco...
mysql -u root -p -e "SELECT 1;" 2>nul
if %errorlevel% neq 0 (
    echo ❌ Nao foi possivel conectar ao MySQL
    echo    Verifique se o servico esta rodando
    pause
    exit /b 1
)

echo ✅ Conexao com banco estabelecida

echo [2/4] Consultando punicoes de Combat Log...
echo ======================================== > forensic-logs\database-evidence.sql
echo -- EVIDENCIAS DO BANCO DE DADOS >> forensic-logs\database-evidence.sql
echo ======================================== >> forensic-logs\database-evidence.sql
echo. >> forensic-logs\database-evidence.sql

echo -- 1. Todas as punicoes de Combat Log >> forensic-logs\database-evidence.sql
echo SELECT * FROM punishments >> forensic-logs\database-evidence.sql
echo WHERE reason LIKE '%%Combat Log%%' >> forensic-logs\database-evidence.sql
echo ORDER BY created_at DESC; >> forensic-logs\database-evidence.sql
echo. >> forensic-logs\database-evidence.sql

echo -- 2. Punições mais recentes >> forensic-logs\database-evidence.sql
echo SELECT * FROM punishments >> forensic-logs\database-evidence.sql
echo ORDER BY created_at DESC LIMIT 5; >> forensic-logs\database-evidence.sql
echo. >> forensic-logs\database-evidence.sql

echo [3/4] Executando consultas...
mysql -u root -p < forensic-logs\database-evidence.sql > forensic-logs\database-results.txt 2>&1

echo [4/4] Analisando resultados...
if exist "forensic-logs\database-results.txt" (
    echo ✅ Resultados salvos em: forensic-logs\database-results.txt
    echo.
    echo Conteudo dos resultados:
    echo ========================================
    type forensic-logs\database-results.txt
    echo ========================================
) else (
    echo ❌ Erro ao executar consultas
)

echo.
echo ========================================
echo   EVIDENCIAS DO BANCO COLETADAS
echo ========================================
echo.
echo Arquivos gerados:
echo - forensic-logs\database-evidence.sql
echo - forensic-logs\database-results.txt
echo.
pause
