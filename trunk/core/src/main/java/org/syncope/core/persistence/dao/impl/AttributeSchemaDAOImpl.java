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
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;

@Repository
public class AttributeSchemaDAOImpl extends AbstractDAOImpl
        implements AttributeSchemaDAO {

    @Autowired
    AttributeDAO attributeDAO;

    @Override
    public AttributeSchema find(String name) {
        AttributeSchema result =
                entityManager.find(AttributeSchema.class, name);
        if (isDeletedOrNotManaged(result)) {
            result = null;
        }

        return result;
    }

    @Override
    public List<AttributeSchema> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM AttributeSchema e");
        return query.getResultList();
    }

    @Override
    @Transactional
    public AttributeSchema save(AttributeSchema attributeSchema) {
        AttributeSchema result = entityManager.merge(attributeSchema);
        entityManager.flush();
        return result;
    }

    @Override
    @Transactional
    public void delete(String name) {
        AttributeSchema schema = find(name);
        if (schema == null) {
            return;
        }

        entityManager.remove(schema);
    }
}
