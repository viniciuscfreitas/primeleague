# ANÁLISE ARQUITETURAL: ESTRUTURA DE BANCO DE DADOS vs CÓDIGO

## CONTEXTO
Estou trabalhando em um sistema Discord Bot para um servidor Minecraft (Prime League) que gerencia vínculos entre contas Discord e contas Minecraft.

## PROBLEMA IDENTIFICADO
O código do bot está tentando acessar uma coluna `dl.player_id` na tabela `discord_links`, mas a estrutura real do banco de dados usa `dl.player_uuid` (UUID do player) em vez de `player_id`.

## ESTRUTURA ATUAL DO BANCO

### Tabela `player_data`:
```sql
- player_id (int, PK, auto_increment)
- uuid (char(36), UNIQUE) 
- name (varchar(16), UNIQUE)
- elo, money, total_playtime, etc.
```

### Tabela `discord_links`:
```sql
- link_id (int, PK, auto_increment)
- discord_id (varchar(20))
- player_uuid (char(36), UNIQUE)  ← AQUI ESTÁ A DIFERENÇA
- is_primary, verified, etc.
```

## CÓDIGO ATUAL (INCORRETO)
```javascript
// O código está tentando fazer JOIN assim:
LEFT JOIN discord_links dl ON pd.player_id = dl.player_id
// Mas deveria ser:
LEFT JOIN discord_links dl ON pd.uuid = dl.player_uuid
```

## PERGUNTA PARA A IA ARQUITETA

**Qual é a abordagem correta arquiteturalmente?**

### OPÇÃO A: Modificar o código do bot
- Corrigir todas as queries para usar `player_uuid` em vez de `player_id`
- Manter a estrutura atual do banco de dados
- Vantagens: Não afeta dados existentes, não requer migração
- Desvantagens: Código fica mais complexo (precisa buscar UUID primeiro)

### OPÇÃO B: Modificar o schema do banco
- Adicionar coluna `player_id` na tabela `discord_links`
- Manter `player_uuid` também (para compatibilidade)
- Vantagens: Código fica mais simples e direto
- Desvantagens: Requer migração de dados, duplicação de informação

### OPÇÃO C: Refatorar completamente
- Remover `player_uuid` e usar apenas `player_id` como FK
- Vantagens: Relacionamento mais direto e eficiente
- Desvantagens: Quebra de compatibilidade, migração complexa

## CONSIDERAÇÕES IMPORTANTES

1. **Performance**: UUIDs são maiores que INTs, mas oferecem mais flexibilidade
2. **Compatibilidade**: O sistema já está em produção com dados existentes
3. **Manutenibilidade**: Qual abordagem será mais fácil de manter a longo prazo?
4. **Escalabilidade**: Qual estrutura suporta melhor o crescimento do sistema?

## PERGUNTA PRINCIPAL
**Como arquiteta de software, qual abordagem você recomendaria e por quê?**
- Devo corrigir o código para se adaptar ao schema atual?
- Devo modificar o schema para se adaptar ao código esperado?
- Existe uma terceira opção melhor?

Por favor, analise considerando:
- Boas práticas de design de banco de dados
- Princípios de normalização
- Performance e escalabilidade
- Facilidade de manutenção
- Impacto em dados existentes
