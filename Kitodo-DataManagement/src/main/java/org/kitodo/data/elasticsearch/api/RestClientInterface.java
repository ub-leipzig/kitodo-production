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
package org.kitodo.data.elasticsearch.api;

import java.io.IOException;

/**
 * Interface for REST clients.
 */
public interface RestClientInterface {

    void initiateClient();

    String getServerInformation() throws IOException;

    void closeClient() throws IOException;
}
