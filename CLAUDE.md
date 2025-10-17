# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Senzing Commons Java Library is a collection of reusable Java utilities, interfaces, and classes common to multiple Senzing projects. Originally refactored from senzing-api-server, this library provides foundational components for command-line parsing, database connection pooling, I/O operations, reflection utilities, and more.

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
- **`util`**: General utilities including JSON, ZIP, collections, timers, worker pools, and semantic versioning
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
- `SemanticVersion`: Version comparison following semantic versioning
- `AccessToken`: Thread-safe access control
- `ErrorLogSuppressor`: Rate-limiting for error messages
- `ZipUtilities`: Archive operations

### Testing Configuration

Tests use JUnit Jupiter 5 with parallel execution enabled:
- Classes run concurrently (configured in pom.xml surefire plugin)
- Methods within a class run in same thread
- Dynamic parallelism factor (1x number of cores)
- System property `project.build.directory` available in tests

## Development Notes

### Java Version

This project requires Java 17 (OpenJDK 17.0.x). The compiler is configured with:
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
- JUnit Jupiter 5.13.4 for testing
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
- Snapshot repository: https://s01.oss.sonatype.org/content/repositories/snapshots
- Release repository: https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/
- GPG signing enabled via `release` profile: `mvn clean deploy -P release`
- Auto-publish configured via central-publishing-maven-plugin

### Main Branch

The main branch for pull requests is `main`.
