# 🚀 Configuração de Debug no Cursor

## **Visão Geral**
Este guia explica como configurar a depuração no **Cursor IDE** para testar o módulo PrimeLeague AdminShop sem precisar abrir o Minecraft.

## **📋 Pré-requisitos**
- ✅ Cursor IDE instalado
- ✅ Projeto PrimeLeague AdminShop aberto
- ✅ Maven configurado

## **🔧 Opções de Debug**

### **Opção 1: Debug Local (Recomendado)**
O servidor inicia diretamente no Cursor e para no primeiro breakpoint.

### **Opção 2: Debug Remoto**
Conecta a um servidor já rodando (mais complexo para Bukkit 1.5.2).

## **🚀 Opção 1: Debug Local (Mais Fácil)**

### **1. Iniciar Debug Local**
```bash
# Execute no diretório primeleague-adminshop
debug-local.bat
```

**O que acontece:**
1. Projeto é compilado automaticamente
2. Servidor inicia em modo debug
3. Para no primeiro breakpoint
4. Cursor assume o controle

### **2. Configurar no Cursor**
1. **Pressione `F5`** ou `Ctrl+Shift+D`
2. **Selecione** "PrimeLeague Debug - Local"
3. **Clique** em "Start Debugging" (▶️)

### **3. Colocar Breakpoints**
1. Abra `ShopConfigManager.java`
2. Clique na linha 287 (validação de campos obrigatórios)
3. Ponto vermelho aparecerá (breakpoint ativo)

### **4. Executar e Debuggar**
1. O servidor iniciará e parará no breakpoint
2. Use **F10** para executar linha por linha
3. Use **F11** para entrar em métodos
4. Use **F5** para continuar

## **🔌 Opção 2: Debug Remoto (Avançado)**

### **1. Iniciar Servidor Normal**
```bash
# Execute no diretório raiz
server/start.bat
```

### **2. Configurar Debug Remoto no Cursor**
1. **Pressione `Ctrl+Shift+D`**
2. **Clique** no ícone de engrenagem (⚙️)
3. **Selecione** "Java" → "Remote Java Application"
4. **Configure:**
   ```json
   {
       "name": "PrimeLeague Debug - Remote",
       "type": "java",
       "request": "attach",
       "hostName": "localhost",
       "port": 1000
   }
   ```

## **🧪 Como Usar o Debug**

### **Controles de Debug**
- **F5**: Continue/Resume
- **F10**: Step Over (executa linha atual)
- **F11**: Step Into (entra no método)
- **Shift+F11**: Step Out (sai do método)
- **F9**: Toggle Breakpoint

### **Painel de Debug**
- **Variables**: Inspeciona variáveis locais
- **Watch**: Adiciona expressões para monitorar
- **Call Stack**: Mostra a pilha de chamadas
- **Breakpoints**: Lista todos os breakpoints

## **🔍 Exemplo Prático**

### **Teste de Validação**
1. **Coloque breakpoint** na linha 287
2. **Inicie o debug** com `debug-local.bat`
3. **Servidor para** na validação
4. **Inspecione** o objeto `itemMap`
5. **Use F10** para executar linha por linha

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
- **Interface moderna** do Cursor
- **Debug local** funciona perfeitamente

### **⚠️ Limitações**
- Bukkit 1.5.2 é versão antiga (2013)
- Debug remoto pode ser instável
- Algumas APIs modernas não existem

## **🚨 Solução de Problemas**

### **Erro: Main Class Not Found**
- Execute `mvn clean compile` primeiro
- Verifique se a classe principal existe
- Confirme o caminho da classe

### **Erro: Port Already in Use**
- Feche outros processos Java
- Mude a porta no arquivo de configuração
- Reinicie o Cursor

### **Debugger Não Para**
- Verifique se o breakpoint está ativo (ponto vermelho)
- Confirme se o código está sendo executado
- Verifique se a linha do breakpoint está correta

## **🎯 Próximos Passos**

1. **Execute os testes** com `mvn test`
2. **Use debug local** com `debug-local.bat`
3. **Configure breakpoints** no código
4. **Teste a depuração** passo a passo
5. **Desenvolva com confiança** sem Minecraft

## **📞 Suporte**
Se encontrar problemas:
1. Verifique se o projeto compila (`mvn compile`)
2. Confirme se a classe principal existe
3. Execute `mvn clean compile` para recompilar
4. Reinicie o Cursor

---

**🎉 Parabéns!** Agora você pode desenvolver e testar plugins Bukkit usando o Cursor IDE com debug local!
