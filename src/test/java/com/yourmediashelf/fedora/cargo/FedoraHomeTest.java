
package com.yourmediashelf.fedora.cargo;

import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class FedoraHomeTest {

    @Test
    public void test() throws Exception {
        InputStream testProps =
                this.getClass().getClassLoader().getResourceAsStream(
                        "test-install.properties");
        assertNotNull(testProps);

        InputStream mavenProps =
                this.getClass().getClassLoader().getResourceAsStream(
                        "test-maven.properties");
        assertNotNull(mavenProps);

        Properties baseProps = new Properties();
        baseProps.load(mavenProps);

        FedoraHomeMojo.applyFilters(testProps, baseProps);
    }

}
