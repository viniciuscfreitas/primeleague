# Prime League Territories

## Visão Geral

O **Módulo de Territórios** é um dos componentes centrais do servidor Prime League, implementando um sistema completo de posse territorial, guerra e economia baseado na filosofia "O Coliseu Competitivo". Este módulo transforma a posse de terra em um recurso estratégico, cuja conquista e defesa dependem de habilidade, economia, diplomacia e combate ativo.

## Características Principais

### 🏰 Sistema de Territórios
- **Claiming de Chunks**: Clãs podem reivindicar chunks do mundo como território
- **Proteção Territorial**: Blocos e contêineres protegidos contra não-membros
- **Limite de Territórios**: Sistema baseado em moral para evitar monopólio
- **Manutenção Contínua**: Custo diário para manter territórios

### ⚔️ Sistema de Guerra
- **Guerra Baseada em Moral**: Apenas clãs vulneráveis podem ser atacados
- **Janela de Exclusividade**: 24 horas para iniciar cerco após declaração
- **Altar da Discórdia**: Item especial para iniciar cercos
- **Contestação Tática**: Mecânica dinâmica de controle do timer

### 🏦 Economia Territorial
- **Banco de Clã**: Sistema de economia virtual por clã
- **Custos de Manutenção**: Taxa exponencial baseada no número de territórios
- **Pilhagem**: Recompensas diretas por vitórias em cerco
- **Integração Econômica**: Conectado ao sistema de economia do servidor

### 🤝 Diplomacia
- **Respeito a Alianças**: Sistema de tréguas e pactos de defesa
- **Chamado às Armas**: Aliados podem ajudar em defesas
- **Integração com Clãs**: Comunicação bidirecional com módulo de clãs

## Arquitetura

### Estrutura de Pacotes
```
br.com.primeleague.territories/
├── PrimeLeagueTerritories.java      # Classe principal
├── api/                             # API de integração
│   ├── TerritoryServiceRegistry.java
│   └── TerritoryServiceImpl.java
├── manager/                         # Gerenciadores principais
│   ├── TerritoryManager.java
│   └── WarManager.java
├── commands/                        # Comandos do módulo
│   ├── TerritoryCommand.java
│   └── WarCommand.java
├── listeners/                       # Event listeners
│   ├── TerritoryProtectionListener.java
│   └── SiegeListener.java
├── model/                          # Modelos de dados
│   ├── TerritoryChunk.java
│   ├── ActiveWar.java
│   ├── ActiveSiege.java
│   ├── ClanBank.java
│   └── TerritoryState.java
├── dao/                            # Acesso a dados
│   └── MySqlTerritoryDAO.java
└── integration/                    # Integração com outros módulos
    └── ClanIntegration.java
```

### Dependências
- **PrimeLeague-Core**: API base e serviços essenciais
- **PrimeLeague-Clans**: Sistema de clãs e moral
- **Bukkit 1.5.2**: API do servidor Minecraft

## Comandos

### Territórios
- `/territory claim` - Reivindicar território
- `/territory unclaim` - Remover território
- `/territory info` - Informações do território atual
- `/territory list` - Listar territórios do clã
- `/territory bank [deposit/withdraw] <quantia>` - Gerenciar banco do clã

### Guerra
- `/war declare <clã>` - Declarar guerra
- `/war status` - Status de guerras ativas
- `/war altar` - Criar Altar da Discórdia
- `/war help` - Ajuda dos comandos

## Configuração

### Arquivo `config.yml`
```yaml
# Configurações de territórios
territories:
  maintenance-cost: 100.0          # Custo base de manutenção
  scale-multiplier: 1.2            # Multiplicador exponencial
  max-territories-per-clan: 50     # Limite de territórios

# Configurações de guerra
war:
  declaration-cost: 500.0          # Custo para declarar guerra
  exclusivity-window: 24           # Janela de exclusividade (horas)
  siege-duration: 20               # Duração do cerco (minutos)
  contestation-radius: 5           # Raio da zona de contestação

# Configurações do Altar
altar:
  item-name: "§c§lAltar da Discórdia"
  channeling-duration: 5           # Duração da canalização (segundos)
  creation-cost: 1000.0            # Custo para criar o altar
```

## Banco de Dados

### Tabelas Principais
- `prime_territories` - Territórios reivindicados
- `prime_active_wars` - Guerras ativas
- `prime_clan_bank` - Bancos dos clãs
- `prime_territory_logs` - Logs de ações territoriais
- `prime_active_sieges` - Cercos ativos

### Procedures
- `CheckTerritoryMaintenance()` - Verificação de manutenção
- `CleanupExpiredTerritoryData()` - Limpeza de dados expirados

## Integração com Outros Módulos

### Módulo de Clãs
- Injeção de informações territoriais em `/clan info`
- Logs territoriais em `/clan logs`
- Verificação de vulnerabilidade para ataques

### Módulo de Eventos (Futuro)
- Verificação de territórios para eventos KOTH
- Bônus de moral por conquistas territoriais
- Estado "Fortificado" temporário

### Módulo de Estatísticas (Futuro)
- Ranking de clãs por territórios
- Estatísticas de guerras e cercos
- Métricas de dominação territorial

## Requisitos Funcionais Implementados

### ✅ RF-TERR-01: Sistema de Posse de Território
- Claiming de chunks baseado em moral
- Proteção contra interação de não-membros
- Comandos `/territory claim` e `/territory unclaim`

### ✅ RF-TERR-02: Economia Territorial
- Banco virtual por clã
- Manutenção diária com custo exponencial
- Comandos de depósito e saque

### ✅ RF-WAR-01: Ciclo de Guerra Baseado em Moral
- Estado de vulnerabilidade baseado em moral vs territórios
- Declaração de guerra apenas a clãs vulneráveis
- Janela de exclusividade de 24 horas

### ✅ RF-WAR-02: O Cerco Ativo
- Altar da Discórdia com canalização de 5 segundos
- Timer de 20 minutos para conquista
- Posicionamento em territórios reivindicados

### ✅ RF-WAR-03: Mecânica de Contestação Tática
- Zona de contestação de 5 blocos
- Timer dinâmico baseado em controle da zona
- Verificação a cada segundo

### ✅ RF-WAR-04: Pilhagem e Consequências
- Fase de pilhagem de 5 minutos após vitória
- Reset de contêineres no chunk conquistado
- Recompensas diretas por vitórias

### ✅ RF-DIP-01: Diplomacia em Guerra
- Respeito a tréguas e alianças
- Chamado às armas para aliados
- Integração com sistema diplomático

### ✅ RF-INFO-01: Integração com Sistemas de Informação
- Injeção de informações em `/clan info`
- Logs territoriais em `/clan logs`
- API para mapas web e eventos

## Instalação

1. **Compilar o módulo**:
   ```bash
   cd primeleague-territories
   mvn clean install
   ```

2. **Instalar dependências**:
   - PrimeLeague-Core
   - PrimeLeague-Clans

3. **Configurar banco de dados**:
   ```sql
   SOURCE database/territories-schema.sql;
   ```

4. **Configurar o servidor**:
   - Copiar `primeleague-territories-1.0.0.jar` para `/plugins/`
   - Configurar `config.yml` conforme necessário

5. **Reiniciar o servidor**

## Desenvolvimento

### Estrutura de Desenvolvimento
- **Java 8**: Compatibilidade com Bukkit 1.5.2
- **Maven**: Gerenciamento de dependências
- **HikariCP**: Pool de conexões de banco
- **Async Operations**: Operações de I/O assíncronas

### Padrões de Código
- **Clean Code**: Código limpo e bem documentado
- **SOLID Principles**: Princípios de design orientado a objetos
- **Error Handling**: Tratamento robusto de erros
- **Logging**: Sistema de logs detalhado

## Suporte

Para suporte técnico ou dúvidas sobre o módulo:
- **Documentação**: Este README e comentários no código
- **Logs**: Verificar logs do servidor para erros
- **Configuração**: Validar arquivo `config.yml`
- **Banco de Dados**: Verificar integridade das tabelas

## Licença

Este módulo é parte do projeto Prime League e segue as mesmas diretrizes de licenciamento do projeto principal.

---

**Versão**: 1.0.0  
**Data**: 28 de Janeiro de 2025  
**Autor**: Prime League Team  
**Status**: Implementação Completa
