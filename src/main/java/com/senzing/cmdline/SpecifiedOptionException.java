package com.senzing.cmdline;

/**
 * Provides an abstract base class for exceptions pertaining to a specific
 * specified option.  Exceptions that extend this class implement the
 * {@link SpecifiedOption} interface.
 */
public abstract class SpecifiedOptionException
    extends CommandLineException implements SpecifiedOption
{
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
   * @param specifier The specifier for the option (e.g.: either command line
   *                  flag or environment variable).
   * @param option The {@link CommandLineOption} that was specified.
   */
  protected SpecifiedOptionException(CommandLineSource  source,
                                     CommandLineOption  option,
                                     String             specifier)
  {
    super();
    this.source     = source;
    this.specifier  = specifier;
    this.option     = option;
  }

  /**
   * Constructs with the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               value was specified.
   * @param specifier The specifier for the option (e.g.: either command line
   *                  flag or environment variable).
   * @param option The {@link CommandLineOption} that was specified.
   *
   * @param message The exception message.
   */
  protected SpecifiedOptionException(CommandLineSource  source,
                                     CommandLineOption  option,
                                     String             specifier,
                                     String             message)
  {
    super(message);
    this.source     = source;
    this.specifier  = specifier;
    this.option     = option;
  }

  /**
   * Gets the {@link CommandLineSource} for the option.
   *
   * @return The {@link CommandLineSource} for the option.
   */
  @Override
  public CommandLineSource getSource() {
    return this.source;
  }

  /**
   * Gets the {@link CommandLineOption} that programmatically identifies the
   * option.
   *
   * @return The {@link CommandLineOption} that programmatically identifies the
   *         option.
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

}
