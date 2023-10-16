package com.senzing.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import com.senzing.util.LoggingUtilities;

import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Provides an {@link InvocationHandler} that prevents the returning the
 * backing JDBC {@link Connection} and ensures closing of the proxied
 * {@link Connection} does not actually close the backing JDBC {@link
 * Connection}, but releases it back to the {@link ConnectionPool}.
 */
class PooledConnectionHandler implements InvocationHandler {
  /**
   * Constant for converting between nanoseconds and milliseconds.
   */
  private static final long ONE_MILLION = 1000000L;

  /**
   * The backing {@link ConnectionPool}.
   */
  private ConnectionPool pool = null;

  /**
   * The backing {@link Connection}.
   */
  private final Connection backingConnection;

  /**
   * The proxied {@link Connection}.
   */
  private final Connection proxiedConnection;

  /**
   * The backing object for sub-objects that are not {@link Connection}
   * instances.
   */
  private final Object backingObject;

  /**
   * Flag indicating if we are closed.
   */
  private boolean closed = false;

  /**
   * The time this instance was created.
   */
  private long createdTimeNanos = 0L;

  /**
   * The time when the connection was closed (only matters for the parent
   * handler).
   */
  private long closedTimeNanos = -1L;

  /**
   * Thread that obtained the connection.
   */
  private Thread thread = null;

  /** 
   * Stack trace when the connection as obtained.
   */
  private StackTraceElement[] stackTrace = null;

  /**
   * Constructs with the backing {@link ConnectionPool}, backing
   * {@link Connection}.  This constructor will create a {@link Proxy}
   * {@link Connection] which can be obtained via {@link
   * #getProxiedConnection()}.
   *
   * @param pool The backing {@link ConnectionPool}.
   * @param backingConnection The backing {@link Connection}.
   */
  PooledConnectionHandler(ConnectionPool  pool,
                          Connection      backingConnection)
  {
    Class[] interfaces = { Connection.class };

    this.createdTimeNanos   = System.nanoTime();
    this.pool               = pool;
    this.backingConnection  = backingConnection;
    this.backingObject      = backingConnection;
    this.closed             = false;
    this.thread             = Thread.currentThread();
    this.stackTrace         = this.thread.getStackTrace();
    this.proxiedConnection  = (Connection) Proxy.newProxyInstance(
        this.getClass().getClassLoader(), interfaces, this);
  }

  /**
   * Internal constructor for handling proxies of other JDBC objects returned
   * from the {@link Connection} that might provide a way to return the
   * backing {@link Connection}.
   *
   * @param parentHandler The {@link PooledConnectionHandler} that is the parent
   *                      for this one.
   * @param backingObject The backing JDBC {@link Object} for this handler.
   */
  private PooledConnectionHandler(PooledConnectionHandler parentHandler,
                                  Object                  backingObject)
  {
    Class[] interfaces = { Connection.class };

    this.createdTimeNanos   = System.nanoTime();
    this.pool               = parentHandler.pool;
    this.backingConnection  = parentHandler.backingConnection;
    this.proxiedConnection  = parentHandler.proxiedConnection;
    this.closed             = false;
    this.backingObject      = backingObject;
    this.thread             = Thread.currentThread();
    this.stackTrace         = this.thread.getStackTrace();
  }

  /**
   * Gets the backing {@link ConnectionPool} for this instance.
   *
   * @return The backing {@link ConnectionPool} for this instance.
   */
  ConnectionPool getPool() {
    return this.pool;
  }

  /**
   * Returns the proxied {@link Connection} that was created when this
   * instance was constructed.
   *
   * @return The proxied {@link Connection} that was created when this
   *         instance was constructed.
   */
  Connection getProxiedConnection() {
    return this.proxiedConnection;
  }

  /**
   * Checks if the handler instance has been marked closed.
   *
   * @return <code>true</code> if closed, otherwise <code>false</code>.
   */
  synchronized boolean isClosed() {
    return this.closed;
  }

  /**
   * Sets this instance as closed.
   */
  synchronized void markClosed() {
    if (this.closed) return;
    this.closed           = true;
    this.closedTimeNanos  = System.nanoTime();
  }

  /**
   * Returns the amount of time in milliseconds that the backing {@link
   * Connection} has been checked if this is the handler for that {@link
   * Connection}.  This returns <code>null</code> if this handler is not the
   * handler for the {@link Connection}.  If this is the handler for the
   * {@link Connection}, but it is not yet closed, it returns the time so far.
   *
   * @return The {@link Connection} lease time if known, otherwise
   *         <code>null</code>.
   */
  synchronized Long getLeaseTime() {
    // check if closing has been recorded
    if (this.closedTimeNanos > 0L) {
      return (this.closedTimeNanos - this.createdTimeNanos) / ONE_MILLION;
    }

    // check if this is the handler for the connection
    if (this.backingConnection == this.backingObject) {
      return (System.nanoTime() - this.createdTimeNanos) / ONE_MILLION;
    }

    // if we get here then return null
    return null;
  }

  /**
   * Overridden to invoke the method being called with special handling to
   * ensure the backing JDBC {@link Connection} is not returned, but rather
   * the proxy of it is returned.  When the proxied {@link Connection} is
   * closed then the backing {@link Connection} is returned to the backing
   * {@link ConnectionPool}.
   *
   * @param proxy The proxy object.
   * @param method The {@link Method} being invoked.
   * @param args The arguments to the method invocation.
   * @return The result from invoking the method.
   * @throws Throwable If a failure occurs.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args)
      throws Throwable
  {
    // initialize the result
    Object result = null;

    // check if closing the connection
    if (proxy == this.proxiedConnection) {
      // check if closed
      boolean open = (! this.isClosed());

      String methodName = method.getName();
      switch (methodName) {
        case "close":
          // check if already closed
          if (!open) return null;

          // release the connection back to the pool -- noop if already released
          this.pool.release(this.proxiedConnection);

          // update the state
          this.markClosed();

          break;
        case "isClosed":
          // return whether (or not) the connection is closed
          synchronized (this) {
            return (this.closed);
          }

        default:
          // check if the connection is already closed
          if (open) {
            try {
              // if not closed then delegate to the backing object
              result = method.invoke(this.backingObject, args);
            } catch (InvocationTargetException e) {
              throw e.getTargetException();
            }
          } else {
            // if closed then throw an exception
            throw new SQLException("Connection already closed.");
          }
      }
    } else {
      // just call the method on the backing object
      try {
        result = method.invoke(this.backingObject, args);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    }

    // check the result
    if (result == this.backingConnection) {
      return this.proxiedConnection;
    }

    // check if the return type is an interface
    Class returnType = method.getReturnType();
    if (returnType.isInterface()
        && "java.sql".equals(returnType.getPackageName()))
    {
      // create a sub-handler to handle the proxying the result
      InvocationHandler subHandler
          = new PooledConnectionHandler(this, result);

      // create the array of interfaces to proxy
      Class[] interfaces = { method.getReturnType() };

      // proxy the result if it is an interface
      return Proxy.newProxyInstance(
          this.getClass().getClassLoader(), interfaces, subHandler);
    }

    // if we get here then just return the result
    return result;
  }

  /**
   * Gets diagnostic info for the 
   */
  public String getDiagnosticInfo() {
    long duration = (System.nanoTime() - this.createdTimeNanos) / ONE_MILLION;
    StringWriter  sw = new StringWriter();
    PrintWriter   pw = new PrintWriter(sw);

    pw.println("-----------------------------------------------------------");
    pw.println("LEASE DURATION: " + duration + "ms");
    pw.println();
    pw.println("LEASE OBTAINED AT: ");
    pw.println(LoggingUtilities.formatStackTrace(this.stackTrace));
    pw.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -");
    pw.println("LEASE-HOLDER CURRENT STACK TRACE: ");
    pw.println(LoggingUtilities.formatStackTrace(this.thread.getStackTrace()));
    return sw.toString();
  }
}
