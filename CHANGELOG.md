# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
[markdownlint](https://dlaa.me/markdownlint/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - 2022-02-03

### Added to 2.0.0

- Initial production release with complete unit tests
- Renamed JsonUtils to JsonUtilities to be consistent with other utility classes
- Added error checking to `com.senzing.util.Timers`
- Added `com.senzing.reflect.PropertyReflector` class
- Added primitive/promoted type functions to `com.senzing.reflect.ReflectionUtilities`
- Changed version to 2.0.0 for initial production release to indicate 
  compatibility with Senzing 2.x (i.e.: `com.senzing.io.RecordReader` will parse
  records as Senzing 2.x expects them)

## [0.7.0] - 2022-01-07

### Added to 0.7.0

- Added `com.senzing.reflect.PropertyReflector` utility class.

## [0.6.0] - 2021-11-20

### Added to 0.6.0

- Modified `com.senzing.cmdline.CommandLineOption` to add `isSensitive()`
  function with a default implementation based on the naming of the enum  
  constant being either `PASSWORD` or ending in `_PASSWORD`.
- Chnaged the semantics of `parseCommandLine()` function in 
  `com.senzing.cmndline.CommandLineUtilites` so that it returns the `Map` 
  describing the command-line options and takes an optional `List` to 
  populate with `DeprecatedOptionWarning` instances if the caller is interested.
- Fixed parameters and `throws` clause on `CommandLineParser.parseCommandLine()`
  to better match `CommandLineUtilities.parseCommandLine()`
- Fixed `throws` clause for `ParameterProcessor.process()` so that it now 
  throws `BadOptionParameterException`.

## [0.5.0] - 2021-11-16

### Added to 0.5.0 (Initial Public Prerelease)

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
