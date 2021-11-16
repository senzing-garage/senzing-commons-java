package com.senzing.io;

import com.senzing.text.TextUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static com.senzing.io.IOUtilities.*;

/**
 * Tests for {@link IOUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class IOUtilitiesTest {
  public List<Arguments> getReadAsciiLineParams() {
    List<Arguments> result = new LinkedList<>();

    try {
      result.add(arguments(
          "Hello, there.\nMy name is mud.".getBytes("ASCII"),
          "Hello, there."));

      result.add(arguments(
          "Hello, there.\r\nMy name is mud.".getBytes("ASCII"),
          "Hello, there."));

      result.add(arguments(
          "Hello, there.\rMy name is mud.".getBytes("ASCII"),
          "Hello, there.\rMy name is mud."));

      result.add(arguments(
          "Hello, there.".getBytes("ASCII"),
          "Hello, there."));

      result.add(arguments(
          "\n".getBytes("ASCII"),
          ""));

      result.add(arguments(
          "\r\n".getBytes("ASCII"),
          ""));

      result.add(arguments(
          "".getBytes("ASCII"),
          null));

    } catch (UnsupportedEncodingException cannotHappen) {
      throw new IllegalStateException("ASCII encoding is not supported");
    }
    return result;
  }

  @MethodSource("getReadAsciiLineParams")
  @ParameterizedTest
  public void readAsciiLineTest(byte[]    data,
                                String    expectedResult)
  {
    StringBuilder sb = new StringBuilder();
    try {
      sb.append("data=[ ").append(new String(data, "ASCII"))
          .append(" ], expectedResult=[ ").append(expectedResult)
          .append(" ]");

    } catch (UnsupportedEncodingException cannotHappen) {
      throw new IllegalStateException("ASCII encoding not supported");
    }
    String testInfo = sb.toString();

    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      String result = readAsciiLine(bais);

      assertEquals(expectedResult, result,
                   "ASCII line result does not match.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("readAsciiLineTest() failed with exception: " + testInfo
               + ", exception=[ " + e + " ]");
    }
  }

  @Test
  public void closeTest()
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(20);
    close(baos);
    close(null);
  }

  public List<Arguments> getReadTextFileAsStringParams() {
    List<Arguments> result = new LinkedList<>();

    result.add(arguments(
        "Hello, there.  This is some text.\nHow about another line.",
        "UTF-8"));

    result.add(arguments(
        "Hello, there.  This is some text.\nSee you ma\u00F1ana.",
        "UTF-8"));

    result.add(arguments(
        "Hello, there.  This is some text.\nSee you ma\u00F1ana.",
        "UTF-16"));

    result.add(arguments(
        "Hello, there.  This is some text.\nSee you ma\u00F1ana.",
        "UTF-32"));

    result.add(arguments(
        "Hello, there.  This is some text.\nSee you ma\u00F1ana.",
        "ISO-8859-1"));

    return result;
  }

  @MethodSource("getReadTextFileAsStringParams")
  @ParameterizedTest
  public void readTextFileAsStringTest(String text, String encoding)
  {
    String testInfo = "text=[ " + text + " ], encoding=[ " + encoding + " ]";

    try {
      File file = File.createTempFile("test-", ".txt");
      file.deleteOnExit();
      try (FileOutputStream fos = new FileOutputStream(file);
           OutputStreamWriter osw = new OutputStreamWriter(fos, encoding))
      {
        osw.write(text);
        osw.flush();
      }
      String result = readTextFileAsString(file, encoding);
      file.delete();

      assertEquals(
          text, result,
          "File read fully as text did not have the correct text: "
              + testInfo);

    } catch (IOException e) {
      e.printStackTrace();
      fail("readTextFileAsStringTest() failed with exception: " + testInfo
               + ", exception=[ " + e + " ]");
    }
  }

  @Test
  public void readFullyTest()
  {
    try {
      String randomText = TextUtilities.randomPrintableText(4096);
      StringReader reader = new StringReader(randomText);

      String result = readFully(reader);

      assertEquals(randomText, result,
                   "Did not get expected text for readFully(Reader)");

    } catch (IOException e) {
      e.printStackTrace();
      fail("readFullyTest(Reader) failed with exception: " + e);
    }
  }

  private String getTextBlock(boolean includeNonLatin) {
    List<String> result = new LinkedList<>();

    String[] names = {
        "\u7231\u5B50", "John", "Jane", "No\u00EBl", "Ren\u00E9e", "Mary",
        "Robert", "Joseph", "William", "Margaret", "Rebecca", "Brian", "Jesse",
        "Edward", "Josh", "Jeff", "Roy", "Anne", "Kelly", "Leslie", "Joan",
        "Thomas", "April", "Andrew", "Christopher", "Christine", "Benjamin",
        "Ronald", "Donald", "Jamie", "Anthony", "James", "Jerry", "Barry",
        "Keith", "Donna", "Michael", "Gerald", "Laura", "Lauren", "Madison",
        "Gregory" };

    String[] greetings = {
        "Hello _NAME_, how are you?",
        "Good morning _NAME_. Did you sleep well?",
        "Happy birthday dear _NAME_.  Happy birthday to you!",
        "Goodnight _NAME_.  Don't let the bed bugs bite.",
        "What's up _NAME_.  How's it going?",
        "Greetings _NAME_.  How may I help you today?",
        "Hello _NAME_, will you be my neighbor?",
        "Hey _NAME_, what's up?",
        "Good evening _NAME_.",
        "Adios _NAME_.  See you ma\u00F1ana."
    };

    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);
    for (String name: names) {
      if (!includeNonLatin && name.equals("\u7231\u5B50")) continue;
      for (String greeting: greetings) {
        pw.println(greeting.replaceAll("_NAME_", name));
      }
    }
    pw.flush();
    return sw.toString();
  }

  public List<Arguments> getDetectCharacterEncodingParams() {
    List<Arguments> result = new LinkedList<>();

    String text = getTextBlock(false);
    result.add(arguments(text, "UTF-8"));
    result.add(arguments(text, "UTF-16BE"));
    result.add(arguments(text, "UTF-16LE"));
    result.add(arguments(text, "UTF-32BE"));
    result.add(arguments(text, "UTF-32LE"));
    result.add(arguments(text, "ISO-8859-1"));

    text = getTextBlock(true);

    result.add(arguments(text, "UTF-8"));
    result.add(arguments(text, "UTF-16BE"));
    result.add(arguments(text, "UTF-16LE"));
    result.add(arguments(text, "UTF-32BE"));
    result.add(arguments(text, "UTF-32LE"));

    return result;
  }

  public List<Arguments> getBOMSkippingReaderParams() {
    List<Arguments> result = new LinkedList<>();

    String text = getTextBlock(false);

    result.add(arguments(text, "UTF-8"));
    result.add(arguments(text, "UTF-16"));
    result.add(arguments(text, "UTF-16BE"));
    result.add(arguments(text, "UTF-16LE"));
    result.add(arguments(text, "UTF-32"));
    result.add(arguments(text, "UTF-32BE"));
    result.add(arguments(text, "UTF-32LE"));
    result.add(arguments(text, "ISO-8859-1"));

    text = getTextBlock(true);

    result.add(arguments(text, "UTF-8"));
    result.add(arguments(text, "UTF-16"));
    result.add(arguments(text, "UTF-16BE"));
    result.add(arguments(text, "UTF-16LE"));
    result.add(arguments(text, "UTF-32"));
    result.add(arguments(text, "UTF-32BE"));
    result.add(arguments(text, "UTF-32LE"));

    return result;
  }

  @MethodSource("getDetectCharacterEncodingParams")
  @ParameterizedTest
  public void detectCharacterEncodingTest(String text, String encoding)
  {
    try {
      File file = File.createTempFile("test-", ".txt");
      file.deleteOnExit();
      try (FileOutputStream fos = new FileOutputStream(file);
           OutputStreamWriter osw = new OutputStreamWriter(fos, encoding))
      {
        osw.write(text);
        osw.flush();
      }

      String result = null;
      try (FileInputStream fis = new FileInputStream(file)) {
        result = detectCharacterEncoding(fis);
      }
      file.delete();

      assertEquals(encoding, result,
                   "Character encoding not as expected.");

    } catch (IOException e) {
      e.printStackTrace();
      fail("detectCharacterEncodingTest() failed with exception: " + e);
    }
  }

  @MethodSource("getBOMSkippingReaderParams")
  @ParameterizedTest
  public void bomSkippingReaderTest(String text, String encoding) {
    try {
      File file = File.createTempFile("test-", ".txt");
      File bomFile = File.createTempFile("test-", ".txt");

      file.deleteOnExit();
      bomFile.deleteOnExit();
      encoding = encoding.toUpperCase();
      try (FileOutputStream   fos    = new FileOutputStream(file);
           FileOutputStream   bomFOS = new FileOutputStream(bomFile))
      {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(text.length());
        OutputStreamWriter    osw  = new OutputStreamWriter(baos, encoding);
        osw.write(text);
        osw.flush();

        byte[] bytes = baos.toByteArray();

        if ((encoding.startsWith("UTF-16") || encoding.startsWith("UTF-32"))
            && (checkBOM(bytes) > 0))
        {
          // the data already has a BOM, exclude it from the non-BOM file
          int offset = checkBOM(bytes);
          fos.write(bytes, offset, bytes.length - offset);
          bomFOS.write(bytes, 0, bytes.length);

          fos.flush();
          bomFOS.flush();

        } else if (encoding.startsWith("UTF-16")
                   || encoding.startsWith("UTF-32"))
        {
          // the data lacks a BOM, we need to write it directly and with a BOM
          fos.write(bytes, 0, bytes.length);
          fos.flush();

          OutputStreamWriter bomOSW = new OutputStreamWriter(bomFOS, encoding);
          bomOSW.write(0xFEFF);
          bomOSW.write(text);
          bomOSW.flush();

        } else {
          fos.write(bytes, 0, bytes.length);
          bomFOS.write(bytes, 0, bytes.length);

          fos.flush();
          bomFOS.flush();
        }
      }

      // do a sanity check that we have the byte-order-markers where expected
      try (FileInputStream    fis     = new FileInputStream(file);
           FileInputStream    bomFIS  = new FileInputStream(bomFile))
      {
        byte[] bytes    = fis.readNBytes(4);
        byte[] bomBytes = bomFIS.readNBytes(4);

        if (checkBOM(bytes) > 0) {
          fail("Non-BOM file contains a BOM: " + encoding);
        }
        if ((checkBOM(bomBytes) <= 0) &&
            (encoding.startsWith("UTF-16") || encoding.startsWith("UTF-32")))
        {
          fail("BOM file missing BOM: " + encoding + " / "
                   + bytesToHex(bomBytes) + " / " + bomFile);

        } else if ((checkBOM(bomBytes) > 0) && !encoding.startsWith("UTF-16")
                   && !encoding.startsWith("UTF-32"))
        {
          fail("BOM file contains a BOM for wrong encoding: " + encoding);
        }
      }

      String result = null;
      String bomResult = null;
      try (FileInputStream    fis     = new FileInputStream(file);
           FileInputStream    bomFIS  = new FileInputStream(bomFile);
           InputStreamReader  isr     = new InputStreamReader(fis, encoding);
           InputStreamReader  bomISR  = new InputStreamReader(bomFIS, encoding))
      {
        Reader reader = bomSkippingReader(isr, encoding);
        if (!encoding.startsWith("UTF-")) {
          assertEquals(isr, reader,
                       "Reader for non-UTF-8 encoding is not identical: "
                           + encoding);
        } else {
          assertNotEquals(isr, reader,
                          "Readers are the same, not wrapped: "
                              + encoding);
        }
        Reader bomReader = bomSkippingReader(bomISR, encoding);
        if (!encoding.startsWith("UTF-")) {
          assertEquals(bomISR, bomReader,
                       "Reader for non-UTF-8 encoding is not identical: "
                           + encoding);
        } else {
          assertNotEquals(bomISR, bomReader,
                          "Readers are the same, not wrapped: "
                              + encoding);
        }

        // read the text
        result = readFully(reader);
        bomResult = readFully(bomReader);
      }
      file.delete();
      bomFile.delete();

      assertEquals(text, result,
                   "Text read via reader with no BOM does not match: "
                       + encoding);
      assertEquals(text, bomResult,
                   "Text read via reader with BOM does not match: "
                       + encoding);

    } catch (IOException e) {
      e.printStackTrace();
      fail("bomSkippingReaderTest() failed with exception: " + e);
    }
  }

  private static int checkBOM(byte[] bytes) {
    if (bytes[0] == ((byte) 0xFF) && bytes[1] == ((byte) 0xFE)) return 2;
    if (bytes[0] == ((byte) 0xFE) && bytes[1] == ((byte) 0xFF)) return 2;
    if (bytes[0] == 0 && bytes[1] == 0
        && bytes[2] == ((byte) 0xFE) && bytes[3] == ((byte) 0xFF))
    {
      return 4;
    }
    return 0;
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder();
    for (byte b: bytes) {
      String hex = Integer.toString((b & 0xFF), 16);
      if (hex.length() == 1) hex = "0" + hex;
      sb.append(hex);
    }
    return sb.toString();
  }

  @Test
  public void createDirectoryIfMissingTest() {
    try {
      File newDirectory
          = new File("TestDirectory-" + System.currentTimeMillis());
      assertFalse(newDirectory.exists(),
                  "New directory already exists: " + newDirectory);
      boolean result = createDirectoryIfMissing(newDirectory);
      assertTrue(newDirectory.exists(),
                 "New directory does NOT exist: " + newDirectory);
      assertTrue(newDirectory.isDirectory(),
                 "New directory exists but is not a directory: "
                     + newDirectory);
      assertTrue(result,
                 "Directory created, but return value was false: "
                     + newDirectory);
      newDirectory.deleteOnExit();
      result = createDirectoryIfMissing(newDirectory);
      assertTrue(newDirectory.exists(),
                 "New directory does NOT exist on recreate: " + newDirectory);
      assertTrue(newDirectory.isDirectory(),
                 "New directory exists but is not a directory on recreate: "
                     + newDirectory);
      assertFalse(result,
                 "Directory already existed, but return value was true: "
                     + newDirectory);
      newDirectory.delete();

    } catch (Exception e) {
      e.printStackTrace();
      fail("createDirectoryIfMissingTest() failed with exception: " + e);
    }
  }

  @Test
  public void createDirectoryIfMissingOnFileTest() {
    File tempFile = null;
    try {
      tempFile = File.createTempFile("test-", ".txt");
      tempFile.deleteOnExit();
      createDirectoryIfMissing(tempFile);
      fail("Expected failure, but succeeded in created directory over a file: "
               + tempFile);

    } catch (IOException expected) {
      // expected the exception

    } catch (Exception e) {
      e.printStackTrace();
      fail("createDirectoryIfMissingTest() failed with exception: " + e);

    } finally {
      if (tempFile != null) {
        tempFile.delete();
      }
    }
  }

  @Test
  public void recursiveDeleteDirectoryTest() {
    try {
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));

      long now = System.currentTimeMillis();
      File rootDir = new File(tmpDir, "Root-" + now);
      createDirectoryIfMissing(rootDir);

      File subDir = new File(rootDir, "SubDir-" + now);
      createDirectoryIfMissing(subDir);

      File tmpFile1 = File.createTempFile("temp-", ".txt", rootDir);
      File tmpFile2 = File.createTempFile("temp-", ".txt", subDir);

      assertTrue(rootDir.exists() && rootDir.isDirectory(),
                  "Root directory does not exist or is not a directory: "
                      + rootDir);
      assertTrue(subDir.exists() && subDir.isDirectory(),
                 "Sub-directory does not exist or is not a directory: "
                     + subDir);
      assertTrue(tmpFile1.exists() && !tmpFile1.isDirectory(),
                 "First temp file does not exist or is a directory: "
                     + tmpFile1);
      assertTrue(tmpFile2.exists() && !tmpFile2.isDirectory(),
                 "Second temp file does not exist or is a directory: "
                     + tmpFile2);

      int result = recursiveDeleteDirectory(rootDir);

      assertEquals(0, result, "Some files could not be deleted: "
          + result);

      assertFalse(tmpFile2.exists(),
                  "Second temp file still exists after recursive delete: "
                      + tmpFile2);
      assertFalse(tmpFile1.exists(),
                  "First temp file still exists after recursive delete: "
                      + tmpFile1);
      assertFalse(subDir.exists(),
                  "Sub-directory still exists after recursive delete: "
                      + subDir);
      assertFalse(rootDir.exists(),
                  "Root directory still exists after recursive delete: "
                      + rootDir);

    } catch (Exception e) {
      e.printStackTrace();
      fail("recursiveDeleteDirectoryTest() failed with exception: " + e);
    }
  }

  @Test
  public void checkFilesDifferSameTest() {
    try {
      File file1 = File.createTempFile("test-", ".txt");
      File file2 = File.createTempFile("test-", ".txt");

      FileWriter fw1 = new FileWriter(file1);
      FileWriter fw2 = new FileWriter(file2);
      fw1.write("This is a test");
      fw1.flush();
      fw1.close();

      Thread.sleep(10); // force different timestamps
      fw2.write("This is a test");
      fw2.flush();
      fw2.close();

      boolean result = checkFilesDiffer(file1, file2);
      assertFalse(result, "Files show different, but are the same (1): "
                  + file1 + " vs " + file2);

      result = checkFilesDiffer(file1, file2, false);
      assertFalse(result, "Files show different, but are the same (2): "
          + file1 + " vs " + file2);

      result = checkFilesDiffer(file1, file2, true);
      assertTrue(result, "Files show the same despite different timestamps: "
          + file1 + " vs " + file2);

      // update modified timestamps to be the same
      long now = System.currentTimeMillis();
      file1.setLastModified(now);
      file2.setLastModified(now);

      result = checkFilesDiffer(file1, file2, true);
      assertFalse(result, "Files show the different despite same timestamps: "
                  + file1 + " vs " + file2);


      // delete the files
      file1.delete();
      file2.delete();

    } catch (Exception e) {
      e.printStackTrace();
      fail("checkFilesDifferTest() failed with exception: " + e);
    }
  }

  @Test
  public void checkFilesDifferDifferentTest() {
    try {
      File file1 = File.createTempFile("test-", ".txt");
      File file2 = File.createTempFile("test-", ".txt");

      FileWriter fw1 = new FileWriter(file1);
      FileWriter fw2 = new FileWriter(file2);
      fw1.write("This is a test");
      fw2.write("THIS IS A TEST");
      fw1.flush();
      fw1.close();
      fw2.flush();
      fw2.close();

      boolean result = checkFilesDiffer(file1, file2);
      assertTrue(result, "Files show same, but are different (1): "
          + file1 + " vs " + file2);

      result = checkFilesDiffer(file1, file2, false);
      assertTrue(result, "Files show same, but are different (2): "
          + file1 + " vs " + file2);

      result = checkFilesDiffer(file1, file2, true);
      assertTrue(result,
                 "Files same despite different content & timestamps: "
                     + file1 + " vs " + file2);

      file1.delete();
      file2.delete();

    } catch (Exception e) {
      e.printStackTrace();
      fail("checkFilesDifferTest() failed with exception: " + e);
    }
  }

  @Test
  public void checkFilesDifferEdgeTest() {
    try {
      File file1 = File.createTempFile("test-", ".txt");
      File file2 = File.createTempFile("test-", ".txt");
      file1.deleteOnExit();
      file2.deleteOnExit();

      FileWriter fw1 = new FileWriter(file1);
      FileWriter fw2 = new FileWriter(file2);
      fw1.write("This is a test");
      fw2.write("This is a test");
      fw1.flush();
      fw1.close();
      fw2.flush();
      fw2.close();

      boolean result = checkFilesDiffer(file1, file1);
      assertFalse(result, "Files show different, but are same file: " + file1);

      file1.delete();

      result = checkFilesDiffer(file1, file2);
      assertTrue(result, "Files show same, but one does not exist: "
          + file1 + " vs " + file2);

      file2.delete();
      result = checkFilesDiffer(file1, file2, false);
      assertFalse(result, "Files show different, but neither exist: "
          + file1 + " vs " + file2);

    } catch (Exception e) {
      e.printStackTrace();
      fail("checkFilesDifferTest() failed with exception: " + e);
    }
  }

  @Test
  public void touchFileTest() {
    try {
      File tmpDir = new File(System.getProperty("java.io.tmpdir"));

      long now = System.currentTimeMillis();

      File newFile = new File(tmpDir, "test-file-" + now + ".txt");

      assertFalse(newFile.exists(),
                  "File to be touched already exists: " + newFile);

      Thread.sleep(10);

      long result = touchFile(newFile);

      assertTrue(newFile.exists(),
                  "Touched file does not exist: " + newFile);

      assertTrue((result > now),
                 "Last modified time of touched file (" + result
                 + ") is older than expected (" + now + "): " + newFile);

      Thread.sleep(10);

      long result2 = touchFile(newFile);

      assertTrue(newFile.exists(),
                 "Retouched file does not exist: " + newFile);

      assertTrue((result2 > result),
                 "Last modified time of retouched file (" + result2
                     + ") is older than expected (" + result + "): " + newFile);

    } catch (Exception e) {
      e.printStackTrace();
      fail("touchFileTest() failed with exception: " + e);
    }

  }

  protected static class TestInputStream extends ByteArrayInputStream {
    private boolean closed = false;
    public boolean isClosed() {
      return this.closed;
    }

    @Override
    public void close() throws IOException {
      this.closed = true;
      super.close();
    }

    public TestInputStream(byte[] data) {
      super(data);
    }
  }

  protected static class TestOutputStream extends ByteArrayOutputStream {
    private boolean closed = false;
    public boolean isClosed() {
      return this.closed;
    }

    @Override
    public void close() throws IOException {
      this.closed = true;
      super.close();
    }

    public TestOutputStream(int initialCapacity) {
      super(initialCapacity);
    }
  }

  protected static class TestReader extends StringReader {
    private boolean closed = false;
    public boolean isClosed() {
      return this.closed;
    }

    @Override
    public void close() {
      this.closed = true;
      super.close();
    }

    public TestReader(String text) {
      super(text);
    }
  }

  protected static class TestWriter extends StringWriter {
    private boolean closed = false;
    public boolean isClosed() {
      return this.closed;
    }

    @Override
    public void close() throws IOException {
      this.closed = true;
      super.close();
    }

    public TestWriter(int initialCapacity) {
      super(initialCapacity);
    }
  }

  @Test
  public void nonClosingInputStreamTest() {
    try {
      byte[] data = new byte[10];
      TestInputStream tis = new TestInputStream(data);
      InputStream is = nonClosingWrapper(tis);
      is.close();
      assertFalse(tis.isClosed(), "Non-closing input stream is closed");
      tis.close();
      assertTrue(tis.isClosed(), "Underlying input stream not closed");

    } catch (Exception e) {
      e.printStackTrace();
      fail("nonClosingInputStreamTest() failed with exception: " + e);
    }
  }

  @Test
  public void nonClosingOutputStreamTest() {
    try {
      TestOutputStream tos = new TestOutputStream(100);
      OutputStream os = nonClosingWrapper(tos);
      os.close();
      assertFalse(tos.isClosed(), "Non-closing output stream is closed");
      tos.close();
      assertTrue(tos.isClosed(), "Underlying output stream not closed");

    } catch (Exception e) {
      e.printStackTrace();
      fail("nonClosingOutputStreamTest() failed with exception: " + e);
    }
  }

  @Test
  public void nonClosingReaderTest() {
    try {
      TestReader tr = new TestReader("This is some data");
      Reader r = nonClosingWrapper(tr);
      r.close();
      assertFalse(tr.isClosed(), "Non-closing reader is closed");
      tr.close();
      assertTrue(tr.isClosed(), "Underlying reader not closed");

    } catch (Exception e) {
      e.printStackTrace();
      fail("nonClosingReaderTest() failed with exception: " + e);
    }
  }

  @Test
  public void nonClosingWriterTest() {
    try {
      TestWriter tw = new TestWriter(100);
      Writer w = nonClosingWrapper(tw);
      w.close();
      assertFalse(tw.isClosed(), "Non-closing writer is closed");
      tw.close();
      assertTrue(tw.isClosed(), "Underlying writer not closed");

    } catch (Exception e) {
      e.printStackTrace();
      fail("nonClosingWriterTest() failed with exception: " + e);
    }
  }
}
