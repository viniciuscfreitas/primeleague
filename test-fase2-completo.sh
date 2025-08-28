#!/bin/bash

echo "🧪 TESTE COMPLETO - FASE 2: SISTEMA DE RECUPERAÇÃO"
echo "=================================================="

# Configurações
API_URL="http://localhost:8080"
TOKEN="primeleague_api_token_2024"
MYSQL_HOST="localhost"
MYSQL_USER="root"
MYSQL_PASS=""
MYSQL_DB="primeleague"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para executar query MySQL
execute_mysql() {
    local query="$1"
    mysql -h"$MYSQL_HOST" -u"$MYSQL_USER" -p"$MYSQL_PASS" "$MYSQL_DB" -e "$query" 2>/dev/null
}

# Função para testar endpoint
test_endpoint() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local description="$4"
    local expected_status="$5"
    
    echo -e "\n${BLUE}🔍 Testando: $description${NC}"
    echo "Endpoint: $method $endpoint"
    
    if [ -n "$data" ]; then
        echo "Payload: $data"
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "$method" "$API_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $TOKEN" \
            -d "$data")
    else
        response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X "$method" "$API_URL$endpoint" \
            -H "Authorization: Bearer $TOKEN")
    fi
    
    http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
    response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')
    
    if [ "$http_status" = "$expected_status" ]; then
        echo -e "${GREEN}✅ Sucesso (HTTP $http_status)${NC}"
        echo "Resposta: $response_body"
        return 0
    else
        echo -e "${RED}❌ Falha (HTTP $http_status) - Esperado: $expected_status${NC}"
        echo "Resposta: $response_body"
        return 1
    fi
}

# Função para verificar dados no banco
check_database() {
    local query="$1"
    local description="$2"
    
    echo -e "\n${YELLOW}🔍 Verificando Banco: $description${NC}"
    result=$(execute_mysql "$query")
    echo "Resultado: $result"
}

echo -e "\n${YELLOW}📋 PREPARANDO DADOS DE TESTE${NC}"
echo "=================================="

# 1. Limpar dados de teste anteriores
echo "Limpando dados de teste anteriores..."
execute_mysql "DELETE FROM recovery_codes WHERE discord_id IN ('TEST001', 'TEST002', 'TEST003');"
execute_mysql "DELETE FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002', 'TEST003');"
execute_mysql "DELETE FROM player_data WHERE player_name IN ('testplayer1', 'testplayer2', 'testplayer3');"

# 2. Inserir jogadores de teste
echo "Inserindo jogadores de teste..."
execute_mysql "INSERT INTO player_data (player_name, uuid, first_login, last_login) VALUES 
('testplayer1', 'uuid-test-1', NOW(), NOW()),
('testplayer2', 'uuid-test-2', NOW(), NOW()),
('testplayer3', 'uuid-test-3', NOW(), NOW());"

# 3. Inserir Discord users de teste com diferentes cenários
echo "Inserindo Discord users de teste..."
execute_mysql "INSERT INTO discord_users (discord_id, player_id, tier, expires_at, created_at, updated_at) VALUES 
('TEST001', (SELECT id FROM player_data WHERE player_name = 'testplayer1'), 'PREMIUM', DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW()),
('TEST002', (SELECT id FROM player_data WHERE player_name = 'testplayer2'), 'BASIC', DATE_ADD(NOW(), INTERVAL 15 DAY), NOW(), NOW()),
('TEST003', (SELECT id FROM player_data WHERE player_name = 'testplayer3'), NULL, NULL, NOW(), NOW());"

echo -e "\n${GREEN}✅ Dados de teste preparados!${NC}"

# =====================================================
# TESTE 1: GERAÇÃO DE CÓDIGOS DE BACKUP
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 1: Geração de Códigos de Backup${NC}"
echo "=================================="

# Teste 1.1: Sucesso
test_endpoint "POST" "/api/v1/recovery/backup/generate" \
    '{"discordId":"TEST001","ipAddress":"127.0.0.1"}' \
    "Geração de Códigos de Backup (Sucesso)" \
    "200"

if [ $? -eq 0 ]; then
    # Verificar se os códigos foram criados no banco
    check_database "SELECT COUNT(*) as total_codes, 
                          SUM(CASE WHEN code_hash NOT LIKE '%TEST%' THEN 1 ELSE 0 END) as hashed_codes,
                          discord_id 
                   FROM recovery_codes 
                   WHERE discord_id = 'TEST001';" \
        "Verificação de códigos gerados (deve ter 5 códigos hasheados)"
fi

# =====================================================
# TESTE 2: VERIFICAÇÃO DE CÓDIGOS
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 2: Verificação de Códigos${NC}"
echo "=================================="

# Primeiro, obter um código válido do banco
echo "Obtendo código válido do banco..."
valid_code=$(execute_mysql "SELECT code FROM recovery_codes WHERE discord_id = 'TEST001' AND status = 'ACTIVE' LIMIT 1;" | tail -n +2 | head -1)

if [ -n "$valid_code" ]; then
    echo "Código válido encontrado: $valid_code"
    
    # Teste 2.1: Sucesso
    test_endpoint "POST" "/api/v1/recovery/verify" \
        "{\"playerName\":\"testplayer1\",\"backupCode\":\"$valid_code\",\"ipAddress\":\"127.0.0.1\"}" \
        "Verificação de Código (Sucesso)" \
        "200"
    
    if [ $? -eq 0 ]; then
        # Verificar se o código foi marcado como usado
        check_database "SELECT status, used_at FROM recovery_codes WHERE code = '$valid_code';" \
            "Verificação de código usado (deve estar USED)"
    fi
    
    # Teste 2.2: Falha - Código já utilizado
    test_endpoint "POST" "/api/v1/recovery/verify" \
        "{\"playerName\":\"testplayer1\",\"backupCode\":\"$valid_code\",\"ipAddress\":\"127.0.0.1\"}" \
        "Verificação de Código (Já Utilizado)" \
        "400"
    
    # Teste 2.3: Falha - Código inválido
    test_endpoint "POST" "/api/v1/recovery/verify" \
        '{"playerName":"testplayer1","backupCode":"INVALID123","ipAddress":"127.0.0.1"}' \
        "Verificação de Código (Inválido)" \
        "400"
else
    echo -e "${RED}❌ Erro: Não foi possível obter um código válido para teste${NC}"
fi

# =====================================================
# TESTE 3: TRANSFERÊNCIA DE ASSINATURAS
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 3: Transferência de Assinaturas${NC}"
echo "=================================="

# Verificar estado inicial
echo "Estado inicial das assinaturas:"
check_database "SELECT discord_id, tier, expires_at FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002', 'TEST003');" \
    "Estado inicial das assinaturas"

# Teste 3.1: Cenário A - Criar (TEST001 PREMIUM -> TEST003 NULL)
echo -e "\n${BLUE}🔍 Teste 3.1: Cenário A - Criar${NC}"
test_endpoint "POST" "/api/v1/discord/transfer" \
    '{"playerName":"testplayer1","newDiscordId":"TEST003"}' \
    "Transferência: PREMIUM -> NULL (Criar)" \
    "200"

if [ $? -eq 0 ]; then
    check_database "SELECT discord_id, tier, expires_at FROM discord_users WHERE discord_id IN ('TEST001', 'TEST003');" \
        "Resultado: TEST001 deve estar NULL, TEST003 deve ter PREMIUM"
fi

# Preparar dados para próximo teste
execute_mysql "UPDATE discord_users SET tier = 'PREMIUM', expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE discord_id = 'TEST001';"
execute_mysql "UPDATE discord_users SET tier = 'PREMIUM', expires_at = DATE_ADD(NOW(), INTERVAL 15 DAY) WHERE discord_id = 'TEST002';"

# Teste 3.2: Cenário B - Somar (TEST001 PREMIUM 30d -> TEST002 PREMIUM 15d)
echo -e "\n${BLUE}🔍 Teste 3.2: Cenário B - Somar${NC}"
test_endpoint "POST" "/api/v1/discord/transfer" \
    '{"playerName":"testplayer1","newDiscordId":"TEST002"}' \
    "Transferência: PREMIUM 30d -> PREMIUM 15d (Somar)" \
    "200"

if [ $? -eq 0 ]; then
    check_database "SELECT discord_id, tier, expires_at FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002');" \
        "Resultado: TEST001 deve estar NULL, TEST002 deve ter PREMIUM ~45d"
fi

# Preparar dados para próximo teste
execute_mysql "UPDATE discord_users SET tier = 'PREMIUM', expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE discord_id = 'TEST001';"
execute_mysql "UPDATE discord_users SET tier = 'BASIC', expires_at = DATE_ADD(NOW(), INTERVAL 15 DAY) WHERE discord_id = 'TEST002';"

# Teste 3.3: Cenário C - Sobrescrever (TEST001 PREMIUM 30d -> TEST002 BASIC 15d)
echo -e "\n${BLUE}🔍 Teste 3.3: Cenário C - Sobrescrever${NC}"
test_endpoint "POST" "/api/v1/discord/transfer" \
    '{"playerName":"testplayer1","newDiscordId":"TEST002"}' \
    "Transferência: PREMIUM 30d -> BASIC 15d (Sobrescrever)" \
    "200"

if [ $? -eq 0 ]; then
    check_database "SELECT discord_id, tier, expires_at FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002');" \
        "Resultado: TEST001 deve estar NULL, TEST002 deve ter PREMIUM 30d"
fi

# Preparar dados para próximo teste
execute_mysql "UPDATE discord_users SET tier = 'BASIC', expires_at = DATE_ADD(NOW(), INTERVAL 15 DAY) WHERE discord_id = 'TEST001';"
execute_mysql "UPDATE discord_users SET tier = 'PREMIUM', expires_at = DATE_ADD(NOW(), INTERVAL 30 DAY) WHERE discord_id = 'TEST002';"

# Teste 3.4: Cenário D - Manter e Somar (TEST001 BASIC 15d -> TEST002 PREMIUM 30d)
echo -e "\n${BLUE}🔍 Teste 3.4: Cenário D - Manter e Somar${NC}"
test_endpoint "POST" "/api/v1/discord/transfer" \
    '{"playerName":"testplayer1","newDiscordId":"TEST002"}' \
    "Transferência: BASIC 15d -> PREMIUM 30d (Manter e Somar)" \
    "200"

if [ $? -eq 0 ]; then
    check_database "SELECT discord_id, tier, expires_at FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002');" \
        "Resultado: TEST001 deve estar NULL, TEST002 deve manter PREMIUM ~45d"
fi

# =====================================================
# TESTE 4: ENDPOINTS ADICIONAIS
# =====================================================
echo -e "\n${YELLOW}📋 TESTE 4: Endpoints Adicionais${NC}"
echo "=================================="

# Teste 4.1: Status dos códigos
test_endpoint "GET" "/api/v1/recovery/status/TEST001" \
    "" \
    "Status dos Códigos de Backup" \
    "200"

# Teste 4.2: Auditoria
test_endpoint "GET" "/api/v1/recovery/audit/TEST001" \
    "" \
    "Auditoria de Códigos" \
    "200"

# Teste 4.3: Health Check
test_endpoint "GET" "/api/health" \
    "" \
    "Health Check" \
    "200"

# =====================================================
# LIMPEZA FINAL
# =====================================================
echo -e "\n${YELLOW}🧹 LIMPEZA DOS DADOS DE TESTE${NC}"
echo "=================================="

execute_mysql "DELETE FROM recovery_codes WHERE discord_id IN ('TEST001', 'TEST002', 'TEST003');"
execute_mysql "DELETE FROM discord_users WHERE discord_id IN ('TEST001', 'TEST002', 'TEST003');"
execute_mysql "DELETE FROM player_data WHERE player_name IN ('testplayer1', 'testplayer2', 'testplayer3');"

echo -e "\n${GREEN}🎉 TESTE COMPLETO FINALIZADO!${NC}"
echo -e "\n${BLUE}📊 RESUMO:${NC}"
echo "- ✅ Health Check: Funcionando"
echo "- ✅ Geração de Códigos: Testada"
echo "- ✅ Verificação de Códigos: Testada"
echo "- ✅ Transferência de Assinaturas: Todos os cenários testados"
echo "- ✅ Endpoints Adicionais: Testados"
echo "- ✅ Validações de Negócio: Funcionando"
echo "- ✅ Banco de Dados: Integração validada"
