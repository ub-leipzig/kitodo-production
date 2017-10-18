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
package de.sub.goobi.forms;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.Helper;
import de.unigoettingen.sub.search.opac.ConfigOpac;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.goobi.production.enums.ImportFormat;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.export.ExportDocket;
import org.goobi.production.flow.helper.JobCreation;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.GoobiHotfolder;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.properties.ImportProperty;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.kitodo.data.database.beans.Batch;
import org.kitodo.data.database.beans.Batch.Type;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.services.ServiceManager;

import ugh.dl.Prefs;

@Named("MassImportForm")
@SessionScoped
public class MassImportForm implements Serializable {
    private static final Logger logger = LogManager.getLogger(MassImportForm.class);
    private static final long serialVersionUID = -4225927414279404442L;
    private Process template;
    private List<Process> processes;
    private List<String> digitalCollections;
    private List<String> possibleDigitalCollections;
    private String opacCatalogue;
    private List<String> ids = new ArrayList<String>();
    private ImportFormat format = null;
    private String idList = "";
    private String records = "";
    private List<String> usablePluginsForRecords = new ArrayList<String>();
    private List<String> usablePluginsForIDs = new ArrayList<String>();
    private List<String> usablePluginsForFiles = new ArrayList<String>();
    private List<String> usablePluginsForFolder = new ArrayList<String>();
    private String currentPlugin = "";
    private IImportPlugin plugin;
    private File importFile = null;
    private final Helper help = new Helper();
    private transient ServiceManager serviceManager = new ServiceManager();
    private UploadedFile uploadedFile = null;

    private List<Process> processList;

    /**
     * Constructor.
     */
    public MassImportForm() {
        usablePluginsForRecords = PluginLoader.getImportPluginsForType(ImportType.Record);
        usablePluginsForIDs = PluginLoader.getImportPluginsForType(ImportType.ID);
        usablePluginsForFiles = PluginLoader.getImportPluginsForType(ImportType.FILE);
        usablePluginsForFolder = PluginLoader.getImportPluginsForType(ImportType.FOLDER);
    }

    /**
     * Prepare.
     *
     * @return String
     */
    public String prepare() {
        if (serviceManager.getProcessService().getContainsUnreachableSteps(this.template)) {
            if (this.template.getTasks().size() == 0) {
                Helper.setFehlerMeldung("noStepsInWorkflow");
            }
            for (Task s : this.template.getTasks()) {
                if (serviceManager.getTaskService().getUserGroupsSize(s) == 0
                        && serviceManager.getTaskService().getUsersSize(s) == 0) {
                    List<String> param = new ArrayList<String>();
                    param.add(s.getTitle());
                    Helper.setFehlerMeldung(Helper.getTranslation("noUserInStep", param));
                }
            }
            return null;
        }
        initializePossibleDigitalCollections();
        return "/newpages/MassImport";
    }

    /**
     * generate a list with all possible collections for given project.
     */

    @SuppressWarnings("unchecked")
    private void initializePossibleDigitalCollections() {
        this.possibleDigitalCollections = new ArrayList<String>();
        ArrayList<String> defaultCollections = new ArrayList<String>();
        String filename = ConfigCore.getKitodoConfigDirectory() + "kitodo_digitalCollections.xml";
        if (!(new File(filename).exists())) {
            Helper.setFehlerMeldung("File not found: ", filename);
            return;
        }
        this.digitalCollections = new ArrayList<String>();
        try {
            /* Datei einlesen und Root ermitteln */
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new File(filename));
            Element root = doc.getRootElement();
            /* alle Projekte durchlaufen */
            List<Element> projekte = root.getChildren();
            for (Iterator<Element> iter = projekte.iterator(); iter.hasNext();) {
                Element projekt = iter.next();

                // collect default collections
                if (projekt.getName().equals("default")) {
                    List<Element> myCols = projekt.getChildren("DigitalCollection");
                    for (Iterator<Element> it2 = myCols.iterator(); it2.hasNext();) {
                        Element col = it2.next();

                        if (col.getAttribute("default") != null
                                && col.getAttributeValue("default").equalsIgnoreCase("true")) {
                            digitalCollections.add(col.getText());
                        }

                        defaultCollections.add(col.getText());
                    }
                } else {
                    // run through the projects
                    List<Element> projektnamen = projekt.getChildren("name");
                    for (Iterator<Element> iterator = projektnamen.iterator(); iterator.hasNext();) {
                        Element projektname = iterator.next();
                        // all all collections to list
                        if (projektname.getText().equalsIgnoreCase(this.template.getProject().getTitle())) {
                            List<Element> myCols = projekt.getChildren("DigitalCollection");
                            for (Iterator<Element> it2 = myCols.iterator(); it2.hasNext();) {
                                Element col = it2.next();

                                if (col.getAttribute("default") != null
                                        && col.getAttributeValue("default").equalsIgnoreCase("true")) {
                                    digitalCollections.add(col.getText());
                                }

                                this.possibleDigitalCollections.add(col.getText());
                            }
                        }
                    }
                }
            }
        } catch (JDOMException | IOException e1) {
            logger.error("error while parsing digital collections", e1);
            Helper.setFehlerMeldung("Error while parsing digital collections", e1);
        }

        if (this.possibleDigitalCollections.size() == 0) {
            this.possibleDigitalCollections = defaultCollections;
        }
    }

    private List<String> allFilenames = new ArrayList<String>();
    private List<String> selectedFilenames = new ArrayList<String>();

    public List<String> getAllFilenames() {

        return this.allFilenames;
    }

    public void setAllFilenames(List<String> allFilenames) {
        this.allFilenames = allFilenames;
    }

    public List<String> getSelectedFilenames() {
        return this.selectedFilenames;
    }

    public void setSelectedFilenames(List<String> selectedFilenames) {
        this.selectedFilenames = selectedFilenames;
    }

    /**
     * Convert data.
     *
     * @return String
     */
    public String convertData() throws IOException, DAOException {
        this.processList = new ArrayList<>();
        if (StringUtils.isEmpty(currentPlugin)) {
            Helper.setFehlerMeldung("missingPlugin");
            return null;
        }
        if (testForData()) {
            List<ImportObject> answer = new ArrayList<ImportObject>();
            Batch batch = null;

            // found list with ids
            Prefs prefs = serviceManager.getRulesetService().getPreferences(this.template.getRuleset());
            String tempfolder = ConfigCore.getParameter("tempfolder");
            this.plugin.setImportFolder(tempfolder);
            this.plugin.setPrefs(prefs);
            this.plugin.setOpacCatalogue(this.getOpacCatalogue());
            this.plugin.setKitodoConfigDirectory(ConfigCore.getKitodoConfigDirectory());

            if (StringUtils.isNotEmpty(this.idList)) {
                List<String> ids = this.plugin.splitIds(this.idList);
                List<Record> recordList = new ArrayList<Record>();
                for (String id : ids) {
                    Record r = new Record();
                    r.setData(id);
                    r.setId(id);
                    r.setCollections(this.digitalCollections);
                    recordList.add(r);
                }

                answer = this.plugin.generateFiles(recordList);
            } else if (this.importFile != null) {
                this.plugin.setFile(this.importFile);
                List<Record> recordList = this.plugin.generateRecordsFromFile();
                for (Record r : recordList) {
                    r.setCollections(this.digitalCollections);
                }
                answer = this.plugin.generateFiles(recordList);
            } else if (StringUtils.isNotEmpty(this.records)) {
                List<Record> recordList = this.plugin.splitRecords(this.records);
                for (Record r : recordList) {
                    r.setCollections(this.digitalCollections);
                }
                answer = this.plugin.generateFiles(recordList);
            } else if (this.selectedFilenames.size() > 0) {
                List<Record> recordList = this.plugin.generateRecordsFromFilenames(this.selectedFilenames);
                for (Record r : recordList) {
                    r.setCollections(this.digitalCollections);
                }
                answer = this.plugin.generateFiles(recordList);

            }

            if (answer.size() > 1) {
                if (importFile != null) {
                    List<String> args = Arrays
                            .asList(new String[] {FilenameUtils.getBaseName(importFile.getAbsolutePath()),
                                    DateTimeFormat.shortDateTime().print(new DateTime()) });
                    batch = new Batch(Helper.getTranslation("importedBatchLabel", args), Type.LOGISTIC);
                } else {
                    batch = new Batch();
                }
            }

            for (ImportObject io : answer) {
                if (batch != null) {
                    io.getBatches().add(batch);
                }
                if (io.getImportReturnValue().equals(ImportReturnValue.ExportFinished)) {
                    Process p = JobCreation.generateProcess(io, this.template);
                    if (p == null) {
                        if (io.getImportFileName() != null
                                && !serviceManager.getFileService().getFileName(io.getImportFileName()).isEmpty()
                                && selectedFilenames != null && !selectedFilenames.isEmpty()) {
                            if (selectedFilenames.contains(io.getImportFileName())) {
                                selectedFilenames.remove(io.getImportFileName());
                            }
                        }
                        Helper.setFehlerMeldung(
                                "import failed for " + io.getProcessTitle() + ", process generation failed");

                    } else {
                        Helper.setMeldung(ImportReturnValue.ExportFinished.getValue() + " for " + io.getProcessTitle());
                        this.processList.add(p);
                    }
                } else {
                    List<String> param = new ArrayList<String>();
                    param.add(io.getProcessTitle());
                    param.add(io.getErrorMessage());
                    Helper.setFehlerMeldung(Helper.getTranslation("importFailedError", param));
                    if (io.getImportFileName() != null
                            && !serviceManager.getFileService().getFileName(io.getImportFileName()).isEmpty()
                            && selectedFilenames != null && !selectedFilenames.isEmpty()) {
                        if (selectedFilenames.contains(io.getImportFileName())) {
                            selectedFilenames.remove(io.getImportFileName());
                        }
                    }
                }
            }
            if (answer.size() != this.processList.size()) {
                // some error on process generation, don't go to next page
                return null;
            }
        } else {
            Helper.setFehlerMeldung("missingData");
            return null;
        }
        this.idList = null;
        if (this.importFile != null) {
            this.importFile.delete();
            this.importFile = null;
        }
        if (selectedFilenames != null && !selectedFilenames.isEmpty()) {
            this.plugin.deleteFiles(this.selectedFilenames);
        }
        this.records = "";
        return "/newpages/MassImportFormPage3";
    }

    /**
     * File upload with binary copying.
     */
    public void uploadFile() throws IOException {

        if (this.uploadedFile == null) {
            Helper.setFehlerMeldung("noFileSelected");
            return;
        }

        String basename = this.uploadedFile.getName();
        if (basename.startsWith(".")) {
            basename = basename.substring(1);
        }
        if (basename.contains("/")) {
            basename = basename.substring(basename.lastIndexOf("/") + 1);
        }
        if (basename.contains("\\")) {
            basename = basename.substring(basename.lastIndexOf("\\") + 1);
        }
        URI temporalFile = serviceManager.getFileService()
                .createResource(ConfigCore.getParameter("tempfolder", "/usr/local/kitodo/temp/") + basename);

        serviceManager.getFileService().copyFile(URI.create(this.uploadedFile.getName()), temporalFile);

    }

    public UploadedFile getUploadedFile() {
        return this.uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    /**
     * tests input fields for correct data.
     *
     * @return true if data is valid or false otherwise
     */

    private boolean testForData() {
        if (StringUtils.isEmpty(this.idList) && StringUtils.isEmpty(this.records) && (this.importFile == null)
                && this.selectedFilenames.size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * Get formats.
     *
     * @return list with all import formats
     */
    public List<String> getFormats() {
        List<String> l = new ArrayList<String>();
        for (ImportFormat input : ImportFormat.values()) {
            l.add(input.getTitle());
        }
        return l;
    }

    /**
     * Get Hotfolder path for plugin.
     *
     * @param pluginId
     *            int
     * @return String
     */
    public String getHotfolderPathForPlugin(int pluginId) {
        for (GoobiHotfolder hotfolder : GoobiHotfolder.getInstances()) {
            if (hotfolder.getTemplate() == pluginId) {
                return hotfolder.getFolderAsString();
            }
        }

        return null;
    }

    /**
     * Get current format.
     *
     * @return current format
     */

    public String getCurrentFormat() {
        if (this.format != null) {
            return this.format.getTitle();
        } else {
            return null;
        }
    }

    /**
     * Set current format.
     *
     * @param formatTitle
     *            current format
     */
    public void setCurrentFormat(String formatTitle) {
        this.format = ImportFormat.getTypeFromTitle(formatTitle);
    }

    /**
     * Set id list.
     *
     * @param idList
     *            the idList to set
     */
    public void setIdList(String idList) {
        this.idList = idList;
    }

    /**
     * Get id list.
     *
     * @return the idList
     */
    public String getIdList() {
        return this.idList;
    }

    /**
     * Set records.
     *
     * @param records
     *            the records to set
     */
    public void setRecords(String records) {
        this.records = records;
    }

    /**
     * Get recrods.
     *
     * @return the records
     */
    public String getRecords() {
        return this.records;
    }

    /**
     * Set processes.
     *
     * @param processes
     *            the process list to set
     */
    public void setProcess(List<Process> processes) {
        this.processes = processes;
    }

    /**
     * Get processes.
     *
     * @return the process
     */
    public List<Process> getProcess() {
        return this.processes;
    }

    /**
     * Set template.
     *
     * @param template
     *            the template to set
     */
    public void setTemplate(Process template) {
        this.template = template;

    }

    /**
     * Get template.
     *
     * @return the template
     */
    public Process getTemplate() {
        return this.template;
    }

    /**
     * Get all OPAC catalogues.
     *
     * @return the opac catalogues
     */
    public List<String> getAllOpacCatalogues() {
        try {
            return ConfigOpac.getAllCatalogueTitles();
        } catch (Throwable t) {
            Helper.setFehlerMeldung("Error while reading von opac-config", t.getMessage());
            return new ArrayList<String>();
        }
    }

    /**
     * Set OPAC catalogue.
     *
     * @param opacCatalogue
     *            the opacCatalogue to set
     */

    public void setOpacCatalogue(String opacCatalogue) {
        this.opacCatalogue = opacCatalogue;
    }

    /**
     * Get OPAC catalogue.
     *
     * @return the opac catalogues
     */

    public String getOpacCatalogue() {
        return this.opacCatalogue;
    }

    /**
     * Set digital collections.
     *
     * @param digitalCollections
     *            the digitalCollections to set
     */
    public void setDigitalCollections(List<String> digitalCollections) {
        this.digitalCollections = digitalCollections;
    }

    /**
     * Get digital collections.
     *
     * @return the digitalCollections
     */
    public List<String> getDigitalCollections() {
        return this.digitalCollections;
    }

    /**
     * Set possible digital collection.
     *
     * @param possibleDigitalCollection
     *            the possibleDigitalCollection to set
     */
    public void setPossibleDigitalCollection(List<String> possibleDigitalCollection) {
        this.possibleDigitalCollections = possibleDigitalCollection;
    }

    /**
     * Get possible digital collection.
     *
     * @return the possibleDigitalCollection
     */
    public List<String> getPossibleDigitalCollection() {
        return this.possibleDigitalCollections;
    }

    public void setPossibleDigitalCollections(List<String> possibleDigitalCollections) {
        this.possibleDigitalCollections = possibleDigitalCollections;
    }

    public void setProcesses(List<Process> processes) {
        this.processes = processes;
    }

    /**
     * Set ids.
     *
     * @param ids
     *            the ids to set
     */
    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    /**
     * Get ids.
     *
     * @return the ids
     */
    public List<String> getIds() {
        return this.ids;
    }

    /**
     * Set format.
     *
     * @param format
     *            the format to set
     */
    public void setFormat(String format) {
        this.format = ImportFormat.getTypeFromTitle(format);
    }

    /**
     * Get format.
     *
     * @return the format
     */
    public String getFormat() {
        if (this.format == null) {
            return null;
        }
        return this.format.getTitle();
    }

    /**
     * Set current plugin.
     *
     * @param currentPlugin
     *            the currentPlugin to set
     */
    public void setCurrentPlugin(String currentPlugin) {
        this.currentPlugin = currentPlugin;
        if (currentPlugin != null && currentPlugin.length() > 0) {
            this.plugin = (IImportPlugin) PluginLoader.getPluginByTitle(PluginType.Import, this.currentPlugin);
            if (this.plugin.getImportTypes().contains(ImportType.FOLDER)) {
                this.allFilenames = this.plugin.getAllFilenames();
            }
            plugin.setPrefs(serviceManager.getRulesetService().getPreferences(template.getRuleset()));
        }
    }

    /**
     * Get current plugin.
     *
     * @return the currentPlugin
     */
    public String getCurrentPlugin() {
        return this.currentPlugin;
    }

    public IImportPlugin getPlugin() {
        return plugin;
    }

    /**
     * Set usable plugins for records.
     *
     * @param usablePluginsForRecords
     *            the usablePluginsForRecords to set
     */
    public void setUsablePluginsForRecords(List<String> usablePluginsForRecords) {
        this.usablePluginsForRecords = usablePluginsForRecords;
    }

    /**
     * Get usable plugins for records.
     *
     * @return the usablePluginsForRecords
     */
    public List<String> getUsablePluginsForRecords() {
        return this.usablePluginsForRecords;
    }

    /**
     * Set usable plugins for ids.
     *
     * @param usablePluginsForIDs
     *            the usablePluginsForIDs to set
     */
    public void setUsablePluginsForIDs(List<String> usablePluginsForIDs) {
        this.usablePluginsForIDs = usablePluginsForIDs;
    }

    /**
     * Get usable plugins for ids.
     *
     * @return the usablePluginsForIDs
     */
    public List<String> getUsablePluginsForIDs() {
        return this.usablePluginsForIDs;
    }

    /**
     * Set usable plugins for files.
     *
     * @param usablePluginsForFiles
     *            the usablePluginsForFiles to set
     */
    public void setUsablePluginsForFiles(List<String> usablePluginsForFiles) {
        this.usablePluginsForFiles = usablePluginsForFiles;
    }

    /**
     * get usable plugins for files.
     *
     * @return the usablePluginsForFiles
     */
    public List<String> getUsablePluginsForFiles() {
        return this.usablePluginsForFiles;
    }

    /**
     * Get has next page.
     *
     * @return boolean
     */
    public boolean getHasNextPage() {
        java.lang.reflect.Method method;
        try {
            method = this.plugin.getClass().getMethod("getCurrentDocStructs");
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (list != null) {
                return true;
            }
        } catch (Exception e) {
        }
        try {
            method = this.plugin.getClass().getMethod("getProperties");
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<ImportProperty> list = (List<ImportProperty>) o;
            if (list.size() > 0) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    /**
     * Get next page.
     * 
     * @return next page
     */
    public String nextPage() {
        if (!testForData()) {
            Helper.setFehlerMeldung("missingData");
            return null;
        }
        java.lang.reflect.Method method;
        try {
            method = this.plugin.getClass().getMethod("getCurrentDocStructs");
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (list != null) {
                return "/newpages/MultiMassImportPage2";
            }
        } catch (Exception e) {
        }
        return "/newpages/MassImportFormPage2";
    }

    /**
     * Get properties.
     *
     * @return list of ImportProperty objects
     */
    public List<ImportProperty> getProperties() {

        if (this.plugin != null) {
            return this.plugin.getProperties();
        }
        return new ArrayList<ImportProperty>();
    }

    public List<Process> getProcessList() {
        return this.processList;
    }

    public void setProcessList(List<Process> processList) {
        this.processList = processList;
    }

    public List<String> getUsablePluginsForFolder() {
        return this.usablePluginsForFolder;
    }

    public void setUsablePluginsForFolder(List<String> usablePluginsForFolder) {
        this.usablePluginsForFolder = usablePluginsForFolder;
    }

    /**
     * Download docket.
     *
     * @return String
     */
    public String downloadDocket() {
        logger.debug("generate docket for process list");
        String rootpath = ConfigCore.getParameter("xsltFolder");
        File xsltfile = new File(rootpath, "docket_multipage.xsl");
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (!facesContext.getResponseComplete()) {
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            String fileName = "batch_docket" + ".pdf";
            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            // write docket to servlet output stream
            try {
                ServletOutputStream out = response.getOutputStream();
                ExportDocket ern = new ExportDocket();
                ern.startExport(this.processList, out, xsltfile.getAbsolutePath());
                out.flush();
            } catch (IOException e) {
                logger.error("IOException while exporting run note", e);
            }

            facesContext.responseComplete();
        }
        return null;
    }

    /**
     * Get document structure.
     *
     * @return list of DocstructElement objects
     */
    public List<? extends DocstructElement> getDocstructs() {
        java.lang.reflect.Method method;
        try {
            method = this.plugin.getClass().getMethod("getCurrentDocStructs");
            Object o = method.invoke(this.plugin);
            @SuppressWarnings("unchecked")
            List<? extends DocstructElement> list = (List<? extends DocstructElement>) o;
            if (list != null) {
                return list;
            }
        } catch (Exception e) {
        }
        return new ArrayList<DocstructElement>();
    }

    public int getDocstructssize() {
        return getDocstructs().size();
    }

    public String getInclude() {
        return "plugins/" + plugin.getTitle() + ".jsp";
    }
}
