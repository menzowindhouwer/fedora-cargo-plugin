
package com.yourmediashelf.fedora.cargo;

import java.io.File;
import java.util.List;

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
     * Version of Fedora (e.g. 3.6 or 3.6.1-SNAPSHOT)
     * @parameter alias="fcrepo.version" 
     *            property="fcrepo.version" 
     *            default-value="3.6.1-SNAPSHOT"
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
    * The project's remote repositories to use for the resolution of plugins and their dependencies.
    *
    * @parameter default-value="${project.remotePluginRepositories}"
    * @readonly
    */
    private List<RemoteRepository> remoteRepos;

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
}
