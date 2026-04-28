package com.senzing.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

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
   */
  @Test
  public void findLocationsThrowsWhenInstallDirIsMissing() throws Exception
  {
    Path emptyRoot = Files.createTempDirectory("senzing-empty-");
    try {
      System.setProperty("senzing.path", emptyRoot.toString());

      IllegalStateException ise = assertThrows(
          IllegalStateException.class,
          SzInstallLocations::findLocations);
      assertTrue(ise.getMessage().contains("installation directory")
                     || ise.getMessage().contains(
                         emptyRoot.resolve("er").toString()),
                 "Expected message to mention the missing install"
                     + " directory: " + ise.getMessage());
    } finally {
      deleteRecursively(emptyRoot);
    }
  }

  /**
   * If {@code senzing.support.dir} points at a non-existent directory,
   * {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} that names the support directory.
   */
  @Test
  public void findLocationsThrowsWhenSupportDirIsMissing()
  {
    Path nowhere = install.root().resolve("nonexistent-support");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.support.dir", nowhere.toString());

    IllegalStateException ise = assertThrows(
        IllegalStateException.class,
        SzInstallLocations::findLocations);
    assertTrue(ise.getMessage().contains("support directory"),
               "Expected message to mention support directory: "
                   + ise.getMessage());
  }

  /**
   * If {@code senzing.resource.dir} points at a non-existent
   * directory, {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} that names the resource directory.
   */
  @Test
  public void findLocationsThrowsWhenResourceDirIsMissing()
  {
    Path nowhere = install.root().resolve("nonexistent-resource");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.resource.dir", nowhere.toString());

    IllegalStateException ise = assertThrows(
        IllegalStateException.class,
        SzInstallLocations::findLocations);
    assertTrue(ise.getMessage().contains("resource directory"),
               "Expected message to mention resource directory: "
                   + ise.getMessage());
  }

  /**
   * If {@code senzing.config.dir} points at a non-existent directory,
   * {@link SzInstallLocations#findLocations()} must throw
   * {@link IllegalStateException} mentioning the config directory.
   */
  @Test
  public void findLocationsThrowsWhenConfigDirIsMissing()
  {
    Path nowhere = install.root().resolve("nonexistent-config");
    System.setProperty("senzing.path", install.root().toString());
    System.setProperty("senzing.config.dir", nowhere.toString());

    IllegalStateException ise = assertThrows(
        IllegalStateException.class,
        SzInstallLocations::findLocations);
    assertTrue(ise.getMessage().contains("config directory"),
               "Expected message to mention config directory: "
                   + ise.getMessage());
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
