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
package de.sub.goobi.helper.encryption;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.bouncycastle.jce.provider.JDKMessageDigest.MD4;

import org.junit.Test;

public class MD4Test {
    static HashMap<String, byte[]> testData;

    static {
        testData = new HashMap<>();
        testData.put("Password",
                new byte[] {-92, -12, -100, 64, 101, 16, -67, -54, -74, -126, 78, -25, -61, 15, -40, 82 });
        testData.put("12345678", new byte[] {37, -105, 69, -53, 18, 58, 82, -86, 46, 105, 58, -86, -52, -94, -37, 82 });
        testData.put("GoobiPassword1234*./",
                new byte[] {82, -81, 20, -80, 72, 78, 95, 109, -12, -42, -105, -55, -29, 12, 109, 79 });
        testData.put("AreallyreallyreallylongPassword",
                new byte[] {45, -50, -121, -8, -30, 74, -60, 71, 56, 76, -127, -90, 98, -48, 80, 126 });
        testData.put("$%!--_-_/*-äöüä",
                new byte[] {-105, 38, -102, 0, -49, 47, -11, 119, 70, -87, 54, 40, 105, -94, 19, 53 });
    }

    @Test
    public void encryptTest() throws Exception {
        for (String clearText : testData.keySet()) {
            MD4 digester = new MD4();
            byte encrypted[] = digester.digest(clearText.getBytes("UnicodeLittleUnmarked"));
            assertTrue("Encrypted password doesn't match the precomputed one! ",
                    Arrays.equals(encrypted, testData.get(clearText)));
        }
    }
}
