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
package org.kitodo.data.elasticsearch.index.type;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.kitodo.data.database.beans.Docket;

/**
 * Test class for DocketType.
 */
public class DocketTypeTest {

    private static List<Docket> prepareData() {

        List<Docket> dockets = new ArrayList<>();

        Docket firstDocket = new Docket();
        firstDocket.setId(1);
        firstDocket.setTitle("default");
        firstDocket.setFile("docket.xsl");
        dockets.add(firstDocket);

        Docket secondDocket = new Docket();
        secondDocket.setId(2);
        secondDocket.setTitle("custom");
        secondDocket.setFile("docket_custom.xsl");
        dockets.add(secondDocket);

        return dockets;
    }

    @Test
    public void shouldCreateDocument() throws Exception {
        DocketType docketType = new DocketType();
        Docket docket = prepareData().get(0);

        HttpEntity document = docketType.createDocument(docket);
        JSONParser parser = new JSONParser();
        JSONObject actual = (JSONObject) parser.parse(EntityUtils.toString(document));
        JSONObject excepted = (JSONObject) parser.parse("{\"title\":\"default\",\"file\":\"docket.xsl\"}");
        assertEquals("Docket JSONObject doesn't match to given JSONObject!", excepted, actual);
    }

    @Test
    public void shouldCreateDocuments() {
        DocketType docketType = new DocketType();

        List<Docket> dockets = prepareData();
        HashMap<Integer, HttpEntity> documents = docketType.createDocuments(dockets);
        assertEquals("HashMap of documents doesn't contain given amount of elements!", 2, documents.size());
    }
}
