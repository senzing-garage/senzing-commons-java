package com.senzing.naming;

import com.senzing.util.AccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NoPermissionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Registry}.
 *
 * <p>Each test asserts the documented contract from {@link Registry}'s
 * javadoc — exception conditions on every {@code @throws} clause, the
 * return-value rules in each {@code @return}, and the cross-method invariants
 * such as the {@link AccessToken} authorization on
 * {@link Registry#unbind(String, AccessToken)}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class RegistryTest
{
  // -------------------------------------------------------------------
  // Constructor / isAllowingNull contract
  // -------------------------------------------------------------------

  /**
   * The default {@code Registry()} constructor must produce an instance that
   * disallows {@code null} bindings, per the constructor's javadoc.
   */
  @Test
  public void defaultConstructorDisallowsNullBindings()
  {
    Registry<Object> registry = new Registry<>();
    assertFalse(registry.isAllowingNull(),
                "Default constructor must disallow null bindings");
  }

  /**
   * {@code new Registry<>(false)} must produce an instance that
   * disallows {@code null} bindings.
   */
  @Test
  public void allowNullFalseDisallowsNullBindings()
  {
    Registry<Object> registry = new Registry<>(false);
    assertFalse(registry.isAllowingNull());
  }

  /**
   * {@code new Registry<>(true)} must produce an instance that allows
   * {@code null} bindings.
   */
  @Test
  public void allowNullTrueAllowsNullBindings()
  {
    Registry<Object> registry = new Registry<>(true);
    assertTrue(registry.isAllowingNull());
  }

  // -------------------------------------------------------------------
  // bind() contract
  // -------------------------------------------------------------------

  /**
   * {@code bind} must return a non-null {@link AccessToken} per its
   * {@code @return} javadoc.
   */
  @Test
  public void bindReturnsNonNullAccessToken() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken token = registry.bind("foo", "value");
    assertNotNull(token, "bind must return a non-null AccessToken");
  }

  /**
   * {@code bind(null, value)} must throw {@link NullPointerException}
   * per the {@code @throws NullPointerException} clause.
   */
  @Test
  public void bindThrowsNpeForNullName()
  {
    Registry<String> registry = new Registry<>();
    assertThrows(NullPointerException.class,
                 () -> registry.bind(null, "value"));
  }

  /**
   * {@code bind(name, null)} on a registry that does not allow null
   * bindings must throw {@link NullPointerException} per the javadoc.
   */
  @Test
  public void bindThrowsNpeForNullObjectWhenNullsDisallowed()
  {
    Registry<String> registry = new Registry<>(false);
    assertThrows(NullPointerException.class,
                 () -> registry.bind("foo", null));
  }

  /**
   * {@code bind(name, null)} on a null-allowing registry must succeed
   * and a subsequent {@code lookup} must return {@code null}, per the
   * registry's null-binding contract.
   */
  @Test
  public void bindAllowsNullObjectWhenNullsAllowed() throws Exception
  {
    Registry<String> registry = new Registry<>(true);
    AccessToken token = registry.bind("foo", null);
    assertNotNull(token, "bind must return a token even for null value");
    assertNull(registry.lookup("foo"),
               "lookup must return null for null-bound name");
  }

  /**
   * Re-binding the same name without first unbinding must throw
   * {@link NameAlreadyBoundException} per the javadoc.
   */
  @Test
  public void bindThrowsNameAlreadyBoundForDuplicateName() throws Exception
  {
    Registry<String> registry = new Registry<>();
    registry.bind("foo", "first");
    assertThrows(NameAlreadyBoundException.class,
                 () -> registry.bind("foo", "second"));
  }

  /**
   * Each successful {@code bind} call must yield a distinct
   * {@link AccessToken} so different names are independently
   * authorized for {@code unbind}.
   */
  @Test
  public void bindReturnsDistinctTokensPerBinding() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken t1 = registry.bind("a", "1");
    AccessToken t2 = registry.bind("b", "2");
    assertNotSame(t1, t2,
                  "Each binding must yield a distinct AccessToken");
  }

  // -------------------------------------------------------------------
  // lookup() contract
  // -------------------------------------------------------------------

  /**
   * {@code lookup} of a bound name must return the bound object.
   */
  @Test
  public void lookupReturnsBoundObject() throws Exception
  {
    Registry<String> registry = new Registry<>();
    registry.bind("foo", "value");
    assertEquals("value", registry.lookup("foo"));
  }

  /**
   * {@code lookup} of an unbound name must throw
   * {@link NameNotFoundException} per the javadoc.
   */
  @Test
  public void lookupThrowsNameNotFoundForUnboundName()
  {
    Registry<String> registry = new Registry<>();
    assertThrows(NameNotFoundException.class,
                 () -> registry.lookup("missing"));
  }

  /**
   * {@code lookup(null)} must throw {@link NullPointerException} per
   * the javadoc {@code @throws NullPointerException If the specified name is
   * null.}
   */
  @Test
  public void lookupThrowsNpeForNullName()
  {
    Registry<String> registry = new Registry<>();
    assertThrows(NullPointerException.class,
                 () -> registry.lookup(null));
  }

  /**
   * For a null-allowing registry, looking up a name that was bound to
   * {@code null} must return {@code null} (not throw).
   */
  @Test
  public void lookupReturnsNullForNullBindingWhenAllowed() throws Exception
  {
    Registry<String> registry = new Registry<>(true);
    registry.bind("foo", null);
    assertNull(registry.lookup("foo"));
  }

  // -------------------------------------------------------------------
  // isBound() contract
  // -------------------------------------------------------------------

  /**
   * {@code isBound} must return {@code true} for a name that has been
   * bound.
   */
  @Test
  public void isBoundReturnsTrueForBoundName() throws Exception
  {
    Registry<String> registry = new Registry<>();
    registry.bind("foo", "value");
    assertTrue(registry.isBound("foo"));
  }

  /**
   * {@code isBound} must return {@code false} for a name that has
   * never been bound.
   */
  @Test
  public void isBoundReturnsFalseForUnboundName()
  {
    Registry<String> registry = new Registry<>();
    assertFalse(registry.isBound("missing"));
  }

  /**
   * {@code isBound(null)} must throw {@link NullPointerException}
   * per the javadoc.
   */
  @Test
  public void isBoundThrowsNpeForNullName()
  {
    Registry<String> registry = new Registry<>();
    assertThrows(NullPointerException.class,
                 () -> registry.isBound(null));
  }

  /**
   * {@code isBound} must return {@code true} for a name bound to a
   * {@code null} value (in a null-allowing registry) — null bindings
   * are still bindings.
   */
  @Test
  public void isBoundReturnsTrueForNullBindingWhenAllowed() throws Exception
  {
    Registry<String> registry = new Registry<>(true);
    registry.bind("foo", null);
    assertTrue(registry.isBound("foo"),
               "Null-valued bindings must still report isBound=true");
  }

  // -------------------------------------------------------------------
  // unbind() contract
  // -------------------------------------------------------------------

  /**
   * {@code unbind} with the matching {@link AccessToken} must remove
   * the binding such that subsequent {@code isBound} returns false.
   */
  @Test
  public void unbindRemovesBinding() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken token = registry.bind("foo", "value");
    registry.unbind("foo", token);
    assertFalse(registry.isBound("foo"),
                "After unbind, isBound must be false");
  }

  /**
   * {@code unbind(null, token)} must throw {@link NullPointerException}
   * per the javadoc.
   */
  @Test
  public void unbindThrowsNpeForNullName() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken token = registry.bind("foo", "value");
    assertThrows(NullPointerException.class,
                 () -> registry.unbind(null, token));
  }

  /**
   * {@code unbind(name, null)} must throw {@link NullPointerException}
   * per the javadoc.
   */
  @Test
  public void unbindThrowsNpeForNullToken() throws Exception
  {
    Registry<String> registry = new Registry<>();
    registry.bind("foo", "value");
    assertThrows(NullPointerException.class,
                 () -> registry.unbind("foo", null));
  }

  /**
   * {@code unbind} of an unknown name must throw
   * {@link NameNotFoundException} per the javadoc.
   */
  @Test
  public void unbindThrowsNameNotFoundForUnknownName()
  {
    Registry<String> registry = new Registry<>();
    assertThrows(NameNotFoundException.class,
                 () -> registry.unbind("missing", new AccessToken()));
  }

  /**
   * {@code unbind} with a token that does not match the one returned
   * from the original {@code bind} must throw
   * {@link NoPermissionException} per the javadoc.
   */
  @Test
  public void unbindThrowsNoPermissionForWrongToken() throws Exception
  {
    Registry<String> registry = new Registry<>();
    registry.bind("foo", "value");
    AccessToken otherToken = new AccessToken();
    assertThrows(NoPermissionException.class,
                 () -> registry.unbind("foo", otherToken));
  }

  /**
   * After a successful {@code unbind}, the same name must be re-bindable with a
   * fresh value and a fresh {@link AccessToken}.
   */
  @Test
  public void rebindAfterUnbindSucceeds() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken first = registry.bind("foo", "first");
    registry.unbind("foo", first);
    AccessToken second = registry.bind("foo", "second");
    assertNotNull(second);
    assertNotSame(first, second,
                  "A fresh bind must return a new AccessToken");
    assertEquals("second", registry.lookup("foo"));
  }

  /**
   * After unbind + rebind, the original (now-stale) token must not authorize a
   * subsequent unbind — it should throw
   * {@link NoPermissionException}.
   */
  @Test
  public void staleTokenDoesNotAuthorizeUnbindAfterRebind() throws Exception
  {
    Registry<String> registry = new Registry<>();
    AccessToken first = registry.bind("foo", "first");
    registry.unbind("foo", first);
    registry.bind("foo", "second");
    assertThrows(NoPermissionException.class,
                 () -> registry.unbind("foo", first));
  }
}
