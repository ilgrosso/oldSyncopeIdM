/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.syncope.core.rest.controller;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.ArrayUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.StatefulJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.SchedTaskTO;
import org.syncope.client.to.SyncTaskTO;
import org.syncope.client.to.TaskExecTO;
import org.syncope.client.to.TaskTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.JobInstanceLoader;
import org.syncope.core.notification.NotificationManager;
import org.syncope.core.persistence.beans.NotificationTask;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.SchedTask;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.propagation.PropagationManager;
import org.syncope.core.rest.data.TaskDataBinder;
import org.syncope.core.scheduling.AbstractTaskJob;
import org.syncope.core.scheduling.NotificationJob;
import org.syncope.core.scheduling.ReportJob;
import org.syncope.core.scheduling.SyncJob;
import org.syncope.core.scheduling.SyncJobActions;
import org.syncope.core.util.TaskUtil;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationTaskExecStatus;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/task")
public class TaskController extends AbstractController {

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private TaskExecDAO taskExecDAO;

    @Autowired
    private TaskDataBinder binder;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private JobInstanceLoader jobInstanceLoader;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private ResourcePatternResolver resResolver;

    @PreAuthorize("hasRole('TASK_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create/sync")
    public TaskTO createSyncTask(final HttpServletResponse response,
            @RequestBody final SyncTaskTO taskTO)
            throws NotFoundException {

        return createSchedTask(response, taskTO);
    }

    @PreAuthorize("hasRole('TASK_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create/sched")
    public TaskTO createSchedTask(
            final HttpServletResponse response,
            @RequestBody final SchedTaskTO taskTO)
            throws NotFoundException {

        LOG.debug("Creating task " + taskTO);

        TaskUtil taskUtil = getTaskUtil(taskTO);

        SchedTask task = binder.createSchedTask(taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(),
                    task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task "
                    + task.getId(), e);

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update/sync")
    public TaskTO updateSync(@RequestBody final SyncTaskTO taskTO)
            throws NotFoundException {

        return updateSched(taskTO);
    }

    @PreAuthorize("hasRole('TASK_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update/sched")
    public TaskTO updateSched(@RequestBody final SchedTaskTO taskTO)
            throws NotFoundException {

        LOG.debug("Task update called with parameter {}", taskTO);

        SchedTask task = taskDAO.find(taskTO.getId());
        if (task == null) {
            throw new NotFoundException(
                    "Task " + String.valueOf(taskTO.getId()));
        }

        TaskUtil taskUtil = getTaskUtil(task);

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        binder.updateSchedTask(task, taskTO, taskUtil);
        task = taskDAO.save(task);

        try {
            jobInstanceLoader.registerJob(task, task.getJobClassName(),
                    task.getCronExpression());
        } catch (Exception e) {
            LOG.error("While registering quartz job for task "
                    + task.getId(), e);

            SyncopeClientException sce = new SyncopeClientException(
                    SyncopeClientExceptionType.Scheduling);
            sce.addElement(e.getMessage());
            scce.addException(sce);
            throw scce;
        }

        return binder.getTaskTO(task, taskUtil);
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/count")
    public ModelAndView count(@PathVariable("kind") final String kind) {
        return new ModelAndView().addObject(
                taskDAO.count(getTaskUtil(kind).taskClass()));
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list")
    public List<TaskTO> list(@PathVariable("kind") final String kind) {
        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(binder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list/{page}/{size}")
    public List<TaskTO> list(
            @PathVariable("kind") final String kind,
            @PathVariable("page") final int page,
            @PathVariable("size") final int size) {

        TaskUtil taskUtil = getTaskUtil(kind);

        List<Task> tasks = taskDAO.findAll(page, size, taskUtil.taskClass());
        List<TaskTO> taskTOs = new ArrayList<TaskTO>(tasks.size());
        for (Task task : tasks) {
            taskTOs.add(binder.getTaskTO(task, taskUtil));
        }

        return taskTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/execution/list")
    public List<TaskExecTO> listExecutions(
            @PathVariable("kind") final String kind) {

        List<TaskExec> executions = taskExecDAO.findAll(
                getTaskUtil(kind).taskClass());
        List<TaskExecTO> executionTOs =
                new ArrayList<TaskExecTO>(executions.size());
        for (TaskExec execution : executions) {
            executionTOs.add(binder.getTaskExecTO(execution));
        }

        return executionTOs;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/jobClasses")
    public ModelAndView getJobClasses() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        Set<String> jobClasses = new HashSet<String>();
        try {
            for (Resource resource : resResolver.getResources(
                    "classpath*:**/*.class")) {

                ClassMetadata metadata =
                        cachingMetadataReaderFactory.getMetadataReader(
                        resource).getClassMetadata();
                if (ArrayUtils.contains(metadata.getInterfaceNames(),
                        Job.class.getName())
                        || AbstractTaskJob.class.getName().equals(
                        metadata.getSuperClassName())
                        || ArrayUtils.contains(metadata.getInterfaceNames(),
                        StatefulJob.class.getName())) {

                    try {
                        Class jobClass = Class.forName(metadata.getClassName());
                        if (!Modifier.isAbstract(jobClass.getModifiers())
                                && !metadata.hasEnclosingClass()
                                && !jobClass.equals(SyncJob.class)
                                && !jobClass.equals(ReportJob.class)
                                && !jobClass.equals(NotificationJob.class)) {

                            jobClasses.add(jobClass.getName());
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Could not load class {}",
                                metadata.getClassName(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}",
                    Job.class.getName(), e);
        }

        ModelAndView result = new ModelAndView();
        result.addObject(jobClasses);
        return result;
    }

    @PreAuthorize("hasRole('TASK_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/jobActionsClasses")
    public ModelAndView getJobActionClasses() {
        CachingMetadataReaderFactory cachingMetadataReaderFactory =
                new CachingMetadataReaderFactory();

        Set<String> jobActionsClasses = new HashSet<String>();
        try {
            for (Resource resource : resResolver.getResources(
                    "classpath*:**/*.class")) {

                ClassMetadata metadata =
                        cachingMetadataReaderFactory.getMetadataReader(
                        resource).getClassMetadata();
                if (ArrayUtils.contains(metadata.getInterfaceNames(),
                        SyncJobActions.class.getName())) {

                    try {
                        Class jobClass = Class.forName(metadata.getClassName());
                        if (!Modifier.isAbstract(jobClass.getModifiers())) {
                            jobActionsClasses.add(jobClass.getName());
                        }
                    } catch (ClassNotFoundException e) {
                        LOG.error("Could not load class {}",
                                metadata.getClassName(), e);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("While searching for class implementing {}",
                    SyncJobActions.class.getName(), e);
        }

        ModelAndView result = new ModelAndView();
        result.addObject(jobActionsClasses);
        return result;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{taskId}")
    public TaskTO read(@PathVariable("taskId") final Long taskId)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        return binder.getTaskTO(task, getTaskUtil(task));
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/read/{executionId}")
    public TaskExecTO readExecution(
            @PathVariable("executionId") final Long executionId)
            throws NotFoundException {

        TaskExec execution = taskExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        return binder.getTaskExecTO(execution);
    }

    @PreAuthorize("hasRole('TASK_EXECUTE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/execute/{taskId}")
    public TaskExecTO execute(@PathVariable("taskId") final Long taskId,
            @RequestParam(value = "dryRun", defaultValue = "false") final boolean dryRun)
            throws NotFoundException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        TaskExecTO result = null;
        LOG.debug("Execution started for {}", task);
        switch (getTaskUtil(task)) {
            case PROPAGATION:
                final TaskExec propExec = propagationManager.execute(
                        (PropagationTask) task);
                result = binder.getTaskExecTO(propExec);
                break;

            case NOTIFICATION:
                final TaskExec notExec = notificationManager.execute(
                        (NotificationTask) task);
                result = binder.getTaskExecTO(notExec);
                break;

            case SCHED:
            case SYNC:
                try {
                    jobInstanceLoader.registerJob(task,
                            ((SchedTask) task).getJobClassName(),
                            ((SchedTask) task).getCronExpression());

                    JobDataMap map = new JobDataMap();
                    map.put(AbstractTaskJob.DRY_RUN_JOBDETAIL_KEY, dryRun);
                    scheduler.getScheduler().triggerJob(
                            JobInstanceLoader.getJobName(task),
                            Scheduler.DEFAULT_GROUP, map);
                } catch (Exception e) {
                    LOG.error("While executing task {}", task, e);

                    SyncopeClientCompositeErrorException scce =
                            new SyncopeClientCompositeErrorException(
                            HttpStatus.BAD_REQUEST);
                    SyncopeClientException sce = new SyncopeClientException(
                            SyncopeClientExceptionType.Scheduling);
                    sce.addElement(e.getMessage());
                    scce.addException(sce);
                    throw scce;
                }

                result = new TaskExecTO();
                result.setTask(taskId);
                result.setStartDate(new Date());
                result.setStatus("JOB_FIRED");
                result.setMessage("Job fired; waiting for results...");
                break;

            default:
        }
        LOG.debug("Execution finished for {}, {}", task, result);

        return result;
    }

    @PreAuthorize("hasRole('TASK_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/execution/report/{executionId}")
    public TaskExecTO report(
            @PathVariable("executionId") final Long executionId,
            @RequestParam("executionStatus")
            final PropagationTaskExecStatus status,
            @RequestParam("message") final String message)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExec exec = taskExecDAO.find(executionId);
        if (exec == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        SyncopeClientException invalidReportException =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPropagationTaskExecReport);

        TaskUtil taskUtil = getTaskUtil(exec.getTask());
        if (taskUtil != TaskUtil.PROPAGATION) {
            invalidReportException.addElement("Task type: " + taskUtil);
        } else {
            PropagationTask task = (PropagationTask) exec.getTask();
            if (task.getPropagationMode() != PropagationMode.TWO_PHASES) {
                invalidReportException.addElement(
                        "Propagation mode: " + task.getPropagationMode());
            }
        }

        switch (status) {
            case SUCCESS:
            case FAILURE:
                break;

            case CREATED:
            case SUBMITTED:
            case UNSUBMITTED:
                invalidReportException.addElement(
                        "Execution status to be set: " + status);
                break;

            default:
        }

        if (!invalidReportException.isEmpty()) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            scce.addException(invalidReportException);
            throw scce;
        }

        exec.setStatus(status.toString());
        exec.setMessage(message);
        exec = taskExecDAO.save(exec);

        return binder.getTaskExecTO(exec);
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{taskId}")
    public void delete(@PathVariable("taskId") Long taskId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        Task task = taskDAO.find(taskId);
        if (task == null) {
            throw new NotFoundException("Task " + taskId);
        }

        if (TaskUtil.SCHED == getTaskUtil(task)
                || TaskUtil.SYNC == getTaskUtil(task)) {

            jobInstanceLoader.unregisterJob(task);
        }

        taskDAO.delete(task);
    }

    @PreAuthorize("hasRole('TASK_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/execution/delete/{executionId}")
    public void deleteExecution(@PathVariable("executionId") Long executionId)
            throws NotFoundException, SyncopeClientCompositeErrorException {

        TaskExec execution = taskExecDAO.find(executionId);
        if (execution == null) {
            throw new NotFoundException("Task execution " + executionId);
        }

        taskExecDAO.delete(execution);
    }
}
