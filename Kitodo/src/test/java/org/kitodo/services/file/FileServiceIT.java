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
package org.kitodo.services.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileServiceIT {

    @BeforeClass
    public static void setUp() {
        FileService fileService = new FileService();
        fileService.createDirectory(URI.create(""), "fileServiceTest");
    }

    @AfterClass
    public static void tearDown() throws IOException {
        FileService fileService = new FileService();
        fileService.delete(URI.create("fileServiceTest"));
    }

    /**
     * This is a very long running Test. It's testing, what happens if the
     * MAX_WAIT_MILLIS is reached in the rename method. This is actually set to
     * 2,5 minutes, thats why I excludet it to an IT test.
     *
     * @throws IOException
     */
    @Test
    public void testRenameFileWithLockedFile() throws IOException {
        FileService fileService = new FileService();

        URI oldUri = fileService.createResource(URI.create("fileServiceTest"), "oldName.xml");
        Assert.assertTrue(fileService.fileExist(oldUri));
        // Open stream to file and lock it, so it cannot be renamed
        FileOutputStream outputStream = new FileOutputStream(new File(fileService.mapUriToKitodoUri(oldUri)));
        outputStream.getChannel().lock();

        try {
            fileService.renameFile(oldUri, "newName.xml");
        } catch (IOException e) {
            URI newUri = URI.create("fileServiceTest/newName.xml");
            Assert.assertFalse(fileService.fileExist(newUri));
            Assert.assertTrue(fileService.fileExist(oldUri));

            outputStream.close();
        }

    }
}
