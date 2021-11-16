package com.senzing.text;

import com.senzing.io.ChunkedEncodingInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.security.SecureRandom;

import static com.senzing.text.TextUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TextUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TextUtilitiesTest {
  private static final SecureRandom PRNG = new SecureRandom();

  @Test
  public void randomPrintableTextTest() {
    try {
      for (int count = 100; count < 150; count += PRNG.nextInt(10)) {
        String text1 = randomPrintableText(count);
        String text2 = randomPrintableText(count);
        assertEquals(count, text1.length(),
                     "First generated text is wrong length");
        assertEquals(count, text2.length(),
                     "Second generated text is wrong length");
        assertNotEquals(text1, text2,
                        "Randomly generated text is identical.");

        for (int index = 0; index < count; index++) {
          char c1 = text1.charAt(index);
          char c2 = text2.charAt(index);
          assertTrue((c1 >= 32 && c1 < 127),
                     "Random character is not printable: " + ((int) c1));
          assertTrue((c2 >= 32 && c2 < 127),
                     "Random character is not printable: " + ((int) c2));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("randomPrintableTextTest() failed with exception: " + e);
    }
  }

  @Test
  public void randomAlphabeticTextTest() {
    try {
      for (int count = 100; count < 150; count += PRNG.nextInt(10)) {
        String text1 = randomAlphabeticText(count);
        String text2 = randomAlphabeticText(count);
        assertEquals(count, text1.length(),
                     "First generated text is wrong length");
        assertEquals(count, text2.length(),
                     "Second generated text is wrong length");
        assertNotEquals(text1, text2,
                        "Randomly generated text is identical.");

        for (int index = 0; index < count; index++) {
          char c1 = text1.charAt(index);
          char c2 = text2.charAt(index);
          assertTrue(Character.isAlphabetic(c1),
                     "Random character is not alphabetic: " + ((int) c1));
          assertTrue(Character.isAlphabetic(c2),
                     "Random character is not alphabetic: " + ((int) c2));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("randomAlphabeticTextTest() failed with exception: " + e);
    }
  }

  @Test
  public void randomAlphanumericTextTest() {
    try {
      for (int count = 100; count < 150; count += PRNG.nextInt(10)) {
        String text1 = randomAlphanumericText(count);
        String text2 = randomAlphanumericText(count);
        assertEquals(count, text1.length(),
                     "First generated text is wrong length");
        assertEquals(count, text2.length(),
                     "Second generated text is wrong length");
        assertNotEquals(text1, text2,
                        "Randomly generated text is identical.");

        for (int index = 0; index < count; index++) {
          char c1 = text1.charAt(index);
          char c2 = text2.charAt(index);
          assertTrue(Character.isAlphabetic(c1) || Character.isDigit(c1),
                     "Random character is not alphanumeric: " + ((int) c1));
          assertTrue(Character.isAlphabetic(c2) || Character.isDigit(c2),
                     "Random character is not alphanumeric: " + ((int) c2));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("randomAlphanumericTextTest() failed with exception: " + e);
    }
  }
}
