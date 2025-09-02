# 🔧 CORREÇÃO DO BANCO DE DADOS PRIMELEAGUE

Este projeto contém scripts para analisar e corrigir problemas de schema no banco de dados MariaDB/MySQL do PrimeLeague.

## 🚨 PROBLEMA IDENTIFICADO

O sistema de clãs está falhando com o erro:
```
Invalid value for getInt() - 'OFFICER'
```

**Causa Raiz:** Inconsistência entre o banco de dados (que armazena roles como strings ENUM) e o código Java (que espera inteiros).

## 📋 ARQUIVOS INCLUÍDOS

- `analyze-database.js` - Script para analisar problemas no banco
- `fix-database-schema.js` - Script para corrigir problemas automaticamente
- `fix-database.sql` - Script SQL para correção manual
- `run-database-fix.bat` - Script Windows para execução fácil
- `package.json` - Dependências Node.js

## 🚀 EXECUÇÃO RÁPIDA (WINDOWS)

1. **Execute o script batch:**
   ```cmd
   run-database-fix.bat
   ```

2. **Escolha a opção desejada:**
   - Opção 1: Analisar (apenas leitura)
   - Opção 2: Corrigir automaticamente
   - Opção 3: Executar script SQL

## 🔧 EXECUÇÃO MANUAL

### 1. Instalar Dependências
```bash
npm install
```

### 2. Configurar Conexão
Edite os arquivos `.js` e ajuste:
```javascript
const dbConfig = {
    host: 'localhost',
    user: 'root',        // Seu usuário MySQL
    password: '',         // Sua senha MySQL
    database: 'primeleague',
    port: 3306
};
```

### 3. Executar Análise
```bash
npm run analyze
```

### 4. Executar Correção
```bash
npm run fix
```

## 📊 O QUE OS SCRIPTS FAZEM

### Análise (`analyze-database.js`)
- ✅ Verifica estrutura das tabelas
- ✅ Analisa dados atuais
- ✅ Identifica problemas de contagem
- ✅ Verifica inconsistências
- ✅ Detecta dados órfãos

### Correção (`fix-database-schema.js`)
- 🔧 Corrige roles incorretos (`OFFICER` → `LEADER`)
- 🧹 Remove dados duplicados
- 👥 Adiciona fundadores como membros se necessário
- 🗑️ Remove jogadores órfãos
- 📊 Verifica integridade final

### SQL (`fix-database.sql`)
- 📋 Script SQL completo para correção manual
- 🔍 Comandos de verificação
- 🔧 Comandos de correção
- 📊 Verificações finais

## 🎯 PROBLEMAS CORRIGIDOS

1. **Roles Incorretos:**
   - `OFFICER` → `LEADER`
   - Mapeamento correto de todos os roles

2. **Dados Duplicados:**
   - Remoção de registros duplicados
   - Manutenção de integridade

3. **Clãs Sem Membros:**
   - Adição automática de fundadores
   - Verificação de consistência

4. **Dados Órfãos:**
   - Remoção de jogadores em clãs inexistentes
   - Remoção de jogadores inexistentes

## ⚠️ IMPORTANTE

- **Faça backup** do banco antes de executar correções
- **Teste em ambiente de desenvolvimento** primeiro
- **Verifique as configurações** de conexão
- **Execute com cuidado** as operações de modificação

## 🔍 VERIFICAÇÃO PÓS-CORREÇÃO

Após a correção, verifique:

1. **Comando `/clan info`** funciona sem erros
2. **Contagem de membros** está correta
3. **Roles dos jogadores** estão mapeados corretamente
4. **Não há erros SQL** nos logs

## 🆘 SOLUÇÃO DE PROBLEMAS

### Erro de Conexão
- Verifique se o MariaDB está rodando
- Confirme usuário e senha
- Verifique se o banco `primeleague` existe

### Erro de Permissão
- Use um usuário com privilégios adequados
- Verifique se o usuário tem acesso às tabelas

### Erro de Dependência
- Execute `npm install` para instalar dependências
- Verifique se o Node.js está instalado

## 📞 SUPORTE

Se encontrar problemas:
1. Execute a análise primeiro (`npm run analyze`)
2. Verifique os logs de erro
3. Confirme as configurações de conexão
4. Execute a correção com cuidado

---

**🎯 Objetivo:** Corrigir o erro `Invalid value for getInt() - 'OFFICER'` e restaurar o funcionamento do sistema de clãs.
