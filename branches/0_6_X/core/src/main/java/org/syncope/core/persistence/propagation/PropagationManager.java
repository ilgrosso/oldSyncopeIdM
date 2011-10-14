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
package org.syncope.core.persistence.propagation;

import com.opensymphony.workflow.Workflow;
import com.opensymphony.workflow.WorkflowException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
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
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.AbstractAttrValue;
import org.syncope.core.persistence.beans.AbstractAttributable;
import org.syncope.core.persistence.beans.AbstractDerSchema;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.TaskExecution;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttr;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.beans.user.UDerAttr;
import org.syncope.core.persistence.beans.user.UDerSchema;
import org.syncope.core.persistence.beans.user.USchema;
import org.syncope.core.persistence.beans.user.UVirAttr;
import org.syncope.core.persistence.beans.user.UVirSchema;
import org.syncope.core.persistence.dao.DerSchemaDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.TaskDAO;
import org.syncope.core.persistence.dao.TaskExecutionDAO;
import org.syncope.core.persistence.dao.VirSchemaDAO;
import org.syncope.core.util.JexlUtil;
import org.syncope.core.workflow.Constants;
import org.syncope.core.workflow.WFUtils;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;
import org.syncope.types.SourceMappingType;
import org.syncope.types.SchemaType;
import org.syncope.types.TaskExecutionStatus;

/**
 * Manage the data propagation to target resources.
 */
public class PropagationManager {

    /**
     * Logger.
     */
    protected static final Logger LOG =
            LoggerFactory.getLogger(PropagationManager.class);

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    /**
     * Schema DAO.
     */
    @Autowired
    private SchemaDAO schemaDAO;

    /**
     * Derived Schema DAO.
     */
    @Autowired
    private DerSchemaDAO derSchemaDAO;

    /**
     * Virtual Schema DAO.
     */
    @Autowired
    private VirSchemaDAO virSchemaDAO;

    /**
     * Task DAO.
     */
    @Autowired
    private TaskDAO taskDAO;

    /**
     * Task execution DAO.
     */
    @Autowired
    private TaskExecutionDAO taskExecutionDAO;

    /**
     * Task execution workflow.
     */
    @Resource(name = "taskExecutionWorkflow")
    private Workflow workflow;

    /**
     * JEXL engine for evaluating connector's account link.
     */
    @Autowired
    private JexlUtil jexlUtil;

    /**
     * Create the user on every associated resource.
     * Exceptions will be ignored.
     * @param user to be created.
     * @param password to be set.
     * @throws PropagationException when anything goes wrong.
     */
    public void create(final SyncopeUser user, final String password)
            throws PropagationException {

        create(user, password, Collections.EMPTY_SET);
    }

    /**
     * Create the user on every associated resource.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be created.
     * @param password to be set.
     * @param mandatoryResourceNames to ask for mandatory or optional
     * provisioning.
     * @throws PropagationException
     */
    public void create(final SyncopeUser user,
            final String password, Set<String> mandatoryResourceNames)
            throws PropagationException {

        if (mandatoryResourceNames == null) {
            mandatoryResourceNames = Collections.EMPTY_SET;
        }

        Set<TargetResource> resources = new HashSet<TargetResource>();
        for (TargetResource resource : user.getTargetResources()) {
            resources.add(resource);
        }
        for (Membership membership : user.getMemberships()) {
            resources.addAll(membership.getSyncopeRole().getTargetResources());
        }

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.set(ResourceOperationType.CREATE, resources);

        provision(user, password, resourceOperations, mandatoryResourceNames);
    }

    /**
     * Performs update on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be updated.
     * @param password to be updated.
     * @param affectedResources resources affected by this update
     * @param mandatoryResourceNames to ask for mandatory or optional update.
     * @throws PropagationException if anything goes wrong
     */
    public void update(final SyncopeUser user,
            final String password,
            final ResourceOperations resourceOperations,
            Set<String> mandatoryResourceNames)
            throws PropagationException {

        if (mandatoryResourceNames == null) {
            mandatoryResourceNames = Collections.EMPTY_SET;
        }

        provision(user, password, resourceOperations, mandatoryResourceNames);
    }

    /**
     * Perform delete on each resource associated to the user.
     * It is possible to ask for a mandatory provisioning for some resources
     * specifying a set of resource names.
     * Exceptions won't be ignored and the process will be stopped if the
     * creation fails onto a mandatory resource.
     *
     * @param user to be deleted
     * @param mandatoryResourceNames to ask for mandatory or optyional delete
     * @throws PropagationException if anything goes wrong
     */
    public void delete(SyncopeUser user, Set<String> mandatoryResourceNames)
            throws PropagationException {

        if (mandatoryResourceNames == null) {
            mandatoryResourceNames = Collections.EMPTY_SET;
        }

        Set<TargetResource> resources = new HashSet<TargetResource>();
        for (TargetResource resource : user.getTargetResources()) {
            resources.add(resource);
        }
        for (Membership membership : user.getMemberships()) {
            resources.addAll(membership.getSyncopeRole().getTargetResources());
        }

        ResourceOperations resourceOperations = new ResourceOperations();
        resourceOperations.set(ResourceOperationType.DELETE, resources);

        provision(user, null, resourceOperations, mandatoryResourceNames);
    }

    /**
     * Implementation of the provisioning feature.
     * @param user
     * @param mandatoryResourceNames
     * @param merge
     * @throws PropagationException
     */
    protected void provision(
            final SyncopeUser user,
            final String password,
            final ResourceOperations resourceOperations,
            final Set<String> mandatoryResourceNames)
            throws PropagationException {

        LOG.debug("Provisioning with user {}:\n{}", user, resourceOperations);

        // Avoid duplicates - see javadoc
        resourceOperations.purge();
        LOG.debug("After purge: {}", resourceOperations);

        Task task;
        TaskExecution execution;
        Long workflowId;
        for (ResourceOperationType type : ResourceOperationType.values()) {
            for (TargetResource resource : resourceOperations.get(type)) {
                Map<String, Set<Attribute>> preparedAttributes =
                        prepareAttributes(user, password, resource);
                String accountId =
                        preparedAttributes.keySet().iterator().next();

                task = new Task();
                task.setResource(resource);
                task.setResourceOperationType(type);
                task.setPropagationMode(
                        mandatoryResourceNames.contains(resource.getName())
                        ? PropagationMode.SYNC
                        : resource.getOptionalPropagationMode());
                task.setAccountId(accountId);
                task.setOldAccountId(
                        resourceOperations.getOldAccountId(resource.getName()));
                task.setAttributes(
                        preparedAttributes.values().iterator().next());

                execution = new TaskExecution();
                execution.setTask(task);

                task.addExecution(execution);
                task = taskDAO.save(task);

                // Re-read execution to get the unique id
                execution = task.getExecutions().iterator().next();

                try {
                    workflowId = workflow.initialize(
                            Constants.TASKEXECUTION_WORKFLOW, 0, null);
                    execution.setWorkflowId(workflowId);
                } catch (WorkflowException e) {
                    LOG.error("While initializing workflow for {}",
                            execution, e);
                }

                LOG.debug("Execution started for {}", task);

                propagate(execution);

                LOG.debug("Execution finished for {}", task);

                // Propagation is interrupted as soon as the result of the
                // communication with a mandatory resource is in error
                if (mandatoryResourceNames.contains(resource.getName())
                        && WFUtils.getTaskExecutionStatus(workflow, execution)
                        != TaskExecutionStatus.SUCCESS) {

                    throw new PropagationException(resource.getName(),
                            execution.getMessage());
                }
            }
        }
    }

    private Class getSourceMappingTypeClass(
            SourceMappingType sourceMappingType) {

        Class result;

        switch (sourceMappingType) {
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

    private Map<String, Set<Attribute>> prepareAttributes(SyncopeUser user,
            String password, TargetResource resource) throws PropagationException {

        LOG.debug("Preparing resource attributes for {}"
                + " on resource {}"
                + " with attributes {}",
                new Object[]{user, resource, user.getAttributes()});

        // set of user attributes
        Set<Attribute> accountAttributes = new HashSet<Attribute>();

        // cast to be applied on SchemaValueType
        Class castToBeApplied;

        // account id
        Map<String, Attribute> accountId = new HashMap<String, Attribute>();

        // resource field values
        Set objValues;

        // syncope user attribute
        UAttr attr;
        UDerAttr derAttr;
        UVirAttr virAttr;

        AbstractSchema schema;
        AbstractDerSchema derSchema;
        AbstractVirSchema virSchema;

        // syncope user attribute schema type
        SchemaType schemaType = null;
        // syncope user attribute values
        List<AbstractAttrValue> values;

        for (SchemaMapping mapping : resource.getMappings()) {
            LOG.debug("Processing schema {}", mapping.getSourceAttrName());

            schema = null;
            derSchema = null;
            virSchema = null;
            values = null;

            try {
                switch (mapping.getSourceMappingType()) {
                    case UserSchema:

                        schema = schemaDAO.find(
                                mapping.getSourceAttrName(),
                                getSourceMappingTypeClass(
                                mapping.getSourceMappingType()));


                        schemaType = schema.getType();

                        attr = user.getAttribute(
                                mapping.getSourceAttrName());

                        values = attr != null
                                ? (schema.isUniqueConstraint()
                                ? Collections.singletonList(
                                attr.getUniqueValue())
                                : attr.getValues())
                                : Collections.EMPTY_LIST;

                        LOG.debug("Retrieved attribute {}", attr
                                + "\n* SourceAttrName {}"
                                + "\n* SourceMappingType {}"
                                + "\n* Attribute values {}",
                                new Object[]{
                                    mapping.getSourceAttrName(),
                                    mapping.getSourceMappingType(),
                                    values});
                        break;

                    case UserVirtualSchema:

                        virSchema = virSchemaDAO.find(
                                mapping.getSourceAttrName(),
                                getSourceMappingTypeClass(
                                mapping.getSourceMappingType()));


                        schemaType = SchemaType.String;

                        virAttr = user.getVirtualAttribute(
                                mapping.getSourceAttrName());

                        values = new ArrayList<AbstractAttrValue>();
                        AbstractAttrValue abstractValue;

                        if (virAttr != null && virAttr.getValues() != null) {
                            for (String value : virAttr.getValues()) {
                                abstractValue = new UAttrValue();
                                abstractValue.setStringValue(value);
                                values.add(abstractValue);
                            }
                        }

                        LOG.debug("Retrieved virtual attribute {}", virAttr
                                + "\n* SourceAttrName {}"
                                + "\n* SourceMappingType {}"
                                + "\n* Attribute values {}",
                                new Object[]{
                                    mapping.getSourceAttrName(),
                                    mapping.getSourceMappingType(),
                                    values});
                        break;

                    case UserDerivedSchema:

                        derSchema = derSchemaDAO.find(
                                mapping.getSourceAttrName(),
                                getSourceMappingTypeClass(
                                mapping.getSourceMappingType()));

                        schemaType = SchemaType.String;

                        derAttr = user.getDerivedAttribute(
                                mapping.getSourceAttrName());

                        if (derAttr != null) {
                            AbstractAttrValue value = new UAttrValue();
                            value.setStringValue(
                                    derAttr.getValue(user.getAttributes()));

                            values = Collections.singletonList(value);
                        } else {
                            values = Collections.EMPTY_LIST;
                        }


                        LOG.debug("Retrieved attribute {}", derAttr
                                + "\n* SourceAttrName {}"
                                + "\n* SourceMappingType {}"
                                + "\n* Attribute values {}",
                                new Object[]{
                                    mapping.getSourceAttrName(),
                                    mapping.getSourceMappingType(),
                                    values});

                        break;

                    case SyncopeUserId:
                    case Password:
                        schema = null;
                        schemaType = SchemaType.String;

                        AbstractAttrValue uAttrValue = new UAttrValue();

                        if (SourceMappingType.SyncopeUserId
                                == mapping.getSourceMappingType()) {

                            uAttrValue.setStringValue(user.getId().toString());
                        }
                        if (SourceMappingType.Password
                                == mapping.getSourceMappingType()
                                && password != null) {

                            uAttrValue.setStringValue(password);
                        }

                        values = Collections.singletonList(uAttrValue);
                        break;

                    default:
                        break;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Define mapping for: "
                            + "\n* DestAttrName " + mapping.getDestAttrName()
                            + "\n* is accountId " + mapping.isAccountid()
                            + "\n* is password " + (mapping.isPassword()
                            || mapping.getSourceMappingType().equals(
                            SourceMappingType.Password))
                            + "\n* mandatory condition "
                            + mapping.getMandatoryCondition()
                            + "\n* Schema " + mapping.getSourceAttrName()
                            + "\n* SourceMappingType "
                            + mapping.getSourceMappingType().toString()
                            + "\n* ClassType " + schemaType.getClassName()
                            + "\n* Values " + values);
                }

                // -----------------------------
                // Retrieve user attribute values
                // -----------------------------
                objValues = new HashSet();

                for (AbstractAttrValue value : values) {
                    castToBeApplied =
                            Class.forName(schemaType.getClassName());

                    if (!FrameworkUtil.isSupportedAttributeType(
                            castToBeApplied)) {

                        castToBeApplied = String.class;
                        objValues.add(value.getValueAsString());
                    } else {
                        objValues.add(value.getValue());
                    }
                }
                // -----------------------------

                if (mapping.isAccountid()) {
                    if (schema != null && schema.isMultivalue()) {
                        accountId.put(objValues.iterator().next().toString(),
                                AttributeBuilder.build(
                                mapping.getDestAttrName(),
                                objValues));
                    } else {
                        accountId.put(objValues.iterator().next().toString(),
                                objValues.isEmpty()
                                ? AttributeBuilder.build(
                                mapping.getDestAttrName())
                                : AttributeBuilder.build(
                                mapping.getDestAttrName(),
                                objValues.iterator().next()));
                    }
                }

                if (mapping.isPassword()) {
                    accountAttributes.add(AttributeBuilder.buildPassword(
                            objValues.iterator().next().toString().
                            toCharArray()));
                }

                if (!mapping.isPassword() && !mapping.isAccountid()) {
                    if (schema != null && schema.isMultivalue()) {
                        accountAttributes.add(AttributeBuilder.build(
                                mapping.getDestAttrName(),
                                objValues));
                    } else {
                        accountAttributes.add(objValues.isEmpty()
                                ? AttributeBuilder.build(
                                mapping.getDestAttrName())
                                : AttributeBuilder.build(
                                mapping.getDestAttrName(),
                                objValues.iterator().next()));
                    }
                }
            } catch (Throwable t) {
                LOG.debug("Attribute '{}' processing failed",
                        mapping.getSourceAttrName(), t);
            }
        }

        if (accountId.isEmpty()) {
            throw new PropagationException(
                    resource.getName(),
                    "Missing accountId specification");
        }

        final String key = accountId.keySet().iterator().next();

        // Evaluate AccountLink expression
        String evaluatedAccountLink =
                evaluateAccountLink(user, resource.getAccountLink());

        // AccountId must be propagated. It could be a simple attribute for
        // the target resource or the key (depending on the accountLink)
        if (evaluatedAccountLink.isEmpty()) {
            // add accountId as __NAME__ attribute ...
            LOG.debug("Add AccountId [{}] as __NAME__", key);
            accountAttributes.add(new Name(key));
        } else {
            LOG.debug("Add AccountLink [{}] as __NAME__", evaluatedAccountLink);
            accountAttributes.add(new Name(evaluatedAccountLink));

            // AccountId not propagated: 
            // it will be used to set the value for __UID__ attribute
            LOG.debug("AccountId will be used just as __UID__ attribute");
        }

        return Collections.singletonMap(key, accountAttributes);

    }

    public void propagate(final TaskExecution execution) {
        final Date startDate = new Date();
        String taskExecutionMessage = null;

        final Task task = execution.getTask();

        // Output parameter to verify the propagation request tryed
        final Set<String> triedPropagationRequests = new HashSet<String>();

        try {
            ConnInstance connectorInstance =
                    task.getResource().getConnector();

            ConnectorFacadeProxy connector =
                    connInstanceLoader.getConnector(
                    connectorInstance.getId().toString());

            if (connector == null) {
                LOG.error("Connector instance bean "
                        + connectorInstance.getId().toString()
                        + " not found");

                throw new NoSuchBeanDefinitionException(
                        "Connector instance bean not found");
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
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("To be ignored, when resolving "
                                    + "username on connector", ignore);
                        }
                    }

                    if (remoteObject != null) {
                        // 0. prepare new set of attributes
                        final Set<Attribute> attributes =
                                new HashSet<Attribute>(task.getAttributes());

                        // 1. check if rename is really required
                        final Name newName = (Name) AttributeUtil.find(
                                Name.NAME, attributes);

                        LOG.debug("Rename required with value {}", newName);

                        if (newName != null) {
                            if (newName.equals(remoteObject.getName())) {
                                LOG.debug("Remote object name unchanged");
                                attributes.remove(newName);
                            }
                        }

                        LOG.debug("Attributes to be replaced {}", attributes);

                        // 2. update with a new "normalized" attribute set
                        connector.update(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                remoteObject.getUid(),
                                attributes,
                                null,
                                triedPropagationRequests);
                    } else {
                        connector.create(
                                task.getPropagationMode(),
                                ObjectClass.ACCOUNT,
                                task.getAttributes(),
                                null,
                                triedPropagationRequests);
                    }
                    break;

                case DELETE:
                    connector.delete(task.getPropagationMode(),
                            ObjectClass.ACCOUNT,
                            new Uid(task.getAccountId()),
                            null,
                            triedPropagationRequests);
                    break;

                default:
            }

            WFUtils.doExecuteAction(workflow,
                    Constants.TASKEXECUTION_WORKFLOW,
                    Constants.ACTION_OK,
                    execution.getWorkflowId(),
                    task.getPropagationMode() == PropagationMode.SYNC
                    ? Collections.singletonMap(
                    PropagationMode.SYNC.toString(), null)
                    : null);

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
                WFUtils.doExecuteAction(workflow,
                        Constants.TASKEXECUTION_WORKFLOW,
                        Constants.ACTION_KO,
                        execution.getWorkflowId(),
                        task.getPropagationMode() == PropagationMode.SYNC
                        ? Collections.singletonMap(
                        PropagationMode.SYNC.toString(), null)
                        : null);
            } catch (Throwable wft) {
                LOG.error("While executing KO action on {}", execution, wft);
            }

            triedPropagationRequests.add(
                    task.getResourceOperationType().toString().toLowerCase());
        } finally {
            LOG.debug("Update execution for {}", task);

            if (!triedPropagationRequests.isEmpty()) {
                execution.setStartDate(startDate);
                execution.setMessage(taskExecutionMessage);
                execution.setEndDate(new Date());

                taskExecutionDAO.save(execution);

                LOG.debug("Execution finished: {}", execution);
            } else {
                taskExecutionDAO.delete(execution);

                LOG.debug("Execution removed: {}", execution);
            }
        }
    }

    public <T extends AbstractAttributable> Set<String> getObjectAttributeValue(
            final T attributable,
            final String attributeName,
            final SourceMappingType sourceMappingType) {

        List values = new ArrayList();

        Set<String> attributeNames;
        ConnInstance connectorInstance;
        ConnectorFacadeProxy connector;
        Set<Attribute> attributes;
        String accountLink;
        String accountId = null;

        LOG.debug("{}: retrieving external values for {}",
                new Object[]{attributable, attributeName});

        for (TargetResource resource :
                attributable.getInheritedTargetResources()) {

            LOG.debug("Retrieving attribute mapped on {}", resource);

            attributeNames = new HashSet<String>();

            accountLink = resource.getAccountLink();

            for (SchemaMapping mapping : resource.getMappings()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Processing mapping."
                            + "\n\tID: " + mapping.getId()
                            + "\n\tSource: " + mapping.getSourceAttrName()
                            + "\n\tDestination: " + mapping.getDestAttrName()
                            + "\n\tType: " + mapping.getSourceMappingType()
                            + "\n\tMandatory condition: "
                            + mapping.getMandatoryCondition()
                            + "\n\tAccountId: " + mapping.isAccountid()
                            + "\n\tPassword: " + mapping.isPassword());
                }

                if (mapping.getSourceAttrName().equals(attributeName)
                        && mapping.getSourceMappingType() == sourceMappingType) {

                    attributeNames.add(mapping.getDestAttrName());
                }

                if (mapping.isAccountid()) {
                    try {
                        accountId = attributable.getAttribute(
                                mapping.getSourceAttrName()).
                                getValuesAsStrings().get(0);
                    } catch (NullPointerException e) {
                        // ignore exception
                        LOG.debug("Invalid accountId specified", e);
                    }
                }
            }

            if (accountId == null && accountLink != null) {
                accountId = evaluateAccountLink(attributable, accountLink);
            }

            if (attributeNames != null && accountId != null) {
                LOG.debug("Get object attribute for entry {}", accountId);

                connectorInstance = resource.getConnector();

                connector = connInstanceLoader.getConnector(
                        connectorInstance.getId().toString());

                try {
                    attributes = connector.getObjectAttributes(
                            ObjectClass.ACCOUNT,
                            new Uid(accountId),
                            null,
                            attributeNames);

                    LOG.debug("Retrieved {}", attributes);

                    for (Attribute attribute : attributes) {
                        values.addAll(attribute.getValue());
                    }
                } catch (Exception e) {
                    LOG.warn("Error connecting to {}", resource.getName(), e);
                    // ignore exception and go ahead
                }
            }
        }

        return new HashSet<String>(values);
    }

    private String evaluateAccountLink(
            final AbstractAttributable attributable, final String accountLink) {

        final JexlContext jexlContext = new MapContext();

        jexlUtil.addAttributesToContext(
                attributable.getAttributes(),
                jexlContext);

        jexlUtil.addDerAttributesToContext(
                attributable.getDerivedAttributes(),
                attributable.getAttributes(),
                jexlContext);

        // Evaluate expression using the context prepared before
        return jexlUtil.evaluateWithAttributes(accountLink, jexlContext);
    }
}