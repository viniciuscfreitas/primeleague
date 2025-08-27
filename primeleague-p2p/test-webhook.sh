#!/bin/bash

# =====================================================
# SCRIPT DE TESTE DO WEBHOOK - PRIME LEAGUE
# =====================================================

# Configurações
WEBHOOK_URL="http://localhost:8765/webhook/payment"
WEBHOOK_SECRET="SEU_WEBHOOK_SECRET_AQUI"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  TESTE DO WEBHOOK - PRIME LEAGUE${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo ""

# Função para testar webhook
test_webhook() {
    local test_name="$1"
    local payload="$2"
    local expected_status="$3"

    echo -e "${YELLOW}🧪 Testando: $test_name${NC}"

    # Fazer requisição
    response=$(curl -s -w "\n%{http_code}" \
        -X POST "$WEBHOOK_URL" \
        -H "Content-Type: application/json" \
        -H "X-Webhook-Secret: $WEBHOOK_SECRET" \
        -d "$payload")

    # Separar resposta e status code
    http_code=$(echo "$response" | tail -n1)
    response_body=$(echo "$response" | head -n -1)

    # Verificar resultado
    if [ "$http_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ Sucesso! Status: $http_code${NC}"
        echo -e "${BLUE}Resposta: $response_body${NC}"
    else
        echo -e "${RED}❌ Falha! Esperado: $expected_status, Recebido: $http_code${NC}"
        echo -e "${BLUE}Resposta: $response_body${NC}"
    fi
    echo ""
}

# Teste 1: Payload válido - Pagamento aprovado
echo -e "${BLUE}📋 TESTE 1: Payload Válido - Pagamento Aprovado${NC}"
valid_payload='{
  "transaction_id": "txn_test_123456789",
  "discord_id": "123456789012345678",
  "player_name": "finicff",
  "amount": 29.90,
  "currency": "BRL",
  "status": "approved",
  "payment_method": "pix",
  "subscription_days": 30,
  "timestamp": "2024-01-15T10:30:00Z"
}'
test_webhook "Payload válido" "$valid_payload" "200"

# Teste 2: Webhook secret inválido
echo -e "${BLUE}📋 TESTE 2: Webhook Secret Inválido${NC}"
echo -e "${YELLOW}🧪 Testando: Webhook secret inválido${NC}"
response=$(curl -s -w "\n%{http_code}" \
    -X POST "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "X-Webhook-Secret: SECRET_INVALIDO" \
    -d "$valid_payload")

http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" = "403" ]; then
    echo -e "${GREEN}✅ Sucesso! Status: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
else
    echo -e "${RED}❌ Falha! Esperado: 403, Recebido: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
fi
echo ""

# Teste 3: Método HTTP inválido
echo -e "${BLUE}📋 TESTE 3: Método HTTP Inválido${NC}"
echo -e "${YELLOW}🧪 Testando: Método GET (inválido)${NC}"
response=$(curl -s -w "\n%{http_code}" \
    -X GET "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "X-Webhook-Secret: $WEBHOOK_SECRET")

http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" = "405" ]; then
    echo -e "${GREEN}✅ Sucesso! Status: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
else
    echo -e "${RED}❌ Falha! Esperado: 405, Recebido: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
fi
echo ""

# Teste 4: Payload inválido - campos obrigatórios ausentes
echo -e "${BLUE}📋 TESTE 4: Payload Inválido - Campos Ausentes${NC}"
invalid_payload='{
  "transaction_id": "txn_test_123456789",
  "amount": 29.90
}'
test_webhook "Campos obrigatórios ausentes" "$invalid_payload" "400"

# Teste 5: Payload inválido - JSON malformado
echo -e "${BLUE}📋 TESTE 5: JSON Malformado${NC}"
echo -e "${YELLOW}🧪 Testando: JSON malformado${NC}"
response=$(curl -s -w "\n%{http_code}" \
    -X POST "$WEBHOOK_URL" \
    -H "Content-Type: application/json" \
    -H "X-Webhook-Secret: $WEBHOOK_SECRET" \
    -d '{ invalid json }')

http_code=$(echo "$response" | tail -n1)
response_body=$(echo "$response" | head -n -1)

if [ "$http_code" = "400" ]; then
    echo -e "${GREEN}✅ Sucesso! Status: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
else
    echo -e "${RED}❌ Falha! Esperado: 400, Recebido: $http_code${NC}"
    echo -e "${BLUE}Resposta: $response_body${NC}"
fi
echo ""

# Teste 6: Pagamento pendente
echo -e "${BLUE}📋 TESTE 6: Pagamento Pendente${NC}"
pending_payload='{
  "transaction_id": "txn_test_987654321",
  "discord_id": "123456789012345678",
  "player_name": "finicff",
  "amount": 29.90,
  "currency": "BRL",
  "status": "pending",
  "payment_method": "pix",
  "subscription_days": 30,
  "timestamp": "2024-01-15T10:30:00Z"
}'
test_webhook "Pagamento pendente" "$pending_payload" "200"

# Teste 7: Pagamento cancelado
echo -e "${BLUE}📋 TESTE 7: Pagamento Cancelado${NC}"
cancelled_payload='{
  "transaction_id": "txn_test_555666777",
  "discord_id": "123456789012345678",
  "player_name": "finicff",
  "amount": 29.90,
  "currency": "BRL",
  "status": "cancelled",
  "payment_method": "pix",
  "subscription_days": 30,
  "timestamp": "2024-01-15T10:30:00Z"
}'
test_webhook "Pagamento cancelado" "$cancelled_payload" "200"

echo -e "${BLUE}=====================================================${NC}"
echo -e "${BLUE}  TESTES CONCLUÍDOS${NC}"
echo -e "${BLUE}=====================================================${NC}"
echo ""
echo -e "${YELLOW}📝 Notas:${NC}"
echo -e "• Certifique-se de que o servidor Minecraft está rodando"
echo -e "• Verifique se o webhook secret está configurado corretamente"
echo -e "• Os testes assumem que o jogador 'finicff' existe no banco"
echo -e "• Para testes reais, use um Discord ID válido vinculado a um jogador"
echo ""
