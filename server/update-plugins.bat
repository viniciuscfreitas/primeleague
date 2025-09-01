@echo off
echo === ATUALIZANDO PLUGINS PRIMELEAGUE ===
echo.

echo [1/4] Verificando se servidor esta parado...
if exist "server.jar" (
    echo ERRO: Servidor ainda esta rodando!
    echo Pare o servidor primeiro com: ./stop-server.bat
    pause
    exit /b 1
)

echo [2/4] Removendo JARs antigas...
cd plugins
del /f /q primeleague-*.jar
if %errorlevel% neq 0 (
    echo AVISO: Algumas JARs nao puderam ser removidas
    echo Tentando continuar...
)

echo [3/4] Copiando novas JARs...
cd ..
cd ..
copy "primeleague-core\target\primeleague-core-1.0.0.jar" "server\plugins\" /y
copy "primeleague-p2p\target\primeleague-p2p-1.0.0.jar" "server\plugins\" /y
copy "primeleague-adminshop\target\primeleague-adminshop-1.0.0.jar" "server\plugins\" /y
copy "primeleague-chat\target\primeleague-chat-1.0.0.jar" "server\plugins\" /y
copy "primeleague-clans\target\primeleague-clans-1.0.0.jar" "server\plugins\" /y
copy "primeleague-admin\target\primeleague-admin-1.0.0.jar" "server\plugins\" /y

echo [4/4] Verificando JARs atualizadas...
cd server\plugins
dir primeleague-*.jar

echo.
echo === ATUALIZACAO CONCLUIDA! ===
echo Agora voce pode iniciar o servidor novamente
pause
