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
package org.syncope.core.rest;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.web.client.HttpStatusCodeException;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.SchemaType;

public class ResourceTestITCase extends AbstractTest {

    @Test
    @ExpectedException(value = SyncopeClientCompositeErrorException.class)
    public void createExistingResource() {
        final String resourceName = "ws-target-resource-1";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);

        restTemplate.postForObject(BASE_URL + "resource/create.json",
                resourceTO, ResourceTO.class);
    }

    @Test
    public void create() {
        String resourceName = "ws-target-resource-create";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(102L);

        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setField("uid");
        schemaMappingTO.setSchemaName("userId");
        schemaMappingTO.setSchemaType(SchemaType.UserSchema);
        resourceTO.addMapping(schemaMappingTO);

        schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setField("icon");
        schemaMappingTO.setSchemaName("icon");
        schemaMappingTO.setSchemaType(SchemaType.RoleSchema);
        resourceTO.addMapping(schemaMappingTO);

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

    @Test
    public void update() {
        final String resourceName = "ws-target-resource-update";
        ResourceTO resourceTO = new ResourceTO();

        resourceTO.setName(resourceName);
        resourceTO.setConnectorId(101L);

        List<SchemaMappingTO> schemaMappingTOs =
                new ArrayList<SchemaMappingTO>();

        // Update with an existing and already assigned mapping
        SchemaMappingTO schemaMappingTO = new SchemaMappingTO();
        schemaMappingTO.setId(112L);
        schemaMappingTO.setField("test3");
        schemaMappingTO.setSchemaName("username");
        schemaMappingTO.setSchemaType(SchemaType.UserSchema);
        schemaMappingTOs.add(schemaMappingTO);

        // Update defining new mapping
        for (int i = 4; i < 6; i++) {
            schemaMappingTO = new SchemaMappingTO();
            schemaMappingTO.setField("test" + i);
            schemaMappingTO.setSchemaName("username");
            schemaMappingTO.setSchemaType(SchemaType.UserSchema);
            schemaMappingTOs.add(schemaMappingTO);
        }

        resourceTO.setMappings(schemaMappingTOs);

        ResourceTO actual = restTemplate.postForObject(
                BASE_URL + "resource/update.json",
                resourceTO, ResourceTO.class);

        assertNotNull(actual);

        // check the existence

        List<SchemaMappingTO> mappings = actual.getMappings();

        assertNotNull(mappings);

        assertEquals(3, mappings.size());
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
        final String resourceName = "ws-target-resource-1";

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
        List<ResourceTO> actuals = restTemplate.getForObject(
                BASE_URL + "resource/list.json", List.class);

        assertNotNull(actuals);

        assertFalse(actuals.isEmpty());
    }
}