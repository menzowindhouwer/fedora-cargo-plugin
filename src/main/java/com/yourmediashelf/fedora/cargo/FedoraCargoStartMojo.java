
package com.yourmediashelf.fedora.cargo;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.cargo.container.LocalContainer;


/**
 * 
 * @author Edwin Shin
 * @goal fedora-start
 * @phase pre-integration-test
 */
public class FedoraCargoStartMojo extends FedoraCargoMojo {

    public void doExecute() throws MojoExecutionException {

        getLog().info("executing FedoraCargoStartMojo");

        LocalContainer container = getContainer();
        container.start();
    }

}
