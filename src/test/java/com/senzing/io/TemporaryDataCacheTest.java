package com.senzing.io;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TemporaryDataCache}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TemporaryDataCacheTest
{
  /**
   * The test file
   */
  private List<File> testFiles;

  /**
   * The base file size for the test files that get generated.
   */
  private static final int BASE_FILE_SIZE = 100;

  /**
   * The test file size.
   */
  private static final int TEST_FILE_COUNT = 4;

  @BeforeAll
  public void setup()
      throws IOException
  {
    this.testFiles = new ArrayList<>(TEST_FILE_COUNT);

    char[] characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    Random random = new Random(System.currentTimeMillis());

    int exponent = 0;
    for (int index = 0; index < TEST_FILE_COUNT; index++) {
      File testFile = File.createTempFile("test-file-", ".dat");
      this.testFiles.add(testFile);
      exponent = index + Math.max(0, index - 1);
      int fileSize = (int) Math.pow(25.0, (double) exponent);

      try (FileOutputStream     fos = new FileOutputStream(testFile);
           BufferedOutputStream bos = new BufferedOutputStream(fos, 8192))
      {
        long writeCount = 0L;
        while (writeCount < fileSize) {
          double nextValue = random.nextDouble();
          int nextIndex = (int) (((double) characters.length) * nextValue);
          bos.write((int) characters[nextIndex]);
          writeCount++;
        }
        bos.flush();
      }
      testFile.deleteOnExit();
    }
    this.testFiles = Collections.unmodifiableList(this.testFiles);
  }

  @AfterAll
  public void teardown()
      throws IOException
  {
    for (File file: this.testFiles) {
      file.delete();
    }
  }

  public List<File> getTestFiles()
  {
    return this.testFiles;
  }

  @ParameterizedTest
  @MethodSource("getTestFiles")
  public void verifyContentAccurate(File file)
      throws IOException
  {
    File tempFile = File.createTempFile("test-file-", ".dat");
    FileInputStream fis = new FileInputStream(file);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is, 8192);
           FileOutputStream     fos = new FileOutputStream(tempFile);
           BufferedOutputStream bos = new BufferedOutputStream(fos, 8192))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          bos.write(byteRead);
        }
        bos.flush();

      } finally {
        tdc.delete();
      }
    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    assertFalse(IOUtilities.checkFilesDiffer(file, tempFile),
                "Read data differs from source file.");
  }

  @Test
  public void testCustomDirectory()
      throws IOException
  {
    File    tempFile      = File.createTempFile("TempDataCache-", ".dat");
    String  tempFileName  = tempFile.getName();
    int     length        = tempFileName.length();
    String  tempDirName   = tempFileName.substring(0, length - ".dat".length());
    File    tempDir       = new File(tempFile.getParent(), tempDirName);

    // create the directory
    tempDir.mkdirs();

    File    inputFile     = this.testFiles.get(0);
    int     preCount      = tempDir.listFiles().length;
    int     maxCount      = 0;

    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis, tempDir);

      assertEquals(tempDir, tdc.getDirectory(),
                   "TemporaryDataCache.getDirectory() has unexpected "
                    + "directory.");

      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          int count = tempDir.listFiles().length;
          if (count > maxCount) {
            maxCount = count;
          }
        }

      } finally {
        tdc.delete();
      }

      int postCount = tempDir.listFiles().length;
      if (preCount > 0) {
        fail("Files already existed in specified directory: " + tempDir);
      }
      if (maxCount == 0) {
        fail("Files were never created in specified directory: " + tempDir);
      }
      if (postCount > 0) {
        fail("Files were never deleted from specified directory: " + tempDir);
      }
    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testFilePrefix()
      throws IOException
  {
    File    tempFile      = File.createTempFile("TempDataCache-", ".dat");
    String  tempFileName  = tempFile.getName();
    int     length        = tempFileName.length();
    String  tempDirName   = tempFileName.substring(0, length - ".dat".length());
    File    tempDir       = new File(tempFile.getParent(), tempDirName);

    // create the directory
    tempDir.mkdirs();

    File    inputFile     = this.testFiles.get(0);
    int     preCount      = tempDir.listFiles().length;
    int     maxCount      = 0;

    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc
          = new TemporaryDataCache(fis, tempDir, "TempDataCache-");

      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          int count = tempDir.listFiles(
              f -> f.getName().startsWith("TempDataCache-")).length;
          if (count > maxCount) {
            maxCount = count;
          }
        }

      } finally {
        tdc.delete();
      }

      int postCount = tempDir.listFiles().length;
      if (preCount > 0) {
        fail("Files already existed in specified directory: " + tempDir);
      }
      if (maxCount == 0) {
        fail("Files with prefix were never created in specified directory: "
                 + tempDir);
      }
      if (postCount > 0) {
        fail("Files were never deleted from specified directory: " + tempDir);
      }
    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testConsuming()
      throws IOException
  {
    File    tempFile      = File.createTempFile("TempDataCache-", ".dat");
    String  tempFileName  = tempFile.getName();
    int     length        = tempFileName.length();
    String  tempDirName   = tempFileName.substring(0, length - ".dat".length());
    File    tempDir       = new File(tempFile.getParent(), tempDirName);

    // create the directory
    tempDir.mkdirs();

    File    inputFile     = this.testFiles.get(this.testFiles.size() - 1);
    int     preCount      = tempDir.listFiles().length;
    int     maxCount      = 0;

    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc
          = new TemporaryDataCache(fis, tempDir, "TempDataCache-");

      tdc.waitUntilAppendingComplete();

      assertFalse(tdc.isAppending(),
                  "TemporaryDataCache is still appending.");

      maxCount = tempDir.listFiles(
          f -> f.getName().startsWith("TempDataCache-")).length;

      try (InputStream          is  = tdc.getInputStream(true);
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          // do nothing
        }
        int count = tempDir.listFiles(
            f -> f.getName().startsWith("TempDataCache-")).length;
        if (count > maxCount) {
          fail("File count increased after consuming TemporaryDataCache: "
                   + tempDir);
        }
        assertEquals(0,count,
                     "File count did not reduce to zero after consuming.");

      } finally {
        int count = tempDir.listFiles(
            f -> f.getName().startsWith("TempDataCache-")).length;
        if (count > 0) {
          fail("Files remaining after consuming entire TemporaryDataCache: "
               + tempDir);
        }
        assertTrue(tdc.isDeleted(),
                   "TemporaryDataCache NOT marked deleted after "
                   + "consuming all data");
        tdc.delete();
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAppending()
      throws IOException
  {
    File inputFile = this.testFiles.get(this.testFiles.size() - 1);
    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      boolean appended = false;
      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read())
        {
          if (!appended) {
            appended = tdc.isAppending();
          }
        }

      } finally {
        assertFalse(tdc.isAppending(),
                    "Instance still appending after read completed");
        tdc.delete();
      }

      if (!appended) {
        fail("Never transitioned to 'appending' state.");
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  /**
   *
   * @throws IOException
   */
  private void backgroundRead(InputStream         is,
                              TemporaryDataCache  tdc,
                              int                 byteCount,
                              long                waitTime)
  {
    // read the bytes
    Thread thread = new Thread(() -> {
      try {
        for (int index = 0; index < byteCount; index++) {
          is.read();
        }
      } catch (IOException ignore) {
        // ignore
      }
    });
    thread.start();
    try {
      // give it 5 seconds to complete
      thread.join(waitTime);
    } catch (InterruptedException ignore) {
      // do nothing
    }
    boolean stillAlive = thread.isAlive();
    if (stillAlive) {
      tdc.delete();
      thread.interrupt();
      fail("Reader thread is still blocked in reading "
               + byteCount + " bytes after " + waitTime + "ms.");
    }
  }

  @Test
  public void testSplitAppending()
      throws IOException
  {
    PipedInputStream  pis = new PipedInputStream();
    PipedOutputStream pos = new PipedOutputStream(pis);

    try {
      TemporaryDataCache tdc = new TemporaryDataCache(pis);
      int writeCount = 128;

      // write a small number of bytes
      for (int index = 0; index < writeCount; index++) {
        pos.write('A');
      }
      pos.flush();
      Thread.sleep(3000L);

      try (InputStream is = tdc.getInputStream()) {
        // read the bytes
        backgroundRead(is, tdc, writeCount, 5000L);

        // write more bytes
        for (int index = 0; index < writeCount; index++) {
          pos.write('A');
        }
        pos.flush();
        pos.close(); // close the pipe

        backgroundRead(is, tdc, writeCount, 5000L);

      } finally {
        tdc.delete();
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testRepeatedRead()
      throws IOException
  {
    File inputFile = this.testFiles.get(0);
    int fileSize = (int) inputFile.length();
    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);

      ByteArrayOutputStream baos1 = new ByteArrayOutputStream(fileSize);
      ByteArrayOutputStream baos2 = new ByteArrayOutputStream(fileSize);

      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          baos1.write(byteRead);
        }
      }

      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          baos2.write(byteRead);
        }
      } finally {
        tdc.delete();
      }

      byte[] bytes1 = baos1.toByteArray();
      byte[] bytes2 = baos2.toByteArray();
      for (int index = 0; index < bytes1.length; index++) {
        if (bytes1[index] != bytes2[index]) {
          fail("TemporaryDataCache data is different after reading it twice");
        }
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testRepeatedReadWithConsume()
      throws IOException
  {
    File inputFile = this.testFiles.get(0);
    int fileSize = (int) inputFile.length();
    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);

      ByteArrayOutputStream baos1 = new ByteArrayOutputStream(fileSize);
      ByteArrayOutputStream baos2 = new ByteArrayOutputStream(fileSize);

      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          baos1.write(byteRead);
        }
      }

      try (InputStream          is  = tdc.getInputStream(true);
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          baos2.write(byteRead);
        }
      }

      byte[] bytes1 = baos1.toByteArray();
      byte[] bytes2 = baos2.toByteArray();
      for (int index = 0; index < bytes1.length; index++) {
        if (bytes1[index] != bytes2[index]) {
          fail("TemporaryDataCache data is different after reading it twice");
        }
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testDeleted()
      throws IOException
  {
    File  inputFile = this.testFiles.get(this.testFiles.size() - 1);
    int   fileSize  = (int) inputFile.length();
    FileInputStream fis = new FileInputStream(inputFile);
    try {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      int readCount = 0;
      try (InputStream          is  = tdc.getInputStream();
           BufferedInputStream  bis = new BufferedInputStream(is))
      {
        for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
          readCount++;
          assertFalse(tdc.isDeleted(),
                      "TemporaryDataCache prematurely marked deleted!");
          if (readCount > fileSize / 2) break;
        }

        tdc.delete();

        // try to read after a delete
        try {
          for (int byteRead = bis.read(); byteRead >= 0; byteRead = bis.read()) {
            readCount++;
            assertTrue(tdc.isDeleted(),
                        "TemporaryDataCache should be marked deleted!");
          }

          fail("Unexpectedly succeeded to read from TemporaryDataCache after "
               + "deletion.");

        } catch (IOException e) {
          // all good
        }

      } finally {
        tdc.delete();
      }

    } catch (RuntimeException|IOException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // -------------------------------------------------------------------
  // ChainFileInputStream — skip / close / read-after-close
  // -------------------------------------------------------------------

  /**
   * {@link InputStream#skip(long) skip(0)} on the cache's input
   * stream must return 0 without advancing.
   */
  @Test
  public void chainStreamSkipZeroReturnsZero() throws Exception
  {
    File f = this.testFiles.get(0);
    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        try (InputStream in = tdc.getInputStream()) {
          assertEquals(0L, in.skip(0L));
        }
      } finally {
        tdc.delete();
      }
    }
  }

  /**
   * Skipping a negative amount must return 0 per the implementation
   * (which short-circuits on {@code n < 0}).
   */
  @Test
  public void chainStreamSkipNegativeReturnsZero() throws Exception
  {
    File f = this.testFiles.get(0);
    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        try (InputStream in = tdc.getInputStream()) {
          assertEquals(0L, in.skip(-100L));
        }
      } finally {
        tdc.delete();
      }
    }
  }

  /**
   * Skipping by N bytes must skip exactly N bytes (or fewer if EOF
   * is reached) and the next byte read must equal the byte at
   * position N in the original file.
   */
  @Test
  public void chainStreamSkipAdvancesByRequestedAmount()
      throws Exception
  {
    File f = this.testFiles.get(0);
    long fileSize = f.length();
    long skipAmount = Math.max(1L, fileSize / 4L);

    int expectedByte;
    try (FileInputStream rawFis = new FileInputStream(f)) {
      rawFis.skip(skipAmount);
      expectedByte = rawFis.read();
    }
    if (expectedByte < 0) return; // file too small; skip the test

    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        try (InputStream in = tdc.getInputStream()) {
          long skipped = in.skip(skipAmount);
          assertEquals(skipAmount, skipped,
                       "Skip must advance by the requested amount"
                           + " when bytes are available");
          int actualByte = in.read();
          assertEquals(expectedByte, actualByte,
                       "Byte after skip must match source-file byte"
                           + " at the same offset");
        }
      } finally {
        tdc.delete();
      }
    }
  }

  /**
   * Calling {@link InputStream#close} twice on the cache's input
   * stream must be safe (the second close is a no-op).
   */
  @Test
  public void chainStreamDoubleCloseIsSafe() throws Exception
  {
    File f = this.testFiles.get(0);
    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        InputStream in = tdc.getInputStream();
        in.close();
        in.close(); // must not throw
      } finally {
        tdc.delete();
      }
    }
  }

  /**
   * Reading from the cache's input stream after it has been closed
   * must throw {@link IOException} per the implementation's
   * explicit "stream already closed" guard.
   */
  @Test
  public void chainStreamReadAfterCloseThrowsIoException()
      throws Exception
  {
    File f = this.testFiles.get(0);
    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        InputStream in = tdc.getInputStream();
        in.close();
        assertThrows(IOException.class, in::read);
      } finally {
        tdc.delete();
      }
    }
  }

  /**
   * Skipping on the cache's input stream after it has been closed
   * must throw {@link IOException}.
   */
  @Test
  public void chainStreamSkipAfterCloseThrowsIoException()
      throws Exception
  {
    File f = this.testFiles.get(0);
    try (FileInputStream fis = new FileInputStream(f)) {
      TemporaryDataCache tdc = new TemporaryDataCache(fis);
      try {
        tdc.waitUntilAppendingComplete();
        InputStream in = tdc.getInputStream();
        in.close();
        assertThrows(IOException.class, () -> in.skip(10L));
      } finally {
        tdc.delete();
      }
    }
  }

  // -------------------------------------------------------------------
  // CacheFilePart — equals / hashCode / compareTo via reflection
  //
  // CacheFilePart is a private static nested class. We reach it via
  // the declaring class's getDeclaredClasses() list and instantiate
  // via the package-private constructor so we can directly assert
  // the documented sort/equality contract.
  // -------------------------------------------------------------------

  private static Object newCacheFilePart(File f, long offset, long length)
      throws Exception
  {
    Class<?>[] inner = TemporaryDataCache.class.getDeclaredClasses();
    Class<?> filePartClass = null;
    for (Class<?> c : inner) {
      if (c.getSimpleName().equals("CacheFilePart")) {
        filePartClass = c;
        break;
      }
    }
    assertNotNull(filePartClass,
                  "CacheFilePart inner class must exist");
    java.lang.reflect.Constructor<?> ctor
        = filePartClass.getDeclaredConstructor(
            File.class, long.class, long.class);
    ctor.setAccessible(true);
    return ctor.newInstance(f, offset, length);
  }

  /**
   * {@code CacheFilePart.equals} must return true for the same
   * instance.
   */
  @Test
  public void cacheFilePartEqualsSameInstance() throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 0L, 100L);
    assertTrue(a.equals(a), "Same-instance equality must hold");
  }

  /**
   * {@code CacheFilePart.equals} must return false for null.
   */
  @Test
  public void cacheFilePartEqualsNullReturnsFalse() throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 0L, 100L);
    assertFalse(a.equals(null));
  }

  /**
   * {@code CacheFilePart.equals} must return false for an instance
   * of a different class.
   */
  @Test
  public void cacheFilePartEqualsDifferentClassReturnsFalse()
      throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 0L, 100L);
    assertFalse(a.equals("not a CacheFilePart"));
  }

  /**
   * Two {@code CacheFilePart} instances with the same offset+length
   * must be equal even if their underlying {@link File} differs
   * (only offset/length participate in equals per the impl).
   */
  @Test
  public void cacheFilePartEqualsByOffsetAndLength() throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 50L, 200L);
    Object b = newCacheFilePart(this.testFiles.get(1), 50L, 200L);
    assertTrue(a.equals(b),
               "Same offset+length must compare equal");
    assertEquals(a.hashCode(), b.hashCode(),
                 "Equal instances must have equal hash codes");
  }

  /**
   * Two {@code CacheFilePart} instances with different offset must
   * not be equal.
   */
  @Test
  public void cacheFilePartEqualsDifferentOffsetReturnsFalse()
      throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 50L, 200L);
    Object b = newCacheFilePart(this.testFiles.get(0), 60L, 200L);
    assertFalse(a.equals(b));
  }

  /**
   * Two {@code CacheFilePart} instances with different length must
   * not be equal.
   */
  @Test
  public void cacheFilePartEqualsDifferentLengthReturnsFalse()
      throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 50L, 200L);
    Object b = newCacheFilePart(this.testFiles.get(0), 50L, 300L);
    assertFalse(a.equals(b));
  }

  /**
   * {@code CacheFilePart.compareTo} must order by offset first, then
   * by length (ascending) — and return 0 for matching offset+length
   * pairs.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void cacheFilePartCompareToOrdersByOffsetThenLength()
      throws Exception
  {
    Object a = newCacheFilePart(this.testFiles.get(0), 50L, 100L);
    Object b = newCacheFilePart(this.testFiles.get(0), 50L, 200L);
    Object c = newCacheFilePart(this.testFiles.get(0), 60L, 100L);
    Object d = newCacheFilePart(this.testFiles.get(0), 50L, 100L);

    Comparable cmpA = (Comparable) a;
    Comparable cmpB = (Comparable) b;
    Comparable cmpC = (Comparable) c;
    Comparable cmpD = (Comparable) d;

    assertEquals(0, cmpA.compareTo(cmpD));
    assertTrue(cmpA.compareTo(cmpB) < 0,
               "Smaller length must sort before larger when offsets"
                   + " match");
    assertTrue(cmpB.compareTo(cmpA) > 0);
    assertTrue(cmpA.compareTo(cmpC) < 0,
               "Smaller offset must sort before larger");
    assertTrue(cmpC.compareTo(cmpA) > 0);
  }
}
