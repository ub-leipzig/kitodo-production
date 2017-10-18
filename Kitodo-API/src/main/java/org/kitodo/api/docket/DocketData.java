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
package org.kitodo.api.docket;

import java.util.ArrayList;

public class DocketData {

    /** The name of the process. */
    private String processName;
    /** The id of the process. */
    private String processId;
    /** The name of the project. */
    private String projectName;
    /** The name of the used ruleset. */
    private String rulesetName;
    /** The creation Date of the process. */
    private String creationDate;
    /** A comment. */
    private String comment;
    /** The template properties. */
    private ArrayList<Property> templateProperties;
    /** The workpiece properties. */
    private ArrayList<Property> workpieceProperties;
    /** The process properties. */
    private ArrayList<Property> processProperties;

    /**
     * Gets the processName.
     * 
     * @return The processName.
     */
    public String getProcessName() {
        return processName;
    }

    /**
     * Sets the processName.
     * 
     * @param processName
     *            The query to execute.
     */
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    /**
     * Gets the processId.
     * 
     * @return The processId.
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * Sets the processId.
     * 
     * @param processId
     *            The processId.
     */
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * Gets the projectName.
     * 
     * @return The projectName.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Sets the projectName.
     * 
     * @param projectName
     *            The projectName.
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * Gets the rulesetName.
     * 
     * @return The rulesetName.
     */
    public String getRulesetName() {
        return rulesetName;
    }

    /**
     * Sets the rulesetName.
     * 
     * @param rulesetName
     *            The rulesetName.
     */
    public void setRulesetName(String rulesetName) {
        this.rulesetName = rulesetName;
    }

    /**
     * Gets the creationDate.
     * 
     * @return The creationDate.
     */
    public String getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creationDate.
     * 
     * @param creationDate
     *            The creationDate.
     */
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Gets the comment.
     * 
     * @return The comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the comment.
     * 
     * @param comment
     *            The comment.
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Gets the templateProperties.
     * 
     * @return The templateProperties.
     */
    public ArrayList<Property> getTemplateProperties() {
        return templateProperties;
    }

    /**
     * Sets the templateProperties.
     * 
     * @param templateProperties
     *            The templateProperties.
     */
    public void setTemplateProperties(ArrayList<Property> templateProperties) {
        this.templateProperties = templateProperties;
    }

    /**
     * Gets the workpieceProperties.
     * 
     * @return The workpieceProperties.
     */
    public ArrayList<Property> getWorkpieceProperties() {
        return workpieceProperties;
    }

    /**
     * Sets the workpieceProperties.
     * 
     * @param workpieceProperties
     *            The workpieceProperties.
     */
    public void setWorkpieceProperties(ArrayList<Property> workpieceProperties) {
        this.workpieceProperties = workpieceProperties;
    }

    /**
     * Gets the processProperties.
     * 
     * @return The processProperties.
     */
    public ArrayList<Property> getProcessProperties() {
        return processProperties;
    }

    /**
     * Sets the processProperties.
     * 
     * @param processProperties
     *            The processProperties.
     */
    public void setProcessProperties(ArrayList<Property> processProperties) {
        this.processProperties = processProperties;
    }
}
