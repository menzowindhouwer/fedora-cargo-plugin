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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * 
 * @author Edwin Shin
 */
public abstract class FedoraCommonMojo extends AbstractMojo {

    /** @parameter default-value="${project}" */
    protected MavenProject mavenProject;

    /**
     * Version of Fedora (e.g. 3.6.2 or 3.7-SNAPSHOT)
     * @parameter alias="fcrepo.version" 
     *            property="fcrepo.version" 
     *            default-value="3.6.2"
     */
    protected String fcrepoVersion;

    /**
     * Destination directory of (unzipped) FEDORA_HOME
     * @parameter alias="fedora.home"
     *            property="fedora.home"
     *            default-value="${project.build.directory}/fedora-home"
     */
    protected File fedoraHomeDir;

    /**
     * @parameter alias="fedora.port" property="fedora.port" default-value="8080"
     */
    protected String fedoraPort;

    /**
    * The entry point to Aether, i.e. the component doing all the work.
    *
    * @component
    */
    private RepositorySystem repoSystem;

    /**
    * The current repository/network configuration of Maven.
    *
    * @parameter default-value="${repositorySystemSession}"
    * @readonly
    */
    private RepositorySystemSession repoSession;

    /**
    * The project's remote repositories to use for the resolution of dependencies.
    * 
    * For example, ${project.remoteProjectRepositories} or ${project.remotePluginRepositories}
    *
    * @parameter default-value="${project.remoteProjectRepositories}"
    * @readonly
    */
    private List<RemoteRepository> remoteRepos;

    private RemoteRepository thirdParty;

    protected ArtifactResult getArtifact(String coords)
            throws MojoExecutionException {
        Artifact artifact;
        try {
            artifact = new DefaultArtifact(coords);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(remoteRepos);
        request.addRepository(getThirdPartyRepo());

        getLog().info("Resolving artifact " + artifact + " from " + remoteRepos);

        ArtifactResult result;
        try {
            result = repoSystem.resolveArtifact(repoSession, request);
        } catch (ArtifactResolutionException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        getLog().info(
                "Resolved artifact " + artifact + " to " +
                        result.getArtifact().getFile() + " from " +
                        result.getRepository());
        return result;
    }

    /**
     * Unzips the InputStream to the given destination directory.
     * 
     * @param zipFile File to unzip
     * @param destDir
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void unzip(File zipFile, File destDir)
            throws FileNotFoundException, IOException {
        int BUFFER = 2048;
        BufferedOutputStream dest = null;
        ZipFile zip = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        ZipEntry entry;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            if (entry.isDirectory()) {
                // Otherwise, empty directories do not get created
                (new File(destDir, entry.getName())).mkdirs();
            } else {
                BufferedInputStream is =
                        new BufferedInputStream(zip.getInputStream(entry));

                File f = new File(destDir, entry.getName());
                f.getParentFile().mkdirs();
                int count;
                byte data[] = new byte[BUFFER];
                // write the files to the disk
                FileOutputStream fos = new FileOutputStream(f);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
                is.close();
            }
        }
    }

    /**
     * Tomcat (as a zip) only became available in Maven Central as of version 
     * 7.0.35 (11 January 2013). This allows us to add a repo for previous
     * versions of Tomcat 6x and 7x).
     * 
     * @return A RemoteRepository configured with the duraspace-thirdparty 
     * Maven repository (https://m2.duraspace.org/content/repositories/thirdparty).
     */
    private RemoteRepository getThirdPartyRepo() {
        if (thirdParty == null) {
            thirdParty =
                    new RemoteRepository("duraspace-thirdparty", "default",
                        "https://m2.duraspace.org/content/repositories/thirdparty");
        }
        return thirdParty;
    }
}
