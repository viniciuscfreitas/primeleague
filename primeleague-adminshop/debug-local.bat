@echo off
echo ========================================
echo    PRIMELEAGUE SERVER - DEBUG LOCAL
echo ========================================
echo.
echo Iniciando servidor em modo de depuracao LOCAL...
echo.
echo Para usar no Cursor:
echo 1. Pressione F5 ou Ctrl+Shift+D
echo 2. Selecione "PrimeLeague Debug - Local"
echo 3. O servidor iniciara e parara no breakpoint
echo.
echo ========================================
echo.

REM Compilar o projeto primeiro
echo 🧪 Compilando projeto...
call mvn clean compile

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erro na compilacao!
    pause
    exit /b 1
)

echo ✅ Compilacao concluida!
echo.
echo 🚀 Iniciando servidor em modo debug...
echo.

REM Iniciar servidor com debug local
java -Xms1024M -Xmx2G -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=1000,suspend=y -cp "target/classes;../lib/*" br.com.primeleague.adminshop.AdminShopPlugin

echo.
echo Servidor parou. Pressione qualquer tecla para sair...
pause
