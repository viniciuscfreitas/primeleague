# Prime League Discord Bot

Bot Discord para o servidor Prime League - Sistema de autenticação, gerenciamento de contas e notificações.

## 🚀 Instalação

1. **Instalar dependências:**
   ```bash
   npm install
   ```

2. **Configurar variáveis de ambiente:**
   - Copie `config.example.js` para `.env`
   - Configure as variáveis necessárias:
     - `DISCORD_BOT_TOKEN`: Token do bot Discord
     - `DISCORD_GUILD_ID`: ID do servidor Discord
     - `DB_HOST`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`: Configurações do banco MySQL

3. **Executar o bot:**
   ```bash
   npm start
   ```

## 📋 Funcionalidades

### Comandos Disponíveis
- `/registrar` - Registrar nova conta
- `/vincular` - Vincular conta Discord ao Minecraft
- `/desvincular` - Desvincular conta
- `/conta` - Ver informações da conta
- `/recuperacao` - Sistema de recuperação de conta
- `/ip-status` - Verificar status de IPs autorizados
- `/upgrade-doador` - Sistema de upgrade de doador
- `/assinatura` - Gerenciar assinaturas
- `/registrar-portfolio` - Registrar portfolio

### Sistemas Integrados
- **NotificationWorker**: Processa notificações do servidor
- **StatusWorker**: Monitora status do servidor
- **IPAuthHandler**: Gerencia autenticação de IPs
- **WebhookHandler**: Recebe webhooks do servidor

## 🔧 Configuração

### Variáveis de Ambiente Obrigatórias
```env
DISCORD_BOT_TOKEN=seu_token_do_bot
DISCORD_GUILD_ID=id_do_servidor
DB_HOST=localhost
DB_USER=usuario_mysql
DB_PASSWORD=senha_mysql
DB_NAME=primeleague
```

### Variáveis Opcionais
```env
STATUS_CHANNEL_ID=id_do_canal_status
WEBHOOK_PORT=3000
API_BASE_URL=http://localhost:8080
```

## 🐛 Solução de Problemas

### Erro: "Could not read package.json"
- **Causa**: Arquivo `package.json` faltando
- **Solução**: Execute `npm install` para recriar as dependências

### Erro: "Unknown column 'processed'"
- **Causa**: Schema do banco desatualizado
- **Solução**: Execute o script `primeleague_schema.sql` atualizado

### Bot não responde aos comandos
- **Verificar**: Token do bot e permissões no Discord
- **Verificar**: ID do servidor está correto
- **Verificar**: Bot está online e no servidor

## 📁 Estrutura do Projeto

```
src/
├── commands/          # Comandos slash do Discord
├── handlers/          # Handlers de interações
├── workers/           # Workers de background
├── database/          # Conexão com banco de dados
└── index.js          # Arquivo principal
```

## 🔄 Atualizações

Para atualizar o bot:
1. Pare o bot (Ctrl+C)
2. Execute `npm install` para atualizar dependências
3. Reinicie com `npm start`

## 📞 Suporte

Para problemas ou dúvidas, verifique:
1. Logs do console para erros
2. Configuração das variáveis de ambiente
3. Status do banco de dados MySQL
4. Permissões do bot no Discord
