@echo off
echo ========================================
echo   TESTE DE DEPENDENCIA - COMBATLOG
echo ========================================
echo.
echo Reiniciando servidor para testar correcao de dependencia...
echo.

echo [1/4] Parando servidor...
REM Parar o servidor (assumindo que esta rodando)
echo stop > server\input.txt
timeout /t 5 /nobreak > nul

echo [2/4] Aguardando parada completa...
timeout /t 10 /nobreak > nul

echo [3/4] Iniciando servidor...
cd server
start /B java -Xmx2G -Xms1G -jar craftbukkit-1.5.2-R1.0.jar nogui
cd ..

echo [4/4] Aguardando carregamento...
timeout /t 30 /nobreak > nul

echo.
echo ✅ Servidor reiniciado!
echo.
echo Verificando se a dependencia foi resolvida...
echo.

REM Verificar se o aviso de dependencia ainda aparece
findstr /C:"PrimeLeague-Admin nao encontrado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ❌ PROBLEMA PERSISTE: Dependencia ainda nao resolvida
    echo.
    echo Verificando logs mais recentes:
    echo ========================================
    findstr /C:"PrimeLeague-CombatLog" server\server.log | tail -5
    echo ========================================
) else (
    echo ✅ SUCESSO: Dependencia resolvida!
    echo.
    echo Verificando logs de sucesso:
    echo ========================================
    findstr /C:"PrimeLeague-CombatLog" server\server.log | tail -5
    echo ========================================
)

echo.
echo ========================================
echo   TESTE DE DEPENDENCIA CONCLUIDO
echo ========================================
echo.
pause
