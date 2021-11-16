# senzing-commons-java

## Overview

The Senzing Commons Java Library contains Java classes, interfaces and utilities
that are common to multiple Senzing projects.  It is initially a refactoring of
classes from [Senzing/senzing-api-server](https://github.com/Senzing/senzing-api-server).

## Dependencies

To build the Senzing Commons Java Library you will need Apache Maven (recommend
version 3.6.1 or later) as well as OpenJDK version 11.0.x (recommend version 
11.0.6+10 or later).   All other dependencies for `senzing-commons-java` are 
maintained in the `pom.xml` file.   No additional dependencies are required.

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

Javadocs are provided for all classes.  You can generate the Javadocs with the
various build commands above.  Please see the Javadocs for usage details.
