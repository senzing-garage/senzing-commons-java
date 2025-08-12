package com.senzing.cmdline;

import java.io.File;
import java.net.InetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Test implementation of {@link CommandLineOption}.
 */
public enum OtherTestOption
    implements CommandLineOption<OtherTestOption, OtherTestOption>
{
  HELP("--help"),
  VERSION("--version"),
  VERBOSE("--verbose"),
  INPUT("--input"),
  OUTPUT("--output");

  OtherTestOption(String cmdLineFlag) {
    this.cmdLineFlag = cmdLineFlag;
  }

  private String cmdLineFlag;

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
    if (this == VERSION || this == HELP) return null;
    String flag = this.getCommandLineFlag();
    return "SENZING_OTHER_" + flag.substring(2).toUpperCase();
  }

  @Override
  public Set<String> getEnvironmentSynonyms() {
    if (this == VERSION || this == HELP) return Collections.emptySet();
    String flag = this.getCommandLineFlag();
    return Collections.singleton("SZ_OTHER_" + flag.substring(2).toUpperCase());
  }

  @Override
  public List<String> getEnvironmentFallbacks() {
    if (this == VERSION || this == HELP) return Collections.emptyList();
    String flag = this.getCommandLineFlag();
    return List.of("SENZING_OTHER_TEST_" + flag.substring(2).toUpperCase());
  }

  @Override
  public boolean isPrimary() {
    switch (this) {
      case HELP:
      case VERSION:
      case INPUT:
        return true;
      default:
        return false;
    }
  }

  @Override
  public Set<CommandLineOption> getConflicts() {
    switch (this) {
      case HELP:
        return Set.of(VERSION, VERBOSE, INPUT, OUTPUT);
      case VERSION:
        return Set.of(HELP, VERBOSE, INPUT, OUTPUT);
      case VERBOSE:
      case INPUT:
      case OUTPUT:
        return Set.of(HELP, VERSION);
      default:
        throw new IllegalStateException("Unrecognized option: " + this);
    }
  }

  @Override
  public Set<Set<CommandLineOption>> getDependencies() {
    switch (this) {
      case HELP:
      case VERSION:
      case VERBOSE:
      case INPUT:
        return Collections.emptySet();
      case OUTPUT:
        return Set.of(Set.of(INPUT));
      default:
        throw new IllegalStateException("Unrecognized option: " + this);
    }
  }

  @Override
  public int getMinimumParameterCount() {
    switch (this) {
      case INPUT:
      case OUTPUT:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public int getMaximumParameterCount() {
    switch (this) {
      case INPUT:
      case OUTPUT:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public List<String> getDefaultParameters() {
    switch (this) {
      case VERBOSE:
        return List.of("false");
      case OUTPUT:
        String param = null;
        return List.of(param);
      default:
        return null;
    }
  }

  /**
   *
   */
  private static class ParamProcessor implements ParameterProcessor
  {
    public Object process(CommandLineOption option,  List<String> params) {
      if (!(option instanceof OtherTestOption)) {
        throw new IllegalArgumentException(
            "Unhandled command-line option: " + option.getCommandLineFlag()
                + " / "+ option);
      }
      OtherTestOption testOption = (OtherTestOption) option;
      switch (testOption) {
        case HELP:
        case VERSION:
          return Boolean.TRUE;

        case VERBOSE:
          if (params.size() == 0) return Boolean.TRUE;
          String boolText = params.get(0);
          if ("false".equalsIgnoreCase(boolText)) {
            return Boolean.FALSE;
          }
          if ("true".equalsIgnoreCase(boolText)) {
            return Boolean.TRUE;
          }
          throw new IllegalArgumentException(
              "The specified parameter for "
                  + option.getCommandLineFlag()
                  + " must be true or false: " + params.get(0));

        case INPUT:
        case OUTPUT: {
          File configFile = new File(params.get(0));
          if (!configFile.exists()) {
            throw new IllegalArgumentException(
                "Specified config file does not exist: " + configFile);
          }
          return configFile;
        }

        default:
          throw new IllegalArgumentException(
              "Unhandled command line option: "
                  + option.getCommandLineFlag()
                  + " / " + option);
      }
    }

    private static int parsePort(String text) {
      int port = Integer.parseInt(text);
      if (port < 0) {
        throw new IllegalArgumentException(
            "Negative port numbers are not allowed: " + port);
      }
      return port;
    }

    private static InetAddress parseInterface(String text) {
      InetAddress addr = null;
      try {
        if ("all".equals(text)) {
          addr = InetAddress.getByName("0.0.0.0");
        } else if ("loopback".equals(text)) {
          addr = InetAddress.getLoopbackAddress();
        } else {
          addr = InetAddress.getByName(text);
        }
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
      return addr;
    }
  }

  /**
   * The parameter processor.
   */
  public static final ParameterProcessor PARAMETER_PROCESSOR
      = new OtherTestOption.ParamProcessor();

}
