package com.senzing.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link ChunkedEncodingInputStream} tests covering the
 * malformed-input error paths and the {@code skip} / {@code reset} entry points
 * not in {@code ChunkedEncodingInputStreamTest}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ChunkedEncodingInputStreamExtraTest
{
  private static ChunkedEncodingInputStream wrap(String chunked)
  {
    return new ChunkedEncodingInputStream(
        new ByteArrayInputStream(
            chunked.getBytes(StandardCharsets.US_ASCII)));
  }

  /**
   * Per the documented contract, {@code reset()} always throws
   * {@link UnsupportedOperationException} regardless of stream state.
   */
  @Test
  public void resetAlwaysThrows()
  {
    ChunkedEncodingInputStream s = wrap("0\r\n");
    assertThrows(UnsupportedOperationException.class, () -> s.reset());
  }

  /**
   * {@link ChunkedEncodingInputStream#skip(long)} must advance past
   * the requested number of bytes when reading from a chunked stream.
   */
  @Test
  public void skipAdvancesPastRequestedBytes() throws IOException
  {
    // Single 8-byte chunk "ABCDEFGH", terminated by zero-length chunk
    String chunked = "8\r\nABCDEFGH\r\n0\r\n\r\n";
    ChunkedEncodingInputStream s = wrap(chunked);

    long skipped = s.skip(4);
    assertEquals(4L, skipped, "skip should return bytes actually skipped");

    // Read the next byte — should be 'E', the byte after the
    // 4-byte skip.
    int next = s.read();
    assertEquals((int) 'E', next,
                 "Read after skip(4) should return the 5th byte");
  }

  // -------------------------------------------------------------------
  // Malformed inputs
  // -------------------------------------------------------------------

  /**
   * A bare line-feed (not preceded by carriage return) inside the chunk-size
   * header must trigger an {@link IOException} per the documented "LF without
   * preceding CR" branch.
   */
  @Test
  public void lfWithoutCrInChunkHeaderThrows()
  {
    // Bare LF after chunk-size — no CR. The chunk-line reader scans
    // until CR-LF, encountering LF-without-CR triggers the error.
    String malformed = "8\nABCDEFGH";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class, s::read);
    assertTrue(ex.getMessage().contains("LF"),
               "Error message should mention LF: " + ex.getMessage());
  }

  /**
   * A carriage-return not followed by line-feed inside the chunk header must
   * trigger an {@link IOException} per the documented "CR without subsequent
   * LF" branch.
   */
  @Test
  public void crWithoutLfInChunkHeaderThrows()
  {
    // CR followed by something other than LF.
    // cspell:disable
    String malformed = "8\rXABCDEFGH";
    // cspell:enable
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class, s::read);
    assertTrue(ex.getMessage().contains("CR"),
               "Error message should mention CR: " + ex.getMessage());
  }

  /**
   * EOF before any CR is encountered while reading the chunk-size header must
   * trigger the documented "Unexpected EOF" branch.
   */
  @Test
  public void eofBeforeChunkSizeTerminationThrows()
  {
    // Stream ends mid-chunk-size-header, no CR or LF.
    String malformed = "8";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class, s::read);
    assertTrue(ex.getMessage().contains("Unexpected EOF"),
               "Error message should mention Unexpected EOF: "
                   + ex.getMessage());
  }

  /**
   * Chunk size with a chunk-extension (separated by ';') must be parsed
   * correctly — the size before the semicolon is what counts.
   */
  @Test
  public void chunkSizeWithExtensionParsesCorrectly() throws IOException
  {
    // 8-byte chunk with extension "name=value", then zero terminator.
    String chunked = "8;name=value\r\nABCDEFGH\r\n0\r\n\r\n";
    ChunkedEncodingInputStream s = wrap(chunked);

    byte[] buf = s.readAllBytes();
    assertEquals("ABCDEFGH",
                 new String(buf, StandardCharsets.US_ASCII),
                 "Chunk-extension should be ignored, only size used");
  }

  /**
   * If the declared chunk size exceeds the bytes actually present before EOF,
   * the documented "Unexpected EOF while reading chunk" branch must throw
   * {@link IOException}.
   */
  @Test
  public void shortChunkBytesThrows()
  {
    // Declares 8 bytes but provides only 4 before EOF.
    String malformed = "8\r\nABCD";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class,
        () -> s.readAllBytes());
    assertTrue(ex.getMessage().contains("Unexpected EOF"),
               "Error message should mention Unexpected EOF: "
                   + ex.getMessage());
  }

  /**
   * Missing trailing CRLF after a chunk's data must throw — specifically the
   * "EOF prior to trailing CR" branch.
   */
  @Test
  public void missingTrailingCrAfterChunkThrows()
  {
    // Chunk data present but no trailing CRLF before EOF.
    String malformed = "4\r\nABCD";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class,
        () -> s.readAllBytes());
    assertTrue(ex.getMessage().contains("trailing CR"),
               "Error message should mention trailing CR: "
                   + ex.getMessage());
  }

  /**
   * A non-CR character where the trailing CR is expected must throw — the
   * "Expected trailing CR" branch.
   */
  @Test
  public void wrongTrailingCharacterAfterChunkThrows()
  {
    // Chunk data followed by 'X' instead of CR.
    // cspell:disable
    String malformed = "4\r\nABCDX\n";
    // cspell:enable
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class,
        () -> s.readAllBytes());
    assertTrue(ex.getMessage().contains("Expected trailing CR"),
               "Error message should mention 'Expected trailing CR': "
                   + ex.getMessage());
  }

  /**
   * EOF where the trailing LF is expected must throw — the "EOF prior to
   * trailing LF" branch.
   */
  @Test
  public void eofBeforeTrailingLfAfterChunkThrows()
  {
    // CR received but stream ends before LF.
    String malformed = "4\r\nABCD\r";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class,
        () -> s.readAllBytes());
    assertTrue(ex.getMessage().contains("trailing LF"),
               "Error message should mention trailing LF: "
                   + ex.getMessage());
  }

  /**
   * A non-LF character where the trailing LF is expected must throw — the
   * "Expected trailing LF" branch.
   */
  @Test
  public void wrongTrailingLfAfterChunkThrows()
  {
    String malformed = "4\r\nABCD\rX";
    ChunkedEncodingInputStream s = wrap(malformed);

    IOException ex = assertThrows(IOException.class,
        () -> s.readAllBytes());
    assertTrue(ex.getMessage().contains("Expected trailing LF"),
               "Error message should mention 'Expected trailing LF': "
                   + ex.getMessage());
  }
}
