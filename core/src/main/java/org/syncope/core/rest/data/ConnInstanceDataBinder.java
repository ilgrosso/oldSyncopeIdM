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
package org.syncope.core.rest.data;

import java.util.Map;
import javassist.NotFoundException;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.util.ConnBundleManager;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ConnInstanceDataBinder {

    private static final String[] IGNORE_PROPERTIES = {
        "id", "resources"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    public ConnInstance getConnInstance(
            final ConnInstanceTO connectorInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceTO.getBundleName() == null) {
            requiredValuesMissing.addElement("bundlename");
        }

        if (connectorInstanceTO.getVersion() == null) {
            requiredValuesMissing.addElement("bundleversion");
        }

        if (connectorInstanceTO.getConnectorName() == null) {
            requiredValuesMissing.addElement("connectorname");
        }

        if (connectorInstanceTO.getConfiguration() == null
                || connectorInstanceTO.getConfiguration().isEmpty()) {
            requiredValuesMissing.addElement("configuration");
        }

        ConnInstance connectorInstance = new ConnInstance();

        BeanUtils.copyProperties(
                connectorInstanceTO, connectorInstance, IGNORE_PROPERTIES);

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnInstance updateConnInstance(
            final long connInstanceId,
            final ConnInstanceTO connInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connInstanceId == 0) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnInstance connInstance = connectorInstanceDAO.find(connInstanceId);

        if (connInstanceTO.getBundleName() != null) {
            connInstance.setBundleName(connInstanceTO.getBundleName());
        }

        if (connInstanceTO.getVersion() != null) {
            connInstance.setVersion(connInstanceTO.getVersion());
        }

        if (connInstanceTO.getConnectorName() != null) {
            connInstance.setConnectorName(connInstanceTO.getConnectorName());
        }

        if (connInstanceTO.getConfiguration() != null
                && !connInstanceTO.getConfiguration().isEmpty()) {

            connInstance.setConfiguration(connInstanceTO.getConfiguration());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(connInstanceTO.getDisplayName());
        }

        connInstance.setCapabilities(connInstanceTO.getCapabilities());

        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connInstance;
    }

    public ConnInstanceTO getConnInstanceTO(final ConnInstance connInstance)
            throws NotFoundException {

        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connInstance.getId() != null
                ? connInstance.getId().longValue() : 0L);

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                connBundleManager.getConfigurationProperties(
                connInstance.getBundleName(),
                connInstance.getVersion(),
                connInstance.getConnectorName());

        BeanUtils.copyProperties(
                connInstance, connInstanceTO, IGNORE_PROPERTIES);

        ConnConfPropSchema connConfPropSchema;
        ConfigurationProperty configurationProperty;

        Map<String, ConnConfProperty> connInstanceToConfMap =
                connInstanceTO.getConfigurationMap();
        for (String propName : properties.getPropertyNames()) {
            configurationProperty = properties.getProperty(propName);

            if (!connInstanceToConfMap.containsKey(propName)) {
                connConfPropSchema = new ConnConfPropSchema();
                connConfPropSchema.setName(
                        configurationProperty.getName());
                connConfPropSchema.setDisplayName(
                        configurationProperty.getDisplayName(propName));
                connConfPropSchema.setHelpMessage(
                        configurationProperty.getHelpMessage(propName));
                connConfPropSchema.setRequired(
                        configurationProperty.isRequired());
                connConfPropSchema.setType(
                        configurationProperty.getType().getName());

                ConnConfProperty property = new ConnConfProperty();
                property.setSchema(connConfPropSchema);
                connInstanceTO.addConfiguration(property);
            } else {
                connInstanceToConfMap.get(propName).getSchema().
                        setDisplayName(
                        configurationProperty.getDisplayName(propName));
            }
        }
        return connInstanceTO;
    }

    /**
     * Get connector object TO from a connector object.
     *
     * @param connObject connector object.
     * @return connector object TO.
     */
    public ConnObjectTO getConnObjectTO(final ConnectorObject connObject) {
        final ConnObjectTO connObjectTO = new ConnObjectTO();

        for (Attribute attr : connObject.getAttributes()) {
            AttributeTO attrTO = new AttributeTO();
            attrTO.setSchema(attr.getName());

            if (attr.getValue() != null) {
                for (Object value : attr.getValue()) {
                    if (value != null) {
                        attrTO.addValue(value.toString());
                    }
                }
            }

            connObjectTO.addAttribute(attrTO);
        }

        return connObjectTO;
    }
}
