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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AccountPolicy;
import org.syncope.core.persistence.beans.PasswordPolicy;
import org.syncope.core.persistence.beans.Policy;
import org.syncope.core.persistence.beans.SyncPolicy;
import org.syncope.core.persistence.dao.PolicyDAO;
import org.syncope.types.PolicyType;

@Repository
public class PolicyDAOImpl extends AbstractDAOImpl
        implements PolicyDAO {

    @Override
    public Policy find(final Long id) {
        return entityManager.find(Policy.class, id);
    }

    @Override
    public List<? extends Policy> find(final PolicyType type) {
        final Query query = entityManager.createQuery(
                "SELECT e FROM Policy e WHERE e.type=:type");
        query.setParameter("type", type);

        return query.getResultList();
    }

    @Override
    public PasswordPolicy getGlobalPasswordPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_PASSWORD);
        return policies == null || policies.isEmpty()
                ? null : (PasswordPolicy) policies.get(0);
    }

    @Override
    public AccountPolicy getGlobalAccountPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_ACCOUNT);
        return policies == null || policies.isEmpty()
                ? null : (AccountPolicy) policies.get(0);
    }

    @Override
    public SyncPolicy getGlobalSyncPolicy() {
        List<? extends Policy> policies = find(PolicyType.GLOBAL_SYNC);
        return policies == null || policies.isEmpty()
                ? null : (SyncPolicy) policies.get(0);
    }

    @Override
    public List<Policy> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM Policy e");
        return query.getResultList();
    }

    @Override
    public Policy save(final Policy policy) {
        return entityManager.merge(policy);
    }

    @Override
    public void delete(final Long id) {
        entityManager.remove(find(id));
    }
}
