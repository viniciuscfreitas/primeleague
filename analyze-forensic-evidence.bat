@echo off
echo ========================================
echo   ANALISE DE EVIDENCIAS FORENSES
echo ========================================
echo.
echo Analisando logs e gerando relatorio para IA Arquiteta...
echo.

if not exist "forensic-logs" (
    echo ❌ Diretorio forensic-logs nao encontrado!
    echo    Execute primeiro: execute-forensic-tests.bat
    pause
    exit /b 1
)

echo [1/5] Analisando logs do servidor...
echo ======================================== > forensic-logs\forensic-report.md
echo   RELATORIO FORENSE - COMBATLOG >> forensic-logs\forensic-report.md
echo ======================================== >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md
echo **Data/Hora**: %date% %time% >> forensic-logs\forensic-report.md
echo **Objetivo**: Evidencias para aprovacao da IA Arquiteta >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md

echo [2/5] Buscando evidencias do Cenario 2 (Combat Log)...
echo. >> forensic-logs\forensic-report.md
echo ## CENARIO 2: COMBAT LOG - EVIDENCIAS >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md

REM Buscar PlayerQuitEvent
echo ### 1. PlayerQuitEvent Processado >> forensic-logs\forensic-report.md
findstr /C:"PlayerQuitEvent" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
if %errorlevel% neq 0 (
    echo ❌ PlayerQuitEvent nao encontrado >> forensic-logs\forensic-report.md
) else (
    echo ✅ PlayerQuitEvent encontrado >> forensic-logs\forensic-report.md
)

echo. >> forensic-logs\forensic-report.md
echo ### 2. CombatPunishmentService Logs >> forensic-logs\forensic-report.md
findstr /C:"CombatPunishmentService" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
if %errorlevel% neq 0 (
    echo ❌ CombatPunishmentService logs nao encontrados >> forensic-logs\forensic-report.md
) else (
    echo ✅ CombatPunishmentService logs encontrados >> forensic-logs\forensic-report.md
)

echo [3/5] Buscando evidencias do Cenario 3 (Reincidencia)...
echo. >> forensic-logs\forensic-report.md
echo ## CENARIO 3: REINCIDENCIA - EVIDENCIAS >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md

REM Buscar logs de reincidencia
echo ### 1. Logs de Reincidencia >> forensic-logs\forensic-report.md
findstr /C:"2ª Ocorrencia" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
findstr /C:"segunda" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
findstr /C:"reincidencia" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul

echo [4/5] Buscando evidencias do Cenario 4 (Zona Segura)...
echo. >> forensic-logs\forensic-report.md
echo ## CENARIO 4: ZONA SEGURA - EVIDENCIAS >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md

REM Buscar logs de zona segura
echo ### 1. EntityDamageByEntityEvent >> forensic-logs\forensic-report.md
findstr /C:"EntityDamageByEntityEvent" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul

echo ### 2. CombatDetectionListener - Zona Segura >> forensic-logs\forensic-report.md
findstr /C:"zona segura" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
findstr /C:"SAFE" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul
findstr /C:"ignorou" forensic-logs\server-after-test.log >> forensic-logs\forensic-report.md 2>nul

echo [5/5] Gerando resumo executivo...
echo. >> forensic-logs\forensic-report.md
echo ## RESUMO EXECUTIVO >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md
echo **Status**: Evidencias coletadas e analisadas >> forensic-logs\forensic-report.md
echo **Pronto para Producao**: Aguardando validacao da IA Arquiteta >> forensic-logs\forensic-report.md
echo. >> forensic-logs\forensic-report.md

echo ✅ Relatorio forense gerado!
echo.
echo Arquivo: forensic-logs\forensic-report.md
echo.
echo ========================================
echo   EVIDENCIAS PRONTAS PARA IA ARQUITETA
echo ========================================
echo.
echo Execute: show-forensic-report.bat
echo.
pause
