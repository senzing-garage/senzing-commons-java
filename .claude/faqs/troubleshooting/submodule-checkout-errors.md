# Submodule Checkout Errors

The most common build failure on a fresh clone is the submodule
not being initialized. The standards live in a git submodule at
`.java-coding-standards/`, and Maven, the IDE, and the FAQ MCP
server all read files from inside it.

## Symptoms

- `mvn -Pcheckstyle validate` fails with
  `Unable to find configuration file at location
   .../.java-coding-standards/checkstyle/senzing-checkstyle.xml`.
- IDE shows no formatter applied to Java files; the
  `java.format.settings.url` setting (if configured) points at
  a path that doesn't exist.
- Claude Code reports the FAQ MCP server failed to start (the
  `command`/`args` in `.mcp.json` invoke a script that doesn't
  exist locally).

## Fix

Initialize submodules:

```bash
git submodule update --init --recursive
```

For fresh clones, prefer one-step initialization:

```bash
git clone --recurse-submodules \
  https://github.com/senzing-garage/senzing-commons-java.git
```

## CI / GitHub Actions

CI must check out submodules too. With `actions/checkout`,
set `submodules: recursive`:

```yaml
- uses: actions/checkout@v4
  with:
    submodules: recursive
```

The senzing-factory shared workflows handle this; if a custom
workflow misses it, the build fails the same way as a local
uninitialized clone.

## After a submodule pin bump

When the project bumps the `.java-coding-standards` pin
(typically to adopt a new release), local checkouts need to
refresh:

```bash
git submodule update --init --recursive
```

The `/init-java` slash command should then be re-run to refresh
any wired settings that changed between the old and new pin.
