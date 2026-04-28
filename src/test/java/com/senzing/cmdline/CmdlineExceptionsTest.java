package com.senzing.cmdline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Arrays;
import java.util.List;

import static com.senzing.cmdline.CommandLineSource.COMMAND_LINE;
import static com.senzing.cmdline.CommandLineSource.DEFAULT;
import static com.senzing.cmdline.CommandLineSource.ENVIRONMENT;
import static com.senzing.cmdline.TestOption.CONFIG;
import static com.senzing.cmdline.TestOption.HELP;
import static com.senzing.cmdline.TestOption.PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the small exception classes in {@code com.senzing.cmdline}:
 * {@link CommandLineException}, {@link SpecifiedOptionException},
 * {@link BadOptionParametersException}, {@link UnrecognizedOptionException},
 * and {@link TooFewArgumentsException}.  {@link SpecifiedOption} default
 * methods are also exercised here since {@link SpecifiedOptionException}
 * implements {@link SpecifiedOption}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class CmdlineExceptionsTest
{
  // -------------------------------------------------------------------
  // CommandLineException
  // -------------------------------------------------------------------

  @Test
  public void commandLineExceptionDefaultConstructor()
  {
    CommandLineException ex = new CommandLineException();
    assertNull(ex.getMessage(), "Default constructor should have null message");
    assertNull(ex.getCause(), "Default constructor should have null cause");
  }

  @Test
  public void commandLineExceptionWithMessage()
  {
    CommandLineException ex = new CommandLineException("boom");
    assertEquals("boom", ex.getMessage(),
                 "Message constructor should preserve message");
    assertNull(ex.getCause(), "Message constructor should have null cause");
  }

  @Test
  public void commandLineExceptionIsCheckedException()
  {
    // CommandLineException extends Exception (checked, not RuntimeException)
    assertTrue(Exception.class.isAssignableFrom(CommandLineException.class));
  }

  // -------------------------------------------------------------------
  // SpecifiedOptionException (via concrete subclass)
  // -------------------------------------------------------------------

  @Test
  public void specifiedOptionExceptionRetainsAllFields()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", List.of("path"));

    assertSame(COMMAND_LINE, ex.getSource(),
               "getSource() should return constructor argument");
    assertSame(CONFIG, ex.getOption(),
               "getOption() should return constructor argument");
    assertEquals("--config", ex.getSpecifier(),
                 "getSpecifier() should return constructor argument");
    assertNull(ex.getMessage(),
               "Three-arg constructor should not set a message");
  }

  @Test
  public void specifiedOptionExceptionWithMessageRetainsMessage()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        ENVIRONMENT, PORT, "MY_PORT", List.of("xyz"), "bad value");

    assertSame(ENVIRONMENT, ex.getSource());
    assertSame(PORT, ex.getOption());
    assertEquals("MY_PORT", ex.getSpecifier());
    assertEquals("bad value", ex.getMessage(),
                 "Message constructor should preserve message");
  }

  @Test
  public void specifiedOptionExceptionImplementsSpecifiedOption()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", List.of());
    SpecifiedOption so = ex;

    assertSame(COMMAND_LINE, so.getSource());
    assertSame(CONFIG, so.getOption());
    assertEquals("--config", so.getSpecifier());
  }

  @Test
  public void specifiedOptionDescriptorForCommandLine()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", List.of());

    assertEquals("--config option", ex.getSourceDescriptor(),
                 "COMMAND_LINE source should produce '<flag> option'");
  }

  @Test
  public void specifiedOptionDescriptorForEnvironment()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        ENVIRONMENT, PORT, "MY_PORT", List.of());

    assertEquals("MY_PORT environment variable", ex.getSourceDescriptor(),
                 "ENVIRONMENT source should produce '<var> environment variable'");
  }

  @Test
  public void specifiedOptionDescriptorForDefault()
  {
    // For DEFAULT source, the descriptor uses the option's flag, not the
    // (typically null) specifier.
    BadOptionParametersException ex = new BadOptionParametersException(
        DEFAULT, CONFIG, null, List.of());

    assertEquals("--config default", ex.getSourceDescriptor(),
                 "DEFAULT source should produce '<flag> default'");
  }

  @Test
  public void specifiedOptionStaticDescriptorMatchesInstance()
  {
    String fromStatic = SpecifiedOption.sourceDescriptor(
        COMMAND_LINE, CONFIG, "--config");

    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", List.of());

    assertEquals(fromStatic, ex.getSourceDescriptor(),
                 "Static and instance source descriptors should match");
  }

  // -------------------------------------------------------------------
  // BadOptionParametersException
  // -------------------------------------------------------------------

  @Test
  public void badOptionParametersWithList()
  {
    List<String> params = Arrays.asList("a", "b");
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", params);

    assertEquals(params, ex.getParameters(),
                 "getParameters() should preserve list contents");
  }

  @Test
  public void badOptionParametersWithNullList()
  {
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", null);

    assertNotNull(ex.getParameters(),
                  "Null params should be converted to empty list, not null");
    assertTrue(ex.getParameters().isEmpty(),
               "Null params should be converted to empty list");
  }

  @Test
  public void badOptionParametersListIsUnmodifiable()
  {
    List<String> params = Arrays.asList("a", "b");
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", params);

    // Per javadoc: "unmodifiable List of parameters"
    assertThrows(UnsupportedOperationException.class,
                 () -> ex.getParameters().add("c"),
                 "getParameters() must return unmodifiable list");
  }

  @Test
  public void badOptionParametersListIsDefensivelyCopied()
  {
    // Mutating the source list after construction should not affect the
    // exception's parameters (List.copyOf takes a snapshot).
    List<String> params = new java.util.ArrayList<>();
    params.add("a");
    BadOptionParametersException ex = new BadOptionParametersException(
        COMMAND_LINE, CONFIG, "--config", params);
    params.add("b");

    assertEquals(1, ex.getParameters().size(),
                 "Exception's parameters should snapshot at construction");
    assertEquals("a", ex.getParameters().get(0));
  }

  @Test
  public void badOptionParametersWithMessage()
  {
    List<String> params = List.of("x");
    BadOptionParametersException ex = new BadOptionParametersException(
        ENVIRONMENT, PORT, "MY_PORT", params, "bad value");

    assertEquals("bad value", ex.getMessage());
    assertEquals(params, ex.getParameters());
  }

  // -------------------------------------------------------------------
  // UnrecognizedOptionException
  // -------------------------------------------------------------------

  @Test
  public void unrecognizedOptionRetainsOptionString()
  {
    UnrecognizedOptionException ex = new UnrecognizedOptionException("--foo");

    assertEquals("--foo", ex.getOption(),
                 "getOption() should preserve constructor argument");
    assertNull(ex.getMessage(),
               "Single-arg constructor should not set a message");
  }

  @Test
  public void unrecognizedOptionWithMessage()
  {
    UnrecognizedOptionException ex = new UnrecognizedOptionException(
        "--bar", "no such option");

    assertEquals("--bar", ex.getOption());
    assertEquals("no such option", ex.getMessage());
  }

  @Test
  public void unrecognizedOptionAcceptsNullOption()
  {
    UnrecognizedOptionException ex = new UnrecognizedOptionException(null);

    assertNull(ex.getOption(),
               "Null option should be retained, not converted");
  }

  @Test
  public void unrecognizedOptionExtendsCommandLineException()
  {
    assertTrue(CommandLineException.class.isAssignableFrom(
        UnrecognizedOptionException.class),
        "UnrecognizedOptionException must extend CommandLineException");
  }

  // -------------------------------------------------------------------
  // TooFewArgumentsException
  // -------------------------------------------------------------------

  @Test
  public void tooFewArgumentsDefaultConstructor()
  {
    TooFewArgumentsException ex = new TooFewArgumentsException();
    assertNull(ex.getMessage(),
               "Default constructor should have null message");
  }

  @Test
  public void tooFewArgumentsWithMessage()
  {
    TooFewArgumentsException ex = new TooFewArgumentsException("need more");
    assertEquals("need more", ex.getMessage());
  }

  @Test
  public void tooFewArgumentsExtendsIllegalArgumentException()
  {
    // Per the class declaration: extends IllegalArgumentException (unchecked).
    assertTrue(IllegalArgumentException.class.isAssignableFrom(
        TooFewArgumentsException.class),
        "TooFewArgumentsException must extend IllegalArgumentException");
  }

  @Test
  public void tooFewArgumentsIsUnchecked()
  {
    // Distinguishes from CommandLineException, which is checked.
    assertTrue(RuntimeException.class.isAssignableFrom(
        TooFewArgumentsException.class));
  }
}
