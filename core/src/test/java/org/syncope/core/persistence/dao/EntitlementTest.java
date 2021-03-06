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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.AbstractTest;

@Transactional
public class EntitlementTest extends AbstractTest {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Test
    public void findAll() {
        List<Entitlement> list = entitlementDAO.findAll();
        // 59 real entitlements + 9 role entitlements
        assertEquals("did not get expected number of entitlements ",
                74, list.size());
    }

    @Test
    public void findByName() {
        Entitlement entitlement = entitlementDAO.find("base");
        assertNotNull("did not find expected entitlement",
                entitlement);
    }

    @Test
    public void save() {
        Entitlement entitlement = new Entitlement();
        entitlement.setName("another");

        entitlementDAO.save(entitlement);

        Entitlement actual = entitlementDAO.find("another");
        assertNotNull("expected save to work", actual);
        assertEquals(entitlement, actual);
    }

    @Test
    public void delete() {
        Entitlement entitlement = entitlementDAO.find("base");
        assertNotNull("did not find expected entitlement",
                entitlement);

        List<SyncopeRole> roles = roleDAO.findByEntitlement(entitlement);
        assertEquals("expected two roles", 2, roles.size());

        entitlementDAO.delete("base");

        roles = roleDAO.findByEntitlement(entitlement);
        assertTrue(roles.isEmpty());
    }
}
