package com.senzing.naming;

import com.senzing.util.AccessToken;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NoPermissionException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Provides a basic registry for objects of a specific type.  This class acts
 * as a simplified version of {@link javax.naming.Context} that is dedicated to
 * instances of a specific class and confined to a single process in-memory
 * lookup.  Unbinding of previously bound objects is only allowed if the caller
 * uses the {@link AccessToken} returned when the original binding was created
 * to authorize the unbinding.
 *
 * @param <T> The type of object provided by the registry instance.
 */
public class Registry<T> {
  /**
   * The {@link Map} of {@link String} keys to the bound objects of the
   * provided type.
   */
  private Map<String, T> bindingMap;

  /**
   * The {@link Map} of {@link String} keys to {@link AccessToken} instances
   * used for authorizing unbinding.
   */
  private Map<String, AccessToken> tokenMap;

  /**
   * Indicates whether <code>null</code> objects can be bound to names in this
   * registry.
   */
  private boolean allowNull;

  /**
   * Default constructor.  Constructing with this constructor creates an
   * instance that will not allow <code>null</code> references in bindings.
   */
  public Registry() {
    this(false);
  }

  /**
   * Constructs with the flag indicating if <code>null</code> references can be
   * bound in the registry.
   *
   * @param allowNull <code>true</code> if bindings in this registry may exist
   *                  to <code>null</code> references, and <code>false</code>
   *                  if bindings must be to non-null objects.
   */
  public Registry(boolean allowNull) {
    this.bindingMap = new LinkedHashMap<>();
    this.tokenMap   = new LinkedHashMap<>();
    this.allowNull  = allowNull;
  }

  /**
   * Checks whether this registry instance allows bindings to <code>null</code>
   * references.  This returns <code>true</code> if <code>null</code> bindings
   * are allowed, otherwise <code>false</code> if they are forbidden and would
   * generate a {@link NullPointerException} on an attempt to bind a
   * <code>null</code> reference.
   *
   * @return <code>true</code> if <code>null</code> bindings are allowed,
   *         otherwise <code>false</code>
   */
  public boolean isAllowingNull() {
    return this.allowNull;
  }

  /**
   * Binds the specified object to the specified name in the registry.  If
   * an object is already bound to the specified name then an {@link
   * NameAlreadyBoundException} is thrown.  If the specified name is
   * <code>null</code> then a {@link NullPointerException} is thrown.  If
   * <code>null</code> bindings {@linkplain #isAllowingNull() are not allowed}
   * by this registry and the specified object is <code>null</code> then a
   * {@link NullPointerException} is thrown.
   *
   * @param name The non-null name to which to bind the specified object should
   *             be bound.
   * @param object The object to bind to the specified name, which {@linkplain
   *               #isAllowingNull()} may be forbidden} from being
   *               <code>null</code>.
   *
   * @return The {@link AccessToken} that the caller can use to {@linkplain
   *         #unbind(String, AccessToken) unbind} the name from the object.
   *
   * @throws NullPointerException If the specified name is <code>null</code> or
   *                              if <code>null</code> bindings {@linkplain
   *                              #isAllowingNull() are not allowed} and the
   *                              specified object is <code>null</code>.
   *
   * @throws NameAlreadyBoundException If the specified name is already bound
   *                                   to an object.
   */
  public synchronized AccessToken bind(String name, T object)
      throws NameAlreadyBoundException, NullPointerException
  {
    Objects.requireNonNull(name, "The bound name cannot be null");
    if (!this.isAllowingNull()) {
      Objects.requireNonNull(object, "The bound object cannot be null");
    }
    if (this.bindingMap.containsKey(name)) {
      throw new NameAlreadyBoundException(
          "The specified name is already bound: " + name);
    }
    AccessToken token = new AccessToken();
    this.bindingMap.put(name, object);
    this.tokenMap.put(name, token);
    return token;
  }

  /**
   * Looks up the object bound to the specified name and returns a reference to
   * it.  If no object is bound to the specified name then a {@link
   * NameNotFoundException} is thrown.  This may return <code>null</code> if
   * the registry {@linkplain #isAllowingNull() allows} <code>null</code>
   * bindings.
   *
   * @param name The name for which the bound object should be returned.
   *
   * @return The object bound to the specified name.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws NameNotFoundException If the specified name is not bound to an
   *                               object.
   */
  public synchronized T lookup(String name)
      throws NullPointerException, NameNotFoundException
  {
    if (!this.bindingMap.containsKey(name)) {
      throw new NameNotFoundException(
          "The specified name is not bound: " + name);
    }
    return this.bindingMap.get(name);
  }

  /**
   * Checks if the specified name is bound to a an object.  Synchronize on this
   * {@link Registry} instance to atomically combine a call to this method with
   * another method call on this instance.
   *
   * @param name The name to check.
   *
   * @return <code>true</code> if the specified name is bound, otherwise
   *         <code>false</code>.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   */
  public synchronized boolean isBound(String name)
    throws NullPointerException
  {
    Objects.requireNonNull(name, "The specified name cannot be null");
    return this.bindingMap.containsKey(name);
  }

  /**
   * Unbinds the object that is bound to the specified name providing the
   * specified {@link AccessToken} is the one that was returned when it was
   * originally bound.
   *
   * @param name The non-null name for which to remove the binding.
   * @param token The non-null {@link AccessToken} for authorizing permission to
   *              unbind the object.
   *
   * @throws NullPointerException If either of the parameters is
   *                              <code>null</code>.
   * @throws NameNotFoundException If the specified name is not bound to any
   *                               object.
   * @throws NoPermissionException If the specified name is recognized, but
   *                               the specified {@link AccessToken} is not
   *                               the one associated with the specified name.
   */
  public synchronized void unbind(String name, AccessToken token)
      throws NullPointerException, NameNotFoundException, NoPermissionException
  {
    Objects.requireNonNull(name, "The provider name cannot be null");
    Objects.requireNonNull(token, "The access token cannot be null");
    AccessToken expected = this.tokenMap.get(name);
    if (expected == null) {
      throw new NameNotFoundException(
          "The specified provider name is not recognized: " + name);
    }
    if (token != expected) {
      throw new NoPermissionException(
          "The specified token does not match the specified name: " + name);
    }
    this.bindingMap.remove(name);
    this.tokenMap.remove(name);
  }
}
