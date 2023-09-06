package com.senzing.util;

import com.senzing.g2.engine.G2Fallible;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides logging utilities.
 */
public class LoggingUtilities {
  /**
   * Used for good measure to avoid interlacing of out debug output.
   */
  private static final Object STDOUT_MONITOR = new Object();

  /**
   * Used for good measure to avoid interlacing of out debug output.
   */
  private static final Object STDERR_MONITOR = new Object();

  /**
   * The date-time pattern for the build number.
   */
  private static final String LOG_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";

  /**
   * The time zone used for the time component of the build number.
   */
  private static final ZoneId LOG_DATE_ZONE = ZoneId.of("UTC");

  /**
   * The {@link DateTimeFormatter} for interpretting the build number as a
   * LocalDateTime instance.
   */
  private static final DateTimeFormatter LOG_DATE_FORMATTER
      = DateTimeFormatter.ofPattern(LOG_DATE_PATTERN).withZone(LOG_DATE_ZONE);

  /**
   * The base product ID to log with if the calling package is not overidden.
   */
  public static final String BASE_PRODUCT_ID = "5025";

  /**
   * The system property for indicating that debug logging should be enabled.
   */
  public static final String DEBUG_SYSTEM_PROPERTY = "com.senzing.debug";

  /**
   * The last logged exception in this thread.
   */
  private static final ThreadLocal<Long> LAST_LOGGED_EXCEPTION
      = new ThreadLocal<>();

  /**
   * The {@link Map} of package prefixes to product ID's.
   */
  private static final Map<String, String> PRODUCT_ID_MAP
      = new LinkedHashMap<>();

  /**
   * Private default constructor
   */
  private LoggingUtilities() {
    // do nothing
  }

  /**
   * Sets the product ID to use when logging messages for classes in the
   * specified package name.
   *
   * @param packageName The package name.
   * @param productId The product ID to use for the package.
   */
  public static void setProductIdForPackage(String packageName,
                                            String productId)
  {
    synchronized (PRODUCT_ID_MAP) {
      PRODUCT_ID_MAP.put(packageName, productId);
    }
  }

  /**
   * Gets the product ID for the package name of the class performing the
   * logging.
   *
   * @param packageName The package name.
   *
   * @return The product ID to log with.
   */
  public static String getProductIdForPackage(String packageName) {
    synchronized (PRODUCT_ID_MAP) {
      do {
        // check if we have a product ID for the package
        if (PRODUCT_ID_MAP.containsKey(packageName)) {
          return PRODUCT_ID_MAP.get(packageName);
        }

        // check if the package name begins with com.senzing and get next part
        int prefixLength = "com.senzing.".length();
        if (packageName.startsWith("com.senzing.")
            && packageName.length() > prefixLength)
        {
          int index = packageName.indexOf(".", prefixLength);
          if (index < 0) index = packageName.length();
          return packageName.substring(prefixLength, index);
        }

        // strip off the last part of the package name
        int index = packageName.lastIndexOf('.');
        if (index <= 0) break;
        if (index == (packageName.length() - 1)) break;
        packageName = packageName.substring(0, index);

      } while (packageName.length() > 0 && !packageName.equals("com.senzing"));

      // return the base product ID if we get here
      return BASE_PRODUCT_ID;
    }
  }

  /**
   * Checks if debug logging is enabled.
   *
   * @return <code>true</code> if debug logging is enabled, and
   *         <code>false</code> if not.
   */
  public static boolean isDebugLogging() {
    String value = System.getProperty(DEBUG_SYSTEM_PROPERTY);
    if (value == null) return false;
    return (value.trim().equalsIgnoreCase(Boolean.TRUE.toString()));
  }

  /**
   * Produces a single or multi-line error log message with a consistent prefix
   * and timestamp.
   *
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logError(Object... lines)
  {
    log(System.err, STDERR_MONITOR, "ERROR", lines, null);
  }

  /**
   * Produces a multi-line error log message with a consistent prefix
   * and timestamp that will conclude with the stack trace for the specified
   * {@link Throwable}.
   *
   * @param throwable The {@link Throwable} whose stack trace should be logged.
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logError(Throwable throwable, Object... lines)
  {
    log(System.err, STDERR_MONITOR, "ERROR", lines, throwable);
  }

  /**
   * Produces a single or multi-line warning log message with a consistent
   * prefix and timestamp.
   *
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logWarning(Object... lines)
  {
    log(System.err, STDERR_MONITOR, "WARNING", lines, null);
  }

  /**
   * Produces a multi-line warning log message with a consistent prefix
   * and timestamp that will conclude with the stack trace for the specified
   * {@link Throwable}.
   *
   * @param throwable The {@link Throwable} whose stack trace should be logged.
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logWarning(Throwable throwable, Object... lines)
  {
    log(System.err, STDERR_MONITOR, "WARNING", lines, throwable);
  }

  /**
   * Produces a single or multi-line error log message with a consistent prefix
   * and timestamp.
   *
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logInfo(Object... lines)
  {
    log(System.out, STDOUT_MONITOR, "INFO", lines, null);
  }

  /**
   * Produces a single or multi-line debug message with a consistent prefix
   * and timestamp IF {@linkplain #isDebugLogging() debug logging} is turned on.
   *
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   */
  public static void logDebug(Object... lines)
  {
    if (!isDebugLogging()) return;
    log(System.out, STDOUT_MONITOR, "DEBUG", lines, null);
  }

  /**
   * Produces a single or multi-line debug message with a consistent prefix
   * and timestamp IF {@linkplain #isDebugLogging() debug logging} is turned on.
   *
   * @param lines The lines of text to log, which may be objects that will be
   *              converted to text via {@link Object#toString()}.
   * @deprecated Use {@link #logDebug(Object...)} instead.
   * @see #logDebug(Object...)
   */
  public static void debugLog(String... lines)
  {
    if (!isDebugLogging()) return;
    log(System.out, STDOUT_MONITOR, "DEBUG", lines, null);
  }

  /**
   * Produces a single or multi-line log message with a consistent prefix
   * and timestamp using the specified {@link PrintStream}, {@link Object} to
   * synchronize on, {@link String} log type and lines of text to log.
   *
   * @param ps The {@link PrintStream} to write to.
   * @param monitor The {@link Object} to synchronize on when writing and
   *                flushing the {@link PrintStream}.
   * @param logType The logging type such as <code>"DEBUG"</code> or
   *                <code>"ERROR"</code>.
   * @param lines The lines of text to log.
   * @param throwable The exception to log.
   */
  private static void log(PrintStream ps,
                          Object      monitor,
                          String      logType,
                          Object[]    lines,
                          Throwable   throwable)
  {
    Thread currentThread = Thread.currentThread();
    StackTraceElement[] stackTrace = currentThread.getStackTrace();
    StackTraceElement caller = stackTrace[3];
    String callingClass = caller.getClassName();
    int index = callingClass.lastIndexOf(".");

    String packageName  = callingClass.substring(0, index);
    callingClass = callingClass.substring(index+1);

    String productId = getProductIdForPackage(packageName);

    StringBuilder sb = new StringBuilder();
    String timestamp
        = LOG_DATE_FORMATTER.format(Instant.now().atZone(LOG_DATE_ZONE));
    sb.append(timestamp).append(" senzing-").append(productId)
        .append(" (").append(logType).append(")")
        .append(" [").append(Thread.currentThread().getId())
        .append("|").append(callingClass).append(".")
        .append(caller.getMethodName()).append(":")
        .append(caller.getLineNumber()).append("] ")
        .append(multilineFormat(lines));

    // handle the stack trace if a throwable is provided
    if (throwable != null) {
      StringWriter  sw = new StringWriter();
      PrintWriter   pw = new PrintWriter(sw);
      throwable.printStackTrace(pw);
      sb.append(sw.toString());
    }

    synchronized (monitor) {
      ps.print(sb);
      ps.flush();
    }
  }

  /**
   * Formats a multiline message.
   *
   * @param lines The lines to be formatted.
   *
   * @return The formatted multiline {@link String}.
   */
  public static String multilineFormat(Object... lines) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    for (Object line : lines) {
      pw.println("" + line);
    }
    pw.flush();
    return sw.toString();
  }


  /**
   * Formats an error message from a {@link G2Fallible} instance, including
   * the details (which may contain sensitive PII information) as part of
   * the result.
   *
   * @param operation The name of the operation from the native API interface
   *                  that was attempted but failed.
   * @param fallible The {@link G2Fallible} from which to extract the error
   *                 message.
   * @return The multi-line formatted log message.
   */
  public static String formatError(String operation, G2Fallible fallible)
  {
    return formatError(operation, fallible, true);
  }

  /**
   * Formats an error message from a {@link G2Fallible} instance, optionally
   * including the details (which may contain sensitive PII information) as
   * part of the result.
   *
   * @param operation The name of the operation from the native API interface
   *                  that was attempted but failed.
   * @param fallible The {@link G2Fallible} from which to extract the error
   *                 message.
   * @param includeDetails <code>true</code> to include the details of the failure
   *                       failure in the resultant message, and <code>false</code>
   *                       to exclude them (usually to avoid logging sensitive
   *                       information).
   * @return The multi-line formatted log message.
   */
  public static String formatError(String     operation,
                                   G2Fallible fallible,
                                   boolean    includeDetails)
  {
    int errorCode = fallible.getLastExceptionCode();
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    pw.println();
    pw.println("Operation Failed : " + operation);
    pw.println("Error Code       : " + errorCode);

    if (includeDetails) {
      String message = fallible.getLastException();
      pw.println("Reason           : " + message);
    }
    pw.println();
    pw.flush();
    return sw.toString();
  }

  /**
   * Logs an error message from a {@link G2Fallible} instance, including the
   * details (which may contain sensitive PII information) as part of the
   * result.
   *
   * @param operation The name of the operation from the native API interface
   *                  that was attempted but failed.
   * @param fallible The {@link G2Fallible} from which to extract the error
   *                 message.
   */
  public static void logError(String      operation,
                              G2Fallible  fallible)
  {
    logError(operation, fallible, true);
  }

  /**
   * Logs an error message from a {@link G2Fallible} instance, optionally
   * including the details (which may contain sensitive PII information) as
   * part of the result.
   *
   * @param operation The name of the operation from the native API interface
   *                  that was attempted but failed.
   * @param fallible The {@link G2Fallible} from which to extract the error
   *                 message.
   * @param includeDetails <code>true</code> to include the details of the failure
   *                       failure in the resultant message, and <code>false</code>
   *                       to exclude them (usually to avoid logging sensitive
   *                       information).
   */
  public static void logError(String      operation,
                              G2Fallible  fallible,
                              boolean     includeDetails)
  {
    String message = formatError(operation, fallible, includeDetails);
    System.err.println(message);
  }

  /**
   * Convert a throwable to a {@link Long} value so we don't keep a reference
   * to what could be a complex exception object.
   * @param t The throwable to convert.
   * @return The long hash representation to identify the throwable instance.
   */
  private static Long throwableToLong(Throwable t) {
    if (t == null) return null;
    if (t.getClass() == RuntimeException.class && t.getCause() != null) {
      t = t.getCause();
    }
    long hash1 = (long) System.identityHashCode(t);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.flush();
    long hash2 = (long) sw.toString().hashCode();
    return ((hash1 << 32) | hash2);
  }

  /**
   * Checks if the specified {@link Throwable} is the last logged exception
   * or if the class of specified {@link Throwable} is {@link RuntimeException}
   * and it has a {@linkplain Throwable#getCause()} then if the cause is the
   * last logged exception.  This is handy for telling if the exception has already been logged by a
   * deeper level of the stack trace.
   * @param t The {@link Throwable} to check.
   * @return <code>true</code> if it is the last logged exception, otherwise
   *         <code>false</code>.
   */
  public static boolean isLastLoggedException(Throwable t) {
    if (t == null) return false;
    if (LAST_LOGGED_EXCEPTION.get() == null) return false;
    long value = throwableToLong(t);
    return (LAST_LOGGED_EXCEPTION.get() == value);
  }

  /**
   * Sets the specified {@link Throwable} as the last logged exception.
   * This is handy for telling if the exception has already been logged by a
   * deeper level of the stack trace.
   *
   * @param t The {@link Throwable} to set as the last logged exception.
   */
  public static void setLastLoggedException(Throwable t) {
    LAST_LOGGED_EXCEPTION.set(throwableToLong(t));
  }

  /**
   * Sets the last logged exception and rethrows the specified exception.
   * @param t The {@link Throwable} describing the exception.
   * @throws T To rethrow the specified exception.
   * @param <T> The type of the specified exception which is then thrown.
   */
  public static <T extends Throwable> void setLastLoggedAndThrow(T t)
    throws T
  {
    setLastLoggedException(t);
    throw t;
  }

  /**
   * Conditionally logs to stderr the specified {@link Throwable} is <b>not</b> the {@linkplain
   * #isLastLoggedException(Throwable) last logged exception} and then rethrows
   * it.
   *
   * @param t The {@link Throwable} to log and throw.
   * @return Never returns anything since it always throws.
   * @throws T To rethrow the specified exception.
   * @param <T> The type of the specified exception which is then thrown.
   */
  public static <T extends Throwable> T logOnceAndThrow(T t)
    throws T
  {
    if (!isLastLoggedException(t)) {
      t.printStackTrace();
    }
    setLastLoggedAndThrow(t);
    return null; // we never get here
  }
}
