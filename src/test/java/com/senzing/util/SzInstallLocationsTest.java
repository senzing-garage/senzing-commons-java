package com.senzing.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.stream.SystemErr;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SzInstallLocations}.
 *
 * <p>Each test asserts the documented contract from
 * {@link SzInstallLocations}'s javadoc: the static
 * {@link SzInstallLocations#findLocations()} method discovers the
 * install via system properties, falls back to subdirectories of the
 * {@code senzing.path} root for support/resource/config, and throws
 * {@link IllegalStateException} when any required directory is
 * missing or invalid.
 *
 * <p>Uses a {@link FakeSenzingInstall} fixture built once per class
 * to provide a hermetic install tree on disk. Tests mutate
 * process-wide system properties, hence
 * {@link Execution.SAME_THREAD execution mode} and an
 * {@link AfterEach} hook that restores the senzing.* properties to
 * their pre-test values.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(Resources.SYSTEM_ERR)
public class SzInstallLocationsTest
{
  private static final String[] PROPERTIES = {
      "senzing.path",
      "senzing.config.dir",
      "senzing.support.dir",
      "senzing.resource.dir",
  };

  private FakeSenzingInstall install;

  // -------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------

  @BeforeAll
  public void setUpFakeInstall() throws IOException
  {
    this.install = FakeSenzingInstall.build();
  }

  @AfterAll
  public void tearDownFakeInstall() throws IOException
  {
    if (this.install != null) this.install.close();
  }

  /**
   * Restore system-property state after every test. We always clear
   * the senzing.* properties; tests opt-in by setting whatever they
   * need.
   */
  @AfterEach
  public void clearProperties()
  {
    for (String key : PROPERTIES) {
      System.clearProperty(key);
    }
  }

  // -------------------------------------------------------------------
  // Happy path
  // -------------------------------------------------------------------

  /**
   * With only {@code senzing.path} set to the fake install root,
   * {@link SzInstallLocations#findLocations()} must derive all five
   * documented locations:
   * {@code installDirectory = <root>/er},
   * {@code configDirectory = <root>/er/etc},
   * {@code supportDirectory = <root>/data},
   * {@code resourceDirectory = <root>/er/resources}, and
   * {@code templatesDirectory = <root>/er/resources/templates}.
   */
  @Test
  public void findLocationsHappyPathFromSenzingPath() throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());

    SzInstallLocations result = SzInstallLocations.findLocations();
    assertNotNull(result);
    assertEquals(install.installDir().toFile().getCanonicalFile(),
                 result.getInstallDirectory().getCanonicalFile());
    assertEquals(install.configDir().toFile().getCanonicalFile(),
                 result.getConfigDirectory().getCanonicalFile());
    assertEquals(install.supportDir().toFile().getCanonicalFile(),
                 result.getSupportDirectory().getCanonicalFile());
    assertEquals(install.resourceDir().toFile().getCanonicalFile(),
                 result.getResourceDirectory().getCanonicalFile());
    assertEquals(
        install.resourceDir().resolve("templates").toFile()
                                                  .getCanonicalFile(),
        result.getTemplatesDirectory().getCanonicalFile());
  }

  // -------------------------------------------------------------------
  // Explicit overrides take precedence over derivation from senzing.path
  // -------------------------------------------------------------------

  /**
   * An explicit {@code senzing.support.dir} system property must
   * override the {@code <senzing.path>/data} default.
   */
  @Test
  public void explicitSupportDirOverridesDefault() throws Exception
  {
    Path altSupport = Files.createTempDirectory("alt-support-");
    try {
      System.setProperty("senzing.path", install.root().toString());
      System.setProperty("senzing.support.dir", altSupport.toString());

      SzInstallLocations result = SzInstallLocations.findLocations();
      assertEquals(altSupport.toFile().getCanonicalFile(),
                   result.getSupportDirectory().getCanonicalFile(),
                   "Explicit senzing.support.dir must override the"
                       + " default <senzing.path>/data");
    } finally {
      deleteRecursively(altSupport);
    }
  }

  /**
   * An explicit {@code senzing.resource.dir} system property must
   * override the {@code <install>/resources} default.
   */
  @Test
  public void explicitResourceDirOverridesDefault() throws Exception
  {
    Path altResource = Files.createTempDirectory("alt-resource-");
    try {
      System.setProperty("senzing.path", install.root().toString());
      System.setProperty("senzing.resource.dir", altResource.toString());

      SzInstallLocations result = SzInstallLocations.findLocations();
      assertEquals(altResource.toFile().getCanonicalFile(),
                   result.getResourceDirectory().getCanonicalFile(),
                   "Explicit senzing.resource.dir must override the"
                       + " default <install>/resources");
    } finally {
      deleteRecursively(altResource);
    }
  }

  /**
   * An explicit {@code senzing.config.dir} system property must
   * override the default. The override directory must contain
   * {@code cfgVariant.json}.
   */
  @Test
  public void explicitConfigDirOverridesDefault() throws Exception
  {
    Path altConfig = Files.createTempDirectory("alt-config-");
    Files.writeString(altConfig.resolve("cfgVariant.json"), "{}");
    try {
      System.setProperty("senzing.path", install.root().toString());
      System.setProperty("senzing.config.dir", altConfig.toString());

      SzInstallLocations result = SzInstallLocations.findLocations();
      assertEquals(altConfig.toFile().getCanonicalFile(),
                   result.getConfigDirectory().getCanonicalFile(),
                   "Explicit senzing.config.dir must override the"
                       + " default");
    } finally {
      deleteRecursively(altConfig);
    }
  }

  // -------------------------------------------------------------------
  // Failure paths — IllegalStateException with a descriptive message
  // -------------------------------------------------------------------

  /**
   * If the {@code senzing.path} root has no {@code er/} install
   * subdirectory, {@link SzInstallLocations#findLocations()} must
   * throw {@link IllegalStateException} mentioning the install
   * directory.
   *
   * <p>{@code findLocations()} prints the stack trace to
   * {@link System#err} before rethrowing, so the assertion runs
   * inside a {@link SystemErr} stub to keep the build log clean.
   */
  @Test
  public void findLocationsThrowsWhenInstallDirIsMissing() throws Exception
  {
    Path emptyRoot = Files.createTempDirectory("senzing-empty-");
    try {
      System.setProperty("senzing.path", emptyRoot.toString());

      new SystemErr().execute(() -> {
        IllegalStateException ise = assertThrows(
            IllegalStateException.class,
            SzInstallLocations::findLocations);
        assertTrue(ise.getMessage().contains("installation directory")
                       || ise.getMessage().contains(
                           emptyRoot.resolve("er").toString()),
                   "Expected message to mention the missing install"
                       + " directory: " + ise.getMessage());
      });
    } finally {
      deleteRecursively(emptyRoot);
    }
  }

  /**
   * If {@code senzing.support.dir} points at a non-existent directory,
   * {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} that names the support directory.
   * Stderr is captured because {@code findLocations()} prints the
   * stack trace before rethrowing.
   */
  @Test
  public void findLocationsThrowsWhenSupportDirIsMissing() throws Exception
  {
    Path nowhere = install.root().resolve("nonexistent-support");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.support.dir", nowhere.toString());

    new SystemErr().execute(() -> {
      IllegalStateException ise = assertThrows(
          IllegalStateException.class,
          SzInstallLocations::findLocations);
      assertTrue(ise.getMessage().contains("support directory"),
                 "Expected message to mention support directory: "
                     + ise.getMessage());
    });
  }

  /**
   * If {@code senzing.resource.dir} points at a non-existent
   * directory, {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} that names the resource directory.
   * Stderr is captured because {@code findLocations()} prints the
   * stack trace before rethrowing.
   */
  @Test
  public void findLocationsThrowsWhenResourceDirIsMissing() throws Exception
  {
    Path nowhere = install.root().resolve("nonexistent-resource");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.resource.dir", nowhere.toString());

    new SystemErr().execute(() -> {
      IllegalStateException ise = assertThrows(
          IllegalStateException.class,
          SzInstallLocations::findLocations);
      assertTrue(ise.getMessage().contains("resource directory"),
                 "Expected message to mention resource directory: "
                     + ise.getMessage());
    });
  }

  /**
   * If {@code senzing.config.dir} points at a non-existent directory,
   * {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} mentioning the config directory.
   * Stderr is captured because {@code findLocations()} prints the
   * stack trace before rethrowing.
   */
  @Test
  public void findLocationsThrowsWhenConfigDirIsMissing() throws Exception
  {
    Path nowhere = install.root().resolve("nonexistent-config");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.config.dir", nowhere.toString());

    new SystemErr().execute(() -> {
      IllegalStateException ise = assertThrows(
          IllegalStateException.class,
          SzInstallLocations::findLocations);
      assertTrue(ise.getMessage().contains("config directory"),
                 "Expected message to mention config directory: "
                     + ise.getMessage());
    });
  }

  // -------------------------------------------------------------------
  // Accessors and toString
  // -------------------------------------------------------------------

  /**
   * {@link SzInstallLocations#toString()} must include each of the
   * five discovered locations and the development-build flag, per
   * the format produced by the implementation.
   */
  @Test
  public void toStringIncludesAllPaths() throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    SzInstallLocations result = SzInstallLocations.findLocations();
    String s = result.toString();
    assertTrue(s.contains("installDirectory"),
               "toString must include installDirectory: " + s);
    assertTrue(s.contains("configDirectory"));
    assertTrue(s.contains("supportDirectory"));
    assertTrue(s.contains("resourceDirectory"));
    assertTrue(s.contains("templatesDirectory"));
    assertTrue(s.contains("developmentBuild"));
  }

  /**
   * For a regular install (install dir named {@code er}),
   * {@link SzInstallLocations#isDevelopmentBuild()} must return
   * false. The {@code dist}-named install dir is what flips this to
   * true; our fixture uses {@code er}.
   */
  @Test
  public void isDevelopmentBuildIsFalseForNormalInstall() throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    SzInstallLocations result = SzInstallLocations.findLocations();
    assertFalse(result.isDevelopmentBuild(),
                "isDevelopmentBuild() must be false for er/-style"
                    + " install");
  }

  // -------------------------------------------------------------------
  // Environment-variable equivalents
  // -------------------------------------------------------------------

  /**
   * If {@code senzing.path} is unset but {@code SENZING_PATH} is set
   * in the environment, {@code findLocations()} must use the env var
   * to locate the install.
   */
  @Test
  public void findLocationsFromSenzingPathEnvVar() throws Exception
  {
    new EnvironmentVariables(
        "SENZING_PATH", install.root().toString()).execute(() -> {
      SzInstallLocations result = SzInstallLocations.findLocations();
      assertNotNull(result);
      assertEquals(install.installDir().toFile().getCanonicalFile(),
                   result.getInstallDirectory().getCanonicalFile());
    });
  }

  /**
   * If the {@code senzing.path} system property is empty / blank, the
   * implementation must fall back to {@code SENZING_PATH} per the
   * {@code length() == 0} guard.
   */
  @Test
  public void emptySenzingPathSystemPropertyFallsBackToEnvVar()
      throws Exception
  {
    System.setProperty("senzing.path", "   ");
    new EnvironmentVariables(
        "SENZING_PATH", install.root().toString()).execute(() -> {
      SzInstallLocations result = SzInstallLocations.findLocations();
      assertNotNull(result,
                    "Empty senzing.path must fall back to SENZING_PATH");
    });
  }

  /**
   * {@code SENZING_SUPPORT_DIR} env var must be honored when
   * {@code senzing.support.dir} is unset.
   */
  @Test
  public void findLocationsFromSupportDirEnvVar() throws Exception
  {
    Path altSupport = Files.createTempDirectory("alt-support-env-");
    try {
      System.setProperty("senzing.path", install.root().toString());
      new EnvironmentVariables(
          "SENZING_SUPPORT_DIR", altSupport.toString()).execute(() -> {
        SzInstallLocations result = SzInstallLocations.findLocations();
        assertEquals(altSupport.toFile().getCanonicalFile(),
                     result.getSupportDirectory().getCanonicalFile());
      });
    } finally {
      deleteRecursively(altSupport);
    }
  }

  /**
   * {@code SENZING_RESOURCE_DIR} env var must be honored when
   * {@code senzing.resource.dir} is unset.
   */
  @Test
  public void findLocationsFromResourceDirEnvVar() throws Exception
  {
    Path altResource = Files.createTempDirectory("alt-resource-env-");
    try {
      System.setProperty("senzing.path", install.root().toString());
      new EnvironmentVariables(
          "SENZING_RESOURCE_DIR", altResource.toString()).execute(() -> {
        SzInstallLocations result = SzInstallLocations.findLocations();
        assertEquals(altResource.toFile().getCanonicalFile(),
                     result.getResourceDirectory().getCanonicalFile());
      });
    } finally {
      deleteRecursively(altResource);
    }
  }

  /**
   * {@code SENZING_CONFIG_DIR} env var must be honored when
   * {@code senzing.config.dir} is unset. The directory must contain
   * {@code cfgVariant.json}.
   */
  @Test
  public void findLocationsFromConfigDirEnvVar() throws Exception
  {
    Path altConfig = Files.createTempDirectory("alt-config-env-");
    Files.writeString(altConfig.resolve("cfgVariant.json"), "{}");
    try {
      System.setProperty("senzing.path", install.root().toString());
      new EnvironmentVariables(
          "SENZING_CONFIG_DIR", altConfig.toString()).execute(() -> {
        SzInstallLocations result = SzInstallLocations.findLocations();
        assertEquals(altConfig.toFile().getCanonicalFile(),
                     result.getConfigDirectory().getCanonicalFile());
      });
    } finally {
      deleteRecursively(altConfig);
    }
  }

  /**
   * Empty {@code senzing.support.dir} is normalized to null, so the
   * implementation falls back to {@code <senzing.path>/data} per
   * {@link FakeSenzingInstall#supportDir()}.
   */
  @Test
  public void emptySupportDirSystemPropertyFallsBackToDefault()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.support.dir", "");

    SzInstallLocations result = SzInstallLocations.findLocations();
    assertEquals(install.supportDir().toFile().getCanonicalFile(),
                 result.getSupportDirectory().getCanonicalFile(),
                 "Empty senzing.support.dir must fall back to the"
                     + " <senzing.path>/data default");
  }

  /**
   * Empty {@code senzing.resource.dir} is normalized to null, so the
   * implementation falls back to {@code <install>/resources}.
   */
  @Test
  public void emptyResourceDirSystemPropertyFallsBackToDefault()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.resource.dir", "");

    SzInstallLocations result = SzInstallLocations.findLocations();
    assertEquals(install.resourceDir().toFile().getCanonicalFile(),
                 result.getResourceDirectory().getCanonicalFile());
  }

  /**
   * Empty {@code senzing.config.dir} is normalized to null, so the
   * implementation falls back to {@code <install>/etc}.
   */
  @Test
  public void emptyConfigDirSystemPropertyFallsBackToDefault()
      throws Exception
  {
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.config.dir", "");

    SzInstallLocations result = SzInstallLocations.findLocations();
    assertEquals(install.configDir().toFile().getCanonicalFile(),
                 result.getConfigDirectory().getCanonicalFile());
  }

  // -------------------------------------------------------------------
  // Dev-build detection and missing-file errors
  // -------------------------------------------------------------------

  /**
   * When the install directory is named {@code "dist"},
   * {@link SzInstallLocations#isDevelopmentBuild()} must return
   * {@code true}.
   */
  @Test
  public void isDevelopmentBuildTrueWhenInstallDirNamedDist()
      throws Exception
  {
    // Build a parallel fake install whose install dir is "dist"
    // instead of "er".
    Path devRoot = Files.createTempDirectory("dev-build-");
    Files.createDirectories(devRoot.resolve("dist/etc"));
    Files.writeString(
        devRoot.resolve("dist/etc/cfgVariant.json"), "{}");
    Files.createDirectories(devRoot.resolve("dist/resources"));
    Files.createDirectories(devRoot.resolve("data"));
    try {
      // The implementation derives installDir as
      // <senzingPath>/er, so "dist" naming must be picked up via a
      // direct file pointing at "dist". Symlink "er" -> "dist" so
      // the fake install's er/ resolves to dist/.
      try {
        Files.createSymbolicLink(
            devRoot.resolve("er"), devRoot.resolve("dist"));
      } catch (UnsupportedOperationException
                 | java.nio.file.FileSystemException e) {
        // Filesystems that don't support symlinks (e.g. some CI
        // environments) make this assertion impossible; skip
        // gracefully.
        org.junit.jupiter.api.Assumptions.assumeTrue(false,
            "Filesystem does not support symbolic links");
      }

      System.setProperty("senzing.path", devRoot.toString());
      SzInstallLocations result = SzInstallLocations.findLocations();

      // The discovered installDir is the symlink's target, "dist".
      assertEquals("dist",
                   result.getInstallDirectory()
                         .getCanonicalFile()
                         .getName(),
                   "installDir resolved through symlink should be "
                       + "named 'dist'");
      assertTrue(result.isDevelopmentBuild(),
                 "Install dir named 'dist' must mark"
                     + " isDevelopmentBuild() = true");
    } finally {
      deleteRecursively(devRoot);
    }
  }

  /**
   * If the config directory exists but is missing
   * {@code cfgVariant.json} (and the support dir does not have it
   * either), {@code findLocations} must throw
   * {@link IllegalStateException} naming the missing file.
   */
  @Test
  public void missingCfgVariantThrows() throws Exception
  {
    // Build a fake install with config dir but no cfgVariant.json.
    Path root = Files.createTempDirectory("missing-cfg-");
    Files.createDirectories(root.resolve("er/etc"));
    Files.createDirectories(root.resolve("er/resources"));
    Files.createDirectories(root.resolve("data"));
    // NOTE: deliberately omit cfgVariant.json under er/etc and data/.
    try {
      System.setProperty("senzing.path", root.toString());

      new SystemErr().execute(() -> {
        IllegalStateException ise = assertThrows(
            IllegalStateException.class,
            SzInstallLocations::findLocations);
        assertTrue(ise.getMessage().contains("cfgVariant.json"),
                   "Error message must name the missing file: "
                       + ise.getMessage());
      });
    } finally {
      deleteRecursively(root);
    }
  }

  /**
   * If {@code senzing.path} points at a non-existent directory, the
   * implementation sets {@code senzingDir = null} and then derives
   * {@code installDir = new File(null, "er")} which fails the
   * {@code installDir.exists()} check, throwing
   * {@link IllegalStateException}.
   */
  @Test
  public void senzingPathNonExistentDirectoryThrows() throws Exception
  {
    Path nonexistent = Files.createTempDirectory("nonexistent-parent-")
                            .resolve("never-created");
    System.setProperty("senzing.path", nonexistent.toString());

    new SystemErr().execute(() -> {
      assertThrows(IllegalStateException.class,
                   SzInstallLocations::findLocations);
    });
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  private static void deleteRecursively(Path dir) throws IOException
  {
    if (dir == null || !Files.exists(dir)) return;
    try (var stream = Files.walk(dir)) {
      stream.sorted(Comparator.reverseOrder())
            .forEach(p -> {
              try {
                Files.delete(p);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
    }
  }
}
