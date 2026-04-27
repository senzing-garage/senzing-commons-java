package com.senzing.util;

import java.util.*;

/**
 * Represents a semantic version described as a {@link String} containing
 * integer numbers separated by decimal points (e.g.: "1.4.5"), optionally
 * followed by a pre-release suffix of {@code "-alpha"}, {@code "-beta"}, or
 * {@code "-rc"} with additional dot-separated version parts (e.g.:
 * "2.0.0-alpha.2.0", "2.0.0-beta.3.2", "2.0.0-rc.2.1"). Pre-release suffixes
 * are normalized to lowercase on construction (e.g.: {@code "RC"} becomes
 * {@code "rc"}).
 */
public class SemanticVersion implements Comparable<SemanticVersion>
{
    /**
     * The value used to represent a release candidate pre-release suffix.
     */
    private static final int RC_VALUE = -1;

    /**
     * The value used to represent a beta pre-release suffix.
     */
    private static final int BETA_VALUE = -2;

    /**
     * The value used to represent an alpha pre-release suffix.
     */
    private static final int ALPHA_VALUE = -3;

    /**
     * An unmodifiable {@link Map} of pre-release integer sentinel values to
     * their corresponding string suffix representations for use in
     * reconstructing the version string via {@link #toString()}.
     */
    private static final Map<Integer, String> SUFFIX_MAP = Map.of(
        RC_VALUE,    "-rc",
        BETA_VALUE,  "-beta",
        ALPHA_VALUE, "-alpha"
    );

    /**
     * The {@link List} of version parts.
     */
    private List<Integer> versionParts;

    /**
     * The version string representation.
     */
    private String versionString;

    /**
     * The normalized version of the {@link String} for calculating the hash
     * code.
     */
    private String normalizedString;

    /**
     * Constructs with the specified version string (e.g.: "1.4.5"). Pre-release
     * suffixes ({@code "alpha"}, {@code "beta"}, {@code "rc"}) are supported
     * and normalized to lowercase (e.g.: {@code "RC"} becomes
     * {@code "rc"}).
     *
     * @param versionString The version string with which to construct.
     *
     * @throws NullPointerException     If the specified parameter is
     *                                  <code>null</code>.
     *
     * @throws IllegalArgumentException If the specified parameter is not
     *                                  properly formatted.
     */
    public SemanticVersion(String versionString)
            throws NullPointerException, IllegalArgumentException
    {
        Objects.requireNonNull(
                versionString, "Version string cannot be null");

        try {
            String[] tokens = versionString.split("[-.]");
            this.versionParts = new ArrayList<>(tokens.length);
            boolean hasSuffix = false;
            for (String token : tokens) {
                String lowerToken = token.toLowerCase();
                switch (lowerToken) {
                    case "alpha":
                    case "beta":
                    case "rc":
                        if (hasSuffix) {
                            throw new IllegalArgumentException(
                                    "Multiple pre-release suffixes are not "
                                    + "allowed: " + versionString);
                        }
                        if (this.versionParts.isEmpty()) {
                            throw new IllegalArgumentException(
                                    "Pre-release suffix must follow at least "
                                    + "one numeric version part: "
                                    + versionString);
                        }
                        hasSuffix = true;
                        this.versionParts.add(
                            lowerToken.equals("alpha") ? ALPHA_VALUE
                            : lowerToken.equals("beta") ? BETA_VALUE
                            : RC_VALUE);
                        break;
                    default:
                        int part = Integer.parseInt(token);
                        if (part < 0) {
                            throw new IllegalArgumentException(
                                    "Negative version part is not allowed: "
                                    + part);
                        }
                        this.versionParts.add(part);
                        break;
                }
            }

            // create a normalized list of version
            // parts by removing trailing zeroes
            List<Integer> normalized = new LinkedList<>(this.versionParts);
            Collections.reverse(normalized);
            Iterator<Integer> iter = normalized.iterator();
            while (iter.hasNext()) {
                Integer part = iter.next();
                if (!part.equals(0)) break;
                iter.remove();
            }
            Collections.reverse(normalized);

            // set the version strings
            this.versionString = buildVersionString(this.versionParts);
            this.normalizedString = buildVersionString(normalized);

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid semantic version string: " + versionString);
        }
    }

    /**
     * Builds a version string from the specified list of version parts,
     * converting pre-release sentinel values back to their string suffixes.
     *
     * @param parts The list of version parts.
     * @return The version string.
     */
    private static String buildVersionString(List<Integer> parts)
    {
        StringBuilder sb = new StringBuilder();
        String prefix = "";
        for (Integer part : parts) {
            String suffix = SUFFIX_MAP.get(part);
            if (suffix != null) {
                sb.append(suffix);
                prefix = ".";
            } else {
                sb.append(prefix).append(part);
                prefix = ".";
            }
        }
        return sb.toString();
    }

    /**
     * Overridden to return <code>true</code> if and only if the specified
     * parameter is a non-null reference to an object of the same class with
     * equivalent version parts.
     *
     * @param object The object to compare with.
     *
     * @return <code>true</code> if the objects are equal, otherwise
     *                           <code>false</code>
     */
    @Override
    public boolean equals(Object object)
    {
        if (object == null) return false;
        if (this == object) return true;
        if (this.getClass() != object.getClass()) return false;
        SemanticVersion version = (SemanticVersion) object;
        return (this.compareTo(version) == 0);
    }

    /**
     * Implemented to return a hash code that is consistent with the {@link
     * #equals(Object)} implementation.
     *
     * @return The hash code for this instance.
     */
    @Override
    public int hashCode()
    {
        return this.normalizedString.hashCode();
    }

    /**
     * Implemented to compare the parts of the semantic version in order.
     */
    @Override
    public int compareTo(SemanticVersion other)
    {
        Objects.requireNonNull(
                other, "The specified parameter cannot be null");
        Iterator<Integer> iter1 = this.versionParts.iterator();
        Iterator<Integer> iter2 = other.versionParts.iterator();

        // iterate over the parts
        while (iter1.hasNext() || iter2.hasNext()) {
            // get the next version parts
            Integer part1 = iter1.hasNext() ? iter1.next() : 0;
            Integer part2 = iter2.hasNext() ? iter2.next() : 0;

            // compare the parts
            int diff = Integer.compare(part1, part2);

            // if the diff is non-zero then return it
            if (diff != 0) return diff;
        }

        // if we have exhausted all version parts without
        // a difference then we have equality
        return 0;
    }

    /**
     * Returns the version string for this instance. This is equivalent to
     * calling {@link #toString(boolean)} with <code>false</code> as the
     * parameter.
     *
     * @return The version string for this instance.
     */
    @Override
    public String toString()
    {
        return this.toString(false);
    }

    /**
     * Returns a version string for this instance that is optionally normalized
     * to remove trailing zeroes.
     *
     * @param normalized <code>true</code> if trailing zeroes should be
     *                   stripped, otherwise <code>false</code>.
     *
     * @return A {@link String} representation of this instance that is
     *           optionally normalized to remove trailing zeroes.
     */
    public String toString(boolean normalized)
    {
        return (normalized) ? this.normalizedString : this.versionString;
    }

    /**
     * Provides a test main method.
     * 
     * @param args The command-line arguments.
     */
    public static void main(String[] args)
    {
        if (args.length == 0) {
            System.err.println("USAGE: java " + SemanticVersion.class.getName()
                    + " <version> [compare-version]*");
            System.exit(1);
        }
        SemanticVersion version = new SemanticVersion(args[0]);
        System.out.println(
                "VERSION: " + version + " (" + version.toString(true) + ")");
        for (int index = 1; index < args.length; index++) {
            SemanticVersion version2 = new SemanticVersion(args[index]);
            System.out.println();
            System.out.println(
                    "VERSUS " + version2 + " (" + version2.toString(true)
                            + "): "
                            + "COMPARE (" + version.compareTo(version2) + ") "
                            + " / EQUALS (" + version.equals(version2) + ") "
                            + " / HASH (" + version2.hashCode() + ") "
                            + " / HASH-EQUALITY ("
                            + (version.hashCode() == version2.hashCode())
                            + ")");
        }
    }
}
