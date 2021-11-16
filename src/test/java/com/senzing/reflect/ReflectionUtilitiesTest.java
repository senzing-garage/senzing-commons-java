package com.senzing.reflect;

import com.senzing.io.ChunkedEncodingInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static com.senzing.reflect.ReflectionUtilities.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ChunkedEncodingInputStream}.
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

}
