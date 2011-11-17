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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ConnInstanceDataBinder {

    private static final String[] ignoreProperties = {
        "id", "resources", "syncToken"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

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
                connectorInstanceTO, connectorInstance, ignoreProperties);

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnInstance updateConnInstance(
            Long connectorInstanceId,
            ConnInstanceTO connInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceId == null) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnInstance connInstance =
                connectorInstanceDAO.find(connectorInstanceId);

        if (connInstanceTO.getBundleName() != null) {
            connInstance.setBundleName(
                    connInstanceTO.getBundleName());
        }

        if (connInstanceTO.getVersion() != null) {
            connInstance.setVersion(connInstanceTO.getVersion());
        }

        if (connInstanceTO.getConnectorName() != null) {
            connInstance.setConnectorName(
                    connInstanceTO.getConnectorName());
        }

        if (connInstanceTO.getConfiguration() != null
                || connInstanceTO.getConfiguration().isEmpty()) {

            connInstance.setConfiguration(
                    connInstanceTO.getConfiguration());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(
                    connInstanceTO.getDisplayName());
        }

        connInstance.setCapabilities(
                connInstanceTO.getCapabilities());

        if (connInstanceTO.getSyncToken() == null) {
            connInstance.setSerializedSyncToken(null);
        }

        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connInstance;
    }

    public ConnInstanceTO getConnInstanceTO(ConnInstance connInstance) {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connInstance.getId());

        BeanUtils.copyProperties(
                connInstance, connInstanceTO, ignoreProperties);

        connInstanceTO.setSyncToken(
                connInstance.getSerializedSyncToken());

        return connInstanceTO;
    }
}