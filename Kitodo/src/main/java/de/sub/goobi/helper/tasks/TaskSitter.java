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
package de.sub.goobi.helper.tasks;

import de.sub.goobi.config.ConfigCore;
import de.sub.goobi.helper.tasks.EmptyTask.Behaviour;

import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.joda.time.Duration;

/**
 * The class TaskSitter takes care of the tasks in the task manager. While the
 * application is working, a scheduler on the TaskManager will call the run()
 * method of the TaskSitter every some seconds to delete threads that have died,
 * replace threads that are to be restarted by new copies of themselves (a
 * Thread can never be started twice) and finally starts some new threads if
 * there aren’t too many working any more. Several limits are configurable for
 * the {@link #run()} method.
 *
 * <p>
 * On shutdown of the servlet container, the TaskSitter will try to shut down
 * all threads that are still running. Because the TaskManager is singleton (its
 * constructor is private) a caring class is needed which will be available for
 * instantiation to the servlet container.
 * </p>
 * 
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 */
public class TaskSitter implements Runnable, ServletContextListener {
    private static final int KEEP_FAILED = 10;
    private static final long KEEP_FAILED_MINS = 250;
    private static final int KEEP_SUCCESSFUL = 3;
    private static final long KEEP_SUCCESSFUL_MINS = 20;

    /**
     * The field autoRunLimit holds the number of threads which at most are
     * allowed to be started automatically. It is by default initialised by the
     * number of available processors of the runtime and set to 0 while the
     * feature is disabled.
     */
    private static int autoRunLimit;

    {
        setAutoRunningThreads(true);
    }

    /**
     * When the servlet is unloaded, i.e. on container shutdown, the TaskManager
     * shall be shut down gracefully.
     *
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg) {
        TaskManager.shutdownNow();
    }

    /**
     * Currently, there is nothing to do when the servlet is loading.
     *
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
    }

    /**
     * The function isAutoRunningThreads() returns whether the TaskManager’s
     * autorun mode is on or not.
     *
     * @return whether the TaskManager is auto-running threads or not
     */
    public static boolean isAutoRunningThreads() {
        return autoRunLimit > 0;
    }

    /**
     * The function run() examines the task list, deletes threads that have
     * died, replaces threads that are to be restarted by new copies of
     * themselves and finally starts new threads up to the given limit.
     *
     * <p>
     * Several limits are configurable: There are both limits in number and in
     * time for successfully finished or erroneous threads which can be set in
     * the configuration. There are internal default values for these settings
     * too, which will be applied in case of missing configuration entries.
     * Since zombie processes will still occupy all their resources and aren’t
     * available for garbage collection, these values have been chosen rather
     * restrictive. For the limit for auto starting threads, see
     * {@link #setAutoRunningThreads(boolean)}.
     * </p>
     *
     * <p>
     * If the task list is empty, the method will exit without further delay,
     * otherwise it will initialise its variables and read the configuration.
     * Reading the configuration is done again in each iteration so
     * configuration changes will propagate here.
     * </p>
     *
     * <p>
     * Then the function iterates along the task list and takes care for each
     * task. To be able to modify the list in passing, we need a
     * {@link java.util.ListIterator} here.
     * </p>
     *
     * <p>
     * Running tasks reduce the clearance to run new tasks. (However, the
     * clearance must not become negative.) New tasks will be added to the
     * launch list, except if they have already been marked for removal, of
     * course. If a task has terminated, it is handled as specified by its
     * behaviour variable: All tasks that are marked DELETE_IMMEDIATELY will
     * instantly be disposed of; otherwise, they will be kept as long as
     * configured and only be removed if their dead body has become older. Tasks
     * marked PREPARE_FOR_RESTART will be replaced (because a
     * {@link java.lang.Thread} cannot be started a second time) by a copy of
     * them.
     * </p>
     *
     * <p>
     * If a ConcurrentModificationException arises during list examination, the
     * method will behave like a polite servant and retire silently until the
     * lordship has scarpered. This is not a pity because it will be started
     * every some seconds.
     * </p>
     *
     * </p>
     * After having finished iterating, the method will reduce the absolute
     * number of expired threads as configured. (Since new threads will be added
     * to the bottom of the list and we therefore want to remove older ones
     * top-down we cannot do this before we know their count, thus we cannot do
     * this while iterating.) Last, new threads will be started up to the
     * remaining available clearance.
     * </p>
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        TaskManager taskManager = TaskManager.singleton();
        if (taskManager.taskList.size() == 0) {
            return;
        }

        LinkedList<EmptyTask> launchableThreads = new LinkedList<EmptyTask>();
        LinkedList<EmptyTask> finishedThreads = new LinkedList<EmptyTask>();
        LinkedList<EmptyTask> failedThreads = new LinkedList<EmptyTask>();
        int availableClearance = autoRunLimit;

        int successfulMaxCount = ConfigCore.getIntParameter("taskManager.keepThreads.successful.count",
                KEEP_SUCCESSFUL);
        int failedMaxCount = ConfigCore.getIntParameter("taskManager.keepThreads.failed.count", KEEP_FAILED);
        Duration successfulMaxAge = ConfigCore.getDurationParameter("taskManager.keepThreads.successful.minutes",
                TimeUnit.MINUTES, KEEP_SUCCESSFUL_MINS);
        Duration failedMaxAge = ConfigCore.getDurationParameter("taskManager.keepThreads.failed.minutes",
                TimeUnit.MINUTES, KEEP_FAILED_MINS);

        ListIterator<EmptyTask> position = taskManager.taskList.listIterator();
        EmptyTask task;
        try {
            while (position.hasNext()) {
                task = position.next();
                switch (task.getTaskState()) {
                    case WORKING:
                    case STOPPING:
                        availableClearance = Math.max(availableClearance - 1, 0);
                        break;
                    case NEW:
                        if (Behaviour.DELETE_IMMEDIATELY.equals(task.getBehaviour())) {
                            position.remove();
                        } else {
                            launchableThreads.addLast(task);
                        }
                        break;
                    default: // cases STOPPED, FINISHED, CRASHED
                        switch (task.getBehaviour()) {
                            case DELETE_IMMEDIATELY:
                                position.remove();
                                break;
                            default: // case KEEP_FOR_A_WHILE
                                boolean taskFinishedSuccessfully = task.getException() == null;
                                Duration durationDead = task.getDurationDead();
                                if (durationDead == null) {
                                    task.setTimeOfDeath();
                                } else if (durationDead
                                        .isLongerThan(taskFinishedSuccessfully ? successfulMaxAge : failedMaxAge)) {
                                    position.remove();
                                    break;
                                }
                                if (taskFinishedSuccessfully) {
                                    finishedThreads.add(task);
                                } else {
                                    failedThreads.add(task);
                                }
                                break;
                            case PREPARE_FOR_RESTART:
                                EmptyTask replacement = task.replace();
                                if (replacement != null) {
                                    position.set(replacement);
                                    launchableThreads.addLast(replacement);
                                }
                                break;
                        }
                }
            }
        } catch (ConcurrentModificationException e) {
            return;
        }

        while (finishedThreads.size() > successfulMaxCount && (task = finishedThreads.pollFirst()) != null) {
            taskManager.taskList.remove(task);
        }

        while (failedThreads.size() > failedMaxCount && (task = failedThreads.pollFirst()) != null) {
            taskManager.taskList.remove(task);
        }

        while (launchableThreads.size() > availableClearance) {
            launchableThreads.removeLast();
        }
        while ((task = launchableThreads.pollFirst()) != null) {
            task.start();
        }
    }

    /**
     * The function setAutoRunningThreads() turns the feature to auto-run tasks
     * on or off. To enable, it will set the limit of auto running threads to
     * the number of available cores of the runtime or to the value set in the
     * global configuration file, if any. To disable auto-running it will set
     * the number to 0.
     *
     * @param on
     *            whether the TaskManager shall auto-run threads
     */
    public static void setAutoRunningThreads(boolean on) {
        if (on) {
            int cores = Runtime.getRuntime().availableProcessors();
            autoRunLimit = ConfigCore.getIntParameter("taskManager.autoRunLimit", cores);
        } else {
            autoRunLimit = 0;
        }
    }
}
