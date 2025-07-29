package com.senzing.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides utilities for Java reflection.
 */
public class ReflectionUtilities {
  /**
   * Private default constructor.
   */
  private ReflectionUtilities() {
    // do nothing
  }

  /**
   * An <b>unmodifiable</b> {@link Map} of primitive types to their
   * corresponding promoted object types.
   */
  private static final Map<Class, Class> PROMOTED_TYPE_MAP;

  /**
   * An <b>unmodifiable</b> {@link Map} of promoted object types to their
   * corresponding primitive types.
   */
  private static final Map<Class, Class> PRIMITIVE_TYPE_MAP;

  static {
    Map<Class, Class> primitiveMap = new LinkedHashMap<>();
    Map<Class, Class> promotedMap = new LinkedHashMap<>();
    try {
      promotedMap.put(void.class, Void.class);
      promotedMap.put(boolean.class, Boolean.class);
      promotedMap.put(char.class, Character.class);
      promotedMap.put(byte.class, Byte.class);
      promotedMap.put(short.class, Short.class);
      promotedMap.put(int.class, Integer.class);
      promotedMap.put(long.class, Long.class);
      promotedMap.put(float.class, Float.class);
      promotedMap.put(double.class, Double.class);

      promotedMap.forEach((primitive, promoted) -> {
        primitiveMap.put(promoted, primitive);
      });

      promotedMap.keySet().forEach((primitive) -> {
        primitiveMap.put(primitive, primitive);
      });

      primitiveMap.keySet().forEach(keyType -> {
        if (!keyType.isPrimitive()) {
          promotedMap.put(keyType, keyType);
        }
      });

    } finally {
      PROMOTED_TYPE_MAP   = Collections.unmodifiableMap(promotedMap);
      PRIMITIVE_TYPE_MAP  = Collections.unmodifiableMap(primitiveMap);
    }
  }

  /**
   * Provides a synchronized handler that synchronizes on the proxy for
   * every method that is invoked.
   */
  private static class SynchronizedHandler implements InvocationHandler {
    /**
     * The target object for invoking the method.
     */
    private Object target;

    /**
     * The object to synchronize on.
     */
    private Object monitor = null;

    /**
     * Constructs with the specified target object on which to invoke the
     * methods.  The constructed instance will synchronize on the proxy object
     * that is passed to {@link #invoke(Object, Method, Object[]))}.
     *
     * @param target The target object to invoke the methods on.
     */
    public SynchronizedHandler(Object target) {
      this(target, null);
    }

    /**
     * Constructs with the specified target object on which to invoke the
     * methods and the specified object to synchronize on, or <code>null</code> if
     * the constructed instance should synchronize on the proxy object that is
     * passed to {@link #invoke(Object, Method, Object[]))}.
     *
     * @param target  The target object to invoke the methods on.
     * @param monitor The object to synchronize on, or <code>null</code> if the
     *                constructed instance should synchronize on the proxy
     *                object that is passed to {@link
     *                #invoke(Object, Method, Object[]))}.
     */
    public SynchronizedHandler(Object target, Object monitor) {
      this.target = target;
      this.monitor = monitor;
    }

    /**
     * Overridden to synchronize on the specified proxy object before calling
     * the specified {@link Method} on the underlying target object with the
     * specified arguments.
     *
     * @param proxy  The proxy object to synchronize on.
     * @param method The {@link Method} to invoke on the underlying target
     *               object.
     * @param args   The arguments to invoke the method with.
     * @return The result from invoking the method.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
      final Object monitor = (this.monitor == null) ? proxy : this.monitor;
      synchronized (monitor) {
        try {
          return method.invoke(this.target, args);

        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Gets the primitive type associated with the specified promoted object
   * type for that primitive type.  If a type is specified that does not
   * correspond to a primitive type then <code>null</code> is returned.  If
   * the specified type is itself {@linkplain Class#isPrimitive() primitive}
   * then the specified type is returned.
   *
   * @param promotedType The promoted type for which the primitive type is
   *                     being requested.
   *
   * @return The primitive type corresponding to the specified promoted type,
   *         or <code>null</code> if the specified type does not have a
   *         corresponding primitive type.
   */
  public static Class getPrimitiveType(Class promotedType) {
    return PRIMITIVE_TYPE_MAP.get(promotedType);
  }

  /**
   * Gets the promoted object type associated with the specified primitive type.
   * If a type is specified that is not primitive then <code>null</code> is
   * returned.
   *
   * @param primitiveType The primitive type for which the promoted object type
   *                      is being requested.
   *
   * @return The primitive type corresponding to the specified promoted type,
   *         or <code>null</code> if the specified type does not have a
   *         corresponding primitive type.
   */
  public static Class getPromotedType(Class primitiveType) {
    return PROMOTED_TYPE_MAP.get(primitiveType);
  }

  /**
   * Converts a {@link Number} value to the target number type that must have
   * a corresponding primitive {@link Number} type (e.g.: {@link
   * java.math.BigInteger} and {@link java.math.BigDecimal} are not allowed
   * here).  This helps with converting between numeric types when trying to
   * invoke methods by reflection.  If the specified value is <code>null</code>
   * then <code>null</code> is returned.
   *
   * @param value The value to be converted.
   * @param numType The {@link Class} indicating the type of primitive number
   *                to convert to which must be associated with a primitive
   *                numeric type.
   * @return The converted value or <code>null</code> if the specified value
   *         was <code>null</code>.
   * @throws NullPointerException If the specified numeric type is
   *                              <code>null</code>.
   * @throws IllegalArgumentException If an illegal numeric type is specified.
   */
  public static Number convertPrimitiveNumber(Number value, Class numType)
  {
    // verify the target number type is valid
    Objects.requireNonNull(numType, "Number type cannot be null");

    if (numType.isPrimitive()) numType = getPromotedType(numType);

    if (!Number.class.isAssignableFrom(numType)
        || (getPrimitiveType(numType) == null))
    {
      throw new IllegalArgumentException(
          "The specified target number type must extend java.lang.Number and "
              + "have a corresponding primitive numeric type: "
              +  numType.getName());
    }

    // if a null value was specified then return null
    if (value == null) return null;

    numType = getPromotedType(numType);

    // switch on the target type name
    switch (numType.getName()) {
      case "java.lang.Byte":
        return value.byteValue();
      case "java.lang.Short":
        return value.shortValue();
      case "java.lang.Integer":
        return value.intValue();
      case "java.lang.Long":
        return value.longValue();
      case "java.lang.Float":
        return value.floatValue();
      case "java.lang.Double":
        return value.doubleValue();
      default:
        throw new IllegalStateException(
            "Unable to convert to target number type: " + numType);
    }
  }

  /**
   * Creates a synchronized proxy wrapper for the specified {@link Object}
   * using the specified proxy interface class.  The returned instance will
   * synchronize on the returned proxy object before invoking any method
   * on the specified target object.
   *
   * @param proxyInterface The interface that the returned synchronized proxy
   *                       will implement.
   * @param targetObject The target object to wrap with the proxy.
   *
   * @return The synchronized proxy wrapper.
   * @param <I> The interface for the return value.
   * @param <T> The type of the target object that implements the
   *            specified interface &lt;I&gt;
   */
  public static <I,T extends I> I synchronizedProxy(Class<I>  proxyInterface,
                                                    T         targetObject)
  {
    return synchronizedProxy(proxyInterface, targetObject, null);
  }

  /**
   * Creates a synchronized proxy wrapper for the specified {@link Object}
   * using the specified proxy interface class and specified monitor object to
   * synchronize on.  If the specified monitor object is <code>null</code> then
   * the returned instance will synchronize on the returned proxy object before
   * invoking any method on the specified target object.
   *
   * @param proxyInterface The interface that the returned synchronized proxy
   *                       will implement.
   * @param targetObject The target object to wrap with the proxy.
   * @param monitor The monitor object to synchronize on.
   *
   * @return The synchronized proxy wrapper.
   * @param <I> The interface for the return value.
   * @param <T> The type of the target object that implements the
   *            specified interface &lt;I&gt;
   */
  @SuppressWarnings("unchecked")
  public static <I,T extends I> I synchronizedProxy(Class<I>  proxyInterface,
                                                    T         targetObject,
                                                    Object    monitor)
  {
    // check the parameters
    Objects.requireNonNull(
        proxyInterface, "The proxy interface cannot be null.");
    Objects.requireNonNull(
        targetObject, "The specified target object cannot be null.");
    if (!proxyInterface.isInterface()) {
      throw new IllegalArgumentException(
          "The specified proxy class is not an interface: "
              + proxyInterface.getName());
    }
    if (!proxyInterface.isAssignableFrom(targetObject.getClass())) {
      throw new IllegalArgumentException(
          "The specified target object does not implement the specified proxy "
          + "interface.  proxyInterface=[ " + proxyInterface.getName() + " ], "
          + "targetObjectClass=[ " + targetObject.getClass().getName() + " ]");
    }
    ClassLoader         classLoader = targetObject.getClass().getClassLoader();
    Class[]             interfaces  = { proxyInterface };
    SynchronizedHandler handler     = new SynchronizedHandler(targetObject,
                                                              monitor);

    return (I) Proxy.newProxyInstance(classLoader, interfaces, handler);
  }
}
