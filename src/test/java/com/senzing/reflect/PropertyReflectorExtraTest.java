package com.senzing.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Supplementary {@link PropertyReflector} tests covering the
 * JSON-type dispatch branches in {@code addToJsonArray} (every value
 * type when added as a collection element), the JsonObject/JsonArray
 * cases in {@code buildJsonObject}, the nested-object recursion path,
 * and the circular-reference detection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class PropertyReflectorExtraTest
{
  // -------------------------------------------------------------------
  // Test fixtures
  // -------------------------------------------------------------------

  /**
   * Bean with a single property whose value is a {@code List<Object>}
   * containing one of every value type the {@code addToJsonArray}
   * dispatch handles via its switch statement.
   *
   * <p>Notably absent: JsonObject and JsonArray values. The switch in
   * {@code addToJsonArray} and {@code buildJsonObject} uses
   * {@code propertyValue.getClass().getName()}, which for instances
   * built via {@code Json.createObjectBuilder()} resolves to the
   * implementation class (e.g. {@code org.glassfish.json.JsonObjectImpl})
   * — never to the {@code javax.json.JsonObject} interface name. So
   * those switch arms are effectively unreachable for ordinary
   * inputs, and including them as collection elements falls through
   * to a default-branch bean-reflection path that fails with
   * {@code IllegalAccessException} because the glassfish package is
   * not exported.</p>
   */
  public static class CollectionElementBean
  {
    public List<Object> getElements()
    {
      List<Object> list = new java.util.ArrayList<>();
      list.add(null);
      list.add(Integer.valueOf(1));
      list.add(Short.valueOf((short) 2));
      list.add(Long.valueOf(3L));
      list.add(Float.valueOf(4.0f));
      list.add(Double.valueOf(5.0));
      list.add(Boolean.TRUE);
      list.add(new BigDecimal("6.5"));
      list.add(BigInteger.valueOf(7L));
      list.add("eight");
      list.add(List.of(9, 10));
      list.add(new int[] {11, 12});
      list.add(new SimpleNested("inner"));
      return list;
    }
  }

  /**
   * A simple bean used to exercise the nested-object branch of
   * {@code addToJsonArray} — anything that isn't one of the
   * documented types and isn't a Collection or array.
   */
  public static class SimpleNested
  {
    private final String label;

    public SimpleNested(String label)
    {
      this.label = label;
    }

    public String getLabel()
    {
      return this.label;
    }
  }

  /**
   * Bean exposing a nested-object property to exercise the
   * {@code default} branch of {@code buildJsonObject}'s switch (where
   * the value is neither a primitive wrapper nor a Collection nor an
   * array).
   */
  public static class NestedBean
  {
    public SimpleNested getNested()
    {
      return new SimpleNested("hello");
    }
  }

  /**
   * Bean used to verify circular-reference detection in
   * {@code addToJsonArray}: a list whose contained object refers back
   * to the bean itself.
   */
  public static class CircularBean
  {
    private final List<Object> list = new java.util.ArrayList<>();

    public CircularBean()
    {
      this.list.add(this);
    }

    public List<Object> getSelfList()
    {
      return this.list;
    }
  }

  // -------------------------------------------------------------------
  // toJsonObject — every type as a Collection element
  //
  // NOTE: The JsonObject / JsonArray switch cases in
  // {@code addToJsonArray} and {@code buildJsonObject} compare on
  // {@code propertyValue.getClass().getName()} — which for instances
  // built via {@code Json.createObjectBuilder()} resolves to the
  // implementation class (e.g. {@code org.glassfish.json.JsonObjectImpl}),
  // not the {@code javax.json.JsonObject} interface name. As a result
  // those switch arms are unreachable for ordinary inputs and trying
  // to feed them through tests fails with IllegalAccessException
  // because the implementation package is not exported. The branch
  // coverage gap there reflects an unrelated production-code issue,
  // not test coverage; the documented Java-bean dispatch is exercised
  // below.
  // -------------------------------------------------------------------

  @Test
  public void toJsonObjectCollectionElementsDispatchAllTypes()
  {
    JsonObject json = PropertyReflector.toJsonObject(
        new CollectionElementBean());

    JsonArray arr = json.getJsonArray("elements");
    // 13 elements: null, Integer, Short, Long, Float, Double, Boolean,
    // BigDecimal, BigInteger, String, nested Collection, nested array,
    // nested object.
    assertEquals(13, arr.size());

    // null
    assertTrue(arr.isNull(0));
    // Integer / Short -> int branch
    assertEquals(1, arr.getInt(1));
    assertEquals(2, arr.getInt(2));
    // Long -> long branch
    assertEquals(3L, arr.getJsonNumber(3).longValue());
    // Float / Double -> double branch
    assertEquals(4.0, arr.getJsonNumber(4).doubleValue(), 1e-6);
    assertEquals(5.0, arr.getJsonNumber(5).doubleValue(), 1e-9);
    // Boolean
    assertEquals(true, arr.getBoolean(6));
    // BigDecimal
    assertEquals(new BigDecimal("6.5"),
                 arr.getJsonNumber(7).bigDecimalValue());
    // BigInteger
    assertEquals(BigInteger.valueOf(7L),
                 arr.getJsonNumber(8).bigIntegerValue());
    // String
    assertEquals("eight", arr.getString(9));
    // Nested Collection
    assertEquals(2, arr.getJsonArray(10).size());
    assertEquals(9, arr.getJsonArray(10).getInt(0));
    // Nested array
    assertEquals(11, arr.getJsonArray(11).getInt(0));
    // Nested object — recurses into addToJsonArray's "default" branch.
    assertEquals("inner", arr.getJsonObject(12).getString("label"));
  }

  // -------------------------------------------------------------------
  // toJsonObject — nested object property (default branch in
  // buildJsonObject's switch)
  // -------------------------------------------------------------------

  @Test
  public void toJsonObjectNestedObjectPropertyRecursesIntoBean()
  {
    JsonObject json = PropertyReflector.toJsonObject(new NestedBean());

    JsonObject nested = json.getJsonObject("nested");
    assertNotNull(nested,
                  "Nested non-collection, non-primitive property "
                      + "should be serialized as a JSON object");
    assertEquals("hello", nested.getString("label"));
  }

  // -------------------------------------------------------------------
  // Circular reference detection
  // -------------------------------------------------------------------

  @Test
  public void toJsonObjectCircularReferenceThrowsIllegalState()
  {
    // CircularBean.list contains itself, so addToJsonArray recurses
    // into a value that is already in the visited map.
    assertThrows(IllegalStateException.class,
                 () -> PropertyReflector.toJsonObject(new CircularBean()),
                 "Circular reference should throw IllegalStateException");
  }

  // -------------------------------------------------------------------
  // buildJsonObject(builder, object) — public overload
  // -------------------------------------------------------------------

  @Test
  public void buildJsonObjectPublicOverloadAddsToBuilder()
  {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonObjectBuilder ret = PropertyReflector.buildJsonObject(
        builder, new NestedBean());
    assertSame(builder, ret,
               "buildJsonObject should return the supplied builder");

    JsonObject obj = builder.build();
    assertEquals("hello",
                 obj.getJsonObject("nested").getString("label"));
  }

  @Test
  public void buildJsonObjectNullBuilderThrows()
  {
    assertThrows(NullPointerException.class,
                 () -> PropertyReflector.buildJsonObject(
                     null, new NestedBean()));
  }

  @Test
  public void buildJsonObjectNullObjectThrows()
  {
    assertThrows(NullPointerException.class,
                 () -> PropertyReflector.buildJsonObject(
                     Json.createObjectBuilder(), null));
  }

  // -------------------------------------------------------------------
  // toJsonObject from a Map<String, ?> — value type dispatch
  // -------------------------------------------------------------------

  @Test
  public void toJsonObjectFromMapDispatchesValueTypes()
  {
    // (See note above — JsonObject / JsonArray inputs go through an
    // unreachable switch arm and would fail on default-branch
    // reflection of an unexported impl class.)
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("nullVal", null);
    map.put("integer", 1);
    map.put("aShort", (short) 2);
    map.put("aLong", 3L);
    map.put("aFloat", 4.0f);
    map.put("aDouble", 5.0);
    map.put("aBool", true);
    map.put("bigDec", new BigDecimal("6.5"));
    map.put("bigInt", BigInteger.valueOf(7L));
    map.put("aString", "eight");
    map.put("aColl", List.of(9, 10));
    map.put("anArray", new int[] {11, 12});
    map.put("nested", new SimpleNested("nine"));

    JsonObject json = PropertyReflector.toJsonObject(map);

    assertTrue(json.isNull("nullVal"));
    assertEquals(1, json.getInt("integer"));
    assertEquals(2, json.getInt("aShort"));
    assertEquals(3L, json.getJsonNumber("aLong").longValue());
    assertEquals(4.0, json.getJsonNumber("aFloat").doubleValue(), 1e-6);
    assertEquals(5.0, json.getJsonNumber("aDouble").doubleValue(), 1e-9);
    assertEquals(true, json.getBoolean("aBool"));
    assertEquals(new BigDecimal("6.5"),
                 json.getJsonNumber("bigDec").bigDecimalValue());
    assertEquals(BigInteger.valueOf(7L),
                 json.getJsonNumber("bigInt").bigIntegerValue());
    assertEquals("eight", json.getString("aString"));
    assertEquals(2, json.getJsonArray("aColl").size());
    assertEquals(2, json.getJsonArray("anArray").size());
    assertEquals("nine",
                 json.getJsonObject("nested").getString("label"));
  }
}
