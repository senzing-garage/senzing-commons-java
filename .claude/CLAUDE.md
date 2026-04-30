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

This project ships a local FAQ MCP server registered in `.mcp.json` under
the name `senzing-commons-faq`. It serves both:

- **Shared FAQs** from the standards-repo submodule
  (`.java-coding-standards/docs/faqs/`) — coding standards, javadoc reflow
  rules, system-stubs/ResourceLock test pattern, FAQ-authoring conventions.
- **Project-local FAQs** from `.claude/faqs/<category>/<topic>.md` —
  project-specific architecture, conventions, build/release notes,
  troubleshooting (including this project's `source-edit-policy` rule).

The server merges both into one BM25-ranked search index. Tool surface:

- `mcp__senzing-commons-faq__get_faq_categories`
- `mcp__senzing-commons-faq__search_faqs(query=...)`
- `mcp__senzing-commons-faq__get_faq(title=...)`

**Use it BEFORE making design assumptions or troubleshooting.** Specifically:

- Before changing build, test, or release configuration (`pom.xml`, surefire,
  checkstyle, jacoco, spotbugs, GPG, `release` profile), call `search_faqs`
  for relevant topics.
- Before modifying public APIs in `cmdline`, `io`, `sql`, `util`, etc., search
  for any documented invariants or rationale.
- When a build, test, or dependency issue surfaces, search the
  `troubleshooting` category first.
- When unsure what is documented, call `get_faq_categories` to enumerate
  what's available.

**After resolving a non-obvious issue**, ask the user whether to capture the
solution as a new FAQ. Project-specific lessons go in
`.claude/faqs/<category>/<topic>.md`. Lessons about the standards themselves
go via PR to `senzing-garage/java-coding-standards`. Restart the session so
the server re-indexes.

FAQs are pulled on demand, so detail is cheap there. Keep CLAUDE.md lean and
push operational/troubleshooting depth into FAQ files.

## Project Overview

Senzing Commons Java Library is a collection of reusable Java utilities, interfaces, and classes common to multiple Senzing projects. Originally refactored from senzing-api-server, this library provides foundational components for command-line parsing, database connection pooling, I/O operations, reflection utilities, and more.

The current version is `4.0.0` (see `CHANGELOG.md` for the per-release history).

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

Tests use JUnit Jupiter 6 with parallel execution enabled (configured in
`pom.xml` surefire plugin):

- Classes run concurrently.
- Methods within a class run in same thread (default).
- Dynamic parallelism factor (1x number of cores).
- System property `project.build.directory` available in tests.

#### System Stubs, ExecutionMode, and ResourceLock

Tests that **stub environment variables** or **capture stdout / stderr**
must follow the project's `system-stubs` + `@Execution(SAME_THREAD)` +
`@ResourceLock` pattern to avoid build-log noise and inter-class capture
races. Before writing such a test, search the FAQ:
`mcp__senzing-commons-faq__search_faqs(query="system stubs")`.

Headline rules:

- Use `system-stubs-jupiter` **programmatically at the method level**
  (`new EnvironmentVariables(...).execute(...)`, `new SystemOut().execute(...)`,
  `new SystemErr().execute(...)`) — never the `@ExtendWith` annotation form.
- Tag the test (or the class) with `@Execution(ExecutionMode.SAME_THREAD)`
  — `System.setOut` / `setErr` are JVM-wide, so concurrent redirects race.
- Add `@ResourceLock(Resources.SYSTEM_OUT)` and/or
  `@ResourceLock(Resources.SYSTEM_ERR)` for cross-class mutual exclusion.
  When both are present, **always declare `SYSTEM_OUT` first, `SYSTEM_ERR`
  second** to avoid deadlock.
- `LoggingUtilities.logDebug` writes to **`System.out`** (not stderr); use
  `SystemOut` to capture it.
- If the production code starts a background thread in its constructor,
  place the `new ...()` call **inside** the `stub.execute(...)` lambda so
  the redirect is active before the thread starts.

Full pattern, examples, and the JVM-warning suppression details are in the
shared `testing/system-stubs-and-output-capture` FAQ (loaded automatically
by the FAQ MCP server from the submodule).

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

**IMPORTANT — apply when generating or modifying Java code:** All Java code
(new and existing) in this repository must conform to the formatting rules in
`.java-coding-standards/docs/java-coding-standards.md`. Apply these rules
**from the start** — do not write code first and reformat afterward. When in
doubt about a specific case (parameter alignment, method continuation,
ternary tier, javadoc reflow), read the full standards document or search
the FAQ:
`mcp__senzing-commons-faq__search_faqs(query="java formatting")`.

### Quick reference

- **80-character line limit** (enforced by checkstyle via `-Pcheckstyle`).
  Lines beyond 80 chars must be wrapped.
- **Allman braces** for class/interface/enum/method/constructor definitions
  (opening `{` on its own line, left-aligned with the declaration).
- **Same-line braces** for control flow: `if`/`else`/`for`/`while`/`do`/
  `try`/`catch`/`finally`/`switch`/`synchronized`, lambdas, array
  initializers, static init blocks.
- **Multi-line conditions**: when an `if`/`catch`/etc. condition wraps to
  multiple lines, the opening brace goes on its own line (Allman) to
  visually separate condition from body.
- **Method parameters** (priority order): single line if it fits; otherwise
  paren-aligned with types/names aligned in columns; otherwise next-line
  double-indented.
- **`throws` clauses** go on their own line, single-indented.
- **Continuation indentation**: 8 spaces (double indent).
- **Operators on continuation lines**: break **before** `+`, `&&`, `||`, `?`,
  `:`, `.` (the operator starts the continuation line).
- **Short-circuit `if`**: `if (cond) statement;` on one line is preferred
  (Tier 1) when it fits; otherwise add braces.
- **Javadoc**: reflow prose and `@param`/`@return`/`@throws` to fill lines
  near 80 chars; do not leave 1-3 orphan words on a line.
- **CSOFF/CSON**: only for deliberately aligned multi-line output
  (column-formatted diagnostics, ASCII art, SQL DDL with aligned clauses)
  — never a general escape hatch.

### Verification

Run checkstyle: `mvn -Pcheckstyle validate` (must report `BUILD SUCCESS`
before opening a PR).

### Bulk formatting scripts

Five scripts in `.java-coding-standards/tooling/scripts/` (run from project
root) automate common reformat passes — useful for legacy code or batch
updates, **not a substitute** for writing compliant code in the first
place:

- `python3 .java-coding-standards/tooling/scripts/fix_allman_braces.py`
- `python3 .java-coding-standards/tooling/scripts/fix_javadoc_reflow.py`
- `python3 .java-coding-standards/tooling/scripts/fix_javadoc_inline_tags.py`
- `python3 .java-coding-standards/tooling/scripts/fix_javadoc_tags.py`
- `python3 .java-coding-standards/tooling/scripts/fix_need_braces.py`

For single-file reformatting (used by the VSCode keybinding and the Claude
Code `PostToolUse` hook):

```bash
python3 .java-coding-standards/tooling/scripts/format_file.py path/to/File.java
```

The orchestrator runs all five scripts in canonical order against the
single file.

VSCode formatter config (`.java-coding-standards/tooling/ide/java-formatter.xml`)
handles Allman for methods/types and same-line for control flow but cannot
fully enforce all rules. The `building/java-formatting-standards` FAQ
summarizes day-to-day usage.
