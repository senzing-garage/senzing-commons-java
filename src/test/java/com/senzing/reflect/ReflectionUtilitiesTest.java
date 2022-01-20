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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ReflectionUtilitiesTest {
  protected interface TestInterface {
    void test();
  }

  protected class TestClass implements TestInterface {
    private Thread currentThread = null;
    private boolean failed = false;
    private final Object monitor = new Object();

    public boolean isFailed() {
      synchronized (this.monitor) {
        return this.failed;
      }
    }

    public void test() {
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
          if (thread == this.currentThread) this.currentThread = null;
        }
      }
    }
  }

  @Test
  public void testSynchronizedProxy() {
    try {
      handleTest(true, null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testSynchronizedProxy() failed with exception: " + e);
    }
  }

  @Test
  public void testSynchronizedWithMonitorProxy() {
    try {
      final Object monitor = new Object();
      handleTest(true, monitor);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testSynchronizedWithMonitorProxy() failed with exception: " + e);
    }
  }

  @Test
  public void testUnsynchronizedAccess() {
    try {
      handleTest(false, null);

    } catch (Exception e) {
      e.printStackTrace();
      fail("testUnsynchronizedAccess() failed with exception: " + e);
    }
  }

  private void handleTest(boolean sync, Object monitor) throws Exception {
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

  public List<Arguments> getPrimitiveTypeTestParams() {
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

  public List<Arguments> getPromotedTypeTestParams() {
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
  public void getPrimitiveTypeTest(Class paramType, Class expectedResult) {
    Class result = ReflectionUtilities.getPrimitiveType(paramType);
    assertEquals(expectedResult, result,
                 "Unexpected primitive type for " + paramType);
  }

  @ParameterizedTest
  @MethodSource("getPromotedTypeTestParams")
  public void getPromotedTypeTest(Class paramType, Class expectedResult) {
    Class result = ReflectionUtilities.getPromotedType(paramType);
    assertEquals(expectedResult, result,
                 "Unexpected promoted type for " + paramType);
  }


  public List<Arguments> getConvertPrimitiveParams() {
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
}
