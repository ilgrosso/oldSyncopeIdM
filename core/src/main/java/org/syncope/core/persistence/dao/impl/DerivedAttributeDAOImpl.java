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
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.AbstractDerivedAttribute;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.dao.DerivedAttributeDAO;

@Repository
public class DerivedAttributeDAOImpl extends AbstractDAOImpl
        implements DerivedAttributeDAO {

    @Override
    public <T extends AbstractDerivedAttribute> T find(Long id, Class<T> reference) {
        T result = entityManager.find(reference, id);
        if (isDeletedOrNotManaged(result)) {
            result = null;
        }

        return result;
    }

    @Override
    public <T extends AbstractDerivedAttribute> List<T> findAll(
            Class<T> reference) {
        
        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public AbstractDerivedAttribute save(AbstractDerivedAttribute attribute) {
        AbstractDerivedAttribute result = entityManager.merge(attribute);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public <T extends AbstractDerivedAttribute> void delete(
            Long id, Class<T> reference) {

        T derivedAttribute = find(id, reference);
        if (derivedAttribute == null) {
            return;
        }

        entityManager.remove(derivedAttribute);
    }
}