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
package org.kitodo.production.plugin.opac.pica;

class ConfigOpacCatalogueBeautifierElement {
    private final String tag;
    private final String subtag;
    private final String value;
    private final String mode;

    ConfigOpacCatalogueBeautifierElement(String tag, String subtag, String value, String mode) {
        this.tag = tag;
        this.subtag = subtag;
        this.value = value;
        this.mode = mode;
    }

    String getTag() {
        return tag;
    }

    String getSubtag() {
        return subtag;
    }

    String getValue() {
        return value;
    }

    String getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return tag + " - " + subtag + " : " + value;
    }
}
