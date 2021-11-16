package com.senzing.cmdline;

/**
 * Thrown when a command-line argument is specified that conflicts with
 * another command-line argument that has been specified.
 */
public class ConflictingOptionsException extends CommandLineException {
  /**
   * The first {@link SpecifiedOption}.
   */
  private SpecifiedOption firstOption;

  /**
   * The second {@link SpecifiedOption}.
   */
  private SpecifiedOption conflictingOption;

  /**
   * Constructs with the two specified options that are conflicting.
   *
   * @param firstOption The first {@link SpecifiedOption}.
   * @param conflictingOption The second {@link SpecifiedOption}.
   */
  public ConflictingOptionsException(SpecifiedOption firstOption,
                                     SpecifiedOption conflictingOption)
  {
    super("Cannot specify both the " + firstOption.getSourceDescriptor()
          + " and " + conflictingOption.getSourceDescriptor() + ".");
    this.firstOption        = firstOption;
    this.conflictingOption  = conflictingOption;
  }

  /**
   * The first {@link SpecifiedOption}.
   *
   * @return The first {@link SpecifiedOption}.
   */
  public SpecifiedOption getFirstOption() {
    return this.firstOption;
  }

  /**
   * The conflicting {@link SpecifiedOption}.
   *
   * @return The conflicting {@link SpecifiedOption}.
   */
  public SpecifiedOption getConflictingOption() {
    return this.conflictingOption;
  }
}
