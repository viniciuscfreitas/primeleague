# **SOLICITAÇÃO DE AJUDA TÉCNICA - IA ARQUITETA**

**Data:** 07 de Setembro de 2025  
**De:** Equipe de Desenvolvimento  
**Para:** IA Arquiteta do Prime League  
**Assunto:** Implementação de Testes Unitários Rigorosos para o Módulo de Territórios  

---

## **CONTEXTO DA SITUAÇÃO**

O **Módulo de Territórios** foi implementado com sucesso e os erros críticos de produção foram corrigidos. O sistema agora está funcional com placeholders inteligentes que garantem estabilidade até que todos os serviços externos estejam disponíveis.

**Status Atual:**
- ✅ Módulo compilando sem erros
- ✅ Deploy realizado com sucesso
- ✅ Erros de integração de API corrigidos
- ✅ Sistema funcionando com placeholders seguros

---

## **SOLICITAÇÃO PRINCIPAL**

**Precisamos implementar uma suíte completa de testes unitários que permita validar todas as funcionalidades do módulo de Territórios sem necessidade de executar o servidor Minecraft.**

### **OBJETIVOS DOS TESTES**

1. **Validação Completa de Funcionalidades**
   - Sistema de claim/unclaim de territórios
   - Sistema de guerra e cercos
   - Sistema de economia territorial
   - Sistema de manutenção automática
   - Sistema de proteção de territórios

2. **Testes de Integração**
   - Comunicação com módulo de Clãs
   - Comunicação com sistema de economia
   - Persistência no banco de dados
   - Validação de permissões

3. **Testes de Performance**
   - Operações assíncronas
   - Cache de territórios
   - Queries de banco de dados
   - Processamento de eventos

4. **Testes de Segurança**
   - Validação de permissões
   - Prevenção de SQL injection
   - Validação de entrada de dados
   - Proteção contra exploits

---

## **ARQUITETURA ATUAL DO MÓDULO**

### **Componentes Principais**
- **TerritoryManager**: Gerencia claims, cache e manutenção
- **WarManager**: Gerencia guerras, cercos e pilhagem
- **MySqlTerritoryDAO**: Persistência no banco de dados
- **TerritoryCommand**: Comandos de território
- **WarCommand**: Comandos de guerra
- **SiegeListener**: Eventos de cerco
- **TerritoryProtectionListener**: Proteção de territórios

### **Dependências Externas**
- **ClanService**: Para dados de clãs
- **EconomyService**: Para transações econômicas
- **IdentityService**: Para mapeamento de jogadores

---

## **REQUISITOS ESPECÍFICOS DOS TESTES**

### **1. Testes Unitários por Componente**

#### **TerritoryManager**
- [ ] Teste de claim de território válido
- [ ] Teste de claim de território já ocupado
- [ ] Teste de unclaim de território
- [ ] Teste de verificação de permissões
- [ ] Teste de cálculo de custos de manutenção
- [ ] Teste de verificação de moral vs territórios
- [ ] Teste de cache de territórios
- [ ] Teste de limpeza de cache

#### **WarManager**
- [ ] Teste de declaração de guerra válida
- [ ] Teste de declaração de guerra inválida
- [ ] Teste de início de cerco
- [ ] Teste de mecânica de contestação
- [ ] Teste de vitória em cerco
- [ ] Teste de derrota em cerco
- [ ] Teste de fase de pilhagem
- [ ] Teste de notificações de guerra

#### **MySqlTerritoryDAO**
- [ ] Teste de inserção de território
- [ ] Teste de consulta de território
- [ ] Teste de atualização de território
- [ ] Teste de exclusão de território
- [ ] Teste de consultas complexas
- [ ] Teste de transações
- [ ] Teste de rollback em caso de erro
- [ ] Teste de performance de queries

### **2. Testes de Integração**

#### **Integração com Clãs**
- [ ] Teste de obtenção de clã do jogador
- [ ] Teste de verificação de permissões de clã
- [ ] Teste de obtenção de moral do clã
- [ ] Teste de fallback para placeholders

#### **Integração com Economia**
- [ ] Teste de verificação de saldo
- [ ] Teste de débito de valores
- [ ] Teste de crédito de valores
- [ ] Teste de transações falhadas

#### **Integração com Banco de Dados**
- [ ] Teste de conexão com banco
- [ ] Teste de criação de tabelas
- [ ] Teste de migração de dados
- [ ] Teste de backup e restore

### **3. Testes de Cenários Complexos**

#### **Cenários de Guerra**
- [ ] Teste de guerra entre múltiplos clãs
- [ ] Teste de alianças em guerra
- [ ] Teste de tréguas ativas
- [ ] Teste de guerra simultânea em múltiplos territórios

#### **Cenários de Economia**
- [ ] Teste de clã sem saldo para manutenção
- [ ] Teste de clã com saldo insuficiente
- [ ] Teste de múltiplas transações simultâneas
- [ ] Teste de inflação de custos

#### **Cenários de Performance**
- [ ] Teste com 1000+ territórios
- [ ] Teste com 100+ jogadores simultâneos
- [ ] Teste de stress de banco de dados
- [ ] Teste de memory leaks

---

## **ESTRUTURA DE TESTES SOLICITADA**

### **1. Framework de Testes**
- **JUnit 4/5**: Para testes unitários
- **Mockito**: Para mocks de dependências
- **H2 Database**: Para testes de banco em memória
- **TestContainers**: Para testes de integração
- **AssertJ**: Para assertions mais legíveis

### **2. Estrutura de Diretórios**
```
src/test/java/
├── unit/
│   ├── manager/
│   │   ├── TerritoryManagerTest.java
│   │   └── WarManagerTest.java
│   ├── dao/
│   │   └── MySqlTerritoryDAOTest.java
│   ├── commands/
│   │   ├── TerritoryCommandTest.java
│   │   └── WarCommandTest.java
│   └── listeners/
│       ├── SiegeListenerTest.java
│       └── TerritoryProtectionListenerTest.java
├── integration/
│   ├── ClanIntegrationTest.java
│   ├── EconomyIntegrationTest.java
│   └── DatabaseIntegrationTest.java
├── performance/
│   ├── TerritoryPerformanceTest.java
│   └── WarPerformanceTest.java
└── scenarios/
    ├── WarScenariosTest.java
    ├── EconomyScenariosTest.java
    └── ComplexScenariosTest.java
```

### **3. Mocks e Stubs Necessários**
- **MockClanService**: Simula respostas do módulo de Clãs
- **MockEconomyService**: Simula transações econômicas
- **MockIdentityService**: Simula mapeamento de jogadores
- **MockBukkitServer**: Simula ambiente Bukkit
- **MockPlayer**: Simula jogadores
- **MockWorld**: Simula mundos do Minecraft

---

## **CASOS DE TESTE ESPECÍFICOS SOLICITADOS**

### **1. Testes de Claim de Território**
```java
@Test
public void testClaimTerritory_ValidPlayer_ValidLocation_ShouldSucceed()

@Test
public void testClaimTerritory_PlayerWithoutClan_ShouldFail()

@Test
public void testClaimTerritory_InsufficientMoral_ShouldFail()

@Test
public void testClaimTerritory_TerritoryAlreadyClaimed_ShouldFail()

@Test
public void testClaimTerritory_InsufficientFunds_ShouldFail()
```

### **2. Testes de Guerra**
```java
@Test
public void testDeclareWar_ValidTarget_ShouldSucceed()

@Test
public void testDeclareWar_TargetNotVulnerable_ShouldFail()

@Test
public void testDeclareWar_InsufficientFunds_ShouldFail()

@Test
public void testDeclareWar_ActiveTruce_ShouldFail()

@Test
public void testStartSiege_ValidWar_ShouldSucceed()
```

### **3. Testes de Economia**
```java
@Test
public void testMaintenanceCost_OneTerritory_ShouldBeBaseCost()

@Test
public void testMaintenanceCost_MultipleTerritories_ShouldBeExponential()

@Test
public void testMaintenancePayment_SufficientFunds_ShouldSucceed()

@Test
public void testMaintenancePayment_InsufficientFunds_ShouldRemoveTerritory()
```

---

## **MÉTRICAS DE QUALIDADE SOLICITADAS**

### **1. Cobertura de Código**
- **Mínimo**: 90% de cobertura de linhas
- **Mínimo**: 85% de cobertura de branches
- **Mínimo**: 80% de cobertura de métodos

### **2. Performance**
- **Máximo**: 100ms para operações de claim
- **Máximo**: 50ms para consultas de cache
- **Máximo**: 500ms para operações de banco

### **3. Confiabilidade**
- **Mínimo**: 99.9% de sucesso em testes
- **Máximo**: 0.1% de falsos positivos
- **Máximo**: 0.1% de falsos negativos

---

## **FERRAMENTAS E CONFIGURAÇÕES**

### **1. Maven Configuration**
```xml
<dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>3.12.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>1.4.200</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### **2. Configuração de Testes**
- **Banco de dados**: H2 em memória para testes
- **Mocks**: Todos os serviços externos mockados
- **Dados de teste**: Conjunto padronizado de dados
- **Cleanup**: Limpeza automática entre testes

---

## **ENTREGÁVEIS ESPERADOS**

### **1. Código de Testes**
- [ ] Suíte completa de testes unitários
- [ ] Testes de integração
- [ ] Testes de performance
- [ ] Testes de cenários complexos

### **2. Documentação**
- [ ] README com instruções de execução
- [ ] Documentação de casos de teste
- [ ] Guia de manutenção dos testes
- [ ] Relatório de cobertura

### **3. Configuração**
- [ ] Arquivos de configuração Maven
- [ ] Scripts de execução de testes
- [ ] Configuração de CI/CD
- [ ] Dados de teste padronizados

---

## **CRONOGRAMA SOLICITADO**

- **Fase 1** (1-2 dias): Estrutura base e testes unitários simples
- **Fase 2** (2-3 dias): Testes de integração e mocks
- **Fase 3** (1-2 dias): Testes de performance e cenários complexos
- **Fase 4** (1 dia): Documentação e configuração final

---

## **JUSTIFICATIVA TÉCNICA**

### **Por que precisamos desses testes?**

1. **Qualidade de Código**: Garantir que todas as funcionalidades funcionem corretamente
2. **Refatoração Segura**: Permitir mudanças no código sem quebrar funcionalidades
3. **Documentação Viva**: Os testes servem como documentação do comportamento esperado
4. **Detecção Precoce**: Identificar bugs antes que cheguem à produção
5. **Confiança**: Permitir deploy com confiança de que o sistema funciona

### **Benefícios Esperados**

- **Redução de 90%** nos bugs em produção
- **Aumento de 80%** na velocidade de desenvolvimento
- **Melhoria de 95%** na confiabilidade do sistema
- **Facilitação de 100%** na manutenção do código

---

## **SOLICITAÇÃO FINAL**

**Precisamos de uma suíte de testes unitários completa, rigorosa e profissional que permita validar todas as funcionalidades do módulo de Territórios sem necessidade de executar o servidor Minecraft.**

**Os testes devem ser:**
- ✅ **Completos**: Cobrindo todas as funcionalidades
- ✅ **Rigorosos**: Validando cenários complexos
- ✅ **Performáticos**: Executando rapidamente
- ✅ **Confiáveis**: Sem falsos positivos/negativos
- ✅ **Manuteníveis**: Fáceis de atualizar e expandir

**Aguardo sua orientação técnica e implementação dos testes solicitados.**

---

**Equipe de Desenvolvimento**  
**Prime League Project**  
**Data:** 07 de Setembro de 2025
