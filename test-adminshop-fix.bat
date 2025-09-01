@echo off
echo ====================================
echo TESTE DAS CORREÇÕES DO ADMINSHOP
echo ====================================
echo.

echo [1/3] Verificando se o JAR foi atualizado...
if exist "server\plugins\primeleague-adminshop-1.0.0.jar" (
    echo ✅ JAR encontrado em server\plugins\
) else (
    echo ❌ JAR não encontrado!
    pause
    exit /b 1
)

echo.
echo [2/3] Verificando timestamp do JAR...
forfiles /m primeleague-adminshop-1.0.0.jar /c "cmd /c echo ✅ JAR modificado em @fdate @ftime" /p server\plugins\

echo.
echo [3/3] Iniciando servidor para teste (pressione Ctrl+C para parar)...
echo ⚠️  Procure no log por:
echo     - Ausência de erros "Index 1 out of bounds"
echo     - Ausência de erros "Cannot invoke doubleValue"
echo     - Mensagem: "Configuração carregada com sucesso!"
echo     - Resumo: "X categorias, Y itens"
echo.

pause
echo Iniciando servidor...
cd server
start.bat

