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
package org.goobi.production.chart;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A ProjectStatusDataTable object holds all the information needed for
 * rendering a project status chart
 *
 * <p>
 * A ProjectStatusDataTable consists of - a name - dates of project begin and
 * project end - a list of project tasks
 * </p>
 *
 * @author Karsten Köhler
 * @author Hendrik Söhnholz
 * @author Steffen Hankiewicz
 * @version 30.10.2009
 *
 * @see ProjectTask
 */
public class ProjectStatusDataTable implements Serializable {
    private static final long serialVersionUID = -6649337945039135394L;
    private String name;

    private Date projectBegin;
    private Date projectEnd;

    private List<ProjectTask> projectTasks;
    private List<String> taskTitles;

    /**
     * public constructor, the name is set here.
     *
     * @param inName
     *            the name to set
     */
    public ProjectStatusDataTable(String inName, Date begin, Date end) {
        super();
        this.name = inName;
        projectBegin = begin;
        projectEnd = end;
        projectTasks = new ArrayList<ProjectTask>();
        taskTitles = new ArrayList<String>();
    }

    /**
     * Remove a task from the list.
     *
     * @param title
     *            String
     */
    public void removeTask(String title) {
        if (taskTitles.contains(title)) {
            int taskIndex = taskTitles.indexOf(title);
            projectTasks.remove(taskIndex);
            taskTitles.remove(taskIndex);

        }
    }

    /**
     * Add a task to the list.
     *
     * @param inTask
     *            The task to add
     *
     */
    public void addTask(IProjectTask inTask) {
        if (!(taskTitles.contains(inTask.getTitle()))) {
            projectTasks.add(new ProjectTask(inTask.getTitle(), inTask.getStepsCompleted(), inTask.getStepsMax()));
            taskTitles.add(inTask.getTitle());

        } else {
            // There can be only one task with this title.
            int taskIndex = taskTitles.indexOf(inTask.getTitle());
            ProjectTask task = projectTasks.get(taskIndex);
            task.setStepsCompleted(inTask.getStepsCompleted());
            task.setStepsMax(inTask.getStepsMax());
        }

    }

    /**
     * getter for name.
     *
     * @return name as string
     */
    public String getName() {
        return name;
    }

    /**
     * setter for name.
     *
     * @param name
     *            as string
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * getter for all tasks.
     *
     * @return list of {@link ProjectTask}
     */
    public List<ProjectTask> getTasks() {
        return projectTasks;
    }

    /**
     * getter for size of task list.
     *
     * @return number of tasks
     */
    public int getNumberOfTasks() {
        if (projectTasks == null) {
            return 0;
        } else {
            return projectTasks.size();
        }
    }

    /**
     * getter for index of task in list.
     *
     * @return index of task
     */
    public int getTaskIndex(String title) {
        if (taskTitles.contains(title)) {
            return taskTitles.indexOf(title);
        } else {
            return -1;
        }
    }

    /**
     * getter for projectBegin.
     *
     * @return projectBegin as Date
     */
    public Date getProjectBegin() {
        return projectBegin;
    }

    /**
     * getter for projectEnd.
     *
     * @return projectEnd as Date
     */
    public Date getProjectEnd() {
        return projectEnd;
    }

}
