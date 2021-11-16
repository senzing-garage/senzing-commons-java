package com.senzing.cmdline;

import java.util.Set;

/**
 * Thrown when a command-line option is illegally specified more than once.
 */
public class RepeatedOptionException extends CommandLineException {
  /**
   * The option that was repeated.
   */
  private CommandLineOption option;

  /**
   * The flags used to specify the option.
   */
  private Set<String> flags;

  /**
   * Constructs with the {@link CommandLineOption} that was repeated and the
   * flags used to specify it.  This is constructed with a {@link Set} of
   * flags that were used in case the same option can be specified with synonym
   * flags.
   *
   * @param option The {@link CommandLineOption} that was not repeated.
   * @param flags The {@link Set} of flags that were used to specify the option.
   */
  public RepeatedOptionException(CommandLineOption option, Set<String> flags)
  {
    super(buildErrorMessage(option, flags));
    this.option = option;
    this.flags  = (flags == null) ? Set.of() : Set.copyOf(flags);
  }

  /**
   * Return the {@link CommandLineOption} that was repeated.
   *
   * @return The {@link CommandLineOption} that was repeated.
   */
  public CommandLineOption getOption() {
    return this.option;
  }

  /**
   * Returns the <b>unmodifiable</b> {@link Set} of command-line flags used to
   * specify the repeated {@link CommandLineOption}.
   *
   * @return The <b>unmodifiable</b> {@link Set} of command-line flags used to
   *         specify the repeated {@link CommandLineOption}.
   */
  public Set<String> getFlags() {
    return this.flags;
  }

  /**
   * Builds the error message describing the repeated option.
   *
   * @param option The {@link CommandLineOption}.
   *
   * @param flags The {@link Set} of {@link String} command-line flags that
   *              were specified.
   *
   * @return The formatted error message.
   *
   * @throws NullPointerException If either of the specified parameters is
   *                              <code>null</code>.
   *
   * @throws IllegalArgumentException If the specified {@link Set} is empty.
   */
  public static String buildErrorMessage(CommandLineOption  option,
                                         Set<String>        flags)
      throws IllegalArgumentException
  {
    StringBuilder sb = new StringBuilder("Option specified more than once: ");

    String  prefix  = "";
    int     count   = 0;

    for (String repeatedFlag: flags) {
      sb.append(prefix).append(repeatedFlag);
      count++;
      prefix = (count == flags.size()) ? " and " : ", ";
    }

    return sb.toString();
  }

}
