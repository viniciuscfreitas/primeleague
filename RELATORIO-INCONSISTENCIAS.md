# Relatório de Inconsistências - PrimeLeague

## 📋 Análise de Consistência entre Módulos

### 🚨 **INCONSISTÊNCIAS CRÍTICAS ENCONTRADAS**

## 1. **Versões do Java Inconsistentes**

### ❌ **Problema**: Diferentes versões do Java sendo usadas
- **primeleague-core**: Java 1.8
- **primeleague-p2p**: Java 1.7
- **primeleague-chat**: Java 1.7
- **primeleague-clans**: Java 1.7
- **primeleague-admin**: Java 1.7
- **primeleague-adminshop**: Java 1.7

### ✅ **Solução**: Padronizar para Java 1.7 (compatível com Minecraft 1.5.2)

## 2. **Versões do Bukkit API Inconsistentes**

### ❌ **Problema**: Diferentes versões da API sendo referenciadas
- **primeleague-core**: `api-version: 1.5.2` (plugin.yml)
- **primeleague-p2p**: `api-version: 1.13` (plugin.yml)
- **Todos os módulos**: `bukkit-1.5.2-R1.0.jar` (pom.xml)

### ✅ **Solução**: Padronizar para `api-version: 1.5.2` em todos os plugin.yml

## 3. **Dependências do HikariCP Inconsistentes**

### ❌ **Problema**: Diferentes versões do HikariCP
- **primeleague-core**: `HikariCP 4.0.3`
- **primeleague-chat**: `HikariCP-java7 2.4.13`
- **primeleague-admin**: `HikariCP-java7 2.4.13`

### ✅ **Solução**: Padronizar para `HikariCP-java7 2.4.13` (compatível com Java 7)

## 4. **Estrutura de Pacotes Inconsistente**

### ❌ **Problema**: Diferentes convenções de nomenclatura
- **primeleague-core**: `managers/`, `utils/`, `util/` (duplicado)
- **primeleague-clans**: `manager/` (singular)
- **primeleague-clans**: `model/` (singular)
- **Outros módulos**: `models/` (plural)

### ✅ **Solução**: Padronizar para plural (`managers/`, `models/`, `utils/`)

## 5. **Configurações do Maven Shade Plugin Inconsistentes**

### ❌ **Problema**: Diferentes configurações de empacotamento
- **primeleague-core**: `minimizeJar: false`
- **primeleague-chat**: `minimizeJar: true`
- **primeleague-admin**: Usa relocations específicas
- **primeleague-p2p**: Usa relocations específicas

### ✅ **Solução**: Padronizar configuração do shade plugin

## 6. **Dependências do PrimeLeague API Inconsistentes**

### ❌ **Problema**: Diferentes formas de referenciar o API
- **primeleague-core**: Dependência normal
- **primeleague-p2p**: `scope: system` com `systemPath`
- **primeleague-chat**: `scope: system` com `systemPath`
- **primeleague-clans**: `scope: system` com `systemPath`
- **primeleague-admin**: Dependência normal

### ✅ **Solução**: Padronizar para dependência normal (sem scope system)

## 7. **Versões do Maven Compiler Plugin Inconsistentes**

### ❌ **Problema**: Diferentes versões do plugin
- **primeleague-core**: `3.1`
- **primeleague-p2p**: `3.1`
- **primeleague-chat**: `3.1`
- **primeleague-clans**: `3.8.1`
- **primeleague-admin**: `3.8.1`
- **primeleague-adminshop**: `3.8.1`

### ✅ **Solução**: Padronizar para versão mais recente (`3.8.1`)

## 8. **Versões do Maven Shade Plugin Inconsistentes**

### ❌ **Problema**: Diferentes versões do plugin
- **primeleague-core**: `2.3`
- **primeleague-p2p**: `3.2.4`
- **primeleague-chat**: `2.3`
- **primeleague-clans**: `3.2.4`
- **primeleague-admin**: `3.2.4`
- **primeleague-adminshop**: `3.2.4`

### ✅ **Solução**: Padronizar para versão mais recente (`3.2.4`)

## 9. **Configurações de Encoding Inconsistentes**

### ❌ **Problema**: Diferentes configurações de encoding
- **primeleague-core**: `UTF-8` apenas em properties
- **Outros módulos**: `UTF-8` em properties e compiler

### ✅ **Solução**: Padronizar configuração de encoding

## 10. **Estrutura de Diretórios Inconsistente**

### ❌ **Problema**: Diferentes organizações de código
- **primeleague-core**: Muitos diretórios (`api/`, `commands/`, `util/`, `utils/`, `services/`, `profile/`, `models/`, `enums/`, `database/`)
- **primeleague-p2p**: Estrutura mais simples (`commands/`, `managers/`, `listeners/`, `web/`, `services/`, `tasks/`)
- **primeleague-chat**: Estrutura mínima (`services/`, `listeners/`, `commands/`)
- **primeleague-clans**: Estrutura mista (`services/`, `model/`, `manager/`, `listeners/`, `commands/`)

### ✅ **Solução**: Padronizar estrutura de diretórios

## 🔧 **PLANO DE CORREÇÃO**

### **Fase 1: Correções Críticas**
1. **Padronizar versão do Java** para 1.7 em todos os módulos
2. **Corrigir api-version** para 1.5.2 em todos os plugin.yml
3. **Padronizar HikariCP** para versão compatível com Java 7
4. **Corrigir dependências do API** para formato consistente

### **Fase 2: Correções de Estrutura**
1. **Padronizar estrutura de pacotes** (usar plural)
2. **Padronizar versões dos plugins Maven**
3. **Padronizar configurações do shade plugin**
4. **Padronizar configurações de encoding**

### **Fase 3: Melhorias de Organização**
1. **Reorganizar estrutura de diretórios** para consistência
2. **Padronizar nomenclatura** de classes e pacotes
3. **Implementar convenções** de código consistentes

## 📊 **Status Atual por Módulo**

| Módulo | Java | Bukkit API | HikariCP | Estrutura | Status |
|--------|------|------------|----------|-----------|---------|
| **primeleague-api** | ✅ 1.7 | ✅ 1.5.2 | ❌ N/A | ✅ Boa | ✅ **OK** |
| **primeleague-core** | ❌ 1.8 | ✅ 1.5.2 | ❌ 4.0.3 | ❌ Complexa | ❌ **CRÍTICO** |
| **primeleague-p2p** | ✅ 1.7 | ❌ 1.13 | ✅ 2.4.13 | ✅ Boa | ❌ **CRÍTICO** |
| **primeleague-chat** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ Boa | ⚠️ **MÉDIO** |
| **primeleague-clans** | ✅ 1.7 | ❌ N/A | ❌ N/A | ❌ Mista | ⚠️ **MÉDIO** |
| **primeleague-admin** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ Boa | ⚠️ **MÉDIO** |
| **primeleague-adminshop** | ✅ 1.7 | ❌ N/A | ❌ N/A | ✅ Boa | ⚠️ **MÉDIO** |

## 🎯 **Prioridades de Correção**

### **ALTA PRIORIDADE**
1. **primeleague-core**: Corrigir Java 1.8 → 1.7
2. **primeleague-p2p**: Corrigir api-version 1.13 → 1.5.2

### **MÉDIA PRIORIDADE**
3. **primeleague-core**: Corrigir HikariCP 4.0.3 → 2.4.13
4. **Todos os módulos**: Padronizar dependências do API

### **BAIXA PRIORIDADE**
5. **Todos os módulos**: Padronizar estrutura de pacotes
6. **Todos os módulos**: Padronizar versões dos plugins Maven

## 11. **Uso Inconsistente do PrimeLeague API**

### ❌ **Problema**: Módulos não seguem o mesmo padrão de uso do API
- **primeleague-adminshop**: ❌ **NÃO USA O API** - Funciona isoladamente
- **primeleague-core**: ✅ **USA CORRETAMENTE** - Implementa interfaces do API
- **primeleague-p2p**: ✅ **USA CORRETAMENTE** - Implementa P2PService
- **primeleague-chat**: ✅ **USA CORRETAMENTE** - Implementa LoggingService
- **primeleague-clans**: ✅ **USA CORRETAMENTE** - Implementa ClanService
- **primeleague-admin**: ✅ **USA CORRETAMENTE** - Implementa AdminService

### ✅ **Solução**: Padronizar uso do API em todos os módulos

## 12. **Duplicação de Funcionalidades**

### ❌ **Problema**: Classes que deveriam estar no API estão duplicadas
- **primeleague-core**: Contém `models/` e `enums/` que deveriam estar no API
- **primeleague-adminshop**: Funciona sem API (isolado)
- **Outros módulos**: Implementam corretamente as interfaces do API

### ✅ **Solução**: Mover classes compartilhadas para o API

## 📊 **Status Atual por Módulo**

| Módulo | Java | Bukkit API | HikariCP | Estrutura | Uso do API | Status |
|--------|------|------------|----------|-----------|------------|---------|
| **primeleague-api** | ✅ 1.7 | ✅ 1.5.2 | ❌ N/A | ✅ Boa | ✅ **CORRETO** | ✅ **OK** |
| **primeleague-core** | ❌ 1.8 | ✅ 1.5.2 | ❌ 4.0.3 | ❌ Complexa | ✅ **CORRETO** | ❌ **CRÍTICO** |
| **primeleague-p2p** | ✅ 1.7 | ❌ 1.13 | ✅ 2.4.13 | ✅ Boa | ✅ **CORRETO** | ❌ **CRÍTICO** |
| **primeleague-chat** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ Boa | ✅ **CORRETO** | ⚠️ **MÉDIO** |
| **primeleague-clans** | ✅ 1.7 | ❌ N/A | ❌ N/A | ❌ Mista | ✅ **CORRETO** | ⚠️ **MÉDIO** |
| **primeleague-admin** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ Boa | ✅ **CORRETO** | ⚠️ **MÉDIO** |
| **primeleague-adminshop** | ✅ 1.7 | ❌ N/A | ❌ N/A | ✅ Boa | ❌ **ISOLADO** | ❌ **CRÍTICO** |

## 🎯 **Prioridades de Correção**

### **ALTA PRIORIDADE**
1. **primeleague-core**: Corrigir Java 1.8 → 1.7
2. **primeleague-p2p**: Corrigir api-version 1.13 → 1.5.2
3. **primeleague-adminshop**: Integrar com o API

### **MÉDIA PRIORIDADE**
4. **primeleague-core**: Corrigir HikariCP 4.0.3 → 2.4.13
5. **Todos os módulos**: Padronizar dependências do API
6. **primeleague-core**: Mover models/ e enums/ para o API

### **BAIXA PRIORIDADE**
7. **Todos os módulos**: Padronizar estrutura de pacotes
8. **Todos os módulos**: Padronizar versões dos plugins Maven

## 📝 **Conclusão**

O projeto tem **inconsistências significativas** que podem causar problemas de compatibilidade e manutenção. As correções de **ALTA PRIORIDADE** devem ser aplicadas imediatamente para garantir a estabilidade do sistema.

**Problemas Críticos Identificados:**
- **primeleague-adminshop** não usa o API (funciona isoladamente)
- **primeleague-core** contém classes que deveriam estar no API
- **Versões do Java e Bukkit API** inconsistentes

**Recomendação**: Implementar as correções em ordem de prioridade, testando cada módulo após as correções para garantir que não há quebras de funcionalidade.
