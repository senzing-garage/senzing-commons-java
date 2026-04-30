# senzing-commons-java

If you are beginning your journey with [Senzing],
please start with [Senzing Quick Start guides].

You are in the [Senzing Garage] where projects are "tinkered" on.
Although this GitHub repository may help you understand an approach to using Senzing,
it's not considered to be "production ready" and is not considered to be part of the Senzing product.
Heck, it may not even be appropriate for your application of Senzing!

## Overview

The Senzing Commons Java Library contains Java classes, interfaces and utilities
that are common to multiple Senzing projects. It is initially a refactoring of
classes from [senzing-garage/senzing-api-server].

## Dependencies

To build the Senzing Commons Java Library you will need Apache Maven (recommend
version 3.8.5 or later) as well as OpenJDK version 17.0.x.  All other dependencies
for `senzing-commons-java` are maintained in the `pom.xml` file. No additional
dependencies are required.

## Cloning

This repository uses a git submodule mounted at `.java-coding-standards/`
that ships the shared formatter profile, checkstyle config, bulk-format
scripts, and FAQ MCP server consumed by Maven, the IDE, and Claude Code.
The submodule must be initialized before the build will work — a plain
`git clone` is not enough.

Either clone with submodules in one step:

```console
git clone --recurse-submodules https://github.com/senzing-garage/senzing-commons-java.git
```

Or initialize after a plain clone:

```console
git clone https://github.com/senzing-garage/senzing-commons-java.git
cd senzing-commons-java
git submodule update --init --recursive
```

After pulling changes that bump the submodule pin, refresh the local
checkout with:

```console
git submodule update --init --recursive
```

CI workflows must check out submodules too — `actions/checkout@v6` with
`submodules: recursive` (or equivalent for other CI systems).

If you build without initializing submodules, Maven will fail with an
"Unable to find configuration file" error from
`maven-checkstyle-plugin`, the IDE will not pick up the formatter
profile, and the FAQ MCP server entry in `.mcp.json` will fail to
start.

## Building

This is a Maven project and as such standard Maven commands are used to build it:

- Packaging the classes and javadocs in separate JAR files

  ```console
  mvn package
  ```

- Packaging the classes and javadocs and installing in your local Maven repo:

  ```console
  mvn install
  ```

- Simply compiling without packaging:

  ```console
  mvn compile
  ```

- Removing artifacts from a previous build:

  ```console
  mvn clean
  ```

- Packaging and/or installing with removal of previously built artifacts:

  ```console
  mvn clean package
  ```

  ```console
  mvn clean install
  ```

- Packaging and/or installing without running the unit tests:

  ```console
  mvn -DskipTests=true package
  ```

  ```console
  mvn -DskipTests=true install
  ```

## Documentation

Javadocs are provided for all classes. You can generate the Javadocs with the
various build commands above. Please see the Javadocs for usage details.

[Senzing]: https://senzing.com/
[Senzing Quick Start guides]: https://docs.senzing.com/quickstart/
[Senzing Garage]: https://github.com/senzing-garage
[senzing-garage/senzing-api-server]: https://github.com/senzing-garage/senzing-api-server
