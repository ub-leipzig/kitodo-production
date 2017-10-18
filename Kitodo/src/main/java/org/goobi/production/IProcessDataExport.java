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
package org.goobi.production;

import java.io.IOException;
import java.io.OutputStream;

import org.kitodo.data.database.beans.Process;

public interface IProcessDataExport {

    abstract void startExport(Process process, OutputStream os, String xsltfile) throws IOException;

}
