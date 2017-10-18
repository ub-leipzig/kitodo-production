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
package org.kitodo.services;

import org.kitodo.services.data.BatchService;
import org.kitodo.services.data.DocketService;
import org.kitodo.services.data.HistoryService;
import org.kitodo.services.data.LdapGroupService;
import org.kitodo.services.data.ProcessService;
import org.kitodo.services.data.ProjectFileGroupService;
import org.kitodo.services.data.ProjectService;
import org.kitodo.services.data.PropertyService;
import org.kitodo.services.data.RulesetService;
import org.kitodo.services.data.TaskService;
import org.kitodo.services.data.TemplateService;
import org.kitodo.services.data.UserGroupService;
import org.kitodo.services.data.UserService;
import org.kitodo.services.data.WorkpieceService;
import org.kitodo.services.file.FileService;

public class ServiceManager {

    private BatchService batchService;
    private DocketService docketService;
    private HistoryService historyService;
    private LdapGroupService ldapGroupService;
    private PropertyService propertyService;
    private ProcessService processService;
    private ProjectFileGroupService projectFileGroupService;
    private ProjectService projectService;
    private RulesetService rulesetService;
    private TaskService taskService;
    private TemplateService templateService;
    private UserGroupService userGroupService;
    private UserService userService;
    private WorkpieceService workpieceService;
    private FileService fileService;

    private void initializeBatchService() {
        if (batchService == null) {
            batchService = new BatchService();
        }
    }

    private void initializeDocketService() {
        if (docketService == null) {
            docketService = new DocketService();
        }
    }

    private void initializeHistoryService() {
        if (historyService == null) {
            historyService = new HistoryService();
        }
    }

    private void initializeLdapGroupService() {
        if (ldapGroupService == null) {
            ldapGroupService = new LdapGroupService();
        }
    }

    private void initializePropertyService() {
        if (propertyService == null) {
            propertyService = new PropertyService();
        }
    }

    private void initializeProcessService() {
        if (processService == null) {
            processService = new ProcessService();
        }
    }

    private void initializeProjectFileGroupService() {
        if (projectFileGroupService == null) {
            projectFileGroupService = new ProjectFileGroupService();
        }
    }

    private void initializeProjectService() {
        if (projectService == null) {
            projectService = new ProjectService();
        }
    }

    private void initializeRulesetService() {
        if (rulesetService == null) {
            rulesetService = new RulesetService();
        }
    }

    private void initializeTaskService() {
        if (taskService == null) {
            taskService = new TaskService();
        }
    }

    private void initializeTemplateService() {
        if (templateService == null) {
            templateService = new TemplateService();
        }
    }

    private void initializeUserGroupService() {
        if (userGroupService == null) {
            userGroupService = new UserGroupService();
        }
    }

    private void initializeUserService() {
        if (userService == null) {
            userService = new UserService();
        }
    }

    private void initializeWorkpieceService() {
        if (workpieceService == null) {
            workpieceService = new WorkpieceService();
        }
    }

    private void initializeFileService() {
        if (fileService == null) {
            fileService = new FileService();
        }
    }

    /**
     * Initialize BatchService if it is not yet initialized and next return it.
     *
     * @return BatchService object
     */
    public BatchService getBatchService() {
        initializeBatchService();
        return batchService;
    }

    /**
     * Initialize DocketService if it is not yet initialized and next return it.
     *
     * @return DocketService object
     */
    public DocketService getDocketService() {
        initializeDocketService();
        return docketService;
    }

    /**
     * Initialize HistoryService if it is not yet initialized and next return
     * it.
     *
     * @return HistoryService object
     */
    public HistoryService getHistoryService() {
        initializeHistoryService();
        return historyService;
    }

    /**
     * Initialize LdapGroupService if it is not yet initialized and next return
     * it.
     *
     * @return LdapGroupService object
     */
    public LdapGroupService getLdapGroupService() {
        initializeLdapGroupService();
        return ldapGroupService;
    }

    /**
     * Initialize PropertyService if it is not yet initialized and next return
     * it.
     *
     * @return PropertyService object
     */
    public PropertyService getPropertyService() {
        initializePropertyService();
        return propertyService;
    }

    /**
     * Initialize ProcessService if it is not yet initialized and next return
     * it.
     *
     * @return ProcessService object
     */
    public ProcessService getProcessService() {
        initializeProcessService();
        return processService;
    }

    /**
     * Initialize ProjectFileGroupService if it is not yet initialized and next
     * return it.
     *
     * @return ProjectFileGroupService object
     */
    public ProjectFileGroupService getProjectFileGroupService() {
        initializeProjectFileGroupService();
        return projectFileGroupService;
    }

    /**
     * Initialize ProjectService if it is not yet initialized and next return
     * it.
     *
     * @return ProjectService object
     */
    public ProjectService getProjectService() {
        initializeProjectService();
        return projectService;
    }

    /**
     * Initialize RulesetService if it is not yet initialized and next return
     * it.
     *
     * @return RulesetService object
     */
    public RulesetService getRulesetService() {
        initializeRulesetService();
        return rulesetService;
    }

    /**
     * Initialize TaskService if it is not yet initialized and next return it.
     *
     * @return TaskService object
     */
    public TaskService getTaskService() {
        initializeTaskService();
        return taskService;
    }

    /**
     * Initialize TemplateService if it is not yet initialized and next return
     * it.
     *
     * @return TemplateService object
     */
    public TemplateService getTemplateService() {
        initializeTemplateService();
        return templateService;
    }

    /**
     * Initialize UserGroupService if it is not yet initialized and next return
     * it.
     *
     * @return UserGroupService object
     */
    public UserGroupService getUserGroupService() {
        initializeUserGroupService();
        return userGroupService;
    }

    /**
     * Initialize UserService if it is not yet initialized and next return it.
     *
     * @return UserService object
     */
    public UserService getUserService() {
        initializeUserService();
        return userService;
    }

    /**
     * Initialize WorkpieceService if it is not yet initialized and next return
     * it.
     *
     * @return WorkpieceService object
     */
    public WorkpieceService getWorkpieceService() {
        initializeWorkpieceService();
        return workpieceService;
    }

    /**
     * Initialize FileService if it is not yet initialized and next return it.
     *
     * @return FileService object
     */
    public FileService getFileService() {
        initializeFileService();
        return fileService;
    }

}
