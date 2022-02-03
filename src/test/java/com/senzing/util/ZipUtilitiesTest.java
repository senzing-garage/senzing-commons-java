package com.senzing.util;

import com.senzing.io.IOUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.*;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ZipUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ZipUtilitiesTest {
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

  private static File tempDirectory() throws IOException {
    File tempFile = File.createTempFile("zip-", "-dirname");
    String dirPath = tempFile.getPath();
    dirPath = dirPath.substring(0, dirPath.length() - 4);
    File directory = new File(dirPath);
    directory.mkdirs();
    directory.deleteOnExit();
    return directory;
  }

  private static File createTextFile(File file) throws IOException {
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

  private static File createDirectoryWithFiles() throws IOException {
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
  public void zipDirectoryToFileTest() {
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
  public void zipFileToFileTest() {
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
}

