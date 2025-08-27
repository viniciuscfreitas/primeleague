# Desenvolvimento de Plugins Minecraft 1.5.2 (Bukkit/Spigot) — Guia Definitivo em Markdown

> Fonte única de verdade (SSOT) para criar plugins que levem a versão 1.5.2 ao limite com segurança, desempenho e compatibilidade. Este documento é hiper completo e detalhado, com foco em APIs e práticas válidas para a era 1.5.2.

- **Alvo**: Servidores Minecraft 1.5.2 utilizando Bukkit/Spigot 1.5.2 (CraftBukkit/Spigot como implementação).
- **Java alvo**: JDK 7 (evitar dependências de Java 8+). Em muitos ambientes 1.5.2, JDK 6 também é usado; recomendamos JDK 7 para tooling suficiente e compatibilidade.
- **Escopo**: setup completo, ciclo de vida, eventos, comandos, permissões, scheduler, configuração YAML, mundo/blocos/entidades, inventários, scoreboard (limites), NMS/CraftBukkit com isolamento de versão, Vault, BungeeCord messaging, ProtocolLib (opcional), boas práticas, anti‑padrões, checklist e apêndices.
- Não incluímos APIs modernas (ex.: `PersistentDataContainer`) inexistentes em 1.5.2. Onde houver divergência entre builds (R1/R2/R3), indicamos cautela e estratégias de compatibilidade.

---

## Sumário
1. Introdução e Escopo
2. Ambiente de Desenvolvimento (JDK/IDE/Projeto)
3. Estrutura de um Plugin 1.5.2
4. `plugin.yml` — Campos, Comandos e Permissões
5. Eventos (Listeners): modelo, prioridades, exemplos
6. Comandos e Autocompletar (Tab Complete)
7. Permissões e Integração com Vault
8. Tarefas e Concorrência (Scheduler, Async vs Main Thread)
9. Configuração (YAML): `FileConfiguration`, defaults, reload
10. Mundo, Blocos, Entidades e Inventários
11. Efeitos, Som e Chat
12. Scoreboard (1.5.2): limites e exemplos
13. NMS/CraftBukkit Avançado: isolamento por reflexão (e NBT)
14. Persistência e I/O (arquivos, JDBC, caches)
15. ProtocolLib (opcional) e Mensageria BungeeCord
16. Boas Práticas e Anti‑padrões (1.5.2)
17. Depuração, Testes Locais e Performance
18. Checklist de Release e Distribuição
19. Apêndices: Templates, Snippets, Tabelas e Índice Remissivo

---

## 1) Introdução e Escopo
- Bukkit é a API; CraftBukkit/Spigot são implementações do servidor. Para compilar plugins, basta depender da API (Bukkit). Para recursos específicos (NMS), exige CraftBukkit/Spigot da mesma versão de pacote (`net.minecraft.server.v1_5_*`).
- 1.5.2 é EOL. Evite referências a APIs posteriores. Teste sempre no servidor alvo real.
- Objetivo: guia “estilo Javadoc + cookbook”, com assinaturas, parâmetros, retornos, exceções e exemplos práticos, cobrindo do básico ao avançado.

## 2) Ambiente de Desenvolvimento (JDK/IDE/Projeto)
- JDK: instale JDK 7. Configure `JAVA_HOME` e verifique com `javac -version`.
- IDE: IntelliJ IDEA ou Eclipse. Nível de linguagem Java 7.
- Dependência da API:
  - Maven/Gradle (preferível) ou pasta `libs/` com `bukkit-1.5.2-Rx.jar` adicionada ao classpath.

### Exemplo Maven (instalando JAR local do Bukkit 1.5.2)
```bash
mvn install:install-file \
	-Dfile=libs/bukkit-1.5.2-R1.0.jar \
	-DgroupId=org.bukkit \
	-DartifactId=bukkit \
	-Dversion=1.5.2-R1.0 \
	-Dpackaging=jar
```
```xml
<!-- pom.xml -->
<project>
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.seuorg</groupId>
	<artifactId>seu-plugin</artifactId>
	<version>1.0.0</version>
	<name>seu-plugin</name>
	<properties>
		<maven.compiler.source>1.7</maven.compiler.source>
		<maven.compiler.target>1.7</maven.compiler.target>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.bukkit</groupId>
			<artifactId>bukkit</artifactId>
			<version>1.5.2-R1.0</version>
			<scope>provided</scope>
		</dependency>
	</dependencies>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.1</version>
				<configuration>
					<source>1.7</source>
					<target>1.7</target>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>2.3</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals><goal>shade</goal></goals>
						<configuration>
							<minimizeJar>true</minimizeJar>
							<relocations>
								<relocation>
									<pattern>com.google.gson</pattern>
									<shadedPattern>com.seuorg.seuplugin.lib.gson</shadedPattern>
								</relocation>
							</relocations>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
</project>
```

### Exemplo Gradle (JAR local)
```groovy
apply plugin: 'java'
sourceCompatibility = 1.7
targetCompatibility = 1.7
repositories { flatDir { dirs 'libs' } }
dependencies { compileOnly name: 'bukkit-1.5.2-R1.0' }
```

## 3) Estrutura de um Plugin 1.5.2
- Classe principal deve estender `org.bukkit.plugin.java.JavaPlugin`.
- Ciclo de vida:
	- `onLoad()` — inicializações mínimas.
	- `onEnable()` — registrar comandos, listeners, carregar config.
	- `onDisable()` — salvar estado, cancelar tarefas, liberar recursos.

### Exemplo de classe principal
```java
package com.seuorg.seuplugin;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public final class SeuPlugin extends JavaPlugin {
	private Logger logger;

	@Override
	public void onLoad() {
		// Inicializações mínimas
	}

	@Override
	public void onEnable() {
		this.logger = getLogger();
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		getCommand("seucomando").setExecutor(new SeuComandoExecutor(this));
		logger.info("Plugin habilitado");
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		logger.info("Plugin desabilitado");
	}
}
```

### Boas práticas de ciclo de vida
- Nunca bloqueie `onEnable` com I/O pesado.
- Cancele tarefas agendadas no `onDisable`.
- Evite singletons globais; prefira injeção via construtor.
- Use `getDataFolder()` para arquivos próprios; chame `saveDefaultConfig()` para criar `config.yml` padrão.

## 4) `plugin.yml` — Campos, Comandos e Permissões
- Localização: `src/main/resources/plugin.yml`.
- Campos básicos:
```yaml
name: SeuPlugin
main: com.seuorg.seuplugin.SeuPlugin
version: 1.0.0
author: SeuNome
website: https://exemplo.com
load: POSTWORLD
commands:
  seucomando:
    description: Executa algo
    usage: /<command> [args]
    permission: seuplugin.use
permissions:
  seuplugin.use:
    description: Permite usar o comando principal
    default: true
```
- Dicas:
  - Declare todas as permissões no `plugin.yml`.
  - Forneça `usage` claro; o Bukkit mostra automaticamente quando o executor retorna `false`.
  - Use `depend`/`softdepend` para ordenar carregamento (ex.: `Vault`).

## 5) Eventos (Listeners): modelo, prioridades, exemplos
- Modelo:
	- Implementar `org.bukkit.event.Listener` e anotar handlers com `@EventHandler`.
	- Registrar via `PluginManager.registerEvents(listener, plugin)`.
	- Prioridades: `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR`.
	- `ignoreCancelled = true` para pular eventos já cancelados.
	- Regra: não modifique o estado no `MONITOR` (somente leitura/logs).

### Exemplo: Listener de jogador/bloco
```java
package com.seuorg.seuplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Player;

public final class PlayerListener implements Listener {
	private final SeuPlugin plugin;

	public PlayerListener(SeuPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		p.sendMessage("Bem-vindo ao servidor 1.5.2!");
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		// Lógica leve; evite I/O aqui
	}

	@EventHandler
	public void onAsyncChat(AsyncPlayerChatEvent event) {
		// Cuidado: assíncrono! Não use API que toca mundo/entidades.
		plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
			@Override public void run() {
				// Interação segura com Bukkit aqui
			}
		});
	}
}
```

### Guias rápidos por grupo de eventos
- Jogador: `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerInteractEvent`, `PlayerMoveEvent`, `PlayerKickEvent`.
- Blocos: `BlockBreakEvent`, `BlockPlaceEvent`, `BlockDamageEvent`.
- Inventário: `InventoryClickEvent`, `InventoryDragEvent`.
- Entidades: `EntityDamageEvent`, `EntityDeathEvent`, `CreatureSpawnEvent`.
- Mundo: `ChunkLoadEvent`, `ChunkUnloadEvent`, `WeatherChangeEvent`.
- Chat: `AsyncPlayerChatEvent` (assíncrono; restrições).

## 6) Comandos e Autocompletar (Tab Complete)
- Executor:
```java
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class SeuComandoExecutor implements CommandExecutor {
	private final SeuPlugin plugin;

	public SeuComandoExecutor(SeuPlugin plugin) { this.plugin = plugin; }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("seuplugin.use")) {
			sender.sendMessage("§cSem permissão.");
			return true;
		}
		if (args.length == 0) {
			sender.sendMessage("Uso: /" + label + " <subcomando>");
			return true;
		}
		String sub = args[0].toLowerCase();
		if (sub.equals("ping")) {
			sender.sendMessage("pong");
			return true;
		}
		sender.sendMessage("Subcomando desconhecido.");
		return true;
	}
}
```
- Autocompletar: em 1.5.2, `TabCompleter` pode estar disponível dependendo do build; como fallback, mantenha lista fixa ou verifique condicionalmente.
```java
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.Arrays;
import java.util.List;

public final class SeuTab implements TabCompleter {
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (args.length == 1) return Arrays.asList("ping", "help");
		return null; // delega ao Bukkit
	}
}
```
- Registro:
```java
getCommand("seucomando").setExecutor(new SeuComandoExecutor(this));
// se suportado
getCommand("seucomando").setTabCompleter(new SeuTab());
```
- Diferenciar Console vs Player: verifique `sender instanceof Player`.

## 7) Permissões e Integração com Vault
- Permissões no `plugin.yml` (ver seção 4) e uso em runtime via `CommandSender#hasPermission`.
- Integração com Vault (perms/economia/chat) — `ServicesManager` para obter provedores.
```java
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;

private Permission perms;

private boolean setupPermissions() {
	RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
	if (rsp != null) perms = rsp.getProvider();
	return perms != null;
}
```
- Trate ausência de Vault com fallback (verifique nulo). Para economia e chat, padrões similares.

## 8) Tarefas e Concorrência (Scheduler, Async vs Main Thread)
- Use `Bukkit.getScheduler()` para agendar tarefas.
- `runTask`/`runTaskLater`/`runTaskTimer`: executam no main thread.
- `runTaskAsynchronously`/`runTaskTimerAsynchronously`: para I/O e CPU fora do main thread. Nunca toque mundo/entidades em async.

### Exemplos
```java
// Delay de 20 ticks (1 segundo)
getServer().getScheduler().runTaskLater(this, new Runnable() {
	@Override public void run() {
		// interação com API Bukkit
	}
}, 20L);

// Tarefa periódica sincronizada (a cada 5 segundos)
getServer().getScheduler().runTaskTimer(this, new Runnable() {
	@Override public void run() {
		// loop leve e determinístico
	}
}, 0L, 100L);

// Tarefa assíncrona para I/O
getServer().getScheduler().runTaskAsynchronously(this, new Runnable() {
	@Override public void run() {
		// I/O de arquivos ou JDBC aqui
	}
});
```
- Cancelamento: guarde `BukkitTask` retornado ou cancele tudo em `onDisable()` com `getServer().getScheduler().cancelTasks(this)`.

## 9) Configuração (YAML): `FileConfiguration`, defaults, reload
- Arquivo: `config.yml` em `resources`. Copiado para `plugins/SeuPlugin/config.yml`.
- Padrão recomendado:
```java
saveDefaultConfig(); // copia config.yml inicial se não existir
getConfig().addDefault("feature.enabled", true);
getConfig().options().copyDefaults(true);
saveConfig();
```
- Leitura segura:
```java
boolean enabled = getConfig().getBoolean("feature.enabled", true);
int max = getConfig().getInt("limits.max", 10);
String message = getConfig().getString("messages.welcome", "Olá");
```
- Reload controlado (evitar travas):
```java
reloadConfig();
// revalidar valores e recalcular caches se necessário
```
- YAML programático extra: `YamlConfiguration.loadConfiguration(File)` para outros arquivos.

## 10) Mundo, Blocos, Entidades e Inventários
- Classes-chave: `World`, `Location`, `Chunk`, `Block`/`BlockState`, `Material`, `Entity`, `Player`, `ItemStack`, `Inventory`.

### Exemplos práticos
```java
// Teleporte
player.teleport(new Location(player.getWorld(), 100, 65, 100));

// Setar bloco
Location l = player.getLocation();
l.getBlock().setType(org.bukkit.Material.GOLD_BLOCK);

// Criar item
ItemStack item = new ItemStack(org.bukkit.Material.DIAMOND_SWORD, 1);

// Inventário custom
org.bukkit.inventory.Inventory inv = getServer().createInventory(null, 27, "Menu");
inv.setItem(13, item);
player.openInventory(inv);
```

- Dicas 1.5.2:
	- `ItemMeta` existe, porém com recursos limitados vs versões modernas.
	- Evite segurar `Player` por muito tempo; armazene nomes e re-resolva (`getPlayerExact`).
	- `Metadata` API (entidades/blocos) está disponível: útil para anexar dados temporários.

## 11) Efeitos, Som e Chat
```java
// Efeito e som
player.playEffect(player.getLocation(), org.bukkit.Effect.MOBSPAWNER_FLAMES, 0);
player.playSound(player.getLocation(), org.bukkit.Sound.LEVEL_UP, 1.0f, 1.0f);

// Chat com cores (ChatColor)
player.sendMessage("§aVerde §cVermelho");
player.sendMessage(org.bukkit.ChatColor.GOLD + "Dourado");
```

## 12) Scoreboard (1.5.2): limites e exemplos
- Limites de comprimento: mantenha nomes/linhas curtos (≈16 chars típicos) para segurança.
- Exemplo:
```java
import org.bukkit.scoreboard.*;

ScoreboardManager manager = getServer().getScoreboardManager();
Scoreboard board = manager.getNewScoreboard();
Objective o = board.registerNewObjective("obj", "dummy");
o.setDisplaySlot(DisplaySlot.SIDEBAR);
o.setDisplayName("Estatísticas");

Score s1 = o.getScore("Kills");
s1.setScore(10);
player.setScoreboard(board);
```
- Dicas: evite updates por tick; agregue; remova antes de recriar.

## 13) NMS/CraftBukkit Avançado: isolamento por reflexão (e NBT)
- Estrutura de pacote sugere versão, ex.: `net.minecraft.server.v1_5_R3`.
- Descobrir versão em runtime:
```java
String pkg = getServer().getClass().getPackage().getName();
String version = pkg.substring(pkg.lastIndexOf('.') + 1); // ex.: v1_5_R3
```
- Carregar classes dinamicamente:
```java
Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
Class<?> nmsPlayerClass = Class.forName("net.minecraft.server." + version + ".EntityPlayer");
```
- Acesso ao handle via reflexão para evitar dependência direta e compilar sem CraftBukkit:
```java
Object craft = craftPlayerClass.cast(player);
java.lang.reflect.Method getHandle = craftPlayerClass.getMethod("getHandle");
Object handle = getHandle.invoke(craft); // instancia de EntityPlayer
```
- NBT de `ItemStack` em 1.5.2 (via NMS):
```java
// Conversão entre Bukkit e NMS
Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit." + version + ".inventory.CraftItemStack");
java.lang.reflect.Method asNMSCopy = craftItemStack.getMethod("asNMSCopy", org.bukkit.inventory.ItemStack.class);
Object nmsStack = asNMSCopy.invoke(null, bukkitItem);

// Obter/definir tag NBT (estruturas e nomes variam entre R1/R2/R3)
Class<?> nbtTagCompound = Class.forName("net.minecraft.server." + version + ".NBTTagCompound");
Object tag = nbtTagCompound.newInstance();
// chame métodos como setString/setInt conforme disponíveis nessa R*
```
- Riscos: quebras entre R1/R2/R3; encapsule em adaptadores e capture exceções.

## 14) Persistência e I/O (arquivos, JDBC, caches)
- Arquivos YAML/JSON leves: preferível para configs e dados simples.
- Dados relacionais: JDBC puro (MySQL/SQLite) com cautela e async.
```java
// Exemplo simples com SQLite
Class.forName("org.sqlite.JDBC");
java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/data.db");
// Use try-with-resources (Java 7) e rode em async
```
- Boas práticas:
	- Faça I/O em tarefas assíncronas.
	- Use caches quando necessário e invalide corretamente.
	- Sincronize acessos compartilhados ou confine por thread.

## 15) ProtocolLib (opcional) e Mensageria BungeeCord
- ProtocolLib (1.5.2): interceptação/injeção de pacotes. Trate como `softdepend` e código isolado.
- BungeeCord Plugin Messaging Channel (`BungeeCord`):
```java
// Envio de mensagem para BungeeCord (conectar a servidor, etc.)
byte[] bytes = /* payload conforme especificação BungeeCord 1.5.2 */ new byte[]{};
player.sendPluginMessage(this, "BungeeCord", bytes);
```
- Dicas: valide compat; documente requisitos no `plugin.yml`.

## 16) Boas Práticas e Anti‑padrões (1.5.2)
- Não mantenha `Player`/`Entity` em singletons; armazene chaves (nome) e recupere dinamicamente.
- Desregistre listeners quando apropriado; confie na desativação do plugin.
- Não chame API do Bukkit fora do main thread.
- Valide entradas de comando; forneça mensagens de erro claras.
- Trate exceções com logs detalhados (use `getLogger()`; evite `System.out`).
- Evite loops pesados por tick; particione trabalho via scheduler.
- Pré-compute coleções (`EnumSet`, `HashSet`) fora de hotpaths.

## 17) Depuração, Testes Locais e Performance
- Ambiente local: servidor CraftBukkit/Spigot 1.5.2; copie seu JAR para `plugins/`.
- Logs úteis: prefixos, IDs, métricas simples (tempos com `System.nanoTime()`).
- Testes manuais: cenários por evento/comando; permissões; concorrência (chat assíncrono).
- Perf: evite alocações em laços tight; reuse objetos simples quando possível.

## 18) Checklist de Release e Distribuição
- `pom.xml`/`build.gradle` com target 1.7.
- `plugin.yml` completo (comandos, permissões, usage, depend/softdepend).
- Sem configs locais embaladas indevidamente.
- Testado em servidor 1.5.2 real.
- Changelog e versionamento claro.

## 19) Apêndices: Templates, Snippets, Tabelas e Índice Remissivo

### A) Template `plugin.yml` completo
```yaml
name: SeuPlugin
main: com.seuorg.seuplugin.SeuPlugin
version: 1.0.0
author: SeuNome
description: Plugin exemplo 1.5.2
website: https://exemplo.com
load: POSTWORLD
depend: []
softdepend: [Vault]
commands:
  seucomando:
    description: Comando principal
    usage: /<command> [sub]
    aliases: [sp]
    permission: seuplugin.use
permissions:
  seuplugin.*:
    description: Todas permissões do plugin
    default: op
    children:
      seuplugin.use: true
  seuplugin.use:
    description: Usar o comando
    default: true
```

### B) Snippets comuns
```java
// Envio seguro ao main thread
getServer().getScheduler().runTask(this, new Runnable() {
	@Override public void run() { /* ... */ }
});

// Guardar nome em vez de Player
String playerKey = player.getName();

// Carregar config extra
File f = new File(getDataFolder(), "dados.yml");
YamlConfiguration data = YamlConfiguration.loadConfiguration(f);
```

### C) Estilo “Javadoc” para itens críticos
- `@EventHandler(priority, ignoreCancelled)` — SEM modificar estado em `MONITOR`.
- `CommandExecutor#onCommand(CommandSender sender, Command command, String label, String[] args)`
  - `@param sender` — jogador ou console
  - `@param command` — metadados do comando
  - `@param label` — rótulo usado
  - `@param args` — argumentos
  - `@return` `true` se tratado; `false` para exibir `usage`
- `BukkitScheduler` — use apenas async para I/O; todas interações de mundo no main thread.
- `FileConfiguration` — `addDefault`, `options().copyDefaults(true)`, `saveConfig()`

### D) Índice remissivo (classes/eventos chave)
- `JavaPlugin`, `PluginManager`, `Bukkit`
- `Listener`, `EventHandler`, `EventPriority`
- `CommandExecutor`, `TabCompleter`
- `World`, `Location`, `Chunk`, `Block`, `Material`, `Entity`, `Player`
- `ItemStack`, `Inventory`, `ItemMeta`
- `ChatColor`, `Effect`, `Sound`
- `Scoreboard`, `Objective`, `Team`, `DisplaySlot`
- `BukkitScheduler`, `BukkitRunnable`
- `FileConfiguration`, `YamlConfiguration`
- Vault `Permission`

---

Este guia é auto-suficiente para criar plugins 1.5.2 robustos e levar a versão ao limite com segurança. Para referência API com todos os membros, gere o Javadoc do Bukkit/Spigot 1.5.2 e use-o junto deste guia (veja o documento complementar: Javadoc a partir do fonte → Markdown).
