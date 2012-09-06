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
