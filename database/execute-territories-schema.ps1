# Script PowerShell para executar o schema do módulo de territórios
# Prime League - Sistema de Territórios

Write-Host "🏰 Executando schema do módulo de territórios..." -ForegroundColor Yellow

# Configurações do banco de dados
$dbHost = "127.0.0.1"
$dbPort = "3306"
$dbName = "primeleague"
$dbUser = "root"
$dbPassword = "root"

# Caminho para o MariaDB (ajuste conforme necessário)
$mysqlPath = "C:\Program Files\MariaDB 10.11\bin\mysql.exe"

# Verificar se o MariaDB existe no caminho padrão
if (-not (Test-Path $mysqlPath)) {
    # Tentar outros caminhos comuns para MariaDB
    $possiblePaths = @(
        "C:\Program Files\MariaDB 10.10\bin\mysql.exe",
        "C:\Program Files\MariaDB 10.9\bin\mysql.exe",
        "C:\Program Files\MariaDB 10.8\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe",
        "C:\wamp64\bin\mysql\mysql8.0.21\bin\mysql.exe",
        "mysql.exe"
    )
    
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $mysqlPath = $path
            break
        }
    }
}

if (-not (Test-Path $mysqlPath)) {
    Write-Host "❌ MariaDB não encontrado. Execute o script SQL manualmente:" -ForegroundColor Red
    Write-Host "   Arquivo: territories-schema.sql" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ MariaDB encontrado em: $mysqlPath" -ForegroundColor Green

# Executar o script SQL
Write-Host "📝 Executando schema de territórios..." -ForegroundColor Yellow

$arguments = @(
    "-h", $dbHost,
    "-P", $dbPort,
    "-u", $dbUser,
    "-p$dbPassword",
    $dbName,
    "-e", "source territories-schema.sql"
)

& $mysqlPath $arguments

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Schema de territórios criado com sucesso!" -ForegroundColor Green
    Write-Host "🎉 Sistema de territórios está pronto para uso!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao executar o schema SQL. Código: $LASTEXITCODE" -ForegroundColor Red
}

Write-Host "📋 Para testar: /clan claim, /clan war [clã], /clan bank balance" -ForegroundColor Cyan