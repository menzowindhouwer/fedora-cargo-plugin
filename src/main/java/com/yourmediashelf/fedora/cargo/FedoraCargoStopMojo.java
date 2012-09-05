
package com.yourmediashelf.fedora.cargo;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.cargo.container.LocalContainer;


/**
 * 
 * @author Edwin Shin
 * @goal fedora-stop
 * @phase post-integration-test
 */
public class FedoraCargoStopMojo extends FedoraCargoMojo {

    public void doExecute() throws MojoExecutionException {

        getLog().info("executing FedoraCargoStopMojo");

        LocalContainer container = getContainer();
        container.stop();
    }
}
