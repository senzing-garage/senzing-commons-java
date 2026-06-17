# Connection Pool Lifecycle

`com.senzing.sql.ConnectionPool` provides a JDBC connection pool with
dynamic sizing and proxy-based lifecycle tracking. The pool itself is
not the public-facing acquire/release API — that's wrapped by a proxy
returned to the caller.

## The acquire/release flow

1. **Acquire**: `pool.acquire()` returns a `java.sql.Connection` that
   is actually a `java.lang.reflect.Proxy` over the real connection.
   The proxy is wired via `PooledConnectionHandler` to intercept
   `close()` calls so that closing redirects to `release()` instead
   of really closing the underlying connection.
2. **Use**: the caller treats the returned connection like any other
   JDBC connection. Any uncommitted transaction at the time of
   release is rolled back automatically — callers should not rely on
   the pool committing for them.
3. **Release**: `pool.release(conn)` (or the proxy's `close()`) hands
   the underlying connection back to the pool. If the connection has
   not yet hit its `retire-limit` use count and is not past its
   expire-time, it returns to the pool for reuse; otherwise it is
   really closed and a fresh one acquired on the next `acquire()`.

## Sizing and expiration

- `minSize` / `maxSize` bound the pool. The background
  `ConnectionExpireThread` watches for connections idle past the
  configured expire time and retires them, never letting the pool
  drop below `minSize`.
- Statistics are surfaced via the `Quantified` interface (acquire
  count, release count, current-in-use, etc.) — useful for
  observability and capacity tests.

## Transaction isolation

`TransactionIsolation` enforces the isolation level on every
acquired connection. Pool callers should not change isolation
mid-use; if they do, the pool resets to the configured level on
release.

## Connector implementations

`PostgreSqlConnector` and `SQLiteConnector` implement the
`Connector` interface. Adding a new database means a new
`Connector`, not changes to `ConnectionPool` itself.
