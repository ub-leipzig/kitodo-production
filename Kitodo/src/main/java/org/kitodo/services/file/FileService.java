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

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ShellScript;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.io.BackupFileRotation;
import org.hibernate.Hibernate;
import org.kitodo.api.filemanagement.ProcessSubType;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.helper.enums.MetadataFormat;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.RulesetService;

import ugh.dl.Fileformat;
import ugh.dl.exceptions.PreferencesException;
import ugh.dl.exceptions.WriteException;
import ugh.dl.fileformats.excel.RDFFile;
import ugh.dl.fileformats.mets.MetsMods;
import ugh.dl.fileformats.mets.XStream;

public class FileService {

    private static final Logger logger = LogManager.getLogger(FileService.class);

    private static final String TEMPORARY_FILENAME_PREFIX = "temporary_";

    private static final ServiceManager serviceManager = new ServiceManager();

    /**
     * Creates a MetaDirectory.
     *
     * @param parentFolderUri
     *            The URI, where the
     * @param directoryName
     *            the name of the directory
     * @throws IOException
     *             an IOException
     */
    public void createMetaDirectory(URI parentFolderUri, String directoryName) throws IOException {
        if (!fileExist(parentFolderUri.resolve(directoryName))) {
            ShellScript createDirScript = new ShellScript(new File(ConfigCore.getParameter("script_createDirMeta")));
            createDirScript.run(Collections.singletonList(parentFolderUri + directoryName));
        }

    }

    /**
     * Creates a Directory at a given uri with a given name.
     *
     * @param parentFolderUri
     *            The uri, where the directory should be created
     * @param directoryName
     *            the name of the directory.
     * @return The URI of the new directory.
     */
    public URI createDirectory(URI parentFolderUri, String directoryName) {
        File file = new File(mapUriToKitodoUri(parentFolderUri).getPath(), directoryName);
        file.mkdir();
        return unmapUriFromKitodoUri(file.toURI());
    }

    /**
     * Creates a Directory with a given name.
     *
     * @param directoryName
     *            the name of the directory.
     * @return The URI of the new directory.
     */
    public URI createDirectory(String directoryName) {
        File file = new File(mapUriToKitodoUri(URI.create(directoryName)));
        file.mkdir();
        return unmapUriFromKitodoUri(file.toURI());
    }

    /**
     * Creates a directory with a name given and assigns permissions to the
     * given user. Under Linux a script is used to set the file system
     * permissions accordingly. This cannot be done from within java code before
     * version 1.7.
     *
     * @param dirName
     *            Name of directory to create
     * @throws IOException
     *             If an I/O error occurs.
     */

    public void createDirectoryForUser(URI dirName, String userName) throws IOException {
        if (!serviceManager.getFileService().fileExist(dirName)) {
            ShellScript createDirScript = new ShellScript(
                    new File(ConfigCore.getParameter("script_createDirUserHome")));
            createDirScript.run(Arrays.asList(userName, dirName.toString()));
        }
    }

    /**
     * This function implements file renaming. Renaming of files is full of
     * mischief under Windows which unaccountably holds locks on files.
     * Sometimes running the JVM’s garbage collector puts things right.
     *
     * @param fileUri
     *            File to rename
     * @param newFileName
     *            New file name / destination
     * @throws IOException
     *             is thrown if the rename fails permanently
     */
    public URI renameFile(URI fileUri, String newFileName) throws IOException {

        final int SLEEP_INTERVAL_MILLIS = 20;
        final int MAX_WAIT_MILLIS = 150000; // 2½ minutes
        URI oldFileUri;
        URI newFileUri;
        int millisWaited = 0;

        if ((fileUri == null) || (newFileName == null)) {
            return null;
        }

        oldFileUri = fileUri;
        String substring = fileUri.toString().substring(0, fileUri.toString().lastIndexOf('/') + 1);
        newFileUri = URI.create(substring + newFileName);
        boolean success;

        if (!fileExist(oldFileUri)) {
            if (logger.isDebugEnabled()) {
                logger.debug("File " + fileUri.getPath() + " does not exist for renaming.");
            }
            throw new FileNotFoundException(fileUri + " does not exist for renaming.");
        }

        if (fileExist(newFileUri)) {
            String message = "Renaming of " + fileUri + " into " + newFileName + " failed: Destination exists.";
            logger.error(message);
            throw new IOException(message);
        }

        do {
            if (SystemUtils.IS_OS_WINDOWS && millisWaited == SLEEP_INTERVAL_MILLIS) {
                logger.warn("Renaming " + fileUri
                        + " failed. This is Windows. Running the garbage collector may yield good results. "
                        + "Forcing immediate garbage collection now!");
                System.gc();
            }
            success = new File(mapUriToKitodoUri(oldFileUri)).renameTo(new File(mapUriToKitodoUri(newFileUri)));
            if (!success) {
                if (millisWaited == 0 && logger.isInfoEnabled()) {
                    logger.info("Renaming " + fileUri + " failed. File may be locked. Retrying...");
                }
                try {
                    Thread.sleep(SLEEP_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    logger.warn("The thread was interrupted");
                }
                millisWaited += SLEEP_INTERVAL_MILLIS;
            }
        } while (!success && millisWaited < MAX_WAIT_MILLIS);

        if (!success) {
            logger.error("Rename " + fileUri + " failed. This is a permanent error. Giving up.");
            throw new IOException("Renaming of " + fileUri + " into " + newFileName + " failed.");
        }

        if (millisWaited > 0 && logger.isInfoEnabled()) {
            logger.info("Rename finally succeeded after" + Integer.toString(millisWaited) + " milliseconds.");
        }

        return fileUri;
    }

    /**
     * calculate all files with given file extension at specified directory
     * recursively.
     *
     * @param directory
     *            the directory to run through
     * @return number of files as Integer
     */
    public Integer getNumberOfFiles(URI directory) {
        int count = 0;
        if (isDirectory(directory)) {
            /*
             * die Unterverzeichnisse durchlaufen
             */
            ArrayList<URI> children = getSubUris(directory);
            for (URI aChildren : children) {
                if (isDirectory(aChildren)) {
                    count += getNumberOfFiles(aChildren);
                } else {
                    count += 1;
                }
            }
        }
        return count;
    }

    /**
     * calculate all files with given file extension at specified directory
     * recursively.
     *
     * @param directory
     *            the directory to run through
     * @return number of files as Integer
     */
    public Integer getNumberOfImageFiles(URI directory) {
        int count = 0;
        if (isDirectory(directory)) {
            /*
             * die Images zählen
             */
            count = getSubUris(Helper.imageNameFilter, directory).size();

            /*
             * die Unterverzeichnisse durchlaufen
             */
            ArrayList<URI> children = getSubUris(directory);
            for (URI aChildren : children) {
                count += getNumberOfImageFiles(aChildren);
            }
        }
        return count;
    }

    /**
     * Copy directory.
     *
     * @param sourceDirectory
     *            source file as uri
     * @param targetDirectory
     *            destination file as uri
     */
    public void copyDirectory(URI sourceDirectory, URI targetDirectory) throws IOException {
        sourceDirectory = mapUriToKitodoUri(sourceDirectory);
        targetDirectory = mapUriToKitodoUri(targetDirectory);
        copyDirectory(new File(sourceDirectory), new File(targetDirectory));
    }

    private void copyDirectory(File sourceDirectory, File targetDirectory) throws IOException {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        FileUtils.copyDirectory(sourceDirectory, targetDirectory, false);
    }

    /**
     * Copies a File from a given URI to a given uri.
     *
     * @param srcFile
     *            the uri to copy from
     * @param destFile
     *            the uri to copy to
     * @throws IOException
     *             if copying fails
     */
    public void copyFile(URI srcFile, URI destFile) throws IOException {
        FileUtils.copyFile(new File(mapUriToKitodoUri(srcFile)), new File(mapUriToKitodoUri(destFile)));
    }

    /**
     * Copies a file to a directory.
     *
     * @param sourceDirectory
     *            The source directory
     * @param targetDirectory
     *            the target directory
     * @throws IOException
     *             if copying fails.
     */
    public void copyFileToDirectory(URI sourceDirectory, URI targetDirectory) throws IOException {
        FileUtils.copyFileToDirectory(new File(mapUriToKitodoUri(sourceDirectory)),
                new File(mapUriToKitodoUri(targetDirectory)));
    }

    /**
     * Writes to a File at a given URI.
     *
     * @param uri
     *            The Uri, to write to.
     * @return An Outputstream to the file at the given URI.
     * @throws IOException
     *             If file cannot be accessed
     */
    public OutputStream write(URI uri) throws IOException {
        if (!fileExist(uri)) {
            new File(mapUriToKitodoUri(uri)).createNewFile();
        }
        return new FileOutputStream(new File(mapUriToKitodoUri(uri)));
    }

    /**
     * Reads a file at a given URI.
     *
     * @param uri
     *            The uri to reas
     * @return an InputStream to read from.
     * @throws IOException
     *             if File cannot be accessed.
     */
    public InputStream read(URI uri) throws IOException {
        URL url = mapUriToKitodoUri(uri).toURL();
        return url.openStream();
    }

    /**
     * Deletes a resource at a given uri.
     *
     * @param uri
     *            The uri to delete-
     * @return True, if successfull, false otherwise.
     * @throws IOException
     *             If the File cannot be accessed.
     */
    public boolean delete(URI uri) throws IOException {
        if (!fileExist(uri)) {
            return true;
        }
        File file = new File(mapUriToKitodoUri(uri));
        if (file.isFile()) {
            return file.delete();
        }
        if (file.isDirectory()) {
            FileUtils.deleteDirectory(file);
            return true;
        }
        return false;
    }

    /**
     * Checks, if a file exists.
     *
     * @param uri
     *            The uri, to check, if there is a file.
     * @return True, if the file exists.
     */
    public boolean fileExist(URI uri) {
        String path = mapUriToKitodoUri(uri).getPath();
        File file = new File(path);
        return file.exists();
    }

    /**
     * Returns the name of a file at a given uri.
     *
     * @param uri
     *            The uri, to get the filename from.
     * @return The name of the file.
     */
    public String getFileName(URI uri) {
        return FilenameUtils.getBaseName(uri.getPath());
    }

    /**
     * Returns the name of a file at a given uri.
     *
     * @param uri
     *            The uri, to get the filename from.
     * @return The name of the file.
     */
    public String getFileNameWithExtension(URI uri) {
        return FilenameUtils.getName(uri.getPath());
    }

    /**
     * Moves a directory from a given URI to a given URI
     *
     * @param sourceUri
     *            The source URI.
     * @param targetUri
     *            The target URI.
     * @throws IOException
     *             if directory cannot be accessed.
     */
    public void moveDirectory(URI sourceUri, URI targetUri) throws IOException {
        copyDirectory(sourceUri, targetUri);
        delete(sourceUri);
    }

    /**
     * Moves a file from a given URI to a given URI
     *
     * @param sourceUri
     *            The source URI.
     * @param targetUri
     *            The target URI.
     * @throws IOException
     *             if directory cannot be accessed.
     */
    public void moveFile(URI sourceUri, URI targetUri) throws IOException {
        copyFile(sourceUri, targetUri);
        delete(sourceUri);
    }

    /**
     * Lists all Files at the given Path.
     *
     * @param file
     *            The Directory to get the Files from.
     * @return an Array of Files.
     */
    private File[] listFiles(File file) {
        File[] unchecked = file.listFiles();
        return unchecked != null ? unchecked : new File[0];
    }

    private File[] listFiles(FilenameFilter filter, File file) {
        File[] unchecked = file.listFiles(filter);
        return unchecked != null ? unchecked : new File[0];
    }

    /**
     * Writes a metadata file.
     *
     * @param gdzfile
     *            the file format
     * @param process
     *            the process
     * @throws IOException
     *             if error occurs
     * @throws PreferencesException
     *             if error occurs
     * @throws WriteException
     *             if error occurs
     */
    public void writeMetadataFile(Fileformat gdzfile, Process process)
            throws IOException, PreferencesException, WriteException {
        serviceManager.getFileService().write(process.getProcessBaseUri()).close();

        RulesetService rulesetService = new RulesetService();
        Fileformat ff;
        URI metadataFileUri;

        Hibernate.initialize(process.getRuleset());
        switch (MetadataFormat.findFileFormatsHelperByName(process.getProject().getFileFormatInternal())) {
            case METS:
                ff = new MetsMods(rulesetService.getPreferences(process.getRuleset()));
                break;
            case RDF:
                ff = new RDFFile(rulesetService.getPreferences(process.getRuleset()));
                break;
            default:
                ff = new XStream(rulesetService.getPreferences(process.getRuleset()));
                break;
        }
        // createBackupFile();
        metadataFileUri = getMetadataFilePath(process);
        String temporaryMetadataFileName = getTemporaryMetadataFileName(metadataFileUri);

        ff.setDigitalDocument(gdzfile.getDigitalDocument());
        // ff.write(getMetadataFilePath());
        boolean writeResult = ff.write(temporaryMetadataFileName);
        File temporaryMetadataFile = new File(temporaryMetadataFileName);
        boolean backupCondition = writeResult && temporaryMetadataFile.exists() && (temporaryMetadataFile.length() > 0);
        if (backupCondition) {
            createBackupFile(process);
            renameFile(metadataFileUri, temporaryMetadataFileName);
            removePrefixFromRelatedMetsAnchorFilesFor(URI.create(temporaryMetadataFileName));
        }

    }

    private void removePrefixFromRelatedMetsAnchorFilesFor(URI temporaryMetadataFilename) throws IOException {
        File temporaryFile = new File(temporaryMetadataFilename);
        File directoryPath = new File(temporaryFile.getParentFile().getPath());
        for (File temporaryAnchorFile : listFiles(directoryPath)) {
            String temporaryAnchorFileName = temporaryAnchorFile.toString();
            if (temporaryAnchorFile.isFile()
                    && FilenameUtils.getBaseName(temporaryAnchorFileName).startsWith(TEMPORARY_FILENAME_PREFIX)) {
                String anchorFileName = FilenameUtils.concat(FilenameUtils.getFullPath(temporaryAnchorFileName),
                        temporaryAnchorFileName.replace(TEMPORARY_FILENAME_PREFIX, ""));
                temporaryAnchorFileName = FilenameUtils.concat(FilenameUtils.getFullPath(temporaryAnchorFileName),
                        temporaryAnchorFileName);
                renameFile(URI.create(anchorFileName), temporaryAnchorFileName);
            }
        }
    }

    // backup of meta.xml
    void createBackupFile(Process process) throws IOException {
        int numberOfBackups;

        numberOfBackups = ConfigCore.getIntParameter("numberOfMetaBackups");

        if (numberOfBackups != 0) {
            BackupFileRotation bfr = new BackupFileRotation();
            bfr.setNumberOfBackups(numberOfBackups);
            bfr.setFormat("meta.*\\.xml");
            bfr.setProcess(process);
            bfr.performBackup();
        } else {
            logger.warn("No backup configured for meta data files.");
        }
    }

    /**
     * Gets the URI of the metadata.xml of a given process.
     *
     * @param process
     *            the process to get the metadata.xml for.
     * @return The URI to the metadata.xml
     */
    public URI getMetadataFilePath(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.META_XML, null);
    }

    private String getTemporaryMetadataFileName(URI fileName) {

        File temporaryFile = new File(fileName);
        String directoryPath = temporaryFile.getParentFile().getPath();
        String temporaryFileName = TEMPORARY_FILENAME_PREFIX + temporaryFile.getName();

        return directoryPath + File.separator + temporaryFileName;
    }

    /**
     * This method is needed for migraion purposes. It mappes existing
     * filePathes to the Correct URI.
     *
     * @param process
     *            the process, the uri is needed for.
     * @return the URI.
     */
    public URI getProcessBaseUriForExistingProcess(Process process) {
        String pfad = process.getId().toString();
        // TODO: Find out, why File.seperator is not working here
        pfad = pfad.replaceAll(" ", "__") + "/";
        return URI.create(pfad);
    }

    /**
     * Get's the URI for a Process Sub-location. Possible Locations are listed
     * in ProcessSubType
     *
     * @param process
     *            the process to get the sublocation for.
     * @param processSubType
     *            The subType.
     * @param id
     *            the id of the single object (e.g. image) if null, the root
     *            folder of the sublocation is returned
     * @return The URI of the requested location
     */
    public URI getProcessSubTypeURI(Process process, ProcessSubType processSubType, String id) {

        URI processDataDirectory = serviceManager.getProcessService().getProcessDataDirectory(process);

        if (id == null) {
            id = "";
        }

        switch (processSubType) {
            case IMAGE:
                return URI.create(processDataDirectory + "image" + File.separator + id);
            case IMAGE_SOURCE:
                return URI.create(getSourceDirectory(process) + id);
            case META_XML:
                return URI.create(processDataDirectory + "meta.xml");
            case TEMPLATE:
                return URI.create(processDataDirectory + "template.xml");
            case IMPORT:
                return URI.create(processDataDirectory + "import" + File.separator + id);
            case OCR:
                return URI.create(processDataDirectory + "ocr" + File.separator);
            case OCR_PDF:
                return URI.create(processDataDirectory + "ocr" + File.separator + process.getTitle() + "_pdf"
                        + File.separator + id);
            case OCR_TXT:
                return URI.create(processDataDirectory + "ocr" + File.separator + process.getTitle() + "_txt"
                        + File.separator + id);
            case OCR_WORD:
                return URI.create(processDataDirectory + "ocr" + File.separator + process.getTitle() + "_wc"
                        + File.separator + id);
            case OCR_ALTO:
                return URI.create(processDataDirectory + "ocr" + File.separator + process.getTitle() + "_alto"
                        + File.separator + id);
            default:
                return processDataDirectory;
        }

    }

    /**
     * deletes all process directorys and their content.
     *
     * @param process
     *            the processt o delete the doirectorys for.
     * @return true, if deletion was successfull.
     */
    public boolean deleteProcessContent(Process process) {
        for (ProcessSubType processSubType : ProcessSubType.values()) {
            URI processSubTypeURI = getProcessSubTypeURI(process, processSubType, null);
            try {
                delete(processSubTypeURI);
            } catch (IOException e) {
                logger.warn("uri " + processSubTypeURI + " could not be deleted");
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the image source directory.
     *
     * @param process
     *            the process, to get the source directory for
     * @return the source directory as a string
     */
    public URI getSourceDirectory(Process process) {
        URI dir = getProcessSubTypeURI(process, ProcessSubType.IMAGE, null);
        FilenameFilter filterVerz = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name.endsWith("_" + "source"));
            }
        };
        URI sourceFolder;
        ArrayList<URI> verzeichnisse = getSubUris(filterVerz, dir);
        if (verzeichnisse == null || verzeichnisse.size() == 0) {
            sourceFolder = dir.resolve(process.getTitle() + "_source");
            if (ConfigCore.getBooleanParameter("createSourceFolder", false)) {
                createDirectory(dir, process.getTitle() + "_source");
            }
        } else {
            sourceFolder = dir.resolve(verzeichnisse.get(0));
        }

        return sourceFolder;
    }

    URI mapUriToKitodoUri(URI uri) {
        return new File(ConfigCore.getKitodoDataDirectory() + uri).toURI();
    }

    URI unmapUriFromKitodoUri(URI uri) {
        String kitodoDataDirectory = ConfigCore.getKitodoDataDirectory();
        if (uri.toString().contains(kitodoDataDirectory)) {
            String[] split = uri.toString().split(kitodoDataDirectory);
            String shortUri = split[1];
            return URI.create(shortUri);
        }

        return uri;
    }

    /**
     * gets all sub URIs of an uri.
     *
     * @param processSubTypeURI
     *            the uri, to get the subUris from.
     * @return A List of sub uris.
     */
    public ArrayList<URI> getSubUris(URI processSubTypeURI) {
        processSubTypeURI = mapUriToKitodoUri(processSubTypeURI);
        ArrayList<URI> resultList = new ArrayList<>();
        File[] files = listFiles(new File(processSubTypeURI));
        for (File file : files) {
            resultList.add(unmapUriFromKitodoUri(file.toURI()));
        }

        return resultList;
    }

    /**
     * gets all sub URIs of an uri with a given filter.
     *
     * @param filter
     *            the filter to filter the subUris
     * @param processSubTypeURI
     *            the uri, to get the subUris from.
     * @return A List of sub uris.
     */
    public ArrayList<URI> getSubUris(FilenameFilter filter, URI processSubTypeURI) {
        processSubTypeURI = mapUriToKitodoUri(processSubTypeURI);
        ArrayList<URI> resultList = new ArrayList<>();
        File[] files = listFiles(filter, new File(processSubTypeURI));
        for (File file : files) {
            resultList.add(unmapUriFromKitodoUri(file.toURI()));
        }

        return resultList;
    }

    /**
     * Creates a new File.
     *
     * @param fileName
     *            the name of the new file
     * @return the uri of the new file
     */
    public URI createResource(String fileName) {
        String path = new File(fileName).toURI().getPath();
        return URI.create(path.substring(path.lastIndexOf('/') + 1));
    }

    /**
     * Creates a resource at a given uri with a given name
     *
     * @param targetFolder
     *            the target folder.
     * @param name
     *            the name of the new resource
     * @return The uri of the created resource
     * @throws IOException
     *             if creation failed.
     */
    public URI createResource(URI targetFolder, String name) throws IOException {
        File file = new File(mapUriToKitodoUri(targetFolder).resolve(name));
        boolean newFile = file.createNewFile();
        return unmapUriFromKitodoUri(file.toURI());
    }

    /**
     * Gets the specific IMAGE sub type.
     *
     * @param process
     *            the process to get the imageDirectory for.
     * @return The uri of the Image Directory.
     */
    public URI getImagesDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.IMAGE, null);
    }

    /**
     * Gets the URI to the ocr directory.
     *
     * @param process
     *            the process tog et the ocr directory for.
     * @return the uri to the ocr directory.
     */
    public URI getOcrDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.OCR, null);
    }

    /**
     * checks, if a URI leads to a directory.
     *
     * @param dir
     *            the uri to check.
     * @return true, if it is a directory.
     */
    public boolean isDirectory(URI dir) {
        return new File(mapUriToKitodoUri(dir)).isDirectory();
    }

    /**
     * Checks if an uri is readable.
     *
     * @param uri
     *            the uri to check.
     * @return true, if it's readable, false otherwise.
     */
    public boolean canRead(URI uri) {
        return new File(uri).canRead();
    }

    /**
     * Gets the uri to the temporal directory.
     *
     * @return the uri to the temporal directory.
     */
    public URI getTemporalDirectory() {
        return URI.create(ConfigCore.getParameter("tempfolder", "/usr/local/kitodo/tmp/"));
    }

    /**
     * Checks if a resource at a given uri is a file.
     *
     * @param uri
     *            the uri to check, if there is a file.
     * @return true, if it is a file, false otherwise
     */
    public boolean isFile(URI uri) {
        return new File(uri).isFile();
    }

    /**
     * Gets the URI to the import directory.
     *
     * @param process
     *            the process to get the import directory for.
     * @return the uri of the import directory
     */
    public URI getImportDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.IMPORT, null);
    }

    /**
     * Gets the URI to the text directory.
     *
     * @param process
     *            the process to get the text directory for.
     * @return the uri of the text directory
     */
    public URI getTxtDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.OCR_TXT, null);
    }

    /**
     * Gets the URI to the pdf directory.
     *
     * @param process
     *            the process to get the pdf directory for.
     * @return the uri of the pdf directory
     */
    public URI getPdfDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.OCR_PDF, null);
    }

    /**
     * Gets the URI to the alto directory.
     *
     * @param process
     *            the process to get the alto directory for.
     * @return the uri of the alto directory
     */
    public URI getAltoDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.OCR_ALTO, null);
    }

    /**
     * Gets the URI to the word directory.
     *
     * @param process
     *            the process to get the word directory for.
     * @return the uri of the word directory
     */
    public URI getWordDirectory(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.OCR_WORD, null);
    }

    /**
     * Gets the URI to the template file.
     *
     * @param process
     *            the process to get the template file for.
     * @return the uri of the template file
     */
    public URI getTemplateFile(Process process) {
        return getProcessSubTypeURI(process, ProcessSubType.TEMPLATE, null);
    }

    public void writeMetadataAsTemplateFile(Fileformat inFile, Process process)
            throws WriteException, PreferencesException {
        inFile.write(getTemplateFile(process).toString());
    }

    /**
     * Creates a symbolic link.
     *
     * @param targetUri
     *            The target URI for the link.
     * @param homeUri
     *            The home URI.
     * @return true, if link creation was successfull.
     */
    public boolean createSymLink(URI targetUri, URI homeUri) {
        return true;
    }

    /**
     * Delets a symbolik link.
     *
     * @param homeUri
     *            The uri of the home folder, where the link should be deleted.
     * @return true, if deletion was successull.
     */
    public boolean deleteSymLink(URI homeUri) {
        String command = ConfigCore.getParameter("script_deleteSymLink");
        ShellScript deleteSymLinkScript;
        try {
            deleteSymLinkScript = new ShellScript(new File(command));
            deleteSymLinkScript.run(Collections.singletonList(homeUri.toString()));
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException in deleteSymLink()", e);
            Helper.setFehlerMeldung("Couldn't find script file, error", e.getMessage());
            return false;
        } catch (IOException e) {
            logger.error("IOException in deleteSymLink()", e);
            Helper.setFehlerMeldung("Aborted deleteSymLink(), error", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Returns the version used in the core code, without direkt File mapping.
     * 
     * @param uri
     *            the uri to unmapp
     * @return the (shorter) intern uri
     */
    public URI getInternUri(URI uri) {
        return unmapUriFromKitodoUri(uri);
    }
}
