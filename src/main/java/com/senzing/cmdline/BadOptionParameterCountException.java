package com.senzing.cmdline;

import java.util.List;

/**
 * Thrown when there are not enough or too many command line parameters for the
 * {@link CommandLineOption}.
 */
public class BadOptionParameterCountException extends SpecifiedOptionException
{
  /**
   * The parameters that were specified (if any).
   */
  private List<String> parameters;

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
   */
  public BadOptionParameterCountException(CommandLineSource source,
                                          CommandLineOption option,
                                          String            specifier,
                                          List<String>      params)
  {
    super(source, option, specifier,
          buildErrorMessage(source, option, specifier, params));
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
   * Formats the exception message for the specified parameters.
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
   * @return The formatted error message.
   *
   * @throws NullPointerException If the {@link CommandLineSource}, {@link
   *                              CommandLineOption} or {@link List} of
   *                              parameters is <code>null</code>.
   *
   * @throws IllegalArgumentException If the number of parameters in the
   *                                  specified {@link List} is actually valid
   *                                  for the specified {@link
   *                                  CommandLineOption}.
   */
  public static String buildErrorMessage(CommandLineSource  source,
                                         CommandLineOption  option,
                                         String             specifier,
                                         List<String>       params)
    throws NullPointerException, IllegalArgumentException
  {
    int minCount    = option.getMaximumParameterCount();
    int maxCount    = option.getMaximumParameterCount();
    int paramCount  = params.size();

    StringBuilder sb = new StringBuilder("Too ");
    if (paramCount < minCount) {
      sb.append("few");
    } else if (maxCount > 0 && paramCount > maxCount) {
      sb.append("many");
    } else {
      throw new IllegalArgumentException(
          "The specified parameter list has a parameter count that is valid "
              + "for the specified option.  source=[ " + source + " ], "
              + "option=[ " + option + " ], specifier=[ " + specifier
              + " ], count=[ " + paramCount + " ], minCount=[ " + minCount
              + " ], maxCount=[ " + maxCount + " ], params=[ " + params + " ]");
    }
    sb.append(" parameters for the ");
    sb.append(SpecifiedOption.sourceDescriptor(source, option, specifier));
    sb.append(".  Expected at ");
    if (paramCount < minCount) {
      sb.append("least ").append(minCount);
    } else if (maxCount > 0 && paramCount > maxCount) {
      sb.append("most ").append(maxCount);
    }
    sb.append(" parameters but received ").append(paramCount).append(": ");

    String prefix = "";
    for (String param: params) {
      sb.append(prefix).append(param);
      prefix = ", ";
    }

    return sb.toString();
  }
}
