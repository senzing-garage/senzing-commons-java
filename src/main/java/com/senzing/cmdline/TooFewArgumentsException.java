package com.senzing.cmdline;

/**
 * Thrown when a command-line argument is specified that conflicts with
 * another command-line argument that has been specified.
 */
public class TooFewArgumentsException extends IllegalArgumentException {
  /**
   * Default constructor.
   */
  public TooFewArgumentsException() {
    super();
  }

  /**
   * Constructs with the specified message.
   *
   * @param message The message for the exception.
   */
  public TooFewArgumentsException(String message) {
    super(message);
  }
}
