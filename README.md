# Claude Chat — Plugin nativo de Claude Code para IntelliJ

> Un Tool Window nativo al estilo Copilot Chat, directamente integrado en IntelliJ.
> Sin terminal embebida. Sin hacks. Markdown, diffs y tool-calls con UI de primera.

![Status](https://img.shields.io/badge/status-v0.0.1%20%E2%80%94%20scaffolding-blue)
![Target IDE](https://img.shields.io/badge/IntelliJ-2024.3%2B-orange)
![Kotlin](https://img.shields.io/badge/kotlin-2.1-purple)
![JDK](https://img.shields.io/badge/JDK-17-green)

---

## ¿Por qué existe este plugin?

El plugin oficial de Claude Code para IntelliJ **embebe la terminal y poco más**. No formatea los
mensajes, no renderiza markdown, no muestra `tool_use` como tarjetas, no abre los diffs con el
`DiffManager` nativo del IDE, no integra con el sistema de acciones.

La extensión de VS Code en cambio tiene un chat propio decente, con markdown, tool cards y diffs
nativos. Este proyecto apunta a **paridad funcional y visual con esa experiencia**, dentro de
IntelliJ.

---

## Estado actual (v0.0.1)

| Área | Estado |
|---|---|
| Scaffolding Gradle + IntelliJ Platform Plugin 2.x | ✅ |
| `plugin.xml` + Tool Window registrado en panel derecho | ✅ |
| Placeholder UI visible en el IDE | ✅ |
| Smoke test del bundle de recursos | ✅ |
| Integración con el CLI `claude` | ⏳ Fase 2 |
| Transcript con markdown (JCEF) | ⏳ Fase 4 |
| Input + context chips + selectores | ⏳ Fase 5 |
| Acciones + Settings page | ⏳ Fase 6 |

---

## Arquitectura (resumen)

```
┌───────────────────────────────────────────────────────────┐
│  Tool Window (Swing + JB components)                      │
│                                                           │
│   ┌────────────────────────────────────────┐              │
│   │ Transcript (JCEF + markdown + Prism)   │  ← Fase 4    │
│   └────────────────────────────────────────┘              │
│                                                           │
│   ┌─ Context chips ───────────────────────┐               │
│   ├─ Input (placeholder: # @ /) ──────────┤  ← Fase 5    │
│   └─ Agent / Auto selectors ──────────────┘               │
└────────────────────────┬──────────────────────────────────┘
                         │ StateFlow<List<ChatItem>>
                         ▼
            ┌────────────────────────────┐
            │  ChatSession (por proyecto)│  ← Fase 3
            │  ClaudeSessionService      │
            └────────────┬───────────────┘
                         │ Flow<ClaudeEvent>
                         ▼
            ┌────────────────────────────┐
            │  ClaudeProcess             │  ← Fase 2
            │  StreamJsonCodec (NDJSON)  │
            │  ClaudeCliLocator          │
            └────────────┬───────────────┘
                         │ stdin / stdout
                         ▼
               $ claude -p --output-format stream-json \
                        --input-format stream-json --verbose
```

**Decisiones clave**:
- **Stack**: Kotlin + Gradle (IntelliJ Platform Plugin Template 2.x)
- **UI**: Swing (JB components) + JCEF para el transcript de markdown, fallback a `JEditorPane`
- **Integración con Claude**: subproceso del CLI `claude` consumiendo `--output-format stream-json`
  — NO rehacemos la API ni usamos el Agent SDK
- **Markdown**: `commonmark-java` (ya bundled en IntelliJ) + Prism.js para highlighting
- **Diffs**: `DiffManager` nativo del SDK
- **Ciclo de vida**: `@Service(Service.Level.PROJECT)` con `Disposer` encadenado

---

## Roadmap

El trabajo está fraccionado en 6 fases + release, trackeadas en Jira interno (BDGRN).
Cada fase empieza con **tests** (TDD obligatorio) y termina con **revisión manual** antes del
commit.

| Fase | Alcance | Ticket |
|---|---|---|
| 1 | Scaffolding Gradle + plugin.xml + Tool Window placeholder | ✅ BDGRN-6869 |
| 2 | CLI layer — `ClaudeProcess`, `ClaudeEvent`, `StreamJsonCodec`, `ClaudeCliLocator` | BDGRN-6870 |
| 3 | Session layer — `ClaudeSessionService`, `ChatSession`, `ChatItem` | BDGRN-6871 |
| 4 | UI transcript — `JBCefBrowser`, `MarkdownRenderer`, `web/*` resources | BDGRN-6872 |
| 5 | UI input + `ChatPanel` — ensamble completo | BDGRN-6873 |
| 6 | Actions + Settings — `Ctrl+Alt+K`, Settings page | BDGRN-6874 |
| — | Release v0.1 — verificación E2E + packaging | BDGRN-6875 |

> Los tickets viven en el Jira interno de Telefónica (`jira.tid.es`) — no son accesibles
> públicamente. La épica madre es **BDGRN-6868**.

### Features del MVP v0.1

El usuario podrá:

1. Abrir **Claude Chat** en el panel derecho del IDE
2. Crear una nueva sesión con **"+ New Agent Session"** (spawnea `claude` en el root del proyecto)
3. Enviar prompts (Enter) / salto de línea (Shift+Enter); ver la respuesta streameada con markdown
4. Ver cada `tool_use` como tarjeta colapsable ("Read X.kt", "Edit foo.kt")
5. Click en **"View diff"** → abre el `DiffManager` nativo de IntelliJ
6. Adjuntar el archivo actual como contexto con `Ctrl+Alt+K` o vía menú contextual
7. Cancelar peticiones en curso con el botón **Stop** (envía `SIGINT`)
8. Configurar path del CLI y modo de permisos en Settings → Tools → Claude Chat

### Diferido a v0.2+

Slash commands picker, `#` symbol search, UI para MCP, múltiples tabs, `--resume`,
prompts de permisos interactivos, imágenes adjuntas, custom agents.

---

## Requisitos

### Para usar el plugin
- **IntelliJ IDEA** 2024.3 o superior (Community o Ultimate)
- **JDK 17+** (incluido en IntelliJ)
- **Claude Code CLI** instalado y autenticado — ver
  [instrucciones oficiales](https://docs.anthropic.com/en/docs/claude-code/setup)
  - El plugin NO maneja login ni autenticación. Corré `claude` una vez en la terminal antes de
    abrir el IDE.

### Para desarrollar
- Las anteriores, más:
- Git
- Conexión a Maven Central + repositorio de IntelliJ Platform (lo resuelve Gradle)

---

## Desarrollo

### Clonar y correr en sandbox

```bash
git clone git@github.com:emadAbuzeid/claude_plugin.git
cd claude_plugin

./gradlew test          # corre los tests unitarios
./gradlew runIde        # arranca un IDE sandbox con el plugin cargado
```

La primera vez `./gradlew runIde` puede tardar varios minutos — descarga IntelliJ Community 2024.3
en `build/idea-sandbox/`. Después es rápido.

### Otros comandos útiles

```bash
./gradlew buildPlugin   # genera el .zip distribuible en build/distributions/
./gradlew verifyPlugin  # corre el plugin verifier contra IDEs target
./gradlew wrapper       # regenera el wrapper (si cambiamos la versión de Gradle)
```

### Estructura del proyecto

```
claude_plugin/
├── build.gradle.kts              Plugin IntelliJ Platform + Kotlin + kotlinx.serialization
├── settings.gradle.kts
├── gradle.properties             Versiones, IDE target, pluginSinceBuild
├── gradle/wrapper/
├── gradlew / gradlew.bat
└── src/
    ├── main/
    │   ├── kotlin/dev/emad/claudechat/
    │   │   ├── ClaudeChatBundle.kt         i18n
    │   │   ├── ClaudeChatIcons.kt          Icon loader
    │   │   ├── cli/                        ← Fase 2
    │   │   ├── session/                    ← Fase 3
    │   │   ├── ui/
    │   │   │   ├── ChatToolWindowFactory.kt
    │   │   │   ├── ChatPanel.kt            ← Fase 5
    │   │   │   ├── transcript/             ← Fase 4
    │   │   │   └── input/                  ← Fase 5
    │   │   ├── actions/                    ← Fase 6
    │   │   └── settings/                   ← Fase 6
    │   └── resources/
    │       ├── META-INF/plugin.xml
    │       ├── messages/ClaudeChatBundle.properties
    │       ├── icons/claudeToolWindow.svg
    │       └── web/                        ← Fase 4 (transcript.html, styles, prism)
    └── test/
        └── kotlin/dev/emad/claudechat/
            └── ClaudeChatBundleTest.kt
```

---

## Convenciones de desarrollo

Reglas firmes del proyecto — documentadas en `~/.claude/skills/kotlin/SKILL.md`:

### 1. Tipos explícitos SIEMPRE
Cada `val`, `var`, parámetro y return type no-`Unit` lleva anotación explícita, aunque la
inferencia funcione. Excepción: funciones que devuelven `Unit` (seguimos la convención oficial
de Kotlin y omitimos `: Unit`).

```kotlin
// ✅
val name: String = "claude"
fun greet(user: String): String = "hi, $user"

// ❌
val name = "claude"
fun greet(user: String) = "hi, $user"
```

### 2. TDD obligatorio
Tests primero, código de producción después. `kotlin.test` + JUnit 5 vía `useJUnitPlatform()`.
Fixtures reales (capturados de ejecuciones verdaderas del CLI) en vez de JSON sintético.

### 3. Nunca commitear sin permiso explícito
El autor revisa cada fase antes de que aterrice en git history. Ningún commit automático.

### 4. Commits convencionales, sin atribución de AI
Formato `<type>: <description>` con body opcional. Sin `Co-Authored-By`.

### 5. Inmutabilidad por defecto
`val` > `var`. `List<T>` > `MutableList<T>` en APIs públicas. `data class` para value objects.

### 6. Coroutines con structured concurrency
Scope-bound siempre. Nunca `GlobalScope`. `Dispatchers.IO` para I/O, `Dispatchers.Default` para
CPU, `Dispatchers.EDT` (de `com.intellij.openapi.application`) para UI.

Ver el archivo de skill para el listado completo (15 patrones) — cubre null handling, sealed
classes, kotlinx.serialization, scope functions, IntelliJ Platform essentials y anti-patterns.

---

## Riesgos conocidos

| Riesgo | Mitigación |
|---|---|
| JCEF no disponible en algún JBR | Gate `JBCefApp.isSupported()` + fallback a `JEditorPane` |
| Schema de `stream-json` cambia entre versiones del CLI | `ClaudeCliLocator` valida `claude --version` |
| El sandbox del IDE tiene PATH distinto al shell del usuario | Setting explícito para path absoluto al binario |
| Windows `.cmd` shim del CLI | Tests específicos + `GeneralCommandLine.withWorkDirectory` |
| Flood de eventos al EDT | Coalescer de ~30 ms con `channelFlow` + `debounce` |
| Autenticación del CLI | No la manejamos — el usuario corre `claude` una vez antes |

---

## Licencia

TBD — pendiente definir antes de la release v0.1.

---

## Autor

[Emad Abuzeid Alvarez](https://github.com/emadAbuzeid) — `emad.abuzeidalvarez@telefonica.com`
