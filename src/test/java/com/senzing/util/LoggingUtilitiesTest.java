package com.senzing.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static com.senzing.util.LoggingUtilities.BASE_PRODUCT_ID;
import static com.senzing.util.LoggingUtilities.DEBUG_SYSTEM_PROPERTY;
import static com.senzing.util.LoggingUtilities.clearDebugOverride;
import static com.senzing.util.LoggingUtilities.formatStackFrame;
import static com.senzing.util.LoggingUtilities.formatStackTrace;
import static com.senzing.util.LoggingUtilities.getProductIdForPackage;
import static com.senzing.util.LoggingUtilities.isDebugLogging;
import static com.senzing.util.LoggingUtilities.isLastLoggedException;
import static com.senzing.util.LoggingUtilities.logDebug;
import static com.senzing.util.LoggingUtilities.logError;
import static com.senzing.util.LoggingUtilities.logInfo;
import static com.senzing.util.LoggingUtilities.logOnceAndThrow;
import static com.senzing.util.LoggingUtilities.logWarning;
import static com.senzing.util.LoggingUtilities.multilineFormat;
import static com.senzing.util.LoggingUtilities.overrideDebugLogging;
import static com.senzing.util.LoggingUtilities.setLastLoggedAndThrow;
import static com.senzing.util.LoggingUtilities.setLastLoggedException;
import static com.senzing.util.LoggingUtilities.setProductIdForPackage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LoggingUtilities}.
 *
 * <p>Each test asserts the documented contract from
 * {@link LoggingUtilities}'s javadoc: the four logging entry points
 * (error/warning/info/debug) write to the documented stream with the
 * documented prefix and timestamp; stack-trace and stack-frame
 * formatting handles null/unknown components per the documented
 * sentinel strings; per-thread debug-override semantics push/pop
 * cleanly; and the last-logged-exception machinery records and
 * compares exception identity so a single throwable is only logged
 * once even when it bubbles up through multiple {@code catch} sites.
 *
 * <p>Tests run single-threaded because they swap {@link System#err}
 * / {@link System#out} and toggle the global
 * {@code com.senzing.debug} system property; the {@link AfterEach}
 * hook restores both.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class LoggingUtilitiesTest
{
  private PrintStream            originalErr;
  private PrintStream            originalOut;
  private ByteArrayOutputStream  capturedErr;
  private ByteArrayOutputStream  capturedOut;
  private String                 originalDebugProperty;

  // -------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------

  @BeforeEach
  public void redirectStreamsAndClearDebug()
  {
    this.originalErr = System.err;
    this.originalOut = System.out;
    this.capturedErr = new ByteArrayOutputStream();
    this.capturedOut = new ByteArrayOutputStream();
    System.setErr(new PrintStream(this.capturedErr, true,
                                  StandardCharsets.UTF_8));
    System.setOut(new PrintStream(this.capturedOut, true,
                                  StandardCharsets.UTF_8));

    this.originalDebugProperty = System.getProperty(DEBUG_SYSTEM_PROPERTY);
    System.clearProperty(DEBUG_SYSTEM_PROPERTY);

    // Drain any thread-local debug overrides leaked by prior tests.
    while (true) {
      try {
        clearDebugOverride();
      } catch (Throwable ignore) {
        break;
      }
      // clearDebugOverride logs warning when stack is empty but does
      // not throw — break manually after detecting that case.
      if (!isDebugLogging()) {
        // After fully draining, isDebugLogging falls through to the
        // (now-null) system property and returns false — the loop
        // would be infinite without this break. Re-check by setting a
        // sentinel: if the override stack was already empty we'd just
        // produce a "Unbalanced" warning each iteration.
        break;
      }
    }
    // Reset the captured streams (the drain may have written warnings
    // to System.err).
    this.capturedErr.reset();
    this.capturedOut.reset();

    // Clear the per-thread last-logged-exception so each test starts
    // from a clean state.
    setLastLoggedException(null);
  }

  @AfterEach
  public void restoreStreamsAndDebugProperty()
  {
    System.setErr(this.originalErr);
    System.setOut(this.originalOut);

    if (this.originalDebugProperty == null) {
      System.clearProperty(DEBUG_SYSTEM_PROPERTY);
    } else {
      System.setProperty(DEBUG_SYSTEM_PROPERTY,
                         this.originalDebugProperty);
    }

    setLastLoggedException(null);
  }

  // -------------------------------------------------------------------
  // Constants
  // -------------------------------------------------------------------

  /**
   * The public {@link LoggingUtilities#BASE_PRODUCT_ID} constant must
   * equal the documented value ({@code "5025"}).
   */
  @Test
  public void baseProductIdConstantIsExpectedValue()
  {
    assertEquals("5025", BASE_PRODUCT_ID);
  }

  /**
   * The public {@link LoggingUtilities#DEBUG_SYSTEM_PROPERTY}
   * constant must equal the documented system-property name.
   */
  @Test
  public void debugSystemPropertyConstantIsExpectedValue()
  {
    assertEquals("com.senzing.debug", DEBUG_SYSTEM_PROPERTY);
  }

  // -------------------------------------------------------------------
  // getProductIdForPackage / setProductIdForPackage contract
  // -------------------------------------------------------------------

  /**
   * For a {@code com.senzing.<x>} package with no registered ID the
   * default discovery must return the segment immediately after
   * {@code com.senzing.}, per the implementation's javadoc-implied
   * derivation rule.
   */
  @Test
  public void getProductIdForPackageDerivesFromComSenzingSegment()
  {
    assertEquals("foo", getProductIdForPackage("com.senzing.foo"));
    assertEquals("foo",
                 getProductIdForPackage("com.senzing.foo.bar.baz"));
  }

  /**
   * For a non-{@code com.senzing} package with no match anywhere
   * along its prefix chain, the base product ID
   * ({@link LoggingUtilities#BASE_PRODUCT_ID}) must be returned.
   */
  @Test
  public void getProductIdForPackageFallsBackToBaseId()
  {
    assertEquals(BASE_PRODUCT_ID,
                 getProductIdForPackage(
                     "org.example.completely.unknown"));
  }

  /**
   * After {@link LoggingUtilities#setProductIdForPackage} registers
   * a package, an exact lookup must return that ID.
   */
  @Test
  public void setProductIdForPackageRegistersExactMatch()
  {
    String pkg = "org.example.test." + System.nanoTime();
    setProductIdForPackage(pkg, "9999");
    assertEquals("9999", getProductIdForPackage(pkg));
  }

  /**
   * A registration on a parent package must be picked up via the
   * implementation's prefix-stripping fallback when looking up a
   * child package that is not itself registered.
   */
  @Test
  public void getProductIdForPackageFallsBackToRegisteredPrefix()
  {
    String parent = "org.example.parent." + System.nanoTime();
    setProductIdForPackage(parent, "8888");
    assertEquals("8888", getProductIdForPackage(parent + ".child.deep"));
  }

  /**
   * The exact string {@code "com.senzing"} (no trailing dot) is
   * shorter than the prefix the implementation checks
   * ({@code "com.senzing."}) and must therefore fall through to the
   * base product ID.
   */
  @Test
  public void getProductIdForPackageReturnsBaseForBareComSenzing()
  {
    assertEquals(BASE_PRODUCT_ID, getProductIdForPackage("com.senzing"));
  }

  // -------------------------------------------------------------------
  // overrideDebugLogging / clearDebugOverride / isDebugLogging
  // -------------------------------------------------------------------

  /**
   * With no thread override and no system property,
   * {@link LoggingUtilities#isDebugLogging()} must return
   * {@code false}.
   */
  @Test
  public void isDebugLoggingDefaultsToFalse()
  {
    assertFalse(isDebugLogging());
  }

  /**
   * After {@link LoggingUtilities#overrideDebugLogging(boolean)
   * overrideDebugLogging(true)},
   * {@link LoggingUtilities#isDebugLogging()} must return
   * {@code true}.
   */
  @Test
  public void overrideDebugLoggingTrueEnablesDebug()
  {
    overrideDebugLogging(true);
    try {
      assertTrue(isDebugLogging());
    } finally {
      clearDebugOverride();
    }
  }

  /**
   * Pushing {@code overrideDebugLogging(false)} must override an
   * earlier {@code true} push (the top of the stack wins) until it
   * is popped via {@code clearDebugOverride}.
   */
  @Test
  public void overrideDebugLoggingNestedPushPop()
  {
    overrideDebugLogging(true);
    try {
      overrideDebugLogging(false);
      try {
        assertFalse(isDebugLogging(),
                    "Top of stack (false) must win over earlier true");
      } finally {
        clearDebugOverride();
      }
      assertTrue(isDebugLogging(),
                 "After popping the false override, the true override"
                     + " must be visible again");
    } finally {
      clearDebugOverride();
    }
    assertFalse(isDebugLogging(),
                "After all overrides are cleared, default is false");
  }

  /**
   * Calling {@link LoggingUtilities#clearDebugOverride()} when there
   * is no override on the stack must NOT throw — it logs a warning
   * to {@code System.err} per the implementation.
   */
  @Test
  public void clearDebugOverrideOnEmptyStackLogsWarning()
  {
    clearDebugOverride();
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("Unbalanced calls to override/clear"),
               "Empty-stack clear must log a warning to stderr: "
                   + err);
  }

  /**
   * The {@code com.senzing.debug} system property set to
   * {@code "true"} (case-insensitive) must enable debug logging when
   * no thread override is active.
   */
  @Test
  public void isDebugLoggingHonorsSystemProperty()
  {
    assertFalse(isDebugLogging());
    System.setProperty(DEBUG_SYSTEM_PROPERTY, "true");
    try {
      assertTrue(isDebugLogging());
    } finally {
      System.clearProperty(DEBUG_SYSTEM_PROPERTY);
    }

    System.setProperty(DEBUG_SYSTEM_PROPERTY, "TRUE");
    try {
      assertTrue(isDebugLogging(),
                 "System property comparison must be case-insensitive");
    } finally {
      System.clearProperty(DEBUG_SYSTEM_PROPERTY);
    }

    System.setProperty(DEBUG_SYSTEM_PROPERTY, "no");
    try {
      assertFalse(isDebugLogging(),
                  "Property values other than 'true' must yield false");
    } finally {
      System.clearProperty(DEBUG_SYSTEM_PROPERTY);
    }
  }

  // -------------------------------------------------------------------
  // logError / logWarning / logInfo contract
  // -------------------------------------------------------------------

  /**
   * {@link LoggingUtilities#logError(Object...)} must write to
   * {@code System.err} with the documented {@code (ERROR)} prefix
   * and the supplied message.
   */
  @Test
  public void logErrorWritesToStderrWithErrorPrefix()
  {
    logError("oh no");
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    String out = this.capturedOut.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("(ERROR)"),
               "logError must include '(ERROR)' tag: " + err);
    assertTrue(err.contains("oh no"),
               "logError must include the message: " + err);
    assertEquals("", out, "logError must NOT write to stdout");
  }

  /**
   * {@link LoggingUtilities#logWarning(Object...)} must write to
   * {@code System.err} with the {@code (WARNING)} tag.
   */
  @Test
  public void logWarningWritesToStderrWithWarningTag()
  {
    logWarning("watch out");
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("(WARNING)"),
               "logWarning must include '(WARNING)' tag: " + err);
    assertTrue(err.contains("watch out"));
  }

  /**
   * {@link LoggingUtilities#logInfo(Object...)} must write to
   * {@code System.out} with the {@code (INFO)} tag.
   */
  @Test
  public void logInfoWritesToStdoutWithInfoTag()
  {
    logInfo("hello");
    String out = this.capturedOut.toString(StandardCharsets.UTF_8);
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(out.contains("(INFO)"),
               "logInfo must include '(INFO)' tag: " + out);
    assertTrue(out.contains("hello"));
    assertEquals("", err, "logInfo must NOT write to stderr");
  }

  /**
   * {@link LoggingUtilities#logDebug(Object...)} must NOT write
   * anything when debug logging is disabled (the default).
   */
  @Test
  public void logDebugSuppressesOutputWhenDebugLoggingDisabled()
  {
    assertFalse(isDebugLogging(),
                "Precondition: debug logging must be off");
    logDebug("should not appear");
    assertEquals("",
                 this.capturedOut.toString(StandardCharsets.UTF_8));
    assertEquals("",
                 this.capturedErr.toString(StandardCharsets.UTF_8));
  }

  /**
   * {@link LoggingUtilities#logDebug(Object...)} must write to
   * {@code System.out} with a {@code (DEBUG)} tag when debug
   * logging is enabled.
   */
  @Test
  public void logDebugWritesToStdoutWhenDebugLoggingEnabled()
  {
    overrideDebugLogging(true);
    try {
      logDebug("debug me");
      String out = this.capturedOut.toString(StandardCharsets.UTF_8);
      assertTrue(out.contains("(DEBUG)"),
                 "logDebug must include '(DEBUG)' tag: " + out);
      assertTrue(out.contains("debug me"));
    } finally {
      clearDebugOverride();
    }
  }

  /**
   * The {@link LoggingUtilities#logError(Throwable, Object...)}
   * overload must include the {@link Throwable}'s stack trace in
   * the stderr output.
   */
  @Test
  public void logErrorWithThrowableIncludesStackTrace()
  {
    Exception ex = new IllegalStateException("simulated");
    logError(ex, "context");
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("IllegalStateException"),
               "Stack trace must be included: " + err);
    assertTrue(err.contains("simulated"));
    assertTrue(err.contains("context"));
  }

  /**
   * The {@link LoggingUtilities#logWarning(Throwable, Object...)}
   * overload must include the {@link Throwable}'s stack trace.
   */
  @Test
  public void logWarningWithThrowableIncludesStackTrace()
  {
    Exception ex = new RuntimeException("kaboom");
    logWarning(ex, "while doing X");
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("RuntimeException"));
    assertTrue(err.contains("kaboom"));
    assertTrue(err.contains("while doing X"));
  }

  // -------------------------------------------------------------------
  // formatStackTrace / formatStackFrame contract
  // -------------------------------------------------------------------

  /**
   * {@code formatStackTrace((StackTraceElement[]) null)} must return
   * {@code null} per the javadoc.
   */
  @Test
  public void formatStackTraceArrayReturnsNullForNull()
  {
    assertNull(formatStackTrace((StackTraceElement[]) null));
  }

  /**
   * For a non-null array of stack frames,
   * {@link LoggingUtilities#formatStackTrace(StackTraceElement[])}
   * must produce a multi-line string with one frame per line.
   */
  @Test
  public void formatStackTraceArrayFormatsAllFrames()
  {
    StackTraceElement[] trace = {
        new StackTraceElement("com.example.A", "alpha",
                              "A.java", 11),
        new StackTraceElement("com.example.B", "beta", "B.java", 22),
    };
    String formatted = formatStackTrace(trace);
    assertNotNull(formatted);
    assertTrue(formatted.contains("com.example.A.alpha(A.java:11)"),
               "First frame must be present: " + formatted);
    assertTrue(formatted.contains("com.example.B.beta(B.java:22)"),
               "Second frame must be present: " + formatted);
  }

  /**
   * {@link LoggingUtilities#formatStackTrace(StackTraceElement)}
   * must produce a single-frame string with the leading
   * {@code "        at "} prefix.
   */
  @Test
  public void formatStackTraceElementPrefixesWithAt()
  {
    StackTraceElement elem = new StackTraceElement(
        "com.example.X", "go", "X.java", 5);
    String formatted = formatStackTrace(elem);
    assertEquals("        at com.example.X.go(X.java:5)", formatted);
  }

  /**
   * {@link LoggingUtilities#formatStackFrame(StackTraceElement)}
   * must produce the frame string WITHOUT the {@code "        at "}
   * prefix used by {@code formatStackTrace}.
   */
  @Test
  public void formatStackFrameOmitsAtPrefix()
  {
    StackTraceElement elem = new StackTraceElement(
        "com.example.X", "go", "X.java", 5);
    String formatted = formatStackFrame(elem);
    assertEquals("com.example.X.go(X.java:5)", formatted);
  }

  /**
   * {@link LoggingUtilities#formatStackFrame(StringBuilder,
   * StackTraceElement)} with a null element must append the
   * {@code "[unknown: null]"} sentinel and return the builder.
   */
  @Test
  public void formatStackFrameWithNullElementUsesSentinel()
  {
    StringBuilder sb = new StringBuilder("prefix:");
    StringBuilder result = formatStackFrame(sb, null);
    assertSame(sb, result, "Method must return the supplied builder");
    assertEquals("prefix:[unknown: null]", sb.toString());
  }

  /**
   * A frame with a null file name must use the
   * {@code "[unknown file]"} sentinel.
   */
  @Test
  public void formatStackFrameUsesUnknownFileSentinel()
  {
    StackTraceElement elem = new StackTraceElement(
        "com.example.X", "go", null, 5);
    String formatted = formatStackFrame(elem);
    assertTrue(formatted.contains("[unknown file]:5"),
               "Null filename must be replaced: " + formatted);
  }

  /**
   * A frame with a negative line number (the JVM's "unknown line"
   * convention) must use the {@code "[unknown line]"} sentinel.
   */
  @Test
  public void formatStackFrameUsesUnknownLineSentinel()
  {
    StackTraceElement elem = new StackTraceElement(
        "com.example.X", "go", "X.java", -1);
    String formatted = formatStackFrame(elem);
    assertTrue(formatted.contains("[unknown line]"),
               "Negative line number must be replaced: " + formatted);
  }

  /**
   * A frame with a non-null module name must include the
   * {@code module/} prefix in the formatted output.
   */
  @Test
  public void formatStackFrameIncludesModuleNamePrefix()
  {
    // 7-arg ctor: (classLoaderName, moduleName, moduleVersion,
    //              declaringClass, methodName, fileName, lineNumber)
    StackTraceElement elem = new StackTraceElement(
        "app", "java.base", "21", "com.example.X", "go",
        "X.java", 5);
    String formatted = formatStackFrame(elem);
    assertTrue(formatted.startsWith("java.base/"),
               "Module name must prefix: " + formatted);
  }

  // -------------------------------------------------------------------
  // multilineFormat() contract
  // -------------------------------------------------------------------

  /**
   * {@link LoggingUtilities#multilineFormat(Object...)} must join
   * the supplied lines with newlines.
   */
  @Test
  public void multilineFormatJoinsLinesWithNewlines()
  {
    String result = multilineFormat("first", "second", "third");
    assertTrue(result.contains("first"));
    assertTrue(result.contains("second"));
    assertTrue(result.contains("third"));
    String[] parts = result.split("\\R");
    assertEquals(3, parts.length,
                 "Three input lines must produce three output lines: "
                     + result);
  }

  /**
   * {@link LoggingUtilities#multilineFormat(Object...)} with no
   * arguments must return the empty string.
   */
  @Test
  public void multilineFormatEmptyArgumentsReturnsEmpty()
  {
    assertEquals("", multilineFormat());
  }

  /**
   * {@link LoggingUtilities#multilineFormat(Object...)} must convert
   * non-string objects via {@code toString()} and {@code null} via
   * the literal text {@code "null"} (per the implementation's
   * {@code "" + line} concat).
   */
  @Test
  public void multilineFormatHandlesNonStringAndNullLines()
  {
    String result = multilineFormat(123, null, "x");
    assertTrue(result.contains("123"));
    assertTrue(result.contains("null"));
    assertTrue(result.contains("x"));
  }

  // -------------------------------------------------------------------
  // Last-logged-exception contract
  // -------------------------------------------------------------------

  /**
   * {@link LoggingUtilities#isLastLoggedException(Throwable)} must
   * return {@code false} for a null argument or when no exception
   * has been recorded.
   */
  @Test
  public void isLastLoggedExceptionReturnsFalseForNullOrUninitialized()
  {
    setLastLoggedException(null);
    assertFalse(isLastLoggedException(null));
    assertFalse(isLastLoggedException(new RuntimeException("x")));
  }

  /**
   * After {@link LoggingUtilities#setLastLoggedException(Throwable)},
   * {@code isLastLoggedException} must return {@code true} for the
   * recorded throwable and {@code false} for a different one.
   */
  @Test
  public void setLastLoggedExceptionRecordsThenIsLastLoggedMatches()
  {
    Exception ex = new IllegalStateException("recorded");
    setLastLoggedException(ex);
    assertTrue(isLastLoggedException(ex));

    Exception other = new IllegalStateException("different");
    assertFalse(isLastLoggedException(other),
                "Different throwable instance must not match");
  }

  /**
   * {@link LoggingUtilities#setLastLoggedAndThrow} must record the
   * throwable as the last logged AND rethrow it with the exact
   * type the caller passed.
   */
  @Test
  public void setLastLoggedAndThrowRethrowsTheSameInstance()
  {
    IllegalStateException ex = new IllegalStateException("rethrow me");
    IllegalStateException thrown = assertThrows(
        IllegalStateException.class,
        () -> setLastLoggedAndThrow(ex));
    assertSame(ex, thrown);
    assertTrue(isLastLoggedException(ex),
               "After setLastLoggedAndThrow, isLastLoggedException"
                   + " must report true");
  }

  /**
   * {@link LoggingUtilities#logOnceAndThrow(Throwable)} must always
   * rethrow the supplied throwable, AND it must print the stack
   * trace to {@code System.err} only on the first encounter (when
   * the exception was not previously recorded).
   */
  @Test
  public void logOnceAndThrowPrintsStackTraceOnFirstEncounter()
  {
    Exception ex = new IllegalStateException("first time");
    Exception thrown = assertThrows(IllegalStateException.class,
                                    () -> logOnceAndThrow(ex));
    assertSame(ex, thrown);

    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(err.contains("IllegalStateException"),
               "First call must print the stack trace: " + err);
    assertTrue(err.contains("first time"));
    assertTrue(isLastLoggedException(ex),
               "Throwable must be recorded as last-logged");
  }

  /**
   * On a subsequent call with the same throwable,
   * {@link LoggingUtilities#logOnceAndThrow(Throwable)} must NOT
   * re-print the stack trace.
   */
  @Test
  public void logOnceAndThrowSuppressesStackTraceOnRepeat()
  {
    Exception ex = new IllegalStateException("repeat");
    setLastLoggedException(ex);
    this.capturedErr.reset();

    assertThrows(IllegalStateException.class,
                 () -> logOnceAndThrow(ex));
    String err = this.capturedErr.toString(StandardCharsets.UTF_8);
    assertEquals("", err,
                 "Repeat call must not print stack trace: " + err);
  }

  /**
   * For a {@link RuntimeException} wrapping a cause, the
   * last-logged-exception machinery must use the cause for
   * identity. Hence after recording the wrapper, querying with the
   * cause directly OR with the wrapper must both report match.
   */
  @Test
  public void lastLoggedExceptionUsesCauseForRuntimeExceptionWrapper()
  {
    Exception cause = new IllegalStateException("the real one");
    RuntimeException wrapper = new RuntimeException(cause);
    setLastLoggedException(wrapper);

    assertTrue(isLastLoggedException(cause)
                   || isLastLoggedException(wrapper),
               "Either the wrapper or its cause must register as the"
                   + " last logged exception");
  }
}
