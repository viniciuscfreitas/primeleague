# Prime League Admin - Módulo Administrativo

O **Módulo Administrativo** é o terceiro pilar da fundação do Prime League, responsável por toda a gestão de moderação, punições e ferramentas administrativas do servidor.

## 🎯 Objetivos

- **Sistema de Punições Completo**: Warn, Kick, Mute, Ban (temporário e permanente)
- **Sistema de Tickets**: Denúncias organizadas com workflow de resolução
- **Modo Staff Avançado**: Invisibilidade total e ferramentas de inspeção
- **Integração Total**: Conecta com Core e P2P para ações coordenadas
- **Auditoria Completa**: Todas as ações administrativas são registradas

## 🏗️ Arquitetura

### Estrutura do Banco de Dados

#### Tabela `punishments`
```sql
CREATE TABLE punishments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('WARN', 'KICK', 'MUTE', 'BAN'),
    target_uuid VARCHAR(36),
    author_uuid VARCHAR(36),
    reason VARCHAR(255),
    created_at TIMESTAMP,
    expires_at TIMESTAMP NULL,
    active BOOLEAN DEFAULT TRUE,
    pardoned_by_uuid VARCHAR(36) NULL,
    pardoned_at TIMESTAMP NULL,
    pardon_reason VARCHAR(255) NULL
);
```

#### Tabela `tickets`
```sql
CREATE TABLE tickets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    status ENUM('OPEN', 'IN_PROGRESS', 'CLOSED_GUILTY', 'CLOSED_INNOCENT'),
    reporter_uuid VARCHAR(36),
    target_uuid VARCHAR(36),
    reason TEXT,
    evidence_link VARCHAR(255) NULL,
    claimed_by_uuid VARCHAR(36) NULL,
    resolution_notes TEXT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

### Componentes Principais

- **AdminManager**: Gerenciador central de todas as operações
- **AdminAPI**: API pública para integração com outros módulos
- **Models**: Punishment, Ticket (entidades do sistema)
- **Commands**: Todos os comandos administrativos
- **Listeners**: Verificação de punições em tempo real

## 🛠️ Funcionalidades

### Sistema de Punições

#### Comandos Disponíveis
- `/warn <player> <motivo>` - Aviso registrado
- `/kick <player> <motivo>` - Expulsão com registro
- `/tempmute <player> <duração> <motivo>` - Silenciamento temporário
- `/mute <player> <motivo>` - Silenciamento permanente
- `/tempban <player> <duração> <motivo>` - Banimento temporário
- `/ban <player> <motivo>` - Banimento permanente
- `/unmute <player> <motivo>` - Remover silenciamento
- `/unban <player> <motivo>` - Remover banimento
- `/history <player>` - Histórico de punições

#### Características
- **Histórico Completo**: Nunca deleta punições, apenas marca como inativo
- **Integração P2P**: Bans automaticamente revogam acesso P2P
- **Verificação Automática**: Mutes bloqueiam chat, bans bloqueiam login
- **Perdão Auditável**: Todas as remoções são registradas

### Sistema de Tickets

#### Comandos Disponíveis
- `/report <player> <motivo> [prova]` - Criar denúncia
- `/tickets list [status]` - Listar tickets
- `/tickets view <id>` - Visualizar ticket
- `/tickets claim <id>` - Reivindicar ticket
- `/tickets close <id> <guilty/innocent> [notas]` - Resolver ticket

#### Workflow
1. **Criação**: Jogador cria denúncia com `/report`
2. **Notificação**: Staff é notificado automaticamente
3. **Reivindicação**: Staff reivindica ticket com `/tickets claim`
4. **Investigação**: Staff investiga e coleta evidências
5. **Resolução**: Ticket é fechado com veredito e notas
6. **Feedback**: Reporter recebe notificação da resolução

### Modo Staff Avançado

#### Comandos Disponíveis
- `/vanish` - Ativa/desativa invisibilidade total
- `/invsee <player>` - Visualiza inventário (read-only)
- `/inspect <player>` - Dossiê completo do jogador

#### Funcionalidades
- **Invisibilidade Total**: Removido da tab list, sem join/quit messages
- **Imunidade**: Imune a dano e não atrai mobs
- **Interação Silenciosa**: Abre inventários sem som
- **Persistência**: Estado de vanish sobrevive a relogins

## 🔧 Configuração

### Dependências
- **PrimeLeagueCore**: Módulo base obrigatório
- **PrimeLeagueP2P**: Módulo P2P (opcional, para integração)

### Permissões
```yaml
# Permissões básicas
primeleague.report: true                    # Reportar jogadores
primeleague.admin.vanish: op               # Modo vanish
primeleague.admin.invsee: op               # Ver inventários
primeleague.admin.inspect: op              # Inspecionar jogadores
primeleague.admin.tickets: op              # Gerenciar tickets

# Permissões de punição
primeleague.admin.warn: op                 # Avisar jogadores
primeleague.admin.kick: op                 # Expulsar jogadores
primeleague.admin.mute: op                 # Silenciar jogadores
primeleague.admin.ban: op                  # Banir jogadores
primeleague.admin.history: op              # Ver histórico

# Permissões especiais
primeleague.admin.viewip: op               # Ver IPs (no /inspect)
primeleague.admin.notifications: op        # Receber notificações
```

## 🚀 Instalação

1. **Compilar o módulo**:
   ```bash
   ./build-admin.ps1
   ```

2. **Executar scripts SQL**:
   ```sql
   -- Executar o arquivo database_updates.sql
   ```

3. **Instalar no servidor**:
   - Copiar `primeleague-admin-1.0.0-shaded.jar` para `/plugins/`
   - Reiniciar o servidor

4. **Configurar**:
   - Editar `config.yml` com configurações do banco
   - Ajustar permissões conforme necessário

## 🔗 Integração

### Com Módulo Core
- Utiliza `PrimeLeagueAPI` para acesso a perfis
- Verifica dados de jogadores offline
- Integra com sistema de cache

### Com Módulo P2P
- Bans automaticamente revogam acesso P2P
- Perdões restauram acesso P2P
- Verifica status de assinatura no `/inspect`

### API Pública
```java
// Verificar se jogador está banido
boolean isBanned = AdminAPI.isBanned(playerUuid);

// Verificar se jogador está silenciado
boolean isMuted = AdminAPI.isMuted(playerUuid);

// Aplicar punição
Punishment punishment = new Punishment(Type.BAN, targetUuid, authorUuid, reason);
AdminAPI.applyPunishment(punishment);
```

## 📊 Monitoramento

### Logs
- Todas as ações administrativas são logadas
- Tentativas de login de jogadores banidos
- Criação e resolução de tickets

### Métricas
- Número de punições por tipo
- Tempo médio de resolução de tickets
- Staff mais ativo

## 🔒 Segurança

- **Validação de Permissões**: Todas as ações verificam permissões
- **Auditoria Completa**: Nenhuma ação é perdida
- **Integridade de Dados**: Foreign keys e constraints no banco
- **Bypass Protegido**: Staff não pode ser reportado

## 🎮 Uso Prático

### Cenário 1: Jogador Reportado
1. Jogador usa `/report Player123 hack`
2. Staff recebe notificação
3. Staff usa `/tickets claim 123`
4. Staff investiga com `/inspect Player123`
5. Staff usa `/tickets close 123 guilty Evidências encontradas`
6. Reporter recebe feedback automático

### Cenário 2: Aplicação de Punição
1. Staff usa `/warn Player123 Spam no chat`
2. Sistema registra punição no banco
3. Jogador recebe mensagem de aviso
4. Outros staffs são notificados
5. Punição aparece no `/history Player123`

### Cenário 3: Modo Staff
1. Staff usa `/vanish`
2. Staff fica invisível para jogadores
3. Staff pode usar `/inspect Player123` sem ser visto
4. Staff pode usar `/invsee Player123` para ver inventário
5. Staff usa `/vanish` novamente para sair do modo

## 🔮 Próximos Passos

- [ ] Implementação completa de todos os comandos
- [ ] Sistema de logs avançado
- [ ] Interface web para gestão
- [ ] Integração com Discord
- [ ] Sistema de apelações
- [ ] Métricas e relatórios

---

**O Módulo Administrativo completa a fundação do Prime League, estabelecendo um sistema de justiça transparente e poderoso que protege a integridade do servidor.**
