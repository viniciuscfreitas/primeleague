@echo off
echo ========================================
echo   REINICIANDO SERVIDOR PRIMELEAGUE
echo ========================================
echo.

echo 🔄 Parando servidor atual...
taskkill /F /IM java.exe >nul 2>&1
timeout /t 3 /nobreak >nul

echo ✅ Servidor parado!
echo.

echo 🚀 Iniciando servidor novamente...
cd server
start start.bat

echo.
echo ✅ Servidor reiniciado com sucesso!
echo 💡 Agora teste se o jogador vini consegue criar clãs.
echo.
pause
