package com.senzing.reflect;

import javax.json.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;

import static com.senzing.util.CollectionUtilities.*;
import static java.util.Collections.*;
import static com.senzing.reflect.ReflectionUtilities.*;

/**
 * Encapsulates the automatic reflection of bean property values.
 *
 * @param <T> The type for which the property reflector will obtain the
 *            properties and operate on.
 */
public class PropertyReflector<T> {
  /**
   * The {@link Map} of {@link Class} objects to their respective
   * {@link PropertyReflector} instances.
   */
  private static final Map<Class, PropertyReflector> REFLECTOR_INSTANCES
      = new HashMap<>();

  /**
   * The {@link Map} of {@link String} property names to the corresponding
   * accessor {@link Method} values for that property.
   */
  private Map<String, Method> accessors;

  /**
   * The {@link Map} of {@link String} property names to the corresponding
   * mutator {@link Method} values for that property.
   */
  private Map<String, List<Method>> mutators;

  /**
   * Gets the {@link PropertyReflector} instance for the specified {@link
   * Class}.
   *
   * @param cls The {@link Class} for which the {@link PropertyReflector} is
   *            being requested.
   *
   * @param <T> The type for the property reflector instance.
   * @return The {@link PropertyReflector} for the specified {@link Class}.
   */
  public static synchronized <T> PropertyReflector<T> getInstance(Class<T> cls)
  {
    PropertyReflector<T> result = REFLECTOR_INSTANCES.get(cls);
    if (result == null) {
      result = new PropertyReflector<>(cls);
      REFLECTOR_INSTANCES.put(cls, result);
    }
    return result;
  }

  /**
   * Constructs with the specified class and uses reflection (introspection)
   * to obtain the accessor and mutator property names and methods.
   *
   * @param cls The {@link Class} the class to introspect.
   */
  protected PropertyReflector(Class<T> cls) {
    this.accessors  = new LinkedHashMap<>();
    this.mutators   = new LinkedHashMap<>();

    Method[] methods = cls.getMethods();
    for (Method method : methods) {
      // check if the declaring class is java.lang.Object and skip if so
      if (method.getDeclaringClass() == Object.class) continue;

      int modifiers = method.getModifiers();

      // ignore static methods
      if (Modifier.isStatic(modifiers)) continue;

      // ignore non-public methods
      if (!Modifier.isPublic(modifiers)) continue;

      String name = method.getName();
      // check if we have a standard getter
      if (name.length() > 3 && name.startsWith("get")
          && Character.isUpperCase(name.charAt(3))
          && method.getParameterCount() == 0
          && method.getReturnType() != void.class)
      {
        String key = name.substring(3, 4).toLowerCase();
        if (name.length() > 4) key += name.substring(4);
        accessors.put(key, method);
        continue;
      }
      // check for a boolean getter
      if (name.length() > 2 && name.startsWith("is")
          && Character.isUpperCase(name.charAt(2))
          && method.getParameterCount() == 0
          && (method.getReturnType() == Boolean.class
              || method.getReturnType() == boolean.class))
      {
        String key = name.substring(2, 3).toLowerCase();
        if (name.length() > 3) key += name.substring(3);
        accessors.put(key, method);
        continue;
      }
      // check for a setter
      if (name.length() > 3 && name.startsWith("set")
          && Character.isUpperCase(name.charAt(3))
          && method.getParameterCount() == 1
          && method.getReturnType() == void.class)
      {
        String key = name.substring(3, 4).toLowerCase();
        if (name.length() > 4) key += name.substring(4);
        List<Method> methodList = mutators.get(key);
        if (methodList == null) {
          methodList = new LinkedList<>();
          mutators.put(key, methodList);
        }
        methodList.add(method);
        continue;
      }
    }

    // make the maps unmodifiable
    this.accessors  = unmodifiableMap(this.accessors);
    this.mutators   = recursivelyUnmodifiableMap(this.mutators);
  }

  /**
   * Provides the {@link Map} of {@link String} property names to the
   * accessor {@link Method} values for that respective property.
   *
   * @return The {@link Map} of {@link String} property names too the
   *         accessor {@link Method} values for that respective property.
   */
  public Map<String, Method> getAccessors() {
    return this.accessors;
  }

  /**
   * Provides the {@link Map} of {@link String} property names to the
   * <b>unmodifiable</b> {@link List} of mutator {@link Method} values for that
   * respective property.
   *
   * @return The {@link Map} of {@link String} property names to the
   *         <b>unmodifiable</b> {@link List} of mutator {@link Method} values
   *         for that respective property.
   */
  public Map<String, List<Method>> getMutators() {
    return this.mutators;
  }

  /**
   * Gets the property value for the specified property key from the specified
   * target object.
   *
   * @param target The target object from which to get the property value.
   * @param propertyKey The property key for the property being requested.
   *
   * @return The property value for the specified property key from the
   *         specified target object.
   *
   * @throws IllegalArgumentException If the property key is not recognized.
   *
   */
  public Object getPropertyValue(T target, String propertyKey)
    throws IllegalArgumentException
  {
    Map<String, Method> accessorMap = this.getAccessors();
    Method method = accessorMap.get(propertyKey);
    if (method == null) {
      // check if this is a known property
      if (this.getMutators().containsKey(propertyKey)) {
        // NOTE: this is an odd case, but allowed
        throw new UnsupportedOperationException(
            "The specified property is write-only: " + propertyKey);
      } else {
        throw new IllegalArgumentException(
            "Unrecognized property key: " + propertyKey);
      }
    }
    try {
      return method.invoke(target);

    } catch (InvocationTargetException|IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the property value for the specified property key on the specified
   * target object.
   *
   * @param target The target object on which to set the property value.
   * @param propertyKey The property key for the property being set.
   * @param propertyValue The property value for the property.
   *
   * @throws IllegalArgumentException If the property key is not recognized.
   */
  public void setPropertyValue(T target, String propertyKey, Object propertyValue)
      throws IllegalArgumentException
  {
    Map<String, List<Method>> mutatorMap  = this.getMutators();
    List<Method>              methods     = mutatorMap.get(propertyKey);
    boolean                   invoked     = false;

    // check if the methods were not found
    if (methods == null) {
      // check if this is a known property
      if (this.getAccessors().containsKey(propertyKey)) {
        // NOTE: this is an odd case, but allowed
        throw new UnsupportedOperationException(
            "The specified property is read-only: " + propertyKey);
      } else {
        throw new IllegalArgumentException(
            "Unrecognized property key: " + propertyKey);
      }
    }

    // if the property value is null then find the best method for setting the
    // value to null (no primitives allowed)
    if (propertyValue == null) {
      invoked = findAndInvokeMutator(
          methods, Class::isPrimitive, target, null);

      if (invoked) return;

      // if we get here then no method was found
      throw new NullPointerException(
          "The specified value for the property (" + propertyKey + ") cannot "
              + "be null.");
    }

    // get the value type
    Class valueType = propertyValue.getClass();

    // for non-null property values, look for an exact type-match first
    invoked = findAndInvokeMutator(
        methods, (argType -> argType == valueType), target, propertyValue);

    if (invoked) return;

    // now check for a corresponding primitive mutator if a promoted type
    Class primType = getPrimitiveType(valueType);

    if (primType != null && primType != valueType) {
      invoked = findAndInvokeMutator(
          methods, (argType -> argType == primType), target, propertyValue);
      if (invoked) return;
    }

    // check if we have an assignable-from method
    invoked = findAndInvokeMutator(
        methods,
        (argType -> argType.isAssignableFrom(valueType)),
        target,
        propertyValue);

    if (invoked) return;

    /*
    // COMMENT THIS OUT FOR NOW -- IT DOES NOT NECCESARILY MAKE SENSE TO CONVERT
    // FLOAT TO DOUBLE OR VICE-VERSA (OR SHORT TO INT OR LONG)
    //
    // finally, check if we have an instance of java.lang.Number or the
    // primitive equivalent
    if (primType != null) {
      // get the corresponding promoted type from the primitive type
      Class promotedType = getPromotedType(primType);

      // check if the promoted type extends java.lang.Number
      if (Number.class.isAssignableFrom(promotedType)) {
        // get the value as a Number
        Number numberValue = (Number) propertyValue;

        // iterate over the methods
        for (Method method : methods) {
          // get the argument type
          Class argType = method.getParameterTypes()[0];

          // check if the argument type is primitive and if so then promote it
          if (argType.isPrimitive()) {
            argType = getPromotedType(argType);
          }

          // test the argument type if a primitive number
          if (!Number.class.isAssignableFrom(argType)) continue;
          if (getPrimitiveType(argType) == null) continue;

          // convert the value to the primitive number type
          Object convertedValue = convertPrimitiveNumber(numberValue, argType);

          try {
            // invoke the method
            method.invoke(target, convertedValue);

            // return here since invoked
            return;

          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
    */

    // if we get here then no suitable mutator was found
    if (propertyValue == null) {
      // a null value was specified but all mutators require primitive values
      throw new NullPointerException(
          "The specified value for the property (" + propertyKey + ") cannot "
          + "be null.");
    } else {
      throw new ClassCastException(
          "The specified value for the property (" + propertyKey + ") was not "
          + "of a valid type: " + propertyValue.getClass().getName());
    }
  }

  /**
   * Finds the first {@link Method} satisfying the specified {@link Predicate}
   * and invokes it on the specified target {@link Object} with the specified
   * property value as a parameter.  This method returns <code>true</code> if
   * a method was found and it was invoked, and <code>false</code> if no method
   * was found.
   *
   * @param methods The {@link List} of {@link Method} instances to search for
   *                one whose first argument type satisfies the specified
   *                predicate.
   * @param predicate The {@link Predicate} that tests the type of the first
   *                  argument to each {@link Method}.
   * @param target The target object on which to invoke the {@link Method}.
   * @param propertyValue The property value to pass as a parameter.
   *
   * @return <code>true</code> if the {@link Method} was found and invoked, and
   *         <code>false</code> if no {@link Method} was found to satisfy the
   *         {@link Predicate}.
   */
  private static boolean findAndInvokeMutator(List<Method>      methods,
                                              Predicate<Class>  predicate,
                                              Object            target,
                                              Object            propertyValue)
  {
    for (Method method: methods) {
      // get the argument type
      Class argType = method.getParameterTypes()[0];

      // test the argument type against the predicate, skip if it fails
      if (!predicate.test(argType)) continue;

      try {
        // invoke the method
        method.invoke(target, propertyValue);

        // return true to indicate it was found and invoked
        return true;

      } catch (InvocationTargetException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    // return false to indicate no method is found
    return false;
  }

  /**
   * Uses reflection to extract the properties from the specified {@link Object}
   * and recursively construct a {@link JsonObject} from them and return that
   * {@link JsonObject}.
   *
   * @param object The non-null {@link Object} to convert to JSON.
   * @return The constructed {@link JsonObject}.
   * @throws NullPointerException If the specified parameter is
   *                              <code>null</code>.
   */
  public static JsonObject toJsonObject(Object object)
    throws NullPointerException
  {
    Objects.requireNonNull("The specified object cannot be null");
    JsonObjectBuilder job = Json.createObjectBuilder();
    return buildJsonObject(job, object).build();
  }

  /**
   * Adds the accessible properties of the specified {@link Object} to the
   * specified {@link JsonObjectBuilder} with their respective property names
   * as property keys.
   *
   * @param builder The non-null {@link JsonObjectBuilder} to which to add the
   *                properties.
   * @param object The non-null {@link Object} whose properties should be added
   *               to the {@link JsonObjectBuilder}.
   * @return The specified {@link JsonObjectBuilder}.
   * @throws NullPointerException If either of the specified parameters is
   *                              <code>null</code>.
   */
  public static JsonObjectBuilder buildJsonObject(JsonObjectBuilder builder,
                                                  Object            object)
    throws NullPointerException
  {
    Objects.requireNonNull(
        builder, "The specified JsonObjectBuilder cannot be null");
    Objects.requireNonNull(
        object, "The specified object cannot be null");

    IdentityHashMap visitedMap = new IdentityHashMap();
    return buildJsonObject(visitedMap, builder, object);
  }

  /**
   * Adds the accessible properties of the specified {@link Object} to the
   * specified {@link JsonObjectBuilder} with their respective property names
   * as property keys.
   *
   * @param visited The {@link IdentityHashMap} used to detect circular
   *                references.
   * @param builder The non-null {@link JsonObjectBuilder} to which to add the
   *                properties.
   * @param object The non-null {@link Object} whose properties should be added
   *               to the {@link JsonObjectBuilder}.
   * @return The specified {@link JsonObjectBuilder}.
   * @throws NullPointerException If either of the specified parameters is
   *                              <code>null</code>.
   */
  private static JsonObjectBuilder buildJsonObject(IdentityHashMap   visited,
                                                   JsonObjectBuilder builder,
                                                   Object            object)
      throws NullPointerException
  {
    if (visited.containsKey(object)) {
      throw new IllegalStateException(
          "Circular reference detected for object: " + object);
    }
    visited.put(object, null);
    try {
      // get an object map if all keys are strings
      Map<String, ?> objectMap = getObjectMap(object);

      // initialize the variables as null
      PropertyReflector   reflector = null;
      Map<String, Method> accessors = null;
      Set<String>         keySet    = null;

      // check if the object map is null
      if (objectMap == null) {
        // if null then initialize the property reflector variables and key set
        Class cls = object.getClass();
        reflector = PropertyReflector.getInstance(cls);
        accessors = reflector.getAccessors();
        keySet    = accessors.keySet();

      } else {
        // get the key set from the object map if we have an object map
        keySet = objectMap.keySet();
      }

      // iterate over the properties
      for (String propertyKey : keySet) {
        // get the property value
        Object propertyValue = (objectMap != null)
            ? objectMap.get(propertyKey)
            : reflector.getPropertyValue(object, propertyKey);

        // check for a null value
        if (propertyValue == null) {
          builder.addNull(propertyKey);
          continue;
        }

        Class propertyType = propertyValue.getClass();
        if (propertyType.isPrimitive()) {
          propertyType = getPromotedType(propertyType);
        }

        switch (propertyType.getName()) {
          case "javax.json.JsonObject":
            JsonObject jsonObj = (JsonObject) propertyValue;
            builder.add(propertyKey, Json.createObjectBuilder(jsonObj));
            break;

          case "javax.json.JsonArray":
            JsonArray jsonArr = (JsonArray) propertyValue;
            builder.add(propertyKey, Json.createArrayBuilder(jsonArr));
            break;

          case "java.lang.Integer":
          case "java.lang.Short":
            builder.add(propertyKey, ((Number) propertyValue).intValue());
            break;

          case "java.lang.Long":
            builder.add(propertyKey, ((Number) propertyValue).longValue());
            break;

          case "java.lang.String":
            builder.add(propertyKey, propertyValue.toString());
            break;

          case "java.lang.Double":
          case "java.lang.Float":
            builder.add(propertyKey, ((Number) propertyValue).doubleValue());
            break;

          case "java.lang.Boolean":
            builder.add(propertyKey, ((Boolean) propertyValue));
            break;

          case "java.math.BigDecimal":
            builder.add(propertyKey, ((BigDecimal) propertyValue));
            break;

          case "java.math.BigInteger":
            builder.add(propertyKey, ((BigInteger) propertyValue));
            break;

          default:
            if (Collection.class.isAssignableFrom(propertyType)) {
              // handle iterating over a collection
              JsonArrayBuilder  jab         = Json.createArrayBuilder();
              Collection        collection  = (Collection) propertyValue;
              for (Object elem: collection) {
                addToJsonArray(visited, jab, elem);
              }
              builder.add(propertyKey, jab);

            } else if (propertyType.isArray()) {
              // handle as an array
              JsonArrayBuilder  jab     = Json.createArrayBuilder();
              int               length  = Array.getLength(propertyValue);
              for (int index = 0; index < length; index++) {
                Object elem = Array.get(propertyValue, index);
                addToJsonArray(visited, jab, elem);
              }
              builder.add(propertyKey, jab);

            } else {
              JsonObjectBuilder job = Json.createObjectBuilder();
              builder.add(propertyKey,
                          buildJsonObject(visited, job, propertyValue));
            }
        }
      }

      // return the builder
      return builder;

    } finally {
      visited.remove(object);
    }
  }

  /**
   * Internal method to add values to a {@link JsonArrayBuilder} when building
   * a {@link JsonObject}.
   *
   * @param visited The {@link IdentityHashMap} used to detect circular
   *                references.
   * @param builder The {@link JsonArrayBuilder} to add the value to.
   * @param value The value to be added.
   * @return The specified {@link JsonArrayBuilder}.
   */
  private static JsonArrayBuilder addToJsonArray(IdentityHashMap  visited,
                                                 JsonArrayBuilder builder,
                                                 Object           value)
  {
    // check for null
    if (value == null) {
      builder.addNull();
      return builder;
    }

    if (visited.containsKey(value)) {
      throw new IllegalStateException(
          "Circular reference detected for object: " + value);
    }
    visited.put(value, null);
    try {
      Class valueType = value.getClass();
      if (valueType.isPrimitive()) {
        valueType = getPromotedType(valueType);
      }

      switch (valueType.getName()) {
        case "javax.json.JsonObject":
          JsonObject jsonObj = (JsonObject) value;
          builder.add(Json.createObjectBuilder(jsonObj));
          break;

        case "javax.json.JsonArray":
          JsonArray jsonArr = (JsonArray) value;
          builder.add(Json.createArrayBuilder(jsonArr));
          break;

        case "java.lang.Integer":
        case "java.lang.Short":
          builder.add(((Number) value).intValue());
          break;

        case "java.lang.Long":
          builder.add(((Number) value).longValue());
          break;

        case "java.lang.String":
          builder.add(value.toString());
          break;

        case "java.lang.Double":
        case "java.lang.Float":
          builder.add(((Number) value).doubleValue());
          break;

        case "java.lang.Boolean":
          builder.add(((Boolean) value));
          break;

        case "java.math.BigDecimal":
          builder.add(((BigDecimal) value));
          break;

        case "java.math.BigInteger":
          builder.add(((BigInteger) value));
          break;

        default:
          if (Collection.class.isAssignableFrom(valueType)) {
            // handle iterating over a collection
            JsonArrayBuilder  jab         = Json.createArrayBuilder();
            Collection        collection  = (Collection) value;
            for (Object elem: collection) {
              addToJsonArray(visited, jab, elem);
            }
            builder.add(jab);

          } else if (valueType.isArray()) {
            // handle as an array
            JsonArrayBuilder  jab     = Json.createArrayBuilder();
            int               length  = Array.getLength(value);
            for (int index = 0; index < length; index++) {
              Object elem = Array.get(value, index);
              addToJsonArray(visited, jab, elem);
            }
            builder.add(jab);

          } else {
            JsonObjectBuilder job = Json.createObjectBuilder();

            // remove the visited value before recursing
            visited.remove(value);
            try {
              builder.add(buildJsonObject(visited, job, value));
            } finally{
              visited.put(value, null);
            }
          }
      }

      // return the builder
      return builder;

    } finally {
      visited.remove(value);
    }
  }

  /**
   *
   */
  private static Map<String, ?> getObjectMap(Object object) {
    // check if we have a map
    if (!(object instanceof Map)) return null;
    Map map = (Map) object;
    for (Object key : map.keySet()) {
      if (!(key instanceof String)) {
        return null;
      }
    }
    return (Map<String, ?>) map;
  }
}
