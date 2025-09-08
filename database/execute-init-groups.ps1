# Script PowerShell para executar a inicialização dos grupos padrão
# Prime League - Sistema de Permissões

Write-Host "🔧 Inicializando grupos padrão do sistema de permissões..." -ForegroundColor Yellow

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
        "mysql.exe"  # Se estiver no PATH
    )
    
    foreach ($path in $possiblePaths) {
        if (Test-Path $path) {
            $mysqlPath = $path
            break
        }
    }
}

if (-not (Test-Path $mysqlPath)) {
    Write-Host "❌ MariaDB não encontrado. Por favor, execute o script SQL manualmente:" -ForegroundColor Red
    Write-Host "   Arquivo: init-default-groups.sql" -ForegroundColor Yellow
    Write-Host "   Conteúdo:" -ForegroundColor Yellow
    Get-Content "init-default-groups.sql" | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
    exit 1
}

Write-Host "✅ MariaDB encontrado em: $mysqlPath" -ForegroundColor Green

# Executar o script SQL
Write-Host "📝 Executando script de inicialização..." -ForegroundColor Yellow

$arguments = @(
    "-h", $dbHost,
    "-P", $dbPort,
    "-u", $dbUser,
    "-p$dbPassword",
    $dbName,
    "-e", "source init-default-groups.sql"
)

& $mysqlPath $arguments

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Grupos padrão criados com sucesso!" -ForegroundColor Green
    Write-Host "🎉 Jogadores agora podem criar clans!" -ForegroundColor Green
} else {
    Write-Host "❌ Erro ao executar o script SQL. Código de saída: $LASTEXITCODE" -ForegroundColor Red
    Write-Host "📋 Execute o script SQL manualmente no seu cliente MariaDB:" -ForegroundColor Yellow
    Write-Host "   Arquivo: init-default-groups.sql" -ForegroundColor Yellow
}

Write-Host "`n📋 Para verificar se funcionou, execute no servidor:" -ForegroundColor Cyan
Write-Host "   /clan create Teste TesteClan" -ForegroundColor White