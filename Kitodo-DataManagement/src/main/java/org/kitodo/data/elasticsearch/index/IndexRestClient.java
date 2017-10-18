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
package org.kitodo.data.elasticsearch.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.kitodo.data.elasticsearch.KitodoRestClient;
import org.kitodo.data.elasticsearch.exceptions.CustomResponseException;

/**
 * Implementation of Elastic Search REST Client for index package.
 */
public class IndexRestClient extends KitodoRestClient {

    private static final Logger logger = LogManager.getLogger(IndexRestClient.class);

    /**
     * Add document to the index. This method will be used for add or update of
     * single document.
     *
     * @param entity
     *            with document which is going to be indexed
     * @param id
     *            of document - equal to the id from table in database
     * @return status code of the response from the server
     */
    public boolean addDocument(HttpEntity entity, Integer id) throws IOException, CustomResponseException {
        Response indexResponse = restClient.performRequest("PUT",
                "/" + this.getIndex() + "/" + this.getType() + "/" + id, Collections.<String, String>emptyMap(),
                entity);
        int statusCode = processStatusCode(indexResponse.getStatusLine());
        return statusCode == 200 || statusCode == 201;
    }

    /**
     * Add list of documents to the index. This method will be used for add
     * whole table to the index. It performs asynchronous request.
     *
     * @param documentsToIndex
     *            list of json documents to the index
     */
    public String addType(HashMap<Integer, HttpEntity> documentsToIndex)
            throws InterruptedException, CustomResponseException {
        final CountDownLatch latch = new CountDownLatch(documentsToIndex.size());
        final ArrayList<String> output = new ArrayList<>();

        for (Map.Entry<Integer, HttpEntity> entry : documentsToIndex.entrySet()) {
            restClient.performRequestAsync("PUT", "/" + this.getIndex() + "/" + this.getType() + "/" + entry.getKey(),
                    Collections.<String, String>emptyMap(), entry.getValue(), new ResponseListener() {
                        @Override
                        public void onSuccess(Response response) {
                            output.add(response.toString());
                            latch.countDown();
                        }

                        @Override
                        public void onFailure(Exception exception) {
                            output.add(exception.getMessage());
                            latch.countDown();
                        }
                    });
        }
        latch.await();
        filterAsynchronousResponses(output);
        return output.toString();
    }

    /**
     * Delete document from the index.
     *
     * @param id
     *            of the document
     * @return status code of the response from server
     */
    public boolean deleteDocument(Integer id) throws IOException, CustomResponseException {
        boolean result = false;
        try {
            Response indexResponse = restClient.performRequest("DELETE",
                    "/" + this.getIndex() + "/" + this.getType() + "/" + id);
            result = processStatusCode(indexResponse.getStatusLine()) == 200;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                logger.debug(e.getMessage());
                result = true;
            } else {
                throw new CustomResponseException(e.getMessage());
            }
        }
        return result;
    }

    /**
     * Delete all documents of certain type from the index.
     *
     * @return response from server
     */
    public boolean deleteType() throws IOException, CustomResponseException {
        String query = "{\n" + "  \"query\": {\n" + "    \"match_all\": {}\n" + "  }\n" + "}";
        HttpEntity entity = new NStringEntity(query, ContentType.APPLICATION_JSON);
        Response indexResponse = restClient.performRequest("POST",
                "/" + this.getIndex() + "/" + this.getType() + "/_delete_by_query?conflicts=proceed",
                Collections.<String, String>emptyMap(), entity);
        return processStatusCode(indexResponse.getStatusLine()) == 200;
    }

    /**
     * Delete the whole index. Used for cleaning after tests!
     *
     * @return status code of the response from server
     */
    public boolean deleteIndex() throws IOException, CustomResponseException {
        Response indexResponse = restClient.performRequest("DELETE", "/kitodo");
        return processStatusCode(indexResponse.getStatusLine()) == 200;
    }

    private void filterAsynchronousResponses(ArrayList<String> responses) throws CustomResponseException {
        if (responses.size() > 0) {
            for (String response : responses) {
                if (response == null || response.equals("")) {
                    throw new CustomResponseException(
                            "ElasticSearch failed to add one or more documents for unknown reason!");
                } else {
                    if (!(response.contains("HTTP/1.1 200") || response.contains("HTTP/1.1 201"))) {
                        throw new CustomResponseException(
                                "ElasticSearch failed to add one or more documents! Reason: " + response);
                    }
                }
            }
        } else {
            throw new CustomResponseException(
                    "ElasticSearch failed to add all documents for unknown reason!");
        }
    }

    private int processStatusCode(StatusLine statusLine) throws CustomResponseException {
        int statusCode = statusLine.getStatusCode();
        if (statusCode >= 400 && statusCode < 452) {
            throw new CustomResponseException("Client error: " + statusLine.toString());
        } else if (statusCode >= 500 && statusCode < 512) {
            throw new CustomResponseException("Server error: " + statusLine.toString());
        }
        return statusCode;
    }
}
