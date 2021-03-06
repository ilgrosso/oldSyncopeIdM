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
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Entitlement;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.membership.Membership;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.EntitlementDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.util.EntitlementUtil;

@Repository
public class RoleDAOImpl extends AbstractDAOImpl implements RoleDAO {

    @Autowired
    private EntitlementDAO entitlementDAO;

    @Override
    public SyncopeRole find(final Long id) {
        TypedQuery<SyncopeRole> query = entityManager.createQuery(
                "SELECT e FROM SyncopeRole e WHERE e.id = :id",
                SyncopeRole.class);
        query.setParameter("id", id);

        SyncopeRole result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
        }

        return result;
    }

    @Override
    public List<SyncopeRole> find(final String name) {
        Query query = entityManager.createQuery(
                "SELECT e FROM SyncopeRole e WHERE e.name = :name");
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public SyncopeRole find(final String name, final Long parentId) {
        Query query;
        if (parentId != null) {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "r.name=:name AND r.parent.id=:parentId");
            query.setParameter("parentId", parentId);
        } else {
            query = entityManager.createQuery(
                    "SELECT r FROM SyncopeRole r WHERE "
                    + "r.name=:name AND r.parent IS NULL");
        }
        query.setParameter("name", name);

        List<SyncopeRole> result = query.getResultList();
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<SyncopeRole> findByEntitlement(final Entitlement entitlement) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeRole.class.getSimpleName() + " e "
                + "WHERE :entitlement MEMBER OF e.entitlements");
        query.setParameter("entitlement", entitlement);

        return query.getResultList();
    }

    @Override
    public List<SyncopeRole> findByResource(final ExternalResource resource) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + SyncopeRole.class.getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources");
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public List<SyncopeRole> findChildren(final Long roleId) {
        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE "
                + "r.parent.id=:roleId");
        query.setParameter("roleId", roleId);
        return query.getResultList();
    }

    @Override
    public List<SyncopeRole> findAll() {
        Query query = entityManager.createQuery("SELECT e FROM SyncopeRole e");
        return query.getResultList();
    }

    @Override
    public List<Membership> findMemberships(final SyncopeRole role) {
        Query query = entityManager.createQuery(
                "SELECT e FROM " + Membership.class.getSimpleName() + " e"
                + " WHERE e.syncopeRole=:role");
        query.setParameter("role", role);

        return query.getResultList();
    }

    @Override
    public SyncopeRole save(final SyncopeRole role) {
        // reset account policy in case of inheritance
        if (role.isInheritAccountPolicy()) {
            role.setAccountPolicy(null);
        }

        // reset password policy in case of inheritance
        if (role.isInheritPasswordPolicy()) {
            role.setPasswordPolicy(null);
        }

        final SyncopeRole savedRole = entityManager.merge(role);
        entitlementDAO.saveEntitlementRole(savedRole);

        return savedRole;
    }

    @Override
    public void delete(final Long id) {
        SyncopeRole role = find(id);
        if (role == null) {
            return;
        }

        Query query = entityManager.createQuery(
                "SELECT r FROM SyncopeRole r WHERE r.parent.id=:id");
        query.setParameter("id", id);
        List<SyncopeRole> childrenRoles = query.getResultList();
        for (SyncopeRole child : childrenRoles) {
            delete(child.getId());
        }

        for (Membership membership : findMemberships(role)) {
            membership.setSyncopeRole(null);
            membership.getSyncopeUser().removeMembership(membership);
            membership.setSyncopeUser(null);

            entityManager.remove(membership);
        }

        role.getEntitlements().clear();

        role.setParent(null);
        entityManager.remove(role);

        entitlementDAO.delete(EntitlementUtil.getEntitlementNameFromRoleId(id));
    }
}
