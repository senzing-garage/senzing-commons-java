package com.senzing.reflect;

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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link PropertyReflector}.
 */
@SuppressWarnings("unchecked")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class PropertyReflectorTest {
  protected static class Address {
    private String street;
    private String city;
    private String state;
    private String zip;

    public Address(String street, String city, String state, String zip) {
      this.street = street;
      this.city = city;
      this.state = state;
      this.zip = zip;
    }

    public String getStreet() {
      return this.street;
    }

    public String getCity() {
      return this.city;
    }

    public String getState() {
      return this.state;
    }

    public String getZip() {
      return this.zip;
    }

    public String toString() {
      return this.getStreet() + "; " + this.getCity() + ", "
          + this.getState() + " " + this.getZip();
    }
  }

  protected static class Person {
    private String name;
    private List<Address> addresses;
    private Person spouse;

    public Person(String name) {
      this.name = name;
      this.addresses = new LinkedList<>();
      this.spouse = null;
    }

    public String getName() {
      return this.name;
    }

    public List<Address> getAddresses() {
      return Collections.unmodifiableList(this.addresses);
    }

    public void addAddress(Address address) {
      this.addresses.add(address);
    }

    public Person getSpouse() {
      return this.spouse;
    }

    public void setSpouse(Person spouse) {
      this.spouse = spouse;
    }

    public String toString() {
      return this.getName();
    }
  }

  protected interface Shape {
    double getPerimeter();

    double getArea();
  }

  protected interface Polygon extends Shape {
    int getSideCount();
  }

  protected static class Rectangle implements Polygon {
    private double width;
    private double length;

    public Rectangle(double width, double length) {
      this.width = width;
      this.length = length;
    }

    public int getSideCount() {
      return 4;
    }

    public double getWidth() {
      return this.width;
    }

    public double getLength() {
      return this.length;
    }

    public void setWidth(double width) {
      this.width = width;
    }

    public void setLength(double length) {
      this.length = length;
    }

    public double getArea() {
      return this.getWidth() * this.getLength();
    }

    public double getPerimeter() {
      return ((2 * this.getWidth()) + (2 * this.getLength()));
    }

    public String toString() {
      return "Rectangle: " + this.getWidth() + " x " + this.getLength();
    }
  }

  protected static class Square extends Rectangle {
    private static boolean shapeNameUpper = false;

    public Square(double length) {
      super(length, length);
    }

    public int getSideCount() {
      return 4;
    }

    public void setWidth(double width) {
      this.setSide(width);
    }

    public void setLength(double length) {
      this.setSide(length);
    }

    // should not be a property
    protected void setSide(double side) {
      super.setWidth(side);
      super.setLength(side);
    }

    // should not be a property
    public static String getShapeName() {
      if (shapeNameUpper) {
        return "SQUARE";
      } else {
        return "square";
      }
    }

    // should not be a property
    public static void setShapeNameUpperCase(boolean upperCase) {
      shapeNameUpper = upperCase;
    }

    public String toString() {
      return "Square: " + this.getWidth() + " x " + this.getLength();
    }
  }

  protected static class Circle implements Shape {
    private double radius;

    public Circle(double radius) {
      this.radius = radius;
    }

    public double getArea() {
      return Math.PI * (this.getRadius() * this.getRadius());
    }

    public double getPerimeter() {
      return this.getCircumference();
    }

    public double getRadius() {
      return this.radius;
    }

    public void setRadius(double radius) {
      this.radius = radius;
    }

    protected double getCircumference() {
      return this.getRadius() * 2.0 * Math.PI;
    }

    public String toString() {
      return "Circle: " + this.getRadius();
    }
  }

  public List<Arguments> getPropertiesTestParameters() {
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
      Set<String> expectedSetters) {
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

  public List<Arguments> getGetPropertyValueTestParameters() {
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
      Class expectedFailure) {
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

  private Rectangle newRectangle() {
    return new Rectangle(10.0, 20.0);
  }

  public List<Arguments> getSetPropertyValueTestParameters() {
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
      Class expectedFailure) {
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
       * // COMMENT THIS OUT SINCE WE ARE NOT DOING NUMERIC CONVERSIONS SINCE THEY
       * // DON'T ALWAYS MAKE SENSE DUE TO PRECISION LOSS
       * //
       * // make sure the result is correct
       * if (propertyValue != null
       * && result != null
       * && propertyValue instanceof Number
       * && result instanceof Number
       * && getPrimitiveType(propertyValue.getClass()) != null)
       * {
       * result = convertPrimitiveNumber((Number) result,
       * propertyValue.getClass());
       * }
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

  public List<Arguments> getToJsonObjectParameters() {
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
      Class expectedFailure) {
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
}
