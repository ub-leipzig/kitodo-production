/*
 *
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */
package org.goobi.production.constants;

/**
 * This class collects file names used throughout the code. TODO: Make all file
 * name String literals constants here.
 *
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class FileNames {
    /**
     * Production main configuration file name.
     */
    public static final String CONFIG_FILE = "kitodo_config.properties";

    /**
     * Configuration file that lists the digital collections available for the
     * different projects.
     */
    public static final String DIGITAL_COLLECTIONS_FILE = "kitodo_digitalCollections.xml";

    /**
     * Configuration file that lists the available library catalogues along with
     * their respective DocType mappings.
     */
    public static final String OPAC_CONFIGURATION_FILE = "kitodo_opac.xml";
}
