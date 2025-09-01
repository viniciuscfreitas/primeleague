@echo off
echo ========================================
echo    PRIMELEAGUE ADMINSHOP - TESTES
echo ========================================
echo.
echo Executando testes do modulo...
echo.

cd /d "%~dp0"

echo 🧪 Compilando projeto...
call mvn clean compile test-compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erro na compilacao!
    pause
    exit /b 1
)

echo.
echo 🧪 Executando testes...
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
echo ✅ Todos os testes passaram com sucesso!
echo.
echo ========================================
echo    CONFIGURACAO DE DEBUG REMOTO
echo ========================================
echo.
echo Para usar a depuracao remota:
echo.
echo 1. Inicie o servidor com: server\start.bat
echo 2. No Eclipse/IDE:
echo    - Debug Configurations -> Remote Java Application
echo    - Host: localhost, Port: 1000
echo    - Source: Adicione o projeto primeleague-adminshop
echo.
echo 3. Coloque breakpoints no codigo
echo 4. Execute os comandos no servidor
echo 5. O debugger parara nos breakpoints
echo.
echo ========================================
echo.
pause
