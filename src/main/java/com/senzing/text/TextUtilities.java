package com.senzing.text;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.senzing.io.IOUtilities.UTF_8;

/**
 * Provides text utility functions.
 */
public class TextUtilities {
    /**
     * The random number generator to use for generating encryption keys.
     */
    private static final SecureRandom PRNG = new SecureRandom();

    /**
     * The minimum line length for formatting multi-line text.
     */
    public static final int MINIMUM_LINE_LENGTH = 30;

    /**
     * The minimum character in the range for generating random characters.
     */
    private static final int MIN_CHAR = (int) '!';

    /**
     * The maximum character in the range for generating random characters.
     */
    private static final int MAX_CHAR = (int) '~';

    /**
     * A list of printable characters.
     */
    private static final List<Character> PRINTABLE_CHARS;

    /**
     * A list of alphabetic characters.
     */
    private static final List<Character> ALPHA_CHARS;

    /**
     * A list of alpha-numeric characters.
     */
    private static final List<Character> ALPHANUMERIC_CHARS;

    static {
        try {
            int capacity = (MAX_CHAR - MIN_CHAR) + 1;
            List<Character> printableChars = new ArrayList<>(capacity);
            List<Character> alphaChars = new ArrayList<>(capacity);
            List<Character> alphaNumChars = new ArrayList<>(capacity);

            for (int index = MIN_CHAR; index <= MAX_CHAR; index++) {
                Character c = (char) index;
                printableChars.add(c);
                if (Character.isAlphabetic(c)) {
                    alphaChars.add(c);
                    alphaNumChars.add(c);
                } else if (Character.isDigit(c)) {
                    alphaNumChars.add(c);
                }
            }

            PRINTABLE_CHARS = Collections.unmodifiableList(printableChars);
            ALPHA_CHARS = Collections.unmodifiableList(alphaChars);
            ALPHANUMERIC_CHARS = Collections.unmodifiableList(alphaNumChars);

        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Default constructor.
     */
    private TextUtilities() {
        // do nothing
    }

    /**
     * Gets the a random count within the specified bounds.
     * 
     * @param minCount The inclusive lower bound for the random count.
     * @param maxCount The inclusive upper bound for the random count.
     * 
     * @return The random count.
     * 
     * @throws IllegalArgumentException If the specified minimum count is greater
     *                                  that the specified maximum count, or if
     *                                  either count is negative.
     */
    private static int randomCount(int minCount, int maxCount) {
        if (minCount < 0 || maxCount < 0) {
            throw new IllegalArgumentException(
                    "Neither of the specified parameters can be negative");
        }

        if (minCount > maxCount) {
            throw new IllegalArgumentException(
                    "The specified minimum count cannot be greater than the "
                            + "maximum count.  minCount=[ " + minCount + " ], maxCount=[ "
                            + maxCount + " ]");
        }

        // determine the count
        return PRNG.nextInt(maxCount - minCount) + minCount;
    }

    /**
     * Utility function to generate random printable non-whitespace ASCII text
     * of a specified length.
     *
     * @param count The number of characters to generate.
     *
     * @return The generated text.
     * 
     * @throws IllegalArgumentException If the specified count is negative.
     */
    public static String randomPrintableText(int count) {
        return randomText(count, PRINTABLE_CHARS);
    }

    /**
     * Utility function to generate random printable non-whitespace ASCII text
     * of a random length within the specified bounds.
     *
     * @param minCount The non-negative minimum number of characters to generate.
     * @param maxCount The non-negative maximum number of characters to generate.
     *
     * @return The generated text.
     * 
     * @throws IllegalArgumentException If the specified minimum count is greater
     *                                  that the specified maximum count, or if
     *                                  either count is negative.
     */
    public static String randomPrintableText(int minCount, int maxCount)
            throws IllegalArgumentException {
        return randomPrintableText(randomCount(minCount, maxCount));
    }

    /**
     * Utility function to generate random ASCII alphabetic text of a specified
     * length.
     *
     * @param count The number of characters to generate.
     *
     * @return The generated text.
     * 
     * @throws IllegalArgumentException If the specified count is negative.
     */
    public static String randomAlphabeticText(int count) {
        return randomText(count, ALPHA_CHARS);
    }

    /**
     * Utility function to generate random ASCII alphabetic text of a specified
     * length.
     *
     * @param minCount The non-negative minimum number of characters to generate.
     * @param maxCount The non-negative maximum number of characters to generate.
     *
     * @return The generated text.
     * 
     * @throws IllegalArgumentException If the specified minimum count is greater
     *                                  that the specified maximum count, or if
     *                                  either count is negative.
     */
    public static String randomAlphabeticText(int minCount, int maxCount) {
        return randomAlphabeticText(randomCount(minCount, maxCount));
    }

    /**
     * Utility function to generate random ASCII alpha-numeric text of a
     * specified length.
     *
     * @param count The number of characters to generate.
     *
     * @return The generated text.
     */
    public static String randomAlphanumericText(int count) {
        return randomText(count, ALPHANUMERIC_CHARS);
    }

    /**
     * Utility function to generate random ASCII alpha-numeric text of a
     * specified length.
     *
     * @param minCount The non-negative minimum number of characters to generate.
     * @param maxCount The non-negative maximum number of characters to generate.
     *
     * @return The generated text.
     * 
     * @throws IllegalArgumentException If the specified minimum count is greater
     *                                  that the specified maximum count, or if
     *                                  either count is negative.
     */
    public static String randomAlphanumericText(int minCount, int maxCount) {
        return randomAlphanumericText(randomCount(minCount, maxCount));
    }

    /**
     * Internal functions to generate random text given a list of allowed
     * characters and a count of the number of desired characters.
     */
    private static String randomText(int count, List<Character> allowedChars) {
        StringBuilder sb = new StringBuilder();
        int max = allowedChars.size();
        for (int index = 0; index < count; index++) {
            sb.append(allowedChars.get(PRNG.nextInt(max)));
        }
        return sb.toString();
    }

    /**
     * URL encodes the specified text using UTF-8 encoding.
     *
     * @param text The text to be encoded.
     *
     * @return The encoded text.
     */
    public static String urlEncodeUtf8(String text) {
        try {
            return URLEncoder.encode(text, UTF_8);
        } catch (UnsupportedEncodingException cannotHappen) {
            throw new IllegalStateException("UTF-8 encoding was not supported");
        }
    }

    /**
     * URL decodes the specified text using UTF-8 encoding.
     *
     * @param encodedText The text to be decoded.
     *
     * @return The decoded text.
     */
    public static String urlDecodeUtf8(String encodedText) {
        try {
            return URLDecoder.decode(encodedText, UTF_8);
        } catch (UnsupportedEncodingException cannotHappen) {
            throw new IllegalStateException("UTF-8 encoding was not supported");
        }
    }
}
