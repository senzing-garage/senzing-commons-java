package com.senzing.cmdline;

/**
 * Thrown when a command-line argument is specified that is not recognized.
 */
public class UnrecognizedOptionException extends CommandLineException {
  /**
   * The unrecognized option.
   */
  private String option;

  /**
   * Constructs with the specified option that was not recognized.
   *
   * @param option The option that was not recognized.
   */
  public UnrecognizedOptionException(String option) {
    super();
    this.option = option;
  }

  /**
   * Constructs with the specified message.
   *
   * @param option The option that was not recognized.
   * @param message The message for the exception.
   */
  public UnrecognizedOptionException(String option, String message) {
    super(message);
    this.option = option;
  }

  /**
   * Gets the option that was not recognized.
   *
   * @return The option that was not recognized.
   */
  public String getOption() {
    return this.option;
  }
}
