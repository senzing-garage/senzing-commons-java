package com.senzing.cmdline;

import java.io.File;
import java.net.InetAddress;
import java.util.*;

/**
 * Test implementation of {@link CommandLineOption}.
 */
public enum TestOption implements CommandLineOption<TestOption, TestOption> {
  HELP("--help"),
  VERSION("--version"),
  VERBOSE("--verbose"),
  IGNORE_ENV("--ignore-env"),
  CONFIG("--config"),
  PORT("--port"),
  INTERFACE("--interface"),
  URL("--url"),
  PASSWORD("--password");

  TestOption(String cmdLineFlag) {
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
    flag = flag.substring(1);
    int index = flag.lastIndexOf('-');
    if (index > 0) {
      // handle hyphenated options
      String first = flag.substring(0, index);
      String last = flag.substring(index + 1, index + 2).toUpperCase()
          + flag.substring(index+2);
      flag = first + last;
    }
    return Collections.singleton(flag);
  }

  @Override
  public boolean isDeprecated() {
    return (this == URL);
  }

  @Override
  public Set<TestOption> getDeprecationAlternatives() {
    if (this == URL) {
      return Set.of(PORT, INTERFACE);
    } else {
      return Set.of();
    }
  }

  @Override
  public String getEnvironmentVariable() {
    if (this == VERSION || this == HELP || this == IGNORE_ENV) return null;
    String flag = this.getCommandLineFlag();
    return "SENZING_TEST_" + flag.substring(2).toUpperCase();
  }

  @Override
  public Set<String> getEnvironmentSynonyms() {
    if (this == VERSION || this == HELP || this == IGNORE_ENV) {
      return Collections.emptySet();
    }
    String flag = this.getCommandLineFlag();
    return Collections.singleton("SZ_TEST_" + flag.substring(2).toUpperCase());
  }

  @Override
  public List<String> getEnvironmentFallbacks() {
    if (this == VERSION || this == HELP || this == IGNORE_ENV) {
      return Collections.emptyList();
    }
    String flag = this.getCommandLineFlag();
    return List.of("SENZING_ALT_TEST_" + flag.substring(2).toUpperCase());
  }

  @Override
  public boolean isPrimary() {
    switch (this) {
      case HELP:
      case VERSION:
      case CONFIG:
        return true;
      default:
        return false;
    }
  }

  @Override
  public Set<CommandLineOption> getConflicts() {
    switch (this) {
      case HELP:
        return Set.of(VERSION, VERBOSE, PORT, INTERFACE);
      case VERSION:
        return Set.of(HELP, VERBOSE, PORT, INTERFACE);
      case URL:
        return Set.of(HELP, VERSION, PORT, INTERFACE);
      case VERBOSE:
      case PORT:
      case INTERFACE:
      case CONFIG:
      case PASSWORD:
        return Set.of(HELP, VERSION);
      case IGNORE_ENV:
        return Set.of();
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
      case CONFIG:
        return Collections.emptySet();
      case PASSWORD:
        return Set.of(Set.of(CONFIG, INTERFACE, PORT), Set.of(CONFIG, URL));
      case URL:
        return Set.of(Set.of(CONFIG));
      case PORT:
        return Set.of(Set.of(CONFIG, INTERFACE));
      case INTERFACE:
        return Set.of(Set.of(CONFIG, PORT));
      case IGNORE_ENV:
        return Set.of();
      default:
        throw new IllegalStateException("Unrecognized option: " + this);
    }
  }

  @Override
  public int getMinimumParameterCount() {
    switch (this) {
      case PORT:
      case INTERFACE:
      case CONFIG:
      case URL:
      case PASSWORD:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public int getMaximumParameterCount() {
    switch (this) {
      case PORT:
      case INTERFACE:
      case CONFIG:
      case URL:
      case PASSWORD:
        return 1;
      default:
        return 0;
    }
  }

  @Override
  public List<String> getDefaultParameters() {
    switch (this) {
      case VERBOSE:
      case IGNORE_ENV:
        return List.of("false");
      case INTERFACE:
        return List.of("localhost");
      case URL:
        return List.of("localhost:9080");
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
      if (!(option instanceof TestOption)) {
        throw new IllegalArgumentException(
            "Unhandled command-line option: " + option.getCommandLineFlag()
            + " / "+ option);
      }
      TestOption testOption = (TestOption) option;
      switch (testOption) {
        case HELP:
        case VERSION:
          return Boolean.TRUE;

        case VERBOSE:
        case IGNORE_ENV:
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

        case CONFIG: {
          File configFile = new File(params.get(0));
          return configFile;
        }

        case PORT:
          return parsePort(params.get(0));

        case INTERFACE:
          return parseInterface(params.get(0));

        case URL: {
          String[] tokens = params.get(0).split(":");
          if (tokens.length != 2) {
            throw new IllegalArgumentException(
                "Expected format <interface>:<port>.  Received: "
                    + params.get(0));
          }
          InetAddress address = parseInterface(tokens[0]);
          int port = parsePort(tokens[1]);

          return address + ":"+ port;
        }

        case PASSWORD:
          return params.get(0);

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
      = new ParamProcessor();
}
