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

import com.sun.research.ws.wadl.HTTPMethods;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.kitodo.data.database.beans.BaseBean;
import org.kitodo.data.elasticsearch.Index;
import org.kitodo.data.elasticsearch.exceptions.CustomResponseException;
import org.kitodo.data.elasticsearch.index.type.BaseType;

/**
 * Implementation of Elastic Search Indexer for index package.
 */
public class Indexer<T extends BaseBean, S extends BaseType> extends Index {

    private HTTPMethods method;

    /**
     * Constructor for indexer with type names equal to table names.
     *
     * @param beanClass
     *            as Class
     */
    public Indexer(Class<?> beanClass) {
        super(beanClass);
    }

    /**
     * Constructor for indexer with type names not equal to table names.
     *
     * @param type
     *            as String
     */
    public Indexer(String type) {
        super(type);
    }

    /**
     * Perform request depending on given parameters of HTTPMethods.
     *
     * @param baseBean
     *            bean object which will be added or deleted from index
     * @param baseType
     *            type on which will be called method createDocument()
     * @return response from the server
     */
    @SuppressWarnings("unchecked")
    public String performSingleRequest(T baseBean, S baseType) throws IOException, CustomResponseException {
        IndexRestClient restClient = initiateRestClient();
        String response;

        if (method == HTTPMethods.PUT) {
            HttpEntity document = baseType.createDocument(baseBean);
            response = String.valueOf(restClient.addDocument(document, baseBean.getId()));
        } else if (method == HTTPMethods.DELETE) {
            response = String.valueOf(restClient.deleteDocument(baseBean.getId()));
        } else {
            response = "Incorrect HTTP method!";
        }

        restClient.closeClient();

        return response;
    }

    /**
     * Perform delete request depending on given id of the bean.
     *
     * @param beanId
     *            response from the server
     */
    public String performSingleRequest(Integer beanId) throws IOException, CustomResponseException {
        IndexRestClient restClient = initiateRestClient();
        String response;

        if (method == HTTPMethods.DELETE) {
            response = String.valueOf(restClient.deleteDocument(beanId));
        } else {
            response = "Incorrect HTTP method!";
        }

        restClient.closeClient();

        return response;
    }

    /**
     * This function is called directly by the administrator of the system.
     *
     * @return response from the server
     * @throws InterruptedException
     *             add description
     */
    @SuppressWarnings("unchecked")
    public String performMultipleRequests(List<T> baseBeans, S baseType)
            throws IOException, InterruptedException, CustomResponseException {
        IndexRestClient restClient = initiateRestClient();
        String response;

        if (method == HTTPMethods.PUT) {
            HashMap<Integer, HttpEntity> documents = baseType.createDocuments(baseBeans);
            response = restClient.addType(documents);
        } else if (method == HTTPMethods.DELETE) {
            response = String.valueOf(restClient.deleteType());
        } else {
            response = "Incorrect HTTP method!";
        }

        restClient.closeClient();

        return response;
    }

    private IndexRestClient initiateRestClient() {
        IndexRestClient restClient = new IndexRestClient();
        restClient.initiateClient();
        restClient.setIndex(index);
        restClient.setType(type);
        return restClient;
    }

    /**
     * Get type of method which will be used during performing request.
     *
     * @return method for request
     */
    public HTTPMethods getMethod() {
        return method;
    }

    /**
     * Set up type of method which will be used during performing request.
     *
     * @param method
     *            Determines if we want to add (update) or delete document -
     *            true add, false delete
     */
    public void setMethod(HTTPMethods method) {
        this.method = method;
    }
}
