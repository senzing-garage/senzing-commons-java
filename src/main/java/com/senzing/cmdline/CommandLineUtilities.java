package com.senzing.cmdline;

import com.senzing.util.JsonUtilities;

import javax.json.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

import static com.senzing.cmdline.CommandLineSource.*;

/**
 * Utility functions for parsing command line arguments.
 *
 */
@SuppressWarnings("unchecked")
public class CommandLineUtilities {
  /**
   * The value to use in the JSON representation of the parsed command-line
   * parameters when a {@linkplain CommandLineOption#isSensitive() sensitive}
   * option is encountered.
   */
  public static final String REDACTED_SENSITIVE_VALUE = "********";

  /***
   * The Regular Expression pattern for JSON arrays.
   */
  private static final Pattern JSON_ARRAY_PATTERN
      = Pattern.compile(
          "\\s*\\[\\s*((\\\".*\\\"\\s*,\\s*)+"
          + "(\\\".*\\\")|(\\\".*\\\")?)\\s*\\]\\s*");

  /**
   * The JAR file name containing this class.
   */
  public static final String JAR_FILE_NAME;

  /**
   * The base URL of the JAR file containing this class.
   */
  public static final String JAR_BASE_URL;

  /**
   * The URL path to the JAR file containing this class.
   */
  public static final String PATH_TO_JAR;

  static {
    String jarBaseUrl = null;
    String jarFileName = null;
    String pathToJar = null;

    try {
      Class<CommandLineUtilities> cls = CommandLineUtilities.class;

      String url = cls.getResource(
          cls.getSimpleName() + ".class").toString();

      if (url.indexOf(".jar") >= 0) {
        int index = url.lastIndexOf(
            cls.getName().replace(".", "/") + ".class");
        jarBaseUrl = url.substring(0, index);

        index = jarBaseUrl.lastIndexOf("!");
        if (index >= 0) {
          url = url.substring(0, index);
          index = url.lastIndexOf("/");

          if (index >= 0) {
            jarFileName = url.substring(index + 1);
          }

          url = url.substring(0, index);
          index = url.indexOf("/");
          pathToJar = url.substring(index);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw new ExceptionInInitializerError(e);

    } finally {
      JAR_BASE_URL = jarBaseUrl;
      JAR_FILE_NAME = jarFileName;
      PATH_TO_JAR = pathToJar;
    }
  }

  /**
   * Private default constructor.
   */
  private CommandLineUtilities() {
    // do nothing
  }

  /**
   * Returns a new {@link String} array that contains the same elements as
   * the specified array except for the first N arguments where N is the
   * specified count.
   *
   * @param args  The array of command line arguments.
   * @param count The number of arguments to shift.
   * @return The shifted argument array.
   * @throws IllegalArgumentException If the specified count is negative.
   * @throws TooFewArgumentsException If there are not enough arguments for
   *                                  the shift.
   */
  public static String[] shiftArguments(String[] args, int count)
  {
    if (count < 0) {
      throw new IllegalArgumentException(
          "The specified count cannot be negative: " + count);
    }
    if ((args.length - count) < 0) {
      throw new TooFewArgumentsException(
          "The specified shift count cannot be greater than the array length.  "
          + "arrayLength=[ " + args.length + " ], shiftCount=[ " + count
          + " ]");
    }
    String[] args2 = new String[args.length - count];
    for (int index = 0; index < args2.length; index++) {
      args2[index] = args[index + count];
    }
    return args2;
  }

  /**
   * Stores the option and its value(s) in the specified option map, first
   * checking to ensure that the option is NOT already specified and
   * checking if the option has any conflicts.  If the specified option is
   * deprecated then a warning message is printed.
   *
   * @param optionMap The {@link Map} to put the option in.
   * @param value the {@link CommandLineValue} to add to the {@link Map}.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   *
   * @throws CommandLineException If the specified {@link CommandLineValue}
   *                              cannot be put in the specified {@link Map}
   *                              due to a validation problem.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
      void putValue(Map<CommandLineOption, CommandLineValue>  optionMap,
                    CommandLineValue                          value)
    throws CommandLineException
  {
    CommandLineOption option = value.getOption();
    Set<CommandLineOption> optionKeys = optionMap.keySet();
    CommandLineValue prevValue = optionMap.get(option);
    if (prevValue != null) {
      Set<String> flags = new HashSet<>();
      if (value.getSource() == COMMAND_LINE) {
        flags.add(value.getSpecifier());
      }
      if (prevValue.getSource() == COMMAND_LINE) {
        flags.add(prevValue.getSpecifier());
      }
      throw new RepeatedOptionException(option, flags);
    }

    // check for conflicts if the value is NOT a defaulted value
    if (!checkNoArgFalse(value) && (value.getSource() != DEFAULT)) {
      Set<CommandLineOption> conflicts = option.getConflicts();
      for (CommandLineOption<?, ?> opt : optionKeys) {
        // get the reverse conflicts in case not symmetrical
        Set<CommandLineOption> revConflicts = opt.getConflicts();

        // check for conflicts
        if ((conflicts != null && conflicts.contains(opt))
            || ((revConflicts != null && revConflicts.contains(option))))
        {
          // get the command-line value
          CommandLineValue conflictValue = optionMap.get(opt);

          // no conflict if the other is a defaulted value
          if (conflictValue.getSource() == DEFAULT) continue;

          // no conflict if the other is a no-arg option with a false value
          if (checkNoArgFalse(conflictValue)) continue;

          throw new ConflictingOptionsException(conflictValue, value);
        }
      }
    }

    // put it in the option map
    optionMap.put(option, value);
  }

  /**
   * Checks if the specified {@link CommandLineValue} is a zero or
   * single-valued boolean flag and the value is false.
   *
   * @param cmdLineValue The {@link CommandLineValue} to check.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    boolean checkNoArgFalse(CommandLineValue cmdLineValue)
  {
    CommandLineOption option = cmdLineValue.getOption();
    // special-case no-arg boolean flags
    int minParamCount = option.getMinimumParameterCount();
    int maxParamCount = option.getMaximumParameterCount();
    List<String> defaultParams = option.getDefaultParameters();
    List<String> actualParams = cmdLineValue.getParameters();
    return (minParamCount == 0 && maxParamCount >= 0
            && defaultParams != null
            && defaultParams.size() == 1
            && "false".equalsIgnoreCase(defaultParams.get(0))
            && actualParams != null
            && actualParams.size() == 1
            && "false".equalsIgnoreCase(actualParams.get(0)));
  }

  /**
   * Returns the enumerated {@link CommandLineOption} value associated with
   * the specified command line flag and enumerated {@link CommandLineOption}
   * class.
   *
   * @param enumClass The {@link Class} for the {@link CommandLineOption}
   *                  implementation.
   * @param commandLineFlag The command line flag.
   *
   * @return The enumerated {@link CommandLineOption} or <code>null</code> if
   *         not found.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
      T lookup(Class<T> enumClass, String commandLineFlag)
  {
    // just iterate to find it rather than using a lookup map given that
    // enums are usually not more than a handful of values
    EnumSet<T> enumSet = EnumSet.allOf(enumClass);
    for (T enumVal : enumSet) {
      if (enumVal.getCommandLineFlag().equals(commandLineFlag)) {
        return enumVal;
      }
      if (enumVal.getSynonymFlags().contains(commandLineFlag)) {
        return enumVal;
      }
    }
    return null;
  }

  /**
   * Checks if a primary option is required.
   *
   * @param enumClass The enum class for the {@link CommandLineOption}
   *                  implementation.
   *
   * @return <code>true</code> if a primary option is required, otherwise
   *         <code>false</code>.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    boolean checkPrimaryRequired(Class<T> enumClass)
  {
    // check if we need a primary option
    boolean primaryRequired = false;
    EnumSet<T> enumSet = EnumSet.allOf(enumClass);
    for (T enumVal : enumSet) {
      if (enumVal.isPrimary()) {
        return true;
      }
    }

    // get the base type
    Class baseType
        = ((CommandLineOption) enumSet.iterator().next()).getBaseOptionType();

    // check the base type
    if (baseType != null && Enum.class.isAssignableFrom(baseType)) {
      return checkPrimaryRequired(baseType);
    }

    // return false if we get here
    return false;
  }

  /**
   * Checks if a primary option is required.
   *
   * @param enumClass The enum class for the {@link CommandLineOption}
   *                  implementation.
   *
   * @return <code>true</code> if a primary option is required, otherwise
   *         <code>false</code>.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    Set<CommandLineOption> getPrimaryOptions(Class<T> enumClass)
  {
    // create the result
    Set<CommandLineOption> result = new LinkedHashSet<>();

    populatePrimaryOptions(result, enumClass);

    return result;
  }

  /**
   * Checks if a primary option is required.
   *
   * @param enumClass The enum class for the {@link CommandLineOption}
   *                  implementation.
   *
   * @return <code>true</code> if a primary option is required, otherwise
   *         <code>false</code>.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    void populatePrimaryOptions(Set<CommandLineOption> set, Class<T> enumClass)
  {
    // check if we need a primary option
    EnumSet<T> enumSet = EnumSet.allOf(enumClass);
    for (T enumVal : enumSet) {
      if (enumVal.isPrimary()) {
        set.add(enumVal);
      }
    }

    // get the base type
    Class baseType
        = ((CommandLineOption) enumSet.iterator().next()).getBaseOptionType();

    // check the base type
    if (baseType != null && Enum.class.isAssignableFrom(baseType)) {
      populatePrimaryOptions(set,baseType);
    }
  }

  /**
   * Gets the ordered {@link Set} describing the type chain for the specified
   * command line option class.
   *
   * @param enumClass The starting class for the type chain.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    Set<Class<? extends CommandLineOption>> getTypeChain(Class<T> enumClass)
  {
    Set<Class<? extends CommandLineOption>> result = new LinkedHashSet<>();
    populateTypeChain(result, enumClass);
    return result;
  }

  /**
   * Populates the type chain for the specified command line option class in
   * the specified {@link Set}.
   *
   * @param set The {@link} set to populate.
   * @param enumClass The starting class for the type chain.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    void populateTypeChain(Set<Class<? extends CommandLineOption>>  set,
                           Class<T>                                 enumClass)
  {
    if (enumClass == null) return;
    set.add(enumClass);

    EnumSet<T>  enumSet   = EnumSet.allOf(enumClass);
    Class       baseType  = enumSet.iterator().next().getBaseOptionType();

    if (baseType != null) {
      populateTypeChain(set, baseType);
    }
  }

  /**
   * Populates the options chain for the specified command line option class in
   * the specified {@link Set}.
   *
   * @param set The {@link} set to populate.
   * @param enumClass The starting class for the options chain.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    void populateOptionsChain(Set<CommandLineOption>  set,
                              Class<T>                enumClass)
  {
    if (enumClass == null) return;
    EnumSet<T> enumSet = EnumSet.allOf(enumClass);
    Class baseType = null;
    for (T option : enumSet) {
      set.add((CommandLineOption) option);
      if (baseType == null) baseType = option.getBaseOptionType();
    }

    if (baseType != null) {
      populateOptionsChain(set, baseType);
    }
  }

  /**
   * Validates the specified {@link Set} of specified {@link CommandLineOption}
   * instances and ensures that they logically make sense together.  This
   * checks for the existing of at least one primary option (if primary options
   * exist), ensures there are no conflicts and that all dependencies are
   * satisfied.
   *
   * @param enumClass The {@link Class} object identifying the enumerated type
   *                  that implements {@link CommandLineOption}.
   * @param optionValues The {@link Map} of {@link CommandLineOption} keys to
   *                     {@link CommandLineValue} values.
   *
   * @return A {@link List} of {@link DeprecatedOptionWarning} instances
   *         describing the deprecation warnings (if any), or <code>null</code>
   *         if there are no deprecation warnings.
   *
   * @throws IllegalArgumentException If the specified options are invalid
   *                                  together.
   * @throws CommandLineException If the specified command-line options fail
   *                              validation.
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    List<DeprecatedOptionWarning> validateOptions(
        Class<T>                                  enumClass,
        Map<CommandLineOption, CommandLineValue>  optionValues)
    throws IllegalArgumentException, CommandLineException
  {
    List<DeprecatedOptionWarning> deprecatedList = new LinkedList<>();

    // create the set of legal option classes
    Set<Class<? extends CommandLineOption>> typeChainSet
        = getTypeChain(enumClass);

    // check the types in the key set
    optionValues.forEach((option, optionValue) -> {
      if (option != optionValue.getOption()) {
        throw new IllegalArgumentException(
            "Mismatch on option values key/value pair.  The option key does "
            + "not the associated CommandLineValue's option.  optionKey=[ "
            + option + " ], optionValue=[ " + optionValue.getOption() + " ]");
      }

      if (!typeChainSet.contains(option.getClass())) {
        throw new IllegalArgumentException(
            "The specified option values map contains an option of an illegal "
                + "type given the specified CommandLineOption type.  found=[ "
                + option + " ], foundType=[ " + option.getClass()
                + " ], expected=[ " + typeChainSet + " ]");
      }
    });

    // check if we need a primary option
    Set<CommandLineOption> primaryOptions = getPrimaryOptions(enumClass);
    boolean primaryRequired = primaryOptions.size() > 0;

    if (primaryRequired) {
      // if primary option is required then check for at least one
      int primaryCount = 0;
      for (CommandLineOption option : optionValues.keySet()) {
        // check if primary
        if (option.isPrimary()) {
          primaryCount++;
        }
      }
      if (primaryCount == 0) {
        throw new NoPrimaryOptionException(primaryOptions);
      }
    }

    // check for conflicts and dependencies
    for (CommandLineValue cmdLineValue : optionValues.values()) {
      // check if this is a default value
      if (cmdLineValue.getSource() == DEFAULT) continue;

      // get the option
      CommandLineOption option = (CommandLineOption) cmdLineValue.getOption();

      // check for deprecation
      if (option.isDeprecated()) {
        deprecatedList.add(new DeprecatedOptionWarning(cmdLineValue));
      }

      // check for conflicts
      Set<CommandLineOption> conflicts = option.getConflicts();

      // get the dependencies
      Set<Set<CommandLineOption>> dependencies = option.getDependencies();
      if (conflicts != null) {
        for (CommandLineOption conflict : conflicts) {
          // skip if the same option -- cannot conflict with itself
          if (option == conflict) continue;

          // check if the conflict is present
          CommandLineValue conflictValue = optionValues.get(conflict);
          if (conflictValue != null) {
            if (conflictValue.getSource() == DEFAULT) continue;

            throw new ConflictingOptionsException(cmdLineValue, conflictValue);
          }
        }
      }

      boolean satisfied = (dependencies == null || dependencies.size() == 0);
      if (!satisfied) {
        for (Set<CommandLineOption> dependencySet : dependencies) {
          if (optionValues.keySet().containsAll(dependencySet)) {
            satisfied = true;
            break;
          }
        }
      }
      if (!satisfied) {
        throw new MissingDependenciesException(cmdLineValue.getSource(),
                                               cmdLineValue.getOption(),
                                               cmdLineValue.getSpecifier(),
                                               optionValues.keySet());
      }
    }

    // return the list of deprecation warnings
    return (deprecatedList.size() == 0) ? null : deprecatedList;
  }

  /**
   * Creates a lookup map of {@link String} command-line flags to the
   * {@link CommandLineOption} values that they map to.  This includes any
   * synonyms for the options if they exist.
   *
   * @param enumClass The enumerated class for the {@link CommandLineOption}.
   */
  private static Map<String, CommandLineOption> createFlagLookupMap(Class enumClass)
  {
    // get all the options
    EnumSet<?> enumSet = EnumSet.allOf((Class<Enum>) enumClass);

    // create a lookup map for the flags
    Map<String, CommandLineOption> lookupMap = new LinkedHashMap<>();
    for (Enum<?> optionEnum : enumSet) {
      // cast to a CommandLineOption
      CommandLineOption option = (CommandLineOption) optionEnum;

      // get the flag
      String flag = option.getCommandLineFlag();

      // do a sanity check
      if (lookupMap.containsKey(flag)) {
        throw new IllegalStateException(
            "Command-line flag (" + flag + ") cannot resolve to different "
                + "options (" + lookupMap.get(flag) + " and " + option
                + ").  It must be unique.");
      }

      // add the primary flag to the lookup map
      lookupMap.put(flag, option);

      // add the synonym flags if any
      Set<String> synonymFlags = option.getSynonymFlags();
      if (synonymFlags == null) synonymFlags = Collections.emptySet();
      for (String synonym : synonymFlags) {
        // check the synonym against the primary flag (sanity check)
        if (synonym.equals(flag)) {
          throw new IllegalStateException(
              "Synonym command-line (" + flag + ") for option (" + option
                  + ") is the same as the primary flag.");
        }

        // do a sanity check
        if (lookupMap.containsKey(synonym)) {
          throw new IllegalStateException(
              "Command-line flag (" + flag + ") cannot resolve to different "
                  + "options (" + lookupMap.get(flag) + " and " + option
                  + ").  It must be unique.");

        }

        // add to the lookup map
        lookupMap.put(synonym, option);
      }
    }

    // get the base type
    Class baseType
        = ((CommandLineOption) enumSet.iterator().next()).getBaseOptionType();

    // check if not null
    if (baseType != null) {
      Map<String, CommandLineOption> baseMap = createFlagLookupMap(baseType);

      baseMap.keySet().forEach(flag -> {
        if (lookupMap.containsKey(flag)) {
          CommandLineOption option      = lookupMap.get(flag);
          CommandLineOption baseOption  = baseMap.get(flag);

          throw new IllegalStateException(
              "Command-line flag (" + flag + ") for " + option + " in "
                  + enumClass + " conflicts with " + baseOption + " in the "
                  + baseType + " base option class.");
        }
      });

      // add the base map entries
      lookupMap.putAll(baseMap);
    }

    // return the lookup map
    return lookupMap;
  }

  /**
   * Parses the command line arguments and returns a {@link Map} of those
   * arguments.  This function will attempt to find values for command-line
   * options that are not explicitly specified on the command line by using
   * environment variables for those options have environment variable
   * equivalents defined.
   *
   * @param enumClass The enumerated {@link CommandLineOption} class.
   * @param args The arguments to parse.
   * @param processor The {@link ParameterProcessor} to use for handling the
   *                  parameters to the options.
   * @param deprecationWarnings The {@link List} to be populated with any
   *                            {@link DeprecatedOptionWarning} instances that
   *                            are generated, or <code>null</code> if the
   *                            caller is not interested in deprecation
   *                            warnings.
   *
   * @return The {@link Map} to populate with the result of the parsing.
   *
   * @throws CommandLineException If a command-line option parsing error occurs.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    Map<CommandLineOption, CommandLineValue> parseCommandLine(
        Class<T>                                  enumClass,
        String[]                                  args,
        ParameterProcessor                        processor,
        List<DeprecatedOptionWarning>             deprecationWarnings)
      throws CommandLineException
  {
    return parseCommandLine(enumClass,
                            args,
                            processor,
                            false,
                            null,
                            deprecationWarnings);
  }

  /**
   * Parses the command line arguments and returns a {@link Map} of those
   * arguments.  Depending on the specified value for
   * <code>ignoreEnvironment</code> this function will optionally attempt to
   * find values for command-line options that are not explicitly specified on
   * the command line by using environment variables for those options have
   * environment variable equivalents defined.
   *
   * @param enumClass The enumerated {@link CommandLineOption} class.
   * @param args The arguments to parse.
   * @param processor The {@link ParameterProcessor} to use for handling the
   *                  parameters to the options.
   * @param ignoreEnvironment Flag indicating if the environment variables
   *                          should be ignored in the processing.
   * @param deprecationWarnings The {@link List} to be populated with any
   *                            {@link DeprecatedOptionWarning} instances that
   *                            are generated, or <code>null</code> if the
   *                            caller is not interested in deprecation
   *                            warnings.
   *
   * @return The {@link Map} to populate with the result of the parsing.
   *
   * @throws CommandLineException If a command-line option parsing error occurs.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    Map<CommandLineOption, CommandLineValue> parseCommandLine(
        Class<T>                                  enumClass,
        String[]                                  args,
        ParameterProcessor                        processor,
        boolean                                   ignoreEnvironment,
        List<DeprecatedOptionWarning>             deprecationWarnings)
    throws CommandLineException
  {
    return parseCommandLine(enumClass,
                            args,
                            processor,
                            false,
                            null,
                            deprecationWarnings);
  }

  /**
   * Parses the command line arguments and returns a {@link Map} of those
   * arguments.  This function will optionally attempt to find values for
   * command-line options that are not explicitly specified on the command line
   * by using environment variables for those options have environment variable
   * equivalents defined.  Use of the environment is disabled if the
   * <code>ignoreEnvOption</code> parameter is non-null and that option is present
   * in the explicitly specified command-line arguments and either has no
   * parameter value or any value other than <code>false</code>.
   *
   * @param enumClass The enumerated {@link CommandLineOption} class.
   * @param args The arguments to parse.
   * @param processor The {@link ParameterProcessor} to use for handling the
   *                  parameters to the options.
   * @param ignoreEnvOption The optional command-line option value that if
   *                        present with no value or present with any value
   *                        other than <code>false</code> will cause the environment
   *                        to be ignored in processing.
   *
   * @param deprecationWarnings The {@link List} to be populated with any
   *                            {@link DeprecatedOptionWarning} instances that
   *                            are generated, or <code>null</code> if the
   *                            caller is not interested in deprecation
   *                            warnings.
   *
   * @return The {@link Map} to populate with the result of the parsing.
   *
   * @throws CommandLineException If a command-line option parsing error occurs.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  public static <T extends Enum<T> & CommandLineOption<T, B>,
                 B extends Enum<B> & CommandLineOption<B, ?>>
    Map<CommandLineOption, CommandLineValue> parseCommandLine(
        Class<T>                                  enumClass,
        String[]                                  args,
        ParameterProcessor                        processor,
        CommandLineOption                         ignoreEnvOption,
        List<DeprecatedOptionWarning>             deprecationWarnings)
      throws CommandLineException
  {
    return parseCommandLine(enumClass,
                            args,
                            processor,
                            false,
                            ignoreEnvOption,
                            deprecationWarnings);
  }

  /**
   * Parses the command line arguments and returns a {@link Map} of those
   * arguments.
   *
   * @param enumClass The enumerated {@link CommandLineOption} class.
   * @param args The arguments to parse.
   * @param processor The {@link ParameterProcessor} to use for handling the
   *                  parameters to the options.
   * @param ignoreEnvironment Flag indicating if the environment variables
   *                          should be ignored in the processing.
   * @param ignoreEnvOption The option to trigger ignoring the environment.
   *
   * @param deprecationWarnings The {@link List} to be populated with any
   *                            {@link DeprecatedOptionWarning} instances that
   *                            are generated, or <code>null</code> if the
   *                            caller is not interested in deprecation
   *                            warnings.
   *
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link
   *         CommandLineValue} values that are the result of the parsing.
   *
   * @throws CommandLineException If the specified command line arguments fail
   *                              to parse.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    Map<CommandLineOption, CommandLineValue> parseCommandLine(
        Class<T>                                  enumClass,
        String[]                                  args,
        ParameterProcessor                        processor,
        boolean                                   ignoreEnvironment,
        CommandLineOption                         ignoreEnvOption,
        List<DeprecatedOptionWarning>             deprecationWarnings)
    throws CommandLineException
  {
    Map<CommandLineOption, CommandLineValue> result = new LinkedHashMap<>();

    // create a lookup map for the flags to their options
    Map<String, CommandLineOption> lookupMap = createFlagLookupMap(enumClass);

    // iterate over the args and build a map
    Map<CommandLineOption, String> usedFlags = new LinkedHashMap<>();
    for (int index = 0; index < args.length; index++) {
      // get the next flag
      String flag = args[index];

      // determine the option from the flag
      CommandLineOption option = lookupMap.get(flag);

      // check if the option is recognized
      if (option == null) {
        throw new UnrecognizedOptionException(
            flag, "Unrecognized command line option: " + flag);
      }

      // check if the option has already been specified
      String usedFlag = usedFlags.get(option);
      if (usedFlag != null) {
        Set<String> flags = Set.of(flag, usedFlag);
        throw new RepeatedOptionException(option, flags);
      }

      // get the option parameters
      int minParamCount = option.getMinimumParameterCount();
      int maxParamCount = option.getMaximumParameterCount();
      if (maxParamCount >= 0 && maxParamCount < minParamCount) {
        throw new IllegalStateException(
            "The non-negative maximum parameter count is less than the minimum "
            + "parameter count.  min=[ " + minParamCount + " ], max=[ "
            + maxParamCount + " ], option=[ " + option + " ]");
      }
      List<String> params = new ArrayList<>(maxParamCount<0 ? 5:maxParamCount);
      if (minParamCount > 0) {
        // check if there are enough parameters
        int max = index + minParamCount;
        for (index++; index <= max; index++) {
          if (index >= args.length) {
            throw new BadOptionParameterCountException(
                COMMAND_LINE, option, flag, params);
          }

          // add the parameter to the list
          params.add(args[index]);
        }

        // back up the index by one so when we advance it later it is correct
        index--;
      }

      // get the default parameters
      List<String> defaultParams = option.getDefaultParameters();

      // check if we have a zero-argument parameter
      if (minParamCount == 0 && maxParamCount == 0
          && defaultParams != null
          && defaultParams.size() == 1
          && "false".equalsIgnoreCase(defaultParams.get(0)))
      {
        // allow a zero-argument parameter to be followed by "true" or "false"
        if (args.length > (index + 1)) {
          // we have arguments after this one -- check the next one
          String param = args[index+1].trim().toLowerCase();

          // check if it is a parameter or a command-line flag
          if (!param.startsWith("-")) {
            // looks like a parameter since a flag starts with "-"
            switch (param) {
              case "true":
              case "false":
                params.add(args[++index].trim().toLowerCase());
                break;
              default:
                throw new BadOptionParametersException(
                    COMMAND_LINE, option, flag, List.of(param),
                    "The " + flag + " command line option can be specified "
                    + "with no parameters, but if a parameter is provided it "
                    + "must be \"true\" or \"false\": " + args[index + 1]);
            }
          }
        }

      } else {
        // get the parameters from the command line
        int bound = (maxParamCount < 0)
            ? args.length
            : index + (maxParamCount - minParamCount) + 1;
        if (bound > args.length) bound = args.length;
        for (int nextIndex = index + 1;
             nextIndex < bound && !lookupMap.containsKey(args[nextIndex]);
             nextIndex++)
        {
          params.add(args[nextIndex]);
          index++;
        }

        // check if there are more (unexpected) parameters
        for (int nextIndex = index + 1;
             (nextIndex < args.length && !args[nextIndex].startsWith("-")
              && !lookupMap.containsKey(args[index]));
             nextIndex++)
        {
          params.add(args[nextIndex]);
          index++;
        }

        // check if too many parameters
        if (maxParamCount >= 0 && params.size() > maxParamCount) {
          throw new BadOptionParameterCountException(
              COMMAND_LINE, option, flag, params);
        }
      }

      defaultParams = option.getDefaultParameters();

      // check if we have a zero-parameter option with a "false" default
      if (minParamCount == 0 && maxParamCount == 0 && params.size() == 0
          && defaultParams != null
          && defaultParams.size() == 1
          && "false".equalsIgnoreCase(defaultParams.get(0)))
      {
        params.add("true");
      }

      // process the parameters
      Object processedValue = processValue(
          COMMAND_LINE, option, flag, processor, params);

      // create the command-line value
      CommandLineValue cmdLineVal = new CommandLineValue(COMMAND_LINE,
                                                         option,
                                                         flag,
                                                         processedValue,
                                                         params);

      // add to the options map
      putValue(result, cmdLineVal);
    }

    // optionally process the environment
    ignoreEnvironment
        = (ignoreEnvironment
          || (result.containsKey(ignoreEnvOption)
              && (!Boolean.FALSE.equals(result.get(ignoreEnvOption)))));
    if (!ignoreEnvironment) {
      try {
        processEnvironment(enumClass, processor, result);
      } catch (NullPointerException e) {
        e.printStackTrace();
        throw e;
      }
    }

    try {
      // handle setting the default values
      processDefaults(enumClass, processor, result);
    } catch (NullPointerException e) {
      e.printStackTrace();
      throw e;
    }

    // validate the options
    try {
      List<DeprecatedOptionWarning> warnings
          = validateOptions(enumClass, result);
      if (deprecationWarnings != null && warnings != null) {
        deprecationWarnings.addAll(warnings);
      }
      return result;

    } catch (CommandLineException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Handles processing the specified parameters for the specified option.
   *
   * @param option The relevant option.
   * @param processor The {@link ParameterProcessor}, or <code>null</code> if none.
   * @param params The {@link List} of {@link String} parameters.
   * @return The processed value.
   * @throws BadOptionParametersException If the parameter values cannot be
   *                                      processed without an error.
   */
  private static Object processValue(
      CommandLineSource   source,
      CommandLineOption   option,
      String              specifier,
      ParameterProcessor  processor,
      List<String>        params)
      throws BadOptionParametersException
  {
    // process the parameters
    if (processor != null) {
      // handle the case where a processor is defined for the parameters
      Object value = null;
      try {
        value = processor.process(option, params);
      } catch (Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println();
        pw.println("Bad parameters for " + option.getCommandLineFlag()
                       + " option:");
        for (String p : params) {
          pw.println("    o " + p);
        }
        pw.println();
        pw.println(e.getMessage());
        pw.flush();
        throw new BadOptionParametersException(
            source, option, specifier, params, sw.toString());
      }
      return value;

    } else if (params.size() == 0) {
      // handle the case of no parameters and no parameter processor
      return null;

    } else if (params.size() == 1) {
      // handle the case of one parameter and no parameter processor
      return params.get(0);

    } else {
      // handle the case of multiple parameters and no parameter processor
      return params.toArray();
    }
  }

  /**
   * Check for command-line option values in the environment.
   * @param enumClass The {@link Class} for the enumerated type that
   *                  implements {@link CommandLineOption}.
   * @param processor The {@link ParameterProcessor} to use.
   * @param optionValues The {@link Map} of {@link CommandLineOption} keys
   *                     to {@link CommandLineValue} values.
   * @throws CommandLineException If a command-line option processing error
   *                              occurs.
   *
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    void processEnvironment(
        Class<T>                                  enumClass,
        ParameterProcessor                        processor,
        Map<CommandLineOption, CommandLineValue>  optionValues)
    throws CommandLineException
  {
    Set<CommandLineOption> options = new LinkedHashSet<>();
    populateOptionsChain(options, enumClass);

    processEnvironment(options,
                       processor,
                       optionValues,
                       false);

    // create a set if fallback options to check
    Set<CommandLineOption> fallbackOptions = new LinkedHashSet<>();

    // check fallbacks for primary options if not specified otherwise
    Set<CommandLineOption> primaryOptions = getPrimaryOptions(enumClass);
    for (CommandLineOption primaryOption : primaryOptions) {
      // check if not contained
      if (!optionValues.containsKey(primaryOption)) {
        List<String> fallbacks = primaryOption.getEnvironmentFallbacks();
        if (fallbacks != null && fallbacks.size() > 0) {
          fallbackOptions.add(primaryOption);
        }
      }
    }

    Set<? extends CommandLineOption>  optionKeys      = optionValues.keySet();
    for (CommandLineOption option : optionKeys) {
      // get the dependency sets
      Set<Set<CommandLineOption>> dependencySets = option.getDependencies();

      // check if no dependencies
      if (dependencySets == null) continue;
      if (dependencySets.size() == 0) continue;

      // set the flag indicating the dependencies are not satisfied
      boolean satisfied = false;

      // iterate over the dependency sets and see if at least one is satisfied
      for (Set<CommandLineOption> dependencySet: dependencySets) {
        if (optionValues.keySet().containsAll(dependencySet)) {
          satisfied = true;
          break;
        }
      }

      // check if the option is not satisfied
      if (!satisfied) {
        // find the first dependency set with missing items with fallbacks
        for (Set<CommandLineOption> dependencySet: dependencySets) {
          // set the fallback count to zero and create the missing set
          int                     fallbackCount = 0;
          Set<CommandLineOption>  missingSet    = new LinkedHashSet<>();

          // iterate over the options
          for (CommandLineOption dependency: dependencySet) {
            // check if dependency is missing
            if (optionValues.containsKey(dependency)) continue;

            // add to the missing set
            missingSet.add(dependency);

            // check if the missing item has fallbacks
            List<String> fallbacks = dependency.getEnvironmentFallbacks();
            if (fallbacks != null && fallbacks.size() > 0) fallbackCount++;
          }

          // check if the fallback count and missing count are equal and if
          // so then use this dependency set
          if (missingSet.size() > 0 && missingSet.size() == fallbackCount) {
            fallbackOptions.addAll(missingSet);
            break;
          }
        }
      }
    }

    // check the fallback options
    if (fallbackOptions.size() > 0) {
      processEnvironment(fallbackOptions, processor, optionValues, true);
    }
  }

  /**
   * Checks for command-line option values in the environment.
   *
   * @throws CommandLineException If the specified environment command line
   *                              arguments are illegal.
   */
  private static void processEnvironment(
      Set<? extends CommandLineOption>          enumSet,
      ParameterProcessor                        processor,
      Map<CommandLineOption, CommandLineValue>  optionValues,
      boolean                                   fallBacks)
    throws CommandLineException
  {
    // get the environment
    Map<String, String> env = System.getenv();

    // iterate over the options
    for (CommandLineOption option: enumSet) {
      // prefer explicit command-line arguments, so skip if already present
      if (optionValues.containsKey(option)) continue;

      List<String> envVars = null;
      if (fallBacks) {
        // get the fallbacks
        envVars = option.getEnvironmentFallbacks();
        if (envVars == null) envVars = Collections.emptyList();

      } else {
        // get the environment variable and its synonyms
        String      envVar      = option.getEnvironmentVariable();
        Set<String> synonymSet  = option.getEnvironmentSynonyms();
        envVars = new ArrayList<>(synonymSet==null ? 1 : synonymSet.size() + 1);
        if (envVar != null) envVars.add(envVar);
        if (synonymSet != null) envVars.addAll(synonymSet);
      }

      // iterate over the items in the list
      String envVal   = null;
      String foundVar = null;
      for (String envVar: envVars) {
        // check if null
        if (envVar == null) continue;

        // check if contained in the environment
        envVal = env.get(envVar);
        if (envVal != null) {
          foundVar = envVar;
          break;
        }
      }

      // check if no value
      if (envVal == null) continue;

      // process the env value
      int minParamCount = option.getMinimumParameterCount();
      int maxParamCount = option.getMaximumParameterCount();
      if (maxParamCount > 0 && maxParamCount < minParamCount) {
        throw new IllegalStateException(
            "The non-negative maximum parameter count is less than the minimum "
                + "parameter count.  min=[ " + minParamCount + " ], max=[ "
                + maxParamCount + " ], option=[ " + option + " ]");
      }
      List<String> params = null;

      // check the number of parameters
      if (minParamCount == 0 && maxParamCount == 0) {
        // no parameters, these are boolean on/off options
        switch (envVal.trim().toLowerCase()) {
          case "":
          case "true":
            params = List.of("true");
            break;
          case "false":
            params = List.of("false");
            break;
          default:
            throw new IllegalArgumentException(
                "The specified value for the " + foundVar
                + " environment variable can only be \"true\""
                + " or \"false\": " + foundVar);
        }

      } else if (minParamCount <= 1 && maxParamCount == 1) {
        // handle the single parameter case
        params = List.of(envVal);

      } else if ((maxParamCount > 1 || maxParamCount < 0)
                 && (JSON_ARRAY_PATTERN.matcher(envVal).matches()))
      {
        // handle the case of multiple parameters and a JSON array
        JsonArray jsonArray = JsonUtilities.parseJsonArray(envVal.trim());
        params = new ArrayList<>(jsonArray.size());
        for (JsonString jsonString: jsonArray.getValuesAs(JsonString.class)) {
          params.add(jsonString.getString());
        }

      } else if (maxParamCount > 1 || maxParamCount < 0) {
        // handle the case of multiple parameters, splitting on commas & spaces
        String[] tokens = envVal.split("(\\s*,\\s*|\\s+)");
        params = Arrays.asList(tokens);
      }

      // get the parameter count
      int paramCount = params.size();

      // check the parameter count
      if (paramCount != 1 || minParamCount != 0 || maxParamCount != 0) {
        // handle the options with parameters
        if (paramCount < minParamCount) {
          throw new BadOptionParameterCountException(
              ENVIRONMENT, option, foundVar, params);
        }
        if ((maxParamCount >= 0) && (paramCount > maxParamCount)) {
          throw new BadOptionParameterCountException(
              ENVIRONMENT, option, foundVar, params);
        }
      }

      // process the parameters
      Object processedValue = processValue(
          ENVIRONMENT, option, foundVar, processor, params);

      // create the command line value
      CommandLineValue cmdLineVal = new CommandLineValue(ENVIRONMENT,
                                                         option,
                                                         foundVar,
                                                         processedValue,
                                                         params);

      // put the value
      putValue(optionValues, cmdLineVal);
    }
  }

  /**
   * Check for command-line option values in the environment.
   *
   * @param enumClass The option enum class that implements {@link
   *                  CommandLineOption}.
   * @param processor The {@link ParameterProcessor} for processing the
   *                  parameters.
   * @param optionValues The {@link Map} of {@link CommandLineOption} keys to
   *                     {@link CommandLineValue} values.
   *
   * @throws CommandLineException If a command-line option processing error
   *                              occurs.
   * @param <T> The enumerated type that implements {@link CommandLineOption}.
   * @param <B> The base enumerated type that the command-line options extend,
   *            <b>OR</b> the same as type <code>T</code> if the command-line
   *            option type has no base and returns <code>null</code> from
   *            {@link CommandLineOption#getBaseOptionType()}.
   */
  private static <T extends Enum<T> & CommandLineOption<T, B>,
                  B extends Enum<B> & CommandLineOption<B, ?>>
    void processDefaults(Class<T>                                  enumClass,
                         ParameterProcessor                        processor,
                         Map<CommandLineOption, CommandLineValue>  optionValues)
    throws CommandLineException
  {
    Set<CommandLineOption> options = new LinkedHashSet<>();
    populateOptionsChain(options, enumClass);

    // iterate over the options
    for (CommandLineOption option: options) {
      // prefer explicit command-line arguments, so skip if already present
      if (optionValues.containsKey(option)) continue;

      // get the default parameters
      List<String> params = option.getDefaultParameters();

      // check if there are none
      if (params == null || params.size() == 0) continue;

      // process the parameters
      Object processedValue = processValue(
          DEFAULT, option, null, processor, params);

      // create the command line value
      CommandLineValue cmdLineVal = new CommandLineValue(DEFAULT,
                                                         option,
                                                         processedValue,
                                                         params);

      // put the value
      putValue(optionValues, cmdLineVal);
    }
  }

  /**
   * Processes the {@link Map} of {@link CommandLineOption} keys to {@link
   * CommandLineValue} values and produces the {@link Map} of {@link
   * CommandLineOption} keys to {@linkplain CommandLineValue#getProcessedValue()
   * processed values}.
   *
   * @param optionValues The {@link Map} of option keys to {@link
   *                     CommandLineValue} values.
   * @param processedValues The {@link Map} to populate with the processed
   *                        values if specified, if <code>null</code> a new {@link
   *                        Map} is created and returned.
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link Object}
   *         values describing the parameters for the option.
   */
  public static Map<CommandLineOption, Object> processCommandLine(
      Map<CommandLineOption, CommandLineValue>  optionValues,
      Map<CommandLineOption, Object>            processedValues)
  {
    return processCommandLine(optionValues,
                              processedValues,
                              null,
                              null);
  }

  /**
   * Processes the {@link Map} of {@link CommandLineOption} keys to {@link
   * CommandLineValue} values and produces the {@link Map} of {@link
   * CommandLineOption} keys to {@linkplain CommandLineValue#getProcessedValue()
   * processed values}.
   *
   * @param optionValues The {@link Map} of option keys to {@link
   *                     CommandLineValue} values.
   * @param processedValues The {@link Map} to populate with the processed
   *                        values if specified, if <code>null</code> a new {@link
   *                        Map} is created and returned.
   * @param jsonBuilder If not <code>null</code> then this {@link JsonObjectBuilder}
   *                    is populated with startup option information.
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link Object}
   *         values describing the parameters for the option.
   */
  public static Map<CommandLineOption, Object> processCommandLine(
      Map<CommandLineOption, CommandLineValue>  optionValues,
      Map<CommandLineOption, Object>            processedValues,
      JsonObjectBuilder                         jsonBuilder)
  {
    return processCommandLine(optionValues,
                              processedValues,
                              jsonBuilder,
                              null);

  }

  /**
   * Processes the {@link Map} of {@link CommandLineOption} keys to {@link
   * CommandLineValue} values and produces the {@link Map} of {@link
   * CommandLineOption} keys to {@linkplain CommandLineValue#getProcessedValue()
   * processed values}.
   *
   * @param optionValues The {@link Map} of option keys to {@link
   *                     CommandLineValue} values.
   * @param processedValues The {@link Map} to populate with the processed
   *                        values if specified, if <code>null</code> a new {@link
   *                        Map} is created and returned.
   * @param stringBuilder If not <code>null</code>then this {@link StringBuilder}
   *                      is populated with JSON text describing the startup
   *                      options.
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link Object}
   *         values describing the parameters for the option.
   */
  public static Map<CommandLineOption, Object> processCommandLine(
      Map<CommandLineOption, CommandLineValue>  optionValues,
      Map<CommandLineOption, Object>            processedValues,
      StringBuilder                             stringBuilder)
  {
    return processCommandLine(optionValues,
                              processedValues,
                              null,
                              stringBuilder);
  }

  /**
   * Processes the {@link Map} of {@link CommandLineOption} keys to {@link
   * CommandLineValue} values and produces the {@link Map} of {@link
   * CommandLineOption} keys to {@linkplain CommandLineValue#getProcessedValue()
   * processed values}.
   *
   * @param optionValues The {@link Map} of option keys to {@link
   *                     CommandLineValue} values.
   * @param processedValues The {@link Map} to populate with the processed
   *                        values if specified, if <code>null</code> a new {@link
   *                        Map} is created and returned.
   * @param jsonBuilder If not <code>null</code> then this {@link JsonObjectBuilder}
   *                    is populated with startup option information.
   * @param stringBuilder If not <code>null</code>then this {@link StringBuilder}
   *                      is populated with JSON text describing the startup
   *                      options.
   * @return The {@link Map} of {@link CommandLineOption} keys to {@link Object}
   *         values describing the parameters for the option.
   */
  public static Map<CommandLineOption, Object> processCommandLine(
      Map<CommandLineOption, CommandLineValue>  optionValues,
      Map<CommandLineOption, Object>            processedValues,
      JsonObjectBuilder                         jsonBuilder,
      StringBuilder                             stringBuilder)
  {
    // create the result map if not specified
    Map<CommandLineOption, Object> result
        = (processedValues == null) ? new LinkedHashMap<>() : processedValues;

    // check if we are generating the JSON
    boolean doJson = (jsonBuilder != null || stringBuilder != null);

    // check if we need to create the JSON object builder
    JsonObjectBuilder job = (doJson && (jsonBuilder == null || stringBuilder != null))
        ? Json.createObjectBuilder() : jsonBuilder;

    // iterate over the option values and handle them
    optionValues.forEach( (key, cmdLineVal) -> {
      // confirm the option is consistent between the key and value
      if (key != cmdLineVal.getOption()) {
        throw new IllegalArgumentException(
            "CommandLineOption key does not match the option from the paired "
            + "CommandLineValue value.  key=[ " + key + " ], value=[ "
            + cmdLineVal + " ]");
      }

      // create the sub-builder if doing JSON
      JsonObjectBuilder job2 = (doJson) ? Json.createObjectBuilder() : null;

      // get the parameters
      List<String> params = cmdLineVal.getParameters();

      // add the parameters if doing JSON
      if (doJson) {
        // check if there is only a single parameter
        if (key.isSensitive()) {
          // handle sensitive values
          job2.add("value", REDACTED_SENSITIVE_VALUE);

        } else if (params.size() == 1) {
          // handle a single parameter
          job2.add("value", params.get(0));
        } else {
          // handle multiple parameters
          JsonArrayBuilder jab = Json.createArrayBuilder();
          for (String param : params) {
            jab.add(param);
          }
          job2.add("values", jab);
        }

        // add the source
        job2.add("source", cmdLineVal.getSource().toString());

        // check if we have a specifier to add
        if (cmdLineVal.getSpecifier() != null) {
          // add the specifier
          job2.add("via", cmdLineVal.getSpecifier());
        }

        // add to the JsonObjectBuilder
        JsonObject jsonObject = job2.build();
        job.add(key.toString(), jsonObject);
        if (jsonBuilder != null && job != jsonBuilder) {
          jsonBuilder.add(key.toString(), jsonObject);
        }
      }

      // add to the map
      result.put(key, cmdLineVal.getProcessedValue());
    });

    // append the JSON text if requested
    if (stringBuilder != null) {
      stringBuilder.append(JsonUtilities.toJsonText(job));
    }

    // return the map
    return result;
  }

  /**
   * Checks if the specified {@link Class} was the class whose static
   * <code>main(String[])</code> function was called to begin execution of the
   * current process.
   *
   * @param cls The {@link Class} to test for.
   *
   * @return <code>true</code> if the specified class' static
   *         <code>main(String[])</code> function was called to begin execution of
   *         the current process.
   */
  public static boolean checkClassIsMain(Class cls) {
    // check if called from the ConfigurationManager.main() directly
    Throwable t = new Throwable();
    StackTraceElement[] trace = t.getStackTrace();
    StackTraceElement lastStackFrame = trace[trace.length-1];
    String className = lastStackFrame.getClassName();
    String methodName = lastStackFrame.getMethodName();
    return ("main".equals(methodName) && cls.getName().equals(className));
  }

  /**
   * Returns a multi-line bulleted list of the specified options with the
   * specified indentation.
   *
   * @param indent The number of spaces to indent.
   * @param options The zero or more options to write.
   *
   * @return The multi-line bulleted list of options.
   */
  public static String formatUsageOptionsList(int                  indent,
                                              CommandLineOption... options)
  {
    StringWriter sw = new StringWriter();
    PrintWriter  pw = new PrintWriter(sw);
    int maxLength = 0;
    StringBuilder sb = new StringBuilder();
    for (int index = 0; index < indent; index++) {
      sb.append(" ");
    }
    String indenter = sb.toString();

    for (CommandLineOption option : options) {
      if (option.getCommandLineFlag().length() > maxLength) {
        maxLength = option.getCommandLineFlag().length();
      }
    }
    String spacer = "  ";
    String bullet = "o ";
    maxLength += (bullet.length() + spacer.length());
    // check how many options per line we can fit
    int columnCount = (80 - indent - spacer.length()) / maxLength;

    // check if we can balance things out if we have a single dangling option
    if (options.length == columnCount + 1) {
        columnCount--;
    }

    // if less than 1, then set a minimum of 1 column
    if (columnCount < 1) columnCount = 1;

    int columnIndex = 0;
    for (CommandLineOption option: options) {
      String flag = option.getCommandLineFlag();
      int spaceCount = maxLength - flag.length() - bullet.length();
      if (columnIndex == 0) pw.print(indenter);
      else pw.print(spacer);
      pw.print(bullet);
      pw.print(flag);
      for (int index = 0; index < spaceCount; index++) {
        pw.print(" ");
      }
      if (columnIndex == columnCount - 1) {
        pw.println();
        columnIndex = 0;
      } else {
        columnIndex++;
      }
    }
    if (columnIndex != 0) {
      pw.println();
    }
    pw.flush();
    return sw.toString();
  }

  /**
   * Gets the base URL for the JAR file where the specified {@link Class} is
   * found.  This method returns <code>null</code> if the JAR base URL cannot be
   * determined.
   *
   * @param cls The {@link Class} for which the JAR base URL is requested.
   *
   * @return The base URL for the JAR that has the specified class, or
   *         <code>null</code> if it cannot be determined.
   */
  public static String getJarBaseUrl(Class cls) {
    String simpleName = cls.getSimpleName();
    String url = cls.getResource(simpleName + ".class").toString();

    int index = url.lastIndexOf(
        cls.getName().replace(".", "/") + ".class");

    if (index < 0) return null;

    return url.substring(0, index);
  }

  /**
   * Gets the name of the JAR file that contains the specified {@link Class}.
   * This method returns <code>null</code> if the JAR file name cannot be
   * determined.
   *
   * @param cls The {@link Class} for which the jar file name is requested.
   *
   * @return The name of the JAR file that has the specified class, or
   *         <code>null</code> if it cannot be determined.
   */
  public static String getJarName(Class cls) {
    String url = getJarBaseUrl(cls);

    if (url == null) return null;

    if (url.indexOf(".jar") >= 0) {
      int index = url.lastIndexOf("!");
      if (index >= 0) {
        url = url.substring(0, index);
        index = url.lastIndexOf("/");
        if (index >= 0) {
          return url.substring(index + 1);
        }
      }
    }
    return null;
  }

  /**
   * Gets the path of the JAR file that contains the specified {@link Class}.
   * This method returns <code>null</code> if the JAR file path cannot be
   * determined.
   *
   * @param cls The {@link Class} for which the jar file path is requested.
   *
   * @return The path to the JAR file that has the specified class, or
   *         <code>null</code> if it cannot be determined.
   */
  public static String getJarPath(Class cls) {
    String url = getJarBaseUrl(cls);

    if (url == null) return null;

    if (url.indexOf(".jar") >= 0) {
      int index = url.lastIndexOf("!");
      if (index >= 0) {
        url = url.substring(0, index);
        index = url.lastIndexOf("/");
        if (index >= 0) {
          url = url.substring(0, index);
          index = url.indexOf("/");
          if (index >= 0) {
            return url.substring(index);
          }
        }
      }
    }
    return null;
  }

  public static void main(String[] args) {
    try {
      Class cls = CommandLineUtilities.class;
      System.out.println("BASE URL: " + getJarBaseUrl(cls));
      System.out.println("JAR NAME: " + getJarName(cls));
      System.out.println("JAR PATH: " + getJarPath(cls));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
