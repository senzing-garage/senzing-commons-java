package com.senzing.cmdline;

import com.senzing.util.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.json.*;
import java.io.File;
import java.net.InetAddress;
import java.util.*;

import static com.senzing.cmdline.CommandLineUtilities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static com.senzing.cmdline.CommandLineSource.*;
import static com.senzing.cmdline.TestOption.*;
import static com.senzing.cmdline.ExtendedTestOption.*;

/**
 * Tests for {@link CommandLineUtilities}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class CommandLineUtilitiesTest {
  /**
   * Converts the specified array to a diagnostic {@link String}.
   * @param array The array to convert to a {@link String}.
   * @return A diagnostic {@link String} describing the specified array.
   */
  private static String arrayToString(String[] array) {
    if (array == null) return "null";
    JsonArrayBuilder jab = Json.createArrayBuilder();
    for (int index = 0; index < array.length; index++) {
      jab.add(array[index]);
    }
    JsonArray jsonArray = jab.build();
    return JsonUtils.toJsonText(jsonArray);
  }

  /**
   * Gets the parameters for testing the shift function.
   *
   * @return The {@link List} of arguments to the test.
   */
  public List<Arguments> getShiftArgParameters() {
    List<Arguments> result = new LinkedList<>();

    String[] initial = {"A", "B", "C"};
    result.add(arguments(initial, 0, initial, null));

    String[] shifted = { "B", "C" };
    result.add(arguments(initial, 1, shifted, null));

    shifted = new String[] { "C" };
    result.add(arguments(initial, 2, shifted, null));

    shifted = new String[] { };
    result.add(arguments(initial, 3, shifted, null));

    result.add(arguments(initial, 4, null, TooFewArgumentsException.class));

    result.add(arguments(initial, -1, null, IllegalArgumentException.class));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getShiftArgParameters")
  @SuppressWarnings("unchecked")
  public void shiftArgumentsTest(String[] initialArray,
                                 int      shiftCount,
                                 String[] expectedShift,
                                 Class    exceptionType)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("initialArray=[ ").append(arrayToString(initialArray))
        .append(" ], shiftCount=[ ").append(shiftCount)
        .append(" ], expectedArray=[ ").append(arrayToString(expectedShift))
        .append(" ], expectedException=[ ").append(
            (exceptionType == null) ? "NONE" : exceptionType.getName())
        .append(" ]");
    String testInfo = sb.toString();
    try {
      String[] shifted = shiftArguments(initialArray, shiftCount);

      // check if we succeeded unexpectedly
      if (expectedShift == null) {
        fail("Expected a failure, but succeeded: shifted=[ "
             + arrayToString(shifted) + " ], " + testInfo);
      }

      // check if length is expected
      assertEquals(expectedShift.length, shifted.length,
          "Length of the shifted array (" + shifted.length
                + ") is not the expected length (" + expectedShift.length
                + "): shifted=[ " + arrayToString(shifted) + " ], " + testInfo);

      // check if the elements in the array are as expected
      for (int index = 0; index < shifted.length; index++) {
        assertEquals(
            expectedShift[index], shifted[index],
            "Element in shifted array (" + shifted[index]
                + ") is not the expected value (" + expectedShift[index]
                + ") at index (" + index + "): shifted=[ "
                + arrayToString(shifted) + " ], " + testInfo);
      }

    } catch (Exception e) {
      // check if success was expected
      if (expectedShift != null) {
        fail("Failed shift with exception: " + testInfo, e);

      } else if (exceptionType != null) {
        if (!exceptionType.isAssignableFrom(e.getClass())) {
          fail("Shift failed with unexpected exception type: "
                   + testInfo, e);
        }

      } else {
        fail("Shift test parameters make no sense: " + testInfo);
      }
    }
  }

  /**
   * Gets the parameters for the putValue() test.
   */
  public List<Arguments> getPutValueParameters() {
    List<Arguments> result = new LinkedList<>();

    // basic option value add
    Map<CommandLineOption, CommandLineValue> optionMap = new LinkedHashMap<>();
    CommandLineValue value = new CommandLineValue(
        COMMAND_LINE, PORT, PORT.getCommandLineFlag(), 9080, List.of("9080") );
    result.add(arguments(optionMap, value, null));

    // adding a duplicated value
    optionMap = new LinkedHashMap<>();
    optionMap.put(value.getOption(), value);
    result.add(arguments(optionMap, value, RepeatedOptionException.class));

    // adding a default conflict (should not conflict)
    optionMap = new LinkedHashMap<>();
    value = new CommandLineValue(COMMAND_LINE,
                                 HELP,
                                 HELP.getCommandLineFlag(),
                                 null,
                                 Collections.emptyList());

    optionMap.put(value.getOption(), value);
    value = new CommandLineValue(DEFAULT,
                                 VERBOSE,
                                 false,
                                 List.of("false"));
    result.add(arguments(optionMap, value, null));

    // add conflicting values
    optionMap = new LinkedHashMap<>();
    value = new CommandLineValue(COMMAND_LINE,
                                 PORT,
                                 PORT.getCommandLineFlag(),
                                 9080,
                                 List.of("9080"));

    optionMap.put(value.getOption(), value);
    value = new CommandLineValue(COMMAND_LINE,
                                 VERSION,
                                 VERSION.getCommandLineFlag(),
                                 null,
                                 Collections.emptyList());
    result.add(arguments(optionMap, value, ConflictingOptionsException.class));

    // add conflicting values with extended option classes
    optionMap = new LinkedHashMap<>();
    value = new CommandLineValue(COMMAND_LINE,
                                 VERSION,
                                 VERSION.getCommandLineFlag(),
                                 null,
                                 Collections.emptyList());

    optionMap.put(value.getOption(), value);
    value = new CommandLineValue(COMMAND_LINE,
                                 DATABASE_TABLE,
                                 DATABASE_TABLE.getCommandLineFlag(),
                                 "SOME_TABLE",
                                 List.of("SOME_TABLE"));
    result.add(arguments(optionMap, value, ConflictingOptionsException.class));

    // add conflicting values with extended option classes
    optionMap = new LinkedHashMap<>();
    value = new CommandLineValue(COMMAND_LINE,
                                 DATABASE_TABLE,
                                 DATABASE_TABLE.getCommandLineFlag(),
                                 "SOME_TABLE",
                                 List.of("SOME_TABLE"));

    optionMap.put(value.getOption(), value);
    value = new CommandLineValue(COMMAND_LINE,
                                 VERSION,
                                 VERSION.getCommandLineFlag(),
                                 null,
                                 Collections.emptyList());

    result.add(arguments(optionMap, value, ConflictingOptionsException.class));

    // adding an option and an extended option
    optionMap = new LinkedHashMap<>();
    value = new CommandLineValue(COMMAND_LINE,
                                 PORT,
                                 PORT.getCommandLineFlag(),
                                 9080,
                                 List.of("9080"));

    optionMap.put(value.getOption(), value);
    value = new CommandLineValue(COMMAND_LINE,
                                 DATABASE_TABLE,
                                 DATABASE_TABLE.getCommandLineFlag(),
                                 "SOME_TABLE",
                                 List.of("SOME_TABLE"));
    result.add(arguments(optionMap, value, null));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getPutValueParameters")
  @SuppressWarnings("unchecked")
  public void putValueTest(
      Map<CommandLineOption, CommandLineValue>  optionMap,
      CommandLineValue                          value,
      Class                                     exceptionType)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("optionMap=[ ").append(optionMap)
        .append(" ], commandLineValue=[ ").append(value)
        .append(" ], expectedException=[ ").append(
            (exceptionType == null) ? "NONE" : exceptionType.getName())
        .append(" ]");
    String testInfo = sb.toString();

    try {
      putValue(optionMap, value);
      if (exceptionType != null) {
        fail("Calling putMap() succeeded when it should have failed: "
             + testInfo);
      }

      assertTrue(optionMap.containsKey(value.getOption()),
                 "Option map does not contain option after putValue()");

      CommandLineValue contained = optionMap.get(value.getOption());
      assertEquals(value.getOption(), contained.getOption(),
                   "Unpexected option for value after putValue()");
      assertEquals(value.getSource(), contained.getSource(),
          "Unexpected source for value after putValue()");
      assertEquals(value.getSpecifier(), contained.getSpecifier(),
                   "Unexpected specifier for value after putValue()");
      assertEquals(value.getProcessedValue(), contained.getProcessedValue(),
                   "Unexpected processed value after putValue()");
      assertEquals(value.getParameters(), contained.getParameters(),
                   "Unexpected parameters for value after putValue()");

    } catch (Exception e) {
      if (exceptionType == null) {
        fail("Unexpected exception during putValue(): " + testInfo, e);
      }
      if (!exceptionType.isAssignableFrom(e.getClass())) {
        fail("Calling putMap() failed with unexpected exception type ("
             + e.getClass().getName() + "): " + testInfo, e);
      }
    }
  }

  /**
   * Gets the parameters for the putValue() test.
   */
  public List<Arguments> getLookupParameters() {
    List<Arguments> result = new LinkedList<>();

    for (TestOption option : TestOption.values()) {
      result.add(
          arguments(TestOption.class, option.getCommandLineFlag(), option));
      for (String synonymFlag : option.getSynonymFlags()) {
        result.add(arguments(TestOption.class, synonymFlag, option));
      }
    }
    for (ExtendedTestOption option: ExtendedTestOption.values()) {
      result.add(arguments(
          ExtendedTestOption.class, option.getCommandLineFlag(), option));
      for (String synonymFlag : option.getSynonymFlags()) {
        result.add(arguments(ExtendedTestOption.class, synonymFlag, option));
      }
    }

    result.add(arguments(TestOption.class, "XXXXX", null));
    result.add(arguments(ExtendedTestOption.class, "XXXXX", null));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getLookupParameters")
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    void lookupTest(
      Class<T>          optionClass,
      String            flag,
      CommandLineOption expectedValue)
  {

    StringBuilder sb = new StringBuilder();
    sb.append("flag=[ ").append(flag)
        .append(" ], expected=[ ").append(expectedValue)
        .append(" ]");
    String testInfo = sb.toString();

    try {
      CommandLineOption option = (CommandLineOption) lookup(optionClass, flag);

      assertEquals(expectedValue, option,
                   "Lookup returned an unexpected value for flag: "
                    + testInfo);

    } catch (Exception e) {
      fail("Unexpected exception during lookup(): " + testInfo, e);
    }
  }

  /**
   * Gets the parameters for the validate options test.
   *
   * @return The {@link List} of arguments.
   */
  public List<Arguments> getValidateOptionsParameters() {
    List<Arguments> result = new LinkedList<>();

    Map<CommandLineOption, CommandLineValue> optionMap = null;

    // try a basic --help option map
    optionMap = Map.of(HELP, new CommandLineValue(COMMAND_LINE,
                                                  HELP,
                                                  HELP.getCommandLineFlag(),
                                                  null,
                                                  Collections.emptyList()));
    result.add(arguments(TestOption.class, optionMap, null, null));
    result.add(arguments(ExtendedTestOption.class, optionMap, null, null));

    // try a basic --version option map
    optionMap = Map.of(VERSION, new CommandLineValue(COMMAND_LINE,
                                                     VERSION,
                                                     VERSION.getCommandLineFlag(),
                                                    null,
                                                     Collections.emptyList()));
    result.add(arguments(TestOption.class, optionMap, null, null));
    result.add(arguments(ExtendedTestOption.class, optionMap, null, null));


    // try a key/value mismatch
    optionMap = Map.of(HELP, new CommandLineValue(COMMAND_LINE,
                                                  VERSION,
                                                  VERSION.getCommandLineFlag(),
                                                  null,
                                                  Collections.emptyList()));
    result.add(arguments(TestOption.class,
                         optionMap,
                         null,
                         IllegalArgumentException.class));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         IllegalArgumentException.class));

    // validating an option with unrelated CommandLineOption types
    optionMap = Map.of(PORT,
                       new CommandLineValue(COMMAND_LINE,
                                            PORT,
                                            PORT.getCommandLineFlag(),
                                            9080,
                                            List.of("9080")),
                       OtherTestOption.INPUT,
                       new CommandLineValue(
                           COMMAND_LINE,
                           OtherTestOption.INPUT,
                           OtherTestOption.INPUT.getCommandLineFlag(),
                           new File("some-file.txt"),
                           List.of("some-file.txt")));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         IllegalArgumentException.class));

    // check for missing primary options
    optionMap = Map.of(URL,
                       new CommandLineValue(COMMAND_LINE,
                                            URL,
                                            URL.getCommandLineFlag(),
                                            "localhost:9080",
                                            List.of("localhost:9080")));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         NoPrimaryOptionException.class));

    // check for deprecation warnings via command-line
    optionMap = Map.of(CONFIG,
                       new CommandLineValue(COMMAND_LINE,
                                            CONFIG,
                                            CONFIG.getCommandLineFlag(),
                                            new File("test.conf"),
                                            List.of("test.conf")),
                       URL,
                       new CommandLineValue(COMMAND_LINE,
                                            URL,
                                            URL.getCommandLineFlag(),
                                            "localhost:1234",
                                            List.of("localhost:1234")));

    result.add(arguments(TestOption.class,
                         optionMap,
                         List.of(
                             new DeprecatedOptionWarning(
                                 COMMAND_LINE,
                                 URL,
                                 URL.getCommandLineFlag())),
                         null));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         List.of(
                             new DeprecatedOptionWarning(
                                 COMMAND_LINE,
                                 URL,
                                 URL.getCommandLineFlag())),
                         null));

    // check for deprecation warnings via environment
    optionMap = Map.of(CONFIG,
                       new CommandLineValue(COMMAND_LINE,
                                            CONFIG,
                                            CONFIG.getCommandLineFlag(),
                                            new File("test.conf"),
                                            List.of("test.conf")),
                       URL,
                       new CommandLineValue(ENVIRONMENT,
                                            URL,
                                            URL.getEnvironmentVariable(),
                                            "localhost:1234",
                                            List.of("localhost:1234")));

    result.add(arguments(TestOption.class,
                         optionMap,
                         List.of(
                             new DeprecatedOptionWarning(
                                 ENVIRONMENT,
                                 URL,
                                 URL.getEnvironmentVariable())),
                         null));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         List.of(
                             new DeprecatedOptionWarning(
                                 ENVIRONMENT,
                                 URL,
                                 URL.getEnvironmentVariable())),
                         null));

    // check for NO deprecation warnings when via default value
    optionMap = Map.of(CONFIG,
                       new CommandLineValue(COMMAND_LINE,
                                            CONFIG,
                                            CONFIG.getCommandLineFlag(),
                                            new File("test.conf"),
                                            List.of("test.conf")),
                       URL,
                       new CommandLineValue(DEFAULT,
                                            URL,
                                            null,
                                            URL.getDefaultParameters().get(0),
                                            URL.getDefaultParameters()));

    result.add(arguments(TestOption.class, optionMap, null, null));
    result.add(arguments(ExtendedTestOption.class, optionMap, null, null));

    // try a conflicting option map
    optionMap = Map.of(
        HELP, new CommandLineValue(COMMAND_LINE,
                                   HELP,
                                   HELP.getCommandLineFlag(),
                                   null,
                                   Collections.emptyList()),
        VERSION, new CommandLineValue(COMMAND_LINE,
                                      VERSION,
                                      VERSION.getCommandLineFlag(),
                                      null,
                                      Collections.emptyList()));

    result.add(arguments(TestOption.class,
                         optionMap,
                         null,
                         ConflictingOptionsException.class));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         ConflictingOptionsException.class));

    // add conflicting values with extended option classes
    optionMap = Map.of(
        VERSION,
        new CommandLineValue(COMMAND_LINE,
                             VERSION,
                             VERSION.getCommandLineFlag(),
                             null,
                             Collections.emptyList()),
        DATABASE_TABLE,
        new CommandLineValue(COMMAND_LINE,
                             DATABASE_TABLE,
                             DATABASE_TABLE.getCommandLineFlag(),
                             "SOME_TABLE",
                             List.of("SOME_TABLE")));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         ConflictingOptionsException.class));

    // add conflicting values with extended option classes
    optionMap = Map.of(
        DATABASE_TABLE,
        new CommandLineValue(COMMAND_LINE,
                             DATABASE_TABLE,
                             DATABASE_TABLE.getCommandLineFlag(),
                             "SOME_TABLE",
                             List.of("SOME_TABLE")),
        VERSION,
        new CommandLineValue(COMMAND_LINE,
                             VERSION,
                             VERSION.getCommandLineFlag(),
                             null,
                             Collections.emptyList()));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         ConflictingOptionsException.class));

    // add options with missing dependencies (PORT without INTERFACE)
    // NOTE: normally interface would have a default value, but we are
    // purposely excluding it from the map of option values
    optionMap = Map.of(
        CONFIG,
        new CommandLineValue(COMMAND_LINE,
                             CONFIG,
                             "test.conf",
                             List.of("test.conf")),
        PORT,
        new CommandLineValue(COMMAND_LINE,
                             PORT,
                             PORT.getCommandLineFlag(),
                             9080,
                             List.of("9080")));

    result.add(arguments(TestOption.class,
                         optionMap,
                         null,
                         MissingDependenciesException.class));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         MissingDependenciesException.class));

    // add options with missing dependencies (INTERFACE without PORT)
    optionMap = Map.of(
        CONFIG,
        new CommandLineValue(COMMAND_LINE,
                             CONFIG,
                             "test.conf",
                             List.of("test.conf")),
        INTERFACE,
        new CommandLineValue(COMMAND_LINE,
                             INTERFACE,
                             INTERFACE.getCommandLineFlag(),
                             "localhost",
                             List.of("localhost")));

    result.add(arguments(TestOption.class,
                         optionMap,
                         null,
                         MissingDependenciesException.class));

    result.add(arguments(ExtendedTestOption.class,
                         optionMap,
                         null,
                         MissingDependenciesException.class));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getValidateOptionsParameters")
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    void validateOptionsTest(
      Class<T>                                  optionClass,
      Map<CommandLineOption, CommandLineValue>  optionMap,
      List<DeprecatedOptionWarning>             deprecatedWarnings,
      Class<?>                                  exceptionType)
  {

    StringBuilder sb = new StringBuilder();
    sb.append("optionClass=[ ").append(optionClass)
        .append(" ], optionMap=[ ").append(optionMap)
        .append(" ], deprecationWarnings=[ ").append(deprecatedWarnings)
        .append(" ], expectedException=[ ").append(
            (exceptionType == null) ? "NONE" : exceptionType.getName())
        .append(" ]");
    String testInfo = sb.toString();

    try {
      List<DeprecatedOptionWarning> deprecations
          = validateOptions(optionClass, optionMap);

      // ceck if an exception was expected
      if (exceptionType != null) {
        fail("Successful option validation when a failure was expected: "
             + testInfo);
      }

      assertEquals(deprecatedWarnings, deprecations,
                   "Unexpected deprecation warning results: "
                       + testInfo);

    } catch (Exception e) {
      // check if no exception was expected
      if (exceptionType == null) {
        e.printStackTrace();
        fail("Failed option validation unexpectedly: " + testInfo, e);
      }

      // check if the wrong exception was thrown
      if (!exceptionType.isAssignableFrom(e.getClass())) {
        fail("Failed option validation with unexpected exception type ("
                 + e.getClass().getName() + "): " + testInfo, e);
      }
    }
  }

  /**
   * @return The {@link List} of arguments to the test.
   */
  public List<Arguments> getParseCommandLineParameters() {
    List<Arguments> result = new LinkedList<>();
    List emptyList = List.of();

    Map<CommandLineOption, CommandLineValue> defaultMap = Map.of(
      VERBOSE,
      new CommandLineValue(
          DEFAULT, VERBOSE, false, List.of("false")),
      IGNORE_ENV,
      new CommandLineValue(
          DEFAULT, IGNORE_ENV, false, List.of("false")),
      INTERFACE,
      new CommandLineValue(
          DEFAULT, INTERFACE, InetAddress.getLoopbackAddress(),
          List.of("localhost")),
      URL,
      new CommandLineValue(DEFAULT,
                           URL,
                           "localhost/127.0.0.1:9080",
                           List.of("localhost:9080")));

    Object[][] ignoreVariants
        = { {null, null}, {true, null}, {null, IGNORE_ENV} };

    for (Object[] ignoreOpts: ignoreVariants) {
      String[] args = { "--help" };
      Map<CommandLineOption, CommandLineValue> expectedResult
          = new LinkedHashMap<>(defaultMap);
      expectedResult.put(HELP,
                         new CommandLineValue(COMMAND_LINE,
                                              HELP,
                                              "--help",
                                              Boolean.TRUE,
                                              List.of()));

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           emptyList,
                           null));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           emptyList,
                           null));

      args = new String[] { "-help" };

      expectedResult = new LinkedHashMap<>(defaultMap);
      expectedResult.put(HELP,
                         new CommandLineValue(COMMAND_LINE,
                                              HELP,
                                              "-help",
                                              Boolean.TRUE,
                                              List.of()));

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           emptyList,
                           null));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           emptyList,
                           null));

      args = new String[] { "--help", "--version" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           ConflictingOptionsException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           ConflictingOptionsException.class));

      args = new String[] { "-help", "-version" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           ConflictingOptionsException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           ConflictingOptionsException.class));

      args = new String[] { "--port", "9080", "--interface", "localhost" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           NoPrimaryOptionException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           NoPrimaryOptionException.class));

      args = new String[] { "--port", "9080", "5080", "--interface", "localhost" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParameterCountException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParameterCountException.class));

      args = new String[] { "--port", "9080", "--interface" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParameterCountException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParameterCountException.class));

      args = new String[] {"--config", "test.conf", "--url", "localhost:9080"};

      expectedResult = new LinkedHashMap<>(defaultMap);
      expectedResult.put(
          CONFIG,
          new CommandLineValue(COMMAND_LINE,
                               CONFIG,
                               "--config",
                               new File("test.conf"),
                               List.of("test.conf")));
      expectedResult.put(
          URL,
          new CommandLineValue(COMMAND_LINE,
                               URL,
                               "--url",
                               "localhost/127.0.0.1:9080",
                               List.of("localhost:9080")));

      List<DeprecatedOptionWarning> expectedWarnings = List.of(
              new DeprecatedOptionWarning(COMMAND_LINE, URL, "--url"));

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           expectedWarnings,
                           null));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           expectedWarnings,
                           null));

      args = new String[] {
          "--config", "test.conf", "--interface", "localhost" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           MissingDependenciesException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           MissingDependenciesException.class));

      args = new String[] { "--config", "test.conf", "--url", "localhost:AB12" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParametersException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           BadOptionParametersException.class));

      args = new String[] { "--config", "test1.conf", "-config", "test2.conf" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           RepeatedOptionException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           RepeatedOptionException.class));

      args = new String[] { "--config", "test.conf", "--input", "input.json" };

      result.add(arguments(TestOption.class,
                           args,
                           TestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           UnrecognizedOptionException.class));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           null,
                           emptyList,
                           UnrecognizedOptionException.class));

      args = new String[] { "--config", "test.conf", "--table", "RECORDS" };

      expectedResult = new LinkedHashMap<>(defaultMap);
      expectedResult.put(
          CONFIG,
          new CommandLineValue(COMMAND_LINE,
                               CONFIG,
                               "--config",
                               new File("test.conf"),
                               List.of("test.conf")));
      expectedResult.put(
          DATABASE_TABLE,
          new CommandLineValue(COMMAND_LINE,
                               DATABASE_TABLE,
                               "--table",
                               "RECORDS",
                               List.of("RECORDS")));

      result.add(arguments(ExtendedTestOption.class,
                           args,
                           ExtendedTestOption.PARAMETER_PROCESSOR,
                           ignoreOpts[0],
                           ignoreOpts[1],
                           expectedResult,
                           emptyList,
                           null));
    }

    return result;
  }

  @ParameterizedTest
  @MethodSource("getParseCommandLineParameters")
  public <T extends Enum<T> & CommandLineOption<T, B>,
      B extends Enum<B> & CommandLineOption<B, ?>>
    void parseCommandLineTest(
      Class<T>                                  optionClass,
      String[]                                  args,
      ParameterProcessor                        processor,
      Boolean                                   ignoreEnv,
      CommandLineOption                         ignoreEnvOption,
      Map<CommandLineOption, CommandLineValue>  expectedResult,
      List<DeprecatedOptionWarning>             expectedWarnings,
      Class<?>                                  expectedException)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("optionClass=[ ").append(optionClass)
        .append(" ], args=[ ").append(arrayToString(args))
        .append(" ], processor=[ ").append(processor.getClass().getName())
        .append(" ], ignoreEnv=[ ").append(ignoreEnv)
        .append(" ], ignoreEnvOption=[ ").append(ignoreEnvOption)
        .append(" ], expectedResult=[ ").append(expectedResult)
        .append(" ], expectedWarnings=[ ").append(expectedWarnings)
        .append(" ], expectedException=[ ").append(
          (expectedException == null) ? "NONE" : expectedException.getName())
        .append(" ]");

    String testInfo = sb.toString();

    try {
      // check for invalid test parameters
      if (ignoreEnv != null && ignoreEnvOption != null) {
        throw new IllegalStateException(
            "INVALID TEST.  Cannot specify both 'ignoreEnv' and "
            + "'ignoreEnvOption': " + testInfo);
      }

      // declare the result
      Map<CommandLineOption, CommandLineValue> map = null;
      List<DeprecatedOptionWarning> list = new LinkedList<>();

      // determine which parseCommandLine() function to call
      if (ignoreEnv == null && ignoreEnvOption == null) {
        map = parseCommandLine(optionClass, args, processor, list);

      } else if (ignoreEnv == null) {
        map = parseCommandLine(
            optionClass, args, processor, ignoreEnvOption, list);

      } else {
        map = parseCommandLine(optionClass, args, processor, ignoreEnv, list);
      }

      // check if an exception was expected
      if (expectedException != null) {
        fail("the top command-line parse when a failure was expected: "
                 + testInfo);
      }

      assertEquals(expectedResult, map,
                   "Unexpected command-line parse results: "
                       + testInfo);

      assertEquals(expectedWarnings, list,
                   "Unexpected deprecation warnings: " + testInfo);

    } catch (Exception e) {
      // check if no exception was expected
      if (expectedException == null) {
        e.printStackTrace();
        fail("Failed command-line parse unexpectedly: " + testInfo, e);
      }

      // check if the wrong exception was thrown
      if (!expectedException.isAssignableFrom(e.getClass())) {
        fail("Failed command-line parse with unexpected exception "
                 + "type (" + e.getClass().getName() + "): " + testInfo, e);
      }
    }
  }

  /**
   * @return The {@link List} of arguments to the test.
   */
  public List<Arguments> getProcessCommandLineParameters() {
    List<Arguments> result = new LinkedList<>();

    Map<CommandLineOption, CommandLineValue> defaultMap = Map.of(
        VERBOSE,
        new CommandLineValue(
            DEFAULT, VERBOSE, false, List.of("false")),
        IGNORE_ENV,
        new CommandLineValue(
            DEFAULT, IGNORE_ENV, false, List.of("false")),
        INTERFACE,
        new CommandLineValue(
            DEFAULT, INTERFACE, InetAddress.getLoopbackAddress(),
            List.of("localhost")),
        URL,
        new CommandLineValue(DEFAULT,
                             URL,
                             "localhost/127.0.0.1:9080",
                             List.of("localhost:9080")));

    Map<CommandLineOption, Object> processedDefaults = Map.of(
        VERBOSE, false,
        IGNORE_ENV, false,
        INTERFACE, InetAddress.getLoopbackAddress(),
        URL, "localhost/127.0.0.1:9080");

    Map<CommandLineOption, CommandLineValue> optionValues
        = new LinkedHashMap<>(defaultMap);
    optionValues.put(HELP,
                     new CommandLineValue(COMMAND_LINE,
                                          HELP,
                                          "--help",
                                          Boolean.TRUE,
                                          List.of()));

    Map<CommandLineOption, Object> expectedResult
        = new LinkedHashMap<>(processedDefaults);
    expectedResult.put(HELP, true);

    JsonObject expectedJson = JsonUtils.parseJsonObject(
        "{\"VERBOSE\": "
        + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
        + "\"IGNORE_ENV\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
        + "\"INTERFACE\": "
            + "{\"value\": \"localhost\", \"source\": \"DEFAULT\"},"
        + "\"URL\": "
            + "{\"value\": \"localhost:9080\", \"source\": \"DEFAULT\"},"
        + "\"HELP\": "
            + "{\"values\": [], \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"--help\"}}");

    result.add(arguments(optionValues,
                         expectedResult,
                         null,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         true));

    optionValues = new LinkedHashMap<>(defaultMap);
    optionValues.put(HELP,
                     new CommandLineValue(COMMAND_LINE,
                                          HELP,
                                          "-help",
                                          Boolean.TRUE,
                                          List.of()));

    expectedJson = JsonUtils.parseJsonObject(
        "{\"VERBOSE\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"IGNORE_ENV\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"INTERFACE\": "
            + "{\"value\": \"localhost\", \"source\": \"DEFAULT\"},"
            + "\"URL\": "
            + "{\"value\": \"localhost:9080\", \"source\": \"DEFAULT\"},"
            + "\"HELP\": "
            + "{\"values\": [], \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"-help\"}}");

    result.add(arguments(optionValues,
                         expectedResult,
                         null,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         true));

    optionValues = new LinkedHashMap<>(defaultMap);
    optionValues.put(
        CONFIG,
        new CommandLineValue(COMMAND_LINE,
                             CONFIG,
                             "--config",
                             new File("test.conf"),
                             List.of("test.conf")));
    optionValues.put(
        URL,
        new CommandLineValue(COMMAND_LINE,
                             URL,
                             "--url",
                             "localhost/127.0.0.1:9080",
                             List.of("localhost:9080")));
    optionValues.put(
        PASSWORD,
        new CommandLineValue(ENVIRONMENT,
                             PASSWORD,
                             "SENZING_TEST_PASSWORD",
                             "secret",
                             List.of("secret")));

    expectedResult = new LinkedHashMap<>(processedDefaults);
    expectedResult.put(CONFIG, new File("test.conf"));
    expectedResult.put(URL, "localhost/127.0.0.1:9080");
    expectedResult.put(PASSWORD, "secret");

    expectedJson = JsonUtils.parseJsonObject(
        "{\"VERBOSE\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"IGNORE_ENV\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"INTERFACE\": "
            + "{\"value\": \"localhost\", \"source\": \"DEFAULT\"},"
            + "\"URL\": "
            + "{\"value\": \"localhost:9080\", \"source\": \"DEFAULT\"},"
            + "\"CONFIG\": "
            + "{\"value\": \"test.conf\", \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"--config\"},"
            + "\"URL\": "
            + "{\"value\": \"localhost:9080\", \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"--url\"},"
            + "\"PASSWORD\": "
            + "{\"value\": \"" + REDACTED_SENSITIVE_VALUE
            + "\", \"source\": \"ENVIRONMENT\","
            + "\"via\": \"SENZING_TEST_PASSWORD\"}}");

    result.add(arguments(optionValues,
                         expectedResult,
                         null,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         true));

    optionValues = new LinkedHashMap<>(defaultMap);
    optionValues.put(
        CONFIG,
        new CommandLineValue(COMMAND_LINE,
                             CONFIG,
                             "--config",
                             new File("test.conf"),
                             List.of("test.conf")));
    optionValues.put(
        DATABASE_TABLE,
        new CommandLineValue(COMMAND_LINE,
                             DATABASE_TABLE,
                             "--table",
                             "RECORDS",
                             List.of("RECORDS")));

    expectedResult = new LinkedHashMap<>(processedDefaults);
    expectedResult.put(CONFIG, new File("test.conf"));
    expectedResult.put(DATABASE_TABLE, "RECORDS");

    expectedJson = JsonUtils.parseJsonObject(
        "{\"VERBOSE\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"IGNORE_ENV\": "
            + "{\"value\": \"false\", \"source\": \"DEFAULT\"},"
            + "\"INTERFACE\": "
            + "{\"value\": \"localhost\", \"source\": \"DEFAULT\"},"
            + "\"URL\": "
            + "{\"value\": \"localhost:9080\", \"source\": \"DEFAULT\"},"
            + "\"CONFIG\": "
            + "{\"value\": \"test.conf\", \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"--config\"},"
            + "\"DATABASE_TABLE\": "
            + "{\"value\": \"RECORDS\", \"source\": "
            + "\"COMMAND_LINE\", \"via\": \"--table\"}}");

    result.add(arguments(optionValues,
                         expectedResult,
                         null,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         false));

    result.add(arguments(optionValues,
                         expectedResult,
                         expectedJson,
                         true));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getProcessCommandLineParameters")
  public void processCommandLineTest(
      Map<CommandLineOption, CommandLineValue>  optionValues,
      Map<CommandLineOption, Object>            expectedResult,
      JsonObject                                expectedJson,
      boolean                                   expectText)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("optionValues=[ ").append(optionValues)
        .append(" ], expectedResult=[ ").append(expectedResult)
        .append(" ], expectedJson=[ ").append(expectedJson)
        .append(" ], expectText=[ ").append(expectText)
        .append(" ]");

    String testInfo = sb.toString();

    try {
      Map<CommandLineOption, Object>  resultMap   = new LinkedHashMap<>();
      String                          resultJson  = null;
      JsonObjectBuilder               resultJob   = Json.createObjectBuilder();

      if (expectedJson == null) {
        processCommandLine(optionValues, resultMap);

      } else if (!expectText) {
        processCommandLine(optionValues, resultMap, resultJob);

      } else {
        sb = new StringBuilder();
        processCommandLine(optionValues, resultMap, resultJob, sb);
        resultJson = sb.toString();
      }

      assertEquals(expectedResult, resultMap,
                   "Unexpected command-line processing results: "
                       + testInfo);

      if (expectedJson != null) {
        JsonObject jsonObject = resultJob.build();
        assertEquals(expectedJson, jsonObject,
                     "Unexpected JSON object result: " + testInfo);

        if (expectText) {
          jsonObject = JsonUtils.parseJsonObject(resultJson);

          assertEquals(expectedJson, jsonObject,
                       "Unexpected JSON text result: " + testInfo);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed command-line parse unexpectedly: " + testInfo, e);
    }
  }

  public List<Arguments> getSensitiveTestParameters() {
    List<Arguments> result = new LinkedList<>();
    result.add(arguments(PORT, false));
    result.add(arguments(CONFIG, false));
    result.add(arguments(INTERFACE, false));
    result.add(arguments(URL, false));
    result.add(arguments(PASSWORD, true));
    result.add(arguments(DATABASE_PASSWORD, true));
    return result;
  }

  @ParameterizedTest
  @MethodSource("getSensitiveTestParameters")
  public <T extends CommandLineOption> void sensitiveTest(T       option,
                                                          boolean sensitive)
  {
    try {
      assertEquals(sensitive, option.isSensitive(),
                   "Option sensitivity not as expected.");

    } catch (Exception e) {
      e.printStackTrace();
      fail("Failed sensitive option test: " + option);
    }
  }
}
