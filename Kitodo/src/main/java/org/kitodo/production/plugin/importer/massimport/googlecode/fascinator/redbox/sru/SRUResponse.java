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
package org.kitodo.production.plugin.importer.massimport.googlecode.fascinator.redbox.sru;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

/**
 * <p>
 * A trivial wrapper for response Objects, allowing access to common information
 * without having to parse continually.
 * </p>
 * 
 * @author Greg Pendlebury
 * 
 */
public class SRUResponse {
    /** Logging **/
    private static Logger logger = LogManager.getLogger(SRUResponse.class);

    /** Record counts **/
    private int totalRecords = 0;
    private int recordsReturned = 0;

    /** Results **/
    private List<Node> resultsList;

    /**
     * <p>
     * Default Constructor. Extract some basic information.
     * </p>
     * 
     * @param searchResponse
     *            A parsed DOM4J Document
     * @throws SRUException
     *             If any of the XML structure does not look like expected
     */

    @SuppressWarnings("unchecked")
    public SRUResponse(Document searchResponse) throws SRUException {
        // Results total
        Node number = searchResponse.selectSingleNode("//srw:numberOfRecords");
        if (number == null) {
            throw new SRUException("Unable to get result numbers from response XML.");
        }
        totalRecords = Integer.parseInt(number.getText());
        logger.debug("SRU Search found {} results(s)", totalRecords);

        // Results List
        if (totalRecords == 0) {
            resultsList = new ArrayList<Node>();
        } else {

            resultsList = searchResponse.selectNodes("//srw:recordData");
        }
        recordsReturned = resultsList.size();
    }

    /**
     * <p>
     * Get the number of rows returned by this search. Not the total results
     * that match the search.
     * </p>
     * 
     * @return int The number of rows returned from this search.
     */
    public int getRows() {
        return recordsReturned;
    }

    /**
     * <p>
     * Get the number of records that match this search. A subset of this will
     * be returned if the total is higher then the number of rows requested (or
     * defaulted).
     * </p>
     * 
     * @return int The number of records that match this search.
     */
    public int getTotalResults() {
        return totalRecords;
    }

    /**
     * <p>
     * Return the List of DOM4J Nodes extracted from the SRU XML wrapping it.
     * </p>
     * 
     * @return int The number of records that match this search.
     */
    public List<Node> getResults() {
        return resultsList;
    }
}
