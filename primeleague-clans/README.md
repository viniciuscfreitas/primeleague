# Prime League Clans

Sistema de Clãs para o servidor Prime League - Minecraft 1.5.2

## Visão Geral

O módulo de Clãs é um componente fundamental do sistema Prime League, permitindo que jogadores se organizem em grupos competitivos. Este módulo foi desenvolvido com baixo acoplamento inicial, permitindo desenvolvimento paralelo enquanto o Core resolve bugs de fundação.

## Características

- ✅ **Criação de Clãs**: Sistema completo de criação com validação
- ✅ **Gerenciamento de Membros**: Adição, remoção e controle de cargos
- ✅ **Chat do Clã**: Comunicação interna entre membros
- ✅ **Sistema de Permissões**: Controle granular de ações
- ✅ **Armazenamento em Memória**: Funcionalidade completa sem dependência do banco
- ✅ **Promoção/Rebaixamento**: Sistema completo de hierarquia de cargos
- ✅ **Sincronização de Estado**: Gerenciamento consistente entre objetos
- ✅ **Validações Robusta**: Verificações de integridade de dados
- ✅ **Contagem Correta**: Informações precisas de membros
- ✅ **Sistema de Convites**: Completo com expiração automática
- ✅ **Limpeza Automática**: Remoção de convites expirados
- ✅ **Feedback Específico**: Mensagens detalhadas para todas as operações
- ✅ **Notificações Globais**: Eventos do clã são notificados a todos os membros
- ✅ **Logs de Segurança**: Monitoramento de tentativas de ações inválidas
- ✅ **Enums de Resultado**: Sistema robusto de retorno de operações
- 🔄 **Integração com Core**: Preparado para futura integração

## Estrutura do Projeto

```
primeleague-clans/
├── src/main/java/br/com/primeleague/clans/
│   ├── PrimeLeagueClans.java          # Classe principal do plugin
│   ├── model/
│   │   ├── Clan.java                  # Modelo de dados do clã
│   │   └── ClanPlayer.java            # Jogador no contexto do clã
│   ├── manager/
│   │   ├── ClanManager.java           # Gerenciador principal
│   │   ├── PromoteResult.java         # Resultados de promoção
│   │   ├── DemoteResult.java          # Resultados de rebaixamento
│   │   └── KickResult.java            # Resultados de expulsão
│   ├── commands/
│   │   ├── ClanCommand.java           # Comando /clan
│   │   └── ClanChatCommand.java       # Comando /c
│   ├── listeners/                     # Event listeners (futuro)
│   └── api/                          # API para outros módulos (futuro)
├── src/main/resources/
│   └── plugin.yml                     # Configuração do plugin
├── pom.xml                           # Configuração Maven
└── README.md                         # Esta documentação
```

## Comandos Disponíveis

### Comando Principal: `/clan`

| Subcomando | Descrição | Permissão |
|------------|-----------|-----------|
| `create <tag> <nome>` | Criar um novo clã | `primeleague.clans.create` |
| `invite <jogador>` | Convidar jogador para o clã | `primeleague.clans.invite` |
| `accept` | Aceitar convite para clã | `primeleague.clans.use` |
| `deny` | Recusar convite para clã | `primeleague.clans.use` |
| `kick <jogador>` | Expulsar membro do clã | `primeleague.clans.kick` |
| `promote <jogador>` | Promover membro | `primeleague.clans.promote` |
| `demote <jogador>` | Rebaixar membro | `primeleague.clans.demote` |
| `leave` | Sair do clã | `primeleague.clans.use` |
| `disband` | Dissolver o clã | `primeleague.clans.disband` |
| `info [tag]` | Informações do clã | `primeleague.clans.use` |
| `help` | Mostrar ajuda | `primeleague.clans.use` |

### Chat do Clã: `/c`

| Uso | Descrição | Permissão |
|-----|-----------|-----------|
| `/c <mensagem>` | Enviar mensagem para o clã | `primeleague.clans.chat` |

## Sistema de Cargos

### Hierarquia de Cargos

1. **Líder** (`LEADER`)
   - Pode dissolver o clã
   - Pode promover/rebaixar membros
   - Pode convidar e expulsar membros
   - Não pode sair do clã (deve dissolver)

2. **Oficial** (`OFFICER`)
   - Pode convidar novos membros
   - Pode expulsar membros
   - Pode usar chat do clã

3. **Membro** (`MEMBER`)
   - Pode usar chat do clã
   - Pode sair do clã

## Validações e Regras

### Criação de Clãs
- Tag deve ter no máximo 5 caracteres
- Nome deve ter no máximo 32 caracteres
- Tag e nome devem ser únicos
- Jogador não pode pertencer a outro clã

### Gerenciamento de Membros
- Apenas oficiais podem convidar/expulsar
- Apenas líderes podem promover/rebaixar
- Líderes não podem sair do clã
- Jogadores não podem pertencer a múltiplos clãs

## Arquitetura Técnica

### Armazenamento Temporário
Atualmente, o sistema utiliza armazenamento em memória com as seguintes estruturas:

- `Map<String, Clan> clansByTag`: Busca rápida por tag
- `Map<String, Clan> clansByName`: Busca rápida por nome
- `Map<String, ClanPlayer> playersByName`: Jogadores por nome
- `Map<UUID, ClanPlayer> playersByUUID`: Jogadores por UUID
- `Map<UUID, ClanInvitation> pendingInvites`: Convites pendentes

### Sistema de Feedback Robusto
O módulo implementa um sistema de enums para fornecer feedback específico:

- `PromoteResult`: Resultados detalhados de operações de promoção
- `DemoteResult`: Resultados detalhados de operações de rebaixamento  
- `KickResult`: Resultados detalhados de operações de expulsão

Cada operação retorna um enum específico, permitindo mensagens de erro precisas e informativas para o usuário.

### Preparação para Integração com Core
O módulo foi projetado para facilitar a futura integração com o Core:

```java
// API prevista para o Core
core.getClanManager().getClanByPlayer(Player player)
core.getClanManager().getClanByTag(String tag)
core.getClanManager().saveClan(Clan clan)
core.getClanManager().deleteClan(Clan clan)
core.getClanManager().saveClanPlayer(ClanPlayer clanPlayer)
```

## Estrutura de Banco de Dados (Futura)

### Tabela: `clans`
| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `id` | `INT AUTO_INCREMENT` | Chave Primária |
| `tag` | `VARCHAR(5)` | Tag do clã (única) |
| `name` | `VARCHAR(32)` | Nome do clã (único) |
| `leader_uuid` | `VARCHAR(36)` | UUID do líder |
| `creation_date` | `TIMESTAMP` | Data de criação |

### Tabela: `clan_players`
| Coluna | Tipo | Descrição |
|--------|------|-----------|
| `player_uuid` | `VARCHAR(36)` | UUID do jogador (PK) |
| `clan_id` | `INT` | ID do clã (FK) |
| `role` | `TINYINT` | Cargo (0: Membro, 1: Oficial, 2: Líder) |
| `join_date` | `TIMESTAMP` | Data de entrada |

## Compilação e Instalação

### Pré-requisitos
- Java 7 ou superior
- Maven 3.x
- Bukkit/Spigot 1.5.2

### Compilação
```bash
cd primeleague-clans
mvn clean package
```

### Instalação
1. Copie o arquivo `target/primeleague-clans-1.0.0.jar` para a pasta `plugins/`
2. Reinicie o servidor
3. Configure as permissões conforme necessário

## Status Atual do Desenvolvimento

### ✅ Concluído (v1.0.0)
- [x] **Estrutura básica do projeto** - Arquitetura modular e escalável
- [x] **Modelos de dados** - Clan, ClanPlayer, ClanInvitation, ClanRelation
- [x] **Sistema de gerenciamento** - ClanManager com lógica de negócios completa
- [x] **Comandos principais** - /clan e /c com todas as funcionalidades
- [x] **Sistema de convites** - Completo com expiração automática
- [x] **Sistema de cargos e permissões** - Hierarquia robusta com validações
- [x] **Chat do clã** - Comunicação interna entre membros
- [x] **Feedback específico** - Mensagens detalhadas para todas as operações
- [x] **Notificações globais** - Eventos do clã são notificados a todos os membros
- [x] **Logs de segurança** - Monitoramento de tentativas de ações inválidas
- [x] **Enums de resultado** - Sistema robusto de retorno de operações
- [x] **Sistema de KDR** - Kill/Death Ratio com persistência
- [x] **Controle de Fogo Amigo** - Toggle de dano entre membros
- [x] **Sistema de Alianças e Rivalidades** - Relações entre clãs
- [x] **Integração completa com PrimeLeague-Core** - DAO pattern implementado
- [x] **Persistência MySQL** - MySqlClanDAO com todas as operações CRUD
- [x] **Sistema de dependências** - Módulos conectados via plugin.yml

### 🔄 Melhorias Futuras (v1.1+)
- [ ] Sistema de convites pendentes (interface melhorada)
- [ ] Limpeza automática de dados inativos (otimização de memória)
- [ ] Eventos customizados para outros módulos
- [ ] API pública para extensões

### 📋 Planejado (v2.0+)
- [ ] Integração com sistema de territórios
- [ ] Sistema de eventos de clã
- [ ] Rankings e estatísticas avançadas
- [ ] Sistema de conquistas de clã

## Desenvolvimento

### Próximos Passos
1. **Testes de Integração**: Validar funcionamento completo com Core
2. **Documentação de API**: Documentar interfaces para outros módulos
3. **Otimizações de Performance**: Refinamentos baseados em uso real
4. **Extensões Avançadas**: Novas funcionalidades baseadas em feedback
5. **Módulo de Territórios**: Desenvolver sistema de territórios (depende deste módulo)

### Contribuição
Para contribuir com o desenvolvimento:

1. Siga o padrão de código estabelecido
2. Adicione documentação JavaDoc
3. Teste as funcionalidades antes de submeter
4. Mantenha compatibilidade com Minecraft 1.5.2

## Licença

Este módulo faz parte do sistema Prime League e segue as mesmas diretrizes de licenciamento do projeto principal.

---

**Versão**: 1.0.0  
**Última Atualização**: Dezembro 2024  
**Compatibilidade**: Minecraft 1.5.2, Bukkit/Spigot
