/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.workflow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javassist.NotFoundException;
import javax.annotation.Resource;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.FormService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.UserTO;
import org.syncope.client.to.WorkflowDefinitionTO;
import org.syncope.client.to.WorkflowFormPropertyTO;
import org.syncope.client.to.WorkflowFormTO;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.propagation.PropagationByResource;
import org.syncope.core.rest.controller.UnauthorizedRoleException;
import org.syncope.types.WorkflowFormPropertyType;

/**
 * Activiti (http://www.activiti.org/) based implementation.
 */
public class ActivitiUserWorkflowAdapter extends AbstractUserWorkflowAdapter {

    /**
     * Logger.
     */
    private static final Logger LOG =
            LoggerFactory.getLogger(ActivitiUserWorkflowAdapter.class);

    private static final String[] PROPERTY_IGNORE_PROPS = {"type"};

    public static final String WF_PROCESS_ID = "userWorkflow";

    public static final String WF_PROCESS_RESOURCE = "userWorkflow.bpmn20.xml";

    public static final String SYNCOPE_USER = "syncopeUser";

    public static final String USER_TO = "userTO";

    public static final String USER_MOD = "userMod";

    public static final String EMAIL_KIND = "emailKind";

    public static final String ACTION = "action";

    public static final String TOKEN = "token";

    public static final String PROP_BY_RESOURCE = "propByResource";

    public static final String PROPAGATE_ENABLE = "propagateEnable";

    @Resource(name = "adminUser")
    private String adminUser;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private FormService formService;

    @Autowired
    private RepositoryService repositoryService;

    private void updateStatus(final SyncopeUser user) {
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(
                user.getWorkflowId()).list();
        if (tasks.isEmpty() || tasks.size() > 1) {
            LOG.warn("While setting user status: unexpected task number ({})",
                    tasks.size());
        } else {
            user.setStatus(tasks.get(0).getTaskDefinitionKey());
        }
    }

    @Override
    public Map.Entry<Long, Boolean> create(final UserTO userTO)
            throws WorkflowException {

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(USER_TO, userTO);

        final ProcessInstance processInstance;
        try {
            processInstance = runtimeService.startProcessInstanceByKey(
                    "userWorkflow", variables);
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        SyncopeUser user = (SyncopeUser) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), SYNCOPE_USER);
        updateStatus(user);
        user = userDAO.save(user);

        // create and save Activiti user
        User activitiUser = identityService.newUser(user.getUsername());
        identityService.saveUser(activitiUser);

        Boolean enable = (Boolean) runtimeService.getVariable(
                processInstance.getProcessInstanceId(), PROPAGATE_ENABLE);

        return new DefaultMapEntry(user.getId(), enable);
    }

    private void doExecuteAction(final SyncopeUser user,
            final String action, final Map<String, Object> moreVariables)
            throws WorkflowException {

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(SYNCOPE_USER, user);
        variables.put(ACTION, action);
        if (moreVariables != null && !moreVariables.isEmpty()) {
            variables.putAll(moreVariables);
        }

        if (StringUtils.isBlank(user.getWorkflowId())) {
            throw new WorkflowException(
                    new NotFoundException("Empty workflow id"));
        }

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(
                user.getWorkflowId()).list();
        if (tasks.size() != 1) {
            LOG.warn("Expected a single task, found {}", tasks.size());
        } else {
            try {
                taskService.complete(tasks.get(0).getId(), variables);
            } catch (ActivitiException e) {
                throw new WorkflowException(e);
            }
        }
    }

    @Override
    protected Long doActivate(final SyncopeUser user, final String token)
            throws WorkflowException {

        doExecuteAction(user, "activate",
                Collections.singletonMap(TOKEN, (Object) token));
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Map.Entry<Long, PropagationByResource> doUpdate(
            final SyncopeUser user, final UserMod userMod)
            throws WorkflowException {

        doExecuteAction(user, "update",
                Collections.singletonMap(USER_MOD, (Object) userMod));
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        PropagationByResource propByRes =
                (PropagationByResource) runtimeService.getVariable(
                user.getWorkflowId(), PROP_BY_RESOURCE);

        return new DefaultMapEntry(updated.getId(), propByRes);
    }

    @Override
    protected Long doSuspend(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "suspend", null);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected Long doReactivate(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "reactivate", null);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    protected void doDelete(final SyncopeUser user)
            throws WorkflowException {

        doExecuteAction(user, "delete", null);
        userDAO.delete(user);

        identityService.deleteUser(user.getUsername());
    }

    @Override
    public Long execute(final UserTO userTO, final String actionId)
            throws UnauthorizedRoleException, NotFoundException,
            WorkflowException {

        SyncopeUser user = dataBinder.getUserFromId(userTO.getId());

        final Map<String, Object> variables = new HashMap<String, Object>();
        variables.put(USER_TO, userTO);

        doExecuteAction(user, actionId, variables);
        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }

    @Override
    public WorkflowDefinitionTO getDefinition()
            throws WorkflowException {

        ProcessDefinition procDef;
        try {
            procDef = repositoryService.createProcessDefinitionQuery().
                    processDefinitionKey(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_ID).latestVersion().
                    singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        InputStream is = repositoryService.getResourceAsStream(
                procDef.getDeploymentId(), WF_PROCESS_RESOURCE);

        Writer writer = new StringWriter();

        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (IOException e) {
            LOG.error("While reading workflow definition {}",
                    procDef.getKey(), e);
        } finally {
            try {
                is.close();
            } catch (IOException ioe) {
                LOG.error("While closing input stream for {}",
                        procDef.getKey(), ioe);
            }
        }

        WorkflowDefinitionTO definitionTO = new WorkflowDefinitionTO();
        definitionTO.setId(ActivitiUserWorkflowAdapter.WF_PROCESS_ID);
        definitionTO.setXmlDefinition(writer.toString());

        return definitionTO;
    }

    @Override
    public void updateDefinition(
            final WorkflowDefinitionTO definition)
            throws NotFoundException, WorkflowException {

        if (!ActivitiUserWorkflowAdapter.WF_PROCESS_ID.equals(
                definition.getId())) {

            throw new NotFoundException("Workflow process id "
                    + definition.getId());
        }

        try {
            repositoryService.createDeployment().addInputStream(
                    ActivitiUserWorkflowAdapter.WF_PROCESS_RESOURCE,
                    new ByteArrayInputStream(
                    definition.getXmlDefinition().getBytes())).deploy();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }
    }

    private WorkflowFormPropertyType fromActivitiFormType(
            final FormType activitiFormType) {

        WorkflowFormPropertyType result = WorkflowFormPropertyType.String;

        if ("string".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.String;
        }
        if ("long".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Long;
        }
        if ("enum".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Enum;
        }
        if ("date".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Date;
        }
        if ("boolean".equals(activitiFormType.getName())) {
            result = WorkflowFormPropertyType.Boolean;
        }

        return result;
    }

    private WorkflowFormTO getFormTO(final Task task,
            final TaskFormData formData) {

        WorkflowFormTO formTO = new WorkflowFormTO();
        formTO.setTaskId(task.getId());
        formTO.setKey(formData.getFormKey());

        BeanUtils.copyProperties(task, formTO);

        WorkflowFormPropertyTO propertyTO;
        for (FormProperty fProp : formData.getFormProperties()) {
            propertyTO = new WorkflowFormPropertyTO();
            BeanUtils.copyProperties(fProp, propertyTO,
                    PROPERTY_IGNORE_PROPS);
            propertyTO.setType(fromActivitiFormType(fProp.getType()));

            formTO.addProperty(propertyTO);
        }

        return formTO;
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        List<WorkflowFormTO> forms = new ArrayList<WorkflowFormTO>();

        TaskFormData formData;
        for (Task task : taskService.createTaskQuery().list()) {
            try {
                formData = formService.getTaskFormData(task.getId());
            } catch (ActivitiException e) {
                LOG.debug("No form found for task {}", task.getId(), e);
                formData = null;
            }

            if (formData != null && !formData.getFormProperties().isEmpty()) {
                forms.add(getFormTO(task, formData));
            }
        }

        return forms;
    }

    private Map.Entry<Task, TaskFormData> checkTask(final String taskId,
            final String userName)
            throws NotFoundException {

        Task task;
        try {
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new NotFoundException("Activiti Task " + taskId, e);
        }

        TaskFormData formData;
        try {
            formData = formService.getTaskFormData(task.getId());
        } catch (ActivitiException e) {
            throw new NotFoundException("Form for Activiti Task " + taskId, e);
        }

        if (!adminUser.equals(userName)) {
            SyncopeUser user = userDAO.find(userName);
            if (user == null) {
                throw new NotFoundException("Syncope User " + userName);
            }
        }

        return new DefaultMapEntry(task, formData);
    }

    @Override
    public WorkflowFormTO getForm(final String workflowId)
            throws NotFoundException, WorkflowException {

        Task task;
        try {
            task = taskService.createTaskQuery().processInstanceId(workflowId).
                    singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        TaskFormData formData;
        try {
            formData = formService.getTaskFormData(task.getId());
        } catch (ActivitiException e) {
            LOG.debug("No form found for task {}", task.getId(), e);
            formData = null;
        }

        WorkflowFormTO result = null;
        if (formData != null && !formData.getFormProperties().isEmpty()) {
            result = getFormTO(task, formData);
        }

        return result;
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId,
            final String userName)
            throws NotFoundException, WorkflowException {

        Map.Entry<Task, TaskFormData> checked = checkTask(taskId, userName);
        Task task;
        try {
            taskService.setOwner(taskId, userName);
            task = taskService.createTaskQuery().taskId(taskId).singleResult();
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        return getFormTO(task, checked.getValue());
    }

    @Override
    public Long submitForm(final WorkflowFormTO form, final String userName)
            throws NotFoundException, WorkflowException {

        Map.Entry<Task, TaskFormData> checked =
                checkTask(form.getTaskId(), userName);

        if (!checked.getKey().getOwner().equals(userName)) {
            throw new WorkflowException(new RuntimeException(
                    "Task " + form.getTaskId() + " assigned to "
                    + checked.getKey().getOwner() + " but summited by "
                    + userName));
        }

        try {
            formService.submitTaskFormData(form.getTaskId(),
                    form.getPropertiesForSubmit());
        } catch (ActivitiException e) {
            throw new WorkflowException(e);
        }

        SyncopeUser user = userDAO.findByWorkflowId(
                checked.getKey().getProcessInstanceId());
        if (user == null) {
            throw new NotFoundException("User with workflow id "
                    + checked.getKey().getProcessInstanceId());
        }

        updateStatus(user);
        SyncopeUser updated = userDAO.save(user);

        return updated.getId();
    }
}