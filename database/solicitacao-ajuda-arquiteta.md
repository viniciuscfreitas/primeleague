# **SOLICITAÇÃO DE AJUDA TÉCNICA - IA ARQUITETA**

**Data:** 07 de Setembro de 2025  
**De:** Equipe de Desenvolvimento  
**Para:** IA Arquiteta do Prime League  
**Assunto:** Erro Crítico no Módulo de Territórios - Foreign Key Constraint e Inconsistência de Dados  

---

## **CONTEXTO DA SITUAÇÃO**

O **Módulo de Territórios** foi implementado com sucesso e está compilando perfeitamente. O JAR foi gerado e instalado no servidor. No entanto, ao tentar executar o comando `/territory claim` no jogo, estamos enfrentando **erros críticos de banco de dados** que impedem o funcionamento do módulo.

---

## **ERRO PRINCIPAL IDENTIFICADO**

### **Foreign Key Constraint Failure**
```
Cannot add or update a child row: a foreign key constraint fails 
(`primeleague`.`prime_territories`, CONSTRAINT `fk_prime_territories_clan` 
FOREIGN KEY (`clan_id`) REFERENCES `clans` (`id`) ON DELETE CASCADE)
```

**Detalhes do Erro:**
- **Tabela:** `prime_territories`
- **Constraint:** `fk_prime_territories_clan`
- **Tentativa:** Inserir `clan_id=2`
- **Problema:** Não existe clã com ID 2 na tabela `clans`

---

## **ANÁLISE TÉCNICA DETALHADA**

### **1. Estrutura do Banco de Dados**
- **Tabela `clans`:** Existe e contém dados
- **Tabela `prime_territories`:** Criada com sucesso via schema
- **Foreign Key:** Configurada corretamente (`clan_id` → `clans.id`)

### **2. Dados do Jogador**
- **Jogador:** `vinicff`
- **UUID Ativo:** `724d9b3f-41f9-07f3-c3eb-03158b4b9d89`
- **UUID Canônico:** `3e556f49-c226-3253-8408-9824b21a6d6a`
- **Status:** Verificado e autorizado

### **3. Problema de Mapeamento**
O sistema está tentando usar `clan_id=2`, mas o jogador `vinicff` pertence ao `clan_id=1`. Isso indica uma **inconsistência no mapeamento de dados** entre o módulo de Territórios e o módulo de Clãs.

---

## **LOGS DETALHADOS DO SERVIDOR**

### **Inicialização do Módulo (Sucesso)**
```
[PrimeLeague-Territories] === INICIANDO MÓDULO DE TERRITÓRIOS ===
[PrimeLeague-Territories] ✓ Todas as dependências encontradas
[PrimeLeague-Territories] MessageManager carregado com 56 mensagens.
[PrimeLeague-Territories] ✓ MessageManager inicializado
[PrimeLeague-Territories] ✓ TerritoryManager inicializado
[PrimeLeague-Territories] ✓ WarManager inicializado
[PrimeLeague-Territories] ✓ Módulo de Territórios carregado com sucesso!
```

### **Erro ao Executar Comando**
```
vinicff issued server command: /territory claim
[GRAVE] [PrimeLeague-Core] Erro ao criar território:
[GRAVE] [PrimeLeague-Core]   Query: INSERT INTO prime_territories (clan_id, world_name, chunk_x, chunk_z, claimed_at) VALUES (?, ?, ?, ?, ?)
[GRAVE] [PrimeLeague-Core]   Parâmetros: clanId=2, world=world, chunkX=11, chunkZ=-6
[GRAVE] [PrimeLeague-Core]   Erro SQL: Cannot add or update a child row: a foreign key constraint fails
```

---

## **PROBLEMAS SECUNDÁRIOS IDENTIFICADOS**

### **1. Inconsistência de UUID**
- O sistema está usando dois UUIDs diferentes para o mesmo jogador
- Isso pode causar problemas de mapeamento de permissões

### **2. Estrutura da Tabela `player_data`**
- Tentativa de acessar coluna `username` que não existe
- Estrutura real da tabela não está clara

---

## **PERGUNTAS ESPECÍFICAS PARA A IA ARQUITETA**

### **1. Arquitetura de Dados**
- **Qual é a estrutura correta da tabela `player_data`?**
- **Como o módulo de Territórios deve obter o `clan_id` correto do jogador?**
- **Existe um padrão estabelecido para mapeamento de jogadores → clãs?**

### **2. Integração entre Módulos**
- **Como o módulo de Territórios deve se comunicar com o módulo de Clãs?**
- **Qual é a API correta para obter dados do clã do jogador?**
- **Existe um serviço centralizado para mapeamento de identidades?**

### **3. Resolução do Problema**
- **Qual é a abordagem recomendada para corrigir o mapeamento de `clan_id`?**
- **Devemos modificar o código do módulo de Territórios ou o banco de dados?**
- **Existe um padrão de migração de dados que devemos seguir?**

---

## **INFORMAÇÕES TÉCNICAS ADICIONAIS**

### **Arquivos Relevantes**
- **Schema:** `database/territories-schema.sql`
- **Código Principal:** `primeleague-territories/src/main/java/br/com/primeleague/territories/`
- **DAO:** `MySqlTerritoryDAO.java`
- **Comando:** `TerritoryCommand.java`

### **Dependências**
- **Core:** PrimeLeague-Core v1.0.0 (funcionando)
- **Clãs:** PrimeLeague-Clans v1.0.0 (funcionando)
- **Banco:** MySQL com HikariCP (funcionando)

---

## **SOLICITAÇÃO DE AJUDA**

**Precisamos de orientação técnica específica sobre:**

1. **Como corrigir o mapeamento de `clan_id`** no módulo de Territórios
2. **Qual é a estrutura correta** da tabela `player_data`
3. **Como implementar a integração correta** entre os módulos
4. **Qual é a abordagem arquitetural recomendada** para resolver este problema

**O módulo está tecnicamente correto e compilando, mas há uma desconexão na camada de dados que precisa ser resolvida para que funcione em produção.**

---

**Aguardo sua orientação técnica para resolver este problema crítico.**

**Equipe de Desenvolvimento**  
**Prime League Project**
