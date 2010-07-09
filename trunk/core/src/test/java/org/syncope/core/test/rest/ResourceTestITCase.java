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

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.ResourceTOs;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.core.persistence.dao.SchemaDAO;

public class ResourceTestITCase extends AbstractTestITCase {

    @Autowired
    SchemaDAO schemaDAO;

    @Test
    public void create() {
        final String resourceName = "ws-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(100L);

        SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();

        SchemaMappingTO schemaMappingTO = null;

        for (int i = 0; i < 3; i++) {
            schemaMappingTO = new SchemaMappingTO();
            schemaMappingTO.setField("test" + i);
            schemaMappingTO.setUserSchema("username");
            schemaMappingTO.setRoleSchema("icon");
            schemaMappingTOs.addMapping(schemaMappingTO);
        }

        resourceTO.setMappings(schemaMappingTOs);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        actual = restTemplate.getForObject(
                BASE_URL + "resource/read/{resourceName}.json",
                ResourceTO.class,
                resourceName);

        assertNotNull(actual);
    }

    @Test
    public void updateWithException() {
        try {

            ResourceTO resourceTO = new ResourceTO();

            resourceTO.setName("resourcenotfound");

            restTemplate.postForObject(BASE_URL + "resource/update.json",
                    resourceTO, ResourceTO.class);

        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    //@Test
    public void update() {
        final String resourceName = "ws-target-resource-update";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(101L);

        SchemaMappingTOs schemaMappingTOs = new SchemaMappingTOs();

        SchemaMappingTO schemaMappingTO = null;

        for (int i = 3; i < 6; i++) {
            schemaMappingTO = new SchemaMappingTO();
            schemaMappingTO.setField("test" + i);
            schemaMappingTO.setUserSchema("username");
            schemaMappingTO.setRoleSchema("icon");
            schemaMappingTOs.addMapping(schemaMappingTO);
        }

        resourceTO.setMappings(schemaMappingTOs);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/update.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        SchemaMappingTOs mappings = actual.getMappings();

        assertNotNull(mappings);

        assertTrue(mappings.getMappings().size() == 3);
    }

    @Test
    public void deleteWithException() {
        try {

            restTemplate.delete(
                    BASE_URL + "resource/delete/{resourceName}.json",
                    "resourcenotfound");

        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void delete() {
        final String resourceName = "ws-target-resource-delete";

        restTemplate.delete(
                BASE_URL + "resource/delete/{resourceName}.json",
                resourceName);

        try {

            restTemplate.getForObject(
                    BASE_URL + "resource/read/{resourceName}.json",
                    ResourceTO.class,
                    resourceName);

        } catch (HttpStatusCodeException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Test
    public void list() {
        ResourceTOs actuals = restTemplate.getForObject(
                BASE_URL + "resource/list.json", ResourceTOs.class);

        assertNotNull(actuals);

        assertFalse(actuals.getResources().isEmpty());
    }
}
