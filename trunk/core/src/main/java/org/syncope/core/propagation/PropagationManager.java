/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.propagation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javassist.NotFoundException;
import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.identityconnectors.framework.common.FrameworkUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.syncope.client.to.AttributeTO;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.core.util.AttributableUtil;
import org.syncope.core.util.JexlUtil;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;
import org.syncope.types.IntMappingType;
import org.syncope.types.SchemaType;
import org.syncope.types.TraceLevel;

/**
 * Manage the data propagation to external resources.
 */
@Transactional(rollbackFor = {
    Throwable.class
})
public class PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(PropagationManager.class);

    /**
     * Connector instance loader.
     */
    @Autowired
    private ConnInstanceLoader connLoader;

    /**
     * User DataBinder.
     */
    @Autowired
    private UserDataBinder userDataBinder;

    /**
     * User DAO.
     */
    @Autowired
    private UserDAO userDAO;

    /**
     * Resource DAO.
     */
    @Autowired
    private ResourceDAO resourceDAO;

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Task execution DAO.
     */
    @Autowired
    private TaskExecDAO taskExecDAO;

    /**
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    private SyncopeUser getSyncopeUser(final Long userId)
            throws NotFoundException {

        SyncopeUser user = userDAO.find(userId);
        if (user == null) {
            throw new NotFoundException("User " + userId);
        }

        return user;
    }

    /**
     * Create the user on every associated resource.
     *
     * @param userId to be created
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param enable wether user must be enabled or not
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getCreateTaskIds(final Long userId,
            final String password, final List<AttributeTO> vAttrs,
            final Boolean enable)
            throws NotFoundException {

        return getCreateTaskIds(userId, password, vAttrs, enable, null);
    }

    /**
     * Create the user on every associated resource.
     *
     * @param userId to be created
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @param enable wether user must be enabled or not
     * @param syncResourceName name of external resource performing sync, hence
     * not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getCreateTaskIds(final Long userId,
            final String password, final List<AttributeTO> vAttrs,
            final Boolean enable, final String syncResourceName)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(userId);
        if (vAttrs != null && !vAttrs.isEmpty()) {
            userDataBinder.fillVirtual(user, vAttrs, AttributableUtil.USER);
            user = userDAO.save(user);
        }

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.CREATE, user.getExternalResources());
        if (syncResourceName != null) {
            propByRes.get(PropagationOperation.CREATE).remove(syncResourceName);
        }

        return provision(user, password, enable, propByRes);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param userId to be updated
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeAdded virtual attributes to be added
     * @param enable wether user must be enabled or not
     * @param propByRes operations to perform on each resource
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(final Long userId,
            final String password, final Set<String> vAttrsToBeRemoved,
            final Set<String> vAttrsToBeAdded, final Boolean enable,
            final PropagationByResource propByRes)
            throws NotFoundException {

        return getUpdateTaskIds(userId, password, vAttrsToBeRemoved,
                vAttrsToBeAdded, enable, propByRes, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param userId to be updated
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeAdded virtual attributes to be added
     * @param enable wether user must be enabled or not
     * @param propByRes operations to perform on each resource
     * @param syncResourceName name of external resource performing sync, hence
     * not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(final Long userId,
            final String password, final Set<String> vAttrsToBeRemoved,
            final Set<String> vAttrsToBeAdded, final Boolean enable,
            final PropagationByResource propByRes,
            final String syncResourceName)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(userId);

        Set<String> vAttrsToRemove = vAttrsToBeRemoved == null
                ? Collections.EMPTY_SET : vAttrsToBeRemoved;
        Set<String> vAttrsToAdd = vAttrsToBeAdded == null
                ? Collections.EMPTY_SET : vAttrsToBeAdded;
        PropagationByResource localPropByRes = userDataBinder.fillVirtual(user,
                vAttrsToRemove, vAttrsToAdd, AttributableUtil.USER);

        if (propByRes != null && !propByRes.isEmpty()) {
            localPropByRes.merge(propByRes);
        } else {
            localPropByRes.addAll(PropagationOperation.UPDATE,
                    user.getExternalResources());
        }
        if (syncResourceName != null) {
            propByRes.get(PropagationOperation.CREATE).remove(syncResourceName);
            propByRes.get(PropagationOperation.UPDATE).remove(syncResourceName);
            propByRes.get(PropagationOperation.DELETE).remove(syncResourceName);
        }

        return provision(user, password, enable, localPropByRes);
    }

    /**
     * Perform delete on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getDeleteTaskIds(final Long userId)
            throws NotFoundException {

        return getDeleteTaskIds(userId, null);
    }

    /**
     * Perform delete on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param userId to be deleted
     * @param syncResourceName name of external resource performing sync, hence
     * not to be considered for propagation
     * @return list of propagation tasks
     * @throws NotFoundException if user is not found
     */
    public List<PropagationTask> getDeleteTaskIds(final Long userId,
            final String syncResourceName)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(userId);

        final PropagationByResource propByRes = new PropagationByResource();
        propByRes.set(PropagationOperation.DELETE,
                user.getExternalResources());
        if (syncResourceName != null) {
            propByRes.get(PropagationOperation.DELETE).remove(syncResourceName);
        }

        return provision(user, null, false, propByRes);
    }

    /**
     * For given source mapping type, return the corresponding Class object.
     *
     * @param intMappingType source mapping type
     * @return corresponding Class object, if any (can be null)
     */
    private Class getIntMappingTypeClass(
            final IntMappingType intMappingType) {

        Class result;

        switch (intMappingType) {
            case UserSchema:
                result = USchema.class;
                break;

            case UserDerivedSchema:
                result = UDerSchema.class;
                break;

            case UserVirtualSchema:
                result = UVirSchema.class;
                break;

            default:
                result = null;
        }

        return result;
    }

    /**
     * Prepare an attribute for sending to a connector instance.
     * 
     * @param mapping schema mapping for the given attribute
     * @param user given user
     * @param password clear-text password
     * @return account link + prepare attributes
     * @throws ClassNotFoundException if schema type for given mapping does not
     * exists in current class loader
     */
    private Map.Entry<String, Attribute> prepareAttribute(
            final SchemaMapping mapping,
            final SyncopeUser user, final String password)
            throws ClassNotFoundException {

        AbstractSchema schema = null;
        SchemaType schemaType = null;
        List<AbstractAttrValue> values = null;
        AbstractAttrValue attrValue;
        switch (mapping.getIntMappingType()) {
            case UserSchema:
                schema = schemaDAO.find(mapping.getIntAttrName(),
                        getIntMappingTypeClass(
                        mapping.getIntMappingType()));
                schemaType = schema.getType();

                UAttr attr = user.getAttribute(mapping.getIntAttrName());
                values = attr != null
                        ? (schema.isUniqueConstraint()
                        ? Collections.singletonList(attr.getUniqueValue())
                        : attr.getValues())
                        : Collections.EMPTY_LIST;

                LOG.debug("Retrieved attribute {}", attr
                        + "\n* IntAttrName {}"
                        + "\n* IntMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getIntAttrName(),
                            mapping.getIntMappingType(), values});
                break;

            case UserVirtualSchema:
                schemaType = SchemaType.String;

                UVirAttr virAttr = user.getVirtualAttribute(
                        mapping.getIntAttrName());

                values = new ArrayList<AbstractAttrValue>();
                if (virAttr != null && virAttr.getValues() != null) {
                    for (String value : virAttr.getValues()) {
                        attrValue = new UAttrValue();
                        attrValue.setStringValue(value);
                        values.add(attrValue);
                    }
                }

                LOG.debug("Retrieved virtual attribute {}", virAttr
                        + "\n* IntAttrName {}"
                        + "\n* IntMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getIntAttrName(),
                            mapping.getIntMappingType(), values});
                break;

            case UserDerivedSchema:
                schemaType = SchemaType.String;

                UDerAttr derAttr = user.getDerivedAttribute(
                        mapping.getIntAttrName());
                attrValue = new UAttrValue();
                if (derAttr != null) {
                    attrValue.setStringValue(
                            derAttr.getValue(user.getAttributes()));

                    values = Collections.singletonList(attrValue);
                } else {
                    values = Collections.EMPTY_LIST;
                }

                LOG.debug("Retrieved attribute {}", derAttr
                        + "\n* IntAttrName {}"
                        + "\n* IntMappingType {}"
                        + "\n* Attribute values {}",
                        new Object[]{mapping.getIntAttrName(),
                            mapping.getIntMappingType(), values});
                break;


            case Username:
                schema = null;
                schemaType = SchemaType.String;

                attrValue = new UAttrValue();
                attrValue.setStringValue(user.getUsername());

                values = Collections.singletonList(attrValue);
                break;

            case SyncopeUserId:
                schema = null;
                schemaType = SchemaType.String;
                attrValue = new UAttrValue();
                attrValue.setStringValue(user.getId().toString());
                values = Collections.singletonList(attrValue);
                break;

            case Password:
                schema = null;
                schemaType = SchemaType.String;
                attrValue = new UAttrValue();

                if (password != null) {
                    attrValue.setStringValue(password);
                }

                values = Collections.singletonList(attrValue);
                break;

            default:
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Define mapping for: "
                    + "\n* ExtAttrName " + mapping.getExtAttrName()
                    + "\n* is accountId " + mapping.isAccountid()
                    + "\n* is password " + (mapping.isPassword()
                    || mapping.getIntMappingType().equals(
                    IntMappingType.Password))
                    + "\n* mandatory condition "
                    + mapping.getMandatoryCondition()
                    + "\n* Schema " + mapping.getIntAttrName()
                    + "\n* IntMappingType "
                    + mapping.getIntMappingType().toString()
                    + "\n* ClassType " + schemaType.getClassName()
                    + "\n* Values " + values);
        }

        List<Object> objValues = new ArrayList<Object>();
        for (AbstractAttrValue value : values) {
            if (!FrameworkUtil.isSupportedAttributeType(
                    Class.forName(schemaType.getClassName()))) {

                objValues.add(value.getValueAsString());
            } else {
                objValues.add(value.getValue());
            }
        }

        String accountId = null;
        if (mapping.isAccountid()) {
            accountId = objValues.iterator().next().toString();
        }

        Attribute attribute = null;
        if (mapping.isPassword()) {
            attribute = AttributeBuilder.buildPassword(
                    objValues.iterator().next().toString().toCharArray());
        }

        if (!mapping.isPassword() && !mapping.isAccountid()) {
            if (schema != null && schema.isMultivalue()) {
                attribute = AttributeBuilder.build(mapping.getExtAttrName(),
                        objValues);
            } else {
                attribute = objValues.isEmpty()
                        ? AttributeBuilder.build(mapping.getExtAttrName())
                        : AttributeBuilder.build(mapping.getExtAttrName(),
                        objValues.iterator().next());
            }
        }

        return new DefaultMapEntry(accountId, attribute);
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param user given user
     * @param password clear-text password
     * @param enable wether user must be enabled or not
     * @param resource target resource
     * @return account link + prepared attributes
     * @throws PropagationException if anything goes wrong
     */
    private Map.Entry<String, Set<Attribute>> prepareAttributes(
            final SyncopeUser user, final String password,
            final Boolean enable, final ExternalResource resource) {

        LOG.debug("Preparing resource attributes for {}"
                + " on resource {}"
                + " with attributes {}",
                new Object[]{user, resource, user.getAttributes()});

        Set<Attribute> attributes = new HashSet<Attribute>();
        String accountId = null;

        Map.Entry<String, Attribute> preparedAttribute;
        for (SchemaMapping mapping : resource.getMappings()) {
            LOG.debug("Processing schema {}", mapping.getIntAttrName());

            try {
                preparedAttribute = prepareAttribute(mapping, user, password);
                if (preparedAttribute.getKey() != null) {
                    accountId = preparedAttribute.getKey();
                }
                if (preparedAttribute.getValue() != null) {
                    attributes.add(preparedAttribute.getValue());
                }
            } catch (Throwable t) {
                LOG.debug("Attribute '{}' processing failed",
                        mapping.getIntAttrName(), t);
            }
        }

        if (!StringUtils.hasText(accountId)) {
            throw new IllegalArgumentException(
                    "Missing accountId specification for "
                    + resource.getName());
        }

        // Evaluate AccountLink expression
        String evaluatedAccountLink =
                jexlUtil.evaluate(resource.getAccountLink(), user);

        // AccountId must be propagated. It could be a simple attribute for
        // the target resource or the key (depending on the accountLink)
        if (evaluatedAccountLink.isEmpty()) {
            // add accountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            attributes.add(new Name(accountId));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evaluatedAccountLink);
            attributes.add(new Name(evaluatedAccountLink));

            // AccountId not propagated: 
            // it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        if (enable != null) {
            attributes.add(AttributeBuilder.buildEnabled(enable));
        }

        return new DefaultMapEntry(accountId, attributes);
    }

    /**
     * Implementation of the provisioning feature.
     *
     * @param user user to be provisioned
     * @param password cleartext password to be provisioned
     * @param enable wether user must be enabled or not
     * @param propByRes operation to be performed per resource
     * @return list of propagation tasks created
     * @throws PropagationException if anything goes wrong
     */
    protected List<PropagationTask> provision(final SyncopeUser user,
            final String password, final Boolean enable,
            final PropagationByResource propByRes)
            throws NotFoundException {

        LOG.debug("Provisioning with user {}:\n{}", user, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        List<ExternalResource> resources = resourceDAO.findAllByPriority();
        List<ExternalResource> resourcesByPriority;
        Map.Entry<String, Set<Attribute>> preparedAttrs;
        PropagationTask task;
        for (PropagationOperation type : PropagationOperation.values()) {
            resourcesByPriority = new ArrayList<ExternalResource>();
            for (ExternalResource resource : resources) {
                if (propByRes.get(type).contains(resource.getName())) {
                    resourcesByPriority.add(resource);
                }
            }

            for (ExternalResource resource : resourcesByPriority) {
                preparedAttrs = prepareAttributes(
                        user, password, enable, resource);

                task = new PropagationTask();
                task.setResource(resource);
                task.setResourceOperationType(type);
                task.setPropagationMode(resource.getPropagationMode());
                task.setAccountId(preparedAttrs.getKey());
                task.setOldAccountId(
                        propByRes.getOldAccountId(resource.getName()));
                task.setAttributes(preparedAttrs.getValue());

                tasks.add(task);

                LOG.debug("Execution started for {}", task);
            }
        }

        return tasks;
    }

    /**
     * Execute a list of PropagationTask, in given order.
     *
     * @param tasks to be execute, in given order
     * @throws PropagationException if propagation goes wrong: propagation is
     * interrupted as soon as the result of the communication with a primary
     * resource is in error
     */
    public void execute(final List<PropagationTask> tasks)
            throws PropagationException {

        TaskExec execution;
        PropagationTaskExecStatus execStatus;
        for (PropagationTask task : tasks) {
            LOG.debug("Execution started for {}", task);

            execution = execute(task);

            LOG.debug("Execution finished for {}, {}", task, execution);

            // Propagation is interrupted as soon as the result of the
            // communication with a primary resource is in error
            try {
                execStatus = PropagationTaskExecStatus.valueOf(
                        execution.getStatus());
            } catch (IllegalArgumentException e) {
                LOG.error("Unexpected execution status found {}",
                        execution.getStatus());
                execStatus = PropagationTaskExecStatus.FAILURE;
            }
            if (task.getResource().isPropagationPrimary()
                    && !execStatus.isSuccessful()) {

                throw new PropagationException(task.getResource().getName(),
                        execution.getMessage());
            }
        }
    }

    /**
     * Check wether an execution has to be stored, for a given task.
     *
     * @param task execution's task
     * @param execution to be decide wether to store or not
     * @return true if execution has to be store, false otherwise
     */
    private boolean hasToBeregistered(final PropagationTask task,
            final TaskExec execution) {

        boolean result;

        final boolean failed = !PropagationTaskExecStatus.valueOf(
                execution.getStatus()).isSuccessful();

        switch (task.getResourceOperationType()) {

            case CREATE:
                result = (failed
                        && task.getResource().getCreateTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getCreateTraceLevel()
                        == TraceLevel.ALL;
                break;

            case UPDATE:
                result = (failed
                        && task.getResource().getUpdateTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getUpdateTraceLevel()
                        == TraceLevel.ALL;
                break;

            case DELETE:
                result = (failed
                        && task.getResource().getDeleteTraceLevel().
                        ordinal() >= TraceLevel.FAILURES.ordinal())
                        || task.getResource().getDeleteTraceLevel()
                        == TraceLevel.ALL;
                break;

            default:
                result = false;
        }

        return result;
    }

    /**
     * Execute a propagation task.
     *
     * @param task to execute
     * @return TaskExecution
     */
    public TaskExec execute(final PropagationTask task) {
        final Date startDate = new Date();

        TaskExec execution = new TaskExec();
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());

        String taskExecutionMessage = null;

        // Flag to state wether any propagation has been attempted
        Set<String> propagationAttempted = new HashSet<String>();

        try {
            final ConnInstance connInstance =
                    task.getResource().getConnector();

            final ConnectorFacadeProxy connector =
                    connLoader.getConnector(task.getResource());

            if (connector == null) {
                final String msg = String.format(
                        "Connector instance bean for resource %s and "
                        + "connInstance %s not found",
                        task.getResource(), connInstance);

                throw new NoSuchBeanDefinitionException(msg);
            }

            switch (task.getResourceOperationType()) {
                case CREATE:
                case UPDATE:
                    ConnectorObject remoteObject = null;
                    try {
                        remoteObject = connector.getObject(
                                task.getPropagationMode(),
                                task.getResourceOperationType(),
                                ObjectClass.ACCOUNT,
                                new Uid(task.getOldAccountId() == null
                                ? task.getAccountId()
                                : task.getOldAccountId()),
                                null);
                    } catch (RuntimeException ignore) {
                        LOG.debug("To be ignored, when resolving "
                                + "username on connector", ignore);
                    }

                    if (remoteObject != null) {
                        // 0. prepare new set of attributes
                        final Set<Attribute> attributes =
                                new HashSet<Attribute>(task.getAttributes());

                        // 1. check if rename is really required
                        final Name newName = (Name) AttributeUtil.find(
                                Name.NAME, attributes);

                        LOG.debug("Rename required with value {}", newName);

                        if (newName != null
                                && newName.equals(remoteObject.getName())) {

                            LOG.debug("Remote object name unchanged");
                            attributes.remove(newName);
                        }

                        LOG.debug("Attributes to be replaced {}", attributes);

                        // 2. update with a new "normalized" attribute set
                        connector.update(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                remoteObject.getUid(),
                                attributes,
                                null,
                                propagationAttempted);
                    } else {
                        connector.create(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                task.getAttributes(),
                                null,
                                propagationAttempted);
                    }
                    break;

                case DELETE:
                    connector.delete(task.getPropagationMode(),
                            ObjectClass.ACCOUNT,
                            new Uid(task.getAccountId()),
                            null,
                            propagationAttempted);
                    break;

                default:
            }

            execution.setStatus(
                    task.getPropagationMode() == PropagationMode.SYNC
                    ? PropagationTaskExecStatus.SUCCESS.name()
                    : PropagationTaskExecStatus.SUBMITTED.name());

            LOG.debug("Successfully propagated to resource {}",
                    task.getResource());
        } catch (Throwable t) {
            LOG.error("Exception during provision on resource "
                    + task.getResource().getName(), t);

            if (t instanceof ConnectorException && t.getCause() != null) {
                taskExecutionMessage = t.getCause().getMessage();
            } else {
                StringWriter exceptionWriter = new StringWriter();
                exceptionWriter.write(t.getMessage() + "\n\n");
                t.printStackTrace(new PrintWriter(exceptionWriter));
                taskExecutionMessage = exceptionWriter.toString();
            }

            try {
                execution.setStatus(
                        task.getPropagationMode() == PropagationMode.SYNC
                        ? PropagationTaskExecStatus.FAILURE.name()
                        : PropagationTaskExecStatus.UNSUBMITTED.name());
            } catch (Throwable wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.add(
                    task.getResourceOperationType().toString().toLowerCase());
        } finally {
            LOG.debug("Update execution for {}", task);

            if (hasToBeregistered(task, execution)) {
                PropagationTask savedTask = taskDAO.save(task);

                execution.setStartDate(startDate);
                execution.setMessage(taskExecutionMessage);
                execution.setEndDate(new Date());
                execution.setTask(savedTask);

                if (!propagationAttempted.isEmpty()) {
                    execution = taskExecDAO.save(execution);

                    LOG.debug("Execution finished: {}", execution);
                } else {
                    LOG.debug("No propagation attemped for {}", execution);
                }
            }
        }

        return execution;
    }
}
