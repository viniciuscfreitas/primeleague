@echo off
echo ========================================
echo   CORREÇÃO DA STORED PROCEDURE
echo   VerifyDiscordLink
echo ========================================
echo.

echo 🔍 Verificando se o Node.js está instalado...
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Node.js não encontrado! Instale o Node.js primeiro.
    echo 💡 Download: https://nodejs.org/
    pause
    exit /b 1
)

echo ✅ Node.js encontrado!
echo.

echo 📦 Verificando dependências...
if not exist "node_modules" (
    echo 📦 Instalando dependências...
    npm install
    if %errorlevel% neq 0 (
        echo ❌ Erro ao instalar dependências!
        pause
        exit /b 1
    )
    echo ✅ Dependências instaladas!
) else (
    echo ✅ Dependências já instaladas!
)
echo.

echo 🔧 Executando correção da stored procedure...
echo 💡 Verifique se as configurações de banco estão corretas no arquivo fix-verify-procedure.js
echo.

node fix-verify-procedure.js

echo.
echo 🎯 Operação concluída!
echo 💡 Verifique o resultado acima
pause
