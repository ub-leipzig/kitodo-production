/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package de.sub.goobi.helper.tasks;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.export.dms.ExportDms;
import de.sub.goobi.forms.LoginForm;
import de.sub.goobi.helper.Helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.goobi.production.constants.Parameters;
import org.hibernate.Hibernate;
import org.kitodo.data.database.beans.Batch;
import org.kitodo.data.database.beans.Process;
import org.kitodo.services.ServiceManager;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Prefs;
import ugh.dl.exceptions.MetadataTypeNotAllowedException;
import ugh.dl.exceptions.PreferencesException;
import ugh.dl.exceptions.ReadException;
import ugh.dl.exceptions.TypeNotAllowedAsChildException;
import ugh.dl.exceptions.TypeNotAllowedForParentException;
import ugh.dl.fileformats.mets.MetsModsImportExport;

/**
 * Thread implementation to export a batch holding a serial publication as set,
 * cross-over inserting METS pointer references to the respective other volumes
 * in the anchor file.
 *
 * <p>
 * Requires the {@code MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE} metadata
 * type ("MetsPointerURL") to be available for adding to the first level child
 * of the logical document structure hierarchy (typically "Volume").
 * </p>
 *
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class ExportSerialBatchTask extends EmptyTask {

    private static final ServiceManager serviceManager = new ServiceManager();

    /**
     * The batch to export.
     */
    private final Batch batch;

    /**
     * The METS pointers of all volumes belonging to this serial publication.
     */
    private final ArrayList<String> pointers;

    /**
     * Counter used for incrementing the progress bar, starts from 0 and ends
     * with “maxsize”.
     */
    private int stepcounter;

    /**
     * Iterator along the processes of the batch during export.
     */
    private Iterator<Process> processesIterator;

    /**
     * Value indicating 100% on the progress bar.
     */
    private final int maxsize;

    /**
     * Creates a new ExportSerialBatchTask from a batch of processes belonging
     * to a serial publication.
     *
     * @param batch
     *            batch holding a serial publication
     */
    public ExportSerialBatchTask(Batch batch) {
        super(batch.getLabel());
        this.batch = batch;
        int batchSize = batch.getProcesses().size();
        pointers = new ArrayList<String>(batchSize);
        stepcounter = 0;
        processesIterator = null;
        maxsize = batchSize + 1;
        initialiseRuleSets(batch.getProcesses());
    }

    /**
     * Returns the display name of the task to show to the user.
     *
     * @see de.sub.goobi.helper.tasks.INameableTask#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Helper.getTranslation("ExportSerialBatchTask");
    }

    /**
     * Initialises the the rule sets of the processes to export that export
     * depends on. This cannot be done later because the therad doesn’t have
     * access to the hibernate session any more.
     *
     * @param processes
     *            collection of processes whose rulesets are to be initialised
     */
    private static final void initialiseRuleSets(Iterable<Process> processes) {
        for (Process process : processes) {
            Hibernate.initialize(process.getRuleset());
        }
    }

    /**
     * Clone constructor. Creates a new ExportSerialBatchTask from another one.
     * This is used for restarting the thread as a Java thread cannot be run
     * twice.
     *
     * @param master
     *            copy master
     */
    public ExportSerialBatchTask(ExportSerialBatchTask master) {
        super(master);
        batch = master.batch;
        pointers = master.pointers;
        stepcounter = master.stepcounter;
        processesIterator = master.processesIterator;
        maxsize = master.maxsize;
    }

    /**
     * The function run() is the main function of this task (which is a thread).
     * It will aggregate the data from all processes and then export all
     * processes with the recombined data. The statusProgress variable is being
     * updated to show the operator how far the task has proceeded.
     *
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        Process process = null;
        try {
            if (stepcounter == 0) {
                pointers.clear();
                for (Process processIterator : batch.getProcesses()) {
                    process = processIterator;
                    pointers.add(ExportNewspaperBatchTask.getMetsPointerURL(process));
                }
                processesIterator = batch.getProcesses().iterator();
                stepcounter++;
                setProgress(100 * stepcounter / maxsize);
            }
            if (stepcounter > 0) {
                while (processesIterator.hasNext()) {
                    if (isInterrupted()) {
                        return;
                    }
                    process = processesIterator.next();
                    DigitalDocument out = buildExportDocument(process, pointers);
                    ExportDms exporter = new ExportDms(
                            ConfigCore.getBooleanParameter(Parameters.EXPORT_WITH_IMAGES, true));
                    exporter.setExportDmsTask(this);
                    exporter.startExport(process, LoginForm.getCurrentUserHomeDir(), out);
                    stepcounter++;
                    setProgress(100 * stepcounter / maxsize);
                }
            }
        } catch (Exception e) {
            // PreferencesException, ReadException, SwapException, DAOException,
            // IOException, InterruptedException
            // and some runtime exceptions
            String message = e.getClass().getSimpleName() + " while " + (stepcounter == 0 ? "examining " : "exporting ")
                    + (process != null ? process.getTitle() : "") + ": " + e.getMessage();
            setException(new RuntimeException(message, e));
        }
    }

    /**
     * The function buildExportableMetsMods() returns a DigitalDocument object
     * whose logical document structure tree has been enriched with all nodes
     * that have to be exported along with the data to make cross-volume
     * referencing work.
     *
     * @param process
     *            process to get the METS/MODS data from
     * @param allPointers
     *            all the METS pointers from all volumes
     * @return an enriched DigitalDocument
     * @throws PreferencesException
     *             if the no node corresponding to the file format is available
     *             in the rule set used
     * @throws ReadException
     *             if the meta data file cannot be read
     * @throws IOException
     *             if creating the process directory or reading the meta data
     *             file fails
     * @throws TypeNotAllowedForParentException
     *             is thrown, if this DocStruct is not allowed for a parent
     * @throws MetadataTypeNotAllowedException
     *             if the DocStructType of this DocStruct instance does not
     *             allow the MetadataType or if the maximum number of Metadata
     *             (of this type) is already available
     * @throws TypeNotAllowedAsChildException
     *             if a child should be added, but it's DocStruct type isn't
     *             member of this instance's DocStruct type
     */
    private static DigitalDocument buildExportDocument(Process process, Iterable<String> allPointers)
            throws PreferencesException, ReadException, IOException, MetadataTypeNotAllowedException,
            TypeNotAllowedForParentException, TypeNotAllowedAsChildException {
        DigitalDocument result = serviceManager.getProcessService().readMetadataFile(process).getDigitalDocument();
        DocStruct root = result.getLogicalDocStruct();
        String type = "Volume";
        try {
            type = root.getAllChildren().get(0).getType().getName();
        } catch (NullPointerException e) {
        }
        String ownPointer = ExportNewspaperBatchTask.getMetsPointerURL(process);
        Prefs ruleset = serviceManager.getRulesetService().getPreferences(process.getRuleset());
        for (String pointer : allPointers) {
            if (!pointer.equals(ownPointer)) {
                root.createChild(type, result, ruleset).addMetadata(MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE,
                        pointer);
            }
        }
        return result;
    }
}
