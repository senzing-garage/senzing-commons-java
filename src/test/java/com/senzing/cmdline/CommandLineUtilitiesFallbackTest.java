package com.senzing.cmdline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.util.Map;

import static com.senzing.cmdline.CommandLineUtilities.parseCommandLine;
import static com.senzing.cmdline.PrimaryEnvTestOption.RUN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the primary-option environment-fallback path in
 * {@link CommandLineUtilities#parseCommandLine}, which only
 * activates when the option is marked
 * {@link CommandLineOption#isPrimary() primary}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class CommandLineUtilitiesFallbackTest
{
  /**
   * When a primary option's primary and synonym env vars are unset but its
   * fallback env var is set, the fallback env var value must populate the
   * option as the
   * {@link CommandLineSource#ENVIRONMENT} source.
   */
  @Test
  public void primaryFallbackEnvVarIsConsultedWhenPrimaryAbsent()
      throws Exception
  {
    new EnvironmentVariables(
        "SENZING_PRIMARY_FALLBACK_RUN", "fallback-value")
        .execute(() -> {
          Map<CommandLineOption, CommandLineValue> result
              = parseCommandLine(
                  PrimaryEnvTestOption.class,
                  new String[] {},
                  PrimaryEnvTestOption.PARAMETER_PROCESSOR,
                  null);

          CommandLineValue val = result.get(RUN);
          assertNotNull(val,
                        "Primary option should be populated from fallback"
                            + " env var when primary/synonym are unset");
          assertEquals("SENZING_PRIMARY_FALLBACK_RUN", val.getSpecifier(),
                       "Specifier should be the fallback env var name");
          assertEquals("fallback-value", val.getProcessedValue());
        });
  }

  /**
   * When the primary env var IS set, the fallback path must be skipped —
   * primary takes precedence.
   */
  @Test
  public void primaryEnvVarTakesPrecedenceOverFallback() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_PRIMARY_TEST_RUN", "primary",
        "SENZING_PRIMARY_FALLBACK_RUN", "fallback").execute(() -> {
      Map<CommandLineOption, CommandLineValue> result
          = parseCommandLine(
              PrimaryEnvTestOption.class,
              new String[] {},
              PrimaryEnvTestOption.PARAMETER_PROCESSOR,
              null);

      CommandLineValue val = result.get(RUN);
      assertEquals("primary", val.getProcessedValue(),
                   "Primary env var must take precedence over fallback");
      assertEquals("SENZING_PRIMARY_TEST_RUN", val.getSpecifier());
    });
  }
}
