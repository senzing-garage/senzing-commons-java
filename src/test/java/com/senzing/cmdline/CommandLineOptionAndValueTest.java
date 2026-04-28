package com.senzing.cmdline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the default-method behavior on {@link CommandLineOption}
 * and the accessor / mutator / equals / hashCode / toString methods
 * on {@link CommandLineValue}, neither of which is exercised
 * directly by the parser-driven tests in
 * {@code CommandLineUtilitiesTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class CommandLineOptionAndValueTest
{
  /**
   * Minimal enum that overrides only {@link
   * CommandLineOption#getCommandLineFlag()}, leaving every other
   * method to its default implementation. Used to verify the
   * default-method behavior.
   */
  enum DefaultsOption
      implements CommandLineOption<DefaultsOption, DefaultsOption>
  {
    PLAIN("--plain"),
    PASSWORD("--password");

    private final String flag;

    DefaultsOption(String flag)
    {
      this.flag = flag;
    }

    @Override
    public String getCommandLineFlag()
    {
      return this.flag;
    }
  }

  // -------------------------------------------------------------------
  // CommandLineOption defaults
  // -------------------------------------------------------------------

  @Test
  public void getSynonymFlagsDefaultsToEmpty()
  {
    assertEquals(Collections.emptySet(),
                 DefaultsOption.PLAIN.getSynonymFlags());
  }

  @Test
  public void getEnvironmentVariableDefaultsToNull()
  {
    assertNull(DefaultsOption.PLAIN.getEnvironmentVariable());
  }

  @Test
  public void getEnvironmentSynonymsDefaultsToEmpty()
  {
    assertEquals(Collections.emptySet(),
                 DefaultsOption.PLAIN.getEnvironmentSynonyms());
  }

  @Test
  public void getEnvironmentFallbacksDefaultsToEmpty()
  {
    assertEquals(Collections.emptyList(),
                 DefaultsOption.PLAIN.getEnvironmentFallbacks());
  }

  @Test
  public void getConflictsDefaultsToEmpty()
  {
    assertEquals(Collections.emptySet(),
                 DefaultsOption.PLAIN.getConflicts());
  }

  @Test
  public void getDependenciesDefaultsToEmpty()
  {
    assertEquals(Collections.emptySet(),
                 DefaultsOption.PLAIN.getDependencies());
  }

  @Test
  public void getDeprecationAlternativesDefaultsToEmpty()
  {
    assertEquals(Collections.emptySet(),
                 DefaultsOption.PLAIN.getDeprecationAlternatives());
  }

  @Test
  public void isPrimaryDefaultsToFalse()
  {
    assertFalse(DefaultsOption.PLAIN.isPrimary());
  }

  @Test
  public void isDeprecatedDefaultsToFalse()
  {
    assertFalse(DefaultsOption.PLAIN.isDeprecated());
  }

  @Test
  public void getMinimumParameterCountDefaultsToZero()
  {
    assertEquals(0, DefaultsOption.PLAIN.getMinimumParameterCount());
  }

  @Test
  public void getMaximumParameterCountDefaultsToMinusOne()
  {
    // Per javadoc: "By default this returns negative one (-1)."
    assertEquals(-1, DefaultsOption.PLAIN.getMaximumParameterCount());
  }

  @Test
  public void getDefaultParametersDefaultsToNull()
  {
    assertNull(DefaultsOption.PLAIN.getDefaultParameters());
  }

  @Test
  public void isSensitiveTrueForOptionNamedPassword()
  {
    // The default isSensitive() reflects on the option's enum
    // declaration and returns true if the field name is literally
    // "PASSWORD" or ends with "_PASSWORD".
    assertTrue(DefaultsOption.PASSWORD.isSensitive(),
               "Option whose enum name is PASSWORD should be sensitive");
  }

  @Test
  public void isSensitiveFalseForOrdinaryOption()
  {
    assertFalse(DefaultsOption.PLAIN.isSensitive(),
                "Ordinary option should not be sensitive");
  }

  // -------------------------------------------------------------------
  // CommandLineValue setters / equals / hashCode / toString
  // -------------------------------------------------------------------

  @Test
  public void setSourceUpdatesSource()
  {
    CommandLineValue val = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));

    val.setSource(CommandLineSource.ENVIRONMENT);
    assertSame(CommandLineSource.ENVIRONMENT, val.getSource());
  }

  @Test
  public void setSpecifierUpdatesSpecifier()
  {
    CommandLineValue val = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));

    val.setSpecifier("MY_VAR");
    assertEquals("MY_VAR", val.getSpecifier());

    val.setSpecifier(null);
    assertNull(val.getSpecifier());
  }

  @Test
  public void setProcessedValueUpdatesProcessedValue()
  {
    CommandLineValue val = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));

    val.setProcessedValue(42);
    assertEquals(42, val.getProcessedValue());
  }

  @Test
  public void setParametersUpdatesParameters()
  {
    CommandLineValue val = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));

    val.setParameters(List.of("a", "b"));
    assertEquals(List.of("a", "b"), val.getParameters());
  }

  @Test
  public void toStringContainsAllComponents()
  {
    CommandLineValue val = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "v",
        List.of("v"));

    String s = val.toString();
    assertTrue(s.contains("PLAIN"), "toString should mention option");
    assertTrue(s.contains("COMMAND_LINE"),
               "toString should mention source");
    assertTrue(s.contains("--plain"),
               "toString should mention specifier");
    assertTrue(s.contains("v"), "toString should mention value");
  }

  @Test
  public void equalsAndHashCodeReflectAllFields()
  {
    CommandLineValue v1 = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));
    CommandLineValue v2 = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN,
        "--plain",
        "x",
        List.of("x"));

    assertEquals(v1, v2,
                 "Two values with identical components must be equal");
    assertEquals(v1.hashCode(), v2.hashCode(),
                 "Equal values must have equal hashCodes");
  }

  @Test
  public void equalsReflectsDifferingSource()
  {
    CommandLineValue v1 = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN, "--plain", "x", List.of("x"));
    CommandLineValue v2 = new CommandLineValue(
        CommandLineSource.ENVIRONMENT,
        DefaultsOption.PLAIN, "--plain", "x", List.of("x"));

    assertNotEquals(v1, v2,
                    "Values with differing source must not be equal");
  }

  @Test
  public void equalsReturnsTrueForSelf()
  {
    CommandLineValue v = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN, "--plain", "x", List.of("x"));
    assertEquals(v, v);
  }

  @Test
  public void equalsReturnsFalseForNull()
  {
    CommandLineValue v = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN, "--plain", "x", List.of("x"));
    assertNotEquals(null, v);
  }

  @Test
  public void equalsReturnsFalseForDifferentClass()
  {
    CommandLineValue v = new CommandLineValue(
        CommandLineSource.COMMAND_LINE,
        DefaultsOption.PLAIN, "--plain", "x", List.of("x"));
    assertNotEquals("string", v);
  }
}
