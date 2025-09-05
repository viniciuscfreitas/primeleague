@echo off
echo ========================================
echo   VALIDACAO DE LOGS - COMBATLOG
echo ========================================
echo.
echo Analisando logs do servidor para validar funcionamento...
echo.

REM Verificar se o arquivo de log existe
if not exist "server\server.log" (
    echo ❌ Arquivo de log nao encontrado: server\server.log
    echo    Execute o servidor primeiro para gerar logs
    pause
    exit /b 1
)

echo [1/4] Verificando carregamento do modulo...
findstr /C:"PrimeLeague-CombatLog" server\server.log | findstr /C:"Loading" > nul
if %errorlevel% equ 0 (
    echo ✅ Modulo carregado com sucesso
) else (
    echo ❌ Modulo nao foi carregado
)

echo [2/4] Verificando inicializacao dos managers...
findstr /C:"CombatLogManager inicializado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ CombatLogManager inicializado
) else (
    echo ❌ CombatLogManager nao inicializado
)

findstr /C:"CombatZoneManager inicializado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ CombatZoneManager inicializado
) else (
    echo ❌ CombatZoneManager nao inicializado
)

findstr /C:"CombatPunishmentService inicializado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ CombatPunishmentService inicializado
) else (
    echo ❌ CombatPunishmentService nao inicializado
)

echo [3/4] Verificando registro de listeners...
findstr /C:"CombatDetectionListener registrado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ CombatDetectionListener registrado
) else (
    echo ❌ CombatDetectionListener nao registrado
)

findstr /C:"PlayerQuitListener registrado" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ PlayerQuitListener registrado
) else (
    echo ❌ PlayerQuitListener nao registrado
)

echo [4/4] Verificando habilitacao completa...
findstr /C:"PrimeLeague CombatLog habilitado com sucesso" server\server.log > nul
if %errorlevel% equ 0 (
    echo ✅ Modulo habilitado com sucesso
) else (
    echo ❌ Modulo nao foi habilitado completamente
)

echo.
echo ========================================
echo   RESUMO DA VALIDACAO
echo ========================================
echo.

REM Contar ocorrencias de sucesso
set /a success_count=0

findstr /C:"✅" server\server.log | findstr /C:"CombatLog" > nul
if %errorlevel% equ 0 set /a success_count+=1

findstr /C:"inicializado com sucesso" server\server.log | findstr /C:"CombatLog" > nul
if %errorlevel% equ 0 set /a success_count+=1

findstr /C:"registrado com sucesso" server\server.log | findstr /C:"CombatLog" > nul
if %errorlevel% equ 0 set /a success_count+=1

findstr /C:"habilitado com sucesso" server\server.log | findstr /C:"CombatLog" > nul
if %errorlevel% equ 0 set /a success_count+=1

echo Componentes validados: %success_count%/4

if %success_count% geq 3 (
    echo.
    echo 🎯 SISTEMA COMBATLOG VALIDADO COM SUCESSO!
    echo    O modulo esta funcionando corretamente no servidor.
) else (
    echo.
    echo ⚠️  SISTEMA PARCIALMENTE VALIDADO
    echo    Alguns componentes podem nao estar funcionando.
)

echo.
echo Logs analisados do arquivo: server\server.log
echo.
pause
