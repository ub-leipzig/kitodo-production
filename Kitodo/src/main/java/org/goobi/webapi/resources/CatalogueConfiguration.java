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
package org.goobi.webapi.resources;

import de.unigoettingen.sub.search.opac.ConfigOpac;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * The CatalogueConfiguration class provides the Jersey API URL pattern
 * ${SERVLET_CONTEXT}/rest/catalogueConfiguration which returns the major data
 * from the ConfigOpac() configuration class in XML or JSON format.
 *
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
@Path("/catalogueConfiguration")
public class CatalogueConfiguration {

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public ConfigOpac getCatalogueConfiguration() {
        return new ConfigOpac();
    }
}
