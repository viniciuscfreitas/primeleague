#!/bin/bash

echo "🔍 TESTE DE DEBUG - VALIDAÇÃO DE DISCORD IDs IGUAIS"
echo "=================================================="

# Configurações
API_URL="http://localhost:8080"
TOKEN="primeleague_api_token_2024"
PLAYER_NAME="TestadorAlfa"
DISCORD_ID="discord_alfa_id"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "\n${BLUE}🔍 Testando transferência para mesmo Discord ID${NC}"
echo "Player: $PLAYER_NAME"
echo "Discord ID: $DISCORD_ID"
echo "Esperado: HTTP 400 (IDs iguais)"

# Teste 1: Transferência para mesmo Discord ID
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/v1/discord/transfer" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"playerName\":\"$PLAYER_NAME\",\"newDiscordId\":\"$DISCORD_ID\"}")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

echo -e "\nStatus: ${YELLOW}HTTP $http_status${NC}"
echo "Resposta: $response_body"

if [ "$http_status" = "400" ]; then
    echo -e "${GREEN}✅ CORRETO - Retornou 400 como esperado${NC}"
else
    echo -e "${RED}❌ PROBLEMA - Retornou $http_status em vez de 400${NC}"
    echo -e "${YELLOW}🔍 Isso indica que a validação não está funcionando corretamente${NC}"
fi

echo -e "\n${BLUE}🔍 Testando transferência para Discord ID diferente${NC}"
echo "Player: $PLAYER_NAME"
echo "Novo Discord ID: discord_beta_id"
echo "Esperado: HTTP 200 (transferência válida)"

# Teste 2: Transferência para Discord ID diferente (deve funcionar)
response2=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/v1/discord/transfer" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"playerName\":\"$PLAYER_NAME\",\"newDiscordId\":\"discord_beta_id\"}")

http_status2=$(echo "$response2" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body2=$(echo "$response2" | sed '/HTTP_STATUS:/d')

echo -e "\nStatus: ${YELLOW}HTTP $http_status2${NC}"
echo "Resposta: $response_body2"

if [ "$http_status2" = "200" ]; then
    echo -e "${GREEN}✅ CORRETO - Transferência funcionou${NC}"
else
    echo -e "${RED}❌ PROBLEMA - Transferência falhou${NC}"
fi

echo -e "\n${BLUE}🔍 RESUMO DO DEBUG:${NC}"
echo "Teste 1 (IDs iguais): HTTP $http_status"
echo "Teste 2 (IDs diferentes): HTTP $http_status2"

if [ "$http_status" = "400" ] && [ "$http_status2" = "200" ]; then
    echo -e "${GREEN}🎉 VALIDAÇÃO FUNCIONANDO CORRETAMENTE!${NC}"
else
    echo -e "${RED}🚨 PROBLEMA IDENTIFICADO - VALIDAÇÃO NÃO FUNCIONA${NC}"
fi
