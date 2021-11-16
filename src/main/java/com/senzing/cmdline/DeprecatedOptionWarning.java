package com.senzing.cmdline;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Set;

/**
 * Describes a deprecated option that was specified including an error message
 * to display.
 */
public class DeprecatedOptionWarning implements SpecifiedOption {
  /**
   * The {@link CommandLineSource} for the deprecated option.
   */
  private CommandLineSource source;

  /**
   * The {@link CommandLineOption} that programmatically identifies the
   * deprecated option.
   */
  private CommandLineOption option;

  /**
   * The specifier (e.g.: command-line flag or environment variable) that
   * was used to specify the option.
   */
  private String specifier;

  /**
   * Constructs with the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               value was specified.
   * @param option The {@link CommandLineOption} that was deprecated.
   * @param specifier The specifier for the option (e.g.: either command line
   *                  flag or environment variable).
   *
   * @throws IllegalArgumentException If the specified {@link CommandLineOption}
   *                                  is <b>not</b> deprecated.
   */
  public DeprecatedOptionWarning(CommandLineSource  source,
                                 CommandLineOption  option,
                                 String             specifier)
    throws IllegalArgumentException
  {
    if (!option.isDeprecated()) {
      throw new IllegalArgumentException(
          "The specified CommandLineOption is not deprecated: " + option
          + " (source=[ " + source + " ], specifier=[ " + specifier + " ])");
    }
    this.source     = source;
    this.specifier  = specifier;
    this.option     = option;
  }

  /**
   * Constructs with the {@link SpecifiedOption} that provides the {@link
   * CommandLineSource}, {@link CommandLineOption} and specifier.
   *
   * @param specifiedOption The {@link SpecifiedOption} that provides the {@link
   *                        CommandLineSource}, {@link CommandLineOption} and
   *                        specifier.
   *
   * @throws IllegalArgumentException If the provided {@link SpecifiedOption}
   *                                  has a {@link CommandLineOption} that is
   *                                  <b>not</b> deprecated.
   */
  public DeprecatedOptionWarning(SpecifiedOption specifiedOption)
  {
    this(specifiedOption.getSource(),
         specifiedOption.getOption(),
         specifiedOption.getSpecifier());
  }

  /**
   * Gets the {@link CommandLineSource} for the deprecated option.
   *
   * @return The {@link CommandLineSource} for the deprecated option.
   */
  @Override
  public CommandLineSource getSource() {
    return this.source;
  }

  /**
   * Gets the {@link CommandLineOption} that programmatically identifies the
   * deprecated option.
   *
   * @return The {@link CommandLineOption} that programmatically identifies the
   *         deprecated option.
   */
  @Override
  public CommandLineOption getOption() {
    return this.option;
  }

  /**
   * Gets the specifier (e.g.: command-line flag or environment variable) that
   * was used to specify the option.
   *
   * @return The specifier (e.g.: command-line flag or environment variable) that
   *         was used to specify the option.
   */
  @Override
  public String getSpecifier() {
    return this.specifier;
  }

  /**
   * Implemented to return a formatted warning message describing the
   * deprecation and alternate options that can be specified.
   */
  @SuppressWarnings("unchecked")
  public String toString() {
    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);
    pw.println("WARNING: The " + this.getSourceDescriptor()
                   + " option is deprecated and will be removed in a "
                   + "future release.");

    Set<CommandLineOption> alternatives
        = this.option.getDeprecationAlternatives();
    if (alternatives.size() == 1) {
      CommandLineOption alternative = alternatives.iterator().next();
      pw.println();
      pw.println(
          "Consider using " + alternative.getCommandLineFlag()
              + " instead.");

    } else if (alternatives.size() > 1) {
      pw.println();
      pw.println("Consider using one of the following instead:");
      for (CommandLineOption alternative : alternatives) {
        pw.println("     o " + alternative.getCommandLineFlag());
      }
    }
    pw.println();
    pw.flush();

    return sw.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeprecatedOptionWarning that = (DeprecatedOptionWarning) o;
    return (this.getSource() == that.getSource()
            && Objects.equals(this.getOption(), that.getOption())
            && Objects.equals(this.getSpecifier(), that.getSpecifier()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSource(), getOption(), getSpecifier());
  }
}
