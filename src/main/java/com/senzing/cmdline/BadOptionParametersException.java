package com.senzing.cmdline;

import java.util.List;

/**
 * Thrown when the parameters specified for a {@link CommandLineOption} are not
 * valid.
 */
public class BadOptionParametersException extends SpecifiedOptionException {
  /**
   * The parameters that were specified (if any).
   */
  private List<String> parameters;

  /**
   * Constructs with the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               was specified.
   * @param specifier The command-line flag or environment variable used to
   *                  specify the option, or <code>null</code> if specified as a
   *                  default value.
   * @param option The {@link CommandLineOption} that was missing required
   *               parameters.
   * @param params The {@link List} of parameters that were specified (if any).
   */
  public BadOptionParametersException(CommandLineSource source,
                                      CommandLineOption option,
                                      String            specifier,
                                      List<String>      params)
  {
    super(source, option, specifier);
    this.parameters = (params == null) ? List.of() : List.copyOf(params);
  }

  /**
   * Constructs with the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               was specified.
   * @param option The {@link CommandLineOption} that was missing required
   *               parameters.
   * @param specifier The command-line flag or environment variable used to
   *                  specify the option, or <code>null</code> if specified as a
   *                  default value.
   * @param params The {@link List} of parameters that were specified (if any).
   *
   * @param message The message for the exception.
   */
  public BadOptionParametersException(CommandLineSource source,
                                      CommandLineOption option,
                                      String            specifier,
                                      List<String>      params,
                                      String            message)
  {
    super(source, option, specifier, message);
    this.parameters = (params == null) ? List.of() : List.copyOf(params);
  }

  /**
   * Returns the <b>unmodifiable</b> {@link List} of parameters that were
   * specified for the {@linkplain #getOption() option} (if any).
   *
   * @return The <b>unmodifiable</b> {@link List} of parameters that were
   *         specified for the {@linkplain #getOption() option} (if any).
   */
  public List<String> getParameters() {
    return this.parameters;
  }

  /**
   *
   */
}
