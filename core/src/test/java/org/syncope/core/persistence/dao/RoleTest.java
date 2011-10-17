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
package org.syncope.core.persistence.dao;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.AbstractTest;

@Transactional
public class RoleTest extends AbstractTest {

    @Autowired
    private RoleDAO roleDAO;

    @Test
    public final void findAll() {
        List<SyncopeRole> list = roleDAO.findAll();
        assertEquals("did not get expected number of roles ", 8, list.size());
    }

    @Test
    public final void findChildren() {
        assertEquals(2, roleDAO.findChildren(4L).size());
    }

    @Test
    public final void find() {
        SyncopeRole role = roleDAO.find("root", null);
        assertNotNull("did not find expected role", role);
        role = roleDAO.find(null, null);
        assertNull("found role but did not expect it", role);
    }

    @Test
    public final void inheritedAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedAttributes().size());
    }

    @Test
    public final void inheritedDerivedAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedDerivedAttributes().size());
    }

    @Test
    public final void inheritedVirtualAttributes() {
        SyncopeRole director = roleDAO.find(7L);

        assertEquals(1, director.findInheritedVirtualAttributes().size());
    }

    @Test
    public final void save() {
        SyncopeRole role = new SyncopeRole();
        role.setName("secondChild");

        SyncopeRole rootRole = roleDAO.find("root", null);
        role.setParent(rootRole);

        role = roleDAO.save(role);

        SyncopeRole actual = roleDAO.find(role.getId());
        assertNotNull("expected save to work", actual);
    }

    @Test
    public final void delete() {
        SyncopeRole role = roleDAO.find(4L);
        roleDAO.delete(role.getId());

        SyncopeRole actual = roleDAO.find(4L);
        assertNull("delete did not work", actual);

        SyncopeRole children = roleDAO.find(7L);
        assertNull("delete of successors did not work", children);

    }
}
