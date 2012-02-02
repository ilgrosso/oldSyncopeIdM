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
import org.syncope.core.persistence.beans.SyncopeLogger;
import org.syncope.core.persistence.dao.LoggerDAO;

@Repository
public class LoggerDAOImpl extends AbstractDAOImpl implements LoggerDAO {

    @Override
    public SyncopeLogger find(final String name) {
        return entityManager.find(SyncopeLogger.class, name);
    }

    @Override
    public List<SyncopeLogger> findAll() {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeLogger.class.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public SyncopeLogger save(final SyncopeLogger logger) {
        return entityManager.merge(logger);
    }

    @Override
    public void delete(final String name) {
        SyncopeLogger logger = find(name);
        if (logger == null) {
            return;
        }

        entityManager.remove(logger);
    }
}
