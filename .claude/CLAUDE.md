# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workflow Preferences

**IMPORTANT**: When working in this repository:

- **Do NOT modify source code files directly** - provide suggestions instead
- **ONLY modify CLAUDE.md directly** when updating documentation
- Always inform the user before making any changes to CLAUDE.md
- Present code changes as recommendations that the user can review and apply
- When editing CLAUDE.md, follow Markdown linting and formatting rules:
  - Use Prettier for Markdown formatting
  - Follow markdownlint rules:
    - Wrap bare URLs in angle brackets: `<https://example.com>`
    - Surround lists with empty lines before and after
    - Follow standard Markdown conventions

## FAQ MCP Server

This project ships a local FAQ MCP server registered in `.mcp.json` under the
name `senzing-commons-faq`. Source: `.claude/faq_server.py`; content:
`.claude/faqs/<category>/<topic>.md`.

**Use it BEFORE making design assumptions or troubleshooting.** Specifically:

- Before changing build, test, or release configuration (`pom.xml`, surefire,
  GPG, `release` profile), call `search_faqs` for relevant topics.
- Before modifying public APIs in `cmdline`, `io`, `sql`, `util`, etc., search
  for any documented invariants or rationale.
- When a build, test, or dependency issue surfaces, search the
  `troubleshooting` category first.
- When unsure what is documented, call `get_faq_categories` to enumerate
  categories and titles.

**After resolving a non-obvious issue**, ask the user whether to capture the
solution as a new FAQ under `.claude/faqs/<category>/<topic>.md`. The
filename (dashes → spaces) becomes the searchable title. Restart the session
so the server re-indexes.

FAQs are pulled on demand, so detail is cheap there. Keep CLAUDE.md lean and
push operational/troubleshooting depth into FAQ files.

## Project Overview

Senzing Commons Java Library is a collection of reusable Java utilities, interfaces, and classes common to multiple Senzing projects. Originally refactored from senzing-api-server, this library provides foundational components for command-line parsing, database connection pooling, I/O operations, reflection utilities, and more.

The current version is `4.0.0-beta.3.0` (a beta line — see `CHANGELOG.md` for the per-release history).

## Build and Test Commands

### Basic Build Operations

```bash
# Compile without packaging
mvn compile

# Package classes and javadocs into JAR files
mvn package

# Install to local Maven repository
mvn install

# Clean previous build artifacts
mvn clean

# Combined clean and build
mvn clean package
mvn clean install
```

### Running Tests

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName

# Skip tests during build
mvn -DskipTests=true package
mvn -DskipTests=true install
```

### Generating Documentation

```bash
# Generate Javadocs (included in package/install)
mvn javadoc:javadoc

# View generated docs at target/site/apidocs/index.html
```

## Architecture Overview

### Package Structure

The codebase is organized into functional packages under `com.senzing`:

- **`cmdline`**: Command-line parsing framework with builder pattern, exception handling, and parameter processing
- **`io`**: I/O utilities including RecordReader (JSON/JSON-Lines/CSV), chunked encoding, and temporary data caching
- **`reflect`**: Reflection utilities for property manipulation and runtime introspection
- **`sql`**: Database connection management including ConnectionPool, Connector implementations (PostgreSQL, SQLite), and transaction isolation
- **`text`**: Text processing utilities
- **`util`**: General utilities including JSON, ZIP, collections, timers, worker pools, semantic versioning, and Senzing-environment helpers (`SzUtilities`, `SzInstallLocations`)
- **`naming`**: Naming conventions and utilities

### Key Architectural Components

#### Command-Line Processing (`cmdline`)

The command-line framework uses a builder pattern centered around:

- `CommandLineParser`: Interface for parsing arguments into options
- `CommandLineBuilder`: Fluent API for defining command-line options and constraints
- `CommandLineOption`: Represents individual command-line options
- Rich exception hierarchy for specific parsing errors (e.g., `ConflictingOptionsException`, `RepeatedOptionException`)

Key pattern: Options are defined declaratively, then parsed into a Map<CommandLineOption, Object> for consumption.

#### Database Connection Pooling (`sql`)

`ConnectionPool` provides sophisticated JDBC connection management:

- Dynamic pool sizing (min/max bounds)
- Connection lifecycle management (expire time, retire limit)
- Proxy-based connection tracking using `PooledConnectionHandler`
- Comprehensive statistics via `Quantified` interface
- Transaction isolation level enforcement via `TransactionIsolation` enum
- Background thread for connection expiration (`ConnectionExpireThread`)

Connection lifecycle: acquire() → proxy wrapping → use → release() → rollback uncommitted → return to pool or retire

Database-specific connectors (`PostgreSqlConnector`, `SQLiteConnector`) implement the `Connector` interface.

#### Record Reading (`io`)

`RecordReader` provides unified interface for reading structured records:

- Auto-detection of format (JSON array, JSON-Lines, CSV) from first character
- Format-specific providers: `JsonArrayRecordProvider`, `JsonLinesRecordProvider`, `CsvRecordProvider`
- Data source mapping and source ID assignment
- Error line number tracking for parsing failures

Pattern: RecordReader → RecordProvider (internal) → JsonObject output

#### Utilities (`util`)

Notable utilities:

- `JsonUtilities`: JSON parsing and manipulation helpers
- `WorkerThreadPool` / `AsyncWorkerPool`: Concurrent task execution
- `Timers`: Performance timing with statistics
- `SemanticVersion`: Version comparison following semantic versioning; supports pre-release suffixes (`-alpha`, `-beta`, `-rc`), normalized to lowercase
- `AccessToken`: Thread-safe access control
- `ErrorLogSuppressor`: Rate-limiting for error messages
- `ZipUtilities`: Archive operations
- `SzUtilities`: Builds JSON settings for Senzing environment initialization (`bootstrapSettings()` for `SzProduct`-only access without a database URI; `basicSettingsFromDatabaseUri()` for full settings)
- `SzInstallLocations`: Locates a Senzing installation on disk

### Testing Configuration

Tests use JUnit Jupiter 6 with parallel execution enabled:

- Classes run concurrently (configured in pom.xml surefire plugin)
- Methods within a class run in same thread
- Dynamic parallelism factor (1x number of cores)
- System property `project.build.directory` available in tests

## Development Notes

### Java and Maven Versions

This project requires Java 17 (the Maven compiler is set to `<release>17</release>`) and Apache Maven 3.8.5 or later. The compiler is configured with:

- `-Xlint:unchecked` for unchecked operation warnings
- `-Xlint:deprecation` for deprecated API usage warnings

### Maven Dependencies

Key runtime dependencies:

- `javax.json` (org.glassfish): JSON processing
- `commons-csv` (Apache): CSV parsing
- `commons-configuration2` (Apache): Configuration management
- `juniversalchardet`: Character encoding detection
- `icu4j` (IBM): Unicode and internationalization support

Test dependencies:

- JUnit Jupiter 6.0.3 for testing
- SQLite JDBC for database tests

### Code Patterns

**Connection Pool Usage**:

```java
ConnectionPool pool = new ConnectionPool(connector, minSize, maxSize);
Connection conn = null;
try {
    conn = pool.acquire();
    // use connection
} finally {
    pool.release(conn);
}
```

**RecordReader Usage**:

```java
try (Reader reader = new FileReader(file)) {
    RecordReader recordReader = new RecordReader(reader);
    for (JsonObject record = recordReader.readRecord();
         record != null;
         record = recordReader.readRecord()) {
        // process record
    }
}
```

**CommandLineBuilder Pattern**:

```java
CommandLineBuilder builder = new CommandLineBuilder();
builder.addOption("--verbose", Boolean.class)
       .addOption("--config", String.class, true) // required
       .addConflict("--verbose", "--quiet");
Map<CommandLineOption, Object> options = builder.parseCommandLine(args);
```

### Release Process

The project uses Maven Central for distribution:

- Snapshot repository: <https://s01.oss.sonatype.org/content/repositories/snapshots>
- Release repository: <https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/>
- GPG signing enabled via `release` profile: `mvn clean deploy -P release`
- Auto-publish configured via central-publishing-maven-plugin

### Main Branch

The main branch for pull requests is `main`.

## Java Coding Standards

**IMPORTANT — apply when generating or modifying Java code:** All Java
code (new and existing) in this repository must conform to the formatting
rules in `.claude/java-coding-standards.md`. Apply these rules **from
the start** — do not write code first and reformat afterward. When in
doubt about a specific case (parameter alignment, method continuation,
ternary tier, javadoc reflow), read the full standards document or
search the FAQ:
`mcp__senzing-commons-faq__search_faqs(query="java formatting")`.

### Quick reference

- **80-character line limit** (enforced by checkstyle via `-Pcheckstyle`).
  Lines beyond 80 chars must be wrapped.
- **Allman braces** for class/interface/enum/method/constructor
  definitions (opening `{` on its own line, left-aligned with the
  declaration).
- **Same-line braces** for control flow: `if`/`else`/`for`/`while`/
  `do`/`try`/`catch`/`finally`/`switch`/`synchronized`, lambdas, array
  initializers, static init blocks.
- **Multi-line conditions**: when an `if`/`catch`/etc. condition wraps
  to multiple lines, the opening brace goes on its own line (Allman) to
  visually separate condition from body.
- **Method parameters** (priority order): single line if it fits;
  otherwise paren-aligned with types/names columnized; otherwise
  next-line double-indented.
- **`throws` clauses** go on their own line, single-indented.
- **Continuation indentation**: 8 spaces (double indent).
- **Operators on continuation lines**: break **before** `+`, `&&`, `||`,
  `?`, `:`, `.` (the operator starts the continuation line).
- **Short-circuit `if`**: `if (cond) statement;` on one line is preferred
  (Tier 1) when it fits; otherwise add braces.
- **Javadoc**: reflow prose and `@param`/`@return`/`@throws` to fill
  lines near 80 chars; do not leave 1-3 orphan words on a line.
- **CSOFF/CSON**: only for deliberately aligned multi-line output
  (column-formatted diagnostics, ASCII art, SQL DDL with aligned
  clauses) — never a general escape hatch.

### Verification

Run checkstyle: `mvn -Pcheckstyle validate` (must report `BUILD SUCCESS`
before opening a PR).

### Bulk formatting scripts

Five scripts in `.claude/scripts/` (run from project root) automate
common reformat passes — useful when reformatting existing files but
**not a substitute** for writing compliant code in the first place:

- `python3 .claude/scripts/fix_allman_braces.py` — brace placement.
- `python3 .claude/scripts/fix_javadoc_reflow.py` — javadoc prose
  reflow.
- `python3 .claude/scripts/fix_javadoc_inline_tags.py` — javadoc
  reflow for paragraphs containing `{@link}`/`<code>` (cases the
  base reflow script skips).
- `python3 .claude/scripts/fix_javadoc_tags.py` — `@param`/`@return`/
  `@throws` description reflow.
- `python3 .claude/scripts/fix_need_braces.py` — collapses
  short-circuit `if`/`else` to single-line (Tier 1) when it fits,
  else adds braces (Tier 2).

VSCode formatter config (`.vscode/java-formatter.xml`) handles Allman
for methods/types and same-line for control flow but cannot fully
enforce all rules. The `building/java-formatting-standards` FAQ
summarizes day-to-day usage.
