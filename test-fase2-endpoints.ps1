# =====================================================
# SCRIPT DE TESTE - FASE 2: SISTEMA DE RECUPERAÇÃO (PowerShell)
# =====================================================

Write-Host "🧪 TESTANDO ENDPOINTS DA FASE 2" -ForegroundColor Yellow
Write-Host "==================================" -ForegroundColor Yellow

# Configurações
$API_URL = "http://localhost:8080"
$TOKEN = "primeleague_api_token_2024"
$PLAYER_NAME = "vini"
$DISCORD_ID = "1234567890123456789"
$NEW_DISCORD_ID = "9876543210987654321"
$IP_ADDRESS = "127.0.0.1"

# Função para testar endpoint
function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Endpoint,
        [string]$Data,
        [string]$Description
    )
    
    Write-Host "`n🔍 Testando: $Description" -ForegroundColor Yellow
    Write-Host "Endpoint: $Method $Endpoint" -ForegroundColor Gray
    
    $headers = @{
        "Content-Type" = "application/json"
        "Authorization" = "Bearer $TOKEN"
    }
    
    try {
        if ($Data) {
            Write-Host "Payload: $Data" -ForegroundColor Gray
            $response = Invoke-RestMethod -Uri "$API_URL$Endpoint" -Method $Method -Headers $headers -Body $Data -ErrorAction Stop
        } else {
            $response = Invoke-RestMethod -Uri "$API_URL$Endpoint" -Method $Method -Headers $headers -ErrorAction Stop
        }
        
        Write-Host "✅ Sucesso (HTTP 200)" -ForegroundColor Green
        Write-Host "Resposta: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Gray
        
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "❌ Falha (HTTP $statusCode)" -ForegroundColor Red
        Write-Host "Erro: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# =====================================================
# TESTE 1: HEALTH CHECK
# =====================================================
Write-Host "`n📋 TESTE 1: Verificação de Saúde da API" -ForegroundColor Yellow
Test-Endpoint -Method "GET" -Endpoint "/api/health" -Description "Health Check"

# =====================================================
# TESTE 2: GERAÇÃO DE CÓDIGOS DE BACKUP
# =====================================================
Write-Host "`n📋 TESTE 2: Geração de Códigos de Backup" -ForegroundColor Yellow
$generateData = @{
    discordId = $DISCORD_ID
    ipAddress = $IP_ADDRESS
} | ConvertTo-Json
Test-Endpoint -Method "POST" -Endpoint "/api/v1/recovery/backup/generate" -Data $generateData -Description "Geração de Códigos de Backup"

# =====================================================
# TESTE 3: STATUS DOS CÓDIGOS
# =====================================================
Write-Host "`n📋 TESTE 3: Status dos Códigos de Backup" -ForegroundColor Yellow
Test-Endpoint -Method "GET" -Endpoint "/api/v1/recovery/status/$DISCORD_ID" -Description "Status dos Códigos de Backup"

# =====================================================
# TESTE 4: AUDITORIA DE CÓDIGOS
# =====================================================
Write-Host "`n📋 TESTE 4: Auditoria de Códigos" -ForegroundColor Yellow
Test-Endpoint -Method "GET" -Endpoint "/api/v1/recovery/audit/$DISCORD_ID" -Description "Auditoria de Códigos de Backup"

# =====================================================
# TESTE 5: TRANSFERÊNCIA DE ASSINATURA
# =====================================================
Write-Host "`n📋 TESTE 5: Transferência de Assinatura" -ForegroundColor Yellow
$transferData = @{
    playerName = $PLAYER_NAME
    newDiscordId = $NEW_DISCORD_ID
} | ConvertTo-Json
Test-Endpoint -Method "POST" -Endpoint "/api/v1/discord/transfer" -Data $transferData -Description "Transferência de Assinatura"

# =====================================================
# TESTE 6: VERIFICAÇÃO DE CÓDIGO (SIMULAÇÃO)
# =====================================================
Write-Host "`n📋 TESTE 6: Verificação de Código de Backup" -ForegroundColor Yellow
$verifyData = @{
    playerName = $PLAYER_NAME
    backupCode = "TEST123"
    ipAddress = $IP_ADDRESS
} | ConvertTo-Json
Test-Endpoint -Method "POST" -Endpoint "/api/v1/recovery/verify" -Data $verifyData -Description "Verificação de Código de Backup"

Write-Host "`n🎉 Testes concluídos!" -ForegroundColor Green
Write-Host "`n📝 Notas:" -ForegroundColor Yellow
Write-Host "- Se o servidor não estiver rodando, todos os testes falharão" -ForegroundColor Gray
Write-Host "- Verifique se o plugin foi carregado corretamente no console do servidor" -ForegroundColor Gray
Write-Host "- Alguns testes podem falhar se os dados de teste não existirem no banco" -ForegroundColor Gray
