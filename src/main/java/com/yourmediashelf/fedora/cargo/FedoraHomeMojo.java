
package com.yourmediashelf.fedora.cargo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
     *            default-value="${project.build.testOutputDirectory}/install.properties"
     */
    private File installProperties;

    /**
     * The {@code <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>} of the artifact to resolve.
     *
     * @parameter alias="fedora.home.zip" property="fedora.home.zip" default-value="org.fcrepo:fcrepo-installer:zip:fedora-home:${fcrepo.version}"
     */
    private String fedoraHomeZip;

    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info(installProperties.toString());
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

    private void applyFedoraHomeInstallProperties()
            throws MojoExecutionException {
        InstallOptions opts;
        Map<String, String> props = new HashMap<String, String>();
        try {
            props.putAll(FedoraHome.loadMap(installProperties));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
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

}
