@echo off
echo ========================================
echo    PRIMELEAGUE SERVER - DEBUG MODE
echo ========================================
echo.
echo Iniciando servidor em modo de depuracao...
echo Porta de debug: 1000
echo.
echo Para conectar o Cursor/IDE:
echo 1. Debug -> Attach to Process
echo 2. Host: localhost, Port: 1000
echo 3. Source: Adicione o projeto primeleague-adminshop
echo.
echo ========================================
echo.

REM Iniciar servidor Bukkit 1.5.2
java -Xms1024M -Xmx2G -jar spigot-1.5.2.jar nogui

echo.
echo Servidor parou. Pressione qualquer tecla para sair...
pause