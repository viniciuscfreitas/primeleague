@echo off
echo ========================================
echo TESTE DE COMANDOS ESSENTIALS
echo ========================================

echo.
echo Testando comando /sethome...
echo sethome casa > server\input.txt
timeout /t 2 /nobreak > nul

echo.
echo Testando comando /home...
echo home casa > server\input.txt
timeout /t 2 /nobreak > nul

echo.
echo Testando comando /homes...
echo homes > server\input.txt
timeout /t 2 /nobreak > nul

echo.
echo Testando comando /spawn...
echo spawn > server\input.txt
timeout /t 2 /nobreak > nul

echo.
echo Testando comando /delhome...
echo delhome casa > server\input.txt
timeout /t 2 /nobreak > nul

echo.
echo ========================================
echo TESTES CONCLUÍDOS
echo ========================================
echo Verifique os logs do servidor para resultados
