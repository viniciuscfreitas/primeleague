# 🚀 Configuração de Debug Remoto no Eclipse

## **Visão Geral**
Este guia explica como configurar a depuração remota para testar o módulo PrimeLeague AdminShop sem precisar abrir o Minecraft ou conectar com uma conta.

## **📋 Pré-requisitos**
- ✅ Eclipse IDE instalado
- ✅ Projeto PrimeLeague AdminShop importado
- ✅ Servidor Bukkit 1.5.2 configurado
- ✅ Maven configurado

## **🔧 Passo a Passo**

### **1. Iniciar o Servidor em Modo Debug**
```bash
# Execute o arquivo modificado
server/start.bat
```

**O servidor deve mostrar:**
```
========================================
    PRIMELEAGUE SERVER - DEBUG MODE
========================================

Iniciando servidor em modo de depuracao...
Porta de debug: 1000
```

### **2. Configurar Debug Remoto no Eclipse**

#### **2.1 Abrir Debug Configurations**
- Menu: `Run` → `Debug Configurations...`
- Ou: Clique na seta ao lado do botão Debug → `Debug Configurations...`

#### **2.2 Criar Nova Configuração**
- Clique com botão direito em `Remote Java Application`
- Selecione `New Configuration`

#### **2.3 Configurar Parâmetros**
```
Name: PrimeLeague Debug
Connection Type: Standard (Socket Attach)
Host: localhost
Port: 1000
```

#### **2.4 Configurar Source**
- Aba `Source`
- Clique `Add...`
- Selecione `Java Project`
- Escolha `primeleague-adminshop`
- Clique `OK`

#### **2.5 Aplicar e Conectar**
- Clique `Apply`
- Clique `Debug`

## **🧪 Como Usar**

### **3. Colocar Breakpoints**
1. Abra o arquivo `ShopConfigManager.java`
2. Clique na margem esquerda da linha 287 (validação de campos obrigatórios)
3. Um ponto azul aparecerá (breakpoint ativo)

### **4. Executar Comandos**
1. No servidor, execute comandos que usem o AdminShop
2. Quando o código atingir a linha 287, o Eclipse parará
3. Você pode inspecionar variáveis, objetos e o estado

### **5. Navegar pelo Código**
- **Step Over (F6)**: Executa linha atual
- **Step Into (F5)**: Entra em métodos
- **Step Return (F7)**: Sai do método atual
- **Resume (F8)**: Continua execução

## **🔍 Exemplo Prático**

### **Teste de Validação**
1. **Coloque breakpoint** na linha 287
2. **Execute comando** que crie item da loja
3. **Debugger para** na validação
4. **Inspecione** o objeto `itemMap`
5. **Verifique** se campos obrigatórios estão presentes

### **Variáveis para Inspecionar**
```java
// No breakpoint da linha 287, inspecione:
itemMap.get("id")           // ID do item
itemMap.get("material")     // Material do item
itemMap.get("price")        // Preço do item
itemMap.size()              // Quantidade de campos
```

## **📊 Benefícios**

### **✅ Vantagens**
- **Teste em tempo real** sem reiniciar servidor
- **Debugging avançado** com breakpoints
- **Inspeção de variáveis** em tempo real
- **Hot reload** de código modificado
- **Sem necessidade** de Minecraft ou conta

### **⚠️ Limitações**
- Bukkit 1.5.2 é versão antiga (2013)
- Algumas APIs modernas não existem
- Eventos podem ter assinaturas diferentes

## **🚨 Solução de Problemas**

### **Erro: Connection Refused**
- Verifique se o servidor está rodando
- Confirme se a porta 1000 está livre
- Reinicie o servidor com `start.bat`

### **Erro: Source Not Found**
- Verifique se o projeto está na aba Source
- Confirme se o projeto foi importado corretamente
- Recompile o projeto com Maven

### **Debugger Não Para**
- Verifique se o breakpoint está ativo (ponto azul)
- Confirme se o código está sendo executado
- Verifique se a linha do breakpoint está correta

## **🎯 Próximos Passos**

1. **Execute os testes** com `test-debug-mode.bat`
2. **Configure o Eclipse** seguindo este guia
3. **Teste a depuração** com breakpoints
4. **Explore o código** em tempo real
5. **Desenvolva com confiança** sem Minecraft

## **📞 Suporte**
Se encontrar problemas:
1. Verifique os logs do servidor
2. Confirme a configuração do Eclipse
3. Execute `mvn clean compile` para recompilar
4. Reinicie o servidor e o Eclipse

---

**🎉 Parabéns!** Agora você pode desenvolver e testar plugins Bukkit sem precisar do Minecraft!
