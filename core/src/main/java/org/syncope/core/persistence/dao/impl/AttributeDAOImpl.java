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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Attribute;
import org.syncope.core.persistence.beans.SyncopeRole;
import org.syncope.core.persistence.beans.SyncopeUser;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.persistence.dao.SyncopeUserDAO;

@Repository
public class AttributeDAOImpl extends AbstractDAOImpl
        implements AttributeDAO {

    @Autowired
    SyncopeUserDAO syncopeUserDAO;
    @Autowired
    SyncopeRoleDAO syncopeRoleDAO;

    @Override
    public Attribute find(long id) {
        Attribute result = entityManager.find(Attribute.class, id);
        if (isDeletedOrNotManaged(result))
            result = null;

        return result;
    }

    @Override
    public List<Attribute> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM Attribute e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public Attribute save(Attribute attribute) {
        Attribute result = entityManager.merge(attribute);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(long id) {
        Attribute attribute = find(id);
        if (attribute == null) {
            return;
        }

        Query query = entityManager.createQuery(
                "SELECT u FROM SyncopeUser u WHERE "
                + ":attribute MEMBER OF u.attributes");
        query.setParameter("attribute", attribute);
        List<SyncopeUser> users = query.getResultList();
        for (SyncopeUser user : users) {
            user.removeAttribute(attribute);
            syncopeUserDAO.save(user);
        }

        if (!isDeletedOrNotManaged(attribute)) {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + ":attribute MEMBER OF r.attributes");
            query.setParameter("attribute", attribute);
            List<SyncopeRole> roles = query.getResultList();
            for (SyncopeRole role : roles) {
                role.removeAttribute(attribute);
                syncopeRoleDAO.save(role);
            }
        }

        entityManager.remove(attribute);
    }
}
