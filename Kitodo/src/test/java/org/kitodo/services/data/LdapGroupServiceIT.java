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
package org.kitodo.services.data;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kitodo.MockDatabase;
import org.kitodo.data.database.beans.LdapGroup;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;

/**
 * Tests for LdapGroupService class.
 */
public class LdapGroupServiceIT {

    @BeforeClass
    public static void prepareDatabase() throws DAOException, DataException {
        MockDatabase.insertProcessesFull();
    }

    @AfterClass
    public static void cleanDatabase() {
        // MockDatabase.cleanDatabase();
    }

    @Test
    public void shouldFindLdapGroup() throws Exception {
        LdapGroupService ldapGroupService = new LdapGroupService();

        LdapGroup ldapGroup = ldapGroupService.find(1);
        boolean condition = ldapGroup.getTitle().equals("LG") && ldapGroup.getDisplayName().equals("Name");
        assertTrue("LDAP group was not found in database!", condition);
    }
}
