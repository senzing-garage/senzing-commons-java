package com.senzing.util;


import java.io.*;
import java.util.Base64;
import java.util.zip.*;

import static com.senzing.io.IOUtilities.*;

/**
 * Utilities for working with ZIP files and ZIP-compressed data.
 */
public class ZipUtilities {
  /**
   * Compresses the specified byte array using the deflater zlib compression
   * algorithm.
   *
   * @param uncompressedData The byte array containing the uncompressed data.
   * @return The compressed byte array.
   */
  public static byte[] zip(byte[] uncompressedData) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
        dos.write(uncompressedData, 0, uncompressedData.length);
        dos.finish();
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(
          "IOException when using Byte-Array streams", e);
    }
  }

  /**
   * Decompresses the specified byte array using the inflater zlib compression
   * algorithm.
   *
   * @param compressedData The byte array containing the compressed data.
   * @return The uncompressed byte array.
   */
  public static byte[] unzip(byte[] compressedData) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
      byte[] buffer = new byte[8192];
      try (InflaterInputStream iis = new InflaterInputStream(bais)) {
        for (int readCount = iis.read(buffer);
             readCount >= 0;
             readCount = iis.read(buffer))
        {
          baos.write(buffer, 0, readCount);
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(
          "IOException when using Byte-Array streams", e);
    }

  }

  /**
   * Compresses the specified byte array using the deflater zlib compression
   * algorithm and returns a Base-64 encoded {@link String}.
   *
   * @param uncompressedData The byte array containing the uncompressed data.
   * @return The compressed data encoded as Base-64.
   */
  public static String zip64(byte[] uncompressedData) {
    try {
      byte[] data = zip(uncompressedData);
      return new String(Base64.getEncoder().encode(data), UTF_8);

    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }
  }

  /**
   * Decompresses the specified Base-64 encoded {@link String} of compressed
   * data using the inflater zlib compression algorithm.
   *
   * @param compressedBase64Data The base-64 encoded {@link String} containing
   *                             the compressed data.
   * @return The uncompressed byte array.
   */
  public static byte[] unzip64(String compressedBase64Data) {
    byte[] compressedData = Base64.getDecoder().decode(compressedBase64Data);
    return unzip(compressedData);
  }

  /**
   * Compresses the specified text by converting it to UTF-8 bytes, compressing
   * it with the deflater zlib compression algorithm and encoding the result
   * in Base-64 encoding.
   *
   * @param uncompressedText The byte array containing the uncompressed data.
   * @return The compressed byte array.
   */
  public static String zipText64(String uncompressedText) {
    try {
      return zip64(uncompressedText.getBytes(UTF_8));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }

  }

  /**
   * Decompresses the specified Base-64 encoded {@link String} of compressed
   * data using the inflater zlib compression algorithm and returns the {@link
   * String} built with the uncompressed data and UTF-8 encoding.
   *
   * @param compressedBase64Data The base-64 encoded {@link String} containing
   *                             the compressed data.
   * @return The uncompressed text reconstructed from the bytes using the
   *         UTF-8 encoding.
   */
  public static String unzipText64(String compressedBase64Data) {
    try {
      byte[] data = unzip64(compressedBase64Data);
      return new String(data, UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }
  }

  /**
   * Compresses the specified byte array using the GZip compression algorithm.
   *
   * @param uncompressedData The byte array containing the uncompressed data.
   * @return The compressed byte array.
   */
  public static byte[] gzip(byte[] uncompressedData) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
        gzos.write(uncompressedData, 0, uncompressedData.length);
        gzos.finish();
      }
      return baos.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException(
          "IOException when using Byte-Array streams", e);
    }
  }

  /**
   * Decompresses the specified byte array using the GZip compression algorithm.
   *
   * @param compressedData The byte array containing the compressed data.
   * @return The uncompressed byte array.
   */
  public static byte[] gunzip(byte[] compressedData) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
      byte[] buffer = new byte[8192];
      try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
        for (int readCount = gzis.read(buffer);
             readCount >= 0;
             readCount = gzis.read(buffer)) {
          baos.write(buffer, 0, readCount);
        }
      }
      return baos.toByteArray();

    } catch (IOException e) {
      throw new IllegalStateException(
          "IOException when using Byte-Array streams", e);
    }
  }

  /**
   * Compresses the specified byte array using the GZip compression algorithm
   * and returns a Base-64 encoded {@link String}.
   *
   * @param uncompressedData The byte array containing the uncompressed data.
   * @return The compressed data encoded as Base-64.
   */
  public static String gzip64(byte[] uncompressedData) {
    try {
      byte[] data = gzip(uncompressedData);
      return new String(Base64.getEncoder().encode(data), UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }
  }

  /**
   * Decompresses the specified Base-64 encoded {@link String} of compressed
   * data using the GZip compression algorithm.
   *
   * @param compressedBase64Data The base-64 encoded {@link String} containing
   *                             the compressed data.
   * @return The uncompressed byte array.
   */
  public static byte[] gunzip64(String compressedBase64Data) {
    byte[] compressedData = Base64.getDecoder().decode(compressedBase64Data);
    return gunzip(compressedData);
  }

  /**
   * Compresses the specified text by converting it to UTF-8 bytes, compressing
   * it with the GZip compression algorithm and encoding the result in Base-64
   * encoding.
   *
   * @param uncompressedText The byte array containing the uncompressed data.
   * @return The compressed byte array.
   */
  public static String gzipText64(String uncompressedText) {
    try {
      return gzip64(uncompressedText.getBytes(UTF_8));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }
  }

  /**
   * Decompresses the specified Base-64 encoded {@link String} of compressed
   * data using the GZip compression algorithm and returns the {@link String}
   * built with the uncompressed data and UTF-8 encoding.
   *
   * @param compressedBase64Data The base-64 encoded {@link String} containing
   *                             the compressed data.
   * @return The uncompressed text reconstructed from the bytes using the
   *         UTF-8 encoding.
   */
  public static String gunzipText64(String compressedBase64Data) {
    try {
      byte[] data = gunzip64(compressedBase64Data);
      return new String(data, UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(
          "UTF-8 encoding not recognized", e);
    }
  }

  /**
   * Creates a ZIP archive of the specified directory or file.
   *
   * @param directoryOrFile THe directory to be zipped.
   *
   * @param targetZipFile The ZIP file to be created.
   *
   * @throws IOException If a failure occurs.
   */
  public static void zip(File directoryOrFile, File targetZipFile)
    throws IOException
  {
    try (FileOutputStream     fos = new FileOutputStream(targetZipFile);
         BufferedOutputStream bos = new BufferedOutputStream(fos);
         ZipOutputStream      zos = new ZipOutputStream(bos, UTF_8_CHARSET))
    {
      zip(directoryOrFile, zos);
      zos.finish();
    }
  }

  /**
   * Creates a ZIP archive of the specified directory or file.
   *
   * @param directoryOrFile THe directory to be zipped.
   *
   * @param targetZipStream The ZIP file to be created.
   *
   * @throws IOException If a failure occurs.
   */
  public static void zip(File directoryOrFile, ZipOutputStream targetZipStream)
      throws IOException {
    String basePath = directoryOrFile.getParentFile().getPath();
    doZip(basePath, directoryOrFile, targetZipStream);
  }

  /**
   * Recursively creates the ZIP file.
   */
  private static void doZip(String          basePath,
                            File            directoryOrFile,
                            ZipOutputStream targetZip)
    throws IOException
  {
    // get the name sans the base path
    String name = directoryOrFile.getPath().substring(basePath.length());
    if (name.startsWith(File.separator)) {
      name = name.substring(1);
    }

    // make sure the we use a forward separator for the file separator
    if (File.separatorChar != '/') {
      name.replace(File.separatorChar, '/');
    }

    // check if we have a directory
    if (directoryOrFile.isDirectory()) {
      String dirName = name;
      if (!dirName.endsWith("/")) dirName = dirName + "/";
      ZipEntry zipEntry = new ZipEntry(dirName);
      targetZip.putNextEntry(zipEntry);
      targetZip.closeEntry();
      File[] children = directoryOrFile.listFiles();
      for (File child : children) {
        doZip(basePath,  child, targetZip);
      }

    } else {
      ZipEntry zipEntry = new ZipEntry(name);
      targetZip.putNextEntry(zipEntry);
      try (FileInputStream fis = new FileInputStream(directoryOrFile)) {
        byte[] bytes = new byte[8192];
        for (int count = fis.read(bytes); count >= 0; count = fis.read(bytes)) {
          targetZip.write(bytes, 0, count);
        }
      }
      targetZip.closeEntry();
    }
  }

  /**
   * Unzips a ZIP archive and places in the contents in the specified
   * directory.
   *
   * @param zipFile The ZIP file to extract.
   * @param targetDirectory The directory to extract the archive to.
   *
   * @throws IOException If a failure occurs.
   */
  public static void unzip(File zipFile, File targetDirectory)
    throws IOException
  {
    try (FileInputStream  fis = new FileInputStream(zipFile);
         ZipInputStream   zis = new ZipInputStream(fis, UTF_8_CHARSET))
    {
      unzip(zis, targetDirectory);
    }
  }

  /**
   * Unzips a ZIP archive and places in the contents in the specified
   * directory.
   *
   * @param zipStream The ZIP file to extract.
   * @param targetDirectory The directory to extract the archive to.
   *
   * @throws IOException If a failure occurs.
   */
  public static void unzip(ZipInputStream zipStream, File targetDirectory)
      throws IOException
  {
    byte[] buffer = new byte[8192];
    for (ZipEntry zipEntry = zipStream.getNextEntry();
         zipEntry != null;
         zipEntry = zipStream.getNextEntry())
    {
      String  name = zipEntry.getName();
      long    size = zipEntry.getSize();
      boolean directory = (size <= 0L && name.endsWith("/"));
      if (File.separatorChar != '/') {
        name = name.replace('/', File.separatorChar);
      }
      File targetFile = new File(targetDirectory, name);
      if (directory) {
        targetFile.mkdirs();
      } else {
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
          for (int count = zipStream.read(buffer);
               count >= 0;
               count = zipStream.read(buffer))
          {
            fos.write(buffer, 0, count);
          }
        }
      }
    }
  }

  /**
   * Manual test function.
   *
   * @param args The command-line arguments.
   */
  public static void main(String[] args) {
    try {
      File source = null;
      File target = null;
      if (args.length == 2) {
        source = new File(args[0]);
        target = new File(args[1]);
        if (!source.exists()) source = null;
      }
      if (source == null || target == null) {
        for (int index = 0; index < args.length; index++) {
          String arg = args[index];
          String compressed = zipText64(arg);
          String uncompressed = unzipText64(compressed);
          System.out.println();
          System.out.println(index + " (zip): From " + arg.length() + " to "
                                 + compressed.length() + " --> "
                                 + arg.equals(uncompressed));

          compressed = gzipText64(arg);
          uncompressed = gunzipText64(compressed);
          System.out.println(index + " (gzip): From " + arg.length() + " to "
                                 + compressed.length() + " --> "
                                 + arg.equals(uncompressed));

        }
      } else {
        boolean sourceZip = (source.getName().toLowerCase().endsWith(".zip")
            && !source.isDirectory());
        boolean targetZip = (target.getName().toLowerCase().endsWith(".zip")
            && !target.isDirectory());

        // check if extracting
        if (target.exists() && target.isDirectory() && sourceZip) {
          unzip(source, target);
        } else if (targetZip) {
          zip(source, target);
        } else {
          System.err.println("At least one argument must be a ZIP file");
          System.exit(1);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

}
