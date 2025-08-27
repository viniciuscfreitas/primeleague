# PrimeLeague-Chat

Sistema de canais de chat para o servidor Prime League.

## 📋 Visão Geral

O `PrimeLeague-Chat` é um módulo especializado que gerencia todos os canais de comunicação do servidor, incluindo:

- **Chat Global**: Comunicação pública para todos os jogadores
- **Chat de Clã**: Comunicação privada entre membros do mesmo clã
- **Chat de Aliança**: Comunicação entre clãs aliados
- **Chat Local**: Comunicação baseada em proximidade geográfica

## 🏗️ Arquitetura

### Componentes Principais

1. **`ChatListener`**: Orquestrador principal que intercepta eventos de chat
2. **`ChannelManager`**: Gerencia canais e formatação de mensagens
3. **`ChatLoggingService`**: Serviço assíncrono para logging de mensagens
4. **Comandos**: `/c`, `/a`, `/chat` para gerenciar canais

### Integrações

- **TagService**: Resolve placeholders como `{clan_tag}` e `{elo}`
- **ClanManager**: Obtém informações de clãs e alianças
- **AdminManager**: Verifica status de mute
- **LimboManager**: Verifica status de verificação P2P

## 🚀 Funcionalidades

### Formatação de Mensagens

O sistema utiliza o `TagService` para resolver placeholders dinamicamente:

```yaml
# Exemplo de formato configurável
channels:
  global:
    format: "&7[&aGlobal&7] {clan_tag}&f{player}&7: &f{message}"
```

**Placeholders Suportados:**
- `{player}`: Nome do jogador
- `{message}`: Conteúdo da mensagem
- `{clan_tag}`: Tag do clã (ex: `[CLAN]`)
- `{elo}`: Rating ELO do jogador

### Canais de Chat

#### Global
- **Acesso**: Todos os jogadores
- **Destinatários**: Todos os jogadores online
- **Comando**: Chat padrão

#### Clã (`/c`)
- **Acesso**: Membros de clãs
- **Permissão**: `primeleague.chat.clan`
- **Destinatários**: Membros do mesmo clã
- **Comando**: `/c <mensagem>`

#### Aliança (`/a`)
- **Acesso**: Membros de clãs com alianças
- **Permissão**: `primeleague.chat.ally`
- **Destinatários**: Membros do clã + clãs aliados
- **Comando**: `/a <mensagem>`

#### Local
- **Acesso**: Todos os jogadores
- **Destinatários**: Jogadores dentro do raio configurado
- **Comando**: `/chat local`

### Logging Assíncrono

Todas as mensagens são registradas na tabela `chat_logs` de forma assíncrona:

```sql
INSERT INTO chat_logs (
    sender_uuid, sender_name, channel_type, 
    clan_name, ally_name, message_content, timestamp
) VALUES (?, ?, ?, ?, ?, ?, ?)
```

**Configurações de Performance:**
- **Batch Size**: 100 mensagens por lote
- **Interval**: 5 segundos entre lotes
- **Thread**: Dedicated daemon thread

## ⚙️ Configuração

### config.yml

```yaml
# Configurações dos canais
channels:
  global:
    format: "&7[&aGlobal&7] {clan_tag}&f{player}&7: &f{message}"
    allow_colors: true
    allow_formatting: true
    
  clan:
    format: "&7[&bClã&7] {clan_tag}&f{player}&7: &f{message}"
    allow_colors: true
    allow_formatting: true
    
  ally:
    format: "&7[&dAliança&7] {clan_tag}&f{player}&7: &f{message}"
    allow_colors: true
    allow_formatting: true
    
  local:
    format: "&7[&eLocal&7] {clan_tag}&f{player}&7: &f{message}"
    allow_colors: true
    allow_formatting: true
    radius: 100

# Configurações de logging
logging:
  enabled: true
  batch_interval: 5000  # 5 segundos
  batch_size: 100       # 100 mensagens por lote

# Configurações de performance
performance:
  tag_cache_duration: 300000  # 5 minutos
  auto_clear_cache: true
  max_message_length: 256
```

### plugin.yml

```yaml
name: PrimeLeague-Chat
version: 1.0.0
main: br.com.primeleague.chat.PrimeLeagueChat
description: Sistema de canais de chat para o servidor Prime League
author: Prime League Team
api-version: 1.5.2
depend: [PrimeLeague-Core]

commands:
  c:
    description: Chat do clã
    usage: /c <mensagem>
    permission: primeleague.chat.clan
  a:
    description: Chat de aliança
    usage: /a <mensagem>
    permission: primeleague.chat.ally
  chat:
    description: Gerenciar canais de chat
    usage: /chat <global|clan|ally|local|info|help>
    permission: primeleague.chat.use

permissions:
  primeleague.chat.clan:
    description: Permite usar o chat de clã
    default: true
  primeleague.chat.ally:
    description: Permite usar o chat de aliança
    default: true
  primeleague.chat.admin:
    description: Permissões administrativas do chat
    default: op
```

## 🔧 Comandos

### `/c <mensagem>`
Envia uma mensagem para o chat do clã.

**Permissão:** `primeleague.chat.clan`

### `/a <mensagem>`
Envia uma mensagem para o chat de aliança.

**Permissão:** `primeleague.chat.ally`

### `/chat <subcomando>`
Gerencia canais de chat.

**Subcomandos:**
- `global` - Define canal global como ativo
- `clan` - Define canal de clã como ativo
- `ally` - Define canal de aliança como ativo
- `local` - Define canal local como ativo
- `info` - Mostra informações do canal atual
- `help` - Mostra ajuda dos comandos

## 🔒 Segurança

### Verificações Automáticas

O sistema realiza verificações automáticas antes de processar mensagens:

1. **Limbo (P2P)**: Jogadores em verificação não podem usar chat
2. **Mute (Admin)**: Jogadores mutados não podem enviar mensagens
3. **Permissões**: Verificação de permissões por canal
4. **Clã**: Verificação de pertencimento a clã para canais específicos

### Orquestração

O `ChatListener` atua como orquestrador único com prioridade `LOWEST`, garantindo que:

- Verificações de segurança sejam aplicadas primeiro
- Integrações com outros módulos funcionem corretamente
- Não haja conflitos entre listeners

## 📊 Performance

### Otimizações Implementadas

1. **Logging Assíncrono**: Não bloqueia o thread principal
2. **Processamento em Lote**: Reduz overhead de banco de dados
3. **Cache de Tags**: Evita recálculos desnecessários
4. **Reflection Controlado**: Fallback seguro para integrações

### Monitoramento

O sistema fornece logs detalhados para monitoramento:

```
[INFO] ChatLoggingService iniciado com sucesso!
[INFO] Processados 100 logs de chat.
[WARNING] Não foi possível verificar status de limbo: Plugin não encontrado
```

## 🔄 Integração com Outros Módulos

### Dependências
- **PrimeLeague-Core**: Conexão com banco de dados e TagService
- **PrimeLeague-Clans**: Informações de clãs e alianças
- **PrimeLeague-Admin**: Verificação de mute
- **PrimeLeague-P2P**: Verificação de status de limbo

### Comunicação
- **TagService**: Resolução de placeholders
- **ClanManager**: Dados de clãs e alianças
- **AdminManager**: Status de punições
- **LimboManager**: Status de verificação

## 🚀 Deploy

### Compilação
```bash
mvn clean package
```

### Instalação
1. Copiar `target/primeleague-chat-1.0.0.jar` para `plugins/`
2. Reiniciar servidor
3. Verificar logs de inicialização

### Verificação
```
[INFO] === PrimeLeague-Chat v1.0.0 ===
[INFO] Sistema de chat inicializado com sucesso!
[INFO] Canais disponíveis: Global, Clã, Aliança, Local
[INFO] =====================================
```

## 🐛 Troubleshooting

### Problemas Comuns

1. **"TagService não encontrado"**
   - Verificar se PrimeLeague-Core está carregado
   - Verificar se TagService está registrado

2. **"ClanManager não encontrado"**
   - Verificar se PrimeLeague-Clans está carregado
   - Verificar se ClanManager está inicializado

3. **"Logs não sendo salvos"**
   - Verificar conexão com banco de dados
   - Verificar se tabela `chat_logs` existe

### Logs de Debug

Ativar logs detalhados no `config.yml`:
```yaml
debug:
  enabled: true
  log_level: DEBUG
```

## 📝 Changelog

### v1.0.0
- Implementação inicial do sistema de canais
- Integração com TagService para placeholders
- Logging assíncrono para chat_logs
- Comandos `/c`, `/a`, `/chat`
- Verificações de segurança (mute, limbo)
- Configuração flexível via config.yml
