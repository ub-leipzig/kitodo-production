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
package org.goobi.io;

import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.data.database.beans.Process;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.file.FileService;

/**
 * Creates backup for files in a given directory that match a regular
 * expression.
 *
 * <p>
 * All backup files are named by the original file with a number appended. The
 * bigger the number, the older the backup. A specified maximum number of backup
 * files are generated:
 * </p>
 * 
 * <pre>
 * file.xml	// would be the original
 * file.xml.1	// the latest backup
 * file.xml.2	// an older backup
 * ...
 * file.xml.6	// the oldest backup, if maximum number was 6
 * </pre>
 */
public class BackupFileRotation {

    private static final Logger logger = LogManager.getLogger(BackupFileRotation.class);

    private int numberOfBackups;
    private String format;
    private Process process;

    private final ServiceManager serviceManager = new ServiceManager();
    public final FileService fileService = serviceManager.getFileService();

    /**
     * Start the configured backup.
     *
     * <p>
     * If the maximum backup count is less then 1, nothing happens.
     * </p>
     *
     * @throws IOException
     *             if a file system operation fails
     */
    public void performBackup() throws IOException {
        ArrayList<URI> metaFiles;

        if (numberOfBackups < 1) {
            return;
        }

        metaFiles = generateBackupBaseNameFileList(format, process);

        if (metaFiles.size() < 1) {
            if (logger.isInfoEnabled()) {
                logger.info("No files matching format '" + format + "' in directory "
                        + serviceManager.getProcessService().getProcessDataDirectory(process) + " found.");
            }
            return;
        }

        for (URI metaFile : metaFiles) {
            createBackupForFile(metaFile);
        }
    }

    /**
     * Set the number of backup files to create for each individual original
     * file.
     *
     * @param numberOfBackups
     *            Maximum number of backup files
     */
    public void setNumberOfBackups(int numberOfBackups) {
        this.numberOfBackups = numberOfBackups;
    }

    /**
     * Set file name matching pattern for original files to create backup files
     * for.
     *
     * @param format
     *            Java regular expression string.
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Sets the process.
     *
     * @param process
     *            the process
     */
    public void setProcess(Process process) {
        this.process = process;
    }

    private void createBackupForFile(URI fileName) throws IOException {
        rotateBackupFilesFor(fileName);

        String newName = fileService.getFileNameWithExtension(fileName) + ".1";
        fileService.renameFile(fileName, newName);
    }

    private void rotateBackupFilesFor(URI fileName) throws IOException {

        URI oldest = URI.create(fileName + "." + numberOfBackups);
        if (fileService.fileExist(oldest)) {
            boolean deleted = fileService.delete(oldest);
            if (!deleted) {
                String message = "Could not delete " + oldest.toString();
                logger.error(message);
                throw new IOException(message);
            }
        }

        for (int count = numberOfBackups; count > 1; count--) {
            URI oldName = URI.create(fileName + "." + (count - 1));
            String newName = fileService.getFileNameWithExtension(fileName) + "." + count;
            try {
                fileService.renameFile(oldName, newName);
            } catch (FileNotFoundException oldNameNotYetPresent) {
                if (logger.isDebugEnabled()) {
                    logger.debug(oldName + " does not yet exist >>> nothing to do");
                }
            }
        }
    }

    private ArrayList<URI> generateBackupBaseNameFileList(String filterFormat, Process process) {

        ArrayList<URI> filteredUris = new ArrayList<>();
        FilenameFilter filter = new FileListFilter(filterFormat);

        URI processDataDirectory = serviceManager.getProcessService().getProcessDataDirectory(process);
        ArrayList<URI> subUris = fileService.getSubUris(filter, processDataDirectory);
        for (URI uri : subUris) {
            filteredUris.add(uri);
        }
        return filteredUris;
    }

}
