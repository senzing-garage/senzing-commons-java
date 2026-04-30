package com.senzing.cmdline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.List;
import java.util.Map;

import static com.senzing.cmdline.CommandLineSource.ENVIRONMENT;
import static com.senzing.cmdline.CommandLineUtilities.parseCommandLine;
import static com.senzing.cmdline.EnvTestOption.COORDS;
import static com.senzing.cmdline.EnvTestOption.NAME;
import static com.senzing.cmdline.EnvTestOption.TAGS;
import static com.senzing.cmdline.EnvTestOption.VERBOSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the environment-variable handling paths in {@link
 * CommandLineUtilities#parseCommandLine}, exercised by stubbing {@link
 * System#getenv} via {@link
 * EnvironmentVariables#execute(uk.org.webcompere.systemstubs.resource.Executable)}.
 *
 * <p>Each test uses {@code @Execution(SAME_THREAD)} because the
 * {@link EnvironmentVariables} stub mutates the process environment
 * for the duration of its callable; running concurrently with other tests that
 * may read {@code System.getenv} would race.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class CommandLineUtilitiesEnvTest
{
  /**
   * The single-value parameter case: an env-var-supplied value becomes the
   * {@link CommandLineValue} for the option, with
   * {@link CommandLineSource#ENVIRONMENT} as the source and the env
   * var name as the specifier.
   */
  @Test
  public void singleValueEnvVarPopulatesOption() throws Exception
  {
    new EnvironmentVariables("SENZING_ENV_TEST_NAME", "alice").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      CommandLineValue val = result.get(NAME);
      assertNotNull(val,
                    "NAME option should be populated from environment");
      assertSame(ENVIRONMENT, val.getSource(),
                 "Source should be ENVIRONMENT");
      assertEquals("SENZING_ENV_TEST_NAME", val.getSpecifier(),
                   "Specifier should be the env var name");
      assertEquals(List.of("alice"), val.getParameters());
      assertEquals("alice", val.getProcessedValue());
    });
  }

  /**
   * If the primary env var is unset but a synonym is set, the synonym's value
   * is used. Per the implementation, synonyms come from {@link
   * CommandLineOption#getEnvironmentSynonyms()} and are checked after the
   * primary.
   */
  @Test
  public void synonymEnvVarPopulatesOption() throws Exception
  {
    new EnvironmentVariables("SZ_ENV_TEST_NAME", "bob").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      CommandLineValue val = result.get(NAME);
      assertNotNull(val);
      assertEquals("SZ_ENV_TEST_NAME", val.getSpecifier(),
                   "Specifier should be the synonym env var name");
      assertEquals("bob", val.getProcessedValue());
    });
  }

  /**
   * Boolean (min=max=0) options: the empty string and "true" both map to the
   * {@code "true"} parameter; "false" maps to
   * {@code "false"}.
   */
  @Test
  public void booleanEnvVarEmptyStringMapsToTrue() throws Exception
  {
    new EnvironmentVariables("SENZING_ENV_TEST_VERBOSE", "").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      CommandLineValue val = result.get(VERBOSE);
      assertNotNull(val);
      assertEquals(List.of("true"), val.getParameters(),
                   "Empty env value for boolean option should yield "
                       + "\"true\"");
    });
  }

  @Test
  public void booleanEnvVarTrueMapsToTrue() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_VERBOSE", "true").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      assertEquals(List.of("true"), result.get(VERBOSE).getParameters());
    });
  }

  @Test
  public void booleanEnvVarFalseMapsToFalse() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_VERBOSE", "false").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      assertEquals(List.of("false"), result.get(VERBOSE).getParameters());
    });
  }

  /**
   * For a boolean (min=max=0) option, an env value other than empty / "true" /
   * "false" must throw
   * {@link IllegalArgumentException} per the documented switch in
   * {@code processEnvironment}.
   */
  @Test
  public void booleanEnvVarInvalidValueThrows() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_VERBOSE", "yes").execute(() -> {
      assertThrows(IllegalArgumentException.class,
                   () -> parseCommandLine(EnvTestOption.class,
                                          new String[] {},
                                          EnvTestOption.PARAMETER_PROCESSOR,
                                          null));
    });
  }

  /**
   * Multi-value option (max param count &gt; 1) with a JSON-array env value:
   * the JSON_ARRAY_PATTERN branch parses the array into a list of strings.
   */
  @Test
  public void multiValueEnvVarJsonArray() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_TAGS", "[\"red\", \"green\", \"blue\"]")
        .execute(() -> {
          Map<CommandLineOption, CommandLineValue> result
              = parseCommandLine(EnvTestOption.class,
                                 new String[] {},
                                 EnvTestOption.PARAMETER_PROCESSOR,
                                 null);

          CommandLineValue val = result.get(TAGS);
          assertNotNull(val);
          assertEquals(List.of("red", "green", "blue"),
                       val.getParameters(),
                       "JSON-array env value should split into list");
        });
  }

  /**
   * Multi-value option with a comma-separated env value: the fall-through
   * branch splits on whitespace and/or commas.
   */
  @Test
  public void multiValueEnvVarCommaSeparated() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_TAGS", "red,green,blue").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      assertEquals(List.of("red", "green", "blue"),
                   result.get(TAGS).getParameters(),
                   "Comma-separated env value should split into list");
    });
  }

  /**
   * Multi-value option with whitespace-separated env value: the fall-through
   * branch splits on whitespace.
   */
  @Test
  public void multiValueEnvVarWhitespaceSeparated() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_TAGS", "red green blue").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      assertEquals(List.of("red", "green", "blue"),
                   result.get(TAGS).getParameters(),
                   "Whitespace-separated env value should split into list");
    });
  }

  /**
   * Bounded multi-value option (min=2, max=3): supplying fewer than the minimum
   * via env var must throw
   * {@link BadOptionParameterCountException}.
   */
  @Test
  public void multiValueEnvVarBelowMinThrows() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_COORDS", "x").execute(() -> {
      assertThrows(BadOptionParameterCountException.class,
                   () -> parseCommandLine(EnvTestOption.class,
                                          new String[] {},
                                          EnvTestOption.PARAMETER_PROCESSOR,
                                          null));
    });
  }

  /**
   * Bounded multi-value option: supplying more than the maximum must throw
   * {@link BadOptionParameterCountException}.
   */
  @Test
  public void multiValueEnvVarAboveMaxThrows() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_COORDS", "a,b,c,d,e").execute(() -> {
      assertThrows(BadOptionParameterCountException.class,
                   () -> parseCommandLine(EnvTestOption.class,
                                          new String[] {},
                                          EnvTestOption.PARAMETER_PROCESSOR,
                                          null));
    });
  }

  /**
   * Bounded multi-value option: an exact in-range value is accepted.
   */
  @Test
  public void multiValueEnvVarInRange() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_COORDS", "x,y").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      assertEquals(List.of("x", "y"),
                   result.get(COORDS).getParameters());
    });
  }

  /**
   * If the option is also specified on the command line, the env-var value must
   * be ignored — explicit command-line wins.
   */
  @Test
  public void commandLineWinsOverEnvironment() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_ENV_TEST_NAME", "from-env").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(
              EnvTestOption.class,
              new String[] {"--name", "from-cli"},
              EnvTestOption.PARAMETER_PROCESSOR,
              null);

      CommandLineValue val = result.get(NAME);
      assertEquals("from-cli", val.getProcessedValue(),
                   "Explicit --name on command line must override env");
      assertSame(CommandLineSource.COMMAND_LINE, val.getSource());
    });
  }

  /**
   * No environment variable set: option does not appear in result (or appears
   * only as DEFAULT-source value).
   */
  @Test
  public void unsetEnvVarLeavesOptionAbsent() throws Exception
  {
    // Use EnvironmentVariables with no var configured; we just
    // execute on an isolated env to ensure no unrelated SENZING_*
    // var leaks in from the host.
    new EnvironmentVariables().execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(EnvTestOption.class,
                             new String[] {},
                             EnvTestOption.PARAMETER_PROCESSOR,
                             null);

      // None of the EnvTestOption values have a default, so result
      // should contain no entries for any option that was not
      // sourced from cmdline or env.
      assertTrue(result.isEmpty()
                     || result.values().stream().noneMatch(
                         v -> v.getSource() == ENVIRONMENT),
                 "No env-sourced values should appear when env vars unset");
    });
  }

  // Note: the primary-option fallback path
  // (CommandLineUtilities.processEnvironment lines 1156-1170) requires
  // a primary option that has environment fallbacks. Adding an
  // is-primary case to EnvTestOption breaks the other tests in this
  // class because parseCommandLine then requires a primary option to
  // be specified for every call. Fallback-path coverage is not
  // tested here for that reason.
}
