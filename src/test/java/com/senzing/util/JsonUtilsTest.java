package com.senzing.util;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.json.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.senzing.util.JsonUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link JsonUtils}.
 */
@SuppressWarnings("unchecked")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class JsonUtilsTest {

  public List<Arguments> provideGetStringParams() {
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
  @MethodSource("provideGetStringParams")
  public void getStringTest(JsonObject  jsonObject,
                            String      key,
                            String      defaultValue,
                            String      expectedResult,
                            Class       expectedFailure)
  {
    try {
      String result = getString(jsonObject, key,  defaultValue);

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

  public List<Arguments> provideGetJsonArrayParams() {
    List<Arguments> result = new LinkedList<>();
    JsonArrayBuilder jab = Json.createArrayBuilder();
    jab.add(1).add(2).add(3);

    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("items", jab);
    job.add("name", "Foo");

    JsonObjectBuilder job2 = Json.createObjectBuilder();
    job2.add("name", "Bar");
    job.add("child", job2);

    JsonObject  jsonObj = job.build();
    JsonArray   array   = jsonObj.getJsonArray("items");

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
  @MethodSource("provideGetJsonArrayParams")
  public void getJsonArrayTest(JsonObject jsonObject,
                               String     key,
                               JsonArray  expectedResult,
                               Class      expectedFailure)
  {
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

  public List<Arguments> provideGetStringsParams() {
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

    JsonObject  jsonObj = job.build();

    JsonObject  childObj = jsonObj.getJsonArray("mixed").getJsonObject(6);
    String      childText = JsonUtils.toJsonText(childObj, true);

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
    result.add(arguments(jsonObj, "name", null, null, classCast));

    return result;
  }

  @ParameterizedTest
  @MethodSource("provideGetStringsParams")
  public void getStringsTest(JsonObject   jsonObject,
                             String       key,
                             List<String> defaultValue,
                             List<String> expectedResult,
                             Class        expectedFailure)
  {
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

  public List<Arguments> provideGetIntegerParams() {
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
  @MethodSource("provideGetIntegerParams")
  public void getIntegerTest(JsonObject jsonObject,
                             String     key,
                             Integer    defaultValue,
                             Integer    expectedResult,
                             Class      expectedFailure)
  {
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

}
