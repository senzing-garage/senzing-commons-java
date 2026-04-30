package com.senzing.cmdline;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Test enum used by {@link CommandLineUtilitiesEnvTest} to exercise the
 * environment-variable handling paths in
 * {@link CommandLineUtilities#parseCommandLine} that the existing
 * {@link TestOption} cannot reach because none of its options have
 * a maximum parameter count greater than 1.
 *
 * <p>Each option's environment variable is named after the
 * command-line flag — e.g. {@code --tags} maps to
 * {@code SENZING_ENV_TEST_TAGS}.</p>
 */
public enum EnvTestOption
    implements CommandLineOption<EnvTestOption, EnvTestOption>
{
  /**
   * Multi-value option (1..n parameters) used to exercise the
   * comma-/space-split branch and the JSON-array branch in
   * {@code processEnvironment}.
   */
  TAGS("--tags", 1, -1),

  /**
   * Bounded multi-value option (2..3 parameters) used to exercise the
   * bounded-range path and parameter-count error branches.
   */
  COORDS("--coords", 2, 3),

  /**
   * Boolean no-arg flag (0..0) used to exercise the empty / "true" / "false" /
   * invalid switch in
   * {@code processEnvironment}.
   */
  VERBOSE("--verbose", 0, 0),

  /**
   * Single-value flag (1..1) used to exercise the single-parameter branch.
   */
  NAME("--name", 1, 1);

  private final String flag;
  private final int minParams;
  private final int maxParams;

  EnvTestOption(String flag, int minParams, int maxParams)
  {
    this.flag = flag;
    this.minParams = minParams;
    this.maxParams = maxParams;
  }

  @Override
  public String getCommandLineFlag()
  {
    return this.flag;
  }

  @Override
  public Set<String> getSynonymFlags()
  {
    return Collections.emptySet();
  }

  @Override
  public String getEnvironmentVariable()
  {
    return "SENZING_ENV_TEST_"
        + this.flag.substring(2).toUpperCase();
  }

  @Override
  public Set<String> getEnvironmentSynonyms()
  {
    return Collections.singleton(
        "SZ_ENV_TEST_" + this.flag.substring(2).toUpperCase());
  }

  @Override
  public List<String> getEnvironmentFallbacks()
  {
    return List.of(
        "SENZING_ENV_FALLBACK_" + this.flag.substring(2).toUpperCase());
  }

  @Override
  public int getMinimumParameterCount()
  {
    return this.minParams;
  }

  @Override
  public int getMaximumParameterCount()
  {
    return this.maxParams;
  }

  @Override
  public boolean isPrimary()
  {
    return false;
  }

  @Override
  public Set<CommandLineOption> getConflicts()
  {
    return Set.of();
  }

  @Override
  public Set<Set<CommandLineOption>> getDependencies()
  {
    return Set.of();
  }

  /**
   * Pass-through processor: returns single-parameter values as the raw {@link
   * String} and multi-parameter values as the {@link List} of strings.
   */
  public static final ParameterProcessor PARAMETER_PROCESSOR
      = (option, params) -> {
        if (option == VERBOSE) {
          if (params.isEmpty()) return Boolean.TRUE;
          return Boolean.valueOf(params.get(0));
        }
        if (params.size() == 1) return params.get(0);
        return params;
      };
}
