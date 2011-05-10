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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractAttributeValue;
import org.syncope.core.persistence.dao.AttributeValueDAO;

@Repository
public class AttributeValueDAOImpl extends AbstractDAOImpl
        implements AttributeValueDAO {

    @Override
    public <T extends AbstractAttributeValue> T find(
            final Long id, final Class<T> reference) {

        return entityManager.find(reference, id);
    }

    @Override
    public <T extends AbstractAttributeValue> boolean nonUniqueAttributeValue(
            final T attributeValue) {

        Query query = entityManager.createQuery(
                "SELECT DISTINCT e FROM "
                + attributeValue.getClass().getSimpleName()
                + " e WHERE e.attribute.schema = :schema AND "
                + " ((e.stringValue IS NOT NULL "
                + "AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL "
                + "AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL "
                + "AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL "
                + "AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL "
                + "AND e.doubleValue = :doubleValue))");

        query.setParameter("schema", attributeValue.getAttribute().getSchema());
        query.setParameter("stringValue", attributeValue.getStringValue());
        query.setParameter("booleanValue", attributeValue.getBooleanValue());
        query.setParameter("dateValue", attributeValue.getDateValue());
        query.setParameter("longValue", attributeValue.getLongValue());
        query.setParameter("doubleValue", attributeValue.getDoubleValue());

        Set<Long> distinctOwners = new HashSet<Long>();
        for (Object foundValue : query.getResultList()) {
            distinctOwners.add(
                    ((T) foundValue).getAttribute().getOwner().getId());
        }
        return distinctOwners.size() > 1;
    }

    @Override
    public <T extends AbstractAttributeValue> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttributeValue> T save(final T attributeValue) {
        return entityManager.merge(attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> void delete(final Long id,
            final Class<T> reference) {

        T attributeValue = find(id, reference);
        if (attributeValue == null) {
            return;
        }

        delete(attributeValue);
    }

    @Override
    public <T extends AbstractAttributeValue> void delete(
            final T attributeValue) {

        if (attributeValue.getAttribute() != null) {
            attributeValue.getAttribute().removeValue(attributeValue);
        }

        entityManager.remove(attributeValue);
    }
}