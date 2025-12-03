package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.senzing.text.TextUtilities;

import javax.json.*;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.senzing.util.JsonUtilities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link JsonUtilities}.
 */
@SuppressWarnings("unchecked")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class JsonUtilitiesTest {

    public List<Arguments> provideGetStringFromObjectParams() {
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("givenName", "John");
        job.add("surname", "Doe");
        job.add("phone", "702-555-1212");
        job.addNull("address");
        job.add("heightInCm", 175);
        job.add("vip", true);
        job.add("vegetarian", false);

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("givenName", "Jane");
        job2.add("surname", "Doe");
        job.add("spouse", job2);

        JsonObject jsonObj = job.build();
        String spouseText = toJsonText(
                jsonObj.getJsonObject("spouse"), true);

        result.add(arguments(jsonObj, "givenName", null, "John", null));
        result.add(arguments(jsonObj, "givenName", "Joe", "John", null));
        result.add(arguments(jsonObj, "surname", "Schmoe", "Doe", null));
        result.add(arguments(jsonObj, "address", null, null, null));
        result.add(arguments(
                jsonObj, "address", "101 Main St.", "101 Main St.", null));
        result.add(arguments(jsonObj, "eyeColor", "Brown", "Brown", null));
        result.add(arguments(jsonObj, null, null, null, null));
        result.add(arguments(null, "surname", null, null, null));
        result.add(arguments(null, "surname", "Schmoe", "Schmoe", null));
        result.add(arguments(jsonObj, "spouse", null, spouseText, null));
        result.add(arguments(jsonObj, "heightInCm", "Foo", "175", null));
        result.add(arguments(jsonObj, "heightInCm", null, "175", null));
        result.add(arguments(jsonObj, "vip", null, "true", null));
        result.add(arguments(jsonObj, "vip", "false", "true", null));
        result.add(arguments(jsonObj, "vegetarian", "false", "false", null));
        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetStringFromObjectParams")
    public void getStringFromObjectTest(JsonObject jsonObject,
            String key,
            String defaultValue,
            String expectedResult,
            Class expectedFailure) {
        try {
            String result = getString(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getString(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetStringFromArrayParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add("John");
        jab.addNull();
        jab.add(175);
        jab.add(true);
        jab.add(false);

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("givenName", "Jane");
        job2.add("surname", "Doe");
        jab.add(job2);

        JsonArray jsonArr = jab.build();
        String spouseText = toJsonText(
                jsonArr.getJsonObject(5), true);

        result.add(arguments(jsonArr, 0, null, "John", null));
        result.add(arguments(jsonArr, 0, "Joe", "John", null));
        result.add(arguments(jsonArr, 1, null, null, null));
        result.add(arguments(jsonArr, 1, "101 Main St.", "101 Main St.", null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 1, "Schmoe", "Schmoe", null));
        result.add(arguments(jsonArr, 2, "Foo", "175", null));
        result.add(arguments(jsonArr, 2, null, "175", null));
        result.add(arguments(jsonArr, 3, null, "true", null));
        result.add(arguments(jsonArr, 3, "false", "true", null));
        result.add(arguments(jsonArr, 4, null, "false", null));
        result.add(arguments(jsonArr, 4, "true", "false", null));
        result.add(arguments(jsonArr, 5, null, spouseText, null));
        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetStringFromArrayParams")
    public void getStringFromArrayTest(JsonArray jsonArray,
            int index,
            String defaultValue,
            String expectedResult,
            Class expectedFailure) {
        try {
            String result = getString(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getString(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetJsonArrayFromObjectParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(3);

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("items", jab);
        job.add("name", "Foo");

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("name", "Bar");
        job.add("child", job2);

        JsonObject jsonObj = job.build();
        JsonArray array = jsonObj.getJsonArray("items");

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "items", array, null));
        result.add(arguments(null, "items", null, null));
        result.add(arguments(null, null, null, null));
        result.add(arguments(jsonObj, null, null, null));
        result.add(arguments(jsonObj, "missing", null, null));
        result.add(arguments(jsonObj, "name", null, classCast));
        result.add(arguments(jsonObj, "child", null, classCast));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetJsonArrayFromObjectParams")
    public void getJsonArrayFromObjectTest(JsonObject jsonObject,
            String key,
            JsonArray expectedResult,
            Class expectedFailure) {
        try {
            JsonArray result = getJsonArray(jsonObject, key);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetJsonArrayFromArrayParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(3);

        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(jab);
        builder.add("Foo");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("name", "Bar");
        builder.add(job);

        JsonArray jsonArr = builder.build();
        JsonArray array = jsonArr.getJsonArray(0);

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, array, null));
        result.add(arguments(null, 0, null, null));
        result.add(arguments(jsonArr, 1, null, classCast));
        result.add(arguments(jsonArr, 2, null, classCast));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetJsonArrayFromArrayParams")
    public void getJsonArrayFromArrayTest(JsonArray jsonArray,
            int index,
            JsonArray expectedResult,
            Class expectedFailure) {
        try {
            JsonArray result = getJsonArray(jsonArray, index);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetStringsFromObjectParams() {
        List<Arguments> result = new LinkedList<>();
        List<String> stringList = List.of("One", "Two", "Three");
        List<String> numList = List.of("1", "2", "3");
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add("One").add("Two").add("Three");

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("items", jab);

        jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(3);
        job.add("numbers", jab);

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("name", "Bar");

        jab = Json.createArrayBuilder();
        jab.add("One").add(2).add(3.0).add(true).add(false).addNull().add(job2);
        job.add("mixed", jab);

        job.add("name", "Foo");
        job.addNull("none");

        JsonObject jsonObj = job.build();

        JsonObject childObj = jsonObj.getJsonArray("mixed").getJsonObject(6);
        String childText = JsonUtilities.toJsonText(childObj, true);

        List<String> mixedList = new ArrayList<>();
        mixedList.add("One");
        mixedList.add("2");
        mixedList.add("3.0");
        mixedList.add("true");
        mixedList.add("false");
        mixedList.add(null);
        mixedList.add(childText);
        mixedList = Collections.unmodifiableList(mixedList);

        List<String> defaults = List.of("A", "B", "C");

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "items", null, stringList, null));
        result.add(arguments(jsonObj, "numbers", null, numList, null));
        result.add(arguments(jsonObj, "mixed", null, mixedList, null));
        result.add(arguments(jsonObj, "items", defaults, stringList, null));
        result.add(arguments(jsonObj, "numbers", defaults, numList, null));
        result.add(arguments(jsonObj, "mixed", defaults, mixedList, null));
        result.add(arguments(jsonObj, "missing", null, null, null));
        result.add(arguments(jsonObj, "missing", defaults, defaults, null));
        result.add(arguments(null, "items", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(jsonObj, null, null, null, null));
        result.add(arguments(null, "items", defaults, defaults, null));
        result.add(arguments(null, null, defaults, defaults, null));
        result.add(arguments(jsonObj, null, defaults, defaults, null));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", defaults, defaults, null));
        result.add(arguments(jsonObj, "name", null, null, classCast));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetStringsFromObjectParams")
    public void getStringsFromObjectTest(JsonObject jsonObject,
            String key,
            List<String> defaultValue,
            List<String> expectedResult,
            Class expectedFailure) {
        try {
            List<String> result = getStrings(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getStrings(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetStringsFromArrayParams() {
        List<Arguments> result = new LinkedList<>();
        List<String> stringList = List.of("One", "Two", "Three");
        List<String> numList = List.of("1", "2", "3");
        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add("One").add("Two").add("Three");

        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add(jab);

        jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(3);
        builder.add(jab);

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("name", "Bar");

        jab = Json.createArrayBuilder();
        jab.add("One").add(2).add(3.0).add(true).add(false).addNull().add(job2);
        builder.add(jab);

        builder.add("Foo");
        builder.addNull();

        JsonArray jsonArr = builder.build();

        JsonObject childObj = jsonArr.getJsonArray(2).getJsonObject(6);
        String childText = JsonUtilities.toJsonText(childObj, true);

        List<String> mixedList = new ArrayList<>();
        mixedList.add("One");
        mixedList.add("2");
        mixedList.add("3.0");
        mixedList.add("true");
        mixedList.add("false");
        mixedList.add(null);
        mixedList.add(childText);
        mixedList = Collections.unmodifiableList(mixedList);

        List<String> defaults = List.of("A", "B", "C");

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, stringList, null));
        result.add(arguments(jsonArr, 1, null, numList, null));
        result.add(arguments(jsonArr, 2, null, mixedList, null));
        result.add(arguments(jsonArr, 0, defaults, stringList, null));
        result.add(arguments(jsonArr, 1, defaults, numList, null));
        result.add(arguments(jsonArr, 2, defaults, mixedList, null));
        result.add(arguments(jsonArr, 4, null, null, null));
        result.add(arguments(jsonArr, 4, defaults, defaults, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, defaults, defaults, null));
        result.add(arguments(jsonArr, 3, null, null, classCast));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetStringsFromArrayParams")
    public void getStringsFromArrayTest(JsonArray jsonArray,
            int index,
            List<String> defaultValue,
            List<String> expectedResult,
            Class expectedFailure) {
        try {
            List<String> result = getStrings(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getStrings(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetIntegerFromObjectParams() {
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0);
        job.add("one", 1);
        job.add("ten", 10);
        job.add("long", ((long) Integer.MAX_VALUE) + 1L);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", 15, 15, null));
        result.add(arguments(jsonObj, "missing", 15, 15, null));
        result.add(arguments(jsonObj, "zero", null, 0, null));
        result.add(arguments(jsonObj, "zero", 15, 0, null));
        result.add(arguments(jsonObj, "one", null, 1, null));
        result.add(arguments(jsonObj, "one", 15, 1, null));
        result.add(arguments(jsonObj, "ten", null, 10, null));
        result.add(arguments(jsonObj, "ten", 15, 10, null));
        result.add(arguments(jsonObj, "long", null, Integer.MIN_VALUE, null));
        result.add(arguments(jsonObj, "long", 15, Integer.MIN_VALUE, null));
        result.add(arguments(jsonObj, "ten.zero", null, 10, null));
        result.add(arguments(jsonObj, "ten.zero", 15, 10, null));
        result.add(arguments(jsonObj, "ten.five", null, 10, null));
        result.add(arguments(jsonObj, "ten.five", 15, 10, null));
        result.add(arguments(jsonObj, "twenty.zero", null, 20, null));
        result.add(arguments(jsonObj, "twenty.zero", 15, 20, null));
        result.add(arguments(jsonObj, "twenty.five", null, 20, null));
        result.add(arguments(jsonObj, "twenty.five", 15, 20, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", 15, 15, null));
        result.add(arguments(null, null, 15, 15, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetIntegerFromObjectParams")
    public void getIntegerFromObjectTest(JsonObject jsonObject,
            String key,
            Integer defaultValue,
            Integer expectedResult,
            Class expectedFailure) {
        try {
            Integer result = getInteger(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getInteger(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetIntegerFromArrayParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC"); // 0
        builder.add(true); // 1
        builder.add(false); // 2
        builder.addNull(); // 3
        builder.add(0); // 4
        builder.add(1); // 5
        builder.add(10); // 6
        builder.add(((long) Integer.MAX_VALUE) + 1L); // 7
        builder.add(10.0); // 8
        builder.add(10.5); // 9
        builder.add(20.0F); // 10
        builder.add(20.5F); // 11
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, 15, 15, null));
        result.add(arguments(jsonArr, 4, null, 0, null));
        result.add(arguments(jsonArr, 4, 15, 0, null));
        result.add(arguments(jsonArr, 5, null, 1, null));
        result.add(arguments(jsonArr, 5, 15, 1, null));
        result.add(arguments(jsonArr, 6, null, 10, null));
        result.add(arguments(jsonArr, 6, 15, 10, null));
        result.add(arguments(jsonArr, 7, null, Integer.MIN_VALUE, null));
        result.add(arguments(jsonArr, 7, 15, Integer.MIN_VALUE, null));
        result.add(arguments(jsonArr, 8, null, 10, null));
        result.add(arguments(jsonArr, 8, 15, 10, null));
        result.add(arguments(jsonArr, 9, null, 10, null));
        result.add(arguments(jsonArr, 9, 15, 10, null));
        result.add(arguments(jsonArr, 10, null, 20, null));
        result.add(arguments(jsonArr, 10, 15, 20, null));
        result.add(arguments(jsonArr, 11, null, 20, null));
        result.add(arguments(jsonArr, 11, 15, 20, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, 15, 15, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetIntegerFromArrayParams")
    public void getIntegerFromArrayTest(JsonArray jsonArray,
            int index,
            Integer defaultValue,
            Integer expectedResult,
            Class expectedFailure) {
        try {
            Integer result = getInteger(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getInteger(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetLongFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", 15L, 15L, null));
        result.add(arguments(jsonObj, "missing", 15L, 15L, null));
        result.add(arguments(jsonObj, "zero", null, 0L, null));
        result.add(arguments(jsonObj, "zero", 15L, 0L, null));
        result.add(arguments(jsonObj, "one", null, 1L, null));
        result.add(arguments(jsonObj, "one", 15L, 1L, null));
        result.add(arguments(jsonObj, "ten", null, 10L, null));
        result.add(arguments(jsonObj, "ten", 15L, 10L, null));
        result.add(arguments(jsonObj, "long", null, maxIntPlus1, null));
        result.add(arguments(jsonObj, "long", 15L, maxIntPlus1, null));
        result.add(arguments(jsonObj, "ten.zero", null, 10L, null));
        result.add(arguments(jsonObj, "ten.zero", 15L, 10L, null));
        result.add(arguments(jsonObj, "ten.five", null, 10L, null));
        result.add(arguments(jsonObj, "ten.five", 15L, 10L, null));
        result.add(arguments(jsonObj, "twenty.zero", null, 20L, null));
        result.add(arguments(jsonObj, "twenty.zero", 15L, 20L, null));
        result.add(arguments(jsonObj, "twenty.five", null, 20L, null));
        result.add(arguments(jsonObj, "twenty.five", 15L, 20L, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", 15L, 15L, null));
        result.add(arguments(null, null, 15L, 15L, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetLongFromObjectParams")
    public void getLongFromObjectTest(JsonObject jsonObject,
            String key,
            Long defaultValue,
            Long expectedResult,
            Class expectedFailure) {
        try {
            Long result = getLong(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getLong(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetLongFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC"); // 0
        builder.add(true); // 1
        builder.add(false); // 2
        builder.addNull(); // 3
        builder.add(0L); // 4
        builder.add(1L); // 5
        builder.add(10L); // 6
        builder.add(maxIntPlus1); // 7
        builder.add(10.0); // 8
        builder.add(10.5); // 9
        builder.add(20.0F); // 10
        builder.add(20.5F); // 11
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, 15L, 15L, null));
        result.add(arguments(jsonArr, 4, null, 0L, null));
        result.add(arguments(jsonArr, 4, 15L, 0L, null));
        result.add(arguments(jsonArr, 5, null, 1L, null));
        result.add(arguments(jsonArr, 5, 15L, 1L, null));
        result.add(arguments(jsonArr, 6, null, 10L, null));
        result.add(arguments(jsonArr, 6, 15L, 10L, null));
        result.add(arguments(jsonArr, 7, null, maxIntPlus1, null));
        result.add(arguments(jsonArr, 7, 15L, maxIntPlus1, null));
        result.add(arguments(jsonArr, 8, null, 10L, null));
        result.add(arguments(jsonArr, 8, 15L, 10L, null));
        result.add(arguments(jsonArr, 9, null, 10L, null));
        result.add(arguments(jsonArr, 9, 15L, 10L, null));
        result.add(arguments(jsonArr, 10, null, 20L, null));
        result.add(arguments(jsonArr, 10, 15L, 20L, null));
        result.add(arguments(jsonArr, 11, null, 20L, null));
        result.add(arguments(jsonArr, 11, 15L, 20L, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, 15L, 15L, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetLongFromArrayParams")
    public void getLongFromArrayTest(JsonArray jsonArray,
            int index,
            Long defaultValue,
            Long expectedResult,
            Class expectedFailure) {
        try {
            Long result = getLong(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getLong(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetDoubleFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", 15.0, 15.0, null));
        result.add(arguments(jsonObj, "missing", 15.0, 15.0, null));
        result.add(arguments(jsonObj, "zero", null, 0.0, null));
        result.add(arguments(jsonObj, "zero", 15.0, 0.0, null));
        result.add(arguments(jsonObj, "one", null, 1.0, null));
        result.add(arguments(jsonObj, "one", 15.0, 1.0, null));
        result.add(arguments(jsonObj, "ten", null, 10.0, null));
        result.add(arguments(jsonObj, "ten", 15.0, 10.0, null));
        result.add(arguments(jsonObj, "long", null, ((double) maxIntPlus1), null));
        result.add(arguments(jsonObj, "long", 15.0, ((double) maxIntPlus1), null));
        result.add(arguments(jsonObj, "ten.zero", null, 10.0, null));
        result.add(arguments(jsonObj, "ten.zero", 15.0, 10.0, null));
        result.add(arguments(jsonObj, "ten.five", null, 10.5, null));
        result.add(arguments(jsonObj, "ten.five", 15.0, 10.5, null));
        result.add(arguments(jsonObj, "twenty.zero", null, 20.0, null));
        result.add(arguments(jsonObj, "twenty.zero", 15.0, 20.0, null));
        result.add(arguments(jsonObj, "twenty.five", null, 20.5, null));
        result.add(arguments(jsonObj, "twenty.five", 15.0, 20.5, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", 15.0, 15.0, null));
        result.add(arguments(null, null, 15.0, 15.0, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetDoubleFromObjectParams")
    public void getDoubleFromObjectTest(JsonObject jsonObject,
            String key,
            Double defaultValue,
            Double expectedResult,
            Class expectedFailure) {
        try {
            Double result = getDouble(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getDouble(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetDoubleFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.add(true);
        builder.add(false);
        builder.addNull();
        builder.add(0L);
        builder.add(1L);
        builder.add(10L);
        builder.add(maxIntPlus1);
        builder.add(10.0);
        builder.add(10.5);
        builder.add(20.0F);
        builder.add(20.5F);
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, 15.0, 15.0, null));
        result.add(arguments(jsonArr, 4, null, 0.0, null));
        result.add(arguments(jsonArr, 4, 15.0, 0.0, null));
        result.add(arguments(jsonArr, 5, null, 1.0, null));
        result.add(arguments(jsonArr, 5, 15.0, 1.0, null));
        result.add(arguments(jsonArr, 6, null, 10.0, null));
        result.add(arguments(jsonArr, 6, 15.0, 10.0, null));
        result.add(arguments(jsonArr, 7, null, ((double) maxIntPlus1), null));
        result.add(arguments(jsonArr, 7, 15.0, ((double) maxIntPlus1), null));
        result.add(arguments(jsonArr, 8, null, 10.0, null));
        result.add(arguments(jsonArr, 8, 15.0, 10.0, null));
        result.add(arguments(jsonArr, 9, null, 10.5, null));
        result.add(arguments(jsonArr, 9, 15.0, 10.5, null));
        result.add(arguments(jsonArr, 10, null, 20.0, null));
        result.add(arguments(jsonArr, 10, 15.0, 20.0, null));
        result.add(arguments(jsonArr, 11, null, 20.5, null));
        result.add(arguments(jsonArr, 11, 15.0, 20.5, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, 15.0, 15.0, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetDoubleFromArrayParams")
    public void getDoubleFromArrayTest(JsonArray jsonArray,
            int index,
            Double defaultValue,
            Double expectedResult,
            Class expectedFailure) {
        try {
            Double result = getDouble(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getDouble(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBigDecimalFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;
        BigDecimal fifteen = new BigDecimal("15");
        BigDecimal zero = new BigDecimal("0");
        BigDecimal one = new BigDecimal("1");
        BigDecimal ten = new BigDecimal("10");
        BigDecimal tenPt0 = new BigDecimal("10.0");
        BigDecimal twentyPt0 = new BigDecimal("20.0");
        BigDecimal decMax = new BigDecimal("" + maxIntPlus1);
        BigDecimal tenPt5 = new BigDecimal("10.5");
        BigDecimal twentyPt5 = new BigDecimal("20.5");

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", fifteen, fifteen, null));
        result.add(arguments(jsonObj, "missing", fifteen, fifteen, null));
        result.add(arguments(jsonObj, "zero", null, zero, null));
        result.add(arguments(jsonObj, "zero", fifteen, zero, null));
        result.add(arguments(jsonObj, "one", null, one, null));
        result.add(arguments(jsonObj, "one", fifteen, one, null));
        result.add(arguments(jsonObj, "ten", null, ten, null));
        result.add(arguments(jsonObj, "ten", fifteen, ten, null));
        result.add(arguments(jsonObj, "long", null, decMax, null));
        result.add(arguments(jsonObj, "long", fifteen, decMax, null));
        result.add(arguments(jsonObj, "ten.zero", null, tenPt0, null));
        result.add(arguments(jsonObj, "ten.zero", fifteen, tenPt0, null));
        result.add(arguments(jsonObj, "ten.five", null, tenPt5, null));
        result.add(arguments(jsonObj, "ten.five", fifteen, tenPt5, null));
        result.add(arguments(jsonObj, "twenty.zero", null, twentyPt0, null));
        result.add(arguments(jsonObj, "twenty.zero", fifteen, twentyPt0, null));
        result.add(arguments(jsonObj, "twenty.five", null, twentyPt5, null));
        result.add(arguments(jsonObj, "twenty.five", fifteen, twentyPt5, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", fifteen, fifteen, null));
        result.add(arguments(null, null, fifteen, fifteen, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetBigDecimalFromObjectParams")
    public void getBigDecimalFromObjectTest(JsonObject jsonObject,
            String key,
            BigDecimal defaultValue,
            BigDecimal expectedResult,
            Class expectedFailure) {
        try {
            BigDecimal result = getBigDecimal(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBigDecimal(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBigDecimalFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.add(true);
        builder.add(false);
        builder.addNull();
        builder.add(0L);
        builder.add(1L);
        builder.add(10L);
        builder.add(maxIntPlus1);
        builder.add(10.0);
        builder.add(10.5);
        builder.add(20.0F);
        builder.add(20.5F);
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;
        BigDecimal fifteen = new BigDecimal("15");
        BigDecimal zero = new BigDecimal("0");
        BigDecimal one = new BigDecimal("1");
        BigDecimal ten = new BigDecimal("10");
        BigDecimal tenPt0 = new BigDecimal("10.0");
        BigDecimal twentyPt0 = new BigDecimal("20.0");
        BigDecimal decMax = new BigDecimal("" + maxIntPlus1);
        BigDecimal tenPt5 = new BigDecimal("10.5");
        BigDecimal twentyPt5 = new BigDecimal("20.5");

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, fifteen, fifteen, null));
        result.add(arguments(jsonArr, 4, null, zero, null));
        result.add(arguments(jsonArr, 4, fifteen, zero, null));
        result.add(arguments(jsonArr, 5, null, one, null));
        result.add(arguments(jsonArr, 5, fifteen, one, null));
        result.add(arguments(jsonArr, 6, null, ten, null));
        result.add(arguments(jsonArr, 6, fifteen, ten, null));
        result.add(arguments(jsonArr, 7, null, decMax, null));
        result.add(arguments(jsonArr, 7, fifteen, decMax, null));
        result.add(arguments(jsonArr, 8, null, tenPt0, null));
        result.add(arguments(jsonArr, 8, fifteen, tenPt0, null));
        result.add(arguments(jsonArr, 9, null, tenPt5, null));
        result.add(arguments(jsonArr, 9, fifteen, tenPt5, null));
        result.add(arguments(jsonArr, 10, null, twentyPt0, null));
        result.add(arguments(jsonArr, 10, fifteen, twentyPt0, null));
        result.add(arguments(jsonArr, 11, null, twentyPt5, null));
        result.add(arguments(jsonArr, 11, fifteen, twentyPt5, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, fifteen, fifteen, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetBigDecimalFromArrayParams")
    public void getBigDecimalFromArrayTest(JsonArray jsonArray,
            int index,
            BigDecimal defaultValue,
            BigDecimal expectedResult,
            Class expectedFailure) {
        try {
            BigDecimal result = getBigDecimal(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBigDecimal(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBigIntegerFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;
        BigInteger fifteen = new BigInteger("15");
        BigInteger zero = new BigInteger("0");
        BigInteger one = new BigInteger("1");
        BigInteger ten = new BigInteger("10");
        BigInteger twenty = new BigInteger("20");
        BigInteger intMax = new BigInteger("" + maxIntPlus1);

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", fifteen, fifteen, null));
        result.add(arguments(jsonObj, "missing", fifteen, fifteen, null));
        result.add(arguments(jsonObj, "zero", null, zero, null));
        result.add(arguments(jsonObj, "zero", fifteen, zero, null));
        result.add(arguments(jsonObj, "one", null, one, null));
        result.add(arguments(jsonObj, "one", fifteen, one, null));
        result.add(arguments(jsonObj, "ten", null, ten, null));
        result.add(arguments(jsonObj, "ten", fifteen, ten, null));
        result.add(arguments(jsonObj, "long", null, intMax, null));
        result.add(arguments(jsonObj, "long", fifteen, intMax, null));
        result.add(arguments(jsonObj, "ten.zero", null, ten, null));
        result.add(arguments(jsonObj, "ten.zero", fifteen, ten, null));
        result.add(arguments(jsonObj, "ten.five", null, ten, null));
        result.add(arguments(jsonObj, "ten.five", fifteen, ten, null));
        result.add(arguments(jsonObj, "twenty.zero", null, twenty, null));
        result.add(arguments(jsonObj, "twenty.zero", fifteen, twenty, null));
        result.add(arguments(jsonObj, "twenty.five", null, twenty, null));
        result.add(arguments(jsonObj, "twenty.five", fifteen, twenty, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", fifteen, fifteen, null));
        result.add(arguments(null, null, fifteen, fifteen, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetBigIntegerFromObjectParams")
    public void getBigIntegerFromObjectTest(JsonObject jsonObject,
            String key,
            BigInteger defaultValue,
            BigInteger expectedResult,
            Class expectedFailure) {
        try {
            BigInteger result = getBigInteger(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBigInteger(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBigIntegerFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.add(true);
        builder.add(false);
        builder.addNull();
        builder.add(0L);
        builder.add(1L);
        builder.add(10L);
        builder.add(maxIntPlus1);
        builder.add(10.0);
        builder.add(10.5);
        builder.add(20.0F);
        builder.add(20.5F);
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;
        BigInteger fifteen = new BigInteger("15");
        BigInteger zero = new BigInteger("0");
        BigInteger one = new BigInteger("1");
        BigInteger ten = new BigInteger("10");
        BigInteger twenty = new BigInteger("20");
        BigInteger intMax = new BigInteger("" + maxIntPlus1);

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, fifteen, fifteen, null));
        result.add(arguments(jsonArr, 4, null, zero, null));
        result.add(arguments(jsonArr, 4, fifteen, zero, null));
        result.add(arguments(jsonArr, 5, null, one, null));
        result.add(arguments(jsonArr, 5, fifteen, one, null));
        result.add(arguments(jsonArr, 6, null, ten, null));
        result.add(arguments(jsonArr, 6, fifteen, ten, null));
        result.add(arguments(jsonArr, 7, null, intMax, null));
        result.add(arguments(jsonArr, 7, fifteen, intMax, null));
        result.add(arguments(jsonArr, 8, null, ten, null));
        result.add(arguments(jsonArr, 8, fifteen, ten, null));
        result.add(arguments(jsonArr, 9, null, ten, null));
        result.add(arguments(jsonArr, 9, fifteen, ten, null));
        result.add(arguments(jsonArr, 10, null, twenty, null));
        result.add(arguments(jsonArr, 10, fifteen, twenty, null));
        result.add(arguments(jsonArr, 11, null, twenty, null));
        result.add(arguments(jsonArr, 11, fifteen, twenty, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 1, fifteen, fifteen, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetBigIntegerFromArrayParams")
    public void getBigIntegerFromArrayTest(JsonArray jsonArray,
            int index,
            BigInteger defaultValue,
            BigInteger expectedResult,
            Class expectedFailure) {
        try {
            BigInteger result = getBigInteger(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBigInteger(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetFloatFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "true", null, null, classCast));
        result.add(arguments(jsonObj, "false", null, null, classCast));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", 15.0F, 15.0F, null));
        result.add(arguments(jsonObj, "missing", 15.0F, 15.0F, null));
        result.add(arguments(jsonObj, "zero", null, 0.0F, null));
        result.add(arguments(jsonObj, "zero", 15.0F, 0.0F, null));
        result.add(arguments(jsonObj, "one", null, 1.0F, null));
        result.add(arguments(jsonObj, "one", 15.0F, 1.0F, null));
        result.add(arguments(jsonObj, "ten", null, 10.0F, null));
        result.add(arguments(jsonObj, "ten", 15.0F, 10.0F, null));
        result.add(arguments(jsonObj, "long", null, ((float) maxIntPlus1), null));
        result.add(arguments(jsonObj, "long", 15.0F, ((float) maxIntPlus1), null));
        result.add(arguments(jsonObj, "ten.zero", null, 10.0F, null));
        result.add(arguments(jsonObj, "ten.zero", 15.0F, 10.0F, null));
        result.add(arguments(jsonObj, "ten.five", null, 10.5F, null));
        result.add(arguments(jsonObj, "ten.five", 15.0F, 10.5F, null));
        result.add(arguments(jsonObj, "twenty.zero", null, 20.0F, null));
        result.add(arguments(jsonObj, "twenty.zero", 15.0F, 20.0F, null));
        result.add(arguments(jsonObj, "twenty.five", null, 20.5F, null));
        result.add(arguments(jsonObj, "twenty.five", 15.0F, 20.5F, null));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, "foo", 15.0F, 15.0F, null));
        result.add(arguments(null, null, 15.0F, 15.0F, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetFloatFromObjectParams")
    public void getFloatFromObjectTest(JsonObject jsonObject,
            String key,
            Float defaultValue,
            Float expectedResult,
            Class expectedFailure) {
        try {
            Float result = getFloat(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getFloat(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetFloatFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.add(true);
        builder.add(false);
        builder.addNull();
        builder.add(0L);
        builder.add(1L);
        builder.add(10L);
        builder.add(maxIntPlus1);
        builder.add(10.0);
        builder.add(10.5);
        builder.add(20.0F);
        builder.add(20.5F);
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, null, classCast));
        result.add(arguments(jsonArr, 2, null, null, classCast));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, 15.0F, 15.0F, null));
        result.add(arguments(jsonArr, 4, null, 0.0F, null));
        result.add(arguments(jsonArr, 4, 15.0F, 0.0F, null));
        result.add(arguments(jsonArr, 5, null, 1.0F, null));
        result.add(arguments(jsonArr, 5, 15.0F, 1.0F, null));
        result.add(arguments(jsonArr, 6, null, 10.0F, null));
        result.add(arguments(jsonArr, 6, 15.0F, 10.0F, null));
        result.add(arguments(jsonArr, 7, null, ((float) maxIntPlus1), null));
        result.add(arguments(jsonArr, 7, 15.0F, ((float) maxIntPlus1), null));
        result.add(arguments(jsonArr, 8, null, 10.0F, null));
        result.add(arguments(jsonArr, 8, 15.0F, 10.0F, null));
        result.add(arguments(jsonArr, 9, null, 10.5F, null));
        result.add(arguments(jsonArr, 9, 15.0F, 10.5F, null));
        result.add(arguments(jsonArr, 10, null, 20.0F, null));
        result.add(arguments(jsonArr, 10, 15.0F, 20.0F, null));
        result.add(arguments(jsonArr, 11, null, 20.5F, null));
        result.add(arguments(jsonArr, 11, 15.0F, 20.5F, null));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, 15.0F, 15.0F, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetFloatFromArrayParams")
    public void getFloatFromArrayTest(JsonArray jsonArray,
            int index,
            Float defaultValue,
            Float expectedResult,
            Class expectedFailure) {
        try {
            Float result = getFloat(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getFloat(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBooleanFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("true", true);
        job.add("false", false);
        job.addNull("none");
        job.add("zero", 0L);
        job.add("one", 1L);
        job.add("ten", 10L);
        job.add("long", maxIntPlus1);
        job.add("ten.zero", 10.0);
        job.add("ten.five", 10.5);
        job.add("twenty.zero", 20.0F);
        job.add("twenty.five", 20.5F);
        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonObj, "true", null, true, null));
        result.add(arguments(jsonObj, "true", false, true, null));
        result.add(arguments(jsonObj, "false", null, false, null));
        result.add(arguments(jsonObj, "false", true, false, null));
        result.add(arguments(jsonObj, "none", null, null, null));
        result.add(arguments(jsonObj, "none", true, true, null));
        result.add(arguments(jsonObj, "none", false, false, null));
        result.add(arguments(jsonObj, "missing", null, null, null));
        result.add(arguments(jsonObj, "missing", true, true, null));
        result.add(arguments(jsonObj, "missing", false, false, null));
        result.add(arguments(jsonObj, "text", null, null, classCast));
        result.add(arguments(jsonObj, "zero", null, null, classCast));
        result.add(arguments(jsonObj, "one", null, null, classCast));
        result.add(arguments(jsonObj, "ten", null, null, classCast));
        result.add(arguments(jsonObj, "long", null, null, classCast));
        result.add(arguments(jsonObj, "ten.zero", null, null, classCast));
        result.add(arguments(jsonObj, "ten.five", null, null, classCast));
        result.add(arguments(jsonObj, "twenty.zero", null, null, classCast));
        result.add(arguments(jsonObj, "twenty.five", null, null, classCast));
        result.add(arguments(null, "foo", null, null, null));
        result.add(arguments(null, "foo", true, true, null));
        result.add(arguments(null, "foo", false, false, null));
        result.add(arguments(null, null, null, null, null));
        result.add(arguments(null, null, true, true, null));
        result.add(arguments(null, null, false, false, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetBooleanFromObjectParams")
    public void getBooleanFromObjectTest(JsonObject jsonObject,
            String key,
            Boolean defaultValue,
            Boolean expectedResult,
            Class expectedFailure) {
        try {
            Boolean result = getBoolean(jsonObject, key, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBoolean(jsonObject, key);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonObject=[ " + jsonObject
                                + " ], key=[ " + key + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetBooleanFromArrayParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.add(true);
        builder.add(false);
        builder.addNull();
        builder.add(0L);
        builder.add(1L);
        builder.add(10L);
        builder.add(maxIntPlus1);
        builder.add(10.0);
        builder.add(10.5);
        builder.add(20.0F);
        builder.add(20.5F);
        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        result.add(arguments(jsonArr, 0, null, null, classCast));
        result.add(arguments(jsonArr, 1, null, true, null));
        result.add(arguments(jsonArr, 1, false, true, null));
        result.add(arguments(jsonArr, 2, null, false, null));
        result.add(arguments(jsonArr, 2, true, false, null));
        result.add(arguments(jsonArr, 3, null, null, null));
        result.add(arguments(jsonArr, 3, true, true, null));
        result.add(arguments(jsonArr, 3, false, false, null));
        result.add(arguments(jsonArr, 4, null, null, classCast));
        result.add(arguments(jsonArr, 5, null, null, classCast));
        result.add(arguments(jsonArr, 6, null, null, classCast));
        result.add(arguments(jsonArr, 7, null, null, classCast));
        result.add(arguments(jsonArr, 8, null, null, classCast));
        result.add(arguments(jsonArr, 9, null, null, classCast));
        result.add(arguments(jsonArr, 10, null, null, classCast));
        result.add(arguments(jsonArr, 11, null, null, classCast));
        result.add(arguments(null, 0, null, null, null));
        result.add(arguments(null, 0, true, true, null));
        result.add(arguments(null, 0, false, false, null));

        return result;

    }

    @ParameterizedTest
    @MethodSource("provideGetBooleanFromArrayParams")
    public void getBooleanFromArrayTest(JsonArray jsonArray,
            int index,
            Boolean defaultValue,
            Boolean expectedResult,
            Class expectedFailure) {
        try {
            Boolean result = getBoolean(jsonArray, index, defaultValue);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

            // check if the default value is null
            if (defaultValue == null) {
                result = getBoolean(jsonArray, index);

                assertEquals(expectedResult, result,
                        "Unexpected result with no default value "
                                + "parameter.  jsonArray=[ " + jsonArray
                                + " ], index=[ " + index + " ]");

            }
        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetJsonValueFromObjectParams() {
        final long maxIntPlus1 = ((long) Integer.MAX_VALUE) + 1L;
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("text", "ABC");
        job.add("number", 123);
        job.add("boolean", true);
        job.addNull("none");

        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(3).add(4);
        job.add("array", jab);

        JsonObjectBuilder job2 = Json.createObjectBuilder();
        job2.add("givenName", "Joe").add("surname", "Schmoe");
        job.add("object", job2);

        JsonObject jsonObj = job.build();

        JsonValue textValue = jsonObj.getValue("/text");
        JsonValue numValue = jsonObj.getValue("/number");
        JsonValue boolValue = jsonObj.getValue("/boolean");
        JsonValue nullValue = jsonObj.getValue("/none");
        JsonValue arrValue = jsonObj.getValue("/array");
        JsonValue objValue = jsonObj.getValue("/object");

        result.add(arguments(jsonObj, "text", textValue, null));
        result.add(arguments(jsonObj, "number", numValue, null));
        result.add(arguments(jsonObj, "boolean", boolValue, null));
        result.add(arguments(jsonObj, "array", arrValue, null));
        result.add(arguments(jsonObj, "object", objValue, null));
        result.add(arguments(jsonObj, "none", nullValue, null));

        result.add(arguments(jsonObj, "missing", null, null));
        result.add(arguments(null, "foo", null, null));
        result.add(arguments(null, null, null, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetJsonValueFromObjectParams")
    public void getJsonValueFromObjectTest(JsonObject jsonObject,
            String key,
            JsonValue expectedResult,
            Class expectedFailure) {
        try {
            JsonValue result = getJsonValue(jsonObject, key);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetJsonObjectFromObjectParams() {
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonObjectBuilder job2 = Json.createObjectBuilder();

        job2.add("givenName", "Joe").add("surname", "Schmoe");
        job.add("person1", job2);

        job2 = Json.createObjectBuilder();
        job2.add("givenName", "John").add("surname", "Doe");
        job.add("person2", job2);

        job2 = Json.createObjectBuilder();
        job2.add("givenName", "Jane").add("surname", "Doe");
        job.add("person3", job2);

        job.addNull("person4");

        job.add("count", 4);

        JsonObject jsonObj = job.build();

        Class classCast = ClassCastException.class;

        JsonObject person1 = jsonObj.getJsonObject("person1");
        JsonObject person2 = jsonObj.getJsonObject("person2");
        JsonObject person3 = jsonObj.getJsonObject("person3");

        result.add(arguments(jsonObj, "person1", person1, null));
        result.add(arguments(jsonObj, "person2", person2, null));
        result.add(arguments(jsonObj, "person3", person3, null));
        result.add(arguments(jsonObj, "person4", null, null));
        result.add(arguments(jsonObj, "missing", null, null));
        result.add(arguments(null, "foo", null, null));
        result.add(arguments(null, null, null, null));
        result.add(arguments(jsonObj, "count", null, classCast));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetJsonObjectFromObjectParams")
    public void getJsonObjectFromObjectTest(JsonObject jsonObject,
            String key,
            JsonObject expectedResult,
            Class expectedFailure) {
        try {
            JsonObject result = getJsonObject(jsonObject, key);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonObject=[ " + jsonObject
                            + " ], key=[ " + key + " ]");

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ], key=[ "
                        + key + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonObject=[ " + jsonObject
                        + " ], key=[ " + key + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    public List<Arguments> provideGetJsonObjectFromArrayParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        JsonObjectBuilder job2 = Json.createObjectBuilder();

        job2.add("givenName", "Joe").add("surname", "Schmoe");
        builder.add(job2);

        job2 = Json.createObjectBuilder();
        job2.add("givenName", "John").add("surname", "Doe");
        builder.add(job2);

        job2 = Json.createObjectBuilder();
        job2.add("givenName", "Jane").add("surname", "Doe");
        builder.add(job2);

        builder.addNull();

        builder.add(4);

        JsonArray jsonArr = builder.build();

        Class classCast = ClassCastException.class;

        JsonObject person1 = jsonArr.getJsonObject(0);
        JsonObject person2 = jsonArr.getJsonObject(1);
        JsonObject person3 = jsonArr.getJsonObject(2);

        result.add(arguments(jsonArr, 0, person1, null));
        result.add(arguments(jsonArr, 1, person2, null));
        result.add(arguments(jsonArr, 2, person3, null));
        result.add(arguments(jsonArr, 3, null, null));
        result.add(arguments(null, 0, null, null));
        result.add(arguments(jsonArr, 4, null, classCast));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideGetJsonObjectFromArrayParams")
    public void getJsonObjectFromArrayTest(JsonArray jsonArray,
            int index,
            JsonObject expectedResult,
            Class expectedFailure) {
        try {
            JsonObject result = getJsonObject(jsonArray, index);

            if (expectedFailure != null) {
                fail("Expected a failure, but got success.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], result=[ " + result
                        + " ], expectedFailure=[ " + expectedFailure.getName() + " ]");
            }

            assertEquals(expectedResult, result,
                    "Unexpected result.  jsonArray=[ " + jsonArray
                            + " ], index=[ " + index + " ]");

        } catch (Exception e) {
            if (expectedFailure == null) {
                e.printStackTrace();
                fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ], index=[ "
                        + index + " ], result=[ " + expectedResult + " ]: " + e);

            } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
                e.printStackTrace();
                fail("Expected a different failure.  jsonArray=[ " + jsonArray
                        + " ], index=[ " + index + " ], expectedFailure=[ "
                        + expectedFailure.getName() + " ]: " + e);
            }
        }
    }

    @Test
    public void addStringToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "text", "ABC");
            add(job, "null", (String) null);

            jsonObject = job.build();

            String textValue = getString(jsonObject, "text");
            String nullValue = getString(jsonObject, "null");

            assertEquals("ABC", textValue,
                    "Unexpected string result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/text");
            assertEquals(JsonValue.ValueType.STRING, jsonValue.getValueType(),
                    "Unexpected string value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addIntegerToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", 123);
            add(job, "null", (Integer) null);

            jsonObject = job.build();

            Integer numValue = getInteger(jsonObject, "number");
            Integer nullValue = getInteger(jsonObject, "null");

            assertEquals(123, numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addLongToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", 123L);
            add(job, "null", (Long) null);

            jsonObject = job.build();

            Long numValue = getLong(jsonObject, "number");
            Long nullValue = getLong(jsonObject, "null");

            assertEquals(123L, numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addDoubleToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", 123.456);
            add(job, "null", (Double) null);

            jsonObject = job.build();

            Double numValue = getDouble(jsonObject, "number");
            Double nullValue = getDouble(jsonObject, "null");

            assertEquals(123.456, numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addFloatToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", 123.456F);
            add(job, "null", (Float) null);

            jsonObject = job.build();

            Float numValue = getFloat(jsonObject, "number");
            Float nullValue = getFloat(jsonObject, "null");

            assertEquals(123.456F, numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addBigIntegerToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", new BigInteger("1234"));
            add(job, "null", (BigInteger) null);

            jsonObject = job.build();

            BigInteger numValue = getBigInteger(jsonObject, "number");
            BigInteger nullValue = getBigInteger(jsonObject, "null");

            assertEquals(new BigInteger("1234"), numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addBigDecimalToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "number", new BigDecimal("123.456"));
            add(job, "null", (BigDecimal) null);

            jsonObject = job.build();

            BigDecimal numValue = getBigDecimal(jsonObject, "number");
            BigDecimal nullValue = getBigDecimal(jsonObject, "null");

            assertEquals(new BigDecimal("123.456"), numValue,
                    "Unexpected numeric result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/number");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addBooleanToObjectTest() {
        JsonObject jsonObject = null;
        try {
            JsonObjectBuilder job = Json.createObjectBuilder();
            add(job, "true", true);
            add(job, "false", false);
            add(job, "null", (Boolean) null);

            jsonObject = job.build();

            Boolean trueValue = getBoolean(jsonObject, "true");
            Boolean falseValue = getBoolean(jsonObject, "false");
            Boolean nullValue = getBoolean(jsonObject, "null");

            assertEquals(true, trueValue,
                    "Unexpected boolean true result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(false, falseValue,
                    "Unexpected boolean false result.  jsonObject=[ "
                            + jsonObject + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonObject=[ "
                            + jsonObject + " ]");

            JsonValue jsonValue = jsonObject.getValue("/true");
            assertEquals(JsonValue.ValueType.TRUE, jsonValue.getValueType(),
                    "Unexpected boolean true value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/false");
            assertEquals(JsonValue.ValueType.FALSE, jsonValue.getValueType(),
                    "Unexpected boolean false value type.  jsonObject=[ "
                            + jsonObject + " ]");

            jsonValue = jsonObject.getValue("/null");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonObject=[ "
                            + jsonObject + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonObject=[ " + jsonObject + " ]: " + e);
        }
    }

    @Test
    public void addStringToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, "ABC");
            add(jab, (String) null);

            jsonArray = jab.build();

            String textValue = getString(jsonArray, 0);
            String nullValue = getString(jsonArray, 1);

            assertEquals("ABC", textValue,
                    "Unexpected string result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.STRING, jsonValue.getValueType(),
                    "Unexpected string value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addIntegerToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, 123);
            add(jab, (Integer) null);

            jsonArray = jab.build();

            Integer numValue = getInteger(jsonArray, 0);
            Integer nullValue = getInteger(jsonArray, 1);

            assertEquals(123, numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addLongToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, 123L);
            add(jab, (Long) null);

            jsonArray = jab.build();

            Long numValue = getLong(jsonArray, 0);
            Long nullValue = getLong(jsonArray, 1);

            assertEquals(123L, numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addDoubleToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, 123.456);
            add(jab, (Double) null);

            jsonArray = jab.build();

            Double numValue = getDouble(jsonArray, 0);
            Double nullValue = getDouble(jsonArray, 1);

            assertEquals(123.456, numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addFloatToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, 123.456F);
            add(jab, (Float) null);

            jsonArray = jab.build();

            Float numValue = getFloat(jsonArray, 0);
            Float nullValue = getFloat(jsonArray, 1);

            assertEquals(123.456F, numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addBigIntegerToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, new BigInteger("1234"));
            add(jab, (BigInteger) null);

            jsonArray = jab.build();

            BigInteger numValue = getBigInteger(jsonArray, 0);
            BigInteger nullValue = getBigInteger(jsonArray, 1);

            assertEquals(new BigInteger("1234"), numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addBigDecimalToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, new BigDecimal("123.456"));
            add(jab, (BigDecimal) null);

            jsonArray = jab.build();

            BigDecimal numValue = getBigDecimal(jsonArray, 0);
            BigDecimal nullValue = getBigDecimal(jsonArray, 1);

            assertEquals(new BigDecimal("123.456"), numValue,
                    "Unexpected numeric result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.NUMBER, jsonValue.getValueType(),
                    "Unexpected numeric value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    @Test
    public void addBooleanToArrayTest() {
        JsonArray jsonArray = null;
        try {
            JsonArrayBuilder jab = Json.createArrayBuilder();
            add(jab, true);
            add(jab, false);
            add(jab, (Boolean) null);

            jsonArray = jab.build();

            Boolean trueValue = getBoolean(jsonArray, 0);
            Boolean falseValue = getBoolean(jsonArray, 1);
            Boolean nullValue = getBoolean(jsonArray, 2);

            assertEquals(true, trueValue,
                    "Unexpected boolean true result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(false, falseValue,
                    "Unexpected boolean false result.  jsonArray=[ "
                            + jsonArray + " ]");
            assertEquals(null, nullValue,
                    "Unexpected null result.  jsonArray=[ "
                            + jsonArray + " ]");

            JsonValue jsonValue = jsonArray.getValue("/0");
            assertEquals(JsonValue.ValueType.TRUE, jsonValue.getValueType(),
                    "Unexpected boolean true value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/1");
            assertEquals(JsonValue.ValueType.FALSE, jsonValue.getValueType(),
                    "Unexpected boolean false value type.  jsonArray=[ "
                            + jsonArray + " ]");

            jsonValue = jsonArray.getValue("/2");
            assertEquals(JsonValue.ValueType.NULL, jsonValue.getValueType(),
                    "Unexpected null value type.  jsonArray=[ "
                            + jsonArray + " ]");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonArray=[ " + jsonArray + " ]: " + e);
        }
    }

    public List<Arguments> provideParseJsonObjectParams() {
        List<Arguments> result = new LinkedList<>();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("givenName", "John");
        builder.add("surname", "Doe");
        JsonObject jsonObject = builder.build();

        result.add(arguments(
                "{\"givenName\": \"John\", \"surname\": \"Doe\" }",
                jsonObject));

        builder = Json.createObjectBuilder();
        builder.add("foo", "Bar");
        builder.add("count", 10);
        builder.addNull("none");
        jsonObject = builder.build();

        result.add(arguments(
                "{\"foo\": \"Bar\", \"count\": 10, \"none\": null}",
                jsonObject));

        builder = Json.createObjectBuilder();
        jsonObject = builder.build();

        result.add(arguments("{ }", jsonObject));
        result.add(arguments(null, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideParseJsonObjectParams")
    public void parseJsonObjectTest(String jsonText,
            JsonObject expectedResult) {
        try {
            JsonObject result = parseJsonObject(jsonText);

            assertEquals(expectedResult, result,
                    "Unexpected result when parsing JSON text: "
                            + jsonText);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonText=[ " + jsonText
                    + " ], jsonObject=[ " + expectedResult
                    + " ]: " + e);
        }
    }

    public List<Arguments> provideParseJsonArrayParams() {
        List<Arguments> result = new LinkedList<>();
        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("John");
        builder.add("Doe");
        JsonArray jsonArray = builder.build();

        result.add(arguments("[\"John\", \"Doe\"]", jsonArray));

        builder = Json.createArrayBuilder();
        builder.add("Bar");
        builder.add(10);
        builder.addNull();
        jsonArray = builder.build();

        result.add(arguments("[\"Bar\", 10, null]", jsonArray));

        builder = Json.createArrayBuilder();
        jsonArray = builder.build();

        result.add(arguments("[ ]", jsonArray));
        result.add(arguments(null, null));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideParseJsonArrayParams")
    public void parseJsonArrayTest(String jsonText,
            JsonArray expectedResult) {
        try {
            JsonArray result = parseJsonArray(jsonText);

            assertEquals(expectedResult, result,
                    "Unexpected result when parsing JSON text: "
                            + jsonText);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonText=[ " + jsonText
                    + " ], jsonArray=[ " + expectedResult
                    + " ]: " + e);
        }
    }

    public List<Arguments> provideNormalizeJsonTextParams() {
        List<Arguments> result = new LinkedList<>();

        result.add(arguments(
                "{\"givenName\": \"John\", \"surname\": \"Doe\" }",
                Map.of("givenName", "John", "surname", "Doe")));

        Map map = new LinkedHashMap();
        map.put("foo", "Bar");
        map.put("count", 10L);
        map.put("none", null);

        result.add(arguments(
                "{\"foo\": \"Bar\", \"count\": 10, \"none\": null}", map));

        List list = new ArrayList();
        list.add("Bar");
        list.add(10L);
        list.add(null);

        result.add(arguments("[\"Bar\", 10, null]", list));

        result.add(arguments("{ }", Map.of()));
        result.add(arguments("[ ]", List.of()));
        result.add(arguments(null, null));
        result.add(arguments("null", null));
        result.add(arguments("true", true));
        result.add(arguments("false", false));
        result.add(arguments("10", 10L));
        result.add(arguments("20.5", 20.5));
        result.add(arguments("\"Foo\"", "Foo"));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideNormalizeJsonTextParams")
    public void normalizeJsonTextTest(String jsonText,
            Object expectedResult) {
        try {
            Object result = normalizeJsonText(jsonText);

            assertEquals(expectedResult, result,
                    "Unexpected result when normalizing JSON text: "
                            + jsonText);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonText=[ " + jsonText + " ]: " + e);
        }
    }

    public List<Arguments> provideNormalizeJsonValueParams() {
        List<Arguments> result = new LinkedList<>();

        JsonObject jsonObject = parseJsonObject(
                "{\"givenName\": \"John\", \"surname\": \"Doe\" }");

        result.add(arguments(
                jsonObject,
                Map.of("givenName", "John", "surname", "Doe")));

        Map map = new LinkedHashMap();
        map.put("foo", "Bar");
        map.put("count", 10L);
        map.put("none", null);

        jsonObject = parseJsonObject(
                "{\"foo\": \"Bar\", \"count\": 10, \"none\": null}");

        result.add(arguments(jsonObject, map));

        List list = new ArrayList();
        list.add("Bar");
        list.add(10L);
        list.add(null);

        JsonArray jsonArray = parseJsonArray("[\"Bar\", 10, null]");

        result.add(arguments(jsonArray, list));

        result.add(arguments(parseJsonObject("{ }"), Map.of()));
        result.add(arguments(parseJsonArray("[ ]"), List.of()));
        result.add(arguments(null, null));
        result.add(arguments(JsonValue.NULL, null));
        result.add(arguments(JsonValue.TRUE, true));
        result.add(arguments(JsonValue.FALSE, false));

        JsonNumber number = toJsonObject("value", 10)
                .getJsonNumber("value");

        result.add(arguments(number, 10L));

        number = toJsonObject("value", 20.5)
                .getJsonNumber("value");

        result.add(arguments(number, 20.5));

        JsonString string = toJsonObject("value", "Foo")
                .getJsonString("value");

        result.add(arguments(string, "Foo"));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideNormalizeJsonValueParams")
    public void normalizeJsonValueTest(JsonValue jsonValue,
            Object expectedResult) {
        try {
            Object result = normalizeJsonValue(jsonValue);

            assertEquals(expectedResult, result,
                    "Unexpected result when normalizing JSON value: "
                            + jsonValue);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  jsonValue=[ " + jsonValue + " ]: " + e);
        }
    }

    public List<Arguments> provideAddPropertyParams() {
        List<Arguments> result = new LinkedList<>();

        result.add(arguments(null, JsonValue.ValueType.NULL));
        result.add(arguments(true, JsonValue.ValueType.TRUE));
        result.add(arguments(false, JsonValue.ValueType.FALSE));
        result.add(arguments("ABC", JsonValue.ValueType.STRING));
        result.add(arguments(((short) 123), JsonValue.ValueType.NUMBER));
        result.add(arguments(123, JsonValue.ValueType.NUMBER));
        result.add(arguments(123L, JsonValue.ValueType.NUMBER));
        result.add(arguments(123.456, JsonValue.ValueType.NUMBER));
        result.add(arguments(123.456F, JsonValue.ValueType.NUMBER));
        result.add(arguments(
                new Object[] { 10L, 5.5, true, "three" }, JsonValue.ValueType.ARRAY));
        result.add(arguments(
                List.of(10L, 5.5, true, "three"), JsonValue.ValueType.ARRAY));
        result.add(arguments(
                Set.of(10L, 5.5, true, "three"), JsonValue.ValueType.ARRAY));
        result.add(arguments(
                Map.of("foo", "bar", "phoo", true, "num", 25L),
                JsonValue.ValueType.OBJECT));
        result.add(arguments(
                Map.of(1, 10, 2, 20), JsonValue.ValueType.STRING));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideAddPropertyParams")
    public void addPropertyTest(Object value,
            JsonValue.ValueType expectedType) {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            addProperty(builder, "value", value);
            JsonObject jsonObject = builder.build();

            assertTrue(jsonObject.containsKey("value"),
                    "Missing expected property key 'value'.");

            JsonValue jsonValue = jsonObject.getValue("/value");

            validateJsonValue(value, jsonValue, expectedType);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  value=[ " + value + " ]: " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideAddPropertyParams")
    public void addElementTest(Object value,
            JsonValue.ValueType expectedType) {
        try {
            JsonArrayBuilder builder = Json.createArrayBuilder();
            addElement(builder, value);
            JsonArray jsonArray = builder.build();

            assertEquals(1, jsonArray.size(),
                    "Json array is incorrect size after addElement: "
                            + jsonArray);

            JsonValue jsonValue = jsonArray.getValue("/0");

            validateJsonValue(value, jsonValue, expectedType);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure.  value=[ " + value + " ]: " + e);
        }
    }

    private void validateJsonValue(Object value,
            JsonValue jsonValue,
            JsonValue.ValueType expectedType) {
        assertEquals(expectedType, jsonValue.getValueType(),
                "Unexpected value type after adding property: "
                        + value + " / "
                        + ((value == null) ? null : value.getClass().getName()));

        if (value != null) {
            Object actual = null;
            Object expected = value;
            switch (value.getClass().getName()) {
                case "java.lang.String":
                    actual = ((JsonString) jsonValue).getString();
                    break;
                case "java.lang.Boolean":
                    actual = (jsonValue.equals(JsonValue.TRUE)) ? true
                            : (jsonValue.equals(JsonValue.FALSE)) ? false : jsonValue;
                    break;
                case "java.lang.Integer":
                    actual = ((JsonNumber) jsonValue).intValue();
                    break;
                case "java.lang.Long":
                    actual = ((JsonNumber) jsonValue).longValue();
                    break;
                case "java.lang.Short":
                    actual = ((JsonNumber) jsonValue).numberValue().shortValue();
                    break;
                case "java.lang.Float":
                    actual = ((JsonNumber) jsonValue).numberValue().floatValue();
                    break;
                case "java.lang.Double":
                    actual = ((JsonNumber) jsonValue).doubleValue();
                    break;
                case "java.math.BigInteger":
                    actual = ((JsonNumber) jsonValue).bigIntegerValue();
                    break;
                case "java.math.BigDecimal":
                    actual = ((JsonNumber) jsonValue).bigDecimalValue();
                    break;
                default:
                    if (value.getClass().isArray()) {
                        // convert to a list
                        actual = normalizeJsonValue(jsonValue);

                        int length = Array.getLength(value);
                        List list = new ArrayList<>(length);
                        for (int index = 0; index < length; index++) {
                            list.add(Array.get(value, index));
                        }
                        expected = list;

                    } else if (value instanceof Collection) {
                        // convert to a list
                        actual = normalizeJsonValue(jsonValue);
                        expected = new ArrayList((Collection) value);

                    } else if (value instanceof Map) {
                        boolean stringKeys = true;
                        for (Object key : ((Map) value).keySet()) {
                            if (key == null || (!(key instanceof String))) {
                                stringKeys = false;
                                break;
                            }
                        }
                        if (stringKeys) {
                            // convert to a Map
                            actual = normalizeJsonValue(jsonValue);

                        } else {
                            actual = ((JsonString) jsonValue).getString();
                            expected = value.toString();
                        }
                    } else {
                        actual = ((JsonString) jsonValue).getString();
                        expected = value.toString();
                    }
                    break;
            }

            assertEquals(expected, actual,
                    "The property value is not what was expected: "
                            + value + " / " + jsonValue.getValueType() + " / "
                            + jsonValue);
        }
    }

    @Test
    public void testToJsonObject1() {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("foo", "bar");
            JsonObject expected = builder.build();

            JsonObject actual = toJsonObject("foo", "bar");

            assertEquals(expected, actual,
                    "Unexpected result for toJsonObject() with 1 argument");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonObject1()");
        }
    }

    @Test
    public void testToJsonObject2() {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("foo", "bar");
            builder.add("phoo", true);
            JsonObject expected = builder.build();

            JsonObject actual = toJsonObject(
                    "foo", "bar",
                    "phoo", true);

            assertEquals(expected, actual,
                    "Unexpected result for toJsonObject() with 2 arguments");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonObject2()");
        }

    }

    @Test
    public void testToJsonObject3() {
        try {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("foo", "bar");
            builder.add("phoo", true);
            builder.add("num", 10L);
            JsonObject expected = builder.build();

            JsonObject actual = toJsonObject(
                    "foo", "bar",
                    "phoo", true,
                    "num", 10L);

            assertEquals(expected, actual,
                    "Unexpected result for toJsonObject() with 3 arguments");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonObject3()");
        }

    }

    public List<Arguments> provideToJsonObjectParams() {
        List<Arguments> result = new LinkedList<>();

        Map<String, Object> map1 = new LinkedHashMap<>();
        map1.put("text", "ABC");
        map1.put("none", null);
        map1.put("int", 10);
        map1.put("long", 20L);
        map1.put("double", 10.5);
        map1.put("float", 20.5F);
        map1.put("bigInt", new BigInteger("30"));
        map1.put("bigDec", new BigDecimal("40.5"));
        map1.put("true", Boolean.TRUE);
        map1.put("false", Boolean.FALSE);
        map1.put("array", List.of(1, 2, true, false));
        map1.put("object",
                Map.of("givenName", "Joe", "surname", "Schmoe"));
        map1.put("random", Map.of(1, 10, 2, 20, 3, 30));

        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(true).add(false);

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("givenName", "Joe").add("surname", "Schmoe");

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("text", "ABC");
        builder.addNull("none");
        builder.add("int", 10);
        builder.add("long", 20L);
        builder.add("double", 10.5);
        builder.add("float", 20.5F);
        builder.add("bigInt", new BigInteger("30"));
        builder.add("bigDec", new BigDecimal("40.5"));
        builder.add("true", Boolean.TRUE);
        builder.add("false", Boolean.FALSE);
        builder.add("array", jab);
        builder.add("object", job);
        builder.add("random", map1.get("random").toString());

        JsonObject jsonObject1 = builder.build();
        result.add(arguments(
                map1.get("object"), jsonObject1.getJsonObject("object")));
        result.add(arguments(map1, jsonObject1));

        Map<String, Object> map2 = Map.of();
        builder = Json.createObjectBuilder();
        JsonObject jsonObject2 = builder.build();

        result.add(arguments(map2, jsonObject2));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideToJsonObjectParams")
    public void testToJsonObject(Map<String, ?> map,
            JsonObject expectedResult) {
        try {
            JsonObject actual = toJsonObject(map);

            assertEquals(expectedResult, actual,
                    "Unexpected result for toJsonObject() with map: "
                            + map);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonObject(): " + map + " / "
                    + expectedResult);
        }
    }

    public List<Arguments> provideToJsonArrayParams() {
        List<Arguments> result = new LinkedList<>();

        Map subObj = Map.of("givenName", "Joe", "surname", "Schmoe");
        Map subMap = Map.of(1, 10, 2, 20, 3, 30);
        List subList = List.of(1, 2, true, false);
        List<Object> list1 = new LinkedList();
        list1.add("ABC");
        list1.add(null);
        list1.add(10);
        list1.add(20L);
        list1.add(10.5);
        list1.add(20.5F);
        list1.add(new BigInteger("30"));
        list1.add(new BigDecimal("40.5"));
        list1.add(Boolean.TRUE);
        list1.add(Boolean.FALSE);
        list1.add(subList);
        list1.add(subObj);
        list1.add(subMap);

        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add(2).add(true).add(false);

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("givenName", "Joe").add("surname", "Schmoe");

        JsonArrayBuilder builder = Json.createArrayBuilder();
        builder.add("ABC");
        builder.addNull();
        builder.add(10);
        builder.add(20L);
        builder.add(10.5);
        builder.add(20.5F);
        builder.add(new BigInteger("30"));
        builder.add(new BigDecimal("40.5"));
        builder.add(Boolean.TRUE);
        builder.add(Boolean.FALSE);
        builder.add(jab);
        builder.add(job);
        builder.add(subMap.toString());

        JsonArray jsonArray1 = builder.build();
        result.add(arguments(subList, jsonArray1.getJsonArray(10)));
        result.add(arguments(list1, jsonArray1));

        List<Object> list2 = List.of();
        builder = Json.createArrayBuilder();
        JsonArray jsonArray2 = builder.build();

        result.add(arguments(list2, jsonArray2));

        return result;
    }

    @Test
    public void testToJsonArrayN() {
        try {
            JsonArrayBuilder builder = Json.createArrayBuilder();
            builder.add("bar");
            builder.add(true);
            builder.add(10L);
            JsonArray expected = builder.build();

            JsonArray actual = toJsonArray("bar", true, 10L);

            assertEquals(expected, actual,
                    "Unexpected result for toJsonArray() with N arguments");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonObjectN()");
        }

    }

    @ParameterizedTest
    @MethodSource("provideToJsonArrayParams")
    public void testToJsonArray(List<?> list,
            JsonArray expectedResult) {
        try {
            JsonArray actual = toJsonArray(list);

            assertEquals(expectedResult, actual,
                    "Unexpected result for toJsonArray() with list: "
                            + list);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for testToJsonArray(): " + list + " / "
                    + expectedResult);
        }
    }

    public List<Arguments> provideToJsonTextParams() {
        List<Arguments> result = new LinkedList<>();

        JsonArrayBuilder jab = Json.createArrayBuilder();
        jab.add(1).add("two").add(true).add(false).addNull();

        String arrText = "[ 1, \"two\", true, false, null ]";

        JsonObjectBuilder job = Json.createObjectBuilder();
        job.add("foo", "bar").add("num", 5).add("true", true);

        String objText = "{ \"foo\": \"bar\", \"num\": 5, \"true\": true }";

        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("int", 5);
        builder.addNull("null");
        builder.add("array", jab);
        builder.add("object", job);
        builder.add("text", "ABC");

        JsonObject jsonObject = builder.build();

        result.add(arguments(jsonObject.getValue("/int"), "5"));
        result.add(arguments(jsonObject.getValue("/null"), "null"));
        result.add(arguments(jsonObject.getValue("/array"), arrText));
        result.add(arguments(jsonObject.getValue("/object"), objText));
        result.add(arguments(jsonObject.getValue("/text"), "\"ABC\""));

        String fullText = "{ "
                + "\"int\": 5, \"null\": null, "
                + "\"array\": " + arrText + ", "
                + "\"object\": " + objText + ", "
                + "\"text\": \"ABC\" }";

        result.add(arguments(jsonObject, fullText));

        return result;
    }

    private static int getPrettyPrintLineCount(JsonValue jsonValue) {
        if (jsonValue instanceof JsonArray) {
            int lineCount = 1; // for the opening bracket
            JsonArray jsonArray = (JsonArray) jsonValue;
            for (JsonValue value : jsonArray) {
                lineCount += getPrettyPrintLineCount(value);
            }
            lineCount++; // for the closing bracket
            return lineCount;

        } else if (jsonValue instanceof JsonStructure) {
            JsonObject jsonObject = (JsonObject) jsonValue;
            int lineCount = 1; // for the opening curly brace
            for (JsonValue value : jsonObject.values()) {
                lineCount += getPrettyPrintLineCount(value);
            }
            lineCount++; // for the closing curly brace
            return lineCount;

        } else {
            return 1;
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText1(JsonValue jsonValue, String expectedText) {
        try {
            String result = toJsonText(jsonValue);

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            result = toJsonText(jsonValue, true);
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(jsonValue);
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText2(JsonValue jsonValue, String expectedText) {
        try {
            JsonObjectBuilder builder1 = null;
            JsonObjectBuilder builder2 = null;
            JsonObjectBuilder builder3 = null;
            if (jsonValue instanceof JsonObject) {
                builder1 = Json.createObjectBuilder((JsonObject) jsonValue);
                builder2 = Json.createObjectBuilder((JsonObject) jsonValue);
                builder3 = Json.createObjectBuilder((JsonObject) jsonValue);
            } else {
                builder1 = Json.createObjectBuilder();
                builder1.add("value", jsonValue);
                builder2 = Json.createObjectBuilder();
                builder2.add("value", jsonValue);
                builder3 = Json.createObjectBuilder();
                builder3.add("value", jsonValue);
                expectedText = "{ \"value\": " + expectedText + " }";
            }

            StringWriter writer = new StringWriter();
            String result = toJsonText(writer, builder1).toString();

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            writer = new StringWriter();
            result = toJsonText(writer, builder2, true).toString();
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(builder3.build());
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText3(JsonValue jsonValue, String expectedText) {
        try {
            JsonArrayBuilder builder1 = null;
            JsonArrayBuilder builder2 = null;
            JsonArrayBuilder builder3 = null;
            if (jsonValue instanceof JsonArray) {
                builder1 = Json.createArrayBuilder((JsonArray) jsonValue);
                builder2 = Json.createArrayBuilder((JsonArray) jsonValue);
                builder3 = Json.createArrayBuilder((JsonArray) jsonValue);
            } else {
                builder1 = Json.createArrayBuilder();
                builder1.add(jsonValue);
                builder2 = Json.createArrayBuilder();
                builder2.add(jsonValue);
                builder3 = Json.createArrayBuilder();
                builder3.add(jsonValue);
                expectedText = "[ " + expectedText + " ]";
            }

            StringWriter writer = new StringWriter();
            String result = toJsonText(writer, builder1).toString();

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            writer = new StringWriter();
            result = toJsonText(writer, builder2, true).toString();
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(builder3.build());
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText4(JsonValue jsonValue, String expectedText) {
        try {
            StringWriter writer = new StringWriter();
            String result = toJsonText(writer, jsonValue).toString();

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            writer = new StringWriter();
            result = toJsonText(jsonValue, true).toString();
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(jsonValue);
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText5(JsonValue jsonValue, String expectedText) {
        try {
            JsonObjectBuilder builder1 = null;
            JsonObjectBuilder builder2 = null;
            JsonObjectBuilder builder3 = null;
            if (jsonValue instanceof JsonObject) {
                builder1 = Json.createObjectBuilder((JsonObject) jsonValue);
                builder2 = Json.createObjectBuilder((JsonObject) jsonValue);
                builder3 = Json.createObjectBuilder((JsonObject) jsonValue);
            } else {
                builder1 = Json.createObjectBuilder();
                builder1.add("value", jsonValue);
                builder2 = Json.createObjectBuilder();
                builder2.add("value", jsonValue);
                builder3 = Json.createObjectBuilder();
                builder3.add("value", jsonValue);
                expectedText = "{ \"value\": " + expectedText + " }";
            }

            String result = toJsonText(builder1);

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            result = toJsonText(builder2, true);
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(builder3.build());
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    @ParameterizedTest
    @MethodSource("provideToJsonTextParams")
    public void testToJsonText6(JsonValue jsonValue, String expectedText) {
        try {
            JsonArrayBuilder builder1 = null;
            JsonArrayBuilder builder2 = null;
            JsonArrayBuilder builder3 = null;
            if (jsonValue instanceof JsonArray) {
                builder1 = Json.createArrayBuilder((JsonArray) jsonValue);
                builder2 = Json.createArrayBuilder((JsonArray) jsonValue);
                builder3 = Json.createArrayBuilder((JsonArray) jsonValue);
            } else {
                builder1 = Json.createArrayBuilder();
                builder1.add(jsonValue);
                builder2 = Json.createArrayBuilder();
                builder2.add(jsonValue);
                builder3 = Json.createArrayBuilder();
                builder3.add(jsonValue);
                expectedText = "[ " + expectedText + " ]";
            }

            String result = toJsonText(builder1);

            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected result for toJsonText(): " + jsonValue);

            result = toJsonText(builder2, true);
            assertEquals(expectedText.replaceAll("\\s", ""),
                    result.replaceAll("\\s", ""),
                    "Unexpected pretty-print result for toJsonText(): "
                            + jsonValue);

            result = result.trim();

            int expectedLineCount = getPrettyPrintLineCount(builder3.build());
            StringReader sr = new StringReader(result);
            BufferedReader br = new BufferedReader(sr);
            int actualLineCount = 0;
            while (br.readLine() != null) {
                actualLineCount++;
            }

            assertEquals(expectedLineCount, actualLineCount,
                    "Pretty-print JSON line count is unexpected: "
                            + result);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for toJsonText(): " + jsonValue + " / " + e);
        }
    }

    public List<Arguments> provideIniToJsonParams() {
        LinkedList<Arguments> result = new LinkedList<>();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("[BASIC]");
        pw.println(" FOO=BAR");
        pw.println(" PHOO=BAX");
        pw.println();
        pw.println("[ADVANCED]");
        pw.println(" FOOX=BARQS");
        pw.println(" PHOOX=BAXTER");
        pw.flush();

        JsonObject jsonObject = toJsonObject(
                Map.of("BASIC",
                        Map.of("FOO", "BAR", "PHOO", "BAX"),
                        "ADVANCED",
                        Map.of("FOOX", "BARQS", "PHOOX", "BAXTER")));

        result.add(arguments(sw.toString(), jsonObject));

        sw = new StringWriter();
        pw = new PrintWriter(sw);

        pw.println("[BASIC]");
        pw.println(" FOO=BAR");
        pw.println(" PHOO=BAX");
        pw.println(" FOOX=BARQS");
        pw.println(" PHOOX=BAXTER");
        pw.flush();

        jsonObject = toJsonObject(
                Map.of("BASIC",
                        Map.of("FOO", "BAR", "PHOO", "BAX",
                                "FOOX", "BARQS", "PHOOX", "BAXTER")));

        result.add(arguments(sw.toString(), jsonObject));

        return result;
    }

    @ParameterizedTest
    @MethodSource("provideIniToJsonParams")
    public void iniToJsonTest(String iniText, JsonObject expectedResult) {
        File file = null;
        try {
            file = File.createTempFile("test-", ".ini");
            file.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(file);
                    OutputStreamWriter osw = new OutputStreamWriter(fos)) {
                osw.write(iniText);
                osw.flush();
            }

            JsonObject result = iniToJson(file);

            assertEquals(expectedResult, result,
                    "Incorrect JSON for INI: " + iniText);

        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected failure for iniToJson(): " + iniText + " / " + e);

        } finally {
            if (file != null) {
                file.delete();
            }
        }
    }

    private static JsonObject createObject(String key, String value) {
        JsonObjectBuilder job = Json.createObjectBuilder();
        if (value != null) {
            job.add(key, value);
        } else {
            job.addNull(key);
        }
        return job.build();
    }

    /**
     * Provides the following parameters:
     * <ol>
     *   <li>A {@link JsonObject} with a propery that has a
     *       date-formatted string value (or a null or exceptional
     *       value).
     *   <li>The propery key that should be tried for
     *       retrieval as a date.
     *   <li>The expected value to get back as an {@link Instant},
     *       or <code>null</code>
     *   <li>The type of exception to expect, or <code>null</code> if
     *       no exception is expected.
     * </ol>
     * 
     * @return The {@link List} of {@link Argumebts} as described above.
     */
    public List<Arguments> getJsonDateObjectParameters() {
        List<Arguments> result = new LinkedList<>();
        long offset = 0;
        Instant instant = Instant.now();
        for (int index = 0; index < 10; index++) {
            // get a distinct instant in time
            instant = instant.minus(offset, ChronoUnit.MINUTES);
            offset = (offset + 10) * (index + 1);

            // format the instant as a JSON date
            String text = JsonUtilities.DATE_TIME_FORMATTER.format(instant);

            // parse it back to account for precision loss due to JSON date format
            instant = Instant.from(JsonUtilities.DATE_TIME_FORMATTER.parse(text));

            // generate a distinct property key
            String key = TextUtilities.randomAlphabeticText(3, 8);

            // add a test case that should successfully obtain a date value
            result.add(Arguments.of(createObject(key, text), key, instant, null));
        }

        // add a test case that will result in a null date value
        result.add(Arguments.of(
            createObject("phoo", null), "phoo", null, null));

        // add a test case that will fail to parse
        result.add(Arguments.of(
            createObject("foo", "bar"), "foo", null, DateTimeParseException.class));
        return result;
    }

    @MethodSource("getJsonDateObjectParameters")
    @ParameterizedTest
    public void testGetInstantFromObject(JsonObject                 obj, 
                                         String                     key, 
                                         Instant                    expected, 
                                         Class<? extends Throwable> exceptionType)
    {
        try {
            Instant instant = JsonUtilities.getInstant(obj, key);
            if (exceptionType != null) {
                fail("Succeeded on getInstant() for key (" + key + ") when should have "
                     + "failed: " + JsonUtilities.toJsonText(obj));
            }
            assertEquals(expected, instant, "Instant value for key (" + key
                + ") is not as expected: " + JsonUtilities.toJsonText(obj));

            String nullResultKey = (expected == null) ? key : key + "-missing";
            Instant now = Instant.now();
            instant = JsonUtilities.getInstant(obj, nullResultKey, now);
            assertEquals(now, instant, "Defaulted instant value is not as expected: "
                + JsonUtilities.toJsonText(obj));
            
        } catch (Exception e) {
            if (exceptionType == null || !exceptionType.isInstance(e)) {
                fail("Unexpected exception on getInstant() for key (" + key 
                     + "): " + JsonUtilities.toJsonText(obj), e);
            }
        }
    }
    
    @MethodSource("getJsonDateObjectParameters")
    @ParameterizedTest
    public void testGetDateFromObject(JsonObject                    obj, 
                                      String                        key, 
                                      Instant                       expected, 
                                      Class<? extends Throwable>    exceptionType)
    {
        try {
            Date date = JsonUtilities.getDate(obj, key);
            if (exceptionType != null) {
                fail("Succeeded on getDate() for key (" + key + ") when should have "
                     + "failed: " + JsonUtilities.toJsonText(obj));
            }
            Date expectedDate = (expected == null) ? null : Date.from(expected);

            assertEquals(expectedDate, date, "Date value for key (" + key
                + ") is not as expected: " + JsonUtilities.toJsonText(obj));

            String missingKey = (expected == null) ? key : key + "-missing";
            Date now = Date.from(Instant.now());
            date = JsonUtilities.getDate(obj, missingKey, now);
            assertEquals(now, date, "Defaulted date value is not as expected: "
                + JsonUtilities.toJsonText(obj));

        } catch (Exception e) {
            if (exceptionType == null || !exceptionType.isInstance(e)) {
                fail("Unexpected exception on getDate() for key (" + key 
                     + "): " + JsonUtilities.toJsonText(obj), e);
            }
        }
    }

    private static JsonArray createArray(int index, String value) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (int i = 0; i < index * 2; i++) {
            if (i != index) {
                if (i % 2 == 0) {
                    jab.addNull(i);
                } else {
                    jab.add(TextUtilities.randomAlphabeticText(2, 4));
                }
            } else {
                if (value != null) {
                    jab.add(value);
                } else {
                    jab.addNull();
                }
            }
        }
        return jab.build();
    }

    /**
     * Provides the following parameters:
     * <ol>
     *   <li>A {@link JsonArray} of at least 4 elements, one of which
     *       being a date-formatted string (or a null or exceptional
     *       value).
     *   <li>The index of the array element that should be tried for
     *       retrieval as a date.
     *   <li>The expected value to get back as an {@link Instant},
     *       or <code>null</code>
     *   <li>The type of exception to expect, or <code>null</code> if
     *       no exception is expected.
     * </ol>
     * 
     * @return The {@link List} of {@link Argumebts} as described above.
     */
    public List<Arguments> getJsonDateArrayParameters() {
        List<Arguments> result = new LinkedList<>();
        long offset = 0;
        Instant instant = Instant.now();
        for (int index = 0; index < 10; index++) {
            // get a distinct instant in tme
            instant = instant.minus(offset, ChronoUnit.MINUTES);
            offset = (offset + 10) * (index + 1);

            // format as a date string
            String text = JsonUtilities.DATE_TIME_FORMATTER.format(instant);

            // parse it back as an instant to account for lot precision
            // from the JSON date formatting
            instant = Instant.from(JsonUtilities.DATE_TIME_FORMATTER.parse(text));

            // pick an array index between 2 and 4
            int arrayIndex = (index % 3) + 2;

            // add a case that should work with varying array indexes and 
            // date values
            result.add(Arguments.of(
                createArray(arrayIndex, text), arrayIndex, instant, null));
        }
        // add a case that will result in a null value
        result.add(Arguments.of(
            createArray(2, null), 2, null, null));

        // add a case that will NOT parse as a date
        result.add(Arguments.of(
            createArray(3, "bar"), 3, null, DateTimeParseException.class));

        // return the result
        return result;
    }

    @MethodSource("getJsonDateArrayParameters")
    @ParameterizedTest
    public void testGetInstantFromArray(JsonArray                   arr, 
                                         int                        index, 
                                         Instant                    expected, 
                                         Class<? extends Throwable> exceptionType)
    {
        try {
            Instant instant = JsonUtilities.getInstant(arr, index);
            if (exceptionType != null) {
                fail("Succeeded on getInstant() for index (" + index + ") when should have "
                     + "failed: " + JsonUtilities.toJsonText(arr));
            }
            assertEquals(expected, instant, "Instant value at index (" + index 
                + ") is not as expected: " + JsonUtilities.toJsonText(arr));

            int nullIndex = (expected == null) ? index : 0;
            Instant now = Instant.now();
            instant = JsonUtilities.getInstant(arr, nullIndex, now);
            assertEquals(now, instant, "Defaulted instant value is not as expected: "
                + JsonUtilities.toJsonText(arr));
            
        } catch (Exception e) {
            if (exceptionType == null || !exceptionType.isInstance(e)) {
                fail("Unexpected exception on getInstant() for index (" + index
                     + "): " + JsonUtilities.toJsonText(arr), e);
            }
        }
    }
    
    @MethodSource("getJsonDateArrayParameters")
    @ParameterizedTest
    public void testGetDateFromArray(JsonArray                  arr, 
                                     int                        index,
                                     Instant                    expected, 
                                     Class<? extends Throwable> exceptionType)
    {
        try {
            Date date = JsonUtilities.getDate(arr, index);
            if (exceptionType != null) {
                fail("Succeeded on getDate() for index (" + index + ") when should have "
                     + "failed: " + JsonUtilities.toJsonText(arr));
            }
            Date expectedDate = (expected == null) ? null : Date.from(expected);
            assertEquals(expectedDate, date, "Date value at index (" + index 
                + ") is not as expected: " + JsonUtilities.toJsonText(arr));

            int nullIndex = (expected == null) ? index : 0;
            Date now = Date.from(Instant.now());
            date = JsonUtilities.getDate(arr, nullIndex, now);
            assertEquals(now, date, "Defaulted date value is not as expected: "
                + JsonUtilities.toJsonText(arr));

        } catch (Exception e) {
            if (exceptionType == null || !exceptionType.isInstance(e)) {
                fail("Unexpected exception on getDate() for index (" + index 
                     + "): " + JsonUtilities.toJsonText(arr), e);
            }
        }
    }

}
