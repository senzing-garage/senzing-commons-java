package com.senzing.cmdline;

import java.util.List;
import java.util.Map;

/**
 * Provides an interface for parsing the command line.
 */
public interface CommandLineParser {
  /**
   * Implement this method to parse the command line arguments and produce a
   * {@link Map} of {@link CommandLineOption} keys to {@link Object} values.
   *
   * @param args The command-line arguments to parse.
   *
   * @param deprecationWarnings The {@link List} to be populated with any
   *                            {@link DeprecatedOptionWarning} instances that
   *                            are generated, or <code>null</code> if the
   *                            caller is not interested in deprecation
   *                            warnings.
   *
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link Object}
   *         values describing the provided arguments.
   *
   * @throws CommandLineException If a command-line parsing/proecesing error
   *                              occurs.
   */
  Map<CommandLineOption, Object> parseCommandLine(
      String[]                      args,
      List<DeprecatedOptionWarning> deprecationWarnings)
    throws CommandLineException;
}
