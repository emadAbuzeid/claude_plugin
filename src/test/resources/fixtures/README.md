# Fixtures del CLI de Claude Code — NO se commitean

Los archivos `*.ndjson` en este directorio se generan localmente con el CLI `claude`
y contienen metadata de sesión (session IDs, contenido de memoria, paths locales).
Cada desarrollador genera los suyos para correr los tests de `ClaudeEventTest`.

## Regeneración

```bash
cd $(git rev-parse --show-toplevel)
mkdir -p src/test/resources/fixtures

claude -p --output-format stream-json --verbose \
  "di hola en 5 palabras" \
  > src/test/resources/fixtures/hello.ndjson

claude -p --output-format stream-json --verbose \
  "lista los archivos del directorio actual" \
  > src/test/resources/fixtures/tool_use_ls.ndjson

claude -p --output-format stream-json --verbose \
  "lee el README.md y resumilo en una frase" \
  > src/test/resources/fixtures/tool_use_read.ndjson
```
