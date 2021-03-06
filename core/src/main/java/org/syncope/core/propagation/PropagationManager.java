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
package org.syncope.core.propagation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerAttr;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirAttr;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.TaskExec;
import org.syncope.core.persistence.beans.membership.MDerSchema;
import org.syncope.core.persistence.beans.membership.MSchema;
import org.syncope.core.persistence.beans.membership.MVirSchema;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.RDerSchema;
import org.syncope.core.persistence.beans.role.RSchema;
import org.syncope.core.persistence.beans.role.RVirSchema;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecDAO;
import org.syncope.core.persistence.dao.UserDAO;
import org.syncope.core.rest.data.UserDataBinder;
import org.syncope.core.util.AttributableUtil;
import org.syncope.core.util.JexlUtil;
import org.syncope.core.workflow.WorkflowResult;
import org.syncope.types.IntMappingType;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;
import org.syncope.types.PropagationTaskExecStatus;
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
     * @param wfResult user to be propagated (and info associated), as per
     * result from workflow
     * @param password to be set
     * @param vAttrs virtual attributes to be set
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getCreateTaskIds(
            final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password, final List<AttributeTO> vAttrs)
            throws NotFoundException {

        return getCreateTaskIds(wfResult, password, vAttrs, null);
    }

    /**
     * Create the user on every associated resource.
     *
     * @param wfResult user to be propagated (and info associated), as per
     * result from workflow.
     * @param password to be set.
     * @param vAttrs virtual attributes to be set.
     * @param syncResourceNames external resources performing sync, hence not to
     * be considered for propagation.
     * @return list of propagation tasks.
     * @throws NotFoundException if userId is not found.
     */
    public List<PropagationTask> getCreateTaskIds(
            final WorkflowResult<Map.Entry<Long, Boolean>> wfResult,
            final String password,
            final List<AttributeTO> vAttrs,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(wfResult.getResult().getKey());
        if (vAttrs != null && !vAttrs.isEmpty()) {
            userDataBinder.fillVirtual(user, vAttrs, AttributableUtil.USER);
        }

        final PropagationByResource propByRes = wfResult.getPropByRes();
        if (propByRes == null || propByRes.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        if (syncResourceNames != null) {
            propByRes.get(PropagationOperation.CREATE).
                    removeAll(syncResourceNames);
        }

        return provision(
                user, password, wfResult.getResult().getValue(), propByRes);
    }

    /**
     * Performs update on each resource associated to the user excluding the
     * specified into 'resourceNames' parameter.
     *
     * @param user to be propagated.
     * @param enable wether user must be enabled or not.
     * @param syncResourceNames external resource names not to be considered for
     * propagation. Use this during sync and disable/enable actions limited to
     * the external resources only.
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(
            final SyncopeUser user,
            final Boolean enable,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        return getUpdateTaskIds(
                user, // SyncopeUser to be updated on external resources
                null, // no propagation by resources
                null, // no password
                null, // no virtual attributes to be managed
                null, // no virtual attributes to be managed
                enable, // status to be propagated
                syncResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per
     * result from workflow.
     * @param enable wether user must be enabled or not.
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(
            final WorkflowResult<Long> wfResult,
            final Boolean enable)
            throws NotFoundException {

        return getUpdateTaskIds(
                wfResult, null, null, null, enable, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per
     * result from workflow
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable wether user must be enabled or not
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(
            final WorkflowResult<Long> wfResult,
            final String password,
            final Set<String> vAttrsToBeRemoved,
            final Set<AttributeMod> vAttrsToBeUpdated,
            final Boolean enable)
            throws NotFoundException {

        return getUpdateTaskIds(wfResult, password, vAttrsToBeRemoved,
                vAttrsToBeUpdated, enable, null);
    }

    /**
     * Performs update on each resource associated to the user.
     *
     * @param wfResult user to be propagated (and info associated), as per
     * result from workflow
     * @param password to be updated
     * @param vAttrsToBeRemoved virtual attributes to be removed
     * @param vAttrsToBeUpdated virtual attributes to be added
     * @param enable wether user must be enabled or not
     * @param syncResourceNames external resource names not to be considered for
     * propagation. Use this during sync and disable/enable actions limited to
     * the external resources only.
     * @return list of propagation tasks
     * @throws NotFoundException if userId is not found
     */
    public List<PropagationTask> getUpdateTaskIds(
            final WorkflowResult<Long> wfResult,
            final String password,
            final Set<String> vAttrsToBeRemoved,
            final Set<AttributeMod> vAttrsToBeUpdated,
            final Boolean enable,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        SyncopeUser user = getSyncopeUser(wfResult.getResult());

        return getUpdateTaskIds(
                user,
                wfResult.getPropByRes(),
                password,
                vAttrsToBeRemoved,
                vAttrsToBeUpdated,
                enable,
                syncResourceNames);
    }

    private List<PropagationTask> getUpdateTaskIds(
            final SyncopeUser user,
            final PropagationByResource propByRes,
            final String password,
            final Set<String> vAttrsToBeRemoved,
            final Set<AttributeMod> vAttrsToBeUpdated,
            final Boolean enable,
            final Set<String> syncResourceNames)
            throws NotFoundException {

        PropagationByResource localPropByRes = userDataBinder.fillVirtual(
                user,
                vAttrsToBeRemoved == null
                ? Collections.EMPTY_SET : vAttrsToBeRemoved,
                vAttrsToBeUpdated == null
                ? Collections.EMPTY_SET : vAttrsToBeUpdated,
                AttributableUtil.USER);

        if (propByRes != null && !propByRes.isEmpty()) {
            localPropByRes.merge(propByRes);
        } else {
            localPropByRes.addAll(
                    PropagationOperation.UPDATE, user.getResourceNames());
        }

        if (syncResourceNames != null) {
            localPropByRes.get(
                    PropagationOperation.CREATE).removeAll(syncResourceNames);
            localPropByRes.get(
                    PropagationOperation.UPDATE).removeAll(syncResourceNames);
            localPropByRes.get(
                    PropagationOperation.DELETE).removeAll(syncResourceNames);
        }

        return provision(user, password, enable, localPropByRes);
    }

    /**
     * Perform delete on each resource associated to the user. It is possible to
     * ask for a mandatory provisioning for some resources specifying a set of
     * resource names. Exceptions won't be ignored and the process will be
     * stopped if the creation fails onto a mandatory resource.
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
     * Perform delete on each resource associated to the user. It is possible to
     * ask for a mandatory provisioning for some resources specifying a set of
     * resource names. Exceptions won't be ignored and the process will be
     * stopped if the creation fails onto a mandatory resource.
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
        propByRes.set(PropagationOperation.DELETE, user.getResourceNames());
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
            case RoleSchema:
                result = RSchema.class;
                break;
            case MembershipSchema:
                result = MSchema.class;
                break;

            case UserDerivedSchema:
                result = UDerSchema.class;
                break;
            case RoleDerivedSchema:
                result = RDerSchema.class;
                break;
            case MembershipDerivedSchema:
                result = MDerSchema.class;
                break;

            case UserVirtualSchema:
                result = UVirSchema.class;
                break;
            case RoleVirtualSchema:
                result = RVirSchema.class;
                break;
            case MembershipVirtualSchema:
                result = MVirSchema.class;
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
            final SyncopeUser user,
            final String password)
            throws ClassNotFoundException {

        final List<AbstractAttributable> attributables =
                new ArrayList<AbstractAttributable>();

        switch (mapping.getIntMappingType().getEntity()) {
            case USER:
                attributables.addAll(Collections.singleton(user));
                break;
            case ROLE:
                final List<Membership> memberships = user.getMemberships();
                for (Membership membership : memberships) {
                    attributables.add(membership.getSyncopeRole());
                }
                break;
            case MEMBERSHIP:
                attributables.addAll(user.getMemberships());
                break;
            default:
        }

        final Entry<AbstractSchema, List<AbstractAttrValue>> entry =
                getAttributeValues(mapping, attributables, password);

        final List<AbstractAttrValue> values = entry.getValue();
        final AbstractSchema schema = entry.getKey();
        final SchemaType schemaType =
                schema == null ? SchemaType.String : schema.getType();

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

        List<Object> objValues = new ArrayList<Object>();
        for (AbstractAttrValue value : values) {
            if (FrameworkUtil.isSupportedAttributeType(
                    Class.forName(schemaType.getClassName()))) {
                objValues.add(value.getValue());
            } else {
                objValues.add(value.getValueAsString());
            }
        }

        Map.Entry<String, Attribute> res;

        if (mapping.isAccountid()) {

            res = new DefaultMapEntry(
                    objValues.iterator().next().toString(), null);

        } else if (mapping.isPassword()) {

            res = new DefaultMapEntry(null,
                    AttributeBuilder.buildPassword(
                    objValues.iterator().next().toString().toCharArray()));

        } else {
            if (schema != null && schema.isMultivalue()) {
                res = new DefaultMapEntry(null,
                        AttributeBuilder.build(mapping.getExtAttrName(),
                        objValues));

            } else {
                res = new DefaultMapEntry(null,
                        objValues.isEmpty()
                        ? AttributeBuilder.build(mapping.getExtAttrName())
                        : AttributeBuilder.build(mapping.getExtAttrName(),
                        objValues.iterator().next()));
            }
        }

        return res;
    }

    /**
     * Get attribute values.
     *
     * @param mapping mapping.
     * @param attributables list of attributables.
     * @param password password.
     * @return schema and attribute values.
     */
    private Entry<AbstractSchema, List<AbstractAttrValue>> getAttributeValues(
            final SchemaMapping mapping,
            final List<AbstractAttributable> attributables,
            final String password) {


        LOG.debug("Get attributes for '{}' and mapping type '{}'",
                attributables, mapping.getIntMappingType());

        AbstractSchema schema = null;

        List<AbstractAttrValue> values = new ArrayList<AbstractAttrValue>();
        AbstractAttrValue attrValue;

        switch (mapping.getIntMappingType()) {
            case UserSchema:
            case RoleSchema:
            case MembershipSchema:
                schema = schemaDAO.find(mapping.getIntAttrName(),
                        getIntMappingTypeClass(mapping.getIntMappingType()));

                for (AbstractAttributable attributable : attributables) {
                    final UAttr attr =
                            attributable.getAttribute(mapping.getIntAttrName());

                    if (attr != null && attr.getValues() != null) {
                        values.addAll(schema.isUniqueConstraint()
                                ? Collections.singletonList(
                                attr.getUniqueValue())
                                : attr.getValues());
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{attr, mapping.getIntAttrName(),
                                mapping.getIntMappingType(), values});
                }

                break;

            case UserVirtualSchema:
            case RoleVirtualSchema:
            case MembershipVirtualSchema:

                for (AbstractAttributable attributable : attributables) {
                    AbstractVirAttr virAttr = attributable.getVirtualAttribute(
                            mapping.getIntAttrName());

                    if (virAttr != null && virAttr.getValues() != null) {
                        for (String value : virAttr.getValues()) {
                            attrValue = new UAttrValue();
                            attrValue.setStringValue(value);
                            values.add(attrValue);
                        }
                    }

                    LOG.debug("Retrieved virtual attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{virAttr, mapping.getIntAttrName(),
                                mapping.getIntMappingType(), values});
                }
                break;

            case UserDerivedSchema:
            case RoleDerivedSchema:
            case MembershipDerivedSchema:
                for (AbstractAttributable attributable : attributables) {
                    AbstractDerAttr derAttr = attributable.getDerivedAttribute(
                            mapping.getIntAttrName());

                    if (derAttr != null) {
                        attrValue = new UAttrValue();
                        attrValue.setStringValue(
                                derAttr.getValue(attributable.getAttributes()));
                        values.add(attrValue);
                    }

                    LOG.debug("Retrieved attribute {}"
                            + "\n* IntAttrName {}"
                            + "\n* IntMappingType {}"
                            + "\n* Attribute values {}",
                            new Object[]{derAttr, mapping.getIntAttrName(),
                                mapping.getIntMappingType(), values});
                }
                break;

            case Username:
                for (AbstractAttributable attributable : attributables) {
                    attrValue = new UAttrValue();
                    attrValue.setStringValue(
                            ((SyncopeUser) attributable).getUsername());

                    values.add(attrValue);
                }
                break;

            case SyncopeUserId:
                for (AbstractAttributable attributable : attributables) {
                    attrValue = new UAttrValue();
                    attrValue.setStringValue(attributable.getId().toString());
                    values.add(attrValue);
                }
                break;

            case Password:
                attrValue = new UAttrValue();

                if (password != null) {
                    attrValue.setStringValue(password);
                }

                values.add(attrValue);
                break;

            default:
        }

        LOG.debug("Retrived values '{}'", values);

        return new DefaultMapEntry(schema, values);
    }

    /**
     * Prepare attributes for sending to a connector instance.
     *
     * @param user given user
     * @param password clear-text password
     * @param enable wether user must be enabled or not
     * @param resource target resource
     * @return account link + prepared attributes
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
                    final Attribute alreadyAdded = AttributeUtil.find(
                            preparedAttribute.getValue().getName(), attributes);

                    if (alreadyAdded == null) {
                        attributes.add(preparedAttribute.getValue());
                    } else {
                        attributes.remove(alreadyAdded);

                        Set values = new HashSet(alreadyAdded.getValue());
                        values.addAll(preparedAttribute.getValue().getValue());

                        attributes.add(AttributeBuilder.build(
                                preparedAttribute.getValue().getName(),
                                values));
                    }

                }
            } catch (Throwable t) {
                LOG.debug("Attribute '{}' processing failed",
                        mapping.getIntAttrName(), t);
            }
        }

        if (!StringUtils.hasText(accountId)) {
            // LOG error but avoid to throw exception: leave it to the 
            //external resource
            LOG.error("Missing accountId for '{}': ", resource.getName());
        }

        // Evaluate AccountLink expression
        String evalAccountLink =
                jexlUtil.evaluate(resource.getAccountLink(), user);

        // AccountId must be propagated. It could be a simple attribute for
        // the target resource or the key (depending on the accountLink)
        if (evalAccountLink.isEmpty()) {
            // add accountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", accountId);
            attributes.add(new Name(accountId));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evalAccountLink);
            attributes.add(new Name(evalAccountLink));

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
     */
    protected List<PropagationTask> provision(
            final SyncopeUser user,
            final String password,
            final Boolean enable,
            final PropagationByResource propByRes) {

        LOG.debug("Provisioning with user {}:\n{}", user, propByRes);

        // Avoid duplicates - see javadoc
        propByRes.purge();
        LOG.debug("After purge: {}", propByRes);

        List<PropagationTask> tasks = new ArrayList<PropagationTask>();

        for (PropagationOperation operation : PropagationOperation.values()) {
            List<ExternalResource> resourcesByPriority =
                    new ArrayList<ExternalResource>();
            for (ExternalResource resource : resourceDAO.findAllByPriority()) {
                if (propByRes.get(operation).contains(resource.getName())) {
                    resourcesByPriority.add(resource);
                }
            }

            for (ExternalResource resource : resourcesByPriority) {

                PropagationTask task = new PropagationTask();
                task.setResource(resource);
                task.setSyncopeUser(user);
                task.setPropagationOperation(operation);
                task.setPropagationMode(resource.getPropagationMode());
                task.setOldAccountId(
                        propByRes.getOldAccountId(resource.getName()));

                Map.Entry<String, Set<Attribute>> preparedAttrs =
                        prepareAttributes(user, password, enable, resource);

                task.setAccountId(preparedAttrs.getKey());
                task.setAttributes(preparedAttrs.getValue());

                tasks.add(task);

                LOG.debug("Execution started for {}", task);
            }
        }

        return tasks;
    }

    public void execute(final List<PropagationTask> tasks)
            throws PropagationException {
        execute(tasks, null);
    }

    /**
     * Execute a list of PropagationTask, in given order.
     *
     * @param tasks to be execute, in given order
     * @throws PropagationException if propagation goes wrong: propagation is
     * interrupted as soon as the result of the communication with a primary
     * resource is in error
     */
    public void execute(
            final List<PropagationTask> tasks,
            final PropagationHandler handler)
            throws PropagationException {

        for (PropagationTask task : tasks) {
            LOG.debug("Execution started for {}", task);

            TaskExec execution = execute(task, handler);

            LOG.debug("Execution finished for {}, {}", task, execution);

            // Propagation is interrupted as soon as the result of the
            // communication with a primary resource is in error
            PropagationTaskExecStatus execStatus;
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

        switch (task.getPropagationOperation()) {

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
        return execute(task, null);
    }

    /**
     * Execute a propagation task.
     *
     * @param task to execute.
     * @param handler propagation handler.
     * @return TaskExecution.
     */
    public TaskExec execute(
            final PropagationTask task, final PropagationHandler handler) {
        final Date startDate = new Date();

        TaskExec execution = new TaskExec();
        execution.setStatus(PropagationTaskExecStatus.CREATED.name());

        String taskExecutionMessage = null;

        // Flag to state wether any propagation has been attempted
        Set<String> propagationAttempted = new HashSet<String>();

        ConnectorObject before = null;
        ConnectorObject after = null;

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

            // Try to read user BEFORE any actual operation
            before = getRemoteObject(connector, task, false);

            try {
                switch (task.getPropagationOperation()) {
                    case CREATE:
                    case UPDATE:
                        // set of attributes to be propagated
                        final Set<Attribute> attributes =
                                new HashSet<Attribute>(task.getAttributes());

                        if (before != null) {

                            // 1. check if rename is really required
                            final Name newName = (Name) AttributeUtil.find(
                                    Name.NAME, attributes);

                            LOG.debug("Rename required with value {}", newName);

                            if (newName != null
                                    && newName.equals(before.getName())
                                    && !before.getUid().getUidValue().equals(
                                    newName.getNameValue())) {

                                LOG.debug("Remote object name unchanged");
                                attributes.remove(newName);
                            }

                            LOG.debug("Attributes to be replaced {}", attributes);

                            // 2. update with a new "normalized" attribute set
                            connector.update(
                                    task.getPropagationMode(),
                                    ObjectClass.ACCOUNT,
                                    before.getUid(),
                                    attributes,
                                    null,
                                    propagationAttempted);
                        } else {
                            // 1. get accountId
                            final String accountId = task.getAccountId();

                            // 2. get name
                            final Name name = (Name) AttributeUtil.find(
                                    Name.NAME, attributes);

                            // 3. check if:
                            //      * accountId is not blank;
                            //      * accountId is not equal to Name.
                            if (StringUtils.hasText(accountId)
                                    && (name == null
                                    || !accountId.equals(name.getNameValue()))) {

                                // 3.a retrieve uid
                                final Uid uid = (Uid) AttributeUtil.find(
                                        Uid.NAME, attributes);

                                // 3.b add Uid if not provided
                                if (uid == null) {
                                    attributes.add(AttributeBuilder.build(
                                            Uid.NAME,
                                            Collections.singleton(accountId)));
                                }
                            }

                            // 4. provision entry
                            connector.create(
                                    task.getPropagationMode(),
                                    ObjectClass.ACCOUNT,
                                    attributes,
                                    null,
                                    propagationAttempted);
                        }
                        break;

                    case DELETE:
                        if (before == null) {
                            LOG.debug("{} not found on external resource:"
                                    + " ignoring delete", task.getAccountId());
                        } else {
                            connector.delete(task.getPropagationMode(),
                                    ObjectClass.ACCOUNT,
                                    before.getUid(),
                                    null,
                                    propagationAttempted);
                        }
                        break;

                    default:
                }

                execution.setStatus(
                        task.getPropagationMode() == PropagationMode.ONE_PHASE
                        ? PropagationTaskExecStatus.SUCCESS.name()
                        : PropagationTaskExecStatus.SUBMITTED.name());

                LOG.debug("Successfully propagated to {}", task.getResource());

                // Try to read user AFTER any actual operation
                after = getRemoteObject(connector, task, true);

            } catch (Exception e) {
                after = getRemoteObject(connector, task, false);
                throw e;
            }

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
                        task.getPropagationMode() == PropagationMode.ONE_PHASE
                        ? PropagationTaskExecStatus.FAILURE.name()
                        : PropagationTaskExecStatus.UNSUBMITTED.name());
            } catch (Throwable wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            propagationAttempted.add(
                    task.getPropagationOperation().name().toLowerCase());
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

        if (handler != null) {
            handler.handle(
                    task.getResource().getName(),
                    PropagationTaskExecStatus.valueOf(execution.getStatus()),
                    before,
                    after);
        }

        return execution;
    }

    /**
     * Get remote object.
     *
     * @param connector connector facade proxy.
     * @param task current propagation task.
     * @param latest 'FALSE' to retrieve object using old accountId if not null.
     * @return remote connector object.
     */
    private ConnectorObject getRemoteObject(
            final ConnectorFacadeProxy connector,
            final PropagationTask task,
            final boolean latest) {
        try {
            return connector.getObject(
                    task.getPropagationMode(),
                    task.getPropagationOperation(),
                    ObjectClass.ACCOUNT,
                    new Uid(latest || task.getOldAccountId() == null
                    ? task.getAccountId()
                    : task.getOldAccountId()),
                    null);
        } catch (RuntimeException ignore) {
            LOG.debug("Resolving username", ignore);
            return null;
        }
    }
}
