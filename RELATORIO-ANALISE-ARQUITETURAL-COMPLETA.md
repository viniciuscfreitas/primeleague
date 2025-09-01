# Relatório de Análise Técnica: Prime League

**Data da Análise:** 28/08/2025  
**Versão do Relatório:** 1.0  
**Analista:** Arquiteto do Prime League  

---

## I. Visão Arquitetural Geral (Macro)

### 1. Diagrama de Dependências de Módulos

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PrimeLeague   │    │   PrimeLeague   │    │   PrimeLeague   │
│      Core       │◄───┤      API        │◄───┤      P2P        │
│   (Central)     │    │   (Interface)   │    │  (Autorização)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PrimeLeague   │    │   PrimeLeague   │    │   PrimeLeague   │
│      Chat       │    │     Clans       │    │     Admin       │
│  (Comunicação)  │    │   (Sistema)     │    │ (Administração) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PrimeLeague   │    │   Discord Bot   │    │   Database      │
│   AdminShop     │    │   (Node.js)     │    │   (MySQL)       │
│   (Loja)        │    │  (Integração)   │    │  (Schema)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Dependências Principais:**
- **Core** → **API**: Interface central para comunicação inter-módulos
- **P2P** → **Core**: Sistema de autorização depende do Core
- **Chat** → **Core**: Sistema de comunicação usa API do Core
- **Clans** → **Core**: Sistema de clãs integra com Core
- **Admin** → **Core**: Sistema administrativo usa Core
- **AdminShop** → **Core**: Loja administrativa depende do Core
- **Discord Bot** → **Core**: Bot externo se comunica via HTTP API

### 2. Padrões de Comunicação

**Padrões Implementados:**

1. **Injeção de Dependência (DI)**
   - Uso de registries (ProfileServiceRegistry, TagServiceRegistry, etc.)
   - Injeção via construtor nos managers

2. **Service Registry Pattern**
   - `ProfileServiceRegistry`
   - `TagServiceRegistry`
   - `DAOServiceRegistry`
   - `ClanServiceRegistry`
   - `AdminServiceRegistry`
   - `P2PServiceRegistry`
   - `LoggingServiceRegistry`

3. **API HTTP para Integração Externa**
   - `HttpApiManager` no Core (porta 8765)
   - Webhook para notificações de pagamento
   - Endpoints REST para bot Discord

4. **Event-Driven Architecture**
   - Listeners de eventos Bukkit
   - Sistema de notificações assíncrono
   - Workers para tarefas em background

### 3. Análise de Conformidade Filosófica

**Filosofia "O Coliseu Competitivo":**

✅ **CONFORME:**
- Sistema de ELO para ranking competitivo
- PvP 1.5.2 sem modificações de meta
- Sistema de clãs para competição em grupo
- Economia baseada em PvP (bounties, recompensas)
- Sistema de punições para fair play

⚠️ **PONTOS DE ATENÇÃO:**
- AdminShop pode introduzir vantagens pagas
- Sistema de doadores com benefícios
- Kits especiais podem afetar balance

---

## II. Análise Detalhada por Módulo

### Módulo 1: Core (Central)

**Estrutura de Pacotes:**
```
br.com.primeleague.core/
├── api/           # API interna e HTTP
├── commands/      # Comandos básicos
├── database/      # Acesso a dados
├── managers/      # Gerenciadores principais
├── models/        # Modelos de dados
├── profile/       # Sistema de perfis
├── services/      # Serviços internos
├── util/          # Utilitários
└── utils/         # Utilitários adicionais
```

**Principais Funcionalidades:**
- ✅ Sistema de perfis de jogadores
- ✅ Gerenciamento de economia
- ✅ Sistema de doadores
- ✅ API HTTP para integração externa
- ✅ Sistema de recuperação de conta
- ✅ Gerenciamento de identidade
- ✅ Sistema de mensagens privadas
- ✅ Gerenciamento de whitelist

**Endpoints da API Interna:**
- `PrimeLeagueAPI.getDataManager()`
- `PrimeLeagueAPI.getIdentityManager()`
- `PrimeLeagueAPI.getDonorManager()`
- `PrimeLeagueAPI.getEconomyManager()`
- `PrimeLeagueAPI.getTagManager()`

**Schema do Banco de Dados:**
- `player_data` (tabela central SSOT)
- `discord_users` (dados Discord)
- `discord_links` (vínculos Discord-Minecraft)
- `recovery_codes` (sistema de recuperação)
- `economy_logs` (auditoria econômica)
- `server_notifications` (notificações)

**Comandos e Permissões:**
- `/msg`, `/tell` - Mensagens privadas
- `/r` - Responder mensagem
- `/money` - Ver saldo
- `/pagar` - Transferir dinheiro
- `/eco` - Comandos admin de economia

**Event Listeners:**
- `ProfileListener` - Gerenciamento de perfis

**Tarefas Agendadas:**
- Limpeza de cache (assíncrona)
- Processamento de notificações

**Pontos de Atenção:**
- ⚠️ Operações de banco na thread principal
- ⚠️ Cache em memória sem persistência
- ✅ Compatibilidade Java 7 + Bukkit 1.5.2

### Módulo 2: Acesso P2P

**Estrutura de Pacotes:**
```
br.com.primeleague.p2p/
├── commands/      # Comandos P2P
├── listeners/     # Listeners de autenticação
├── managers/      # Gerenciadores P2P
├── services/      # Serviços P2P
├── tasks/         # Tarefas agendadas
└── web/           # Webhook HTTP
```

**Principais Funcionalidades:**
- ✅ Sistema de autorização de IP
- ✅ Verificação via Discord
- ✅ Sistema de limbo para pendentes
- ✅ Webhook para notificações
- ✅ Sistema de bypass para admins
- ✅ Recuperação de conta

**Endpoints da API Interna:**
- `P2PServiceRegistry.getService()`
- Integração com `LimboManager`
- Integração com `IpAuthCache`

**Schema do Banco de Dados:**
- `player_authorized_ips` (IPs autorizados)
- `discord_links` (verificação)
- `recovery_codes` (recuperação)

**Comandos e Permissões:**
- `/minhaassinatura` - Ver assinatura
- `/p2p` - Comandos admin P2P
- `/verify` - Verificar código Discord
- `/recuperar` - Recuperação de conta

**Event Listeners:**
- `AuthenticationListener` - Autenticação
- `BypassListener` - Bypass para admins

**Tarefas Agendadas:**
- `CleanupTask` - Limpeza automática

**Pontos de Atenção:**
- ✅ Sistema robusto de autenticação
- ✅ Integração segura com Discord
- ⚠️ Cache de IPs em memória

### Módulo 3: Administrativo

**Estrutura de Pacotes:**
```
br.com.primeleague.admin/
├── api/           # API administrativa
├── commands/      # Comandos admin
├── listeners/     # Listeners admin
├── managers/      # Gerenciadores admin
├── models/        # Modelos admin
└── services/      # Serviços admin
```

**Principais Funcionalidades:**
- ✅ Sistema de punições (warn, kick, mute, ban)
- ✅ Sistema de tickets de denúncia
- ✅ Modo staff (vanish, invsee, inspect)
- ✅ Sistema de whitelist
- ✅ Histórico de punições

**Endpoints da API Interna:**
- `AdminServiceRegistry.getService()`
- `AdminAPI` para comunicação inter-módulo

**Schema do Banco de Dados:**
- `punishments` (sistema de punições)
- `tickets` (sistema de denúncias)
- `staff_vanish` (modo staff)
- `whitelist_players` (whitelist)

**Comandos e Permissões:**
- `/warn`, `/kick`, `/mute`, `/ban` - Punições
- `/tempmute`, `/tempban` - Punições temporárias
- `/unmute`, `/unban` - Remover punições
- `/history` - Histórico de punições
- `/vanish`, `/invsee`, `/inspect` - Modo staff
- `/report`, `/tickets` - Sistema de denúncias
- `/whitelist` - Gerenciar whitelist

**Event Listeners:**
- `ChatListener` - Verificação de punições
- `LoginListener` - Verificação de banimentos
- `VanishListener` - Modo staff
- `JoinListener` - Carregar estado de vanish

**Tarefas Agendadas:**
- Verificação automática de punições expiradas

**Pontos de Atenção:**
- ✅ Sistema completo de moderação
- ✅ Integração com sistema P2P
- ✅ Auditoria de ações administrativas

### Módulo 4: Clãs

**Estrutura de Pacotes:**
```
br.com.primeleague.clans/
├── commands/      # Comandos de clã
├── listeners/     # Listeners de clã
├── manager/       # Gerenciador de clãs
├── model/         # Modelos de clã
└── services/      # Serviços de clã
```

**Principais Funcionalidades:**
- ✅ Criação e gerenciamento de clãs
- ✅ Sistema de hierarquia (Leader, Co-Leader, Officer, Member)
- ✅ Sistema de alianças entre clãs
- ✅ Sistema de friendly fire
- ✅ Sistema de sanções e penalidades
- ✅ Estatísticas de KDR por membro
- ✅ Sistema de convites

**Endpoints da API Interna:**
- `ClanServiceRegistry.getService()`
- `DAOServiceRegistry.getClanDAO()`
- `TagServiceRegistry` para placeholders

**Schema do Banco de Dados:**
- `clans` (dados dos clãs)
- `clan_players` (membros dos clãs)
- `clan_alliances` (alianças)
- `clan_logs` (logs de ações)

**Comandos e Permissões:**
- `/clan` - Comando principal de clãs

**Event Listeners:**
- `PlayerStatsListener` - Estatísticas KDR
- `DamageListener` - Sistema friendly fire
- `PunishmentListener` - Sanções de clã
- `PlayerConnectionListener` - Status online/offline

**Tarefas Agendadas:**
- Limpeza de convites expirados (5 min)
- Limpeza de membros inativos (diária)

**Pontos de Atenção:**
- ✅ Sistema robusto de clãs
- ✅ Integração com sistema de tags
- ✅ Sistema de penalidades automático

### Módulo 5: Chat e Tags

**Estrutura de Pacotes:**
```
br.com.primeleague.chat/
├── commands/      # Comandos de chat
├── gui/           # Interface gráfica
├── listeners/     # Listeners de chat
└── services/      # Serviços de chat
```

**Principais Funcionalidades:**
- ✅ Canais de comunicação (Global, Local, Clã, Aliança)
- ✅ Sistema de ignore de canais
- ✅ Rate limiting para spam
- ✅ Logging assíncrono de mensagens
- ✅ Sistema de mensagens privadas
- ✅ Rotação automática de logs
- ✅ Filtros avançados
- ✅ Social spy para admins

**Endpoints da API Interna:**
- `LoggingServiceRegistry.getService()`
- Integração com sistema de tags

**Schema do Banco de Dados:**
- `chat_logs` (logs de mensagens)

**Comandos e Permissões:**
- `/g` - Chat global
- `/c` - Chat de clã
- `/a` - Chat de aliança
- `/chat` - Ajuda do chat
- `/ignore` - Ignorar canais
- `/msg` - Mensagens privadas
- `/r` - Responder mensagem
- `/socialspy` - Social spy (admin)
- `/logrotation` - Rotação de logs

**Event Listeners:**
- `ChatListener` - Interceptação de chat
- `InventoryListener` - GUI de ignore

**Tarefas Agendadas:**
- Rotação automática de logs
- Processamento assíncrono de mensagens

**Pontos de Atenção:**
- ✅ Sistema robusto de comunicação
- ✅ Performance otimizada
- ✅ Logging completo para auditoria

### Módulo 6: Loja de Servidor (AdminShop)

**Estrutura de Pacotes:**
```
br.com.primeleague.adminshop/
├── commands/      # Comandos da loja
├── listeners/     # Listeners da loja
├── managers/      # Gerenciadores da loja
└── models/        # Modelos da loja
```

**Principais Funcionalidades:**
- ✅ Categorias modulares de itens
- ✅ Sistema de preços configurável
- ✅ Descontos por tier de doador
- ✅ Comandos VIP
- ✅ Kits especiais
- ✅ Logs detalhados de transações

**Endpoints da API Interna:**
- Integração com `EconomyManager` do Core
- Integração com `DonorManager` do Core

**Schema do Banco de Dados:**
- `economy_logs` (logs de transações)

**Comandos e Permissões:**
- `/shop` - Acessar loja
- `/adminshop` - Comandos admin da loja

**Event Listeners:**
- `ShopListener` - Interações com a loja

**Tarefas Agendadas:**
- Limpeza de cache da loja

**Pontos de Atenção:**
- ✅ Sistema robusto de validação
- ✅ Tratamento de erros melhorado
- ⚠️ Pode afetar balance do PvP

### Módulo 7: Bot Discord (Node.js)

**Estrutura de Pacotes:**
```
primeleague-discord-bot-node/
├── src/
│   ├── commands/      # Comandos Discord
│   ├── database/      # Acesso a dados
│   ├── handlers/      # Handlers de interação
│   └── workers/       # Workers em background
```

**Principais Funcionalidades:**
- ✅ Sistema de autorização de IP via Discord
- ✅ Sistema de notificações
- ✅ Sistema de status do servidor
- ✅ Webhook para pagamentos
- ✅ Sistema de recuperação de conta
- ✅ Comandos slash do Discord

**Endpoints da API Externa:**
- Servidor Express na porta 3000
- Webhook para notificações de pagamento
- Integração com HTTP API do Core

**Schema do Banco de Dados:**
- Acesso direto ao MySQL
- Tabelas: `discord_users`, `discord_links`, `recovery_codes`

**Comandos Discord:**
- Comandos slash para autorização
- Botões interativos para pagamentos
- Sistema de recuperação

**Workers:**
- `NotificationWorker` - Notificações
- `StatusWorker` - Status do servidor

**Pontos de Atenção:**
- ✅ Integração robusta com Discord
- ✅ Sistema de webhook seguro
- ✅ Workers assíncronos

---

## III. Análise da Base de Dados (Schema Global)

### 1. Schema Completo

**Tabelas Principais (SSOT):**
- `player_data` - Dados centrais dos jogadores
- `discord_users` - Dados dos usuários Discord
- `discord_links` - Vínculos Discord-Minecraft

**Tabelas de Sistema:**
- `recovery_codes` - Sistema de recuperação
- `punishments` - Sistema de punições
- `tickets` - Sistema de denúncias
- `staff_vanish` - Modo staff
- `player_authorized_ips` - Autorização P2P
- `whitelist_players` - Whitelist

**Tabelas de Clãs:**
- `clans` - Dados dos clãs
- `clan_players` - Membros dos clãs
- `clan_alliances` - Alianças
- `clan_logs` - Logs de ações

**Tabelas de Comunicação:**
- `chat_logs` - Logs de mensagens

**Tabelas de Auditoria:**
- `economy_logs` - Logs econômicos
- `server_notifications` - Notificações
- `server_stats` - Estatísticas do servidor

### 2. Relações Inter-Módulos

**Chaves Estrangeiras Principais:**
- `player_id` como chave central em todas as tabelas
- `discord_id` para integração Discord
- `clan_id` para sistema de clãs
- Relacionamentos bem definidos com CASCADE/SET NULL

### 3. Estratégia de Indexação

**Índices de Performance:**
- Índices em `player_id` em todas as tabelas
- Índices em `status` para consultas de estado
- Índices em `timestamp` para consultas temporais
- Índices compostos para consultas complexas

---

## IV. Análise de Dependências e Stack Técnica

### 1. Dependências Externas

**Java (Módulos Bukkit):**
- Bukkit API 1.5.2-R1.0
- HikariCP-java7 2.4.13 (pool de conexões)
- MySQL Connector Java 5.1.49
- SLF4J 1.7.25 (logging)
- JBCrypt 0.4 (hash seguro)

**Node.js (Bot Discord):**
- Discord.js (cliente Discord)
- Express (servidor web)
- MySQL2 (driver MySQL)
- Dotenv (variáveis de ambiente)

### 2. Versão Java e Bukkit API

**Compatibilidade:**
- ✅ Core: Java 1.7 + Bukkit 1.5.2
- ✅ P2P: Java 1.8 + Bukkit 1.5.2
- ✅ Todos os módulos: API 1.5.2
- ✅ HikariCP versão compatível com Java 7

---

## V. Conclusões e Recomendações

### Pontos Fortes da Arquitetura

1. **Modularidade Excelente**
   - Separação clara de responsabilidades
   - Comunicação via API bem definida
   - Baixo acoplamento entre módulos

2. **Sistema de Registries Robusto**
   - Padrão Service Registry bem implementado
   - Injeção de dependência adequada
   - Comunicação inter-módulo eficiente

3. **Integração Discord Sólida**
   - Bot Node.js bem estruturado
   - Webhook seguro para pagamentos
   - Sistema de recuperação de conta

4. **Base de Dados Bem Projetada**
   - Schema normalizado
   - Índices de performance
   - Relacionamentos bem definidos

### Pontos de Atenção

1. **Operações de I/O na Thread Principal**
   - Algumas operações de banco podem bloquear
   - Recomendação: Migrar para operações assíncronas

2. **Cache em Memória**
   - Caches não persistem entre restarts
   - Recomendação: Implementar persistência

3. **AdminShop e Balance**
   - Itens pagos podem afetar PvP
   - Recomendação: Revisar balance dos itens

### Recomendações de Melhoria

1. **Performance**
   - Implementar mais operações assíncronas
   - Otimizar consultas de banco
   - Implementar cache distribuído

2. **Segurança**
   - Reforçar validação de entrada
   - Implementar rate limiting mais robusto
   - Auditoria mais detalhada

3. **Monitoramento**
   - Implementar métricas de performance
   - Logs estruturados
   - Alertas automáticos

### Conformidade com Filosofia

**✅ ALINHADO:**
- Sistema competitivo baseado em ELO
- PvP 1.5.2 sem modificações de meta
- Economia baseada em PvP
- Sistema de clãs para competição

**⚠️ ATENÇÃO:**
- AdminShop pode introduzir vantagens
- Sistema de doadores com benefícios
- Necessário monitorar impacto no balance

---

## VI. Status Geral do Projeto

### Módulos Implementados (7/12)
1. ✅ **Core** - Completo e funcional
2. ✅ **P2P** - Completo e funcional
3. ✅ **Admin** - Completo e funcional
4. ✅ **Chat** - Completo e funcional
5. ✅ **Clans** - Completo e funcional
6. ✅ **AdminShop** - Completo e funcional
7. ✅ **Discord Bot** - Completo e funcional

### Módulos Pendentes (5/12)
8. ⏳ **Territórios** - Não implementado
9. ⏳ **Comandos Essenciais** - Parcialmente no Core
10. ⏳ **Lojas de Jogadores** - Não implementado
11. ⏳ **Placar de Estatísticas** - Parcialmente implementado
12. ⏳ **Prevenção de Combat Log** - Não implementado
13. ⏳ **Eventos Automatizados** - Não implementado

### Qualidade Geral
- **Arquitetura:** Excelente (9/10)
- **Código:** Bom (8/10)
- **Documentação:** Adequada (7/10)
- **Testes:** Limitada (5/10)
- **Performance:** Boa (8/10)
- **Segurança:** Boa (8/10)

---

**Este relatório serve como a única fonte da verdade (SSOT) sobre o estado atual do ecossistema Prime League. Todas as decisões arquiteturais futuras devem ser baseadas nesta análise.**
