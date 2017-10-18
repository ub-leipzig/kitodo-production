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
package org.kitodo.data.exceptions;

/**
 * Exception for wrapping CustomResponseException, DAOException and IOException.
 */
public class DataException extends Exception {

    private static final long serialVersionUID = 1987853363232807999L;

    public DataException(Exception e) {
        super(e);
    }

    public DataException(String message) {
        super(message);
    }
}
