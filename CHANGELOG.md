# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
[markdownlint](https://dlaa.me/markdownlint/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0] - 2022-08-09

### Changed in 3.1.0

- Added `com.senzing.sql.ConnectionPool` class
- Added `com.senzing.sql.SQLUtilities` class
- Added `com.senzing.sql.DatabaseType` class
- Added `com.senzing.sql.TransactionIsolation` class
- Added `com.senzing.sql.ConnectionProvider` interface
- Added `com.senzing.sql.PoolConnectionProvider` class
- Added `com.senzing.sql.PooledConnectionHandler` class
- Added `com.senzing.sql.Connector` interface
- Added `com.senzing.sql.SQLiteConnector` class
- Added `com.senzing.sql.PostgreSqlConnector` class
- Added `com.senzing.naming.Registry` class
- Modified `com.senzing.util.ZipUtilities` with new compression methods
- Updated dependencies to newer versions:
  - Updated ICU4j to version `71.1`
  - Updated Apache Commons-CSV to version `1.9.0`
  - Updated Maven Compiler Plugin to version `3.10.1`
  - Updated Maven Javadoc Plugin to version `3.4.0`
  - Updated Nexus Staging Maven Plugin to version `1.6.13`

## [3.0.1] - 2022-08-23

### Changed in 3.0.1
- Fixed a bug in `CommandLineUtilities.java` that prevented primary options
  (typically options with no dependencies) from using fallback environment
  variables.

## [3.0.0] - 2022-05-04

### Changed in 3.0.0
- Modified `RecordReader` to eliminate functionality pertaining to entity types.
- Modified `pom.xml` dependency for `g2-sdk-java` for version `3.0.0` or later.

## [2.1.0] - 2022-04-08

### Changed in 2.1.0
- Added `AsyncWorkerPool.isBusy()` function and associated unit tests.
- Added `Timers.getElapsedTime()` function and associated unit tests.
- Minor change to `JsonUtilities.addProperty()` and `JsonUtilities.addElement()`
  to allow conversion of `Set` and `Array` values to be treated the same as
  `List`.  It now handles all `Collection` values the same as `List` and all 
  `Array` values as lists as well.
- Modified `JsonUtilitiesTest` to validate the changes made to
  `JsonUtilities.addProperty()` and `JsonUtilities.addElement()`.

## [2.0.2] - 2022-03-18

### Changed in 2.0.2
- Added `Timers.getElapsedTime(String)` function to obtain the time for a
  specific named timer.
- Added `AsyncWorkerPool.isBusy()` function to check if a task is currently 
  executing.

## [2.0.1] - 2022-03-07

### Changed in 2.0.1
- Updates to `pom.xml` to prevent pulling beta versions of `senzing-sdk-java`
  version `3.0.0` via the Maven dependency range.

## [2.0.0] - 2022-02-03

### Changed in 2.0.0
- Initial production release with complete unit tests
- Added error checking to `com.senzing.util.Timers`
- Added `com.senzing.reflect.PropertyReflector` class
- Added primitive/promoted type functions to `com.senzing.reflect.ReflectionUtilities`
- Renamed JsonUtils to JsonUtilities to be consistent with other utility classes
- Changed version to 2.0.0 for initial production release to indicate
  compatibility with Senzing 2.x (i.e.: `com.senzing.io.RecordReader` will parse
  records as Senzing 2.x expects them)

## [0.7.0] - 2022-01-07

### Changed in 0.7.0
- Added `com.senzing.reflect.PropertyReflector` utility class.

## [0.6.0] - 2021-11-20

### Changed in 0.6.0
- Modified `com.senzing.cmdline.CommandLineOption` to add `isSensitive()`
  function with a default implementation based on the naming of the enum  
  constant being either `PASSWORD` or ending in `_PASSWORD`.
- Changed the semantics of `parseCommandLine()` function in 
  `com.senzing.cmndline.CommandLineUtilites` so that it returns the `Map` 
  describing the command-line options and takes an optional `List` to 
  populate with `DeprecatedOptionWarning` instances if the caller is interested.
- Fixed parameters and `throws` clause on `CommandLineParser.parseCommandLine()`
  to better match `CommandLineUtilities.parseCommandLine()`
- Fixed `throws` clause for `ParameterProcessor.process()` so that it now 
  throws `BadOptionParameterException`.

## [0.5.0] - 2021-11-16

### Changed in 0.5.0 (Initial Public Prerelease)

- Initial refactoring from `Senzing/senzing-api-server` project
- Added many unit tests
- Unit tests still pending before version `1.0.0` release are as follows:
  - `com.senzing.util.JsonUtils`
  - `com.senzing.util.LoggingUtilities`
  - `com.senzing.util.OperatingSystemFamily`
  - `com.senzing.util.SemanticVersion`
  - `com.senzing.util.ThreadJoinerPool`
  - `com.senzing.util.Timers`
  - `com.senzing.util.WorkerThreadPool`
  - `com.senzing.util.ZipUtilities`
