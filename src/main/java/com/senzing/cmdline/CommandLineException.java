package com.senzing.cmdline;

/**
 * Provides a base class for a checked exception for command-line failures.
 */
public class CommandLineException extends Exception {
  /**
   * Default constructor.
   */
  public CommandLineException() {
    super();
  }

  /**
   * Constructs with the specified message.
   *
   * @param message The message describing the failure.
   */
  public CommandLineException(String message) {
    super(message);
  }
}
