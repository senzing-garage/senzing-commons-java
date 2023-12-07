package com.senzing.cmdline;

import com.sun.source.doctree.SeeTree;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

/**
 * Thrown when a command-line argument is specified without the required
 * dependencies for that argument.
 */
public class MissingDependenciesException extends SpecifiedOptionException {
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
   * @param specifiedOptions The {@link Set} of {@link CommandLineOption}
   *                         instances that were specified.
   */
  public MissingDependenciesException(CommandLineSource       source,
                                      CommandLineOption       option,
                                      String                  specifier,
                                      Set<CommandLineOption>  specifiedOptions)
  {
    super(source, option, specifier,
          buildErrorMessage(source, 
                            option, 
                            option.getDependencies(),
                            specifier, 
                            specifiedOptions));
  }

  /**
   * Constructs with the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               was specified.
   * @param option The {@link CommandLineOption} that was missing required
   *               parameters.
   * @param dependencySets The {@link Set} of {@link CommandLineOption} 
   *                       {@link Set} values representing the missing
   *                       dependencies to report.
   * @param specifier The command-line flag or environment variable used to
   *                  specify the option, or <code>null</code> if specified as a
   *                  default value.
   * @param specifiedOptions The {@link Set} of {@link CommandLineOption}
   *                         instances that were specified.
   */
  public MissingDependenciesException(CommandLineSource           source,
                                      CommandLineOption           option,
                                      Set<Set<CommandLineOption>> dependencySets,
                                      String                      specifier,
                                      Set<CommandLineOption>      specifiedOptions)
  {
    super(source, option, specifier,
          buildErrorMessage(source, 
                            option, 
                            dependencySets,
                            specifier,
                            specifiedOptions));
  }

  /**
   * Formats the exception message for the specified parameters.
   *
   * @param source The {@link CommandLineSource} describing how the option
   *               was specified.
   * @param option The {@link CommandLineOption} that was missing required
   *               parameters.
   * @param dependencySets The {@link Set} of {@link CommandLineOption} 
   *                       {@link Set} values representing the missing
   *                       dependencies to report.
   * @param specifier The command-line flag or environment variable used to
   *                  specify the option, or <code>null</code> if specified as a
   *                  default value.
   * @param specifiedOptions The {@link Set} of {@link CommandLineOption}
   *                         instances that were specified.
   *
   * @return The formatted error message.
   *
   * @throws NullPointerException If the {@link CommandLineSource}, {@link
   *                              CommandLineOption} or {@link Set} of
   *                              specified {@link CommandLineOption} instances
   *                              is <code>null</code>.
   */
  @SuppressWarnings("unchecked")
  public static String buildErrorMessage(
      CommandLineSource           source,
      CommandLineOption           option,
      Set<Set<CommandLineOption>> dependencySets,
      String                      specifier,
      Set<CommandLineOption>      specifiedOptions)
  {
    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);

    String sourceDescriptor
        = SpecifiedOption.sourceDescriptor(source, option, specifier);

    pw.println("Dependent options for the " + sourceDescriptor
                   + " are missing.");
    pw.println("The " + sourceDescriptor + " also requires:");

    String prefix = null;
    for (Set<CommandLineOption> dependencySet : dependencySets) {
      boolean conflicting = false;
      // ignore dependency sets that conflict with other specified options
      for (CommandLineOption specifiedOption: specifiedOptions) {
        Set<CommandLineOption> conflicts = specifiedOption.getConflicts();
        if (conflicts != null) {
          for (CommandLineOption conflict : conflicts) {
            if (dependencySet.contains(conflict)) {
              conflicting = true;
              break;
            }
          }
        }
        if (conflicting) break;
      }
      if (conflicting) continue;
      pw.println();
      if (prefix != null) {
        pw.println(prefix);
        pw.println();
      }
      for (CommandLineOption dependency : dependencySet) {
        if (!specifiedOptions.contains(dependency)) {
          pw.print("     o " + dependency.getCommandLineFlag());
          if (dependency.getEnvironmentVariable() != null) {
            pw.print(" (env: " + dependency.getEnvironmentVariable() + ")");
          }
          pw.println();
        }
      }
      prefix = "     ==================================  OR  "
          + "==================================";
    }

    pw.flush();
    return sw.toString();
  }
}
