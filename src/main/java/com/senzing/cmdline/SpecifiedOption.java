package com.senzing.cmdline;

/**
 * Implemented by classes that describe an option that was specified via
 * a specific {@link CommandLineSource}, {@link CommandLineOption} and
 * specifier.
 */
public interface SpecifiedOption {
  /**
   * Gets the associated {@link CommandLineSource}.
   *
   * @return The associated {@link CommandLineSource}.
   */
  CommandLineSource getSource();

  /**
   * Gets the associated {@link CommandLineOption}.
   *
   * @return The associated {@link CommandLineOption}.
   */
  CommandLineOption getOption();

  /**
   * Gets the specifier associated with the {@link CommandLineSource} (if any).
   * This returns <code>null</code> if none.
   *
   * @return The specifier associated with the {@link CommandLineSource}, or
   *         <code>null</code> if none.
   */
  String getSpecifier();

  /**
   * Returns a descriptor for the source of the specified option for building
   * user messages.
   *
   * @return A descriptor for the source of the specified option for building
   *         user messages.
   */
  default String getSourceDescriptor() {
    return sourceDescriptor(this.getSource(),
                            this.getOption(),
                            this.getSpecifier());
  }

  /**
   * Utility method for building the {@linkplain #getSourceDescriptor()
   * source descriptor} from the {@link CommandLineSource}, specifier and
   * {@link CommandLineOption}.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               was specified.
   * @param specifier The command-line flag or environment variable used to
   *                  specify the option, or <code>null</code> if specified as a
   *                  default value.
   * @param option The {@link CommandLineOption} that was missing required
   *               parameters.
   *
   * @return A descriptor for the source of the specified option for building
   *         user messages.
   */
  static String sourceDescriptor(CommandLineSource  source,
                                 CommandLineOption  option,
                                 String             specifier)
  {
    switch (source) {
      case COMMAND_LINE:
        return specifier + " option";
      case ENVIRONMENT:
        return specifier + " environment variable";
      default:
        return option.getCommandLineFlag() + " default";
    }
  }
}
