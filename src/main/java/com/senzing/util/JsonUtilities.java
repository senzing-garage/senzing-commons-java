package com.senzing.util;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import javax.json.*;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

public class JsonUtilities {
  /**
   * Pretty printing {@link JsonWriterFactory}.
   */
  private static JsonWriterFactory PRETTY_WRITER_FACTORY
      = Json.createWriterFactory(
      Collections.singletonMap(PRETTY_PRINTING, true));

  /**
   * Private constructor since this class only has static methods.
   */
  private JsonUtilities() {
    // do nothing
  }

  /**
   * Gets a {@link String} value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * <code>null</code> is returned, otherwise the {@link String} value is
   * returned.  <b>NOTE:</b> This function will convert any non-string
   * value that is found to a {@link String} representation.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link String} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static String getString(JsonObject obj, String key)
  {
    return getString(obj, key, null);
  }

  /**
   * Gets a {@link String} value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link String}
   * value is returned.  <b>NOTE:</b> This function will convert any
   * non-string value that is found to a {@link String} representation.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link String} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static String getString(JsonObject obj,
                                 String     key,
                                 String     defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    JsonValue jsonValue = obj.get(key);
    return valueAsString(jsonValue, defaultValue);
  }

  /**
   * Gets a {@link String} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If the specified index has a null
   * value then <code>null</code> is returned, otherwise the {@link String}
   * value is returned.  <b>NOTE:</b> This function will convert any
   * non-string value that is found to a {@link String} representation.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link String} value at the index, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static String getString(JsonArray arr, int index)
  {
    return getString(arr, index, null);
  }

  /**
   * Gets a String value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link String}
   * value is returned.  <b>NOTE:</b> This function will convert any
   * non-string value that is found to a {@link String} representation.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the value at the array position
   *                     is <code>null</code>.
   *
   * @return The {@link String} value at the specified index or specified
   *         default value if the JSON value is <code>null</code>.
   */
  public static String getString(JsonArray arr, int index, String defaultValue)
  {
    if (arr == null) return defaultValue;
    JsonValue jsonValue = arr.get(index);
    return valueAsString(jsonValue, defaultValue);
  }

  /**
   * Converts a {@link JsonValue} to a {@link String}.
   *
   * @param jsonValue The {@link JsonValue} to convert.
   * @return The {@link String} representation of the value, or
   *         <code>null</code> if the specified {@link JsonValue}
   *         is <code>null</code>.
   */
  private static String valueAsString(JsonValue jsonValue)
  {
    return valueAsString(jsonValue, null);
  }

  /**
   * Converts a {@link JsonValue} to a {@link String}.
   *
   * @param jsonValue The {@link JsonValue} to convert.
   * @param defaultValue The default value to return if the specified {@link
   *                     JsonValue} is <code>null</code> or {@link
   *                     JsonValue.ValueType#NULL}.
   * @return The {@link String} representation of the value, or the specified
   *         default value if <code>null</code>.
   */
  private static String valueAsString(JsonValue jsonValue, String defaultValue)
  {
    if (jsonValue == null) return defaultValue;
    switch (jsonValue.getValueType()) {
      case STRING:
        return ((JsonString) jsonValue).getString();
      case NULL:
        return defaultValue;
      case TRUE:
        return Boolean.TRUE.toString();
      case FALSE:
        return Boolean.FALSE.toString();
      case NUMBER:
        return "" + ((JsonNumber) jsonValue).numberValue();
      default:
        return JsonUtilities.toJsonText(jsonValue, true);
    }
  }

  /**
   * Obtains the {@link JsonArray} for specified key from the specified
   * {@link JsonObject}.  If there is no value associated with the specified
   * key or if the value is null, then <code>null</code> is returned.
   *
   * @param obj The {@link JsonObject} from which to obtain the array.
   *
   * @param key The key for the property to obtain.
   *
   * @return The {@link JsonArray} for the specified key, or <code>null</code>
   *         if not found or if the value is null.
   */
  public static JsonArray getJsonArray(JsonObject obj, String key) {
    if (obj == null) return null;
    if (!obj.containsKey(key) || obj.isNull(key)) return null;
    return obj.getJsonArray(key);
  }

  /**
   * Obtains the {@link JsonArray} at the specified position from the specified
   * {@link JsonArray}.  If the JSON value at the position is null, then
   * <code>null</code> is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link JsonArray} for the specified key, or <code>null</code>
   *         if not found or if the value is null.
   */
  public static JsonArray getJsonArray(JsonArray arr, int index) {
    if (arr == null) return null;
    if (arr.isNull(index)) return null;
    return arr.getJsonArray(index);
  }

  /**
   * Obtains a {@link List} of {@link String} instances from a {@link JsonArray}
   * of {@link JsonString} instances bound to the specified {@link JsonObject}
   * by the specified key.  If the specified key is missing or has a null value
   * then <code>null</code> is returned, otherwise a {@link List} containing the
   * {@link String} instances from the array is returned.
   *
   * @param obj The {@link JsonObject} from which to obtain the {@link String}
   *            values.
   *
   * @param key The non-null {@link String} key for the values.
   *
   * @return A {@link List} of {@link String} values for the values found in
   *         the array or <code>null</code> if the associated key is missing or its
   *         value is null.
   */
  public static List<String> getStrings(JsonObject obj, String key)
  {
    return getStrings(obj, key, null);
  }

  /**
   * Obtains a {@link List} of {@link String} instances from a {@link JsonArray}
   * of {@link JsonString} instances bound to the specified {@link JsonObject}
   * by the specified key.  If the specified key is missing or has a null value
   * then <code>null</code> is returned, otherwise a {@link List} containing the
   * {@link String} instances from the array is returned.
   *
   * @param obj The {@link JsonObject} from which to obtain the {@link String}
   *            values.
   *
   * @param key The non-null {@link String} key for the values.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return A {@link List} of {@link String} values for the values found in
   *         the array or the specified default value if the associated key is
   *         missing or its value is null.
   */
  public static List<String> getStrings(JsonObject    obj,
                                        String        key,
                                        List<String>  defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key) || obj.isNull(key)) return defaultValue;
    return Collections.unmodifiableList(
        obj.getJsonArray(key).getValuesAs(JsonUtilities::valueAsString));
  }

  /**
   * Obtains a {@link List} of {@link String} instances from a {@link
   * JsonArray} of {@link JsonString} instances bound to the specified {@link
   * JsonArray} at the specified index.  If there is a null JSON value at the
   * specified index then <code>null</code> is returned, otherwise a {@link
   * List} containing the {@link String} instances from the array is returned.
   * <b>NOTE:</b> This function will convert any non-string value that is found
   * to a {@link String} representation.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return A {@link List} of {@link String} values for the values found in
   *         the array at the specified position or <code>null</code> if the
   *         JSON value at the specified position is null.
   */
  public static List<String> getStrings(JsonArray arr, int index)
  {
    return getStrings(arr, index, null);
  }

  /**
   * Obtains a {@link List} of {@link String} instances from a {@link JsonArray}
   * of {@link JsonString} instances bound to the specified {@link JsonObject}
   * by the specified key.  If the specified key is missing or has a null value
   * then <code>null</code> is returned, otherwise a {@link List} containing the
   * {@link String} instances from the array is returned.  <b>NOTE:</b> This
   * function will convert any non-string value that is found to a
   * {@link String} representation.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return A {@link List} of {@link String} values for the values found in
   *         the array at the specified position or the specified default value
   *         if the JSON value at the specified position is null.
   */
  public static List<String> getStrings(JsonArray     arr,
                                        int           index,
                                        List<String>  defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) return defaultValue;
    return Collections.unmodifiableList(
        arr.getJsonArray(index).getValuesAs(JsonUtilities::valueAsString));
  }

  /**
   * Gets an integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * <code>null</code> is returned, otherwise the {@link Integer} value is
   * returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Integer} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static Integer getInteger(JsonObject obj, String key)
  {
    return getInteger(obj, key, null);
  }

  /**
   * Gets an integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link Integer}
   * value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link Integer} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static Integer getInteger(JsonObject obj,
                                   String     key,
                                   Integer    defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;

    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).intValue();
    }
  }

  /**
   * Gets an integer value from the specified {@link JsonArray} at the array
   * position for the specified index.  If the array has a JSON null value at
   * the specified position then <code>null</code> is returned, otherwise the
   * {@link Integer} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link Integer} value at the specified array position, or
   *         <code>null</code> if the JSON value at the array position is null.
   */
  public static Integer getInteger(JsonArray arr, int index)
  {
    return getInteger(arr, index, null);
  }

  /**
   * Gets an integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link Integer}
   * value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link Integer} value at the specified array position, or the
   *         specified default value if the JSON value at the array position
   *         is null.
   */
  public static Integer getInteger(JsonArray  arr,
                                   int        index,
                                   Integer    defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).intValue();
    }
  }

  /**
   * Gets a long integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * <code>null</code> is returned, otherwise the {@link Long} value is
   * returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Long} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static Long getLong(JsonObject obj, String key)
  {
    return getLong(obj, key, null);
  }

  /**
   * Gets a long integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link Long} value
   * is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link Long} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static Long getLong(JsonObject obj,
                             String     key,
                             Long       defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).longValue();
    }
  }

  /**
   * Gets a long integer value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link Long} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link Long} value at the array position for the specified
   *         index, or <code>null</code> if the JSON value at the array
   *         position is null.
   */
  public static Long getLong(JsonArray arr, int index)
  {
    return getLong(arr, index, null);
  }

  /**
   * Gets a long integer value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link Long} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link Long} value at the array position for the specified
   *         index, or the specified default value if the JSON value at the
   *         array position is null.
   */
  public static Long getLong(JsonArray  arr,
                             int        index,
                             Long       defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).longValue();
    }
  }

  /**
   * Gets a {@link BigInteger} value from the specified {@link JsonObject}
   * using the specified key.  If the specified key is missing or has a null
   * value then <code>null</code> is returned, otherwise the {@link BigInteger}
   * value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link BigInteger} value for the key, or <code>null</code> if
   *         the JSON value is <code>null</code> or missing.
   */
  public static BigInteger getBigInteger(JsonObject obj, String key)
  {
    return getBigInteger(obj, key, null);
  }

  /**
   * Gets a {@link BigInteger} value from the specified {@link JsonObject}
   * using the specified key.  If the specified key is missing or has a null
   * value then the specified default value is returned, otherwise the
   * {@link BigInteger} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link BigInteger} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static BigInteger getBigInteger(JsonObject obj,
                                         String     key,
                                         BigInteger defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).bigIntegerValue();
    }
  }

  /**
   * Gets a {@link BigInteger} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link BigInteger} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link BigInteger} value at the array position for the
   *         specified index, or <code>null</code> if the JSON value at the
   *         array position is null.
   */
  public static BigInteger getBigInteger(JsonArray arr, int index)
  {
    return getBigInteger(arr, index, null);
  }

  /**
   * Gets a {@link BigInteger} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link BigInteger} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link BigInteger} value at the array position for the
   *         specified index, or the specified default value if the JSON value
   *         at the array position is null.
   */
  public static BigInteger getBigInteger(JsonArray  arr,
                                         int        index,
                                         BigInteger defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).bigIntegerValue();
    }
  }

  /**
   * Gets a single-precision floating-point value from the specified {@link
   * JsonObject} using the specified key.  If the specified key is missing or
   * has a null value then <code>null</code> is returned, otherwise the
   * {@link Float} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Long} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static Float getFloat(JsonObject obj, String key)
  {
    return getFloat(obj, key, null);
  }

  /**
   * Gets a single-precision floating-point value from the specified {@link
   * JsonObject} using the specified key.  If the specified key is missing or
   * has a null value then the specified default value is returned, otherwise
   * the {@link Float} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link Double} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static Float getFloat(JsonObject obj,
                               String     key,
                               Float      defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).numberValue().floatValue();
    }
  }

  /**
   * Gets a {@link Float} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link Float} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link Float} value at the array position for the
   *         specified index, or <code>null</code> if the JSON value at the
   *         array position is null.
   */
  public static Float getFloat(JsonArray arr, int index)
  {
    return getFloat(arr, index, null);
  }

  /**
   * Gets a {@link Float} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link Float} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link Float} value at the array position for the specified
   *         index, or the specified default value if the JSON value at the
   *         array position is null.
   */
  public static Float getFloat(JsonArray arr, int index, Float defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).numberValue().floatValue();
    }
  }

  /**
   * Gets a double-precision floating-point value from the specified {@link
   * JsonObject} using the specified key.  If the specified key is missing or
   * has a null value then <code>null</code> is returned, otherwise the
   * {@link Double} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Long} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static Double getDouble(JsonObject obj, String key)
  {
    return getDouble(obj, key, null);
  }

  /**
   * Gets a double-precision floating-point value from the specified {@link
   * JsonObject} using the specified key.  If the specified key is missing or
   * has a null value then the specified default value is returned, otherwise
   * the {@link Double} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link Double} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static Double getDouble(JsonObject obj,
                                 String     key,
                                 Double     defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).doubleValue();
    }
  }

  /**
   * Gets a {@link Double} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link Double} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link Double} value at the array position for the
   *         specified index, or <code>null</code> if the JSON value at the
   *         array position is null.
   */
  public static Double getDouble(JsonArray arr, int index)
  {
    return getDouble(arr, index, null);
  }

  /**
   * Gets a {@link Double} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link Double} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link Double} value at the array position for the specified
   *         index, or the specified default value if the JSON value at the
   *         array position is null.
   */
  public static Double getDouble(JsonArray arr, int index, Double defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).doubleValue();
    }
  }

  /**
   * Gets a {@link BigDecimal} value from the specified {@link JsonObject}
   * using the specified key.  If the specified key is missing or has a null
   * value then <code>null</code> is returned, otherwise the {@link BigDecimal}
   * value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Long} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static BigDecimal getBigDecimal(JsonObject obj, String key)
  {
    return getBigDecimal(obj, key, null);
  }

  /**
   * Gets a {@link BigDecimal} value from the specified {@link JsonObject} using
   * the specified key.  If the specified key is missing or has a null value
   * then the specified default value is returned, otherwise the
   * {@link BigDecimal} value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link BigDecimal} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static BigDecimal getBigDecimal(JsonObject obj,
                                         String     key,
                                         BigDecimal defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getJsonNumber(key).bigDecimalValue();
    }
  }

  /**
   * Gets a {@link BigDecimal} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link BigDecimal} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link BigDecimal} value at the array position for the
   *         specified index, or <code>null</code> if the JSON value at the
   *         array position is null.
   */
  public static BigDecimal getBigDecimal(JsonArray arr, int index)
  {
    return getBigDecimal(arr, index, null);
  }

  /**
   * Gets a {@link BigDecimal} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link BigDecimal} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link BigDecimal} value at the array position for the
   *         specified index, or the specified default value if the JSON value
   *         at the array position is null.
   */
  public static BigDecimal getBigDecimal(JsonArray  arr,
                                         int        index,
                                         BigDecimal defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getJsonNumber(index).bigDecimalValue();
    }
  }

  /**
   * Gets a boolean value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * <code>null</code> is returned, otherwise the {@link Boolean} value is
   * returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @return The {@link Boolean} value for the key, or <code>null</code> if the
   *         JSON value is <code>null</code> or missing.
   */
  public static Boolean getBoolean(JsonObject obj, String key)
  {
    return getBoolean(obj, key, null);
  }

  /**
   * Gets an integer value from the specified {@link JsonObject} using the
   * specified key.  If the specified key is missing or has a null value then
   * the specified default value is returned, otherwise the {@link Boolean}
   * value is returned.
   *
   * @param obj The {@link JsonObject} to get the value from.
   *
   * @param key The non-null {@link String} key for the value.
   *
   * @param defaultValue The value to return if the key is missing or its
   *                     value is <code>null</code>.
   *
   * @return The {@link Boolean} value for the key, or the specified default
   *         value if the JSON value is <code>null</code> or missing.
   */
  public static Boolean getBoolean(JsonObject obj,
                                   String     key,
                                   Boolean    defaultValue)
  {
    if (obj == null) return defaultValue;
    if (!obj.containsKey(key)) return defaultValue;
    if (obj.isNull(key)) {
      return defaultValue;
    } else {
      return obj.getBoolean(key);
    }
  }

  /**
   * Gets a {@link Boolean} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then <code>null</code> is
   * returned, otherwise the {@link Boolean} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link Boolean} value at the array position for the
   *         specified index, or <code>null</code> if the JSON value at the
   *         array position is null.
   */
  public static Boolean getBoolean(JsonArray arr, int index)
  {
    return getBoolean(arr, index, null);
  }

  /**
   * Gets a {@link Boolean} value from the specified {@link JsonArray} at the
   * array position for the specified index.  If there is a JSON null value at
   * the array position for the specified index then the specified default
   * value is returned, otherwise the {@link Boolean} value is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @param defaultValue The value to return if the JSON value at the specified
   *                     array position is <code>null</code>.
   *
   * @return The {@link Boolean} value at the array position for the
   *         specified index, or the specified default value if the JSON value
   *         at the array position is null.
   */
  public static Boolean getBoolean(JsonArray  arr,
                                   int        index,
                                   Boolean    defaultValue)
  {
    if (arr == null) return defaultValue;
    if (arr.isNull(index)) {
      return defaultValue;
    } else {
      return arr.getBoolean(index);
    }
  }

  /**
   * Gets the {@link JsonValue} for the specified key in the specified object.
   * If the specified {@link JsonObject} is <code>null</code> <b>or</b> if the
   * key is not found in the object then <code>null</code> is returned.
   *
   * @param obj The {@link JsonObject} from which to obtain the value.
   *
   * @param key The {@link String} key for the property to retrieve.
   *
   * @return The {@link JsonValue} for the key or <code>null</code> if the key is
   *         not found.
   */
  public static JsonValue getJsonValue(JsonObject obj, String key) {
    if (obj == null) return null;
    if (!obj.containsKey(key)) return null;
    return obj.getValue("/" + key);
  }

  /**
   * Gets the {@link JsonObject} for the specified key in the specified object.
   * If the specified {@link JsonObject} is <code>null</code> <b>or</b> if the
   * key is not found in the object then <code>null</code> is returned.
   *
   * @param obj The {@link JsonObject} from which to obtain the value.
   *
   * @param key The {@link String} key for the property to retrieve.
   *
   * @return The {@link JsonObject} for the key or <code>null</code> if the key is
   *         not found.
   */
  public static JsonObject getJsonObject(JsonObject obj, String key) {
    if (obj == null) return null;
    if (!obj.containsKey(key)) return null;
    if (obj.isNull(key)) {
      return null;
    } else {
      return obj.getJsonObject(key);
    }
  }

  /**
   * Gets the {@link JsonObject} at the array position for the specified index
   * from the specified {@link JsonArray}.  If the specified {@link JsonArray}
   * is <code>null</code> <b>or</b> if a null JSON value is found at
   * the array position then <code>null</code> is returned.
   *
   * @param arr The {@link JsonArray} to get the value from.
   *
   * @param index The index of the array position to read.
   *
   * @return The {@link JsonObject} at the array position for the specified
   *         index or <code>null</code> if the JSON value at the array
   *         position is <code>null</code>.
   */
  public static JsonObject getJsonObject(JsonArray arr, int index) {
    if (arr == null) return null;
    if (arr.isNull(index)) {
      return null;
    } else {
      return arr.getJsonObject(index);
    }
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,JsonObjectBuilder)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link JsonObjectBuilder} value, or <code>null</code>.
   *
   * @return The first {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, 
                                      String            key, 
                                      JsonObjectBuilder val) 
  {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,JsonArrayBuilder)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link JsonArrayBuilder} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, 
                                      String            key, 
                                      JsonArrayBuilder  val) 
  {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,JsonValue)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link JsonValue} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, 
                                      String            key, 
                                      JsonValue         val) 
  {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,String)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link String} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, String val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,int)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link Integer} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, Integer val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,long)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link Long} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, Long val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,double)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link Double} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, Double val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,double)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link Float} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, Float val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val.doubleValue());
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,BigInteger)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link BigInteger} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, BigInteger val)
  {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,BigDecimal)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link BigDecimal} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, BigDecimal val)
  {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonObjectBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonObjectBuilder#addNull(String)} is used, otherwise
   * {@link JsonObjectBuilder#add(String,boolean)} is used.
   *
   * @param job The {@link JsonObjectBuilder} to add the key/value pair to.
   *
   * @param key The {@link String} key.
   *
   * @param val The {@link Boolean} value, or <code>null</code>.
   *
   * @return The {@link JsonObjectBuilder} that was specified.
   */
  public static JsonObjectBuilder add(JsonObjectBuilder job, String key, Boolean val) {
    if (val == null) {
      job.addNull(key);
    } else {
      job.add(key, val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(JsonObjectBuilder)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link JsonObjectBuilder} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder   job, 
                                     JsonObjectBuilder  val) 
  {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(JsonArrayBuilder)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link JsonArrayBuilder} value, or <code>null</code>.
   *
   * @return The first {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, 
                                     JsonArrayBuilder val) 
  {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(JsonValue)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link JsonValue} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder   job, 
                                     JsonValue          val) 
  {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(String)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link String} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, String val) {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(int)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link Integer} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, Integer val) {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(long)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link Long} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, Long val) {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(double)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link Double} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, Double val) {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(double)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link Float} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, Float val) {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val.doubleValue());
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(BigInteger)} is used.
   *
   * @param job The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link BigInteger} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder job, BigInteger val)
  {
    if (val == null) {
      job.addNull();
    } else {
      job.add(val);
    }
    return job;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(BigDecimal)} is used.
   *
   * @param jab The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link BigDecimal} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder jab, BigDecimal val)
  {
    if (val == null) {
      jab.addNull();
    } else {
      jab.add(val);
    }
    return jab;
  }

  /**
   * Adds the specified key/value pair to the specified {@link
   * JsonArrayBuilder}.  If the specified value is <code>null</code> then
   * {@link JsonArrayBuilder#addNull()} is used, otherwise
   * {@link JsonArrayBuilder#add(boolean)} is used.
   *
   * @param jab The {@link JsonArrayBuilder} to add the key/value pair to.
   *
   * @param val The {@link Boolean} value, or <code>null</code>.
   *
   * @return The {@link JsonArrayBuilder} that was specified.
   */
  public static JsonArrayBuilder add(JsonArrayBuilder jab, Boolean val) {
    if (val == null) {
      jab.addNull();
    } else {
      jab.add(val);
    }
    return jab;
  }

  /**
   * Parses JSON text as a {@link JsonValue}.  If the specified text is not
   * formatted as a JSON then an exception will be thrown.
   *
   * @param jsonText The JSON text to be parsed.
   *
   * @return The parsed {@link JsonValue}.
   */
  public static JsonValue parseValue(String jsonText) {
    if (jsonText == null) return null;
    StringReader sr = new StringReader(jsonText);
    JsonReader jsonReader = Json.createReader(sr);
    return jsonReader.readValue();
  }

  /**
   * Parses JSON text as a {@link JsonObject}.  If the specified text is not
   * formatted as a JSON object then an exception will be thrown.
   *
   * @param jsonText The JSON text to be parsed.
   *
   * @return The parsed {@link JsonObject}.
   */
  public static JsonObject parseJsonObject(String jsonText) {
    if (jsonText == null) return null;
    StringReader sr = new StringReader(jsonText);
    JsonReader jsonReader = Json.createReader(sr);
    return jsonReader.readObject();
  }

  /**
   * Parses JSON text as a {@link JsonArray}.  If the specified text is not
   * formatted as a JSON array then an exception will be thrown.
   *
   * @param jsonText The JSON text to be parsed.
   *
   * @return The parsed {@link JsonArray}.
   */
  public static JsonArray parseJsonArray(String jsonText) {
    if (jsonText == null) return null;
    StringReader sr = new StringReader(jsonText);
    JsonReader jsonReader = Json.createReader(sr);
    return jsonReader.readArray();
  }

  /**
   * Same as {@link #normalizeJsonValue(JsonValue)}, but first parses the
   * specified text as a {@link JsonValue}.
   *
   * @param jsonText The JSON-formatted text to parse.
   *
   * @return The normalized object representation.
   *
   * @see #normalizeJsonValue(JsonValue)
   */
  public static Object normalizeJsonText(String jsonText) {
    if (jsonText == null) return null;
    jsonText = jsonText.trim();
    JsonValue jsonValue = null;
    if ((jsonText.indexOf("{") == 0) || (jsonText.indexOf("[") == 0)) {
      StringReader  sr          = new StringReader(jsonText);
      JsonReader    jsonReader  = Json.createReader(sr);
      jsonValue = jsonReader.read();
    } else if (jsonText.equals("true")) {
      jsonValue = JsonValue.TRUE;
    } else if (jsonText.equals("false")) {
      jsonValue = JsonValue.FALSE;
    } else if (jsonText.equals("null")) {
      jsonValue = JsonValue.NULL;
    } else {
      String harnessText = "{\"value\": " + jsonText + "}";
      StringReader sr         = new StringReader(harnessText);
      JsonReader   jsonReader = Json.createReader(sr);
      JsonObject   jsonObject = jsonReader.readObject();
      jsonValue = jsonObject.getValue("/value");
    }
    return normalizeJsonValue(jsonValue);
  }

  /**
   * Converts the specified {@link JsonValue} to a basic hierarchical object
   * representation.  The returned value depends on the {@link
   * JsonValue.ValueType}.
   * <ul>
   *   <li>{@link JsonValue.ValueType#NULL} yields <code>null</code></li>
   *   <li>{@link JsonValue.ValueType#TRUE} yields {@link Boolean#TRUE}</li>
   *   <li>{@link JsonValue.ValueType#FALSE} yields {@link Boolean#FALSE}</li>
   *   <li>{@link JsonValue.ValueType#STRING} yields a {@link String}</li>
   *   <li>{@link JsonValue.ValueType#NUMBER} yields a {@link Long}
   *        or {@link Double}</li>
   *   <li>{@link JsonValue.ValueType#ARRAY} yields a {@link List}</li>
   *   <li>{@link JsonValue.ValueType#OBJECT} yields a {@link Map}</li>
   * </ul>
   *
   * @param jsonValue The {@link JsonValue} to normalize.
   *
   * @return The normalized version of the {@link JsonValue}.
   */
  @SuppressWarnings("unchecked")
  public static Object normalizeJsonValue(JsonValue jsonValue)
  {
    if (jsonValue == null) return null;
    switch (jsonValue.getValueType()) {
      case NULL: {
        return null;
      }
      case TRUE: {
        return Boolean.TRUE;
      }
      case FALSE: {
        return Boolean.FALSE;
      }
      case NUMBER: {
        JsonNumber jsonNumber = (JsonNumber) jsonValue;
        if (jsonNumber.isIntegral()) {
          return (Long) jsonNumber.longValue();
        } else {
          return (Double) jsonNumber.doubleValue();
        }
      }
      case STRING: {
        JsonString jsonString = (JsonString) jsonValue;
        return jsonString.getString();
      }
      case ARRAY: {
        JsonArray jsonArray = (JsonArray) jsonValue;
        int count = jsonArray.size();
        ArrayList result = new ArrayList(count);
        for (JsonValue jv : jsonArray) {
          result.add(normalizeJsonValue(jv));
        }
        return result;
      }
      case OBJECT: {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        JsonObject jsonObject = (JsonObject) jsonValue;

        for (Map.Entry entry : jsonObject.entrySet()) {
          String    key   = (String) entry.getKey();
          Object    value = normalizeJsonValue((JsonValue) entry.getValue());
          result.put(key, value);
        }
        return result;
      }
      default:
        throw new IllegalStateException(
            "Unrecognized JsonValue.ValueType: " + jsonValue.getValueType());
    }
  }

  /**
   * Adds a property to the specified {@link JsonObjectBuilder} with the
   * specified property name and value.  The specified value can be null, a
   * {@link String}, {@link Boolean}, {@link Integer}, {@link Long},
   * {@link Short}, {@link Float}, {@link Double}, {@link BigInteger} or
   * {@link BigDecimal}.  It can also be a {@link List} or a {@link Map} with
   * non-null {@link String} keys.  If a {@link List} then the property value
   * is set as a {@link JsonArray} using {@link #addElement(JsonArrayBuilder,
   * Object)}.  If a {@link Map} with all non-null {@link String} keys then
   * this function is recursively called to make a {@link JsonObject} property
   * value with the keys and values of the {@link Map} as the properties of the
   * {@link JsonObject}.  Anything else is converted to a {@link String} via
   * its {@link #toString()} method.
   *
   * @param builder The {@link JsonObjectBuilder} to add the property to.
   *
   * @param property The property name.
   *
   * @param value The value for the property.
   *
   * @return The specified {@link JsonObjectBuilder}.
   */
  @SuppressWarnings("unchecked")
  public static JsonObjectBuilder addProperty(JsonObjectBuilder builder,
                                              String            property,
                                              Object            value)
  {
    if (value == null) {
      builder.addNull(property);
    } else if (value instanceof String) {
      builder.add(property, (String) value);
    } else if (value instanceof Boolean) {
      builder.add(property, (Boolean) value);
    } else if (value instanceof Integer) {
      builder.add(property, (Integer) value);
    } else if (value instanceof Long) {
      builder.add(property, (Long) value);
    } else if (value instanceof Short) {
      builder.add(property, (Short) value);
    } else if (value instanceof Float) {
      builder.add(property, (Float) value);
    } else if (value instanceof Double) {
      builder.add(property, (Double) value);
    } else if (value instanceof BigInteger) {
      builder.add(property, (BigInteger) value);
    } else if (value instanceof BigDecimal) {
      builder.add(property, (BigDecimal) value);

    } else if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (int index = 0; index < length; index++) {
        addElement(jab,Array.get(value, index));
      }
      builder.add(property, jab);

    } else if (value instanceof Collection) {
      Collection collection = (Collection) value;
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (Object elem: collection) {
        addElement(jab, elem);
      }
      builder.add(property, jab);

    } else if (value instanceof Map) {
      Map map = (Map) value;

      // check if all the map keys are strings
      boolean stringKeys = true;
      for (Object key : map.keySet()) {
        if (key == null || (!(key instanceof String))) {
          stringKeys = false;
          break;
        }
      }

      // choose how to add based on the keys
      if (stringKeys) {
        // if all string keys then add an object property with sub-object value
        JsonObjectBuilder job = Json.createObjectBuilder();
        map.forEach((subKey, subValue) -> {
          addProperty(job, ((String) subKey), subValue);
        });
        builder.add(property, job);

      } else {
        // if NOT all strings then add the value as a string
        builder.add(property, value.toString());
      }

    } else {
      builder.add(property, value.toString());
    }

    return builder;
  }

  /**
   * Adds a property to the specified {@link JsonArrayBuilder} with the
   * specified value.  The specified value can be null, a {@link String},
   * {@link Boolean}, {@link Integer}, {@link Long}, {@link Short},
   * {@link Float}, {@link Double}, {@link BigInteger} or {@link BigDecimal}.
   * It can also be a {@link List} or a {@link Map} with non-null {@link String}
   * keys.  If a {@link List} then the property value is set as a {@link
   * JsonArray} by recursively calling this function using the liste elements
   * as the array values.  If a {@link Map} with all non-null {@link String}
   * keys then {@link #addProperty(JsonObjectBuilder,String, Object)} is called
   * to make a {@link JsonObject} property value with the keys and values of
   * the {@link Map} as the properties of the {@link JsonObject}.  Anything
   * else is converted to a {@link String} via its {@link #toString()} method.
   * Anything else is converted to a {@link String} via its {@link #toString()}
   * method.
   *
   * @param builder The {@link JsonObjectBuilder} to add the property to.
   *
   * @param value The value for the property.
   *
   * @return The specified {@link JsonObjectBuilder}.
   */
  @SuppressWarnings("unchecked")
  public static JsonArrayBuilder addElement(JsonArrayBuilder builder,
                                            Object           value)
  {
    if (value == null) {
      builder.addNull();
    } else if (value instanceof String) {
      builder.add((String) value);
    } else if (value instanceof Boolean) {
      builder.add((Boolean) value);
    } else if (value instanceof Integer) {
      builder.add((Integer) value);
    } else if (value instanceof Long) {
      builder.add((Long) value);
    } else if (value instanceof Short) {
      builder.add((Short) value);
    } else if (value instanceof Float) {
      builder.add((Float) value);
    } else if (value instanceof Double) {
      builder.add((Double) value);
    } else if (value instanceof BigInteger) {
      builder.add((BigInteger) value);
    } else if (value instanceof BigDecimal) {
      builder.add((BigDecimal) value);

    } else if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (int index = 0; index < length; index++) {
        addElement(jab,Array.get(value, index));
      }
      builder.add(jab);

    } else if (value instanceof Collection) {
      Collection collection = (Collection) value;
      JsonArrayBuilder jab = Json.createArrayBuilder();
      for (Object elem: collection) {
        addElement(jab, elem);
      }
      builder.add(jab);

    } else if (value instanceof Map) {
      Map map = (Map) value;

      // check if all the map keys are strings
      boolean stringKeys = true;
      for (Object key : map.keySet()) {
        if (key == null || (!(key instanceof String))) {
          stringKeys = false;
          break;
        }
      }

      // choose how to add based on the keys
      if (stringKeys) {
        // if all string keys then add an object property with sub-object value
        JsonObjectBuilder job = Json.createObjectBuilder();
        map.forEach((subKey, subValue) -> {
          addProperty(job, ((String) subKey), subValue);
        });
        builder.add(job);

      } else {
        // if NOT all strings then add the value as a string
        builder.add(value.toString());
      }
    } else {
      builder.add(value.toString());
    }
    return builder;
  }

  /**
   * Creates a {@link JsonObject} with the specified property and value.
   * The value is interpreted according to {@link
   * #addProperty(JsonObjectBuilder, String, Object)}.
   *
   * @param property The property name.
   *
   * @param value The property value.
   *
   * @return The created {@link JsonObject}.
   */
  public static JsonObject toJsonObject(String property, Object value)
  {
    return toJsonObject(property, value, null, null);
  }

  /**
   * Creates a {@link JsonObject} with the specified properties and values.
   * The values are interpreted according to {@link
   * #addProperty(JsonObjectBuilder, String, Object)}.
   *
   * @param property1 The first property name.
   *
   * @param value1 The first property value.
   *
   * @param property2 The second property name.
   *
   * @param value2 The second property value.
   *
   * @return The created {@link JsonObject}.
   *
   */
  public static JsonObject toJsonObject(String property1,
                                        Object value1,
                                        String property2,
                                        Object value2)
  {
    return toJsonObject(property1,
                        value1,
                        property2,
                        value2,
                        null,
                        null);
  }

  /**
   * Creates a {@link JsonObject} with the specified properties and values.
   * The values are interpreted according to {@link
   * #addProperty(JsonObjectBuilder, String, Object)}.
   *
   * @param property1 The first property name.
   *
   * @param value1 The first property value.
   *
   * @param property2 The second property name.
   *
   * @param value2 The second property value.
   *
   * @param property3 The third property name.
   *
   * @param value3 The third property value.
   *
   * @return The created {@link JsonObject}.
   */
  public static JsonObject toJsonObject(String property1,
                                        Object value1,
                                        String property2,
                                        Object value2,
                                        String property3,
                                        Object value3)
  {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    if (property1 != null) {
      addProperty(builder, property1, value1);
    }
    if (property2 != null) {
      addProperty(builder, property2, value2);
    }
    if (property3 != null) {
      addProperty(builder, property3, value3);
    }
    return builder.build();
  }

  /**
   * Creates a {@link JsonArray} with the specified values. The values are
   * interpreted according to {@link #addElement(JsonArrayBuilder, Object)}.
   *
   * @param values The zero or more values.
   *
   * @return The created {@link JsonArray}.
   */
  public static JsonArray toJsonArray(Object... values)
  {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (Object value: values) {
      addElement(builder, value);
    }
    return builder.build();
  }

  /**
   * Attempts to convert the specified {@link Map} of {@link String} keys to
   * {@link Object} values to a {@link JsonObject}.  This will leverage the
   * {@link #addProperty(JsonObjectBuilder,String,Object)} and {@link
   * #addElement(JsonArrayBuilder, Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param map The {@link Map} of {@link String} keys to {@link Object} values.
   *
   * @return The {@link JsonObject} created for the specified {@link Map}, or
   *         <code>null</code> if the specified {@link Map} is
   *         <code>null</code>.
   */
  public static JsonObject toJsonObject(Map<String, ?> map) {
    if (map == null) return null;
    return toJsonObjectBuilder(map).build();
  }

  /**
   * Attempts to convert the specified {@link Map} of {@link String} keys to
   * {@link Object} values to a {@link JsonObjectBuilder}.  This will leverage
   * the {@link #addProperty(JsonObjectBuilder,String,Object)} and {@link
   * #addElement(JsonArrayBuilder, Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param map The {@link Map} of {@link String} keys to {@link Object} values.
   *
   * @return The {@link JsonObjectBuilder} created for the specified {@link
   *         Map}, or <code>null</code> if the specified {@link Map} is
   *         <code>null</code>.
   */
  public static JsonObjectBuilder toJsonObjectBuilder(Map<String, ?> map) {
    if (map == null) return null;
    JsonObjectBuilder builder = Json.createObjectBuilder();
    map.forEach((key, value) -> {
      addProperty(builder, key, value);
    });
    return builder;
  }

  /**
   * Attempts to convert the specified {@link List} of {@link Object} values to
   * a {@link JsonArray}.  This will leverage the {@link
   * #addElement(JsonArrayBuilder, Object)} and {@link
   * #addProperty(JsonObjectBuilder,String,Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param list The {@link List} of {@link Object} values.
   *
   * @return The {@link JsonArray} created for the specified {@link List}, or
   *         <code>null</code> if the specified {@link List} is
   *         <code>null</code>.
   */
  public static JsonArray toJsonArray(List<?> list) {
    return toJsonArray((Collection<?>) list);
  }

  /**
   * Attempts to convert the specified {@link Collection} of {@link Object}
   * values to a {@link JsonArray}.  This will leverage the {@link
   * #addElement(JsonArrayBuilder, Object)} and {@link
   * #addProperty(JsonObjectBuilder,String,Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param collection The {@link Collection} of {@link Object} values.
   *
   * @return The {@link JsonArray} created for the specified {@link Collection},
   *         or <code>null</code> if the specified {@link Collection} is
   *         <code>null</code>.
   */
  public static JsonArray toJsonArray(Collection<?> collection) {
    if (collection == null) return null;
    return toJsonArrayBuilder(collection).build();
  }

  /**
   * Attempts to convert the specified {@link List} of {@link Object} values to
   * a {@link JsonArrayBuilder}.  This will leverage the {@link
   * #addElement(JsonArrayBuilder, Object)} and {@link
   * #addProperty(JsonObjectBuilder,String,Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param list The {@link List} of {@link Object} values.
   *
   * @return The {@link JsonArrayBuilder} created for the specified
   *         {@link List}, or <code>null</code> if the specified {@link List}
   *         is <code>null</code>.
   */
  public static JsonArrayBuilder toJsonArrayBuilder(List<?> list) {
    return toJsonArrayBuilder((Collection<?>) list);
  }

  /**
   * Attempts to convert the specified {@link Collection} of {@link Object}
   * values to a {@link JsonArrayBuilder}.  This will leverage the {@link
   * #addElement(JsonArrayBuilder, Object)} and {@link
   * #addProperty(JsonObjectBuilder,String,Object)} functions and has the same
   * limitations that those have with regard to the types of values that can
   * be handled and how they are handled.  For example, if a value is
   * encountered that is a {@link Map} whose keys are <b>not</b> non-null
   * {@link String} values then that object will simply be converted to a
   * {@link JsonString} value via its {@link Object#toString()} implementation.
   *
   * @param collection The {@link Collection} of {@link Object} values.
   *
   * @return The {@link JsonArrayBuilder} created for the specified
   *         {@link Collection}, or <code>null</code> if the specified {@link
   *         Collection} is <code>null</code>.
   */
  public static JsonArrayBuilder toJsonArrayBuilder(Collection<?> collection) {
    if (collection == null) return null;
    JsonArrayBuilder builder = Json.createArrayBuilder();
    collection.forEach((value) -> {
      addElement(builder, value);
    });
    return builder;
  }

  /**
   * Converts the specified {@link JsonValue} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param jsonValue The {@link JsonValue} describing the JSON.
   *
   * @return The specified {@link Writer}.
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T writer, JsonValue jsonValue)
  {
    return JsonUtilities.toJsonText(writer, jsonValue, false);
  }

  /**
   * Converts the specified {@link JsonValue} to a JSON string.
   *
   * @param jsonValue The {@link JsonValue} describing the JSON.
   *
   * @return The specified {@link JsonValue} converted to a JSON string.
   */
  public static String toJsonText(JsonValue jsonValue) {
    return JsonUtilities.toJsonText(jsonValue, false);
  }

  /**
   * Converts the specified {@link JsonObjectBuilder} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param builder The {@link JsonObjectBuilder} describing the object.
   *
   * @return The specified {@link Writer}.
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T                 writer,
                                                JsonObjectBuilder builder)
  {
    return JsonUtilities.toJsonText(writer, builder, false);
  }


  /**
   * Converts the specified {@link JsonObjectBuilder} to a JSON string.
   *
   * @param builder The {@link JsonObjectBuilder} describing the object.
   *
   * @return The specified {@link JsonObjectBuilder} converted to a JSON string.
   */
  public static String toJsonText(JsonObjectBuilder builder) {
    return JsonUtilities.toJsonText(
        new StringWriter(), builder, false).toString();
  }

  /**
   * Converts the specified {@link JsonArrayBuilder} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param builder The {@link JsonArrayBuilder} describing the object.
   *
   * @return The specified {@link Writer}
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T                 writer,
                                                JsonArrayBuilder  builder)
  {
    return JsonUtilities.toJsonText(writer, builder, false);
  }

  /**
   * Converts the specified {@link JsonArrayBuilder} to a JSON string.
   *
   * @param builder The {@link JsonArrayBuilder} describing the object.
   *
   * @return The specified {@link JsonArrayBuilder} converted to a JSON string.
   */
  public static String toJsonText(JsonArrayBuilder builder) {
    return JsonUtilities.toJsonText(
        new StringWriter(), builder, false).toString();
  }

  /**
   * Converts the specified {@link JsonValue} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param jsonValue The {@link JsonValue} describing the JSON.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link Writer}.
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T         writer,
                                                JsonValue jsonValue,
                                                boolean   prettyPrint)
  {
    Objects.requireNonNull(writer, "Writer cannot be null");

    JsonWriter jsonWriter = (prettyPrint)
        ? PRETTY_WRITER_FACTORY.createWriter(writer)
        : Json.createWriter(writer);

    if (jsonValue != null) {
      jsonWriter.write(jsonValue);
    } else {
      jsonWriter.write(JsonValue.NULL);
    }

    return writer;
  }

  /**
   * Converts the specified {@link JsonValue} to a JSON string.
   *
   * @param jsonValue The {@link JsonValue} describing the JSON.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link JsonValue} converted to a JSON string.
   */
  public static String toJsonText(JsonValue jsonValue, boolean prettyPrint) {
    return JsonUtilities.toJsonText(
        new StringWriter(), jsonValue, prettyPrint).toString();
  }

  /**
   * Converts the specified {@link JsonObjectBuilder} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param builder The {@link JsonObjectBuilder} describing the object.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link Writer}.
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T                 writer,
                                                JsonObjectBuilder builder,
                                                boolean           prettyPrint)
  {
    Objects.requireNonNull(writer, "Writer cannot be null");

    JsonWriter jsonWriter = (prettyPrint)
        ? PRETTY_WRITER_FACTORY.createWriter(writer)
        : Json.createWriter(writer);

    JsonObject jsonObject = builder.build();

    jsonWriter.writeObject(jsonObject);

    return writer;
  }

  /**
   * Converts the specified {@link JsonObjectBuilder} to a JSON string.
   *
   * @param builder The {@link JsonObjectBuilder} describing the object.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link JsonObjectBuilder} converted to a JSON string.
   */
  public static String toJsonText(JsonObjectBuilder builder,
                                  boolean           prettyPrint)
  {
    return JsonUtilities.toJsonText(
        new StringWriter(), builder, prettyPrint).toString();
  }

  /**
   * Converts the specified {@link JsonArrayBuilder} to a JSON string.
   *
   * @param writer The {@link Writer} to write to.
   *
   * @param builder The {@link JsonArrayBuilder} describing the object.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link Writer}
   *
   * @param <T> The type of the writer to which the write the {@link JsonValue}.
   */
  public static <T extends Writer> T toJsonText(T                 writer,
                                                JsonArrayBuilder  builder,
                                                boolean           prettyPrint)
  {
    Objects.requireNonNull(writer, "Writer cannot be null");

    JsonWriter jsonWriter = (prettyPrint)
        ? PRETTY_WRITER_FACTORY.createWriter(writer)
        : Json.createWriter(writer);

    JsonArray jsonArray = builder.build();

    jsonWriter.writeArray(jsonArray);

    return writer;
  }

  /**
   * Converts the specified {@link JsonArrayBuilder} to a JSON string.
   *
   * @param builder The {@link JsonArrayBuilder} describing the object.
   *
   * @param prettyPrint Whether or not to pretty-print the JSON text.
   *
   * @return The specified {@link JsonArrayBuilder} converted to a JSON string.
   */
  public static String toJsonText(JsonArrayBuilder builder,
                                  boolean          prettyPrint)
  {
    return JsonUtilities.toJsonText(
        new StringWriter(), builder, prettyPrint).toString();
  }

  /**
   * Converts an INI file to JSON format.
   *
   * @param iniFile The INI file to read.
   *
   * @return The {@link JsonObject} constructed from the file.
   */
  public static JsonObject iniToJson(File iniFile) {
    try {
      INIConfiguration ini = new INIConfiguration();

      try (FileReader fileReader = new FileReader(iniFile)) {
        ini.read(fileReader);
      }

      JsonObjectBuilder job = Json.createObjectBuilder();

      for (String sectionKey : ini.getSections()) {
        SubnodeConfiguration section = ini.getSection(sectionKey);

        JsonObjectBuilder sectionBuilder = Json.createObjectBuilder();

        Iterator<String> iter = section.getKeys();

        while (iter.hasNext()) {
          String  key   = iter.next();
          String  value = section.getProperty(key).toString();
          JsonUtilities.add(sectionBuilder, key, value);
        }

        job.add(sectionKey, sectionBuilder);
      }

      return job.build();

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
