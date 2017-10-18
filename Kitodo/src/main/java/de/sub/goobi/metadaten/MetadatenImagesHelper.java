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
package de.sub.goobi.metadaten;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.InvalidImagesException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;

import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.filemanagement.ProcessSubType;
import org.kitodo.data.database.beans.Process;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.file.FileService;

import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;
import ugh.dl.RomanNumeral;
import ugh.dl.exceptions.ContentFileNotLinkedException;
import ugh.dl.exceptions.DocStructHasNoTypeException;
import ugh.dl.exceptions.MetadataTypeNotAllowedException;
import ugh.dl.exceptions.TypeNotAllowedAsChildException;
import ugh.dl.exceptions.TypeNotAllowedForParentException;

public class MetadatenImagesHelper {
    private static final Logger logger = LogManager.getLogger(MetadatenImagesHelper.class);
    private final Prefs myPrefs;
    private final DigitalDocument mydocument;
    private int myLastImage = 0;
    private static final ServiceManager serviceManager = new ServiceManager();
    private static final FileService fileService = serviceManager.getFileService();

    public MetadatenImagesHelper(Prefs inPrefs, DigitalDocument inDocument) {
        this.myPrefs = inPrefs;
        this.mydocument = inDocument;
    }

    /**
     * Markus baut eine Seitenstruktur aus den vorhandenen Images --- Steps -
     * ---- Validation of images compare existing number images with existing
     * number of page DocStructs if it is the same don't do anything if
     * DocStructs are less add new pages to physicalDocStruct if images are less
     * delete pages from the end of pyhsicalDocStruct.
     */
    public void createPagination(Process process, URI directory) throws TypeNotAllowedForParentException, IOException {
        DocStruct physicaldocstruct = this.mydocument.getPhysicalDocStruct();

        DocStruct log = this.mydocument.getLogicalDocStruct();
        while (log.getType().getAnchorClass() != null && log.getAllChildren() != null
                && log.getAllChildren().size() > 0) {
            log = log.getAllChildren().get(0);
        }

        /*
         * der physische Baum wird nur angelegt, wenn er noch nicht existierte
         */
        if (physicaldocstruct == null) {
            DocStructType dst = this.myPrefs.getDocStrctTypeByName("BoundBook");
            physicaldocstruct = this.mydocument.createDocStruct(dst);

            /*
             * Probleme mit dem FilePath
             */
            MetadataType metadataTypeForPath = this.myPrefs.getMetadataTypeByName("pathimagefiles");
            try {
                Metadata mdForPath = new Metadata(metadataTypeForPath);
                if (SystemUtils.IS_OS_WINDOWS) {
                    mdForPath.setValue(
                            "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process));
                } else {
                    mdForPath.setValue(
                            "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process));
                }
                physicaldocstruct.addMetadata(mdForPath);
            } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e1) {
            }
            this.mydocument.setPhysicalDocStruct(physicaldocstruct);
        }

        if (directory == null) {
            checkIfImagesValid(process.getTitle(),
                    serviceManager.getProcessService().getImagesTifDirectory(true, process));
        } else {
            checkIfImagesValid(process.getTitle(),
                    fileService.getProcessSubTypeURI(process, ProcessSubType.IMAGE, null).resolve(directory));
        }

        /*
         * retrieve existing pages/images
         */
        DocStructType newPage = this.myPrefs.getDocStrctTypeByName("page");
        List<DocStruct> oldPages = physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*");
        if (oldPages == null) {
            oldPages = new ArrayList<DocStruct>();
        }

        /*
         * add new page/images if necessary
         */

        if (oldPages.size() == this.myLastImage) {
            return;
        }

        String defaultPagination = ConfigCore.getParameter("MetsEditorDefaultPagination", "uncounted");
        Map<String, DocStruct> assignedImages = new HashMap<String, DocStruct>();
        List<DocStruct> pageElementsWithoutImages = new ArrayList<>();
        List<URI> imagesWithoutPageElements = new ArrayList<>();

        if (physicaldocstruct.getAllChildren() != null && !physicaldocstruct.getAllChildren().isEmpty()) {
            for (DocStruct page : physicaldocstruct.getAllChildren()) {
                if (page.getImageName() != null) {
                    URI imageFile = null;
                    if (directory == null) {
                        imageFile = serviceManager.getProcessService().getImagesTifDirectory(true, process)
                                .resolve(page.getImageName());
                    } else {
                        imageFile = fileService.getProcessSubTypeURI(process, ProcessSubType.IMAGE,
                                directory + page.getImageName());
                    }
                    if (fileService.fileExist(imageFile)) {
                        assignedImages.put(page.getImageName(), page);
                    } else {
                        try {
                            page.removeContentFile(page.getAllContentFiles().get(0));
                            pageElementsWithoutImages.add(page);
                        } catch (ContentFileNotLinkedException e) {
                            logger.error(e);
                        }
                    }
                } else {
                    pageElementsWithoutImages.add(page);

                }
            }

        }
        try {
            List<URI> imageNamesInMediaFolder = getDataFiles(process);
            if (imageNamesInMediaFolder != null) {
                for (URI imageName : imageNamesInMediaFolder) {
                    if (!assignedImages.containsKey(imageName)) {
                        imagesWithoutPageElements.add(imageName);
                    }
                }
            }
        } catch (InvalidImagesException e1) {
            logger.error(e1);
        }

        // handle possible cases

        // case 1: existing pages but no images (some images are removed)
        if (!pageElementsWithoutImages.isEmpty() && imagesWithoutPageElements.isEmpty()) {
            for (DocStruct pageToRemove : pageElementsWithoutImages) {
                physicaldocstruct.removeChild(pageToRemove);
                List<Reference> refs = new ArrayList<Reference>(pageToRemove.getAllFromReferences());
                for (ugh.dl.Reference ref : refs) {
                    ref.getSource().removeReferenceTo(pageToRemove);
                }
            }
        }
        // case 2: no page docs but images (some images are added)
        else if (pageElementsWithoutImages.isEmpty() && !imagesWithoutPageElements.isEmpty()) {
            int currentPhysicalOrder = assignedImages.size();
            for (URI newImage : imagesWithoutPageElements) {
                DocStruct dsPage = this.mydocument.createDocStruct(newPage);
                try {
                    // physical page no
                    physicaldocstruct.addChild(dsPage);
                    MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
                    Metadata mdTemp = new Metadata(mdt);
                    mdTemp.setValue(String.valueOf(++currentPhysicalOrder));
                    dsPage.addMetadata(mdTemp);

                    // logical page no
                    mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
                    mdTemp = new Metadata(mdt);

                    if (defaultPagination.equalsIgnoreCase("arabic")) {
                        mdTemp.setValue(String.valueOf(currentPhysicalOrder));
                    } else if (defaultPagination.equalsIgnoreCase("roman")) {
                        RomanNumeral roman = new RomanNumeral();
                        roman.setValue(currentPhysicalOrder);
                        mdTemp.setValue(roman.getNumber());
                    } else {
                        mdTemp.setValue("uncounted");
                    }

                    dsPage.addMetadata(mdTemp);
                    log.addReferenceTo(dsPage, "logical_physical");

                    // image name
                    ContentFile cf = new ContentFile();
                    if (SystemUtils.IS_OS_WINDOWS) {
                        cf.setLocation("file:/"
                                + serviceManager.getProcessService().getImagesTifDirectory(false, process) + newImage);
                    } else {
                        cf.setLocation("file://"
                                + serviceManager.getProcessService().getImagesTifDirectory(false, process) + newImage);
                    }
                    dsPage.addContentFile(cf);

                } catch (TypeNotAllowedAsChildException e) {
                    logger.error(e);
                } catch (MetadataTypeNotAllowedException e) {
                    logger.error(e);
                }
            }
        }

        // case 3: empty page docs and unassinged images
        else {
            for (DocStruct page : pageElementsWithoutImages) {
                if (!imagesWithoutPageElements.isEmpty()) {
                    // assign new image name to page
                    URI newImageName = imagesWithoutPageElements.get(0);
                    imagesWithoutPageElements.remove(0);
                    ContentFile cf = new ContentFile();
                    if (SystemUtils.IS_OS_WINDOWS) {
                        cf.setLocation(
                                "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                        + newImageName);
                    } else {
                        cf.setLocation(
                                "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                        + newImageName);
                    }
                    page.addContentFile(cf);
                } else {
                    // remove page
                    physicaldocstruct.removeChild(page);
                    List<Reference> refs = new ArrayList<Reference>(page.getAllFromReferences());
                    for (ugh.dl.Reference ref : refs) {
                        ref.getSource().removeReferenceTo(page);
                    }
                }
            }
            if (!imagesWithoutPageElements.isEmpty()) {
                // create new page elements

                int currentPhysicalOrder = physicaldocstruct.getAllChildren().size();
                for (URI newImage : imagesWithoutPageElements) {
                    DocStruct dsPage = this.mydocument.createDocStruct(newPage);
                    try {
                        // physical page no
                        physicaldocstruct.addChild(dsPage);
                        MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
                        Metadata mdTemp = new Metadata(mdt);
                        mdTemp.setValue(String.valueOf(++currentPhysicalOrder));
                        dsPage.addMetadata(mdTemp);

                        // logical page no
                        mdt = this.myPrefs.getMetadataTypeByName("logicalPageNumber");
                        mdTemp = new Metadata(mdt);

                        if (defaultPagination.equalsIgnoreCase("arabic")) {
                            mdTemp.setValue(String.valueOf(currentPhysicalOrder));
                        } else if (defaultPagination.equalsIgnoreCase("roman")) {
                            RomanNumeral roman = new RomanNumeral();
                            roman.setValue(currentPhysicalOrder);
                            mdTemp.setValue(roman.getNumber());
                        } else {
                            mdTemp.setValue("uncounted");
                        }

                        dsPage.addMetadata(mdTemp);
                        log.addReferenceTo(dsPage, "logical_physical");

                        // image name
                        ContentFile cf = new ContentFile();
                        if (SystemUtils.IS_OS_WINDOWS) {
                            cf.setLocation(
                                    "file:/" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                            + newImage);
                        } else {
                            cf.setLocation(
                                    "file://" + serviceManager.getProcessService().getImagesTifDirectory(false, process)
                                            + newImage);
                        }
                        dsPage.addContentFile(cf);

                    } catch (TypeNotAllowedAsChildException e) {
                        logger.error(e);
                    } catch (MetadataTypeNotAllowedException e) {
                        logger.error(e);
                    }
                }

            }
        }
        int currentPhysicalOrder = 1;
        MetadataType mdt = this.myPrefs.getMetadataTypeByName("physPageNumber");
        if (physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*") != null) {
            for (DocStruct page : physicaldocstruct.getAllChildrenByTypeAndMetadataType("page", "*")) {
                List<? extends Metadata> pageNoMetadata = page.getAllMetadataByType(mdt);
                if (pageNoMetadata == null || pageNoMetadata.size() == 0) {
                    currentPhysicalOrder++;
                    break;
                }
                for (Metadata pageNo : pageNoMetadata) {
                    pageNo.setValue(String.valueOf(currentPhysicalOrder));
                }
                currentPhysicalOrder++;
            }
        }
    }

    /**
     * scale given image file to png using internal embedded content server.
     */
    public void scaleFile(URI inFileName, URI outFileName, int inSize, int intRotation)
            throws ImageManagerException, IOException, ImageManipulatorException {
        logger.trace("start scaleFile");
        int tmpSize = inSize / 3;
        if (tmpSize < 1) {
            tmpSize = 1;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("tmpSize: " + tmpSize);
        }
        if (ConfigCore.getParameter("kitodoContentServerUrl", "").equals("")) {
            logger.trace("api");
            ImageManager im = new ImageManager(inFileName.toURL());
            logger.trace("im");
            RenderedImage ri = im.scaleImageByPixel(tmpSize, tmpSize, ImageManager.SCALE_BY_PERCENT, intRotation);
            logger.trace("ri");
            JpegInterpreter pi = new JpegInterpreter(ri);
            logger.trace("pi");
            FileOutputStream outputFileStream = (FileOutputStream) fileService.write(outFileName);
            logger.trace("output");
            pi.writeToStream(null, outputFileStream);
            logger.trace("write stream");
            outputFileStream.close();
            logger.trace("close stream");
        } else {
            String cs = ConfigCore.getParameter("kitodoContentServerUrl") + inFileName + "&scale=" + tmpSize
                    + "&rotate=" + intRotation + "&format=jpg";
            cs = cs.replace("\\", "/");
            if (logger.isTraceEnabled()) {
                logger.trace("url: " + cs);
            }
            URL csUrl = new URL(cs);
            HttpClient httpclient = new HttpClient();
            GetMethod method = new GetMethod(csUrl.toString());
            logger.trace("get");
            Integer contentServerTimeOut = ConfigCore.getIntParameter("kitodoContentServerTimeOut", 60000);
            method.getParams().setParameter("http.socket.timeout", contentServerTimeOut);
            int statusCode = httpclient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                return;
            }
            if (logger.isTraceEnabled()) {
                logger.trace("statusCode: " + statusCode);
            }
            InputStream inStream = method.getResponseBodyAsStream();
            logger.trace("inStream");
            try (BufferedInputStream bis = new BufferedInputStream(inStream);
                    OutputStream fos = fileService.write(outFileName);) {
                logger.trace("BufferedInputStream");
                logger.trace("FileOutputStream");
                byte[] bytes = new byte[8192];
                int count = bis.read(bytes);
                while (count != -1 && count <= 8192) {
                    fos.write(bytes, 0, count);
                    count = bis.read(bytes);
                }
                if (count != -1) {
                    fos.write(bytes, 0, count);
                }
            }
            logger.trace("write");
        }
        logger.trace("end scaleFile");
    }

    // Add a method to validate the image files

    /**
     * Die Images eines Prozesses auf Vollständigkeit prüfen.
     */
    public boolean checkIfImagesValid(String title, URI folder) {
        boolean isValid = true;
        this.myLastImage = 0;

        /*
         * alle Bilder durchlaufen und dafür die Seiten anlegen
         */
        if (fileService.fileExist(folder)) {
            ArrayList<URI> files = fileService.getSubUris(Helper.dataFilter, folder);
            if (files == null || files.size() == 0) {
                Helper.setFehlerMeldung("[" + title + "] No objects found");
                return false;
            }

            this.myLastImage = files.size();
            if (ConfigCore.getParameter("ImagePrefix", "\\d{8}").equals("\\d{8}")) {
                List<URI> filesDirs = files;
                Collections.sort(filesDirs);
                int counter = 1;
                int myDiff = 0;
                String currentFileName = null;
                try {
                    for (Iterator<URI> iterator = filesDirs.iterator(); iterator.hasNext(); counter++) {
                        currentFileName = fileService.getFileName(iterator.next());

                        int curFileNumber = Integer
                                .parseInt(currentFileName.substring(0, currentFileName.indexOf(".")));
                        if (curFileNumber != counter + myDiff) {
                            Helper.setFehlerMeldung("[" + title + "] expected Image " + (counter + myDiff)
                                    + " but found File " + currentFileName);
                            myDiff = curFileNumber - counter;
                            isValid = false;
                        }
                    }
                } catch (NumberFormatException e1) {
                    isValid = false;
                    Helper.setFehlerMeldung(
                            "[" + title + "] Filename of image wrong - not an 8-digit-number: " + currentFileName);
                }
                return isValid;
            }
            return true;
        }
        Helper.setFehlerMeldung("[" + title + "] No image-folder found");
        return false;
    }

    public static class GoobiImageFileComparator implements Comparator<URI> {

        @Override
        public int compare(URI firstUri, URI secondUri) {
            String firstString = fileService.getFileName(firstUri);
            String secondString = fileService.getFileName(secondUri);
            String imageSorting = ConfigCore.getParameter("ImageSorting", "number");
            firstString = firstString.substring(0, firstString.lastIndexOf("."));
            secondString = secondString.substring(0, secondString.lastIndexOf("."));

            if (imageSorting.equalsIgnoreCase("number")) {
                try {
                    Integer firstInteger = Integer.valueOf(firstString);
                    Integer secondInteger = Integer.valueOf(secondString);
                    return firstInteger.compareTo(secondInteger);
                } catch (NumberFormatException e) {
                    return firstString.compareToIgnoreCase(secondString);
                }
            } else if (imageSorting.equalsIgnoreCase("alphanumeric")) {
                return firstString.compareToIgnoreCase(secondString);
            } else {
                return firstString.compareToIgnoreCase(secondString);
            }
        }

    }

    /**
     * Get image files.
     *
     * @param myProcess
     *            current process
     * @param directory
     *            current folder
     * @return sorted list with strings representing images of process
     */
    public List<URI> getImageFiles(Process myProcess, URI directory) throws InvalidImagesException {
        URI dir;
        try {
            dir = fileService.getProcessSubTypeURI(myProcess, ProcessSubType.IMAGE, null).resolve(directory);
        } catch (Exception e) {
            throw new InvalidImagesException(e);
        }
        /* Verzeichnis einlesen */
        ArrayList<URI> files = fileService.getSubUris(Helper.imageNameFilter, dir);
        List<URI> dataList = new ArrayList<>();
        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {
                dataList.add(files.get(i));
            }
            /* alle Dateien durchlaufen */
        }
        List<URI> orderedFilenameList = new ArrayList<>();
        if (dataList.size() != 0) {
            List<DocStruct> pagesList = mydocument.getPhysicalDocStruct().getAllChildren();
            if (pagesList != null) {
                for (DocStruct page : pagesList) {
                    String filename = page.getImageName();
                    String filenamePrefix = filename.replace(Metadaten.getFileExtension(filename), "");
                    for (URI currentImage : dataList) {
                        String currentFileName = fileService.getFileName(currentImage);
                        String currentImagePrefix = currentFileName.replace(Metadaten.getFileExtension(currentFileName),
                                "");
                        if (currentImagePrefix.equals(filenamePrefix)) {
                            orderedFilenameList.add(currentImage);
                            break;
                        }
                    }
                }
                // orderedFilenameList.add(page.getImageName());
            }

            if (orderedFilenameList.size() == dataList.size()) {
                return orderedFilenameList;

            } else {
                Collections.sort(dataList, new GoobiImageFileComparator());
                return dataList;
            }
        } else {
            return null;
        }
    }

    /**
     * Get image files.
     *
     * @param physical
     *            DocStruct object
     * @return list of Strings
     */
    public List<URI> getImageFiles(DocStruct physical) {
        List<URI> orderedFileList = new ArrayList<>();
        List<DocStruct> pages = physical.getAllChildren();
        if (pages != null) {
            for (DocStruct page : pages) {
                URI filename = URI.create(page.getImageName());
                if (filename != null) {
                    orderedFileList.add(filename);
                } else {
                    logger.error("cannot find image");
                }
            }
        }
        return orderedFileList;
    }

    /**
     * Get data files.
     *
     * @param myProcess
     *            Process object
     * @return list of Strings
     */
    public List<URI> getDataFiles(Process myProcess) throws InvalidImagesException {
        URI dir;
        try {
            dir = serviceManager.getProcessService().getImagesTifDirectory(true, myProcess);
        } catch (Exception e) {
            throw new InvalidImagesException(e);
        }
        /* Verzeichnis einlesen */
        ArrayList<URI> files = fileService.getSubUris(Helper.dataFilter, dir);
        ArrayList<URI> dataList = new ArrayList<>();
        if (files != null && files.size() > 0) {
            for (int i = 0; i < files.size(); i++) {
                dataList.add(files.get(i));
            }
            /* alle Dateien durchlaufen */
            if (dataList.size() != 0) {
                Collections.sort(dataList, new GoobiImageFileComparator());
            }
            return dataList;
        } else {
            return null;
        }
    }

}
