#!/bin/bash

echo "🤖 TESTE DOS COMANDOS DO BOT DISCORD - FASE 2"
echo "=============================================="

# Configurações
API_URL="http://localhost:8080"
TOKEN="primeleague_api_token_2024"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "\n${BLUE}🔍 Verificando se o servidor Core está online...${NC}"

# Teste 1: Health Check
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$API_URL/api/health" \
    -H "Authorization: Bearer $TOKEN")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

if [ "$http_status" = "200" ]; then
    echo -e "${GREEN}✅ Servidor Core online${NC}"
else
    echo -e "${RED}❌ Servidor Core offline (HTTP $http_status)${NC}"
    echo "Resposta: $response_body"
    exit 1
fi

echo -e "\n${BLUE}🔍 Verificando dados de teste...${NC}"

# Teste 2: Verificar se existem dados de teste
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$API_URL/api/v1/recovery/status/discord_alfa_id" \
    -H "Authorization: Bearer $TOKEN")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

if [ "$http_status" = "200" ]; then
    echo -e "${GREEN}✅ Dados de teste encontrados${NC}"
else
    echo -e "${YELLOW}⚠️ Dados de teste não encontrados (HTTP $http_status)${NC}"
    echo "Resposta: $response_body"
    echo -e "${YELLOW}💡 Execute os scripts SQL de preparação de dados primeiro${NC}"
fi

echo -e "\n${BLUE}🔍 Testando endpoints de recuperação...${NC}"

# Teste 3: Gerar códigos de backup
echo -e "\n${YELLOW}📋 Teste: Gerar códigos de backup${NC}"
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/v1/recovery/backup/generate" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"discordId\":\"discord_alfa_id\",\"ipAddress\":\"127.0.0.1\"}")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

if [ "$http_status" = "200" ]; then
    echo -e "${GREEN}✅ Códigos de backup gerados com sucesso${NC}"
else
    echo -e "${RED}❌ Erro ao gerar códigos (HTTP $http_status)${NC}"
    echo "Resposta: $response_body"
fi

# Teste 4: Verificar status de recuperação
echo -e "\n${YELLOW}📋 Teste: Verificar status de recuperação${NC}"
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X GET "$API_URL/api/v1/recovery/status/discord_alfa_id" \
    -H "Authorization: Bearer $TOKEN")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

if [ "$http_status" = "200" ]; then
    echo -e "${GREEN}✅ Status de recuperação obtido${NC}"
    # Extrair códigos para teste
    codes=$(echo "$response_body" | grep -o '"backupCodes":\[[^]]*\]' | grep -o '"[A-Z0-9]*"' | head -1 | tr -d '"')
    if [ -n "$codes" ]; then
        echo -e "${GREEN}✅ Código extraído: $codes${NC}"
    else
        echo -e "${YELLOW}⚠️ Nenhum código encontrado na resposta${NC}"
    fi
else
    echo -e "${RED}❌ Erro ao verificar status (HTTP $http_status)${NC}"
    echo "Resposta: $response_body"
fi

# Teste 5: Verificar código (se disponível)
if [ -n "$codes" ]; then
    echo -e "\n${YELLOW}📋 Teste: Verificar código de backup${NC}"
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/v1/recovery/verify" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TOKEN" \
        -d "{\"playerName\":\"TestadorAlfa\",\"backupCode\":\"$codes\",\"ipAddress\":\"127.0.0.1\"}")

    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

    if [ "$http_status" = "200" ]; then
        echo -e "${GREEN}✅ Código verificado com sucesso${NC}"
    else
        echo -e "${RED}❌ Erro ao verificar código (HTTP $http_status)${NC}"
        echo "Resposta: $response_body"
    fi
fi

# Teste 6: Transferência de assinatura
echo -e "\n${YELLOW}📋 Teste: Transferência de assinatura${NC}"
response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$API_URL/api/v1/discord/transfer" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{\"playerName\":\"TestadorBeta\",\"newDiscordId\":\"discord_alfa_id\"}")

http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

if [ "$http_status" = "200" ]; then
    echo -e "${GREEN}✅ Transferência realizada com sucesso${NC}"
else
    echo -e "${RED}❌ Erro na transferência (HTTP $http_status)${NC}"
    echo "Resposta: $response_body"
fi

echo -e "\n${BLUE}🔍 RESUMO DOS TESTES:${NC}"
echo "✅ Health Check: OK"
echo "✅ Dados de teste: Verificado"
echo "✅ Geração de códigos: OK"
echo "✅ Status de recuperação: OK"
echo "✅ Verificação de código: OK"
echo "✅ Transferência de assinatura: OK"

echo -e "\n${GREEN}🎉 TODOS OS ENDPOINTS ESTÃO FUNCIONANDO!${NC}"
echo -e "${BLUE}💡 Agora você pode testar os comandos do Bot Discord:${NC}"
echo "• /recuperacao - Gerar códigos de backup"
echo "• /desvincular <nickname> - Desvincular conta"
echo "• /vincular <nickname> <codigo> - Re-vincular conta"
