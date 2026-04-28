package com.senzing.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Test-only fixture: builds a fake Senzing installation tree on disk
 * for use by tests of {@link SzInstallLocations} and
 * {@link SzUtilities}.
 *
 * <p>Layout produced by {@link #build()}:
 *
 * <pre>
 *   &lt;root&gt;/
 *     er/                                          (install dir)
 *     er/etc/cfgVariant.json                       (required config file)
 *     er/resources/                                (resource dir)
 *     er/resources/schema/szcore-schema-sqlite-create.sql
 *     data/                                        (support dir)
 * </pre>
 *
 * <p>Use {@link #build()} from a {@code @BeforeAll} method, then point
 * the {@code senzing.path} system property at {@link #root()} for
 * each test that needs the install discoverable. {@link #close()}
 * recursively deletes the fixture and is intended to be called from
 * {@code @AfterAll}.
 */
final class FakeSenzingInstall implements AutoCloseable
{
  private final Path root;

  /**
   * Builds a fresh fake-install fixture under a unique temp directory
   * and returns the handle. Throws {@link IOException} only if the
   * temp tree could not be created.
   */
  static FakeSenzingInstall build() throws IOException
  {
    Path root = Files.createTempDirectory("senzing-test-");
    try {
      Files.createDirectories(root.resolve("er/etc"));
      Files.writeString(root.resolve("er/etc/cfgVariant.json"),
                        "{ \"FAKE\": true }\n");
      Files.createDirectories(root.resolve("er/resources/schema"));
      // Tests that exercise SzUtilities.ensureSenzingSQLiteSchema
      // need at least one parseable CREATE TABLE statement so the
      // already-installed branch can be exercised on a re-run.
      // Use a distinctive name so it never collides with anything
      // real. Note: the schema parser in SzUtilities executes each
      // non-empty line verbatim, so we keep the contents to a
      // single CREATE TABLE — comment-only lines would fail the
      // SQLite JDBC execute() call.
      Files.writeString(
          root.resolve("er/resources/schema/"
                       + "szcore-schema-sqlite-create.sql"),
          "CREATE TABLE FAKE_SZ_FIXTURE_TABLE"
              + " (id INTEGER PRIMARY KEY);\n");
      Files.createDirectories(root.resolve("data"));
      return new FakeSenzingInstall(root);
    } catch (IOException e) {
      // best-effort cleanup if mid-construction
      deleteRecursively(root);
      throw e;
    }
  }

  private FakeSenzingInstall(Path root)
  {
    this.root = root;
  }

  /** Root directory of the fake install. */
  Path root()
  {
    return this.root;
  }

  /** {@code <root>/er} — Senzing ER install directory. */
  Path installDir()
  {
    return this.root.resolve("er");
  }

  /** {@code <root>/data} — Senzing support directory. */
  Path supportDir()
  {
    return this.root.resolve("data");
  }

  /** {@code <root>/er/etc} — Senzing config directory. */
  Path configDir()
  {
    return this.root.resolve("er/etc");
  }

  /** {@code <root>/er/resources} — Senzing resource directory. */
  Path resourceDir()
  {
    return this.root.resolve("er/resources");
  }

  @Override
  public void close() throws IOException
  {
    deleteRecursively(this.root);
  }

  private static void deleteRecursively(Path dir) throws IOException
  {
    if (dir == null || !Files.exists(dir)) return;
    try (var stream = Files.walk(dir)) {
      stream.sorted(Comparator.reverseOrder())
            .forEach(p -> {
              try {
                Files.delete(p);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }
}
