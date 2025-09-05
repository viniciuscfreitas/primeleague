# 🏗️ Padrões Arquiteturais do Prime League

## **Padrão-Ouro Definitivo (Versão 3.0)**

Este documento define os padrões arquiteturais definitivos para o ecossistema Prime League, estabelecidos após a "Grande Reestruturação" que resolveu inconsistências entre módulos.

---

## **📋 Princípios Fundamentais**

### **1. Arquitetura Modular com Núcleo Forte**
- **Core**: Infraestrutura central e interfaces
- **Módulos**: Implementações específicas e lógica de negócio
- **API**: Contratos e interfaces compartilhadas

### **2. Responsabilidade Única por Camada**
- **DAO**: Apenas persistência de dados
- **Manager**: Apenas lógica de negócio
- **Command**: Apenas validação de entrada e delegação

### **3. Injeção de Dependência via Registry**
- Módulos registram suas implementações no Core
- Core gerencia o registry para desacoplamento
- Acesso via interfaces, não implementações

---

## **🗂️ Estrutura de Diretórios Padrão**

```
primeleague-api/
├── dao/
│   ├── ClanDAO.java           ← Interface
│   ├── EssentialsDAO.java     ← Interface
│   └── WarpDAO.java           ← Interface
└── dto/
    ├── ClanDTO.java
    └── HomeDTO.java

primeleague-core/
├── api/
│   └── PrimeLeagueAPI.java    ← API pública
├── services/
│   └── DAOServiceRegistry.java ← Registry central
├── managers/
│   ├── DataManager.java       ← Manager central
│   └── IdentityManager.java   ← Manager central
└── database/
    └── (apenas implementações centrais)

primeleague-clans/
├── dao/
│   └── MySqlClanDAO.java      ← Implementação específica
├── managers/
│   └── ClanManager.java       ← Lógica de negócio
├── models/
│   └── Clan.java              ← Modelo específico
└── PrimeLeagueClans.java      ← Plugin principal

primeleague-essentials/
├── dao/
│   ├── MySqlEssentialsDAO.java ← Implementação específica
│   └── MySqlWarpDAO.java       ← Implementação específica
├── managers/
│   ├── EssentialsManager.java  ← Lógica de negócio
│   └── WarpManager.java        ← Lógica de negócio
├── models/
│   ├── Home.java               ← Modelo específico
│   └── Warp.java               ← Modelo específico
└── EssentialsPlugin.java       ← Plugin principal
```

---

## **🔧 Padrões de Implementação**

### **1. Interface DAO (primeleague-api)**

```java
package br.com.primeleague.api.dao;

import java.util.function.Consumer;
import java.util.List;

public interface ClanDAO {
    void createClanAsync(ClanDTO clanDTO, Consumer<ClanDTO> callback);
    void loadAllClansAsync(Consumer<Map<Integer, ClanDTO>> callback);
    // ... outros métodos assíncronos
}
```

### **2. Implementação DAO (módulo específico)**

```java
package br.com.primeleague.clans.dao;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.core.api.PrimeLeagueAPI;

public class MySqlClanDAO implements ClanDAO {
    private final PrimeLeagueCore core;
    private final DataManager dataManager;
    
    public MySqlClanDAO(PrimeLeagueCore core) {
        this.core = core;
        this.dataManager = core.getDataManager();
    }
    
    @Override
    public void createClanAsync(ClanDTO clanDTO, Consumer<ClanDTO> callback) {
        core.getServer().getScheduler().runTaskAsynchronously(core, () -> {
            // Lógica SQL aqui
            core.getServer().getScheduler().runTask(core, () -> callback.accept(result));
        });
    }
}
```

### **3. Manager (módulo específico)**

```java
package br.com.primeleague.clans.manager;

import br.com.primeleague.api.dao.ClanDAO;
import br.com.primeleague.clans.dao.MySqlClanDAO;

public class ClanManager {
    private final ClanDAO clanDAO;
    
    public ClanManager(PrimeLeagueClans plugin) {
        // Instanciar DAO específico do módulo
        this.clanDAO = new MySqlClanDAO(plugin.getCore());
    }
    
    public void createClan(ClanDTO clanDTO, Consumer<ClanDTO> callback) {
        // Lógica de negócio aqui
        clanDAO.createClanAsync(clanDTO, callback);
    }
    
    public ClanDAO getClanDAO() {
        return clanDAO;
    }
}
```

### **4. Plugin Principal (módulo específico)**

```java
package br.com.primeleague.clans;

public class PrimeLeagueClans extends JavaPlugin {
    private ClanManager clanManager;
    
    @Override
    public void onEnable() {
        // Inicializar manager (que instancia seu próprio DAO)
        this.clanManager = new ClanManager(this);
        
        // Registrar DAO no Core via DAOServiceRegistry
        registerDAO();
    }
    
    private void registerDAO() {
        DAOServiceRegistry registry = PrimeLeagueAPI.getDAOServiceRegistry();
        registry.registerDAO(ClanDAO.class, clanManager.getClanDAO());
    }
}
```

### **5. DAOServiceRegistry (primeleague-core)**

```java
package br.com.primeleague.core.services;

public class DAOServiceRegistry {
    private final Map<Class<?>, Object> daoInstances;
    
    public <T> void registerDAO(Class<T> daoInterface, T daoInstance) {
        daoInstances.put(daoInterface, daoInstance);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getDAO(Class<T> daoInterface) {
        return (T) daoInstances.get(daoInterface);
    }
}
```

---

## **✅ Checklist para Novos Módulos**

### **Criação de Módulo**
- [ ] Criar estrutura de diretórios padrão
- [ ] Definir interfaces DAO na `primeleague-api`
- [ ] Implementar DAOs no módulo específico
- [ ] Criar Managers com lógica de negócio
- [ ] Implementar Plugin principal com registro de DAO

### **Integração com Core**
- [ ] Adicionar dependência `primeleague-core` no `pom.xml`
- [ ] Registrar DAO no `DAOServiceRegistry` no `onEnable()`
- [ ] Usar `PrimeLeagueAPI` para acessar managers centrais
- [ ] Implementar `depend: [PrimeLeague-Core]` no `plugin.yml`

### **Padrões de Código**
- [ ] Todos os métodos DAO são assíncronos (`...Async`)
- [ ] Managers não contêm SQL, apenas orquestração
- [ ] Commands validam entrada e delegam para Managers
- [ ] Callbacks incluem "hardening" (`if (!player.isOnline())`)
- [ ] Compatibilidade Java 7 (sem lambdas Java 8)

---

## **🚫 Anti-Padrões (NÃO FAZER)**

### **❌ Implementações DAO no Core**
```java
// ERRADO: DAO específico no Core
primeleague-core/database/MySqlClanDAO.java

// CORRETO: DAO específico no módulo
primeleague-clans/dao/MySqlClanDAO.java
```

### **❌ Instanciação Direta de DAOs**
```java
// ERRADO: Manager instancia DAO diretamente
public class ClanManager {
    private final ClanDAO clanDAO = new MySqlClanDAO();
}

// CORRETO: Manager instancia seu próprio DAO
public class ClanManager {
    private final ClanDAO clanDAO;
    
    public ClanManager(PrimeLeagueClans plugin) {
        this.clanDAO = new MySqlClanDAO(plugin.getCore());
    }
}
```

### **❌ SQL em Managers**
```java
// ERRADO: SQL no Manager
public class ClanManager {
    public void createClan() {
        Connection conn = dataManager.getConnection();
        // SQL aqui...
    }
}

// CORRETO: Manager delega para DAO
public class ClanManager {
    public void createClan(ClanDTO dto, Consumer<ClanDTO> callback) {
        clanDAO.createClanAsync(dto, callback);
    }
}
```

---

## **📊 Benefícios da Arquitetura**

### **Modularidade**
- Cada módulo é autossuficiente
- Fácil adição de novos módulos
- Evolução independente

### **Manutenibilidade**
- Código organizado por responsabilidade
- Fácil localização de funcionalidades
- Testes isolados

### **Performance**
- Operações assíncronas não-bloqueantes
- Cache em memória por módulo
- Pool de conexões centralizado

### **Escalabilidade**
- Registry permite injeção dinâmica
- Core não cresce com novos módulos
- Desacoplamento total entre módulos

---

## **🎯 Conclusão**

Esta arquitetura representa o **padrão-ouro definitivo** do Prime League, estabelecido após análise crítica e debate técnico. Ela combina:

- **Modularidade** para facilidade de manutenção
- **Centralização** de infraestrutura crítica
- **Injeção de dependência** para desacoplamento
- **Padrões consistentes** em todos os módulos

**Todos os futuros módulos devem seguir rigorosamente estes padrões.**

---

*Documento criado após a "Grande Reestruturação" - Versão 3.0*
*Data: $(date)*
*Autor: PrimeLeague Development Team*
