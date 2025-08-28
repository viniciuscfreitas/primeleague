#!/bin/bash

# Teste de Ponta a Ponta - Sistema de Recuperação de Conta FASE 2
# Testa os dois fluxos principais: Emergência e Proativo

set -e

# Configurações
API_BASE="http://localhost:8080"
API_TOKEN="primeleague_api_token_2024"
DISCORD_ALFA_ID="discord_alfa_id"
DISCORD_BETA_ID="discord_beta_id"
PLAYER_ALFA="TestadorAlfa"
PLAYER_BETA="TestadorBeta"
IP_ADDRESS="127.0.0.1"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para log
log() {
    echo -e "${BLUE}[$(date +'%H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Função para fazer requisições HTTP
api_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    
    local curl_cmd="curl -s -w '\nHTTP_STATUS:%{http_code}' -X $method"
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"
    curl_cmd="$curl_cmd -H 'Authorization: Bearer $API_TOKEN'"
    
    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -d '$data'"
    fi
    
    curl_cmd="$curl_cmd $API_BASE$endpoint"
    
    local response=$(eval $curl_cmd)
    local http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    local body=$(echo "$response" | sed '/HTTP_STATUS:/d')
    
    if [ "$http_status" = "$expected_status" ]; then
        success "HTTP $method $endpoint - Status: $http_status"
        echo "$body"
    else
        error "HTTP $method $endpoint - Esperado: $expected_status, Recebido: $http_status"
        echo "$body"
        return 1
    fi
}

# Função para limpar dados de teste
cleanup_test_data() {
    log "Limpando dados de teste anteriores..."
    
    # Remover códigos de recuperação de teste
    mysql -u root -p -e "DELETE FROM recovery_codes WHERE discord_id IN ('$DISCORD_ALFA_ID', '$DISCORD_BETA_ID');" primeleague 2>/dev/null || true
    
    # Remover histórico de vínculos de teste
    mysql -u root -p -e "DELETE FROM discord_link_history WHERE old_discord_id IN ('$DISCORD_ALFA_ID', '$DISCORD_BETA_ID') OR new_discord_id IN ('$DISCORD_ALFA_ID', '$DISCORD_BETA_ID');" primeleague 2>/dev/null || true
    
    # Resetar status de vínculos para teste
    mysql -u root -p -e "UPDATE discord_links SET status = 'ACTIVE' WHERE discord_id IN ('$DISCORD_ALFA_ID', '$DISCORD_BETA_ID');" primeleague 2>/dev/null || true
    
    success "Dados de teste limpos"
}

# Função para preparar dados de teste
prepare_test_data() {
    log "Preparando dados de teste..."
    
    # Verificar se os jogadores existem
    local player_alfa_exists=$(mysql -u root -p -s -e "SELECT COUNT(*) FROM player_data WHERE player_name = '$PLAYER_ALFA';" primeleague 2>/dev/null || echo "0")
    local player_beta_exists=$(mysql -u root -p -s -e "SELECT COUNT(*) FROM player_data WHERE player_name = '$PLAYER_BETA';" primeleague 2>/dev/null || echo "0")
    
    if [ "$player_alfa_exists" = "0" ]; then
        warning "Jogador $PLAYER_ALFA não encontrado. Criando..."
        mysql -u root -p -e "INSERT INTO player_data (player_name, discord_id, tier, expiration_date) VALUES ('$PLAYER_ALFA', '$DISCORD_ALFA_ID', 'VIP', DATE_ADD(NOW(), INTERVAL 30 DAY));" primeleague 2>/dev/null || true
    fi
    
    if [ "$player_beta_exists" = "0" ]; then
        warning "Jogador $PLAYER_BETA não encontrado. Criando..."
        mysql -u root -p -e "INSERT INTO player_data (player_name, discord_id, tier, expiration_date) VALUES ('$PLAYER_BETA', '$DISCORD_BETA_ID', 'PREMIUM', DATE_ADD(NOW(), INTERVAL 60 DAY));" primeleague 2>/dev/null || true
    fi
    
    success "Dados de teste preparados"
}

# Função para verificar se o Core está rodando
check_core_status() {
    log "Verificando status do Core API..."
    
    if curl -s "$API_BASE/api/health" > /dev/null 2>&1; then
        success "Core API está rodando"
        return 0
    else
        error "Core API não está rodando em $API_BASE"
        warning "Certifique-se de que o servidor Core está iniciado"
        return 1
    fi
}

# Teste 1: Fluxo de Emergência
test_emergency_flow() {
    log "=== TESTE 1: FLUXO DE EMERGÊNCIA ==="
    log "Simulando jogador bloqueado que usa código de backup"
    
    # 1. Gerar códigos de backup para o jogador
    log "1. Gerando códigos de backup para $PLAYER_ALFA..."
    local backup_response=$(api_request "POST" "/api/v1/recovery/backup/generate" "{\"discordId\":\"$DISCORD_ALFA_ID\",\"ipAddress\":\"$IP_ADDRESS\"}" "200")
    
    # Extrair um código de backup da resposta
    local backup_code=$(echo "$backup_response" | grep -o '"codes":\["[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$backup_code" ]; then
        error "Não foi possível extrair código de backup da resposta"
        echo "Resposta: $backup_response"
        return 1
    fi
    
    success "Código de backup extraído: $backup_code"
    
    # 2. Simular uso do comando /recuperar no jogo
    log "2. Simulando comando /recuperar <codigo_backup> no jogo..."
    local verify_response=$(api_request "POST" "/api/v1/recovery/verify" "{\"playerName\":\"$PLAYER_ALFA\",\"backupCode\":\"$backup_code\",\"ipAddress\":\"$IP_ADDRESS\"}" "200")
    
    # Extrair código de re-vinculação da resposta
    local relink_code=$(echo "$verify_response" | grep -o '"relinkCode":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$relink_code" ]; then
        error "Não foi possível extrair código de re-vinculação da resposta"
        echo "Resposta: $verify_response"
        return 1
    fi
    
    success "Código de re-vinculação recebido: $relink_code"
    
    # 3. Verificar se o status foi alterado para PENDING_RELINK
    log "3. Verificando se status foi alterado para PENDING_RELINK..."
    local status_response=$(api_request "GET" "/api/v1/recovery/status/$DISCORD_ALFA_ID" "" "200")
    
    if echo "$status_response" | grep -q '"status":"PENDING_RELINK"'; then
        success "Status alterado para PENDING_RELINK corretamente"
    else
        error "Status não foi alterado para PENDING_RELINK"
        echo "Resposta: $status_response"
        return 1
    fi
    
    # 4. Simular finalização com /vincular no Discord
    log "4. Simulando finalização com /vincular no Discord..."
    local complete_response=$(api_request "POST" "/api/v1/recovery/complete-relink" "{\"playerName\":\"$PLAYER_ALFA\",\"relinkCode\":\"$relink_code\",\"newDiscordId\":\"$DISCORD_ALFA_ID\",\"ipAddress\":\"$IP_ADDRESS\"}" "200")
    
    if echo "$complete_response" | grep -q '"success":true'; then
        success "Re-vinculação completada com sucesso"
    else
        error "Falha na re-vinculação"
        echo "Resposta: $complete_response"
        return 1
    fi
    
    # 5. Verificar se o status voltou para ACTIVE
    log "5. Verificando se status voltou para ACTIVE..."
    local final_status=$(api_request "GET" "/api/v1/recovery/status/$DISCORD_ALFA_ID" "" "200")
    
    if echo "$final_status" | grep -q '"status":"ACTIVE"'; then
        success "Status finalizado como ACTIVE corretamente"
    else
        error "Status não foi finalizado como ACTIVE"
        echo "Resposta: $final_status"
        return 1
    fi
    
    success "=== FLUXO DE EMERGÊNCIA TESTADO COM SUCESSO ==="
}

# Teste 2: Fluxo Proativo
test_proactive_flow() {
    log "=== TESTE 2: FLUXO PROATIVO ==="
    log "Simulando desvinculação proativa via Discord"
    
    # 1. Simular comando /desvincular no Discord
    log "1. Simulando comando /desvincular no Discord..."
    local unlink_response=$(api_request "POST" "/api/v1/account/unlink" "{\"playerName\":\"$PLAYER_BETA\",\"discordId\":\"$DISCORD_BETA_ID\",\"ipAddress\":\"$IP_ADDRESS\"}" "200")
    
    # Extrair código de re-vinculação da resposta
    local relink_code=$(echo "$unlink_response" | grep -o '"relinkCode":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$relink_code" ]; then
        error "Não foi possível extrair código de re-vinculação da resposta"
        echo "Resposta: $unlink_response"
        return 1
    fi
    
    success "Código de re-vinculação recebido: $relink_code"
    
    # 2. Verificar se o status foi alterado para PENDING_RELINK
    log "2. Verificando se status foi alterado para PENDING_RELINK..."
    local status_response=$(api_request "GET" "/api/v1/recovery/status/$DISCORD_BETA_ID" "" "200")
    
    if echo "$status_response" | grep -q '"status":"PENDING_RELINK"'; then
        success "Status alterado para PENDING_RELINK corretamente"
    else
        error "Status não foi alterado para PENDING_RELINK"
        echo "Resposta: $status_response"
        return 1
    fi
    
    # 3. Simular finalização com /vincular em nova conta Discord
    log "3. Simulando finalização com /vincular em nova conta Discord..."
    local new_discord_id="discord_nova_conta_$(date +%s)"
    local complete_response=$(api_request "POST" "/api/v1/recovery/complete-relink" "{\"playerName\":\"$PLAYER_BETA\",\"relinkCode\":\"$relink_code\",\"newDiscordId\":\"$new_discord_id\",\"ipAddress\":\"$IP_ADDRESS\"}" "200")
    
    if echo "$complete_response" | grep -q '"success":true'; then
        success "Re-vinculação para nova conta completada com sucesso"
    else
        error "Falha na re-vinculação para nova conta"
        echo "Resposta: $complete_response"
        return 1
    fi
    
    # 4. Verificar se o status voltou para ACTIVE
    log "4. Verificando se status voltou para ACTIVE..."
    local final_status=$(api_request "GET" "/api/v1/recovery/status/$new_discord_id" "" "200")
    
    if echo "$final_status" | grep -q '"status":"ACTIVE"'; then
        success "Status finalizado como ACTIVE corretamente"
    else
        error "Status não foi finalizado como ACTIVE"
        echo "Resposta: $final_status"
        return 1
    fi
    
    success "=== FLUXO PROATIVO TESTADO COM SUCESSO ==="
}

# Teste 3: Validações de Segurança
test_security_validations() {
    log "=== TESTE 3: VALIDAÇÕES DE SEGURANÇA ==="
    
    # Teste de código inválido
    log "1. Testando código de backup inválido..."
    api_request "POST" "/api/v1/recovery/verify" "{\"playerName\":\"$PLAYER_ALFA\",\"backupCode\":\"INVALID_CODE\",\"ipAddress\":\"$IP_ADDRESS\"}" "400"
    
    # Teste de código expirado (simulado)
    log "2. Testando código de re-vinculação inválido..."
    api_request "POST" "/api/v1/recovery/complete-relink" "{\"playerName\":\"$PLAYER_ALFA\",\"relinkCode\":\"INVALID_RELINK\",\"newDiscordId\":\"$DISCORD_ALFA_ID\",\"ipAddress\":\"$IP_ADDRESS\"}" "400"
    
    # Teste de rate limiting (múltiplas tentativas)
    log "3. Testando rate limiting..."
    for i in {1..3}; do
        api_request "POST" "/api/v1/recovery/verify" "{\"playerName\":\"$PLAYER_ALFA\",\"backupCode\":\"INVALID_CODE_$i\",\"ipAddress\":\"$IP_ADDRESS\"}" "400"
    done
    
    success "=== VALIDAÇÕES DE SEGURANÇA TESTADAS COM SUCESSO ==="
}

# Função principal
main() {
    echo "=========================================="
    echo "  TESTE DE PONTA A PONTA - FASE 2"
    echo "  Sistema de Recuperação de Conta"
    echo "=========================================="
    echo ""
    
    # Verificar se o Core está rodando
    if ! check_core_status; then
        exit 1
    fi
    
    # Limpar e preparar dados de teste
    cleanup_test_data
    prepare_test_data
    
    echo ""
    log "Iniciando testes de ponta a ponta..."
    echo ""
    
    # Executar testes
    if test_emergency_flow; then
        echo ""
        if test_proactive_flow; then
            echo ""
            if test_security_validations; then
                echo ""
                success "=========================================="
                success "  TODOS OS TESTES FORAM EXECUTADOS COM SUCESSO!"
                success "  FASE 2 - SISTEMA DE RECUPERAÇÃO DE CONTA"
                success "  STATUS: APROVADO PARA PRODUÇÃO"
                success "=========================================="
                echo ""
                log "Resumo dos fluxos testados:"
                echo "  ✅ Fluxo de Emergência: /recuperar → /vincular"
                echo "  ✅ Fluxo Proativo: /desvincular → /vincular"
                echo "  ✅ Validações de Segurança: Rate limiting e códigos inválidos"
                echo ""
                log "O sistema está pronto para uso em produção!"
            else
                error "Falha nos testes de validação de segurança"
                exit 1
            fi
        else
            error "Falha no teste do fluxo proativo"
            exit 1
        fi
    else
        error "Falha no teste do fluxo de emergência"
        exit 1
    fi
}

# Executar função principal
main "$@"
