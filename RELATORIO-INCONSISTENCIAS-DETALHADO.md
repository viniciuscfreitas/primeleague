# Relatório Detalhado de Inconsistências - PrimeLeague

## 📋 **ANÁLISE ESPECÍFICA POR MÓDULO**

### 🚨 **PROBLEMAS CRÍTICOS IDENTIFICADOS**

## 1. **primeleague-adminshop - MÓDULO ISOLADO**

### ❌ **Problema Específico**: Não usa o PrimeLeague API
- **Dependência no pom.xml**: Apenas `primeleague-core` (via systemPath)
- **Ausência**: Não há dependência do `primeleague-api`
- **Consequência**: Funciona isoladamente, quebrando o ecossistema integrado

### 📁 **Estrutura Atual**:
```
primeleague-adminshop/
├── models/
│   ├── ShopItem.java (416 linhas)
│   ├── ShopGUIHolder.java (66 linhas)
│   └── ShopCategory.java (199 linhas)
├── managers/
├── listeners/
└── commands/
```

### ✅ **Solução Específica**:
1. Adicionar dependência do `primeleague-api` no pom.xml
2. Criar interfaces no API: `ShopService`, `ShopItemDTO`, `ShopCategoryDTO`
3. Implementar essas interfaces no módulo
4. Mover classes compartilhadas para o API

---

## 2. **primeleague-core - VERSÃO JAVA INCOMPATÍVEL**

### ❌ **Problema Específico**: Java 1.8 em projeto Minecraft 1.5.2
- **pom.xml linha 15**: `<maven.compiler.source>1.8</maven.compiler.source>`
- **pom.xml linha 16**: `<maven.compiler.target>1.8</maven.compiler.target>`
- **Consequência**: Incompatibilidade com Minecraft 1.5.2 (requer Java 1.7)

### ❌ **Problema Específico**: HikariCP versão incompatível
- **pom.xml linha 45**: `<version>4.0.3</version>` (HikariCP)
- **Problema**: HikariCP 4.0.3 requer Java 1.8+
- **Solução**: Usar `HikariCP-java7 2.4.13`

### 📁 **Classes que deveriam estar no API**:
```
primeleague-core/src/main/java/br/com/primeleague/core/
├── models/
│   ├── DonorLevel.java (180 linhas) ← DEVERIA ESTAR NO API
│   ├── PlayerProfile.java (366 linhas) ← DEVERIA ESTAR NO API
│   └── EconomyResponse.java (81 linhas) ← DEVERIA ESTAR NO API
└── enums/
    └── TransactionReason.java (60 linhas) ← DEVERIA ESTAR NO API
```

### ✅ **Solução Específica**:
1. Corrigir Java 1.8 → 1.7
2. Corrigir HikariCP 4.0.3 → 2.4.13
3. Mover classes para o API

---

## 3. **primeleague-p2p - API VERSION ERRADA**

### ❌ **Problema Específico**: api-version incorreta
- **plugin.yml linha 6**: `api-version: 1.13`
- **Problema**: Minecraft 1.5.2 não suporta API 1.13
- **Solução**: `api-version: 1.5.2`

---

## 4. **Dependências do API Inconsistentes**

### ❌ **Problema Específico**: Diferentes formas de referenciar o API

#### **primeleague-core** (CORRETO):
```xml
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### **primeleague-p2p** (INCORRETO):
```xml
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../primeleague-api/target/primeleague-api-1.0.0.jar</systemPath>
</dependency>
```

#### **primeleague-chat** (INCORRETO):
```xml
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../primeleague-api/target/primeleague-api-1.0.0.jar</systemPath>
</dependency>
```

#### **primeleague-clans** (INCORRETO):
```xml
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../primeleague-api/target/primeleague-api-1.0.0.jar</systemPath>
</dependency>
```

#### **primeleague-admin** (CORRETO):
```xml
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 5. **Estrutura de Pacotes Inconsistente**

### ❌ **Problema Específico**: Convenções de nomenclatura diferentes

#### **primeleague-core**:
```
├── managers/ (plural) ✅
├── utils/ (plural) ✅
├── util/ (singular) ❌ DUPLICADO
├── models/ (plural) ✅
└── enums/ (plural) ✅
```

#### **primeleague-clans**:
```
├── services/ (plural) ✅
├── model/ (singular) ❌ DEVERIA SER models/
├── manager/ (singular) ❌ DEVERIA SER managers/
├── listeners/ (plural) ✅
└── commands/ (plural) ✅
```

#### **primeleague-p2p**:
```
├── commands/ (plural) ✅
├── managers/ (plural) ✅
├── listeners/ (plural) ✅
├── web/ (singular) ❌ DEVERIA SER webs/
├── services/ (plural) ✅
└── tasks/ (plural) ✅
```

---

## 6. **Versões dos Plugins Maven Inconsistentes**

### ❌ **Problema Específico**: Diferentes versões do Maven Compiler Plugin

| Módulo | Versão | Arquivo |
|--------|--------|---------|
| primeleague-core | 3.1 | pom.xml linha 67 |
| primeleague-p2p | 3.1 | pom.xml linha 67 |
| primeleague-chat | 3.1 | pom.xml linha 67 |
| primeleague-clans | 3.8.1 | pom.xml linha 67 |
| primeleague-admin | 3.8.1 | pom.xml linha 67 |
| primeleague-adminshop | 3.8.1 | pom.xml linha 67 |

### ❌ **Problema Específico**: Diferentes versões do Maven Shade Plugin

| Módulo | Versão | Arquivo |
|--------|--------|---------|
| primeleague-core | 2.3 | pom.xml linha 75 |
| primeleague-p2p | 3.2.4 | pom.xml linha 75 |
| primeleague-chat | 2.3 | pom.xml linha 75 |
| primeleague-clans | 3.2.4 | pom.xml linha 75 |
| primeleague-admin | 3.2.4 | pom.xml linha 75 |
| primeleague-adminshop | 3.2.4 | pom.xml linha 75 |

---

## 7. **Configurações do Shade Plugin Inconsistentes**

### ❌ **Problema Específico**: Diferentes configurações de empacotamento

#### **primeleague-core**:
```xml
<minimizeJar>false</minimizeJar>
```

#### **primeleague-chat**:
```xml
<minimizeJar>true</minimizeJar>
```

#### **primeleague-admin**:
```xml
<relocations>
    <relocation>
        <pattern>com.zaxxer.hikari</pattern>
        <shadedPattern>br.com.primeleague.admin.libs.hikari</shadedPattern>
    </relocation>
</relocations>
```

#### **primeleague-p2p**:
```xml
<relocations>
    <relocation>
        <pattern>br.com.primeleague.api</pattern>
        <shadedPattern>br.com.primeleague.api</shadedPattern>
    </relocation>
</relocations>
```

---

## 🔧 **PLANO DE CORREÇÃO DETALHADO**

### **FASE 1: Correções Críticas (ALTA PRIORIDADE)**

#### 1.1 **Corrigir primeleague-core**
```xml
<!-- pom.xml - Linha 15-16 -->
<maven.compiler.source>1.7</maven.compiler.source>
<maven.compiler.target>1.7</maven.compiler.target>

<!-- pom.xml - Linha 45 -->
<version>2.4.13</version> <!-- HikariCP-java7 -->
```

#### 1.2 **Corrigir primeleague-p2p**
```yaml
# plugin.yml - Linha 6
api-version: 1.5.2
```

#### 1.3 **Integrar primeleague-adminshop**
```xml
<!-- Adicionar ao pom.xml -->
<dependency>
    <groupId>br.com.primeleague</groupId>
    <artifactId>primeleague-api</artifactId>
    <version>1.0.0</version>
</dependency>
```

### **FASE 2: Padronização de Dependências (MÉDIA PRIORIDADE)**

#### 2.1 **Padronizar dependências do API**
- Remover `scope: system` e `systemPath` de todos os módulos
- Usar dependência normal como no `primeleague-core`

#### 2.2 **Padronizar versões dos plugins Maven**
- Maven Compiler Plugin: 3.8.1
- Maven Shade Plugin: 3.2.4

### **FASE 3: Reorganização Estrutural (BAIXA PRIORIDADE)**

#### 3.1 **Mover classes para o API**
```
primeleague-core/src/main/java/br/com/primeleague/core/
├── models/ → primeleague-api/src/main/java/br/com/primeleague/api/dto/
└── enums/ → primeleague-api/src/main/java/br/com/primeleague/api/enums/
```

#### 3.2 **Padronizar estrutura de pacotes**
- Usar sempre plural: `models/`, `managers/`, `utils/`
- Remover duplicações: `util/` e `utils/`

---

## 📊 **STATUS DETALHADO POR MÓDULO**

| Módulo | Java | Bukkit API | HikariCP | API Usage | Estrutura | Prioridade |
|--------|------|------------|----------|-----------|-----------|------------|
| **primeleague-api** | ✅ 1.7 | ✅ 1.5.2 | ❌ N/A | ✅ DEFINE | ✅ Boa | ✅ **OK** |
| **primeleague-core** | ❌ 1.8 | ✅ 1.5.2 | ❌ 4.0.3 | ✅ USA | ❌ Complexa | ❌ **CRÍTICO** |
| **primeleague-p2p** | ✅ 1.7 | ❌ 1.13 | ✅ 2.4.13 | ✅ USA | ✅ Boa | ❌ **CRÍTICO** |
| **primeleague-chat** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ USA | ✅ Boa | ⚠️ **MÉDIO** |
| **primeleague-clans** | ✅ 1.7 | ❌ N/A | ❌ N/A | ✅ USA | ❌ Mista | ⚠️ **MÉDIO** |
| **primeleague-admin** | ✅ 1.7 | ❌ N/A | ✅ 2.4.13 | ✅ USA | ✅ Boa | ⚠️ **MÉDIO** |
| **primeleague-adminshop** | ✅ 1.7 | ❌ N/A | ❌ N/A | ❌ ISOLADO | ✅ Boa | ❌ **CRÍTICO** |

---

## 🎯 **COMANDOS ESPECÍFICOS PARA CORREÇÃO**

### **1. Corrigir Java no primeleague-core**
```bash
# Editar primeleague-core/pom.xml
# Linha 15: <maven.compiler.source>1.7</maven.compiler.source>
# Linha 16: <maven.compiler.target>1.7</maven.compiler.target>
```

### **2. Corrigir HikariCP no primeleague-core**
```bash
# Editar primeleague-core/pom.xml
# Linha 45: <version>2.4.13</version>
# Linha 44: <artifactId>HikariCP-java7</artifactId>
```

### **3. Corrigir api-version no primeleague-p2p**
```bash
# Editar primeleague-p2p/src/main/resources/plugin.yml
# Linha 6: api-version: 1.5.2
```

### **4. Adicionar API ao primeleague-adminshop**
```bash
# Editar primeleague-adminshop/pom.xml
# Adicionar dependência do primeleague-api
```

---

## 📝 **CONCLUSÃO ESPECÍFICA**

**Problemas Críticos Identificados:**
1. **primeleague-core**: Java 1.8 incompatível com Minecraft 1.5.2
2. **primeleague-p2p**: api-version 1.13 incompatível com Minecraft 1.5.2
3. **primeleague-adminshop**: Não usa o API (funciona isoladamente)
4. **Dependências do API**: Formato inconsistente entre módulos

**Impacto**: Essas inconsistências podem causar:
- Falhas de compilação
- Incompatibilidade em runtime
- Quebra do ecossistema integrado
- Dificuldade de manutenção

**Recomendação**: Implementar as correções na ordem especificada, testando cada módulo após as correções.
