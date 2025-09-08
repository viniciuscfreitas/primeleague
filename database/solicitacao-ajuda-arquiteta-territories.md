# **SOLICITAÇÃO DE AJUDA TÉCNICA - IA ARQUITETA**

**Data:** 07 de Setembro de 2025  
**De:** Equipe de Desenvolvimento  
**Para:** IA Arquiteta do Prime League  
**Assunto:** Erro Crítico no Módulo de Territórios - NoSuchMethodError e Falha na Inicialização  

---

## **CONTEXTO DA SITUAÇÃO**

O **Módulo de Territórios** foi implementado com funcionalidade real (removendo placeholders) e está compilando perfeitamente. O JAR foi gerado e instalado no servidor. No entanto, o módulo está **falhando ao carregar** devido a um erro de método não encontrado, impedindo que qualquer comando funcione.

---

## **ERRO PRINCIPAL IDENTIFICADO**

### **NoSuchMethodError na Inicialização**
```
java.lang.NoSuchMethodError: br.com.primeleague.core.api.PrimeLeagueAPI.getClanServiceRegistry()Lbr/com/primeleague/api/ClanService;
        at br.com.primeleague.territories.PrimeLeagueTerritories.initializeManagers(PrimeLeagueTerritories.java:143)
        at br.com.primeleague.territories.PrimeLeagueTerritories.onEnable(PrimeLeagueTerritories.java:63)
```

**Detalhes do Erro:**
- **Método:** `PrimeLeagueAPI.getClanServiceRegistry()`
- **Retorno Esperado:** `br.com.primeleague.api.ClanService`
- **Problema:** Método não encontrado ou assinatura incorreta
- **Impacto:** Módulo não carrega, comandos não funcionam

---

## **ANÁLISE TÉCNICA DETALHADA**

### **1. Estado Atual do Sistema**
- **Core:** Carregado com sucesso (linha 198)
- **Clans:** Carregado com sucesso (linha 290)
- **Territories:** **FALHA na inicialização** (linha 304-318)
- **Outros módulos:** Funcionando normalmente

### **2. Implementações Realizadas**
- ✅ **EconomyServiceImpl:** Criado e registrado no Core
- ✅ **Injeção de Dependência:** Implementada nos managers
- ✅ **APIs Reais:** Conectadas (ClanService, EconomyService)
- ✅ **HikariDataSource:** Ativo e funcional
- ❌ **Carregamento do Módulo:** FALHA

### **3. Problema de Compatibilidade**
O erro indica que o método `getClanServiceRegistry()` não existe na versão atual da `PrimeLeagueAPI`, ou tem uma assinatura diferente da esperada.

---

## **LOGS DETALHADOS DO SERVIDOR**

### **Inicialização Bem-Sucedida (Core e Clans)**
```
19:47:23 [INFORMAÇÕES] [PrimeLeague-Core] [Core] PrimeLeague Core habilitado
19:47:23 [INFORMAÇÕES] [PrimeLeague-Clans] [Clans] PrimeLeague Clans habilitado
```

### **Falha na Inicialização (Territories)**
```
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] Enabling PrimeLeague-Territories v1.0.0
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] === INICIANDO MÓDULO DE TERRITÓRIOS ===
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] ✓ Todas as dependências encontradas
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] MessageManager carregado com 56 mensagens.
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] ✓ MessageManager inicializado
19:47:23 [INFORMAÇÕES] [PrimeLeague-Territories] ✓ Inicializando managers com injeção de dependência...
19:47:23 [GRAVE] Error occurred while enabling PrimeLeague-Territories v1.0.0 (Is it up to date?)
```

### **Comportamento dos Comandos**
```
19:49:21 [INFORMAÇÕES] vinicff issued server command: /territory
19:49:23 [INFORMAÇÕES] vinicff issued server command: /territory claim
```
**Resultado:** Comandos executados mas **sem resposta** (módulo não carregado)

---

## **CÓDIGO PROBLEMÁTICO IDENTIFICADO**

### **Arquivo:** `PrimeLeagueTerritories.java` (linha 143)
```java
// Obter ClanService da API (pode ser null inicialmente)
br.com.primeleague.api.ClanService clanService = 
    br.com.primeleague.core.api.PrimeLeagueAPI.getClanServiceRegistry();
```

### **Método Esperado vs Realidade**
- **Esperado:** `getClanServiceRegistry() : ClanService`
- **Realidade:** Método não existe ou tem assinatura diferente

---

## **VERIFICAÇÕES REALIZADAS**

### **1. API do Core**
- ✅ `PrimeLeagueAPI` existe e está acessível
- ❌ `getClanServiceRegistry()` não encontrado
- ✅ `ClanServiceRegistry.getInstance()` existe

### **2. Registro de Serviços**
- ✅ `ClanService` registrado no módulo Clans (linha 45)
- ✅ `EconomyService` registrado no Core
- ❌ Acesso via `PrimeLeagueAPI` falha

### **3. Compilação**
- ✅ Módulo compila sem erros
- ✅ JAR gerado com sucesso (86,641 bytes)
- ❌ Falha na execução/runtime

---

## **PERGUNTAS ESPECÍFICAS PARA A IA ARQUITETA**

### **1. API do Core**
- **Qual é o método correto para obter o ClanService via PrimeLeagueAPI?**
- **Existe uma diferença entre a API compilada e a API em runtime?**
- **Devemos usar `ClanServiceRegistry.getInstance()` diretamente?**

### **2. Compatibilidade de Versões**
- **O módulo de Territórios está usando uma versão incompatível da API?**
- **Precisamos recompilar o Core com as novas implementações?**
- **Existe um problema de classpath ou dependências?**

### **3. Arquitetura de Serviços**
- **Qual é o padrão correto para acessar serviços entre módulos?**
- **Devemos usar registries diretamente ou sempre via PrimeLeagueAPI?**
- **Existe um problema na ordem de carregamento dos módulos?**

### **4. Resolução Imediata**
- **Como corrigir o acesso ao ClanService no módulo de Territórios?**
- **Precisamos modificar o código ou recompilar dependências?**
- **Qual é a abordagem mais robusta para evitar este problema?**

---

## **INFORMAÇÕES TÉCNICAS ADICIONAIS**

### **Arquivos Modificados Recentemente**
- ✅ `EconomyServiceImpl.java` - Criado no Core
- ✅ `PrimeLeagueCore.java` - Registro do EconomyService
- ✅ `TerritoryManager.java` - Injeção de dependência
- ✅ `WarManager.java` - Injeção de dependência
- ✅ `PrimeLeagueTerritories.java` - Inicialização com APIs reais

### **Dependências e Versões**
- **Core:** PrimeLeague-Core v1.0.0 (carregado)
- **Clans:** PrimeLeague-Clans v1.0.0 (carregado)
- **Territories:** PrimeLeague-Territories v1.0.0 (falha)
- **Bukkit:** 1.5.2-R1.1-SNAPSHOT

### **Estrutura de Compilação**
- **Maven:** Compilação bem-sucedida
- **JAR:** Gerado e copiado para plugins/
- **Runtime:** Falha na inicialização

---

## **IMPACTO NO SISTEMA**

### **Funcionalidades Afetadas**
- ❌ **Comandos de Território:** Não funcionam
- ❌ **Sistema de Guerra:** Não disponível
- ❌ **Integração com Clãs:** Falha
- ❌ **Sistema Econômico:** Não conectado

### **Funcionalidades Preservadas**
- ✅ **Core:** Funcionando normalmente
- ✅ **Clans:** Funcionando normalmente
- ✅ **Economy:** Funcionando normalmente
- ✅ **Outros módulos:** Funcionando normalmente

---

## **SOLICITAÇÃO DE AJUDA**

**Precisamos de orientação técnica específica sobre:**

1. **Como corrigir o NoSuchMethodError** no acesso ao ClanService
2. **Qual é a API correta** para acessar serviços entre módulos
3. **Se precisamos recompilar o Core** com as novas implementações
4. **Qual é a abordagem arquitetural recomendada** para evitar este problema

**O módulo está tecnicamente correto e compilando, mas há uma incompatibilidade na API que impede o carregamento em runtime.**

---

## **PRIORIDADE**

**🔴 CRÍTICA** - O módulo de Territórios é essencial para o funcionamento do sistema e está completamente inoperante.

---

**Aguardo sua orientação técnica para resolver este problema crítico de compatibilidade de API.**

**Equipe de Desenvolvimento**  
**Prime League Project**
