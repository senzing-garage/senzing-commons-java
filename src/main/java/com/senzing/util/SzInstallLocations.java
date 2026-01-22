package com.senzing.util;

import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import static com.senzing.util.OperatingSystemFamily.RUNTIME_OS_FAMILY;

/**
 * Describes the directories on disk used to find the Senzing product
 * installation and the support directories.
 */
public class SzInstallLocations {
    /**
     * The installation location.
     */
    private File installDir;

    /**
     * The location of the configuration files for the config directory.
     */
    private File configDir;

    /**
     * The location of the resource files for the resource directory.
     */
    private File resourceDir;

    /**
     * The location of the support files for the support directory.
     */
    private File supportDir;

    /**
     * The location of the template files for the template directory.
     */
    private File templatesDir;

    /**
     * Indicates if the installation direction is from a development build.
     */
    private boolean devBuild = false;

    /**
     * Default constructor.
     */
    private SzInstallLocations() {
        this.installDir = null;
        this.configDir = null;
        this.resourceDir = null;
        this.supportDir = null;
        this.templatesDir = null;
        this.devBuild = false;
    }

    /**
     * Gets the primary installation directory.
     *
     * @return The primary installation directory.
     */
    public File getInstallDirectory() {
        return this.installDir;
    }

    /**
     * Gets the configuration directory.
     *
     * @return The configuration directory.
     */
    public File getConfigDirectory() {
        return this.configDir;
    }

    /**
     * Gets the resource directory.
     *
     * @return The resource directory.
     */
    public File getResourceDirectory() {
        return this.resourceDir;
    }

    /**
     * Gets the support directory.
     *
     * @return The support directory.
     */
    public File getSupportDirectory() {
        return this.supportDir;
    }

    /**
     * Gets the templates directory.
     *
     * @return The templates directory.
     */
    public File getTemplatesDirectory() {
        return this.templatesDir;
    }

    /**
     * Checks if the installation is actually a development build.
     * 
     * @return <code>true</code> if this installation represents a
     *         development build, otherwise <code>false</code>.
     */
    public boolean isDevelopmentBuild() {
        return this.devBuild;
    }

    /**
     * Produces a {@link String} describing this instance.
     * 
     * @return A {@link String} describing this instance.
     */
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println();
        pw.println("--------------------------------------------------");
        pw.println("installDirectory   : " + this.getInstallDirectory());
        pw.println("configDirectory    : " + this.getConfigDirectory());
        pw.println("supportDirectory   : " + this.getSupportDirectory());
        pw.println("resourceDirectory  : " + this.getResourceDirectory());
        pw.println("templatesDirectory : " + this.getTemplatesDirectory());
        pw.println("developmentBuild   : " + this.isDevelopmentBuild());

        return sw.toString();
    }

    /**
     * Finds the install directories and returns the {@link SzInstallLocations}
     * instance describing those locations.
     *
     * @return The {@link SzInstallLocations} instance describing the install
     *         locations.
     * 
     * @throws IllegalStateException If the install locations could not be found.
     */
    public static SzInstallLocations findLocations() throws IllegalStateException
    {
        File homeDir        = new File(System.getProperty("user.home"));
        File homeSenzing    = new File(homeDir, "senzing");
        File homeSupport    = new File(homeSenzing, "data");
        
        File senzingDir     = null;
        File installDir     = null;
        File configDir      = null;
        File resourceDir    = null;
        File supportDir     = null;
        File templatesDir   = null;
        try {
            String defaultSenzingPath = null;
            String defaultConfigPath = null;
            String defaultSupportPath = null;
            
            // get the senzing path
            String senzingPath = System.getProperty("senzing.path");
            if (senzingPath == null || senzingPath.trim().length() == 0) {
                senzingPath = System.getenv("SENZING_PATH");
            }
            if (senzingPath != null && senzingPath.trim().length() == 0) {
                senzingPath = null;
            }

            // check if we are in the dev structure with no senzing path defined
            switch (RUNTIME_OS_FAMILY) {
                case WINDOWS:
                    defaultSenzingPath = homeSenzing.getCanonicalPath();
                    defaultSupportPath = homeSupport.getCanonicalPath();
                    break;
                case MAC_OS:
                    defaultSenzingPath = homeSenzing.getCanonicalPath();
                    defaultSupportPath = homeSupport.getCanonicalPath();
                    break;
                case UNIX:
                    defaultSenzingPath  = "/opt/senzing";
                    defaultSupportPath  = defaultSenzingPath + "/data";
                    defaultConfigPath   = "/etc/opt/senzing";
                    break;
                default:
                    throw new IllegalStateException(
                            "Unrecognized Operating System: " + RUNTIME_OS_FAMILY);
            }    

            // check for senzing system properties
            String configPath = System.getProperty("senzing.config.dir");
            String supportPath = System.getProperty("senzing.support.dir");
            String resourcePath = System.getProperty("senzing.resource.dir");

            // try environment variables if system properties don't work
            if (configPath == null || configPath.trim().length() == 0) {
                configPath = System.getenv("SENZING_CONFIG_DIR");
            }
            if (supportPath == null || supportPath.trim().length() == 0) {
                supportPath = System.getenv("SENZING_SUPPORT_DIR");
            }
            if (resourcePath == null || resourcePath.trim().length() == 0) {
                resourcePath = System.getenv("SENZING_RESOURCE_DIR");
            }

            // normalize empty strings as null
            if (configPath != null && configPath.trim().length() == 0) {
                configPath = null;
            }
            if (supportPath != null && supportPath.trim().length() == 0) {
                supportPath = null;
            }
            if (resourcePath != null && resourcePath.trim().length() == 0) {
                resourcePath = null;
            }

            // check for the root senzing dir
            senzingDir = new File((senzingPath == null) ? defaultSenzingPath : senzingPath);
            
            if (!senzingDir.exists()) {
                senzingDir = null;
            }

            // check the senzing install directory
            installDir = new File(senzingDir, "er");

            if ((!installDir.exists()) || (!installDir.isDirectory())) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                if (!installDir.exists()) {
                    pw.println("Could not find Senzing ER installation directory:");
                } else {
                    pw.println("Senzing ER installation directory appears invalid:");
                }
                pw.println("     " + installDir);
                pw.println();
                if (senzingPath != null) {
                    pw.println("Check the -Dsenzing.path=[path] command line option "
                        + "or SENZING_PATH environment variable.");
        
                } else {
                    pw.println("Use the -Dsenzing.path=[path] command line option or SENZING_PATH "
                        + "environment variable to specify a base Senzing path.");
                }
                pw.flush();
                throw new IllegalStateException(sw.toString());
            }


            // check the senzing support path
            supportDir = (supportPath != null) ? new File(supportPath) : null;

            // check if support dir is not defined BUT senzing path is defined
            if (supportDir == null && senzingPath != null && senzingDir != null) 
            {
                supportDir = new File(senzingDir, "data");
                if (!supportDir.exists()) {
                    supportDir = null;
                }
            }

            // fall back to whatever the default support directory path is
            if (supportDir == null) 
            {
                supportDir = new File(defaultSupportPath);
            }

            // verify the discovered support directory
            if ((!supportDir.exists()) || (!supportDir.isDirectory())) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                if (!supportDir.exists()) {
                    pw.println("Could not find Senzing support directory:");
                } else {
                    pw.println("Senzing support directory appears invalid:");
                }
                pw.println("     " + supportDir);
                pw.println();
                if (supportPath != null) {
                    pw.println("Check the -Dsenzing.support.dir=[path] command line option "
                        + "or SENZING_SUPPORT_DIR environment variable.");
                
                } else if (senzingPath != null) {
                    pw.println("Check the -Dsenzing.path=[path] command line option "
                        + "or SENZING_PATH environment variable.");
        
                } else {
                    pw.println("Use the -Dsenzing.path=[path] command line option or SENZING_PATH "
                        + "environment variable to specify a base Senzing path.");
                    pw.println();
                    pw.println("Alternatively, use the -Dsenzing.support.dir=[path] command line option or "
                        + "SENZING_SUPPORT_DIR environment variable to specify a Senzing ER path.");
                }
                pw.println("The support directory does not exist or is invalid: " + supportDir);
                pw.flush();
                throw new IllegalStateException(sw.toString());
            }

            // now determine the resource path
            resourceDir = (resourcePath != null) ? new File(resourcePath) : null;

            // try the "resources" sub-directory of the installation
            if (resourceDir == null && installDir != null) {
                resourceDir = new File(installDir, "resources");
                if (!resourceDir.exists()) {
                    resourceDir = null;
                }
            }

            // set the templates directory if we have the resource directory
            if (resourceDir != null && resourceDir.exists()
                && resourceDir.isDirectory()) 
            {
                templatesDir = new File(resourceDir, "templates");
            }

            // verify the discovered resource path
            if ((resourceDir == null) || (!resourceDir.exists()) 
                 || (!resourceDir.isDirectory())) 
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                if (resourceDir == null || !resourceDir.exists()) {
                    pw.println("Could not find Senzing resource directory:");
                } else {
                    pw.println("Senzing resource directory appears invalid:");
                }
                if (resourceDir != null) pw.println("         " + resourceDir);
                
                pw.println();

                if (resourcePath != null) {
                    pw.println("Check the -Dsenzing.resource.dir=[path] command line option "
                        + "or SENZING_RESOURCE_DIR environment variable.");
                
                } else if (senzingPath != null) {
                    pw.println("Check the -Dsenzing.path=[path] command line option "
                        + "or SENZING_PATH environment variable.");
        
                } else {
                    pw.println("Use the -Dsenzing.path=[path] command line option or SENZING_PATH "
                        + "environment variable to specify a valid base Senzing path.");
                    pw.println();
                    pw.println("Alternatively, use the -Dsenzing.resource.dir=[path] command line option or "
                        + "SENZING_RESOURCE_DIR environment variable to specify a Senzing resource path.");
                }

                pw.println("The resource directory does not exist or is invalid: " + resourceDir);
                pw.flush();
                throw new IllegalStateException(sw.toString());
            }

            // check the senzing config path
            configDir = (configPath != null) ? new File(configPath) : null;

            // check if config dir is still not defined and fall back to default
            if (configDir == null && defaultConfigPath != null) {
                configDir = new File(defaultConfigPath);
                if (!configDir.exists()) {
                    configDir = null;
                }
            }

            // if still null, try to use the install's etc directory
            if (configDir == null && installDir != null) {
                configDir = new File(installDir, "etc");
                if (!configDir.exists()) {
                    configDir = null;
                }                
            }

            // validate the contents of the config directory
            List<String> missingFiles = new LinkedList<>();

            // check if the config directory does not exist
            if (configDir != null && configDir.exists()) {
                String[] requiredFiles = { "cfgVariant.json" };

                for (String fileName : requiredFiles) {
                    File configFile = new File(configDir, fileName);
                    File supportFile = new File(supportDir, fileName);
                    if (!configFile.exists() && !supportFile.exists()) {
                        missingFiles.add(fileName);
                    }
                }
            }

            // verify the discovered config directory
            if ((configDir == null) || (!configDir.exists()) 
                 || (!configDir.isDirectory()) || (missingFiles.size() > 0))
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                if (configDir == null || !configDir.exists()) {
                    pw.println("Could not find Senzing config directory:");
                } else {
                    pw.println("Senzing config directory appears invalid:");
                }
                if (configDir != null) pw.println("     " + configDir);

                if (missingFiles.size() > 0) {
                    for (String missing : missingFiles) {
                        pw.println("         " + missing + " was not found in config directory");
                    }
                }

                pw.println();
                if (configPath != null) {
                    pw.println("Check the -Dsenzing.config.dir=[path] command line option "
                        + "or SENZING_CONFIG_DIR environment variable.");
                
                } else if (senzingPath != null) {
                    pw.println("Check the -Dsenzing.path=[path] command line option "
                        + "or SENZING_PATH environment variable.");
        
                } else {
                    pw.println("Use the -Dsenzing.path=[path] command line option or SENZING_PATH "
                        + "environment variable to specify a valid base Senzing path.");
                    pw.println();
                    pw.println("Alternatively, use the -Dsenzing.config.dir=[path] command line option or "
                        + "SENZING_CONFIG_DIR environment variable to specify a Senzing config path.");
                }

                pw.println("The config directory does not exist or is invalid: " + configDir
                    + (missingFiles.size() == 0 ? "" : ", missingFiles=[ " + missingFiles + " ]"));
                pw.flush();
                throw new IllegalStateException(sw.toString());
            }

            // construct and initialize the result
            SzInstallLocations result = new SzInstallLocations();
            result.installDir = installDir;
            result.configDir = configDir;
            result.supportDir = supportDir;
            result.resourceDir = resourceDir;
            result.templatesDir = templatesDir;
            result.devBuild = ("dist".equals(installDir.getName()));

            // return the result
            return result;

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
