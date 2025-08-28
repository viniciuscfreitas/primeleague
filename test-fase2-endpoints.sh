#!/bin/bash

# =====================================================
# SCRIPT DE TESTE - FASE 2: SISTEMA DE RECUPERAÇÃO
# =====================================================

echo "🧪 TESTANDO ENDPOINTS DA FASE 2"
echo "=================================="

# Configurações
API_URL="http://localhost:8080"
TOKEN="primeleague_api_token_2024"
PLAYER_NAME="vini"
DISCORD_ID="1234567890123456789"
NEW_DISCORD_ID="9876543210987654321"
IP_ADDRESS="127.0.0.1"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para testar endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "\n${YELLOW}🔍 Testando: $description${NC}"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$data" ]; then
        echo "Payload: $data"
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X $method "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN")
    fi
    
    # Extrair status HTTP
    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')
    
    if [ "$http_status" = "200" ]; then
        echo -e "${GREEN}✅ Sucesso (HTTP $http_status)${NC}"
        echo "Resposta: $response_body"
    else
        echo -e "${RED}❌ Falha (HTTP $http_status)${NC}"
        echo "Resposta: $response_body"
    fi
}

# =====================================================
# TESTE 1: HEALTH CHECK
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 1: Verificação de Saúde da API${NC}"
test_endpoint "GET" "/api/health" "" "Health Check"

# =====================================================
# TESTE 2: GERAÇÃO DE CÓDIGOS DE BACKUP
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 2: Geração de Códigos de Backup${NC}"
test_endpoint "POST" "/api/v1/recovery/backup/generate" \
    "{\"discordId\":\"$DISCORD_ID\",\"ipAddress\":\"$IP_ADDRESS\"}" \
    "Geração de Códigos de Backup"

# =====================================================
# TESTE 3: STATUS DOS CÓDIGOS
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 3: Status dos Códigos de Backup${NC}"
test_endpoint "GET" "/api/v1/recovery/status/$DISCORD_ID" "" \
    "Status dos Códigos de Backup"

# =====================================================
# TESTE 4: AUDITORIA DE CÓDIGOS
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 4: Auditoria de Códigos${NC}"
test_endpoint "GET" "/api/v1/recovery/audit/$DISCORD_ID" "" \
    "Auditoria de Códigos de Backup"

# =====================================================
# TESTE 5: TRANSFERÊNCIA DE ASSINATURA
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 5: Transferência de Assinatura${NC}"
test_endpoint "POST" "/api/v1/discord/transfer" \
    "{\"playerName\":\"$PLAYER_NAME\",\"newDiscordId\":\"$NEW_DISCORD_ID\"}" \
    "Transferência de Assinatura"

# =====================================================
# TESTE 6: VERIFICAÇÃO DE CÓDIGO (SIMULAÇÃO)
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 6: Verificação de Código de Backup${NC}"
test_endpoint "POST" "/api/v1/recovery/verify" \
    "{\"playerName\":\"$PLAYER_NAME\",\"backupCode\":\"TEST123\",\"ipAddress\":\"$IP_ADDRESS\"}" \
    "Verificação de Código de Backup"

echo -e "\n${GREEN}🎉 Testes concluídos!${NC}"
echo -e "\n${YELLOW}📝 Notas:${NC}"
echo "- Se o servidor não estiver rodando, todos os testes falharão"
echo "- Verifique se o plugin foi carregado corretamente no console do servidor"
echo "- Alguns testes podem falhar se os dados de teste não existirem no banco"
