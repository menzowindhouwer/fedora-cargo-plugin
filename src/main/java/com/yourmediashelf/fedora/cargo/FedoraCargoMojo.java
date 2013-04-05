/**
 * Copyright (C) 2012 MediaShelf <http://www.yourmediashelf.com/>
 *
 * This file is part of fedora-cargo-plugin.
 *
 * fedora-cargo-plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * fedora-cargo-plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with fedora-cargo-plugin.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.yourmediashelf.fedora.cargo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.configuration.StandaloneLocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.sonatype.aether.resolution.ArtifactResult;

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
     * The {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} 
     * of the war to resolve.
     *
     * @parameter alias="fedora.war" 
     *            property="fedora.war" 
     *            default-value="org.fcrepo:fcrepo-webapp-fedora:war:${fcrepo.version}"
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
     * container.id (e.g. tomcat7x)
     * @parameter alias="container.id"
     *            property="container.id" 
     *            default-value="tomcat7x"
     */
    protected String containerId;

    /**
     * Container log file location
     * @parameter property="container.log" 
     *            default-value="${project.build.directory}/container.log"
     */
    private String containerLog;

    /**
     * @parameter alias="container.artifact"
     *            property="container.artifact" 
     *            default-value="org.fcrepo:apache-tomcat:zip:7.0.39"
     */
    private String containerArtifact;

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

    protected String installContainer() throws MojoExecutionException {
        File buildDir =
                new File(mavenProject.getBuild().getDirectory(), "extracts");
        ArtifactResult result = getArtifact(containerArtifact);
        try {
            unzip(result.getArtifact().getFile(), buildDir);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        // Some potentially unwarranted assumptions to determine the 
        // install directory:
        // if buildDir contains only one file, we assume that the child is the 
        // home dir, otherwise we assume the archive did not contain a root dir 
        // and we just return the buildDir
        File[] subdirs = buildDir.listFiles();
        if (subdirs != null && subdirs.length == 1) {
            return subdirs[0].getAbsolutePath();
        } else {
            return buildDir.getAbsolutePath();
        }
    }

    private InstalledLocalContainer configureContainer(String home)
            throws MojoExecutionException {
        getLog().info("Using container home: " + home);

        ConfigurationFactory configurationFactory =
                new DefaultConfigurationFactory();
        StandaloneLocalConfiguration configuration =
                (StandaloneLocalConfiguration) configurationFactory
                        .createConfiguration(
                        containerId, ContainerType.INSTALLED,
                        ConfigurationType.STANDALONE);
        configuration.setProperty(ServletPropertySet.PORT, fedoraPort);

        DefaultContainerFactory cf = new DefaultContainerFactory();
        InstalledLocalContainer c =
                (InstalledLocalContainer) cf.createContainer(containerId,
                        ContainerType.INSTALLED, configuration);
        c.setHome(home);
        c.setOutput(containerLog);
        c.setSystemProperties(getSystemProperties());

        deploy(c.getConfiguration());
        return c;
    }

    //FIXME: this currently deploys fedoraWar unless the current artifact
    // is itself a war. We really should be more flexible here.
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
     * @return a Map of properties that includes the key "fedora.home" if it
     * was defined.
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
