package com.senzing.reflect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

import static com.senzing.reflect.ReflectionUtilities.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link ReflectionUtilities}.
 */
@SuppressWarnings("unchecked")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ReflectionUtilitiesTest
{
  protected interface TestInterface
  {
    void test();
  }

  protected class TestClass implements TestInterface
  {
    private Thread currentThread = null;
    private boolean failed = false;
    private final Object monitor = new Object();

    public boolean isFailed()
    {
      synchronized (this.monitor) {
        return this.failed;
      }
    }

    public void test()
    {
      Thread thread = Thread.currentThread();
      try {
        // set the current thread if null
        synchronized (this.monitor) {
          if (this.currentThread == null) {
            this.currentThread = thread;
          } else {
            this.failed = true;
          }
        }

        // sleep outside the synchronized block
        try {
          Thread.sleep(20);
        } catch (Exception e) {
          this.failed = true;
        }

      } finally {
        synchronized (this.monitor) {
          if (thread == this.currentThread) {
              this.currentThread = null;
          }
        }
      }
    }
  }

  @Test
  public void testSynchronizedProxy()
  {
    try {
      handleTest(true, null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testSynchronizedProxy() failed with exception: " + e);
    }
  }

  @Test
  public void testSynchronizedWithMonitorProxy()
  {
    try {
      final Object monitor = new Object();
      handleTest(true, monitor);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testSynchronizedWithMonitorProxy() failed with exception: " + e);
    }
  }

  @Test
  public void testUnsynchronizedAccess()
  {
    try {
      handleTest(false, null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testUnsynchronizedAccess() failed with exception: " + e);
    }
  }

  private void handleTest(boolean sync, Object monitor)
      throws Exception
  {
    final TestClass     testObject = new TestClass();
    final TestInterface syncObject = (monitor == null)
        ? synchronizedProxy(TestInterface.class, testObject)
        : synchronizedProxy(TestInterface.class, testObject, monitor);

    final TestInterface targetObject = (sync) ? syncObject : testObject;

    Runnable runnable = () -> {
      for (int index = 0; index < 100; index++) {
        targetObject.test();
      }
    };

    Thread thread1 = new Thread(runnable);
    Thread thread2 = new Thread(runnable);
    thread1.start();
    thread2.start();
    thread1.join();
    thread2.join();
    if (sync) {
      assertFalse(testObject.isFailed(),
                  "Synchronized test failed to protect data");
    } else {
      assertTrue(testObject.isFailed(),
                 "Unsynchronized test managed to protect data");
    }
  }

  public List<Arguments> getPrimitiveTypeTestParams()
  {
    List<Arguments> result = new LinkedList<>();
    result.add(arguments(byte.class, byte.class));
    result.add(arguments(short.class, short.class));
    result.add(arguments(int.class, int.class));
    result.add(arguments(long.class, long.class));
    result.add(arguments(float.class, float.class));
    result.add(arguments(double.class, double.class));
    result.add(arguments(boolean.class, boolean.class));
    result.add(arguments(char.class, char.class));
    result.add(arguments(void.class, void.class));

    result.add(arguments(Byte.class, byte.class));
    result.add(arguments(Short.class, short.class));
    result.add(arguments(Integer.class, int.class));
    result.add(arguments(Long.class, long.class));
    result.add(arguments(Float.class, float.class));
    result.add(arguments(Double.class, double.class));
    result.add(arguments(Boolean.class, boolean.class));
    result.add(arguments(Character.class, char.class));
    result.add(arguments(Void.class, void.class));

    result.add(arguments(null, null));
    result.add(arguments(String.class, null));
    result.add(arguments(BigInteger.class, null));
    result.add(arguments(BigDecimal.class, null));

    return result;
  }

  public List<Arguments> getPromotedTypeTestParams()
  {
    List<Arguments> result = new LinkedList<>();
    result.add(arguments(Byte.class, Byte.class));
    result.add(arguments(Short.class, Short.class));
    result.add(arguments(Integer.class, Integer.class));
    result.add(arguments(Long.class, Long.class));
    result.add(arguments(Float.class, Float.class));
    result.add(arguments(Double.class, Double.class));
    result.add(arguments(Boolean.class, Boolean.class));
    result.add(arguments(Character.class, Character.class));
    result.add(arguments(Void.class, Void.class));

    result.add(arguments(byte.class, Byte.class));
    result.add(arguments(short.class, Short.class));
    result.add(arguments(int.class, Integer.class));
    result.add(arguments(long.class, Long.class));
    result.add(arguments(float.class, Float.class));
    result.add(arguments(double.class, Double.class));
    result.add(arguments(boolean.class, Boolean.class));
    result.add(arguments(char.class, Character.class));
    result.add(arguments(void.class, Void.class));

    result.add(arguments(null, null));
    result.add(arguments(String.class, null));
    result.add(arguments(BigInteger.class, null));
    result.add(arguments(BigDecimal.class, null));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getPrimitiveTypeTestParams")
  public void getPrimitiveTypeTest(Class paramType, Class expectedResult)
  {
    Class result = ReflectionUtilities.getPrimitiveType(paramType);
    assertEquals(expectedResult, result,
                 "Unexpected primitive type for " + paramType);
  }

  @ParameterizedTest
  @MethodSource("getPromotedTypeTestParams")
  public void getPromotedTypeTest(Class paramType, Class expectedResult)
  {
    Class result = ReflectionUtilities.getPromotedType(paramType);
    assertEquals(expectedResult, result,
                 "Unexpected promoted type for " + paramType);
  }


  public List<Arguments> getConvertPrimitiveParams()
  {
    List<Arguments> result = new LinkedList<>();

    short shortVal = (short) 10;
    byte byteVal = (byte) 112;

    result.add(arguments(shortVal, Integer.class, 10, null));
    result.add(arguments(byteVal, Integer.class, 112, null));
    result.add(arguments(12.0F, Long.class, 12L, null));
    result.add(arguments(15.0, Float.class, 15.0F, null));
    result.add(arguments(1012L, Integer.class, 1012, null));
    result.add(arguments(12.5F, Double.class, 12.5, null));
    result.add(arguments(null, Integer.class, null, null));

    result.add(arguments(shortVal, int.class, 10, null));
    result.add(arguments(byteVal, int.class, 112, null));
    result.add(arguments(12.0F, long.class, 12L, null));
    result.add(arguments(15.0, float.class, 15.0F, null));
    result.add(arguments(1012L, int.class, 1012, null));
    result.add(arguments(12.5F, double.class, 12.5, null));
    result.add(arguments(null, int.class, null, null));

    result.add(arguments(10, null, null, NullPointerException.class));
    result.add(arguments(
        10, String.class, null, IllegalArgumentException.class));

    return result;
  }

  @ParameterizedTest
  @MethodSource("getConvertPrimitiveParams")
  public void convertPrimitiveNumberTest(Number value,
                                         Class  numType,
                                         Number expectedResult,
                                         Class  exceptionClass)
  {
    String valueClass = (value == null) ? null : value.getClass().getName();
    String targetType = (numType == null) ? null : numType.getName();

    try {
      Number result = ReflectionUtilities.convertPrimitiveNumber(value, numType);

      if (exceptionClass != null) {
        fail("Expected failure when converting " + value + " from "
                 + valueClass + " to " + targetType
                 + ": " + exceptionClass.getName());
      }
      assertEquals(expectedResult, result,
                   "Unexpected result when converting " + value
                       + " from " + valueClass + " to " + targetType);

    } catch (Exception e) {
      if (exceptionClass == null) {
        e.printStackTrace();
        fail("Expected " + expectedResult + " when converting " + value
                 + " from " + valueClass + " to " + targetType
                 + ", but got an exception: " + e.getClass().getName());

      } else if (!exceptionClass.isAssignableFrom(e.getClass())) {
        e.printStackTrace();
        fail("Expected a different exception when converting " + value
                 + " from " + valueClass + " to " + targetType
                 + ": " + e.getClass().getName());
      }
    }

  }

  // -------------------------------------------------------------------
  // restrictedProxy() contract — exercises ReflectionUtilities and
  // its private RestrictedHandler inner class.
  // -------------------------------------------------------------------

  /**
   * Test interface used by the {@code restrictedProxy} tests below. Two methods
   * so we can restrict one and verify the other still delegates to the
   * underlying target.
   */
  protected interface RestrictableInterface
  {
    String allowed();
    String restricted();
    String throwingMethod();
  }

  /** Test class that implements zero interfaces. */
  protected static class NoInterfacesClass
  {
    public String getValue()
    {
      return "no-interfaces";
    }
  }

  /** Concrete implementation used as the target for restricted proxies. */
  protected static class RestrictableImpl implements RestrictableInterface
  {
    @Override
    public String allowed()
    {
      return "allowed-result";
    }

    @Override
    public String restricted()
    {
      return "restricted-result";
    }

    @Override
    public String throwingMethod()
    {
      throw new IllegalStateException("intentional from target");
    }
  }

  /**
   * {@link ReflectionUtilities#restrictedProxy(Object,
   * java.lang.reflect.Method...)} must produce a proxy where the restricted
   * method throws {@link UnsupportedOperationException} per the javadoc, while
   * unrestricted methods still delegate to the target object.
   */
  @Test
  public void restrictedProxyBlocksOnlyRestrictedMethods() throws Exception
  {
    RestrictableImpl target = new RestrictableImpl();
    java.lang.reflect.Method restrictedMethod
        = RestrictableInterface.class.getMethod("restricted");

    RestrictableInterface proxy = (RestrictableInterface)
        ReflectionUtilities.restrictedProxy(target, restrictedMethod);

    assertEquals("allowed-result", proxy.allowed(),
                 "Unrestricted method must still delegate to target");
    assertThrows(UnsupportedOperationException.class,
                 proxy::restricted,
                 "Restricted method must throw"
                     + " UnsupportedOperationException");
  }

  /**
   * When the target's underlying method throws an exception, the proxy must
   * propagate the cause (not the wrapping
   * {@link java.lang.reflect.InvocationTargetException}).
   */
  @Test
  public void restrictedProxyPropagatesTargetExceptionCause()
      throws Exception
  {
    RestrictableImpl target = new RestrictableImpl();
    RestrictableInterface proxy = (RestrictableInterface)
        ReflectionUtilities.restrictedProxy(target);
    assertThrows(IllegalStateException.class, proxy::throwingMethod,
                 "Proxy must unwrap InvocationTargetException and"
                     + " propagate the original cause");
  }

  /**
   * {@code restrictedProxy(null, ...)} must throw
   * {@link NullPointerException} per the javadoc.
   */
  @Test
  public void restrictedProxyThrowsNpeForNullTarget()
  {
    assertThrows(NullPointerException.class,
                 () -> ReflectionUtilities.restrictedProxy(null));
  }

  /**
   * {@code restrictedProxy(target, (Method) null)} must throw
   * {@link NullPointerException} when one of the supplied methods is
   * null.
   */
  @Test
  public void restrictedProxyThrowsNpeForNullMethodArgument()
  {
    RestrictableImpl target = new RestrictableImpl();
    assertThrows(NullPointerException.class,
                 () -> ReflectionUtilities.restrictedProxy(
                     target, (java.lang.reflect.Method) null));
  }

  /**
   * The two-arg overload {@code restrictedProxy(ClassLoader, Object,
   * Method...)} must throw {@link NullPointerException} when given a null class
   * loader.
   */
  @Test
  public void restrictedProxyThrowsNpeForNullClassLoader()
  {
    RestrictableImpl target = new RestrictableImpl();
    assertThrows(NullPointerException.class,
                 () -> ReflectionUtilities.restrictedProxy(
                     null, target, new java.lang.reflect.Method[0]));
  }

  /**
   * If the target object implements no interfaces,
   * {@code restrictedProxy} must throw
   * {@link IllegalArgumentException} per the javadoc. Uses a custom
   * test class (rather than {@code java.lang.Object}) because system classes
   * have a {@code null} classloader, which would trip the NPE check in the
   * {@code (ClassLoader, Object, Method...)} overload before the
   * interface-check fires.
   */
  @Test
  public void restrictedProxyThrowsIaeForTargetWithNoInterfaces()
  {
    NoInterfacesClass target = new NoInterfacesClass();
    assertThrows(IllegalArgumentException.class,
                 () -> ReflectionUtilities.restrictedProxy(target));
  }

  /**
   * Re-restricting an already-restricted proxy with the same method set must
   * return the same proxy instance unchanged (per the implementation's "all
   * already restricted → return as-is" branch).
   */
  @Test
  public void restrictedProxyReturnsSameInstanceWhenAlreadyRestricted()
      throws Exception
  {
    RestrictableImpl target = new RestrictableImpl();
    java.lang.reflect.Method restrictedMethod
        = RestrictableInterface.class.getMethod("restricted");

    RestrictableInterface proxy = (RestrictableInterface)
        ReflectionUtilities.restrictedProxy(target, restrictedMethod);

    Object reproxied = ReflectionUtilities.restrictedProxy(
        proxy, restrictedMethod);
    assertSame(proxy, reproxied,
               "Re-applying the same restriction must short-circuit"
                   + " and return the same proxy");
  }

  /**
   * Re-restricting an already-restricted proxy with an additional method must
   * yield a new proxy that restricts both methods.
   */
  @Test
  public void restrictedProxyAddsAdditionalRestriction() throws Exception
  {
    RestrictableImpl target = new RestrictableImpl();
    java.lang.reflect.Method restrictedMethod
        = RestrictableInterface.class.getMethod("restricted");
    java.lang.reflect.Method allowedMethod
        = RestrictableInterface.class.getMethod("allowed");

    RestrictableInterface proxy1 = (RestrictableInterface)
        ReflectionUtilities.restrictedProxy(target, restrictedMethod);
    RestrictableInterface proxy2 = (RestrictableInterface)
        ReflectionUtilities.restrictedProxy(proxy1, allowedMethod);

    assertNotSame(proxy1, proxy2,
                  "Adding a new restriction must produce a new proxy");
    assertThrows(UnsupportedOperationException.class,
                 proxy2::allowed);
    assertThrows(UnsupportedOperationException.class,
                 proxy2::restricted);
  }

  // -------------------------------------------------------------------
  // Private MethodComparator — branch coverage via reflective access
  // -------------------------------------------------------------------

  /**
   * Test fixtures used to obtain {@link java.lang.reflect.Method} instances
   * that exercise each documented branch of the private
   * {@code ReflectionUtilities.MethodComparator}.
   *
   * <p>{@link CompA} provides several overloads of {@code alpha} (so
   * we can construct method pairs that share a name but differ in return type,
   * arity, or parameter type) and a separate
   * {@code beta} (different name). {@link CompB} provides an
   * {@code alpha} with a different return type from {@link CompA}'s
   * — used to exercise the "same name, different return types" branch since
   * Java disallows return-type-only differences within a single declaring
   * class.
   */
  protected interface CompA
  {
    void alpha();
    void alpha(int x);
    void alpha(String x);
    void alpha(int x, int y);
    void beta();
  }

  protected interface CompB
  {
    int alpha();
  }

  /**
   * Reflectively grab the private {@code METHOD_COMPARATOR} for direct testing.
   * Wrapped in an unchecked rethrow so the test methods don't have to declare
   * {@code throws}.
   */
  @SuppressWarnings("unchecked")
  private static java.util.Comparator<java.lang.reflect.Method>
      methodComparator()
  {
    try {
      java.lang.reflect.Field f
          = ReflectionUtilities.class.getDeclaredField(
              "METHOD_COMPARATOR");
      f.setAccessible(true);
      return (java.util.Comparator<java.lang.reflect.Method>)
          f.get(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The {@code MethodComparator} must return 0 when the two arguments are the
   * same {@link java.lang.reflect.Method}.
   */
  @Test
  public void methodComparatorReturnsZeroForEqualMethods() throws Exception
  {
    java.lang.reflect.Method alpha
        = CompA.class.getMethod("alpha");
    assertEquals(0, methodComparator().compare(alpha, alpha));
  }

  /**
   * The {@code MethodComparator} must order by method name when names differ,
   * returning the sign of {@code name1.compareTo(name2)}.
   */
  @Test
  public void methodComparatorOrdersByNameWhenNamesDiffer()
      throws Exception
  {
    java.lang.reflect.Method alpha
        = CompA.class.getMethod("alpha");
    java.lang.reflect.Method beta = CompA.class.getMethod("beta");
    int expected = "alpha".compareTo("beta");
    int actual = methodComparator().compare(alpha, beta);
    assertEquals(Integer.signum(expected), Integer.signum(actual),
                 "Sign must match the names' compareTo result");
  }

  /**
   * When two methods share a name but have different return types, the
   * comparator must order them by the return type's name (the second-tier
   * fall-through).
   */
  @Test
  public void methodComparatorOrdersByReturnTypeWhenNamesMatch()
      throws Exception
  {
    java.lang.reflect.Method aAlpha
        = CompA.class.getMethod("alpha");        // returns void
    java.lang.reflect.Method bAlpha
        = CompB.class.getMethod("alpha");        // returns int
    int expected
        = "void".compareTo("int");
    int actual = methodComparator().compare(aAlpha, bAlpha);
    assertEquals(Integer.signum(expected), Integer.signum(actual));
  }

  /**
   * When two methods share name and return type but differ in parameter count,
   * the comparator must order them by parameter count (longer parameter list
   * sorts later).
   */
  @Test
  public void methodComparatorOrdersByParamCount() throws Exception
  {
    java.lang.reflect.Method noArgs
        = CompA.class.getMethod("alpha");          // 0 params
    java.lang.reflect.Method twoArgs = CompA.class.getMethod(
        "alpha", int.class, int.class);            // 2 params
    int actual = methodComparator().compare(noArgs, twoArgs);
    assertTrue(actual < 0,
               "Method with fewer params must sort before one with"
                   + " more: got " + actual);

    int reverse = methodComparator().compare(twoArgs, noArgs);
    assertTrue(reverse > 0,
               "Reverse comparison must be positive: got " + reverse);
  }

  /**
   * When two methods share name, return type, and arity but differ in parameter
   * type, the comparator must order them by the parameter type's name.
   */
  @Test
  public void methodComparatorOrdersByParamTypeWhenAllElseEqual()
      throws Exception
  {
    java.lang.reflect.Method intArg = CompA.class.getMethod(
        "alpha", int.class);           // void alpha(int)
    java.lang.reflect.Method strArg = CompA.class.getMethod(
        "alpha", String.class);        // void alpha(String)
    int expected
        = int.class.getName().compareTo(String.class.getName());
    int actual = methodComparator().compare(intArg, strArg);
    assertEquals(Integer.signum(expected), Integer.signum(actual));
  }
}
