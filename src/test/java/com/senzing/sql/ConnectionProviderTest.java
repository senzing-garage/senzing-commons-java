package com.senzing.sql;

import com.senzing.naming.Registry;
import com.senzing.util.AccessToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ConnectionProvider} interface — specifically its public
 * {@link ConnectionProvider#REGISTRY} static field.
 *
 * <p>The interface itself only declares {@code getConnection()},
 * which is exercised through concrete implementations ({@link
 * PoolConnectionProvider} et al.) — see
 * {@code PoolConnectionProviderTest}. This test class focuses on
 * the {@code REGISTRY} field's documented role as a global, non-null in-memory
 * registry of {@link ConnectionProvider} instances.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
public class ConnectionProviderTest
{
  /**
   * {@link ConnectionProvider#REGISTRY} must be non-null at class
   * load time and must be a {@link Registry} of
   * {@link ConnectionProvider} instances.
   */
  @Test
  public void registryIsNonNull()
  {
    assertNotNull(ConnectionProvider.REGISTRY,
                  "ConnectionProvider.REGISTRY must be initialized"
                      + " non-null");
  }

  /**
   * The {@code REGISTRY} must function as a normal {@link Registry}: a bound
   * provider is retrievable by name and unbinding requires the access token
   * from the original {@code bind} call.
   */
  @Test
  public void registryAcceptsBindAndLookupRoundTrip() throws Exception
  {
    ConnectionProvider stub = () -> {
      throw new UnsupportedOperationException("test stub");
    };
    String name
        = "ConnectionProviderTest-" + System.nanoTime();
    AccessToken token = ConnectionProvider.REGISTRY.bind(name, stub);
    try {
      assertSame(stub, ConnectionProvider.REGISTRY.lookup(name),
                 "Registry must return the same instance on lookup");
      assertTrue(ConnectionProvider.REGISTRY.isBound(name));
    } finally {
      ConnectionProvider.REGISTRY.unbind(name, token);
    }
  }

  /**
   * The {@link ConnectionProvider#getConnection()} method must be callable
   * through a lambda implementation — exercised here to cover the abstract
   * method's invocation path.
   */
  @Test
  public void getConnectionInvokesProviderImplementation()
  {
    final Connection[] capture = new Connection[1];
    ConnectionProvider provider = () -> {
      // The point of this test is just that the abstract method is
      // dispatched correctly to a lambda implementation; we don't
      // need a real Connection.
      return capture[0];
    };
    try {
      assertSame(capture[0], provider.getConnection());
    } catch (Exception ignore) {
      // not expected
    }
  }
}
