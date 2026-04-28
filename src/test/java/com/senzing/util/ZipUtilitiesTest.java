package com.senzing.util;

import com.senzing.io.IOUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ZipUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ZipUtilitiesTest
{
  private static final SecureRandom PRNG = new SecureRandom();

  private static final String[] SENTENCES = {
      "The quick brown fox jumped over the lazy dog.",
      "Old Mother Hubbard went to the cupboard to give her poor dog a bone.",
      "When she came there the cupboard was bare and so the poor dog had none.",
      "Twinkle, twinkle little star, how I wonder what you are.",
      "Up above the world so high, like a diamond in the sky.",
      "Sing a song of six pence, a pocket full of rye.",
      "Hey, diddle, diddle, the cat and the fiddle, the cow jumped over the moon.",
      "The little dog laughed to see such fun and the dish ran away with the spoon.",
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
      "Aenean at eros diam. Donec sodales feugiat elit sed dapibus. Fusce iaculis quis tortor tincidunt dignissim. Cras vestibulum nibh non mi vulputate condimentum. Sed quis ornare sapien.",
  };

  private static File tempDirectory()
      throws IOException
  {
    File tempFile = File.createTempFile("zip-", "-dirname");
    String dirPath = tempFile.getPath();
    dirPath = dirPath.substring(0, dirPath.length() - 4);
    File directory = new File(dirPath);
    directory.mkdirs();
    directory.deleteOnExit();
    return directory;
  }

  private static File createTextFile(File file)
      throws IOException
  {
    int fileSize = 100000 + PRNG.nextInt(100000);
    int writeCount = 0;
    try (FileOutputStream   fos = new FileOutputStream(file);
         OutputStreamWriter osw = new OutputStreamWriter(fos);
         PrintWriter        pw  = new PrintWriter(osw))
    {
      while (writeCount < fileSize) {
        int nextSentence = PRNG.nextInt(11);
        String sentence = null;
        if (nextSentence >= 0 && nextSentence < SENTENCES.length) {
          sentence = SENTENCES[nextSentence];
        } else {
          int sentenceLength = 50 + PRNG.nextInt(200);
          StringBuilder sb = new StringBuilder();
          for (int index2 = 0; index2 < sentenceLength; index2++) {
            int charCode = 32 + PRNG.nextInt(91);
            sb.append((char) charCode);
          }
          sentence = sb.toString();
        }
        pw.println(sentence);
        writeCount += sentence.length();
      }
    }
    return file;
  }

  private static File createDirectoryWithFiles()
      throws IOException
  {
    File directory = tempDirectory();

    for (int index = 0; index < 100; index++) {
      File file = new File(directory, "file-" + index + ".txt");
      file.deleteOnExit();
      createTextFile(file);
    }
    directory.deleteOnExit();
    return directory;
  }

  @Test
  public void zipDirectoryToFileTest()
  {
    File dir = null;
    File targetFile = null;
    File targetDir = null;

    try {
      dir = createDirectoryWithFiles();

      File[] origFiles = dir.listFiles();
      long totalSize = 0L;
      for (File file: origFiles) {
        totalSize += file.length();
      }

      targetFile = File.createTempFile("test-", ".zip");
      targetFile.deleteOnExit();

      ZipUtilities.zip(dir, targetFile);

      long zipLength = targetFile.length();

      assertTrue(targetFile.exists(),
                 "ZIP file does not exist: " + targetFile);
      assertNotEquals(0, zipLength,
                      "ZIP file appears to be empty: " + targetFile);

      assertTrue(zipLength < totalSize,
                 "The ZIP file (" + targetFile + ") is larger ("
                     + zipLength + ") than the original files: " + totalSize);

      targetDir = tempDirectory();

      ZipUtilities.unzip(targetFile, targetDir);

      File[] files = targetDir.listFiles();
      assertEquals(1, files.length,
                   "Target directory contained an unexpected number "
                   + "of files.");
      assertTrue(files[0].isDirectory(),
                 "Target directory does not contain a directory");
      assertEquals(dir.getName(), files[0].getName(),
      "Unzipped directory name differs from original directory name.");

      files = files[0].listFiles();

      assertEquals(origFiles.length, files.length,
                   "Unzipped a different number of files than "
                       + "originally existed: " + targetDir
                       + "\n\n" + Arrays.asList(origFiles)
                       + "\n\n" + Arrays.asList(files));

      Map<String, File> origFileMap = new LinkedHashMap<>();
      Map<String, File> fileMap     = new LinkedHashMap<>();

      for (File file: origFiles) {
        origFileMap.put(file.getName(), file);
      }
      for (File file: files) {
        fileMap.put(file.getName(), file);
      }

      assertEquals(origFileMap.keySet(), fileMap.keySet(),
          "Files unzipped with different names.");

      fileMap.forEach((name, file) -> {
        File origFile = origFileMap.get(name);
        try {
          assertFalse(IOUtilities.checkFilesDiffer(file, origFile),
                      "Unzipped file (" + file
                          + ") differs from original (" + origFile + ")");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed with an unexpected exception: " + e);

    } finally {
      if (dir != null) {
        try {
          IOUtilities.recursiveDeleteDirectory(dir);
        } catch (IOException e) {
          // ignore
        }
      }
      if (targetDir != null) {
        try {
          IOUtilities.recursiveDeleteDirectory(targetDir);
        } catch (IOException e) {
          // ignore
        }
      }
      if (targetFile != null) {
        targetFile.delete();
      }
    }
  }

  @Test
  public void zipFileToFileTest()
  {
    File file = null;
    File targetFile = null;
    File targetDir = null;

    try {
      file = File.createTempFile("test-zip-", ".txt");
      createTextFile(file);

      long totalSize = file.length();

      targetFile = File.createTempFile("test-", ".zip");
      targetFile.deleteOnExit();

      ZipUtilities.zip(file, targetFile);

      long zipLength = targetFile.length();

      assertTrue(targetFile.exists(),
                 "ZIP file does not exist: " + targetFile);
      assertNotEquals(0, zipLength,
                      "ZIP file appears to be empty: " + targetFile);

      assertTrue(zipLength < totalSize,
                 "The ZIP file (" + targetFile + ") is larger ("
                     + zipLength + ") than the original file: " + totalSize);

      targetDir = tempDirectory();

      ZipUtilities.unzip(targetFile, targetDir);

      File[] files = targetDir.listFiles();
      assertEquals(1, files.length,
                   "Target directory contained an unexpected number "
                       + "of files.");
      assertFalse(files[0].isDirectory(),
                 "Target directory should not contain a directory");
      assertEquals(file.getName(), files[0].getName(),
                   "Unzipped file name differs from original file name.");

      assertFalse(IOUtilities.checkFilesDiffer(file, files[0]),
                  "Unzipped file (" + files[0]
                      + ") differs from original (" + file + ")");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed with an unexpected exception: " + e);

    } finally {
      if (file != null) {
        file.delete();
      }
      if (targetDir != null) {
        try {
          IOUtilities.recursiveDeleteDirectory(targetDir);
        } catch (IOException e) {
          // ignore
        }
      }
      if (targetFile != null) {
        targetFile.delete();
      }
    }
  }

  // -------------------------------------------------------------------
  // Byte-array compression helpers (zip / unzip)
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#zip(byte[])} followed by
   * {@link ZipUtilities#unzip(byte[])} must produce a byte-for-byte
   * identical result (round-trip identity per the deflate / inflate
   * contract).
   */
  @Test
  public void zipUnzipByteArrayRoundTrip()
  {
    byte[] original = repeatedTextBytes();
    byte[] compressed = ZipUtilities.zip(original);
    byte[] back = ZipUtilities.unzip(compressed);
    assertArrayEquals(original, back,
                      "zip/unzip must round-trip byte-for-byte");
  }

  /**
   * For non-empty input, {@link ZipUtilities#zip(byte[])} must
   * produce a non-zero-length output (it includes the deflate
   * header/footer at minimum).
   */
  @Test
  public void zipProducesNonEmptyOutputForNonEmptyInput()
  {
    byte[] original = "hello world".getBytes(StandardCharsets.UTF_8);
    byte[] compressed = ZipUtilities.zip(original);
    assertTrue(compressed.length > 0,
               "Compressed output must be non-empty");
  }

  /**
   * Empty input to {@link ZipUtilities#zip(byte[])} must produce a
   * deflate output that round-trips back to an empty byte array.
   */
  @Test
  public void zipUnzipByteArrayEmptyInputRoundTrips()
  {
    byte[] empty = new byte[0];
    byte[] compressed = ZipUtilities.zip(empty);
    byte[] back = ZipUtilities.unzip(compressed);
    assertArrayEquals(empty, back);
  }

  // -------------------------------------------------------------------
  // Base-64 deflate helpers (zip64 / unzip64)
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#zip64(byte[])} must produce a valid Base-64
   * string whose decoded bytes equal {@link ZipUtilities#zip(byte[])}.
   */
  @Test
  public void zip64ProducesValidBase64()
  {
    byte[] original = repeatedTextBytes();
    String base64 = ZipUtilities.zip64(original);
    byte[] decoded = Base64.getDecoder().decode(base64);
    byte[] zipped  = ZipUtilities.zip(original);
    assertArrayEquals(zipped, decoded,
                      "zip64 must equal Base64-encode(zip(...))");
  }

  /**
   * {@link ZipUtilities#zip64(byte[])} +
   * {@link ZipUtilities#unzip64(String)} must round-trip.
   */
  @Test
  public void zip64Unzip64RoundTrip()
  {
    byte[] original = repeatedTextBytes();
    String base64 = ZipUtilities.zip64(original);
    byte[] back = ZipUtilities.unzip64(base64);
    assertArrayEquals(original, back);
  }

  // -------------------------------------------------------------------
  // Text Base-64 helpers (zipText64 / unzipText64)
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#zipText64(String)} +
   * {@link ZipUtilities#unzipText64(String)} must round-trip a UTF-8
   * text payload (including non-ASCII codepoints).
   */
  @Test
  public void zipText64UnzipText64RoundTripWithUtf8()
  {
    String original = "Hello, 世界! Привет! 🚀 "
        + "The quick brown fox jumps over the lazy dog.";
    String base64 = ZipUtilities.zipText64(original);
    String back   = ZipUtilities.unzipText64(base64);
    assertEquals(original, back);
  }

  /**
   * {@link ZipUtilities#zipText64(String)} on the empty string must
   * round-trip back to the empty string.
   */
  @Test
  public void zipText64UnzipText64RoundTripWithEmptyString()
  {
    String base64 = ZipUtilities.zipText64("");
    assertEquals("", ZipUtilities.unzipText64(base64));
  }

  // -------------------------------------------------------------------
  // GZip helpers (gzip / gunzip)
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#gzip(byte[])} +
   * {@link ZipUtilities#gunzip(byte[])} must round-trip.
   */
  @Test
  public void gzipGunzipByteArrayRoundTrip()
  {
    byte[] original = repeatedTextBytes();
    byte[] compressed = ZipUtilities.gzip(original);
    byte[] back = ZipUtilities.gunzip(compressed);
    assertArrayEquals(original, back);
  }

  /**
   * GZip output must include the standard GZip magic bytes
   * ({@code 0x1F 0x8B}) at the start of the byte stream.
   */
  @Test
  public void gzipOutputIncludesMagicHeader()
  {
    byte[] original = "hello".getBytes(StandardCharsets.UTF_8);
    byte[] compressed = ZipUtilities.gzip(original);
    assertTrue(compressed.length >= 2,
               "Compressed output too short for GZip header");
    assertEquals((byte) 0x1F, compressed[0],
                 "GZip magic byte 0 must be 0x1F");
    assertEquals((byte) 0x8B, compressed[1],
                 "GZip magic byte 1 must be 0x8B");
  }

  // -------------------------------------------------------------------
  // GZip + Base-64 helpers
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#gzip64(byte[])} +
   * {@link ZipUtilities#gunzip64(String)} must round-trip.
   */
  @Test
  public void gzip64Gunzip64RoundTrip()
  {
    byte[] original = repeatedTextBytes();
    String base64 = ZipUtilities.gzip64(original);
    byte[] back = ZipUtilities.gunzip64(base64);
    assertArrayEquals(original, back);
  }

  /**
   * {@link ZipUtilities#gzipText64(String)} +
   * {@link ZipUtilities#gunzipText64(String)} must round-trip a UTF-8
   * text payload.
   */
  @Test
  public void gzipText64GunzipText64RoundTrip()
  {
    String original = "Lorem ipsum dolor sit amet, "
        + "consectetur adipiscing elit. 中文测试 🦊";
    String base64 = ZipUtilities.gzipText64(original);
    assertEquals(original, ZipUtilities.gunzipText64(base64));
  }

  // -------------------------------------------------------------------
  // Stream-based archive variants
  // -------------------------------------------------------------------

  /**
   * {@link ZipUtilities#zip(File, ZipOutputStream)} must write to
   * the supplied stream a ZIP archive that
   * {@link ZipUtilities#unzip(ZipInputStream, File)} can extract
   * round-trip identical.
   */
  @Test
  public void zipUnzipStreamVariantsRoundTrip() throws IOException
  {
    // Create a small file to zip.
    File source = File.createTempFile("zip-stream-", ".txt");
    source.deleteOnExit();
    Files.writeString(source.toPath(),
                      "stream-variant round-trip payload\n");

    // Zip into a byte array via the stream variant.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      ZipUtilities.zip(source, zos);
      zos.finish();
    }
    byte[] zipBytes = baos.toByteArray();
    assertTrue(zipBytes.length > 0, "Zip output must be non-empty");

    // Unzip into a fresh temp directory via the stream variant.
    File extractDir = tempDirectory();
    try {
      try (ZipInputStream zis = new ZipInputStream(
          new ByteArrayInputStream(zipBytes))) {
        ZipUtilities.unzip(zis, extractDir);
      }

      File extracted = new File(extractDir, source.getName());
      assertTrue(extracted.exists(),
                 "Extracted file must exist: " + extracted);
      assertEquals(Files.readString(source.toPath()),
                   Files.readString(extracted.toPath()),
                   "Extracted content must match the source");
    } finally {
      IOUtilities.recursiveDeleteDirectory(extractDir);
      source.delete();
    }
  }

  // -------------------------------------------------------------------
  // main(String[])
  // -------------------------------------------------------------------

  /**
   * {@code ZipUtilities.main} with one or more text arguments (none
   * of which are existing files) must run the deflate/gzip
   * round-trip diagnostics and report success on stdout without
   * throwing or calling {@link System#exit}.
   */
  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  public void mainWithTextArgsPrintsRoundTripDiagnostics()
  {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      ZipUtilities.main(new String[] { "hello world" });
      String out = captured.toString(StandardCharsets.UTF_8);
      assertTrue(out.contains("(zip):"),
                 "main output must include zip diagnostic: " + out);
      assertTrue(out.contains("(gzip):"),
                 "main output must include gzip diagnostic: " + out);
      assertTrue(out.contains("--> true"),
                 "main output must show successful round-trip: " + out);
    } finally {
      System.setOut(originalOut);
    }
  }

  /**
   * {@code ZipUtilities.main} with two arguments where the first is
   * an existing directory and the second is a {@code .zip} target
   * path must produce a valid ZIP file at the target location.
   */
  @Test
  public void mainWithDirAndZipTargetCreatesArchive() throws Exception
  {
    File dir = createDirectoryWithFiles();
    File targetZip = File.createTempFile("main-", ".zip");
    targetZip.delete();
    try {
      ZipUtilities.main(new String[] {
          dir.getAbsolutePath(), targetZip.getAbsolutePath()
      });
      assertTrue(targetZip.exists(),
                 "main must create the target ZIP file: "
                     + targetZip);
      assertTrue(targetZip.length() > 0,
                 "main-created ZIP file must be non-empty");
    } finally {
      IOUtilities.recursiveDeleteDirectory(dir);
      targetZip.delete();
    }
  }

  /**
   * {@code ZipUtilities.main} with two arguments where the first
   * is an existing {@code .zip} file and the second is an existing
   * directory must extract the archive into the directory (the
   * unzip branch of main's args==2 logic).
   */
  @Test
  public void mainWithZipSourceAndDirTargetExtractsArchive()
      throws Exception
  {
    // First produce a ZIP archive of a small directory.
    File sourceDir = createDirectoryWithFiles();
    File zipFile = File.createTempFile("main-extract-", ".zip");
    zipFile.delete();
    File extractDir = tempDirectory();
    try {
      ZipUtilities.zip(sourceDir, zipFile);
      assertTrue(zipFile.exists());

      // Run main(zip, dir) — should call unzip and populate the
      // directory.
      ZipUtilities.main(new String[] {
          zipFile.getAbsolutePath(), extractDir.getAbsolutePath()
      });

      File[] extracted = extractDir.listFiles();
      assertNotNull(extracted,
                    "Extract directory must contain entries");
      assertEquals(1, extracted.length,
                   "Extracted root must contain exactly one entry"
                       + " (the source dir)");
      assertTrue(extracted[0].isDirectory(),
                 "Extracted entry must be a directory");
    } finally {
      IOUtilities.recursiveDeleteDirectory(sourceDir);
      IOUtilities.recursiveDeleteDirectory(extractDir);
      zipFile.delete();
    }
  }

  /**
   * {@code ZipUtilities.main} with multiple text arguments must
   * iterate through each, printing both the zip and gzip
   * round-trip diagnostics for every argument.
   */
  @Test
  @Execution(ExecutionMode.SAME_THREAD)
  public void mainWithMultipleTextArgsIteratesAllArguments()
  {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setOut(new PrintStream(captured, true,
                                  StandardCharsets.UTF_8));
    try {
      ZipUtilities.main(new String[] {
          "first arg", "second arg", "third arg"
      });
      String out = captured.toString(StandardCharsets.UTF_8);
      // For each of the 3 args, expect one zip line and one gzip
      // line — so 6 diagnostic lines total.
      int zipCount  = countOccurrences(out, "(zip):");
      int gzipCount = countOccurrences(out, "(gzip):");
      assertEquals(3, zipCount,
                   "Expected one zip diagnostic per argument: " + out);
      assertEquals(3, gzipCount,
                   "Expected one gzip diagnostic per argument: " + out);
    } finally {
      System.setOut(originalOut);
    }
  }

  private static int countOccurrences(String haystack, String needle)
  {
    int count = 0;
    int from = 0;
    while ((from = haystack.indexOf(needle, from)) >= 0) {
      count++;
      from += needle.length();
    }
    return count;
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  /**
   * Returns a non-trivial byte payload that compresses well — repeats
   * one of the test sentences enough times that compression provides
   * a meaningful size reduction in the assertions above.
   */
  private static byte[] repeatedTextBytes()
  {
    StringBuilder sb = new StringBuilder();
    String sentence = SENTENCES[0];
    for (int i = 0; i < 200; i++) {
      sb.append(sentence).append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }
}

