
package com.yourmediashelf.fedora.cargo;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

/**
 * 
 * @author Edwin Shin
 */
public abstract class FedoraCargoMojo extends FedoraCommonMojo {

    /**
     * The key under which the container instance is stored in the plugin context. We store it so
     * that it's possible to get back the same container instance even if this mojo is called in a
     * different Maven execution context. This is required for stopping embedded containers for
     * example as we need to use the same instance that was started in order to stop them.
     */
    public static final String CONTEXT_CONTAINER_KEY_PREFIX =
            FedoraCargoMojo.class.getName() + "-Container";

    /**
     * Version of Fedora (e.g. 3.6 or 3.6.1-SNAPSHOT)
     * @parameter property="containerId" 
     *            default-value="tomcat7x"
     */
    protected String containerId;

    /**
     * @parameter alias="fedora.port" property="fedora.port" default-value="8080"
     */
    protected String fedoraPort;

    /**
     * The {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} of the artifact to resolve.
     *
     * @parameter alias="fedora.war" property="fedora.war" default-value="org.fcrepo:fcrepo-webapp-fedora:war:${fcrepo.version}"
     */
    private String fedoraWar;

    /**
     * System properties to pass through to the container.
     *
     * @parameter alias="systemProperties" 
     *            property="systemProperties"
     */
    private Map<String, String> systemProperties;

    /**
     * Container log file location
     * @parameter property="container.log" 
     *            default-value="${project.build.directory}/container.log"
     */
    private String containerLog;

    public void execute() throws MojoExecutionException {

        doExecute();
    }

    protected abstract void doExecute() throws MojoExecutionException;

    protected LocalContainer getContainer() throws MojoExecutionException {
        LocalContainer container = null;
        @SuppressWarnings("unchecked")
        Map<Object, Object> context = getPluginContext();

        String containerKey = CONTEXT_CONTAINER_KEY_PREFIX + "." + containerId;

        if (context != null) {
            container = (LocalContainer) context.get(containerKey);
        }

        if (container == null) {
            container = createNewContainer();
        }

        if (context != null) {
            context.put(containerKey, container);
        }

        return container;
    }

    private LocalContainer createNewContainer() throws MojoExecutionException {
        String home = installContainer();
        return configureContainer(home);
    }

    private String installContainer() throws MojoExecutionException {
        Installer installer;
        try {
            installer =
                    new ZipURLInstaller(
                            new URL(
                                    "https://archive.apache.org/dist/tomcat/tomcat-7/v7.0.29/bin/apache-tomcat-7.0.29.zip"),
                            "target/downloads", "target/extracts");
        } catch (MalformedURLException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        installer.install();
        getLog().info("Installed container to " + installer.getHome());
        return installer.getHome();
    }

    private InstalledLocalContainer configureContainer(String home)
            throws MojoExecutionException {
        ConfigurationFactory configurationFactory =
                new DefaultConfigurationFactory();
        LocalConfiguration configuration =
                (LocalConfiguration) configurationFactory.createConfiguration(
                        containerId, ContainerType.INSTALLED,
                        ConfigurationType.STANDALONE);
        configuration.setProperty(ServletPropertySet.PORT, fedoraPort);

        DefaultContainerFactory cf = new DefaultContainerFactory();
        InstalledLocalContainer c =
                (InstalledLocalContainer) cf.createContainer(containerId,
                        ContainerType.INSTALLED, configuration);
        c.setHome(home);

        deploy(c.getConfiguration());

        c.setOutput(containerLog);
        c.setSystemProperties(getSystemProperties());

        return c;
    }

    //FIXME: this currently deploys fedoraWar unless the current artifact
    // is itself a war. Should be flexible enough to support one or many wars.
    private void deploy(LocalConfiguration cfg) throws MojoExecutionException {
        WAR war = null;
        if (mavenProject.getPackaging().equalsIgnoreCase("war")) {
            getLog().info("Deploying " + mavenProject.getArtifact().getFile()
                                    .getAbsolutePath());
            war = new WAR(mavenProject.getArtifact().getFile()
                            .getAbsolutePath());
        } else {
            war =
                    new WAR(getArtifact(fedoraWar).getArtifact().getFile()
                            .getAbsolutePath());
        }
        war.setContext("fedora");
        cfg.addDeployable(war);
    }

    /**
     * Ensures that fedora.home is set as a system prop (if it was defined)
     * @return
     */
    private Map<String, String> getSystemProperties() {
        if (systemProperties == null) {
            systemProperties = new HashMap<String, String>();
        }
        if (!systemProperties.containsKey("fedora.home") &&
                fedoraHomeDir != null) {
            systemProperties
                    .put("fedora.home", fedoraHomeDir.getAbsolutePath());
        }

        return systemProperties;
    }
}
