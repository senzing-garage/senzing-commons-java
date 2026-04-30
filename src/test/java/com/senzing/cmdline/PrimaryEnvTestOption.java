package com.senzing.cmdline;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Test enum used by {@link CommandLineUtilitiesFallbackTest} to exercise {@code
 * CommandLineUtilities.processEnvironment}'s primary-option fallback path. The
 * single option is marked
 * {@linkplain CommandLineOption#isPrimary() primary} so the
 * fallback-env-var loop runs.
 */
public enum PrimaryEnvTestOption
    implements CommandLineOption<PrimaryEnvTestOption, PrimaryEnvTestOption>
{
  RUN("--run");

  private final String flag;

  PrimaryEnvTestOption(String flag)
  {
    this.flag = flag;
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
    return "SENZING_PRIMARY_TEST_RUN";
  }

  @Override
  public Set<String> getEnvironmentSynonyms()
  {
    return Collections.singleton("SZ_PRIMARY_TEST_RUN");
  }

  @Override
  public List<String> getEnvironmentFallbacks()
  {
    return List.of("SENZING_PRIMARY_FALLBACK_RUN");
  }

  @Override
  public int getMinimumParameterCount()
  {
    return 1;
  }

  @Override
  public int getMaximumParameterCount()
  {
    return 1;
  }

  @Override
  public boolean isPrimary()
  {
    return true;
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
   * Pass-through processor returning the single parameter.
   */
  public static final ParameterProcessor PARAMETER_PROCESSOR
      = (option, params) -> params.get(0);
}
