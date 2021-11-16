package com.senzing.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.Arrays;

import static com.senzing.text.TextUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChunkedEncodingInputStream}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ChunkedEncodingInputStreamTest {
  private static final SecureRandom PRNG = new SecureRandom();

  private static byte[] encodeChunks(String text) {
    try {
      byte[] CRLF = "\r\n".getBytes("ASCII");
      byte[] data = text.getBytes("UTF-8");
      ByteArrayInputStream  bais = new ByteArrayInputStream(data);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 2);

      int totalLength   = data.length;
      int minChunkSize  = totalLength / 5;
      int remaining     = totalLength;
      while (remaining > 0) {
        int count = Math.min((PRNG.nextInt(minChunkSize) + 2) * 2, remaining);
        String hexSize  = Integer.toString(count, 16).toUpperCase();
        byte[] buffer   = bais.readNBytes(count);
        baos.write(hexSize.getBytes("ASCII"));
        baos.write(CRLF);
        baos.write(buffer);
        baos.write(CRLF);
        remaining -= count;
      }
      baos.write("0".getBytes("ASCII"));
      baos.write(CRLF);
      baos.write(CRLF);
      return baos.toByteArray();

    } catch (UnsupportedEncodingException cannotHappen) {
      throw new IllegalStateException("UTF-8 encoding is not supported.");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void chunkedEncodingAvailableTest() {
    int totalAvailable = 0;
    try {
      String text   = randomPrintableText(8192);
      byte[] bytes  = text.getBytes("UTF-8");

      byte[] chunks = encodeChunks(text);

      ByteArrayInputStream       bais = new ByteArrayInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(bais);

      int available = 0;
      do {
        available = ceis.available();
        assertTrue(available >= 0,
                   "Available bytes less-than zero: " + available);
        assertTrue(available < bytes.length,
                   "More bytes available than should be ("
                       + bytes.length + "): " + available);

        totalAvailable += available;

        if (available > 0) {
          ceis.readNBytes(available);
        }

      } while (available > 0);

      assertEquals(bytes.length, totalAvailable,
                   "Total available bytes on chunked input stream not "
                   + "as expected.");

    } catch (Exception e) {
      System.out.println("TOTAL AVAILABLE: " + totalAvailable);
      e.printStackTrace();
      fail("chunkedEncodingAvailableTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingCloseTest() {
    try {
      String text   = "Does not matter";
      byte[] chunks = encodeChunks(text);

      TestInputStream            tis  = new TestInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(tis);

      assertFalse(tis.isClosed(), "Pre-condition false, test stream closed.");
      ceis.close();
      assertTrue(tis.isClosed(), "Test stream not closed via chunked stream");

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingCloseTest() failed with exception: " + e);
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

  @Test
  public void chunkedEncodingMarkTest() {
    try {
      String text   = "Does not matter";
      byte[] chunks = encodeChunks(text);

      TestInputStream            tis  = new TestInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(tis);

      ceis.mark(10);
      fail("ChunkedEncodingInputStream.mark() succeeded unexpectedly without "
           + "UnsupportedOperationException.");

    } catch (UnsupportedOperationException expected) {
      // expected -- success

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingMarkTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingMarkSupportedTest() {
    try {
      String text   = "Does not matter";
      byte[] chunks = encodeChunks(text);

      TestInputStream            tis  = new TestInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(tis);

      assertFalse(ceis.markSupported(),
                  "Mark is unexpectedly supported for "
                  + "ChunkedEncodingInputStream.");

    } catch (Exception e) {
      fail("chunkedEncodingMarkSupportedTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingReadTest() {
    try {
      String text   = randomPrintableText(8192);
      byte[] bytes  = text.getBytes("UTF-8");

      byte[] chunks = encodeChunks(text);

      ByteArrayInputStream       bais = new ByteArrayInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(bais);
      ByteArrayOutputStream      baos = new ByteArrayOutputStream(bytes.length);

      for (int readByte = ceis.read(); readByte >= 0; readByte = ceis.read()) {
        baos.write((byte) readByte);
      }

      byte[] result = baos.toByteArray();
      assertEquals(bytes.length, result.length,
                   "Unchunked data length does not match pre-chunked.");
      assertTrue(Arrays.equals(bytes, result),
                 "Unchunked data does not match pre-chunked data.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingReadTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingReadBufferTest() {
    try {
      String text   = randomPrintableText(8192);
      byte[] bytes  = text.getBytes("UTF-8");

      byte[] chunks = encodeChunks(text);

      ByteArrayInputStream       bais = new ByteArrayInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(bais);
      ByteArrayOutputStream      baos = new ByteArrayOutputStream(bytes.length);

      byte[] buffer = new byte[100];
      for (int readCount = ceis.read(buffer, 0, buffer.length);
           readCount >= 0;
           readCount = ceis.read(buffer, 0, buffer.length))
      {
        baos.write(buffer, 0, readCount);
      }

      byte[] result = baos.toByteArray();
      assertEquals(bytes.length, result.length,
                   "Unchunked data length does not match pre-chunked.");
      assertTrue(Arrays.equals(bytes, result),
                 "Unchunked data does not match pre-chunked data.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingReadBufferTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingResetTest() {
    try {
      String text   = "Does not matter";
      byte[] chunks = encodeChunks(text);

      TestInputStream            tis  = new TestInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(tis);

      ceis.mark(10);
      fail("ChunkedEncodingInputStream.reset() succeeded unexpectedly without "
               + "UnsupportedOperationException.");

    } catch (UnsupportedOperationException expected) {
      // expected -- success

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingResetTest() failed with exception: " + e);
    }
  }

  @Test
  public void chunkedEncodingSkipTest() {
    try {
      String text   = randomPrintableText(8192);
      byte[] bytes  = text.getBytes("UTF-8");

      byte[] chunks = encodeChunks(text);

      ByteArrayInputStream       bais = new ByteArrayInputStream(chunks);
      ChunkedEncodingInputStream ceis = new ChunkedEncodingInputStream(bais);
      ByteArrayOutputStream      baos = new ByteArrayOutputStream(bytes.length);

      // skip 100 bytes
      long result = ceis.skip(100L);

      assertEquals(100L, result,
                   "The number of returned skipped bytes do not match.");

      byte[] buffer = new byte[100];
      for (int readCount = ceis.read(buffer, 0, buffer.length);
           readCount >= 0;
           readCount = ceis.read(buffer, 0, buffer.length))
      {
        baos.write(buffer, 0, readCount);
      }

      byte[] readBytes = baos.toByteArray();
      assertEquals(bytes.length - 100, readBytes.length,
                   "Skipped unchunked data length does not match "
                   + "pre-chunked.");

      byte[] copyArray = Arrays.copyOfRange(bytes, 100, bytes.length);

      assertTrue(Arrays.equals(copyArray, readBytes),
                 "Skipped unchunked data does not match pre-chunked data.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("chunkedEncodingSkipTest() failed with exception: " + e);
    }
  }

}
