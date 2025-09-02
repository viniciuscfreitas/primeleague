@echo off
echo ========================================
echo   CORREÇÃO DO BANCO DE DADOS PRIMELEAGUE
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

echo 📦 Instalando dependências...
npm install
if %errorlevel% neq 0 (
    echo ❌ Erro ao instalar dependências!
    pause
    exit /b 1
)

echo ✅ Dependências instaladas!
echo.

echo 🔧 Executando análise do banco de dados...
echo 💡 Ajuste as configurações de conexão nos arquivos .js se necessário
echo.

echo Escolha uma opção:
echo 1. Analisar banco de dados (apenas leitura)
echo 2. Corrigir banco de dados (modifica dados)
echo 3. Executar script SQL direto
echo 4. Sair
echo.

set /p choice="Digite sua escolha (1-4): "

if "%choice%"=="1" (
    echo.
    echo 🔍 Executando análise...
    npm run analyze
) else if "%choice%"=="2" (
    echo.
    echo ⚠️ ATENÇÃO: Esta operação MODIFICA dados do banco!
    set /p confirm="Digite 'SIM' para confirmar: "
    if /i "%confirm%"=="SIM" (
        echo 🔧 Executando correção...
        npm run fix
    ) else (
        echo ❌ Operação cancelada pelo usuário.
    )
) else if "%choice%"=="3" (
    echo.
    echo 📋 Executando script SQL...
    echo 💡 Use o HeidiSQL ou outro cliente MySQL para executar o arquivo fix-database.sql
    echo 💡 Ou execute: mysql -u root -p primeleague < fix-database.sql
) else if "%choice%"=="4" (
    echo 👋 Saindo...
    exit /b 0
) else (
    echo ❌ Opção inválida!
)

echo.
echo 🎯 Operação concluída!
pause
