@echo off
echo ========================================
echo    PRIMELEAGUE - TESTE DE TODOS OS MODULOS
echo ========================================
echo.
echo Testando Core, P2P e AdminShop...
echo.

cd /d "%~dp0"

echo 🧪 Testando PrimeLeague Core...
cd primeleague-core
call mvn clean compile test-compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erro na compilacao do Core!
    pause
    exit /b 1
)

echo ✅ Core compilado com sucesso!
echo.

echo 🧪 Testando PrimeLeague P2P...
cd ../primeleague-p2p
call mvn clean compile test-compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erro na compilacao do P2P!
    pause
    exit /b 1
)

echo ✅ P2P compilado com sucesso!
echo.

echo 🧪 Testando PrimeLeague AdminShop...
cd ../primeleague-adminshop
call mvn clean compile test-compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erro na compilacao do AdminShop!
    pause
    exit /b 1
)

echo ✅ AdminShop compilado com sucesso!
echo.

echo 🧪 Executando testes do AdminShop...
call mvn test

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Alguns testes falharam!
    echo.
    echo Para debug detalhado, execute:
    echo   mvn test -X
    pause
    exit /b 1
)

echo.
echo ========================================
echo    RESULTADO DOS TESTES
echo ========================================
echo.
echo ✅ PrimeLeague Core: Compilado com sucesso
echo ✅ PrimeLeague P2P: Compilado com sucesso  
echo ✅ PrimeLeague AdminShop: Compilado e testado com sucesso
echo.
echo 🎯 Todos os módulos estão funcionando!
echo.
echo ========================================
echo    CONFIGURACAO DE DEBUG
echo ========================================
echo.
echo Para usar a depuracao no Cursor:
echo.
echo 1. Debug Local (Recomendado):
echo    - Execute: primeleague-adminshop\debug-local.bat
echo    - No Cursor: F5 -> "PrimeLeague Debug - Local"
echo.
echo 2. Debug Remoto:
echo    - Execute: server\start.bat
echo    - No Cursor: Configure Remote Java Application
echo.
echo ========================================
echo.
pause
