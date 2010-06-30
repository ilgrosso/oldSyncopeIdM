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
package org.syncope.core.test.persistence;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.ConnectorInstance;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Transactional
public class ResourceDAOTest extends AbstractTest {

    @Autowired
    ResourceDAO resourceDAO;

    @Autowired
    ConnectorInstanceDAO connectorInstanceDAO;

    @Autowired
    SchemaMappingDAO schemaMappingDAO;

    @Autowired
    SyncopeUserDAO syncopeUserDAO;

    @Test
    public final void findById() {
        Resource resource =
                resourceDAO.find("ws-target-resource-1");

        assertNotNull("findById did not work", resource);

        ConnectorInstance connector = resource.getConnector();

        assertNotNull("connector not found", connector);

        assertEquals("invalid connector name", "WebServiceConnector",
                connector.getConnectorName());

        assertEquals("invalid bundle name",
                "org.syncope.identityconnectors.bundles.staticws",
                connector.getBundleName());

        assertEquals("invalid bundle version",
                "0.1-SNAPSHOT", connector.getVersion());

        Set<SchemaMapping> mappings = resource.getMappings();

        assertNotNull("mappings not found", mappings);

        assertFalse("no mapping specified", mappings.isEmpty());

        assertTrue(mappings.iterator().next().getId() == 100L);
    }

    @Test
    public final void save() throws ClassNotFoundException {
        Resource resource = new Resource();
        resource.setName("ws-target-resource-3");

        // specify the connector
        ConnectorInstance connector = connectorInstanceDAO.find(100L);

        assertNotNull("connector not found", connector);

        resource.setConnector(connector);

        // specify a mapping
        List<SchemaMapping> mappings = schemaMappingDAO.findAll();

        assertNotNull("mappings not found", mappings);

        assertFalse("no mapping specified", mappings.isEmpty());

        resource.setMappings(new HashSet<SchemaMapping>(mappings));

        // specify an user schema
        SyncopeUser user = syncopeUserDAO.find(1L);

        assertNotNull("user not found", user);

        resource.setUsers(Collections.singleton(user));

        // save the resource
        Resource actual = resourceDAO.save(resource);

        assertNotNull(actual);
    }

    @Test
    public final void delete() {
        Resource resource = resourceDAO.find("ws-target-resource-2");

        assertNotNull("find to delete did not work", resource);

        resourceDAO.delete(resource.getName());

        Resource actual = resourceDAO.find("ws-target-resource-2");
        assertNull("delete did not work", actual);
    }
}