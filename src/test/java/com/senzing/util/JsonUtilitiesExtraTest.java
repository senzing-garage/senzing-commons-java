package com.senzing.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.senzing.util.JsonUtilities.add;
import static com.senzing.util.JsonUtilities.addElement;
import static com.senzing.util.JsonUtilities.addProperty;
import static com.senzing.util.JsonUtilities.getDate;
import static com.senzing.util.JsonUtilities.getInstant;
import static com.senzing.util.JsonUtilities.getJsonArray;
import static com.senzing.util.JsonUtilities.getValue;
import static com.senzing.util.JsonUtilities.iniToJson;
import static com.senzing.util.JsonUtilities.jsonEscape;
import static com.senzing.util.JsonUtilities.normalizeJsonValue;
import static com.senzing.util.JsonUtilities.parseJsonObject;
import static com.senzing.util.JsonUtilities.toJsonArray;
import static com.senzing.util.JsonUtilities.toJsonArrayBuilder;
import static com.senzing.util.JsonUtilities.toJsonObject;
import static com.senzing.util.JsonUtilities.toJsonObjectBuilder;
import static com.senzing.util.JsonUtilities.toJsonText;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary tests for {@link JsonUtilities} targeting the corners not
 * covered by {@code JsonUtilitiesTest}: {@link JsonUtilities#jsonEscape}, the
 * {@code add()} overloads for typed builders, the {@code addProperty} / {@code
 * addElement} type-dispatch branches, the {@code toJsonObject} / {@code
 * toJsonArray} {@link Map} / {@link java.util.Collection} factories, the
 * reflective {@code getValue} from-Instant and parse fallbacks, and the {@code
 * iniToJson} error path.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class JsonUtilitiesExtraTest
{
  // -------------------------------------------------------------------
  // jsonEscape
  // -------------------------------------------------------------------

  @Test
  public void jsonEscapeNullReturnsLiteralNull()
  {
    // Per javadoc: if string is null, returns the literal "null".
    assertEquals("null", jsonEscape(null));
  }

  @Test
  public void jsonEscapeEmptyReturnsQuotedEmpty()
  {
    assertEquals("\"\"", jsonEscape(""));
  }

  @Test
  public void jsonEscapePlainStringReturnsQuoted()
  {
    // No characters need escaping; result is "<string>" verbatim.
    assertEquals("\"hello world\"", jsonEscape("hello world"));
  }

  @Test
  public void jsonEscapeQuoteAndBackslash()
  {
    // " and \ must be backslash-escaped per JSON.
    assertEquals("\"a\\\"b\"", jsonEscape("a\"b"));
    assertEquals("\"a\\\\b\"", jsonEscape("a\\b"));
  }

  @Test
  public void jsonEscapeBackspace()
  {
    assertEquals("\"a\\bb\"", jsonEscape("a\bb"));
  }

  @Test
  public void jsonEscapeFormfeed()
  {
    assertEquals("\"a\\fb\"", jsonEscape("a\fb"));
  }

  @Test
  public void jsonEscapeNewline()
  {
    assertEquals("\"a\\nb\"", jsonEscape("a\nb"));
  }

  @Test
  public void jsonEscapeCarriageReturn()
  {
    assertEquals("\"a\\rb\"", jsonEscape("a\rb"));
  }

  @Test
  public void jsonEscapeTab()
  {
    assertEquals("\"a\\tb\"", jsonEscape("a\tb"));
  }

  @Test
  public void jsonEscapeControlCharacterUnder0x10()
  {
    // U+0001 escapes as \u0001 (hex single-digit needs leading zero).
    assertEquals("\"\\u0001\"", jsonEscape("\u0001"));
    // U+0007 (BEL) — not in the named-escape set, control < 0x20.
    assertEquals("\"\\u0007\"", jsonEscape("\u0007"));
  }

  @Test
  public void jsonEscapeControlCharacterAtOrAbove0x10()
  {
    // U+0010 — two-digit hex, no leading zero needed.
    assertEquals("\"\\u0010\"", jsonEscape("\u0010"));
    // U+001F is the last control character.
    assertEquals("\"\\u001f\"", jsonEscape("\u001f"));
  }

  @Test
  public void jsonEscapeMixedContent()
  {
    // Combination of plain characters and several escapes in one string.
    String input = "x\"y\nz\tw";
    assertEquals("\"x\\\"y\\nz\\tw\"", jsonEscape(input));
  }

  @Test
  public void jsonEscapeSpaceIsNotEscaped()
  {
    // Space (0x20) is the boundary — NOT a control character, no escape.
    assertEquals("\" \"", jsonEscape(" "));
  }

  // -------------------------------------------------------------------
  // add(JsonObjectBuilder, key, <Type>) — null and non-null branches
  // -------------------------------------------------------------------

  /**
   * Builds a JsonObject by invoking {@code adder} on a fresh builder and
   * returns the resulting JsonObject.
   */
  private static JsonObject buildObject(java.util.function.Consumer<JsonObjectBuilder> adder)
  {
    JsonObjectBuilder job = Json.createObjectBuilder();
    adder.accept(job);
    return job.build();
  }

  @Test
  public void addJobJobBuilder()
  {
    JsonObjectBuilder inner = Json.createObjectBuilder().add("x", 1);
    JsonObject obj = buildObject(job -> add(job, "k", inner));
    assertEquals(1, obj.getJsonObject("k").getInt("x"));

    JsonObject nul = buildObject(job -> add(job, "k", (JsonObjectBuilder) null));
    assertTrue(nul.isNull("k"), "Null JsonObjectBuilder must add JSON null");
  }

  @Test
  public void addJobArrayBuilder()
  {
    JsonArrayBuilder inner = Json.createArrayBuilder().add("a");
    JsonObject obj = buildObject(job -> add(job, "k", inner));
    assertEquals("a", obj.getJsonArray("k").getString(0));

    JsonObject nul = buildObject(job -> add(job, "k", (JsonArrayBuilder) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobJsonValue()
  {
    JsonValue val = Json.createValue("hello");
    JsonObject obj = buildObject(job -> add(job, "k", val));
    assertEquals("hello", obj.getString("k"));

    JsonObject nul = buildObject(job -> add(job, "k", (JsonValue) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobString()
  {
    JsonObject obj = buildObject(job -> add(job, "k", "v"));
    assertEquals("v", obj.getString("k"));

    JsonObject nul = buildObject(job -> add(job, "k", (String) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobInteger()
  {
    JsonObject obj = buildObject(job -> add(job, "k", Integer.valueOf(42)));
    assertEquals(42, obj.getInt("k"));

    JsonObject nul = buildObject(job -> add(job, "k", (Integer) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobLong()
  {
    JsonObject obj = buildObject(job -> add(job, "k", Long.valueOf(99L)));
    assertEquals(99L, obj.getJsonNumber("k").longValue());

    JsonObject nul = buildObject(job -> add(job, "k", (Long) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobDouble()
  {
    JsonObject obj = buildObject(job -> add(job, "k", Double.valueOf(3.5)));
    assertEquals(3.5, obj.getJsonNumber("k").doubleValue(), 1e-9);

    JsonObject nul = buildObject(job -> add(job, "k", (Double) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobFloat()
  {
    // Per javadoc: Float is added via the double overload.
    JsonObject obj = buildObject(job -> add(job, "k", Float.valueOf(2.5f)));
    assertEquals(2.5, obj.getJsonNumber("k").doubleValue(), 1e-6);

    JsonObject nul = buildObject(job -> add(job, "k", (Float) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobBigInteger()
  {
    JsonObject obj = buildObject(
        job -> add(job, "k", new BigInteger("123456789012345678901234567890")));
    assertEquals(new BigInteger("123456789012345678901234567890"),
                 obj.getJsonNumber("k").bigIntegerValue());

    JsonObject nul = buildObject(job -> add(job, "k", (BigInteger) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobBigDecimal()
  {
    JsonObject obj = buildObject(
        job -> add(job, "k", new BigDecimal("1.23")));
    assertEquals(new BigDecimal("1.23"),
                 obj.getJsonNumber("k").bigDecimalValue());

    JsonObject nul = buildObject(job -> add(job, "k", (BigDecimal) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobBoolean()
  {
    JsonObject obj = buildObject(job -> add(job, "k", Boolean.TRUE));
    assertTrue(obj.getBoolean("k"));

    JsonObject nul = buildObject(job -> add(job, "k", (Boolean) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobDate()
  {
    Date date = Date.from(Instant.parse("2025-01-02T03:04:05Z"));
    JsonObject obj = buildObject(job -> add(job, "k", date));
    // Should round-trip through DATE_TIME_FORMATTER.
    Date parsed = getDate(obj, "k");
    assertEquals(date, parsed,
                 "Date should round-trip through add()/getDate()");

    JsonObject nul = buildObject(job -> add(job, "k", (Date) null));
    assertTrue(nul.isNull("k"));
  }

  @Test
  public void addJobTemporalAccessor()
  {
    Instant when = Instant.parse("2025-06-07T08:09:10Z");
    JsonObject obj = buildObject(job -> add(job, "k", when));
    Instant parsed = getInstant(obj, "k");
    assertEquals(when, parsed,
                 "Instant should round-trip through add()/getInstant()");

    JsonObject nul = buildObject(
        job -> add(job, "k", (java.time.temporal.TemporalAccessor) null));
    assertTrue(nul.isNull("k"));
  }

  // -------------------------------------------------------------------
  // add(JsonArrayBuilder, <Type>) — null and non-null branches
  // -------------------------------------------------------------------

  private static JsonArray buildArray(java.util.function.Consumer<JsonArrayBuilder> adder)
  {
    JsonArrayBuilder jab = Json.createArrayBuilder();
    adder.accept(jab);
    return jab.build();
  }

  @Test
  public void addJabJobBuilder()
  {
    JsonObjectBuilder inner = Json.createObjectBuilder().add("x", 1);
    JsonArray arr = buildArray(jab -> add(jab, inner));
    assertEquals(1, arr.getJsonObject(0).getInt("x"));

    JsonArray nul = buildArray(jab -> add(jab, (JsonObjectBuilder) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabArrayBuilder()
  {
    JsonArrayBuilder inner = Json.createArrayBuilder().add("a");
    JsonArray arr = buildArray(jab -> add(jab, inner));
    assertEquals("a", arr.getJsonArray(0).getString(0));

    JsonArray nul = buildArray(jab -> add(jab, (JsonArrayBuilder) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabJsonValue()
  {
    JsonArray arr = buildArray(jab -> add(jab, Json.createValue(7)));
    assertEquals(7, arr.getInt(0));

    JsonArray nul = buildArray(jab -> add(jab, (JsonValue) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabString()
  {
    JsonArray arr = buildArray(jab -> add(jab, "x"));
    assertEquals("x", arr.getString(0));

    JsonArray nul = buildArray(jab -> add(jab, (String) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabInteger()
  {
    JsonArray arr = buildArray(jab -> add(jab, Integer.valueOf(11)));
    assertEquals(11, arr.getInt(0));

    JsonArray nul = buildArray(jab -> add(jab, (Integer) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabLong()
  {
    JsonArray arr = buildArray(jab -> add(jab, Long.valueOf(22L)));
    assertEquals(22L, arr.getJsonNumber(0).longValue());

    JsonArray nul = buildArray(jab -> add(jab, (Long) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabDouble()
  {
    JsonArray arr = buildArray(jab -> add(jab, Double.valueOf(3.14)));
    assertEquals(3.14, arr.getJsonNumber(0).doubleValue(), 1e-9);

    JsonArray nul = buildArray(jab -> add(jab, (Double) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabFloat()
  {
    JsonArray arr = buildArray(jab -> add(jab, Float.valueOf(1.5f)));
    assertEquals(1.5, arr.getJsonNumber(0).doubleValue(), 1e-6);

    JsonArray nul = buildArray(jab -> add(jab, (Float) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabBigInteger()
  {
    JsonArray arr = buildArray(jab -> add(jab, new BigInteger("99999999999999999")));
    assertEquals(new BigInteger("99999999999999999"),
                 arr.getJsonNumber(0).bigIntegerValue());

    JsonArray nul = buildArray(jab -> add(jab, (BigInteger) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabBigDecimal()
  {
    JsonArray arr = buildArray(jab -> add(jab, new BigDecimal("0.5")));
    assertEquals(new BigDecimal("0.5"),
                 arr.getJsonNumber(0).bigDecimalValue());

    JsonArray nul = buildArray(jab -> add(jab, (BigDecimal) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabBoolean()
  {
    JsonArray arr = buildArray(jab -> add(jab, Boolean.FALSE));
    assertEquals(false, arr.getBoolean(0));

    JsonArray nul = buildArray(jab -> add(jab, (Boolean) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabDate()
  {
    Date date = Date.from(Instant.parse("2024-12-25T00:00:00Z"));
    JsonArray arr = buildArray(jab -> add(jab, date));
    assertNotNull(arr.getString(0),
                  "Date should be serialized as ISO-8601 text");

    JsonArray nul = buildArray(jab -> add(jab, (Date) null));
    assertTrue(nul.isNull(0));
  }

  @Test
  public void addJabTemporalAccessor()
  {
    Instant when = Instant.parse("2024-12-25T00:00:00Z");
    JsonArray arr = buildArray(jab -> add(jab, when));
    assertNotNull(arr.getString(0));

    JsonArray nul = buildArray(
        jab -> add(jab, (java.time.temporal.TemporalAccessor) null));
    assertTrue(nul.isNull(0));
  }

  // -------------------------------------------------------------------
  // addProperty / addElement type-dispatch branches
  // -------------------------------------------------------------------

  @Test
  public void addPropertyDispatchesAcrossAllTypes()
  {
    JsonObjectBuilder job = Json.createObjectBuilder();
    addProperty(job, "null", null);
    addProperty(job, "jobBuilder", Json.createObjectBuilder().add("x", 1));
    addProperty(job, "jabBuilder", Json.createArrayBuilder().add("y"));
    addProperty(job, "jsonValue", Json.createValue("z"));
    addProperty(job, "string", "hello");
    addProperty(job, "boolean", Boolean.TRUE);
    addProperty(job, "int", Integer.valueOf(1));
    addProperty(job, "long", Long.valueOf(2L));
    addProperty(job, "short", Short.valueOf((short) 3));
    addProperty(job, "float", Float.valueOf(4.5f));
    addProperty(job, "double", Double.valueOf(5.5));
    addProperty(job, "bigint", new BigInteger("6"));
    addProperty(job, "bigdec", new BigDecimal("7.7"));
    addProperty(job, "date", Date.from(Instant.parse("2025-01-01T00:00:00Z")));
    addProperty(job, "instant", Instant.parse("2025-02-02T00:00:00Z"));
    addProperty(job, "intArray", new int[] {1, 2, 3});
    addProperty(job, "objArray", new String[] {"a", "b"});
    addProperty(job, "list", List.of("c", "d"));
    addProperty(job, "stringMap",
                Map.of("k1", "v1", "k2", 2));
    Map<Object, Object> nonStringKeys = new LinkedHashMap<>();
    nonStringKeys.put(Integer.valueOf(1), "v");
    addProperty(job, "intKeyMap", nonStringKeys);
    addProperty(job, "fallback", new StringBuilder("toStrFallback"));

    JsonObject obj = job.build();

    assertTrue(obj.isNull("null"));
    assertEquals(1, obj.getJsonObject("jobBuilder").getInt("x"));
    assertEquals("y", obj.getJsonArray("jabBuilder").getString(0));
    assertEquals("z", obj.getString("jsonValue"));
    assertEquals("hello", obj.getString("string"));
    assertEquals(true, obj.getBoolean("boolean"));
    assertEquals(1, obj.getInt("int"));
    assertEquals(2L, obj.getJsonNumber("long").longValue());
    assertEquals(3, obj.getInt("short"));
    assertEquals(4.5, obj.getJsonNumber("float").doubleValue(), 1e-6);
    assertEquals(5.5, obj.getJsonNumber("double").doubleValue(), 1e-9);
    assertEquals(new BigInteger("6"), obj.getJsonNumber("bigint").bigIntegerValue());
    assertEquals(new BigDecimal("7.7"),
                 obj.getJsonNumber("bigdec").bigDecimalValue());
    assertNotNull(obj.getString("date"));
    assertNotNull(obj.getString("instant"));
    assertEquals(3, obj.getJsonArray("intArray").size());
    assertEquals(2, obj.getJsonArray("objArray").size());
    assertEquals("c", obj.getJsonArray("list").getString(0));
    // String-keyed map → nested object.
    assertEquals("v1", obj.getJsonObject("stringMap").getString("k1"));
    // Non-string-keyed map → toString fallback (a JSON string).
    assertEquals(JsonValue.ValueType.STRING,
                 obj.get("intKeyMap").getValueType());
    // StringBuilder is not handled by any special case — falls through to
    // value.toString().
    assertEquals("toStrFallback", obj.getString("fallback"));
  }

  @Test
  public void addPropertyMapWithNullKeyTreatedAsNonString()
  {
    JsonObjectBuilder job = Json.createObjectBuilder();
    Map<String, Object> mapWithNullKey = new HashMap<>();
    mapWithNullKey.put(null, "x");
    mapWithNullKey.put("k", "y");
    addProperty(job, "m", mapWithNullKey);

    JsonObject obj = job.build();
    // Null key disqualifies the "all string keys" branch; falls through
    // to value.toString().
    assertEquals(JsonValue.ValueType.STRING, obj.get("m").getValueType());
  }

  @Test
  public void addElementDispatchesAcrossAllTypes()
  {
    JsonArrayBuilder jab = Json.createArrayBuilder();
    addElement(jab, null);
    addElement(jab, Json.createObjectBuilder().add("x", 1));
    addElement(jab, Json.createArrayBuilder().add("y"));
    addElement(jab, Json.createValue("z"));
    addElement(jab, "hello");
    addElement(jab, Boolean.FALSE);
    addElement(jab, Integer.valueOf(1));
    addElement(jab, Long.valueOf(2L));
    addElement(jab, Short.valueOf((short) 3));
    addElement(jab, Float.valueOf(4.5f));
    addElement(jab, Double.valueOf(5.5));
    addElement(jab, new BigInteger("6"));
    addElement(jab, new BigDecimal("7.7"));
    addElement(jab, Date.from(Instant.parse("2025-01-01T00:00:00Z")));
    addElement(jab, Instant.parse("2025-02-02T00:00:00Z"));
    addElement(jab, new int[] {1, 2, 3});
    addElement(jab, List.of("c", "d"));
    addElement(jab, Map.of("k", "v"));
    Map<Object, Object> nonStringMap = new LinkedHashMap<>();
    nonStringMap.put(Integer.valueOf(1), "x");
    addElement(jab, nonStringMap);
    addElement(jab, new StringBuilder("toStrFallback"));

    JsonArray arr = jab.build();
    assertEquals(20, arr.size());
    assertTrue(arr.isNull(0));
    assertEquals(1, arr.getJsonObject(1).getInt("x"));
    assertEquals("y", arr.getJsonArray(2).getString(0));
    assertEquals("z", arr.getString(3));
    assertEquals("hello", arr.getString(4));
    assertEquals(false, arr.getBoolean(5));
    assertEquals(1, arr.getInt(6));
    assertEquals(2L, arr.getJsonNumber(7).longValue());
    assertEquals(3, arr.getInt(8));
    assertEquals(4.5, arr.getJsonNumber(9).doubleValue(), 1e-6);
    assertEquals(5.5, arr.getJsonNumber(10).doubleValue(), 1e-9);
    assertEquals(new BigInteger("6"), arr.getJsonNumber(11).bigIntegerValue());
    assertEquals(new BigDecimal("7.7"),
                 arr.getJsonNumber(12).bigDecimalValue());
    // Date and Instant serialize as ISO-8601 strings.
    assertEquals(JsonValue.ValueType.STRING, arr.get(13).getValueType());
    assertEquals(JsonValue.ValueType.STRING, arr.get(14).getValueType());
    assertEquals(3, arr.getJsonArray(15).size());
    assertEquals("c", arr.getJsonArray(16).getString(0));
    assertEquals("v", arr.getJsonObject(17).getString("k"));
    // Non-string-keyed map falls through to toString (a JSON string).
    assertEquals(JsonValue.ValueType.STRING, arr.get(18).getValueType());
    // StringBuilder fallback → toString().
    assertEquals("toStrFallback", arr.getString(19));
  }

  @Test
  public void addElementFallbackToToStringForUnknownType()
  {
    JsonArrayBuilder jab = Json.createArrayBuilder();
    Object custom = new Object()
    {
      @Override
      public String toString()
      {
        return "CUSTOM";
      }
    };
    addElement(jab, custom);
    JsonArray arr = jab.build();

    assertEquals("CUSTOM", arr.getString(0),
                 "Unknown type should be added via toString()");
  }

  // -------------------------------------------------------------------
  // toJsonObject / toJsonArray with Map / Collection
  // -------------------------------------------------------------------

  @Test
  public void toJsonObjectFromMapNullReturnsNull()
  {
    assertNull(toJsonObject((Map<String, ?>) null));
  }

  @Test
  public void toJsonObjectFromMapBuildsObject()
  {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", "two");
    JsonObject obj = toJsonObject(map);

    assertEquals(1, obj.getInt("a"));
    assertEquals("two", obj.getString("b"));
  }

  @Test
  public void toJsonObjectBuilderFromMapNullReturnsNull()
  {
    assertNull(toJsonObjectBuilder(null));
  }

  @Test
  public void toJsonObjectBuilderFromMapBuildsBuilder()
  {
    Map<String, Object> map = Map.of("k", "v");
    JsonObjectBuilder builder = toJsonObjectBuilder(map);

    assertNotNull(builder);
    assertEquals("v", builder.build().getString("k"));
  }

  @Test
  public void toJsonArrayFromListNullReturnsNull()
  {
    assertNull(toJsonArray((List<?>) null));
  }

  @Test
  public void toJsonArrayFromListBuildsArray()
  {
    JsonArray arr = toJsonArray(List.of("a", "b", "c"));
    assertEquals(3, arr.size());
    assertEquals("b", arr.getString(1));
  }

  @Test
  public void toJsonArrayFromCollectionNullReturnsNull()
  {
    assertNull(toJsonArray((java.util.Collection<?>) null));
  }

  @Test
  public void toJsonArrayBuilderFromListNullReturnsNull()
  {
    assertNull(toJsonArrayBuilder((List<?>) null));
  }

  @Test
  public void toJsonArrayBuilderFromCollectionNullReturnsNull()
  {
    assertNull(toJsonArrayBuilder((java.util.Collection<?>) null));
  }

  @Test
  public void toJsonArrayBuilderFromCollectionBuildsBuilder()
  {
    JsonArrayBuilder builder = toJsonArrayBuilder(Arrays.asList(1, 2, 3));
    assertNotNull(builder);
    assertEquals(3, builder.build().size());
  }

  // -------------------------------------------------------------------
  // normalizeJsonValue cases
  // -------------------------------------------------------------------

  @Test
  public void normalizeJsonValueAllValueTypes()
  {
    // Per javadoc:
    //   NULL -> null, TRUE -> Boolean.TRUE, FALSE -> Boolean.FALSE,
    //   STRING -> String, NUMBER (integral) -> Long, NUMBER (frac) -> Double,
    //   ARRAY -> List, OBJECT -> Map.
    assertNull(normalizeJsonValue(null),
               "null input must yield null result");
    assertNull(normalizeJsonValue(JsonValue.NULL));
    assertSame(Boolean.TRUE, normalizeJsonValue(JsonValue.TRUE));
    assertSame(Boolean.FALSE, normalizeJsonValue(JsonValue.FALSE));
    assertEquals("hi", normalizeJsonValue(Json.createValue("hi")));
    assertEquals(Long.valueOf(7L), normalizeJsonValue(Json.createValue(7)));
    assertEquals(Double.valueOf(2.5),
                 normalizeJsonValue(Json.createValue(2.5)));

    JsonArray arr = Json.createArrayBuilder().add(1).add("x").build();
    Object normalizedArr = normalizeJsonValue(arr);
    assertTrue(normalizedArr instanceof List,
               "ARRAY must normalize to List");
    assertEquals(2, ((List<?>) normalizedArr).size());

    JsonObject obj = Json.createObjectBuilder().add("a", 1).build();
    Object normalizedObj = normalizeJsonValue(obj);
    assertTrue(normalizedObj instanceof Map,
               "OBJECT must normalize to Map");
    assertEquals(1L, ((Map<?, ?>) normalizedObj).get("a"));
  }

  // -------------------------------------------------------------------
  // getInstant edge cases
  // -------------------------------------------------------------------

  @Test
  public void getInstantFromNullObjectReturnsDefault()
  {
    Instant now = Instant.now();
    assertEquals(now, getInstant((JsonObject) null, "k", now),
                 "Null object should yield default value");
  }

  @Test
  public void getInstantFromNullArrayReturnsDefault()
  {
    Instant now = Instant.now();
    assertEquals(now, getInstant((JsonArray) null, 0, now),
                 "Null array should yield default value");
  }

  @Test
  public void getInstantFromArrayThrowsOnNonStringNonNullValue()
  {
    // Per javadoc switch: NULL -> default, STRING -> parse, default ->
    // IllegalArgumentException.
    JsonArray arr = Json.createArrayBuilder().add(42).build();
    assertThrows(IllegalArgumentException.class,
                 () -> getInstant(arr, 0, null),
                 "Non-string, non-null value must throw IllegalArgumentException");
  }

  @Test
  public void getInstantFromArrayMalformedStringThrowsParseException()
  {
    JsonArray arr = Json.createArrayBuilder().add("not-a-date").build();
    assertThrows(DateTimeParseException.class,
                 () -> getInstant(arr, 0, null));
  }

  @Test
  public void getJsonArrayFromNullObjectReturnsNull()
  {
    assertNull(getJsonArray((JsonObject) null, "k"),
               "Null object should yield null");
  }

  @Test
  public void getJsonArrayFromNullArrayReturnsNull()
  {
    assertNull(getJsonArray((JsonArray) null, 0));
  }

  @Test
  public void getJsonArrayAtNullIndexReturnsNull()
  {
    JsonArray arr = Json.createArrayBuilder().addNull().build();
    assertNull(getJsonArray(arr, 0));
  }

  // -------------------------------------------------------------------
  // getValue(Class<T>, ...) reflective dispatch
  // -------------------------------------------------------------------

  @Test
  public void getValueFromObjectMissingKeyReturnsDefault()
  {
    JsonObject obj = Json.createObjectBuilder().add("a", 1).build();
    String def = "default";
    assertEquals(def, getValue(String.class, obj, "missing", def),
                 "Missing key should yield default value");
  }

  @Test
  public void getValueFromNullObjectReturnsDefault()
  {
    String def = "default";
    assertEquals(def, getValue(String.class, (JsonObject) null, "k", def));
  }

  @Test
  public void getValueFromObjectDirectMethodMatch()
  {
    JsonObject obj = Json.createObjectBuilder().add("k", "hello").build();
    // String has a direct getString getter, so the OBJECT_TYPE_METHODS
    // dispatch hits that branch.
    assertEquals("hello", getValue(String.class, obj, "k"));
  }

  @Test
  public void getValueFromObjectFromInstantFallback()
  {
    // Date.from(Instant) is public static; the reflective fallback
    // should locate it and use the Instant getter to populate.
    // The text must conform to JsonUtilities.DATE_TIME_FORMATTER pattern
    // (yyyy-MM-dd'T'HH:mm:ss.SSS'Z').
    JsonObject obj = Json.createObjectBuilder()
        .add("k", "2025-04-01T12:00:00.000Z")
        .build();
    Date result = getValue(Date.class, obj, "k");
    assertNotNull(result, "Date.from(Instant) fallback should populate");
    assertEquals(Instant.parse("2025-04-01T12:00:00Z"), result.toInstant());
  }

  @Test
  public void getValueFromArrayMissingIndexFallsBackToDefault()
  {
    JsonArray arr = Json.createArrayBuilder().build();
    String def = "default";
    // Out-of-bounds path is undocumented but the null-array path is.
    assertEquals(def, getValue(String.class, (JsonArray) null, 0, def),
                 "Null array should yield default");
  }

  @Test
  public void getValueFromObjectPrimitivePromoted()
  {
    JsonObject obj = Json.createObjectBuilder().add("k", 5).build();
    // int.class (primitive) should promote to Integer for the lookup.
    Integer result = getValue(int.class, obj, "k");
    assertEquals(5, result.intValue());
  }

  // -------------------------------------------------------------------
  // toJsonText overloads
  // -------------------------------------------------------------------

  @Test
  public void toJsonTextWriterJsonValueReturnsWriter()
  {
    StringWriter sw = new StringWriter();
    Writer ret = toJsonText(sw, Json.createValue("x"));
    assertSame(sw, ret, "toJsonText(Writer, JsonValue) must return same writer");
    assertEquals("\"x\"", sw.toString());
  }

  @Test
  public void toJsonTextWriterNullJsonValueWritesJsonNull()
  {
    StringWriter sw = new StringWriter();
    toJsonText(sw, (JsonValue) null);
    // Per implementation: null jsonValue writes JsonValue.NULL.
    assertEquals("null", sw.toString());
  }

  @Test
  public void toJsonTextObjectBuilderReturnsWriter()
  {
    StringWriter sw = new StringWriter();
    JsonObjectBuilder job = Json.createObjectBuilder().add("k", "v");
    Writer ret = toJsonText(sw, job);
    assertSame(sw, ret);
    assertTrue(sw.toString().contains("\"k\""));
  }

  @Test
  public void toJsonTextArrayBuilderReturnsWriter()
  {
    StringWriter sw = new StringWriter();
    JsonArrayBuilder jab = Json.createArrayBuilder().add(1).add(2);
    Writer ret = toJsonText(sw, jab);
    assertSame(sw, ret);
    assertTrue(sw.toString().contains("1"));
  }

  @Test
  public void toJsonTextObjectBuilderToString()
  {
    String s = toJsonText(Json.createObjectBuilder().add("k", 1));
    assertTrue(s.contains("\"k\""));
  }

  @Test
  public void toJsonTextArrayBuilderToString()
  {
    String s = toJsonText(Json.createArrayBuilder().add(7));
    assertTrue(s.contains("7"));
  }

  @Test
  public void toJsonTextPrettyPrintPath()
  {
    // Pretty-print returns formatted text containing a newline.
    String s = toJsonText(
        Json.createObjectBuilder().add("k", 1), true);
    assertTrue(s.contains("\n"),
               "Pretty-print should produce multi-line output");
  }

  @Test
  public void toJsonTextWriterNullThrows()
  {
    // Per Objects.requireNonNull in implementation.
    assertThrows(NullPointerException.class,
                 () -> toJsonText(null, Json.createValue("x")));
  }

  // -------------------------------------------------------------------
  // iniToJson
  // -------------------------------------------------------------------

  @Test
  public void iniToJsonNullFileThrows()
  {
    assertThrows(NullPointerException.class,
                 () -> iniToJson(null));
  }

  @Test
  public void iniToJsonReadsFile() throws IOException
  {
    File temp = File.createTempFile("jutest-", ".ini");
    temp.deleteOnExit();
    Files.writeString(temp.toPath(),
                      "[section]\nkey1=value1\nkey2=value2\n");
    JsonObject result = iniToJson(temp);

    assertEquals("value1",
                 result.getJsonObject("section").getString("key1"));
    assertEquals("value2",
                 result.getJsonObject("section").getString("key2"));
  }

  @Test
  public void iniToJsonNonexistentFileThrowsRuntime()
  {
    File missing = new File("/nonexistent/path/missing.ini");
    // Per implementation: IOException is wrapped in RuntimeException.
    assertThrows(RuntimeException.class, () -> iniToJson(missing));
  }
}
