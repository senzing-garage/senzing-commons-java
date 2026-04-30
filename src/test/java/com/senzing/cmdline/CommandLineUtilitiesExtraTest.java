package com.senzing.cmdline;

import com.senzing.util.JsonUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.stream.SystemOut;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.Json;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.senzing.cmdline.CommandLineSource.COMMAND_LINE;
import static com.senzing.cmdline.CommandLineSource.DEFAULT;
import static com.senzing.cmdline.CommandLineSource.ENVIRONMENT;
import static com.senzing.cmdline.CommandLineUtilities.checkClassIsMain;
import static com.senzing.cmdline.CommandLineUtilities.formatUsageOptionsList;
import static com.senzing.cmdline.CommandLineUtilities.getJarBaseUrl;
import static com.senzing.cmdline.CommandLineUtilities.getJarName;
import static com.senzing.cmdline.CommandLineUtilities.getJarPath;
import static com.senzing.cmdline.CommandLineUtilities.processCommandLine;
import static com.senzing.cmdline.TestOption.CONFIG;
import static com.senzing.cmdline.TestOption.HELP;
import static com.senzing.cmdline.TestOption.IGNORE_ENV;
import static com.senzing.cmdline.TestOption.INTERFACE;
import static com.senzing.cmdline.TestOption.PASSWORD;
import static com.senzing.cmdline.TestOption.PORT;
import static com.senzing.cmdline.TestOption.URL;
import static com.senzing.cmdline.TestOption.VERBOSE;
import static com.senzing.cmdline.TestOption.VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary tests for {@link CommandLineUtilities} that target the paths
 * not covered by {@code CommandLineUtilitiesTest}: the JAR-related static
 * helpers, {@link CommandLineUtilities#formatUsageOptionsList},
 * {@link CommandLineUtilities#checkClassIsMain},
 * {@link CommandLineUtilities#main}, and the
 * {@link CommandLineUtilities#processCommandLine} sensitive-value /
 * multi-value / specifier-null branches.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class CommandLineUtilitiesExtraTest
{
  // -------------------------------------------------------------------
  // formatUsageOptionsList
  // -------------------------------------------------------------------

  @Test
  public void formatUsageOptionsListSingleOption()
  {
    String text = formatUsageOptionsList(0, HELP);
    // Per implementation: bullet "o " + flag + newline.
    assertTrue(text.contains("--help"),
               "Output should contain the option flag");
    assertTrue(text.startsWith("o "),
               "Output should begin with bullet marker");
    assertTrue(text.endsWith("\n"),
               "Output should end with a newline");
  }

  @Test
  public void formatUsageOptionsListIndentedSingleOption()
  {
    String text = formatUsageOptionsList(4, HELP);
    // Indent of 4 produces 4 spaces of indent before bullet.
    assertTrue(text.startsWith("    "),
               "Output should begin with indent spaces");
    assertTrue(text.contains("--help"));
  }

  @Test
  public void formatUsageOptionsListZeroOptions()
  {
    // No options → no output beyond possible final newline.
    String text = formatUsageOptionsList(2);
    // Implementation does not emit a final newline if columnIndex is 0
    // and no options were processed.
    assertEquals("", text,
                 "Empty options list should produce empty string");
  }

  @Test
  public void formatUsageOptionsListMultipleOptions()
  {
    String text = formatUsageOptionsList(0, HELP, VERSION, VERBOSE, CONFIG);
    assertTrue(text.contains("--help"));
    assertTrue(text.contains("--version"));
    assertTrue(text.contains("--verbose"));
    assertTrue(text.contains("--config"));
  }

  // -------------------------------------------------------------------
  // checkClassIsMain
  // -------------------------------------------------------------------

  @Test
  public void checkClassIsMainFalseWhenCalledFromTest()
  {
    // The current call stack starts in JUnit's runner, not main(),
    // so checkClassIsMain should return false for any class.
    assertFalse(checkClassIsMain(CommandLineUtilitiesExtraTest.class));
    assertFalse(checkClassIsMain(CommandLineUtilities.class));
    assertFalse(checkClassIsMain(Object.class));
  }

  // -------------------------------------------------------------------
  // getJarBaseUrl / getJarName / getJarPath / main
  // -------------------------------------------------------------------

  @Test
  public void getJarBaseUrlForClasspathClassReturnsBaseUrl()
  {
    // Classes in target/test-classes are loaded from a directory, not
    // a JAR. The base URL is computed by stripping the ".class" suffix
    // and the package path; for non-JAR classes it returns the
    // directory URL.
    String url = getJarBaseUrl(CommandLineUtilitiesExtraTest.class);
    assertNotNull(url, "Base URL should be non-null for a loaded class");
    assertTrue(url.startsWith("file:") || url.startsWith("jar:")
                   || url.startsWith("nested:"),
               "Base URL should be a URL: " + url);
  }

  @Test
  public void getJarNameForClasspathClassReturnsNull()
  {
    // The test class lives in a directory, not a JAR — no ".jar" in URL,
    // so getJarName returns null per its contract.
    String name = getJarName(CommandLineUtilitiesExtraTest.class);
    assertNull(name,
               "JAR name must be null for non-JAR-loaded class");
  }

  @Test
  public void getJarPathForClasspathClassReturnsNull()
  {
    String path = getJarPath(CommandLineUtilitiesExtraTest.class);
    assertNull(path,
               "JAR path must be null for non-JAR-loaded class");
  }

  @Test
  public void getJarNameForJarLoadedClass()
  {
    // JsonString comes from a JAR (javax.json/glassfish), so getJarName
    // should return the file name of that JAR.
    String name = getJarName(JsonString.class);
    if (name != null) {
      // Loaded from a JAR — should end with ".jar"
      assertTrue(name.endsWith(".jar"),
                 "JAR name should end with .jar: " + name);
    }
    // If null, the JAR may not be on the test classpath as a JAR — that
    // is also a legal outcome for this static helper.
  }

  @Test
  public void getJarPathForJarLoadedClass()
  {
    // For a JAR-loaded class, getJarPath should return the directory
    // path containing the JAR, or null if it cannot be determined.
    String path = getJarPath(JsonString.class);
    // No assertion on a specific value — just exercise the code path
    // without throwing.
    if (path != null) {
      assertTrue(path.startsWith("/"),
                 "JAR path should be absolute: " + path);
    }
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_OUT)
  public void mainDoesNotThrow() throws Exception
  {
    // CommandLineUtilities.main is a smoke driver that prints the URL,
    // jar name, and jar path. Capture stdout to keep the build log
    // clean; SAME_THREAD so the redirect does not interleave with
    // concurrent tests.
    new SystemOut().execute(() -> {
      CommandLineUtilities.main(new String[] {});
    });
    // Reaching here means it did not throw.
  }

  // -------------------------------------------------------------------
  // processCommandLine — sensitive values, no specifier, multi-value
  // -------------------------------------------------------------------

  /**
   * Builds a {@link CommandLineValue} via reflection because the constructor is
   * package-private but we are in the same package.
   */
  private static CommandLineValue makeValue(CommandLineSource source,
                                            CommandLineOption option,
                                            String specifier,
                                            Object processedValue,
                                            List<String> params)
  {
    return new CommandLineValue(source, option, specifier,
                                processedValue, params);
  }

  @Test
  public void processCommandLineRedactsSensitiveValues()
  {
    // PASSWORD is sensitive — should serialize to REDACTED in JSON.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(PASSWORD,
              makeValue(COMMAND_LINE, PASSWORD, "--password",
                        "secret", List.of("secret")));

    Map<CommandLineOption, Object> result = new LinkedHashMap<>();
    JsonObjectBuilder job = Json.createObjectBuilder();
    StringBuilder sb = new StringBuilder();
    processCommandLine(input, result, job, sb);

    assertEquals("secret", result.get(PASSWORD));

    // The JSON should NOT contain the literal "secret" — it must be
    // redacted.
    String json = sb.toString();
    assertFalse(json.contains("\"secret\""),
                "Sensitive value should be redacted from JSON");
  }

  @Test
  public void processCommandLineMultiValueParameters()
  {
    // CONFIG has 1-1 params, but we'll synthesize a multi-value list
    // to exercise the "values" array branch (size > 1).
    // Use VERBOSE for a multi-value test by hand-crafting a CommandLineValue.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(VERBOSE,
              makeValue(COMMAND_LINE, VERBOSE, "--verbose",
                        Boolean.TRUE,
                        List.of("a", "b", "c")));

    JsonObjectBuilder job = Json.createObjectBuilder();
    StringBuilder sb = new StringBuilder();
    processCommandLine(input, new LinkedHashMap<>(), job, sb);

    JsonObject result = job.build();
    JsonObject verbose = result.getJsonObject(VERBOSE.toString());
    assertNotNull(verbose.getJsonArray("values"),
                  "Multi-value params should serialize as 'values' array");
    assertEquals(3, verbose.getJsonArray("values").size());
  }

  @Test
  public void processCommandLineSingleValueParameter()
  {
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(PORT,
              makeValue(COMMAND_LINE, PORT, "--port",
                        Integer.valueOf(8080),
                        List.of("8080")));

    JsonObjectBuilder job = Json.createObjectBuilder();
    processCommandLine(input, new LinkedHashMap<>(), job, null);

    JsonObject result = job.build();
    assertEquals("8080",
                 result.getJsonObject(PORT.toString()).getString("value"));
  }

  @Test
  public void processCommandLineDefaultSourceOmitsViaSpecifier()
  {
    // DEFAULT source values typically have a null specifier — the
    // "via" field should not be emitted in JSON.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(VERBOSE,
              makeValue(DEFAULT, VERBOSE, null, Boolean.FALSE,
                        List.of("false")));

    JsonObjectBuilder job = Json.createObjectBuilder();
    processCommandLine(input, new LinkedHashMap<>(), job, null);

    JsonObject result = job.build();
    JsonObject verbose = result.getJsonObject(VERBOSE.toString());
    assertEquals("DEFAULT", verbose.getString("source"));
    assertFalse(verbose.containsKey("via"),
                "DEFAULT source with null specifier should omit 'via'");
  }

  @Test
  public void processCommandLineEnvironmentSourceIncludesViaSpecifier()
  {
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(PORT,
              makeValue(ENVIRONMENT, PORT, "SENZING_TEST_PORT",
                        Integer.valueOf(8080), List.of("8080")));

    JsonObjectBuilder job = Json.createObjectBuilder();
    processCommandLine(input, new LinkedHashMap<>(), job, null);

    JsonObject result = job.build();
    JsonObject port = result.getJsonObject(PORT.toString());
    assertEquals("ENVIRONMENT", port.getString("source"));
    assertEquals("SENZING_TEST_PORT", port.getString("via"));
  }

  @Test
  public void processCommandLineKeyMismatchThrows()
  {
    // The key in the map and the option in the value must match.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    // Put under PORT key but with HELP option in the value.
    input.put(PORT,
              makeValue(COMMAND_LINE, HELP, "--help",
                        Boolean.TRUE, List.of()));

    assertThrows(IllegalArgumentException.class,
                 () -> processCommandLine(input,
                                          new LinkedHashMap<>(), null, null));
  }

  @Test
  public void processCommandLineWithProvidedJsonBuilder()
  {
    // When jsonBuilder is supplied (and stringBuilder is null), it
    // populates that builder directly without an intermediate.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(PORT,
              makeValue(COMMAND_LINE, PORT, "--port",
                        Integer.valueOf(80), List.of("80")));

    JsonObjectBuilder job = Json.createObjectBuilder();
    Map<CommandLineOption, Object> resultMap = new LinkedHashMap<>();
    Map<CommandLineOption, Object> returned
        = processCommandLine(input, resultMap, job, null);

    assertSame(resultMap, returned,
               "Should return the supplied result map when non-null");
    assertEquals(80, ((Integer) returned.get(PORT)).intValue());

    JsonObject built = job.build();
    assertNotNull(built.getJsonObject(PORT.toString()));
  }

  @Test
  public void processCommandLineCreatesResultMapIfNull()
  {
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(VERBOSE,
              makeValue(COMMAND_LINE, VERBOSE, "--verbose",
                        Boolean.TRUE, List.of()));

    Map<CommandLineOption, Object> result
        = processCommandLine(input, null, (StringBuilder) null);

    assertNotNull(result, "Null result map arg should yield a fresh map");
    assertEquals(Boolean.TRUE, result.get(VERBOSE));
  }

  @Test
  public void processCommandLineNoJsonBuilderOrStringBuilderDoesNotBuildJson()
  {
    // When neither jsonBuilder nor stringBuilder is supplied, the JSON
    // path is skipped entirely.
    Map<CommandLineOption, CommandLineValue> input = new LinkedHashMap<>();
    input.put(VERBOSE,
              makeValue(COMMAND_LINE, VERBOSE, "--verbose",
                        Boolean.TRUE, List.of()));

    Map<CommandLineOption, Object> result = new LinkedHashMap<>();
    Map<CommandLineOption, Object> returned
        = processCommandLine(input, result, null, null);

    assertSame(result, returned);
    assertEquals(Boolean.TRUE, returned.get(VERBOSE));
  }

  // -------------------------------------------------------------------
  // shiftArguments edge cases (already partially covered)
  // -------------------------------------------------------------------

  @Test
  public void shiftArgumentsZeroFromEmptyArrayReturnsEmpty()
  {
    String[] args = new String[] {};
    String[] shifted = CommandLineUtilities.shiftArguments(args, 0);
    assertEquals(0, shifted.length);
  }

  // -------------------------------------------------------------------
  // extractJarLocation — package-private helper extracted from the
  // class's static initializer so its URL-parsing logic can be tested
  // without needing a class loaded from a real JAR.
  // -------------------------------------------------------------------

  @Test
  public void extractJarLocationReturnsEmptyForNullUrl()
  {
    CommandLineUtilities.JarLocation loc
        = CommandLineUtilities.extractJarLocation(
            null, "com.example.Foo");

    assertSame(CommandLineUtilities.JarLocation.EMPTY, loc,
               "Null URL should yield the EMPTY sentinel");
    assertNull(loc.baseUrl());
    assertNull(loc.fileName());
    assertNull(loc.pathToJar());
  }

  @Test
  public void extractJarLocationReturnsEmptyForNonJarUrl()
  {
    // file:/path/to/classes/com/example/Foo.class — no ".jar" in URL
    CommandLineUtilities.JarLocation loc
        = CommandLineUtilities.extractJarLocation(
            "file:/path/to/classes/com/example/Foo.class",
            "com.example.Foo");

    assertSame(CommandLineUtilities.JarLocation.EMPTY, loc);
  }

  @Test
  public void extractJarLocationParsesJarUrl()
  {
    // Classic JAR URL: jar:file:/some/path/foo.jar!/com/example/Foo.class
    String url
        = "jar:file:/some/path/foo.jar!/com/example/Foo.class";
    CommandLineUtilities.JarLocation loc
        = CommandLineUtilities.extractJarLocation(
            url, "com.example.Foo");

    // Base URL is everything up to (but not including) the package
    // path inside the JAR.
    assertEquals("jar:file:/some/path/foo.jar!/", loc.baseUrl(),
                 "baseUrl should end at the JAR's '!/' boundary");
    assertEquals("foo.jar", loc.fileName(),
                 "fileName should be the bare JAR file name");
    assertEquals("/some/path", loc.pathToJar(),
                 "pathToJar should be the directory containing the JAR");
  }

  @Test
  public void extractJarLocationParsesNestedPackageJarUrl()
  {
    String url
        = "jar:file:/lib/myapp.jar!/com/senzing/cmdline/CommandLineUtilities.class";
    CommandLineUtilities.JarLocation loc
        = CommandLineUtilities.extractJarLocation(
            url, "com.senzing.cmdline.CommandLineUtilities");

    assertEquals("myapp.jar", loc.fileName());
    assertEquals("/lib", loc.pathToJar());
    assertTrue(loc.baseUrl().endsWith("!/"));
  }

  @Test
  public void extractJarLocationWithoutBangSentinelLeavesFileAndPathNull()
  {
    // A URL that contains ".jar" but lacks the "!" boundary marker —
    // the inner block that sets fileName / pathToJar is skipped.
    String url = "file:/some/odd/foo.jar/com/example/Foo.class";
    CommandLineUtilities.JarLocation loc
        = CommandLineUtilities.extractJarLocation(
            url, "com.example.Foo");

    assertNotNull(loc.baseUrl(),
                  "baseUrl should still be derived from class name path");
    assertNull(loc.fileName(),
               "fileName should be null when URL has no '!' marker");
    assertNull(loc.pathToJar(),
               "pathToJar should be null when URL has no '!' marker");
  }

  @Test
  public void staticInitJarLocationFieldsArePopulatedConsistently()
  {
    // The class-level JAR_BASE_URL/JAR_FILE_NAME/PATH_TO_JAR fields
    // are populated by the static initializer using
    // extractJarLocation. Test classpath loads CommandLineUtilities
    // from a directory (target/classes), so all three should be null.
    // When run from a JAR (e.g. consumer projects), all three would
    // be non-null. Either is a valid outcome — we just verify the
    // three fields are mutually consistent.
    boolean fromJar = CommandLineUtilities.JAR_FILE_NAME != null;
    if (fromJar) {
      assertNotNull(CommandLineUtilities.JAR_BASE_URL,
                    "If FILE_NAME is set, BASE_URL must be set");
      assertNotNull(CommandLineUtilities.PATH_TO_JAR,
                    "If FILE_NAME is set, PATH_TO_JAR must be set");
    } else {
      assertNull(CommandLineUtilities.JAR_BASE_URL);
      assertNull(CommandLineUtilities.PATH_TO_JAR);
    }
  }
}
