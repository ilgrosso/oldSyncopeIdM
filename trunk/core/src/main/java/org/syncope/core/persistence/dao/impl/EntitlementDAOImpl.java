/*
 *  Copyright 2010 ilgrosso.
 * 
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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.SyncopeRole;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;

@Repository
public class EntitlementDAOImpl extends AbstractDAOImpl implements EntitlementDAO {

    @Autowired
    SyncopeRoleDAO syncopeRoleDAO;

    @Override
    public Entitlement find(String name) {
        Entitlement result = entityManager.find(Entitlement.class, name);
        if (isDeletedOrNotManaged(result))
            result = null;

        return result;
    }

    @Override
    public List<Entitlement> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Entitlement e");
        return query.getResultList();
    }

    @Override
    public Entitlement save(Entitlement entitlement) {
        Entitlement result = entityManager.merge(entitlement);
        entityManager.flush();
        return result;
    }

    @Override
    public void delete(String name) {
        Entitlement entitlement = find(name);
        if (entitlement == null)
            return;

        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + ":entitlement MEMBER OF r.entitlements");
        query.setParameter("entitlement", entitlement);
        List<SyncopeRole> roles = query.getResultList();
        for (SyncopeRole role : roles) {
            role.removeEntitlement(entitlement);
            syncopeRoleDAO.save(role);
        }

        entityManager.remove(find(name));
    }
}
