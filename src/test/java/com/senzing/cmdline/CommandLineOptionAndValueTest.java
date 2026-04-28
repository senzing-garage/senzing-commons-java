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

  /**
   * Enum with a public non-static instance field so that
   * {@link CommandLineOption#isSensitive()}'s reflective field
   * iteration sees a non-public-static field and exercises the
   * "skip if not public-static" continue branch.
   */
  enum NonStaticFieldOption
      implements CommandLineOption<NonStaticFieldOption, NonStaticFieldOption>
  {
    ONLY;

    /**
     * A public non-static field on the enum constant. {@code
     * Class.getFields()} returns this field, but
     * {@code isSensitive()} must skip it via the
     * "modifiers & PUBLIC_STATIC == 0" guard.
     */
    public String publicInstanceField = "ignored";

    @Override
    public String getCommandLineFlag()
    {
      return "--only";
    }
  }

  /**
   * Non-enum implementation of {@link CommandLineOption} used to
   * exercise the {@code return false} fall-through in
   * {@link CommandLineOption#isSensitive()} — for a non-enum,
   * {@code Class.getFields()} contains no static field whose value
   * is the instance, so the loop completes without returning.
   *
   * <p>Uses raw types because {@code CommandLineOption}'s generic
   * type parameter requires {@code T extends Enum<T>}, which a
   * regular class cannot satisfy.</p>
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  static class NonEnumOption implements CommandLineOption
  {
    @Override
    public String getCommandLineFlag()
    {
      return "--non-enum";
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

  /**
   * When the implementing enum has a non-static public instance
   * field, {@link CommandLineOption#isSensitive()}'s reflective
   * loop must {@code continue} past it (the "modifiers &
   * PUBLIC_STATIC == 0" branch) without trying to read it. The
   * enum constant's own field still drives the result.
   */
  @Test
  public void isSensitiveSkipsNonStaticFields()
  {
    // Should return false without throwing — the public instance
    // field is correctly skipped, and the enum constant's name
    // ("ONLY") is not "PASSWORD" so the eventual result is false.
    assertFalse(NonStaticFieldOption.ONLY.isSensitive(),
                "Option whose enum has a non-static public field"
                    + " must skip that field and not throw");
  }

  /**
   * For a non-enum implementation of {@link CommandLineOption},
   * {@link CommandLineOption#isSensitive()} must reach its
   * {@code return false} fall-through: no static field on the
   * implementing class equals the instance, so the loop completes
   * without an inner return.
   */
  @Test
  public void isSensitiveFalseForNonEnumImplementation()
  {
    assertFalse(new NonEnumOption().isSensitive(),
                "Non-enum option must fall through to 'return false'"
                    + " — no public-static field equals the instance");
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
