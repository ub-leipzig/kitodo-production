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

import com.sun.research.ws.wadl.HTTPMethods;

import de.sub.goobi.helper.ProjectHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.index.query.QueryBuilder;
import org.goobi.production.flow.statistics.StepInformation;
import org.goobi.webapi.beans.Field;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.persistence.ProjectDAO;
import org.kitodo.data.elasticsearch.exceptions.CustomResponseException;
import org.kitodo.data.elasticsearch.index.Indexer;
import org.kitodo.data.elasticsearch.index.type.ProjectType;
import org.kitodo.data.elasticsearch.search.SearchResult;
import org.kitodo.data.elasticsearch.search.Searcher;
import org.kitodo.data.elasticsearch.search.enums.SearchCondition;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.services.ServiceManager;
import org.kitodo.services.data.base.TitleSearchService;

public class ProjectService extends TitleSearchService<Project> {

    private List<StepInformation> commonWorkFlow = null;
    private ProjectDAO projectDAO = new ProjectDAO();
    private ProjectType projectType = new ProjectType();
    private Indexer<Project, ProjectType> indexer = new Indexer<>(Project.class);
    private final ServiceManager serviceManager = new ServiceManager();
    private static final Logger logger = LogManager.getLogger(ProjectService.class);

    /**
     * Constructor with searcher's assigning.
     */
    public ProjectService() {
        super(new Searcher(Project.class));
    }

    /**
     * Method saves project object to database.
     *
     * @param project
     *            object
     */
    public void saveToDatabase(Project project) throws DAOException {
        projectDAO.save(project);
    }

    /**
     * Method saves project document to the index of Elastic Search.
     *
     * @param project
     *            object
     */
    public void saveToIndex(Project project) throws CustomResponseException, IOException {
        indexer.setMethod(HTTPMethods.PUT);
        indexer.performSingleRequest(project, projectType);
    }

    /**
     * Method saves processes related to modified project.
     *
     * @param project
     *            object
     */
    protected void manageDependenciesForIndex(Project project)
            throws CustomResponseException, DataException, IOException {
        for (Process process : project.getProcesses()) {
            serviceManager.getProcessService().saveToIndex(process);
        }
    }

    public Project find(Integer id) throws DAOException {
        return projectDAO.find(id);
    }

    public List<Project> findAll() {
        return projectDAO.findAll();
    }

    /**
     * Method removes project object from database.
     *
     * @param project
     *            object
     */
    public void removeFromDatabase(Project project) throws DAOException {
        projectDAO.remove(project);
    }

    /**
     * Method removes project object from database.
     *
     * @param id
     *            of project object
     */
    public void removeFromDatabase(Integer id) throws DAOException {
        projectDAO.remove(id);
    }

    /**
     * Method removes project object from index of Elastic Search.
     *
     * @param project
     *            object
     */
    public void removeFromIndex(Project project) throws CustomResponseException, IOException {
        indexer.setMethod(HTTPMethods.DELETE);
        indexer.performSingleRequest(project, projectType);
    }

    public List<Project> search(String query) throws DAOException {
        return projectDAO.search(query);
    }

    /**
     * Find projects for exact start date.
     *
     * @param startDate
     *            of the searched projects as Date
     * @param searchCondition
     *            as SearchCondition - bigger, smaller and so on
     * @return list of search results
     */
    public List<SearchResult> findByStartDate(Date startDate, SearchCondition searchCondition) throws DataException {
        QueryBuilder query = createSimpleCompareDateQuery("startDate", startDate, searchCondition);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find projects for exact end date.
     *
     * @param endDate
     *            of the searched projects as Date
     * @param searchCondition
     *            as SearchCondition - bigger, smaller and so on
     * @return list of search results
     */
    public List<SearchResult> findByEndDate(Date endDate, SearchCondition searchCondition) throws DataException {
        QueryBuilder query = createSimpleCompareDateQuery("endDate", endDate, searchCondition);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find projects for exact amount of pages.
     * 
     * @param numberOfPages
     *            as Integer
     * @param searchCondition
     *            as SearchCondition - bigger, smaller and so on
     * @return list of search results
     */
    public List<SearchResult> findByNumberOfPages(Integer numberOfPages, SearchCondition searchCondition)
            throws DataException {
        QueryBuilder query = createSimpleCompareQuery("numberOfPages", numberOfPages, searchCondition);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find projects for exact amount of volumes.
     *
     * @param numberOfVolumes
     *            as Integer
     * @param searchCondition
     *            as SearchCondition - bigger, smaller and so on
     * @return list of search results
     */
    public List<SearchResult> findByNumberOfVolumes(Integer numberOfVolumes, SearchCondition searchCondition)
            throws DataException {
        QueryBuilder query = createSimpleCompareQuery("numberOfVolumes", numberOfVolumes, searchCondition);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find archived or not archived projects.
     * 
     * @param archived
     *            if true - find archived projects, if false - find not archived
     *            projects
     * @return list of search results
     */
    public List<SearchResult> findByArchived(Boolean archived) throws DataException {
        QueryBuilder query = createSimpleQuery("archived", archived.toString(), true);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find project by id of process.
     *
     * @param id
     *            of process
     * @return search result
     */
    public SearchResult findByProcessId(Integer id) throws DataException {
        QueryBuilder query = createSimpleQuery("processes.id", id, true);
        return searcher.findDocument(query.toString());
    }

    /**
     * Find projects by title of process.
     *
     * @param title
     *            of process
     * @return list of search results with projects for specific process title
     */
    public List<SearchResult> findByProcessTitle(String title) throws DataException {
        List<SearchResult> projects = new ArrayList<>();

        List<SearchResult> processes = serviceManager.getProcessService().findByTitle(title, true);
        for (SearchResult process : processes) {
            projects.add(findByProcessId(process.getId()));
        }
        return projects;
    }

    /**
     * Find project by id of user.
     *
     * @param id
     *            of user
     * @return list of search results
     */
    public List<SearchResult> findByUserId(Integer id) throws DataException {
        QueryBuilder query = createSimpleQuery("users.id", id, true);
        return searcher.findDocuments(query.toString());
    }

    /**
     * Find projects by login of user.
     *
     * @param login
     *            of user
     * @return list of search result with projects for specific user login
     */
    public List<SearchResult> findByUserLogin(String login) throws DataException {
        SearchResult user = serviceManager.getUserService().findByLogin(login);
        return findByUserId(user.getId());
    }

    /**
     * Method adds all object found in database to Elastic Search index.
     */
    public void addAllObjectsToIndex() throws CustomResponseException, InterruptedException, IOException {
        indexer.setMethod(HTTPMethods.PUT);
        indexer.performMultipleRequests(findAll(), projectType);
    }

    /**
     * Get workflow.
     *
     * @return a list with information for each step on workflow
     */

    public List<StepInformation> getWorkFlow(Project project) {
        if (this.commonWorkFlow == null) {
            if (project.getId() != null) {
                this.commonWorkFlow = ProjectHelper.getProjectWorkFlowOverview(project);
            } else {
                this.commonWorkFlow = new ArrayList<>();
            }
        }
        return this.commonWorkFlow;
    }

    @XmlElement(name = "field")
    public List<Field> getFieldConfig(Project project) throws IOException {
        return Field.getFieldConfigForProject(project);
    }
}
