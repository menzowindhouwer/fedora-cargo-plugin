/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also
 * available online at http://fedora-commons.org/license/).
 */
package com.yourmediashelf.sandbox.fcfg;

/**
 * Constants of general utility.
 *
 */
public interface Constants {

    /**
     * The "Fedora Home" directory.
     * <p>
     * This is normally derived from the <code>FEDORA_HOME</code> environment
     * variable, but if defined, the <code>fedora.home</code> system property
     * will be used instead.
     * </p>
     */
    public static final String FEDORA_HOME = FedoraHome.getValue();


    //---
    // Static helpers
    //---

    /**
     * Utility to determine and provide the value of the "Fedora Home" constant.
     */
    static class FedoraHome {

        private static String value;

        /**
         * Determines the value of "Fedora Home" based on the
         * <code>servlet.fedora.home</code> system property (checked first)
         * <code>fedora.home</code> system property (checked next) or the
         * <code>FEDORA_HOME</code> environment variable (checked last).
         * <p>
         * Once successfully determined, the value is guaranteed not to change
         * during the life of the application.
         *
         * @returns the value, or <code>null</code> if undefined in any way.
         */
        public static final String getValue() {
            if (value == null) {
                if (System.getProperty("servlet.fedora.home") != null) {
                    value = System.getProperty("servlet.fedora.home");
                } else if (System.getProperty("fedora.home") != null) {
                    value = System.getProperty("fedora.home");
                } else {
                    value = System.getenv("FEDORA_HOME");
                }
            }
            return value;
        }
    }

}
