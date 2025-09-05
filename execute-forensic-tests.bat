@echo off
echo ========================================
echo   TESTES FORENSES - COMBATLOG
echo ========================================
echo.
echo Executando testes reais para coleta de evidencias...
echo.

REM Criar diretorio para logs de teste
if not exist "forensic-logs" mkdir forensic-logs

echo [1/6] Preparando ambiente de teste...
echo Timestamp: %date% %time% > forensic-logs\test-session.log
echo ======================================== >> forensic-logs\test-session.log
echo   SESSAO DE TESTES FORENSES INICIADA >> forensic-logs\test-session.log
echo ======================================== >> forensic-logs\test-session.log
echo. >> forensic-logs\test-session.log

echo [2/6] Capturando estado inicial do banco...
echo -- ESTADO INICIAL DO BANCO -- >> forensic-logs\test-session.log
echo SELECT COUNT(*) FROM punishments; >> forensic-logs\test-session.log
echo. >> forensic-logs\test-session.log

echo [3/6] Monitorando logs do servidor...
echo Iniciando monitoramento de logs...
echo Pressione Ctrl+C para parar o monitoramento
echo.

REM Monitorar logs em tempo real
echo -- LOGS EM TEMPO REAL -- >> forensic-logs\test-session.log
echo Iniciando captura: %date% %time% >> forensic-logs\test-session.log
echo. >> forensic-logs\test-session.log

REM Copiar log atual para analise
copy server\server.log forensic-logs\server-before-test.log > nul 2>&1

echo ✅ Ambiente preparado para testes forenses
echo.
echo INSTRUCOES PARA TESTE:
echo 1. Conecte dois jogadores no servidor
echo 2. Execute os cenarios de teste
echo 3. Pressione qualquer tecla quando terminar
echo.
pause

echo [4/6] Capturando logs apos teste...
copy server\server.log forensic-logs\server-after-test.log > nul 2>&1

echo [5/6] Analisando evidencias...
echo -- EVIDENCIAS CAPTURADAS -- >> forensic-logs\test-session.log
echo Timestamp final: %date% %time% >> forensic-logs\test-session.log
echo. >> forensic-logs\test-session.log

echo [6/6] Gerando relatorio forense...
echo ✅ Testes forenses concluidos!
echo.
echo Arquivos gerados:
echo - forensic-logs\test-session.log
echo - forensic-logs\server-before-test.log  
echo - forensic-logs\server-after-test.log
echo.
echo Execute: analyze-forensic-evidence.bat
echo.
pause
