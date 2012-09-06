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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.aether.resolution.ArtifactResult;

/**
 * 
 * @author Edwin Shin
 * @goal fedora-home
 * @requiresProject false
 */
public class FedoraHomeMojo extends FedoraCommonMojo {

    /**
     * Location of install.properties File
     * @parameter alias="install.properties" 
     *            property="install.properties"
     */
    private File installProperties;

    /**
     * The {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} of the artifact to resolve.
     *
     * @parameter alias="fedora.home.zip" property="fedora.home.zip" default-value="org.fcrepo:fcrepo-installer:zip:fedora-home:${fcrepo.version}"
     */
    private String fedoraHomeZip;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info(fcrepoVersion.toString());

        ArtifactResult result = getArtifact(fedoraHomeZip);

        try {
            unzip(result.getArtifact().getFile(), fedoraHomeDir);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        applyFedoraHomeInstallProperties();
    }

    /**
     * Applies install.properties to FEDORA_HOME, performing property
     * substitutions for ${fedora.home} and the like.
     * 
     * If the install.properties parameter is not set, applies a set of
     * default properties.
     * 
     * @throws MojoExecutionException
     */
    private void applyFedoraHomeInstallProperties()
            throws MojoExecutionException {
        InstallOptions opts;
        Map<String, String> props = getInstallProperties();

        try {
            opts = new InstallOptions(props);
        } catch (OptionValidationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        FedoraHome fh = new FedoraHome(opts);
        try {
            fh.install();
        } catch (InstallationFailedException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Map<String, String> getInstallProperties()
            throws MojoExecutionException {
        Map<String, String> filteredProps = new HashMap<String, String>();
        if (installProperties == null) {
            getLog().info("install.properties not set, using default");
            InputStream is =
                    this.getClass().getClassLoader().getResourceAsStream(
                            "default_install.properties");
            try {
                // Use the Maven project properties as the lookup table for 
                // property substition values
                Properties mavenProps = mavenProject.getProperties();
                Properties p = applyFilters(is, mavenProps);
                filteredProps.putAll(FedoraHome.loadMap(p));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            try {
                filteredProps.putAll(FedoraHome.loadMap(installProperties));
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        return filteredProps;
    }

    /**
     * Applies values defined in lookup to source.
     * 
     * @param source
     * @param lookup
     * @return
     * @throws IOException
     */
    public static Properties applyFilters(InputStream source,
            Properties lookup) throws IOException {

        final Properties sourceProps = new Properties();
        try {
            sourceProps.load(source);
        } finally {
            IOUtil.close(source);
        }

        final Properties filterdProps = new Properties();
        filterdProps.putAll(sourceProps);

        for (Iterator<?> iter = sourceProps.keySet().iterator(); iter.hasNext();) {
            final String k = (String) iter.next();
            final String propValue =
                    getPropertyValue(k, filterdProps, lookup);
            sourceProps.setProperty(k, propValue);
        }
        return sourceProps;
    }

    /**
     * A less complete, modified version of PropertyUtils.getPropertyValue
     * 
     * @param k
     * @param p
     * @param lookup
     * @return
     */
    private static String getPropertyValue(String k, Properties p,
            Properties lookup) {
        String v = p.getProperty(k);
        String ret = "";
        int idx, idx2;

        while ((idx = v.indexOf("${")) >= 0) {
            // append prefix to result
            ret += v.substring(0, idx);

            // strip prefix from original
            v = v.substring(idx + 2);

            // if no matching } then bail
            if ((idx2 = v.indexOf('}')) < 0) {
                break;
            }

            // strip out the key and resolve it
            // resolve the key/value for the ${statement}
            String nk = v.substring(0, idx2);
            v = v.substring(idx2 + 1);
            String nv = lookup.getProperty(nk);

            // try global environment..
            if (nv == null && !StringUtils.isEmpty(nk)) {
                nv = System.getProperty(nk);
            }

            // if the key cannot be resolved, leave it as is
            if (nv == null || nv.equals(k)) {
                ret += "${" + nk + "}";
            } else {
                v = nv + v;
            }
        }
        return ret + v;
    }
}
