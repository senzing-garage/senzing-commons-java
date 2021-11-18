package com.senzing.cmdline;

import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.senzing.cmdline.TestOption.*;

/**
 * Test implementation of {@link CommandLineOption}.
 */
public enum ExtendedTestOption
    implements CommandLineOption<ExtendedTestOption, TestOption>
{
  SQS_QUEUE_URL("--sqs"),
  DATABASE_TABLE("--table"),
  DATABASE_PASSWORD("--database-password");

  ExtendedTestOption(String cmdLineFlag) {
    this.cmdLineFlag = cmdLineFlag;
  }

  private String cmdLineFlag;

  @Override
  public Class<TestOption> getBaseOptionType() {
    return TestOption.class;
  }

  @Override
  public String getCommandLineFlag() {
    return this.cmdLineFlag;
  }

  @Override
  public Set<String> getSynonymFlags() {
    String flag = this.getCommandLineFlag();
    return Collections.singleton(flag.substring(1));
  }

  @Override
  public String getEnvironmentVariable() {
    String flag = this.getCommandLineFlag();
    return "SENZING_EXT_TEST_" + flag.substring(2).toUpperCase();
  }

  @Override
  public Set<String> getEnvironmentSynonyms() {
    String flag = this.getCommandLineFlag();
    return Collections.singleton(
        "SZ_EXT_TEST_" + flag.substring(2).toUpperCase());
  }

  @Override
  public List<String> getEnvironmentFallbacks() {
    String flag = this.getCommandLineFlag();
    return List.of("SENZING_ALT_EXT_TEST_" + flag.substring(2).toUpperCase());
  }

  @Override
  public int getMinimumParameterCount() {
    switch (this) {
      case SQS_QUEUE_URL:
      case DATABASE_TABLE:
      case DATABASE_PASSWORD:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public int getMaximumParameterCount() {
    switch (this) {
      case SQS_QUEUE_URL:
      case DATABASE_TABLE:
      case DATABASE_PASSWORD:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public Set<CommandLineOption> getConflicts() {
    switch (this) {
      case SQS_QUEUE_URL:
        return Set.of(DATABASE_TABLE, DATABASE_PASSWORD, HELP, VERSION);
      case DATABASE_TABLE:
        return Set.of(SQS_QUEUE_URL, HELP, VERSION);
      default:
        throw new IllegalStateException("Unrecognized option: " + this);
    }
  }

  @Override
  public Set<Set<CommandLineOption>> getDependencies() {
    switch (this) {
      case DATABASE_PASSWORD:
        return Set.of(Set.of(DATABASE_TABLE));
      case SQS_QUEUE_URL:
      case DATABASE_TABLE:
        return Set.of();
      default:
        throw new IllegalStateException("Unrecognized option: " + this);
    }
  }

  /**
   *
   */
  private static class ParamProcessor implements ParameterProcessor
  {
    public Object process(CommandLineOption option,  List<String> params)
      throws BadOptionParametersException
    {
      if (option instanceof TestOption) {
        return TestOption.PARAMETER_PROCESSOR.process(option, params);
      }

      if (!(option instanceof ExtendedTestOption)) {
        throw new IllegalArgumentException(
            "Unhandled command-line option: " + option.getCommandLineFlag()
                + " / "+ option);
      }

      ExtendedTestOption testOption = (ExtendedTestOption) option;
      switch (testOption) {
        case SQS_QUEUE_URL:
        case DATABASE_TABLE:
          return params.get(0);

        default:
          throw new IllegalArgumentException(
              "Unhandled command line option: "
                  + option.getCommandLineFlag()
                  + " / " + option);
      }
    }
  }

  /**
   * The parameter processor.
   */
  public static final ParameterProcessor PARAMETER_PROCESSOR
      = new ExtendedTestOption.ParamProcessor();

}
