package com.senzing.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import javax.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link RecordReader} tests targeting the constructor
 * pass-through overloads, the CSV empty-value/error paths, and the
 * {@link RecordReader#main} smoke driver.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class RecordReaderExtraTest
{
  // -------------------------------------------------------------------
  // Constructor overloads — exercise each pass-through constructor
  // -------------------------------------------------------------------

  @Test
  public void readerOnlyConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        new StringReader("{\"a\":1}\n"));
    assertEquals(RecordReader.Format.JSON_LINES, rr.getFormat());
    JsonObject rec = rr.readRecord();
    assertNotNull(rec);
    assertEquals(1, rec.getInt("a"));
  }

  @Test
  public void formatAndReaderConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        RecordReader.Format.JSON_LINES,
        new StringReader("{\"x\":1}\n"));
    assertEquals(RecordReader.Format.JSON_LINES, rr.getFormat());
  }

  @Test
  public void readerAndDataSourceConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        new StringReader("{\"a\":1}\n"), "TEST");
    JsonObject rec = rr.readRecord();
    // The dataSource is applied via the null-key mapping.
    assertEquals("TEST", rec.getString("DATA_SOURCE"));
  }

  @Test
  public void formatReaderAndDataSourceConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        RecordReader.Format.JSON_LINES,
        new StringReader("{\"a\":1}\n"),
        "TEST2");
    JsonObject rec = rr.readRecord();
    assertEquals("TEST2", rec.getString("DATA_SOURCE"));
  }

  @Test
  public void readerDataSourceAndSourceIdConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        new StringReader("{\"a\":1}\n"), "DS", "SRC");
    JsonObject rec = rr.readRecord();
    assertEquals("DS", rec.getString("DATA_SOURCE"));
    assertEquals("SRC", rec.getString("SOURCE_ID"));
  }

  @Test
  public void formatReaderDataSourceSourceIdConstructor() throws IOException
  {
    RecordReader rr = new RecordReader(
        RecordReader.Format.JSON_LINES,
        new StringReader("{\"a\":1}\n"),
        "DS",
        "SRC");
    JsonObject rec = rr.readRecord();
    assertEquals("DS", rec.getString("DATA_SOURCE"));
    assertEquals("SRC", rec.getString("SOURCE_ID"));
  }

  @Test
  public void readerAndDataSourceMapConstructor() throws IOException
  {
    Map<String, String> map = Map.of("OLD", "NEW");
    RecordReader rr = new RecordReader(
        new StringReader("{\"DATA_SOURCE\":\"OLD\",\"a\":1}\n"), map);
    JsonObject rec = rr.readRecord();
    assertEquals("NEW", rec.getString("DATA_SOURCE"),
                 "Mapped data source code should be substituted");
  }

  @Test
  public void formatReaderAndDataSourceMapConstructor() throws IOException
  {
    Map<String, String> map = Map.of("OLD", "NEW");
    RecordReader rr = new RecordReader(
        RecordReader.Format.JSON_LINES,
        new StringReader("{\"DATA_SOURCE\":\"OLD\"}\n"),
        map);
    JsonObject rec = rr.readRecord();
    assertEquals("NEW", rec.getString("DATA_SOURCE"));
  }

  @Test
  public void readerDataSourceMapSourceIdConstructor() throws IOException
  {
    Map<String, String> map = Map.of("OLD", "NEW");
    RecordReader rr = new RecordReader(
        new StringReader("{\"DATA_SOURCE\":\"OLD\"}\n"), map, "SRC1");
    JsonObject rec = rr.readRecord();
    assertEquals("NEW", rec.getString("DATA_SOURCE"));
    assertEquals("SRC1", rec.getString("SOURCE_ID"));
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_ERR)
  public void dataSourceMapWithNullValueRaisesNPE() throws IOException
  {
    // Per implementation: null values in dataSourceMap trigger NPE
    // (followed by stderr printing and rethrow).
    Map<String, String> map = new HashMap<>();
    map.put("OLD", null);
    PrintStream origErr = System.err;
    try {
      System.setErr(new PrintStream(new ByteArrayOutputStream()));
      assertThrows(NullPointerException.class,
                   () -> new RecordReader(new StringReader("{}\n"), map));
    } finally {
      System.setErr(origErr);
    }
  }

  // -------------------------------------------------------------------
  // Format auto-detection branches
  // -------------------------------------------------------------------

  @Test
  public void formatInferredJsonArrayFromOpenBracket() throws IOException
  {
    RecordReader rr = new RecordReader(
        new StringReader("[{\"a\":1},{\"a\":2}]"));
    assertEquals(RecordReader.Format.JSON, rr.getFormat());

    JsonObject r1 = rr.readRecord();
    JsonObject r2 = rr.readRecord();
    JsonObject r3 = rr.readRecord();
    assertNotNull(r1);
    assertNotNull(r2);
    assertNull(r3);
  }

  @Test
  public void formatInferredCsvFromAlphabetic() throws IOException
  {
    RecordReader rr = new RecordReader(
        new StringReader("a,b\n1,2\n"));
    assertEquals(RecordReader.Format.CSV, rr.getFormat());
  }

  // -------------------------------------------------------------------
  // CSV empty-value handling and error path
  // -------------------------------------------------------------------

  @Test
  public void csvEmptyValuesRemoved() throws IOException
  {
    // Per implementation: null/empty string values are removed from the
    // resulting JSON record.
    RecordReader rr = new RecordReader(
        RecordReader.Format.CSV,
        new StringReader("a,b,c\n1,,3\n"));
    JsonObject rec = rr.readRecord();
    assertTrue(rec.containsKey("a"));
    assertEquals("1", rec.getString("a"));
    // Empty value for "b" is removed.
    assertEquals(false, rec.containsKey("b"),
                 "Empty CSV column should be omitted from JSON record");
    assertTrue(rec.containsKey("c"));
  }

  @Test
  public void csvWhitespaceOnlyValueRemoved() throws IOException
  {
    // Whitespace-only values trim() to empty and are removed.
    // CSV setTrim(true) trims fields, but the explicit check guards
    // against null/empty after trim.
    RecordReader rr = new RecordReader(
        RecordReader.Format.CSV,
        new StringReader("a,b\n1,   \n"));
    JsonObject rec = rr.readRecord();
    assertEquals(false, rec.containsKey("b"),
                 "Whitespace-only CSV value should be omitted");
  }

  // -------------------------------------------------------------------
  // JSON-Lines IOException wrapping
  // -------------------------------------------------------------------

  @Test
  public void jsonLinesReaderClosedAfterEof() throws IOException
  {
    // The JSON-Lines provider closes its reader after reading null.
    // After EOF, subsequent readRecord calls should keep returning null.
    RecordReader rr = new RecordReader(
        RecordReader.Format.JSON_LINES,
        new StringReader("{\"a\":1}\n"));
    assertNotNull(rr.readRecord());
    assertNull(rr.readRecord());
    assertNull(rr.readRecord(),
               "Subsequent calls after EOF should remain null");
  }

  // -------------------------------------------------------------------
  // main() smoke driver
  // -------------------------------------------------------------------

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_OUT)
  public void mainPrintsRecordsForJsonLinesFile() throws IOException
  {
    File temp = File.createTempFile("rrtest-", ".jsonl");
    temp.deleteOnExit();
    Files.writeString(temp.toPath(), "{\"a\":1}\n{\"b\":2}\n");

    PrintStream origOut = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(baos));
      RecordReader.main(new String[] {temp.getAbsolutePath()});
    } finally {
      System.setOut(origOut);
    }

    String output = baos.toString();
    assertTrue(output.contains("FORMAT"),
               "main() should print FORMAT header: " + output);
    assertTrue(output.contains("COUNT"),
               "main() should print COUNT footer: " + output);
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_ERR)
  public void mainHandlesMissingFile()
  {
    // main catches all exceptions and prints them via printStackTrace.
    // We just verify it does not throw.
    PrintStream origErr = System.err;
    try {
      System.setErr(new PrintStream(new ByteArrayOutputStream()));
      RecordReader.main(new String[] {"/nonexistent/path/file.json"});
    } finally {
      System.setErr(origErr);
    }
  }

  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  @ResourceLock(Resources.SYSTEM_OUT)
  public void mainNoArgsDoesNothing()
  {
    PrintStream origOut = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(baos));
      RecordReader.main(new String[] {});
    } finally {
      System.setOut(origOut);
    }
    assertEquals("", baos.toString().trim(),
                 "main with no args should produce no output");
  }
}
