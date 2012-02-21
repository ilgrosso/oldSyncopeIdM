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
package org.syncope.core.test.rest;

import java.io.InputStream;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ConnectorBundleTOs;
import org.syncope.client.to.ConnectorInstanceTO;
import org.syncope.client.to.ConnectorInstanceTOs;
import org.syncope.client.to.PropertyTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.identityconnectors.bundles.staticwebservice.WebServiceConnector;

public class ConnectorInstanceTestITCase extends AbstractTestITCase {

    protected static String bundles_version;
    protected static String bundles_directory;

    @Before
    public void init() {
        Properties props = new java.util.Properties();
        try {
            InputStream propStream =
                    getClass().getResourceAsStream(
                    "/bundles.properties");
            props.load(propStream);
            bundles_version = props.getProperty("bundles.version");
            bundles_directory = props.getProperty("bundles.directory");
        } catch (Throwable t) {
            LOG.error("Could not load bundles.properties", t);
        }
        assertNotNull(bundles_version);
        assertNotNull(bundles_directory);
    }

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createWithException() {
        ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();

        restTemplate.postForObject(BASE_URL + "connector/create.json",
                connectorTO, ConnectorInstanceTO.class);
    }

    @Test
    public void create() {

        ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();

        // set connector version
        connectorTO.setVersion(bundles_version);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getSimpleName());

        // set bundle name
        connectorTO.setBundleName(
                "org.syncope.identityconnectors.bundles.staticws");

        // set the connector configuration using PropertyTO
        Set<PropertyTO> conf = new HashSet<PropertyTO>();

        PropertyTO endpoint = new PropertyTO();
        endpoint.setKey("endpoint");
        endpoint.setValue("http://localhost:8888/wstarget/services");

        PropertyTO servicename = new PropertyTO();
        servicename.setKey("servicename");
        servicename.setValue("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        ConnectorInstanceTO actual =
                (ConnectorInstanceTO) restTemplate.postForObject(
                BASE_URL + "connector/create.json",
                connectorTO, ConnectorInstanceTO.class);

        assertNotNull(actual);

        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());

        Throwable t = null;

        // check for the updating

        connectorTO.setId(actual.getId());

        try {

            restTemplate.postForObject(
                    BASE_URL + "connector/update.json",
                    connectorTO, ConnectorInstanceTO.class);

        } catch (HttpStatusCodeException e) {
            LOG.error("update failed", e);
            t = e;
        }

        assertNull(t);

        // check also for the deletion of the created object

        t = null;

        try {

            restTemplate.delete(
                    BASE_URL + "connector/delete/{connectorId}.json",
                    actual.getId().toString());

        } catch (HttpStatusCodeException e) {
            LOG.error("delete failed", e);
            t = e;
        }

        assertNull(t);

        // check the non existence

        try {

            restTemplate.getForObject(
                    BASE_URL + "connector/read/{connectorId}",
                    ConnectorInstanceTO.class,
                    actual.getId().toString());

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void update() {
        ConnectorInstanceTO connectorTO = new ConnectorInstanceTO();

        // set connector instance id
        connectorTO.setId(100L);

        // set connector version
        connectorTO.setVersion(bundles_version);

        // set connector name
        connectorTO.setConnectorName(WebServiceConnector.class.getSimpleName());

        // set bundle name
        connectorTO.setBundleName(
                "org.syncope.identityconnectors.bundles.staticws");

        // set the connector configuration using PropertyTO
        Set<PropertyTO> conf = new HashSet<PropertyTO>();

        PropertyTO endpoint = new PropertyTO();
        endpoint.setKey("endpoint");
        endpoint.setValue("http://localhost:8888/wstarget/services");

        PropertyTO servicename = new PropertyTO();
        servicename.setKey("servicename");
        servicename.setValue("Provisioning");

        conf.add(endpoint);
        conf.add(servicename);

        // set connector configuration
        connectorTO.setConfiguration(conf);

        ConnectorInstanceTO actual =
                (ConnectorInstanceTO) restTemplate.postForObject(
                BASE_URL + "connector/update.json",
                connectorTO, ConnectorInstanceTO.class);

        assertNotNull(actual);

        actual = restTemplate.getForObject(
                BASE_URL + "connector/read/{connectorId}",
                ConnectorInstanceTO.class,
                actual.getId().toString());

        assertNotNull(actual);
        assertEquals(actual.getBundleName(), connectorTO.getBundleName());
        assertEquals(actual.getConnectorName(), connectorTO.getConnectorName());
        assertEquals(actual.getVersion(), connectorTO.getVersion());
    }

    @Test
    public void deleteWithException() {
        try {

            restTemplate.delete(
                    BASE_URL + "connector/delete/{connectorId}.json", "0");

        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode(), HttpStatus.NOT_FOUND);
        }
    }

    @Test
    public void list() {
        ConnectorInstanceTOs connectorInstanceTOs = restTemplate.getForObject(
                BASE_URL + "connector/list.json", ConnectorInstanceTOs.class);

        assertNotNull(connectorInstanceTOs);
        assertFalse(connectorInstanceTOs.getInstances().isEmpty());
    }

    @Test
    public void read() {
        ConnectorInstanceTO connectorInstanceTO = restTemplate.getForObject(
                BASE_URL + "connector/read/{connectorId}.json",
                ConnectorInstanceTO.class, "100");

        assertNotNull(connectorInstanceTO);
    }

    @Test
    public void check() {
        Boolean verify = restTemplate.getForObject(
                BASE_URL + "connector/check/{connectorId}.json",
                Boolean.class, 100L);

        assertTrue(verify);
    }

    @Test
    public void getBundles() {
        ConnectorBundleTOs bundles = restTemplate.getForObject(
                BASE_URL + "connector/getBundles.json",
                ConnectorBundleTOs.class);

        assertNotNull(bundles);

        assertFalse(bundles.getBundles().isEmpty());
    }
}