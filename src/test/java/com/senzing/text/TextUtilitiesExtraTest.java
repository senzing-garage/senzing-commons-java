package com.senzing.text;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static com.senzing.text.TextUtilities.randomAlphabeticText;
import static com.senzing.text.TextUtilities.randomAlphanumericText;
import static com.senzing.text.TextUtilities.randomPrintableText;
import static com.senzing.text.TextUtilities.urlDecodeUtf8;
import static com.senzing.text.TextUtilities.urlEncodeUtf8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Supplementary {@link TextUtilities} tests targeting the argument-validation
 * paths and URL-encode/decode round-trip not in
 * {@code TextUtilitiesTest}: each {@code random*} variant rejects
 * negative counts; {@code random*Text(min, max)} rejects negative bounds and
 * {@code min > max}; {@code min == max} is the single-bound shortcut; and the
 * URL utilities round-trip correctly.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class TextUtilitiesExtraTest
{
  // -------------------------------------------------------------------
  // randomCount(min, max) validation — exercised through the public
  // bounded random-text methods.
  // -------------------------------------------------------------------

  @Test
  public void boundedRandomPrintableTextRejectsNegativeMin()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomPrintableText(-1, 10));
  }

  @Test
  public void boundedRandomPrintableTextRejectsNegativeMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomPrintableText(0, -1));
  }

  @Test
  public void boundedRandomPrintableTextRejectsMinGreaterThanMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomPrintableText(10, 5));
  }

  @Test
  public void boundedRandomAlphabeticTextRejectsNegativeMin()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphabeticText(-5, 10));
  }

  @Test
  public void boundedRandomAlphabeticTextRejectsMinGreaterThanMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphabeticText(10, 5));
  }

  @Test
  public void boundedRandomAlphanumericTextRejectsNegativeMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphanumericText(0, -3));
  }

  @Test
  public void boundedRandomAlphanumericTextRejectsMinGreaterThanMax()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphanumericText(20, 5));
  }

  /**
   * When {@code min == max}, the bounded helpers must return a string of
   * exactly that length — exercising the
   * {@code (max == min) ? min : ...} shortcut in
   * {@code randomCount}.
   */
  @Test
  public void boundedRandomPrintableTextWithEqualMinMaxReturnsExactLength()
  {
    String text = randomPrintableText(8, 8);
    assertEquals(8, text.length(),
                 "When min == max, length must be exactly that value");
  }

  @Test
  public void boundedRandomAlphabeticTextWithEqualMinMaxReturnsExactLength()
  {
    String text = randomAlphabeticText(12, 12);
    assertEquals(12, text.length());
  }

  @Test
  public void boundedRandomAlphanumericTextWithEqualMinMaxReturnsExactLength()
  {
    String text = randomAlphanumericText(5, 5);
    assertEquals(5, text.length());
  }

  // -------------------------------------------------------------------
  // randomText(count) negative-count validation — exercised through
  // each public single-count variant.
  // -------------------------------------------------------------------

  @Test
  public void randomPrintableTextRejectsNegativeCount()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomPrintableText(-1));
  }

  @Test
  public void randomAlphabeticTextRejectsNegativeCount()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphabeticText(-1));
  }

  @Test
  public void randomAlphanumericTextRejectsNegativeCount()
  {
    assertThrows(IllegalArgumentException.class,
                 () -> randomAlphanumericText(-1));
  }

  /**
   * {@code randomXxxText(0)} must produce an empty string per the
   * documented contract (count is the number of characters; zero is the valid
   * lower bound).
   */
  @Test
  public void randomPrintableTextZeroCountReturnsEmpty()
  {
    assertEquals("", randomPrintableText(0));
  }

  @Test
  public void randomAlphabeticTextZeroCountReturnsEmpty()
  {
    assertEquals("", randomAlphabeticText(0));
  }

  @Test
  public void randomAlphanumericTextZeroCountReturnsEmpty()
  {
    assertEquals("", randomAlphanumericText(0));
  }

  // -------------------------------------------------------------------
  // Character-set guarantees — alphabetic vs. alphanumeric vs.
  // printable
  // -------------------------------------------------------------------

  @Test
  public void randomAlphabeticTextProducesOnlyLetters()
  {
    String text = randomAlphabeticText(64);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      assertEquals(true, Character.isAlphabetic(c),
                   "All characters in randomAlphabeticText output "
                       + "must be alphabetic; got: " + c);
    }
  }

  @Test
  public void randomAlphanumericTextProducesOnlyLettersOrDigits()
  {
    String text = randomAlphanumericText(64);
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      assertEquals(true,
                   Character.isAlphabetic(c) || Character.isDigit(c),
                   "All characters in randomAlphanumericText output "
                       + "must be alphabetic or digit; got: " + c);
    }
  }

  // -------------------------------------------------------------------
  // urlEncodeUtf8 / urlDecodeUtf8
  // -------------------------------------------------------------------

  @Test
  public void urlEncodeAndDecodeRoundTrip()
  {
    String original = "hello world & friends";
    String encoded = urlEncodeUtf8(original);
    String decoded = urlDecodeUtf8(encoded);
    assertEquals(original, decoded,
                 "urlEncode/urlDecode round-trip must preserve text");
  }

  @Test
  public void urlEncodeReplacesSpecialCharacters()
  {
    String encoded = urlEncodeUtf8("a b");
    // Per URLEncoder default: space → "+" or "%20" depending on form.
    // The implementation uses URLEncoder.encode which produces "+".
    assertEquals(true, encoded.contains("+") || encoded.contains("%20"),
                 "URL encoding of space should produce + or %20: "
                     + encoded);
  }

  @Test
  public void urlEncodeAndDecodeNonAsciiCharacters()
  {
    // Non-ASCII characters must round-trip through the UTF-8
    // encode/decode pair.
    String original = "café résumé";
    assertEquals(original, urlDecodeUtf8(urlEncodeUtf8(original)));
  }

  @Test
  public void urlEncodeEmptyString()
  {
    assertEquals("", urlEncodeUtf8(""));
  }

  @Test
  public void urlDecodeEmptyString()
  {
    assertEquals("", urlDecodeUtf8(""));
  }
}
