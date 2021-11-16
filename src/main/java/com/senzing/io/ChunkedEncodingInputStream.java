package com.senzing.io;

import java.io.*;
import java.nio.file.FileSystemNotFoundException;

/**
 * Provides a {@link FilterInputStream} implementation that will wrap an
 * {@link InputStream} that provides data encoded with chunked transfer encoding
 * and then decode the chunked transfer encoding.
 */
public class ChunkedEncodingInputStream extends FilterInputStream {
  /**
   * The current chunk.
   */
  private ByteArrayInputStream currentChunk = null;

  /**
   * Whether or not we have hit EOF.
   */
  private boolean eof = false;

  /**
   * Constructs with the specified {@link InputStream}.
   *
   * @param inputStream The {@link InputStream} to wrap.
   */
  public ChunkedEncodingInputStream(InputStream inputStream) {
    super(inputStream);
  }

  /**
   * Overridden to return the number of bytes on the current chunk and if the
   * next chunk is available and there is no current chunk to read the next
   * chunk and return how many bytes are available.
   */
  @Override
  public int available() throws IOException {
    // check if we do not have a current chunk
    if (this.currentChunk == null) {
      // check if the underlying input stream has a chunk available
      int available = this.in.available();
      if (available > 0) {
        // if a chunk is available, read it -- this should not block
        this.readChunk();
      } else {
        // if no chunk is available return zero (0)
        return 0;
      }
    }
    // if we read a chunk return how many available, otherwise zero (0)
    return this.currentChunk == null ? 0 : this.currentChunk.available();
  }

  /**
   * Closes the underlying {@link InputStream}.
   *
   * @throws IOException If a failure occurs.
   */
  @Override
  public void close() throws IOException  {
    this.in.close();
  }

  /**
   * Implemented to throw {@link UnsupportedOperationException} since mark
   * is not supported.
   *
   * @throws UnsupportedOperationException Whenever this method is called.
   */
  @Override
  public void mark(int readLimit) throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "Mark is not supported when decoding chunked transfer encoding");
  }

  /**
   * Overridden to return <code>false</code> to indicate that mark and reset are
   * not supported.
   *
   * @return <code>false</code> to indicate that mark and reset are not supported.
   */
  @Override
  public boolean markSupported() {
    return false;
  }

  /**
   * Reads the next byte.
   *
   * @throws IOException If a failure occurs.
   */
  @Override
  public int read() throws IOException {
    if (this.currentChunk == null || this.currentChunk.available() == 0) {
      this.readChunk();
    }
    return (this.currentChunk == null) ? -1 : this.currentChunk.read();
  }

  /**
   * Reads the next bytes and populates the buffer.
   *
   * @throws IOException If a failure occurs.
   */
  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    if (this.currentChunk == null || this.currentChunk.available() == 0) {
      this.readChunk();
    }
    return (this.currentChunk == null)
        ? -1 : this.currentChunk.read(buffer, offset, length);
  }

  /**
   * Implemented to throw {@link UnsupportedOperationException} since mark
   * is not supported.
   *
   * @throws UnsupportedOperationException Whenever this method is called.
   */
  @Override
  public void reset() throws UnsupportedOperationException {
    throw new UnsupportedOperationException(
        "Reset is not supported when decoding chunked transfer encoding");
  }

  /**
   * Skips the specified number of bytes.
   *
   * @param n The number of bytes to skip.
   *
   * @throws IOException If a failure occurs.
   */
  @Override
  public long skip(long n) throws IOException {
    long skipped = 0;
    while (!this.eof && skipped < n) {
      if (this.currentChunk == null || this.currentChunk.available() == 0) {
        this.readChunk();
      }
      if (this.currentChunk != null) {
        skipped += this.currentChunk.skip(n - skipped);
      }
    }
    return skipped;
  }

  /**
   * Reads the next chunkm, discarding the current chunk if any.
   */
  private boolean readChunk() throws IOException {
    this.currentChunk = null;
    StringBuilder sb = new StringBuilder();
    boolean cr = false;
    for (int readByte = this.in.read();
         readByte >= 0;
         readByte = this.in.read())
    {
      // if we have a carriage return then look for a line-feed
      if (cr) {
        if (readByte != ((int) '\n')) {
          throw new IOException(
              "Bad chunked encoding.  Encountered CR without subsequent LF: "
              + sb.toString());
        }

        // we should have the chunk size so break here
        break;

      } else if (readByte == ((int) '\n')) {
        throw new IOException(
            "Bad chunked encoding.  Encountered LF without preceding CR: "
                + sb.toString());
      }

      // look for carriage return character
      if (readByte == ((int) '\r')) {
        cr = true;
        continue;
      }

      // append the character
      sb.append((char) readByte);
    }

    // check if we reached EOF before the last chunk
    if (!cr) {
      this.eof = true;
      throw new IOException(
          "Bad chunked encoding.  Unexpected EOF (" + sb.length() + "): " + sb);
    }

    // get the chunk line
    String chunkLine = sb.toString();
    int index = chunkLine.indexOf(';');
    String chunkSize = (index <= 0) ? chunkLine : chunkLine.substring(0, index);
    int readCount = Integer.parseInt(chunkSize, 16);

    // check if final chunk was received
    if (readCount == 0) {
      this.eof = true;
    }

    // get the chunk bytes
    byte[] chunkBytes = null;
    if (readCount > 0) {
      chunkBytes = this.in.readNBytes(readCount);

      if (chunkBytes.length != readCount) {
        this.eof = true;
        throw new IOException(
            "Bad chunked encoding.  Unexpected EOF while reading chunk of size "
                + readCount + " (only " + chunkBytes.length + " read): " + chunkLine);
      }
    }

    // read the next 2 bytes and assert that they are CRLF
    int trailerByte = this.in.read();
    if (trailerByte < 0) {
      throw new IOException(
          "Bad chunked encoding.  EOF prior to trailing CR following chunk: "
          + Integer.toString(readCount, 16).toUpperCase());
    }
    if (trailerByte != '\r') {
      throw new IOException(
          "Bad chunked encoding.  Expected trailing CR following chunk ("
          + Integer.toString(readCount, 16).toUpperCase()
              + "), but got code point: " + trailerByte);
    }
    trailerByte = this.in.read();
    if (trailerByte < 0) {
      throw new IOException(
          "Bad chunked encoding.  EOF prior to trailing LF following chunk: "
              + Integer.toString(readCount, 16).toUpperCase());
    }
    if (trailerByte != '\n') {
      throw new IOException(
          "Bad chunked encoding.  Expected trailing LF following chunk ("
              + Integer.toString(readCount, 16).toUpperCase()
              + "), but got code point: " + trailerByte);
    }

    // create the next chunk stream
    this.currentChunk = (chunkBytes == null)
        ? null : new ByteArrayInputStream(chunkBytes);
    return (this.currentChunk != null);
  }
}
