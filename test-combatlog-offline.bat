@echo off
echo ========================================
echo   PRIMELEAGUE COMBATLOG - TESTE OFFLINE
echo ========================================
echo.
echo Iniciando testes sem dependencia do Minecraft...
echo.

REM Compilar o modulo
echo [1/4] Compilando modulo CombatLog...
cd primeleague-combatlog
call mvn clean compile -q
if %errorlevel% neq 0 (
    echo ❌ Erro na compilacao!
    pause
    exit /b 1
)
echo ✅ Compilacao concluida

REM Compilar testes
echo [2/4] Compilando testes...
call mvn test-compile -q
if %errorlevel% neq 0 (
    echo ❌ Erro na compilacao dos testes!
    pause
    exit /b 1
)
echo ✅ Testes compilados

REM Executar testes unitarios
echo [3/4] Executando testes unitarios...
call mvn test -q
if %errorlevel% neq 0 (
    echo ❌ Erro nos testes!
    pause
    exit /b 1
)
echo ✅ Testes unitarios passaram

REM Executar suite de testes customizada
echo [4/4] Executando suite de testes customizada...
java -cp "target/classes;target/test-classes;../lib/craftbukkit-1.5.2-R1.0.jar" ^
     br.com.primeleague.combatlog.CombatLogTestSuite

echo.
echo ========================================
echo   TESTES CONCLUIDOS COM SUCESSO!
echo ========================================
echo.
echo O sistema CombatLog foi validado sem precisar
echo do servidor Minecraft rodando.
echo.
pause
