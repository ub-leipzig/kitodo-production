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
package org.kitodo.api.validation.longtimepreservation;

/**
 * Determines the File Types supported by the module. Must be implemented by an
 * Enum.
 */
public interface FileType {

    /**
     * returns the enum values, is overwritten by enums default 'values()'
     * method
     */
    FileType[] values();

}
