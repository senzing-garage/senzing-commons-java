# System Stubs, ExecutionMode, and ResourceLock — the project pattern

The project uses
[`uk.org.webcompere:system-stubs-jupiter`](https://github.com/webcompere/system-stubs)
(test scope, declared in `pom.xml`) to:

- Stub environment variables that production code reads via
  `System.getenv(...)`, since `getenv()` is unmodifiable on JDK 17+.
- Capture `System.out` / `System.err` for tests that exercise
  expected-error paths so the captured output does not pollute the
  build log.

There is a specific pattern that **every** test using these stubs must
follow. Deviating from it causes either flaky inter-class races or
output leakage in the build log.

## The pattern

```java
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.stream.SystemErr;
import uk.org.webcompere.systemstubs.stream.SystemOut;

@Test
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
public void someTest() throws Exception
{
  // Programmatic at method level — never via @ExtendWith.
  new EnvironmentVariables("MY_VAR", "value").execute(() -> {
    // code under test runs with the env var set
  });

  SystemErr stub = new SystemErr();
  stub.execute(() -> {
    // code under test; stderr is captured during this lambda
  });
  String captured = stub.getText();
}
```

Three rules in this pattern, all required:

### 1. Programmatic, at method level — not annotation-driven

Use `new EnvironmentVariables(...).execute(...)` and
`new SystemOut().execute(...)` / `new SystemErr().execute(...)`
**inside** the test method. **Never** the
`@ExtendWith(SystemStubsExtension.class)` + `@SystemStub` annotation
form.

The annotation form applies the stub class-wide, interacts poorly
with parallel test execution, and makes it harder to scope the stub
to just the lines that need it. Method-level execute-with-callable
keeps the scope explicit and test-local.

### 2. `@Execution(ExecutionMode.SAME_THREAD)` on the test (or class)

`System.setOut` / `System.setErr` are JVM-wide. If two test methods
in the same class redirect concurrently, their captured output races.
`SAME_THREAD` keeps the test from running concurrently with siblings
in the same class.

### 3. `@ResourceLock` for cross-class mutual exclusion

Add `@ResourceLock(Resources.SYSTEM_OUT)` and/or
`@ResourceLock(Resources.SYSTEM_ERR)` on the test (or class) to
serialize against other classes that also redirect those streams.

`SAME_THREAD` only orders execution **within** a single test class.
Two tests in **different** classes that each redirect stderr can
still run concurrently and clobber each other's capture. JUnit's
`@ResourceLock(Resources.SYSTEM_OUT)` and
`@ResourceLock(Resources.SYSTEM_ERR)` provide the cross-class
mutex.

Use the lock matching what the test redirects:

- `SystemOut.execute(...)` → `@ResourceLock(Resources.SYSTEM_OUT)`
- `SystemErr.execute(...)` → `@ResourceLock(Resources.SYSTEM_ERR)`
- Both → both locks (see ordering rule below)

## Lock ordering convention — avoid deadlocks

When a test holds **both** `SYSTEM_OUT` and `SYSTEM_ERR` locks, declare
them in this canonical order:

```java
@ResourceLock(Resources.SYSTEM_OUT)
@ResourceLock(Resources.SYSTEM_ERR)
```

JUnit acquires resource locks in declaration order. If test A declares
`OUT` then `ERR` and test B declares `ERR` then `OUT`, two parallel
runs can deadlock: A holds `OUT` waiting for `ERR`, B holds `ERR`
waiting for `OUT`. The project convention is **always
`SYSTEM_OUT` first, `SYSTEM_ERR` second**, which makes deadlock
impossible.

## Class-level vs. method-level locking

- **Class-level locks** make sense when **most** tests in the class
  redirect (e.g. `LoggingUtilitiesTest`, `SQLUtilitiesTest`,
  `SzInstallLocationsTest`). Annotate the class with
  `@Execution(SAME_THREAD)` + the relevant `@ResourceLock(...)`
  annotations.
- **Method-level locks** are right when only one or two tests in the
  class redirect (e.g. `RecordReaderExtraTest.mainPrintsRecords...`,
  `CommandLineUtilitiesExtraTest.mainDoesNotThrow`). Annotate the
  individual methods with `@Execution(SAME_THREAD)` and
  `@ResourceLock(...)`. Don't class-lock; non-redirecting tests in
  the class would needlessly serialize against unrelated tests in
  other classes.

## Gotchas

### Wrap the **right** stream

`LoggingUtilities.logDebug(...)` writes to **`System.out`** (despite
the word "log" suggesting stderr). Tests that exercise debug-logging
paths must wrap with `SystemOut`, not `SystemErr`. The
`TemporaryDataCacheExtraTest.attachStreamDebugLoggingBranchEnabled`
test originally used `SystemErr` and the debug output leaked to the
build log because of this.

In general, when a test fails to capture output, double-check
whether the production code writes to `out` vs `err`:

- `LoggingUtilities.logInfo` / `logDebug` → `System.out`
- `LoggingUtilities.logError` / `logWarning` → `System.err`
- `printStackTrace()` (no args) → `System.err`
- `Throwable.printStackTrace(System.out)` → `System.out`

### Background-thread output: wrap the construction call too

If the production code starts a background thread inside its
constructor (e.g. `TemporaryDataCache` starts a `ConsumerThread` that
consumes the source `InputStream`), that thread can throw and the
JVM's default uncaught-exception handler prints its stack trace to
`System.err` — **outside** the test's redirect window if the
constructor ran before `stub.execute(...)`.

The fix is to move the construction call **inside** the
`stub.execute(...)` lambda so the redirect is active before the
background thread starts:

```java
// WRONG — consumer thread starts and may throw before stub redirects:
TemporaryDataCache tdc = new TemporaryDataCache(failingSource);
new SystemErr().execute(() -> {
  tdc.waitUntilAppendingComplete();
  ...
});

// RIGHT — construction is inside the redirect window:
new SystemErr().execute(() -> {
  TemporaryDataCache tdc = new TemporaryDataCache(failingSource);
  try {
    tdc.waitUntilAppendingComplete();
    ...
  } finally {
    tdc.delete();
  }
});
```

### JVM warnings about byte-buddy

`system-stubs-jupiter` transitively pulls in `byte-buddy-agent` so it
can dynamically attach as a Java agent and modify JDK-internal fields
(needed for `EnvironmentVariables` to mutate
`ProcessEnvironment.theEnvironment`). On JDK 21+ that triggers a
"Java agent loaded dynamically" warning, plus
"Sharing is only supported for boot loader classes" on some JDKs.

The `pom.xml` surefire plugin is configured with:

```xml
<argLine>@{argLine} -XX:+EnableDynamicAgentLoading -Xshare:off</argLine>
```

which silences both. The `@{argLine}` late-bound reference (paired
with an empty `<argLine>` property declared at the top of `pom.xml`)
lets the `jacoco` profile's `prepare-agent` still supply its
JaCoCo agent flags via the `argLine` property when running under
`-Pjacoco`. Surefire then evaluates `@{argLine}` at fork time,
producing a final argument list with the JaCoCo flags followed by
`-XX:+EnableDynamicAgentLoading -Xshare:off`.

## Tests that don't use system-stubs

A small number of older tests redirect `System.out` / `System.err`
with raw `System.setOut(new PrintStream(baos))` plus a try/finally
restore (e.g.
`ZipUtilitiesTest.mainWithMultipleTextArgsIteratesAllArguments`).
That style predates system-stubs, is fine to leave alone for
fully-synchronous redirects, and avoids the byte-buddy dependency
on a per-test basis. **All other rules above still apply** —
`@Execution(SAME_THREAD)` and the appropriate `@ResourceLock`
annotations are mandatory because they protect against concurrent
JVM-wide stream redirects regardless of which redirect mechanism is
used.

When **adding** a new test, prefer system-stubs over raw
`setOut`/`setErr` for consistency with the rest of the codebase.
Raw redirection is grandfathered, not recommended.

If a test only needs to verify a method **doesn't throw**, capture
is unnecessary — but wrapping with `SystemOut` / `SystemErr` is
still cheap and keeps the build log clean.

## Examples in this codebase

- `CommandLineUtilitiesEnvTest` — `EnvironmentVariables.execute`,
  class-level `@Execution(SAME_THREAD)`.
- `CommandLineUtilitiesFallbackTest` — env-var fallback resolution.
- `LoggingUtilitiesTest`, `SQLUtilitiesTest` — class-level
  `@ResourceLock(SYSTEM_OUT)` + `@ResourceLock(SYSTEM_ERR)`.
- `SzInstallLocationsTest` — class-level `@ResourceLock(SYSTEM_ERR)`,
  per-method `new SystemErr().execute(...)` for the four
  exception-path tests that print stack traces.
- `TemporaryDataCacheExtraTest.attachStreamDebugLoggingBranchEnabled`
  — `SystemOut` (debug output goes to stdout).
- `TemporaryDataCacheExtraTest.failureInSourceStreamSurfacesToReader`
  — illustrates the background-thread gotcha; the cache constructor
  is inside the `SystemErr.execute(...)` lambda.
- `WorkerThreadPoolExtraTest.mainExecutesArgumentsAndPrintsOutput`
  — both `SystemOut` and `SystemErr`, with locks declared in
  canonical order.
