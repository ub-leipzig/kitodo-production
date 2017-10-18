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
package de.sub.goobi.helper;

import static junit.framework.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kitodo.services.file.FileService;

public class FilesystemHelperTest {

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() throws Exception {
        FileService fileService = new FileService();
        fileService.delete(URI.create("old.xml"));
        fileService.delete(URI.create("new.xml"));
    }

    @Test(expected = java.io.FileNotFoundException.class)
    public void renamingOfNonExistingFileShouldThrowFileNotFoundException() throws IOException {
        FileService fileService = new FileService();
        URI oldFileName = new File("old.xml").toURI();
        String newFileName = "new.xml";

        fileService.renameFile(oldFileName, newFileName);
    }

    @Test
    public void shouldRenameAFile() throws IOException {
        FileService fileService = new FileService();
        URI file = createFile("old.xml");
        fileService.renameFile(file, "new.xml");
        assertFileExists("new.xml");
        assertFileNotExists("old.xml");
    }

    @Test
    public void nothingHappensIfSourceFilenameIsNotSet() throws IOException {
        FileService fileService = new FileService();
        fileService.renameFile(null, "new.xml");
        assertFileNotExists("new.xml");
    }

    @Test
    public void nothingHappensIfTargetFilenameIsNotSet() throws IOException {
        URI file = createFile("old.xml");
        FileService fileService = new FileService();
        fileService.renameFile(file, null);
        assertFileNotExists("new.xml");
    }

    private void assertFileExists(String fileName) {
        FileService fileService = new FileService();
        if (!fileService.fileExist(URI.create(fileName))) {
            fail("File " + fileName + " does not exist.");
        }
    }

    private void assertFileNotExists(String fileName) {
        File newFile = new File(fileName);
        if (newFile.exists()) {
            fail("File " + fileName + " should not exist.");
        }
    }

    private URI createFile(String fileName) throws IOException {
        FileService fileService = new FileService();
        URI resource = fileService.createResource(fileName);
        fileService.write(resource).write(4);
        return resource;
    }

    private void deleteFile(String fileName) {
        File testFile = new File(fileName);
        testFile.delete();
    }
}
