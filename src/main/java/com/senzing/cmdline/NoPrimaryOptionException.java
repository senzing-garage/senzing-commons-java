package com.senzing.cmdline;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Set;

/**
 * Thrown when a command-line argument designated as {@linkplain
 * CommandLineOption#isPrimary()} primary} is required, but none has been
 * specified.
 */
public class NoPrimaryOptionException extends CommandLineException {
  /**
   * The {@link Set} of {@link CommandLineOption} instances that represent the
   * primary options.
   */
  private Set<CommandLineOption> primaryOptions;

  /**
   * Constructs with the specified {@link Set} of {@link CommandLineOption}
   * instances identifying the primary options (one of which is required).
   *
   * @param primaryOptions The {@link Set} of {@link CommandLineOption}
   *                       instances identifying the primary options.
   *
   * @throws NullPointerException If the specified {@link Set} is
   *                              <code>null</code>.
   *
   * @throws IllegalArgumentException If the specified {@link Set} is empty
   *                                  or contains a {@link CommandLineOption}
   *                                  that is not a {@linkplain
   *                                  CommandLineOption#isPrimary() primary}.
   */
  public NoPrimaryOptionException(Set<CommandLineOption> primaryOptions)
    throws NullPointerException, IllegalArgumentException
  {
    super(buildErrorMessage(primaryOptions));
    this.primaryOptions = Set.copyOf(primaryOptions);
  }

  /**
   * Constructs with the specified message.
   *
   * @param message The message for the exception.
   */
  public NoPrimaryOptionException(String message) {
    super(message);
  }

  /**
   * Returns the <b>unmodifiable</b> {@link Set} of {@link CommandLineOption}
   * instances representing the primary options that could be specified.
   *
   * @return The <b>unmodifiable</b> {@link Set} of {@link CommandLineOption}
   *         instances representing the primary options that could be specified.
   */
  public Set<CommandLineOption> getPrimaryOptions() {
    return this.primaryOptions;
  }

  /**
   * Builds the error message describing the need to specify at least one
   * of the specified {@link Set} of options.
   *
   * @param primaryOptions The {@link Set} of {@link CommandLineOption}
   *                       instances identifying the primary options.
   *
   * @return The formatted error message.
   *
   * @throws NullPointerException If the specified {@link Set} is
   *                              <code>null</code>.
   *
   * @throws IllegalArgumentException If the specified {@link Set} is empty
   *                                  or contains a {@link CommandLineOption}
   *                                  that is not a {@linkplain
   *                                  CommandLineOption#isPrimary() primary}.
   */
  public static String buildErrorMessage(Set<CommandLineOption> primaryOptions)
    throws IllegalArgumentException
  {
    Objects.requireNonNull(
        primaryOptions, "The Set of primary options cannot be null");

    if (primaryOptions.size() == 0) {
      throw new IllegalArgumentException(
          "There must be at least one primary option");
    }
    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);
    pw.println("Must specify at least one of the following:");
    for (CommandLineOption option: primaryOptions) {
      // check if not primary
      if (!option.isPrimary()) {
        throw new IllegalArgumentException(
            "At least one of the specified options in the set is NOT a primary "
            + "option: " + option);
      }

      // add info on this option
      pw.println("     o " + option.getCommandLineFlag()
                     + (option.isDeprecated() ? " (deprecated)" : ""));
    }
    pw.flush();
    return sw.toString();
  }
}
