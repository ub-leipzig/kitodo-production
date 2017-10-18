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
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.UserGroup;

/**
 * Test class for UserType.
 */
public class UserTypeTest {

    private static List<User> prepareData() {

        List<User> users = new ArrayList<>();
        List<UserGroup> userGroups = new ArrayList<>();
        List<Property> properties = new ArrayList<>();

        UserGroup firstUserGroup = new UserGroup();
        firstUserGroup.setId(1);
        userGroups.add(firstUserGroup);

        UserGroup secondUserGroup = new UserGroup();
        secondUserGroup.setId(2);
        userGroups.add(secondUserGroup);

        Property firstProperty = new Property();
        firstProperty.setId(1);
        properties.add(firstProperty);

        Property secondProperty = new Property();
        secondProperty.setId(2);
        properties.add(secondProperty);

        User firstUser = new User();
        firstUser.setId(1);
        firstUser.setName("Jan");
        firstUser.setSurname("Kowalski");
        firstUser.setLogin("jkowalski");
        firstUser.setActive(true);
        firstUser.setLocation("Dresden");
        users.add(firstUser);

        User secondUser = new User();
        secondUser.setId(2);
        secondUser.setName("Anna");
        secondUser.setSurname("Nowak");
        secondUser.setLogin("anowak");
        secondUser.setActive(true);
        secondUser.setLocation("Berlin");
        secondUser.setUserGroups(userGroups);
        secondUser.setProperties(properties);
        users.add(secondUser);

        User thirdUser = new User();
        thirdUser.setId(3);
        thirdUser.setName("Peter");
        thirdUser.setSurname("Müller");
        thirdUser.setLogin("pmueller");
        thirdUser.setProperties(properties);
        users.add(thirdUser);

        return users;
    }

    @Test
    public void shouldCreateDocument() throws Exception {
        UserType userType = new UserType();
        JSONParser parser = new JSONParser();

        User user = prepareData().get(0);
        HttpEntity document = userType.createDocument(user);
        JSONObject actual = (JSONObject) parser.parse(EntityUtils.toString(document));
        JSONObject expected = (JSONObject) parser.parse("{\"ldapLogin\":null,\"userGroups\":[],"
                + "\"surname\":\"Kowalski\",\"name\":\"Jan\",\"metadataLanguage\":null,\"login\":\"jkowalski\","
                + "\"active\":\"true\",\"location\":\"Dresden\",\"properties\":[]}");
        assertEquals("User JSONObject doesn't match to given JSONObject!", expected, actual);

        user = prepareData().get(1);
        document = userType.createDocument(user);
        actual = (JSONObject) parser.parse(EntityUtils.toString(document));
        expected = (JSONObject) parser.parse("{\"ldapLogin\":null,\"userGroups\":[{\"id\":1},{\"id\":2}],"
                + "\"surname\":\"Nowak\",\"name\":\"Anna\",\"metadataLanguage\":null,\"active\":\"true\","
                + "\"location\":\"Berlin\",\"login\":\"anowak\",\"properties\":[{\"id\":1},{\"id\":2}]}");
        assertEquals("User JSONObject doesn't match to given JSONObject!", expected, actual);

        user = prepareData().get(2);
        document = userType.createDocument(user);
        actual = (JSONObject) parser.parse(EntityUtils.toString(document));
        expected = (JSONObject) parser.parse("{\"login\":\"pmueller\",\"ldapLogin\":null,\"userGroups\":[],"
                + "\"surname\":\"Müller\",\"name\":\"Peter\",\"metadataLanguage\":null,\"active\":\"true\","
                + "\"location\":null,\"properties\":[{\"id\":1},{\"id\":2}]}");
        assertEquals("User JSONObject doesn't match to given JSONObject!", expected, actual);
    }

    @Test
    public void shouldCreateDocuments() {
        UserType userType = new UserType();

        List<User> users = prepareData();
        HashMap<Integer, HttpEntity> documents = userType.createDocuments(users);
        assertEquals("HashMap of documents doesn't contain given amount of elements!", 3, documents.size());
    }
}
