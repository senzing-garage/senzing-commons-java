package com.senzing.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link PropertyReflector}.
 */
@SuppressWarnings("unchecked")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class PropertyReflectorTest
{
  protected static class Address
  {
    private String street;
    private String city;
    private String state;
    private String zip;

    public Address(String street, String city, String state, String zip)
    {
      this.street = street;
      this.city = city;
      this.state = state;
      this.zip = zip;
    }

    public String getStreet()
    {
      return this.street;
    }

    public String getCity()
    {
      return this.city;
    }

    public String getState()
    {
      return this.state;
    }

    public String getZip()
    {
      return this.zip;
    }

    public String toString()
    {
      return this.getStreet() + "; " + this.getCity() + ", "
          + this.getState() + " " + this.getZip();
    }
  }

  protected static class Person
  {
    private String name;
    private List<Address> addresses;
    private Person spouse;

    public Person(String name)
    {
      this.name = name;
      this.addresses = new LinkedList<>();
      this.spouse = null;
    }

    public String getName()
    {
      return this.name;
    }

    public List<Address> getAddresses()
    {
      return Collections.unmodifiableList(this.addresses);
    }

    public void addAddress(Address address)
    {
      this.addresses.add(address);
    }

    public Person getSpouse()
    {
      return this.spouse;
    }

    public void setSpouse(Person spouse)
    {
      this.spouse = spouse;
    }

    public String toString()
    {
      return this.getName();
    }
  }

  protected interface Shape
  {
    double getPerimeter();

    double getArea();
  }

  protected interface Polygon extends Shape
  {
    int getSideCount();
  }

  protected static class Rectangle implements Polygon
  {
    private double width;
    private double length;

    public Rectangle(double width, double length)
    {
      this.width = width;
      this.length = length;
    }

    public int getSideCount()
    {
      return 4;
    }

    public double getWidth()
    {
      return this.width;
    }

    public double getLength()
    {
      return this.length;
    }

    public void setWidth(double width)
    {
      this.width = width;
    }

    public void setLength(double length)
    {
      this.length = length;
    }

    public double getArea()
    {
      return this.getWidth() * this.getLength();
    }

    public double getPerimeter()
    {
      return ((2 * this.getWidth()) + (2 * this.getLength()));
    }

    public String toString()
    {
      return "Rectangle: " + this.getWidth() + " x " + this.getLength();
    }
  }

  protected static class Square extends Rectangle
  {
    private static boolean shapeNameUpper = false;

    public Square(double length)
    {
      super(length, length);
    }

    public int getSideCount()
    {
      return 4;
    }

    public void setWidth(double width)
    {
      this.setSide(width);
    }

    public void setLength(double length)
    {
      this.setSide(length);
    }

    // should not be a property
    protected void setSide(double side)
    {
      super.setWidth(side);
      super.setLength(side);
    }

    // should not be a property
    public static String getShapeName()
    {
      if (shapeNameUpper) {
        return "SQUARE";
      } else {
        return "square";
      }
    }

    // should not be a property
    public static void setShapeNameUpperCase(boolean upperCase)
    {
      shapeNameUpper = upperCase;
    }

    public String toString()
    {
      return "Square: " + this.getWidth() + " x " + this.getLength();
    }
  }

  protected static class Circle implements Shape
  {
    private double radius;

    public Circle(double radius)
    {
      this.radius = radius;
    }

    public double getArea()
    {
      return Math.PI * (this.getRadius() * this.getRadius());
    }

    public double getPerimeter()
    {
      return this.getCircumference();
    }

    public double getRadius()
    {
      return this.radius;
    }

    public void setRadius(double radius)
    {
      this.radius = radius;
    }

    protected double getCircumference()
    {
      return this.getRadius() * 2.0 * Math.PI;
    }

    public String toString()
    {
      return "Circle: " + this.getRadius();
    }
  }

  public List<Arguments> getPropertiesTestParameters()
  {
    List<Arguments> result = new LinkedList<>();
    result.add(arguments(Shape.class, Set.of("area", "perimeter"), Set.of()));

    result.add(arguments(
        Polygon.class, Set.of("area", "perimeter", "sideCount"), Set.of()));

    result.add(arguments(
        Rectangle.class,
        Set.of("area", "perimeter", "sideCount", "width", "length"),
        Set.of("width", "length")));

    result.add(arguments(
        Square.class,
        Set.of("area", "perimeter", "sideCount", "width", "length"),
        Set.of("width", "length")));

    result.add(arguments(
        Circle.class,
        Set.of("area", "perimeter", "radius"),
        Set.of("radius")));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getPropertiesTestParameters")
  public void testProperties(Class<?> cls,
      Set<String> expectedGetters,
      Set<String> expectedSetters)
  {
    try {
      PropertyReflector<?> propRef = PropertyReflector.getInstance(cls);
      Map<String, Method> getters = propRef.getAccessors();
      Map<String, List<Method>> setters = propRef.getMutators();

      assertEquals(expectedGetters, getters.keySet(),
          "Unexpected accessor properties encountered");
      assertEquals(expectedSetters, setters.keySet(),
          "Unexpected mutator properties encountered");

      getters.forEach((propName, method) -> {
        assertTrue(method.getName().toUpperCase().endsWith(propName.toUpperCase()),
            "Unexpected accessor method for '" + propName
                + "' property: " + method);
        assertTrue(method.getName().startsWith("get")
            || method.getName().startsWith("is"),
            "Unexpected accessor method prefix for '" + propName
                + "' property: " + method);
      });

      setters.forEach((propName, methods) -> {
        methods.forEach((method) -> {
          assertTrue(method.getName().toUpperCase().endsWith(propName.toUpperCase()),
              "Unexpected mutator method for '" + propName
                  + "' property: " + method);
          assertTrue(method.getName().startsWith("set"),
              "Unexpected mutator method prefix for '" + propName
                  + "' property: " + method);
        });
      });

    } catch (Exception e) {
      e.printStackTrace();
      fail("testProperties() failed with exception: " + e);
    }
  }

  public List<Arguments> getGetPropertyValueTestParameters()
  {
    List<Arguments> result = new LinkedList<>();

    Rectangle rectangle = newRectangle();

    PropertyReflector<Rectangle> propRef = PropertyReflector.getInstance(Rectangle.class);

    Class illegalArg = IllegalArgumentException.class;

    result.add(arguments(propRef, rectangle, "width", 10.0, null));

    result.add(arguments(propRef, rectangle, "length", 20.0, null));

    result.add(arguments(propRef, rectangle, "area", 200.0, null));

    result.add(arguments(propRef, rectangle, "perimeter", 60.0, null));

    result.add(arguments(propRef, rectangle, "volume", null, illegalArg));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getGetPropertyValueTestParameters")
  public void testGetPropertyValue(PropertyReflector propertyReflector,
      Object target,
      String propertyKey,
      Object expectedValue,
      Class expectedFailure)
  {
    try {
      Object result = propertyReflector.getPropertyValue(target, propertyKey);

      // check if an exception was expected
      if (expectedFailure != null) {
        fail("Expected an exception when getting property (" + propertyKey
            + "), on target (" + target + "): "
            + expectedFailure.getName());
      }

      // make sure the result is correct
      assertEquals(expectedValue, result,
          "Unexpected result when getting property ("
              + propertyKey + "), on target (" + target + ").");

    } catch (Exception e) {
      // check if no failure is expected
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success when getting property (" + propertyKey
            + "), on target (" + target + "): " + e);
      }

      // check if the wrong exception type was produced
      if (!e.getClass().isAssignableFrom(expectedFailure)) {
        e.printStackTrace();
        fail("Unexpected exception type (" + e.getClass().getName()
            + ") when getting property (" + propertyKey
            + "), on target (" + target + ").  Expected: "
            + expectedFailure.getName());

      }
    }
  }

  private Rectangle newRectangle()
  {
    return new Rectangle(10.0, 20.0);
  }

  public List<Arguments> getSetPropertyValueTestParameters()
  {
    List<Arguments> result = new LinkedList<>();

    Rectangle rectangle = new Rectangle(10.0, 20.0);

    PropertyReflector<Rectangle> propRef = PropertyReflector.getInstance(Rectangle.class);

    Class illegalArg = IllegalArgumentException.class;
    Class unsupported = UnsupportedOperationException.class;
    Class classCast = ClassCastException.class;

    result.add(arguments(propRef, newRectangle(), "width", 12.0, null));

    result.add(arguments(propRef, newRectangle(), "length", 22.0, null));

    result.add(arguments(propRef, newRectangle(), "length", 22, classCast));

    result.add(arguments(propRef, newRectangle(), "length", 22.0F, classCast));

    result.add(arguments(propRef, newRectangle(), "length", "22.0", classCast));

    result.add(arguments(propRef, newRectangle(), "area", 200.0, unsupported));

    result.add(
        arguments(propRef, newRectangle(), "perimeter", 60.0, unsupported));

    result.add(arguments(propRef, newRectangle(), "volume", null, illegalArg));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getSetPropertyValueTestParameters")
  public void testSetPropertyValue(PropertyReflector propertyReflector,
      Object target,
      String propertyKey,
      Object propertyValue,
      Class expectedFailure)
  {
    try {
      propertyReflector.setPropertyValue(target, propertyKey, propertyValue);

      String propVal = (propertyValue == null) ? String.valueOf(null)
          : (propertyValue.getClass().getName() + ": " + propertyValue);

      // check if an exception was expected
      if (expectedFailure != null) {
        fail("Expected an exception when setting property (" + propertyKey
            + ") to value (" + propVal + "), on target (" + target
            + "): " + expectedFailure.getName());
      }

      Object result = propertyReflector.getPropertyValue(target, propertyKey);

      /*
       * // COMMENT THIS OUT SINCE WE ARE NOT DOING NUMERIC CONVERSIONS SINCE
       * THEY // DON'T ALWAYS MAKE SENSE DUE TO PRECISION LOSS // // make sure
       * the result is correct if (propertyValue != null && result != null &&
       * propertyValue instanceof Number && result instanceof Number &&
       * getPrimitiveType(propertyValue.getClass()) != null) { result =
       * convertPrimitiveNumber((Number) result, propertyValue.getClass()); }
       */

      assertEquals(propertyValue, result,
          "Unexpected property value after setting property ("
              + propertyKey + ") to value (" + propVal
              + "), on target (" + target + ").");

    } catch (Exception e) {
      String propVal = (propertyValue == null) ? ("" + null)
          : (propertyValue.getClass().getName() + ": " + propertyValue);

      // check if no failure is expected
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success when setting property (" + propertyKey
            + ") to value (" + propVal + "), on target (" + target
            + "): " + e);
      }

      // check if the wrong exception type was produced
      if (!e.getClass().isAssignableFrom(expectedFailure)) {
        e.printStackTrace();
        fail("Unexpected exception type (" + e.getClass().getName()
            + ") when setting property (" + propertyKey
            + ") to value (" + propVal + "), on target (" + target
            + ").  Expected: " + expectedFailure.getName());

      }
    }
  }

  public List<Arguments> getToJsonObjectParameters()
  {
    List<Arguments> result = new LinkedList<>();

    result.add(arguments(null, null, NullPointerException.class));

    Rectangle rect = new Rectangle(10.0, 20.0);
    JsonObjectBuilder job = Json.createObjectBuilder();
    job.add("width", 10.0);
    job.add("length", 20.0);
    job.add("sideCount", 4);
    job.add("area", 200.0);
    job.add("perimeter", 60.0);
    JsonObject rectObj = job.build();

    result.add(arguments(rect, rectObj, null));

    Circle circle = new Circle(15.0);

    job = Json.createObjectBuilder();
    job.add("radius", 15.0);
    job.add("area", 15.0 * 15.0 * Math.PI);
    job.add("perimeter", 2.0 * 15.0 * Math.PI);
    JsonObject circleObj = job.build();

    result.add(arguments(circle, circleObj, null));

    Person joeSchmoe = new Person("Joe Schmoe");
    joeSchmoe.addAddress(new Address("101 Main Street",
        "Las Vegas",
        "NV",
        "89143"));
    joeSchmoe.addAddress(new Address("35401 Beach Road",
        "Capistrano Beach",
        "CA",
        "92624"));

    job = Json.createObjectBuilder();
    job.add("name", "Joe Schmoe");
    job.addNull("spouse");
    JsonArrayBuilder jab = Json.createArrayBuilder();
    JsonObjectBuilder addrJob = Json.createObjectBuilder();
    addrJob.add("street", "101 Main Street");
    addrJob.add("city", "Las Vegas");
    addrJob.add("state", "NV");
    addrJob.add("zip", "89143");
    jab.add(addrJob);
    addrJob = Json.createObjectBuilder();
    addrJob.add("street", "35401 Beach Road");
    addrJob.add("city", "Capistrano Beach");
    addrJob.add("state", "CA");
    addrJob.add("zip", "92624");
    jab.add(addrJob);
    job.add("addresses", jab);

    JsonObject joeObj = job.build();
    result.add(arguments(joeSchmoe, joeObj, null));

    Person johnDoe = new Person("John Doe");
    Person janeDoe = new Person("Jane Doe");
    johnDoe.setSpouse(janeDoe);
    janeDoe.setSpouse(johnDoe);

    result.add(arguments(johnDoe, null, IllegalStateException.class));
    result.add(arguments(janeDoe, null, IllegalStateException.class));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getToJsonObjectParameters")
  public void toJsonObjectTest(Object object,
      JsonObject expectedResult,
      Class expectedFailure)
  {
    try {
      JsonObject result = PropertyReflector.toJsonObject(object);

      // check if an exception was expected
      if (expectedFailure != null) {
        fail("Expected an exception when converting object (" + object
            + ") to JSON (" + result + "): "
            + expectedFailure.getName());
      }

      assertEquals(expectedResult, result,
          "Unexpected converted JsonObject value");

    } catch (Exception e) {
      // check if no failure is expected
      if (expectedFailure == null) {
        e.printStackTrace();
        fail("Expected success when converting object (" + object
            + ") to JSON: " + e);

      } else if (!expectedFailure.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected a different exception when converting object ("
            + object + ") to JSON: " + e.getClass().getName());
      }
    }

  }

  // -------------------------------------------------------------------
  // Additional fixtures and tests for previously-uncovered branches
  // -------------------------------------------------------------------

  /**
   * Bean with a boolean property exposed via the {@code isXxx} naming
   * convention rather than {@code getXxx}. Used to exercise the
   * is-prefix accessor branch in the PropertyReflector constructor.
   */
  protected static class BoolBean
  {
    private boolean done;
    private Boolean valid;

    public boolean isDone()         { return this.done; }
    public Boolean isValid()        { return this.valid; }
    public void setDone(boolean d)  { this.done = d; }
    public void setValid(Boolean v) { this.valid = v; }
  }

  /**
   * Bean with a property that has only a setter (write-only property).
   * Used to exercise the {@code getPropertyValue} branch where the
   * key exists in the mutator map but not the accessor map.
   */
  protected static class WriteOnlyBean
  {
    private String secret;

    public void setSecret(String secret) { this.secret = secret; }

    /** Helper for the test to read back the value (not a property). */
    String peekSecret() { return this.secret; }
  }

  /**
   * Bean with a property that has overloaded setters of different
   * argument types. Used to exercise the type-matching branches in
   * {@code setPropertyValue}: exact-type match, primitive
   * promotion, and assignable-from fallback.
   */
  protected static class MultiSetterBean
  {
    private String tag;

    public String getTag() { return this.tag; }

    public void setTag(String s)   { this.tag = "S:" + s; }
    public void setTag(int n)      { this.tag = "I:" + n; }
    public void setTag(Object o)   { this.tag = "O:" + o; }
  }

  /**
   * Bean with a single non-primitive property mutator. Used to
   * verify that {@code setPropertyValue(target, key, null)}
   * succeeds when the (only) mutator accepts a non-primitive type.
   */
  protected static class NullableSetterBean
  {
    private String name;

    public String getName()           { return this.name; }
    public void setName(String name)  { this.name = name; }
  }

  /**
   * Bean with a single primitive-only mutator. Used to verify that
   * {@code setPropertyValue(target, key, null)} throws
   * {@link NullPointerException} when there is no non-primitive
   * mutator that can accept null.
   */
  protected static class PrimitiveOnlySetterBean
  {
    private int count;

    public int getCount()           { return this.count; }
    public void setCount(int count) { this.count = count; }
  }

  /**
   * Bean exercising every {@code switch} case in
   * {@code PropertyReflector.buildJsonObject} for the JSON-type
   * dispatch — Integer / Short / Long / Float / Double / Boolean /
   * BigDecimal / BigInteger / String / Collection / array.
   */
  protected static class TypedBean
  {
    public Integer    getI()  { return 1; }
    public Short      getSh() { return (short) 2; }
    public Long       getL()  { return 3L; }
    public Float      getF()  { return 4.0f; }
    public Double     getD()  { return 5.0d; }
    public Boolean    getB()  { return true; }
    public BigDecimal getBd() { return new BigDecimal("6.5"); }
    public BigInteger getBi() { return BigInteger.valueOf(7L); }
    public String     getS()  { return "eight"; }
    public List<Integer> getColl() { return List.of(9, 10); }
    public int[]      getArr() { return new int[] { 11, 12 }; }
  }

  // -------------------------------------------------------------------
  // Boolean is-prefix accessor
  // -------------------------------------------------------------------

  /**
   * The {@link PropertyReflector} constructor must recognize an
   * {@code isXxx} method that returns {@code boolean} (or
   * {@link Boolean}) as an accessor for property {@code xxx}, per
   * the implementation's Java-bean naming convention.
   */
  @Test
  public void isPrefixBooleanMethodsAreRecognizedAsAccessors()
  {
    PropertyReflector<BoolBean> reflector
        = PropertyReflector.getInstance(BoolBean.class);
    Set<String> accessors = reflector.getAccessors().keySet();
    assertTrue(accessors.contains("done"),
               "isDone() must be recognized as accessor for 'done'");
    assertTrue(accessors.contains("valid"),
               "isValid() returning Boolean must be recognized");
  }

  // -------------------------------------------------------------------
  // Read-only / write-only property behavior
  // -------------------------------------------------------------------

  /**
   * For a write-only property,
   * {@link PropertyReflector#getPropertyValue} must throw
   * {@link UnsupportedOperationException} (the property exists in
   * the mutator map but not the accessor map).
   */
  @Test
  public void getPropertyValueThrowsUnsupportedForWriteOnlyProperty()
  {
    PropertyReflector<WriteOnlyBean> reflector
        = PropertyReflector.getInstance(WriteOnlyBean.class);
    WriteOnlyBean bean = new WriteOnlyBean();
    bean.setSecret("x");
    assertThrows(UnsupportedOperationException.class,
                 () -> reflector.getPropertyValue(bean, "secret"));
  }

  /**
   * For a read-only property (no setter),
   * {@link PropertyReflector#setPropertyValue} must throw
   * {@link UnsupportedOperationException} per the existing
   * Rectangle "area"/"perimeter" cases. Adds a regression-style
   * assertion using the dedicated {@code Address} fixture which is
   * already entirely read-only.
   */
  @Test
  public void setPropertyValueThrowsUnsupportedForReadOnlyProperty()
  {
    PropertyReflector<Address> reflector
        = PropertyReflector.getInstance(Address.class);
    Address addr = new Address("a", "b", "c", "d");
    assertThrows(UnsupportedOperationException.class,
                 () -> reflector.setPropertyValue(addr, "city", "X"));
  }

  // -------------------------------------------------------------------
  // setPropertyValue overload resolution
  // -------------------------------------------------------------------

  /**
   * When multiple setters are available and the value type is an
   * exact match for one of them, that setter must be selected.
   */
  @Test
  public void setPropertyValueExactTypeMatchSelectsCorrectOverload()
  {
    PropertyReflector<MultiSetterBean> reflector
        = PropertyReflector.getInstance(MultiSetterBean.class);
    MultiSetterBean bean = new MultiSetterBean();
    reflector.setPropertyValue(bean, "tag", "hello");
    assertEquals("S:hello", bean.getTag(),
                 "Exact String overload must be invoked");
  }

  /**
   * When the value is a boxed type (Integer) and there's a primitive
   * overload (int), the primitive-promotion fallback must select
   * the int overload.
   */
  @Test
  public void setPropertyValuePrimitivePromotionSelectsIntOverload()
  {
    PropertyReflector<MultiSetterBean> reflector
        = PropertyReflector.getInstance(MultiSetterBean.class);
    MultiSetterBean bean = new MultiSetterBean();
    reflector.setPropertyValue(bean, "tag", 42);
    assertEquals("I:42", bean.getTag(),
                 "Integer-to-int primitive promotion must select"
                     + " setTag(int)");
  }

  /**
   * When neither exact match nor primitive promotion applies, the
   * isAssignableFrom fallback must select an overload whose
   * parameter type is a supertype of the value's type — here,
   * passing a {@link Date} to a class that has only
   * {@code setTag(String)}, {@code setTag(int)}, and
   * {@code setTag(Object)} must select the Object overload.
   */
  @Test
  public void setPropertyValueAssignableFromFallbackSelectsObjectOverload()
  {
    PropertyReflector<MultiSetterBean> reflector
        = PropertyReflector.getInstance(MultiSetterBean.class);
    MultiSetterBean bean = new MultiSetterBean();
    Date now = new Date(0L);
    reflector.setPropertyValue(bean, "tag", now);
    assertTrue(bean.getTag().startsWith("O:"),
               "Date should fall through to Object overload, got: "
                   + bean.getTag());
  }

  /**
   * Setting {@code null} on a property whose mutator accepts a
   * non-primitive type must succeed and the value must be
   * observable as {@code null} via {@code getPropertyValue}.
   */
  @Test
  public void setPropertyValueAcceptsNullForNonPrimitiveSetter()
  {
    PropertyReflector<NullableSetterBean> reflector
        = PropertyReflector.getInstance(NullableSetterBean.class);
    NullableSetterBean bean = new NullableSetterBean();
    bean.setName("non-null");

    reflector.setPropertyValue(bean, "name", null);
    assertNull(reflector.getPropertyValue(bean, "name"),
               "Setting null on a non-primitive setter must succeed"
                   + " and produce a null value");
  }

  /**
   * Setting {@code null} on a property whose only mutator accepts a
   * primitive type must throw {@link NullPointerException} with a
   * descriptive message (per the implementation's comment "no
   * primitives allowed").
   */
  @Test
  public void setPropertyValueRejectsNullForPrimitiveOnlySetter()
  {
    PropertyReflector<PrimitiveOnlySetterBean> reflector
        = PropertyReflector.getInstance(
            PrimitiveOnlySetterBean.class);
    PrimitiveOnlySetterBean bean = new PrimitiveOnlySetterBean();
    bean.setCount(5);

    NullPointerException npe = assertThrows(
        NullPointerException.class,
        () -> reflector.setPropertyValue(bean, "count", null));
    assertTrue(npe.getMessage().contains("count"),
               "NPE message must mention the property key: "
                   + npe.getMessage());
  }

  // -------------------------------------------------------------------
  // toJsonObject — value-type dispatch and Map handling
  // -------------------------------------------------------------------

  /**
   * {@link PropertyReflector#toJsonObject(Object)} on a Java-bean
   * with one accessor per documented {@code switch} case must
   * produce a JSON object whose keys are the property names and
   * whose values match the corresponding JSON-type representation
   * for each Java type (Integer, Short, Long, Float, Double,
   * Boolean, BigDecimal, BigInteger, String, Collection, array).
   */
  @Test
  public void toJsonObjectDispatchesEveryDocumentedType()
  {
    JsonObject json = PropertyReflector.toJsonObject(new TypedBean());

    assertEquals(1, json.getInt("i"));
    assertEquals(2, json.getInt("sh"));
    assertEquals(3L, json.getJsonNumber("l").longValue());
    // Float/Double both use the double JSON value branch.
    assertEquals(4.0d, json.getJsonNumber("f").doubleValue(), 0.0001);
    assertEquals(5.0d, json.getJsonNumber("d").doubleValue(), 0.0001);
    assertTrue(json.getBoolean("b"));
    assertEquals(new BigDecimal("6.5"),
                 json.getJsonNumber("bd").bigDecimalValue());
    assertEquals(BigInteger.valueOf(7L),
                 json.getJsonNumber("bi").bigIntegerValue());
    assertEquals("eight", json.getString("s"));

    // Collection branch
    assertEquals(2, json.getJsonArray("coll").size());
    assertEquals(9, json.getJsonArray("coll").getInt(0));
    assertEquals(10, json.getJsonArray("coll").getInt(1));

    // Array branch
    assertEquals(2, json.getJsonArray("arr").size());
    assertEquals(11, json.getJsonArray("arr").getInt(0));
    assertEquals(12, json.getJsonArray("arr").getInt(1));
  }

  /**
   * When the input object is a {@code Map<String, ?>},
   * {@link PropertyReflector#toJsonObject(Object)} must use the
   * map's keys directly as JSON property keys (not bean reflection).
   */
  @Test
  public void toJsonObjectUsesMapKeysAsJsonKeys()
  {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("alpha", 1);
    input.put("beta", "two");
    input.put("gamma", true);

    JsonObject json = PropertyReflector.toJsonObject(input);
    assertEquals(1, json.getInt("alpha"));
    assertEquals("two", json.getString("beta"));
    assertTrue(json.getBoolean("gamma"));
  }

  /**
   * A {@link Map} input with non-String keys must NOT be treated as
   * a key/value JSON object — instead, the implementation falls
   * through to bean reflection on the {@link Map} class itself
   * (which produces an empty/near-empty result, since {@link Map}'s
   * own getters are inherited from {@link Object} and skipped).
   */
  @Test
  public void toJsonObjectFallsBackToBeanReflectionForNonStringMap()
  {
    Map<Integer, String> input = new LinkedHashMap<>();
    input.put(1, "one");
    input.put(2, "two");

    // Should not throw; should produce a JsonObject (the contents
    // depend on what bean accessors LinkedHashMap exposes — the
    // assertion here is just that no map-key/value entries appear).
    JsonObject json = PropertyReflector.toJsonObject(input);
    assertFalse(json.containsKey("1"),
                "Non-String-keyed map must not be exposed as JSON"
                    + " entries: " + json);
    assertFalse(json.containsKey("2"));
  }
}
