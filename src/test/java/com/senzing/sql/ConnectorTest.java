package com.senzing.sql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the {@link Connector} interface — specifically the static
 * helper {@link Connector#formatConnectionProperties(Map)}.
 *
 * <p>Each test asserts the documented contract: {@code null} or empty
 * input yields the empty string; a populated map yields a URL-encoded
 * query string with a leading {@code ?} and ampersand-separated
 * key/value pairs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ConnectorTest
{
  /**
   * {@code formatConnectionProperties(null)} must return the empty
   * string per the javadoc.
   */
  @Test
  public void formatNullMapReturnsEmptyString()
  {
    assertEquals("", Connector.formatConnectionProperties(null));
  }

  /**
   * {@code formatConnectionProperties} on an empty map must return
   * the empty string per the javadoc.
   */
  @Test
  public void formatEmptyMapReturnsEmptyString()
  {
    assertEquals("",
                 Connector.formatConnectionProperties(
                     Collections.emptyMap()));
  }

  /**
   * A single key/value entry must produce {@code "?key=value"}.
   */
  @Test
  public void formatSingleEntryProducesLeadingQuestionMark()
  {
    Map<String, String> props = new LinkedHashMap<>();
    props.put("mode", "memory");
    assertEquals("?mode=memory",
                 Connector.formatConnectionProperties(props));
  }

  /**
   * Multiple entries must be ampersand-separated, in insertion order
   * for an order-preserving map ({@link LinkedHashMap}).
   */
  @Test
  public void formatMultipleEntriesUsesAmpersandSeparator()
  {
    Map<String, String> props = new LinkedHashMap<>();
    props.put("user", "alice");
    props.put("password", "secret");
    assertEquals("?user=alice&password=secret",
                 Connector.formatConnectionProperties(props));
  }

  /**
   * Special characters in keys or values must be URL-encoded per the
   * implementation's use of {@code urlEncodeUtf8}.
   */
  @Test
  public void formatUrlEncodesSpecialCharacters()
  {
    Map<String, String> props = new LinkedHashMap<>();
    props.put("name with space", "value/with&special=chars");
    String formatted = Connector.formatConnectionProperties(props);
    // Space encoded as either '+' or '%20'; '&' and '=' must be
    // percent-encoded in the value to avoid corrupting the structure.
    assertEquals(true, formatted.startsWith("?"),
                 "Output must start with '?'");
    assertEquals(true,
                 formatted.contains("name+with+space")
                     || formatted.contains("name%20with%20space"),
                 "Key spaces must be URL-encoded: " + formatted);
    assertEquals(true,
                 formatted.contains("%26") && formatted.contains("%3D"),
                 "'&' and '=' in value must be percent-encoded: "
                     + formatted);
  }
}
