# 🚀 Setup Nova Máquina - Prime League

## 📋 Pré-requisitos

### 1. **Java 8+**
```bash
# Verificar versão
java -version
```

### 2. **Node.js 16+**
```bash
# Verificar versão
node --version
npm --version
```

### 3. **MySQL 8.0+**
```bash
# Verificar versão
mysql --version
```

### 4. **Maven 3.6+**
```bash
# Verificar versão
mvn --version
```

### 5. **Git**
```bash
# Verificar versão
git --version
```

## 🗄️ Setup do Banco de Dados

### 1. **Configurar MySQL**
```sql
-- Criar usuário (se necessário)
CREATE USER 'root'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

### 2. **Executar Schema Final**
```bash
# Navegar para pasta database
cd database

# Executar script automatizado
./EXECUTE-SCHEMA-FINAL.bat
```

**Ou manualmente:**
```bash
mysql -h localhost -P 3306 -u root -proot < SCHEMA-FINAL-AUTOMATIZADO.sql
```

## 🤖 Setup do Bot Discord

### 1. **Instalar Dependências**
```bash
cd primeleague-discord-bot-node
npm install
```

### 2. **Configurar Variáveis de Ambiente**
```bash
# Criar arquivo .env
cp .env.example .env

# Editar .env com suas configurações
nano .env
```

**Conteúdo do .env:**
```env
# Discord Bot
DISCORD_TOKEN=seu_token_aqui
DISCORD_CLIENT_ID=seu_client_id_aqui

# Database
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASS=root
DB_NAME=primeleague

# API Core
CORE_API_URL=http://localhost:8080
CORE_API_KEY=sua_api_key_aqui
```

### 3. **Testar Bot**
```bash
npm start
```

## ☕ Setup dos Plugins Java

### 1. **Compilar Core API**
```bash
cd primeleague-api
mvn clean install
```

### 2. **Compilar Core**
```bash
cd ../primeleague-core
mvn clean install
```

### 3. **Compilar P2P**
```bash
cd ../primeleague-p2p
mvn clean install
```

### 4. **Copiar para Servidor**
```bash
# Copiar plugins para pasta do servidor
cp target/primeleague-core-1.0.0.jar ../../server/plugins/
cp target/primeleague-p2p-1.0.0.jar ../../server/plugins/
```

## 🎮 Setup do Servidor Minecraft

### 1. **Configurar server.properties**
```properties
server-port=25565
online-mode=false
spawn-protection=0
max-players=20
```

### 2. **Configurar plugins**
```yaml
# config.yml do Core
database:
  host: localhost
  port: 3306
  user: root
  password: root
  database: primeleague

# config.yml do P2P
discord:
  webhook_url: "sua_webhook_url"
  bot_token: "seu_bot_token"
```

## 🔧 Configurações Específicas

### **UUID Compatibility Fix**
O schema já inclui a correção para UUID compatibility entre Node.js e Java.

### **Discord First Registration Flow**
- Players devem se registrar primeiro no Discord
- Use `/registrar <nickname>` no Discord
- Depois use `/verify <código>` no jogo

### **Shared Subscriptions**
- Assinaturas são compartilhadas por Discord ID
- Múltiplas contas Minecraft podem usar a mesma assinatura

## 🧪 Testes

### 1. **Testar Banco**
```sql
USE primeleague;
SHOW TABLES;
CALL GetServerStats();
```

### 2. **Testar Bot Discord**
```
/registrar vini
```

### 3. **Testar Servidor**
```
Conectar como 'vini'
/verify <código>
```

## 📁 Estrutura de Arquivos

```
primeleague/
├── database/
│   ├── SCHEMA-FINAL-AUTOMATIZADO.sql
│   └── EXECUTE-SCHEMA-FINAL.bat
├── primeleague-api/
├── primeleague-core/
├── primeleague-p2p/
├── primeleague-discord-bot-node/
└── server/
    └── plugins/
```

## 🚨 Troubleshooting

### **Erro de Conexão MySQL**
```bash
# Verificar se MySQL está rodando
net start mysql

# Verificar credenciais
mysql -u root -p
```

### **Erro de Compilação Java**
```bash
# Limpar e recompilar
mvn clean install -U
```

### **Erro de Bot Discord**
```bash
# Verificar token
# Verificar permissões do bot
# Verificar se bot está no servidor
```

### **Erro de UUID**
```bash
# Verificar se UUID está correto no banco
SELECT uuid, name FROM player_data WHERE name = 'vini';
```

## ✅ Checklist de Setup

- [ ] Java 8+ instalado
- [ ] Node.js 16+ instalado
- [ ] MySQL 8.0+ instalado
- [ ] Maven 3.6+ instalado
- [ ] Git instalado
- [ ] Schema executado com sucesso
- [ ] Bot Discord configurado e funcionando
- [ ] Plugins Java compilados
- [ ] Servidor Minecraft configurado
- [ ] Teste de registro funcionando
- [ ] Teste de verificação funcionando

## 🎯 Próximos Passos

Após o setup completo, você pode:

1. **Desenvolver novos comandos** no bot Discord
2. **Adicionar novos plugins** Java
3. **Implementar novas funcionalidades** no servidor
4. **Otimizar performance** do banco de dados
5. **Adicionar novos recursos** de assinatura

## 📞 Suporte

Para dúvidas ou problemas:
- Verificar logs do bot: `npm start`
- Verificar logs do servidor: `server/logs/`
- Verificar logs do MySQL: `mysql_error.log`

---

**🎉 Setup concluído! O Prime League está pronto para desenvolvimento!**
