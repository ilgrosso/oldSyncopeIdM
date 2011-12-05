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
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.syncope.client.to.ConnBundleTO;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.util.ConnBundleManager;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.data.ConnInstanceDataBinder;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/connector")
public class ConnInstanceController extends AbstractController {

    @Autowired
    private ConnInstanceLoader connInstanceLoader;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ResourceDAO respourceDAO;

    @Autowired
    private ResourceDataBinder resourceDataBinder;

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnBundleManager bundleManager;

    @PreAuthorize("hasRole('CONNECTOR_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ConnInstanceTO create(final HttpServletResponse response,
            @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("ConnInstance create called with configuration {}",
                connectorTO);

        ConnInstance connInstance = binder.getConnInstance(connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (Throwable t) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(t.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ConnInstanceTO update(
            @RequestBody final ConnInstanceTO connectorTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Connector update called with configuration {}", connectorTO);

        ConnInstance connInstance = binder.updateConnInstance(
                connectorTO.getId(), connectorTO);

        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (RuntimeException e) {
            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException invalidConnInstance =
                    new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidConnInstance);
            invalidConnInstance.addElement(e.getMessage());

            scce.addException(invalidConnInstance);
            throw scce;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{connectorId}")
    public void delete(@PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);

        if (connInstance == null) {
            LOG.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        connInstanceDAO.delete(connectorId);
    }

    @PreAuthorize("hasRole('CONNECTOR_LIST')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ConnInstanceTO> list(
            @RequestParam(value = "lang", required = false) final String lang)
            throws NotFoundException {

        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnInstance> connInstances = connInstanceDAO.findAll();

        List<ConnInstanceTO> connInstanceTOs =
                new ArrayList<ConnInstanceTO>();
        for (ConnInstance connector : connInstances) {
            connInstanceTOs.add(binder.getConnInstanceTO(connector));
        }

        return connInstanceTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{connectorId}")
    public ConnInstanceTO read(
            @PathVariable("connectorId") Long connectorId)
            throws NotFoundException {

        ConnInstance connInstance = connInstanceDAO.find(connectorId);

        if (connInstance == null) {
            LOG.error("Could not find connector '" + connectorId + "'");

            throw new NotFoundException(String.valueOf(connectorId));
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/check/{resourceName}")
    public ModelAndView check(@PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        final ExternalResource resource = respourceDAO.find(resourceName);

        if (resource == null) {
            LOG.error("Missing resource: {}", resourceName);
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        ConnectorFacadeProxy connector;

        try {
            connector = connInstanceLoader.getConnector(resource);
        } catch (BeansException e) {
            throw new NotFoundException(
                    "Connector " + resource.getConnector().getId(), e);
        }

        ModelAndView mav = new ModelAndView();

        Boolean verify = Boolean.FALSE;
        try {
            if (connector != null) {
                connector.validate();
                verify = Boolean.TRUE;
            }
        } catch (RuntimeException ignore) {
            LOG.warn("Connector validation failed", ignore);
        }

        mav.addObject(verify);

        return mav;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/bundle/list")
    public List<ConnBundleTO> getBundles(
            @RequestParam(value = "lang", required = false) final String lang)
            throws NotFoundException, MissingConfKeyException {

        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        ConnectorInfoManager manager =
                bundleManager.getConnectorManager();

        List<ConnectorInfo> bundles = manager.getConnectorInfos();

        if (LOG.isDebugEnabled() && bundles != null) {
            LOG.debug("#Bundles: {}", bundles.size());

            for (ConnectorInfo bundle : bundles) {
                LOG.debug("Bundle: {}", bundle.getConnectorDisplayName());
            }
        }

        ConnBundleTO connectorBundleTO;
        ConnectorKey key;
        ConfigurationProperties properties;

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<ConnBundleTO>();
        if (bundles != null) {
            for (ConnectorInfo bundle : bundles) {
                connectorBundleTO = new ConnBundleTO();
                connectorBundleTO.setDisplayName(
                        bundle.getConnectorDisplayName());

                key = bundle.getConnectorKey();

                LOG.debug("\nBundle name: {}"
                        + "\nBundle version: {}"
                        + "\nBundle class: {}",
                        new Object[]{
                            key.getBundleName(),
                            key.getBundleVersion(),
                            key.getConnectorName()});

                connectorBundleTO.setBundleName(key.getBundleName());
                connectorBundleTO.setConnectorName(key.getConnectorName());
                connectorBundleTO.setVersion(key.getBundleVersion());

                properties = bundleManager.getConfigurationProperties(bundle);

                ConnConfPropSchema connConfPropSchema;
                ConfigurationProperty configurationProperty;

                for (String propName : properties.getPropertyNames()) {
                    connConfPropSchema = new ConnConfPropSchema();

                    configurationProperty = properties.getProperty(propName);

                    // set name
                    connConfPropSchema.setName(
                            configurationProperty.getName());

                    // set display name
                    connConfPropSchema.setDisplayName(
                            configurationProperty.getDisplayName(propName));

                    // set help message
                    connConfPropSchema.setHelpMessage(
                            configurationProperty.getHelpMessage(propName));

                    // set if mandatory
                    connConfPropSchema.setRequired(
                            configurationProperty.isRequired());

                    // set type
                    connConfPropSchema.setType(
                            configurationProperty.getType().getName());

                    connectorBundleTO.addProperty(connConfPropSchema);
                }

                LOG.debug("Bundle properties: {}",
                        connectorBundleTO.getProperties());

                connectorBundleTOs.add(connectorBundleTO);
            }
        }

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/schema/list")
    public List<String> getSchemaNames(
            @RequestBody final ResourceTO resourceTO)
            throws NotFoundException {

        return getSchemaNames(
                resourceDataBinder.getResource(resourceTO), false);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/schema/list/all")
    public List<String> getAllSchemaNames(
            @RequestBody final ResourceTO resourceTO)
            throws NotFoundException {

        return getSchemaNames(
                resourceDataBinder.getResource(resourceTO), true);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{connectorId}/configurationProperty/list")
    public List<ConnConfProperty> getConfigurationProperties(
            @PathVariable("connectorId") final Long connectorId)
            throws NotFoundException {

        final ConnInstance connector = connInstanceDAO.find(connectorId);
        if (connector == null) {
            throw new NotFoundException(String.format(
                    "Connector instance with id %d not found", connectorId));
        }
        return new ArrayList<ConnConfProperty>(connector.getConfiguration());
    }

    private List<String> getSchemaNames(
            final ExternalResource resource, final boolean showall)
            throws NotFoundException {

        // We cannot use bean because this method could be used in phase of
        // resource definition or modification: bean couldn't exist or bean
        // couldn't be updated.
        // This is the reason why we should take a "not mature" connector
        // facade proxy to ask for schema names.

        return connInstanceLoader.createConnectorBean(resource).
                getSchema(showall);
    }
}