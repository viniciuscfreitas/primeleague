@echo off
echo ========================================
echo   RELATORIO FORENSE - COMBATLOG
echo ========================================
echo.

if not exist "forensic-logs\forensic-report.md" (
    echo ❌ Relatorio forense nao encontrado!
    echo    Execute primeiro: analyze-forensic-evidence.bat
    pause
    exit /b 1
)

type forensic-logs\forensic-report.md

echo.
echo ========================================
echo   FIM DO RELATORIO FORENSE
echo ========================================
echo.
echo Este relatorio contem as evidencias solicitadas
echo pela IA Arquiteta para aprovacao de producao.
echo.
pause
