# Gerando Javadoc do Bukkit/Spigot 1.5.2 e Convertendo para Markdown

Este guia descreve como obter o código-fonte da API 1.5.2, gerar a documentação Javadoc e convertê-la para `.md`, para uso junto ao guia principal.

## 1) Obter o código-fonte (Bukkit/Spigot 1.5.2)
- Bukkit: clone o repositório e faça checkout da tag/commit correspondente à 1.5.2 (ex.: `1.5.2-R1.0`).
```bash
git clone https://github.com/Bukkit/Bukkit.git
cd Bukkit
# Selecione a tag/commit da 1.5.2
# git checkout 1.5.2-R1.0
```
- Spigot/CraftBukkit: versões 1.5.2 são legadas. Use um CraftBukkit/Spigot 1.5.2 de arquivo local ou repositório legado compatível, respeitando licenças. Para fins de Javadoc de API, o Bukkit é suficiente.

## 2) Gerar Javadoc
- Via Maven (plugin oficial):
```xml
<!-- pom.xml do projeto da API ou wrapper -->
<build>
	<plugins>
		<plugin>
			<groupId>org.apache.maven.plugins</groupId>
			<artifactId>maven-javadoc-plugin</artifactId>
			<version>2.10.4</version>
			<configuration>
				<source>1.7</source>
				<encoding>UTF-8</encoding>
				<charset>UTF-8</charset>
			</configuration>
		</plugin>
	</plugins>
</build>
```
- Execute:
```bash
mvn -q clean package -DskipTests
mvn -q javadoc:javadoc
```
- Resultado típico: HTML em `target/site/apidocs`.

- Alternativa IDE (Eclipse/IntelliJ): `Project > Generate Javadoc...` e selecione os pacotes da API.

## 3) Converter HTML → Markdown
- Opção A — Pandoc:
```bash
# converter todo o diretório apidocs para md mantendo links relativos
for f in target/site/apidocs/*.html; do \
	pandoc -f html -t gfm "$f" -o "docs_api_md/$(basename "${f%.html}").md"; \
	done
```
- Opção B — Turndown (Node.js):
```bash
npm init -y --silent
npm install turndown jsdom --silent
```
```javascript
// tools/html2md.js
const fs = require('fs');
const path = require('path');
const TurndownService = require('turndown');
const turndownService = new TurndownService({ codeBlockStyle: 'fenced' });
const src = 'target/site/apidocs';
const dst = 'docs_api_md';
if (!fs.existsSync(dst)) fs.mkdirSync(dst);
for (const file of fs.readdirSync(src)) {
	if (!file.endsWith('.html')) continue;
	const html = fs.readFileSync(path.join(src, file), 'utf8');
	const md = turndownService.turndown(html);
	fs.writeFileSync(path.join(dst, file.replace(/\.html$/, '.md')), md);
}
```
```bash
node tools/html2md.js
```

## 4) Organização e Cross‑linking
- Mantenha a estrutura de pacotes como diretórios em `docs_api_md/`.
- Crie um índice principal `docs_api_md/README.md` com links para classes/interfaces/eventos mais usados.
- No guia principal (`docs/mc152-bukkit-spigot-plugin-dev.md`), linke seções para os `.md` específicos da API quando desejar mais detalhes por membro.

## 5) Notas de Compatibilidade (1.5.2)
- Alguns nomes de classes/métodos variam entre `R1/R2/R3`. Ao converter, guarde a origem (ex.: `v1_5_R3`).
- NMS/CraftBukkit não possuem Javadoc oficial; caso necessário, gere a partir das fontes ou mantenha trechos exemplificativos no guia principal com ressalvas.

## 6) Publicação
- Versione `docs_api_md/` junto ao repositório.
- Opcional: publique com um gerador estático (ex.: GitHub Pages) para consulta web.

## 7) Dicas de Qualidade
- Configure `-Xdoclint:none` não é aplicável ao Java 7; evite construções que quebrem o javadoc.
- Garanta encoding UTF‑8 nas execuções.

Com isso, você terá a API em `.md` e este repositório servirá como fonte única de verdade para 1.5.2, em conjunto com o guia principal.
