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
package org.syncope.core.rest.data;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UserAttribute;
import org.syncope.core.persistence.beans.user.UserAttributeValue;
import org.syncope.core.persistence.beans.user.UserDerivedAttribute;
import org.syncope.core.persistence.beans.user.UserDerivedSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.validation.ValidationException;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class UserDataBinder {

    private static final Logger log = LoggerFactory.getLogger(
            UserDataBinder.class);
    private SchemaDAO schemaDAO;
    private AttributeValueDAO attributeValueDAO;
    private DerivedSchemaDAO derivedSchemaDAO;
    private SyncopeRoleDAO syncopeRoleDAO;
    private ResourceDAO resourceDAO;

    @Autowired
    public UserDataBinder(SchemaDAO schemaDAO,
            AttributeValueDAO attributeValueDAO,
            DerivedSchemaDAO derivedSchemaDAO,
            SyncopeRoleDAO syncopeRoleDAO,
            ResourceDAO resourceDAO) {

        this.schemaDAO = schemaDAO;
        this.attributeValueDAO = attributeValueDAO;
        this.derivedSchemaDAO = derivedSchemaDAO;
        this.syncopeRoleDAO = syncopeRoleDAO;
        this.resourceDAO = resourceDAO;
    }

    public SyncopeUser createSyncopeUser(UserTO userTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);
        SyncopeClientException invalidPassword = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidPassword);
        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);
        SyncopeClientException invalidValues = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidValues);
        SyncopeClientException invalidUniques = new SyncopeClientException(
                SyncopeClientExceptionType.InvalidUniques);
        SyncopeClientException invalidDerivedSchemas =
                new SyncopeClientException(
                SyncopeClientExceptionType.InvalidDerivedSchemas);

        SyncopeUser syncopeUser = new SyncopeUser();

        // 0. password
        // TODO: check password policies
        if (userTO.getPassword() == null
                || userTO.getPassword().length() == 0) {

            log.error("No password provided");

            invalidPassword.addElement("Null password");
            compositeErrorException.addException(invalidPassword);
        } else {
            syncopeUser.setPassword(userTO.getPassword());
        }

        // 1. attributes
        UserSchema schema = null;
        UserAttribute attribute = null;
        Set<String> valuesProvided = null;
        UserAttributeValue attributeValue = null;
        for (AttributeTO attributeTO : userTO.getAttributes()) {
            schema = schemaDAO.find(attributeTO.getSchema(), UserSchema.class);

            // safely ignore invalid schemas from AttributeTO
            // see http://code.google.com/p/syncope/issues/detail?id=17
            if (schema == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid schema "
                            + attributeTO.getSchema());
                }
            } else {
                if (schema.isVirtual()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring virtual schema" + schema.getName());
                    }
                } else {
                    attribute = new UserAttribute();
                    attribute.setSchema(schema);
                    attribute.setOwner(syncopeUser);

                    // if the schema is multivale, all values are considered for
                    // addition, otherwise only the fist one - if provided - is
                    // considered
                    valuesProvided = schema.isMultivalue()
                            ? attributeTO.getValues()
                            : (attributeTO.getValues().isEmpty()
                            ? Collections.EMPTY_SET
                            : Collections.singleton(
                            attributeTO.getValues().iterator().next()));
                    for (String value : valuesProvided) {
                        attributeValue = new UserAttributeValue();

                        try {
                            attributeValue = attribute.addValue(value,
                                    attributeValue);
                        } catch (ValidationException e) {
                            log.error("Invalid value for attribute "
                                    + schema.getName() + ": " + value, e);

                            invalidValues.addElement(schema.getName());
                        }

                        // if the schema is uniquevalue, check the uniqueness
                        if (schema.isUniquevalue()
                                && attributeValueDAO.existingAttributeValue(
                                attributeValue)) {

                            log.error("Unique value schema " + schema.getName()
                                    + " with no unique value: "
                                    + attributeValue.getValueAsString());

                            invalidUniques.addElement(schema.getName());
                            attribute.setAttributeValues(Collections.EMPTY_SET);
                        }
                    }

                    if (!attribute.getAttributeValues().isEmpty()) {
                        syncopeUser.addAttribute(attribute);
                    }
                }
            }
        }

        // 2. derived attributes
        UserDerivedSchema derivedSchema = null;
        UserDerivedAttribute derivedAttribute = null;
        for (AttributeTO attributeTO : userTO.getDerivedAttributes()) {
            derivedSchema = derivedSchemaDAO.find(attributeTO.getSchema(),
                    UserDerivedSchema.class);

            if (derivedSchema == null) {
                invalidDerivedSchemas.addElement(attributeTO.getSchema());
            } else {
                derivedAttribute = new UserDerivedAttribute();
                derivedAttribute.setDerivedSchema(derivedSchema);
                derivedAttribute.setOwner(syncopeUser);
                syncopeUser.addDerivedAttribute(derivedAttribute);
            }
        }

        // Check if there is some mandatory schema defined for which no value
        // has been provided
        List<UserSchema> allUserSchemas = schemaDAO.findAll(UserSchema.class);
        for (UserSchema userSchema : allUserSchemas) {
            if (syncopeUser.getAttribute(userSchema.getName()) == null
                    && userSchema.isMandatory()) {

                log.error("Mandatory schema " + userSchema.getName()
                        + " not provided with values");

                requiredValuesMissing.addElement(userSchema.getName());
            }
        }

        // 3. roles
        SyncopeRole role = null;
        for (Long roleId : userTO.getRoles()) {
            role = syncopeRoleDAO.find(roleId);

            if (role == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid role " + roleId);
                }
            } else {
                syncopeUser.addRole(role);
                role.addUser(syncopeUser);
            }
        }

        // 4. resources
        Resource resource = null;
        for (String resourceName : userTO.getResources()) {
            resource = resourceDAO.find(resourceName);

            if (resource == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Ignoring invalid resource " + resourceName);
                }
            } else {
                syncopeUser.addResource(resource);
                resource.addUser(syncopeUser);
            }
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }
        if (!invalidValues.getElements().isEmpty()) {
            compositeErrorException.addException(invalidValues);
        }
        if (!invalidUniques.getElements().isEmpty()) {
            compositeErrorException.addException(invalidUniques);
        }
        if (!invalidDerivedSchemas.getElements().isEmpty()) {
            compositeErrorException.addException(invalidDerivedSchemas);
        }
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return syncopeUser;
    }

    public UserTO getUserTO(SyncopeUser user) {
        UserTO userTO = new UserTO();
        userTO.setId(user.getId());
        userTO.setCreationTime(user.getCreationTime());
        userTO.setToken(user.getToken());
        userTO.setTokenExpireTime(user.getTokenExpireTime());
        userTO.setPassword(user.getPassword());

        AttributeTO attributeTO = null;
        for (AbstractAttribute attribute : user.getAttributes()) {
            attributeTO = new AttributeTO();
            attributeTO.setSchema(attribute.getSchema().getName());
            attributeTO.setValues(attribute.getAttributeValuesAsStrings());

            userTO.addAttribute(attributeTO);
        }

        for (AbstractDerivedAttribute derivedAttribute :
                user.getDerivedAttributes()) {

            attributeTO = new AttributeTO();
            attributeTO.setSchema(
                    derivedAttribute.getDerivedSchema().getName());
            attributeTO.setValues(Collections.singleton(
                    derivedAttribute.getValue(user.getAttributes())));

            userTO.addDerivedAttribute(attributeTO);
        }

        for (SyncopeRole role : user.getRoles()) {
            userTO.addRole(role.getId());
        }

        for (Resource resource : user.getResources()) {
            userTO.addResource(resource.getName());
        }

        return userTO;
    }
}
